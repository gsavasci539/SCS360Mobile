package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.OutputStream
import java.util.UUID

internal enum class ChatAuditStatus(val label: String) {
    NEEDS_REVIEW("İnceleme gerekiyor"), VERIFIED("Doğrulanmış"), CORRECTED("Düzeltilmiş"),
}

internal enum class ChatAuditExport(val code: String, val label: String, val extension: String) {
    JSONL("jsonl", "JSONL", "jsonl"), ALPACA("alpaca", "Alpaca JSON", "json"),
    LLAMA("llama", "Llama metni", "txt"), CSV("csv", "CSV", "csv"),
}

internal data class ChatAuditEntry(
    val id: String, val sender: String, val role: String, val question: String, val response: String,
    val topic: String, val intent: String, val sentiment: String, val createdAt: String,
    val rating: Int, val status: ChatAuditStatus, val correction: String?, val trainingReady: Boolean,
)

internal data class ChatAuditPage(val entries: List<ChatAuditEntry>, val total: Int)
internal data class ChatAuditOverview(
    val total: Int, val averageLatency: Double?, val averageQuality: Double?, val rated: Int,
    val trainingReady: Int, val distributions: Map<String, Map<String, Int>>,
)

internal class NativeChatAuditClient(private val organization: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = (ApiClientProvider.activeBaseUrl + "v1/chatbot/ai/").toHttpUrl()
    private val http = ApiClientProvider.createOkHttpClient(unwrapApiData = false)

    private fun request(path: String, parameters: Map<String, String>, payload: JSONObject? = null): Request {
        val url = checkNotNull(base.resolve(path)).newBuilder().apply {
            parameters.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        return Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).apply {
            if (payload != null) put(payload.toString().toRequestBody("application/json".toMediaType()))
        }.build()
    }

    private suspend fun json(path: String, parameters: Map<String, String> = emptyMap(), payload: JSONObject? = null): JSONObject = scope.io {
        http.newCall(request(path, parameters, payload)).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val envelope = JSONObject(requireNotNull(response.body).string())
            check(envelope.getBoolean("success"))
            envelope.getJSONObject("data")
        }
    }

    suspend fun page(number: Int, size: Int, topic: String, status: ChatAuditStatus?): ChatAuditPage {
        val parameters = buildMap {
            put("page", number.toString())
            put("page_size", size.toString())
            if (topic.isNotBlank()) put("topic", topic.trim())
            status?.let { put("feedback_status", it.name) }
        }
        val result = json("audit-logs", parameters)
        check(result.getInt("page") == number && result.getInt("page_size") == size)
        return ChatAuditPage(entries(result), result.getInt("total"))
    }

    private fun entries(data: JSONObject): List<ChatAuditEntry> {
        val logs = data.getJSONArray("logs")
        return (0 until logs.length()).map { index ->
            val row = logs.getJSONObject(index)
            check(row.getString("organization_id") == organization) { "Denetim kaydı seçili kuruma ait değil." }
            ChatAuditEntry(UUID.fromString(row.getString("id")).toString(), row.getString("sender_name"),
                row.getString("sender_role"), row.getString("user_query"), row.getString("ai_response"),
                row.optString("topic_category").takeUnless { row.isNull("topic_category") }.orEmpty(),
                row.optString("detected_intent").takeUnless { row.isNull("detected_intent") }.orEmpty(), row.getString("sentiment"),
                row.getString("created_at"), row.getInt("quality_rating"), ChatAuditStatus.valueOf(row.getString("feedback_status")),
                row.optString("corrected_response").takeUnless { row.isNull("corrected_response") },
                row.getBoolean("is_fine_tuning_ready"))
        }
    }

    suspend fun feedback(id: String, rating: Int, status: ChatAuditStatus, text: String): ChatAuditEntry {
        val identifier = UUID.fromString(id).toString()
        require(rating in 1..5)
        val corrected = text.trim().takeIf { status == ChatAuditStatus.CORRECTED }
        require(corrected == null || corrected.isNotEmpty())
        val ready = status == ChatAuditStatus.VERIFIED
        val body = JSONObject().put("quality_rating", rating).put("feedback_status", status.name)
            .put("corrected_response", corrected ?: JSONObject.NULL).put("is_fine_tuning_ready", ready)
        check(json("audit-logs/$identifier/verify", payload = body).getString("log_id") == identifier)
        val current = entries(json("audit-logs", mapOf("record_id" to identifier, "page" to "1", "page_size" to "1"))).single()
        check(current.id == identifier && current.rating == rating && current.status == status &&
            current.correction == corrected && current.trainingReady == ready) { "Kaydedilen geri bildirim doğrulanamadı." }
        return current
    }

    suspend fun overview(): ChatAuditOverview {
        val data = json("analytics/overview")
        val summary = data.getJSONObject("overview")
        val distributions = data.getJSONObject("distributions")
        fun average(key: String) = if (summary.isNull(key)) null else summary.getDouble(key)
        return ChatAuditOverview(summary.getInt("total_logs"), average("avg_latency_ms"), average("avg_quality_rating"),
            summary.getInt("quality_rated_count"), summary.getInt("finetuning_ready_count"),
            listOf("topic", "intent", "sentiment", "feedback_status").associateWith { section ->
                val group = distributions.getJSONObject(section)
                group.keys().asSequence().associateWith(group::getInt)
            })
    }

    suspend fun export(format: ChatAuditExport, status: ChatAuditStatus?, destination: () -> OutputStream) = scope.io {
        val parameters = buildMap {
            put("format", format.code)
            status?.let { put("feedback_status", it.name) }
        }
        http.newCall(request("export-dataset", parameters)).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val body = requireNotNull(response.body)
            check(body.contentLength() != 0L) { "Dışa aktarılacak kayıt bulunamadı." }
            scope.ensureCurrent()
            destination().use { sink ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(16_384)
                    var count = input.read(buffer)
                    while (count != -1) {
                        scope.ensureCurrent()
                        sink.write(buffer, 0, count)
                        count = input.read(buffer)
                    }
                }
            }
        }
    }
}
