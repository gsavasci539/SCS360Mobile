package com.example.data.remote

import okhttp3.Request
import org.json.JSONObject
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class VerifiedReceipt(val id: String, val paymentId: String, val number: String, val issuedAt: String,
    val status: String, val paymentAmount: BigDecimal, val remainingStudentDebt: BigDecimal)
class ReceiptHttpException(val code: Int) : java.io.IOException("Makbuz isteği HTTP $code")

/** Every receipt is selected through an authorized payment linked to this debt. */
class NativeReceiptClient(private val debtId: String, private val parentStudentId: String? = null) {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val client = ApiClientProvider.createOkHttpClient()
    private val receipts = mutableMapOf<String, VerifiedReceipt>()
    private val parentPath = parentStudentId?.let { "parent/child/${UUID.fromString(it)}/debts/$debtId/receipts" }
    init { UUID.fromString(debtId) }
    fun current() = scope.current()
    private suspend fun json(path: String): JSONObject = scope.io {
        client.newCall(Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope).build()).execute().use { reply ->
            if (!reply.isSuccessful) throw ReceiptHttpException(reply.code)
            val value = JSONObject(reply.body?.string() ?: "null")
            if (value.has("success")) { check(value.getBoolean("success")); value.getJSONObject("data") } else value
        }
    }
    private fun money(row: JSONObject, key: String) = row.getString(key).toBigDecimal().also {
        check(it >= BigDecimal.ZERO && it <= BigDecimal("9999999999.99") && it.stripTrailingZeros().scale() <= 2)
    }
    private fun verifiedReceipt(row: JSONObject, receiptId: String, paymentId: String, amount: BigDecimal): VerifiedReceipt {
        check(row.getString("id") == receiptId && row.getString("payment_id") == paymentId)
        val issued = row.getString("issued_at")
        java.time.format.DateTimeFormatter.ISO_DATE_TIME.parse(issued)
        val status = row.getString("status")
        check(status in setOf("ACTIVE", "CANCELLED"))
        val number = row.getString("receipt_number").also { check(it.isNotBlank()) }
        return VerifiedReceipt(receiptId, paymentId, number, issued, status, amount, money(row, "remaining_debt"))
    }
    suspend fun load(): List<VerifiedReceipt> = scope.io {
        receipts.clear()
        var page = 1; var total: Int? = null
        val seen = mutableSetOf<String>(); val result = mutableListOf<VerifiedReceipt>()
        while (true) {
            val data = json(parentPath?.let { "$it?page=$page&page_size=100" }
                ?: "payments?debt_id=$debtId&status=ALL&page=$page&page_size=100")
            if (parentPath != null) {
                check(data.getString("student_id") == parentStudentId && data.getString("debt_id") == debtId)
            }
            val count = data.getInt("total"); check(count in 0..100000 && (total == null || total == count)); total = count
            val rows = data.getJSONArray("items")
            for (i in 0 until rows.length()) {
                val payment = rows.getJSONObject(i)
                if (parentPath != null) {
                    val id = UUID.fromString(payment.getString("payment_id")).toString()
                    val receiptId = UUID.fromString(payment.getString("id")).toString()
                    check(seen.add(id))
                    result.add(verifiedReceipt(payment, receiptId, id, money(payment, "amount")))
                } else {
                    val id = UUID.fromString(payment.getString("id")).toString()
                    check(seen.add(id) && payment.getString("debt_id") == debtId)
                    if (payment.isNull("receipt_id")) continue
                    val receiptId = UUID.fromString(payment.getString("receipt_id")).toString()
                    result.add(verifiedReceipt(json("receipts/$receiptId"), receiptId, id, money(payment, "amount")))
                }
            }
            check(seen.size <= count)
            if (seen.size == count) break
            check(rows.length() > 0); page++
        }
        result.forEach { check(receipts.put(it.id, it) == null) }
        result
    }
    suspend fun pdf(receipt: VerifiedReceipt): ByteArray = scope.io {
        check(receipts[receipt.id] == receipt) { "Önce yetkili makbuz listesini yükleyin" }
        val path = "${parentPath ?: "receipts"}/${receipt.id}/pdf"
        client.newCall(Request.Builder().url(base + path)
            .tag(NativeRequestScope::class.java, scope).build()).execute().use { reply ->
            if (!reply.isSuccessful) throw ReceiptHttpException(reply.code)
            check(reply.header("Content-Type")?.substringBefore(';') == "application/pdf")
            val body = checkNotNull(reply.body); check(body.contentLength() <= 20_000_000)
            val bytes = body.byteStream().use { input ->
                val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while (true) { val n = input.read(buffer); if (n < 0) break; check(output.size() + n <= 20_000_000); output.write(buffer, 0, n) }
                output.toByteArray()
            }
            check(bytes.size in 5..20_000_000 && bytes.copyOfRange(0, 5).contentEquals("%PDF-".toByteArray()))
            bytes
        }
    }
}
