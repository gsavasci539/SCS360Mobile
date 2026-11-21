package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class SurveyQuestionRecord(val id: String, val text: String, val type: String,
    val options: List<String>, val required: Boolean, val order: Int)
internal data class NativeSurveyRecord(val id: String, val title: String, val description: String,
    val audience: String, val startDate: String, val endDate: String, val anonymous: Boolean,
    val status: String, val submitted: Boolean, val questions: List<SurveyQuestionRecord>,
    val organization: String, val responseCount: Int)
internal data class SurveyDraft(val title: String = "", val description: String = "", val audience: String = "PARENTS",
    val startDate: String = "", val endDate: String = "", val anonymous: Boolean = false)
internal data class SurveyQuestionDraft(val text: String = "", val type: String = "TEXT",
    val options: List<String> = emptyList(), val required: Boolean = true, val order: Int = 0)

internal class NativeSurveyClient(private val organization: String, student: String? = null) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val parentPath = student?.let { "parent/child/${id(it)}/surveys" }
    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else getString(key)
    private fun JSONArray.objects() = (0 until length()).map(::getJSONObject)

    private suspend fun call(path: String, query: Map<String, String> = emptyMap(), payload: JSONObject? = null,
        method: String = if (payload == null) "GET" else "POST"): Any = scope.io {
        val url = (base + path).toHttpUrl().newBuilder().apply {
            query.forEach { (name, value) -> addQueryParameter(name, value) }
        }.build()
        val body = payload?.toString()?.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).method(method, body).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val document = JSONObject(requireNotNull(response.body).string())
            check(document.getBoolean("success"))
            document.opt("data") ?: JSONObject.NULL
        }
    }

    private fun question(row: JSONObject, survey: String): SurveyQuestionRecord {
        check(row.getString("survey_id") == survey)
        val options = row.optJSONArray("options")
        return SurveyQuestionRecord(id(row.getString("id")), row.getString("question_text"),
            row.getString("question_type"), options?.let { (0 until it.length()).map(it::getString) }.orEmpty(),
            row.getBoolean("is_required"), row.getInt("order_index"))
    }

    private fun record(row: JSONObject): NativeSurveyRecord {
        val identifier = id(row.getString("id"))
        val owner = id(row.getString("organization_id"))
        check(owner == organization)
        return NativeSurveyRecord(identifier, row.getString("title"), row.text("description"), row.getString("target_audience"),
            row.getString("start_date"), row.getString("end_date"), row.getBoolean("is_anonymous"),
            row.getString("status"), row.optBoolean("submitted"),
            row.optJSONArray("questions")?.objects()?.map { question(it, identifier) }.orEmpty(),
            owner, row.getInt("response_count"))
    }

    suspend fun list(page: Int, participate: Boolean): List<NativeSurveyRecord> {
        val path = if (participate) parentPath ?: "surveys/my-surveys" else "surveys/surveys"
        check(participate || parentPath == null)
        return (call(path, mapOf("limit" to "21", "offset" to (page * 20).toString())) as JSONArray).objects().map(::record)
    }

    suspend fun detail(identifier: String) =
        record(call("surveys/surveys/${id(identifier)}") as JSONObject).also { check(it.id == identifier) }

    suspend fun questions(identifier: String): List<SurveyQuestionRecord> =
        (call("surveys/questions", mapOf("survey_id" to id(identifier), "limit" to "100")) as JSONArray)
            .objects().map { question(it, identifier) }

    suspend fun results(identifier: String) = (call("surveys/surveys/${id(identifier)}/results") as JSONObject)
        .also { check(it.getString("survey_id") == identifier) }

    suspend fun responses(identifier: String, page: Int): List<JSONObject> =
        (call("surveys/responses", mapOf("survey_id" to id(identifier), "limit" to "21",
            "offset" to (page * 20).toString())) as JSONArray).objects().onEach { check(it.getString("survey_id") == identifier) }

    suspend fun save(draft: SurveyDraft, request: UUID, existing: String?) {
        val expected = existing?.let(::id) ?: request.toString()
        val body = JSONObject().put("organization_id", id(organization)).put("title", draft.title)
            .put("description", draft.description.ifBlank { null } ?: JSONObject.NULL).put("target_audience", draft.audience)
            .put("start_date", draft.startDate).put("end_date", draft.endDate).put("is_anonymous", draft.anonymous)
        if (existing == null) body.put("request_id", expected)
        val saved = if (existing == null) call("surveys/surveys", payload = body)
        else call("surveys/surveys/$expected", payload = body, method = "PUT")
        check((saved as JSONObject).getString("id") == expected)
        val record = detail(expected)
        check(record.status == "DRAFT" && record.title == draft.title && record.description == draft.description &&
            record.audience == draft.audience && record.startDate == draft.startDate && record.endDate == draft.endDate &&
            record.anonymous == draft.anonymous)
    }

    suspend fun saveQuestion(survey: String, draft: SurveyQuestionDraft, request: UUID) {
        val expected = request.toString()
        val body = JSONObject().put("survey_id", id(survey)).put("question_text", draft.text).put("question_type", draft.type)
            .put("options", draft.options.takeIf { it.isNotEmpty() }?.let(::JSONArray) ?: JSONObject.NULL)
            .put("is_required", draft.required).put("order_index", draft.order).put("request_id", expected)
        val ack = call("surveys/questions", payload = body) as JSONObject
        check(ack.getString("id") == expected)
        check(questions(survey).single { it.id == expected } ==
            SurveyQuestionRecord(expected, draft.text, draft.type, draft.options, draft.required, draft.order))
    }

    suspend fun deleteQuestion(survey: String, question: String) {
        val ack = call("surveys/questions/${id(question)}", method = "DELETE") as JSONObject
        check(ack.getString("id") == question && questions(survey).none { it.id == question })
    }

    suspend fun status(survey: String, publish: Boolean) {
        val state = if (publish) "ACTIVE" else "CLOSED"
        val operation = if (publish) "publish" else "close"
        val ack = call("surveys/surveys/${id(survey)}/$operation", payload = JSONObject()) as JSONObject
        check(ack.getString("id") == survey && ack.getString("status") == state && detail(survey).status == state)
    }

    suspend fun submit(survey: String, answers: JSONObject) {
        val identifier = id(survey)
        val endpoint = parentPath?.let { "$it/$identifier/respond" } ?: "surveys/responses"
        val payload = JSONObject().put("response_data", answers)
        if (parentPath == null) payload.put("survey_id", identifier)
        try {
            val ack = call(endpoint, payload = payload) as JSONObject
            id(ack.getString("id"))
        } catch (failure: RegistryHttpException) {
            if (failure.code != 409) throw failure
        }
        val readPath = parentPath ?: "surveys/my-surveys"
        val stored = call("$readPath/$identifier/participation") as JSONObject
        check(stored.getString("survey_id") == identifier && stored.getBoolean("submitted"))
    }
}
