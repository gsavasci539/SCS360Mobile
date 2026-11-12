package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.*
import com.example.ui.components.financePresentationScope
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun FinanceExpenseCreateDialog(state: Okul360UiState, onDismiss: () -> Unit, onRecorded: () -> Unit) {
    val identity=financePresentationScope(state); val opening=remember { identity }
    if(identity!=opening) { LaunchedEffect(identity) { onDismiss() }; return }
    val context=LocalContext.current
    val session=remember { runCatching { NativeExpenseCreateSession(context,state.currentUser.id) }.getOrNull() }
    val coroutine=rememberCoroutineScope()
    var branches by remember { mutableStateOf<List<RegistryChoice>>(emptyList()) }
    var chosen by remember { mutableStateOf(session?.selectedBranch!=null) }
    var branch by remember { mutableStateOf(session?.selectedBranch) }
    var title by remember { mutableStateOf("") }; var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }; var date by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }; var ready by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }; var ack by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }; var revision by remember { mutableIntStateOf(0) }
    val canWrite="finance.write" in state.currentUser.permissions || (state.currentUser.is_super_admin==true && !state.workspaceOrganizationId.isNullOrBlank())
    LaunchedEffect(session,revision,canWrite) {
        if(!canWrite) return@LaunchedEffect
        busy=true; ready=false; error=null
        try { branches=checkNotNull(session).load(); ready=true }
        catch(cancelled:CancellationException) { throw cancelled }
        catch(_:Exception) { error="Yetkili gider kapsamları yüklenemedi. Yeniden deneyin." }
        finally {
            session?.pendingDraft()?.let { draft -> chosen=true; branch=draft.branchId; title=draft.title; category=draft.category; amount=draft.amount; date=draft.date }
            pending=session?.hasPending==true; busy=false
        }
    }
    if(session!=null && !session.current()) { LaunchedEffect(session) { onDismiss() }; return }
    val readonly=!canWrite || busy || pending || ack!=null
    Dialog(onDismissRequest={ if(!busy) onDismiss() },properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.padding(12.dp).widthIn(max=600.dp).fillMaxWidth().heightIn(max=(LocalConfiguration.current.screenHeightDp*.9f).dp).imePadding(),shape=MaterialTheme.shapes.large) {
            LazyColumn(Modifier.padding(16.dp).testTag("expense_create_list"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item { Text("Yeni gider",style=MaterialTheme.typography.titleLarge) }
                if(!canWrite) item { Text("Gider oluşturma yetkiniz yok; form salt okunur.") }
                if(busy) item { Text("Sunucu yanıtı bekleniyor…") }
                if(pending) item { Text("Bekleyen gider isteği korunuyor. Aynı içeriği doğrulayın.") }
                item { Text("Kapsam seçin") }
                if(session?.allowsOrganizationWide==true) item {
                    TextButton(enabled=!readonly,onClick={ chosen=true; branch=null },modifier=Modifier.heightIn(min=48.dp).testTag("expense_scope_org")) { Text(if(chosen && branch==null) "✓ Kurum geneli" else "Kurum geneli") }
                }
                branches.forEach { choice -> item(key=choice.id) {
                    TextButton(enabled=!readonly,onClick={ chosen=true; branch=choice.id },modifier=Modifier.heightIn(min=48.dp)) { Text(if(branch==choice.id) "✓ ${choice.name}" else choice.name) }
                } }
                item { OutlinedTextField(title,{ title=it; error=null },readOnly=readonly,label={ Text("Açıklama") },modifier=Modifier.fillMaxWidth().testTag("expense_title")) }
                item { OutlinedTextField(category,{ category=it; error=null },readOnly=readonly,label={ Text("Kategori") },modifier=Modifier.fillMaxWidth().testTag("expense_category")) }
                item { OutlinedTextField(amount,{ amount=it; error=null },readOnly=readonly,label={ Text("Tutar (₺)") },keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth().testTag("expense_amount")) }
                item { OutlinedTextField(date,{ date=it; error=null },readOnly=readonly,label={ Text("Tarih (YYYY-AA-GG)") },modifier=Modifier.fillMaxWidth().testTag("expense_date")) }
                error?.let { message -> item { Text(message,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("expense_error")) } }
                if(ack!=null) {
                    item { Text("Gider kaydı doğrulandı: $ack",modifier=Modifier.testTag("expense_ack")) }
                    item { Button(onClick={ session?.finish(); onRecorded() },modifier=Modifier.heightIn(min=48.dp).testTag("expense_done")) { Text("Tamam") } }
                } else {
                    if(!ready && !busy && canWrite) item { Button(onClick={ revision++ },modifier=Modifier.heightIn(min=48.dp)) { Text("Kapsamları yeniden yükle") } }
                    item { Button(enabled=canWrite && ready && !busy,modifier=Modifier.heightIn(min=48.dp).testTag("expense_submit"),onClick={
                        if(busy || ack!=null || session==null) return@Button
                        val valid=chosen && title.isNotBlank() && category.isNotBlank() && runCatching { NativeExpenseClient.money(amount); LocalDate.parse(date) }.isSuccess
                        if(!valid) { error="Kapsam, açıklama, kategori, kuruşlu tutar ve tarih girin."; return@Button }
                        val draft=ExpenseDraft(branch,title,category,amount,date); busy=true; error=null
                        coroutine.launch {
                            try { ack=session.create(draft).id }
                            catch(cancelled:CancellationException) { throw cancelled }
                            catch(_:Exception) { error=if(session.hasPending) "Sonuç doğrulanamadı. Taslak ve işlem kimliği korunuyor; aynı isteği yeniden doğrulayın." else "Gider gönderilmedi; bilgileri ve yetkinizi kontrol edin." }
                            finally { pending=session.hasPending; busy=false }
                        }
                    }) { Text(if(pending) "Aynı isteği doğrula" else "Gideri sunucuya gönder") } }
                }
                item { TextButton(enabled=!busy,onClick=onDismiss,modifier=Modifier.heightIn(min=48.dp).testTag("expense_close")) { Text("Kapat") } }
            }
        }
    }
}
