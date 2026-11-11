package com.example.data.payment

import android.content.Context
import com.example.data.remote.ApiClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.math.BigDecimal
import java.util.UUID

class SchoolCheckout(context: Context, private val debtId: String) {
    private val prefs = context.getSharedPreferences("school_checkout_v1", Context.MODE_PRIVATE)
    private val userId = com.example.data.local.SessionManager(context).getUser()?.id ?: error("Oturum gerekli")
    private val actor = ApiClientProvider.interceptor.authToken.orEmpty()
    private val baseUrl = ApiClientProvider.activeBaseUrl
    private val workspace = ApiClientProvider.interceptor.workspaceOrganizationId
    private val tenant = ApiClientProvider.interceptor.tenantId
    private val fingerprint = java.security.MessageDigest.getInstance("SHA-256")
        .digest("$baseUrl|$userId|$tenant|$workspace|$debtId".toByteArray()).joinToString("") { "%02x".format(it) }
    private var pending: JSONObject? = null
    private val client = ApiClientProvider.createOkHttpClient()
    init { UUID.fromString(debtId); UUID.fromString(userId); require(actor.isNotBlank()) { "Oturum gerekli" } }
    fun saved(): JSONObject? = prefs.getString(fingerprint, null)?.let { JSONObject(it) }
    private fun sameSession() = check(actor == ApiClientProvider.interceptor.authToken && tenant == ApiClientProvider.interceptor.tenantId && workspace == ApiClientProvider.interceptor.workspaceOrganizationId && baseUrl == ApiClientProvider.activeBaseUrl) { "Oturum değişti; ödeme ekranını yeniden açın" }
    private suspend fun call(path: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        sameSession()
        val request = Request.Builder().url(baseUrl + path).apply {
            if (body != null) post(body.toString().toRequestBody("application/json".toMediaType()))
        }.build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Ödeme isteği tamamlanamadı (HTTP ${response.code}); yeni işlem açmadan sonucu kontrol edin" }
            val parsed = JSONObject(response.body?.string() ?: "{}")
            sameSession()
            parsed.optJSONObject("data") ?: parsed
        }
    }
    suspend fun debt() = call("payments/online/debts/$debtId")
    private fun validate(result: JSONObject): JSONObject {
        val previous = saved() ?: error("Ödeme kaydı bulunamadı")
        check(result.optString("id", result.optString("conversation_id")) == previous.getString("request_id") && result.getString("debt_id") == debtId &&
            BigDecimal(result.getString("amount")).compareTo(BigDecimal(previous.getString("amount"))) == 0) { "Ödeme sonucu eşleşmiyor" }
        if (result.optString("status") == "PAID") check(!result.isNull("payment_id") && result.optString("payment_id").isNotBlank()) { "Tahsilat doğrulanamadı" }
        return result
    }
    suspend fun start(amount: String, buyer: JSONObject): JSONObject {
        sameSession()
        var previous = saved()
        if (previous == null) {
            previous = JSONObject().put("request_id", UUID.randomUUID().toString()).put("debt_id", debtId).put("amount", amount)
            check(prefs.edit().putString(fingerprint, previous.toString()).commit()) { "Ödeme kimliği saklanamadı" }
        }
        check(previous.getString("amount") == amount) { "Önceki ödemenin sonucunu kontrol edin" }
        if (pending == null) pending = JSONObject(previous.toString()).put("buyer", buyer).put("currency", "TRY").put("payment_channel", "MOBILE")
        return validate(call("payments/online/initialize", pending))
    }
    suspend fun refresh(): JSONObject? = saved()?.let { validate(call("payments/online/checkouts/${it.getString("request_id")}/refresh", JSONObject())) }
    suspend fun clearFailed() {
        val previous = saved() ?: return
        val result = validate(call("payments/online/checkouts/${previous.getString("request_id")}"))
        check(result.optString("status") in listOf("FAILED", "REFUNDED")) { "Önceki ödeme hâlâ kontrol edilmeli" }
        check(prefs.edit().remove(fingerprint).commit()) { "Ödeme kaydı güncellenemedi" }
        pending = null
    }
    suspend fun resume(): String? {
        val previous = saved() ?: return null
        val result = validate(call("payments/online/checkouts/${previous.getString("request_id")}/resume", JSONObject()))
        if (!result.optBoolean("can_resume")) return null
        val url = result.optString("checkout_url", result.optString("payment_page_url"))
        val parsed = java.net.URI(url)
        val host = parsed.host ?: error("Ödeme adresi geçersiz")
        val allowed = ((host == "api.iyzipay.com" || host == "sandbox-api.iyzipay.com") && parsed.path.startsWith("/checkoutform/")) ||
            ((host == "cpp.iyzipay.com" || host == "sandbox-cpp.iyzipay.com") && parsed.path in listOf("", "/") && !parsed.query.isNullOrBlank()) ||
            (host == "www.paytr.com" && parsed.path.startsWith("/odeme/guvenli/")) || host.endsWith(".craftgate.io") ||
            ((host == "app.sipay.com.tr" || host == "provisioning.sipay.com.tr") && parsed.path.startsWith("/ccpayment/makepayment/"))
        check(allowed && parsed.scheme == "https" && parsed.userInfo == null && parsed.fragment == null && parsed.port in listOf(-1, 443)) { "Ödeme adresi doğrulanamadı" }
        return url
    }
}
