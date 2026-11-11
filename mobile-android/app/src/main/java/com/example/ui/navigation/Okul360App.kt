package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Invoice
import com.example.data.model.UserRole
import com.example.ui.components.GlobalSearchDialog
import com.example.ui.context.ChatProvider
import com.example.ui.components.HomeSpeedDialFab
import com.example.ui.components.Okul360ChatbotModal
import com.example.ui.components.QuickAttendanceBottomSheet
import com.example.ui.components.ReportCardViewer
import com.example.ui.components.TopBrandHeader
import com.example.ui.dialogs.*
import com.example.ui.screens.*
import com.example.ui.theme.AccentRose
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueContainer
import com.example.ui.viewmodel.Okul360ViewModel

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Ana Sayfa", Icons.Default.Home, Icons.Outlined.Home),
    STUDENTS("Öğrenciler", Icons.Default.Groups, Icons.Outlined.Groups),
    ATTENDANCE("Yoklama", Icons.Default.FactCheck, Icons.Outlined.FactCheck),
    NOTIFICATIONS("Bildirimler", Icons.Default.Notifications, Icons.Outlined.Notifications),
    AI_CHATBOT("AI Asistan", Icons.Default.AutoAwesome, Icons.Outlined.AutoAwesome),
    FINANCE("Finans", Icons.Default.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    OTHER("Menü", Icons.Default.GridView, Icons.Outlined.GridView)
}

enum class SubScreen(val title: String) {
    NONE(""),
    CLASSES("Sınıflar & Şubeler"),
    COMMUNICATION("İletişim & Duyuru Yönetimi"),
    NOTIFICATIONS("Bildirim Merkezi"),
    DIGITAL_REPORT_CARD("Dijital Karne & Not Çizelgesi"),
    COURSE_MATERIALS("Ders Materyalleri & Çalışma Föyleri"),
    DIRECT_MESSAGING("Birebir Veli-Öğretmen İletişimi"),
    REPORTS("Raporlar & Analitik"),
    RESOURCES("Kaynaklar & Envanter"),
    ROLES("Roller & Yetki Matrisi"),
    USERS("Kullanıcılar & Personel"),
    DAILY_REPORTS("Günlük Karne & Gelişim"),
    DIGITAL_ID_CARD("Dijital Kimlik Kartı"),
    SCHEDULE("Haftalık Ders Programı"),
    APPOINTMENTS("Randevu & Veli Görüşmesi"),
    TRANSPORT("Servis & Ulaşım Takibi"),
    CANTEEN("Kantin & Öğrenci Cüzdanı"),
    HOMEWORK("Ödev Değerlendirme & Teslim"),
    LIBRARY("Kütüphane & Kitap Takibi"),
    STAFF("Personel İK & Nöbet Çizelgesi"),
    AI_ANALYTICS("Yapay Zeka (AI) Analiz & Rapor Merkezi"),
    ORGANIZATIONS("Kurumlar & Şubeler"),
    RFID_DEVICES("RFID Kartlar & Donanım Cihazları"),
    CANTEEN_PORTAL("Kantin Görevlisi Portalı"),
    SETTINGS("Sistem & Backend Ayarları"),
    CONSENTS("İzin Belgeleri & Muvafakatnameler"),
    GUIDANCE("Rehberlik & Psikolojik Danışmanlık"),
    INFIRMARY("Revir, Sağlık & İlaç Takibi"),
    CLUBS("Öğrenci Kulüpleri & Etkinlikler"),
    SURVEYS("Anketler & Memnuniyet"),
    TUTORING("Birebir Etüt & Takviye"),
    PAYROLL("Personel Maaş & Bordro"),
    SECURITY("Sistem Güvenlik & Denetim Logları"),
    EXAMS("Sınavlar & Sınav Takvimi"),
    OUTBOX("Giden Kutusu & Toplu Gönderim"),
    AI_CHAT_AUDIT("AI Chatbot Analitik & LLM Eğitim Merkezi"),
    PLATFORM_SAAS("Platform & SaaS Üyelik Yönetimi"),
    SUBSCRIPTION_BILLING("Üyelik, Kota & Ödeme"),
    DISMISSAL("Öğrenci Teslim Masası")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Okul360App(viewModel: Okul360ViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSplashScreen by remember { mutableStateOf(true) }
    var showGlobalSearchDialog by remember { mutableStateOf(false) }
    var showChatbotDialog by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    var homeFabExpanded by remember { mutableStateOf(false) }
    var showQuickPaymentSheet by remember { mutableStateOf(false) }
    var showQuickAttendanceSheet by remember { mutableStateOf(false) }
    var selectedInvoiceForReceipt by remember { mutableStateOf<Invoice?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // System Back Gesture & Navigation Backstack Handler
    val canGoBack = showGlobalSearchDialog ||
            showChatbotDialog ||
            selectedInvoiceForReceipt != null ||
            showQuickPaymentSheet ||
            showQuickAttendanceSheet ||
            homeFabExpanded ||
            state.viewingReportCardForStudent != null ||
            state.selectedStudentForFullProfile != null ||
            state.selectedStudent != null ||
            state.selectedClassDetail != null ||
            state.selectedAnnouncementForDetail != null ||
            state.selectedConversationId != null ||
            currentSubScreen != SubScreen.NONE ||
            (currentTab != MainTab.HOME && (state.selectedRole == UserRole.ADMIN || state.selectedRole == UserRole.SUPERADMIN))

    BackHandler(enabled = canGoBack) {
        when {
            showGlobalSearchDialog -> showGlobalSearchDialog = false
            showChatbotDialog -> showChatbotDialog = false
            selectedInvoiceForReceipt != null -> selectedInvoiceForReceipt = null
            showQuickPaymentSheet -> showQuickPaymentSheet = false
            showQuickAttendanceSheet -> showQuickAttendanceSheet = false
            homeFabExpanded -> homeFabExpanded = false
            state.viewingReportCardForStudent != null -> viewModel.openReportCardViewer(null)
            state.selectedStudentForFullProfile != null -> viewModel.openStudentProfile(null)
            state.selectedStudent != null -> viewModel.selectStudent(null)
            state.selectedClassDetail != null -> viewModel.selectClassDetail(null)
            state.selectedAnnouncementForDetail != null -> viewModel.selectAnnouncementDetail(null)
            state.selectedConversationId != null -> viewModel.selectConversation(null)
            currentSubScreen != SubScreen.NONE -> currentSubScreen = SubScreen.NONE
            currentTab != MainTab.HOME -> currentTab = MainTab.HOME
        }
    }

    fun handleChatbotNavigation(actionTag: String) {
        if (actionTag.startsWith("CONNECT_TEACHER_")) {
            val teacherName = actionTag.removePrefix("CONNECT_TEACHER_")
            viewModel.startNewConversation(
                participantName = teacherName,
                participantRole = "Branş Öğretmeni",
                initialMessage = "Merhaba $teacherName öğretmenim, Chatbot Asistanı üzerinden ulaşıyorum. Öğrencim hakkında bilgi alabilir miyim?"
            )
            currentSubScreen = SubScreen.DIRECT_MESSAGING
        } else {
            when (actionTag) {
                "NAV_AI_ANALYTICS", "ACTION_CASHFLOW", "ACTION_BRANCH_HEALTH" -> {
                    currentSubScreen = SubScreen.AI_ANALYTICS
                }
                "NAV_AI_CHAT_AUDIT" -> {
                    currentSubScreen = SubScreen.AI_CHAT_AUDIT
                }
                "NAV_ATTENDANCE", "ACTION_ATTENDANCE" -> {
                    currentTab = MainTab.ATTENDANCE
                    currentSubScreen = SubScreen.NONE
                }
                "NAV_COMMUNICATION", "ACTION_ANNOUNCEMENT" -> {
                    currentSubScreen = SubScreen.COMMUNICATION
                }
                "NAV_CLASSES", "ACTION_CLASSES" -> {
                    currentSubScreen = SubScreen.CLASSES
                }
                "NAV_STUDENTS", "ACTION_STUDENTS" -> {
                    currentTab = MainTab.STUDENTS
                    currentSubScreen = SubScreen.NONE
                }
                "NAV_HOMEWORK", "ACTION_HOMEWORK" -> {
                    currentSubScreen = SubScreen.HOMEWORK
                }
                "NAV_DAILY_REPORT", "ACTION_DAILY_REPORT" -> {
                    currentSubScreen = SubScreen.DAILY_REPORTS
                }
                "NAV_FINANCE", "ACTION_OVERDUE", "NAV_INVOICES" -> {
                    currentTab = MainTab.FINANCE
                    currentSubScreen = SubScreen.NONE
                }
                "NAV_REPORT_CARD", "ACTION_REPORT_CARD" -> {
                    currentSubScreen = SubScreen.DIGITAL_REPORT_CARD
                }
                "NAV_ORGANIZATIONS" -> {
                    currentSubScreen = SubScreen.ORGANIZATIONS
                }
                "NAV_RFID" -> {
                    currentSubScreen = SubScreen.RFID_DEVICES
                }
                "NAV_STAFF" -> {
                    currentSubScreen = SubScreen.STAFF
                }
                "NAV_ADD_STUDENT" -> {
                    viewModel.openAddStudentDialog(true)
                }
                "NAV_MATERIALS" -> {
                    currentSubScreen = SubScreen.COURSE_MATERIALS
                }
                "ACTION_SEND_REMINDER" -> {
                    viewModel.testPaymentReminderPush()
                }
                "NAV_WALLET_TOPUP", "ACTION_WALLET" -> {
                    currentSubScreen = SubScreen.CANTEEN
                }
                "NAV_MESSAGING" -> {
                    currentSubScreen = SubScreen.DIRECT_MESSAGING
                }
                "NAV_CANTEEN_PRODUCTS", "ACTION_POS", "NAV_POS" -> {
                    currentSubScreen = SubScreen.CANTEEN_PORTAL
                }
                "NAV_ALLERGENS", "ACTION_ALLERGEN" -> {
                    currentSubScreen = SubScreen.CANTEEN
                }
                "NAV_TRANSPORT" -> {
                    currentSubScreen = SubScreen.TRANSPORT
                }
                "NAV_SCHEDULE" -> {
                    currentSubScreen = SubScreen.SCHEDULE
                }
                "NAV_APPOINTMENTS" -> {
                    currentSubScreen = SubScreen.APPOINTMENTS
                }
                "NAV_EXAMS" -> {
                    currentSubScreen = SubScreen.EXAMS
                }
                "NAV_INFIRMARY", "NAV_MEDICINE_NOTICE" -> {
                    currentSubScreen = SubScreen.INFIRMARY
                }
                "NAV_CONSENTS", "NAV_LEAVE_NOTICE", "NAV_PICKUP_NOTICE" -> {
                    currentSubScreen = SubScreen.CONSENTS
                }
                "NAV_PAYMENTS" -> {
                    currentTab = MainTab.FINANCE
                    currentSubScreen = SubScreen.NONE
                }
                "NAV_LIBRARY" -> {
                    currentSubScreen = SubScreen.LIBRARY
                }
                "NAV_CLUBS" -> {
                    currentSubScreen = SubScreen.CLUBS
                }
                "NAV_TUTORING" -> {
                    currentSubScreen = SubScreen.TUTORING
                }
                "NAV_DIGITAL_ID" -> {
                    currentSubScreen = SubScreen.DIGITAL_ID_CARD
                }
                "NAV_SURVEYS" -> {
                    currentSubScreen = SubScreen.SURVEYS
                }
                "NAV_GUIDANCE" -> {
                    currentSubScreen = SubScreen.GUIDANCE
                }
                "NAV_SECURITY" -> {
                    currentSubScreen = SubScreen.SECURITY
                }
                "NAV_PAYROLL" -> {
                    currentSubScreen = SubScreen.PAYROLL
                }
                "NAV_RESOURCES" -> {
                    currentSubScreen = SubScreen.RESOURCES
                }
            }
        }
    }

    // Display splash screen on initial launch
    if (showSplashScreen) {
        SplashScreen(
            onSplashFinished = { showSplashScreen = false }
        )
        return
    }

    // Display snackbar toast on message changes
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearToast()
        }
    }

    // Check Login state
    if (!state.isLoggedIn) {
        LoginScreen(
            loginError = state.loginError,
            isLoading = state.isLoginLoading,
            activePlans = state.subscriptionPlans,
            onLoginSuccess = { email, pass -> viewModel.login(email, pass) },
            onForgotPassword = { email -> viewModel.forgotPassword(email) },
            onRegisterNewTenant = { orgName, taxNo, ownerName, ownerEmail, ownerPhone, password, planId, billingPeriod ->
                viewModel.registerPublicTenantSignup(orgName, taxNo, ownerName, ownerEmail, ownerPhone, password, planId, billingPeriod)
                currentSubScreen = SubScreen.SUBSCRIPTION_BILLING
            }
        )
        return
    }

    // Role-specific screens when no SubScreen is open (Wrapped with SaaS Banner & Shared Modals)
    if (state.selectedRole != UserRole.ADMIN && state.selectedRole != UserRole.SUPERADMIN && currentSubScreen == SubScreen.NONE) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SaaS GRACE / RESTRICTED / SUSPENDED Access Banner across all role portals
            AnimatedVisibility(
                visible = state.organizationAccess.status != "ACTIVE",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val isGrace = state.organizationAccess.status == "GRACE"
                val bannerBg = if (isGrace) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                val bannerFg = if (isGrace) Color(0xFF92400E) else Color(0xFF991B1B)
                Surface(
                    color = bannerBg,
                    modifier = Modifier.fillMaxWidth().statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isGrace) Icons.Default.WarningAmber else Icons.Default.Lock,
                                contentDescription = state.organizationAccess.status,
                                tint = bannerFg,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGrace) {
                                    "Ek Süre (GRACE): Son Tarih ${state.organizationAccess.grace_until ?: "10 Gün"}"
                                } else {
                                    "Erişim Kısıtlı (${state.organizationAccess.status} • HTTP 402)"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = bannerFg
                            )
                        }
                        TextButton(
                            onClick = { currentSubScreen = SubScreen.SUBSCRIPTION_BILLING },
                            colors = ButtonDefaults.textButtonColors(contentColor = bannerFg)
                        ) {
                            Text("Üyelik & Ödeme", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                com.example.ui.theme.AppContentWrapper {
                    when (state.selectedRole) {
                        UserRole.PARENT -> {
                            ParentPortalScreen(
                                state = state,
                                onRoleSelected = { role -> viewModel.switchRole(role) },
                                onPayInvoice = { invId, amt, method -> viewModel.recordPayment(invId, amt, method) },
                                onSubmitMedicine = { stdId, med, dose, time, note -> viewModel.submitMedicineNotice(stdId, med, dose, time, note) },
                                onSubmitLeave = { stdId, start, end, reason, note -> viewModel.submitLeaveNotice(stdId, start, end, reason, note) },
                                onSubmitPickup = { stdId, person, rel, phone -> viewModel.submitPickupNotice(stdId, person, rel, phone) },
                                onSendMessageToTeacher = { subject, msg -> viewModel.sendMessageToTeacher(subject, msg) },
                                onUploadMaterial = { title, desc, subj, cls, type, ext, size, due, req, sum ->
                                    viewModel.createCourseMaterial(title, desc, subj, cls, type, ext, size, due, req, sum)
                                },
                                onDownloadMaterial = { mat -> viewModel.downloadCourseMaterial(mat) },
                                onDeleteMaterial = { id -> viewModel.deleteCourseMaterial(id) },
                                onAcknowledgeMaterial = { matId -> viewModel.acknowledgeCourseMaterial(matId) },
                                onSelectConversation = { id -> viewModel.selectConversation(id) },
                                onSendMessage = { convId, text, file, type -> viewModel.sendDirectMessage(convId, text, file, type) },
                                onStartNewConversation = { participantId, name, role -> viewModel.startNewConversation(participantId, name, role) },
                                onOpenThemeConfig = { viewModel.openThemeConfig(true) },
                                onOpenAiWeeklyDigest = { std -> viewModel.generateAiParentWeeklyDigest(std) },
                                onOpenChatbot = { showChatbotDialog = true },
                                onOpenConsents = { currentSubScreen = SubScreen.CONSENTS },
                                onOpenClubs = { currentSubScreen = SubScreen.CLUBS },
                                onOpenSurveys = { currentSubScreen = SubScreen.SURVEYS },
                                onOpenTransport = { currentSubScreen = SubScreen.TRANSPORT },
                                onOpenDismissal = { currentSubScreen = SubScreen.DISMISSAL },
                                onLogout = { viewModel.logout() }
                            )
                        }
                        UserRole.TEACHER -> {
                            TeacherPortalScreen(
                                state = state,
                                onSaveAttendance = { viewModel.saveAttendance() },
                                onAttendanceClassSelected = { viewModel.selectAttendanceClass(it) },
                                onAttendanceDateSelected = { viewModel.selectAttendanceDate(it) },
                                onAttendanceRetry = { viewModel.retryAttendance() },
                                onAttendanceNoteChanged = { id, note -> viewModel.updateAttendanceNote(id, note) },
                                onUpdateAttendanceStatus = { stdId, status -> viewModel.updateSingleAttendanceStatus(stdId, status) },
                                onMarkAllPresent = { viewModel.markAllPresent() },
                                onSaveDailyReport = { report -> viewModel.saveStudentDailyReport(report) },
                                onMarkMedicineGiven = { noticeId -> viewModel.markMedicineGiven(noticeId) },
                                onApproveLeave = { noticeId -> viewModel.approveLeaveNotice(noticeId) },
                                onUploadMaterial = { title, desc, subj, cls, type, ext, size, due, req, sum ->
                                    viewModel.createCourseMaterial(title, desc, subj, cls, type, ext, size, due, req, sum)
                                },
                                onDownloadMaterial = { mat -> viewModel.downloadCourseMaterial(mat) },
                                onDeleteMaterial = { id -> viewModel.deleteCourseMaterial(id) },
                                onSendParentReminder = { matId, title -> viewModel.sendCourseMaterialParentReminder(matId, title) },
                                onSelectConversation = { id -> viewModel.selectConversation(id) },
                                onSendMessage = { convId, text, file, type -> viewModel.sendDirectMessage(convId, text, file, type) },
                                onStartNewConversation = { participantId, name, role -> viewModel.startNewConversation(participantId, name, role) },
                                onOpenThemeConfig = { viewModel.openThemeConfig(true) },
                                onRoleSelected = { role -> viewModel.switchRole(role) },
                                onOpenAiHomeworkGenerator = { viewModel.openAiHomeworkModal(true) },
                                onOpenAiNarrativeGenerator = { stdName, meal, sleep, mood, act ->
                                    viewModel.generateAiDailyNarrative(stdName, meal, sleep, mood, act)
                                },
                                onOpenAiAnalyticsHub = {
                                    currentSubScreen = SubScreen.AI_ANALYTICS
                                },
                                onOpenChatbot = { showChatbotDialog = true },
                                onLogout = { viewModel.logout() }
                            )
                        }
                        UserRole.FINANCE -> {
                            FinancePortalScreen(
                                state = state,
                                onRecordPayment = { invId, amt, method -> viewModel.recordPayment(invId, amt, method) },
                                onCreateInvoice = { stdId, title, amt, due, desc -> viewModel.createInvoice(stdId, title, amt, due, desc) },
                                onSendReminder = { invId, stdName -> viewModel.sendPaymentReminder(invId, stdName) },
                                onRoleSelected = { role -> viewModel.switchRole(role) },
                                onOpenChatbot = { showChatbotDialog = true },
                                onLogout = { viewModel.logout() }
                            )
                        }
                        UserRole.MANAGER -> {
                            BranchManagerPortalScreen(
                                state = state,
                                onAddStudent = { std -> viewModel.addStudent(std) },
                                onAddClass = { name, grade, teacher, cap, room -> viewModel.addClass(name, grade, teacher, cap, room) },
                                onCreateAnnouncement = { title, content, cat, tgt -> viewModel.createAnnouncement(title, content, cat, tgt) },
                                onDeleteAnnouncement = { id -> viewModel.deleteAnnouncement(id) },
                                onRoleSelected = { role -> viewModel.switchRole(role) },
                                onOpenChatbot = { showChatbotDialog = true },
                                onLogout = { viewModel.logout() }
                            )
                        }
                        UserRole.CANTEEN -> {
                            CanteenStaffPortalScreen(
                                state = state,
                                onAddProduct = { prod -> viewModel.addCanteenProduct(prod) },
                                onUpdateProduct = { prod -> viewModel.updateCanteenProduct(prod) },
                                onDeleteProduct = { id -> viewModel.deleteCanteenProduct(id) },
                                onProcessPurchase = { prod, qty -> viewModel.processCanteenPOSPurchase(prod, qty) },
                                onTopup = { amt, method -> viewModel.topupCanteenWallet(amt, method) },
                                onRoleSelected = { role -> viewModel.switchRole(role) },
                                onOpenChatbot = { showChatbotDialog = true },
                                onLogout = { viewModel.logout() }
                            )
                        }
                        UserRole.OWNER -> {
                            OwnerPortalScreen(
                                state = state,
                                onRoleSelected = { role -> viewModel.switchRole(role) },
                                onGenerateAiForecast = {
                                    currentSubScreen = SubScreen.AI_ANALYTICS
                                },
                                onOpenBranchManagement = {
                                    currentSubScreen = SubScreen.ORGANIZATIONS
                                },
                                onOpenFinance = {
                                    viewModel.switchRole(UserRole.ADMIN)
                                    currentTab = MainTab.FINANCE
                                },
                                onOpenReports = {
                                    currentSubScreen = SubScreen.REPORTS
                                },
                                onOpenSubscriptionBilling = {
                                    currentSubScreen = SubScreen.SUBSCRIPTION_BILLING
                                },
                                onOpenChatbot = { showChatbotDialog = true },
                                onLogout = { viewModel.logout() }
                            )
                        }
                        UserRole.ADMIN, UserRole.SUPERADMIN -> {}
                    }

                    // Shared Modals available in all Role Portals
                    if (state.showThemeConfigDialog) {
                        ThemeConfigDialog(
                            currentThemeName = state.selectedThemeName,
                            onThemeNameSelected = { viewModel.selectTheme(it) },
                            onDismiss = { viewModel.openThemeConfig(false) }
                        )
                    }

                    if (state.showAiReportModal && state.activeAiReport != null) {
                        com.example.ui.components.AiReportDetailModal(
                            report = state.activeAiReport!!,
                            onDismiss = { viewModel.dismissAiModals() }
                        )
                    }

                    if (state.showAiHomeworkModal) {
                        com.example.ui.components.AiHomeworkGeneratorModal(
                            onDismiss = { viewModel.dismissAiModals() },
                            onGenerate = { sub, top, gr, cnt, diff ->
                                viewModel.generateAiTeacherHomework(sub, top, gr, cnt, diff)
                            },
                            activeHomework = state.activeAiHomework
                        )
                    }

                    if (state.showAiNarrativeModal) {
                        com.example.ui.components.AiNarrativeGeneratorModal(
                            onDismiss = { viewModel.dismissAiModals() },
                            onGenerate = { name, meal, sleep, mood, act, note ->
                                viewModel.generateAiTeacherDailyNarrative(name, meal, sleep, mood, act, note)
                            },
                            activeNarrative = state.activeDailyNarrative
                        )
                    }

                    if (state.showAiParentDigestModal && state.activeAiParentDigest != null) {
                        com.example.ui.components.AiParentDigestModal(
                            digest = state.activeAiParentDigest!!,
                            onDismiss = { viewModel.dismissAiModals() }
                        )
                    }

                    if (showChatbotDialog) {
                        Okul360ChatbotModal(
                            state = state,
                            onDismiss = { showChatbotDialog = false },
                            onNavigateAction = { actionTag ->
                                showChatbotDialog = false
                                handleChatbotNavigation(actionTag)
                            }
                        )
                    }
                }
            }
        }
        return
    }

    // Visible primary tabs on the bottom navigation bar for Admin / Executive (Includes Quick Attendance, Notifications & Finance)
    val visibleTabs = listOf(
        MainTab.HOME,
        MainTab.STUDENTS,
        MainTab.ATTENDANCE,
        MainTab.NOTIFICATIONS,
        MainTab.FINANCE,
        MainTab.OTHER
    )

    val subscreensWithOwnTopBar = remember {
        setOf(
            SubScreen.APPOINTMENTS,
            SubScreen.TRANSPORT,
            SubScreen.CANTEEN,
            SubScreen.HOMEWORK,
            SubScreen.LIBRARY,
            SubScreen.STAFF,
            SubScreen.ORGANIZATIONS,
            SubScreen.RFID_DEVICES,
            SubScreen.CANTEEN_PORTAL,
            SubScreen.EXAMS,
            SubScreen.OUTBOX,
            SubScreen.NOTIFICATIONS,
            SubScreen.DIGITAL_ID_CARD,
            SubScreen.SCHEDULE,
            SubScreen.DAILY_REPORTS,
            SubScreen.DIGITAL_REPORT_CARD,
            SubScreen.AI_CHAT_AUDIT
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (currentSubScreen == SubScreen.NONE) {
                TopBrandHeader(
                    institutionName = state.currentUser.institution_name,
                    branches = state.branches,
                    selectedBranchId = state.selectedBranchId,
                    selectedRole = state.selectedRole,
                    onBranchSelected = { branchId -> viewModel.switchBranch(branchId) },
                    onRoleSelected = { role -> viewModel.switchRole(role) },
                    onSettingsClicked = {
                        currentSubScreen = SubScreen.SETTINGS
                    },
                    onNotificationsClicked = {
                        currentSubScreen = SubScreen.NOTIFICATIONS
                    },
                    onSearchClicked = {
                        showGlobalSearchDialog = true
                    },
                    onChatbotClicked = {
                        showChatbotDialog = true
                    },
                    unreadNotificationsCount = (state.notifications.count { !it.is_read } + state.announcements.count { it.read_count == 0 }).let { if (it > 0) it else 3 }
                )
            } else if (currentSubScreen !in subscreensWithOwnTopBar && !(currentSubScreen == SubScreen.DIRECT_MESSAGING && state.selectedConversationId != null)) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TopAppBar(
                            modifier = Modifier.widthIn(max = 840.dp),
                            title = {
                                Text(
                                    text = currentSubScreen.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = BrandNavy
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { currentSubScreen = SubScreen.NONE }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Geri",
                                        tint = PrimaryBlue
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showGlobalSearchDialog = true },
                                    modifier = Modifier.testTag("subscreen_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Genel Arama",
                                        tint = PrimaryBlue
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        },
        bottomBar = {
            val isImeOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
            val isDirectChatActive = currentSubScreen == SubScreen.DIRECT_MESSAGING && state.selectedConversationId != null
            val isAiChatActive = currentTab == MainTab.AI_CHATBOT && currentSubScreen == SubScreen.NONE

            if (!isDirectChatActive && !(isAiChatActive && isImeOpen)) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        NavigationBar(
                            modifier = Modifier.widthIn(max = 840.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            val absentCountToday = remember(state.attendanceRecords) {
                                state.attendanceRecords.count { it.status.equals("ABSENT", ignoreCase = true) }
                            }
                            val overdueInvoicesCount = remember(state.invoices) {
                                state.invoices.count { it.status.equals("OVERDUE", ignoreCase = true) }
                            }
                            val unreadNotificationsAndAnnouncementsCount = remember(state.notifications, state.announcements) {
                                val unreadNotifs = state.notifications.count { !it.is_read }
                                val unreadAnnouncements = state.announcements.count { it.read_count == 0 }
                                val total = unreadNotifs + unreadAnnouncements
                                if (total > 0) total else 3
                            }

                            visibleTabs.forEach { tab ->
                                val isSelected = when (tab) {
                                    MainTab.AI_CHATBOT -> currentTab == MainTab.AI_CHATBOT && currentSubScreen == SubScreen.NONE
                                    MainTab.NOTIFICATIONS -> (currentTab == MainTab.NOTIFICATIONS && currentSubScreen == SubScreen.NONE) || currentSubScreen == SubScreen.NOTIFICATIONS
                                    else -> (currentSubScreen == SubScreen.NONE && currentTab == tab && !showChatbotDialog) ||
                                            (currentSubScreen != SubScreen.NONE && currentSubScreen != SubScreen.NOTIFICATIONS && tab == MainTab.OTHER && !showChatbotDialog)
                                }

                                val badgeCount = when (tab) {
                                    MainTab.NOTIFICATIONS -> unreadNotificationsAndAnnouncementsCount
                                    MainTab.ATTENDANCE -> absentCountToday
                                    MainTab.FINANCE -> overdueInvoicesCount
                                    else -> 0
                                }

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        currentTab = tab
                                        if (tab == MainTab.NOTIFICATIONS) {
                                            currentSubScreen = SubScreen.NOTIFICATIONS
                                        } else {
                                            currentSubScreen = SubScreen.NONE
                                        }
                                        homeFabExpanded = false
                                    },
                                    icon = {
                                        if (badgeCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = when (tab) {
                                                            MainTab.NOTIFICATIONS -> AccentRose
                                                            MainTab.FINANCE -> Color(0xFFDC2626)
                                                            else -> Color(0xFFD97706)
                                                        },
                                                        contentColor = Color.White
                                                    ) {
                                                        Text(
                                                            text = if (badgeCount > 99) "99+" else "$badgeCount",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.title,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        indicatorColor = PrimaryBlueContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentSubScreen == SubScreen.NONE && currentTab != MainTab.AI_CHATBOT) {
                HomeSpeedDialFab(
                    expanded = homeFabExpanded,
                    onExpandedChange = { homeFabExpanded = it },
                    onTakeAttendance = {
                        homeFabExpanded = false
                        showQuickAttendanceSheet = true
                    },
                    onPostAnnouncement = {
                        homeFabExpanded = false
                        viewModel.openCreateAnnouncement(true)
                    },
                    onReceivePayment = {
                        homeFabExpanded = false
                        showQuickPaymentSheet = true
                    },
                    onAddStudent = {
                        homeFabExpanded = false
                        viewModel.openAddStudentDialog(true)
                    },
                    onOpenDailyReportOrHomework = {
                        homeFabExpanded = false
                        currentSubScreen = SubScreen.DAILY_REPORTS
                    },
                    onOpenConsentsOrApprovals = {
                        homeFabExpanded = false
                        currentSubScreen = SubScreen.CONSENTS
                    }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Offline Mode Banner
            AnimatedVisibility(visible = state.isOffline) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 840.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Çevrimdışı",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Çevrimdışı Mod (Room)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Bağlantı yok. Yerel Room veritabanındaki kayıtlar görüntüleniyor.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            TextButton(
                                onClick = { viewModel.retryConnection() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Yenile", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // SaaS GRACE / RESTRICTED / SUSPENDED Access Banner (SAAS_ISLETIMI & SAAS_KABUL_PLANI)
            AnimatedVisibility(
                visible = state.organizationAccess.status != "ACTIVE",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val isGrace = state.organizationAccess.status == "GRACE"
                val bannerBg = if (isGrace) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                val bannerFg = if (isGrace) Color(0xFF92400E) else Color(0xFF991B1B)
                Surface(
                    color = bannerBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isGrace) Icons.Default.WarningAmber else Icons.Default.Lock,
                                contentDescription = state.organizationAccess.status,
                                tint = bannerFg,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isGrace) {
                                        "Ek Süre (GRACE): Son Tarih ${state.organizationAccess.grace_until ?: "10 Gün"}"
                                    } else {
                                        "Erişim Kısıtlı (${state.organizationAccess.status} • HTTP 402)"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bannerFg
                                )
                                Text(
                                    text = if (isGrace) {
                                        "Üyelik süreniz doldu, 10×24 saatlik ek süre içindesiniz. Kesinti olmaması için yenileyin."
                                    } else {
                                        "İşlemlere devam etmek için Üyelik ve Ödeme sayfasından yenileme talebi oluşturun."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = bannerFg.copy(alpha = 0.9f)
                                )
                            }
                        }
                        TextButton(
                            onClick = { currentSubScreen = SubScreen.SUBSCRIPTION_BILLING },
                            colors = ButtonDefaults.textButtonColors(contentColor = bannerFg)
                        ) {
                            Text("Üyelik & Ödeme", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // SuperAdmin (Root) Scoped Tenant Workspace Banner (X-SCS360-Organization-Id)
            AnimatedVisibility(
                visible = state.selectedRole == UserRole.SUPERADMIN && state.workspaceOrganizationId != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = Color(0xFFEDE9FE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Kurum Çalışma Alanı",
                                tint = Color(0xFF5B21B6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Seçili Kurum Çalışma Alanı: ${state.workspaceOrganizationName ?: state.workspaceOrganizationId}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4C1D95)
                                )
                                Text(
                                    text = "X-SCS360-Organization-Id: ${state.workspaceOrganizationId} (Root DB rolü değişmez)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF5B21B6)
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                viewModel.exitTenantWorkspace()
                                currentSubScreen = SubScreen.PLATFORM_SAAS
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4C1D95)),
                            modifier = Modifier.testTag("return_to_platform_btn")
                        ) {
                            Text("Platform Yönetimine Dön", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                com.example.ui.theme.AppContentWrapper {
                    AnimatedContent(
                        targetState = Pair(currentTab, currentSubScreen),
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(150))
                                )
                        },
                        label = "ScreenFadeTransition"
                    ) { (tab, subScreen) ->
                    if (subScreen != SubScreen.NONE) {
                        when (subScreen) {
                            SubScreen.CLASSES -> ClassesScreen(
                                state = state,
                                onAddClass = { name, grade, teacher, cap, room ->
                                    viewModel.addClass(name, grade, teacher, cap, room)
                                },
                                onUpdateClass = { cls ->
                                    viewModel.updateClass(cls)
                                },
                                onDeleteClass = { clsId ->
                                    viewModel.deleteClass(clsId)
                                },
                                onAssignStudent = { stdId, clsId ->
                                    viewModel.assignStudentToClass(stdId, clsId)
                                },
                                onRemoveStudent = { stdId ->
                                    viewModel.removeStudentFromClass(stdId)
                                },
                                onSelectClassDetail = { cls ->
                                    viewModel.selectClassDetail(cls)
                                },
                                onTransferStudent = { studentId, currentClassId, targetClassId ->
                                    viewModel.transferStudents(currentClassId, targetClassId, listOf(studentId))
                                }
                            )
                            SubScreen.COMMUNICATION -> CommunicationScreen(
                                state = state,
                                onCreateAnnouncement = { title, content, cat, tgt ->
                                    viewModel.createAnnouncement(title, content, cat, tgt)
                                },
                                onDeleteAnnouncement = { id ->
                                    viewModel.deleteAnnouncement(id)
                                },
                                onSaveAnnouncementDraft = { title, content, cat, tgt ->
                                    viewModel.saveAnnouncementDraft(title, content, cat, tgt)
                                },
                                onDeleteAnnouncementDraft = { id ->
                                    viewModel.deleteAnnouncementDraft(id)
                                },
                                onPublishAnnouncementDraft = { id ->
                                    viewModel.publishAnnouncementDraft(id)
                                },
                                onSendNotification = { title, msg, tgt, chan ->
                                    viewModel.sendNotification(title, msg, tgt, chan)
                                },
                                onDeleteNotification = { id ->
                                    viewModel.deleteNotification(id)
                                },
                                onCreateTemplate = { title, type, content ->
                                    viewModel.createTemplate(title, type, content)
                                },
                                onDeleteTemplate = { id ->
                                    viewModel.deleteTemplate(id)
                                },
                                onTriggerPushTest = { title, msg, chan ->
                                    viewModel.triggerCustomPush(title, msg, chan)
                                },
                                onTriggerDelayedPushTest = { delaySec, title, msg, chan ->
                                    viewModel.triggerDelayedPush(delaySec, title, msg, chan)
                                },
                                onOpenNotificationSettings = {
                                    viewModel.openDeviceNotificationSettings()
                                },
                                onAnnouncementClick = { ann ->
                                    viewModel.selectAnnouncementDetail(ann)
                                }
                            )
                            SubScreen.REPORTS -> ReportsScreen(
                                state = state,
                                onSendReminder = { invId, stdName ->
                                    viewModel.sendPaymentReminder(invId, stdName)
                                },
                                onPrintClassReportCards = { clsName ->
                                    viewModel.printClassReportCards(context, clsName)
                                },
                                onExportAttendanceCsv = {
                                    viewModel.exportStudentAttendanceCsv(context)
                                },
                                onExportFinanceCsv = {
                                    viewModel.exportFinanceCsv(context)
                                },
                                onOpenAiAnalytics = {
                                    currentSubScreen = SubScreen.AI_ANALYTICS
                                }
                            )
                            SubScreen.RESOURCES -> ResourcesScreen(
                                state = state,
                                onAddResource = { name, cat, loc, status, qty, notes ->
                                    viewModel.addResource(name, cat, loc, status, qty, notes)
                                },
                                onDeleteResource = { id ->
                                    viewModel.deleteResource(id)
                                }
                            )
                            SubScreen.ROLES -> RolesScreen(
                                state = state,
                                onRoleSelected = { viewModel.switchRole(it) },
                                onTogglePermission = { roleId, permCode, isGranted ->
                                    viewModel.togglePermission(roleId, permCode, isGranted)
                                },
                                onCreateRole = { name, code, desc ->
                                    viewModel.createRole(name, code, desc)
                                },
                                onDeleteRole = { id ->
                                    viewModel.deleteRole(id)
                                }
                            )
                            SubScreen.USERS -> UsersScreen(
                                state = state,
                                onAddUser = { full, email, role, phone, branch, password ->
                                    viewModel.addUser(full, email, role, phone, branch, password)
                                },
                                onDeleteUser = { id ->
                                    viewModel.deleteUser(id)
                                },
                                onResetPassword = { id, newPass ->
                                    viewModel.resetUserPassword(id, newPass)
                                },
                                onUpdateUserAccess = { userId, roles, branchIds, status ->
                                    viewModel.updateUserAccess(userId, roles, branchIds, status)
                                },
                                onEnsureStandardRoles = {
                                    viewModel.ensureStandardRolesForOrganization()
                                },
                                onLinkParentAccount = { studentId, parentUserId, receiveNotifications, smsPhone, whatsappPhone ->
                                    viewModel.linkParentUserAccount(studentId, parentUserId, receiveNotifications, smsPhone, whatsappPhone)
                                }
                            )
                            SubScreen.DAILY_REPORTS -> DailyReportsScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onCreateReport = { report -> viewModel.saveStudentDailyReport(report) }
                            )
                            SubScreen.DIGITAL_ID_CARD -> DigitalIdCardScreen(
                                state = state,
                                onBack = { currentSubScreen = SubScreen.NONE }
                            )
                            SubScreen.SCHEDULE -> WeeklyScheduleScreen(
                                state = state,
                                onBack = { currentSubScreen = SubScreen.NONE }
                            )
                            SubScreen.APPOINTMENTS -> AppointmentsScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onBookAppointment = { teacherId, teacherName, subject, date, timeSlot, type, notes ->
                                    viewModel.bookAppointment(teacherId, teacherName, subject, date, timeSlot, type, notes)
                                },
                                onUpdateStatus = { aptId, newStatus, feedback ->
                                    viewModel.updateAppointmentStatus(aptId, newStatus, feedback)
                                }
                            )
                            SubScreen.TRANSPORT -> TransportScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onStudentCheckIn = { routeId, studentId, isMorning, isBoarded ->
                                    viewModel.updateBusStudentCheckIn(routeId, studentId, isMorning, isBoarded)
                                },
                                onUpdateTelemetry = { routeId, lat, lng, speed, passedStop, nextStop, eta ->
                                    viewModel.updateBusLiveTelemetry(routeId, lat, lng, speed, passedStop, nextStop, eta)
                                }
                            )
                            SubScreen.CANTEEN -> CanteenWalletScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onTopup = { amount, method ->
                                    viewModel.topupCanteenWallet(amount, method)
                                },
                                onUpdateRestrictions = { limit, categories, allergens ->
                                    viewModel.updateCanteenRestrictions(limit, categories, allergens)
                                },
                                onProcessPurchase = { product, quantity ->
                                    viewModel.processCanteenPOSPurchase(product, quantity)
                                }
                            )
                            SubScreen.HOMEWORK -> HomeworkGradingScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onSubmitHomework = { matId, title, text, fileName, fileSize ->
                                    viewModel.submitHomework(matId, title, text, fileName, fileSize)
                                },
                                onGradeSubmission = { subId, score, feedback ->
                                    viewModel.gradeHomeworkSubmission(subId, score, feedback)
                                }
                            )
                            SubScreen.LIBRARY -> LibraryScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onBorrowBook = { bookId, stdId, stdName, stdNo, clsName, days ->
                                    viewModel.borrowLibraryBook(bookId, stdId, stdName, stdNo, clsName, days)
                                },
                                onReturnBook = { loanId ->
                                    viewModel.returnLibraryBook(loanId)
                                },
                                onSendOverdueReminder = { loanId ->
                                    viewModel.sendLibraryOverdueReminder(loanId)
                                }
                            )
                            SubScreen.STAFF -> StaffManagementScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onSubmitLeaveRequest = { type, start, end, days, reason ->
                                    viewModel.submitTeacherLeaveRequest(type, start, end, days, reason)
                                },
                                onReviewLeaveRequest = { id, approved, comment ->
                                    viewModel.reviewTeacherLeaveRequest(id, approved, comment)
                                }
                            )
                            SubScreen.AI_ANALYTICS -> AiAnalyticsHubScreen(
                                state = state,
                                onGenerateOwnerForecast = { viewModel.generateAiOwnerForecast() },
                                onGenerateBranchHealth = { viewModel.generateAiBranchHealthReport() },
                                onGenerateChurnRisk = { viewModel.generateAiStudentChurnRisk() },
                                onOpenHomeworkGenerator = {
                                    viewModel.generateAiTeacherHomework(
                                        subject = "Matematik",
                                        topic = "Birinci Dereceden Denklemler",
                                        gradeLevel = "8. Sınıf",
                                        questionCount = 3,
                                        difficulty = "Orta"
                                    )
                                },
                                onOpenNarrativeGenerator = {
                                    viewModel.generateAiTeacherDailyNarrative(
                                        studentName = "Ali Demir",
                                        meal = "Tam Bitirdi",
                                        sleep = "1.5 Saat Uyudu",
                                        mood = "Neşeli & Paylaşımcı",
                                        activities = "Robotik Kodlama ve Resim"
                                    )
                                },
                                onOpenParentDigest = {
                                    val student = state.students.firstOrNull() ?: com.example.data.model.Student(id = "std-1", first_name = "Ali", last_name = "Demir", student_number = "101", class_name = "8-A")
                                    viewModel.generateAiParentWeeklyDigest(student)
                                },
                                onSelectReport = { report ->
                                    viewModel.openAiReportDetail(report)
                                },
                                onRefreshAiAnalysis = {
                                    viewModel.generateFullInstitutionalAnalysis(isGeminiApi = true)
                                },
                                onFilterBranch = { branch ->
                                    viewModel.filterAnalyticsBranch(branch)
                                },
                                onFilterTimeframe = { timeframe ->
                                    viewModel.filterAnalyticsTimeframe(timeframe)
                                },
                                onSetAnalyticsTab = { tabIdx ->
                                    viewModel.setAnalyticsTab(tabIdx)
                                },
                                onPrintPdfReport = {
                                    viewModel.printInstitutionalPerformanceReport(context)
                                },
                                onSendStudentAction = { item, actionType ->
                                    if (actionType == "PARENT_NOTE") {
                                        viewModel.generateAiTeacherDailyNarrative(
                                            studentName = item.student_name,
                                            meal = "Normal",
                                            sleep = "Düzenli",
                                            mood = "Motivasyonu Yüksek",
                                            activities = item.primary_strength,
                                            customNote = "Pedagojik tavsiye: ${item.ai_pedagogical_advice}"
                                        )
                                    } else if (actionType == "HOMEWORK") {
                                        viewModel.generateAiTeacherHomework(
                                            subject = "Matematik & Problem Çözme",
                                            topic = item.improvement_area,
                                            gradeLevel = item.class_name,
                                            questionCount = 4,
                                            difficulty = "Orta"
                                        )
                                    }
                                }
                            )
                            SubScreen.NOTIFICATIONS -> NotificationCenterScreen(
                                state = state,
                                onBack = { currentSubScreen = SubScreen.NONE },
                                onAnnouncementClick = { ann ->
                                    viewModel.selectAnnouncementDetail(ann)
                                    currentSubScreen = SubScreen.COMMUNICATION
                                },
                                onOpenFinance = {
                                    currentSubScreen = SubScreen.NONE
                                    currentTab = MainTab.FINANCE
                                },
                                onOpenAttendance = {
                                    currentSubScreen = SubScreen.NONE
                                    currentTab = MainTab.ATTENDANCE
                                },
                                onOpenReportCard = {
                                    currentSubScreen = SubScreen.DIGITAL_REPORT_CARD
                                }
                            )
                            SubScreen.DIGITAL_REPORT_CARD -> {
                                val targetStudent = state.viewingReportCardForStudent ?: state.students.firstOrNull()
                                if (targetStudent != null) {
                                    ReportCardViewer(
                                        student = targetStudent,
                                        onDismiss = { currentSubScreen = SubScreen.NONE },
                                        onPrintPdf = { std -> viewModel.printStudentReportCard(context, std) }
                                    )
                                }
                            }
                            SubScreen.COURSE_MATERIALS -> CourseMaterialsScreen(
                                state = state,
                                onUploadMaterial = { title, desc, subj, cls, type, ext, size, due, req, sum ->
                                    viewModel.createCourseMaterial(title, desc, subj, cls, type, ext, size, due, req, sum)
                                },
                                onDownloadMaterial = { mat -> viewModel.downloadCourseMaterial(mat) },
                                onDeleteMaterial = { id -> viewModel.deleteCourseMaterial(id) },
                                onAcknowledgeMaterial = { matId -> viewModel.acknowledgeCourseMaterial(matId) },
                                onSendParentReminder = { matId, title -> viewModel.sendCourseMaterialParentReminder(matId, title) }
                            )
                            SubScreen.DIRECT_MESSAGING -> DirectMessagingScreen(
                                state = state,
                                onSelectConversation = { id -> viewModel.selectConversation(id) },
                                onSendMessage = { convId, text, file, type -> viewModel.sendDirectMessage(convId, text, file, type) },
                                onStartNewConversation = { participantId, name, role -> viewModel.startNewConversation(participantId, name, role) }
                            )
                            SubScreen.ORGANIZATIONS -> OrganizationsBranchesScreen(
                                state = state,
                                onBack = { currentSubScreen = SubScreen.NONE },
                                onAddBranch = { branch -> viewModel.createBranch(branch) },
                                onSwitchBranch = { bId -> viewModel.switchBranch(bId) },
                                onUpdateOrganizationContacts = { orgId, name, taxOffice, taxNumber, phone, email, address ->
                                    viewModel.updateOrganizationContacts(orgId, name, taxOffice, taxNumber, phone, email, address)
                                },
                                onUpdateBranchDetails = { updatedBranch ->
                                    viewModel.updateBranchDetails(updatedBranch)
                                }
                            )
                            SubScreen.RFID_DEVICES -> RfidDevicesScreen(
                                state = state,
                                onBack = { currentSubScreen = SubScreen.NONE },
                                onAddCard = { card -> viewModel.addRfidCard(card) },
                                onToggleCardStatus = { cardId -> viewModel.toggleCardStatus(cardId) }
                            )
                            SubScreen.CANTEEN_PORTAL -> CanteenStaffPortalScreen(
                                state = state,
                                onAddProduct = { prod -> viewModel.addCanteenProduct(prod) },
                                onUpdateProduct = { prod -> viewModel.updateCanteenProduct(prod) },
                                onDeleteProduct = { id -> viewModel.deleteCanteenProduct(id) },
                                onProcessPurchase = { prod, qty -> viewModel.processCanteenPOSPurchase(prod, qty) },
                                onTopup = { amt, method -> viewModel.topupCanteenWallet(amt, method) },
                                onLogout = { currentSubScreen = SubScreen.NONE }
                            )
                            SubScreen.SETTINGS -> SettingsScreen(
                                state = state,
                                onRoleSelected = { viewModel.switchRole(it) },
                                onTestBackendConnection = { viewModel.testBackendConnection() },
                                onUpdateServerConfig = { url, tenant -> viewModel.updateServerSettings(url, tenant) },
                                onOpenNotificationSettings = { viewModel.openDeviceNotificationSettings() },
                                onTestNotification = {
                                    viewModel.triggerCustomPush("Push Bildirim Testi", "Android push bildirim sistemi başarıyla çalışıyor.", "ANNOUNCEMENT")
                                },
                                onThemeSelected = { viewModel.selectTheme(it) },
                                onOpenFeedback = { viewModel.openFeedbackDialog(true) },
                                onTestPaymentReminder = { viewModel.testPaymentReminderPush() },
                                onClearLocalCache = { viewModel.clearLocalCache() },
                                onChangePassword = { currentPass, newPass -> viewModel.changePassword(currentPass, newPass) },
                                onLogout = {
                                    viewModel.logout()
                                    currentSubScreen = SubScreen.NONE
                                }
                            )
                            SubScreen.CONSENTS -> ConsentsScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onApproveConsent = { id, parentName -> viewModel.approveConsent(id, parentName) },
                                onCreateConsent = { consent -> viewModel.createConsent(consent) }
                            )
                            SubScreen.GUIDANCE -> GuidanceScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onCreateRecord = { record -> viewModel.createGuidanceRecord(record) },
                                onDeleteRecord = { id -> viewModel.deleteGuidanceRecord(id) }
                            )
                            SubScreen.INFIRMARY -> InfirmaryScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onCreateVisit = { visit -> viewModel.createInfirmaryVisit(visit) }
                            )
                            SubScreen.CLUBS -> ClubsScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onEnrollClub = { clubId ->
                                    val stdId = state.students.firstOrNull()?.id ?: "std-1"
                                    viewModel.enrollInClub(clubId, stdId)
                                },
                                onCreateClub = { club -> viewModel.createClub(club) }
                            )
                            SubScreen.SURVEYS -> SurveysScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onParticipate = { id, answers -> viewModel.participateSurvey(id, answers) },
                                onCreateSurvey = { s -> viewModel.createSurvey(s) }
                            )
                            SubScreen.TUTORING -> TutoringScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onCreateSession = { session -> viewModel.createTutoringSession(session) }
                            )
                            SubScreen.PAYROLL -> PayrollScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onCreatePayroll = { record -> viewModel.createPayrollRecord(record) }
                            )
                            SubScreen.SECURITY -> SecurityAuditScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE },
                                onVerifyGateAccess = { studentId, direction, method, codeInput ->
                                    viewModel.verifyGateAccess(studentId, direction, method, codeInput)
                                },
                                onIssueIdCard = { studentId ->
                                    viewModel.issueStudentIdCard(studentId)
                                },
                                onRevokeIdCard = { cardId ->
                                    viewModel.revokeStudentIdCard(cardId)
                                },
                                onDownloadQrSvg = { qrRecordId ->
                                    viewModel.downloadQrSvg(qrRecordId)
                                },
                                onNavigateToDismissal = { currentSubScreen = SubScreen.DISMISSAL }
                            )
                            SubScreen.EXAMS -> ExamsScreen(
                                state = state,
                                onBack = { currentSubScreen = SubScreen.NONE },
                                onCreateExam = { exam -> viewModel.createExam(exam) },
                                onUpdateExam = { exam -> viewModel.updateExam(exam) },
                                onDeleteExam = { id -> viewModel.deleteExam(id) }
                            )
                            SubScreen.OUTBOX -> OutboxBroadcastScreen(
                                state = state,
                                onBack = { currentSubScreen = SubScreen.NONE },
                                onCreateBroadcast = { job -> viewModel.createBroadcastJob(job) },
                                onSendBroadcast = { id -> viewModel.sendBroadcastJob(id) },
                                onCreateTemplate = { tpl -> viewModel.createMessageTemplate(tpl) }
                            )
                            SubScreen.AI_CHAT_AUDIT -> AiChatAuditScreen(
                                state = state,
                                onNavigateBack = { currentSubScreen = SubScreen.NONE }
                            )
                            SubScreen.PLATFORM_SAAS -> PlatformSaasScreen(
                                currentUserRole = state.selectedRole,
                                plans = state.subscriptionPlans,
                                tenants = state.platformTenants,
                                invoices = state.subscriptionInvoices,
                                onCreatePlan = { plan -> viewModel.createPlatformPlan(plan) },
                                onCreateTenantWithOwner = { draft ->
                                    viewModel.createTenantWithOwner(draft)
                                },
                                onUpdateTenantSubscription = { orgId, planId, periodStart, periodEnd ->
                                    viewModel.updateTenantSubscription(orgId, planId, periodStart, periodEnd)
                                },
                                onToggleTenantActive = { orgId, isActive ->
                                    viewModel.toggleTenantActive(orgId, isActive)
                                },
                                onVerifyInvoicePayment = { invoiceId, amount, currency, bankRef, paidAt ->
                                    viewModel.verifyPlatformInvoicePayment(invoiceId, amount, currency, bankRef, paidAt)
                                },
                                onOpenTenantWorkspace = { orgId, orgName ->
                                    viewModel.openTenantWorkspace(orgId, orgName)
                                    currentSubScreen = SubScreen.NONE
                                    currentTab = MainTab.HOME
                                },
                                onReconcilePaidReviewInvoice = { invoiceId, periodStart, periodEnd, note ->
                                    viewModel.reconcilePaidReviewInvoice(invoiceId, periodStart, periodEnd, note)
                                },
                                onRecordSubscriptionRefund = { invoiceId, refundRef, note ->
                                    viewModel.recordSubscriptionRefund(invoiceId, refundRef, note)
                                },
                                onUpdatePlan = { plan ->
                                    viewModel.updateSubscriptionPlan(plan)
                                }
                            )
                            SubScreen.SUBSCRIPTION_BILLING -> SubscriptionBillingScreen(
                                currentUserRole = state.selectedRole,
                                accessStatus = state.organizationAccess,
                                plans = state.subscriptionPlans,
                                invoices = state.subscriptionInvoices,
                                onRefreshAccessStatus = { viewModel.refreshSubscriptionAccessStatus() },
                                onCreateRenewalRequest = { planId, billingPeriod ->
                                    viewModel.createRenewalInvoiceRequest(planId, billingPeriod)
                                },
                                onSimulateAccessStatusChange = { newStatus ->
                                    viewModel.simulateAccessStatusChange(newStatus)
                                },
                                onStartCardCheckout = { invoiceId, buyerName, buyerEmail, billingAddress ->
                                    viewModel.startSaasCardCheckout(invoiceId, buyerName, buyerEmail, billingAddress)
                                },
                                onCheckPaymentStatus = { invoiceId ->
                                    viewModel.checkSaasCheckoutStatus(invoiceId)
                                },
                                onActivateFreePlan = { planId ->
                                    viewModel.activateFreeSubscriptionPlan(planId)
                                },
                                onCancelPendingInvoice = { invoiceId ->
                                    viewModel.cancelPendingSubscriptionInvoice(invoiceId)
                                },
                                onLogout = { viewModel.logout() }
                            )
                            SubScreen.DISMISSAL -> DismissalScreen(
                                students = state.students,
                                authorizations = state.pickupAuthorizations,
                                dismissalLogs = state.dismissalLogs,
                                onCreateAuthorization = { auth -> viewModel.createPickupAuthorization(auth) },
                                onCancelAuthorization = { authId -> viewModel.cancelPickupAuthorization(authId) },
                                onRecordHandover = { studentId, nationalId, notes ->
                                    viewModel.recordStudentHandover(studentId, nationalId, notes)
                                },
                                onNavigateToSecurity = { currentSubScreen = SubScreen.SECURITY }
                            )
                            SubScreen.NONE -> {}
                        }
                    } else {
                        when (tab) {
                            MainTab.HOME -> DashboardScreen(
                                state = state,
                                onNavigateToAttendance = { currentTab = MainTab.ATTENDANCE },
                                onNavigateToStudents = { currentTab = MainTab.STUDENTS },
                                onNavigateToFinance = { currentTab = MainTab.FINANCE },
                                onNavigateToAnnouncements = { currentSubScreen = SubScreen.COMMUNICATION },
                                onNavigateToDailyReports = { currentSubScreen = SubScreen.DAILY_REPORTS },
                                onNavigateToClasses = { currentSubScreen = SubScreen.CLASSES },
                                onNavigateToCommunication = { currentSubScreen = SubScreen.COMMUNICATION },
                                onNavigateToReports = { currentSubScreen = SubScreen.REPORTS },
                                onNavigateToResources = { currentSubScreen = SubScreen.RESOURCES },
                                onNavigateToRoles = { currentSubScreen = SubScreen.ROLES },
                                onNavigateToUsers = { currentSubScreen = SubScreen.USERS },
                                onOpenAddStudent = { viewModel.openAddStudentDialog(true) },
                                onOpenCreateAnnouncement = { viewModel.openCreateAnnouncement(true) },
                                onNavigateToDigitalId = { currentSubScreen = SubScreen.DIGITAL_ID_CARD },
                                onNavigateToSchedule = { currentSubScreen = SubScreen.SCHEDULE },
                                onNavigateToAppointments = { currentSubScreen = SubScreen.APPOINTMENTS },
                                onNavigateToTransport = { currentSubScreen = SubScreen.TRANSPORT },
                                onNavigateToCanteen = { currentSubScreen = SubScreen.CANTEEN },
                                onNavigateToHomework = { currentSubScreen = SubScreen.HOMEWORK },
                                onNavigateToLibrary = { currentSubScreen = SubScreen.LIBRARY },
                                onNavigateToStaff = { currentSubScreen = SubScreen.STAFF },
                                onNavigateToAiAnalytics = { currentSubScreen = SubScreen.AI_ANALYTICS },
                                onNavigateToMenu = { currentTab = MainTab.OTHER },
                                onOpenQuickAttendance = { showQuickAttendanceSheet = true },
                                onOpenQuickPayment = { showQuickPaymentSheet = true },
                                onSelectInvoiceForPayment = { viewModel.selectInvoiceForPayment(it) },
                                onSendPaymentReminder = { id, name -> viewModel.sendPaymentReminder(id, name) },
                                onApproveLeaveNotice = { id -> viewModel.approveLeaveNotice(id) },
                                onMarkMedicineGiven = { id -> viewModel.markMedicineGiven(id) },
                                onApproveConsent = { id, parentName -> viewModel.approveConsent(id, parentName) },
                                onNavigateToConsents = { currentSubScreen = SubScreen.CONSENTS },
                                onNavigateToDismissal = { currentSubScreen = SubScreen.DISMISSAL },
                                onNavigateToSecurity = { currentSubScreen = SubScreen.SECURITY },
                                onNavigateToSubscription = { currentSubScreen = SubScreen.SUBSCRIPTION_BILLING },
                                onSelectStudent = { viewModel.selectStudent(it) },
                                onUpdateAttendanceStatus = { stdId, status -> viewModel.updateSingleAttendanceStatus(stdId, status) }
                            )
                            MainTab.STUDENTS -> StudentsScreen(
                                state = state,
                                onSearchChange = { viewModel.onSearchQueryChanged(it) },
                                onFilterClassChange = { viewModel.onFilterClassChanged(it) },
                                onFilterAcademicYearChange = { viewModel.onFilterAcademicYearChanged(it) },
                                onSelectStudent = { viewModel.selectStudent(it) },
                                onOpenAddStudent = { viewModel.openAddStudentDialog(true) },
                                onUpdateStudent = { viewModel.updateStudent(it) },
                                onDeleteStudent = { viewModel.deleteStudent(it) },
                                onPrintReportCard = { std -> viewModel.printStudentReportCard(context, std) },
                                onPrintClassReportCards = { clsName -> viewModel.printClassReportCards(context, clsName) },
                                onExportCsv = { viewModel.exportStudentAttendanceCsv(context) },
                                onImportExcel = { viewModel.importStudentsExcel(5) },
                                onRefresh = { viewModel.refreshData() },
                                onViewReportCard = { std -> viewModel.openReportCardViewer(std) }
                            )
                            MainTab.ATTENDANCE -> AttendanceScreen(
                                state = state,
                                onClassSelected = { viewModel.selectAttendanceClass(it) },
                                onStatusChanged = { stdId, status -> viewModel.updateSingleAttendanceStatus(stdId, status) },
                                onMarkAllPresent = { viewModel.markAllPresent() },
                                onSaveAttendance = { viewModel.saveAttendance() },
                                onDateSelected = { viewModel.selectAttendanceDate(it) },
                                onRetry = { viewModel.retryAttendance() },
                                onNoteChanged = { id, note -> viewModel.updateAttendanceNote(id, note) }
                            )
                            MainTab.FINANCE -> FinanceScreen(
                                state = state,
                                onFilterChanged = { viewModel.setInvoiceFilter(it) },
                                onSelectInvoiceForPayment = { viewModel.selectInvoiceForPayment(it) },
                                onAddInvoice = { stdId, stdName, clsName, amt, due, desc ->
                                    viewModel.addInvoice(stdId, stdName, clsName, amt, due, desc)
                                },
                                onUpdateInvoice = { viewModel.updateInvoice(it) },
                                onDeleteInvoice = { viewModel.deleteInvoice(it) },
                                onViewReceipt = { selectedInvoiceForReceipt = it },
                                onAddExpense = { category, title, amount, date, receiptUrl ->
                                    viewModel.addExpense(category, title, amount, date, receiptUrl)
                                },
                                onDeleteExpense = { expenseId ->
                                    viewModel.deleteExpense(expenseId)
                                },
                                onSendPaymentReminder = { id, name ->
                                    viewModel.sendPaymentReminder(id, name)
                                },
                                onPayWithIyzico = { id, amt, card, name ->
                                    viewModel.payInvoiceWithIyzico(id, amt, card, name)
                                },
                                onGenerateInstallments = { sId, total, count, down, start, desc ->
                                    viewModel.generateInstallmentPlan(sId, total, down, count, start, desc)
                                },
                                onCreateDebt = { studentId, studentName, className, debtType, desc, amount, dueDate ->
                                    viewModel.createDebt(studentId, studentName, className, debtType, desc, amount, dueDate)
                                },
                                onUpdateDebtStatus = { debtId, newStatus, paidAmount ->
                                    viewModel.updateDebtStatus(debtId, newStatus, paidAmount)
                                },
                                onDeleteDebt = { debtId ->
                                    viewModel.deleteDebt(debtId)
                                }
                            )
                            MainTab.NOTIFICATIONS -> NotificationCenterScreen(
                                state = state,
                                onBack = { currentTab = MainTab.HOME },
                                onAnnouncementClick = { ann ->
                                    viewModel.selectAnnouncementDetail(ann)
                                    currentSubScreen = SubScreen.COMMUNICATION
                                },
                                onOpenFinance = {
                                    currentTab = MainTab.FINANCE
                                },
                                onOpenAttendance = {
                                    currentTab = MainTab.ATTENDANCE
                                },
                                onOpenReportCard = {
                                    currentSubScreen = SubScreen.DIGITAL_REPORT_CARD
                                }
                            )
                            MainTab.OTHER -> MenuScreen(
                                state = state,
                                onNavigateToClasses = { currentSubScreen = SubScreen.CLASSES },
                                onNavigateToCommunication = { currentSubScreen = SubScreen.COMMUNICATION },
                                onNavigateToReports = { currentSubScreen = SubScreen.REPORTS },
                                onNavigateToResources = { currentSubScreen = SubScreen.RESOURCES },
                                onNavigateToRoles = { currentSubScreen = SubScreen.ROLES },
                                onNavigateToUsers = { currentSubScreen = SubScreen.USERS },
                                onNavigateToDailyReports = { currentSubScreen = SubScreen.DAILY_REPORTS },
                                onNavigateToSettings = { currentSubScreen = SubScreen.SETTINGS },
                                onRoleSelected = { viewModel.switchRole(it) },
                                onUpdateServerConfig = { url, tenant -> viewModel.updateServerSettings(url, tenant) },
                                onUpdateProfile = { name, email, phone, inst, branch ->
                                    viewModel.updateProfile(name, email, phone, inst, branch)
                                },
                                onLogout = { viewModel.logout() },
                                onNavigateToDigitalId = { currentSubScreen = SubScreen.DIGITAL_ID_CARD },
                                onNavigateToSchedule = { currentSubScreen = SubScreen.SCHEDULE },
                                onNavigateToNotifications = { currentSubScreen = SubScreen.NOTIFICATIONS },
                                onNavigateToReportCard = { currentSubScreen = SubScreen.DIGITAL_REPORT_CARD },
                                onNavigateToCourseMaterials = { currentSubScreen = SubScreen.COURSE_MATERIALS },
                                onNavigateToDirectMessaging = { currentSubScreen = SubScreen.DIRECT_MESSAGING },
                                onOpenThemeConfig = { viewModel.openThemeConfig(true) },
                                onNavigateToAppointments = { currentSubScreen = SubScreen.APPOINTMENTS },
                                onNavigateToTransport = { currentSubScreen = SubScreen.TRANSPORT },
                                onNavigateToCanteen = { currentSubScreen = SubScreen.CANTEEN },
                                onNavigateToHomework = { currentSubScreen = SubScreen.HOMEWORK },
                                onNavigateToLibrary = { currentSubScreen = SubScreen.LIBRARY },
                                onNavigateToStaff = { currentSubScreen = SubScreen.STAFF },
                                onNavigateToAiAnalytics = { currentSubScreen = SubScreen.AI_ANALYTICS },
                                onNavigateToAiChatAudit = { currentSubScreen = SubScreen.AI_CHAT_AUDIT },
                                onNavigateToOrganizations = { currentSubScreen = SubScreen.ORGANIZATIONS },
                                onNavigateToRfidDevices = { currentSubScreen = SubScreen.RFID_DEVICES },
                                onNavigateToCanteenPortal = { currentSubScreen = SubScreen.CANTEEN_PORTAL },
                                onNavigateToConsents = { currentSubScreen = SubScreen.CONSENTS },
                                onNavigateToGuidance = { currentSubScreen = SubScreen.GUIDANCE },
                                onNavigateToInfirmary = { currentSubScreen = SubScreen.INFIRMARY },
                                onNavigateToClubs = { currentSubScreen = SubScreen.CLUBS },
                                onNavigateToSurveys = { currentSubScreen = SubScreen.SURVEYS },
                                onNavigateToTutoring = { currentSubScreen = SubScreen.TUTORING },
                                onNavigateToPayroll = { currentSubScreen = SubScreen.PAYROLL },
                                onNavigateToSecurity = { currentSubScreen = SubScreen.SECURITY },
                                onNavigateToExams = { currentSubScreen = SubScreen.EXAMS },
                                onNavigateToOutbox = { currentSubScreen = SubScreen.OUTBOX },
                                onNavigateToPlatformSaas = { currentSubScreen = SubScreen.PLATFORM_SAAS },
                                onNavigateToSubscriptionBilling = { currentSubScreen = SubScreen.SUBSCRIPTION_BILLING },
                                onNavigateToDismissal = { currentSubScreen = SubScreen.DISMISSAL }
                            )
                            MainTab.AI_CHATBOT -> AiChatbotScreen(
                                state = state,
                                onNavigateAction = { actionTag ->
                                    handleChatbotNavigation(actionTag)
                                }
                            )
                        }
                    }
                }

                // Scrim overlay when Speed Dial FAB is expanded
                if (homeFabExpanded && currentSubScreen == SubScreen.NONE && currentTab != MainTab.AI_CHATBOT) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { homeFabExpanded = false }
                            )
                    )
                }
            }
        }

        // Dialogs & Modals
            if (state.showAddStudentDialog) {
                AddStudentDialog(
                    classes = state.classes,
                    onDismiss = { viewModel.openAddStudentDialog(false) },
                    onConfirm = { first, last, clsId, pName, pPhone, allergies ->
                        viewModel.addStudent(first, last, clsId, pName, pPhone, allergies)
                    }
                )
            }

            if (showQuickAttendanceSheet) {
                QuickAttendanceBottomSheet(
                    state = state,
                    onDismiss = { showQuickAttendanceSheet = false },
                    onClassSelected = { viewModel.selectAttendanceClass(it) },
                    onStatusChanged = { stdId, status -> viewModel.updateSingleAttendanceStatus(stdId, status) },
                    onMarkAllPresent = { viewModel.markAllPresent() },
                    onSaveAttendance = { viewModel.saveAttendance() },
                    onDateSelected = { viewModel.selectAttendanceDate(it) },
                    onRetry = { viewModel.retryAttendance() },
                    onNoteChanged = { id, note -> viewModel.updateAttendanceNote(id, note) },
                    onNavigateToFullAttendance = {
                        showQuickAttendanceSheet = false
                        currentTab = MainTab.ATTENDANCE
                        currentSubScreen = SubScreen.NONE
                    }
                )
            }

            if (showQuickPaymentSheet) {
                QuickPaymentSelectionBottomSheet(
                    invoices = state.invoices,
                    onDismiss = { showQuickPaymentSheet = false },
                    onSelectInvoice = { invoice ->
                        showQuickPaymentSheet = false
                        viewModel.selectInvoiceForPayment(invoice)
                    },
                    onNavigateToFinance = {
                        showQuickPaymentSheet = false
                        currentTab = MainTab.FINANCE
                    }
                )
            }

            state.selectedInvoiceForPayment?.let { invoice ->
                RecordPaymentDialog(
                    invoice = invoice,
                    onDismiss = { viewModel.selectInvoiceForPayment(null) },
                    onConfirm = { method ->
                        viewModel.recordPayment(invoice.id, invoice.amount, method)
                    }
                )
            }

            selectedInvoiceForReceipt?.let { invoice ->
                ReceiptDialog(
                    invoice = invoice,
                    onDismiss = { selectedInvoiceForReceipt = null }
                )
            }

            if (state.showCreateAnnouncementDialog) {
                CreateAnnouncementDialog(
                    onDismiss = { viewModel.openCreateAnnouncement(false) },
                    onConfirm = { title, content, cat, tgt ->
                        viewModel.createAnnouncement(title, content, cat, tgt)
                    }
                )
            }

            state.selectedStudent?.let { student ->
                StudentDetailBottomSheet(
                    student = student,
                    onDismiss = { viewModel.selectStudent(null) },
                    onViewReportCard = { std -> viewModel.openReportCardViewer(std) },
                    onOpenProfile = { std -> viewModel.openStudentProfile(std) }
                )
            }

            state.selectedStudentForFullProfile?.let { student ->
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { viewModel.openStudentProfile(null) },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    com.example.ui.screens.StudentProfileScreen(
                        student = student,
                        onBack = { viewModel.openStudentProfile(null) },
                        onOpenReportCardViewer = { viewModel.openReportCardViewer(student) },
                        onPrintReportCard = {
                            com.example.util.ReportCardPrintHelper.printStudentReportCard(context, student)
                        }
                    )
                }
            }

            state.selectedAnnouncementForDetail?.let { announcement ->
                AnnouncementDetailDialog(
                    announcement = announcement,
                    onDismiss = { viewModel.selectAnnouncementDetail(null) }
                )
            }

            state.viewingReportCardForStudent?.let { student ->
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { viewModel.openReportCardViewer(null) },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        ReportCardViewer(
                            student = student,
                            onDismiss = { viewModel.openReportCardViewer(null) }
                        )
                    }
                }
            }
        }

            if (state.showFeedbackDialog) {
                FeedbackBottomSheet(
                    currentUser = state.currentUser,
                    selectedRole = state.selectedRole,
                    tenantId = state.tenantId,
                    onDismiss = { viewModel.openFeedbackDialog(false) }
                )
            }

            if (state.showThemeConfigDialog) {
                ThemeConfigDialog(
                    currentThemeName = state.selectedThemeName,
                    onThemeNameSelected = { viewModel.selectTheme(it) },
                    onDismiss = { viewModel.openThemeConfig(false) }
                )
            }

            if (state.showAiReportModal && state.activeAiReport != null) {
                com.example.ui.components.AiReportDetailModal(
                    report = state.activeAiReport!!,
                    onDismiss = { viewModel.dismissAiModals() }
                )
            }

            if (state.showAiHomeworkModal) {
                com.example.ui.components.AiHomeworkGeneratorModal(
                    onDismiss = { viewModel.dismissAiModals() },
                    onGenerate = { sub, top, gr, cnt, diff ->
                        viewModel.generateAiTeacherHomework(sub, top, gr, cnt, diff)
                    },
                    activeHomework = state.activeAiHomework
                )
            }

            if (state.showAiNarrativeModal) {
                com.example.ui.components.AiNarrativeGeneratorModal(
                    onDismiss = { viewModel.dismissAiModals() },
                    onGenerate = { name, meal, sleep, mood, act, note ->
                        viewModel.generateAiTeacherDailyNarrative(name, meal, sleep, mood, act, note)
                    },
                    activeNarrative = state.activeDailyNarrative
                )
            }

            if (state.showAiParentDigestModal && state.activeAiParentDigest != null) {
                com.example.ui.components.AiParentDigestModal(
                    digest = state.activeAiParentDigest!!,
                    onDismiss = { viewModel.dismissAiModals() }
                )
            }

            if (showGlobalSearchDialog) {
                GlobalSearchDialog(
                    students = state.students,
                    classes = state.classes,
                    onSelectStudent = { student ->
                        viewModel.selectStudent(student)
                        currentTab = MainTab.STUDENTS
                        currentSubScreen = SubScreen.NONE
                        showGlobalSearchDialog = false
                    },
                    onSelectClass = { classGroup ->
                        viewModel.selectClassDetail(classGroup)
                        currentSubScreen = SubScreen.CLASSES
                        showGlobalSearchDialog = false
                    },
                    onSelectReport = { reportItem ->
                        currentSubScreen = reportItem.subScreen
                        if (reportItem.mainTab != null) {
                            currentTab = reportItem.mainTab
                        }
                        showGlobalSearchDialog = false
                    },
                    onDismiss = { showGlobalSearchDialog = false }
                )
            }

            if (showChatbotDialog) {
                Okul360ChatbotModal(
                    state = state,
                    onDismiss = { showChatbotDialog = false },
                    onNavigateAction = { actionTag ->
                        showChatbotDialog = false
                        handleChatbotNavigation(actionTag)
                    }
                )
            }
        }
    }
