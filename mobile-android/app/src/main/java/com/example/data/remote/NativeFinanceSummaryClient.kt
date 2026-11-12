package com.example.data.remote

import com.example.data.model.RecordedCollectionMonth
import com.example.data.model.RecordedFinanceSummary
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class FinanceSummaryHttpException(val code: Int) : IOException("Finans özeti yüklenemedi (HTTP $code)")

class NativeFinanceSummaryClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val branch = ApiClientProvider.interceptor.branchId
    private val client = ApiClientProvider.createOkHttpClient()

    suspend fun load(branchId: String?): RecordedFinanceSummary = scope.io {
        val selected = branchId?.takeUnless { it == "all" || it.isBlank() }
        require(selected == branch) { "Şube seçimi değişti; yeniden yükleyin" }
        selected?.let { UUID.fromString(it) }
        val url = base + "ai/payment-forecast?months=6" + (selected?.let { "&branch_id=$it" } ?: "")
        client.newCall(Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).build()).execute().use { reply ->
            if (!reply.isSuccessful) throw FinanceSummaryHttpException(reply.code)
            val envelope = JSONObject(reply.body?.string() ?: "null")
            val data = if (envelope.has("success")) {
                check(envelope.getBoolean("success")) { "Sunucu finans özetini doğrulamadı" }
                envelope.getJSONObject("data")
            } else envelope
            check(data.getString("analysis_type") == "recorded_finance" && !data.getBoolean("forecast_available"))
            val echoed = if (data.isNull("branch_id")) null else data.getString("branch_id")
            check(data.has("branch_id") && echoed == selected) { "Finans özeti şubesi doğrulanamadı" }
            val asOf = LocalDate.parse(data.getString("as_of"))
            fun amount(row: JSONObject, key: String): BigDecimal? {
                check(row.has(key)) { "Eksik finans alanı" }
                if (row.isNull(key)) return null
                return row.getString(key).toBigDecimal().also {
                    check(it >= BigDecimal.ZERO && it <= BigDecimal("9999999999.99") && it.stripTrailingZeros().scale() <= 2)
                }
            }
            fun count(row: JSONObject, key: String) = row.getInt(key).also { check(it >= 0) }
            val history = data.getJSONArray("historical_monthly")
            val rows = (0 until history.length()).map { index ->
                val row = history.getJSONObject(index)
                val month = YearMonth.parse(row.getString("month"))
                check(month <= YearMonth.from(asOf))
                RecordedCollectionMonth(month.toString(), amount(row, "collected_amount"), count(row, "payment_count"))
            }
            check(rows.map { it.month }.distinct().size == rows.size)
            val rate = amount(data, "collection_rate")?.also { check(it <= BigDecimal(100)) }
            RecordedFinanceSummary(asOf.toString(), echoed, amount(data, "total_debt"), amount(data, "total_paid"),
                amount(data, "outstanding_debt"), amount(data, "overdue_amount"), amount(data, "collected_total"), rate,
                count(data, "debt_count"), count(data, "payment_count"), data.getBoolean("requires_review"), rows)
        }
    }
}
