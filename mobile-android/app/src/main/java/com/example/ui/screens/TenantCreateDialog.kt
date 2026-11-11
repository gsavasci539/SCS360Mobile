package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.SubscriptionPlan
import com.example.data.remote.TenantProvisionDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun TenantCreateDialog(plans: List<SubscriptionPlan>, onCreate: suspend (TenantProvisionDraft) -> String?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var planId by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Kurum ve sahip hesabı oluştur") },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dönem seçilen pakete göre sunucuda başlar. Bu işlem kurum, sahip hesabı ve üyelik oluşturur.")
                OutlinedTextField(name, { name = it }, enabled = !busy, label = { Text("Kurum adı") }, singleLine = true, modifier = Modifier.testTag("tenant_name"))
                OutlinedTextField(first, { first = it }, enabled = !busy, label = { Text("Sahibin adı") }, singleLine = true, modifier = Modifier.testTag("tenant_first"))
                OutlinedTextField(last, { last = it }, enabled = !busy, label = { Text("Sahibin soyadı") }, singleLine = true, modifier = Modifier.testTag("tenant_last"))
                OutlinedTextField(email, { email = it }, enabled = !busy, label = { Text("Sahibin e-postası") }, singleLine = true, modifier = Modifier.testTag("tenant_email"))
                OutlinedTextField(password, { password = it }, enabled = !busy, label = { Text("Sahip parolası (en az 8 karakter)") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.testTag("tenant_password"))
                Text("Paket seçin")
                plans.filter { it.is_active }.forEach { plan ->
                    FilterChip(selected = planId == plan.id, onClick = { planId = plan.id }, enabled = !busy,
                        label = { Text(plan.name) }, modifier = Modifier.testTag("tenant_plan_${plan.id}"))
                }
                if (plans.none { it.is_active }) Text("Yeni üyeliğe açık paket bulunmuyor.")
                error?.let { Text(it, modifier = Modifier.testTag("tenant_error")) }
            }
        },
        confirmButton = {
            Button(enabled = !busy && plans.any { it.id == planId && it.is_active }, modifier = Modifier.testTag("tenant_create"), onClick = {
                if (!busy) {
                    val draft = TenantProvisionDraft(name, first, last, email, password, planId)
                    try { draft.body(UUID(0, 0).toString()) }
                    catch (_: Exception) { error = "Kurum, sahip bilgileri, parola ve paketi kontrol edin."; return@Button }
                    busy = true; error = null
                    scope.launch {
                        try {
                            val failure = onCreate(draft)
                            if (failure == null) { password = ""; onDismiss() } else error = failure
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Sonuç doğrulanamadı. Aynı bilgilerle tekrar deneyin; yeni kurum oluşturma isteği açmayın." }
                        finally { busy = false }
                    }
                }
            }) { Text(if (busy) "Oluşturuluyor…" else "Kurumu oluştur") }
        },
        dismissButton = { TextButton(enabled = !busy, modifier = Modifier.testTag("tenant_close"), onClick = { password = ""; onDismiss() }) { Text("Kapat") } })
}
