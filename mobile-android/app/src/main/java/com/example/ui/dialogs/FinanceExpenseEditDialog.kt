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
import com.example.data.remote.*
import com.example.ui.components.financePresentationScope
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun FinanceExpenseEditDialog(expenseId:String,state:Okul360UiState,onDismiss:()->Unit,onRecorded:()->Unit) {
    val identity=financePresentationScope(state); val opening=remember { identity }; val openingId=remember { expenseId }
    if(identity!=opening || openingId!=expenseId) { LaunchedEffect(identity,expenseId) { onDismiss() }; return }
    val session=remember { runCatching { NativeExpenseEditSession(expenseId,checkNotNull(state.workspaceOrganizationId ?: state.currentUser.organization_id)) }.getOrNull() }
    val coroutine=rememberCoroutineScope(); var revision by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf<ExpenseEditDraft?>(null) }; var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }; var acknowledged by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val canWrite="finance.write" in state.currentUser.permissions || (state.currentUser.is_super_admin==true && !state.workspaceOrganizationId.isNullOrBlank())
    LaunchedEffect(session,revision) {
        busy=true; error=null; draft=null
        try { draft=checkNotNull(session).load().draft(); pending=false; acknowledged=false }
        catch(cancelled:CancellationException) { throw cancelled }
        catch(_:Exception) { error="Güncel gider okunamadı. Kurum, yetki ve bağlantınızı kontrol edin." }
        finally { busy=false }
    }
    if(session!=null && !session.current()) { LaunchedEffect(session) { onDismiss() }; return }
    val readonly=!canWrite || busy || pending || acknowledged
    val dismiss={ if(!busy) { if(acknowledged) onRecorded() else onDismiss() } }
    Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.padding(12.dp).widthIn(max=600.dp).fillMaxWidth().heightIn(max=(LocalConfiguration.current.screenHeightDp*.9f).dp).imePadding(),shape=MaterialTheme.shapes.large) {
            LazyColumn(Modifier.padding(16.dp).testTag("expense_edit_list"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item { Text("Gideri düzenle",style=MaterialTheme.typography.titleLarge) }
                item { Text("Kayıt kimliği: $expenseId") }
                if(!canWrite) item { Text("Düzenleme yetkiniz yok; kayıt salt okunur.") }
                if(busy) item { Text("Sunucu yanıtı bekleniyor…") }
                if(pending) item { Text("Önceki değişikliğin sonucu bekleniyor. Aynı taslağı doğrulayın veya sunucudan yeniden okuyun.") }
                draft?.let { value ->
                    item { OutlinedTextField(value.title,{ draft=value.copy(title=it) },readOnly=readonly,label={ Text("Açıklama") },modifier=Modifier.fillMaxWidth().testTag("expense_edit_title")) }
                    item { OutlinedTextField(value.category,{ draft=value.copy(category=it) },readOnly=readonly,label={ Text("Kategori") },modifier=Modifier.fillMaxWidth().testTag("expense_edit_category")) }
                    item { OutlinedTextField(value.amount,{ draft=value.copy(amount=it) },readOnly=readonly,label={ Text("Tutar (₺)") },keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth().testTag("expense_edit_amount")) }
                    item { OutlinedTextField(value.date,{ draft=value.copy(date=it) },readOnly=readonly,label={ Text("Tarih (YYYY-AA-GG)") },modifier=Modifier.fillMaxWidth().testTag("expense_edit_date")) }
                    item { OutlinedTextField(value.notes.orEmpty(),{ draft=value.copy(notes=it) },readOnly=readonly,label={ Text("Not / belge referansı (metin)") },modifier=Modifier.fillMaxWidth().testTag("expense_edit_notes")) }
                }
                error?.let { message -> item { Text(message,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("expense_edit_error")) } }
                if(acknowledged) {
                    item { Text("Gider değişikliği sunucuda doğrulandı.",modifier=Modifier.testTag("expense_edit_ack")) }
                    item { Button(onClick=onRecorded,modifier=Modifier.heightIn(min=48.dp).testTag("expense_edit_done")) { Text("Tamam") } }
                } else {
                    item { Button(enabled=canWrite && draft!=null && !busy,onClick={
                        if(busy || acknowledged || !canWrite || session==null) return@Button
                        val value=draft ?: return@Button; busy=true; error=null
                        coroutine.launch {
                            try { draft=session.save(value).draft(); acknowledged=true }
                            catch(cancelled:CancellationException) { throw cancelled }
                            catch(_:Exception) { error="Değişiklik doğrulanamadı. Taslağınız korunuyor; geçerli bilgileri ve yetkinizi kontrol edin veya güncel kaydı yeniden okuyun." }
                            finally { pending=session.hasPending; busy=false }
                        }
                    },modifier=Modifier.heightIn(min=48.dp).testTag("expense_edit_save")) { Text(if(pending) "Aynı değişikliği doğrula" else "Değişiklikleri kaydet") } }
                    item { TextButton(enabled=!busy,onClick={ revision++ },modifier=Modifier.heightIn(min=48.dp).testTag("expense_edit_reload")) { Text("Sunucudan yeniden oku") } }
                }
                item { TextButton(enabled=!busy,onClick=dismiss,modifier=Modifier.heightIn(min=48.dp).testTag("expense_edit_close")) { Text("Kapat") } }
            }
        }
    }
}
