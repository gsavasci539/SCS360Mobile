package com.example.data.remote

import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID

internal data class ConsentDocumentRow(
    val id: String,
    val title: String,
    val type: String,
    val version: String,
    val content: String,
    val effectiveDate: String,
)

internal data class ConsentDecisionRow(
    val id: String,
    val studentName: String?,
    val documentTitle: String?,
    val documentVersion: String?,
    val parentName: String?,
    val status: String,
    val method: String?,
    val decidedAt: String?,
)

internal data class ConsentChoice(val id: String, val label: String)
internal data class ConsentOptions(val students: List<ConsentChoice>, val parents: List<ConsentChoice>, val moreStudents: Boolean)
internal data class ConsentDocumentDraft(val title: String, val type: String, val content: String, val version: String, val effectiveDate: String)

internal class NativeConsentClient(
    private val scope: NativeRequestScope = ApiClientProvider.interceptor.captureScope(),
) {
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Any = scope.io {
        val request = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
            .method(method, body?.toString()?.toRequestBody("application/json".toMediaType()))
        http.newCall(request.build()).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val result = JSONTokener(response.body?.string().orEmpty()).nextValue()
            if (result is JSONObject && result.has("success")) {
                check(result.getBoolean("success")) { "Sunucu izin kayıtlarını doğrulamadı." }
                result.get("data")
            } else result
        }
    }

    private fun JSONArray.rows(): List<JSONObject> = (0 until length()).map(::getJSONObject)
    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(key: String) = if (isNull(key)) null else getString(key)

    suspend fun documents(active: Boolean, offset: Int, limit: Int): List<ConsentDocumentRow> =
        (call("consents/documents?is_active=$active&offset=$offset&limit=$limit") as JSONArray).rows().map { doc ->
            ConsentDocumentRow(id(doc.getString("id")), doc.getString("title"),
                doc.getString("document_type"), doc.getString("version"), doc.getString("content"), doc.getString("effective_date"))
        }

    suspend fun decisions(offset: Int, limit: Int): List<ConsentDecisionRow> =
        (call("consents/student-consents?offset=$offset&limit=$limit") as JSONArray).rows().map { decision ->
            ConsentDecisionRow(id(decision.getString("id")), decision.text("student_name"),
                decision.text("document_title"), decision.text("document_version"), decision.text("parent_name"),
                decision.getString("consent_status"), decision.text("consent_method"), decision.text("consented_at"))
        }

    suspend fun options(organization: String, search: String, student: ConsentChoice?): ConsentOptions {
        val query = (base + "consents/options").toHttpUrl().newBuilder()
            .addQueryParameter("organization_id", id(organization)).addQueryParameter("search", search)
        student?.let { query.addQueryParameter("student_id", id(it.id)) }
        val data = call("consents/options?${query.build().encodedQuery}") as JSONObject
        fun choices(key: String) = data.getJSONArray(key).rows().map { ConsentChoice(id(it.getString("id")), it.getString("name")) }
        return ConsentOptions(choices("students"), choices("parents"), data.getBoolean("more_students"))
    }

    suspend fun createDocument(organization: String, draft: ConsentDocumentDraft) {
        val body = JSONObject().put("organization_id", id(organization)).put("title", draft.title)
            .put("document_type", draft.type).put("content", draft.content)
            .put("version", draft.version).put("effective_date", draft.effectiveDate)
        val record = call("consents/documents", "POST", body) as JSONObject
        id(record.getString("id"))
        check(record.getString("organization_id") == organization && record.getString("title") == draft.title &&
            record.getString("document_type") == draft.type && record.getString("content") == draft.content &&
            record.getString("version") == draft.version && record.getString("effective_date") == draft.effectiveDate) {
            "Kaydedilen belge doğrulanamadı."
        }
    }

    suspend fun changeDocumentStatus(document: ConsentDocumentRow, active: Boolean) {
        val record = call("consents/documents/${id(document.id)}/status", "PATCH", JSONObject().put("is_active", active)) as JSONObject
        check(record.getString("id") == document.id && record.getBoolean("is_active") == active) { "Belgenin durumu doğrulanamadı." }
    }

    suspend fun saveDecision(document: ConsentDocumentRow, student: ConsentChoice, parent: ConsentChoice, status: String, method: String) {
        require(status in setOf("APPROVED", "REJECTED") && method in setOf("MANUAL", "PAPER"))
        val body = JSONObject().put("consent_document_id", id(document.id)).put("student_id", id(student.id))
            .put("parent_id", id(parent.id)).put("consent_status", status).put("consent_method", method)
        val record = call("consents/student-consents", "POST", body) as JSONObject
        id(record.getString("id"))
        check(record.getString("student_id") == student.id && record.getString("consent_document_id") == document.id &&
            record.getString("consent_status") == status) { "Kaydedilen veli kararı doğrulanamadı." }
    }
}
