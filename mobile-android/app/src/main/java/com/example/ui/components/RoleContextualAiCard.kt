package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.NativeFinanceSummaryClient
import com.example.data.remote.NativeRecordedInsightsClient
import com.example.data.remote.RecordedInsight
import com.example.data.model.RecordedFinanceSummary
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.navigation.availableTo
import com.example.ui.viewmodel.Okul360UiState
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

@Composable
fun RoleContextualAiCard(
    screenName: String,
    state: Okul360UiState,
    modifier: Modifier = Modifier,
    onQuickActionClick: ((actionType: String) -> Unit)? = null,
    onOpenAiHubClick: (() -> Unit)? = null,
    initialExpanded: Boolean = false,
    isLoading: Boolean = false
) {
    val user = state.currentUser
    if (!state.isLoggedIn || user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) return
    val insight = when (screenName) {
        "DASHBOARD", "MENU", "REPORTS" -> RecordedInsight.OVERVIEW
        "EXAMS" -> RecordedInsight.GRADES
        "CLASSES" -> RecordedInsight.OPERATIONS
        "FINANCE" -> RecordedInsight.FINANCE
        else -> return
    }
    val branch = state.selectedBranchId
    fun has(vararg permissions: String) = NativeNavigationPolicy.has(user, *permissions)
    if (!insight.availableTo(user, branch)) return
    if (isLoading) {
        RoleContextualAiCardSkeleton(modifier = modifier)
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl, screenName) {
        var expanded by remember { mutableStateOf(initialExpanded) }
        var revision by remember { mutableIntStateOf(0) }
        Card(modifier.fillMaxWidth().testTag("recorded_insight_card")) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(insight.title, style = MaterialTheme.typography.titleMedium)
                }
                if (expanded) {
                    val figures by produceState<Result<List<String>>?>(null, revision) {
                        value = null
                        value = try {
                            Result.success(if (insight == RecordedInsight.FINANCE) {
                                financeFigures(NativeFinanceSummaryClient().load(branch))
                            } else formatFigures(insight, NativeRecordedInsightsClient().load(insight)))
                        } catch (failure: Exception) {
                            if (failure is CancellationException) throw failure
                            Result.failure(failure)
                        }
                    }
                    when {
                        figures == null -> Text("Kayıtlı göstergeler yükleniyor…")
                        figures?.isFailure == true -> Text("Kayıtlı göstergeler alınamadı.")
                        else -> figures?.getOrNull()?.forEach { Text(it) }
                    }
                    TextButton(enabled = figures != null, onClick = { revision++ }) { Text("Yenile") }
                    if (screenName == "DASHBOARD" && onOpenAiHubClick != null) {
                        TextButton(onClick = onOpenAiHubClick) { Text("Analiz ekranını aç") }
                    }
                    if (screenName == "REPORTS" && onQuickActionClick != null) {
                        TextButton(onClick = { onQuickActionClick("OPEN_ANALYTICS") }) { Text("Analiz ekranını aç") }
                    }
                    if (screenName == "EXAMS" && onQuickActionClick != null && has("exam.write")) {
                        TextButton(onClick = { onQuickActionClick("CREATE_EXAM") }) { Text("Sınav oluştur") }
                    }
                }
            }
        }
    }
}

private fun money(amount: BigDecimal?): String =
    amount?.let { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("tr-TR")).format(it) } ?: "—"

internal fun financeFigures(summary: RecordedFinanceSummary): List<String> = buildList {
    add(summary.asOf)
    add("Borç kaydı ${summary.debtCount}")
    add("Tamamlanan tahsilat ${summary.paymentCount}")
    add("Açık borç ${money(summary.outstandingDebt)}")
    add("Tahsilat oranı ${summary.collectionRate?.let { "%$it" } ?: "—"}")
    if (summary.requiresReview) add("Bazı finans kayıtları incelenmeli.")
    summary.historicalMonthly.forEach { row ->
        add("${row.month} · ${money(row.amount)} · ${row.paymentCount} tahsilat")
    }
}

private fun JSONObject.number(key: String, suffix: String = "", prefix: String = ""): String {
    check(has(key))
    if (isNull(key)) return "—"
    val value = getString(key).toBigDecimal()
    return prefix + value.stripTrailingZeros().toPlainString() + suffix
}

internal fun formatFigures(insight: RecordedInsight, data: JSONObject): List<String> = when (insight) {
    RecordedInsight.OVERVIEW -> {
        val metrics = data.getJSONObject("insights")
        listOf("${data.getString("period_start")} – ${data.getString("period_end")}",
            "Not ortalaması ${metrics.getJSONObject("academic_performance").number("average_grade", "/100")}",
            "Devam oranı ${metrics.getJSONObject("attendance").number("overall_rate", prefix = "%")}",
            "Borç tahsilat oranı ${metrics.getJSONObject("financial").number("collection_rate", prefix = "%")}",
            "Veli memnuniyeti ölçümü ${metrics.getJSONObject("parent_satisfaction").number("score")}")
    }
    RecordedInsight.GRADES -> listOf("${data.getString("period_start")} – ${data.getString("period_end")}",
        "Geçerli not kaydı ${data.getInt("valid_grade_count")}",
        "Not ortalaması ${data.number("average_grade", "/100")}",
        "Notu bulunmayan öğrenci ${data.getInt("students_without_grades")}")
    RecordedInsight.ATTENDANCE -> listOf("${data.getString("period_start")} – ${data.getString("period_end")}",
        "Yoklama kaydı ${data.getInt("total_records")}",
        "Devam oranı ${data.number("current_rate", prefix = "%")}",
        "Yoklaması bulunmayan öğrenci ${data.getInt("students_without_records")}")
    RecordedInsight.PARENTS -> {
        val records = data.getJSONObject("registration_metrics")
        val communication = data.getJSONObject("communication_metrics")
        listOf("Veli kaydı ${records.getInt("total_records")}",
            "İletişim bilgisi olan ${records.getInt("records_with_contact")}",
            "Gönderimi kayıtlı ${communication.getInt("sent_count")}",
            "Başarısız ${communication.getInt("failed_count")}",
            "Bekleyen ${communication.getInt("pending_count")}")
    }
    RecordedInsight.OPERATIONS -> {
        val counts = data.getJSONObject("counts")
        listOf("Etkin sınıf ${counts.getInt("class_count")}",
            "Etkin öğrenci ${counts.getInt("active_students")}",
            "Kayıtlı personel ${counts.getInt("staff_count")}",
            "Sınıf doluluğu ${data.getJSONObject("optimization_scores").number("classroom_utilization", prefix = "%")}")
    }
    RecordedInsight.FINANCE -> error("Finans ayrı kapsam doğrulaması kullanır")
}
