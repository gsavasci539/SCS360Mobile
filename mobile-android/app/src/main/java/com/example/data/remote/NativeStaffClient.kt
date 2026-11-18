package com.example.data.remote

import com.example.data.model.*
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class DutyOption(val id: String, val name: String)

data class DutyDraft(
    val branch: DutyOption,
    val teacher: DutyOption,
    val day: DayOfWeek,
    val location: DutyLocation,
    val start: String,
    val end: String,
    val requestId: UUID = UUID.randomUUID(),
)

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
    private suspend fun dutyRows(branchId: String?): List<JSONObject> {
        val filter = branchId?.let { "?branch_id=${id(it)}" }.orEmpty()
        return (call("staff/duty-rosters$filter") as JSONArray).rows()
    }

    private fun duty(row: JSONObject): TeacherDutyRoster =
        TeacherDutyRoster(id = id(row.getString("id")), day_name = row.getString("day_of_week"), date = "",
            duty_location = row.getString("duty_location"), teacher_id = id(row.getString("teacher_id")),
            teacher_name = row.getString("teacher_name"), teacher_branch = "",
            shift_hours = row.getString("start_time") + " - " + row.getString("end_time"),
            is_active_today = false, notes = row.text("notes"))

    suspend fun duties(branchId: String? = null) = dutyRows(branchId).map(::duty)

    suspend fun dutyOptions(branchId: String? = null): List<DutyOption> {
        val filter = branchId?.let { "&branch_id=${id(it)}" }.orEmpty()
        val options = linkedMapOf<String, DutyOption>()
        var page = 1
        do {
            val response = call("staff/duty-rosters/options?page=$page&page_size=100$filter") as JSONObject
            check(response.getInt("page") == page)
            val totalPages = response.getInt("total_pages")
            for (row in response.getJSONArray("items").rows()) {
                val option = DutyOption(id(row.getString("id")), row.getString("name"))
                options[option.id] = option
            }
            page++
        } while (page <= totalPages)
        return options.values.toList()
    }

    private fun dutyMatches(row: JSONObject, draft: DutyDraft): Boolean =
        row.getString("id") == draft.requestId.toString() &&
            row.getString("branch_id") == id(draft.branch.id) &&
            row.getString("teacher_id") == id(draft.teacher.id) &&
            row.getString("day_of_week") == draft.day.name &&
            row.getString("duty_location") == draft.location.name &&
            row.getString("start_time") == draft.start && row.getString("end_time") == draft.end &&
            row.getString("status") == "ACTIVE"

    suspend fun confirmDuty(draft: DutyDraft, visibleBranchId: String?): List<TeacherDutyRoster>? {
        val rows = dutyRows(visibleBranchId)
        val saved = rows.singleOrNull { it.getString("id") == draft.requestId.toString() } ?: return null
        check(dutyMatches(saved, draft)) { "Nöbet kaydı sunucudan doğrulanamadı" }
        return rows.map(::duty)
    }

    suspend fun createDuty(draft: DutyDraft, visibleBranchId: String?): List<TeacherDutyRoster> {
        require(LocalTime.parse(draft.start) < LocalTime.parse(draft.end))
        val body = JSONObject().put("branch_id", id(draft.branch.id)).put("teacher_id", id(draft.teacher.id))
            .put("teacher_name", draft.teacher.name).put("day_of_week", draft.day.name)
            .put("duty_location", draft.location.name).put("start_time", draft.start).put("end_time", draft.end)
            .put("request_id", draft.requestId.toString())
        val saved = call("staff/duty-rosters", "POST", body) as JSONObject
        check(dutyMatches(saved, draft)) { "Sunucu nöbet kaydını doğrulamadı" }
        return checkNotNull(confirmDuty(draft, visibleBranchId)) { "Nöbet listesi güncellenemedi" }
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
