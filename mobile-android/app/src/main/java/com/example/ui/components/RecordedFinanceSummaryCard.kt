package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.RecordedFinanceSummary
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FinanceDataUnavailable(message: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) { Text(message, Modifier.padding(16.dp)) }
}

@Composable
fun RecordedFinanceSummaryCard(
    summary: RecordedFinanceSummary?, loading: Boolean, error: String?, onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("tr-TR"))
    fun money(value: BigDecimal?) = value?.let { currency.format(it) } ?: "Bilinmiyor"
    Card(modifier.fillMaxWidth().testTag("recorded_finance_summary")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Kayıtlı borç ve tahsilat özeti", style = MaterialTheme.typography.titleMedium)
            when {
                loading -> { CircularProgressIndicator(Modifier.size(32.dp)); Text("Finans özeti yükleniyor…") }
                error != null || summary == null -> {
                    Text(error ?: "Finans özeti henüz yüklenmedi.")
                    Button(onRetry, Modifier.heightIn(min = 48.dp).testTag("finance_summary_retry")) { Text("Yeniden yükle") }
                }
                else -> {
                    Text("${summary.asOf} itibarıyla • Seçili erişim kapsamındaki tüm kayıtlar")
                    if (summary.requiresReview) Text("Bazı tutar veya tarihler incelenmeli. Bilinmeyen değerler toplama katılmaz.", color = MaterialTheme.colorScheme.error)
                    listOf("Toplam tahakkuk" to summary.totalDebt, "Borçlara işlenen ödeme" to summary.totalPaid,
                        "Kalan borç" to summary.outstandingDebt, "Vadesi geçmiş kalan" to summary.overdueAmount,
                        "Tamamlanmış tahsilat" to summary.collectedTotal).forEach { (title, value) ->
                        Column(Modifier.fillMaxWidth()) {
                            Text(title, style = MaterialTheme.typography.labelLarge)
                            Text(money(value), fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Tahsilat oranı: ${summary.collectionRate?.let { "%${it.toPlainString()}" } ?: "Bilinmiyor"}")
                    Text("${summary.debtCount} borç kaydı • ${summary.paymentCount} tahsilat kaydı")
                    Text("Kayıtlı aylık tahsilatlar", style = MaterialTheme.typography.titleSmall)
                    if (summary.historicalMonthly.isEmpty()) Text("Tamamlanmış tahsilat geçmişi yok.")
                    summary.historicalMonthly.forEach { row ->
                        Text("${row.month}: ${money(row.amount)} • ${row.paymentCount} kayıt")
                    }
                    Text("Gider ve net kasa bu özetin kapsamında değil. Gelir tahmini üretilmez.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
