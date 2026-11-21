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

internal data class DismissalStudent(val id: String, val name: String, val active: Boolean)
internal data class DismissalOptions(val students: List<DismissalStudent>, val today: LocalDate, val more: Boolean)
internal data class DismissalTotals(val active: Int, val pickups: Int)
internal data class DismissalGrant(
    val id: String, val studentId: String, val student: String, val status: String, val effectiveStatus: String,
    val form: DismissalGrantForm,
)
internal data class DismissalGrantForm(
    val name: String = "", val identity: String = "", val relationship: String = "", val phone: String = "",
    val from: String = "", val until: String = "", val photo: String = "", val notes: String = "",
)
internal data class DismissalRecord(val id: String, val studentId: String, val student: String,
    val adult: String, val time: String, val operator: String, val method: String, val notes: String)

internal class NativeDismissalClient(selectedBranch: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val branch = selectedBranch.takeUnless { it == "all" || it.isBlank() }?.let(::id)

    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else getString(key)
    private fun JSONArray.rows() = (0 until length()).map(::getJSONObject)

    private suspend fun call(path: String, query: Map<String, String?> = emptyMap(), method: String = "GET",
        body: JSONObject? = null): Any = scope.io {
        val url = (base + "dismissal/" + path).toHttpUrl().newBuilder().apply {
            branch?.let { addQueryParameter("branch_id", it) }
            query.forEach { (key, value) -> value?.let { addQueryParameter(key, it) } }
        }.build()
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope)
            .method(method, body?.toString()?.toRequestBody("application/json".toMediaType())).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val data = JSONTokener(response.body?.string().orEmpty()).nextValue()
            if (data is JSONObject && data.has("success")) {
                check(data.getBoolean("success")) { "Teslim işlemi doğrulanamadı." }
                data.get("data")
            } else data
        }
    }

    suspend fun options(search: String, student: String?): DismissalOptions {
        val data = call("options", mapOf("search" to search, "student_id" to student?.let(::id))) as JSONObject
        return DismissalOptions(data.getJSONArray("students").rows().map {
            DismissalStudent(id(it.getString("id")), it.getString("name"), it.getBoolean("active"))
        }, LocalDate.parse(data.getString("today")), data.getBoolean("more_students"))
    }

    suspend fun totals(student: String?, day: LocalDate): DismissalTotals {
        val data = call("summary", mapOf("student_id" to student?.let(::id), "date" to day.toString())) as JSONObject
        check(data.getString("date") == day.toString())
        return DismissalTotals(data.getInt("valid_authorizations"), data.getInt("total_pickups"))
    }

    suspend fun grants(student: String?, limit: Int, offset: Int, record: String? = null): List<DismissalGrant> {
        val rows = call("authorizations", mapOf("student_id" to student?.let(::id), "limit" to limit.toString(),
            "offset" to offset.toString(), "record_id" to record?.let(::id))) as JSONArray
        return rows.rows().map {
            val item = DismissalGrant(id(it.getString("id")), id(it.getString("student_id")), it.text("student_name"),
                it.getString("status"), it.getString("effective_status"), DismissalGrantForm(
                    it.getString("authorized_person_name"), it.text("authorized_person_id"), it.text("relationship"),
                    it.text("phone"), it.getString("valid_from"), it.text("valid_until"), it.text("photo_url"), it.text("notes")))
            check((student == null || item.studentId == student) && (record == null || item.id == record))
            item
        }
    }

    suspend fun records(student: String?, day: LocalDate?, limit: Int, offset: Int,
        record: String? = null): List<DismissalRecord> {
        val rows = call("logs", mapOf("student_id" to student?.let(::id), "start_date" to day?.toString(),
            "end_date" to day?.toString(), "limit" to limit.toString(), "offset" to offset.toString(),
            "record_id" to record?.let(::id))) as JSONArray
        return rows.rows().map {
            val item = DismissalRecord(id(it.getString("id")), id(it.getString("student_id")), it.text("student_name"),
                it.getString("pickup_person_name"), it.getString("pickup_time"), it.text("verified_by_name"),
                it.getString("verification_method"), it.text("notes"))
            check((student == null || item.studentId == student) && (record == null || item.id == record))
            item
        }
    }

    suspend fun save(student: String, form: DismissalGrantForm, requestId: UUID, editing: String?) {
        val payload = JSONObject().put("student_id", id(student)).put("request_id", requestId.toString())
            .put("authorized_person_name", form.name).put("authorized_person_id", form.identity)
            .put("valid_from", form.from)
        mapOf("relationship" to form.relationship, "phone" to form.phone, "valid_until" to form.until,
            "notes" to form.notes, "photo_url" to form.photo).forEach { (key, value) ->
            payload.put(key, value.ifBlank { null } ?: JSONObject.NULL)
        }
        val path = if (editing == null) "authorizations" else "authorizations/${id(editing)}"
        val ack = call(path, method = if (editing == null) "POST" else "PUT", body = payload) as JSONObject
        val expected = editing ?: requestId.toString()
        check(ack.getString("id") == expected && grants(student, 1, 0, expected).single().form == form) {
            "Teslim yetkisi sunucudan doğrulanamadı."
        }
    }

    suspend fun revoke(grant: DismissalGrant) {
        val ack = call("authorizations/${id(grant.id)}", method = "DELETE") as JSONObject
        check(ack.getString("id") == grant.id && ack.getString("status") == "REVOKED" &&
            grants(grant.studentId, 1, 0, grant.id).single().status == "REVOKED")
    }

    suspend fun verify(student: String, identity: String, requestId: UUID): DismissalRecord? {
        val payload = JSONObject().put("student_id", id(student)).put("pickup_person_id", identity)
            .put("request_id", requestId.toString())
        val ack = call("verify-pickup", method = "POST", body = payload) as JSONObject
        check(ack.getString("student_id") == student)
        if (!ack.getBoolean("authorized")) return null
        val saved = ack.getString("pickup_log_id")
        check(saved == requestId.toString() && ack.text("authorization_id").isNotEmpty())
        return records(student, null, 1, 0, saved).single()
    }
}
