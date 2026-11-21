package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.ExamClassChoice
import com.example.data.remote.ExamsPage
import com.example.data.remote.NativeExamsClient
import com.example.data.remote.RegistryHttpException
import com.example.data.remote.examText
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

private val examTypes = mapOf("QUIZ" to "Kısa sınav", "MIDTERM" to "Ara sınav", "FINAL" to "Dönem sonu",
    "MAKEUP" to "Telafi", "PROJECT" to "Proje", "OTHER" to "Diğer")
private val examStatuses = mapOf("" to "Tümü", "SCHEDULED" to "Planlandı", "COMPLETED" to "Tamamlandı", "CANCELLED" to "İptal")

@Composable
fun ExamsScreen(state: Okul360UiState, onBack: () -> Unit) {
    val user = state.currentUser
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, "exam.read", "exam.write") ||
        user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) {
        Column(Modifier.padding(16.dp)) {
            Text("Sınavlar için yetkili kurum çalışma alanını açın.")
            TextButton(onClick = onBack) { Text("Geri") }
        }
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl) {
        ExamsWorkspace(state, remember { NativeExamsClient(state.selectedBranchId) }, NativeNavigationPolicy.has(user, "exam.write"), onBack)
    }
}

@Composable
private fun ExamsWorkspace(state: Okul360UiState, client: NativeExamsClient, canWrite: Boolean, onBack: () -> Unit) {
    var page by remember { mutableIntStateOf(1) }
    var search by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var showStatus by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<JSONObject?>(null) }
    var cancelled by remember { mutableStateOf<JSONObject?>(null) }
    var grading by remember { mutableStateOf<JSONObject?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val coroutine = rememberCoroutineScope()
    val data by key(page, search, status, revision) { produceState<Result<ExamsPage<JSONObject>>?>(null) {
        value = examResult {
            if (search.isNotBlank()) delay(250)
            client.list(page, search, status)
        }
    } }

    fun write(operation: suspend () -> String) {
        if (busy) return
        busy = true
        notice = null
        error = null
        coroutine.launch {
            try {
                notice = operation()
                revision++
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                error = if (failure is RegistryHttpException && failure.code == 409)
                    "Sınav değişti veya bu işlem artık geçerli değil. Güncel kaydı yeniden açın."
                else "Sınav kaydı doğrulanamadı."
            } finally { busy = false }
        }
    }

    val exam = grading
    if (exam != null) {
        key(exam.getString("id")) {
            NativeExamRoster(client, exam, canWrite, onChanged = { revision++ }, onBack = { grading = null })
        }
        return
    }

    val rows = data?.getOrNull()?.rows.orEmpty()
    LazyColumn(Modifier.fillMaxSize().testTag("native_exams_list"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Sınavlar ve karne", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack, enabled = !busy) { Text("Geri") }
            notice?.let { Text(it) }
            if (canWrite) Button(onClick = { editing = true; target = null; error = null; notice = null }, enabled = !busy) {
                Text("Yeni sınav")
            }
            ExamField(search, { search = it; page = 1 }, "Sınav ara", 200, !busy)
            TextButton(onClick = { showStatus = true }, enabled = !busy) { Text("Sınav durumu · ${examStatuses[status]}") }
            TextButton(onClick = { revision++ }, enabled = !busy) { Text("Listeyi yenile") }
            RoleContextualAiCard(screenName = "EXAMS", state = state)
            when {
                data == null -> Text("Sınavlar yükleniyor…")
                data?.isFailure == true -> Text("Sınav listesi alınamadı.")
                rows.isEmpty() -> Text("Bu kapsamda sınav bulunamadı.")
            }
        }
        items(rows, key = { it.getString("id") }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(row.getString("title"), style = MaterialTheme.typography.titleMedium)
                    row.examText("class_name")?.let { Text(it) }
                    Text(listOfNotNull(row.examText("exam_date"), row.examText("start_time")?.take(5)).joinToString(" · "))
                    Text(examStatuses[row.getString("status")] ?: row.getString("status"))
                    row.examText("max_score")?.let { Text("Tam puan $it") }
                    TextButton(onClick = { grading = row }, enabled = !busy) { Text("Notlar ve karneler") }
                    if (canWrite && row.getString("status") != "CANCELLED") {
                        TextButton(onClick = { editing = true; target = row; error = null }, enabled = !busy) { Text("Sınavı düzenle") }
                        TextButton(onClick = { cancelled = row; error = null }, enabled = !busy) { Text("Sınavı iptal et") }
                    }
                }
            }
        }
        if (page > 1 || data?.getOrNull()?.more == true) item {
            if (page > 1) TextButton(onClick = { page-- }, enabled = !busy) { Text("Önceki sayfa") }
            if (data?.getOrNull()?.more == true) TextButton(onClick = { page++ }, enabled = !busy) { Text("Sonraki sayfa") }
        }
    }

    if (showStatus) AlertDialog(onDismissRequest = { showStatus = false }, title = { Text("Sınav durumu") }, text = {
        Column {
            examStatuses.forEach { (code, label) ->
                TextButton(onClick = { status = code; page = 1; showStatus = false }) { Text(label) }
            }
        }
    }, confirmButton = { TextButton(onClick = { showStatus = false }) { Text("Kapat") } })

    if (editing) key(target?.getString("id")) {
        NativeExamEditor(client, target, busy, error, onClose = { editing = false }) { room, requestId, form ->
            write {
                client.save(target, room, requestId, form)
                editing = false
                "Sınav kaydı doğrulandı."
            }
        }
    }
    cancelled?.let { row ->
        AlertDialog(onDismissRequest = { if (!busy) cancelled = null }, title = { Text("Sınavı iptal et") },
            text = { Column { Text(row.getString("title")); Text("Not geçmişi korunur."); error?.let { Text(it) } } },
            confirmButton = { Button(onClick = { write { client.cancel(row.getString("id")); cancelled = null; "Sınav iptali doğrulandı." } },
                enabled = !busy) { Text("İptali onayla") } },
            dismissButton = { TextButton(onClick = { cancelled = null }, enabled = !busy) { Text("Vazgeç") } })
    }
}

@Composable
private fun NativeExamEditor(
    client: NativeExamsClient, current: JSONObject?, busy: Boolean, error: String?, onClose: () -> Unit,
    onSave: (ExamClassChoice, UUID, JSONObject) -> Unit
) {
    var title by remember { mutableStateOf(current?.examText("title").orEmpty()) }
    var date by remember { mutableStateOf(current?.examText("exam_date").orEmpty()) }
    var start by remember { mutableStateOf(current?.examText("start_time")?.take(5).orEmpty()) }
    var duration by remember { mutableStateOf(current?.examText("duration_minutes") ?: "60") }
    var maximum by remember { mutableStateOf(if (current == null) "100" else current.examText("max_score").orEmpty()) }
    var weight by remember { mutableStateOf(if (current == null) "1" else current.examText("weight").orEmpty()) }
    var description by remember { mutableStateOf(current?.examText("description").orEmpty()) }
    var type by remember { mutableStateOf(current?.examText("exam_type") ?: "QUIZ") }
    var room by remember { mutableStateOf(current?.let {
        val branch = it.examText("branch_id") ?: return@let null
        ExamClassChoice(it.getString("class_id"), it.examText("class_name") ?: "Kayıtlı sınıf", branch)
    }) }
    var chooseRoom by remember { mutableStateOf(false) }
    var chooseType by remember { mutableStateOf(false) }
    val attempts = remember { mutableMapOf<String, UUID>() }
    val locked = (current?.optInt("result_count") ?: 0) > 0
    val day = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
    val time = runCatching { LocalTime.parse(start.trim()) }.getOrNull()
    val minutes = duration.trim().toIntOrNull()
    val max = examDecimal(maximum)
    val ratio = examDecimal(weight)
    val valid = title.isNotBlank() && room != null && day != null && (start.isBlank() || time != null) &&
        minutes != null && minutes in 15..300 && max != null && max > BigDecimal.ZERO && max <= BigDecimal("999.99") &&
        ratio != null && ratio in BigDecimal.ZERO..BigDecimal("2")

    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text(if (current == null) "Yeni sınav" else "Sınavı düzenle") },
        text = { Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ExamField(title, { title = it }, "Sınav adı", 255, !busy)
            TextButton(onClick = { chooseRoom = true }, enabled = !busy && !locked) { Text(room?.name ?: "Sınıf seçin") }
            TextButton(onClick = { chooseType = true }, enabled = !busy) { Text(examTypes[type] ?: type) }
            ExamField(date, { date = it }, "Tarih (YYYY-AA-GG)", 10, !busy)
            ExamField(start, { start = it }, "Saat (SS:DD)", 5, !busy)
            ExamField(duration, { duration = it }, "Süre (dakika)", 3, !busy)
            ExamField(maximum, { maximum = it }, "Tam puan", 8, !busy && !locked)
            ExamField(weight, { weight = it }, "Ağırlık", 8, !busy)
            ExamField(description, { description = it }, "Açıklama", 1000, !busy)
            if (locked) Text("Not girilmiş sınavın sınıfı ve tam puanı korunur.")
            error?.let { Text(it) }
        } },
        confirmButton = { Button(enabled = valid && !busy, onClick = {
            val classroom = requireNotNull(room)
            val form = JSONObject().put("class_id", classroom.id).put("title", title.trim()).put("exam_type", type)
                .put("exam_date", requireNotNull(day).toString()).put("start_time", time?.toString() ?: JSONObject.NULL)
                .put("duration_minutes", requireNotNull(minutes)).put("max_score", requireNotNull(max).toPlainString())
                .put("weight", requireNotNull(ratio).toPlainString()).put("description", description.trim().ifBlank { null } ?: JSONObject.NULL)
            current?.examText("teacher_id")?.let { form.put("teacher_id", it) }
            current?.examText("subject_id")?.let { form.put("subject_id", it) }
            onSave(classroom, attempts.getOrPut(form.toString(), UUID::randomUUID), form)
        }) { Text("Sınavı kaydet") } },
        dismissButton = { TextButton(onClick = onClose, enabled = !busy) { Text("Vazgeç") } })

    if (chooseRoom) ExamClassPicker(client, current?.examText("id"), onClose = { chooseRoom = false }) {
        room = it
        chooseRoom = false
    }
    if (chooseType) AlertDialog(onDismissRequest = { chooseType = false }, title = { Text("Sınav türü") },
        text = { Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
            examTypes.forEach { (code, name) -> TextButton(onClick = { type = code; chooseType = false }) { Text(name) } }
        } }, confirmButton = { TextButton(onClick = { chooseType = false }) { Text("Kapat") } })
}

@Composable
private fun ExamClassPicker(client: NativeExamsClient, examId: String?, onClose: () -> Unit, onChoose: (ExamClassChoice) -> Unit) {
    var source by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var revision by remember { mutableIntStateOf(0) }
    val data by key(source, page, revision) { produceState<Result<ExamsPage<ExamClassChoice>>?>(null) {
        value = examResult { if (source.isNotBlank()) delay(250); client.classes(page, source, examId) }
    } }
    val rows = data?.getOrNull()?.rows.orEmpty()
    AlertDialog(onDismissRequest = onClose, title = { Text("Sınıf seçin") }, text = {
        Column {
            ExamField(source, { source = it; page = 1 }, "Sınıf ara", 200, true)
            if (data == null) Text("Sınıflar yükleniyor…")
            else if (data?.isFailure == true) TextButton(onClick = { revision++ }) { Text("Sınıfları yeniden yükle") }
            else if (rows.isEmpty()) Text("Yetkili sınıf bulunamadı.")
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                items(rows, key = { it.id }) { item -> TextButton(onClick = { onChoose(item) }) { Text(item.name) } }
            }
            if (page > 1) TextButton(onClick = { page-- }) { Text("Önceki sayfa") }
            if (data?.getOrNull()?.more == true) TextButton(onClick = { page++ }) { Text("Sonraki sayfa") }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Kapat") } })
}

@Composable
internal fun ExamField(value: String, onChange: (String) -> Unit, label: String, maxLength: Int, enabled: Boolean) {
    OutlinedTextField(value, { onChange(it.take(maxLength)) }, Modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(label) }, singleLine = maxLength < 1000)
}

internal fun examDecimal(value: String): BigDecimal? = value.trim().replace(',', '.').toBigDecimalOrNull()
    ?.takeIf { it.scale() in 0..2 }

internal suspend fun <T> examResult(load: suspend () -> T): Result<T> = try {
    Result.success(load())
} catch (failure: Exception) {
    if (failure is CancellationException) throw failure
    Result.failure(failure)
}
