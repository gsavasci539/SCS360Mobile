package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.remote.NativeOutboxClient
import com.example.data.remote.outboxText
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

private val audiences = mapOf("ALL_PARENTS" to "Yetkili veliler", "BRANCH_PARENTS" to "Şube velileri",
    "CLASS_PARENTS" to "Sınıf velileri", "ALL_TEACHERS" to "Öğretmenler", "ALL_STAFF" to "Personel")
private val channels = mapOf("SMS" to "SMS", "EMAIL" to "E-posta", "WHATSAPP" to "WhatsApp")
private val broadcastStates = mapOf("DRAFT" to "Taslak", "QUEUED" to "Kuyrukta", "COMPLETED" to "Sağlayıcı işlemi tamamladı",
    "FAILED" to "Başarısız", "CANCELLED" to "İptal edildi")

@Composable
internal fun NativeBroadcastWorkspace(state: Okul360UiState, onBack: () -> Unit) {
    val client = remember { NativeOutboxClient(state.selectedBranchId) }
    val canWrite = NativeNavigationPolicy.has(state.currentUser, "communications.write")
    var templates by remember { mutableStateOf(false) }
    var providers by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(0) }
    var reportPage by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf<String?>(null) }
    val selected = chosen
    val lists by outboxLoad(templates, providers, page, revision) {
        when {
            providers -> client.providers()
            templates -> client.templates(page)
            else -> client.broadcasts(page)
        }
    }
    val detail by outboxLoad(selected, revision) { selected?.let { client.broadcast(it) } }
    val summary by outboxLoad(selected, revision) { selected?.let { client.report(it) } }
    val recipients by outboxLoad(selected, reportPage, revision) { if (selected == null) emptyList() else client.deliveries(selected, reportPage) }
    val record = detail?.getOrNull()
    val coroutine = rememberCoroutineScope()
    fun write(work: suspend () -> String) {
        if (busy) return
        busy = true
        notice = null
        coroutine.launch {
            try {
                notice = work()
                revision++
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                notice = "Toplu ileti işlemi doğrulanamadı. ${failure.message.orEmpty()}"
            } finally { busy = false }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(when { providers -> "SMS Sağlayıcıları"; templates -> "İleti Şablonları"; else -> "Toplu İletiler" },
                style = MaterialTheme.typography.headlineSmall)
            TextButton(enabled = !busy, onClick = { if (selected == null) onBack() else { chosen = null; notice = null } }) {
                Text(if (selected == null) "Geri" else "Toplu ileti listesine dön")
            }
            notice?.let { Text(it) }
            TextButton(enabled = !busy, onClick = { revision++ }) { Text("Kayıtları yenile") }
            if (selected == null) {
                TextButton(enabled = !busy && !providers, onClick = { templates = !templates; page = 0; notice = null }) {
                    Text(if (templates) "Toplu iletileri aç" else "Şablonları aç")
                }
                if (state.currentUser.is_super_admin == true) TextButton(enabled = !busy, onClick = {
                    providers = !providers; page = 0; notice = null
                }) { Text(if (providers) "Sağlayıcı listesinden dön" else "SMS sağlayıcılarını aç") }
                if (canWrite && !providers) Button(enabled = !busy, onClick = { form = if (templates) "template" else "draft" }) {
                    Text(if (templates) "Şablon oluştur" else "Taslak oluştur")
                }
            }
        }
        if (selected == null) {
            val rows = lists?.getOrNull().orEmpty()
            if (rows.isEmpty()) item { Text(outboxMessage(lists, "Bu kapsamda kayıt yok.")) }
            items(if (providers) rows else rows.take(20), key = { it.getString("id") }) { row -> BroadcastCard {
                val key = when { providers -> "provider_name"; templates -> "name"; else -> "title" }
                Text(row.getString(key), style = MaterialTheme.typography.titleMedium)
                if (providers) {
                    Text("Öncelik ${row.getInt("priority")}")
                    Text(if (row.getBoolean("is_active")) "Etkin yapılandırma" else "Pasif yapılandırma")
                } else if (templates) {
                    Text(channels[row.outboxText("channel")] ?: row.outboxText("channel"))
                    row.outboxText("subject").takeIf(String::isNotBlank)?.let { Text(it) }
                    Text(row.outboxText("content"))
                } else {
                    Text(broadcastStates[row.outboxText("status")] ?: row.outboxText("status"))
                    TextButton(onClick = { chosen = row.getString("id"); reportPage = 0; notice = null }) { Text("İleti kaydını aç") }
                }
            } }
            if (!providers && (page > 0 || rows.size > 20)) item {
                if (page > 0) TextButton(onClick = { page-- }) { Text("Önceki sayfa") }
                if (rows.size > 20) TextButton(onClick = { page++ }) { Text("Sonraki sayfa") }
            }
        } else if (record == null) item { Text(outboxMessage(detail, "Toplu ileti bulunamadı.")) }
        else {
            val status = record.getString("status")
            item { BroadcastCard {
                Text(record.getString("title"), style = MaterialTheme.typography.titleMedium)
                Text(record.getString("message_content"))
                Text(broadcastStates[status] ?: status)
                Text("Hedeflenen alıcı ${record.getInt("total_recipients")}")
                record.outboxText("scheduled_for").takeIf(String::isNotBlank)?.let { Text("Planlanan zaman $it") }
                if (canWrite && status == "DRAFT") {
                    TextButton(enabled = !busy, onClick = { form = "edit" }) { Text("Taslağı düzenle") }
                    Button(enabled = !busy && record.getInt("total_recipients") > 0, onClick = { form = "queue" }) { Text("Gönderim kuyruğuna al") }
                }
                if (canWrite && status in setOf("DRAFT", "QUEUED", "FAILED")) {
                    TextButton(enabled = !busy, onClick = { form = "cancel" }) { Text("İletiyi iptal et") }
                }
            } }
            if (status != "DRAFT") {
                item { BroadcastCard {
                    Text("Sağlayıcı ve kuyruk durumları", style = MaterialTheme.typography.titleMedium)
                    val counts = summary?.getOrNull()
                    if (counts == null) Text(outboxMessage(summary, "Gönderim özeti yok."))
                    else {
                        Text("Sağlayıcı kabul etti ${counts.getInt("sent")}")
                        Text("Bekleyen ${counts.getInt("pending")}")
                        Text("Başarısız ${counts.getInt("failed")}")
                    }
                } }
                val rows = recipients?.getOrNull().orEmpty()
                if (rows.isEmpty()) item { Text(outboxMessage(recipients, "Bu iletiye bağlı alıcı kaydı yok.")) }
                items(rows.take(20), key = { it.getString("id") }) { row -> BroadcastCard {
                    listOf(row.outboxText("recipient_name"), row.outboxText("recipient_phone"),
                        outboxStates[row.outboxText("status")] ?: row.outboxText("status"), row.outboxText("error_message"))
                        .filter(String::isNotBlank).forEach { Text(it) }
                } }
                if (reportPage > 0 || rows.size > 20) item {
                    if (reportPage > 0) TextButton(onClick = { reportPage-- }) { Text("Önceki alıcılar") }
                    if (rows.size > 20) TextButton(onClick = { reportPage++ }) { Text("Sonraki alıcılar") }
                }
            }
        }
    }
    val action = form
    if (canWrite && action in setOf("draft", "edit", "template")) BroadcastEditor(client, if (action == "edit") record else null,
        action == "template", busy, notice, { form = null }) { body, request ->
        write {
            if (action == "template") client.saveTemplate(body, request)
            else client.saveBroadcast(body, request, selected)
            form = null
            page = 0
            if (action == "template") "Şablon sunucudan doğrulandı." else "Toplu ileti taslağı sunucudan doğrulandı."
        }
    }
    if (canWrite && selected != null && record != null && action in setOf("queue", "cancel")) {
        val queue = action == "queue"
        val label = if (queue) "Gönderim kuyruğuna al" else "İletiyi iptal et"
        AlertDialog(onDismissRequest = { if (!busy) form = null }, title = { Text(label) },
            text = { Text(notice ?: record.getString("title")) },
            confirmButton = { Button(enabled = !busy, onClick = { write {
                val feedback = if (queue) {
                    val current = client.queueBroadcast(selected).getString("status")
                    "Kuyruk isteği doğrulandı. Güncel durum ${broadcastStates[current] ?: current}."
                } else {
                    client.cancelBroadcast(selected)
                    "Toplu iletinin iptal edildiği doğrulandı."
                }
                form = null
                feedback
            } }) { Text(label) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { form = null }) { Text("Vazgeç") } })
    }
}

@Composable
private fun BroadcastEditor(client: NativeOutboxClient, original: JSONObject?, template: Boolean, busy: Boolean,
    notice: String?, close: () -> Unit, save: (JSONObject, UUID) -> Unit) {
    var title by remember { mutableStateOf(original?.outboxText("title").orEmpty()) }
    var message by remember { mutableStateOf(original?.outboxText("message_content").orEmpty()) }
    var channel by remember { mutableStateOf(original?.outboxText("channel") ?: "SMS") }
    var audience by remember { mutableStateOf(original?.outboxText("target_audience") ?: "ALL_PARENTS") }
    var branch by remember { mutableStateOf(client.selectedBranch.ifBlank { original?.outboxText("target_branch_id").orEmpty() }) }
    var classroom by remember { mutableStateOf(original?.outboxText("target_class_id").orEmpty()) }
    var role by remember { mutableStateOf(original?.outboxText("target_role").orEmpty()) }
    var subject by remember { mutableStateOf("") }
    var scheduled by remember {
        mutableStateOf(original?.outboxText("scheduled_for")?.takeIf(String::isNotBlank)?.let {
            OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.of("Europe/Istanbul")).toLocalDateTime().toString()
        }.orEmpty())
    }
    var revision by remember { mutableIntStateOf(0) }
    val options by outboxLoad(branch, revision) { if (template) null else client.broadcastOptions(branch) }
    val metadata = options?.getOrNull()
    fun rows(key: String): List<JSONObject> {
        val array = metadata?.optJSONArray(key) ?: JSONArray()
        return (0 until array.length()).map(array::getJSONObject)
    }
    val classes = rows("classes")
    val branchRequired = audience in setOf("BRANCH_PARENTS", "CLASS_PARENTS")
    val classRequired = audience == "CLASS_PARENTS"
    val classOk = !classRequired || classes.any { it.getString("id") == classroom && it.getString("branch_id") == branch }
    val scheduledTime = runCatching {
        LocalDateTime.parse(scheduled.trim()).atZone(ZoneId.of("Europe/Istanbul")).toOffsetDateTime().toString()
    }.getOrNull()
    val scheduleOk = scheduled.isBlank() || scheduledTime != null
    val body = if (template) JSONObject().put("name", title.trim()).put("content", message.trim())
        .put("subject", subject.trim().ifBlank { null } ?: JSONObject.NULL).put("channel", channel).put("is_active", true)
    else JSONObject().put("title", title.trim()).put("message_content", message.trim()).put("channel", channel)
        .put("target_audience", audience).put("target_branch_id", branch.ifBlank { null } ?: JSONObject.NULL)
        .put("target_class_id", classroom.takeIf { classRequired && it.isNotBlank() } ?: JSONObject.NULL)
        .put("target_role", role.takeIf { audience == "ALL_STAFF" && it.isNotBlank() } ?: JSONObject.NULL)
        .put("scheduled_for", scheduledTime ?: JSONObject.NULL)
    val request = remember(body.toString()) { UUID.randomUUID() }
    ConsentFormDialog(if (template) "İleti şablonu" else "Toplu ileti taslağı", "native_broadcast_editor", busy, close, fields = {
        item { BroadcastField(title, if (template) "Şablon adı" else "Başlık", 255, busy) { title = it } }
        if (template) item { BroadcastField(subject, "Konu", 255, busy) { subject = it } }
        item { BroadcastField(message, "İleti metni", 10000, busy) { message = it } }
        item { Text("Kanal") }
        items(channels.entries.toList(), key = { it.key }) { (code, label) ->
            BroadcastOption(label, channel == code, busy) { channel = code }
        }
        if (!template) {
            item { Text("Hedef kitle") }
            items(audiences.entries.toList(), key = { it.key }) { (code, label) ->
                BroadcastOption(label, audience == code, busy) {
                    audience = code
                    if (code != "CLASS_PARENTS") classroom = ""
                    if (code != "ALL_STAFF") role = ""
                }
            }
            item {
                if (options == null) Text("Hedefler yükleniyor…")
                if (options?.isFailure == true) TextButton(onClick = { revision++ }) { Text("Hedefleri tekrar yükle") }
                if (client.selectedBranch.isNotBlank()) Text("Seçili şube")
                else {
                    Text("Hedef şube")
                    if (!branchRequired) BroadcastOption("Kurum kapsamı", branch.isBlank(), busy) { branch = ""; classroom = "" }
                }
            }
            val branches = rows("branches")
            items(branches, key = { it.getString("id") }) { row ->
                val id = row.getString("id")
                if (client.selectedBranch.isBlank()) BroadcastOption(row.getString("name"), branch == id, busy) { branch = id; classroom = "" }
                else if (id == branch) Text(row.getString("name"))
            }
            if (classRequired && branch.isNotBlank()) items(classes, key = { it.getString("id") }) { row ->
                val id = row.getString("id")
                BroadcastOption(row.getString("name"), classroom == id, busy) { classroom = id }
            }
            if (audience == "ALL_STAFF") {
                val roles = listOf("" to "Tüm personel", "OWNER" to "Kurum sahipleri", "ADMIN" to "Yöneticiler",
                    "TEACHER" to "Öğretmenler", "STAFF" to "Personel rolü")
                item { Text("Personel rolü") }
                items(roles, key = { it.first }) { (code, label) ->
                    BroadcastOption(label, role == code, busy) { role = code }
                }
            }
            item { BroadcastField(scheduled, "Planlanan İstanbul saati (YYYY-AA-GGTSS:DD)", 19, busy) { scheduled = it } }
        }
        notice?.let { item { Text(it) } }
    }, confirm = {
        val targetsOk = template || metadata != null && (!branchRequired || branch.isNotBlank()) && classOk && scheduleOk
        Button(enabled = !busy && targetsOk && title.isNotBlank() && message.isNotBlank(), onClick = { save(body, request) }) { Text("Kaydet") }
    })
}

@Composable
private fun BroadcastField(value: String, label: String, max: Int, busy: Boolean, update: (String) -> Unit) {
    OutlinedTextField(value, { update(it.take(max)) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text(label) })
}

@Composable
private fun BroadcastOption(label: String, selected: Boolean, busy: Boolean, pick: () -> Unit) {
    TextButton(onClick = pick, enabled = !busy) { Text(if (selected) "✓ $label" else label) }
}

@Composable
private fun BroadcastCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}
