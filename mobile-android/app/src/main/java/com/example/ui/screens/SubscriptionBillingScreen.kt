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
import java.util.Locale

@Composable
fun SubscriptionBillingScreen(
    currentUserRole: UserRole,
    accessStatus: OrganizationAccessStatus,
    plans: List<SubscriptionPlan>,
    invoices: List<SubscriptionInvoice>,
    onRefreshAccessStatus: () -> Unit,
    onCreateRenewalRequest: (planId: String, billingPeriod: String) -> Unit,
    onSimulateAccessStatusChange: (String) -> Unit,
    onStartCardCheckout: (invoiceId: String, buyerName: String, buyerEmail: String, billingAddress: String) -> Unit = { _, _, _, _ -> },
    onCheckPaymentStatus: (invoiceId: String) -> Unit = {},
    onActivateFreePlan: (planId: String) -> Unit = {},
    onCancelPendingInvoice: (invoiceId: String) -> Unit = {},
    onLogout: () -> Unit
) {
    val isBillingAuthorized = accessStatus.is_billing_authorized && (currentUserRole == UserRole.SUPERADMIN ||
        currentUserRole == UserRole.OWNER ||
        currentUserRole == UserRole.ADMIN ||
        currentUserRole == UserRole.MANAGER ||
        currentUserRole == UserRole.FINANCE)

    var selectedPlanId by remember(accessStatus.plan_id) { mutableStateOf(accessStatus.plan_id) }
    var selectedPeriod by remember(accessStatus.billing_period) { mutableStateOf(accessStatus.billing_period) }
    var checkoutInvoice by remember { mutableStateOf<SubscriptionInvoice?>(null) }

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
                                    "Üyelik döneminiz sona erdi. 10×24 saatlik ek süre (GRACE) içindesiniz (${accessStatus.remaining_grace_days} gün kaldı). Hizmet kesintisi yaşamamak için lütfen yenileme talebi oluşturun.",
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
                                    "SUBSCRIPTION_REQUIRED (402): Ek kullanım süresi dolduğu için operasyonel modüller kısıtlanmıştır. Ödeme doğrulandığında erişim otomatik olarak açılacaktır.",
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
        item {
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
                        Text("Paket Seçimi & Yenileme Ödeme Talebi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Fiyat sunucu tarafından paket tanımından hesaplanır. Aynı paket için açık ödeme talebi varsa tekrar kullanılır; paket değişirse eski açık talep iptal edilir (Geçerlilik: 1 Gün).",
                            fontSize = 12.sp,
                            color = NeutralTextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        plans.forEach { plan ->
                            val selected = selectedPlanId == plan.id
                            Surface(
                                color = if (selected) ScsContainerLight else NeutralLightSurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = { selectedPlanId = plan.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "${plan.branch_limit} Şube • ${plan.student_limit} Öğrenci • ${plan.user_limit} Kullanıcı",
                                            fontSize = 12.sp,
                                            color = NeutralTextSecondary
                                        )
                                    }
                                    Text(
                                        "${plan.currency} ${String.format(Locale.US, "%,.0f", plan.price)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ScsPetrolGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedPeriod == "MONTHLY",
                                onClick = { selectedPeriod = "MONTHLY" },
                                label = { Text("Aylık Yenileme") }
                            )
                            FilterChip(
                                selected = selectedPeriod == "YEARLY",
                                onClick = { selectedPeriod = "YEARLY" },
                                label = { Text("Yıllık Yenileme") }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        val chosenPlan = plans.find { it.id == selectedPlanId }
                        if (chosenPlan != null && chosenPlan.price <= 0.0) {
                            Button(
                                onClick = { onActivateFreePlan(selectedPlanId) },
                                colors = ButtonDefaults.buttonColors(containerColor = ScsPetrolGreen),
                                modifier = Modifier.fillMaxWidth().testTag("activate_free_plan_btn")
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ücretsiz Paketi Etkinleştir (0 TL • Sağlayıcısız)")
                            }
                        } else {
                            Button(
                                onClick = { onCreateRenewalRequest(selectedPlanId, selectedPeriod) },
                                modifier = Modifier.fillMaxWidth().testTag("create_renewal_invoice_btn")
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Yenileme Faturası / Ödeme Talebi Oluştur")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = NeutralLightSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (accessStatus.saas_iyzico_enabled) Icons.Default.CreditCard else Icons.Default.CreditCardOff,
                                    contentDescription = null,
                                    tint = if (accessStatus.saas_iyzico_enabled) ScsPetrolGreen else NeutralTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (accessStatus.saas_iyzico_enabled) {
                                        "Uygulama içi üyelik satın alımı mağaza abonelik yapılandırması bekliyor. Kurumun okul/kurs tahsilatı ayrı bir ödeme akışıdır."
                                    } else {
                                        "Uygulama içi üyelik satın alımı henüz açık değil. Bu ekrandaki üyelik ve ödeme durumları sunucudan okunur."
                                    },
                                    fontSize = 11.sp,
                                    color = NeutralTextSecondary
                                )
                            }
                        }
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
                        if (inv.status == "OPEN" || inv.status == "PENDING_VERIFICATION") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (inv.amount > 0.0 && accessStatus.saas_iyzico_enabled) {
                                    Button(
                                        onClick = { checkoutInvoice = inv },
                                        modifier = Modifier.weight(1f).testTag("saas_card_checkout_btn_${inv.id}"),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kartla Öde", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onCheckPaymentStatus(inv.id) },
                                    modifier = Modifier.weight(1f).testTag("saas_check_payment_btn_${inv.id}"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ödeme Sonucunu Kontrol Et", fontSize = 10.5.sp, maxLines = 1)
                                }
                                TextButton(
                                    onClick = { onCancelPendingInvoice(inv.id) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = StatusError),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Text("İptal Et", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    checkoutInvoice?.let { inv ->
        var buyerName by remember { mutableStateOf(accessStatus.organization_name) }
        var buyerEmail by remember { mutableStateOf("muhasebe@scs360.com.tr") }
        var billingAddress by remember { mutableStateOf("Ataşehir / İstanbul") }

        AlertDialog(
            onDismissRequest = { checkoutInvoice = null },
            title = { Text("Kartla Öde • Hosted Checkout (${inv.invoice_no})", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = StatusInfoContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "Güvenlik: Kart numarası, son kullanma tarihi ve CVV bilgileri SCS360 içinde toplanmaz; doğrulanmış HTTPS Hosted Checkout oturumuna yönlendirilirsiniz.",
                            fontSize = 11.5.sp,
                            color = NeutralTextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    OutlinedTextField(value = buyerName, onValueChange = { buyerName = it }, label = { Text("Alıcı / Kurum Unvanı") }, singleLine = true)
                    OutlinedTextField(value = buyerEmail, onValueChange = { buyerEmail = it }, label = { Text("Fatura E-Postası") }, singleLine = true)
                    OutlinedTextField(value = billingAddress, onValueChange = { billingAddress = it }, label = { Text("Fatura Adresi") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStartCardCheckout(inv.id, buyerName.trim(), buyerEmail.trim(), billingAddress.trim())
                        checkoutInvoice = null
                    }
                ) {
                    Text("Güvenli Ödeme Sayfasını Başlat")
                }
            },
            dismissButton = {
                TextButton(onClick = { checkoutInvoice = null }) { Text("Kapat") }
            }
        )
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
