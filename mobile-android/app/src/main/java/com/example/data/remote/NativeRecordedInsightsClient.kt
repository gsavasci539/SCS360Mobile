package com.example.data.remote

import okhttp3.Request
import org.json.JSONObject

internal enum class RecordedInsight(val route: String, val kind: String, val title: String) {
    OVERVIEW("insights-report", "RECORDED_OVERVIEW", "Kayıtlı genel değerlendirme"),
    GRADES("academic-performance", "RECORDED_GRADES", "Kayıtlı not analizi"),
    OPERATIONS("ai-operational-optimization", "RECORDED_OPERATIONS", "Kayıtlı operasyon özeti"),
    FINANCE("payment-forecast", "recorded_finance", "Kayıtlı finans özeti")
}

internal class NativeRecordedInsightsClient {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val client = ApiClientProvider.createOkHttpClient()

    suspend fun load(insight: RecordedInsight): JSONObject = scope.io {
        require(insight != RecordedInsight.FINANCE)
        check(ApiClientProvider.interceptor.branchId.isNullOrBlank()) { "Özet için tüm şubeleri seçin" }
        val request = Request.Builder().url(base + "ai/" + insight.route).tag(NativeRequestScope::class.java, scope).build()
        client.newCall(request).execute().use { reply ->
            if (!reply.isSuccessful) throw RegistryHttpException(reply.code)
            val envelope = JSONObject(requireNotNull(reply.body).string())
            check(envelope.getBoolean("success"))
            envelope.getJSONObject("data").also {
                check(it.getString("analysis_kind") == insight.kind && !it.getBoolean("prediction_available"))
                check(it.has("ai_model") && it.isNull("ai_model"))
            }
        }
    }
}
