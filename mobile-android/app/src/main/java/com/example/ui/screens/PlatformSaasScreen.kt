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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.remote.PlatformInvoiceDraft
import com.example.data.remote.PlatformInvoiceOperation
import com.example.data.remote.TenantBillingUser
import com.example.data.remote.TenantMembershipDraft
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlatformSaasScreen(
    currentUserRole: UserRole,
    plans: List<SubscriptionPlan>,
    tenants: List<PlatformTenantSummary>,
    invoices: List<SubscriptionInvoice>,
    onCreatePlan: suspend (SubscriptionPlan) -> String?,
    onCreateTenantWithOwner: suspend (com.example.data.remote.TenantProvisionDraft) -> String?,
    onUpdateTenantSubscription: suspend (orgId: String, draft: TenantMembershipDraft) -> String?,
    onToggleTenantActive: suspend (orgId: String, isActive: Boolean) -> String?,
    onSaveInvoice: suspend (SubscriptionInvoice, PlatformInvoiceDraft) -> String?,
    onCheckInvoiceStatus: suspend (SubscriptionInvoice) -> String? = { "Ödeme durumu sorgulanamıyor." },
    onOpenTenantWorkspace: suspend (String) -> String? = { "Kurum çalışma alanı kullanılamıyor." },
    loadTenantBillingUsers: suspend (String) -> List<TenantBillingUser> = { emptyList() },
    onUpdatePlan: suspend (SubscriptionPlan) -> String? = { "Paket düzenleme kullanılamıyor." }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var showCreateTenantDialog by remember { mutableStateOf(false) }
    var editingTenant by remember { mutableStateOf<PlatformTenantSummary?>(null) }
    var invoiceAction by remember { mutableStateOf<Pair<SubscriptionInvoice, PlatformInvoiceOperation>?>(null) }
    val actionScope = rememberCoroutineScope()
    var savingPlanId by remember { mutableStateOf<String?>(null) }
    var failedPlanAction by remember { mutableStateOf<Pair<String, String>?>(null) }
    var checkingInvoiceId by remember { mutableStateOf<String?>(null) }
    var failedInvoiceStatus by remember { mutableStateOf<Pair<String, String>?>(null) }
    var tenantActionId by remember { mutableStateOf<String?>(null) }
    var tenantActionError by remember { mutableStateOf<Pair<String, String>?>(null) }
    fun runTenantAction(id: String, action: suspend () -> String?) {
        if (tenantActionId != null) return
        tenantActionId = id
        tenantActionError = null
        actionScope.launch {
            try {
                tenantActionError = action()?.let { id to it }
            } finally {
                tenantActionId = null
            }
        }
    }

    if (currentUserRole != UserRole.SUPERADMIN) {
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
                        Icons.Default.AdminPanelSettings,
                        contentDescription = "SuperAdmin Koruması",
                        tint = StatusError,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Platform Yönetimi Erişim Kısıtı (403)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Bu ekran yalnızca is_super_admin yetkisine sahip SCS360 Platform Yöneticisi için açıktır. Kurum yöneticileri ve personel bu alana erişemez.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeutralTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Yetkili hesabınızla oturum açın; yerel rol değişimi erişim sağlamaz.")
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeutralLightBackground)
    ) {
        val overviewHeader: @Composable () -> Unit = {
            Surface(color = ScsPetrolGreen, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SaaS yönetimi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Surface(color = ScsOrbitGold, shape = RoundedCornerShape(8.dp)) {
                            Text("Superadmin", color = ScsPetrolGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                    Text("Kurum, paket ve üyelik yönetimi", color = ScsMintPage, fontSize = 12.sp)
                    val compact = AdaptiveTheme.metrics.screenWidthDp / androidx.compose.ui.platform.LocalDensity.current.fontScale < 600
                    FlowRow(maxItemsInEachRow = if (compact) 1 else 3, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlatformMetricChip("Aktif kurum", "${tenants.count { it.is_active }}/${tenants.size}", Modifier.weight(1f))
                        PlatformMetricChip("Paket", "${plans.size}", Modifier.weight(1f))
                        PlatformMetricChip("Bekleyen fatura", "${invoices.count { it.status in setOf("PENDING", "PAYMENT_REVIEW", "PAID_REVIEW", "OPEN", "PENDING_VERIFICATION") }}", Modifier.weight(1f))
                    }
                }
            }
        }
        val tableTabs: @Composable () -> Unit = {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Kurumlar (${tenants.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Paketler (${plans.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Faturalar (${invoices.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
            )
        }

        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("platform_list_0"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { overviewHeader() }
                    stickyHeader { tableTabs() }
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Müşteri Kurumlar (Tenants)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showCreateTenantDialog = true },
                                modifier = Modifier.testTag("create_tenant_btn")
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Yeni Kurum + Sahip Ekle")
                            }
                        }
                    }

                    items(tenants, key = { it.organization_id }) { tenant ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
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
                                        Text(tenant.organization_name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(
                                            "Vergi No: ${tenant.tax_number} • Kurucu: ${tenant.owner_name} (${tenant.owner_email})",
                                            fontSize = 12.sp,
                                            color = NeutralTextSecondary
                                        )
                                    }
                                    SaasStatusBadge(if (!tenant.is_active) "SUSPENDED" else tenant.access_status)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "Paket: ${tenant.plan_name} (${if (tenant.billing_period == "YEARLY") "Yıllık" else "Aylık"}) • Bitiş: ${tenant.period_end} • Ek Süre (Grace): ${tenant.grace_until}",
                                    fontSize = 12.sp,
                                    color = NeutralTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuotaMiniPill("Şube", tenant.active_branches, tenant.branch_limit, Modifier.weight(1f))
                                    QuotaMiniPill("Öğrenci", tenant.active_students, tenant.student_limit, Modifier.weight(1f))
                                    QuotaMiniPill("Kullanıcı", tenant.active_users, tenant.user_limit, Modifier.weight(1f))
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                FilledTonalButton(
                                    enabled = tenantActionId == null,
                                    onClick = { runTenantAction(tenant.organization_id) { onOpenTenantWorkspace(tenant.organization_id) } },
                                    modifier = Modifier.fillMaxWidth().testTag("open_tenant_workspace_${tenant.organization_id}"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Kurum çalışma alanını aç",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { editingTenant = tenant },
                                        enabled = tenantActionId == null,
                                        modifier = Modifier.testTag("edit_tenant_sub_${tenant.organization_id}")
                                    ) {
                                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Üyelik / Süre Güncelle", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        enabled = tenantActionId == null,
                                        onClick = {
                                            runTenantAction(tenant.organization_id) {
                                                onToggleTenantActive(tenant.organization_id, !tenant.is_active)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (tenant.is_active) StatusError else StatusSuccess
                                        ),
                                        modifier = Modifier.testTag("toggle_tenant_status_${tenant.organization_id}")
                                    ) {
                                        Text(
                                            if (tenant.is_active) "Kurumu Kapat (Askıya Al)" else "Kurumu Yeniden Aç",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                tenantActionError?.takeIf { it.first == tenant.organization_id }?.let {
                                    Text(it.second, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("platform_list_1"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { overviewHeader() }
                    stickyHeader { tableTabs() }
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Tanımlı SaaS Paketleri",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showCreatePlanDialog = true },
                                modifier = Modifier.testTag("create_plan_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Yeni Paket Oluştur")
                            }
                        }
                    }

                    items(plans, key = { it.id }) { plan ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Kod: ${plan.code} • ${plan.description}", fontSize = 12.sp, color = NeutralTextSecondary)
                                    }
                                    Text(
                                        "${plan.currency} ${String.format(Locale.US, "%,.2f", plan.price)} / ${if (plan.billing_period == "YEARLY") "Yıl" else "Ay"}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ScsPetrolGreen,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(onClick = {}, label = { Text("Şube: ${plan.branch_limit}", fontSize = 11.sp) })
                                    AssistChip(onClick = {}, label = { Text("Öğrenci: ${plan.student_limit}", fontSize = 11.sp) })
                                    AssistChip(onClick = {}, label = { Text("Kullanıcı: ${plan.user_limit}", fontSize = 11.sp) })
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SaasStatusBadge(if (plan.is_active) "ACTIVE" else "SUSPENDED")
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { editingPlan = plan },
                                            enabled = savingPlanId == null,
                                            modifier = Modifier.testTag("edit_plan_${plan.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Düzenle", fontSize = 12.sp)
                                        }
                                        FilledTonalButton(
                                            enabled = savingPlanId == null,
                                            onClick = {
                                                if (savingPlanId == null) {
                                                    failedPlanAction = null
                                                    savingPlanId = plan.id
                                                    actionScope.launch {
                                                        try {
                                                            failedPlanAction = onUpdatePlan(plan.copy(is_active = !plan.is_active))
                                                                ?.let { plan.id to it }
                                                        } finally {
                                                            savingPlanId = null
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("toggle_plan_active_${plan.id}")
                                        ) {
                                            Text(if (plan.is_active) "Pasife Al" else "Aktifleştir", fontSize = 12.sp)
                                        }
                                    }
                                }
                                failedPlanAction?.takeIf { it.first == plan.id }?.let {
                                    Text(it.second, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("platform_list_2"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { overviewHeader() }
                    stickyHeader { tableTabs() }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusInfoContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = StatusInfo)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Manuel Havale/EFT doğrulaması tutar, para birimi ve banka referansını kontrol eder. Farklı faturada aynı banka referansı 409 Conflict ile reddedilir. Kapalı kurumda ödeme doğrulansa bile kurum otomatik açılmaz.",
                                    fontSize = 12.sp,
                                    color = NeutralTextPrimary
                                )
                            }
                        }
                    }

                    items(invoices, key = { it.id }) { inv ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${inv.invoice_no} • ${inv.organization_name}", fontWeight = FontWeight.Bold)
                                        Text(
                                            "${inv.plan_name} (${inv.billing_period}) • Geçerlilik: ${inv.valid_until}",
                                            fontSize = 12.sp,
                                            color = NeutralTextSecondary
                                        )
                                    }
                                    Text(
                                        "${inv.currency} ${String.format(Locale.US, "%,.2f", inv.amount)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ScsPetrolGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Fatura: ${inv.status}",
                                    fontSize = 12.sp,
                                    color = when (inv.status) {
                                        "PAID" -> StatusSuccess
                                        "REFUNDED" -> StatusError
                                        else -> StatusWarning
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                                inv.checkout_status?.let { Text("Online ödeme: $it", fontSize = 12.sp) }
                                failedInvoiceStatus?.takeIf { it.first == inv.id }?.let {
                                    Text(it.second, color = MaterialTheme.colorScheme.error)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (inv.status == "PENDING" && inv.can_cancel) {
                                        Button(
                                            onClick = { invoiceAction = inv to PlatformInvoiceOperation.SETTLE },
                                            enabled = checkingInvoiceId == null,
                                            modifier = Modifier.testTag("verify_invoice_btn_${inv.id}")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ödemeyi Doğrula", fontSize = 11.5.sp)
                                        }
                                    }
                                    if (inv.checkout_status in setOf("INITIALIZING", "PENDING", "REVIEW")) {
                                        OutlinedButton(enabled = checkingInvoiceId == null, onClick = {
                                            if (checkingInvoiceId == null) {
                                                checkingInvoiceId = inv.id
                                                failedInvoiceStatus = null
                                                actionScope.launch {
                                                    try {
                                                        failedInvoiceStatus = onCheckInvoiceStatus(inv)?.let { inv.id to it }
                                                    } finally {
                                                        checkingInvoiceId = null
                                                    }
                                                }
                                            }
                                        }, modifier = Modifier.testTag("platform_check_payment_${inv.id}")) {
                                            Text("Ödeme sonucunu kontrol et")
                                        }
                                    }
                                    if (inv.status == "PAID_REVIEW") {
                                        Button(
                                            onClick = { invoiceAction = inv to PlatformInvoiceOperation.APPLY },
                                            enabled = checkingInvoiceId == null,
                                            colors = ButtonDefaults.buttonColors(containerColor = ScsPetrolGreen),
                                            modifier = Modifier.testTag("reconcile_invoice_btn_${inv.id}")
                                        ) {
                                            Icon(Icons.Default.Rule, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ödemeyi Uzlaştır (PAID_REVIEW)", fontSize = 11.5.sp)
                                        }
                                    }
                                    if (inv.status == "PAID_REVIEW") {
                                        OutlinedButton(
                                            onClick = { invoiceAction = inv to PlatformInvoiceOperation.REFUND_RECORDED },
                                            enabled = checkingInvoiceId == null,
                                            modifier = Modifier.testTag("refund_invoice_btn_${inv.id}")
                                        ) {
                                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Dış İade Kaydet", fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlanDialog) {
        PlatformPlanEditor(null, onCreatePlan) { showCreatePlanDialog = false }
    }

    if (showCreateTenantDialog) {
        TenantCreateDialog(plans, onCreateTenantWithOwner, onDismiss = { showCreateTenantDialog = false })
    }

    editingTenant?.let { tenant ->
        MembershipEditorDialog(tenant, plans, loadTenantBillingUsers, onUpdateTenantSubscription, onDismiss = { editingTenant = null })
    }

    invoiceAction?.let { (invoice, operation) ->
        PlatformInvoiceEditor(invoice, operation, onSaveInvoice) { invoiceAction = null }
    }

    editingPlan?.let { plan ->
        PlatformPlanEditor(plan, onUpdatePlan) { editingPlan = null }
    }
}

@Composable
private fun PlatformMetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, modifier = Modifier.weight(1f), color = ScsMintPage, fontSize = 11.sp, maxLines = 2)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
fun SaasStatusBadge(status: String) {
    val (bg, fg, label) = when (status.uppercase()) {
        "ACTIVE" -> Triple(StatusSuccessContainer, StatusSuccess, "AKTİF")
        "GRACE" -> Triple(StatusWarningContainer, StatusWarning, "EK SÜRE (GRACE)")
        "RESTRICTED" -> Triple(StatusErrorContainer, StatusError, "KISITLI (402)")
        "SUSPENDED" -> Triple(StatusErrorContainer, StatusError, "ASKIDA / KAPALI")
        else -> Triple(NeutralLightSurfaceVariant, NeutralTextSecondary, status)
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun QuotaMiniPill(title: String, used: Int, limit: Int, modifier: Modifier = Modifier) {
    val overLimit = used >= limit
    Surface(
        color = if (overLimit) StatusWarningContainer else NeutralLightSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, fontSize = 11.sp, color = NeutralTextSecondary)
            Text(
                "$used / $limit",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (overLimit) StatusWarning else NeutralTextPrimary
            )
        }
    }
}
