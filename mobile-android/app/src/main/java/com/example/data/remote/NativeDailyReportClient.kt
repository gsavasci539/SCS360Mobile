package com.example.data.remote

import com.example.data.model.DailyReport
import com.example.data.model.DailyReportDraft
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

internal class DailyReportHttpException(val code: Int) : IOException("Günlük karne isteği başarısız (HTTP $code)")

internal class NativeDailyReportClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val client = ApiClientProvider.createOkHttpClient()

    suspend fun list(branch: String?): List<DailyReport> = pages(
        branch?.takeUnless { it == "all" || it.isBlank() }?.let { "&branch_id=${UUID.fromString(it)}" }.orEmpty(),
    )

    suspend fun save(draft: DailyReportDraft, actorId: String, canRead: Boolean): DailyReport {
        require(draft.hasObservation && (draft.sleepMinutes == null || draft.sleepMinutes in 0..150)) { "En az bir geçerli gözlem girin." }
        val student = UUID.fromString(draft.studentId).toString()
        val classroom = UUID.fromString(draft.classId).toString()
        val actor = UUID.fromString(actorId).toString()
        val day = draft.date.toString()
        val meals = listOf("Kahvaltı" to draft.breakfast, "Öğle" to draft.lunch, "Ara öğün" to draft.snack)
            .filter { it.second.isNotBlank() }.joinToString(" / ") { (label, value) -> "$label: $value" }
        val sleep = draft.sleepMinutes?.let { if (it == 0) "Uyumadı" else "$it dk" }
        val body = JSONObject().put("student_id", student).put("class_id", classroom).put("date", day)
        listOf("meal_breakfast" to draft.breakfast, "meal_lunch" to draft.lunch, "meal_snack" to draft.snack,
            "sleep_status" to sleep, "mood" to draft.mood, "teacher_notes" to draft.note).forEach { (key, value) ->
            if (!value.isNullOrBlank()) body.put(key, value)
        }
        if (draft.activities.isNotEmpty()) body.put("activities", org.json.JSONArray(draft.activities))

        fun confirm(row: DailyReport): DailyReport {
            check(row.student_id == student && row.class_id == classroom && row.date == day && row.recorded_by == actor &&
                row.meal_status.orEmpty() == meals && row.sleep_status == sleep && row.mood_level == body.optString("mood") &&
                row.activities_done == draft.activities && row.teacher_note == body.optString("teacher_notes")) {
                "Bu öğrenci ve tarihte farklı bir karne var. Güncel kaydı kontrol edin."
            }
            return row
        }

        suspend fun existing(): DailyReport? {
            val matches = pages("&student_id=$student&date=$day")
            check(matches.size <= 1) { "Öğrenci ve tarih için birden fazla karne var." }
            return matches.singleOrNull()?.let(::confirm)
        }
        if (canRead) existing()?.let { return it }

        val created = try {
            parse(call("daily-reports", body))
        } catch (failure: IOException) {
            val status = (failure as? DailyReportHttpException)?.code
            if (canRead && (status == null || status == 409 || status >= 500)) {
                existing()?.let { return it }
            }
            throw failure
        }
        confirm(created)
        return if (canRead) confirm(parse(call("daily-reports/${created.id}"))) else created
    }

    private suspend fun pages(filters: String): List<DailyReport> = buildList {
        var page = 1
        while (true) {
            val result = call("daily-reports?page=$page&page_size=100$filters")
            check(result.getInt("page") == page)
            val items = result.getJSONArray("items")
            for (index in 0 until items.length()) add(parse(items.getJSONObject(index)))
            if (page >= result.getInt("total_pages")) break
            check(items.length() > 0) { "Karne listesi tamamlanamadı." }
            page++
        }
    }

    private suspend fun call(path: String, body: JSONObject? = null): JSONObject = scope.io {
        val request = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (body != null) request.post(body.toString().toRequestBody("application/json".toMediaType()))
        client.newCall(request.build()).execute().use { reply ->
            if (!reply.isSuccessful) throw DailyReportHttpException(reply.code)
            val data = JSONTokener(reply.body?.string().orEmpty()).nextValue() as JSONObject
            if (data.has("success")) {
                check(data.getBoolean("success")) { "Günlük karne işlemi doğrulanamadı." }
                data.getJSONObject("data")
            } else data
        }
    }

    private fun parse(row: JSONObject): DailyReport {
        fun text(key: String) = if (row.isNull(key)) null else row.getString(key)
        val activities = row.optJSONArray("activities")
        return DailyReport(
            id = UUID.fromString(row.getString("id")).toString(),
            student_id = UUID.fromString(row.getString("student_id")).toString(),
            student_name = text("student_name").orEmpty(),
            class_id = UUID.fromString(row.getString("class_id")).toString(),
            date = LocalDate.parse(row.getString("date")).toString(),
            meal_status = text("meal_status"), sleep_status = text("sleep_status"), mood_level = text("mood").orEmpty(),
            activities_done = activities?.let { values -> (0 until values.length()).map(values::getString) }.orEmpty(),
            teacher_note = text("teacher_notes").orEmpty(), recorded_by = text("recorded_by"),
        )
    }
}
