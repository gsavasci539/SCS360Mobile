package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.*
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

/** Render only records returned for this child; absence is never a synthetic score or health fact. */
@Composable
fun ParentVerifiedRecordsPane(studentId: String, section: String, modifier: Modifier = Modifier) {
    val captured = remember(studentId, section) { ApiClientProvider.interceptor.captureScope() }
    var dateInput by remember(studentId, section) { mutableStateOf("") }
    val date = parseDailyReportDate(dateInput)
    val validDate = dateInput.isBlank() || date != null
    var revision by remember(studentId, section) { mutableIntStateOf(0) }
    var value by remember(studentId, section, date, validDate, revision) { mutableStateOf<Any?>(null) }
    var busy by remember(studentId, section, date, validDate, revision) { mutableStateOf(validDate) }
    var error by remember(studentId, section, date, validDate, revision) { mutableStateOf<String?>(null) }
    LaunchedEffect(studentId, section, date, validDate, revision) {
        if (!validDate) return@LaunchedEffect
        try { val loaded = captured.io { NativeParentPortalClient().section(studentId, section, date) }; captured.ensureCurrent(); value = loaded }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (captured.current()) error = "Bu öğrencinin kayıtları yüklenemedi. Yetki ve bağlantınızı kontrol edin." }
        finally { if (captured.current()) busy = false }
    }
    if (!captured.current()) return
    val data = value
    val rows: List<JSONObject> = when (data) {
        is JSONArray -> (0 until data.length()).map { data.getJSONObject(it) }
        is JSONObject -> (data.optJSONArray("events") ?: data.optJSONArray("medicine_requests") ?: data.optJSONArray("excuse_requests"))?.let { array -> (0 until array.length()).map { array.getJSONObject(it) } } ?: listOf(data)
        else -> emptyList()
    }
    LazyColumn(modifier.fillMaxSize().testTag("parent_verified_$section"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (section == "timeline") item {
            OutlinedTextField(value = dateInput, onValueChange = { dateInput = it.take(10) },
                modifier = Modifier.fillMaxWidth().testTag("parent_timeline_date"), label = { Text("Akış tarihi") },
                placeholder = { Text("YYYY-AA-GG") }, isError = !validDate, singleLine = true,
                supportingText = { Text(if (validDate) "Boşsa okulun bugünkü tarihi kullanılır." else "Geçerli bir tarih girin.") })
        }
        item { TextButton(enabled = !busy && validDate, onClick = { revision++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Kayıtları yenile") } }
        item { if (busy) Text("Kayıtlar yükleniyor…"); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (validDate && !busy && error == null && rows.isEmpty()) item { Text("Bu bölümde kayıt bulunamadı.") }
        items(rows) { row ->
            OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (section == "today-status") {
                    Text(row.text("date"), style = MaterialTheme.typography.titleMedium)
                    row.optJSONObject("attendance")?.let { Text("Yoklama: ${it.text("status")}"); it.optional("check_in_time")?.let { time -> Text("Giriş: $time") } } ?: Text("Bugün için yoklama kaydı yok.")
                    row.optJSONObject("daily_report")?.let { report ->
                        for ((key, label) in listOf("meal_status" to "Yemek", "sleep_status" to "Uyku", "mood" to "Duygu durumu", "teacher_notes" to "Öğretmen notu")) report.optional(key)?.let { Text("$label: $it") }
                        report.activitiesText()?.let { Text("Etkinlikler: $it") }
                    } ?: Text("Bugün için günlük rapor kaydı yok.")
                    val lessons = row.optJSONArray("today_schedule")
                    if (lessons == null || lessons.length() == 0) Text("Bugün için ders programı kaydı yok.")
                    else repeat(lessons.length()) { Text(lesson(lessons.getJSONObject(it))) }
                } else if (section == "grades") {
                    Text(row.text("subject"), style = MaterialTheme.typography.titleMedium)
                    Text(row.text("exam_date"))
                    if (row.optBoolean("valid_numeric_score")) Text("Puan: ${row.text("score")} / ${row.text("max_score")}")
                    else Text("Sayısal puan doğrulanmadı.")
                    row.optional("status")?.let { Text(it) }
                } else if (section == "schedule") {
                    Text(row.text("day_of_week"), style = MaterialTheme.typography.titleMedium)
                    row.optJSONArray("lessons")?.let { lessons -> repeat(lessons.length()) { Text(lesson(lessons.getJSONObject(it))) } }
                } else {
                    for ((key, label) in listOf("title" to "", "subject" to "Ders", "teacher_name" to "Öğretmen", "date" to "Tarih", "time" to "Saat", "status" to "Durum", "description" to "Açıklama", "content" to "İçerik", "message" to "Mesaj", "due_date" to "Son tarih", "file_name" to "Dosya", "notes" to "Not", "medicine_name" to "İlaç", "dosage" to "Doz", "time_to_give" to "İstenen saat", "instructions" to "Açıklama", "start_date" to "Başlangıç", "end_date" to "Bitiş", "reason_type" to "Mazeret türü")) row.optional(key)?.let { Text(if (label.isEmpty()) it else "$label: $it") }
                    row.optJSONObject("details")?.let { details ->
                        for ((key, label) in listOf("meal_status" to "Yemek", "sleep_status" to "Uyku", "mood" to "Duygu durumu", "teacher_notes" to "Öğretmen notu", "medicine_name" to "İlaç", "dosage" to "Doz", "notes" to "Not", "complaint" to "Şikâyet", "treatment_given" to "Uygulama", "pickup_person_name" to "Teslim alan")) details.optional(key)?.let { Text("$label: $it") }
                        details.activitiesText()?.let { Text("Etkinlikler: $it") }
                    }
                }
            } }
        }
    }
}
private fun JSONObject.optional(key: String): String? = if (isNull(key)) null else opt(key)?.toString()?.takeIf { it.isNotBlank() }
private fun JSONObject.text(key: String) = optional(key).orEmpty()
private fun JSONObject.activitiesText(): String? = when (val value = opt("activities")) {
    is JSONArray -> (0 until value.length()).mapNotNull { value.opt(it) as? String }
        .map(String::trim).filter(String::isNotEmpty).joinToString(", ").takeIf(String::isNotBlank)
    is String -> value.trim().takeIf(String::isNotBlank)
    else -> null
}
private fun lesson(value: JSONObject) = listOf(value.text("start_time"), value.text("end_time"), value.text("subject"), value.text("teacher")).filter { it.isNotBlank() }.joinToString(" · ")
