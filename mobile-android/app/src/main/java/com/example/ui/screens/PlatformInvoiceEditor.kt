package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SubscriptionInvoice
import com.example.data.remote.PlatformInvoiceDraft
import com.example.data.remote.PlatformInvoiceOperation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
internal fun PlatformInvoiceEditor(invoice: SubscriptionInvoice, operation: PlatformInvoiceOperation,
    onSave: suspend (SubscriptionInvoice, PlatformInvoiceDraft) -> String?, onDismiss: () -> Unit) {
    var reference by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paidAt by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val requestId = remember(operation, invoice.id, reference, amount, paidAt, start, end, reason) { UUID.randomUUID().toString() }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 540.dp).fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(operation.label, style = MaterialTheme.typography.titleLarge)
                if (invoice.organization_name.isNotBlank()) Text(invoice.organization_name)
                Text("Talep tutarı ${invoice.amount} ${invoice.currency}")
                if (operation == PlatformInvoiceOperation.SETTLE) {
                    OutlinedTextField(reference, { reference = it }, label = { Text("Banka dekont referansı") },
                        enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(amount, { amount = it }, label = { Text("Alınan tutar") }, enabled = !busy,
                        singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(paidAt, { paidAt = it }, label = { Text("Ödeme zamanı ve saat dilimi (isteğe bağlı)") },
                        enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                } else {
                    if (operation == PlatformInvoiceOperation.APPLY) {
                        OutlinedTextField(start, { start = it }, label = { Text("Dönem başlangıcı (UTC, YYYY-AA-GG)") },
                            enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(end, { end = it }, label = { Text("Dönem bitişi (UTC, YYYY-AA-GG)") },
                            enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text("Banka veya sağlayıcı panelinde tamamlanmış iadenin referansını girin.")
                        OutlinedTextField(reference, { reference = it }, label = { Text("Doğrulanan iade referansı") },
                            enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(reason, { reason = it }, label = { Text("Kontrol açıklaması") },
                        enabled = !busy, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    if (busy) return@Button
                    val draft = PlatformInvoiceDraft(operation, requestId, reference, amount, paidAt, start, end, reason)
                    error = null
                    busy = true
                    scope.launch {
                        try {
                            error = onSave(invoice, draft)
                            if (error == null) onDismiss()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } finally {
                            busy = false
                        }
                    }
                }) { Text(if (busy) "Doğrulanıyor…" else "Kaydı doğrula") }
                TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Vazgeç") }
            }
        }
    }
}
