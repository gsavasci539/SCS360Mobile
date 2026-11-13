package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.AttendanceMonthlyCalendarView
import com.example.ui.components.FinancialSummaryCard
import com.example.ui.components.PaymentStatusPieChart
import com.example.ui.components.ReportCardViewer
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.RoleSpeedDialFab
import com.example.ui.components.RoleSwitchBadge
import com.example.ui.components.SpeedDialAction
import com.example.ui.components.UpcomingPaymentReminderBanner
import com.example.ui.dialogs.ReceiptDialog
import com.example.ui.dialogs.ceppos.CepPosPaymentDialog
import com.example.ui.dialogs.iyzico.IyzicoPaymentDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.text.SimpleDateFormat
import java.util.*

enum class ParentNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY("Bugün", Icons.Default.Face, Icons.Outlined.Face),
    MATERIALS("Materyal", Icons.Default.MenuBook, Icons.Outlined.MenuBook),
    MESSAGING("İletişim", Icons.Default.ChatBubble, Icons.Outlined.ChatBubble),
    PAYMENTS("Ödemeler", Icons.Default.CreditCard, Icons.Outlined.CreditCard),
    AI_CHATBOT("AI Asistan", Icons.Default.AutoAwesome, Icons.Outlined.AutoAwesome),
    DIARY("Gelişim", Icons.Default.AutoStories, Icons.Outlined.AutoStories)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPortalScreen(
    state: Okul360UiState,
    onRoleSelected: (UserRole) -> Unit,
    onPayInvoice: (String, Double, String) -> Unit,
    onSubmitMedicine: (String, String, String, String, String) -> Unit,
    onSubmitLeave: (String, String, String, String, String) -> Unit,
    onSubmitPickup: (String, String, String, String) -> Unit,
    onSendMessageToTeacher: (String, String) -> Unit,
    onUploadMaterial: (title: String, desc: String, subject: String, className: String, type: String, ext: String, size: String, dueDate: String?, isRequired: Boolean, summary: String) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onDownloadMaterial: (CourseMaterial) -> Unit = {},
    onDeleteMaterial: (String) -> Unit = {},
    onAcknowledgeMaterial: (String) -> Unit = {},
    onSelectConversation: (String?) -> Unit = {},
    onSendMessage: (String, String, String?, String?) -> Unit = { _, _, _, _ -> },
    onStartNewConversation: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenThemeConfig: () -> Unit = {},
    onOpenAiWeeklyDigest: ((Student) -> Unit)? = null,
    onOpenChatbot: (() -> Unit)? = null,
    onOpenConsents: () -> Unit = {},
    onOpenClubs: () -> Unit = {},
    onOpenSurveys: () -> Unit = {},
    onOpenTransport: () -> Unit = {},
    onOpenDismissal: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit
) {
    if (state.parentChildren.isEmpty()) {
        LazyColumn(Modifier.fillMaxSize().testTag("parent_empty"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(state.currentUser.resolvedName, style = MaterialTheme.typography.titleLarge) }
            item { Text(when { state.isParentPortalLoading -> "Veli kayıtları yükleniyor"; state.parentPortalError != null -> "Kayıtlar yüklenemedi"; else -> "Kayıtlı Öğrenci Bulunamadı" }, style = MaterialTheme.typography.titleMedium) }
            item { Text(state.parentPortalError ?: if (state.isParentPortalLoading) "Sunucudaki öğrenci ilişkileri kontrol ediliyor." else "Hesabınıza bağlı öğrenci kaydı bulunamadı. Kurum yönetimiyle görüşebilirsiniz.") }
            item { Button(onClick = onRefresh, enabled = !state.isParentPortalLoading, modifier = Modifier.heightIn(min = 48.dp)) { Text("Yenile") } }
            item { TextButton(onClick = onLogout, modifier = Modifier.heightIn(min = 48.dp)) { Text("Çıkış yap") } }
        }
        return
    }
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(ParentNavTab.TODAY) }
    var parentFabExpanded by remember { mutableStateOf(false) }

    val presentationScope = com.example.ui.components.financePresentationScope(state)
    // The parent portal endpoint has already authorized every relationship.
    val parentChildren = state.parentChildren

    var selectedChildId by remember(presentationScope) { mutableStateOf(parentChildren.firstOrNull()?.id ?: "") }
    val currentChild = parentChildren.find { it.id == selectedChildId } ?: parentChildren.firstOrNull()

    // Dialog States
    var showMedicineDialog by remember(presentationScope, selectedChildId) { mutableStateOf(false) }
    var showLeaveDialog by remember(presentationScope, selectedChildId) { mutableStateOf(false) }
    var showPickupDialog by remember(presentationScope, selectedChildId) { mutableStateOf(false) }
    var showMessageDialog by remember(presentationScope, selectedChildId) { mutableStateOf(false) }
    var showPaymentDialogForInvoice by remember(presentationScope, selectedChildId) { mutableStateOf<Invoice?>(null) }
    var showReceiptForInvoice by remember(presentationScope, selectedChildId) { mutableStateOf<Invoice?>(null) }

    val parentActions = remember(presentationScope, selectedChildId) {
        listOf(
            SpeedDialAction(
                id = "parent_pay_tuition",
                title = "Aidat / Ödeme",
                description = "Bekleyen okul aidatı veya yemek ödemesi",
                icon = Icons.Default.CreditCard,
                containerColor = Color(0xFF2563EB), // Blue
                testTag = "parent_fab_action_pay",
                onClick = {
                    parentFabExpanded = false
                    selectedTab = ParentNavTab.PAYMENTS
                }
            ),
            SpeedDialAction(
                id = "parent_medicine_leave",
                title = "İlaç & İzin Bildir",
                description = "Öğrenci ilaç saati veya devamsızlık bildir",
                icon = Icons.Default.MedicalServices,
                containerColor = Color(0xFFE11D48), // Rose
                testTag = "parent_fab_action_medicine",
                onClick = {
                    parentFabExpanded = false
                    showMedicineDialog = true
                }
            ),
            SpeedDialAction(
                id = "parent_message_teacher",
                title = "Öğretmene Mesaj",
                description = "Sınıf öğretmeniyle doğrudan iletişim",
                icon = Icons.Default.ChatBubble,
                containerColor = Color(0xFF7C3AED), // Purple
                testTag = "parent_fab_action_message",
                onClick = {
                    parentFabExpanded = false
                    selectedTab = ParentNavTab.MESSAGING
                }
            )
        )
    }

    val isChatRoomActive = selectedTab == ParentNavTab.MESSAGING && state.selectedConversationId != null

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isChatRoomActive) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF7C3AED), Color(0xFF3B82F6))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FamilyRestroom,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "SCS360 Veli Portalı",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.5.sp,
                                    color = BrandNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Sayın ${state.currentUser.full_name}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            RoleSwitchBadge(
                                selectedRole = state.selectedRole,
                                onRoleSelected = onRoleSelected
                            )

                            // Theme Switcher Button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .clickable { onOpenThemeConfig() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Tema Ayarları",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Logout button on top bar for safety
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                                    .clickable { onLogout() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Çıkış Yap",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Child Switcher Row (Compact)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) {
                        item {
                            Text(
                                text = "Öğrencim:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(parentChildren) { child ->
                            val isSelected = currentChild != null && child.id == currentChild.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedChildId = child.id },
                                label = {
                                    Text(
                                        text = "${child.first_name} (${child.class_name.take(12)})",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Face,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF7C3AED).copy(alpha = 0.15f),
                                    selectedLabelColor = Color(0xFF7C3AED),
                                    selectedLeadingIconColor = Color(0xFF7C3AED)
                                )
                            )
                        }
                    }
                }
            }
        }
    },
    bottomBar = {
            if (!isChatRoomActive) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    ParentNavTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab && tab != ParentNavTab.AI_CHATBOT
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (tab == ParentNavTab.AI_CHATBOT) {
                                    onOpenChatbot?.invoke()
                                } else {
                                    selectedTab = tab
                                }
                            },
                            icon = {
                                val unreadBadge = if (tab == ParentNavTab.MESSAGING) {
                                    state.announcements.count { it.read_count == 0 }
                                } else 0
                                if (unreadBadge > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = AccentRose, contentColor = Color.White) {
                                                Text("$unreadBadge", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    softWrap = false,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF7C3AED),
                                selectedTextColor = Color(0xFF7C3AED),
                                indicatorColor = Color(0xFF7C3AED).copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (currentChild == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Kayıtlı Öğrenci Bulunamadı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.parentPortalError ?: "Sistemde veli hesabınıza bağlı öğrenci kaydı bulunamadı. Şube yönetimi ile irtibata geçebilirsiniz.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    ParentNavTab.TODAY -> ParentTodayTab(
                        student = currentChild,
                        state = state,
                        onOpenMedicine = { showMedicineDialog = true },
                        onOpenLeave = { showLeaveDialog = true },
                        onOpenPickup = { showPickupDialog = true },
                        onOpenMessage = { selectedTab = ParentNavTab.MESSAGING },
                        onPayClicked = { invoice -> showPaymentDialogForInvoice = invoice },
                        onNavigateToPayments = { selectedTab = ParentNavTab.PAYMENTS },
                        onOpenConsents = onOpenConsents,
                        onOpenClubs = onOpenClubs,
                        onOpenSurveys = onOpenSurveys,
                        onOpenTransport = onOpenTransport,
                        onOpenDismissal = onOpenDismissal
                    )
                    ParentNavTab.MATERIALS -> CourseMaterialsScreen(
                        state = state,
                        onUploadMaterial = onUploadMaterial,
                        onDownloadMaterial = onDownloadMaterial,
                        onDeleteMaterial = onDeleteMaterial,
                        onAcknowledgeMaterial = onAcknowledgeMaterial
                    )
                    ParentNavTab.MESSAGING -> key(presentationScope, currentChild.id) { ParentVerifiedMessagingPane(currentChild.id, state.currentUser) }
                    ParentNavTab.PAYMENTS -> ParentPaymentsTab(
                        student = currentChild,
                        state = state,
                        onPayClicked = { invoice -> showPaymentDialogForInvoice = invoice },
                        onReceiptClicked = { invoice -> showReceiptForInvoice = invoice }
                    )
                    ParentNavTab.DIARY -> ParentDiaryAndGrowthTab(
                        student = currentChild,
                        state = state,
                        currentUser = state.currentUser,
                        onOpenAiWeeklyDigest = onOpenAiWeeklyDigest,
                        onLogout = onLogout
                    )
                    ParentNavTab.AI_CHATBOT -> {}
                }
            }

            // Dialogs
            if (showMedicineDialog && currentChild != null) com.example.ui.dialogs.NativeParentRequestDialog(currentChild.id, state.currentUser, "MEDICINE", { showMedicineDialog = false }, onRefresh)
            if (showLeaveDialog && currentChild != null) com.example.ui.dialogs.NativeParentRequestDialog(currentChild.id, state.currentUser, "EXCUSE", { showLeaveDialog = false }, onRefresh)
            if (showPickupDialog && currentChild != null) com.example.ui.dialogs.NativeParentRequestDialog(currentChild.id, state.currentUser, "PICKUP", { showPickupDialog = false }, onRefresh)

            showPaymentDialogForInvoice?.let { invoice ->
                if (invoice.student_id == currentChild?.id && state.parentChildren.any { it.id == invoice.student_id }) {
                    com.example.ui.dialogs.iyzico.HostedPaymentDialog(invoice.id,
                        onDismiss = { showPaymentDialogForInvoice = null }, onReconciled = onRefresh)
                }
            }

            showReceiptForInvoice?.let { invoice ->
                ReceiptDialog(
                    invoice = invoice,
                    onDismiss = { showReceiptForInvoice = null }
                )
            }

            TextButton(onClick = onRefresh, enabled = !state.isParentPortalLoading, modifier = Modifier.align(Alignment.TopEnd)) { Text("Yenile") }

            // Role-specific Floating Action Button for Parent
            RoleSpeedDialFab(
                actions = parentActions,
                expanded = parentFabExpanded,
                onExpandedChange = { parentFabExpanded = it },
                fabColor = Color(0xFF7C3AED),
                fabLabel = "Veli Hızlı Aksiyon",
                testTag = "parent_speed_dial_fab",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// 1. TODAY TAB (Çocuğum & Günlük Canlı Durum)
// -------------------------------------------------------------
@Composable
private fun ParentTodayTab(
    student: Student,
    state: Okul360UiState,
    onOpenMedicine: () -> Unit,
    onOpenLeave: () -> Unit,
    onOpenPickup: () -> Unit,
    onOpenMessage: () -> Unit,
    onPayClicked: (Invoice) -> Unit,
    onNavigateToPayments: () -> Unit,
    onOpenConsents: () -> Unit = {},
    onOpenClubs: () -> Unit = {},
    onOpenSurveys: () -> Unit = {},
    onOpenTransport: () -> Unit = {},
    onOpenDismissal: () -> Unit = {}
) {

    Column(Modifier.fillMaxSize()) {
        Text(student.full_name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Button(onClick = onNavigateToPayments, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ödemeler") } }
            item { OutlinedButton(onClick = onOpenMedicine, modifier = Modifier.heightIn(min = 48.dp)) { Text("İlaç bildir") } }
            item { OutlinedButton(onClick = onOpenLeave, modifier = Modifier.heightIn(min = 48.dp)) { Text("Mazeret bildir") } }
            item { OutlinedButton(onClick = onOpenPickup, modifier = Modifier.heightIn(min = 48.dp)) { Text("Teslim yetkisi") } }
            item { OutlinedButton(onClick = onOpenMessage, modifier = Modifier.heightIn(min = 48.dp)) { Text("Mesajlar") } }
        }
        key(com.example.ui.components.financePresentationScope(state), student.id) { ParentVerifiedRecordsPane(student.id, "today-status", Modifier.weight(1f)) }
    }
}

@Composable
private fun ParentDiaryAndGrowthTab(
    student: Student,
    state: Okul360UiState,
    currentUser: User,
    onOpenAiWeeklyDigest: ((Student) -> Unit)? = null,
    onLogout: () -> Unit
) {

    var selected by remember(student.id) { mutableIntStateOf(0) }
    val sections = listOf("Günlük kayıtlar" to "timeline", "Notlar" to "grades", "Program" to "schedule", "Ödevler" to "homework", "Ders materyalleri" to "materials", "Duyurular" to "announcements", "Mesajlar" to "communications", "İlaç talepleri" to "medicine-requests", "Mazeret talepleri" to "excuse-requests", "Profil" to "profile")
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selected) {
            sections.forEachIndexed { index, item -> Tab(selected == index, onClick = { selected = index }, text = { Text(item.first) }) }
        }
        if (sections[selected].second == "profile") {
            ParentStudentProfileTab(student, currentUser, state, onLogout)
        } else key(com.example.ui.components.financePresentationScope(state), student.id) { ParentVerifiedRecordsPane(student.id, sections[selected].second, Modifier.weight(1f)) }
    }
}

@Composable
internal fun ParentPaymentsTab(
    student: Student,
    state: Okul360UiState,
    onPayClicked: (Invoice) -> Unit,
    onReceiptClicked: (Invoice) -> Unit
) {
    // Strictly isolate invoices to THIS student only
    val studentInvoices = remember(state.invoices, student.id) {
        state.invoices.filter { it.student_id == student.id }

    }

    val totalPaid = studentInvoices.sumOf { it.paid_amount }
    val totalPending = studentInvoices.sumOf { it.amount - it.paid_amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (studentInvoices.isEmpty()) item { Text(state.parentPortalError ?: if (state.isParentPortalLoading) "Borçlar yükleniyor" else "Kayıtlı borç bulunamadı") }
        item {
            Text(
                text = "💳 Ödemeler & Aidat Takibi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BrandNavy
            )
            Text(
                text = "${student.full_name} için fatura ve taksit dökümü",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Upcoming / Overdue Payment In-App Alert Banner
        if (studentInvoices.any { !it.status.equals("PAID", ignoreCase = true) && !it.status.equals("ODENDI", ignoreCase = true) }) {
            item {
                UpcomingPaymentReminderBanner(
                    upcomingInvoices = studentInvoices,
                    onPayClicked = onPayClicked
                )
            }
        }

        // Financial Summary Card (Borç & Ödeme Görselleştirmesi)
        item {
            FinancialSummaryCard(
                student = student,
                invoices = studentInvoices,
                financeSummary = state.financeSummary,
                isParentView = true,
                onPayClick = onPayClicked,
                onViewReceipts = {
                    studentInvoices.firstOrNull { it.status.equals("PAID", ignoreCase = true) || it.status.equals("ODENDI", ignoreCase = true) }?.let { onReceiptClicked(it) }
                }
            )
        }

        // Interactive Student Payment Pie Chart
        item {
            PaymentStatusPieChart(
                invoices = studentInvoices,
                totalInvoiced = totalPaid + totalPending,
                totalCollected = totalPaid,
                pendingBalance = totalPending,
                overdueBalance = studentInvoices.filter { it.status.equals("OVERDUE", ignoreCase = true) }.sumOf { it.amount - it.paid_amount }
            )
        }

        item {
            Text(
                text = "Taksit & Fatura Listesi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )
        }

        items(studentInvoices) { inv ->
            val isPaid = inv.status.equals("PAID", ignoreCase = true) || inv.status.equals("ODENDI", ignoreCase = true) || (inv.paid_amount >= inv.amount && inv.amount > 0)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(inv.invoice_number, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                            Text("Vade: ${inv.due_date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            color = if (isPaid) SecondaryEmerald.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isPaid) "ÖDENDİ" else "BEKLİYOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaid) SecondaryEmerald else AccentAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(inv.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₺${inv.amount.toInt()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = BrandNavy
                        )

                        if (isPaid) {
                            OutlinedButton(
                                onClick = { onReceiptClicked(inv) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Makbuz İncele", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { onPayClicked(inv) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Online Öde", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. ANNOUNCEMENTS TAB (Duyurular & Bildirimler - SADECE VELİ DUYURULARI)
// -------------------------------------------------------------
@Composable
private fun ParentStudentProfileTab(
    student: Student,
    currentUser: User,
    state: Okul360UiState,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var showFullProfile by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "👤 Öğrenci & Aile Profili",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = BrandNavy
        )

        // Student Info Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("👶 Öğrenci Resmi Kayıt Bilgileri", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                ProfileItemRow(label = "Adı Soyadı", value = student.full_name)
                ProfileItemRow(label = "Sınıf / Şube", value = student.class_name)
                ProfileItemRow(label = "Öğrenci Numarası", value = student.student_number)
                ProfileItemRow(label = "Doğum Tarihi", value = student.birth_date)
                ProfileItemRow(label = "Cinsiyet", value = student.gender)
                ProfileItemRow(label = "Kan Grubu", value = student.blood_type ?: "Bilgi yok")
                ProfileItemRow(label = "Alerji & Diyet", value = student.allergies_or_diet ?: "Bilgi yok")
                ProfileItemRow(label = "Kayıt Durumu", value = "Aktif Öğrenci")

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { showFullProfile = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🎓 Detaylı Öğrenci & Not Profilini Görüntüle", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Parent Info Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("👨‍👩‍👧 Veli İletişim Bilgileri", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                ProfileItemRow(label = "Veli Adı", value = currentUser.full_name)
                ProfileItemRow(label = "Kayıtlı E-Posta", value = currentUser.email)
                ProfileItemRow(label = "Telefon", value = currentUser.phone ?: "Bilgi yok")
                ProfileItemRow(label = "Kurum / Kampüs", value = currentUser.institution_name)
                ProfileItemRow(label = "Acil Durum İrtibat", value = "Baba (+90 533 222 3344)")
            }
        }

        // Notices Sent History
        if (state.medicineNotices.isNotEmpty() || state.leaveNotices.isNotEmpty() || state.pickupNotices.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📋 Gönderilen Bildirimlerim", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                    
                    state.medicineNotices.forEach { med ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("💊 ${med.medicine_name} (${med.time_to_give})", fontSize = 12.sp, color = BrandNavy)
                            Text("İletildi", fontSize = 11.sp, color = SecondaryEmerald, fontWeight = FontWeight.Bold)
                        }
                    }

                    state.leaveNotices.forEach { leave ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📅 İzin: ${leave.start_date} - ${leave.end_date}", fontSize = 12.sp, color = BrandNavy)
                            Text("Onaylandı", fontSize = 11.sp, color = SecondaryEmerald, fontWeight = FontWeight.Bold)
                        }
                    }

                    state.pickupNotices.forEach { pickup ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🚗 Teslim: ${pickup.authorized_person_name}", fontSize = 12.sp, color = BrandNavy)
                            Text("Kayıtlı", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Logout only
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Oturumu Kapat (Güvenli Çıkış)", fontWeight = FontWeight.Bold)
        }
    }

    if (showFullProfile) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullProfile = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            StudentProfileScreen(
                student = student,
                onBack = { showFullProfile = false },
                onOpenReportCardViewer = {},
                onPrintReportCard = {
                    com.example.util.ReportCardPrintHelper.printStudentReportCard(context, student)
                }
            )
        }
    }
}

// -------------------------------------------------------------
// HELPER COMPOSABLES & DIALOGS
// -------------------------------------------------------------
@Composable
private fun StatusMiniPill(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = BrandNavy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MealStatusPill(
    label: String,
    status: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(status, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun ParentActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 4.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = BrandNavy,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandNavy)
    }
}

// -------------------------------------------------------------
// MODALS / DIALOGS
// -------------------------------------------------------------
