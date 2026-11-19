package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.User
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.ChatFile
import com.example.data.remote.NativeParentPortalClient
import com.example.data.remote.RegistryHttpException
import com.example.data.remote.StudentChatDraft
import com.example.data.remote.readChatFile
import com.example.data.remote.studentChatAttachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ParentVerifiedMessagingPane(studentId: String, actor: User) {
    key(studentId, actor.id) { ScopedParentMessagingPane(studentId, actor) }
}

@Composable
private fun ScopedParentMessagingPane(studentId: String, actor: User) {
    val captured = remember { ApiClientProvider.interceptor.captureScope() }
    val client = remember { NativeParentPortalClient() }
    val coroutine = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    val allowed = actor.is_super_admin == true || "parent.send_message" in actor.permissions
    var teachers by remember { mutableStateOf<List<JSONObject>?>(null) }
    var teacherId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var file by remember { mutableStateOf<ChatFile?>(null) }
    var pending by remember { mutableStateOf<StudentChatDraft?>(null) }
    var canRetry by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf("") }
    var actionError by remember { mutableStateOf("") }
    var fileStatus by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var pickingFor by remember { mutableStateOf<String?>(null) }
    var savingFor by remember { mutableStateOf<Pair<String, ChatFile>?>(null) }

    val chooseDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = pickingFor
        pickingFor = null
        if (uri != null && target != null && target == teacherId && captured.current()) {
            busy = true
            coroutine.launch {
                try {
                    file = captured.io {
                        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                            if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
                        }
                        val bytes = requireNotNull(resolver.openInputStream(uri)).use { it.readChatFile() }
                        ChatFile(requireNotNull(name), bytes)
                    }
                    fileStatus = ""
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (captured.current()) fileStatus = "En fazla 10 MB PDF, PNG veya JPEG seçin."
                } finally {
                    busy = false
                }
            }
        }
    }
    val saveDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val request = savingFor
        savingFor = null
        if (uri != null && request != null && request.first == teacherId && captured.current()) {
            busy = true
            coroutine.launch {
                try {
                    captured.io {
                        requireNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(request.second.bytes) }
                    }
                    fileStatus = "Dosya seçilen konuma kaydedildi."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (captured.current()) fileStatus = "Dosya seçilen konuma yazılamadı."
                } finally {
                    busy = false
                }
            }
        }
    }

    LaunchedEffect(allowed) {
        if (allowed) {
            try { teachers = captured.io { (client.section(studentId, "teachers") as JSONArray).rows() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (captured.current()) historyError = "Öğretmenler yüklenemedi." }
        }
    }
    LaunchedEffect(teacherId, revision, allowed) {
        val partner = teacherId ?: return@LaunchedEffect
        if (!allowed) return@LaunchedEffect
        historyError = ""
        busy = true
        try {
            messages = captured.io {
                client.chat(studentId, partner).rows().also { rows ->
                    check(rows.all {
                        val mine = it.getBoolean("is_current_user")
                        it.getString("sender_id") == (if (mine) actor.id else partner) &&
                            it.getString("receiver_id") == (if (mine) partner else actor.id)
                    })
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (captured.current()) historyError = "Mesaj geçmişi yüklenemedi."
        } finally {
            busy = false
        }
    }

    fun submit(checkOnly: Boolean) {
        val partner = teacherId ?: return
        if (busy || !allowed || !captured.current()) return
        val unresolved = pending
        val draft = unresolved ?: StudentChatDraft(text.trim(), file)
        busy = true
        canRetry = false
        actionError = ""
        coroutine.launch {
            var reading = checkOnly
            try {
                if (!checkOnly) client.sendStudentChat(studentId, partner, actor.id, draft)
                reading = true
                val stored = client.studentChatReceipt(studentId, partner, actor.id, draft)
                captured.ensureCurrent()
                messages = (messages + stored).distinctBy { it.getString("id") }
                text = ""
                file = null
                pending = null
                fileStatus = ""
                revision++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (!captured.current()) return@launch
                val status = (failure as? RegistryHttpException)?.code
                if (!reading && status in setOf(413, 422)) {
                    pending = null
                    actionError = "Mesaj veya dosya kabul edilmedi. Taslağı düzenleyin."
                } else {
                    pending = draft
                    canRetry = reading && status == 404
                    actionError = "Gönderim sonucu doğrulanamadı."
                }
            } finally {
                busy = false
            }
        }
    }

    fun download(partner: String, row: JSONObject) {
        if (busy || savingFor != null) return
        busy = true
        fileStatus = ""
        coroutine.launch {
            try {
                val verified = client.downloadStudentChat(studentId, partner, row)
                captured.ensureCurrent()
                savingFor = partner to verified
                saveDocument.launch(verified.name)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                savingFor = null
                if (captured.current()) fileStatus = "Ek indirilemedi veya doğrulanamadı."
            } finally {
                busy = false
            }
        }
    }

    if (!captured.current()) return
    val partner = teacherId
    LazyColumn(
        Modifier.fillMaxSize().imePadding().testTag("parent_messages"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Öğretmenle iletişim", style = MaterialTheme.typography.titleLarge) }
        if (!allowed) {
            item { Text("Bu hesapla mesajlaşma yetkisi yok.") }
            return@LazyColumn
        }
        val contacts = teachers
        items(contacts.orEmpty(), key = { it.getString("id") }) { row ->
            val id = row.getString("id")
            OutlinedButton(enabled = !busy && pending == null && pickingFor == null && savingFor == null,
                onClick = {
                    if (teacherId != id) {
                        teacherId = id
                        messages = emptyList()
                        text = ""
                        file = null
                        actionError = ""
                        fileStatus = ""
                    }
                }, modifier = Modifier.heightIn(min = 48.dp).testTag("parent_teacher_$id")) {
                Text(row.getString("name"))
            }
        }
        if (contacts?.isEmpty() == true) item { Text("Bu öğrenci için kayıtlı öğretmen bulunamadı.") }
        if (historyError.isNotBlank()) item { Text(historyError) }
        if (partner == null) return@LazyColumn
        items(messages, key = { it.getString("id") }) { row ->
            val id = row.getString("id")
            OutlinedCard(Modifier.fillMaxWidth().testTag("parent_message_$id")) {
                Column(Modifier.padding(12.dp)) {
                    Text(row.optString("sender_name").ifBlank { if (row.optBoolean("is_current_user")) "Siz" else "" })
                    if (row.getString("content").isNotEmpty()) Text(row.getString("content"))
                    row.studentChatAttachment()?.let { attachment ->
                        Text(attachment.name)
                        TextButton(enabled = !busy, onClick = { download(partner, row) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("parent_message_attachment_$id")) {
                            Text("Eki indir")
                        }
                    }
                    Text(row.optString("sent_at"), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (actionError.isNotBlank()) item { Text(actionError) }
        if (fileStatus.isNotBlank()) item { Text(fileStatus) }
        item {
            OutlinedTextField(text, { if (it.length <= 10000) text = it }, enabled = !busy && pending == null,
                label = { Text("Mesaj") }, maxLines = 4, modifier = Modifier.fillMaxWidth().testTag("parent_message_text"))
        }
        file?.let { selected -> item { Text(selected.name) } }
        if (pending == null) {
            item {
                OutlinedButton(enabled = !busy, onClick = {
                    try {
                        pickingFor = partner
                        chooseDocument.launch(arrayOf("application/pdf", "image/png", "image/jpeg"))
                    } catch (_: ActivityNotFoundException) {
                        pickingFor = null
                        fileStatus = "Belge seçici açılamadı."
                    }
                }, modifier = Modifier.heightIn(min = 48.dp).testTag("parent_message_choose_file")) { Text("Dosya ekle") }
            }
            if (file != null) item {
                TextButton(enabled = !busy, onClick = { file = null; fileStatus = "" },
                    modifier = Modifier.heightIn(min = 48.dp)) { Text("Eki kaldır") }
            }
            item {
                Button(enabled = !busy && (text.isNotBlank() || file != null), onClick = { submit(false) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("parent_message_send")) { Text("Gönder") }
            }
        } else {
            item {
                Button(enabled = !busy, onClick = { submit(true) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("parent_message_check")) {
                    Text("Gönderimi kontrol et")
                }
            }
            if (canRetry) item {
                OutlinedButton(enabled = !busy, onClick = { submit(false) },
                    modifier = Modifier.heightIn(min = 48.dp)) { Text("Yeniden dene") }
            }
        }
        item {
            TextButton(enabled = !busy, onClick = { revision++ }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Geçmişi yenile")
            }
        }
    }
}

private fun JSONArray.rows() = (0 until length()).map { getJSONObject(it) }
