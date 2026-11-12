package com.example.data.remote

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.UUID

data class CashDebt(val id: String, val amount: BigDecimal, val paid: BigDecimal, val status: String) {
    val remaining: BigDecimal get() = amount - paid
}
data class CashPaymentAck(val paymentId: String, val receiptId: String, val receiptNumber: String, val remainingStudentDebt: BigDecimal)
class CashCollectionHttpException(val code: Int) : java.io.IOException("Tahsilat yanıtı HTTP $code")

/** One explicit ledger collection. Unknown responses retain the same durable UUID/body. */
class NativeCashCollection(context: Context, actorId: String, val debtId: String) {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val workspace = ApiClientProvider.interceptor.workspaceOrganizationId.orEmpty()
    private val branch = ApiClientProvider.interceptor.branchId.orEmpty()
    private val actor = UUID.fromString(actorId).toString()
    private val prefs = context.getSharedPreferences("cash_collection_requests_v1", Context.MODE_PRIVATE)
    private val key = MessageDigest.getInstance("SHA-256").digest("$base|$actor|$workspace|$branch|$debtId".toByteArray())
        .joinToString("") { "%02x".format(it) }
    private val client = ApiClientProvider.createOkHttpClient()
    private val mutex = locks.getOrPut(key) { Mutex() }
    private companion object { val locks = java.util.concurrent.ConcurrentHashMap<String, Mutex>() }
    private var debt: CashDebt? = null
    private var body: String? = null
    private var ack: CashPaymentAck? = null
    val hasPending: Boolean get() = body != null && ack == null
    fun current() = scope.current()
    fun pendingAmount(): String? = body?.let { JSONObject(it).getString("amount") }
    fun pendingMethod(): String? = body?.let { JSONObject(it).getString("payment_method") }

    init { require(UUID.fromString(debtId).toString() == debtId) }
    private suspend fun request(path: String, payload: String? = null): JSONObject = scope.io {
        val builder = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (payload != null) builder.post(payload.toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use { reply ->
            if (!reply.isSuccessful) throw CashCollectionHttpException(reply.code)
            val value = JSONObject(reply.body?.string() ?: "null")
            if (value.has("success")) {
                check(value.getBoolean("success")) { "Sunucu tahsilatı onaylamadı" }; value.getJSONObject("data")
            } else value
        }
    }
    private fun money(value: String) = value.toBigDecimal().also {
        require(it >= BigDecimal.ZERO && it <= BigDecimal("9999999999.99") && it.stripTrailingZeros().scale() <= 2)
    }
    suspend fun load(): CashDebt = mutex.withLock {
        val value = request("debts/$debtId")
        check(value.getString("id") == debtId)
        val loaded = CashDebt(debtId, money(value.getString("amount")), money(value.getString("paid_amount")), value.getString("status"))
        check(loaded.amount > BigDecimal.ZERO && loaded.remaining >= BigDecimal.ZERO)
        debt = loaded
        body = prefs.getString(key, null)?.also { stored ->
            val saved = JSONObject(stored)
            check(saved.getString("debt_id") == debtId); UUID.fromString(saved.getString("request_id"))
            check(saved.getString("payment_method") in setOf("CASH", "BANK_TRANSFER", "EFT"))
            require(money(saved.getString("amount")) > BigDecimal.ZERO)
        }
        loaded
    }
    suspend fun collect(amountText: String, method: String): CashPaymentAck = mutex.withLock {
        scope.ensureCurrent()
        ack?.let { return@withLock it }
        val amount = money(amountText).setScale(2)
        require(amount > BigDecimal.ZERO && method in setOf("CASH", "BANK_TRANSFER", "EFT")) { "Geçerli tutar ve kayıt yöntemi seçin" }
        if (body == null) body = prefs.getString(key, null)
        if (body == null) {
            val loaded = checkNotNull(debt) { "Önce güncel borcu yükleyin" }
            val latest = request("debts/$debtId")
            check(latest.getString("id") == debtId && money(latest.getString("amount")) == loaded.amount &&
                money(latest.getString("paid_amount")) == loaded.paid && latest.getString("status") == loaded.status) {
                "Borç değişmiş; güncel kaydı yeniden yükleyin"
            }
            require(loaded.status != "CANCELLED" && amount <= loaded.remaining) { "Tutar kalan borcu aşamaz" }
            val pending = JSONObject().put("debt_id", debtId).put("amount", amount.toPlainString())
                .put("payment_method", method).put("request_id", UUID.randomUUID().toString()).toString()
            check(prefs.edit().putString(key, pending).commit()) { "İstek kimliği saklanamadı; tahsilat gönderilmedi" }
            body = pending
        }
        val saved = JSONObject(body!!)
        check(saved.getString("amount") == amount.toPlainString() && saved.getString("payment_method") == method) {
            "Bekleyen isteğin tutarı/yöntemi değiştirilemez; önce aynı isteğin sonucunu doğrulayın"
        }
        val result = request("payments", body)
        val paymentId = UUID.fromString(result.getString("payment_id")).toString()
        check(paymentId == saved.getString("request_id")) { "Tahsilat kimliği eşleşmedi" }
        CashPaymentAck(paymentId, UUID.fromString(result.getString("receipt_id")).toString(),
            result.getString("receipt_number").also { check(it.isNotBlank()) }, money(result.getString("remaining_debt"))).also { ack = it }
    }
    /** Only an acknowledged operation may be dismissed as complete. */
    fun finish() {
        scope.ensureCurrent(); check(ack != null)
        check(prefs.edit().remove(key).commit()); body = null
    }
}
