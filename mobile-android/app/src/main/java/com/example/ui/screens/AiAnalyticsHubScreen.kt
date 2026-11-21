package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.NativeFinanceSummaryClient
import com.example.data.remote.NativeRecordedInsightsClient
import com.example.data.remote.RecordedInsight
import com.example.ui.components.financePresentationScope
import com.example.ui.components.financeFigures
import com.example.ui.components.formatFigures
import com.example.ui.navigation.availableTo
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException

@Composable
fun AiAnalyticsHubScreen(state: Okul360UiState, onBack: () -> Unit) {
    val user = state.currentUser
    if (!state.isLoggedIn || user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) {
        Column(Modifier.padding(16.dp)) {
            Text("Özetler için yetkili kurum çalışma alanını açın.")
            TextButton(onClick = onBack) { Text("Geri") }
        }
        return
    }
    val branch = state.selectedBranchId
    val tabs = RecordedInsight.entries.filter { it.availableTo(user, branch) }
    key(financePresentationScope(state), state.serverBaseUrl) {
        RecordedAnalyticsHub(tabs, branch, onBack)
    }
}

@Composable
private fun RecordedAnalyticsHub(tabs: List<RecordedInsight>, branch: String, onBack: () -> Unit) {
    var tab by remember(tabs) { mutableStateOf(tabs.firstOrNull()) }
    var days by remember { mutableIntStateOf(30) }
    var months by remember { mutableIntStateOf(6) }
    var revision by remember { mutableIntStateOf(0) }
    val selected = tab
    val loadKey = when (selected) {
        RecordedInsight.FINANCE -> months
        RecordedInsight.OPERATIONS, null -> 0
        else -> days
    }
    val result by key(selected, loadKey, revision) {
        produceState<Result<List<String>>?>(null) {
            if (selected != null) {
                value = try {
                    Result.success(if (selected == RecordedInsight.FINANCE) {
                        financeFigures(NativeFinanceSummaryClient().load(branch, months))
                    } else formatFigures(selected, NativeRecordedInsightsClient().load(selected, days)))
                } catch (failure: Exception) {
                    if (failure is CancellationException) throw failure
                    Result.failure(failure)
                }
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Kayıtlı analizler", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Geri") }
            if (tabs.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tabs.forEach { item ->
                    FilterChip(selected = item == selected, onClick = { tab = item }, label = { Text(item.title) })
                }
            }
        }
        if (selected == null) item {
            val hasBranch = branch.isNotBlank() && branch != "all"
            Text(if (hasBranch) "Bu şube için yetkinize açık özet yok." else "Yetkiniz kapsamında kayıtlı özet yok.")
        } else item {
            val periods = when (selected) {
                RecordedInsight.FINANCE -> listOf(1, 3, 6, 12, 24)
                RecordedInsight.OPERATIONS -> emptyList()
                else -> listOf(7, 30, 90)
            }
            if (periods.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                periods.forEach { period ->
                    FilterChip(selected = period == loadKey, onClick = {
                        if (selected == RecordedInsight.FINANCE) months = period else days = period
                    }, label = { Text(if (selected == RecordedInsight.FINANCE) "$period ay" else "$period gün") })
                }
            }
            TextButton(onClick = { revision++ }, enabled = result != null) { Text("Yenile") }
            when {
                result == null -> Text("Kayıtlı göstergeler yükleniyor…")
                result?.isFailure == true -> Text("Göstergeler alınamadı.")
                else -> Card(Modifier.fillMaxWidth().testTag("recorded_analytics_result")) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        result?.getOrNull()?.forEach { Text(it) }
                    }
                }
            }
        }
    }
}
