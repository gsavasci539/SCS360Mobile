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
                                    state.announcements.count { it.read_count == 0 }.let { if (it > 0) it else 3 }
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
                    ParentNavTab.MESSAGING -> DirectMessagingScreen(
                        state = state,
                        onSelectConversation = onSelectConversation,
                        onSendMessage = onSendMessage,
                        onStartNewConversation = onStartNewConversation
                    )
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
            if (showMedicineDialog && currentChild != null) {
                ParentMedicineDialog(
                    student = currentChild,
                    onDismiss = { showMedicineDialog = false },
                    onSubmit = { med, dose, time, note ->
                        onSubmitMedicine(currentChild.id, med, dose, time, note)
                        showMedicineDialog = false
                    }
                )
            }

            if (showLeaveDialog && currentChild != null) {
                ParentLeaveDialog(
                    student = currentChild,
                    onDismiss = { showLeaveDialog = false },
                    onSubmit = { start, end, reason, note ->
                        onSubmitLeave(currentChild.id, start, end, reason, note)
                        showLeaveDialog = false
                    }
                )
            }

            if (showPickupDialog && currentChild != null) {
                ParentPickupDialog(
                    student = currentChild,
                    onDismiss = { showPickupDialog = false },
                    onSubmit = { person, rel, phone, note ->
                        onSubmitPickup(currentChild.id, person, rel, phone)
                        showPickupDialog = false
                    }
                )
            }

            if (showMessageDialog && currentChild != null) {
                ParentMessageTeacherDialog(
                    student = currentChild,
                    onDismiss = { showMessageDialog = false },
                    onSend = { subject, msg ->
                        onSendMessageToTeacher(subject, msg)
                        showMessageDialog = false
                    }
                )
            }

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
    val context = LocalContext.current
    val todayDate = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr")).format(Date())

    val studentInvoices = remember(state.invoices, student.id) {
        state.invoices.filter { it.student_id == student.id || it.student_name.contains(student.first_name, ignoreCase = true) }
    }

    val todayMeals = listOf(
        MealMenuItem("🍳 Sabah Kahvaltısı", listOf("Kaşarlı omlet", "Doğal süzme bal", "Organik süt", "Salatalık dilimleri"), "08:30 - 09:15", "380 kcal"),
        MealMenuItem("🍲 Öğle Yemeği", listOf("Süzme mercimek çorbası", "Fırında ızgara köfte", "Şehriyeli pirinç pilavı", "Mevsim meyvesi"), "12:00 - 13:00", "560 kcal"),
        MealMenuItem("🧁 İkindi Beslenmesi", listOf("Ev yapımı havuçlu tarçınlı kek", "Taze sıkılmış elma-portakal suyu"), "15:30 - 16:00", "240 kcal")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bugünün Özeti",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandNavy
                    )
                    Text(
                        text = todayDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = SecondaryEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SecondaryEmerald)
                        )
                        Text(
                            text = "Canlı Takip",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryEmerald
                        )
                    }
                }
            }
        }

        // Veliye Özel Yapay Zeka Günlük Gelişim & Durum Kartı
        item {
            RoleContextualAiCard(
                screenName = "PARENT_PORTAL",
                state = state,
                onQuickActionClick = { actionType ->
                    when (actionType) {
                        "OPEN_PARENT_DIGEST" -> onNavigateToPayments()
                        "PAY_ONLINE_IYZICO" -> onNavigateToPayments()
                        "CREATE_LEAVE_NOTICE" -> onOpenLeave()
                        else -> {}
                    }
                }
            )
        }

        // Upcoming / Overdue Payment Reminder In-App Banner
        if (studentInvoices.any { !it.status.equals("PAID", ignoreCase = true) && !it.status.equals("ODENDI", ignoreCase = true) }) {
            item {
                UpcomingPaymentReminderBanner(
                    upcomingInvoices = studentInvoices,
                    onPayClicked = onPayClicked,
                    onNavigateToPayments = onNavigateToPayments
                )
            }
        }

        // Live Status Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(student.avatar_color_hex).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${student.first_name.take(1)}${student.last_name.take(1)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(student.avatar_color_hex)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = student.full_name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = BrandNavy
                                )
                                Text(
                                    text = "${student.class_name} • No: ${student.student_number}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = SecondaryEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "OKULDA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = SecondaryEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Metric Pill Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatusMiniPill(
                                icon = Icons.Default.Login,
                                label = "Giriş Saati",
                                value = "08:45",
                                color = PrimaryBlue
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatusMiniPill(
                                icon = Icons.Default.Thermostat,
                                label = "Ateş Ölçümü",
                                value = "36.5 °C",
                                color = SecondaryEmerald
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatusMiniPill(
                                icon = Icons.Default.DirectionsBus,
                                label = "Ulaşım",
                                value = "Veli Teslim",
                                color = AccentAmber
                            )
                        }
                    }

                    if (!student.allergies_or_diet.isNullOrBlank() && student.allergies_or_diet != "Yok") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Alerji / Sağlık Notu: ${student.allergies_or_diet}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Parent Action Buttons
        item {
            Text(
                text = "⚡ Veli Hızlı Bildirimleri",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ParentActionCard(
                    title = "İlaç Bildirimi",
                    subtitle = "Gün içi ilaç talimatı",
                    icon = Icons.Default.Medication,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMedicine
                )
                ParentActionCard(
                    title = "İzin / Devamsızlık",
                    subtitle = "Mazeret ilet",
                    icon = Icons.Default.EventBusy,
                    color = Color(0xFFD97706),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLeave
                )
                ParentActionCard(
                    title = "Teslim Alma",
                    subtitle = "Kim teslim alacak?",
                    icon = Icons.Default.DirectionsCar,
                    color = Color(0xFF059669),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenPickup
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AssistChip(
                        onClick = onOpenConsents,
                        label = { Text("İzin Onayları (SHA-256)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF6D28D9)) },
                        modifier = Modifier.testTag("parent_open_consents_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = onOpenClubs,
                        label = { Text("Kulüpler & Sertifika PDF", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD97706)) },
                        modifier = Modifier.testTag("parent_open_clubs_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = onOpenSurveys,
                        label = { Text("Veli Anketleri", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Poll, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2563EB)) },
                        modifier = Modifier.testTag("parent_open_surveys_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = onOpenTransport,
                        label = { Text("Canlı Servis GPS", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.DirectionsBus, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFDC2626)) },
                        modifier = Modifier.testTag("parent_open_transport_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = onOpenDismissal,
                        label = { Text("Teslim Yetkisi (TCKN)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.TransferWithinAStation, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0F766E)) },
                        modifier = Modifier.testTag("parent_open_dismissal_chip")
                    )
                }
            }
        }

        // Teacher Communication Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.06f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Sınıf Öğretmeni", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Ayşe Kaya", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                            Text(student.class_name, fontSize = 12.sp, color = PrimaryBlue)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+905321112233"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Ara", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = onOpenMessage,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "Mesaj İlet", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Daily Menu Section
        item {
            Text(
                text = "🍽️ Günün Yemek Menüsü",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                todayMeals.forEach { meal ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        tonalElevation = 1.dp,
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = meal.meal_type,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = BrandNavy
                                )
                                Text(
                                    text = "${meal.time_range} • ${meal.calories}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = meal.items.joinToString(" • "),
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. DIARY & GROWTH TAB (Günlük Karne, Haftalık Program & Gelişim & Diğer)
// -------------------------------------------------------------
@Composable
private fun ParentDiaryAndGrowthTab(
    student: Student,
    state: Okul360UiState,
    currentUser: User,
    onOpenAiWeeklyDigest: ((Student) -> Unit)? = null,
    onLogout: () -> Unit
) {
    var diarySubTab by remember { mutableStateOf(0) }

    val report = state.dailyReports.firstOrNull { it.student_id == student.id } ?: DailyReport(
        id = "rep-1",
        student_id = student.id,
        student_name = student.full_name,
        class_name = student.class_name,
        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
        breakfast_status = "Tümünü Yedi",
        lunch_status = "Yarısını Yedi",
        snack_status = "Tümünü Yedi",
        sleep_duration_minutes = 90,
        mood_level = "Çok Neşeli & Uyumlu",
        activities_done = listOf("Robotik Kodlama & Algoritma", "Suluboya Resim Atölyesi", "Bahçede Top Oyunları", "İngilizce Şarkı ve Drama"),
        teacher_note = "Bugün robotik atölyesinde arkadaşlarına liderlik etti ve lego parçalarını başarıyla birleştirdi. Öğle yemeğinde sebze çorbasını çok sevdi.",
        teacher_name = "Ayşe Öğretmen"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📖 Karne, Gelişim & Bilgiler",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BrandNavy
            )
            Text(
                text = "${student.full_name} için gelişim, duyuru ve profil dökümü",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Gemini AI Weekly Pedagogical Digest Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7C3AED)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "✨ Akıllı Haftalık Gelişim Özeti",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color(0xFF5B21B6)
                                )
                                Text(
                                    text = "Yapay zeka ile kişiselleştirilmiş ev etkinlikleri & analiz",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6D28D9)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${student.first_name}'in bu haftaki yoklama, ders içi katılım, beslenme ve sosyal etkileşim verileri yapay zeka tarafından analiz edildi. Evde destekleyici aktiviteler ve rehberlik önerileri hazırlandı.",
                        fontSize = 12.sp,
                        color = Color(0xFF4C1D95),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onOpenAiWeeklyDigest?.invoke(student) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Gelişim Bültenini İncele", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Sub Tab Selector
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                val subTabs = listOf(
                    "Günlük Karne",
                    "Aylık Yoklama",
                    "Haftalık Program",
                    "Dönem Karnesi",
                    "Gelişim Karnesi",
                    "Okul Duyuruları",
                    "Profil & Ayarlar"
                )
                items(subTabs.size) { index ->
                    val title = subTabs[index]
                    val isSelected = diarySubTab == index
                    FilterChip(
                        selected = isSelected,
                        onClick = { diarySubTab = index },
                        label = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        when (diarySubTab) {
            0 -> {
                // Günlük Karne
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tarih: ${report.date}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BrandNavy
                                    )
                                }

                                Surface(
                                    color = SecondaryEmerald.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "😊 ${report.mood_level}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Meal Consumption Table
                            Text("🥣 Öğün Tüketim Durumu", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MealStatusPill(label = "Kahvaltı", status = report.breakfast_status, color = SecondaryEmerald)
                                MealStatusPill(label = "Öğle Yemeği", status = report.lunch_status, color = AccentAmber)
                                MealStatusPill(label = "İkindi", status = report.snack_status, color = SecondaryEmerald)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Sleep
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF7C3AED).copy(alpha = 0.08f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Öğle Uykusu / Dinlenme", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandNavy)
                                }
                                Text("${report.sleep_duration_minutes} Dakika", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF7C3AED))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Activities Done
                            Text("🎨 Günün Etkinlikleri & Kazanımları", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                report.activities_done.forEach { act ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SecondaryEmerald, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(act, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Teacher Note
                            Text("👩‍🏫 Öğretmen Notu (${report.teacher_name})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${report.teacher_note}\"",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
            1 -> {
                // Aylık Devamsızlık Takvimi
                item {
                    AttendanceMonthlyCalendarView(
                        students = listOf(student),
                        selectedStudentId = student.id,
                        onSelectStudent = {},
                        existingRecords = state.attendanceRecords
                    )
                }
            }
            2 -> {
                // Haftalık Ders Çizelgesi & Günlük Akış
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "📅 ${student.class_name} Haftalık Ders & Etkinlik Programı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandNavy
                        )

                        com.example.ui.components.WeeklyTimetableComponent(
                            classes = state.classes,
                            timetableSlots = state.timetableSlots,
                            selectedClassId = student.class_id,
                            showClassSelector = false
                        )
                    }
                }
            }
            3 -> {
                // Dönem Karnesi (Notlar, Ağırlıklı Ortalama ve MEB Belge)
                item {
                    ReportCardViewer(
                        student = student,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            4 -> {
                // Gelişim Karnesi
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("📊 Dönemsel Gelişim & Yetenek Karnesi", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                            
                            DevelopmentSkillRow(title = "İnce & Kaba Motor Becerileri", score = 95, note = "Makas kullanımı, çizim ve denge koordinasyonunda çok başarılı.")
                            DevelopmentSkillRow(title = "Sosyal Uyum & Paylaşım", score = 98, note = "Grup oyunlarında liderlik ediyor, kurallara tam uyum sağlıyor.")
                            DevelopmentSkillRow(title = "Dil & Kendini İfade Etme", score = 92, note = "Zengin kelime dağarcığı ile duygu ve düşüncelerini net ifade ediyor.")
                            DevelopmentSkillRow(title = "Müzik & Ritim Kabiliyeti", score = 90, note = "Şarkı ve orff çalgılarında ritmi başarıyla takip ediyor.")
                            DevelopmentSkillRow(title = "Öz Bakım & Düzen", score = 96, note = "Kıyafetlerini ve çantasını bağımsız şekilde toparlayabiliyor.")
                        }
                    }
                }
            }
            5 -> {
                // Okul Duyuruları
                item {
                    ParentAnnouncementsTab(student = student, state = state)
                }
            }
            6 -> {
                // Profil & Ayarlar
                item {
                    ParentStudentProfileTab(
                        student = student,
                        currentUser = currentUser,
                        state = state,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun DevelopmentSkillRow(
    title: String,
    score: Int,
    note: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandNavy)
            Text("%$score", fontWeight = FontWeight.Black, fontSize = 13.sp, color = PrimaryBlue)
        }
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (score > 90) SecondaryEmerald else PrimaryBlue,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
        Text(note, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// -------------------------------------------------------------
// 3. PAYMENTS TAB (Ödemeler & Taksitler - SADECE BU ÖĞRENCİ)
// -------------------------------------------------------------
@Composable
private fun ParentPaymentsTab(
    student: Student,
    state: Okul360UiState,
    onPayClicked: (Invoice) -> Unit,
    onReceiptClicked: (Invoice) -> Unit
) {
    // Strictly isolate invoices to THIS student only
    val studentInvoices = remember(state.invoices, student.id) {
        state.invoices.filter { it.student_id == student.id || it.student_name.contains(student.first_name, ignoreCase = true) }
            .ifEmpty {
                listOf(
                    Invoice("inv-1", "FAT-2026-101", student.id, student.full_name, student.class_name, 4500.0, 4500.0, "2026-08-15", "2026-08-01", "PAID", "Ağustos Ayı Eğitim & Yemek Bedeli", "CREDIT_CARD"),
                    Invoice("inv-2", "FAT-2026-102", student.id, student.full_name, student.class_name, 4500.0, 0.0, "2026-09-15", "2026-09-01", "PENDING", "Eylül Ayı Eğitim & Yemek Bedeli", null),
                    Invoice("inv-3", "FAT-2026-103", student.id, student.full_name, student.class_name, 4500.0, 0.0, "2026-10-15", "2026-10-01", "PENDING", "Ekim Ayı Eğitim & Yemek Bedeli", null)
                )
            }
    }

    val totalPaid = studentInvoices.filter { it.status.equals("PAID", ignoreCase = true) || it.status.equals("ODENDI", ignoreCase = true) || (it.paid_amount >= it.amount && it.amount > 0) }.sumOf { if (it.paid_amount > 0) it.paid_amount else it.amount }
    val totalPending = studentInvoices.filter { !it.status.equals("PAID", ignoreCase = true) && !it.status.equals("ODENDI", ignoreCase = true) && (it.paid_amount < it.amount || it.amount == 0.0) }.sumOf { it.amount - it.paid_amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
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
private fun ParentAnnouncementsTab(
    student: Student,
    state: Okul360UiState
) {
    // Filter announcements: only show parent announcements, not internal staff/finance
    val parentAnnouncements = remember(state.announcements) {
        state.announcements.filter {
            !it.category.contains("Personel", ignoreCase = true) &&
            !it.category.contains("Maaş", ignoreCase = true) &&
            !it.category.contains("Finans", ignoreCase = true)
        }.ifEmpty {
            listOf(
                Announcement(
                    id = "ann-1",
                    title = "2026-2027 Eğitim-Öğretim Yılı Veli Bilgilendirme Toplantısı",
                    content = "Değerli velilerimiz, yeni dönem eğitim programımız, rehberlik planlarımız ve atölye çalışmalarımızın paylaşılacağı genel veli toplantımız Cumartesi günü saat 11:00'da konferans salonunda gerçekleşecektir.",
                    category = "Veli Toplantısı",
                    target_audience = "ALL_PARENTS",
                    created_at = "2026-08-15",
                    author_name = "Kurum Müdürlüğü",
                    author_role = "Okul Müdürü"
                ),
                Announcement(
                    id = "ann-2",
                    title = "Göz ve Diş Sağlığı Tarama Programı",
                    content = "Kurum hekimimiz ve uzman diş hekimleri eşliğinde tüm öğrencilerimiz için yıllık koruyucu sağlık taraması yapılacaktır. Tarama sonuç karneleri veli portalınızdan paylaşılacaktır.",
                    category = "Sağlık & Rehberlik",
                    target_audience = "ALL_PARENTS",
                    created_at = "2026-08-10",
                    author_name = "Rehberlik & Sağlık Birimi",
                    author_role = "Okul Hekimi"
                )
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "📢 Okul & Şube Duyuruları",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BrandNavy
            )
            Text(
                text = "Veli ve öğrenci bilgilendirme bültenleri",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(parentAnnouncements) { ann ->
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
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = ann.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(ann.created_at, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(ann.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(ann.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Gönderen: ${ann.author_name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. PROFILE TAB (Öğrenci & Veli Profili, İzin/İlaç Geçmişi)
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
                ProfileItemRow(label = "Kan Grubu", value = student.blood_type ?: "A Rh+")
                ProfileItemRow(label = "Alerji & Diyet", value = student.allergies_or_diet ?: "Yok")
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
                ProfileItemRow(label = "Telefon", value = currentUser.phone ?: "+90 532 111 2233")
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentMedicineDialog(
    student: Student,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    var medicineName by remember { mutableStateOf("Calpol Şurup") }
    var dose by remember { mutableStateOf("1 Ölçek (5 ml)") }
    var time by remember { mutableStateOf("13:00 (Öğle yemeği sonrası)") }
    var instructions by remember { mutableStateOf("Ateşi 38'i geçerse verilebilir.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💊 İlaç Takip Bildirimi", fontWeight = FontWeight.Bold, color = BrandNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Öğretmene gün içi verilecek ilaç talimatını iletin:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = medicineName,
                    onValueChange = { medicineName = it },
                    label = { Text("İlaç Adı") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text("Doz Miktarı") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Veriliş Saati") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Özel Kullanım Talimatı") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (medicineName.isNotBlank()) {
                        onSubmit(medicineName, dose, time, instructions)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Öğretmene İlet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentLeaveDialog(
    student: Student,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    var startDate by remember { mutableStateOf("2026-08-20") }
    var endDate by remember { mutableStateOf("2026-08-21") }
    var reason by remember { mutableStateOf("Hastalık / Doktor Kontrolü") }
    var note by remember { mutableStateOf("Mevsimsel grip sebebiyle dinlenecek.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📅 İzin & Mazeret Talebi", fontWeight = FontWeight.Bold, color = BrandNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Devamsızlık mazeretini okul yönetimine bildirin:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Başlangıç Tarihi") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Bitiş Tarihi") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Mazeret Türü") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Açıklama / Veli Notu") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(startDate, endDate, reason, note) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Talebi İlet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPickupDialog(
    student: Student,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    var personName by remember { mutableStateOf("Fatma Kaya") }
    var relationship by remember { mutableStateOf("Büyükanne") }
    var phone by remember { mutableStateOf("+90 535 999 8877") }
    var note by remember { mutableStateOf("Bugün saat 16:30'da teslim alacaktır.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🚗 Teslim Alma Yetkilendirmesi", fontWeight = FontWeight.Bold, color = BrandNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Çocuğunuzu teslim alacak kişiyi okul güvenliğine tanımlayın:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Teslim Alacak Kişinin Adı") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Yakınlık Derecesi (Anneanne, Teyze, Servis vb.)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("İletişim Numarası") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Not") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(personName, relationship, phone, note) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Yetkilendir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentMessageTeacherDialog(
    student: Student,
    onDismiss: () -> Unit,
    onSend: (String, String) -> Unit
) {
    var subject by remember { mutableStateOf("Beslenme Hakkında") }
    var message by remember { mutableStateOf("Merhaba Ayşe Öğretmenim, bugün öğleden sonra su tüketimine biraz daha dikkat edebilirsek çok sevinirim.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💬 Sınıf Öğretmenine Mesaj", fontWeight = FontWeight.Bold, color = BrandNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Öğretmene anlık iletmek istediğiniz not:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Konu") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Mesajınız") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(subject, message) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Gönder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentOnlinePaymentDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onConfirmPayment: (String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("CREDIT_CARD") }
    var cardNumber by remember { mutableStateOf("4543 •••• •••• 1289") }
    var expiry by remember { mutableStateOf("12/28") }
    var cvv by remember { mutableStateOf("382") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💳 Güvenli Online Tahsilat", fontWeight = FontWeight.Bold, color = BrandNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${invoice.invoice_number} - ${invoice.description}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandNavy)
                
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ödenecek Tutar:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₺${invoice.amount.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = PrimaryBlue)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMethod == "CREDIT_CARD",
                        onClick = { selectedMethod = "CREDIT_CARD" },
                        label = { Text("Kredi Kartı / 3D") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedMethod == "BANK_TRANSFER",
                        onClick = { selectedMethod = "BANK_TRANSFER" },
                        label = { Text("Havale / FAST") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedMethod == "CREDIT_CARD") {
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Kart Numarası") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = expiry,
                            onValueChange = { expiry = it },
                            label = { Text("SKT") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { cvv = it },
                            label = { Text("CVV") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("IBAN: TR12 0006 1005 1980 0012 3456 78", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                            Text("Alıcı: SCS360 Eğitim Hizmetleri A.Ş.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Açıklama: ${invoice.student_name} - ${invoice.invoice_number}", fontSize = 11.sp, color = PrimaryBlue)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPayment(selectedMethod) },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)
            ) {
                Text("₺${invoice.amount.toInt()} Ödemeyi Tamamla", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
