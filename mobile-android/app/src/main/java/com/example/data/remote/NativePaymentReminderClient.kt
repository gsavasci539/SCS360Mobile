package com.example.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal sealed interface PaymentReminderResult {
    data class Queued(val recipients: Int) : PaymentReminderResult
    data class Skipped(val reason: String) : PaymentReminderResult
}

internal class NativePaymentReminderClient(
    private val scope: NativeRequestScope = ApiClientProvider.interceptor.captureScope(),
) {
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()

    suspend fun queueSms(invoiceId: String, studentId: String): PaymentReminderResult = scope.io {
        val invoice = UUID.fromString(invoiceId).toString()
        val student = UUID.fromString(studentId).toString()
        val body = JSONObject().put("invoice_ids", JSONArray().put(invoice)).put("channel", "SMS")
        val request = Request.Builder().url(base + "payments/finances/reminders/send")
            .tag(NativeRequestScope::class.java, scope).post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val payload = JSONObject(response.body?.string().orEmpty())
            val data = if (payload.has("success")) {
                check(payload.getBoolean("success")) { "Hatırlatma sonucu doğrulanamadı." }
                payload.getJSONObject("data")
            } else payload
            val queued = data.getJSONArray("reminders")
            val skipped = data.getJSONArray("skipped")
            check(data.getInt("total_sent") == 0 && data.getInt("total_queued") == queued.length() &&
                queued.length() + skipped.length() == 1) { "Hatırlatma sonucu doğrulanamadı." }
            if (queued.length() == 1) {
                val item = queued.getJSONObject(0)
                UUID.fromString(item.getString("job_id"))
                val recipients = item.getInt("recipients")
                check(item.getString("invoice_id") == invoice && item.getString("student_id") == student &&
                    item.getString("channel") == "SMS" && item.getString("status") == "QUEUED" && recipients > 0) {
                    "Kuyruk kaydı seçili borçla eşleşmiyor."
                }
                PaymentReminderResult.Queued(recipients)
            } else {
                val item = skipped.getJSONObject(0)
                check(item.getString("invoice_id") == invoice) { "Hatırlatma sonucu seçili borçla eşleşmiyor." }
                PaymentReminderResult.Skipped(item.getString("reason"))
            }
        }
    }
}
