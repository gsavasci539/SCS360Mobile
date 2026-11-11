package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import com.example.data.model.Invoice
import com.example.data.model.Student
import com.example.ui.components.AnnouncementCardSkeleton
import com.example.ui.components.DashboardHeroHeaderSkeleton
import com.example.ui.components.InteractiveDashboardWidgetsSection
import com.example.ui.components.QuickShortcutStripSkeleton
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.SimplifiedActionTileSkeleton
import com.example.ui.hooks.useDashboardData
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

data class DashboardActionTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: Okul360UiState,
    onNavigateToAttendance: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToAnnouncements: () -> Unit,
    onNavigateToDailyReports: () -> Unit,
    onNavigateToClasses: () -> Unit,
    onNavigateToCommunication: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToRoles: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onOpenAddStudent: () -> Unit,
    onOpenCreateAnnouncement: () -> Unit,
    onNavigateToDigitalId: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToTransport: () -> Unit = {},
    onNavigateToCanteen: () -> Unit = {},
    onNavigateToHomework: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToStaff: () -> Unit = {},
    onNavigateToAiAnalytics: () -> Unit = {},
    onNavigateToMenu: () -> Unit = {},
    onOpenQuickAttendance: () -> Unit = onNavigateToAttendance,
    onOpenQuickPayment: () -> Unit = onNavigateToFinance,
    onSelectInvoiceForPayment: (Invoice) -> Unit = {},
    onSendPaymentReminder: (String, String) -> Unit = { _, _ -> },
    onApproveLeaveNotice: (String) -> Unit = {},
    onMarkMedicineGiven: (String) -> Unit = {},
    onApproveConsent: (String, String) -> Unit = { _, _ -> },
    onNavigateToConsents: () -> Unit = {},
    onNavigateToDismissal: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onSelectStudent: (Student) -> Unit = {},
    onUpdateAttendanceStatus: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // Anti-flicker custom data hook: stabilizes state & manages skeleton loading
    val dashboardData = useDashboardData(state = state)
    val stableState = dashboardData.stableState
    val spacing = DesignTokens.spacing

    // Quick Action Bottom Sheet / Drawer Modal state
    var showQuickActionModal by rememberSaveable { mutableStateOf(false) }
    val primaryActions = remember(
        dashboardData.totalStudentsCount,
        dashboardData.totalClassesCount,
        dashboardData.attendanceRatePercent,
        dashboardData.collectionRatePercent
    ) {
        listOf(
            DashboardActionTile(
                id = "attendance",
                title = "Yoklama",
                subtitle = "Bugün %${dashboardData.attendanceRatePercent} katılım",
                icon = Icons.Default.FactCheck,
                iconBgColor = Color(0xFFE6F4EA),
                iconColor = Color(0xFF137333),
                onClick = onNavigateToAttendance
            ),
            DashboardActionTile(
                id = "students",
                title = "Öğrenciler",
                subtitle = "${dashboardData.totalStudentsCount} Kayıtlı",
                icon = Icons.Default.Groups,
                iconBgColor = Color(0xFFE8F0FE),
                iconColor = PrimaryBlue,
                onClick = onNavigateToStudents
            ),
            DashboardActionTile(
                id = "classes",
                title = "Sınıflar",
                subtitle = "${dashboardData.totalClassesCount} Şube",
                icon = Icons.Default.School,
                iconBgColor = Color(0xFFF3E8FF),
                iconColor = Color(0xFF6D28D9),
                onClick = onNavigateToClasses
            ),
            DashboardActionTile(
                id = "schedule",
                title = "Ders Programı",
                subtitle = "Haftalık takvim",
                icon = Icons.Default.CalendarMonth,
                iconBgColor = Color(0xFFE0F2FE),
                iconColor = StatusInfo,
                onClick = onNavigateToSchedule
            ),
            DashboardActionTile(
                id = "transport",
                title = "Servis & GPS",
                subtitle = "${dashboardData.activeBusCount} Aktif Hat",
                icon = Icons.Default.DirectionsBus,
                iconBgColor = Color(0xFFFFEFE6),
                iconColor = Color(0xFFC2410C),
                onClick = onNavigateToTransport
            ),
            DashboardActionTile(
                id = "canteen",
                title = "Kantin Cüzdan",
                subtitle = "Temassız bakiye",
                icon = Icons.Default.Fastfood,
                iconBgColor = Color(0xFFE6F8ED),
                iconColor = StatusSuccess,
                onClick = onNavigateToCanteen
            ),
            DashboardActionTile(
                id = "homework",
                title = "Ödev & Teslim",
                subtitle = "Puanlama & takip",
                icon = Icons.Default.Assignment,
                iconBgColor = Color(0xFFFCE7F3),
                iconColor = Color(0xFFBE185D),
                onClick = onNavigateToHomework
            ),
            DashboardActionTile(
                id = "finance",
                title = "Kasa & Finans",
                subtitle = "%${dashboardData.collectionRatePercent} Tahsilat",
                icon = Icons.Default.AccountBalanceWallet,
                iconBgColor = Color(0xFFFEF7E0),
                iconColor = AccentAmber,
                onClick = onNavigateToFinance
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NeutralLightBackground),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontal,
            end = spacing.screenHorizontal,
            top = spacing.screenVertical,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)
    ) {
        // 1. Sade & Havalı Karşılama Başlığı (WCAG AAA Contrast & Compact Height)
        item {
            if (dashboardData.isInitialSkeletonLoading) {
                DashboardHeroHeaderSkeleton()
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = PrimaryBlueContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initial = stableState.currentUser.full_name.firstOrNull()?.uppercase() ?: "K"
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OnPrimaryBlueContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Merhaba, ${stableState.currentUser.full_name.split(" ").firstOrNull() ?: "Kullanıcı"}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = NeutralTextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${stableState.currentUser.institution_name} • ${stableState.currentUser.branch_name}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NeutralTextSecondary,
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showQuickActionModal = true }
                            .testTag("dashboard_quick_action_modal_header_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Hızlı İşlemler",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hızlı İşlem",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = Color.White,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryBlueContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenAddStudent() }
                            .testTag("dashboard_add_student_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = OnPrimaryBlueContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Öğrenci",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = OnPrimaryBlueContainer,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 1.5 Tek Dokunuşla Sık Kullanılan İşlemler Şeridi (Quick Access Strip)
        item {
            if (dashboardData.isInitialSkeletonLoading) {
                QuickShortcutStripSkeleton()
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                QuickShortcutPill(
                    label = "⚡ Hızlı İşlemler",
                    icon = Icons.Default.Bolt,
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color(0xFFFBBF24),
                    borderColor = Color(0xFF334155),
                    testTag = "quick_pill_open_quick_modal",
                    onClick = { showQuickActionModal = true }
                )
                QuickShortcutPill(
                    label = "Yoklama Al",
                    icon = Icons.Default.FactCheck,
                    containerColor = Color(0xFFECFDF5),
                    contentColor = Color(0xFF047857),
                    borderColor = Color(0xFFA7F3D0),
                    testTag = "quick_pill_take_attendance",
                    onClick = onOpenQuickAttendance
                )
                QuickShortcutPill(
                    label = "Ödeme Tahsil Et",
                    icon = Icons.Default.Payments,
                    containerColor = Color(0xFFEFF6FF),
                    contentColor = PrimaryBlue,
                    borderColor = Color(0xFFBFDBFE),
                    testTag = "quick_pill_add_payment",
                    onClick = onOpenQuickPayment
                )
                QuickShortcutPill(
                    label = "Öğrenci Kaydı",
                    icon = Icons.Default.PersonAdd,
                    containerColor = Color(0xFFF0F9FF),
                    contentColor = Color(0xFF0284C7),
                    borderColor = Color(0xFFBAE6FD),
                    testTag = "quick_pill_add_student",
                    onClick = onOpenAddStudent
                )
                QuickShortcutPill(
                    label = "Duyuru Yayınla",
                    icon = Icons.Default.Campaign,
                    containerColor = Color(0xFFFFFBEB),
                    contentColor = Color(0xFFB45309),
                    borderColor = Color(0xFFFDE68A),
                    testTag = "quick_pill_post_announcement",
                    onClick = onOpenCreateAnnouncement
                )
                QuickShortcutPill(
                    label = "Günlük Karne & Ödev",
                    icon = Icons.Default.Assignment,
                    containerColor = Color(0xFFFDF2F8),
                    contentColor = Color(0xFFBE185D),
                    borderColor = Color(0xFFFBCFE8),
                    testTag = "quick_pill_daily_report",
                    onClick = onNavigateToDailyReports
                )
                QuickShortcutPill(
                    label = "İzin Onayları",
                    icon = Icons.Default.VerifiedUser,
                    containerColor = Color(0xFFF3E8FF),
                    contentColor = Color(0xFF6D28D9),
                    borderColor = Color(0xFFDDD6FE),
                    testTag = "quick_pill_approvals",
                    onClick = onNavigateToConsents
                )
                QuickShortcutPill(
                    label = "Teslim Masası (TCKN)",
                    icon = Icons.Default.TransferWithinAStation,
                    containerColor = Color(0xFFCCFBF1),
                    contentColor = Color(0xFF0F766E),
                    borderColor = Color(0xFF99F6E4),
                    testTag = "quick_pill_dismissal",
                    onClick = onNavigateToDismissal
                )
                QuickShortcutPill(
                    label = "Kapı & QR Güvenlik",
                    icon = Icons.Default.QrCodeScanner,
                    containerColor = Color(0xFFFEE2E2),
                    contentColor = Color(0xFFB91C1C),
                    borderColor = Color(0xFFFECACA),
                    testTag = "quick_pill_security",
                    onClick = onNavigateToSecurity
                )
                    QuickShortcutPill(
                        label = "Üyelik & Kota",
                        icon = Icons.Default.WorkspacePremium,
                        containerColor = Color(0xFFE0E7FF),
                        contentColor = Color(0xFF3730A3),
                        borderColor = Color(0xFFC7D2FE),
                        testTag = "quick_pill_subscription",
                        onClick = onNavigateToSubscription
                    )
                }
            }
        }

        // 2. Etkileşimli ve Özet Tabanlı Dashboard Widget Kartları (Skeleton Loading + Anti-Flicker Stabilized)
        item {
            InteractiveDashboardWidgetsSection(
                state = stableState,
                onOpenQuickAttendance = onOpenQuickAttendance,
                onNavigateToAttendance = onNavigateToAttendance,
                onOpenQuickPayment = onOpenQuickPayment,
                onSelectInvoiceForPayment = onSelectInvoiceForPayment,
                onSendPaymentReminder = onSendPaymentReminder,
                onNavigateToFinance = onNavigateToFinance,
                onApproveLeaveNotice = onApproveLeaveNotice,
                onMarkMedicineGiven = onMarkMedicineGiven,
                onApproveConsent = onApproveConsent,
                onNavigateToConsents = onNavigateToConsents,
                onNavigateToTransport = onNavigateToTransport,
                onNavigateToAiAnalytics = onNavigateToAiAnalytics,
                onSelectStudent = onSelectStudent,
                onUpdateAttendanceStatus = onUpdateAttendanceStatus,
                onNavigateToAnnouncements = onNavigateToAnnouncements,
                isSkeletonLoading = dashboardData.isInitialSkeletonLoading,
                isBackgroundRefreshing = dashboardData.isBackgroundRefreshing,
                onRefreshClicked = { dashboardData.triggerRefreshWithSkeleton() }
            )
        }

        // 3. Her Yetkiye Özel Dinamik AI Analiz Kartı
        item {
            RoleContextualAiCard(
                screenName = "DASHBOARD",
                state = stableState,
                isLoading = dashboardData.isInitialSkeletonLoading,
                onOpenAiHubClick = onNavigateToAiAnalytics,
                onQuickActionClick = { actionType ->
                    when (actionType) {
                        "OPEN_AI_FINANCE" -> onNavigateToFinance()
                        "OPEN_BRANCH_HEALTH" -> onNavigateToReports()
                        "OPEN_AI_HOMEWORK" -> onNavigateToHomework()
                        "REMIND_OVERDUE" -> onNavigateToFinance()
                        "OPEN_PARENT_DIGEST" -> onNavigateToDailyReports()
                        "OPEN_CANTEEN_STOCK" -> onNavigateToCanteen()
                        else -> onNavigateToAiAnalytics()
                    }
                }
            )
        }

        // 4. Ana Modüller Başlığı
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showQuickActionModal = true }
                        .testTag("dashboard_quick_actions_section_header")
                ) {
                    Text(
                        text = "Hızlı İşlemler",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Çekmece",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = "Tüm Modüller",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue,
                    modifier = Modifier.clickable { onNavigateToMenu() }
                )
            }
        }

        // 4. Sade 4x2 Modül Izgarası (Ferah, Net, Çarpıcı)
        val chunkedActions = primaryActions.chunked(2)
        chunkedActions.forEach { rowItems ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { tile ->
                        SimplifiedActionTile(
                            tile = tile,
                            isLoading = dashboardData.isInitialSkeletonLoading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 6. Sade Son Duyuru & Bildirim Özeti
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Son Duyurular",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Tümü",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue,
                    modifier = Modifier.clickable { onNavigateToCommunication() }
                )
            }
        }

        val latestAnnouncement = dashboardData.latestAnnouncement
        if (dashboardData.isInitialSkeletonLoading) {
            item {
                AnnouncementCardSkeleton()
            }
        } else if (latestAnnouncement != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DesignTokens.components.cardRadiusLarge))
                        .clickable { onNavigateToAnnouncements() },
                    shape = RoundedCornerShape(DesignTokens.components.cardRadiusLarge),
                    color = NeutralLightSurface,
                    border = BorderStroke(1.dp, NeutralLightBorder.copy(alpha = 0.7f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.cardInnerPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF3E8FF)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = Color(0xFF6D28D9),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(spacing.medium))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = latestAnnouncement.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeutralTextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = latestAnnouncement.content,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = NeutralTextSecondary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = NeutralTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesignTokens.components.cardRadiusMedium),
                    color = NeutralLightSurface,
                    border = BorderStroke(1.dp, NeutralLightBorder.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "Henüz yeni bir duyuru bulunmuyor.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = NeutralTextMuted
                        ),
                        modifier = Modifier.padding(spacing.cardInnerPadding)
                    )
                }
            }
        }
    }

    // Sık Kullanılan İşlemler (Yeni Kayıt, Hızlı Ödeme, Duyuru Ekle) Modalı / Çekmecesi
    if (showQuickActionModal) {
        DashboardQuickActionModal(
            onDismiss = { showQuickActionModal = false },
            onAddStudent = onOpenAddStudent,
            onQuickPayment = onOpenQuickPayment,
            onAddAnnouncement = onOpenCreateAnnouncement,
            onQuickAttendance = onOpenQuickAttendance,
            onDailyReports = onNavigateToDailyReports,
            onConsents = onNavigateToConsents,
            onDismissal = onNavigateToDismissal,
            onSecurity = onNavigateToSecurity
        )
    }
}

/**
 * 🌟 Sadeleştirilmiş, Göz Yormayan ve Ergonomik Modül Kartı
 */
@Composable
fun SimplifiedActionTile(
    tile: DashboardActionTile,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        SimplifiedActionTileSkeleton(
            modifier = modifier,
            testTag = "skeleton_tile_${tile.id}"
        )
        return
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { tile.onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tile.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.title,
                    tint = tile.iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tile.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tile.subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun QuickShortcutPill(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

/**
 * ⚡ Sık Kullanılan İşlemler (Yeni Kayıt, Hızlı Ödeme, Duyuru Ekle vb.) Çekmecesi / Modalı
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardQuickActionModal(
    onDismiss: () -> Unit,
    onAddStudent: () -> Unit,
    onQuickPayment: () -> Unit,
    onAddAnnouncement: () -> Unit,
    onQuickAttendance: () -> Unit,
    onDailyReports: () -> Unit,
    onConsents: () -> Unit,
    onDismissal: () -> Unit,
    onSecurity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategory by remember { mutableStateOf("ALL") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("dashboard_quick_action_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Hızlı İşlemler",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hızlı İşlem Merkezi",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = NeutralTextPrimary
                            )
                        )
                        Text(
                            text = "Sık kullanılan işlemleri anında tetikleyin",
                            fontSize = 12.sp,
                            color = NeutralTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = NeutralTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Tüm İşlemler",
                    "ENROLLMENT" to "Yeni Kayıt",
                    "FINANCE" to "Ödeme & Tahsilat",
                    "ANNOUNCEMENT" to "Duyuru & İletişim",
                    "OPERATIONS" to "Yoklama & Güvenlik"
                ).forEach { (catId, label) ->
                    val isSelected = selectedCategory == catId
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = catId },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Section 1: Öncelikli Sık Kullanılan İşlemler (Yeni Kayıt, Hızlı Ödeme, Duyuru Ekle)
            if (selectedCategory == "ALL" || selectedCategory == "ENROLLMENT" || selectedCategory == "FINANCE" || selectedCategory == "ANNOUNCEMENT") {
                Text(
                    text = "ÖNCELİKLİ HIZLI AKSİYONLAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    letterSpacing = 0.5.sp
                )

                if (selectedCategory == "ALL" || selectedCategory == "ENROLLMENT") {
                    QuickActionCard(
                        title = "Yeni Öğrenci & Veli Kaydı",
                        subtitle = "T.C. Kimlik, sınıf seçimi ve veli hesabı bağlantısı ile anında kayıt oluşturun",
                        tagLabel = "Öğrenci Kaydı",
                        tagColor = Color(0xFF0284C7),
                        icon = Icons.Default.PersonAdd,
                        iconBg = Color(0xFFE0F2FE),
                        iconTint = Color(0xFF0284C7),
                        testTag = "modal_action_add_student",
                        onClick = {
                            onDismiss()
                            onAddStudent()
                        }
                    )
                }

                if (selectedCategory == "ALL" || selectedCategory == "FINANCE") {
                    QuickActionCard(
                        title = "Hızlı Ödeme Tahsilatı",
                        subtitle = "Fatura veya borç seçerek Nakit, Kredi Kartı veya Havale ile anında tahsilat yapın",
                        tagLabel = "Tahsilat & POS",
                        tagColor = Color(0xFF2563EB),
                        icon = Icons.Default.Payments,
                        iconBg = Color(0xFFEFF6FF),
                        iconTint = Color(0xFF2563EB),
                        testTag = "modal_action_quick_payment",
                        onClick = {
                            onDismiss()
                            onQuickPayment()
                        }
                    )
                }

                if (selectedCategory == "ALL" || selectedCategory == "ANNOUNCEMENT") {
                    QuickActionCard(
                        title = "Yeni Duyuru Ekle & Yayınla",
                        subtitle = "Tüm okula, belirli sınıflara veya velilere anlık mobil duyuru ve SMS gönderin",
                        tagLabel = "Duyuru Ekle",
                        tagColor = Color(0xFFD97706),
                        icon = Icons.Default.Campaign,
                        iconBg = Color(0xFFFEF3C7),
                        iconTint = Color(0xFFB45309),
                        testTag = "modal_action_add_announcement",
                        onClick = {
                            onDismiss()
                            onAddAnnouncement()
                        }
                    )
                }
            }

            // Section 2: Operasyonel & Günlük Hızlı İşlemler
            if (selectedCategory == "ALL" || selectedCategory == "OPERATIONS") {
                Text(
                    text = "GÜNLÜK VE OPERASYONEL İŞLEMLER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )

                QuickActionMiniRow(
                    items = listOf(
                        QuickMiniAction(
                            title = "Hızlı Yoklama",
                            subtitle = "Bugünkü katılım",
                            icon = Icons.Default.FactCheck,
                            color = Color(0xFF059669),
                            testTag = "modal_action_take_attendance",
                            onClick = {
                                onDismiss()
                                onQuickAttendance()
                            }
                        ),
                        QuickMiniAction(
                            title = "Günlük Karne",
                            subtitle = "Öğün & ödev",
                            icon = Icons.Default.Assignment,
                            color = Color(0xFFBE185D),
                            testTag = "modal_action_daily_report",
                            onClick = {
                                onDismiss()
                                onDailyReports()
                            }
                        )
                    )
                )

                QuickActionMiniRow(
                    items = listOf(
                        QuickMiniAction(
                            title = "İzin & Onaylar",
                            subtitle = "Veli izinleri",
                            icon = Icons.Default.VerifiedUser,
                            color = Color(0xFF7C3AED),
                            testTag = "modal_action_consents",
                            onClick = {
                                onDismiss()
                                onConsents()
                            }
                        ),
                        QuickMiniAction(
                            title = "Teslim Masası",
                            subtitle = "TCKN çıkış",
                            icon = Icons.Default.TransferWithinAStation,
                            color = Color(0xFF0F766E),
                            testTag = "modal_action_dismissal",
                            onClick = {
                                onDismiss()
                                onDismissal()
                            }
                        )
                    )
                )

                QuickActionMiniRow(
                    items = listOf(
                        QuickMiniAction(
                            title = "Kapı & QR",
                            subtitle = "Turnike kontrolü",
                            icon = Icons.Default.QrCodeScanner,
                            color = Color(0xFFDC2626),
                            testTag = "modal_action_security",
                            onClick = {
                                onDismiss()
                                onSecurity()
                            }
                        ),
                        QuickMiniAction(
                            title = "Duyuru Panosu",
                            subtitle = "Tüm duyurular",
                            icon = Icons.Default.NotificationsActive,
                            color = Color(0xFF6366F1),
                            testTag = "modal_action_announcements_board",
                            onClick = {
                                onDismiss()
                                onAddAnnouncement()
                            }
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    tagLabel: String,
    tagColor: Color,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest,
        border = BorderStroke(1.dp, NeutralLightBorder),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NeutralTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tagColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = tagLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = tagColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = NeutralTextSecondary,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NeutralTextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class QuickMiniAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionMiniRow(
    items: List<QuickMiniAction>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLow,
                border = BorderStroke(1.dp, NeutralLightBorder),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { item.onClick() }
                    .testTag(item.testTag)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(item.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = item.color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NeutralTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.subtitle,
                            fontSize = 10.sp,
                            color = NeutralTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}


