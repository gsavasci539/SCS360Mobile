package com.example.data.remote

import com.example.data.model.ParentTeacherAppointment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID

data class AppointmentChoice(val id: String, val name: String)
data class AppointmentSlot(val start: String, val end: String, val duration: Int)
data class ManagedAppointmentSlot(val id: String, val start: String, val end: String, val available: Boolean)
data class AppointmentDraft(
    val teacher: AppointmentChoice,
    val student: AppointmentChoice,
    val date: String,
    val time: String,
    val note: String,
    val duration: Int,
    val requestId: UUID = UUID.randomUUID(),
) {
    fun matches(row: ParentTeacherAppointment) =
        row.id == requestId.toString() && row.teacher_id == teacher.id && row.student_id == student.id &&
            row.date == date && row.time_slot == time && row.parent_topic_notes == note && row.duration_minutes == duration
}

data class AppointmentDecision(val id: String, val status: String, val note: String?) {
    fun matches(row: ParentTeacherAppointment) = row.id == id && row.status == status &&
        (note == null || row.teacher_feedback_notes.orEmpty() == note)
}

class NativeAppointmentClient(private val branchId: String? = null) {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    fun current() = scope.current()

    private suspend fun exchange(path: String, method: String = "GET", body: JSONObject? = null): Any = scope.io {
        val request = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (method == "DELETE") request.delete()
        if (body != null) request.method(method, body.toString().toRequestBody("application/json".toMediaType()))
        http.newCall(request.build()).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val envelope = JSONTokener(response.body?.string() ?: "null").nextValue() as JSONObject
            check(envelope.getBoolean("success")) { "Sunucu randevu işlemini doğrulamadı" }
            envelope.get("data")
        }
    }

    private fun JSONArray.rows() = (0 until length()).map(::getJSONObject)
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else optString(key)
    private fun id(raw: String) = UUID.fromString(raw).toString()
    private fun branchFilter() = branchId?.let { "&branch_id=${id(it)}" }.orEmpty()

    suspend fun records(): List<ParentTeacherAppointment> {
        var page = 1
        val rows = linkedMapOf<String, ParentTeacherAppointment>()
        do {
            val response = exchange("appointments?page=$page&page_size=100${branchFilter()}")
            val values = if (response is JSONArray) response else (response as JSONObject).getJSONArray("items")
            for (row in values.rows()) {
                val saved = ParentTeacherAppointment(
                    id = id(row.getString("id")), teacher_id = id(row.getString("teacher_id")),
                    teacher_name = row.getString("teacher_name"), parent_id = id(row.getString("parent_id")),
                    student_id = id(row.getString("student_id")), student_name = row.getString("student_name"),
                    date = row.getString("appointment_date"), time_slot = row.getString("appointment_time"),
                    status = row.getString("status"), parent_topic_notes = row.text("notes"),
                    teacher_feedback_notes = row.text("feedback_notes").ifEmpty { null },
                    created_at = row.text("created_at"), duration_minutes = row.getInt("duration_minutes"),
                )
                rows[saved.id] = saved
            }
            if (response !is JSONObject) break
            check(response.getInt("page") == page)
            page++
        } while (page <= response.getInt("total_pages"))
        return rows.values.toList()
    }

    suspend fun options(studentId: String? = null): List<AppointmentChoice> {
        val student = studentId?.let { "&student_id=${id(it)}" }.orEmpty()
        val choices = linkedMapOf<String, AppointmentChoice>()
        var page = 1
        do {
            val body = exchange("appointments/options?page=$page&page_size=100$student${branchFilter()}") as JSONObject
            check(body.getInt("page") == page)
            val total = body.getInt("total_pages")
            for (row in body.getJSONArray("items").rows()) {
                val choice = AppointmentChoice(id(row.getString("id")), row.getString("name"))
                choices[choice.id] = choice
            }
            page++
        } while (page <= total)
        return choices.values.toList()
    }

    suspend fun slots(teacherId: String, date: String): List<AppointmentSlot> =
        (exchange("appointments/teacher-slots/${id(teacherId)}?date=$date") as JSONArray).rows().map { row ->
            check(row.getString("date") == date && id(row.getString("teacher_id")) == id(teacherId))
            AppointmentSlot(row.getString("start_time"), row.getString("end_time"), row.getInt("duration_minutes"))
        }.filter { it.duration in 15..60 }

    suspend fun create(draft: AppointmentDraft, fromParent: Boolean) {
        val body = JSONObject().put("request_id", draft.requestId.toString())
            .put("teacher_id", id(draft.teacher.id)).put("student_id", id(draft.student.id))
            .put("slot_datetime", "${draft.date}T${draft.time}").put("parent_note", draft.note)
            .put("duration_minutes", draft.duration)
        val route = if (fromParent) "appointments/request" else "appointments"
        val ack = exchange(route, "POST", body) as JSONObject
        check(id(ack.getString("id")) == draft.requestId.toString())
    }

    suspend fun decide(decision: AppointmentDecision) {
        val key = id(decision.id)
        val body = JSONObject().put("status", decision.status)
        decision.note?.let { body.put("feedback_notes", it.takeIf(String::isNotEmpty) ?: JSONObject.NULL) }
        val ack = exchange("appointments/$key/status", "PATCH", body) as JSONObject
        check(id(ack.getString("id")) == key && ack.getString("status") == decision.status)
    }

    suspend fun delete(appointmentId: String) {
        exchange("appointments/${id(appointmentId)}", "DELETE")
    }

    suspend fun managedSlots(teacherId: String, date: String): List<ManagedAppointmentSlot> =
        (exchange("appointments/slots?date=$date") as JSONArray).rows().map { row ->
            check(row.getString("date") == date && id(row.getString("teacher_id")) == id(teacherId))
            ManagedAppointmentSlot(id(row.getString("id")), row.getString("start_time"),
                row.getString("end_time"), row.getBoolean("is_available"))
        }

    suspend fun createSlot(teacherId: String, date: String, start: String, end: String) {
        val payload = JSONObject().put("teacher_id", id(teacherId)).put("date", date)
            .put("start_time", start).put("end_time", end).put("is_available", true)
        exchange("appointments/slots", "POST", payload)
    }

    suspend fun deleteSlot(slotId: String) {
        exchange("appointments/slots/${id(slotId)}", "DELETE")
    }
}
