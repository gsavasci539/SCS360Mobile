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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.data.payment.SchoolCheckout
import com.example.data.payment.SchoolCheckoutReference
import com.example.ui.components.AttendanceMonthlyCalendarView
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import java.time.LocalDate
import com.example.ui.components.ReportCardViewer
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.RoleSpeedDialFab
import com.example.ui.components.RoleSwitchBadge
import com.example.ui.components.SpeedDialAction
import com.example.ui.dialogs.VerifiedReceiptDialog
import com.example.ui.dialogs.ceppos.CepPosPaymentDialog
import com.example.ui.dialogs.iyzico.IyzicoPaymentDialog
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import com.example.ui.viewmodel.ParentInvoiceLoadState
import java.text.NumberFormat
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

private data class ParentCheckoutRequest(
    val invoice: Invoice,
    val recoveryOnly: Boolean = false,
    val reference: SchoolCheckoutReference? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPortalScreen(
    state: Okul360UiState,
    onRoleSelected: (UserRole) -> Unit,
    onPayInvoice: (String, Double, String) -> Unit,
    onSubmitMedicine: (String, String, String, String, String) -> Unit,
    onSubmitLeave: (String, String, String, String, String) -> Unit,
    onSubmitPickup: (String, String, String, String) -> Unit,
    onUploadMaterial: (title: String, desc: String, subject: String, className: String, type: String, ext: String, size: String, dueDate: String?, isRequired: Boolean, summary: String) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onDownloadMaterial: (CourseMaterial) -> Unit = {},
    onDeleteMaterial: (String) -> Unit = {},
    onAcknowledgeMaterial: (String) -> Unit = {},
    onOpenThemeConfig: () -> Unit = {},
    onOpenAiWeeklyDigest: ((Student) -> Unit)? = null,
    onOpenChatbot: (() -> Unit)? = null,
    onOpenConsents: () -> Unit = {},
    onOpenClubs: () -> Unit = {},
    onOpenSurveys: () -> Unit = {},
    onOpenTransport: () -> Unit = {},
    onOpenDismissal: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRefreshInvoices: (String) -> Unit = {},
    chatbotTab: ParentNavTab? = null,
    chatbotStudentId: String? = null,
    onChatbotDestinationOpened: () -> Unit = {},
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
    val presentationScope = com.example.ui.components.financePresentationScope(state)
    val canReadAttendance = NativeNavigationPolicy.has(state.currentUser, "parent.view_attendance")
    val canReadFinances = NativeNavigationPolicy.canReadParentFinances(state.currentUser)
    val limitedToModules = !NativeNavigationPolicy.canReadParentChildProfile(state.currentUser) &&
        (canReadAttendance || canReadFinances)
    val visibleTabs = if (limitedToModules) buildList {
        if (canReadAttendance) add(ParentNavTab.DIARY)
        if (canReadFinances) add(ParentNavTab.PAYMENTS)
    } else ParentNavTab.entries
    // The parent portal endpoint has already authorized every relationship.
    val parentChildren = state.parentChildren
    val validShortcut = chatbotStudentId == null || parentChildren.any { it.id == chatbotStudentId }
    var selectedTab by remember(presentationScope, visibleTabs) {
        mutableStateOf(chatbotTab?.takeIf { validShortcut && it in visibleTabs } ?: visibleTabs.first())
    }
    var parentFabExpanded by remember { mutableStateOf(false) }
    var selectedChildId by remember(presentationScope) {
        mutableStateOf(chatbotStudentId?.takeIf { validShortcut } ?: parentChildren.first().id)
    }
    LaunchedEffect(chatbotTab, chatbotStudentId, presentationScope, visibleTabs) {
        if (chatbotTab != null) {
            if (validShortcut && chatbotTab in visibleTabs) {
                androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                    if (chatbotStudentId != null) selectedChildId = chatbotStudentId
                    selectedTab = chatbotTab
                }
            }
            onChatbotDestinationOpened()
        }
    }
    val currentChild = parentChildren.find { it.id == selectedChildId } ?: parentChildren.firstOrNull()

    // Dialog States
    var showMedicineDialog by remember(presentationScope, selectedChildId) { mutableStateOf(false) }
    var showLeaveDialog by remember(presentationScope, selectedChildId) { mutableStateOf(false) }
    var showPickupDialog by remember(presentationScope, selectedChildId) { mutableStateOf(false) }
    var showMessageDialog by remember(presentationScope, selectedChildId) { mutableStateOf(false) }
    var checkoutRequest by remember(presentationScope, selectedChildId) { mutableStateOf<ParentCheckoutRequest?>(null) }
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
                                        text = if (limitedToModules) child.full_name else "${child.first_name} (${child.class_name.take(12)})",
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
                    visibleTabs.forEach { tab ->
                        val label = if (limitedToModules && tab == ParentNavTab.DIARY) "Yoklama" else tab.title
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
                                            contentDescription = label,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = label,
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
                        onPayClicked = { invoice -> checkoutRequest = ParentCheckoutRequest(invoice) },
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
                        onPayClicked = { invoice -> checkoutRequest = ParentCheckoutRequest(invoice) },
                        onReceiptClicked = { invoice -> showReceiptForInvoice = invoice },
                        onRecoverCheckout = { invoice, reference ->
                            checkoutRequest = ParentCheckoutRequest(invoice, recoveryOnly = true, reference = reference)
                        },
                    )
                    ParentNavTab.DIARY -> ParentDiaryAndGrowthTab(
                        student = currentChild,
                        state = state,
                        currentUser = state.currentUser,
                        onOpenAiWeeklyDigest = onOpenAiWeeklyDigest,
                        showChildDetails = !limitedToModules,
                        onLogout = onLogout
                    )
                    ParentNavTab.AI_CHATBOT -> {}
                }
            }

            // Dialogs
            if (showMedicineDialog && currentChild != null) com.example.ui.dialogs.NativeParentRequestDialog(currentChild.id, state.currentUser, "MEDICINE", { showMedicineDialog = false }, onRefresh)
            if (showLeaveDialog && currentChild != null) com.example.ui.dialogs.NativeParentRequestDialog(currentChild.id, state.currentUser, "EXCUSE", { showLeaveDialog = false }, onRefresh)
            if (showPickupDialog && currentChild != null) com.example.ui.dialogs.NativeParentRequestDialog(currentChild.id, state.currentUser, "PICKUP", { showPickupDialog = false }, onRefresh)

            checkoutRequest?.takeIf { NativeNavigationPolicy.has(state.currentUser, "parent.make_payment") }?.let { request ->
                val invoice = request.invoice
                if (invoice.student_id == currentChild?.id && state.parentChildren.any { it.id == invoice.student_id }) {
                    com.example.ui.dialogs.iyzico.HostedPaymentDialog(invoice.id,
                        onDismiss = { checkoutRequest = null },
                        onReconciled = { onRefreshInvoices(invoice.student_id) },
                        parentStudentId = invoice.student_id, recoveryOnly = request.recoveryOnly,
                        recoveryReference = request.reference)
                }
            }

            showReceiptForInvoice?.takeIf { canReadFinances && it.student_id == currentChild?.id }?.let { invoice ->
                VerifiedReceiptDialog(
                    debtId = invoice.id,
                    parentStudentId = invoice.student_id,
                    onDismiss = { showReceiptForInvoice = null }
                )
            }

            TextButton(onClick = onRefresh, enabled = !state.isParentPortalLoading, modifier = Modifier.align(Alignment.TopEnd)) { Text("Yenile") }

            // Role-specific Floating Action Button for Parent
            if (!limitedToModules) RoleSpeedDialFab(
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
        val canReadAttendance = state.currentUser?.let {
            com.example.ui.navigation.NativeNavigationPolicy.has(it, "parent.view_attendance")
        } == true
        key(com.example.ui.components.financePresentationScope(state), student.id) {
            ParentVerifiedRecordsPane(student.id, "today-status", Modifier.weight(1f), canReadAttendance = canReadAttendance)
        }
    }
}

@Composable
internal fun ParentDiaryAndGrowthTab(
    student: Student,
    state: Okul360UiState,
    currentUser: User,
    onOpenAiWeeklyDigest: ((Student) -> Unit)? = null,
    showChildDetails: Boolean = true,
    onLogout: () -> Unit
) {

    val scope = com.example.ui.components.financePresentationScope(state)
    val canReadAttendance = com.example.ui.navigation.NativeNavigationPolicy.has(currentUser, "parent.view_attendance")
    if (!showChildDetails && !canReadAttendance) return
    key(scope, student.id) {
        var selected by remember { mutableStateOf("timeline") }
        var attendanceDate by remember { mutableStateOf<LocalDate?>(null) }
        val timelineState = rememberSaveableStateHolder()
        val sections = buildList {
            if (showChildDetails) add("Günlük kayıtlar" to "timeline")
            if (canReadAttendance) add("Yoklama" to "attendance")
            if (showChildDetails) addAll(listOf("Notlar" to "grades", "Program" to "schedule", "Ödevler" to "homework", "Ders materyalleri" to "materials",
                "Duyurular" to "announcements", "Mesajlar" to "communications", "İlaç talepleri" to "medicine-requests",
                "Mazeret talepleri" to "excuse-requests", "Profil" to "profile"))
        }
        val index = sections.indexOfFirst { it.second == selected }.coerceAtLeast(0)
        val section = sections[index].second
        Column(Modifier.fillMaxSize()) {
            if (showChildDetails) ScrollableTabRow(selectedTabIndex = index) {
                sections.forEach { (label, value) -> Tab(section == value, onClick = { selected = value }, text = { Text(label) }) }
            }
            when (section) {
                "profile" -> ParentStudentProfileTab(student, currentUser, state, onLogout)
                "attendance" -> AttendanceMonthlyCalendarView(student.id, attendanceDate, Modifier.weight(1f))
                "timeline" -> timelineState.SaveableStateProvider("timeline") {
                    val openAttendance: ((LocalDate) -> Unit)? = if (canReadAttendance) { day ->
                        attendanceDate = day
                        selected = "attendance"
                    } else null
                    ParentVerifiedRecordsPane(student.id, section, Modifier.weight(1f),
                        canReadAttendance = canReadAttendance, onOpenAttendance = openAttendance)
                }
                else -> ParentVerifiedRecordsPane(student.id, section, Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun ParentPaymentsTab(
    student: Student,
    state: Okul360UiState,
    onPayClicked: (Invoice) -> Unit,
    onReceiptClicked: (Invoice) -> Unit,
    onRecoverCheckout: (Invoice, SchoolCheckoutReference?) -> Unit = { _, _ -> },
) {
    val invoiceListState = key(student.id) { rememberLazyListState() }
    val snapshot = state.parentInvoiceSnapshots[student.id]
    val loadMessage = if (!NativeNavigationPolicy.canReadParentFinances(state.currentUser)) {
        "Borçları görüntüleme yetkiniz yok."
    } else when (state.parentInvoiceStates[student.id]) {
        ParentInvoiceLoadState.LOADING -> "Borçlar yükleniyor."
        ParentInvoiceLoadState.FAILED -> "Borçlar yüklenemedi. Yeniden deneyin."
        ParentInvoiceLoadState.LOADED -> null
        null -> "Borçlar henüz doğrulanmadı."
    }
    if (loadMessage != null || snapshot == null) {
        Text(loadMessage ?: "Borçlar henüz doğrulanmadı.", modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 48.dp))
        return
    }

    val canCheckPayment = NativeNavigationPolicy.has(state.currentUser, "parent.make_payment")
    val canMakePayment = snapshot.onlinePaymentAvailable && canCheckPayment
    val context = LocalContext.current
    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("tr-TR")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val totalRemaining = snapshot.rows.filter { it.isPayable }.sumOf { it.remaining }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("parent_invoices"),
        state = invoiceListState,
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
            if (snapshot.rows.isNotEmpty()) Text("Ödenebilir kalan borç: ${currency.format(totalRemaining)}")
        }
        if (snapshot.rows.isEmpty()) item { Text("Kayıtlı borç bulunamadı") }

        item {
            Text(
                text = "Taksit & Fatura Listesi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )
        }

        items(snapshot.rows, key = { it.invoice.id }) { row ->
            val invoice = row.invoice
            val status = when (invoice.status) {
                "PAID" -> "Ödendi"
                "UNPAID" -> "Ödenmedi"
                "PARTIALLY_PAID" -> "Kısmi ödendi"
                "OVERDUE" -> "Gecikmiş"
                "CANCELLED" -> "İptal"
                else -> invoice.status.ifBlank { "Belirsiz" }
            }
            OutlinedCard(Modifier.fillMaxWidth().testTag("parent_invoice_${invoice.id}")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(invoice.description.ifBlank { row.type ?: "Borç kaydı" }, style = MaterialTheme.typography.titleMedium)
                    if (invoice.due_date.isNotBlank()) Text("Vade: ${invoice.due_date}")
                    Text(status, fontWeight = FontWeight.SemiBold)
                    Text("Tutar: ${currency.format(row.amount)}")
                    Text("Ödenen: ${currency.format(row.paid)}")
                    Text("Kalan: ${currency.format(row.remaining)}", fontWeight = FontWeight.Bold)
                    if (invoice.status == "PAID" || row.paid.signum() > 0) OutlinedButton(
                            onClick = { onReceiptClicked(invoice) }, modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Makbuz İncele") }
                    val openCheckout = row.openCheckout
                    val alreadySaved = canCheckPayment &&
                        SchoolCheckout.hasSavedAttempt(context, state.currentUser.id, invoice.id, openCheckout)
                    if (canMakePayment && row.isPayable && (!row.paymentInProgress || openCheckout != null && alreadySaved)) {
                        Button(
                            onClick = { onPayClicked(invoice) }, modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(if (openCheckout == null) "Online Öde" else "Ödeme işlemine dön") }
                    } else if (canCheckPayment && (openCheckout != null || !row.paymentInProgress && alreadySaved)) {
                        OutlinedButton(
                            onClick = { onRecoverCheckout(invoice, row.openCheckout) }, modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Ödeme sonucunu kontrol et") }
                    } else if (canCheckPayment && row.paymentInProgress) {
                        Text("Bu borç için açık bir ödeme işlemi var.")
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
