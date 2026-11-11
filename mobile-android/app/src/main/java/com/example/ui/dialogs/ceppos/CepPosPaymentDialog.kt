package com.example.ui.dialogs.ceppos
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.data.model.Invoice
import com.example.data.payment.ceppos.CepPosTransactionResult

@Composable
fun CepPosPaymentDialog(
    invoice: Invoice? = null, canteenTopupAmount: Double? = null, studentName: String = "Öğrenci",
    initialAmount: Double = invoice?.amount ?: canteenTopupAmount ?: 0.0,
    paymentDescription: String = invoice?.description ?: "", onDismiss: () -> Unit,
    onPaymentSuccess: (CepPosTransactionResult, String) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Temassız / QR tahsilatı") },
        text = { Text("SoftPOS için sağlayıcının terminal ve SDK kabulü henüz tamamlanmamış. Herhangi bir tahsilat yapılmadı. Okul borçları için güvenli online ödeme kanalını kullanın.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } })
}
