package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.*
import com.example.ui.dialogs.iyzico.HostedPaymentDialog
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun NativeCashCollectionDialog(debtId: String, actorId: String, onDismiss: () -> Unit, onRecorded: () -> Unit) {
    val context = LocalContext.current
    val session = remember(debtId, actorId) { runCatching { NativeCashCollection(context, actorId, debtId) }.getOrNull() }
    val scope = rememberCoroutineScope()
    var debt by remember { mutableStateOf<CashDebt?>(null) }
    var ack by remember { mutableStateOf<CashPaymentAck?>(null) }
    var amount by remember { mutableStateOf("") }; var method by remember { mutableStateOf("CASH") }
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }; var online by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    val currency = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("tr-TR")) }
    LaunchedEffect(session, revision) {
        busy = true; error = null
        try {
            checkNotNull(session) { "Oturum veya borç kimliği doğrulanamadı" }
            debt = session.load(); pending = session.hasPending
            amount = session.pendingAmount() ?: debt!!.remaining.toPlainString(); method = session.pendingMethod() ?: "CASH"
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Güncel borç yüklenemedi. Yetki ve bağlantınızı kontrol edin." }
        finally { busy = false }
    }
    if (online) { HostedPaymentDialog(debtId) { online = false }; return }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 600.dp).fillMaxWidth()
            .heightIn(max = (LocalConfiguration.current.screenHeightDp * .9f).dp).imePadding(), shape = MaterialTheme.shapes.large) {
            LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Tahsilat kaydı", style = MaterialTheme.typography.titleLarge) }
                item { Text("Bu ekran önceden alınmış nakit/havale tutarını deftere işler. Online ödeme ayrı sağlayıcı ekranında yapılır.") }
                if (busy) item { Text("Sunucu yanıtı bekleniyor…") }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                if (debt == null) item {
                    Button(onClick = { revision++ }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Yeniden yükle") }
                }
                if (ack == null && debt != null) {
                    item { Text("Sunucudaki kalan borç: ${currency.format(debt!!.remaining)}") }
                    if (pending) item { Text("Sonucu belirsiz istek korunuyor. Aynı tutar/yöntem/kimlik ile doğrulayın; yeni tahsilat oluşturulmaz.") }
                    item { OutlinedTextField(amount, { amount = it }, enabled = !busy && !pending,
                        label = { Text("Tutar (ör. 100.25)") }, modifier = Modifier.fillMaxWidth().testTag("cash_amount")) }
                    item { Column { listOf("CASH" to "Nakit", "BANK_TRANSFER" to "Havale", "EFT" to "EFT").forEach { (value, title) ->
                        TextButton(onClick = { method = value }, enabled = !busy && !pending,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text(if (method == value) "✓ $title" else title) }
                    } } }
                    item {
                        Button(enabled = !busy && session != null, modifier = Modifier.heightIn(min = 48.dp).testTag("cash_save"), onClick = {
                            busy = true; error = null
                            scope.launch {
                                try { ack = session!!.collect(amount, method) }
                                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                catch (failure: Exception) { error = when ((failure as? CashCollectionHttpException)?.code) {
                                    403 -> "Tahsilat kaydı için yetkiniz yok."
                                    409 -> "İşlem kimliği veya kayıt değişmiş. Yeni ödeme başlatmadan sonucu inceleyin."
                                    422 -> "Tutar veya borç kaydı reddedildi. Aynı isteğin sonucunu inceleyin."
                                    else -> if (session!!.hasPending) "Sonuç belirsiz; aynı isteği tekrar doğrulayın." else failure.message ?: "Kaydedilemedi."
                                } }
                                finally { pending = session!!.hasPending; busy = false }
                            }
                        }) { Text(if (pending) "Aynı isteği doğrula" else "Deftere kaydet") }
                    }
                    item { TextButton(onClick = { online = true }, enabled = !busy && !pending,
                        modifier = Modifier.heightIn(min = 48.dp)) { Text("Güvenli online ödeme") } }
                }
                ack?.let { result ->
                    item { Text("Sunucu tahsilatı kaydetti. Makbuz: ${result.receiptNumber}") }
                    item { Text("Sunucunun bildirdiği öğrenci toplam kalan borcu: ${currency.format(result.remainingStudentDebt)}") }
                    item { Button(onClick = { session!!.finish(); onRecorded() }, modifier = Modifier.heightIn(min = 48.dp).testTag("cash_done")) { Text("Tamam") } }
                }
                if (ack == null) item { TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Kapat") } }
            }
        }
    }
}
