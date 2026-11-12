package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Invoice
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeDebtLedgerClient
import com.example.ui.components.financePresentationScope
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

/** Keep cents and the user's draft until the canonical server edit is acknowledged. */
@Composable
fun FinanceDebtEditDialog(state: Okul360UiState, invoice: Invoice, onDismiss: () -> Unit, onRecorded: () -> Unit) {
    val identity = financePresentationScope(state)
    val openingIdentity = remember(invoice.id) { identity }
    if (identity != openingIdentity) { LaunchedEffect(identity) { onDismiss() }; return }
    val client = remember(invoice.id, identity) { NativeDebtLedgerClient() }
    val requestScope = remember(invoice.id, identity) { ApiClientProvider.interceptor.captureScope() }
    val coroutine = rememberCoroutineScope()
    var amount by remember(invoice.id, identity) { mutableStateOf(runCatching { BigDecimal.valueOf(invoice.amount).setScale(2).toPlainString() }.getOrDefault("")) }
    var due by remember(invoice.id, identity) { mutableStateOf(invoice.due_date) }
    var description by remember(invoice.id, identity) { mutableStateOf(invoice.description) }
    var busy by remember(invoice.id, identity) { mutableStateOf(false) }
    var acknowledged by remember(invoice.id, identity) { mutableStateOf(false) }
    var error by remember(invoice.id, identity) { mutableStateOf<String?>(null) }
    val canWrite = "finance.write" in state.currentUser.permissions ||
        (state.currentUser.is_super_admin == true && !state.workspaceOrganizationId.isNullOrBlank())
    val readonly = !canWrite || busy || acknowledged
    if (!requestScope.current()) { LaunchedEffect(requestScope) { onDismiss() }; return }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max=600.dp).fillMaxWidth()
            .heightIn(max=(LocalConfiguration.current.screenHeightDp * .9f).dp).imePadding(), shape=MaterialTheme.shapes.large) {
            LazyColumn(Modifier.padding(16.dp).testTag("debt_edit_list"), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item { Text("Tahakkuku düzenle", style=MaterialTheme.typography.titleLarge) }
                item { Text("Tahakkuk kimliği: ${invoice.id}") }
                item { Text("Öğrenci: ${invoice.student_name} (${invoice.class_name})") }
                if (!canWrite) item { Text("Düzenleme yetkiniz yok; form salt okunur.") }
                if (invoice.paid_amount > 0) item { Text("Tahsilatı olan borcun tutarı değiştirilemez.") }
                item { OutlinedTextField(amount, { amount=it; error=null }, readOnly=readonly || invoice.paid_amount>0,
                    label={ Text("Tutar (₺)") }, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                    modifier=Modifier.fillMaxWidth().testTag("debt_edit_amount")) }
                item { OutlinedTextField(due, { due=it; error=null }, readOnly=readonly, label={ Text("Vade (YYYY-AA-GG)") },
                    modifier=Modifier.fillMaxWidth().testTag("debt_edit_due")) }
                item { OutlinedTextField(description, { description=it; error=null }, readOnly=readonly, label={ Text("Açıklama") },
                    modifier=Modifier.fillMaxWidth().testTag("debt_edit_description")) }
                if (busy) item { Text("Sunucu yanıtı bekleniyor…") }
                error?.let { message -> item { Text(message, color=MaterialTheme.colorScheme.error, modifier=Modifier.testTag("debt_edit_error")) } }
                if (acknowledged) {
                    item { Text("Sunucu kaydı güncellendi.", modifier=Modifier.testTag("debt_edit_ack")) }
                    item { Button(onClick=onRecorded, modifier=Modifier.heightIn(min=48.dp).testTag("debt_edit_done")) { Text("Tamam") } }
                } else item { Button(enabled=canWrite && !busy, modifier=Modifier.heightIn(min=48.dp).testTag("debt_edit_save"), onClick={
                    if (busy || acknowledged || !canWrite) return@Button
                    val decimal=amount.trim().replace(',', '.').toBigDecimalOrNull()
                    val day=runCatching { LocalDate.parse(due).toString() }.getOrNull()
                    if (decimal==null || decimal<=BigDecimal.ZERO || decimal>BigDecimal("9999999999.99") || decimal.stripTrailingZeros().scale()>2 || day==null) {
                        error="Geçerli kuruşlu tutar ve vade girin."; return@Button
                    }
                    val draft=invoice.copy(amount=decimal.toDouble(), due_date=day, description=description)
                    busy=true; error=null
                    coroutine.launch {
                        try { client.update(draft); requestScope.ensureCurrent(); acknowledged=true }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error="Güncelleme doğrulanamadı. Taslağınız korunuyor; yetki ve bağlantınızı kontrol edip yeniden deneyin." }
                        finally { busy=false }
                    }
                }) { Text("Değişiklikleri kaydet") } }
                item { TextButton(onClick=onDismiss, enabled=!busy, modifier=Modifier.heightIn(min=48.dp).testTag("debt_edit_close")) { Text("Kapat") } }
            }
        }
    }
}
