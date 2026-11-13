package com.example.ui.dialogs.iyzico

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.testTag
import com.example.data.remote.ApiClientProvider
import kotlinx.coroutines.CancellationException
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
fun HostedPaymentDialog(debtId: String, onDismiss: () -> Unit, onReconciled: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val captured = remember(debtId) { ApiClientProvider.interceptor.captureScope() }
    val dismiss by rememberUpdatedState(onDismiss)
    val reload by rememberUpdatedState(onReconciled)
    var closed by remember(debtId) { mutableStateOf(false) }
    fun close() {
        if (closed) return
        closed = true
        if (captured.current()) reload()
        dismiss()
    }
    val checkout = remember(debtId) { runCatching { SchoolCheckout(context, debtId) } }
    var debt by remember(debtId) { mutableStateOf<JSONObject?>(null) }
    var result by remember(debtId) { mutableStateOf<JSONObject?>(null) }
    var error by remember(debtId) { mutableStateOf("") }
    var busy by remember(debtId) { mutableStateOf(false) }
    var identity by remember(debtId) { mutableStateOf("") }
    var phone by remember(debtId) { mutableStateOf("") }
    var address by remember(debtId) { mutableStateOf("") }
    var city by remember(debtId) { mutableStateOf("") }
    var country by remember(debtId) { mutableStateOf("") }
    fun run(action: suspend (SchoolCheckout) -> Unit) {
        if (busy || closed || !captured.current()) return
        busy = true; error = ""
        scope.launch {
            try { captured.ensureCurrent(); action(checkout.getOrThrow()); captured.ensureCurrent() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (e: Exception) { if (captured.current()) error = e.message ?: "Ödeme sonucu kontrol edilmeli" }
            finally { if (captured.current()) busy = false }
        }
    }
    LaunchedEffect(debtId) { run { service -> debt = service.debt(); if (service.saved() != null) result = service.refresh() } }
    if (!captured.current()) { LaunchedEffect(captured) { dismiss() }; return }
    val paid = result?.optString("status") == "PAID"
    val shownResult = result
    val listState = key(paid) { androidx.compose.foundation.lazy.rememberLazyListState() }
    androidx.compose.ui.window.Dialog(onDismissRequest = ::close,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(Modifier.padding(12.dp).widthIn(max = 600.dp).fillMaxWidth()
            .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * .9f).dp).imePadding(),
            shape = androidx.compose.material3.MaterialTheme.shapes.large) {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.padding(16.dp).testTag("hosted_payment"), state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item(key = "heading") { Text("Kurum güvenli ödemesi", style = androidx.compose.material3.MaterialTheme.typography.titleLarge) }
                item(key = "instruction") { Text("Kart bilgileri sağlayıcının güvenli sayfasında girilir. Tahsilat yalnız sunucu doğrulamasıyla kaydedilir.") }
                item(key = "remaining") { debt?.let { Text("Kalan: ${it.optString("remaining_amount")} TRY") } }
                item(key = "error") { if (error.isNotBlank()) Text(error) }
                item(key = "status") { shownResult?.let { Text(it.optString("message", it.optString("status")), Modifier.testTag("hosted_status")) } }
                if (!paid) {
                    item(key = "identity") { OutlinedTextField(identity, { identity = it }, enabled = !busy, label = { Text("Kimlik / pasaport numarası") }, modifier = Modifier.fillMaxWidth()) }
                    item(key = "phone") { OutlinedTextField(phone, { phone = it }, enabled = !busy, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth()) }
                    item(key = "address") { OutlinedTextField(address, { address = it }, enabled = !busy, label = { Text("Fatura adresi") }, modifier = Modifier.fillMaxWidth()) }
                    item(key = "city") { OutlinedTextField(city, { city = it }, enabled = !busy, label = { Text("Şehir") }, modifier = Modifier.fillMaxWidth()) }
                    item(key = "country") { OutlinedTextField(country, { country = it }, enabled = !busy, label = { Text("Ülke") }, modifier = Modifier.fillMaxWidth()) }
                    item(key = "prepare") {
                        Button(enabled = !busy && debt?.optBoolean("online_payment_available") == true && listOf(identity, phone, address, city, country).all { it.isNotBlank() }, onClick = {
                            run { service ->
                                val buyer = JSONObject().put("identityNumber", identity.trim()).put("gsmNumber", phone.trim())
                                    .put("registrationAddress", address.trim()).put("city", city.trim()).put("country", country.trim())
                                result = service.start(service.saved()?.getString("amount") ?: debt!!.getString("remaining_amount"), buyer)
                            }
                        }) { Text("Ödeme sayfasını hazırla / tekrar dene") }
                    }
                    if (shownResult?.optBoolean("can_resume") == true) item(key = "resume") {
                        Button(enabled = !busy, onClick = { run { service -> service.resume()?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } } }) { Text("Güvenli ödeme sayfasını aç") }
                    }
                    item(key = "refresh") {
                        Button(enabled = !busy, onClick = { run { service -> result = service.refresh(); debt = service.debt(); captured.ensureCurrent(); reload() } },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("hosted_refresh")) { Text("Sunucudan sonucu kontrol et") }
                    }
                    if (shownResult?.optString("status") in listOf("FAILED", "REFUNDED")) item(key = "clear") {
                        Button(enabled = !busy, onClick = { run { service -> service.clearFailed(); result = null; debt = service.debt() } }) { Text("Yeni işlem için önceki kaydı kapat") }
                    }
                }
                item(key = "close") { TextButton(onClick = ::close, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("hosted_close")) { Text(if (paid) "Tahsilat doğrulandı · Kapat" else "Kapat") } }
            }
        }
    }
    // No legacy onPaymentSuccess: those callbacks create a second local/manual payment.
}
