package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.data.remote.InfirmaryOptions
import com.example.data.remote.HealthFileField
import com.example.data.remote.InfirmaryHealthFile
import com.example.data.remote.InfirmaryDose
import com.example.data.remote.InfirmaryMedication
import com.example.data.remote.InfirmaryMedicationDraft
import com.example.data.remote.InfirmaryStudent
import com.example.data.remote.InfirmaryVisitDraft
import com.example.data.remote.NativeInfirmaryClient
import com.example.data.remote.NativeInfirmaryVisit
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val VISIT_PAGE_SIZE = 20
private val visitSeverities = linkedMapOf("LOW" to "Düşük", "MEDIUM" to "Orta", "HIGH" to "Yüksek", "EMERGENCY" to "Acil")
private val visitTimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val medicationStatuses = mapOf("PENDING" to "Bekliyor", "APPROVED" to "Onaylı", "REJECTED" to "Reddedildi", "COMPLETED" to "Kapandı")
private val doseStatuses = linkedMapOf("GIVEN" to "Uygulandı", "SKIPPED" to "Atlandı", "CANCELLED" to "İptal edildi")
private val mealInstructions = linkedMapOf("NOT_SPECIFIED" to "Belirtilmedi", "BEFORE_MEAL" to "Yemekten önce",
    "AFTER_MEAL" to "Yemekten sonra", "WITH_MEAL" to "Yemekle birlikte", "EMPTY_STOMACH" to "Aç karnına")
private enum class InfirmarySection(val label: String) {
    VISITS("Revir ziyaretleri"), FILES("Sağlık dosyaları"),
    PROGRAMS("İlaç programları ve talepler"), DOSES("Günlük doz kayıtları"),
}

@Composable
fun InfirmaryScreen(state: Okul360UiState, onNavigateBack: () -> Unit = {}) {
    val user = state.currentUser
    key(financePresentationScope(state), state.serverBaseUrl) {
        when {
            !state.isLoggedIn || !NativeNavigationPolicy.has(user, "health.read") ->
                Text("Revir kayıtlarını görüntüleme yetkiniz yok.", Modifier.padding(16.dp))
            user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank() ->
                Text("Revir kayıtları için bir kurum çalışma alanı seçin.", Modifier.padding(16.dp))
            else -> InfirmaryWorkspace(state.selectedBranchId, user.id, NativeNavigationPolicy.has(user, "health.write"), onNavigateBack)
        }
    }
}

@Composable
private fun InfirmaryWorkspace(branch: String, actor: String, canWrite: Boolean, onBack: () -> Unit) {
    val client = remember { NativeInfirmaryClient(branch, actor) }
    var section by remember { mutableStateOf(InfirmarySection.VISITS) }
    var options by remember { mutableStateOf<InfirmaryOptions?>(null) }
    var optionsError by remember { mutableStateOf<String?>(null) }
    var optionsRevision by remember { mutableIntStateOf(0) }
    var student by remember { mutableStateOf<InfirmaryStudent?>(null) }
    var healthFile by remember(student) { mutableStateOf<Result<InfirmaryHealthFile?>?>(null) }
    var fileRevision by remember { mutableIntStateOf(0) }
    var editingFile by remember(student) { mutableStateOf(false) }
    var fileSaved by remember(student) { mutableStateOf(false) }
    var dayText by remember { mutableStateOf("") }
    var day by remember { mutableStateOf<LocalDate?>(null) }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var visits by remember { mutableStateOf<List<NativeInfirmaryVisit>?>(null) }
    var visitsError by remember { mutableStateOf<String?>(null) }
    var choosing by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf<NativeInfirmaryVisit?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    fun reload(targetPage: Int) {
        page = targetPage
        details = null
        visits = null
        visitsError = null
        revision++
    }

    LaunchedEffect(optionsRevision) {
        infirmaryRequest({ client.options() }, { data ->
            options = data
            if (day == null) {
                day = data.today
                dayText = data.today.toString()
            }
        }, { optionsError = it })
    }
    LaunchedEffect(section, student, day, revision) {
        if (section != InfirmarySection.VISITS) return@LaunchedEffect
        val selectedDate = day ?: return@LaunchedEffect
        visits = null
        visitsError = null
        infirmaryRequest({ client.visits(student, selectedDate, page * VISIT_PAGE_SIZE, VISIT_PAGE_SIZE + 1) },
            { visits = it }, { visitsError = it })
    }
    val currentStudent = student
    LaunchedEffect(section, currentStudent, fileRevision) {
        if (section == InfirmarySection.FILES && currentStudent != null) {
            healthFile = null
            infirmaryRequest({ client.healthFile(currentStudent) }, { healthFile = Result.success(it) },
                { healthFile = Result.failure(IllegalStateException(it)) })
        }
    }

    val header: LazyListScope.() -> Unit = {
        item {
            TextButton(onClick = onBack) { Text("Geri") }
            Text("Revir ve sağlık", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Column {
                InfirmarySection.entries.forEach { item ->
                    FilterChip(section == item, onClick = { if (section != item) { section = item; feedback = null } },
                        label = { Text(item.label) })
                }
            }
        }
        item {
            Text(student?.name ?: "Tüm öğrenciler")
            OutlinedButton(enabled = options != null, onClick = { choosing = true }) { Text("Öğrenci seç") }
            if (student != null) TextButton(onClick = { student = null; feedback = null; reload(0) }) { Text("Öğrenci filtresini temizle") }
            if (optionsError != null) {
                Text(optionsError.orEmpty())
                OutlinedButton(onClick = { optionsError = null; optionsRevision++ }) { Text("Seçenekleri yeniden yükle") }
            }
        }
    }
    if (section == InfirmarySection.PROGRAMS || section == InfirmarySection.DOSES) {
        key(section, currentStudent) {
            InfirmaryMedicationSection(client, currentStudent, canWrite, section == InfirmarySection.DOSES, options?.today,
                day, feedback, header) { message, recordedDay ->
                day = recordedDay
                dayText = recordedDay.toString()
                feedback = message
                section = InfirmarySection.DOSES
            }
        }
    } else LazyColumn(Modifier.fillMaxSize().testTag("infirmary_visits"),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        header()
        if (section == InfirmarySection.VISITS) {
            item {
                val parsed = visitDate(dayText)
                OutlinedTextField(dayText, { dayText = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Ziyaret günü (YYYY-AA-GG)") }, isError = dayText.isNotBlank() && parsed == null)
                OutlinedButton(enabled = parsed != null && parsed != day, onClick = {
                    day = parsed
                    feedback = null
                    reload(0)
                }) { Text("Günü uygula") }
                if (canWrite && student != null && day != null && options != null) {
                    Button(onClick = { creating = true; feedback = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Revir ziyareti ekle") }
                }
                feedback?.let { Text(it) }
            }
            val rows = visits
            when {
                optionsError != null && day == null -> Unit
                visitsError != null -> item {
                    Text(visitsError.orEmpty())
                    OutlinedButton(onClick = { reload(page) }) { Text("Ziyaretleri yeniden yükle") }
                }
                rows == null -> item { Text("Revir ziyaretleri yükleniyor…") }
                rows.isEmpty() -> item { Text("Bu tarih ve kapsamda revir ziyareti bulunamadı.") }
                else -> items(rows.take(VISIT_PAGE_SIZE), key = { it.id }) { visit ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            visit.studentName?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                            Text("${visit.date} · ${visit.time.format(visitTimeFormat)}")
                            Text(visitSeverities[visit.severity] ?: visit.severity)
                            visit.complaint?.let { Text(it) }
                            if (visit.followUpRequired) Text("Takip gerekli")
                            TextButton(onClick = { details = visit }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ziyareti incele") }
                        }
                    }
                }
            }
            if (rows != null && (page > 0 || rows.size > VISIT_PAGE_SIZE)) item {
                Text("Sayfa ${page + 1}")
                OutlinedButton(enabled = page > 0, onClick = { reload(page - 1) }) { Text("Önceki") }
                OutlinedButton(enabled = rows.size > VISIT_PAGE_SIZE, onClick = { reload(page + 1) }) { Text("Sonraki") }
            }
        } else {
            healthFileRows(currentStudent, healthFile, canWrite, fileSaved, {
                healthFile = null
                fileRevision++
            }) {
                fileSaved = false
                editingFile = true
            }
        }
    }
    if (choosing) options?.let { initial ->
        InfirmaryStudentPicker(client, initial, { choosing = false }) { selected, clock ->
            student = selected
            options = initial.copy(today = clock.today, time = clock.time)
            feedback = null
            choosing = false
            reload(0)
        }
    }
    if (editingFile && section == InfirmarySection.FILES && currentStudent != null && healthFile?.isSuccess == true) {
        InfirmaryHealthFileEditor(client, currentStudent, healthFile?.getOrNull(), { editingFile = false }) {
            editingFile = false
            fileSaved = true
            healthFile = null
            fileRevision++
        }
    }
    val currentDate = day
    val clock = options?.time
    if (creating && canWrite && currentStudent != null && currentDate != null && clock != null) {
        InfirmaryVisitEditor(client, currentStudent, currentDate, clock, { creating = false }) { date ->
            creating = false
            day = date
            dayText = date.toString()
            feedback = "Revir ziyareti kaydedildi."
            reload(0)
        }
    }
    details?.let { visit ->
        InfirmaryVisitDetails(client, visit, canWrite, { details = null }) { matches ->
            feedback = if (matches) "Ziyaret takibi kaydedildi." else "Sunucudaki takip notu farklı. Kayıt yeniden yüklendi."
            reload(page)
        }
    }
}

private fun LazyListScope.healthFileRows(student: InfirmaryStudent?, result: Result<InfirmaryHealthFile?>?, canWrite: Boolean,
    saved: Boolean, onRetry: () -> Unit, onEdit: () -> Unit) {
    when {
        student == null -> item { Text("Sağlık dosyası için öğrenci seçin.") }
        result == null -> item { Text("Sağlık dosyası yükleniyor…") }
        result.isFailure -> item {
            Text(result.exceptionOrNull()?.message ?: "Sağlık dosyası yüklenemedi.")
            OutlinedButton(onClick = onRetry) { Text("Dosyayı yeniden yükle") }
        }
        else -> {
            val file = result.getOrNull()
            if (saved) item { Text("Sağlık dosyası kaydedildi.") }
            if (file == null) item { Text("Seçili öğrenci için sağlık dosyası bulunamadı.") }
            file?.values?.forEach { (field, value) -> if (value.isNotBlank()) item {
                Text(field.label, style = MaterialTheme.typography.labelLarge)
                Text(value)
            } }
            if (canWrite) item {
                Button(onClick = onEdit) { Text(if (file == null) "Sağlık dosyası oluştur" else "Sağlık dosyasını düzenle") }
            }
        }
    }
}

@Composable
private fun InfirmaryHealthFileEditor(client: NativeInfirmaryClient, student: InfirmaryStudent, file: InfirmaryHealthFile?,
    onClose: () -> Unit, onSaved: () -> Unit) {
    val initial = file?.values.orEmpty()
    var fields by remember { mutableStateOf(initial) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val changes = fields.mapValues { (_, value) -> value.trim() }.filter { (field, value) ->
        value != initial[field].orEmpty().trim()
    }
    val checkup = fields[HealthFileField.CHECKUP_DATE].orEmpty().trim()
    ConsentFormDialog("Sağlık dosyası", "infirmary_file", busy, onClose, {
        item { Text(student.name) }
        items(HealthFileField.entries, key = { it.key }) { field ->
            VisitField(fields[field].orEmpty(), { fields = fields + (field to it) }, field.label, busy, field.maxLength)
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }) {
        Button(enabled = !busy && changes.isNotEmpty() && (checkup.isEmpty() || visitDate(checkup) != null), onClick = {
            busy = true
            error = null
            scope.launch {
                infirmaryRequest({ client.saveHealthFile(student, file, changes) }, { onSaved() }, { error = it })
                busy = false
            }
        }) { Text("Sağlık dosyasını kaydet") }
    }
}

@Composable
private fun InfirmaryMedicationSection(client: NativeInfirmaryClient, student: InfirmaryStudent?, canWrite: Boolean,
    doseList: Boolean, today: LocalDate?, initialDay: LocalDate?, navigationFeedback: String?, header: LazyListScope.() -> Unit,
    onDoseRecorded: (String, LocalDate) -> Unit) {
    var programs by remember { mutableStateOf<List<InfirmaryMedication>?>(null) }
    var doses by remember { mutableStateOf<List<InfirmaryDose>?>(null) }
    var date by remember { mutableStateOf(initialDay ?: today) }
    var dateText by remember { mutableStateOf(date?.toString().orEmpty()) }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<InfirmaryMedication?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(navigationFeedback) }
    fun reload(nextPage: Int) {
        page = nextPage
        revision++
        error = null
        programs = null
        doses = null
        selected = null
    }
    LaunchedEffect(student, revision, date) {
        val child = student ?: return@LaunchedEffect
        val offset = page * VISIT_PAGE_SIZE
        if (doseList) {
            val day = date ?: return@LaunchedEffect
            infirmaryRequest({ client.doses(child, day, offset, VISIT_PAGE_SIZE + 1) }, { doses = it }, { error = it })
        } else {
            infirmaryRequest({ client.medications(child, offset, VISIT_PAGE_SIZE + 1) }, { programs = it }, { error = it })
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("infirmary_visits"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        header()
        if (student == null) {
            item { Text("İlaç kayıtlarını görüntülemek için öğrenci seçin.") }
            return@LazyColumn
        }
        item {
            if (doseList) {
                val parsed = visitDate(dateText)
                VisitField(dateText, { dateText = it }, "Doz günü (YYYY-AA-GG)", false, 10)
                OutlinedButton(enabled = parsed != null && date != parsed, onClick = {
                    date = parsed
                    feedback = null
                    reload(0)
                }) { Text("Doz gününü uygula") }
            } else if (canWrite && today != null) {
                Button(onClick = { feedback = null; creating = true }) { Text("İlaç programı ekle") }
            }
            feedback?.let { Text(it) }
        }
        val count = if (doseList) doses?.size else programs?.size
        when {
            error != null -> item {
                Text(error.orEmpty())
                OutlinedButton(onClick = { reload(page) }) { Text("Kayıtları yeniden yükle") }
            }
            count == null -> item { Text("İlaç kayıtları yükleniyor…") }
            count == 0 -> item { Text(if (doseList) "Bu gün için doz kaydı bulunamadı." else "Bu öğrenci için ilaç programı veya talebi bulunamadı.") }
            doseList -> items(doses.orEmpty().take(VISIT_PAGE_SIZE), key = { it.id }) { dose ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        dose.name?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                        dose.scheduledDate?.let { Text(it.toString()) }
                        Text(dose.scheduledTime.format(visitTimeFormat))
                        Text(doseStatuses[dose.status] ?: if (dose.status == "SCHEDULED") "Planlandı" else dose.status)
                        dose.givenTime?.let { Text("Uygulama zamanı · $it") }
                        dose.notes?.let { Text(it) }
                        dose.recorder?.let { Text("Kaydeden · $it") }
                    }
                }
            }
            else -> items(programs.orEmpty().take(VISIT_PAGE_SIZE), key = { it.id }) { program ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(program.name, style = MaterialTheme.typography.titleMedium)
                        Text(medicationStatuses[program.status] ?: program.status)
                        TextButton(onClick = { selected = program }, modifier = Modifier.heightIn(min = 48.dp)) { Text("İlaç kaydını incele") }
                    }
                }
            }
        }
        if (count != null && (page > 0 || count > VISIT_PAGE_SIZE)) item {
            Text("Sayfa ${page + 1}")
            OutlinedButton(enabled = page > 0, onClick = { reload(page - 1) }) { Text("Önceki") }
            OutlinedButton(enabled = count > VISIT_PAGE_SIZE, onClick = { reload(page + 1) }) { Text("Sonraki") }
        }
    }
    if (creating && canWrite && student != null && today != null) {
        InfirmaryMedicationEditor(client, student, today, { creating = false }) {
            creating = false
            feedback = "İlaç programı kaydedildi."
            reload(0)
        }
    }
    selected?.let { program ->
        MedicationDetails(client, program, canWrite, today, { selected = null }, { message ->
            feedback = message
            reload(page)
        }, onDoseRecorded)
    }
}

@Composable
private fun InfirmaryMedicationEditor(client: NativeInfirmaryClient, student: InfirmaryStudent, today: LocalDate,
    onClose: () -> Unit, onSaved: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var timing by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("NOT_SPECIFIED") }
    var startText by remember { mutableStateOf(today.toString()) }
    var endText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val start = visitDate(startText)
    val end = visitDate(endText)
    ConsentFormDialog("İlaç programı ekle", "infirmary_medication", busy, onClose, {
        item { Text(student.name) }
        item { VisitField(name, { name = it }, "Program ilaç adı", busy, 255) }
        item { VisitField(amount, { amount = it }, "Kayıtlı doz", busy, 100) }
        item { VisitField(timing, { timing = it }, "Saat / sıklık talimatı", busy, 100) }
        item { VisitField(startText, { startText = it }, "Başlangıç günü", busy, 10) }
        item { VisitField(endText, { endText = it }, "Bitiş günü", busy, 10) }
        item {
            Text("Öğün talimatı")
            mealInstructions.forEach { (value, label) ->
                FilterChip(value == instruction, onClick = { instruction = value }, enabled = !busy, label = { Text(label) })
            }
        }
        item { VisitField(note, { note = it }, "Program ek notu", busy, 1000) }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }) {
        Button(enabled = !busy && name.isNotBlank() && amount.isNotBlank() && timing.isNotBlank() &&
            start != null && (endText.isBlank() || end != null && end >= start), onClick = {
            busy = true
            error = null
            val draft = InfirmaryMedicationDraft(name.trim(), amount.trim(), timing.trim(), instruction,
                requireNotNull(start), end, note.trim())
            scope.launch {
                infirmaryRequest({ client.createMedication(student, draft) }, { onSaved() }, { error = it })
                busy = false
            }
        }) { Text("İlaç programını kaydet") }
    }
}

@Composable
private fun MedicationDetails(client: NativeInfirmaryClient, program: InfirmaryMedication, canWrite: Boolean, today: LocalDate?,
    onClose: () -> Unit, onDecision: (String) -> Unit, onDoseRecorded: (String, LocalDate) -> Unit) {
    var timeText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val time = runCatching { LocalTime.parse(timeText) }.getOrNull()?.takeIf { timeText.length == 5 }
    val canLog = canWrite && program.status == "APPROVED" && today != null && today >= program.start && (program.end == null || today <= program.end)
    val decisions = when (program.status) {
        "PENDING" -> listOf("APPROVED" to "Talebi onayla", "REJECTED" to "Talebi reddet")
        "APPROVED" -> listOf("COMPLETED" to "Programı kapat")
        else -> emptyList()
    }
    ConsentFormDialog("İlaç kaydı", "infirmary_medication_detail", busy, onClose, {
        item { Text(program.name, style = MaterialTheme.typography.titleMedium) }
        item { Text(medicationStatuses[program.status] ?: program.status) }
        item { Text("Kayıtlı doz · ${program.dosage}") }
        item { Text("Saat / sıklık · ${program.frequency}") }
        item { Text("Başlangıç · ${program.start}") }
        program.end?.let { item { Text("Bitiş · $it") } }
        program.instructions?.let { item { Text(it) } }
        program.reviewer?.let { item { Text("Karar veren · $it") } }
        if (canWrite) items(decisions, key = { it.first }) { (decision, label) ->
            OutlinedButton(enabled = !busy, onClick = {
                busy = true
                error = null
                scope.launch {
                    infirmaryRequest({ client.decideMedication(program, decision) }, { matches ->
                        onDecision(if (matches) "İlaç durumu kaydedildi." else "Sunucudaki mevcut ilaç durumu yeniden okundu.")
                    }, { error = it })
                    busy = false
                }
            }) { Text(label) }
        }
        if (canLog) {
            item { Text("Doz günü · $today") }
            item { VisitField(timeText, { timeText = it }, "Planlanan doz saati (SS:DD)", busy, 5) }
            item {
                Text("Doz sonucu")
                doseStatuses.forEach { (value, label) ->
                    FilterChip(value == status, onClick = { status = value }, enabled = !busy, label = { Text(label) })
                }
            }
            item { VisitField(notes, { notes = it }, "Doz kayıt notu", busy, 1000) }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }) {
        if (canLog && today != null) Button(enabled = !busy && time != null && status != null, onClick = {
            busy = true
            error = null
            scope.launch {
                infirmaryRequest({ client.recordDose(program, today, requireNotNull(time), requireNotNull(status), notes.trim()) }, { matches ->
                    onDoseRecorded(if (matches) "Doz kaydı kaydedildi." else "Sunucudaki mevcut doz kaydı yeniden okundu.", today)
                }, { error = it })
                busy = false
            }
        }) { Text("Doz sonucunu kaydet") }
    }
}

@Composable
private fun InfirmaryStudentPicker(client: NativeInfirmaryClient, initial: InfirmaryOptions, onClose: () -> Unit,
    onSelect: (InfirmaryStudent, InfirmaryOptions) -> Unit) {
    var search by remember { mutableStateOf("") }
    var retry by remember { mutableIntStateOf(0) }
    var options by remember { mutableStateOf<InfirmaryOptions?>(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(search, retry) {
        if (search.isNotBlank()) delay(250)
        if (options == null) infirmaryRequest({ client.options(search.trim()) }, { options = it }, { error = it })
    }
    ConsentFormDialog("Öğrenci seç", "infirmary_students", false, onClose, {
        item {
            OutlinedTextField(search, { search = it.take(100); options = null; error = null },
                label = { Text("Öğrenci ara") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        val result = options
        when {
            error != null -> item {
                Text(error.orEmpty())
                OutlinedButton(onClick = { error = null; retry++ }) { Text("Yeniden yükle") }
            }
            result == null -> item { Text("Öğrenciler yükleniyor…") }
            result.students.isEmpty() -> item { Text("Bu kapsamda öğrenci bulunamadı.") }
            else -> items(result.students, key = { it.id }) { student ->
                OutlinedButton(onClick = { onSelect(student, result) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(student.name) }
            }
        }
        if (result?.more == true) item { Text("Diğer öğrencileri adla arayın.") }
    }) {}
}

@Composable
private fun InfirmaryVisitEditor(client: NativeInfirmaryClient, student: InfirmaryStudent, initialDate: LocalDate, initialTime: LocalTime,
    onClose: () -> Unit, onSaved: (LocalDate) -> Unit) {
    var dateText by remember { mutableStateOf(initialDate.toString()) }
    var timeText by remember { mutableStateOf(initialTime.format(visitTimeFormat)) }
    var complaint by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("LOW") }
    var temperature by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var followUp by remember { mutableStateOf(false) }
    var followUpNotes by remember { mutableStateOf("") }
    var notification by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val date = visitDate(dateText)
    val time = runCatching { LocalTime.parse(timeText) }.getOrNull()?.takeIf { timeText.length == 5 }
    val degrees = temperature.trim().replace(',', '.').toDoubleOrNull()
    val validTemperature = temperature.isBlank() || (degrees != null && degrees in 35.0..42.0)
    ConsentFormDialog("Revir ziyareti ekle", "infirmary_create", busy, onClose, {
        item { Text(student.name) }
        item { VisitField(dateText, { dateText = it }, "Ziyaret tarihi", busy, 10) }
        item { VisitField(timeText, { timeText = it }, "Ziyaret saati", busy, 5) }
        item { VisitField(complaint, { complaint = it }, "Şikâyet", busy, 1000) }
        item {
            Column {
                Text("Öncelik")
                visitSeverities.forEach { (value, label) ->
                    FilterChip(severity == value, onClick = { severity = value }, enabled = !busy, label = { Text(label) })
                }
            }
        }
        item { VisitField(temperature, { temperature = it }, "Ateş (35–42 °C)", busy, 8) }
        item { VisitField(treatment, { treatment = it }, "Kaydedilen işlem", busy, 1000) }
        item { VisitCheckbox("Takip gerekli", followUp, { followUp = it }, busy) }
        item { VisitField(followUpNotes, { followUpNotes = it }, "Takip notu", busy, 1000) }
        item { VisitCheckbox("Veliye daha önce bilgi verildi", notification, { notification = it }, busy) }
        if (notification) item { VisitField(method, { method = it }, "Manuel bilgilendirme yöntemi", busy, 50) }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }) {
        Button(enabled = !busy && date != null && time != null && complaint.isNotBlank() && validTemperature && (!notification || method.isNotBlank()),
            onClick = {
                val draft = InfirmaryVisitDraft(student, requireNotNull(date), requireNotNull(time), severity, complaint.trim(),
                    degrees, treatment.trim(), followUp, followUpNotes.trim(), notification, method.trim())
                busy = true
                error = null
                scope.launch {
                    infirmaryRequest({ client.create(draft) }, { onSaved(draft.date) }, { error = it })
                    busy = false
                }
            }) { Text("Revir ziyaretini kaydet") }
    }
}

@Composable
private fun InfirmaryVisitDetails(client: NativeInfirmaryClient, visit: NativeInfirmaryVisit, canWrite: Boolean,
    onClose: () -> Unit, onCompleted: (Boolean) -> Unit) {
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val editable = visit.followUpRequired && canWrite
    ConsentFormDialog("Revir ziyareti", "infirmary_detail", busy, onClose, {
        visit.studentName?.let { item { Text(it) } }
        visit.complaint?.let { item { Text(it) } }
        visit.examiner?.let { item { Text("Muayene eden · $it") } }
        visit.temperature?.let { item { Text("Ateş · $it °C") } }
        visit.treatment?.let { item { Text("Kaydedilen işlem · $it") } }
        visit.followUpNotes?.let { item { Text("Takip notu · $it") } }
        if (visit.parentNotified) item { Text("Manuel veli bilgilendirmesi · ${visit.notificationMethod.orEmpty()}") }
        if (editable) item { VisitField(note, { note = it }, "Takip sonuç notu", busy, 1000) }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }) {
        if (editable) Button(enabled = !busy && note.isNotBlank(), onClick = {
            busy = true
            error = null
            scope.launch {
                infirmaryRequest({ client.complete(visit, note.trim()) }, onCompleted, { error = it })
                busy = false
            }
        }) { Text("Takibi tamamla") }
    }
}

@Composable
private fun VisitField(value: String, onChange: (String) -> Unit, label: String, busy: Boolean, max: Int) {
    OutlinedTextField(value, { onChange(it.take(max)) }, label = { Text(label) }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
        singleLine = max < 1000)
}

@Composable
private fun VisitCheckbox(label: String, checked: Boolean, onChange: (Boolean) -> Unit, busy: Boolean) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, enabled = !busy, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked, null, modifier = Modifier.padding(end = 12.dp), enabled = !busy)
        Text(label)
    }
}

private fun visitDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

private suspend fun <T> infirmaryRequest(request: suspend () -> T, onResult: (T) -> Unit, onError: (String) -> Unit) {
    val result = try {
        request()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        onError(failure.message ?: "Revir isteği tamamlanamadı.")
        return
    }
    onResult(result)
}
