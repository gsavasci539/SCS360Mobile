package com.example.ui.screens

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
import androidx.compose.ui.unit.dp
import com.example.data.remote.DailyReportAnalysis
import com.example.data.remote.DailyReportHttpException
import com.example.data.remote.NativeDailyReportClient
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun DailyReportAnalysisPanel(state: Okul360UiState, modifier: Modifier = Modifier, listRevision: Int = 0) {
    if (!NativeNavigationPolicy.has(state.currentUser, "daily_reports.read")) return

    val scope = listOf(state.currentUser.id, state.currentUser.resolvedTenantId, state.currentUser.branch_ids,
        state.workspaceOrganizationId, state.selectedBranchId, state.serverBaseUrl)
    var expanded by remember(scope) { mutableStateOf(false) }
    var retry by remember(scope) { mutableIntStateOf(0) }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Karne analizini gizle" else "Karne analizini göster")
            }
            if (expanded) key(scope, state.dailyReportSavedRevision, listRevision, retry) {
                val result by produceState<Result<DailyReportAnalysis>?>(null) {
                    try {
                        value = Result.success(NativeDailyReportClient().analysis(state.selectedBranchId))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        value = Result.failure(failure)
                    }
                }
                when (val response = result) {
                    null -> Text("Karne analizi yükleniyor…")
                    else -> response.fold(
                        onSuccess = { DailyReportAnalysisContent(it) },
                        onFailure = { failure ->
                            val denied = (failure as? DailyReportHttpException)?.code == 403
                            Text(if (denied) "Karne analizine erişim yetkiniz yok." else "Karne analizi yüklenemedi.",
                                color = MaterialTheme.colorScheme.error)
                            if (!denied) TextButton(onClick = { retry++ }) { Text("Tekrar dene") }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyReportAnalysisContent(analysis: DailyReportAnalysis) {
    val format = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("tr-TR")).apply { maximumFractionDigits = 1 } }
    Text("${analysis.total} günlük karne", style = MaterialTheme.typography.titleSmall)
    listOf("Mutlu oranı" to analysis.mood, "Uyudu oranı" to analysis.sleep, "Tam yedi oranı" to analysis.meals)
        .forEach { (label, metric) ->
            val value = metric.percent?.let { "%${format.format(it)} · ${metric.observations} gözlem" } ?: "Gözlem yok"
            Text("$label · $value")
        }
    if (analysis.followUps.isNotEmpty()) {
        Text("Takip edilebilecek gözlemler", style = MaterialTheme.typography.titleSmall)
        analysis.followUps.take(3).forEach { student ->
            Text(student.studentName)
            Text("${student.mood} yorgunluk veya huzursuzluk · ${student.sleep} kısa uyku veya uyumama · ${student.meals} az yenen veya yenmeyen öğün",
                style = MaterialTheme.typography.bodySmall)
        }
    }
    analysis.notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
}
