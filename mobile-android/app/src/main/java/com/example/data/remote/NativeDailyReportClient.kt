package com.example.data.remote

import com.example.data.model.DailyReport
import com.example.data.model.DailyReportDraft
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.json.JSONTokener
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder
import java.time.LocalDate
import java.util.UUID

internal class DailyReportHttpException(val code: Int) : IOException("Günlük karne isteği başarısız (HTTP $code)")

internal data class DailyReportStudentOption(val id: String, val label: String, val studentName: String, val classId: String)
internal data class DailyReportStudentPage(val rows: List<DailyReportStudentOption>, val totalPages: Int)
internal data class DailyReportPage(val rows: List<DailyReport>, val total: Int, val totalPages: Int)
internal data class DailyReportMetric(val observations: Int, val percent: Double?)
internal data class DailyReportFollowUp(val studentName: String, val mood: Int, val sleep: Int, val meals: Int)
internal data class DailyReportAnalysis(val total: Int, val mood: DailyReportMetric, val sleep: DailyReportMetric,
    val meals: DailyReportMetric, val followUps: List<DailyReportFollowUp>, val notes: List<String>)

internal data class DailyReportEdit(val meals: String, val sleep: String, val mood: String, val activities: List<String>, val note: String) {
    fun body() = JSONObject().put("meal_status", meals.takeUnless(String::isBlank) ?: JSONObject.NULL)
        .put("sleep_status", sleep.takeUnless(String::isBlank) ?: JSONObject.NULL)
        .put("mood", mood.takeUnless(String::isBlank) ?: JSONObject.NULL)
        .put("activities", JSONArray(activities))
        .put("teacher_notes", note.takeUnless(String::isBlank) ?: JSONObject.NULL)

    companion object {
        fun from(row: DailyReport) = DailyReportEdit(row.meal_status.orEmpty(), row.sleep_status.orEmpty(),
            row.mood_level, row.activities_done, row.teacher_note)
    }
}

internal class NativeDailyReportClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val client = ApiClientProvider.createOkHttpClient()

    suspend fun list(branch: String?): List<DailyReport> = pages(
        branch?.takeUnless { it == "all" || it.isBlank() }?.let { "&branch_id=${UUID.fromString(it)}" }.orEmpty(),
    )

    suspend fun listPage(branch: String?, classroom: String?, search: String, date: LocalDate?, page: Int): DailyReportPage {
        require(page > 0)
        val branchId = branch?.takeUnless { it.isBlank() || it == "all" }?.let { UUID.fromString(it).toString() }
        val classId = classroom?.let { UUID.fromString(it).toString() }
        val term = search.trim()
        val path = buildString {
            append("daily-reports?page=$page&page_size=20")
            if (branchId != null) append("&branch_id=$branchId")
            if (classId != null) append("&class_id=$classId")
            if (term.isNotEmpty()) append("&search=${URLEncoder.encode(term, "UTF-8")}")
            if (date != null) append("&date=$date")
        }
        val result = call(path)
        check(result.getInt("page") == page) { "Karne sayfası doğrulanamadı." }
        val items = result.getJSONArray("items")
        val rows = (0 until items.length()).map { parse(items.getJSONObject(it)) }
        check(rows.all { (classId == null || it.class_id == classId) && (date == null || it.date == date.toString()) }) {
            "Karne sonuçları seçilen filtreyle eşleşmedi."
        }
        return DailyReportPage(rows, result.getInt("total"), result.getInt("total_pages"))
    }

    suspend fun analysis(branch: String?): DailyReportAnalysis {
        val branchId = branch?.takeUnless { it.isBlank() || it == "all" }?.let { UUID.fromString(it).toString() }
        val query = branchId?.let { "?branch_id=$it" }.orEmpty()
        val body = call("ai/ai-daily-reports-analysis$query")
        val responseBranch = if (body.isNull("branch_id")) null else body.getString("branch_id")
        check(body.has("branch_id") && responseBranch == branchId) {
            "Karne analizi şubesi doğrulanamadı."
        }
        val stats = body.getJSONObject("report_stats")
        fun metric(name: String): DailyReportMetric {
            val count = stats.getInt("${name}_observations")
            val field = "${name}_rate"
            check(stats.has(field)) { "Karne analizi oranı eksik." }
            val percent = if (stats.isNull(field)) null else stats.getDouble(field)
            check((count == 0 && percent == null) || (count > 0 && percent != null && percent in 0.0..100.0)) {
                "Karne analizi oranı doğrulanamadı."
            }
            return DailyReportMetric(count, percent)
        }
        val rows = body.getJSONArray("students_needing_attention")
        val followUps = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            DailyReportFollowUp(row.getString("student_name"), row.getInt("poor_mood"), row.getInt("poor_sleep"), row.getInt("poor_meal"))
        }
        val notes = body.getJSONArray("recommendations")
        return DailyReportAnalysis(stats.getInt("total"), metric("mood"), metric("sleep"), metric("meal"),
            followUps, (0 until notes.length()).map(notes::getString))
    }

    suspend fun studentOptions(branch: String?, classroom: String?, search: String, page: Int): DailyReportStudentPage {
        require(page > 0)
        val branchId = branch?.takeUnless { it.isBlank() || it == "all" }?.let { UUID.fromString(it).toString() }
        val classId = classroom?.takeUnless { it.isBlank() || it == "all" }?.let { UUID.fromString(it).toString() }
        val path = buildString {
            append("daily-reports/student-options?page=$page&page_size=25")
            if (branchId != null) append("&branch_id=$branchId")
            if (classId != null) append("&class_id=$classId")
            if (search.isNotBlank()) append("&search=${URLEncoder.encode(search.trim(), "UTF-8")}")
        }
        val result = call(path)
        check(result.getInt("page") == page) { "Öğrenci seçenekleri sayfası doğrulanamadı." }
        val items = result.getJSONArray("items")
        val rows = (0 until items.length()).map { index ->
            val row = items.getJSONObject(index)
            val assignedClass = UUID.fromString(row.getString("class_id")).toString()
            check(classId == null || classId == assignedClass) { "Öğrenci sınıfı doğrulanamadı." }
            DailyReportStudentOption(UUID.fromString(row.getString("id")).toString(), row.getString("name"),
                row.getString("student_name"), assignedClass)
        }
        return DailyReportStudentPage(rows, result.getInt("total_pages"))
    }

    suspend fun detail(id: String): DailyReport {
        val canonicalId = UUID.fromString(id).toString()
        return parse(call("daily-reports/$canonicalId")).also { check(it.id == canonicalId) { "Karne kimliği eşleşmedi." } }
    }

    suspend fun update(original: DailyReport, draft: DailyReportEdit, actorId: String): DailyReport {
        val actor = UUID.fromString(actorId).toString()
        val edit = draft.copy(meals = draft.meals.takeUnless(String::isBlank).orEmpty(),
            sleep = draft.sleep.takeUnless(String::isBlank).orEmpty(),
            mood = draft.mood.takeUnless(String::isBlank).orEmpty(), note = draft.note.takeUnless(String::isBlank).orEmpty())
        fun matches(row: DailyReport) = sameRecord(row, original) && row.recorded_by == actor && DailyReportEdit.from(row) == edit
        val latest = detail(original.id)
        if (matches(latest)) return latest
        requireCurrent(latest, original)
        try {
            val response = parse(call("daily-reports/${latest.id}", edit.body(), "PUT", original.version))
            check(matches(response)) { "Güncelleme yanıtı karneyle eşleşmedi." }
        } catch (failure: IOException) {
            if (!uncertain(failure)) throw failure
        }
        return detail(latest.id).also { check(matches(it)) { "Güncelleme doğrulanamadı. Taslağınız açık." } }
    }

    suspend fun delete(original: DailyReport) {
        val latest = detailOrMissing(original.id) ?: return
        requireCurrent(latest, original)
        try {
            call("daily-reports/${latest.id}", method = "DELETE", version = original.version)
        } catch (failure: IOException) {
            if (!uncertain(failure) && (failure as? DailyReportHttpException)?.code != 404) throw failure
        }
        check(detailOrMissing(latest.id) == null) { "Silme doğrulanamadı. Yeniden deneyebilirsiniz." }
    }

    private suspend fun detailOrMissing(id: String): DailyReport? = try {
        detail(id)
    } catch (failure: DailyReportHttpException) {
        if (failure.code == 404) null else throw failure
    }

    private fun sameRecord(left: DailyReport, right: DailyReport) = left.id == right.id &&
        left.student_id == right.student_id && left.class_id == right.class_id && left.date == right.date

    private fun requireCurrent(latest: DailyReport, original: DailyReport) {
        check(!original.version.isNullOrBlank()) { "Karne sürümü alınamadı. Güncel kaydı açın." }
        check(sameRecord(latest, original) && latest.version == original.version && latest.recorded_by == original.recorded_by &&
            DailyReportEdit.from(latest) == DailyReportEdit.from(original)) { "Karne değişti. Güncel kaydı açın." }
    }

    private fun uncertain(failure: IOException): Boolean =
        (failure as? DailyReportHttpException)?.let { it.code >= 500 } ?: true

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

    private suspend fun call(path: String, body: JSONObject? = null, method: String = if (body == null) "GET" else "POST",
        version: String? = null): JSONObject = scope.io {
        val request = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (version != null) request.header("If-Match", "\"$version\"")
        request.method(method, body?.toString()?.toRequestBody("application/json".toMediaType()))
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request.build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resumeWith(runCatching {
                        response.use { reply ->
                            if (!reply.isSuccessful) throw DailyReportHttpException(reply.code)
                            val data = JSONTokener(reply.body?.string().orEmpty()).nextValue() as JSONObject
                            if (!data.has("success")) return@use data
                            check(data.getBoolean("success")) { "Günlük karne işlemi doğrulanamadı." }
                            if (method == "DELETE" && data.isNull("data")) JSONObject() else data.getJSONObject("data")
                        }
                    })
                }
            })
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
            teacher_note = text("teacher_notes").orEmpty(), recorded_by = text("recorded_by"), version = text("version"),
        )
    }
}
