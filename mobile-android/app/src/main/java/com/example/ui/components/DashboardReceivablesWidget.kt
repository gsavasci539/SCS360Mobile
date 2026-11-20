package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Invoice
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun DashboardReceivablesWidget(state: Okul360UiState, onOpenQuickPayment: () -> Unit, onSelectInvoiceForPayment: (Invoice) -> Unit,
    onSendPaymentReminder: (String, String) -> Unit, onNavigateToFinance: () -> Unit, modifier: Modifier = Modifier) {
    val actor = state.currentUser
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(actor, "finance.read") ||
        actor.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) return
    val canWrite = NativeNavigationPolicy.has(actor, "finance.write")
    val branch = state.selectedBranchId.takeUnless { it.isBlank() || it == "all" }
    val summary = state.recordedFinanceSummary?.takeIf { it.branchId == branch }
    key(financePresentationScope(state), state.serverBaseUrl) {
        var showDebts by remember { mutableStateOf(false) }
        val currency = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("tr-TR")) }
        OutlinedCard(modifier.fillMaxWidth().testTag("widget_uncollected_fees")) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tahsil edilmemiş ücretler", style = MaterialTheme.typography.titleMedium)
                summary?.outstandingDebt?.let { Text("Kalan borç · ${currency.format(it)}") }
                summary?.overdueAmount?.let { Text("Vadesi geçmiş kalan · ${currency.format(it)}") }
                summary?.collectionRate?.let { Text("Tahsilat oranı · %${it.toPlainString()}") }
                if (summary == null) {
                    Text(state.financeSummaryError ?: if (state.isFinanceLoading) "Finans özeti yükleniyor…" else "Finans özeti alınamadı.")
                }
                FilterChip(!showDebts, onClick = { showDebts = false }, label = { Text("Kayıtlı tahsilat geçmişi") })
                FilterChip(showDebts, onClick = { showDebts = true }, label = { Text("Bekleyen borçlar") })
                if (!showDebts && summary != null) {
                    if (summary.historicalMonthly.isEmpty()) Text("Tamamlanmış tahsilat geçmişi yok.")
                    summary.historicalMonthly.forEach { month ->
                        Text("${month.month} · ${month.amount?.let(currency::format) ?: "Tutar bilinmiyor"} · ${month.paymentCount} kayıt")
                    }
                }
                if (showDebts) when {
                    state.isFinanceRowsLoading -> Text("Borç listesi yükleniyor…")
                    state.financeRowsError != null -> Text(state.financeRowsError)
                    else -> {
                        val debts = state.invoices.filter { invoice ->
                            invoice.status.uppercase(Locale.ROOT) in setOf("PENDING", "UNPAID", "PARTIAL", "PARTIALLY_PAID", "OVERDUE", "BEKLIYOR", "GECIKTI") &&
                                invoice.amount.isFinite() && invoice.paid_amount.isFinite() && invoice.amount > invoice.paid_amount
                        }.sortedBy { invoice -> !invoice.status.equals("OVERDUE", ignoreCase = true) }.take(3)
                        if (debts.isEmpty()) Text("Bu listede açık borç bulunamadı.")
                        debts.forEach { invoice ->
                            key(invoice.id) {
                                val amount = BigDecimal.valueOf(invoice.amount).subtract(BigDecimal.valueOf(invoice.paid_amount))
                                Text(invoice.student_name, style = MaterialTheme.typography.titleSmall)
                                Text("${currency.format(amount)} · ${invoice.due_date}")
                                if (canWrite) {
                                    TextButton(onClick = { onSelectInvoiceForPayment(invoice) }) { Text("Tahsilatı aç") }
                                    TextButton(onClick = { onSendPaymentReminder(invoice.id, invoice.student_name) }) {
                                        Text("SMS hatırlatmasını kuyruğa al")
                                    }
                                }
                            }
                        }
                    }
                }
                if (canWrite) TextButton(onClick = onOpenQuickPayment) { Text("Hızlı tahsilatı aç") }
                TextButton(onClick = onNavigateToFinance) { Text("Finans kayıtlarını aç") }
            }
        }
    }
}
