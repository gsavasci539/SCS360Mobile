package com.example.data.payment.iyzico
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// Compatibility result type. Hosted payments settle only on the server.
data class IyzicoPaymentResult(
    val isSuccess: Boolean,
    val status: String,
    val paymentId: String?,
    val conversationId: String?,
    val authCode: String?,
    val paidAmount: Double,
    val currency: String = "TRY",
    val installment: Int = 1,
    val cardType: String = "",
    val cardAssociation: String = "",
    val cardFamily: String = "",
    val fraudStatus: Int = 0,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val rawJson: String? = null,
    val timestamp: String = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("tr", "TR")).format(Date())
)