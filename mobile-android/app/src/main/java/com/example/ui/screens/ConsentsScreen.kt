package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.ConsentChoice
import com.example.data.remote.ConsentDecisionRow
import com.example.data.remote.ConsentDocumentDraft
import com.example.data.remote.ConsentDocumentRow
import com.example.data.remote.ConsentOptions
import com.example.data.remote.NativeConsentClient
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val CONSENT_PAGE_SIZE = 20

@Composable
fun ConsentsScreen(state: Okul360UiState, onNavigateBack: () -> Unit = {}) {
    val user = state.currentUser
    val organization = if (user.is_super_admin == true) state.workspaceOrganizationId else user.organization_id
    key(financePresentationScope(state), state.serverBaseUrl) {
        when {
            !state.isLoggedIn || !NativeNavigationPolicy.has(user, "consents.read") ->
                Text("İzin kayıtlarını görüntüleme yetkiniz yok.", Modifier.padding(16.dp))
            organization.isNullOrBlank() ->
                Text("İzin belgeleri için bir kurum çalışma alanı seçin.", Modifier.padding(16.dp))
            else -> InstitutionConsents(organization, NativeNavigationPolicy.has(user, "consents.write"), onNavigateBack)
        }
    }
}

@Composable
private fun InstitutionConsents(organization: String, canWrite: Boolean, onNavigateBack: () -> Unit) {
    val client = remember { NativeConsentClient() }
    val actions = rememberCoroutineScope()
    var decisionsTab by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(true) }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var documents by remember { mutableStateOf<List<ConsentDocumentRow>>(emptyList()) }
    var decisions by remember { mutableStateOf<List<ConsentDecisionRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failure by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf<ConsentDocumentRow?>(null) }
    var recording by remember { mutableStateOf(false) }
    fun reload(targetPage: Int = page) {
        loading = true
        failure = null
        opened = null
        page = targetPage
        revision++
    }
    LaunchedEffect(revision) {
        loading = true
        try {
            if (decisionsTab) decisions = client.decisions(page * CONSENT_PAGE_SIZE, CONSENT_PAGE_SIZE + 1)
            else documents = client.documents(active, page * CONSENT_PAGE_SIZE, CONSENT_PAGE_SIZE + 1)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            failure = error.message ?: "İzin kayıtları yüklenemedi."
        } finally {
            loading = false
        }
    }
    fun submit(success: String, afterSave: () -> Unit, work: suspend () -> Unit) {
        if (!canWrite || busy) return
        busy = true
        feedback = null
        actions.launch {
            try {
                work()
                afterSave()
                feedback = success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                feedback = error.message ?: "Sunucu işlemi doğrulamadı."
            } finally {
                busy = false
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().testTag("consent_records"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onNavigateBack) { Text("Geri") }
            Text("Veli Onay Masası", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(!decisionsTab, enabled = !busy, onClick = { decisionsTab = false; reload(0) }, label = { Text("İzin belgeleri") })
                FilterChip(decisionsTab, enabled = !busy, onClick = { decisionsTab = true; reload(0) }, label = { Text("Veli kararları") })
                if (!decisionsTab) {
                    FilterChip(!active, enabled = !busy, onClick = { active = !active; reload(0) },
                        label = { Text(if (active) "Arşivi göster" else "Etkin belgeleri göster") })
                    if (canWrite) Button(enabled = !busy, onClick = { creating = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Yeni izin belgesi")
                    }
                }
                feedback?.let { Text(it) }
            }
        }
        when {
            loading -> item { Text("İzin kayıtları yükleniyor…") }
            failure != null -> item {
                Text(failure.orEmpty())
                OutlinedButton(onClick = { reload() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Yeniden yükle") }
            }
            decisionsTab -> {
                if (decisions.isEmpty()) item { Text("Kayıtlı veli kararı bulunamadı.") }
                items(decisions.take(CONSENT_PAGE_SIZE), key = { it.id }) { decision ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            decision.documentTitle?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                            decision.documentVersion?.let { Text("Sürüm $it") }
                            decision.studentName?.let { Text(it) }
                            decision.parentName?.let { Text("Veli $it") }
                            Text(when (decision.status) {
                                "APPROVED" -> "Onaylandı"
                                "REJECTED" -> "Reddedildi"
                                "PENDING" -> "Bekliyor"
                                else -> decision.status
                            })
                            if (decision.method == "MANUAL") Text("Elle kaydedildi")
                            if (decision.method == "PAPER") Text("Kâğıt belge")
                            decision.decidedAt?.let { Text(it) }
                        }
                    }
                }
            }
            else -> {
                if (documents.isEmpty()) item { Text("İzin belgesi bulunamadı.") }
                items(documents.take(CONSENT_PAGE_SIZE), key = { it.id }) { document ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(document.title, style = MaterialTheme.typography.titleMedium)
                            Text("Sürüm ${document.version}")
                            Text("Yürürlük ${document.effectiveDate}")
                            TextButton(enabled = !busy, onClick = { opened = document }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text("Belgeyi incele")
                            }
                        }
                    }
                }
            }
        }
        if (!loading && failure == null) {
            val itemCount = if (decisionsTab) decisions.size else documents.size
            item {
                Column {
                    Text("Sayfa ${page + 1}")
                    OutlinedButton(enabled = page > 0 && !busy, onClick = { reload(page - 1) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Önceki")
                    }
                    OutlinedButton(enabled = itemCount > CONSENT_PAGE_SIZE && !busy, onClick = { reload(page + 1) },
                        modifier = Modifier.heightIn(min = 48.dp)) { Text("Sonraki") }
                }
            }
        }
    }

    if (creating && canWrite) ConsentDocumentForm(busy, feedback, { creating = false }) { draft ->
        submit("İzin belgesi kaydedildi.", { creating = false; active = true; reload(0) }) {
            client.createDocument(organization, draft)
        }
    }
    val document = opened
    if (document != null && !recording) AlertDialog(
        onDismissRequest = { if (!busy) opened = null },
        title = { Text(document.title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 240.dp).testTag("consent_document"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(document.content) }
                if (canWrite && active) item {
                    Button(enabled = !busy, onClick = { recording = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Veli kararı kaydet")
                    }
                }
                if (canWrite) item {
                    OutlinedButton(enabled = !busy, onClick = {
                        submit("Belge durumu güncellendi.", { reload() }) { client.changeDocumentStatus(document, !active) }
                    }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (active) "Arşive al" else "Etkinleştir") }
                }
                feedback?.let { item { Text(it) } }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = { opened = null }) { Text("Kapat") } },
    )
    if (document != null && recording && canWrite) ConsentDecisionForm(
        document, client, organization, busy, feedback, { recording = false },
    ) { student, parent, status, method ->
        submit("Veli kararı kaydedildi.", { recording = false; decisionsTab = true; reload(0) }) {
            client.saveDecision(document, student, parent, status, method)
        }
    }
}

@Composable
private fun ConsentDocumentForm(busy: Boolean, feedback: String?, onClose: () -> Unit, onSave: (ConsentDocumentDraft) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("TRIP_CONSENT") }
    var version by remember { mutableStateOf("1.0") }
    var effectiveDate by remember { mutableStateOf("") }
    ConsentFormDialog("Yeni izin belgesi", "consent_create", busy, onClose, {
        item { OutlinedTextField(title, { title = it.take(255) }, enabled = !busy, label = { Text("Belge başlığı") }) }
        item { OutlinedTextField(content, { content = it }, enabled = !busy, label = { Text("Belge metni") }) }
        items(listOf("TRIP_CONSENT" to "Gezi izni", "PHOTO_CONSENT" to "Fotoğraf izni", "KVKK" to "KVKK", "OTHER" to "Diğer")) { (code, label) ->
            FilterChip(type == code, enabled = !busy, onClick = { type = code }, label = { Text(label) })
        }
        item { OutlinedTextField(version, { version = it.take(20) }, enabled = !busy, label = { Text("Sürüm") }) }
        item {
            OutlinedTextField(effectiveDate, { effectiveDate = it.take(10) },
                enabled = !busy, label = { Text("Yürürlük tarihi (YYYY-AA-GG)") })
        }
        feedback?.let { item { Text(it) } }
    }) {
        val date = runCatching { LocalDate.parse(effectiveDate) }.getOrNull()
        Button(enabled = !busy && title.isNotBlank() && content.isNotBlank() && content.length <= 100000 &&
            version.isNotBlank() && date != null,
            onClick = { onSave(ConsentDocumentDraft(title.trim(), type, content.trim(), version.trim(), requireNotNull(date).toString())) }) {
            Text("Kaydet")
        }
    }
}

@Composable
private fun ConsentDecisionForm(
    document: ConsentDocumentRow, client: NativeConsentClient, organization: String, busy: Boolean,
    feedback: String?, onClose: () -> Unit, onSave: (ConsentChoice, ConsentChoice, String, String) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var student by remember { mutableStateOf<ConsentChoice?>(null) }
    var parent by remember { mutableStateOf<ConsentChoice?>(null) }
    var options by remember { mutableStateOf<ConsentOptions?>(null) }
    var status by remember { mutableStateOf("APPROVED") }
    var method by remember { mutableStateOf("MANUAL") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(search, student) {
        options = null
        error = null
        try {
            if (search.isNotBlank() && student == null) delay(250)
            options = client.options(organization, search, student)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "Öğrenci ve veli seçenekleri yüklenemedi."
        }
    }
    ConsentFormDialog("Veli kararı", "consent_decision", busy, onClose, {
        item { Text("${document.title} · ${document.version}") }
        item {
            OutlinedTextField(search, { search = it.take(100); student = null; parent = null; options = null },
                enabled = !busy, label = { Text("Öğrenci ara") })
        }
        if (student == null) {
            if (options?.moreStudents == true) item { Text("Öğrenci adını yazarak listeyi daraltın.") }
            items(options?.students.orEmpty(), key = { it.id }) { row ->
                TextButton(enabled = !busy, onClick = { student = row; parent = null; options = null }) { Text(row.label) }
            }
        } else {
            item { Text(student?.label.orEmpty(), style = MaterialTheme.typography.titleSmall) }
            items(options?.parents.orEmpty(), key = { it.id }) { row ->
                FilterChip(parent == row, enabled = !busy, onClick = { parent = row }, label = { Text(row.label) })
            }
            if (options?.parents?.isEmpty() == true) item { Text("Öğrenciye bağlı veli bulunamadı.") }
        }
        if (options == null && error == null) item { Text("Seçenekler yükleniyor…") }
        error?.let { item { Text(it) } }
        if (parent != null) {
            items(listOf("APPROVED" to "Onay", "REJECTED" to "Ret")) { (code, title) ->
                FilterChip(status == code, enabled = !busy, onClick = { status = code }, label = { Text(title) })
            }
            items(listOf("MANUAL" to "Elle kayıt", "PAPER" to "Kâğıt belge")) { (code, title) ->
                FilterChip(method == code, enabled = !busy, onClick = { method = code }, label = { Text(title) })
            }
        }
        feedback?.let { item { Text(it) } }
    }) {
        val selectedStudent = student
        val selectedParent = parent
        Button(enabled = !busy && selectedStudent != null && selectedParent != null && options?.parents?.contains(selectedParent) == true,
            onClick = { if (selectedStudent != null && selectedParent != null) onSave(selectedStudent, selectedParent, status, method) }) {
            Text("Kararı kaydet")
        }
    }
}

@Composable
private fun ConsentFormDialog(
    title: String, tag: String, busy: Boolean, onClose: () -> Unit,
    fields: LazyListScope.() -> Unit, confirm: @Composable () -> Unit,
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * .85f).dp
    Dialog(onDismissRequest = { if (!busy) onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 600.dp).fillMaxWidth().heightIn(max = maxHeight).imePadding(),
            shape = MaterialTheme.shapes.large) {
            LazyColumn(Modifier.testTag(tag), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(title, style = MaterialTheme.typography.titleLarge) }
                fields()
                item { confirm() }
                item { TextButton(enabled = !busy, onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("İptal") } }
            }
        }
    }
}
