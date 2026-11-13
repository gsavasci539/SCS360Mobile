package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.remote.NativeStaffClient
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    val canRead = NativeNavigationPolicy.has(state.currentUser, "user.read")
    val canReview = NativeNavigationPolicy.has(state.currentUser, "user.write")
    suspend fun refresh() {
        val nextLeaves = api.leaves()
        val nextDuties = api.duties()
        if (api.current()) { leaves = nextLeaves; duties = nextDuties; loaded = true }
    }
    fun run(action: (suspend () -> Unit)? = null) {
        if (busy || !canRead) return
        busy = true; error = ""
        scope.launch {
            try {
                action?.invoke(); refresh()
                if (api.current() && action != null) saved++
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (api.current()) { loaded = false; error = "İşlem doğrulanamadı. Listeyi yenileyip kontrol edin; aynı talebi tekrar göndermeyin." }
            } finally { if (api.current()) busy = false }
        }
    }
    LaunchedEffect(canRead) { run() }
    if (!api.current()) return
    Column(Modifier.fillMaxSize()) {
        if (!canRead) {
            Text("Personel kayıtlarını görme yetkiniz yok.", Modifier.padding(16.dp))
            TextButton(onClick = onNavigateBack) { Text("Geri") }
        } else {
            TextButton(enabled = !busy, onClick = { run() }) { Text("Listeyi yenile") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error.isNotBlank()) Text(error, Modifier.padding(12.dp))
            StaffManagementScreen(state.copy(leaveRequests = leaves, dutyRosters = duties), onNavigateBack,
                actionsEnabled = loaded && !busy, canReview = canReview, savedRevision = saved, operationError = error,
                onSubmitLeaveRequest = { type, start, end, _, reason ->
                    run { api.requestLeave(state.currentUser, type, start, end, reason) }
                },
                onReviewLeaveRequest = { id, approved, comment ->
                    if (canReview) run { api.reviewLeave(id, approved, comment) }
                })
        }
    }
}
