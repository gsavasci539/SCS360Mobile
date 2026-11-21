package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject

internal enum class RecordedInsight(val route: String, val kind: String, val title: String) {
    OVERVIEW("insights-report", "RECORDED_OVERVIEW", "Kayıtlı genel değerlendirme"),
    GRADES("ai-academic-predictions", "RECORDED_GRADES", "Kayıtlı not analizi"),
    ATTENDANCE("ai-attendance-predictions", "RECORDED_ATTENDANCE", "Devam özeti"),
    OPERATIONS("ai-operational-optimization", "RECORDED_OPERATIONS", "Kayıtlı operasyon özeti"),
    PARENTS("ai-parent-sentiment", "recorded_parent_engagement", "Veli iletişimi"),
    FINANCE("payment-forecast", "recorded_finance", "Kayıtlı finans özeti")
}

internal class NativeRecordedInsightsClient {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val client = ApiClientProvider.createOkHttpClient()

    suspend fun load(insight: RecordedInsight, days: Int = 30): JSONObject = scope.io {
        require(insight != RecordedInsight.FINANCE)
        require(days in 1..365)
        check(ApiClientProvider.interceptor.branchId.isNullOrBlank()) { "Özet için tüm şubeleri seçin" }
        val url = (base + "ai/" + insight.route).toHttpUrl().newBuilder().apply {
            if (insight != RecordedInsight.OPERATIONS) addQueryParameter("days", days.toString())
        }.build()
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).build()
        client.newCall(request).execute().use { reply ->
            if (!reply.isSuccessful) throw RegistryHttpException(reply.code)
            val envelope = JSONObject(requireNotNull(reply.body).string())
            check(envelope.getBoolean("success"))
            envelope.getJSONObject("data").also {
                val parent = insight == RecordedInsight.PARENTS
                check(it.getString(if (parent) "analysis_type" else "analysis_kind") == insight.kind)
                check(!it.getBoolean(if (parent) "sentiment_available" else "prediction_available"))
                check(it.has("ai_model") && it.isNull("ai_model"))
            }
        }
    }
}
