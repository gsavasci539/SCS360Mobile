package com.example.ui.dialogs.iyzico
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.data.model.Invoice
import com.example.data.payment.iyzico.IyzicoPaymentResult

@Composable
fun IyzicoPaymentDialog(
    invoice: Invoice? = null, canteenTopupAmount: Double? = null, studentName: String = "Öğrenci",
    initialAmount: Double = invoice?.amount ?: canteenTopupAmount ?: 0.0,
    paymentDescription: String = invoice?.description ?: "", onDismiss: () -> Unit,
    onPaymentSuccess: (IyzicoPaymentResult, String) -> Unit
) {
    if (invoice != null) HostedPaymentDialog(invoice.id, onDismiss)
    else AlertDialog(onDismissRequest = onDismiss, title = { Text("Online bakiye yükleme") },
        text = { Text("Kantin cüzdanına online bakiye yükleme kabulü henüz tamamlanmamış. Tahsilat yapılmadı.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } })
}
