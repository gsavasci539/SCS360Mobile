package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubscriptionBillingScreen(
    accessStatus: OrganizationAccessStatus,
    plans: List<SubscriptionPlan>,
    invoices: List<SubscriptionInvoice>,
    onRefreshAccessStatus: () -> Unit,
    onRequestFreePlan: suspend (SubscriptionPlan, String) -> String?,
    onCheckPaymentStatus: suspend (SubscriptionInvoice) -> String?,
    onChangeInvoice: suspend (invoice: SubscriptionInvoice, activateFree: Boolean) -> String?,
    onLogout: () -> Unit
) {
    val isBillingAuthorized = accessStatus.is_billing_authorized && accessStatus.organization_id.isNotBlank() && accessStatus.status != "PLATFORM"
    var selectedPlanId by remember(accessStatus.organization_id) { mutableStateOf("") }
    var requestId by remember(accessStatus.organization_id, selectedPlanId) { mutableStateOf(UUID.randomUUID().toString()) }
    val taskScope = rememberCoroutineScope()
    var busyAction by remember(accessStatus.organization_id) { mutableStateOf<String?>(null) }
    var actionError by remember(accessStatus.organization_id) { mutableStateOf<Pair<String, String>?>(null) }
    fun runAction(key: String, action: suspend () -> String?) {
        if (busyAction != null) return
        busyAction = key
        actionError = null
        taskScope.launch {
            try {
                actionError = action()?.let { key to it }
            } finally {
                busyAction = null
            }
        }
    }

    // If organization is RESTRICTED or SUSPENDED and current user is NOT authorized for billing (e.g., Teacher / Parent / Staff),
    // show the exact non-billing restricted screen without leaking invoice details.
    if ((accessStatus.status == "RESTRICTED" || accessStatus.status == "SUSPENDED") && !isBillingAuthorized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NeutralLightBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.LockClock,
                        contentDescription = "Üyelik Kısıtlı",
                        tint = StatusError,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Kurum Erişimi Kısıtlandı (HTTP 402)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Kurumunuzun SCS360 abonelik süresi ve 10 günlük ek kullanım (Grace) süresi sona ermiştir. Fatura ve ödeme detayları yalnızca kurum sahibi ve muhasebe yetkililerine açıktır. Lütfen kurum sahibi ile iletişime geçin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeutralTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onRefreshAccessStatus) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Üyelik Durumunu Yenile")
                        }
                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Çıkış Yap")
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("subscription_billing_list")
            .background(NeutralLightBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Hero Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                accessStatus.organization_name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                "Aktif Paket: ${accessStatus.plan_name} (${if (accessStatus.billing_period == "YEARLY") "Yıllık" else "Aylık"})",
                                fontSize = 13.sp,
                                color = NeutralTextSecondary
                            )
                        }
                        SaasStatusBadge(accessStatus.status)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Dönem: ${accessStatus.period_start} → ${accessStatus.period_end} • 10 Günlük Ek Süre Sonu: ${accessStatus.grace_until}",
                        fontSize = 12.sp,
                        color = NeutralTextPrimary
                    )

                    if (accessStatus.status == "GRACE") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = StatusWarningContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StatusWarning)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    accessStatus.warning ?: "Üyelik ek sürede. ${accessStatus.remaining_grace_days} gün kaldı.",
                                    fontSize = 12.sp,
                                    color = NeutralTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (accessStatus.status == "RESTRICTED" || accessStatus.status == "SUSPENDED") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = StatusErrorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    accessStatus.warning ?: "Kurum erişimi kısıtlı.",
                                    fontSize = 12.sp,
                                    color = StatusError,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRefreshAccessStatus,
                            modifier = Modifier.weight(1f).testTag("refresh_subscription_status_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Üyelik Durumunu Yenile", fontSize = 12.sp)
                        }
                        if (accessStatus.status == "RESTRICTED" || accessStatus.status == "SUSPENDED") {
                            Button(
                                onClick = onLogout,
                                colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                                modifier = Modifier.testTag("restricted_logout_btn")
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Çıkış Yap", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }


        // Quota Progress Bars
        if (isBillingAuthorized) item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Kurum Kota ve Kapasite Kullanımı", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    QuotaUsageRow(
                        title = "Aktif Şube Kotası",
                        subtitle = "Pasif şubeler kotadan düşer; yeniden aktifleştirmede kota denetlenir.",
                        used = accessStatus.active_branches,
                        limit = accessStatus.branch_limit
                    )
                    HorizontalDivider()
                    QuotaUsageRow(
                        title = "Aktif Öğrenci Kotası",
                        subtitle = "Silinen (soft-delete) ve pasif öğrenciler kotadan düşer.",
                        used = accessStatus.active_students,
                        limit = accessStatus.student_limit
                    )
                    HorizontalDivider()
                    QuotaUsageRow(
                        title = "Aktif Giriş Kullanıcısı Kotası",
                        subtitle = "SuperAdmin kotaya dahil edilmez; veli giriş hesapları kotaya dahildir.",
                        used = accessStatus.active_users,
                        limit = accessStatus.user_limit
                    )
                }
            }
        }

        // Renewal Package Selection & Payment Request
        if (isBillingAuthorized) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Üyelik paketleri", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (plans.isEmpty()) Text("Kullanılabilir paket bulunamadı.")
                        plans.forEach { plan ->
                            val selected = selectedPlanId == plan.id
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                val available = accessStatus.accepts_payment_requests && plan.is_active && plan.price == 0.0
                                if (available) {
                                    RadioButton(selected, onClick = { selectedPlanId = plan.id }, enabled = busyAction == null,
                                        modifier = Modifier.testTag("select_free_plan_${plan.id}"))
                                }
                                Column(Modifier.weight(1f).padding(8.dp)) {
                                    Text(plan.name, fontWeight = FontWeight.Bold)
                                    Text("${plan.currency} ${String.format(Locale.US, "%,.2f", plan.price)} / ${if (plan.billing_period == "YEARLY") "Yıl" else "Ay"}")
                                    Text("${plan.branch_limit} şube • ${plan.student_limit} öğrenci • ${plan.user_limit} kullanıcı",
                                        fontSize = 12.sp, color = NeutralTextSecondary)
                                }
                            }
                        }
                        val selectedPlan = plans.firstOrNull { it.id == selectedPlanId && it.is_active && it.price == 0.0 }
                        if (accessStatus.accepts_payment_requests && selectedPlan != null) {
                            actionError?.takeIf { it.first == selectedPlan.id }?.let {
                                Text(it.second, color = MaterialTheme.colorScheme.error)
                            }
                            Button(onClick = {
                                runAction(selectedPlan.id) {
                                    onRequestFreePlan(selectedPlan, requestId).also { if (it == null) requestId = UUID.randomUUID().toString() }
                                }
                            }, enabled = busyAction == null, modifier = Modifier.fillMaxWidth().testTag("request_free_package")) {
                                Text("Ücretsiz paket talebi oluştur")
                            }
                        }
                        Text("Ücretli uygulama içi üyelik satın alımı mağaza abonelik yapılandırması bekliyor.",
                            fontSize = 12.sp, color = NeutralTextSecondary)
                    }
                }
            }

            item {
                Text(
                    "Kurum SaaS Faturaları (${invoices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(invoices, key = { it.id }) { inv ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${inv.invoice_no} • ${inv.plan_name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "Son Geçerlilik: ${inv.valid_until} • Durum: ${inv.status}",
                                    fontSize = 12.sp,
                                    color = NeutralTextSecondary
                                )
                            }
                            Text(
                                "${inv.currency} ${String.format(Locale.US, "%,.2f", inv.amount)}",
                                fontWeight = FontWeight.ExtraBold,
                                color = when (inv.status) {
                                    "PAID" -> StatusSuccess
                                    "REFUNDED" -> StatusError
                                    else -> StatusWarning
                                }
                            )
                        }
                        inv.checkout_status?.let { Text("Online ödeme: $it") }
                        actionError?.takeIf { it.first == inv.id }?.let {
                            Text(it.second, color = MaterialTheme.colorScheme.error)
                        }
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (inv.checkout_status in setOf("INITIALIZING", "PENDING", "REVIEW")) {
                                OutlinedButton(enabled = busyAction == null, onClick = {
                                    runAction(inv.id) { onCheckPaymentStatus(inv) }
                                }, modifier = Modifier.testTag("saas_check_payment_btn_${inv.id}")) {
                                    Text("Ödeme sonucunu kontrol et")
                                }
                            }
                            if (inv.status == "PENDING" && inv.can_cancel && accessStatus.accepts_payment_requests) {
                                if (inv.amount == 0.0) {
                                    Button(enabled = busyAction == null, onClick = {
                                        runAction(inv.id) { onChangeInvoice(inv, true) }
                                    }, modifier = Modifier.testTag("activate_free_plan_${inv.id}")) {
                                        Text("Ücretsiz paketi etkinleştir")
                                    }
                                }
                                OutlinedButton(enabled = busyAction == null, onClick = {
                                    runAction(inv.id) { onChangeInvoice(inv, false) }
                                }, modifier = Modifier.testTag("cancel_subscription_invoice_${inv.id}")) {
                                    Text("Talebi iptal et")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun QuotaUsageRow(title: String, subtitle: String, used: Int, limit: Int) {
    val progress = if (limit > 0) (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(if (limit > 0) "$used / $limit" else "Bilgi yok", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ScsPetrolGreen)
        }
        Text(subtitle, fontSize = 11.sp, color = NeutralTextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (progress >= 0.9f) StatusWarning else ScsAccentGreen,
            trackColor = NeutralLightSurfaceVariant
        )
    }
}
