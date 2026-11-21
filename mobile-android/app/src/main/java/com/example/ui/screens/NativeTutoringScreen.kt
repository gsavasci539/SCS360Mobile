package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.remote.*
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private val days = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
private val attendance = mapOf("PRESENT" to "Katıldı", "ABSENT" to "Katılmadı", "LATE" to "Geç geldi")
private val statuses = mapOf("SCHEDULED" to "Planlandı", "COMPLETED" to "Tamamlandı", "CANCELLED" to "İptal edildi",
    "PENDING" to "Bekliyor", "APPROVED" to "Onaylandı", "REJECTED" to "Reddedildi")

@Composable
internal fun NativeTutoringScreen(state: Okul360UiState, onBack: () -> Unit) {
    val user = state.currentUser
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, "tutoring.read") ||
        user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) {
        Column(Modifier.padding(16.dp)) {
            Text("Etüt kayıtları için yetkili çalışma alanını açın.")
            TextButton(onClick = onBack) { Text("Geri") }
        }
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl) {
        TutoringWorkspace(remember { NativeTutoringClient(state.selectedBranchId) },
            NativeNavigationPolicy.has(user, "tutoring.write"), onBack)
    }
}

@Composable
private fun TutoringWorkspace(client: NativeTutoringClient, canWrite: Boolean, onBack: () -> Unit) {
    var kind by remember { mutableStateOf(TutoringRecords.SESSIONS) }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var student by remember { mutableStateOf<Pair<String, String>?>(null) }
    var teacher by remember { mutableStateOf<Pair<String, String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf<JSONObject?>(null) }
    var filter by remember { mutableStateOf<String?>(null) }
    val studentId = student?.first.orEmpty()
    val teacherId = teacher?.first.orEmpty()
    val list by tutoringLoad(kind, page, studentId, teacherId, revision) { client.list(kind, page, studentId, teacherId) }
    val summary by tutoringLoad(studentId, teacherId, revision) { client.summary(studentId, teacherId) }
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
                notice = "Etüt işlemi doğrulanamadı. ${failure.message.orEmpty()}"
            } finally { busy = false }
        }
    }
    val rows = list?.getOrNull().orEmpty()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Birebir Etüt", style = MaterialTheme.typography.headlineSmall)
            TextButton(enabled = !busy, onClick = onBack) { Text("Geri") }
            TextButton(enabled = !busy, onClick = { revision++ }) { Text("Etüt kayıtlarını yenile") }
            notice?.let { Text(it) }
            summary?.getOrNull()?.let {
                Text("${it.getInt("scheduled_sessions")} planlanmış ders · ${it.getInt("pending_requests")} bekleyen talep")
            }
            if (kind != TutoringRecords.AVAILABILITY) TextButton(enabled = !busy, onClick = { filter = "student" }) {
                Text(student?.second ?: "Tüm yetkili öğrenciler")
            }
            TextButton(enabled = !busy, onClick = { filter = "teacher" }) { Text(teacher?.second ?: "Tüm yetkili öğretmenler") }
        }
        items(TutoringRecords.entries) { tab ->
            TextButton(enabled = !busy && tab != kind, onClick = { kind = tab; page = 0; notice = null }) { Text(tab.label) }
        }
        if (canWrite) item {
            Button(enabled = !busy, onClick = { target = null; editing = true; notice = null }) { Text(when (kind) {
                TutoringRecords.SESSIONS -> "Etüt dersi planla"
                TutoringRecords.REQUESTS -> "Etüt talebi oluştur"
                TutoringRecords.AVAILABILITY -> "Müsaitlik ekle"
            }) }
        }
        if (rows.isEmpty()) item { Text(when {
            list == null -> "Etüt kayıtları yükleniyor…"
            list?.isFailure == true -> "Etüt kayıtları yüklenemedi."
            else -> "Bu kapsamda etüt kaydı yok."
        }) }
        items(rows.take(20), key = { it.getString("id") }) { row ->
            val status = row.tutoringText("status")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val fields = when (kind) {
                        TutoringRecords.SESSIONS -> listOf(row.tutoringText("student_name"), row.tutoringText("teacher_name"), row.tutoringText("subject"),
                            row.tutoringText("session_date"), timeRange(row, "start_time", "end_time"), row.tutoringText("location"),
                            attendance[row.tutoringText("attendance_status")].orEmpty(), row.tutoringText("progress_score").takeIf(String::isNotBlank)?.let { "Puan $it" }.orEmpty(),
                            row.tutoringText("notes"), statuses[status].orEmpty())
                        TutoringRecords.REQUESTS -> listOf(row.tutoringText("student_name"), row.tutoringText("teacher_name"),
                            row.tutoringText("subject"), row.tutoringText("requested_date"),
                            timeRange(row, "preferred_time_start", "preferred_time_end"), row.tutoringText("reason"),
                            statuses[status].orEmpty(), row.tutoringText("approved_by_name"), row.tutoringText("rejection_reason"))
                        TutoringRecords.AVAILABILITY -> listOf(row.tutoringText("teacher_name"), days.getOrNull(row.getInt("day_of_week")).orEmpty(),
                            timeRange(row, "start_time", "end_time"), "Kapasite ${row.getInt("max_students")}",
                            if (row.getBoolean("is_available")) "Etkin" else "Pasif")
                    }
                    fields.filter(String::isNotBlank).forEach { Text(it) }
                    if (canWrite) when (kind) {
                        TutoringRecords.SESSIONS -> {
                            if (status == "SCHEDULED") {
                                TextButton(enabled = !busy, onClick = { target = row; editing = true; notice = null }) { Text("Dersi düzenle") }
                                TextButton(enabled = !busy, onClick = { target = row; mode = "cancel"; notice = null }) { Text("Dersi iptal et") }
                            }
                            if (status != "CANCELLED") TextButton(enabled = !busy, onClick = { target = row; mode = "attendance"; notice = null }) { Text("Yoklama ve ilerleme") }
                        }
                        TutoringRecords.REQUESTS -> {
                            if (status == "PENDING") {
                                TextButton(enabled = !busy, onClick = { target = row; mode = "approve"; notice = null }) { Text("Onayla") }
                                TextButton(enabled = !busy, onClick = { target = row; mode = "reject"; notice = null }) { Text("Reddet") }
                            }
                            if (status == "APPROVED") TextButton(enabled = !busy, onClick = {
                                target = JSONObject(row.toString()).apply {
                                    remove("id")
                                    put("session_date", row.tutoringText("requested_date"))
                                    put("start_time", row.tutoringText("preferred_time_start"))
                                    put("end_time", row.tutoringText("preferred_time_end"))
                                    put("notes", row.tutoringText("reason"))
                                }
                                kind = TutoringRecords.SESSIONS
                                editing = true
                                notice = null
                            }) { Text("Bu talep için ders planla") }
                        }
                        TutoringRecords.AVAILABILITY -> if (row.getBoolean("is_available")) {
                            TextButton(enabled = !busy, onClick = { target = row; mode = "disable"; notice = null }) { Text("Müsaitliği kapat") }
                        }
                    }
                }
            }
        }
        if (page > 0 || rows.size > 20) item {
            if (page > 0) TextButton(enabled = !busy, onClick = { page-- }) { Text("Önceki sayfa") }
            if (rows.size > 20) TextButton(enabled = !busy, onClick = { page++ }) { Text("Sonraki sayfa") }
        }
    }
    filter?.let { which -> TutoringChoice(client, which, if (kind == TutoringRecords.AVAILABILITY) "" else studentId, teacherId, true,
        { filter = null }) { selection ->
        if (which == "student") { student = selection; teacher = null } else teacher = selection
        page = 0
        filter = null
    } }
    val existing = target
    if (canWrite && editing) TutoringEditor(client, kind, existing, student, teacher, busy, notice, { editing = false }) { fields, request ->
        write {
            client.save(kind, fields, request, existing?.tutoringText("id")?.takeIf(String::isNotBlank))
            editing = false
            page = 0
            "Etüt kaydı sunucudan doğrulandı."
        }
    }
    val action = mode
    if (canWrite && existing != null && action != null) TutoringDecision(client, action, existing, busy, notice, { mode = null }) { fields ->
        write { client.action(kind, existing, action, fields); mode = null; "Etüt işlemi sunucudan doğrulandı." }
    }
}

private fun timeRange(row: JSONObject, start: String, end: String) =
    listOf(row.tutoringText(start), row.tutoringText(end)).filter(String::isNotBlank).joinToString(" – ")

@Composable
private fun TutoringChoice(client: NativeTutoringClient, field: String, studentId: String, teacherId: String,
    canClear: Boolean, close: () -> Unit, select: (Pair<String, String>?) -> Unit) {
    var search by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    val forStudents = field == "student"
    val options by tutoringLoad(field, search, revision, studentId, teacherId) {
        if (search.isNotBlank()) delay(250)
        client.options(studentId, teacherId, search = if (forStudents) search.trim() else "",
            teacherSearch = if (forStudents) "" else search.trim())
    }
    val array = options?.getOrNull()?.getJSONArray(if (forStudents) "students" else "teachers") ?: JSONArray()
    val rows = (0 until array.length()).map(array::getJSONObject)
    ConsentFormDialog(if (forStudents) "Öğrenci seç" else "Öğretmen seç", "native_tutoring_options", false, close, fields = {
        item { TutoringField(search, "Ad veya soyad ara", 100, false) { search = it } }
        if (canClear) item { TextButton(onClick = { select(null) }) { Text("Seçimi kaldır") } }
        if (rows.isEmpty()) item {
            when {
                options == null -> Text("Seçenekler yükleniyor…")
                options?.isFailure == true -> TextButton(onClick = { revision++ }) { Text("Seçenekleri tekrar yükle") }
                else -> Text("Bu kapsamda kayıt bulunamadı.")
            }
        }
        items(rows, key = { it.getString("id") }) { row ->
            TextButton(onClick = { select(row.getString("id") to row.getString("name")) }) { Text(row.getString("name")) }
        }
        if (options?.getOrNull()?.optBoolean(if (forStudents) "more_students" else "more_teachers") == true) item {
            Text("Daha fazla sonuç için aramayı daraltın.")
        }
    }, confirm = {})
}

private fun existingChoice(row: JSONObject?, key: String): Pair<String, String>? {
    if (row == null) return null
    return row.tutoringText("${key}_id").takeIf(String::isNotBlank)?.let { it to row.tutoringText("${key}_name") }
}

@Composable
private fun TutoringEditor(client: NativeTutoringClient, kind: TutoringRecords, record: JSONObject?,
    selectedStudent: Pair<String, String>?, selectedTeacher: Pair<String, String>?, busy: Boolean, notice: String?,
    close: () -> Unit, save: (JSONObject, UUID) -> Unit) {
    val requestForm = kind == TutoringRecords.REQUESTS
    val availability = kind == TutoringRecords.AVAILABILITY
    var student by remember { mutableStateOf(existingChoice(record, "student") ?: selectedStudent) }
    var teacher by remember { mutableStateOf(existingChoice(record, "teacher") ?: selectedTeacher) }
    var picking by remember { mutableStateOf<String?>(null) }
    var subject by remember { mutableStateOf(record?.tutoringText("subject").orEmpty()) }
    var date by remember { mutableStateOf(record?.tutoringText("session_date").orEmpty()) }
    var day by remember { mutableIntStateOf(-1) }
    var start by remember { mutableStateOf(record?.tutoringText("start_time").orEmpty()) }
    var end by remember { mutableStateOf(record?.tutoringText("end_time").orEmpty()) }
    var location by remember { mutableStateOf(record?.tutoringText("location").orEmpty()) }
    var notes by remember { mutableStateOf(record?.tutoringText("notes").orEmpty()) }
    var capacity by remember { mutableStateOf("1") }
    val selected = picking
    if (selected != null) {
        TutoringChoice(client, selected, if (availability) "" else student?.first.orEmpty(), teacher?.first.orEmpty(), requestForm,
            { picking = null }) { option ->
            if (selected == "student") { student = option; teacher = null } else teacher = option
            picking = null
        }
        return
    }
    val dateValue = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
    val timeValid = if (requestForm && start.isBlank() && end.isBlank()) true else runCatching {
        start.length == 5 && end.length == 5 && LocalTime.parse(start).isBefore(LocalTime.parse(end))
    }.getOrDefault(false)
    val body = JSONObject().put("teacher_id", teacher?.first ?: JSONObject.NULL)
    fun putText(name: String, value: String) { body.put(name, value.trim().ifBlank { null } ?: JSONObject.NULL) }
    if (availability) {
        body.put("day_of_week", day).put("max_students", capacity.toIntOrNull() ?: 0)
    } else {
        body.put("student_id", student?.first ?: JSONObject.NULL)
        putText("subject", subject)
        putText(if (requestForm) "requested_date" else "session_date", date)
        putText(if (requestForm) "reason" else "notes", notes)
        if (!requestForm) putText("location", location)
    }
    putText(if (requestForm) "preferred_time_start" else "start_time", start)
    putText(if (requestForm) "preferred_time_end" else "end_time", end)
    val request = remember(body.toString()) { UUID.randomUUID() }
    ConsentFormDialog(if (record?.has("id") == true) "Etüt dersini düzenle" else "Yeni etüt kaydı", "native_tutoring_editor", busy, close, fields = {
        if (!availability) {
            item { TextButton(enabled = !busy, onClick = { picking = "student" }) { Text(student?.second ?: "Öğrenci seç") } }
            item { TutoringField(subject, "Ders veya konu", 100, busy) { subject = it } }
            item { TutoringField(date, "Tarih (YYYY-AA-GG)", 10, busy) { date = it } }
        }
        item { TextButton(enabled = !busy, onClick = { picking = "teacher" }) { Text(teacher?.second ?: "Öğretmen seç") } }
        if (availability) {
            items(days.size) { index -> TextButton(enabled = !busy, onClick = { day = index }) {
                Text(if (day == index) "✓ ${days[index]}" else days[index])
            } }
            item { TutoringField(capacity, "Kapasite", 3, busy) { capacity = it } }
        }
        item { TutoringField(start, "Başlangıç (SS:DD)", 5, busy) { start = it } }
        item { TutoringField(end, "Bitiş (SS:DD)", 5, busy) { end = it } }
        if (kind == TutoringRecords.SESSIONS) item { TutoringField(location, "Yer", 255, busy) { location = it } }
        if (!availability) item { TutoringField(notes, if (requestForm) "Gerekçe" else "Notlar", 4000, busy) { notes = it } }
        notice?.let { item { Text(it) } }
    }, confirm = {
        val ready = if (availability) day in 0..6 && (capacity.toIntOrNull() ?: 0) in 1..100
        else student != null && dateValue != null
        Button(enabled = !busy && ready && timeValid && (requestForm || teacher != null), onClick = { save(body, request) }) { Text("Kaydet") }
    })
}

@Composable
private fun TutoringDecision(client: NativeTutoringClient, action: String, record: JSONObject, busy: Boolean, notice: String?,
    close: () -> Unit, save: (JSONObject) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    var teacher by remember { mutableStateOf(existingChoice(record, "teacher")) }
    var status by remember { mutableStateOf(record.tutoringText("attendance_status")) }
    var score by remember { mutableStateOf(record.tutoringText("progress_score")) }
    var notes by remember { mutableStateOf(if (action == "attendance") record.tutoringText("notes") else "") }
    if (choosing) {
        TutoringChoice(client, "teacher", record.tutoringText("student_id"), teacher?.first.orEmpty(), false, { choosing = false }) {
            teacher = it; choosing = false
        }
        return
    }
    val body = JSONObject()
    when (action) {
        "attendance" -> body.put("attendance_status", status)
            .put("progress_score", score.toIntOrNull() ?: JSONObject.NULL)
            .put("notes", notes.trim().ifBlank { null } ?: JSONObject.NULL)
        "approve" -> body.put("teacher_id", teacher?.first ?: JSONObject.NULL)
        "reject" -> body.put("rejection_reason", notes.trim())
    }
    val title = when (action) {
        "approve" -> "Talebi onayla"
        "reject" -> "Talebi reddet"
        "attendance" -> "Yoklama ve ilerleme"
        "cancel" -> "Dersi iptal et"
        else -> "Müsaitliği kapat"
    }
    ConsentFormDialog(title, "native_tutoring_decision", busy, close, fields = {
        item { Text(record.tutoringText("student_name").ifBlank { record.tutoringText("teacher_name") }) }
        if (action == "approve") item {
            TextButton(enabled = !busy, onClick = { choosing = true }) { Text(teacher?.second ?: "Öğretmen seç") }
        }
        if (action == "attendance") {
            items(attendance.entries.toList(), key = { it.key }) { (code, label) ->
                TextButton(enabled = !busy, onClick = { status = code }) { Text(if (status == code) "✓ $label" else label) }
            }
            item { TutoringField(score, "İlerleme puanı (0–100)", 3, busy) { score = it } }
        }
        if (action in setOf("attendance", "reject")) item {
            TutoringField(notes, if (action == "reject") "Ret gerekçesi" else "Notlar", if (action == "reject") 2000 else 4000, busy) { notes = it }
        }
        notice?.let { item { Text(it) } }
    }, confirm = {
        val valid = when (action) {
            "approve" -> teacher != null
            "reject" -> notes.isNotBlank()
            "attendance" -> status in attendance && (score.isBlank() || (score.toIntOrNull() ?: -1) in 0..100)
            else -> true
        }
        Button(enabled = valid && !busy, onClick = { save(body) }) { Text(title) }
    })
}

@Composable
private fun TutoringField(value: String, label: String, limit: Int, busy: Boolean, change: (String) -> Unit) {
    OutlinedTextField(value, { change(it.take(limit)) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text(label) })
}

@Composable
private fun <T> tutoringLoad(vararg keys: Any?, load: suspend () -> T): State<Result<T>?> = key(keys.toList()) {
    produceState<Result<T>?>(null) {
        value = try { Result.success(load()) } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            Result.failure(failure)
        }
    }
}
