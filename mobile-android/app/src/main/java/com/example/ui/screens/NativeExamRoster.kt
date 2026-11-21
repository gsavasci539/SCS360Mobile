package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.ExamGradeDraft
import com.example.data.remote.ExamsPage
import com.example.data.remote.NativeExamsClient
import com.example.data.remote.examText
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun NativeExamRoster(client: NativeExamsClient, exam: JSONObject, canWrite: Boolean, onChanged: () -> Unit, onBack: () -> Unit) {
    val examId = exam.getString("id")
    val maximum = exam.examText("max_score")?.toBigDecimalOrNull()
    val editable = canWrite && exam.getString("status") != "CANCELLED" && maximum?.signum() == 1
    var page by remember { mutableIntStateOf(1) }
    var search by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var drafts by remember { mutableStateOf<Map<String, ExamGradeDraft>>(emptyMap()) }
    var checking by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var student by remember { mutableStateOf<JSONObject?>(null) }
    val coroutine = rememberCoroutineScope()
    val roster by key(page, search, revision) { produceState<Result<ExamsPage<JSONObject>>?>(null) {
        value = examResult { if (search.isNotBlank()) delay(250); client.roster(examId, page, search) }
    } }
    val rows = roster?.getOrNull()?.rows.orEmpty()
    val valid = maximum != null && drafts.values.all { draft ->
        val score = examDecimal(draft.score)
        draft.score.isBlank() || score != null && score.signum() >= 0 && score <= maximum
    }

    fun goBack() {
        if (busy) return
        if (drafts.isEmpty()) onBack() else checking = true
    }
    BackHandler(enabled = student == null) { goBack() }

    LazyColumn(Modifier.fillMaxSize().testTag("exam_roster"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(exam.getString("title"), style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = ::goBack, enabled = !busy) { Text("Sınavlara dön") }
            Text("Tam puan ${maximum?.toPlainString() ?: "—"}")
            if (exam.getString("status") == "CANCELLED") Text("Sınav iptal edildi. Önceki notlar saklanıyor.")
            if (maximum?.signum() != 1) Text("Tam puan kaydı incelenmeli.")
            message?.let { Text(it) }
            ExamField(search, { search = it; page = 1 }, "Öğrenci ara", 200, !busy)
            TextButton(onClick = { revision++ }, enabled = !busy) { Text("Öğrencileri yenile") }
            if (editable) {
                if (!valid) Text("Notlar sıfır ile tam puan arasında ve en fazla iki ondalık basamaklı olmalı.")
                Button(enabled = !busy && drafts.size in 1..1000 && valid, onClick = {
                    val sent = drafts.mapValues { (_, draft) -> draft.copy(score = examDecimal(draft.score)?.toPlainString().orEmpty()) }
                    busy = true
                    message = null
                    coroutine.launch {
                        try {
                            client.grades(examId, sent)
                            drafts = emptyMap()
                            revision++
                            onChanged()
                            message = "Notlar sunucudan doğrulandı."
                        } catch (failure: Exception) {
                            if (failure is CancellationException) throw failure
                            message = "Notlar doğrulanamadı. Taslaklar korunuyor."
                        } finally { busy = false }
                    }
                }) { Text("Notları kaydet (${drafts.size})") }
            }
            when {
                roster == null -> Text("Öğrenciler yükleniyor…")
                roster?.isFailure == true -> Text("Sınav öğrencileri alınamadı.")
                rows.isEmpty() -> Text("Bu kapsamda öğrenci bulunamadı.")
            }
        }
        items(rows, key = { it.getString("id") }) { row ->
            val id = row.getString("id")
            val draft = drafts[id] ?: ExamGradeDraft(row.examText("score").orEmpty(), row.examText("grade_scale").orEmpty(),
                row.examText("notes").orEmpty())
            val duplicate = row.getBoolean("duplicate_results")
            val change: (ExamGradeDraft) -> Unit = { drafts = drafts + (id to it); message = null }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.getString("name"), style = MaterialTheme.typography.titleMedium)
                    if (row.getBoolean("historical")) Text("Önceki sınıf kaydı")
                    if (duplicate) Text("Yinelenen eski notlar incelenmeli.")
                    if (row.getBoolean("invalid_numeric_score")) Text("Eski not sayısal olarak okunamadı.")
                    if (editable && !duplicate) {
                        ExamField(draft.score, { change(draft.copy(score = it)) }, "Not", 8, !busy)
                        ExamField(draft.scale, { change(draft.copy(scale = it)) }, "Harf notu", 10, !busy)
                        ExamField(draft.notes, { change(draft.copy(notes = it)) }, "Açıklama", 1000, !busy)
                    } else {
                        Text("Not ${draft.score.ifBlank { "—" }}")
                        if (draft.scale.isNotBlank()) Text(draft.scale)
                        if (draft.notes.isNotBlank()) Text(draft.notes)
                    }
                    TextButton(onClick = { student = row }, enabled = !busy) { Text("Karneler") }
                }
            }
        }
        if (page > 1 || roster?.getOrNull()?.more == true) item {
            if (page > 1) TextButton(onClick = { page-- }, enabled = !busy) { Text("Önceki öğrenciler") }
            if (roster?.getOrNull()?.more == true) TextButton(onClick = { page++ }, enabled = !busy) { Text("Sonraki öğrenciler") }
        }
    }
    if (checking) AlertDialog(onDismissRequest = { checking = false }, title = { Text("Taslak notlar var") },
        text = { Text("Kaydedilmemiş not girişleri kapatılsın mı?") },
        confirmButton = { Button(onClick = onBack) { Text("Çık") } },
        dismissButton = { TextButton(onClick = { checking = false }) { Text("Düzenlemeye dön") } })
    student?.let { row ->
        key(row.getString("id")) { ExamCardsDialog(client, row, canWrite, onClose = { student = null }) }
    }
}

private val terms = mapOf("FALL" to "Güz", "SPRING" to "Bahar", "SUMMER" to "Yaz")

@Composable
private fun ExamCardsDialog(client: NativeExamsClient, student: JSONObject, canWrite: Boolean, onClose: () -> Unit) {
    val id = student.getString("id")
    val today = remember { LocalDate.now(ZoneId.of("Europe/Istanbul")) }
    var year by remember { mutableStateOf((if (today.monthValue < 9) today.year - 1 else today.year).let { "$it-${it + 1}" }) }
    var term by remember { mutableStateOf(when (today.monthValue) { in 2..6 -> "SPRING"; in 7..8 -> "SUMMER"; else -> "FALL" }) }
    var teacher by remember { mutableStateOf("") }
    var management by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val coroutine = rememberCoroutineScope()
    val result by key(revision) { produceState<Result<List<JSONObject>>?>(null) { value = examResult { client.cards(id) } } }
    val row = year.trim().split('-')
    val first = row.getOrNull(0)?.toIntOrNull()
    val verifiedYear = year.matches(Regex("[0-9]{4}-[0-9]{4}")) && first != null && first in 1..9998 &&
        row.getOrNull(1)?.toIntOrNull() == first + 1

    fun write(action: suspend () -> String) {
        if (busy) return
        busy = true
        message = null
        coroutine.launch {
            try {
                message = action()
                revision++
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                message = "Karne işlemi doğrulanamadı."
            } finally { busy = false }
        }
    }

    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text("${student.getString("name")} · Karneler") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item {
                    message?.let { Text(it) }
                    TextButton(onClick = { revision++ }, enabled = !busy) { Text("Karneleri yenile") }
                    when {
                        result == null -> Text("Karneler yükleniyor…")
                        result?.isFailure == true -> Text("Karneler alınamadı.")
                        result?.getOrNull()?.isEmpty() == true -> Text("Öğrencinin kayıtlı karnesi yok.")
                    }
                }
                items(result?.getOrNull().orEmpty(), key = { it.getString("id") }) { card ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${card.getString("academic_year")} · ${terms[card.getString("semester")]}", style = MaterialTheme.typography.titleSmall)
                            Text("Ortalama ${card.examText("gpa")?.let { "$it / 4" } ?: "—"}")
                            Text("Sınıf sırası ${card.examText("class_rank") ?: "—"}")
                            Text("Devam ${card.examText("attendance_rate")?.let { "%$it" } ?: "—"}")
                            card.examText("teacher_comments")?.let { Text("Öğretmen · $it") }
                            card.examText("principal_comments")?.let { Text("Yönetim · $it") }
                            val published = card.getBoolean("is_published")
                            Text(if (published) "Veliye yayımlandı" else "Taslak")
                            if (canWrite && !published) TextButton(enabled = !busy, onClick = {
                                write { client.publish(id, card.getString("id")); "Karne yayını doğrulandı." }
                            }) { Text("Veliye yayımla") }
                        }
                    }
                }
                if (canWrite) item {
                    ExamField(year, { year = it }, "Akademik yıl", 9, !busy)
                    terms.forEach { (code, label) ->
                        FilterChip(selected = term == code, onClick = { term = code }, enabled = !busy, label = { Text(label) })
                    }
                    ExamField(teacher, { teacher = it }, "Öğretmen yorumu", 2000, !busy)
                    ExamField(management, { management = it }, "Yönetim yorumu", 2000, !busy)
                    Button(enabled = !busy && verifiedYear, onClick = {
                        val draft = JSONObject().put("academic_year", year).put("semester", term)
                            .put("teacher_comments", teacher.trim().ifBlank { null } ?: JSONObject.NULL)
                            .put("principal_comments", management.trim().ifBlank { null } ?: JSONObject.NULL)
                        write { client.generate(id, draft); "Karne taslağı doğrulandı." }
                    }) { Text("Karne taslağı oluştur") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose, enabled = !busy) { Text("Kapat") } })
}
