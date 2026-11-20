package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.AnnouncementTargets
import com.example.data.remote.NativeAnnouncement
import com.example.data.remote.NativeAnnouncementClient
import com.example.data.remote.NativeAnnouncementDraft
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ANNOUNCEMENT_PAGE_SIZE = 20
private val announcementStatusLabels = mapOf("DRAFT" to "Taslak", "SENT" to "Yayımlanmış", "SCHEDULED" to "Planlandı", "CANCELLED" to "İptal edildi")
private val channelLabels = mapOf("EMAIL" to "E-posta", "SMS" to "SMS", "WHATSAPP" to "WhatsApp")
private enum class AnnouncementAudience(val label: String, val kind: String? = null) {
    ALL_PARENTS("Tüm veliler"), BRANCH_PARENTS("Şube velileri", "branches"),
    CLASS_PARENTS("Sınıf velileri", "classes"), SINGLE_STUDENT("Tek öğrenci velisi", "students"),
}

@Composable
internal fun NativeAnnouncementManagementPane(state: Okul360UiState, modifier: Modifier = Modifier) {
    val user = state.currentUser
    val canWrite = NativeNavigationPolicy.has(user, "announcement.write")
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, "announcement.read") ||
        user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) {
        Text("Duyuru kayıtlarını açmak için yetkili kurum çalışma alanını seçin.", modifier.padding(16.dp))
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl) {
        val client = remember { NativeAnnouncementClient() }
        var status by remember { mutableStateOf<String?>(null) }
        var page by remember { mutableIntStateOf(0) }
        var attempt by remember { mutableIntStateOf(0) }
        var detail by remember { mutableStateOf<NativeAnnouncement?>(null) }
        var editing by remember { mutableStateOf<NativeAnnouncement?>(null) }
        var creating by remember { mutableStateOf(false) }
        var notice by remember { mutableStateOf<String?>(null) }
        fun reload(message: String? = null) {
            notice = message
            detail = null
            editing = null
            creating = false
            page = 0
            attempt++
        }
        val result by key(status, page, attempt) {
            produceState<Result<List<NativeAnnouncement>>?>(null) {
                value = announcementResult { client.list(state.selectedBranchId, status, ANNOUNCEMENT_PAGE_SIZE + 1, page * ANNOUNCEMENT_PAGE_SIZE) }
            }
        }
        LazyColumn(modifier.fillMaxSize().testTag("native_announcement_records"), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Duyuru kayıtları", style = MaterialTheme.typography.titleLarge)
                listOf(null to "Tümü", "DRAFT" to "Taslaklar", "SENT" to "Yayımlananlar").forEach { (value, label) ->
                    FilterChip(status == value, onClick = { status = value; page = 0; notice = null }, label = { Text(label) })
                }
                if (canWrite) Button(onClick = { creating = true; notice = null }) { Text("Duyuru taslağı oluştur") }
                notice?.let { Text(it) }
            }
            val records = result?.getOrNull()
            when {
                result == null -> item { Text("Duyurular yükleniyor…") }
                records == null -> item {
                    Text(result?.exceptionOrNull()?.message ?: "Duyuru kayıtları yüklenemedi.")
                    TextButton(onClick = { attempt++ }) { Text("Duyuruları yeniden yükle") }
                }
                records.isEmpty() -> item { Text("Bu kapsamda duyuru kaydı bulunamadı.") }
                else -> items(records.take(ANNOUNCEMENT_PAGE_SIZE), key = { it.id }) { row ->
                    OutlinedCard(onClick = { detail = row }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(row.title, style = MaterialTheme.typography.titleMedium)
                            Text(announcementStatusLabels[row.status] ?: row.status)
                            Text(row.createdAt.take(10))
                        }
                    }
                }
            }
            if (records != null && (page > 0 || records.size > ANNOUNCEMENT_PAGE_SIZE)) item {
                Text("Sayfa ${page + 1}")
                OutlinedButton(enabled = page > 0, onClick = { page-- }) { Text("Önceki") }
                OutlinedButton(enabled = records.size > ANNOUNCEMENT_PAGE_SIZE, onClick = { page++ }) { Text("Sonraki") }
            }
        }
        detail?.let { row ->
            AnnouncementRecordDialog(client, row, canWrite, { detail = null }, { selected ->
                detail = null
                editing = selected
            }, { reload(it) })
        }
        if (creating || editing != null) {
            NativeAnnouncementDraftDialog(state, editing, { creating = false; editing = null }) { reload("Duyuru taslağı kaydedildi.") }
        }
    }
}

@Composable
private fun AnnouncementRecordDialog(client: NativeAnnouncementClient, row: NativeAnnouncement, canWrite: Boolean,
    onClose: () -> Unit, onEdit: (NativeAnnouncement) -> Unit, onChange: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var action by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val detail by produceState<Result<NativeAnnouncement>?>(null, row.id) {
        value = announcementResult {
            client.detail(row.id).also { check(it.branch == row.branch) { "Duyuru ayrıntısı seçili şubeyle eşleşmiyor." } }
        }
    }
    val record = detail?.getOrNull()
    ConsentFormDialog(row.title, "native_announcement_detail", busy, onClose, {
        if (record == null) {
            item { Text(detail?.exceptionOrNull()?.message ?: "Duyuru ayrıntısı yükleniyor…") }
        } else {
            item { Text(record.content) }
            item { Text(announcementStatusLabels[record.status] ?: record.status) }
            if (record.channels.isNotEmpty()) item { Text(record.channels.joinToString(", ") { channelLabels[it] ?: it }) }
            if (canWrite && record.status == "DRAFT" && action == null) {
                item { TextButton(onClick = { onEdit(record) }) { Text("Taslağı düzenle") } }
                if (record.channels.isNotEmpty()) item { TextButton(onClick = { action = "queue" }) { Text("Kuyruğa al") } }
                item { TextButton(onClick = { action = "delete" }) { Text("Taslağı sil") } }
            }
            if (action != null) item {
                Text(if (action == "queue") "Bu taslak seçili kanallar için bildirim kuyruğuna alınacak." else "Bu taslak silinecek.")
                TextButton(enabled = !busy, onClick = { action = null; error = null }) { Text("Vazgeç") }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }) {
        val currentAction = action
        if (canWrite && record?.status == "DRAFT" && currentAction != null) Button(enabled = !busy, onClick = {
            busy = true
            error = null
            scope.launch {
                val result = announcementResult {
                    if (currentAction == "queue") {
                        val queue = client.enqueue(record.id)
                        if (queue.alreadyQueued) "Mevcut kuyruk kaydı doğrulandı." else "${queue.jobs} bildirim işi kuyruğa alındı."
                    } else {
                        client.delete(record.id)
                        "Duyuru taslağı silindi."
                    }
                }
                busy = false
                result.fold(onChange) { failure -> error = failure.message ?: "Duyuru işlemi doğrulanamadı." }
            }
        }) { Text(if (currentAction == "queue") "Kuyruğu onayla" else "Silmeyi onayla") }
    }
}

@Composable
internal fun NativeAnnouncementDraftDialog(state: Okul360UiState, existing: NativeAnnouncement? = null,
    onClose: () -> Unit, onSaved: () -> Unit) {
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(state.currentUser, "announcement.read") ||
        !NativeNavigationPolicy.has(state.currentUser, "announcement.write") ||
        state.currentUser.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) return
    key(financePresentationScope(state), state.serverBaseUrl, existing?.id) {
        val client = remember { NativeAnnouncementClient() }
        val defaultBranch = state.selectedBranchId.takeUnless { it == "all" || it.isBlank() }
        val initialAudience = AnnouncementAudience.entries.find { it.name == existing?.audience }
            ?: if (defaultBranch == null) AnnouncementAudience.ALL_PARENTS else AnnouncementAudience.BRANCH_PARENTS
        var audience by remember { mutableStateOf(initialAudience) }
        var selected by remember {
            mutableStateOf(when (initialAudience) {
                AnnouncementAudience.BRANCH_PARENTS -> existing?.branch ?: defaultBranch
                AnnouncementAudience.CLASS_PARENTS -> existing?.classroom
                AnnouncementAudience.SINGLE_STUDENT -> existing?.student
                AnnouncementAudience.ALL_PARENTS -> null
            })
        }
        var title by remember { mutableStateOf(existing?.title.orEmpty()) }
        var content by remember { mutableStateOf(existing?.content.orEmpty()) }
        var channels by remember { mutableStateOf(existing?.channels.orEmpty().toSet()) }
        var search by remember { mutableStateOf("") }
        var retry by remember { mutableIntStateOf(0) }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val kind = audience.kind
        val options by key(kind, search, selected, retry) {
            produceState<Result<AnnouncementTargets>?>(null) {
                if (kind != null) {
                    if (search.isNotBlank()) delay(250)
                    value = announcementResult { client.targets(kind, search.trim(), selected) }
                }
            }
        }
        val snapshot = options?.getOrNull()
        ConsentFormDialog("Duyuru taslağı", "native_announcement_draft", busy, onClose, {
            item { OutlinedTextField(title, { title = it.take(255) }, label = { Text("Duyuru başlığı") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(content, { content = it.take(10000) }, label = { Text("Duyuru içeriği") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) }
            item { Text("Hedef kitle") }
            items(AnnouncementAudience.entries, key = { it.name }) { entry ->
                FilterChip(audience == entry, onClick = {
                    audience = entry
                    search = ""
                    selected = if (entry == AnnouncementAudience.BRANCH_PARENTS) defaultBranch else null
                }, enabled = !busy, label = { Text(entry.label) })
            }
            if (kind != null) {
                item { OutlinedTextField(search, { search = it.take(100) }, label = { Text("Hedef ara") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) }
                when {
                    options == null -> item { Text("Hedefler yükleniyor…") }
                    snapshot == null -> item {
                        Text(options?.exceptionOrNull()?.message ?: "Hedefler yüklenemedi.")
                        TextButton(onClick = { retry++ }) { Text("Hedefleri yeniden yükle") }
                    }
                    snapshot.choices.isEmpty() -> item { Text("Bu kapsamda hedef bulunamadı.") }
                    else -> items(snapshot.choices, key = { it.id }) { entry ->
                        FilterChip(selected == entry.id, onClick = { selected = entry.id }, enabled = !busy, label = { Text(entry.name) })
                    }
                }
                if (snapshot?.more == true) item { Text("Diğer hedefleri adla arayın.") }
            }
            item { Text("Bildirim kanalları") }
            items(channelLabels.entries.toList(), key = { it.key }) { channel ->
                FilterChip(channel.key in channels, onClick = {
                    channels = if (channel.key in channels) channels - channel.key else channels + channel.key
                }, enabled = !busy, label = { Text(channel.value) })
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }) {
            val targetReady = kind == null || snapshot?.choices?.any { it.id == selected } == true
            Button(enabled = !busy && title.trim().length >= 2 && content.isNotBlank() && targetReady, onClick = {
                busy = true
                error = null
                val draft = NativeAnnouncementDraft(title.trim(), content.trim(), audience.name, channels.sorted(),
                    selected.takeIf { audience == AnnouncementAudience.BRANCH_PARENTS },
                    selected.takeIf { audience == AnnouncementAudience.CLASS_PARENTS },
                    selected.takeIf { audience == AnnouncementAudience.SINGLE_STUDENT })
                scope.launch {
                    val result = announcementResult { client.save(draft, existing?.id) }
                    busy = false
                    result.fold({ onSaved() }) { failure -> error = failure.message ?: "Duyuru taslağı kaydedilemedi." }
                }
            }) { Text("Taslağı kaydet") }
        }
    }
}

private suspend fun <T> announcementResult(action: suspend () -> T): Result<T> = try {
    Result.success(action())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Result.failure(failure)
}
