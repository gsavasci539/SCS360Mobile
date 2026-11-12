package com.example.ui.dialogs
import androidx.compose.runtime.Composable
import com.example.data.model.Invoice
import com.example.data.model.PaymentReceipt

@Composable
fun ReceiptDialog(invoice: Invoice, onDismiss: () -> Unit, onPrintOrShare: () -> Unit = {}) {
    VerifiedReceiptDialog(invoice.id, onDismiss)
}
@Composable
fun ReceiptDialog(receipt: PaymentReceipt, onDismiss: () -> Unit, onPrintOrShare: () -> Unit) {
    VerifiedReceiptDialog(receipt.invoice_id, onDismiss)
}
