package com.example.data.remote

import com.example.data.model.SubscriptionInvoice
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

enum class PlatformInvoiceOperation(val label: String) {
    SETTLE("Banka ödemesini doğrula"),
    APPLY("Ödemeyi üyeliğe uygula"),
    REFUND_RECORDED("Dış iadeyi kaydet"),
}

data class PlatformInvoiceDraft(
    val operation: PlatformInvoiceOperation,
    val requestId: String,
    val reference: String,
    val amount: String,
    val paidAt: String,
    val periodStart: String,
    val periodEnd: String,
    val reason: String,
) {
    internal fun payload(invoice: SubscriptionInvoice): JSONObject {
        val body = JSONObject()
        if (operation == PlatformInvoiceOperation.SETTLE) {
            require(invoice.status == "PENDING" && invoice.can_cancel) { "Ödeme talebi banka kaydına açık değil." }
            require(reference.trim().length in 1..240) { "Banka dekont referansını girin." }
            val received = amount.trim().replace(',', '.').toBigDecimalOrNull()
            require(received != null && received.scale() in 0..2 && received.compareTo(BigDecimal.valueOf(invoice.amount)) == 0) {
                "Alınan tutar talepteki tutarla eşleşmelidir."
            }
            body.put("payment_reference", reference.trim()).put("amount", received.toPlainString()).put("currency", invoice.currency)
            if (paidAt.isNotBlank()) {
                val timestamp = runCatching { OffsetDateTime.parse(paidAt.trim()).toInstant().toString() }.getOrNull()
                requireNotNull(timestamp) { "Ödeme zamanını saat dilimiyle birlikte girin." }
                body.put("paid_at", timestamp)
            }
            return body
        }
        require(invoice.status == "PAID_REVIEW") { "Ödeme uzlaştırma beklemiyor." }
        require(reason.trim().length in 10..1000) { "Kontrol açıklaması 10 ile 1000 karakter arasında olmalıdır." }
        body.put("request_id", UUID.fromString(requestId).toString()).put("action", operation.name).put("reason", reason.trim())
        when (operation) {
            PlatformInvoiceOperation.APPLY -> {
                val dates = runCatching { LocalDate.parse(periodStart.trim()) to LocalDate.parse(periodEnd.trim()) }.getOrNull()
                require(dates != null && dates.second > dates.first) { "Başlangıç ve daha ileri bitiş tarihini YYYY-AA-GG biçiminde girin." }
                body.put("period_start", dates.first.atStartOfDay().toInstant(ZoneOffset.UTC).toString())
                    .put("period_end", dates.second.atStartOfDay().toInstant(ZoneOffset.UTC).toString())
            }
            PlatformInvoiceOperation.REFUND_RECORDED -> {
                require(reference.trim().length in 1..240) { "Kontrol edilen dış iadenin referansını girin." }
                body.put("refund_reference", reference.trim())
            }
            PlatformInvoiceOperation.SETTLE -> error("Unexpected receipt operation")
        }
        return body
    }
}
