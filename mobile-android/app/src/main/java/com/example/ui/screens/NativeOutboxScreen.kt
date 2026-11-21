package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.remote.NativeOutboxClient
import com.example.data.remote.RecordedOutboxMessage
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val outboxStates = mapOf(
    "PENDING" to "Kuyrukta", "SENT" to "Sağlayıcı kabul etti", "FAILED" to "Başarısız", "CANCELLED" to "İptal edildi",
)
private val outboxChannels = mapOf("SMS" to "SMS", "EMAIL" to "E-posta", "WHATSAPP" to "WhatsApp")

@Composable
internal fun NativeOutboxScreen(state: Okul360UiState, onBack: () -> Unit) {
    val actor = state.currentUser
    val canRead = NativeNavigationPolicy.has(actor, "outbox.read", "notification.read")
    val canBroadcast = NativeNavigationPolicy.has(actor, "communications.read")
    if (!state.isLoggedIn || !canRead && !canBroadcast || actor.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) {
        Column(Modifier.padding(16.dp)) {
            Text("Gönderim kayıtları için yetkili çalışma alanını açın.")
            TextButton(onClick = onBack) { Text("Geri") }
        }
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl) {
        var broadcast by remember { mutableStateOf(!canRead) }
        Column(Modifier.fillMaxSize()) {
            if (canRead && canBroadcast) TextButton(onClick = { broadcast = !broadcast }) {
                Text(if (broadcast) "Gönderim kayıtlarını aç" else "Toplu ileti ve şablonları aç")
            }
            if (broadcast) NativeBroadcastWorkspace(state, onBack)
            else OutboxMessages(remember { NativeOutboxClient(state.selectedBranchId) },
                NativeNavigationPolicy.has(actor, "notification.write"), onBack)
        }
    }
}

@Composable
private fun OutboxMessages(client: NativeOutboxClient, canWrite: Boolean, onBack: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(1) }
    var revision by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf<String?>(null) }
    val coroutine = rememberCoroutineScope()
    val chosen = selected
    val records by outboxLoad(page, search, channel, status, revision) {
        if (search.isNotBlank()) delay(250)
        client.messages(page, search.trim(), channel, status)
    }
    val detail by outboxLoad(chosen, revision) { chosen?.let { client.message(it) } }
    val record = detail?.getOrNull()
    fun write(action: suspend () -> String) {
        if (busy) return
        notice = null
        busy = true
        coroutine.launch {
            try {
                notice = action()
                revision++
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                notice = "Gönderim işlemi doğrulanamadı. ${failure.message.orEmpty()}"
            } finally { busy = false }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Giden Kutusu", style = MaterialTheme.typography.headlineSmall)
            TextButton(enabled = !busy, onClick = {
                if (chosen == null) onBack() else { selected = null; notice = null }
            }) { Text(if (chosen == null) "Geri" else "Gönderim listesine dön") }
            notice?.let { Text(it) }
            TextButton(enabled = !busy, onClick = { revision++ }) { Text("Kayıtları yenile") }
        }
        if (chosen == null) {
            item {
                OutlinedTextField(search, { search = it.take(100); page = 1 }, Modifier.fillMaxWidth(),
                    label = { Text("Alıcı veya konu ara") }, singleLine = true)
                TextButton(onClick = { filter = "channel" }) { Text(outboxChannels[channel] ?: "Tüm kanallar") }
                TextButton(onClick = { filter = "status" }) { Text(outboxStates[status] ?: "Tüm durumlar") }
            }
            val listing = records?.getOrNull()
            val rows = listing?.first.orEmpty()
            if (rows.isEmpty()) item { Text(outboxMessage(records, "Bu kapsamda gönderim kaydı yok.")) }
            items(rows, key = { it.id }) { row -> OutboxCard(row, false) {
                TextButton(onClick = { selected = row.id; notice = null }) { Text("Gönderim kaydını aç") }
            } }
            if (page > 1 || (listing?.second ?: 0) > page) item {
                if (page > 1) TextButton(onClick = { page-- }) { Text("Önceki sayfa") }
                if ((listing?.second ?: 0) > page) TextButton(onClick = { page++ }) { Text("Sonraki sayfa") }
            }
        } else item {
            if (record == null) Text(outboxMessage(detail, "Gönderim kaydı bulunamadı."))
            else OutboxCard(record, true) {
                if (canWrite && record.status in setOf("FAILED", "CANCELLED")) {
                    Button(enabled = !busy, onClick = { confirming = "retry" }) { Text("Yeniden kuyruğa al") }
                }
                if (canWrite) TextButton(enabled = !busy, onClick = { confirming = "delete" }) { Text("Kaydı kaldır") }
            }
        }
    }
    filter?.let { type ->
        val options = if (type == "channel") outboxChannels else outboxStates
        ConsentFormDialog("Filtre seçin", "native_outbox_filter", false, { filter = null }, fields = {
            items(listOf("" to "Tümü") + options.toList(), key = { it.first }) { (value, label) ->
                TextButton(onClick = {
                    if (type == "channel") channel = value else status = value
                    page = 1
                    filter = null
                }) { Text(label) }
            }
        }, confirm = {})
    }
    val pending = confirming
    if (canWrite && record != null && pending != null) {
        val label = if (pending == "retry") "Yeniden kuyruğa al" else "Kaydı kaldır"
        AlertDialog(onDismissRequest = { if (!busy) confirming = null }, title = { Text(label) },
            text = { Text(notice ?: record.subject) },
            confirmButton = { Button(enabled = !busy, onClick = { write {
                val feedback = if (pending == "retry") {
                    val saved = client.retry(record)
                    "Kuyruk isteği doğrulandı. Güncel durum ${outboxStates[saved.status] ?: saved.status}."
                } else {
                    client.delete(record.id)
                    selected = null
                    "Gönderim kaydının kaldırıldığı doğrulandı."
                }
                confirming = null
                feedback
            } }) { Text(label) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { confirming = null }) { Text("Vazgeç") } })
    }
}

@Composable
private fun OutboxCard(row: RecordedOutboxMessage, detail: Boolean, actions: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(row.subject.ifBlank { "Başlıksız gönderim" }, style = MaterialTheme.typography.titleMedium)
            listOf(row.recipientName, row.recipient, outboxChannels[row.channel] ?: row.channel,
                outboxStates[row.status] ?: row.status, row.sentAt.takeUnless { it.isBlank() }.orEmpty()).filter(String::isNotBlank).forEach { Text(it) }
            if (detail) listOf(row.body, row.error, "Planlanan zaman ${row.scheduledAt}", "Deneme sayısı ${row.retries}")
                .filter(String::isNotBlank).forEach { Text(it) }
            actions()
        }
    }
}

internal fun outboxMessage(result: Result<*>?, empty: String) = when {
    result == null -> "Gönderim kayıtları yükleniyor…"
    result.isFailure -> "Gönderim kayıtları yüklenemedi."
    else -> empty
}

@Composable
internal fun <T> outboxLoad(vararg keys: Any?, load: suspend () -> T): State<Result<T>?> = key(keys.toList()) {
    produceState<Result<T>?>(null) {
        value = try { Result.success(load()) } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            Result.failure(failure)
        }
    }
}
