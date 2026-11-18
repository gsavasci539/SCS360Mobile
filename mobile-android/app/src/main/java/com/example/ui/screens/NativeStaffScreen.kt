package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.remote.NativeStaffClient
import com.example.data.remote.RegistryHttpException
import com.example.data.remote.StaffLeaveDecision
import com.example.data.remote.StaffLeaveDraft
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private class StaffLeaveOperation(
    val submit: suspend () -> Unit,
    val matches: (TeacherLeaveRequest) -> Boolean,
)

@Composable
fun NativeStaffScreen(state: Okul360UiState, onNavigateBack: () -> Unit) {
    key(financePresentationScope(state)) { StaffContent(state, onNavigateBack) }
}

@Composable
private fun StaffContent(state: Okul360UiState, onNavigateBack: () -> Unit) {
    val api = remember { NativeStaffClient() }
    val scope = rememberCoroutineScope()
    var leaves by remember { mutableStateOf<List<TeacherLeaveRequest>>(emptyList()) }
    var duties by remember { mutableStateOf<List<TeacherDutyRoster>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var saved by remember { mutableIntStateOf(0) }
    var showDutyForm by remember { mutableStateOf(false) }
    var pendingLeave by remember { mutableStateOf<StaffLeaveOperation?>(null) }
    val selectedBranchId = state.selectedBranchId.takeUnless { it == "all" }
    val canRead = NativeNavigationPolicy.has(state.currentUser, "user.read")
    val canReview = NativeNavigationPolicy.has(state.currentUser, "user.write")
    val canRequestLeave = state.currentUser.is_super_admin != true &&
        !state.currentUser.organization_id.isNullOrBlank()
    fun refresh() {
        if (busy || !canRead) return
        busy = true; error = ""
        scope.launch {
            try {
                val nextLeaves = api.leaves()
                val nextDuties = api.duties(selectedBranchId)
                if (api.current()) { leaves = nextLeaves; duties = nextDuties; loaded = true }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (api.current()) { loaded = false; error = "Personel kayıtları yüklenemedi. Listeyi yenileyin." }
            } finally { if (api.current()) busy = false }
        }
    }
    fun performLeave(operation: StaffLeaveOperation, checkOnly: Boolean) {
        if (busy || !canRead) return
        busy = true
        error = ""
        scope.launch {
            var received = false
            try {
                if (!checkOnly) {
                    operation.submit()
                    received = true
                }
                val confirmed = api.confirmLeave(operation.matches)
                if (api.current() && confirmed != null) {
                    leaves = confirmed
                    pendingLeave = null
                    saved++
                } else if (api.current()) {
                    pendingLeave = operation
                    error = "Değişiklik henüz listede görünmüyor."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (api.current()) {
                    if (!checkOnly && !received && failure is RegistryHttpException &&
                        failure.code in setOf(401, 403, 404, 422)) {
                        error = "İşlem reddedildi. Bilgileri ve yetkinizi kontrol edin."
                    } else {
                        pendingLeave = operation
                        error = "İşlem doğrulanamadı. Sonucu tekrar kontrol edebilirsiniz."
                    }
                }
            } finally {
                if (api.current()) busy = false
            }
        }
    }
    LaunchedEffect(canRead) { refresh() }
    if (!api.current()) return
    Column(Modifier.fillMaxSize()) {
        if (!canRead) {
            Text("Personel kayıtlarını görme yetkiniz yok.", Modifier.padding(16.dp))
            TextButton(onClick = onNavigateBack) { Text("Geri") }
        } else {
            TextButton(enabled = !busy, onClick = ::refresh) { Text("Listeyi yenile") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error.isNotBlank()) Text(error, Modifier.padding(12.dp))
            StaffManagementScreen(state.copy(leaveRequests = leaves, dutyRosters = duties), onNavigateBack,
                actionsEnabled = loaded && !busy && pendingLeave == null, canReview = canReview,
                canRequestLeave = canRequestLeave, savedRevision = saved, operationError = error, leaveBusy = busy,
                onCheckLeaveResult = pendingLeave?.let { attempt -> { performLeave(attempt, checkOnly = true) } },
                onDismissLeaveDialog = { pendingLeave = null; error = "" },
                onOpenDutyForm = if (canReview) ({ showDutyForm = true }) else null,
                onSubmitLeaveRequest = { type, start, end, _, reason ->
                    if (canRequestLeave) {
                        val draft = StaffLeaveDraft(state.currentUser, type, start, end, reason)
                        performLeave(StaffLeaveOperation({ api.requestLeave(draft) }, draft::matches), checkOnly = false)
                    }
                },
                onReviewLeaveRequest = { id, approved, comment ->
                    if (canReview) {
                        val decision = StaffLeaveDecision(id, approved, comment)
                        performLeave(StaffLeaveOperation({ api.reviewLeave(decision) }, decision::matches), checkOnly = false)
                    }
                })
            if (showDutyForm && canReview) {
                NativeDutyDialog(api, selectedBranchId, onDismiss = { showDutyForm = false }) { confirmed ->
                    duties = confirmed
                    showDutyForm = false
                }
            }
        }
    }
}
