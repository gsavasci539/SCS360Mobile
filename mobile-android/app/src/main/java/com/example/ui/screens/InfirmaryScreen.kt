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
    LaunchedEffect(student, day, revision) {
        val selectedDate = day ?: return@LaunchedEffect
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

    LazyColumn(Modifier.fillMaxSize().testTag("infirmary_visits"),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            TextButton(onClick = onBack) { Text("Geri") }
            Text("Revir ve sağlık", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Column {
                InfirmarySection.entries.forEach { item ->
                    FilterChip(section == item, onClick = { section = item }, label = { Text(item.label) })
                }
            }
        }
        if (section == InfirmarySection.VISITS || section == InfirmarySection.FILES) {
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
        } else if (section == InfirmarySection.FILES) {
            healthFileRows(currentStudent, healthFile, canWrite, fileSaved, {
                healthFile = null
                fileRevision++
            }) {
                fileSaved = false
                editingFile = true
            }
        } else item {
            Text("${section.label} için Web revir ekranını kullanın.")
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
