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

internal data class SecurityStudent(val id: String, val name: String, val active: Boolean)
internal data class SecurityOptions(val students: List<SecurityStudent>, val today: LocalDate, val more: Boolean)
internal data class SecurityCredential(val id: String, val studentId: String, val student: String, val barcode: String,
    val status: String, val expiresAt: String)
internal data class RecordedGateEvent(val id: String, val studentId: String?, val person: String, val operator: String,
    val method: String, val direction: String, val status: String, val time: String, val location: String, val notes: String)
internal data class RecordedGateStats(val entries: Int, val exits: Int, val inside: Int, val review: Int)
internal data class RecordedAuditEvent(val id: String, val action: String, val entity: String, val time: String, val actor: String)

internal class NativeSecurityClient(selectedBranch: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val branch = selectedBranch.takeUnless { it == "all" || it.isBlank() }?.let(::id)

    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else getString(key)
    private fun JSONArray.rows() = (0 until length()).map(::getJSONObject)
    private fun matchesBranch(row: JSONObject) = branch == null || row.text("branch_id") == branch
    fun ensureCurrent() = scope.ensureCurrent()

    private suspend fun call(path: String, query: Map<String, String?> = emptyMap(), body: JSONObject? = null,
        svg: Boolean = false): Any = scope.io {
        val url = (base + "security/" + path).toHttpUrl().newBuilder().apply {
            branch?.let { addQueryParameter("branch_id", it) }
            query.forEach { (key, value) -> value?.let { addQueryParameter(key, it) } }
        }.build()
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).apply {
            body?.let { post(it.toString().toRequestBody("application/json".toMediaType())) }
        }.build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            if (svg) {
                val source = requireNotNull(response.body)
                check(source.contentType()?.toString()?.startsWith("image/svg+xml") == true)
                val bytes = source.source()
                check(!bytes.request(512L * 1024 + 1))
                return@use bytes.readByteArray().also { check(it.isNotEmpty()) }
            }
            val parsed = JSONTokener(response.body?.string().orEmpty()).nextValue()
            if (parsed is JSONObject && parsed.has("success")) {
                check(parsed.getBoolean("success")) { "Güvenlik kaydı sunucudan doğrulanamadı." }
                parsed.get("data")
            } else parsed
        }
    }

    suspend fun options(search: String = "", selected: String? = null): SecurityOptions {
        val data = call("options", mapOf("search" to search, "student_id" to selected?.let(::id))) as JSONObject
        return SecurityOptions(data.getJSONArray("students").rows().map {
            SecurityStudent(id(it.getString("id")), it.getString("name"), it.getBoolean("active"))
        }, LocalDate.parse(data.getString("today")), data.getBoolean("more_students"))
    }

    suspend fun credentials(student: String?, limit: Int, offset: Int, record: String? = null): List<SecurityCredential> {
        val rows = call("digital-ids", mapOf("student_id" to student?.let(::id), "limit" to limit.toString(),
            "offset" to offset.toString(), "record_id" to record?.let(::id))) as JSONArray
        return rows.rows().map {
            val item = SecurityCredential(id(it.getString("id")), id(it.getString("student_id")), it.getString("student_name"),
                it.text("barcode_number"), it.getString("status"), it.text("expires_at"))
            check(matchesBranch(it) && (student == null || item.studentId == student) && (record == null || item.id == record))
            item
        }
    }

    suspend fun issue(student: String, expirationHours: Int, request: UUID) {
        val row = call("digital-id/generate", body = JSONObject().put("student_id", id(student))
            .put("expires_hours", expirationHours).put("request_id", request.toString())) as JSONObject
        check(matchesBranch(row) && row.getString("id") == request.toString() && row.getString("student_id") == student)
        check(credentials(student, 1, 0, request.toString()).single().status == "ACTIVE")
    }

    suspend fun revoke(credential: SecurityCredential) {
        val row = call("digital-ids/${id(credential.id)}/revoke", body = JSONObject()) as JSONObject
        check(matchesBranch(row) && row.getString("id") == credential.id && row.getString("status") == "REVOKED")
        check(credentials(credential.studentId, 1, 0, credential.id).single().status == "REVOKED")
    }

    suspend fun download(credential: SecurityCredential): ByteArray =
        call("digital-ids/${id(credential.id)}/qr-code", svg = true) as ByteArray

    suspend fun events(day: LocalDate?, limit: Int, offset: Int, record: String? = null): List<RecordedGateEvent> {
        val data = call("gate-logs", mapOf("start_date" to day?.toString(), "end_date" to day?.toString(),
            "limit" to limit.toString(), "offset" to offset.toString(), "record_id" to record?.let(::id))) as JSONArray
        return data.rows().map {
            val item = RecordedGateEvent(id(it.getString("id")), it.text("student_id").takeIf(String::isNotBlank)?.let(::id),
                it.text("student_name").ifBlank { it.text("user_name") }, it.text("verified_by_name"), it.text("verification_method"),
                it.getString("direction"), it.getString("status"), it.getString("created_at"), it.text("gate_location"), it.text("notes"))
            check(matchesBranch(it) && (record == null || item.id == record))
            item
        }
    }

    suspend fun totals(day: LocalDate): RecordedGateStats {
        val row = call("gate-logs/stats", mapOf("date" to day.toString())) as JSONObject
        check(row.getString("date") == day.toString())
        return RecordedGateStats(row.getInt("total_entries"), row.getInt("total_exits"),
            row.getInt("currently_in_school"), row.getInt("presence_review_required"))
    }

    suspend fun audit(limit: Int, offset: Int): List<RecordedAuditEvent> {
        val data = call("audit-logs", mapOf("limit" to limit.toString(), "offset" to offset.toString())) as JSONArray
        return data.rows().map {
            check(matchesBranch(it))
            RecordedAuditEvent(id(it.getString("id")), it.getString("action"), it.getString("module"),
                it.getString("timestamp"), it.text("user_name"))
        }
    }

    suspend fun verify(token: String, direction: String, method: String, location: String, notes: String,
        request: UUID): RecordedGateEvent? {
        val ack = call("gate-logs/verify-qr", body = JSONObject().put("token", token).put("direction", direction)
            .put("verification_method", method).put("gate_location", location.ifBlank { null } ?: JSONObject.NULL)
            .put("notes", notes.ifBlank { null } ?: JSONObject.NULL).put("request_id", request.toString())) as JSONObject
        check(ack.getString("verification_scope") == "STUDENT_ID_ONLY")
        if (!ack.getBoolean("authorized")) return null
        check(matchesBranch(ack) && ack.getString("gate_log_id") == request.toString())
        return events(null, 1, 0, request.toString()).single().also {
            check(it.status == "AUTHORIZED" && it.direction == direction && it.method == method &&
                it.studentId == ack.getString("student_id"))
        }
    }
}
