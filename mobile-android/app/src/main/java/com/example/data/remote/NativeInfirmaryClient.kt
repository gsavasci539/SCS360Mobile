package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

internal data class InfirmaryStudent(val id: String, val name: String, val branch: String?)
internal data class InfirmaryOptions(val students: List<InfirmaryStudent>, val today: LocalDate, val time: LocalTime, val more: Boolean)
internal data class NativeInfirmaryVisit(
    val id: String, val studentId: String, val studentName: String?, val branch: String?, val date: LocalDate,
    val time: LocalTime, val severity: String, val complaint: String?, val temperature: String?, val treatment: String?,
    val followUpRequired: Boolean, val followUpNotes: String?, val examiner: String?, val parentNotified: Boolean, val notificationMethod: String?,
)
internal data class InfirmaryVisitDraft(
    val student: InfirmaryStudent, val date: LocalDate, val time: LocalTime, val severity: String, val complaint: String,
    val temperature: Double?, val treatment: String, val followUpRequired: Boolean, val followUpNotes: String,
    val parentNotified: Boolean, val notificationMethod: String,
)

internal class NativeInfirmaryClient(selectedBranch: String, private val actorId: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val branch = selectedBranch.takeUnless { it.isBlank() || it == "all" }?.let(::id)

    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(field: String) = if (isNull(field)) null else getString(field)
    private fun JSONArray.rows() = (0 until length()).map(::getJSONObject)

    private suspend fun call(resource: String, query: Map<String, String?> = emptyMap(), body: JSONObject? = null): Any =
        scope.io {
            val url = (base + "infirmary/" + resource).toHttpUrl().newBuilder().apply {
                query.forEach { (key, value) -> if (value != null) addQueryParameter(key, value) }
            }.build()
            val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope)
            if (body != null) request.post(body.toString().toRequestBody("application/json".toMediaType()))
            http.newCall(request.build()).execute().use { response ->
                if (!response.isSuccessful) throw RegistryHttpException(response.code)
                val result = JSONTokener(response.body?.string().orEmpty()).nextValue()
                if (result is JSONObject && result.has("success")) {
                    check(result.getBoolean("success")) { "Sunucu revir işlemini doğrulamadı." }
                    result.get("data")
                } else result
            }
        }

    suspend fun options(search: String = ""): InfirmaryOptions {
        val result = call("options", mapOf("branch_id" to branch, "search" to search.takeIf { it.isNotBlank() })) as JSONObject
        val students = result.getJSONArray("students").rows().map { row ->
            val studentBranch = row.text("branch_id")?.let(::id)
            check(branch == null || branch == studentBranch) { "Öğrenci seçili şubeyle eşleşmiyor." }
            InfirmaryStudent(id(row.getString("id")), row.getString("name"), studentBranch)
        }
        return InfirmaryOptions(students, LocalDate.parse(result.getString("today")), LocalTime.parse(result.getString("time")), result.getBoolean("more_students"))
    }

    suspend fun visits(student: InfirmaryStudent?, date: LocalDate, offset: Int, limit: Int): List<NativeInfirmaryVisit> {
        val query = mapOf("student_id" to student?.id, "branch_id" to branch, "start_date" to date.toString(),
            "end_date" to date.toString(), "offset" to offset.toString(), "limit" to limit.toString())
        return (call("infirmary-visits", query) as JSONArray).rows().map { row ->
            val studentId = id(row.getString("student_id"))
            val receivedBranch = row.text("branch_id")?.let(::id)
            val visitDate = LocalDate.parse(row.getString("visit_date"))
            check((student == null || student.id == studentId) &&
                (branch == null || branch == receivedBranch) && visitDate == date) { "Ziyaret seçili öğrenci, şube veya tarihle eşleşmiyor." }
            NativeInfirmaryVisit(id(row.getString("id")), studentId, row.text("student_name"), receivedBranch, visitDate,
                LocalTime.parse(row.getString("visit_time")), row.getString("severity"), row.text("complaint"), row.text("temperature"),
                row.text("treatment_given"), row.getBoolean("follow_up_required"), row.text("follow_up_notes"),
                row.text("examined_by_name"), row.getBoolean("parent_notified"), row.text("notification_method"))
        }
    }

    suspend fun create(draft: InfirmaryVisitDraft): String {
        val body = JSONObject().put("student_id", draft.student.id).put("branch_id", draft.student.branch)
            .put("visit_date", draft.date.toString()).put("visit_time", draft.time.toString()).put("severity", draft.severity)
            .put("complaint", draft.complaint).put("temperature", draft.temperature).put("treatment_given", draft.treatment.ifBlank { null })
            .put("follow_up_required", draft.followUpRequired).put("follow_up_notes", draft.followUpNotes.ifBlank { null })
            .put("parent_notified", draft.parentNotified)
            .put("notification_method", draft.notificationMethod.takeIf { draft.parentNotified })
        val saved = call("infirmary-visits", body = body) as JSONObject
        check(saved.getString("student_id") == draft.student.id && saved.text("branch_id") == draft.student.branch &&
            saved.getString("examined_by") == id(actorId) && saved.getString("visit_date") == draft.date.toString() &&
            LocalTime.parse(saved.getString("visit_time")) == draft.time && saved.getString("severity") == draft.severity &&
            saved.getBoolean("follow_up_required") == draft.followUpRequired && saved.getBoolean("parent_notified") == draft.parentNotified) {
            "Kaydedilen ziyaret gönderilen öğrenci, şube ve ziyaret bilgileriyle eşleşmiyor."
        }
        return id(saved.getString("id"))
    }

    suspend fun complete(visit: NativeInfirmaryVisit, notes: String): Boolean {
        val saved = call("infirmary-visits/${id(visit.id)}/follow-up", body = JSONObject().put("follow_up_notes", notes)) as JSONObject
        check(saved.getString("id") == visit.id && saved.getString("student_id") == visit.studentId &&
            saved.text("branch_id") == visit.branch && !saved.getBoolean("follow_up_required")) {
            "Sunucu yanıtı seçili ziyaret ile eşleşmiyor."
        }
        return saved.text("follow_up_notes") == notes
    }
}
