package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.DashboardApprovals
import com.example.data.remote.DashboardExcuse
import com.example.data.remote.NativeDashboardApprovalsClient
import com.example.data.remote.RegistryHttpException
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.screens.ConsentFormDialog
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun DashboardApprovalsWidget(state: Okul360UiState, onNavigateToConsents: () -> Unit,
    onNavigateToInfirmary: () -> Unit, onNavigateToFinance: () -> Unit, modifier: Modifier = Modifier) {
    val user = state.currentUser
    val canSeeDashboard = NativeNavigationPolicy.has(user, "dashboard.view", "dashboard.read")
    val canSeeMedicine = NativeNavigationPolicy.has(user, "health.read")
    val canReview = NativeNavigationPolicy.has(user, "students.write")
    val canSeeFinance = NativeNavigationPolicy.has(user, "finance.read")
    val branch = state.selectedBranchId.takeUnless { it.isBlank() || it == "all" }
    if (!state.isLoggedIn || user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) return
    key(financePresentationScope(state), state.serverBaseUrl) {
        val client = remember { NativeDashboardApprovalsClient() }
        var data by remember { mutableStateOf<Result<DashboardApprovals>?>(null) }
        var attempt by remember { mutableIntStateOf(0) }
        var excuse by remember { mutableStateOf<DashboardExcuse?>(null) }
        var notice by remember { mutableStateOf<String?>(null) }
        var cashTab by remember { mutableStateOf(!canSeeDashboard && canSeeFinance) }
        var showExcuses by remember { mutableStateOf(false) }
        LaunchedEffect(attempt) {
            if (branch == null || !canSeeDashboard || !canSeeMedicine && !canReview) return@LaunchedEffect
            data = null
            data = try {
                Result.success(client.pending(branch, canSeeMedicine, canReview))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Result.failure(failure)
            }
        }
        OutlinedCard(modifier.fillMaxWidth().testTag("widget_cash_and_approvals")) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kasa ve bekleyen onaylar", style = MaterialTheme.typography.titleMedium)
                if (canSeeFinance && canSeeDashboard) {
                    FilterChip(!cashTab, onClick = { cashTab = false }, label = { Text("Bekleyen onaylar") })
                    FilterChip(cashTab, onClick = { cashTab = true }, label = { Text("Tahsilat") })
                }
                if (cashTab && canSeeFinance) {
                    val summary = state.recordedFinanceSummary?.takeIf { it.branchId == branch }
                    val format = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("tr-TR")) }
                    val collected = summary?.historicalMonthly?.find { it.month == summary.asOf.take(7) }?.amount
                    if (summary != null) {
                        Text("Bu ay kayıtlı tahsilat · ${collected?.let(format::format) ?: "Tutar bulunamadı"}")
                        summary.outstandingDebt?.let { Text("Toplam kalan borç · ${format.format(it)}") }
                    } else {
                        Text(state.financeSummaryError ?: if (state.isFinanceLoading) "Tahsilat özeti yükleniyor…" else "Tahsilat özeti alınamadı.")
                    }
                    TextButton(onClick = onNavigateToFinance) { Text("Finans kayıtlarını aç") }
                } else if (canSeeDashboard) {
                    notice?.let { Text(it) }
                    val reply = data
                    val approvals = reply?.getOrNull()
                    when {
                        !canSeeMedicine && !canReview -> Text("Bekleyen onayları görüntüleme yetkiniz yok.")
                        branch == null -> Text("Bekleyen onayları görmek için bir şube seçin.")
                        reply == null -> Text("Bekleyen onaylar yükleniyor…")
                        approvals == null -> {
                            Text(reply.exceptionOrNull()?.message ?: "Bekleyen onaylar yüklenemedi.")
                            OutlinedButton(onClick = { attempt++ }) { Text("Yeniden yükle") }
                        }
                        else -> {
                            if (canSeeMedicine) Text("Bekleyen ilaç talebi · ${approvals.medicineCount}")
                            if (canReview) Text("Bekleyen mazeret · ${approvals.excuseCount}")
                            if (approvals.excuses.isNotEmpty()) {
                                TextButton(onClick = { showExcuses = !showExcuses }) {
                                    Text(if (showExcuses) "Mazeretleri gizle" else "Mazeretleri göster")
                                }
                            }
                            if (showExcuses) approvals.excuses.forEach { pending ->
                                key(pending.id) {
                                    Text(pending.studentName, style = MaterialTheme.typography.titleSmall)
                                    Text("${pending.start} – ${pending.end}")
                                    TextButton(onClick = { notice = null; excuse = pending }) { Text("Mazereti incele") }
                                }
                            }
                            if (showExcuses && approvals.excuseCount > approvals.excuses.size) {
                                Text("En yeni ${approvals.excuses.size} bekleyen talep gösteriliyor.")
                            }
                        }
                    }
                    if (canSeeMedicine) TextButton(onClick = onNavigateToInfirmary) { Text("Revir kayıtlarını aç") }
                    if (NativeNavigationPolicy.has(user, "consents.read")) {
                        TextButton(onClick = onNavigateToConsents) { Text("Muvafakatnameleri aç") }
                    }
                }
            }
        }
        excuse?.let { selected ->
            DashboardExcuseReview(client, selected, user.id, { excuse = null }) { feedback ->
                excuse = null
                notice = feedback
                data = null
                attempt++
            }
        }
    }
}

@Composable
private fun DashboardExcuseReview(client: NativeDashboardApprovalsClient, excuse: DashboardExcuse, actorId: String,
    onClose: () -> Unit, onReviewed: (String) -> Unit) {
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun submit(status: String) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                client.review(excuse, actorId, status, note.trim())
                onReviewed("Mazeret kararı kaydedildi.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if ((failure as? RegistryHttpException)?.code == 409) {
                    onReviewed("Talep daha önce değerlendirilmiş. Liste yeniden okundu.")
                } else {
                    error = failure.message ?: "Mazeret kararı doğrulanamadı."
                }
            } finally {
                busy = false
            }
        }
    }
    ConsentFormDialog("Mazeret talebi", "dashboard_excuse_review", busy, onClose, {
        item { Text(excuse.studentName) }
        item { Text("${excuse.start} – ${excuse.end}") }
        item { Text(excuse.description) }
        item {
            OutlinedTextField(note, { note = it.take(2000) }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                label = { Text("Değerlendirme notu") })
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { OutlinedButton(enabled = !busy, onClick = { submit("REJECTED") }) { Text("Talebi reddet") } }
    }) {
        Button(enabled = !busy, onClick = { submit("APPROVED") }) { Text("Talebi onayla") }
    }
}
