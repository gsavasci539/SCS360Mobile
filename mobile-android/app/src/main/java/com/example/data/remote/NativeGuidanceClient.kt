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
internal data class GuidanceSessionRow(
    val id: String, val studentName: String?, val counselorName: String?, val type: String, val date: String,
    val confidentiality: String, val summary: String?, val actionPlan: String?, val followUp: String?,
)
internal data class GuidanceSessionDraft(
    val student: GuidanceChoice, val counselor: GuidanceChoice, val type: String, val date: LocalDate,
    val confidentiality: String, val summary: String, val actionPlan: String, val followUp: LocalDate?,
)

internal class NativeGuidanceClient {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private fun id(value: String): String = UUID.fromString(value).toString()
    private fun JSONArray.rows() = (0 until length()).map(::getJSONObject)
    private fun JSONObject.text(key: String) = if (isNull(key)) null else getString(key)

    private suspend fun call(resource: String, query: List<Pair<String, String>> = emptyList(), body: JSONObject? = null): Any =
        scope.io {
            val url = (base + "guidance/" + resource).toHttpUrl().newBuilder().apply {
                query.forEach { (key, value) -> addQueryParameter(key, value) }
            }.build()
            val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope)
            if (body != null) request.post(body.toString().toRequestBody("application/json".toMediaType()))
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

    suspend fun sessions(student: GuidanceChoice?, offset: Int, limit: Int): List<GuidanceSessionRow> {
        val query = buildList {
            add("offset" to offset.toString())
            add("limit" to limit.toString())
            student?.let { add("student_id" to id(it.id)) }
        }
        return (call("sessions", query) as JSONArray).rows().map { row ->
            val studentId = id(row.getString("student_id"))
            check(student == null || studentId == student.id) { "Görüşme seçili öğrenciyle eşleşmiyor." }
            GuidanceSessionRow(id(row.getString("id")), row.text("student_name"), row.text("counselor_name"),
                row.getString("session_type"), row.getString("session_date"), row.getString("confidentiality_level"),
                row.text("summary"), row.text("action_plan"), row.text("follow_up_date"))
        }
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
}
