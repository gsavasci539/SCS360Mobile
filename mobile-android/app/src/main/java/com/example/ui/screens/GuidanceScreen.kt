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
import com.example.data.remote.GuidanceOptions
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

@Composable
fun GuidanceScreen(state: Okul360UiState, onNavigateBack: () -> Unit = {}) {
    val user = state.currentUser
    key(financePresentationScope(state), state.serverBaseUrl) {
        when {
            !state.isLoggedIn || !NativeNavigationPolicy.has(user, "guidance.read") ->
                Text("Rehberlik görüşmelerini görüntüleme yetkiniz yok.", Modifier.padding(16.dp))
            user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank() ->
                Text("Rehberlik görüşmeleri için bir kurum çalışma alanı seçin.", Modifier.padding(16.dp))
            else -> GuidanceSessions(NativeNavigationPolicy.has(user, "guidance.write"), onNavigateBack)
        }
    }
}

@Composable
private fun GuidanceSessions(canWrite: Boolean, onBack: () -> Unit) {
    val client = remember { NativeGuidanceClient() }
    var student by remember { mutableStateOf<GuidanceChoice?>(null) }
    var records by remember { mutableStateOf<List<GuidanceSessionRow>?>(null) }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var choosing by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<GuidanceSessionRow?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun reload(targetPage: Int) {
        records = null
        error = null
        page = targetPage
        revision++
    }
    LaunchedEffect(student, revision) {
        try {
            records = client.sessions(student, page * GUIDANCE_PAGE_SIZE, GUIDANCE_PAGE_SIZE + 1)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "Rehberlik görüşmeleri yüklenemedi."
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("guidance_sessions"),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            TextButton(onClick = onBack) { Text("Geri") }
            Text("Rehberlik görüşmeleri", style = MaterialTheme.typography.headlineSmall)
            Text("Öğrenci: ${student?.name ?: "Tüm öğrenciler"}")
            OutlinedButton(onClick = { choosing = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Öğrenci seç") }
            if (student != null) TextButton(onClick = { student = null; feedback = null; reload(0) }) { Text("Tüm öğrenciler") }
            if (canWrite && student != null) Button(onClick = { creating = true; feedback = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Yeni görüşme")
            }
            feedback?.let { Text(it) }
        }
        val rows = records
        when {
            error != null -> item {
                Text(error.orEmpty())
                OutlinedButton(onClick = { reload(page) }) { Text("Yeniden yükle") }
            }
            rows == null -> item { Text("Görüşmeler yükleniyor…") }
            rows.isEmpty() -> item { Text("Rehberlik görüşmesi bulunamadı.") }
            else -> items(rows.take(GUIDANCE_PAGE_SIZE), key = { it.id }) { record ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(guidanceTypes[record.type] ?: record.type, style = MaterialTheme.typography.titleMedium)
                        record.studentName?.let { Text(it) }
                        record.counselorName?.let { Text("Danışman: $it") }
                        Text(record.date)
                        Text(confidentialityLevels[record.confidentiality] ?: record.confidentiality)
                        TextButton(onClick = { selectedRecord = record }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Görüşmeyi incele") }
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
        GuidanceCreateSession(client, currentStudent, { creating = false }) {
            feedback = "Rehberlik görüşmesi kaydedildi."
            creating = false
            reload(0)
        }
    }
    selectedRecord?.let { record ->
        ConsentFormDialog(guidanceTypes[record.type] ?: record.type, "guidance_detail", false, { selectedRecord = null }, {
            record.summary?.let { item { Text(it) } }
            record.actionPlan?.let { item { Text("Takip planı: $it") } }
            record.followUp?.let { item { Text("Takip tarihi: $it") } }
        }) {}
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
private fun GuidanceCreateSession(client: NativeGuidanceClient, student: GuidanceChoice, onClose: () -> Unit, onSaved: () -> Unit) {
    val actions = rememberCoroutineScope()
    var options by remember { mutableStateOf<GuidanceOptions?>(null) }
    var counselor by remember { mutableStateOf<GuidanceChoice?>(null) }
    var counselorSearch by remember { mutableStateOf("") }
    var optionError by remember { mutableStateOf<String?>(null) }
    var optionRevision by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("INDIVIDUAL") }
    var confidentiality by remember { mutableStateOf("STANDARD") }
    var summary by remember { mutableStateOf("") }
    var actionPlan by remember { mutableStateOf("") }
    var followUp by remember { mutableStateOf("") }
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
    ConsentFormDialog("Yeni görüşme", "guidance_create", busy, onClose, {
        item { Text(student.name) }
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
        item { OutlinedTextField(date, { date = it.take(10) }, enabled = !busy, label = { Text("Görüşme tarihi (YYYY-AA-GG)") }) }
        items(guidanceTypes.toList()) { (value, title) ->
            FilterChip(type == value, enabled = !busy, onClick = { type = value }, label = { Text(title) })
        }
        items(confidentialityLevels.toList()) { (value, title) ->
            FilterChip(confidentiality == value, enabled = !busy, onClick = { confidentiality = value }, label = { Text(title) })
        }
        item { OutlinedTextField(summary, { summary = it.take(2000) }, enabled = !busy, label = { Text("Görüşme özeti") }) }
        item { OutlinedTextField(actionPlan, { actionPlan = it.take(2000) }, enabled = !busy, label = { Text("Takip planı") }) }
        item { OutlinedTextField(followUp, { followUp = it.take(10) }, enabled = !busy, label = { Text("Takip tarihi (YYYY-AA-GG)") }) }
        error?.let { item { Text(it) } }
    }) {
        val sessionDate = runCatching { LocalDate.parse(date) }.getOrNull()
        val followUpDate = runCatching { LocalDate.parse(followUp) }.getOrNull()
        val selectedCounselor = counselor
        val validFollowUp = followUp.isBlank() || followUpDate != null && sessionDate != null && followUpDate >= sessionDate
        Button(enabled = !busy && sessionDate != null && selectedCounselor != null &&
            options?.counselors?.contains(selectedCounselor) == true && validFollowUp,
            modifier = Modifier.heightIn(min = 48.dp), onClick = {
                if (sessionDate == null || selectedCounselor == null || busy) return@Button
                val draft = GuidanceSessionDraft(student, selectedCounselor, type, sessionDate, confidentiality,
                    summary.trim(), actionPlan.trim(), followUpDate)
                busy = true
                error = null
                actions.launch {
                    try {
                        client.create(draft)
                        onSaved()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        error = failure.message ?: "Rehberlik görüşmesi kaydedilemedi."
                    } finally {
                        busy = false
                    }
                }
            }) { Text("Görüşmeyi kaydet") }
    }
}
