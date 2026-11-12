package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Expense
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeExpenseClient
import com.example.ui.components.financePresentationScope
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/** Deletes only the explicitly selected ledger row; never moves money. */
@Composable
fun FinanceExpenseDeleteDialog(expense: Expense, state: Okul360UiState, onDismiss: () -> Unit, onRecorded: () -> Unit) {
    val identity=financePresentationScope(state); val opening=remember { identity }
    val openingId=remember { expense.id }
    if(identity!=opening || expense.id!=openingId) { LaunchedEffect(identity,expense.id) { onDismiss() }; return }
    val requestScope=remember { ApiClientProvider.interceptor.captureScope() }
    val client=remember { NativeExpenseClient() }; val coroutine=rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }; var acknowledged by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val canWrite="finance.write" in state.currentUser.permissions || (state.currentUser.is_super_admin==true && !state.workspaceOrganizationId.isNullOrBlank())
    if(!requestScope.current()) { LaunchedEffect(requestScope) { onDismiss() }; return }
    val dismiss={ if(!busy) { if(acknowledged) onRecorded() else onDismiss() } }
    Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.padding(12.dp).widthIn(max=600.dp).fillMaxWidth()
            .heightIn(max=(LocalConfiguration.current.screenHeightDp*.9f).dp).imePadding(),shape=MaterialTheme.shapes.large) {
            LazyColumn(Modifier.padding(16.dp).testTag("expense_delete_list"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                item { Text("Gider kaydını sil",style=MaterialTheme.typography.titleLarge) }
                item { Text(expense.title,modifier=Modifier.testTag("expense_delete_title")) }
                item { Text("${expense.category} • ${expense.date}") }
                item { Text(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("tr-TR")).format(expense.amount)) }
                item { Text("Kayıt kimliği: ${expense.id}") }
                item { Text("Bu işlem gider kaydını listeden kaldırır; banka ödemesi veya iade yapmaz.") }
                if(!canWrite) item { Text("Gider silme yetkiniz yok; kayıt salt okunur.") }
                if(busy) item { Text("Sunucu yanıtı bekleniyor…") }
                error?.let { message -> item { Text(message,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("expense_delete_error")) } }
                if(acknowledged) {
                    item { Text("Gider kaydının silindiği sunucuda doğrulandı.",modifier=Modifier.testTag("expense_delete_ack")) }
                    item { Button(onClick=onRecorded,modifier=Modifier.heightIn(min=48.dp).testTag("expense_delete_done")) { Text("Tamam") } }
                } else item { Button(enabled=canWrite && !busy,onClick={
                    if(busy || acknowledged || !canWrite) return@Button
                    busy=true; error=null
                    coroutine.launch {
                        try { client.delete(openingId); requestScope.ensureCurrent(); acknowledged=true }
                        catch(cancelled:CancellationException) { throw cancelled }
                        catch(_:Exception) { error="Silme sonucu doğrulanamadı. Kayıt listeden kaldırılmış gibi gösterilmedi; bağlantı ve yetkinizi kontrol edip gider listesini yeniden okuyun." }
                        finally { busy=false }
                    }
                },modifier=Modifier.heightIn(min=48.dp).testTag("expense_delete_submit")) { Text("Seçili gideri sil") } }
                item { TextButton(onClick=dismiss,enabled=!busy,modifier=Modifier.heightIn(min=48.dp).testTag("expense_delete_close")) { Text("Kapat") } }
            }
        }
    }
}
