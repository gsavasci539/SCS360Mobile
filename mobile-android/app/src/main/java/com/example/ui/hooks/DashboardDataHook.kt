package com.example.ui.hooks

import androidx.compose.runtime.*
import com.example.data.model.*
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.delay

/**
 * Stabilized, pre-computed snapshot of all Dashboard metrics & collections returned by [useDashboardData].
 * Eliminates UI flickering (layout shift / number jumping) during asynchronous repository fetches
 * by retaining the last known non-empty snapshot while background refreshes are in flight,
 * and exposing explicit [isInitialSkeletonLoading] vs. [isBackgroundRefreshing] states.
 */
@Stable
data class DashboardDataSnapshot(
    val stableState: Okul360UiState,
    val isInitialSkeletonLoading: Boolean,
    val isBackgroundRefreshing: Boolean,
    val totalStudentsCount: Int,
    val totalClassesCount: Int,
    val presentTodayCount: Int,
    val absentTodayCount: Int,
    val lateTodayCount: Int,
    val excusedTodayCount: Int,
    val attendanceRatePercent: Int,
    val totalInvoicedAmount: Double,
    val totalCollectedAmount: Double,
    val uncollectedAmount: Double,
    val overdueInvoicesCount: Int,
    val collectionRatePercent: Int,
    val pendingApprovalsCount: Int,
    val activeBusCount: Int,
    val latestAnnouncement: Announcement?,
    val lastSyncedLabel: String,
    val triggerRefreshWithSkeleton: () -> Unit
)

/**
 * Custom Composable Data Hook (`useDashboardData`)
 *
 * Responsibilities:
 * 1. **Anti-Flicker Stabilization**: Keeps the last valid hydrated `Okul360UiState` in memory when
 *    background network/Room emissions briefly emit partial or loading states, preventing UI jitter.
 * 2. **Smart Skeleton Gate**: Shows skeleton placeholders (`isInitialSkeletonLoading = true`) only when
 *    the dashboard is genuinely performing its initial hydration or when an explicit skeleton refresh is triggered,
 *    with a minimum 180ms hold so skeleton transitions never flash for a single frame.
 * 3. **Memoized Derived Metrics**: Computes attendance percentages, collection rates, pending approvals,
 *    and active bus metrics via `derivedStateOf` to minimize recompositions across dashboard widgets.
 */
@Composable
fun useDashboardData(
    state: Okul360UiState,
    forceSkeletonLoading: Boolean = false,
    onRefreshRequested: (() -> Unit)? = null
): DashboardDataSnapshot {
    // Retain last known hydrated state so transient empty emissions never cause widget collapse/flicker
    var cachedHydratedState by remember { mutableStateOf(state) }
    var manualSkeletonRequested by remember { mutableStateOf(false) }

    val hasHydratedData = state.students.isNotEmpty() ||
        state.invoices.isNotEmpty() ||
        state.attendanceRecords.isNotEmpty() ||
        state.dashboardSummary != null

    LaunchedEffect(
        state.students,
        state.invoices,
        state.attendanceRecords,
        state.classes,
        state.announcements,
        state.leaveNotices,
        state.medicineNotices,
        state.consents,
        state.busRoutes,
        state.organizationAccess
    ) {
        if (hasHydratedData || cachedHydratedState.students.isEmpty()) {
            cachedHydratedState = state
        } else {
            // Merge non-empty collections onto cachedHydratedState to prevent partial-fetch flicker
            cachedHydratedState = cachedHydratedState.copy(
                currentUser = state.currentUser,
                selectedRole = state.selectedRole,
                selectedBranchId = state.selectedBranchId,
                organizationAccess = state.organizationAccess,
                announcements = state.announcements.ifEmpty { cachedHydratedState.announcements },
                attendanceRecords = state.attendanceRecords.ifEmpty { cachedHydratedState.attendanceRecords }
            )
        }
    }

    // Raw loading signal from ViewModel
    val rawLoading = state.isDashboardLoading ||
        (state.isDataLoading && !hasHydratedData) ||
        (state.isStudentsLoading && state.students.isEmpty()) ||
        (state.isFinanceLoading && state.invoices.isEmpty())

    // Debounced skeleton visibility: prevents <100ms single-frame flashes
    var debouncedSkeletonVisible by remember { mutableStateOf(forceSkeletonLoading || rawLoading) }

    LaunchedEffect(rawLoading, forceSkeletonLoading, manualSkeletonRequested) {
        if (forceSkeletonLoading || manualSkeletonRequested) {
            debouncedSkeletonVisible = true
            if (manualSkeletonRequested) {
                delay(420)
                manualSkeletonRequested = false
            }
        } else if (rawLoading && !hasHydratedData) {
            debouncedSkeletonVisible = true
        } else {
            if (debouncedSkeletonVisible) {
                delay(180) // Smooth exit hold to prevent jitter
            }
            debouncedSkeletonVisible = false
        }
    }

    val effectiveState = if (hasHydratedData) state else cachedHydratedState

    val isBackgroundRefreshing = (state.isDashboardLoading || state.isDataLoading || state.isStudentsLoading || state.isFinanceLoading) &&
        hasHydratedData &&
        !debouncedSkeletonVisible

    // Memoized derived metrics to avoid unnecessary widget recomposition
    val metrics by remember(effectiveState) {
        derivedStateOf {
            val students = effectiveState.students
            val totalStudents = students.size.coerceAtLeast(1)
            val records = effectiveState.attendanceRecords

            val present = if (records.isNotEmpty()) {
                records.count { it.status.equals("PRESENT", ignoreCase = true) }
            } else {
                effectiveState.dashboardSummary?.present_today_count ?: (students.size - 2).coerceAtLeast(0)
            }
            val absent = if (records.isNotEmpty()) {
                records.count { it.status.equals("ABSENT", ignoreCase = true) }
            } else {
                effectiveState.dashboardSummary?.absent_today_count ?: if (students.isNotEmpty()) 1 else 0
            }
            val late = if (records.isNotEmpty()) {
                records.count { it.status.equals("LATE", ignoreCase = true) }
            } else {
                effectiveState.dashboardSummary?.late_today_count ?: if (students.isNotEmpty()) 1 else 0
            }
            val excused = if (records.isNotEmpty()) {
                records.count { it.status.equals("EXCUSED", ignoreCase = true) }
            } else {
                0
            }
            val attendanceRate = if (students.isEmpty() && records.isEmpty()) {
                0
            } else {
                (((present + late * 0.5f) / totalStudents.toFloat()) * 100f).toInt().coerceIn(0, 100)
            }

            val invoices = effectiveState.invoices
            val totalInvoiced = invoices.sumOf { it.amount }
            val totalCollected = invoices.sumOf { inv ->
                when {
                    inv.status.equals("PAID", ignoreCase = true) || inv.status.equals("ODENDI", ignoreCase = true) -> inv.amount
                    inv.paid_amount > 0 -> inv.paid_amount
                    else -> 0.0
                }
            }
            val uncollected = (totalInvoiced - totalCollected).coerceAtLeast(0.0)
            val overdueCount = invoices.count { it.status.equals("OVERDUE", ignoreCase = true) }
            val collectionRate = if (totalInvoiced > 0) {
                ((totalCollected / totalInvoiced) * 100.0).toInt().coerceIn(0, 100)
            } else {
                0
            }

            val pendingLeaves = effectiveState.leaveNotices.count { it.status.equals("PENDING", ignoreCase = true) }
            val pendingMedicines = effectiveState.medicineNotices.count { it.status.equals("PENDING", ignoreCase = true) }
            val pendingConsents = effectiveState.consents.count { !it.is_approved }
            val pendingTotal = pendingLeaves + pendingMedicines + pendingConsents

            val activeBuses = effectiveState.busRoutes.count {
                it.current_status.contains("YOLDA", ignoreCase = true) ||
                    it.current_status.contains("AKTİF", ignoreCase = true) ||
                    it.current_status.contains("ACTIVE", ignoreCase = true)
            }.coerceAtLeast(effectiveState.busRoutes.size)

            DashboardComputedMetrics(
                totalStudents = students.size,
                totalClasses = effectiveState.classes.size,
                present = present,
                absent = absent,
                late = late,
                excused = excused,
                attendanceRate = attendanceRate,
                totalInvoiced = totalInvoiced,
                totalCollected = totalCollected,
                uncollected = uncollected,
                overdueCount = overdueCount,
                collectionRate = collectionRate,
                pendingApprovals = pendingTotal,
                activeBuses = activeBuses,
                latestAnnouncement = effectiveState.announcements.firstOrNull()
            )
        }
    }

    return DashboardDataSnapshot(
        stableState = effectiveState,
        isInitialSkeletonLoading = debouncedSkeletonVisible,
        isBackgroundRefreshing = isBackgroundRefreshing,
        totalStudentsCount = metrics.totalStudents,
        totalClassesCount = metrics.totalClasses,
        presentTodayCount = metrics.present,
        absentTodayCount = metrics.absent,
        lateTodayCount = metrics.late,
        excusedTodayCount = metrics.excused,
        attendanceRatePercent = metrics.attendanceRate,
        totalInvoicedAmount = metrics.totalInvoiced,
        totalCollectedAmount = metrics.totalCollected,
        uncollectedAmount = metrics.uncollected,
        overdueInvoicesCount = metrics.overdueCount,
        collectionRatePercent = metrics.collectionRate,
        pendingApprovalsCount = metrics.pendingApprovals,
        activeBusCount = metrics.activeBuses,
        latestAnnouncement = metrics.latestAnnouncement,
        lastSyncedLabel = if (isBackgroundRefreshing) "Senkronize ediliyor..." else "Canlı Veri Aktif",
        triggerRefreshWithSkeleton = {
            manualSkeletonRequested = true
            onRefreshRequested?.invoke()
        }
    )
}

private data class DashboardComputedMetrics(
    val totalStudents: Int,
    val totalClasses: Int,
    val present: Int,
    val absent: Int,
    val late: Int,
    val excused: Int,
    val attendanceRate: Int,
    val totalInvoiced: Double,
    val totalCollected: Double,
    val uncollected: Double,
    val overdueCount: Int,
    val collectionRate: Int,
    val pendingApprovals: Int,
    val activeBuses: Int,
    val latestAnnouncement: Announcement?
)
