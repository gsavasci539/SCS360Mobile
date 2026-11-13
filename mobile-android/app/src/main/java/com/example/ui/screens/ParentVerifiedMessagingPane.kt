package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.User
import com.example.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ParentVerifiedMessagingPane(studentId: String, actor: User) {
    val captured = remember(studentId) { ApiClientProvider.interceptor.captureScope() }
    val client = remember(studentId) { NativeParentPortalClient() }
    val coroutine = rememberCoroutineScope()
    var teachers by remember(studentId) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var teacher by remember(studentId) { mutableStateOf<JSONObject?>(null) }
    var messages by remember(studentId) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var text by remember(studentId) { mutableStateOf("") }
    var busy by remember(studentId) { mutableStateOf(false) }
    var uncertain by remember(studentId) { mutableStateOf(false) }
    var error by remember(studentId) { mutableStateOf("") }
    var revision by remember(studentId) { mutableIntStateOf(0) }
    val teacherId = teacher?.getString("id")
    val allowed = actor.is_super_admin == true || "parent.send_message" in actor.permissions
    LaunchedEffect(studentId) {
        try { teachers = captured.io { (client.section(studentId, "teachers") as JSONArray).rows() } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (captured.current()) error = "Öğretmenler yüklenemedi." }
    }
    LaunchedEffect(studentId, teacherId, revision) {
        messages = emptyList(); error = ""
        if (teacherId != null && allowed) {
            busy = true
            try { messages = captured.io { client.chat(studentId, teacherId).rows() }; uncertain = false }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (captured.current()) error = "Mesaj geçmişi yüklenemedi." }
            finally { if (captured.current()) busy = false }
        }
    }
    if (!captured.current()) return
    val shownTeachers = teachers
    val shownMessages = messages
    LazyColumn(Modifier.fillMaxSize().imePadding().testTag("parent_messages"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Öğretmenle iletişim", style = MaterialTheme.typography.titleLarge) }
        if (!allowed) item { Text("Bu hesapla mesajlaşma yetkisi yok.") }
        items(shownTeachers, key = { it.getString("id") }) { row -> OutlinedButton(enabled = !busy && allowed, onClick = { teacher = row; text = ""; uncertain = false }, modifier = Modifier.heightIn(min = 48.dp).testTag("parent_teacher_${row.getString("id")}")) { Text(row.getString("name")) } }
        if (shownTeachers.isEmpty()) item { Text("Bu öğrenci için kayıtlı öğretmen bulunamadı.") }
        item { if (error.isNotBlank()) Text(error) }
        if (teacherId != null) {
            items(shownMessages, key = { it.getString("id") }) { row -> OutlinedCard(Modifier.fillMaxWidth().testTag("parent_message_${row.getString("id")}")) { Column(Modifier.padding(12.dp)) { Text(row.optString("sender_name")); Text(row.getString("content")); Text(row.optString("sent_at")) } } }
            item { OutlinedTextField(text, { text = it }, enabled = !busy && !uncertain && allowed, label = { Text("Mesaj") }, modifier = Modifier.fillMaxWidth().testTag("parent_message_text")) }
            item { Button(enabled = allowed && !busy && !uncertain && text.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp).testTag("parent_message_send"), onClick = {
                if (!captured.current()) return@Button
                val content = text; busy = true; error = ""
                coroutine.launch {
                    try { client.sendChat(studentId, teacherId, content, actor.id); captured.ensureCurrent(); text = ""; revision++ }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { if (captured.current()) { uncertain = true; error = "Mesaj sonucu doğrulanamadı. Yeniden göndermeden önce geçmişi yenileyin." } }
                    finally { if (captured.current()) busy = false }
                }
            }) { Text("Gönder") } }
            item { TextButton(enabled = !busy, onClick = { revision++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Geçmişi yenile") } }
        }
    }
}
private fun JSONArray.rows() = (0 until length()).map { getJSONObject(it) }
