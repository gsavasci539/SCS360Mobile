package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PlatformTenantSummary
import com.example.data.model.SubscriptionPlan
import com.example.data.remote.SaasApi
import com.example.data.remote.TenantBillingUser
import com.example.data.remote.TenantMembershipDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MembershipEditorDialog(
    tenant: PlatformTenantSummary,
    plans: List<SubscriptionPlan>,
    loadUsers: suspend (String) -> List<TenantBillingUser>,
    onSave: suspend (String, TenantMembershipDraft) -> String?,
    onDismiss: () -> Unit
) {
    var planId by remember(tenant.organization_id) { mutableStateOf(tenant.plan_id) }
    var start by remember(tenant.organization_id) { mutableStateOf(tenant.period_start) }
    var end by remember(tenant.organization_id) { mutableStateOf(tenant.period_end) }
    var status by remember(tenant.organization_id) {
        mutableStateOf(tenant.admin_status ?: if (tenant.access_status == "SUSPENDED") "SUSPENDED" else "ACTIVE")
    }
    var owner by remember(tenant.organization_id) { mutableStateOf(tenant.billing_owner_id) }
    var users by remember(tenant.organization_id) { mutableStateOf<List<TenantBillingUser>?>(null) }
    var usersError by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    var choosingOwner by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var visibleUsers by remember(search) { mutableIntStateOf(20) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(tenant.organization_id, loadAttempt) {
        usersError = false
        try {
            users = loadUsers(tenant.organization_id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            usersError = true
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.padding(12.dp).widthIn(max = 540.dp).fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${tenant.organization_name} üyeliği", style = MaterialTheme.typography.titleLarge)
                Text("Üyelik paketi")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    plans.forEach { plan ->
                        FilterChip(selected = planId == plan.id, enabled = !busy,
                            onClick = { planId = plan.id }, label = { Text(plan.name) })
                    }
                }
                OutlinedTextField(value = start, onValueChange = { start = it }, enabled = !busy,
                    label = { Text("Başlangıç (YYYY-AA-GG veya saat dilimli tarih)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("membership_start"))
                OutlinedTextField(value = end, onValueChange = { end = it }, enabled = !busy,
                    label = { Text("Bitiş (YYYY-AA-GG veya saat dilimli tarih)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("membership_end"))
                Text("Gün olarak girilen tarihler UTC 00.00 kabul edilir.")
                Text("Üyelik erişimi")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = status == "ACTIVE", enabled = !busy, onClick = { status = "ACTIVE" }, label = { Text("Açık") })
                    FilterChip(selected = status == "SUSPENDED", enabled = !busy,
                        onClick = { status = "SUSPENDED" }, label = { Text("Kapalı") })
                }
                if (!tenant.is_active) Text("Kurum hesabı kapalı. Hesap durumunu kurum listesinden yönetebilirsiniz.")
                val ownerLabel = users?.firstOrNull { it.id == owner }?.let { "${it.name} · ${it.email}" }
                    ?: if (owner == null) "Atama yok" else "Mevcut ödeme yetkilisi"
                Text("Ödeme yetkilisi")
                OutlinedButton(onClick = { choosingOwner = !choosingOwner }, enabled = !busy && users != null,
                    modifier = Modifier.fillMaxWidth().testTag("billing_owner_picker")) {
                    Text(ownerLabel)
                }
                if (usersError) {
                    Text("Kurum kullanıcıları alınamadı.", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { loadAttempt++ }, enabled = !busy) { Text("Kullanıcıları yeniden getir") }
                }
                if (choosingOwner) {
                    OutlinedTextField(search, { search = it }, label = { Text("Ad veya e-posta ara") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                    TextButton(onClick = { owner = null; choosingOwner = false }, enabled = !busy) { Text("Atama yok") }
                    val matching = users.orEmpty().filter {
                        it.name.contains(search.trim(), ignoreCase = true) || it.email.contains(search.trim(), ignoreCase = true)
                    }
                    matching.take(visibleUsers).forEach { person ->
                        TextButton(onClick = { owner = person.id; choosingOwner = false }, enabled = !busy,
                            modifier = Modifier.fillMaxWidth().testTag("billing_owner_${person.id}")) {
                            Text("${person.name} · ${person.email}")
                        }
                    }
                    if (matching.size > visibleUsers) TextButton(onClick = { visibleUsers += 20 }, enabled = !busy) {
                        Text("Daha fazla göster")
                    }
                }
                error?.let { Text(it, modifier = Modifier.testTag("membership_error"), color = MaterialTheme.colorScheme.error) }
                Button(enabled = !busy && plans.any { it.id == planId } && start.isNotBlank() && end.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("membership_save"), onClick = {
                        if (busy) return@Button
                        val draft = TenantMembershipDraft(planId, start, end, status, owner)
                        try {
                            SaasApi.membershipBody(tenant, draft.planId, draft.periodStart, draft.periodEnd, draft.adminStatus, draft.billingOwnerId)
                        } catch (_: IllegalArgumentException) {
                            error = "Paket ve tarihleri kontrol edin. Bitiş başlangıçtan sonra olmalı."
                            return@Button
                        } catch (_: java.time.DateTimeException) {
                            error = "Tarih biçimini ve saat dilimini kontrol edin."
                            return@Button
                        }
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                error = onSave(tenant.organization_id, draft)
                                if (error == null) onDismiss()
                            } finally {
                                busy = false
                            }
                        }
                    }) { Text(if (busy) "Kaydediliyor…" else "Üyeliği güncelle") }
                TextButton(enabled = !busy, onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("membership_close")) {
                    Text("Kapat")
                }
            }
        }
    }
}