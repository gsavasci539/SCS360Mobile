package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.data.remote.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.Okul360UiState
import java.math.BigDecimal
import java.time.LocalDate

/** User-entered draft only; submission never closes this dialog before server review. */
@Composable
fun FinanceInvoiceDraftDialog(state: Okul360UiState, onDismiss: () -> Unit,
    onRecorded: () -> Unit = {}) {
    var student by remember { mutableStateOf("") }; var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }; var due by remember { mutableStateOf("") }
    val context = LocalContext.current
    val session = remember(state.currentUser.id) { runCatching { NativeDebtCreateSession(context, state.currentUser.id) }.getOrNull() }
    val coroutine = rememberCoroutineScope()
    var choices by remember { mutableStateOf<List<RegistryChoice>>(emptyList()) }
    var category by remember { mutableStateOf("EDUCATION") }
    var busy by remember { mutableStateOf(false) }; var pending by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }; var ack by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    val canWrite = state.currentUser.permissions.contains("finance.write") ||
        (state.currentUser.is_super_admin == true && !state.workspaceOrganizationId.isNullOrBlank())
    LaunchedEffect(session, revision) {
        busy = true; error = null
        try { choices = checkNotNull(session).load() }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Yetkili öğrenci seçenekleri yüklenemedi. Yeniden deneyin." }
        finally {
            session?.pendingDraft()?.let { draft -> student = draft.studentId; category = draft.type
                amount = draft.amount; due = draft.due; title = draft.description }
            pending = session?.hasPending == true; busy = false
        }
    }
    if (session != null && !session.current()) { LaunchedEffect(session) { onDismiss() }; return }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 600.dp).fillMaxWidth().imePadding()
            .heightIn(max = (LocalConfiguration.current.screenHeightDp * .9f).dp), shape = MaterialTheme.shapes.large) {
            LazyColumn(Modifier.padding(16.dp).testTag("invoice_draft_list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Tahakkuk taslağı", style = MaterialTheme.typography.titleLarge) }
                item { Text("Öğrenci, tutar ve vade bilgisini siz girin. Bu form resmi fatura veya makbuz üretmez.") }
                if (!canWrite) item { Text("Tahakkuk oluşturma yetkiniz yok; form salt okunur.") }
                if (busy) item { Text("Sunucu yanıtı bekleniyor…") }
                if (pending) item { Text("Bekleyen tahakkuk isteği korunuyor; aynı içerik ve işlem kimliğiyle doğrulayın.") }
                if (choices.isEmpty() && !busy) item { Button(onClick = { revision++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Seçenekleri yeniden yükle") } }
                item { Text("Öğrenci seçin") }
                item { Column { listOf("EDUCATION" to "Eğitim", "FOOD" to "Yemek", "SERVICE" to "Servis", "STATIONERY" to "Kırtasiye", "ACTIVITY" to "Etkinlik", "OTHER" to "Diğer").forEach { (value, label) ->
                    TextButton(onClick = { category = value }, enabled = canWrite && !busy && !pending && ack == null,
                        modifier = Modifier.heightIn(min = 48.dp)) { Text(if (category == value) "✓ $label" else label) }
                } } }
                choices.forEach { row -> item(key = row.id) {
                    TextButton(onClick = { student = row.id }, enabled = canWrite && !submitted && !busy && !pending,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("invoice_student_${row.id}")) {
                        Text((if (student == row.id) "✓ " else "") + row.name)
                    }
                } }
                if (choices.isEmpty()) item { Text("Erişilebilir öğrenci listesi henüz yüklenmedi.") }
                item { OutlinedTextField(title, { title = it }, label = { Text("Açıklama") }, enabled = canWrite && !submitted && !busy && !pending,
                    modifier = Modifier.fillMaxWidth().testTag("invoice_title")) }
                item { OutlinedTextField(amount, { amount = it }, label = { Text("Tutar") }, enabled = canWrite && !submitted && !busy && !pending,
                    modifier = Modifier.fillMaxWidth().testTag("invoice_amount")) }
                item { OutlinedTextField(due, { due = it }, label = { Text("Vade (YYYY-AA-GG)") }, enabled = canWrite && !submitted && !busy && !pending,
                    modifier = Modifier.fillMaxWidth().testTag("invoice_due")) }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                ack?.let { identifier -> item {
                    Text("Sunucu tahakkuku kaydetti: $identifier")
                    Button(onClick = { session!!.finish(); onRecorded() }, modifier = Modifier.heightIn(min = 48.dp).testTag("invoice_done")) { Text("Tamam") }
                } }
                item { Button(enabled = canWrite && !busy && ack == null, modifier = Modifier.heightIn(min = 48.dp).testTag("invoice_submit"), onClick = {
                    if (session == null) { error = "Oturum doğrulanamadı"; return@Button }
                    busy = true; error = null
                    coroutine.launch {
                        try { ack = session.create(DebtCreateDraft(student, category, amount, due, title)); submitted = true }
                        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (_: Exception) { error = if (session.hasPending) "Sonuç doğrulanamadı; yeni kayıt başlatmadan aynı isteği tekrar doğrulayın."
                            else "Öğrenci, açıklama, geçerli kuruşlu tutar ve vade girin; yetki ve bağlantınızı kontrol edin." }
                        finally { pending = session.hasPending; busy = false }
                    }
                }) { Text(if (pending) "Aynı isteği doğrula" else "Taslağı sunucuya gönder") } }
                item { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag("invoice_close")) { Text("Kapat") } }
            }
        }
    }
}
