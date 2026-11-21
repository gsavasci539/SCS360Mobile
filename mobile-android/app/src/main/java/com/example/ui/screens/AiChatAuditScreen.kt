package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.ChatAuditEntry
import com.example.data.remote.ChatAuditExport
import com.example.data.remote.ChatAuditOverview
import com.example.data.remote.ChatAuditPage
import com.example.data.remote.ChatAuditStatus
import com.example.data.remote.NativeChatAuditClient
import com.example.data.remote.RegistryHttpException
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val AuditPageSize = 20
private val AuditTabs = listOf("Diyalog denetimi", "Analitik", "Veri dışa aktarımı")

@Composable
fun AiChatAuditScreen(state: Okul360UiState, onNavigateBack: () -> Unit) {
    val organization = state.workspaceOrganizationId
    val authorized = state.isLoggedIn && state.currentUser.is_super_admin == true
    if (!authorized || organization.isNullOrBlank()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onNavigateBack) { Text("Geri") }
            Text(if (authorized) "Sohbet denetimi için bir kurum çalışma alanı seçin."
                else "Sohbet denetimi platform yöneticisine açıktır.")
        }
        return
    }
    key(state.currentUser.id, state.serverBaseUrl, organization) {
        ChatAuditWorkspace(organization, onNavigateBack)
    }
}

@Composable
private fun ChatAuditWorkspace(organization: String, onNavigateBack: () -> Unit) {
    val client = remember(organization) { NativeChatAuditClient(organization) }
    val actions = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var topic by remember { mutableStateOf("") }
    var topicDraft by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<ChatAuditStatus?>(null) }
    var results by remember { mutableStateOf<ChatAuditPage?>(null) }
    var analytics by remember { mutableStateOf<ChatAuditOverview?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var opened by remember { mutableStateOf<ChatAuditEntry?>(null) }
    var format by remember { mutableStateOf(ChatAuditExport.JSONL) }
    var exportFilter by remember { mutableStateOf<ChatAuditStatus?>(null) }
    var exportChoice by remember { mutableStateOf<Pair<ChatAuditExport, ChatAuditStatus?>?>(null) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val choice = exportChoice
        if (uri == null || choice == null) {
            exportChoice = null
        } else {
            actions.launch {
                try {
                    client.export(choice.first, choice.second) {
                        checkNotNull(context.contentResolver.openOutputStream(uri, "w")) { "Dosya açılamadı." }
                    }
                    exportMessage = "Dosya kaydedildi."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    exportMessage = if ((failure as? RegistryHttpException)?.code == 404) "Dışa aktarılacak kayıt bulunamadı."
                        else "Dosya kaydedilemedi. Yeniden deneyin."
                } finally {
                    exportChoice = null
                }
            }
        }
    }
    LaunchedEffect(tab, page, topic, status, revision) {
        error = null
        loading = tab != 2
        try {
            when (tab) {
                0 -> results = client.page(page, AuditPageSize, topic, status)
                1 -> analytics = client.overview()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = if (tab == 1) "Analitik verileri alınamadı." else "Denetim kayıtları alınamadı."
        } finally {
            loading = false
        }
    }

    val activeTab = tab
    val issue = error
    val pending = loading
    val current = results
    val overview = analytics
    val logs = current?.entries.orEmpty().filter {
        query.isBlank() || listOf(it.sender, it.question, it.response, it.topic).any { field -> field.contains(query.trim(), ignoreCase = true) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onNavigateBack) { Text("Geri") }
                Text("Sohbet denetimi", style = MaterialTheme.typography.headlineSmall)
                PreferenceChoice("Görünüm", AuditTabs[activeTab], AuditTabs, true) { tab = AuditTabs.indexOf(it) }
            }
        }
        if (activeTab == 0) item(key = "filters") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("Sayfada ara") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(topicDraft, { topicDraft = it }, label = { Text("Konu") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                val choices = listOf("Tüm durumlar") + ChatAuditStatus.entries.map { it.label }
                PreferenceChoice("Geri bildirim durumu", status?.label ?: choices.first(), choices, true) { label ->
                    status = ChatAuditStatus.entries.firstOrNull { it.label == label }
                    page = 1
                }
                OutlinedButton(onClick = { topic = topicDraft.trim(); page = 1; revision++ }) { Text("Filtreyi uygula") }
            }
        }
        when {
            pending -> item(key = "loading") { Text("Veriler yükleniyor…") }
            issue != null -> item(key = "error") {
                Text(issue, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { revision++ }) { Text("Tekrar dene") }
            }
            activeTab == 0 -> {
                if (logs.isEmpty()) item(key = "empty") { Text("Denetim kaydı bulunamadı.") }
                items(logs, key = { "log-" + it.id }) { entry ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(entry.question, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                            Text(listOf(entry.sender, entry.role, entry.topic).filter(String::isNotBlank).joinToString(" · "))
                            Text(entry.status.label)
                            TextButton(onClick = { opened = entry }) { Text("Kaydı incele") }
                        }
                    }
                }
                item(key = "page") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sayfa $page · Toplam ${current?.total ?: 0} kayıt")
                        OutlinedButton(enabled = page > 1, onClick = { page-- }) { Text("Önceki") }
                        OutlinedButton(enabled = page * AuditPageSize < (current?.total ?: 0), onClick = { page++ }) { Text("Sonraki") }
                    }
                }
            }
            activeTab == 1 && overview != null -> item(key = "metrics") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Toplam kayıt ${overview.total}")
                    Text(overview.averageLatency?.let { "Ortalama yanıt süresi $it ms" } ?: "Yanıt süresi verisi yok.")
                    Text(overview.averageQuality?.let { "Kalite $it/5 · ${overview.rated} puanlı kayıt" } ?: "Puanlanmadı")
                    Text("Dışa aktarıma hazır ${overview.trainingReady}")
                    listOf("topic" to "Konular", "intent" to "Niyetler", "sentiment" to "Duygu durumları", "feedback_status" to "Geri bildirim")
                        .forEach { (section, title) ->
                            Text(title, style = MaterialTheme.typography.titleSmall)
                            overview.distributions[section].orEmpty().entries.sortedByDescending { it.value }.forEach { (label, count) ->
                                Text("$label · $count")
                            }
                        }
                    OutlinedButton(onClick = { revision++ }) { Text("Yenile") }
                }
            }
            activeTab == 2 -> item(key = "export") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val choices = listOf("Tüm durumlar") + ChatAuditStatus.entries.map { it.label }
                    val availableFormats = ChatAuditExport.entries
                    PreferenceChoice("Dosya biçimi", format.label, availableFormats.map { it.label }, exportChoice == null) { label ->
                        format = availableFormats.first { it.label == label }
                    }
                    PreferenceChoice("Geri bildirim durumu", exportFilter?.label ?: choices.first(), choices, exportChoice == null) { label ->
                        exportFilter = ChatAuditStatus.entries.firstOrNull { it.label == label }
                    }
                    exportMessage?.let { Text(it) }
                    Button(enabled = exportChoice == null, onClick = {
                        exportMessage = null
                        exportChoice = format to exportFilter
                        picker.launch("scs360_denetim.${format.extension}")
                    }) { Text(if (exportChoice == null) "Dosyayı kaydet" else "Dışa aktarılıyor…") }
                }
            }
        }
    }
    opened?.let { entry ->
        ChatAuditFeedback(entry, client, onDismiss = { opened = null }, onSaved = { opened = null; revision++ })
    }
}

@Composable
private fun ChatAuditFeedback(entry: ChatAuditEntry, client: NativeChatAuditClient, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var rating by remember(entry.id) { mutableIntStateOf(entry.rating) }
    var status by remember(entry.id) { mutableStateOf(entry.status) }
    var correction by remember(entry.id) { mutableStateOf(entry.correction ?: entry.response) }
    var error by remember(entry.id) { mutableStateOf<String?>(null) }
    var saving by remember(entry.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = { if (!saving) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 560.dp).fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Diyalog kaydı", style = MaterialTheme.typography.titleLarge)
                Text(listOf(entry.sender, entry.role, entry.createdAt).filter(String::isNotBlank).joinToString(" · "))
                Text("Soru", style = MaterialTheme.typography.titleSmall)
                Text(entry.question)
                Text("Kayıtlı yanıt", style = MaterialTheme.typography.titleSmall)
                Text(entry.response)
                Text(listOf(entry.topic, entry.intent, entry.sentiment).filter(String::isNotBlank).joinToString(" · "))
                PreferenceChoice("Puan", if (rating == 0) "Puanlanmadı" else rating.toString(), (1..5).map(Int::toString), !saving) {
                    rating = it.toInt()
                }
                PreferenceChoice("Durum", status.label, ChatAuditStatus.entries.map { it.label }, !saving) { label ->
                    status = ChatAuditStatus.entries.first { it.label == label }
                }
                if (status == ChatAuditStatus.CORRECTED) OutlinedTextField(correction, { correction = it },
                    label = { Text("Düzeltilmiş yanıt") }, modifier = Modifier.fillMaxWidth(), enabled = !saving)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(enabled = !saving && rating in 1..5 && (status != ChatAuditStatus.CORRECTED || correction.isNotBlank()),
                    onClick = {
                        if (saving) return@Button
                        saving = true
                        error = null
                        scope.launch {
                            try {
                                client.feedback(entry.id, rating, status, correction)
                                onSaved()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                error = "Kaydedilen geri bildirim doğrulanamadı. Taslak korunuyor."
                            } finally {
                                saving = false
                            }
                        }
                    }) { Text(if (saving) "Kaydediliyor…" else "Geri bildirimi kaydet") }
                TextButton(onClick = onDismiss, enabled = !saving) { Text("Kapat") }
            }
        }
    }
}
