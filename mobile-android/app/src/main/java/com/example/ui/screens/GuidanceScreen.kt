package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.GuidanceChoice
import com.example.data.remote.GuidanceIncidentDraft
import com.example.data.remote.GuidanceIncidentRow
import com.example.data.remote.GuidanceNoteDraft
import com.example.data.remote.GuidanceNoteRow
import com.example.data.remote.GuidanceOptions
import com.example.data.remote.GuidanceRow
import com.example.data.remote.GuidanceSessionDraft
import com.example.data.remote.GuidanceSessionRow
import com.example.data.remote.NativeGuidanceClient
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val GUIDANCE_PAGE_SIZE = 20
private val guidanceTypes = mapOf("INDIVIDUAL" to "Bireysel", "GROUP" to "Grup", "FAMILY" to "Aile", "CRISIS" to "Kriz")
private val confidentialityLevels = mapOf("STANDARD" to "Standart", "RESTRICTED" to "Kısıtlı", "CONFIDENTIAL" to "Gizli")
private val noteTypes = mapOf("OBSERVATION" to "Gözlem", "ASSESSMENT" to "Değerlendirme", "INTERVENTION" to "Müdahale")
private val severityLevels = mapOf("MINOR" to "Düşük", "MODERATE" to "Orta", "SEVERE" to "Yüksek", "CRITICAL" to "Kritik")
private enum class GuidanceSection(val title: String, val createTitle: String, val saveTitle: String, val saved: String) {
    SESSIONS("Görüşmeler", "Yeni görüşme", "Görüşmeyi kaydet", "Rehberlik görüşmesi kaydedildi."),
    NOTES("Danışman notları", "Yeni danışman notu", "Notu kaydet", "Danışman notu kaydedildi."),
    INCIDENTS("Davranış olayları", "Yeni davranış olayı", "Olayı kaydet", "Davranış olayı kaydedildi."),
}

@Composable
fun GuidanceScreen(state: Okul360UiState, onNavigateBack: () -> Unit = {}) {
    val user = state.currentUser
    key(financePresentationScope(state), state.serverBaseUrl) {
        when {
            !state.isLoggedIn || !NativeNavigationPolicy.has(user, "guidance.read") ->
                Text("Rehberlik görüşmelerini görüntüleme yetkiniz yok.", Modifier.padding(16.dp))
            user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank() ->
                Text("Rehberlik görüşmeleri için bir kurum çalışma alanı seçin.", Modifier.padding(16.dp))
            else -> GuidanceRecords(user.id, NativeNavigationPolicy.has(user, "guidance.write"), onNavigateBack)
        }
    }
}

@Composable
private fun GuidanceRecords(actorId: String, canWrite: Boolean, onBack: () -> Unit) {
    val client = remember { NativeGuidanceClient() }
    var section by remember { mutableStateOf(GuidanceSection.SESSIONS) }
    var student by remember { mutableStateOf<GuidanceChoice?>(null) }
    var records by remember { mutableStateOf<List<GuidanceRow>?>(null) }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var choosing by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<GuidanceRow?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun reload(targetPage: Int) {
        records = null
        selectedRecord = null
        error = null
        page = targetPage
        revision++
    }
    LaunchedEffect(section, student, revision) {
        try {
            val offset = page * GUIDANCE_PAGE_SIZE
            val limit = GUIDANCE_PAGE_SIZE + 1
            records = when (section) {
                GuidanceSection.SESSIONS -> client.sessions(student, offset, limit)
                GuidanceSection.NOTES -> client.notes(student, offset, limit)
                GuidanceSection.INCIDENTS -> client.incidents(student, offset, limit)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "Rehberlik kayıtları yüklenemedi."
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("guidance_sessions"),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            TextButton(onClick = onBack) { Text("Geri") }
            Text("Rehberlik servisi", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Column {
                GuidanceSection.entries.forEach { option ->
                    FilterChip(section == option, onClick = {
                        section = option
                        feedback = null
                        reload(0)
                    }, label = { Text(option.title) })
                }
            }
        }
        item {
            Text("Öğrenci: ${student?.name ?: "Tüm öğrenciler"}")
            OutlinedButton(onClick = { choosing = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Öğrenci seç") }
            if (student != null) TextButton(onClick = { student = null; feedback = null; reload(0) }) { Text("Tüm öğrenciler") }
            if (canWrite && student != null) Button(onClick = { creating = true; feedback = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(section.createTitle)
            }
            feedback?.let { Text(it) }
        }
        val rows = records
        when {
            error != null -> item {
                Text(error.orEmpty())
                OutlinedButton(onClick = { reload(page) }) { Text("Yeniden yükle") }
            }
            rows == null -> item { Text("Rehberlik kayıtları yükleniyor…") }
            rows.isEmpty() -> item { Text("Bu kapsamda rehberlik kaydı bulunamadı.") }
            else -> items(rows.take(GUIDANCE_PAGE_SIZE), key = { it.id }) { record ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        record.studentName?.let { Text(it) }
                        val action = when (record) {
                            is GuidanceSessionRow -> {
                                Text(guidanceTypes[record.type] ?: record.type, style = MaterialTheme.typography.titleMedium)
                                record.counselorName?.let { Text("Danışman: $it") }
                                Text(record.date)
                                Text(confidentialityLevels[record.confidentiality] ?: record.confidentiality)
                                "Görüşmeyi incele"
                            }
                            is GuidanceNoteRow -> {
                                Text(noteTypes[record.type] ?: record.type, style = MaterialTheme.typography.titleMedium)
                                record.counselorName?.let { Text("Danışman: $it") }
                                Text(record.createdAt.take(10))
                                Text(confidentialityLevels[record.confidentiality] ?: record.confidentiality)
                                "Notu incele"
                            }
                            is GuidanceIncidentRow -> {
                                Text(severityLevels[record.severity] ?: record.severity, style = MaterialTheme.typography.titleMedium)
                                record.reporter?.let { Text("Bildiren: $it") }
                                Text(record.date)
                                Text(if (record.resolved) "Çözüldü" else "Açık olay")
                                "Olayı incele"
                            }
                        }
                        TextButton(onClick = { selectedRecord = record }, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
                    }
                }
            }
        }
        if (rows != null && (page > 0 || rows.size > GUIDANCE_PAGE_SIZE)) item {
            Column {
                Text("Sayfa ${page + 1}")
                OutlinedButton(enabled = page > 0, onClick = { reload(page - 1) }) { Text("Önceki") }
                OutlinedButton(enabled = rows.size > GUIDANCE_PAGE_SIZE, onClick = { reload(page + 1) }) { Text("Sonraki") }
            }
        }
    }
    if (choosing) GuidanceStudentPicker(client, { choosing = false }) { selected ->
        student = selected
        feedback = null
        choosing = false
        reload(0)
    }
    val currentStudent = student
    if (creating && canWrite && currentStudent != null) {
        GuidanceCreateRecord(client, actorId, section, currentStudent, { creating = false }) {
            feedback = section.saved
            creating = false
            reload(0)
        }
    }
    when (val record = selectedRecord) {
        is GuidanceSessionRow -> ConsentFormDialog(guidanceTypes[record.type] ?: record.type, "guidance_detail", false, { selectedRecord = null }, {
            record.summary?.let { item { Text(it) } }
            record.actionPlan?.let { item { Text("Takip planı: $it") } }
            record.followUp?.let { item { Text("Takip tarihi: $it") } }
        }) {}
        is GuidanceNoteRow -> ConsentFormDialog(noteTypes[record.type] ?: record.type, "guidance_detail", false, { selectedRecord = null }, {
            item { Text(record.content) }
            if (record.tags.isNotEmpty()) item { Text(record.tags.joinToString(", ")) }
        }) {}
        is GuidanceIncidentRow -> GuidanceIncidentDetails(client, record, canWrite, { selectedRecord = null }) {
            feedback = "Olay çözümü kaydedildi."
            reload(page)
        }
        null -> Unit
    }
}

@Composable
private fun GuidanceStudentPicker(client: NativeGuidanceClient, onClose: () -> Unit, onSelect: (GuidanceChoice) -> Unit) {
    var search by remember { mutableStateOf("") }
    var options by remember { mutableStateOf<GuidanceOptions?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(search, revision) {
        try {
            if (search.isNotBlank()) delay(250)
            options = client.options(null, search, "")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "Öğrenciler yüklenemedi."
        }
    }
    ConsentFormDialog("Rehberlik öğrencisi", "guidance_students", false, onClose, {
        item {
            OutlinedTextField(search, { search = it.take(100); options = null; error = null },
                modifier = Modifier.fillMaxWidth(), label = { Text("Öğrenci ara") })
        }
        when {
            error != null -> item {
                Text(error.orEmpty())
                OutlinedButton(onClick = { error = null; revision++ }) { Text("Yeniden yükle") }
            }
            options == null -> item { Text("Öğrenciler yükleniyor…") }
            options?.students?.isEmpty() == true -> item { Text("Öğrenci bulunamadı.") }
        }
        if (options?.moreStudents == true) item { Text("Öğrenci adını yazarak listeyi daraltın.") }
        items(options?.students.orEmpty(), key = { it.id }) { choice ->
            TextButton(onClick = { onSelect(choice) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(choice.name) }
        }
    }) {}
}

@Composable
private fun GuidanceCreateRecord(
    client: NativeGuidanceClient, actorId: String, section: GuidanceSection, student: GuidanceChoice,
    onClose: () -> Unit, onSaved: () -> Unit,
) {
    val actions = rememberCoroutineScope()
    var options by remember { mutableStateOf<GuidanceOptions?>(null) }
    var counselor by remember { mutableStateOf<GuidanceChoice?>(null) }
    var counselorSearch by remember { mutableStateOf("") }
    var optionError by remember { mutableStateOf<String?>(null) }
    var optionRevision by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf("") }
    val types = when (section) {
        GuidanceSection.SESSIONS -> guidanceTypes
        GuidanceSection.NOTES -> noteTypes
        GuidanceSection.INCIDENTS -> severityLevels
    }
    var type by remember { mutableStateOf(types.keys.first()) }
    var confidentiality by remember { mutableStateOf("STANDARD") }
    var summary by remember { mutableStateOf("") }
    var actionPlan by remember { mutableStateOf("") }
    var followUp by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var parentNotified by remember { mutableStateOf(false) }
    LaunchedEffect(counselorSearch, optionRevision) {
        try {
            if (counselorSearch.isNotBlank()) delay(250)
            val loaded = client.options(student, "", counselorSearch)
            options = loaded
            if (date.isEmpty()) date = loaded.today.toString()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            optionError = failure.message ?: "Danışmanlar yüklenemedi."
        }
    }
    ConsentFormDialog(section.createTitle, "guidance_create", busy, onClose, {
        item { Text(student.name) }
        if (section != GuidanceSection.INCIDENTS) {
            item {
                OutlinedTextField(counselorSearch, { counselorSearch = it.take(100); counselor = null; options = null; optionError = null },
                    enabled = !busy, modifier = Modifier.fillMaxWidth(), label = { Text("Danışman ara") })
            }
            when {
                optionError != null -> item {
                    Text(optionError.orEmpty())
                    OutlinedButton(onClick = { optionError = null; optionRevision++ }) { Text("Yeniden yükle") }
                }
                options == null -> item { Text("Danışmanlar yükleniyor…") }
                options?.counselors?.isEmpty() == true -> item { Text("Uygun danışman bulunamadı.") }
            }
            if (options?.moreCounselors == true) item { Text("Danışman adını yazarak listeyi daraltın.") }
            items(options?.counselors.orEmpty(), key = { it.id }) { choice ->
                FilterChip(counselor == choice, enabled = !busy, onClick = { counselor = choice }, label = { Text(choice.name) })
            }
        } else if (optionError != null) item {
            Text(optionError.orEmpty())
            OutlinedButton(onClick = { optionError = null; optionRevision++ }) { Text("Yeniden yükle") }
        }
        if (section != GuidanceSection.NOTES) item {
            val label = if (section == GuidanceSection.SESSIONS) "Görüşme tarihi (YYYY-AA-GG)" else "Olay tarihi (YYYY-AA-GG)"
            OutlinedTextField(date, { date = it.take(10) }, enabled = !busy, label = { Text(label) })
        }
        items(types.toList()) { (value, title) ->
            FilterChip(type == value, enabled = !busy, onClick = { type = value }, label = { Text(title) })
        }
        if (section != GuidanceSection.INCIDENTS) {
            items(confidentialityLevels.toList()) { (value, title) ->
                FilterChip(confidentiality == value, enabled = !busy, onClick = { confidentiality = value }, label = { Text(title) })
            }
        }
        when (section) {
            GuidanceSection.SESSIONS -> {
                item { OutlinedTextField(summary, { summary = it.take(2000) }, enabled = !busy, label = { Text("Görüşme özeti") }) }
                item { OutlinedTextField(actionPlan, { actionPlan = it.take(2000) }, enabled = !busy, label = { Text("Takip planı") }) }
                item { OutlinedTextField(followUp, { followUp = it.take(10) }, enabled = !busy, label = { Text("Takip tarihi (YYYY-AA-GG)") }) }
            }
            GuidanceSection.NOTES -> {
                item { OutlinedTextField(summary, { summary = it.take(5000) }, enabled = !busy, label = { Text("Danışman notu") }) }
                item { OutlinedTextField(tags, { tags = it.take(1000) }, enabled = !busy, label = { Text("Etiketler (virgülle ayırın)") }) }
            }
            GuidanceSection.INCIDENTS -> {
                item { OutlinedTextField(location, { location = it.take(255) }, enabled = !busy, label = { Text("Olay yeri") }) }
                item { OutlinedTextField(summary, { summary = it.take(2000) }, enabled = !busy, label = { Text("Olay açıklaması") }) }
                item {
                    Row {
                        Checkbox(parentNotified, { parentNotified = it }, enabled = !busy)
                        Text("Veliye daha önce bilgi verildi")
                    }
                }
            }
        }
        error?.let { item { Text(it) } }
    }) {
        val sessionDate = runCatching { LocalDate.parse(date) }.getOrNull()
        val followUpDate = runCatching { LocalDate.parse(followUp) }.getOrNull()
        val selectedCounselor = counselor
        val validFollowUp = followUp.isBlank() || followUpDate != null && sessionDate != null && followUpDate >= sessionDate
        val validCounselor = selectedCounselor != null && options?.counselors?.contains(selectedCounselor) == true
        val tagList = tags.split(",").map(String::trim).filter(String::isNotEmpty)
        val ready = when (section) {
            GuidanceSection.SESSIONS -> sessionDate != null && validCounselor && validFollowUp
            GuidanceSection.NOTES -> validCounselor && summary.isNotBlank() && tagList.size <= 20 && tagList.all { it.length <= 50 }
            GuidanceSection.INCIDENTS -> sessionDate != null && options != null && summary.isNotBlank()
        }
        Button(enabled = !busy && ready,
            modifier = Modifier.heightIn(min = 48.dp), onClick = {
                if (!ready || busy) return@Button
                busy = true
                error = null
                actions.launch {
                    try {
                        when (section) {
                            GuidanceSection.SESSIONS -> client.create(GuidanceSessionDraft(student, requireNotNull(selectedCounselor), type,
                                requireNotNull(sessionDate), confidentiality, summary.trim(), actionPlan.trim(), followUpDate))
                            GuidanceSection.NOTES -> client.createNote(GuidanceNoteDraft(student, requireNotNull(selectedCounselor), type,
                                confidentiality, summary.trim(), tagList))
                            GuidanceSection.INCIDENTS -> client.createIncident(GuidanceIncidentDraft(student, requireNotNull(sessionDate), type,
                                summary.trim(), location.trim(), parentNotified), actorId)
                        }
                        onSaved()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        error = failure.message ?: "Rehberlik kaydı oluşturulamadı."
                    } finally {
                        busy = false
                    }
                }
            }) { Text(section.saveTitle) }
    }
}

@Composable
private fun GuidanceIncidentDetails(
    client: NativeGuidanceClient, incident: GuidanceIncidentRow, canWrite: Boolean, onClose: () -> Unit, onResolved: () -> Unit,
) {
    val actions = rememberCoroutineScope()
    var notes by remember(incident.id) { mutableStateOf("") }
    var busy by remember(incident.id) { mutableStateOf(false) }
    var error by remember(incident.id) { mutableStateOf<String?>(null) }
    ConsentFormDialog("Davranış olayı", "guidance_detail", busy, onClose, {
        item { Text(incident.description) }
        incident.location?.let { item { Text("Olay yeri: $it") } }
        if (incident.parentNotified) item { Text("Kayıtta veliye bilgi verildiği belirtilmiş.") }
        incident.resolutionNotes?.let { item { Text("Çözüm: $it") } }
        if (canWrite && !incident.resolved) item {
            OutlinedTextField(notes, { notes = it.take(2000) }, enabled = !busy, label = { Text("Çözüm notu") })
        }
        error?.let { item { Text(it) } }
    }) {
        if (canWrite && !incident.resolved) Button(enabled = !busy, modifier = Modifier.heightIn(min = 48.dp), onClick = {
            if (busy) return@Button
            busy = true
            error = null
            actions.launch {
                try {
                    client.resolve(incident, notes.trim().ifBlank { null })
                    onResolved()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    error = failure.message ?: "Çözüm kaydedilemedi."
                } finally {
                    busy = false
                }
            }
        }) { Text("Çözüldü olarak kaydet") }
    }
}
