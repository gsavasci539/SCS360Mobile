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
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlatformSaasScreen(
    currentUserRole: UserRole,
    plans: List<SubscriptionPlan>,
    tenants: List<PlatformTenantSummary>,
    invoices: List<SubscriptionInvoice>,
    onCreatePlan: (SubscriptionPlan) -> Unit,
    onCreateTenantWithOwner: suspend (com.example.data.remote.TenantProvisionDraft) -> String?,
    onUpdateTenantSubscription: suspend (orgId: String, planId: String, periodStart: String, periodEnd: String) -> String?,
    onToggleTenantActive: (orgId: String, isActive: Boolean) -> Unit,
    onVerifyInvoicePayment: (invoiceId: String, amount: Double, currency: String, bankRef: String, paidAt: String) -> Unit,
    onOpenTenantWorkspace: (orgId: String, orgName: String) -> Unit = { _, _ -> },
    onReconcilePaidReviewInvoice: (invoiceId: String, periodStart: String, periodEnd: String, note: String) -> Unit = { _, _, _, _ -> },
    onRecordSubscriptionRefund: (invoiceId: String, refundRef: String, note: String) -> Unit = { _, _, _ -> },
    onUpdatePlan: (SubscriptionPlan) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var showCreateTenantDialog by remember { mutableStateOf(false) }
    var editingTenant by remember { mutableStateOf<PlatformTenantSummary?>(null) }
    var verifyingInvoice by remember { mutableStateOf<SubscriptionInvoice?>(null) }
    var reconcilingInvoice by remember { mutableStateOf<SubscriptionInvoice?>(null) }
    var refundingInvoice by remember { mutableStateOf<SubscriptionInvoice?>(null) }

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
                                    onClick = { onOpenTenantWorkspace(tenant.organization_id, tenant.organization_name) },
                                    modifier = Modifier.fillMaxWidth().testTag("open_tenant_workspace_${tenant.organization_id}"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Kurum Çalışma Alanını Aç (X-SCS360-Organization-Id)",
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
                                        modifier = Modifier.testTag("edit_tenant_sub_${tenant.organization_id}")
                                    ) {
                                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Üyelik / Süre Güncelle", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onToggleTenantActive(tenant.organization_id, !tenant.is_active) },
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
                                        "${plan.currency} ${String.format(Locale.US, "%,.0f", plan.price)} / ${if (plan.billing_period == "YEARLY") "Yıl" else "Ay"}",
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
                                            modifier = Modifier.testTag("edit_plan_${plan.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Düzenle", fontSize = 12.sp)
                                        }
                                        FilledTonalButton(
                                            onClick = { onUpdatePlan(plan.copy(is_active = !plan.is_active)) },
                                            modifier = Modifier.testTag("toggle_plan_active_${plan.id}")
                                        ) {
                                            Text(if (plan.is_active) "Pasife Al" else "Aktifleştir", fontSize = 12.sp)
                                        }
                                    }
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
                                    "Durum: ${inv.status} ${inv.bank_reference?.let { "• Ref: $it" } ?: ""} ${inv.refund_reference?.let { "• İade Ref: $it" } ?: ""}",
                                    fontSize = 12.sp,
                                    color = when (inv.status) {
                                        "PAID" -> StatusSuccess
                                        "REFUNDED" -> StatusError
                                        else -> StatusWarning
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (inv.status == "OPEN" || inv.status == "PENDING_VERIFICATION") {
                                        Button(
                                            onClick = { verifyingInvoice = inv },
                                            modifier = Modifier.testTag("verify_invoice_btn_${inv.id}")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ödemeyi Doğrula", fontSize = 11.5.sp)
                                        }
                                    }
                                    if (inv.status == "PAID_REVIEW") {
                                        Button(
                                            onClick = { reconcilingInvoice = inv },
                                            colors = ButtonDefaults.buttonColors(containerColor = ScsPetrolGreen),
                                            modifier = Modifier.testTag("reconcile_invoice_btn_${inv.id}")
                                        ) {
                                            Icon(Icons.Default.Rule, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ödemeyi Uzlaştır (PAID_REVIEW)", fontSize = 11.5.sp)
                                        }
                                    }
                                    if (inv.status == "PAID" || inv.status == "PAID_REVIEW") {
                                        OutlinedButton(
                                            onClick = { refundingInvoice = inv },
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
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("KURUMSAL_PLUS") }
        var price by remember { mutableStateOf("18500") }
        var billingPeriod by remember { mutableStateOf("MONTHLY") }
        var branchLimit by remember { mutableStateOf("5") }
        var studentLimit by remember { mutableStateOf("2000") }
        var userLimit by remember { mutableStateOf("350") }

        AlertDialog(
            onDismissRequest = { showCreatePlanDialog = false },
            title = { Text("Yeni SaaS Paketi Oluştur", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Paket Adı") }, singleLine = true)
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Paket Kodu") }, singleLine = true)
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Fiyat (TRY)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = billingPeriod == "MONTHLY",
                            onClick = { billingPeriod = "MONTHLY" },
                            label = { Text("Aylık (MONTHLY)") }
                        )
                        FilterChip(
                            selected = billingPeriod == "YEARLY",
                            onClick = { billingPeriod = "YEARLY" },
                            label = { Text("Yıllık (YEARLY)") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = branchLimit,
                            onValueChange = { branchLimit = it },
                            label = { Text("Şube") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = studentLimit,
                            onValueChange = { studentLimit = it },
                            label = { Text("Öğrenci") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = userLimit,
                            onValueChange = { userLimit = it },
                            label = { Text("Kullanıcı") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreatePlan(
                                SubscriptionPlan(
                                    id = "plan-${System.currentTimeMillis()}",
                                    code = code.uppercase(),
                                    name = name.trim(),
                                    price = price.toDoubleOrNull() ?: 15000.0,
                                    billing_period = billingPeriod,
                                    branch_limit = branchLimit.toIntOrNull() ?: 5,
                                    student_limit = studentLimit.toIntOrNull() ?: 1000,
                                    user_limit = userLimit.toIntOrNull() ?: 200
                                )
                            )
                            showCreatePlanDialog = false
                        }
                    }
                ) {
                    Text("Paketi Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlanDialog = false }) { Text("İptal") }
            }
        )
    }

    if (showCreateTenantDialog) {
        TenantCreateDialog(plans, onCreateTenantWithOwner, onDismiss = { showCreateTenantDialog = false })
    }

    editingTenant?.let { tenant ->
        MembershipEditorDialog(tenant, plans, onUpdateTenantSubscription, onDismiss = { editingTenant = null })
    }

    verifyingInvoice?.let { inv ->
        var amount by remember { mutableStateOf(inv.amount.toString()) }
        var bankRef by remember { mutableStateOf("TR-EFT-${System.currentTimeMillis() % 100000}") }
        var paidAt by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { verifyingInvoice = null },
            title = { Text("Manuel Ödeme Doğrula (${inv.invoice_no})", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kurum: ${inv.organization_name}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Ödenen Tutar (${inv.currency})") }, singleLine = true)
                    OutlinedTextField(value = bankRef, onValueChange = { bankRef = it }, label = { Text("Banka Dekont / Referans No") }, singleLine = true)
                    OutlinedTextField(value = paidAt, onValueChange = { paidAt = it }, label = { Text("Ödeme Zamanı (UTC)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onVerifyInvoicePayment(inv.id, amount.toDoubleOrNull() ?: inv.amount, inv.currency, bankRef.trim(), paidAt.trim())
                        verifyingInvoice = null
                    }
                ) {
                    Text("Doğrula ve Dönemi Uzat")
                }
            },
            dismissButton = {
                TextButton(onClick = { verifyingInvoice = null }) { Text("Vazgeç") }
            }
        )
    }

    reconcilingInvoice?.let { inv ->
        var periodStart by remember { mutableStateOf("") }
        var periodEnd by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("Tahsilat onaylandı; çakışan üyelik dönem tarihleri kontrol edilerek uzlaştırıldı.") }

        AlertDialog(
            onDismissRequest = { reconcilingInvoice = null },
            title = { Text("PAID_REVIEW Ödeme Uzlaştırma (${inv.invoice_no})", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ödeme alınmış ancak üyelik penceresi çakışması nedeniyle incelemeye ayrılmış faturayı dönem tarihlerini belirleyerek PAID durumuna geçirin.",
                        fontSize = 12.sp,
                        color = NeutralTextSecondary
                    )
                    OutlinedTextField(value = periodStart, onValueChange = { periodStart = it }, label = { Text("Dönem Başlangıç (YYYY-MM-DD)") }, singleLine = true)
                    OutlinedTextField(value = periodEnd, onValueChange = { periodEnd = it }, label = { Text("Dönem Bitiş (YYYY-MM-DD)") }, singleLine = true)
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Uzlaştırma Açıklaması") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReconcilePaidReviewInvoice(inv.id, periodStart.trim(), periodEnd.trim(), note.trim())
                        reconcilingInvoice = null
                    }
                ) {
                    Text("Uzlaştır ve Aktifleştir")
                }
            },
            dismissButton = {
                TextButton(onClick = { reconcilingInvoice = null }) { Text("Vazgeç") }
            }
        )
    }

    refundingInvoice?.let { inv ->
        var refundRef by remember { mutableStateOf("IYZ-REF-${System.currentTimeMillis() % 100000}") }
        var note by remember { mutableStateOf("Banka/ödeme sağlayıcısı panelinde tamamlanan iade referansı kaydedildi.") }

        AlertDialog(
            onDismissRequest = { refundingInvoice = null },
            title = { Text("Dış İade Kaydet (${inv.invoice_no})", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Not: Bu işlem banka veya ödeme sağlayıcısında para transferi başlatmaz; yalnızca doğrulanmış dış iade referansını muhasebe kaydı olarak işler.",
                        fontSize = 12.sp,
                        color = NeutralTextSecondary
                    )
                    OutlinedTextField(value = refundRef, onValueChange = { refundRef = it }, label = { Text("Dış İade Referans No") }, singleLine = true)
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("İade Notu") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (refundRef.isNotBlank()) {
                            onRecordSubscriptionRefund(inv.id, refundRef.trim(), note.trim())
                            refundingInvoice = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("İade Kaydını İşle")
                }
            },
            dismissButton = {
                TextButton(onClick = { refundingInvoice = null }) { Text("Vazgeç") }
            }
        )
    }

    editingPlan?.let { plan ->
        var planName by remember(plan) { mutableStateOf(plan.name) }
        var planPrice by remember(plan) { mutableStateOf(plan.price.toInt().toString()) }
        var branchLimit by remember(plan) { mutableStateOf(plan.branch_limit.toString()) }
        var studentLimit by remember(plan) { mutableStateOf(plan.student_limit.toString()) }
        var userLimit by remember(plan) { mutableStateOf(plan.user_limit.toString()) }
        var planDesc by remember(plan) { mutableStateOf(plan.description) }

        AlertDialog(
            onDismissRequest = { editingPlan = null },
            title = { Text("${plan.name} • Paketi Düzenle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = planName, onValueChange = { planName = it }, label = { Text("Paket Adı") }, singleLine = true)
                    OutlinedTextField(value = planPrice, onValueChange = { planPrice = it }, label = { Text("Fiyat (TRY)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = branchLimit, onValueChange = { branchLimit = it }, label = { Text("Şube") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = studentLimit, onValueChange = { studentLimit = it }, label = { Text("Öğrenci") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = userLimit, onValueChange = { userLimit = it }, label = { Text("Kullanıcı") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = planDesc, onValueChange = { planDesc = it }, label = { Text("Açıklama") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePlan(
                            plan.copy(
                                name = planName.ifBlank { plan.name },
                                price = planPrice.toDoubleOrNull() ?: plan.price,
                                branch_limit = (branchLimit.toIntOrNull() ?: plan.branch_limit).coerceAtLeast(1),
                                student_limit = (studentLimit.toIntOrNull() ?: plan.student_limit).coerceAtLeast(1),
                                user_limit = (userLimit.toIntOrNull() ?: plan.user_limit).coerceAtLeast(1),
                                description = planDesc
                            )
                        )
                        editingPlan = null
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlan = null }) { Text("İptal") }
            }
        )
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
