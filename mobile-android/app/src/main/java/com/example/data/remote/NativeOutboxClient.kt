package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class RecordedOutboxMessage(val id: String, val job: String, val subject: String, val body: String,
    val recipient: String, val recipientName: String, val channel: String, val status: String, val sentAt: String,
    val scheduledAt: String, val error: String, val retries: Int)

internal class NativeOutboxClient(selectedBranch: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val branch = selectedBranch.takeUnless { it.isBlank() || it == "all" }?.let(::id)
    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else getString(key)
    private fun JSONArray.objects() = (0 until length()).map(::getJSONObject)

    private suspend fun call(path: String, query: Map<String, String> = emptyMap(), payload: JSONObject? = null,
        method: String = if (payload == null) "GET" else "POST", useBranch: Boolean = true): Any = scope.io {
        val url = (base + path).toHttpUrl().newBuilder().apply {
            if (useBranch) branch?.let { addQueryParameter("branch_id", it) }
            query.filterValues(String::isNotBlank).forEach { (name, value) -> addQueryParameter(name, value) }
        }.build()
        val body = payload?.toString()?.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).method(method, body).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val result = JSONObject(requireNotNull(response.body).string())
            check(result.getBoolean("success"))
            result.opt("data") ?: JSONObject.NULL
        }
    }

    private fun record(row: JSONObject): RecordedOutboxMessage {
        check(branch == null || row.getString("branch_id") == branch)
        return RecordedOutboxMessage(id(row.getString("id")), id(row.getString("job_id")), row.text("subject"),
            row.text("message"), row.text("recipient"), row.text("recipient_name"), row.getString("channel"),
            row.getString("status"), row.text("sent_at"), row.text("scheduled_at"), row.text("error_message"), row.getInt("retry_count"))
    }

    suspend fun messages(page: Int, search: String, channel: String, status: String): Pair<List<RecordedOutboxMessage>, Int> {
        val result = call("outbox", mapOf("page" to page.toString(), "page_size" to "20", "search" to search,
            "channel" to channel, "status" to status)) as JSONObject
        return result.getJSONArray("items").objects().map(::record) to result.getInt("total_pages")
    }

    suspend fun message(identifier: String) = record(call("outbox/${id(identifier)}") as JSONObject)
        .also { check(it.id == identifier) }

    suspend fun retry(row: RecordedOutboxMessage): RecordedOutboxMessage {
        val receipt = record(call("outbox/retry/${id(row.id)}", payload = JSONObject()) as JSONObject)
        check(receipt.id == row.id && receipt.job == row.job && receipt.status == "PENDING" && receipt.sentAt.isEmpty())
        return message(row.id).also { check(it.job == row.job) }
    }

    suspend fun delete(identifier: String) {
        call("outbox/${id(identifier)}", method = "DELETE")
        try {
            message(identifier)
            error("Gönderim kaydının kaldırıldığı doğrulanamadı.")
        } catch (failure: RegistryHttpException) {
            if (failure.code != 404) throw failure
        }
    }

    private val jobs = "communications/broadcast-jobs"
    private val templates = "communications/message-templates"
    val selectedBranch get() = branch.orEmpty()

    suspend fun broadcasts(page: Int): List<JSONObject> =
        (call(jobs, mapOf("limit" to "21", "offset" to (page * 20).toString())) as JSONArray).objects()

    suspend fun templates(page: Int): List<JSONObject> =
        (call(templates, mapOf("limit" to "21", "offset" to (page * 20).toString()), useBranch = false) as JSONArray).objects()

    suspend fun providers(): List<JSONObject> =
        (call("communications/sms-providers", useBranch = false) as JSONArray).objects()

    suspend fun broadcast(identifier: String): JSONObject = (call("$jobs/${id(identifier)}") as JSONObject)
        .also { check(it.getString("id") == identifier) }

    suspend fun broadcastOptions(branch: String = selectedBranch): JSONObject =
        call("communications/broadcast-options", mapOf("branch_id" to branch), useBranch = false) as JSONObject

    suspend fun report(identifier: String): JSONObject = (call("$jobs/${id(identifier)}/delivery-summary") as JSONObject)
        .also { check(it.getString("broadcast_job_id") == identifier) }

    suspend fun deliveries(identifier: String, page: Int): List<JSONObject> =
        (call("communications/delivery-reports", mapOf("broadcast_job_id" to id(identifier),
            "limit" to "21", "offset" to (page * 20).toString())) as JSONArray).objects()
            .onEach { check(it.getString("broadcast_job_id") == identifier) }

    suspend fun saveBroadcast(fields: JSONObject, request: UUID, existing: String?) {
        val identifier = existing?.let(::id) ?: request.toString()
        val path = if (existing == null) jobs else "$jobs/$identifier"
        val body = JSONObject(fields.toString())
        if (existing == null) body.put("request_id", identifier)
        val ack = call(path, payload = body, method = if (existing == null) "POST" else "PUT") as JSONObject
        check(ack.getString("id") == identifier)
        val saved = broadcast(identifier)
        check(saved.getString("status") == "DRAFT" && fields.keys().asSequence().filter { it != "scheduled_for" }
            .all { saved.text(it) == fields.text(it) })
        val expectedSchedule = fields.text("scheduled_for")
        val actualSchedule = saved.text("scheduled_for")
        if (expectedSchedule.isBlank()) check(actualSchedule.isBlank())
        else check(java.time.OffsetDateTime.parse(actualSchedule).toInstant() ==
            java.time.OffsetDateTime.parse(expectedSchedule).toInstant())
    }

    suspend fun queueBroadcast(identifier: String): JSONObject {
        val ack = call("$jobs/${id(identifier)}/send", payload = JSONObject()) as JSONObject
        check(ack.getString("id") == identifier && ack.getString("status") == "QUEUED")
        return broadcast(identifier).also { check(it.getString("status") in setOf("QUEUED", "COMPLETED", "FAILED", "CANCELLED")) }
    }

    suspend fun cancelBroadcast(identifier: String) {
        val ack = call("$jobs/${id(identifier)}", method = "DELETE") as JSONObject
        check(ack.getString("id") == identifier && ack.getString("status") == "CANCELLED" &&
            broadcast(identifier).getString("status") == "CANCELLED")
    }

    suspend fun saveTemplate(fields: JSONObject, request: UUID) {
        val identifier = request.toString()
        val body = JSONObject(fields.toString()).put("request_id", identifier)
        val ack = call(templates, payload = body, useBranch = false) as JSONObject
        check(ack.getString("id") == identifier)
        val readback = (call(templates, mapOf("record_id" to identifier, "limit" to "1"), useBranch = false) as JSONArray)
            .objects().single()
        check(readback.getString("id") == identifier && fields.keys().asSequence().all { readback.text(it) == fields.text(it) })
    }
}

internal fun JSONObject.outboxText(name: String) = if (isNull(name)) "" else getString(name)
