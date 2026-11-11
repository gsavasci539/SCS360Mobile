package com.example.data.payment.ceppos

enum class CepPosPaymentMode { NFC_CONTACTLESS, QR_FAST_PAY, MANUAL_CARD, SMS_LINK }
data class CepPosTransactionResult(
    val isSuccess: Boolean = false,
    val transactionId: String = "",
    val rrn: String = "",
    val authCode: String = "",
    val terminalId: String = "",
    val amount: Double = 0.0,
    val paymentMode: CepPosPaymentMode = CepPosPaymentMode.NFC_CONTACTLESS,
    val maskedCardNumber: String? = null,
    val cardScheme: String = "",
    val errorMessage: String? = "SoftPOS sağlayıcı SDK ve terminal kabulü yapılandırılmamış",
    val errorCode: String? = "NOT_CONFIGURED"
)
