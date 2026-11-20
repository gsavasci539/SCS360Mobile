package com.example.data.remote

import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

internal data class TeacherDayClass(val id: String, val name: String, val studentCount: Int)
internal data class TeacherDayLesson(val subject: String, val time: String, val location: String)
internal data class TeacherDayTask(val classroom: String, val kind: String, val remaining: Int, val date: LocalDate)
internal data class TeacherWeekDay(val name: String, val lessons: List<TeacherDayLesson>)
internal data class TeacherMedicineRequest(val id: String, val studentName: String, val classroom: String,
    val name: String, val dose: String, val timing: String, val instructions: String?, val status: String)

internal class NativeTeacherDayClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val client = ApiClientProvider.createOkHttpClient()

    private suspend fun get(path: String): Any = scope.io {
        val request = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Öğretmen verisi yüklenemedi (HTTP ${response.code})")
            val payload = JSONTokener(response.body?.string().orEmpty()).nextValue()
            if (payload is JSONObject && payload.has("success")) {
                check(payload.getBoolean("success")) { "Öğretmen verisi doğrulanamadı" }
                payload.get("data")
            } else payload
        }
    }

    suspend fun classes(): List<TeacherDayClass> = rows(get("teacher/my-classes") as JSONArray).map { row ->
        TeacherDayClass(UUID.fromString(row.getString("id")).toString(), row.getString("name"),
            row.getInt("student_count").also { check(it >= 0) })
    }

    suspend fun today(): List<TeacherDayLesson> = rows(get("teacher/today-schedule") as JSONArray).map { row ->
        val start = row.getString("start_time")
        val end = row.getString("end_time")
        val location = listOfNotNull(row.getString("class_name"), optionalText(row, "room_number")).joinToString(" · ")
        TeacherDayLesson(row.getString("subject"), if (end.isBlank()) start else "$start - $end", location)
    }

    suspend fun pending(): List<TeacherDayTask> {
        val payload = get("teacher/pending-tasks") as JSONObject
        val date = LocalDate.parse(payload.getString("date"))
        return rows(payload.getJSONArray("tasks")).map { row ->
            val kind = when (row.getString("task_type")) {
                "attendance" -> "Yoklama"
                "daily_report" -> "Günlük rapor"
                else -> error("Tanımsız öğretmen görevi")
            }
            check(LocalDate.parse(row.getString("due_date")) == date)
            TeacherDayTask(row.getString("class_name"), kind, row.getInt("remaining_count").also { check(it > 0) }, date)
        }
    }

    suspend fun week(classId: String): List<TeacherWeekDay> {
        val id = UUID.fromString(classId)
        return rows(get("classes/$id/schedule") as JSONArray).map { day ->
            val lessons = rows(day.getJSONArray("periods")).map { row ->
                TeacherDayLesson(row.getString("subject"), optionalText(row, "time").orEmpty(),
                    optionalText(row, "room_number").orEmpty())
            }
            TeacherWeekDay(day.getString("day"), lessons)
        }
    }

    suspend fun medicines(selectedBranch: String, offset: Int, limit: Int): List<TeacherMedicineRequest> {
        val branch = selectedBranch.takeUnless { it.isBlank() || it == "all" }?.let { UUID.fromString(it).toString() }
        val path = "teacher/medicine-requests?offset=$offset&limit=$limit" + (branch?.let { "&branch_id=$it" }.orEmpty())
        return rows(get(path) as JSONArray).map { row ->
            check(branch == null || row.getString("branch_id") == branch) { "İlaç talebi seçili şubeyle eşleşmiyor." }
            UUID.fromString(row.getString("student_id"))
            TeacherMedicineRequest(UUID.fromString(row.getString("id")).toString(), row.getString("student_name"),
                row.getString("class_name"), row.getString("medicine_name"), row.getString("dosage"),
                row.getString("time_to_give"), optionalText(row, "instructions"), row.getString("status"))
        }
    }

    private fun rows(values: JSONArray): List<JSONObject> = (0 until values.length()).map { values.getJSONObject(it) }

    private fun optionalText(row: JSONObject, field: String) =
        if (row.isNull(field)) null else row.getString(field).takeIf { it.isNotBlank() }
}
