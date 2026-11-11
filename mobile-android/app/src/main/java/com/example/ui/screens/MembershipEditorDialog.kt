package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.PlatformTenantSummary
import com.example.data.model.SubscriptionPlan
import com.example.data.remote.SaasApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun MembershipEditorDialog(
    tenant: PlatformTenantSummary,
    plans: List<SubscriptionPlan>,
    onSave: suspend (String, String, String, String) -> String?,
    onDismiss: () -> Unit
) {
    var planId by remember(tenant.organization_id) { mutableStateOf(tenant.plan_id) }
    var start by remember(tenant.organization_id) { mutableStateOf(tenant.period_start) }
    var end by remember(tenant.organization_id) { mutableStateOf(tenant.period_end) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("${tenant.organization_name} · Üyelik güncelle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Paket ve dönemi seçin. Gün olarak girilen tarihler UTC 00:00 kabul edilir.")
                plans.forEach { plan ->
                    FilterChip(selected = planId == plan.id, enabled = !busy, onClick = { planId = plan.id }, label = { Text(plan.name) })
                }
                OutlinedTextField(value = start, onValueChange = { start = it }, enabled = !busy,
                    label = { Text("Başlangıç (YYYY-AA-GG veya saat dilimli tarih)") }, singleLine = true, modifier = Modifier.testTag("membership_start"))
                OutlinedTextField(value = end, onValueChange = { end = it }, enabled = !busy,
                    label = { Text("Bitiş (YYYY-AA-GG veya saat dilimli tarih)") }, singleLine = true, modifier = Modifier.testTag("membership_end"))
                if (!tenant.is_active || tenant.access_status == "SUSPENDED") Text("Kurumun kapalı durumu korunur. Bu işlem kurumu yeniden açmaz.")
                error?.let { Text(it, modifier = Modifier.testTag("membership_error")) }
            }
        },
        confirmButton = {
            Button(enabled = !busy && plans.any { it.id == planId } && start.isNotBlank() && end.isNotBlank(),
                modifier = Modifier.testTag("membership_save"), onClick = {
                    if (!busy) {
                        try { SaasApi.membershipBody(tenant, planId, start, end) }
                        catch (_: Exception) { error = "Paket ve tarihleri kontrol edin; bitiş başlangıçtan sonra ve tarih saat dilimi belirli olmalıdır."; return@Button }
                        busy = true; error = null
                        scope.launch {
                            try {
                                val failure = onSave(tenant.organization_id, planId, start, end)
                                if (failure == null) onDismiss() else error = failure
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { error = "Sonuç doğrulanamadı. Bilgileriniz korunuyor; tekrar deneyebilir veya listeyi yenileyebilirsiniz." }
                            finally { busy = false }
                        }
                    }
                }) { Text(if (busy) "Kaydediliyor…" else "Üyeliği güncelle") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss, modifier = Modifier.testTag("membership_close")) { Text("Kapat") } }
    )
}
