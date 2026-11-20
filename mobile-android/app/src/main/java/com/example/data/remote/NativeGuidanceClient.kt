package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate
import java.util.UUID

internal data class GuidanceChoice(val id: String, val name: String)
internal data class GuidanceOptions(
    val students: List<GuidanceChoice>, val counselors: List<GuidanceChoice>, val today: LocalDate,
    val moreStudents: Boolean, val moreCounselors: Boolean,
)
internal data class GuidanceSummary(
    val sessions: Int, val notes: Int, val incidents: Int, val activeCases: Int, val resolutionRate: Double,
)
internal data class GuidanceTimelineEntry(
    val id: String, val type: String, val category: String, val date: String, val description: String?,
    val author: String?, val confidentiality: String?, val resolved: Boolean?,
)
internal sealed interface GuidanceRow {
    val id: String
    val studentName: String?
}
internal data class GuidanceSessionRow(
    override val id: String, override val studentName: String?, val counselorName: String?, val type: String, val date: String,
    val confidentiality: String, val summary: String?, val actionPlan: String?, val followUp: String?,
) : GuidanceRow
internal data class GuidanceNoteRow(
    override val id: String, override val studentName: String?, val counselorName: String?, val type: String, val createdAt: String,
    val confidentiality: String, val content: String, val tags: List<String>,
) : GuidanceRow
internal data class GuidanceIncidentRow(
    override val id: String, override val studentName: String?, val studentId: String, val reporter: String?, val date: String,
    val severity: String, val description: String, val location: String?, val parentNotified: Boolean,
    val resolved: Boolean, val resolutionNotes: String?,
) : GuidanceRow
internal data class GuidanceSessionDraft(
    val student: GuidanceChoice, val counselor: GuidanceChoice, val type: String, val date: LocalDate,
    val confidentiality: String, val summary: String, val actionPlan: String, val followUp: LocalDate?,
)
internal data class GuidanceNoteDraft(
    val student: GuidanceChoice, val counselor: GuidanceChoice, val type: String, val confidentiality: String,
    val content: String, val tags: List<String>,
)
internal data class GuidanceIncidentDraft(
    val student: GuidanceChoice, val date: LocalDate, val severity: String, val description: String,
    val location: String, val parentNotified: Boolean,
)

internal class NativeGuidanceClient {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private fun id(value: String): String = UUID.fromString(value).toString()
    private fun JSONArray.rows() = (0 until length()).map(::getJSONObject)
    private fun JSONObject.text(key: String) = if (isNull(key)) null else getString(key)

    private suspend fun call(
        resource: String, query: List<Pair<String, String>> = emptyList(), body: JSONObject? = null, method: String = "POST",
    ): Any =
        scope.io {
            val url = (base + "guidance/" + resource).toHttpUrl().newBuilder().apply {
                query.forEach { (key, value) -> addQueryParameter(key, value) }
            }.build()
            val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope)
            if (body != null) request.method(method, body.toString().toRequestBody("application/json".toMediaType()))
            http.newCall(request.build()).execute().use { response ->
                if (!response.isSuccessful) throw RegistryHttpException(response.code)
                val result = JSONTokener(response.body?.string().orEmpty()).nextValue()
                if (result is JSONObject && result.has("success")) {
                    check(result.getBoolean("success")) { "Sunucu rehberlik işlemini doğrulamadı." }
                    result.get("data")
                } else result
            }
        }

    suspend fun options(student: GuidanceChoice?, search: String, counselorSearch: String): GuidanceOptions {
        val query = buildList {
            add("search" to search)
            add("counselor_search" to counselorSearch)
            student?.let { add("student_id" to id(it.id)) }
        }
        val result = call("options", query) as JSONObject
        fun choices(key: String) = result.getJSONArray(key).rows().map { GuidanceChoice(id(it.getString("id")), it.getString("name")) }
        val students = choices("students")
        check(student == null || students.any { it.id == student.id }) { "Seçili öğrenci doğrulanamadı." }
        return GuidanceOptions(students, choices("counselors"), LocalDate.parse(result.getString("today")),
            result.getBoolean("more_students"), result.getBoolean("more_counselors"))
    }

    suspend fun summary(student: GuidanceChoice?): GuidanceSummary {
        val query = student?.let { listOf("student_id" to id(it.id)) }.orEmpty()
        val result = call("summary", query) as JSONObject
        check(result.text("student_id") == student?.id) { "Rehberlik özeti seçili öğrenciyle eşleşmiyor." }
        return GuidanceSummary(result.getInt("total_sessions"), result.getInt("total_notes"),
            result.getInt("total_incidents"), result.getInt("active_cases"), result.getDouble("resolution_rate"))
    }

    suspend fun timeline(student: GuidanceChoice): List<GuidanceTimelineEntry> =
        (call("students/${id(student.id)}/timeline") as JSONArray).rows().map { row ->
            check(id(row.getString("student_id")) == student.id) { "Geçmiş kaydı seçili öğrenciyle eşleşmiyor." }
            val type = row.getString("type")
            GuidanceTimelineEntry(id(row.getString("id")), type, row.getString("category"), row.getString("date"),
                row.text("description"), row.text("counselor") ?: row.text("reported_by"), row.text("confidentiality_level"),
                if (row.isNull("resolved")) null else row.getBoolean("resolved"))
        }

    private suspend fun scopedRows(resource: String, student: GuidanceChoice?, offset: Int, limit: Int): List<JSONObject> {
        val query = buildList {
            add("offset" to offset.toString())
            add("limit" to limit.toString())
            student?.let { add("student_id" to id(it.id)) }
        }
        return (call(resource, query) as JSONArray).rows().onEach { row ->
            val studentId = id(row.getString("student_id"))
            check(student == null || studentId == student.id) { "Kayıt seçili öğrenciyle eşleşmiyor." }
        }
    }

    suspend fun sessions(student: GuidanceChoice?, offset: Int, limit: Int): List<GuidanceSessionRow> =
        scopedRows("sessions", student, offset, limit).map { row ->
            GuidanceSessionRow(id(row.getString("id")), row.text("student_name"), row.text("counselor_name"),
                row.getString("session_type"), row.getString("session_date"), row.getString("confidentiality_level"),
                row.text("summary"), row.text("action_plan"), row.text("follow_up_date"))
        }

    suspend fun notes(student: GuidanceChoice?, offset: Int, limit: Int): List<GuidanceNoteRow> =
        scopedRows("notes", student, offset, limit).map { row ->
            val tags = row.optJSONArray("tags")
            GuidanceNoteRow(id(row.getString("id")), row.text("student_name"), row.text("counselor_name"),
                row.getString("note_type"), row.getString("created_at"), row.getString("confidentiality_level"),
                row.getString("content"), if (tags == null) emptyList() else (0 until tags.length()).map(tags::getString))
        }

    suspend fun incidents(student: GuidanceChoice?, offset: Int, limit: Int): List<GuidanceIncidentRow> =
        scopedRows("incidents", student, offset, limit).map { row ->
            GuidanceIncidentRow(id(row.getString("id")), row.text("student_name"), id(row.getString("student_id")),
                row.text("reported_by_name"), row.getString("incident_date"), row.getString("severity"),
                row.getString("description"), row.text("location"), row.getBoolean("parent_notified"),
                row.getBoolean("resolved"), row.text("resolution_notes"))
        }

    suspend fun create(draft: GuidanceSessionDraft) {
        val body = JSONObject().put("student_id", id(draft.student.id)).put("counselor_id", id(draft.counselor.id))
            .put("session_type", draft.type).put("session_date", draft.date.toString())
            .put("confidentiality_level", draft.confidentiality)
        if (draft.summary.isNotBlank()) body.put("summary", draft.summary)
        if (draft.actionPlan.isNotBlank()) body.put("action_plan", draft.actionPlan)
        draft.followUp?.let { body.put("follow_up_date", it.toString()) }
        val saved = call("sessions", body = body) as JSONObject
        id(saved.getString("id"))
        check(saved.getString("student_id") == draft.student.id && saved.getString("counselor_id") == draft.counselor.id &&
            saved.getString("session_type") == draft.type && saved.getString("session_date") == draft.date.toString()) {
            "Kaydedilen görüşme seçili öğrenci, danışman ve tarihle eşleşmiyor."
        }
    }

    suspend fun createNote(draft: GuidanceNoteDraft) {
        val body = JSONObject().put("student_id", id(draft.student.id)).put("counselor_id", id(draft.counselor.id))
            .put("note_type", draft.type).put("confidentiality_level", draft.confidentiality)
            .put("content", draft.content).put("tags", JSONArray(draft.tags))
        val saved = call("notes", body = body) as JSONObject
        id(saved.getString("id"))
        check(saved.getString("student_id") == draft.student.id && saved.getString("counselor_id") == draft.counselor.id &&
            saved.getString("note_type") == draft.type && saved.getString("confidentiality_level") == draft.confidentiality) {
            "Kaydedilen not seçili öğrenci ve danışmanla eşleşmiyor."
        }
    }

    suspend fun createIncident(draft: GuidanceIncidentDraft, actorId: String) {
        val body = JSONObject().put("student_id", id(draft.student.id)).put("incident_date", draft.date.toString())
            .put("severity", draft.severity).put("description", draft.description).put("parent_notified", draft.parentNotified)
        if (draft.location.isNotBlank()) body.put("location", draft.location)
        val saved = call("incidents", body = body) as JSONObject
        id(saved.getString("id"))
        check(saved.getString("student_id") == draft.student.id && saved.getString("reported_by") == id(actorId) &&
            saved.getString("severity") == draft.severity && saved.getString("incident_date") == draft.date.toString()) {
            "Kaydedilen olay seçili öğrenci ve bildiren kişiyle eşleşmiyor."
        }
    }

    suspend fun resolve(incident: GuidanceIncidentRow, notes: String?) {
        val result = call("incidents/${id(incident.id)}/resolve",
            body = JSONObject().put("resolution_notes", notes ?: JSONObject.NULL), method = "PUT") as JSONObject
        check(result.getString("id") == incident.id && result.getString("student_id") == incident.studentId &&
            result.getBoolean("resolved") && result.text("resolution_notes")?.takeUnless(String::isBlank) == notes) {
            "Sunucudaki çözüm notu eşleşmiyor. Olay kaydını yeniden yükleyin."
        }
    }
}
