package com.example.data.remote

import com.example.data.model.*
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Canonical operational records, with no local-success writes or generated identities. */
class NativeStaffClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    fun current() = scope.current()
    private fun id(value: String) = UUID.fromString(value).toString()
    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Any = scope.io {
        val request = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (method != "GET") request.method(method, if (method == "DELETE") null else (body ?: JSONObject()).toString().toRequestBody("application/json".toMediaType()))
        http.newCall(request.build()).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val value = JSONTokener(response.body?.string() ?: "null").nextValue()
            if (value is JSONObject && value.has("success")) {
                check(value.getBoolean("success")) { "Sunucu işlemi onaylamadı" }; value.opt("data") ?: JSONObject.NULL
            } else value
        }
    }
    private fun JSONArray.rows() = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else optString(key)
    suspend fun duties(): List<TeacherDutyRoster> = (call("staff/duty-rosters") as JSONArray).rows().map { row ->
        TeacherDutyRoster(id = id(row.getString("id")), day_name = row.getString("day_of_week"), date = "",
            duty_location = row.getString("duty_location"), teacher_id = id(row.getString("teacher_id")),
            teacher_name = row.getString("teacher_name"), teacher_branch = "",
            shift_hours = row.getString("start_time") + " - " + row.getString("end_time"),
            is_active_today = false, notes = row.text("notes"))
    }
    private fun leave(row: JSONObject) = TeacherLeaveRequest(id = id(row.getString("id")), teacher_id = id(row.getString("staff_id")),
        teacher_name = row.getString("staff_name"), branch = row.text("branch"), leave_type = row.getString("leave_type"),
        start_date = row.getString("start_date"), end_date = row.getString("end_date"), total_days = row.getInt("days_count"),
        reason = row.getString("reason"), status = row.getString("status"), admin_response = row.text("admin_comment").ifBlank { null }, created_at = row.text("created_at"))
    suspend fun leaves() = (call("staff/leave-requests") as JSONArray).rows().map(::leave)
    suspend fun requestLeave(actor: User, type: String, start: String, end: String, reason: String): TeacherLeaveRequest {
        val days = ChronoUnit.DAYS.between(LocalDate.parse(start), LocalDate.parse(end)) + 1
        require(days > 0 && days <= Int.MAX_VALUE && type.isNotBlank() && reason.isNotBlank())
        val body = JSONObject().put("staff_id", id(actor.id)).put("staff_name", actor.resolvedName).put("leave_type", type)
            .put("start_date", start).put("end_date", end).put("days_count", days.toInt()).put("reason", reason)
        val record = leave(call("staff/leave-requests", "POST", body) as JSONObject)
        check(record.teacher_id == actor.id && record.start_date == start && record.end_date == end && record.status == "PENDING" && record.leave_type == type && record.reason == reason && record.total_days == days.toInt())
        return leaves().single { it.id == record.id }.also { check(it == record) { "İzin kaydı sunucudan doğrulanamadı" } }
    }
    suspend fun reviewLeave(leaveId: String, approved: Boolean, comment: String): TeacherLeaveRequest {
        val key = id(leaveId); val status = if (approved) "APPROVED" else "REJECTED"
        val result = leave(call("staff/leave-requests/$key/review", "PUT", JSONObject().put("status", status).put("admin_comment", comment)) as JSONObject)
        check(result.id == key && result.status == status && result.admin_response.orEmpty() == comment)
        return leaves().single { it.id == key }.also { check(it == result) { "İzin değerlendirmesi doğrulanamadı" } }
    }
}
