package com.example.ui.dialogs.iyzico

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.payment.SchoolCheckout
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun HostedPaymentDialog(debtId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val checkout = remember(debtId) { runCatching { SchoolCheckout(context, debtId) } }
    var debt by remember { mutableStateOf<JSONObject?>(null) }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var identity by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    fun run(action: suspend (SchoolCheckout) -> Unit) {
        if (busy) return
        busy = true; error = ""
        scope.launch {
            try { action(checkout.getOrThrow()) }
            catch (e: Exception) { error = e.message ?: "Ödeme sonucu kontrol edilmeli" }
            finally { busy = false }
        }
    }
    LaunchedEffect(debtId) { run { service -> debt = service.debt(); if (service.saved() != null) result = service.refresh() } }
    val paid = result?.optString("status") == "PAID"
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Kurum güvenli ödemesi") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kart bilgileri sağlayıcının güvenli sayfasında girilir. Tahsilat yalnız sunucu doğrulamasıyla kaydedilir.")
                debt?.let { Text("Kalan: ${it.optString("remaining_amount")} TRY") }
                if (error.isNotBlank()) Text(error)
                result?.let { Text(it.optString("message", it.optString("status"))) }
                if (!paid) {
                    OutlinedTextField(identity, { identity = it }, enabled = !busy, label = { Text("Kimlik / pasaport numarası") })
                    OutlinedTextField(phone, { phone = it }, enabled = !busy, label = { Text("Telefon") })
                    OutlinedTextField(address, { address = it }, enabled = !busy, label = { Text("Fatura adresi") })
                    OutlinedTextField(city, { city = it }, enabled = !busy, label = { Text("Şehir") })
                    OutlinedTextField(country, { country = it }, enabled = !busy, label = { Text("Ülke") })
                    Button(enabled = !busy && debt?.optBoolean("online_payment_available") == true && listOf(identity, phone, address, city, country).all { it.isNotBlank() }, onClick = {
                        run { service ->
                            val buyer = JSONObject().put("identityNumber", identity.trim()).put("gsmNumber", phone.trim())
                                .put("registrationAddress", address.trim()).put("city", city.trim()).put("country", country.trim())
                            val value = service.saved()?.getString("amount") ?: debt!!.getString("remaining_amount")
                            result = service.start(value, buyer)
                        }
                    }) { Text("Ödeme sayfasını hazırla / tekrar dene") }
                    if (result?.optBoolean("can_resume") == true) Button(enabled = !busy, onClick = {
                        run { service -> service.resume()?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
                    }) { Text("Güvenli ödeme sayfasını aç") }
                    Button(enabled = !busy, onClick = { run { service -> result = service.refresh() } }) { Text("Sunucudan sonucu kontrol et") }
                    if (result?.optString("status") in listOf("FAILED", "REFUNDED")) Button(enabled = !busy, onClick = {
                        run { service -> service.clearFailed(); result = null; debt = service.debt() }
                    }) { Text("Yeni işlem için önceki kaydı kapat") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (paid) "Tahsilat doğrulandı · Kapat" else "Kapat") } })
    // No legacy onPaymentSuccess: those callbacks create a second local/manual payment.
}
