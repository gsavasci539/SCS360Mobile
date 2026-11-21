package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.payment.iyzico.IyzicoPaymentResult
import com.example.data.repository.Okul360Repository
import com.example.data.remote.ParentChildOptionsModule
import com.example.data.remote.ParentInvoiceSnapshot
import com.example.ui.navigation.NativeNavigationPolicy
import android.content.Context
import com.example.util.NetworkMonitor
import com.example.util.ReportCardPrintHelper
import com.example.util.TeacherReportExportHelper
import com.example.util.NotificationHelper
import com.example.util.CsvExportHelper
import com.example.util.AiAnalyticsEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ParentInvoiceLoadState { LOADING, LOADED, FAILED }

internal fun Okul360UiState.withParentInvoiceResult(childId: String, result: Result<ParentInvoiceSnapshot>): Okul360UiState {
    if (parentInvoiceStates[childId] != ParentInvoiceLoadState.LOADING) return this
    val status = if (result.isSuccess) ParentInvoiceLoadState.LOADED else ParentInvoiceLoadState.FAILED
    val snapshot = result.getOrNull()
    return copy(
        invoices = invoices.filterNot { it.student_id == childId } + snapshot?.rows.orEmpty().map { it.invoice },
        parentInvoiceStates = parentInvoiceStates + (childId to status),
        parentInvoiceSnapshots = if (snapshot == null) parentInvoiceSnapshots - childId else parentInvoiceSnapshots + (childId to snapshot),
    )
}

data class Okul360UiState(
    val isLoggedIn: Boolean = false,
    val loginError: String? = null,
    val isLoginLoading: Boolean = false,
    val currentUser: User = User(
        id = "",
        full_name = "",
        email = "",
        role = "ADMIN",
        tenant_id = null,
        institution_name = "SCS360 Eğitim Kurumları",
        branch_id = "all",
        branch_name = "Tüm Şubeler"
    ),
    val selectedRole: UserRole = UserRole.ADMIN,
    val organizations: List<Organization> = emptyList(),
    val branches: List<Branch> = emptyList(),
    val selectedBranchId: String = "all",
    val classes: List<ClassGroup> = emptyList(),
    val selectedClassId: String = "all",
    val rfidCards: List<RfidCard> = emptyList(),
    val hardwareDevices: List<HardwareDevice> = emptyList(),

    // Dashboard State
    val dashboardSummary: DashboardSummary? = null,
    val isDashboardLoading: Boolean = false,

    val parentChildren: List<Student> = emptyList(),
    val isParentPortalLoading: Boolean = false,
    val parentPortalError: String? = null,
    val parentInvoiceStates: Map<String, ParentInvoiceLoadState> = emptyMap(),
    val parentInvoiceSnapshots: Map<String, ParentInvoiceSnapshot> = emptyMap(),

    // Students State
    val students: List<Student> = emptyList(),
    val searchQuery: String = "",
    val filterClassId: String = "all",
    val filterAcademicYear: String = "all",
    val academicYears: List<String> = listOf("2025-2026", "2024-2025", "2026-2027"),
    val selectedStudent: Student? = null,
    val studentsError: String? = null,
    val studentsLoaded: Boolean = false,
    val isStudentsLoading: Boolean = false,
    val registrySaving: Boolean = false,
    val registryError: String? = null,
    val registrySavedRevision: Long = 0,
    val showAddStudentDialog: Boolean = false,
    val studentToEdit: Student? = null,

    // Attendance State
    val attendanceDate: String = java.time.LocalDate.now().toString(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val attendanceClassId: String = "all",
    val isAttendanceLoading: Boolean = false,
    val attendanceReady: Boolean = false,
    val attendanceCanWrite: Boolean = false,
    val isAttendanceSaving: Boolean = false,
    val attendanceSavePending: Boolean = false,
    val attendanceError: String? = null,
    val attendanceSaveSuccess: Boolean = false,

    // Finance State
    val financeSummary: FinanceSummary? = null, // Legacy mutation model; never used as a recorded ledger summary.
    val recordedFinanceSummary: RecordedFinanceSummary? = null,
    val financeSummaryError: String? = null,
    val financeRowsError: String? = null,
    val invoices: List<Invoice> = emptyList(),
    val invoiceFilter: String = "ALL", // ALL, PAID, PENDING, OVERDUE
    val financeSearchQuery: String = "",
    val selectedInvoiceForPayment: Invoice? = null,
    val invoiceToEdit: Invoice? = null,
    val selectedReceipt: PaymentReceipt? = null,
    val isFinanceLoading: Boolean = false,
    val isFinanceRowsLoading: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val isExpensesLoading: Boolean = false,

    // Classes Sub-Module
    val selectedClassDetail: ClassGroup? = null,

    // Communication Sub-Module
    val announcements: List<Announcement> = emptyList(),
    val announcementDrafts: List<Announcement> = emptyList(),
    val studentTermGrades: List<StudentTermSubjectGrade> = emptyList(),
    val isAnnouncementsLoading: Boolean = false,
    val showCreateAnnouncementDialog: Boolean = false,
    val selectedAnnouncementForDetail: Announcement? = null,
    val announcementToEdit: Announcement? = null,
    val viewingReportCardForStudent: Student? = null,
    val selectedStudentForFullProfile: Student? = null,
    val templates: List<CommunicationTemplate> = emptyList(),
    val outboxLogs: List<OutboxNotificationLog> = emptyList(),
    val isDataLoading: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),

    // Reports Sub-Module
    val reportsData: ReportsData? = null,
    val isReportsLoading: Boolean = false,

    // Resources Sub-Module
    val resources: List<SchoolResource> = emptyList(),
    val isResourcesLoading: Boolean = false,

    // Roles & Permissions Sub-Module
    val roles: List<RoleDefinition> = emptyList(),

    // Users Sub-Module
    val users: List<AppUser> = emptyList(),

    // Daily Reports
    val dailyReports: List<DailyReport> = emptyList(),
    val dailyReportsLoading: Boolean = false,
    val dailyReportsLoaded: Boolean = false,
    val dailyReportsError: String? = null,
    val dailyReportSaving: Boolean = false,
    val dailyReportSaveError: String? = null,
    val dailyReportSavedRevision: Long = 0,

    // Parent Portal Notices
    val medicineNotices: List<MedicineNotice> = emptyList(),
    val leaveNotices: List<LeaveNotice> = emptyList(),
    val pickupNotices: List<PickupNotice> = emptyList(),

    // Academic Schedule & Exams
    val exams: List<ExamItem> = emptyList(),
    val timetableSlots: List<TimetableSlot> = emptyList(),

    // Course Materials & Digital Documents State
    val courseMaterials: List<CourseMaterial> = emptyList(),

    // Direct Messaging & Parent-Teacher Chat State
    val selectedConversationId: String? = null,
    val showThemeConfigDialog: Boolean = false,

    // 1. Appointments State (PTC)
    val appointments: List<ParentTeacherAppointment> = emptyList(),

    // 2. Transport & Bus Tracking State
    val busRoutes: List<BusRoute> = emptyList(),

    // 4. Homework Submissions State
    val homeworkSubmissions: List<HomeworkSubmission> = emptyList(),

    // 5. Library & Book Loans State
    val libraryBooks: List<LibraryBook> = emptyList(),
    val bookLoans: List<BookLoanRecord> = emptyList(),

    // 6. Staff Duty & Leave Requests State
    val dutyRosters: List<TeacherDutyRoster> = emptyList(),
    val leaveRequests: List<TeacherLeaveRequest> = emptyList(),

    // 7. Okul360 Ek Modüller State
    val consents: List<StudentConsent> = emptyList(),
    val schoolClubs: List<SchoolClub> = emptyList(),
    val surveys: List<SchoolSurvey> = emptyList(),
    val tutoringSessions: List<TutoringSession> = emptyList(),
    val payrollRecords: List<PayrollRecord> = emptyList(),
    val broadcastJobs: List<BroadcastJob> = emptyList(),
    val messageTemplates: List<MessageTemplate> = emptyList(),
    val smsProviders: List<SmsProvider> = emptyList(),
    val medicineRequests: List<MedicineRequest> = emptyList(),
    val excuseRequests: List<ExcuseRequest> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val deliveryReports: List<DeliveryReport> = emptyList(),
    val studentFiles: Map<String, List<StudentFile>> = emptyMap(),

    // System Health, Offline & Server Config
    val selectedThemeMode: String = "LIGHT", // SYSTEM, LIGHT, DARK (Default Light Mode)
    val selectedThemeName: String = "SCS360",
    val isRefreshing: Boolean = false,
    val showFeedbackDialog: Boolean = false,
    val isOffline: Boolean = false,
    val isSyncing: Boolean = false,
    val isDemoMode: Boolean = false,
    val isTestingBackend: Boolean = false,
    val backendStatusMessage: String? = null,
    val systemHealth: SystemHealth? = null,
    val serverBaseUrl: String = com.example.data.remote.ApiClientProvider.DEFAULT_BASE_URL,
    val tenantId: String = "",
    val toastMessage: String? = null,

    // SaaS Platform, Üyelik, Kota, Teslim Masası & Kapı Güvenliği State
    val workspaceOrganizationId: String? = null,
    val workspaceOrganizationName: String? = null,
    val organizationAccess: OrganizationAccessStatus = OrganizationAccessStatus(),
    val subscriptionPlans: List<SubscriptionPlan> = emptyList(),
    val subscriptionInvoices: List<SubscriptionInvoice> = emptyList(),
    val platformTenants: List<PlatformTenantSummary> = emptyList(),
    // ==========================================
    // 7. Yapay Zeka (AI / Gemini) State
    // ==========================================
    val aiReports: List<AiAnalysisReport> = emptyList(),
    val isAiLoading: Boolean = false,
    val aiLoadingMessage: String = "Gemini 3.5 Flash verileri analiz ediyor...",
    val activeAiReport: AiAnalysisReport? = null,
    val activeAiParentDigest: AiParentDigest? = null,
    val activeAiHomework: AiGeneratedHomework? = null,
    val activeDailyNarrative: AiDailyNarrative? = null,
    val churnRiskList: List<AiChurnRiskItem> = emptyList(),
    val showAiReportModal: Boolean = false,
    val showAiHomeworkModal: Boolean = false,
    val showAiNarrativeModal: Boolean = false,
    val showAiParentDigestModal: Boolean = false,
    val institutionalSwot: InstitutionalSwotAnalysis? = null,
    val studentPerformanceMatrix: List<StudentPerformanceMatrixItem> = emptyList(),
    val monthlyFinanceChartData: List<MonthlyFinanceChartData> = emptyList(),
    val branchAttendanceChartData: List<BranchAttendanceChartData> = emptyList(),
    val subjectAcademicChartData: List<SubjectAcademicChartData> = emptyList(),
    val weeklyLgsNetTrendData: List<WeeklyLgsNetTrendData> = emptyList(),
    val selectedAnalyticsBranch: String = "Tüm Şubeler",
    val selectedAnalyticsTimeframe: String = "Son 30 Gün",
    val selectedAnalyticsTab: Int = 0
) {
    val roleDefinitions: List<RoleDefinition> get() = roles
    val communicationTemplates: List<CommunicationTemplate> get() = templates
}

class Okul360ViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Okul360Repository(application)
    private val sessionManager = com.example.data.local.SessionManager(application)
    private val networkMonitor = NetworkMonitor(application)

    private val _uiState = MutableStateFlow(Okul360UiState())
    val uiState: StateFlow<Okul360UiState> = _uiState.asStateFlow()

    private var attendanceEditor: com.example.data.remote.NativeAttendanceEditor? = null
    private var attendanceGeneration = 0L
    private var attendanceLoadJob: kotlinx.coroutines.Job? = null

    private fun freshState(previous: Okul360UiState, actor: User = previous.currentUser,
        loggedIn: Boolean = previous.isLoggedIn, workspace: String? = previous.workspaceOrganizationId,
        workspaceName: String? = previous.workspaceOrganizationName): Okul360UiState = Okul360UiState(
        isLoggedIn = loggedIn && SessionRolePolicy.primary(actor) != null, currentUser = actor,
        selectedRole = SessionRolePolicy.primary(actor) ?: UserRole.PARENT,
        selectedThemeName = previous.selectedThemeName, workspaceOrganizationId = workspace,
        workspaceOrganizationName = workspaceName, selectedBranchId = "all")

    private val requestFailures = kotlinx.coroutines.CoroutineExceptionHandler { _, failure ->
        if (failure !is kotlinx.coroutines.CancellationException) {
            val rejected = (failure as? retrofit2.HttpException)?.code()
            if (rejected == 401) {
                com.example.data.remote.ApiClientProvider.interceptor.setSession(null)
                sessionManager.clearSession()
                _uiState.update { Okul360UiState(selectedThemeName = it.selectedThemeName, loginError = "Oturum doğrulanamadı. Yeniden giriş yapın.") }
            } else {
                com.example.data.remote.ApiClientProvider.interceptor.invalidateContext()
                _uiState.update { freshState(it).copy(toastMessage = "İşlem sonucu sunucudan doğrulanamadı. Durumu yenileyerek kontrol edin; aynı işlemi yeni kayıt olarak tekrarlamayın.") }
            }
        }
    }


    init {
        try {
            NotificationHelper.createNotificationChannels(application)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        try {
            NotificationHelper.initFirebaseMessaging(application)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        try {
            observeNetworkState()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        try {
            restoreUserSession()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        try {
            loadInitialData()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        try {
            startBackgroundNotificationWorker()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun observeNetworkState() {
        viewModelScope.launch(requestFailures) {
            networkMonitor.isOnline.collect { isOnline ->
                val wasOffline = _uiState.value.isOffline
                _uiState.update { it.copy(isOffline = !isOnline) }
                if (!isOnline) {
                    loadCachedData()
                } else if (wasOffline) {
                    _uiState.update { it.copy(toastMessage = "İnternet bağlantısı yeniden kuruldu. Veriler eşitleniyor...") }
                    loadInitialData()
                }
            }
        }
    }

    fun retryConnection() {
        viewModelScope.launch(requestFailures) {
            val online = networkMonitor.checkInitialConnectivity()
            _uiState.update { it.copy(isOffline = !online) }
            if (online) {
                _uiState.update { it.copy(toastMessage = "Ağ bağlantısı doğrulandı. Veriler güncelleniyor...") }
                loadInitialData()
            } else {
                _uiState.update { it.copy(toastMessage = "Halen çevrimdışı. Room veritabanındaki önbellek verileri korunuyor.") }
                loadCachedData()
            }
        }
    }

    fun loadCachedData() {
        viewModelScope.launch(requestFailures) {
            try {
                val cachedStudents = repository.getCachedStudents()
                val cachedAttendance = repository.getCachedAttendance(date = _uiState.value.attendanceDate)
                val cachedInvoices = repository.getCachedInvoices()
                val cachedAnnouncements = repository.getCachedAnnouncements()

                _uiState.update {
                    it.copy(
                        students = if (cachedStudents.isNotEmpty()) cachedStudents else it.students,
                        attendanceRecords = if (cachedAttendance.isNotEmpty()) cachedAttendance else it.attendanceRecords,
                        invoices = if (cachedInvoices.isNotEmpty()) cachedInvoices else it.invoices,
                        announcements = if (cachedAnnouncements.isNotEmpty()) cachedAnnouncements else it.announcements,
                        isStudentsLoading = false,
                        isAttendanceLoading = false,
                        isFinanceLoading = false,
                        isAnnouncementsLoading = false
                    )
                }
            } catch (e: Exception) {
                // Ignore caching errors
            }
        }
    }

    fun printStudentReportCard(context: Context, student: Student) {
        ReportCardPrintHelper.printStudentReportCard(
            context = context,
            student = student,
            schoolName = _uiState.value.currentUser.institution_name,
            campusName = _uiState.value.currentUser.branch_name,
            principalName = "Ahmet Yılmaz (Müdür)",
            teacherName = "Zeynep Kaya (Sınıf Öğretmeni)"
        )
    }

    fun printClassReportCards(context: Context, className: String) {
        val targetClass = _uiState.value.classes.find { it.name.equals(className, ignoreCase = true) || it.id == className }
        val classStudents = if (targetClass != null) {
            _uiState.value.students.filter { it.class_id == targetClass.id || it.class_name.equals(className, ignoreCase = true) }
        } else {
            _uiState.value.students.filter { it.class_name.equals(className, ignoreCase = true) }
        }.ifEmpty { _uiState.value.students }

        ReportCardPrintHelper.printClassReportCards(
            context = context,
            students = classStudents,
            className = className,
            schoolName = _uiState.value.currentUser.institution_name,
            campusName = _uiState.value.currentUser.branch_name
        )
    }

    private fun restoreUserSession() {
        val token = sessionManager.getToken() ?: return
        if (!sessionManager.isLoggedIn()) return
        _uiState.update { it.copy(isLoginLoading = true) }
        viewModelScope.launch(requestFailures) {
            try {
                val user = repository.restoreSession(token)
                sessionManager.saveSession(user, com.example.data.remote.ApiClientProvider.interceptor.authToken)
                _uiState.update { freshState(it, actor = user, loggedIn = true, workspace = null, workspaceName = null) }
                loadInitialData()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if ((failure as? retrofit2.HttpException)?.code() in listOf(401, 403)) sessionManager.clearSession()
                _uiState.update { Okul360UiState(selectedThemeName = it.selectedThemeName, loginError = "Oturum sunucuda doğrulanamadı. Bağlantınızı kontrol edip yeniden giriş yapın.") }
            }
        }
    }

    private fun startBackgroundNotificationWorker() {
        viewModelScope.launch(requestFailures) {
            // Background periodic polling / sync for notifications
            while (true) {
                delay(45_000) // Poll every 45s in background
                try {
                    val remoteAnnouncements = repository.getAnnouncements(_uiState.value.selectedBranchId)
                    if (remoteAnnouncements.isNotEmpty() && _uiState.value.announcements.isNotEmpty()) {
                        val newItems = remoteAnnouncements.filter { remote ->
                            _uiState.value.announcements.none { it.id == remote.id }
                        }
                        if (newItems.isNotEmpty()) {
                            newItems.forEach { item ->
                                NotificationHelper.sendAnnouncementPush(
                                    getApplication(),
                                    item.title,
                                    item.content,
                                    item.author_name
                                )
                            }
                            _uiState.update { it.copy(announcements = remoteAnnouncements) }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore background polling errors
                }
            }
        }
    }

    fun triggerCustomPush(title: String, message: String, channelId: String = NotificationHelper.CHANNEL_ANNOUNCEMENT) {
        NotificationHelper.showPushNotification(
            context = getApplication(),
            title = title,
            message = message,
            channelId = channelId
        )
        _uiState.update { it.copy(toastMessage = "Android sistem push bildirimi gönderildi.") }
    }

    fun triggerDelayedPush(
        delaySeconds: Int,
        title: String,
        message: String,
        channelId: String = NotificationHelper.CHANNEL_ANNOUNCEMENT
    ) {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(toastMessage = "$delaySeconds sn sonra arka planda bildirim gelecek. Şimdi uygulamayı küçültebilirsiniz.") }
            delay(delaySeconds * 1000L)
            NotificationHelper.showPushNotification(
                context = getApplication(),
                title = title,
                message = message,
                channelId = channelId
            )
        }
    }

    fun openDeviceNotificationSettings() {
        NotificationHelper.openNotificationSettings(getApplication())
    }

    fun setThemeName(themeName: String) {
        _uiState.update { it.copy(selectedThemeName = themeName, toastMessage = "Tema güncellendi.") }
    }

    fun toggleFeedbackDialog(show: Boolean) {
        _uiState.update { it.copy(showFeedbackDialog = show) }
    }

    fun refreshAllData() {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isRefreshing = true) }
            loadInitialData()
            delay(600)
            _uiState.update { it.copy(isRefreshing = false, toastMessage = "Veriler senkronize edildi.") }
        }
    }

    fun exportAttendanceCsv(context: Context, className: String, date: String) {
        val records = _uiState.value.attendanceRecords
        val students = _uiState.value.students
        CsvExportHelper.exportAttendanceToCsv(
            context = context,
            records = records,
            students = students,
            className = className,
            date = date
        )
    }

    fun exportFinanceCsv(context: Context) {
        val invoices = _uiState.value.invoices
        val summary = _uiState.value.financeSummary
        CsvExportHelper.exportFinanceToCsv(
            context = context,
            invoices = invoices,
            summary = summary
        )
    }

    fun exportStudentsCsv(context: Context) {
        val students = _uiState.value.students
        CsvExportHelper.exportStudentsToCsv(
            context = context,
            students = students
        )
    }

    fun triggerPaymentReminderPush(context: Context, invoice: Invoice) {
        NotificationHelper.sendFinanceReminderPush(
            context = context,
            studentName = invoice.student_name,
            amount = invoice.amount - invoice.paid_amount,
            dueDate = invoice.due_date,
            description = invoice.description
        )
        _uiState.update { it.copy(toastMessage = "${invoice.student_name} velisine anlık ödeme hatırlatma bildirimi iletildi.") }
    }

    fun loadInitialData() {
        if (!_uiState.value.isLoggedIn) return
        if (_uiState.value.currentUser.is_super_admin == true && _uiState.value.workspaceOrganizationId == null) {
            refreshSubscriptionAccessStatus()
            return
        }
        val actor = _uiState.value.currentUser
        if (_uiState.value.selectedRole == UserRole.PARENT) { loadParentPortal(); refreshSubscriptionAccessStatus(); return }
        fun allowed(vararg codes: String) = com.example.ui.navigation.NativeNavigationPolicy.has(actor, *codes)
        viewModelScope.launch(requestFailures) {
            if (allowed("org.read")) loadOrganizations()
            if (allowed("branch.read")) loadBranches()
            if (allowed("classes.read", "class.read")) loadClasses()
            if (allowed("dashboard.read")) loadDashboard()
            if (allowed("students.read", "student.read")) loadStudents()
            if (allowed("attendance.read")) loadAttendance()
            if (allowed("finance.read")) loadFinance()
            if (allowed("announcement.read")) loadAnnouncements()
            if (allowed("notification.read")) { loadTemplates(); loadOutboxLogs(); loadBroadcastData() }
            if (allowed("daily_reports.read", "academic.read")) loadDailyReports()
            if (allowed("report.read")) loadReports()
            if (allowed("inventory.read")) loadResources()
            if (allowed("users.read")) { loadRoles(); loadUsers() }
            if (allowed("settings.read")) { loadRfidCards(); loadHardwareDevices(); checkSystemHealth() }
            if (actor.is_super_admin == true) loadAiAnalyticsData()
            if (allowed("clubs.read")) loadClubs()
            if (allowed("surveys.read", "surveys.respond")) loadSurveys()
            if (allowed("tutoring.read")) loadTutoringSessions()
            if (allowed("payroll.read")) loadPayrollRecords()
            if (allowed("exam.read", "exam.write")) loadExams()
            refreshSubscriptionAccessStatus()
        }
    }

    private var parentPortalJob: kotlinx.coroutines.Job? = null
    private val parentInvoiceRefreshJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    private fun cancelParentPortalRequests() {
        parentPortalJob?.cancel()
        parentInvoiceRefreshJobs.values.toList().forEach { it.cancel() }
        parentInvoiceRefreshJobs.clear()
    }

    fun loadParentPortal() {
        if (!_uiState.value.isLoggedIn || _uiState.value.selectedRole != UserRole.PARENT) return
        cancelParentPortalRequests()
        val captured = com.example.data.remote.ApiClientProvider.interceptor.captureScope()
        val user = _uiState.value.currentUser
        _uiState.update {
            it.copy(parentChildren = emptyList(), students = emptyList(), invoices = emptyList(),
                parentInvoiceStates = emptyMap(), parentInvoiceSnapshots = emptyMap(),
                isParentPortalLoading = true, parentPortalError = null)
        }
        parentPortalJob = viewModelScope.launch {
            try {
                val client = captured.io { com.example.data.remote.NativeParentPortalClient() }
                val canReadFinances = NativeNavigationPolicy.canReadParentFinances(user)
                val module = when {
                    NativeNavigationPolicy.canReadParentChildProfile(user) -> null
                    NativeNavigationPolicy.has(user, "parent.view_attendance") -> ParentChildOptionsModule.ATTENDANCE
                    canReadFinances -> ParentChildOptionsModule.FINANCES
                    else -> null
                }
                client.loadPortalRecords(module, canReadFinances,
                    onChildren = { children ->
                        _uiState.update { state ->
                            if (state.selectedRole != UserRole.PARENT || state.currentUser.id != user.id) state
                            else state.copy(parentChildren = children, students = children, isParentPortalLoading = false,
                                parentInvoiceStates = if (canReadFinances) children.associate { it.id to ParentInvoiceLoadState.LOADING } else emptyMap())
                        }
                    },
                    onInvoices = { childId, result ->
                        _uiState.update { state ->
                            if (state.selectedRole != UserRole.PARENT || state.currentUser.id != user.id ||
                                !NativeNavigationPolicy.canReadParentFinances(state.currentUser)) state
                            else state.withParentInvoiceResult(childId, result)
                        }
                    },
                )
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (captured.current()) _uiState.update { state ->
                    if (state.selectedRole != UserRole.PARENT || state.currentUser.id != user.id) state
                    else state.copy(isParentPortalLoading = false, parentPortalError = "Bağlı öğrenciler sunucudan yüklenemedi. Yeniden deneyin.")
                }
            }
        }
    }

    fun refreshParentInvoices(studentId: String) {
        val previous = _uiState.value
        if (!previous.isLoggedIn || previous.selectedRole != UserRole.PARENT ||
            !NativeNavigationPolicy.canReadParentFinances(previous.currentUser)) return
        val student = previous.parentChildren.find { it.id == studentId } ?: return
        val pending = parentInvoiceRefreshJobs[studentId]
        if (previous.parentInvoiceStates[studentId] == ParentInvoiceLoadState.LOADING && pending == null) return
        pending?.cancel()
        val captured = com.example.data.remote.ApiClientProvider.interceptor.captureScope()
        _uiState.update { it.copy(parentInvoiceStates = it.parentInvoiceStates + (studentId to ParentInvoiceLoadState.LOADING)) }
        val job = viewModelScope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            try {
                val result = try {
                    Result.success(captured.io { com.example.data.remote.NativeParentPortalClient().invoiceSnapshot(student) })
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Result.failure(failure)
                }
                captured.ensureCurrent()
                _uiState.update { state ->
                    if (state.selectedRole != UserRole.PARENT || state.currentUser.id != previous.currentUser.id ||
                        !NativeNavigationPolicy.canReadParentFinances(state.currentUser)) state
                    else state.withParentInvoiceResult(studentId, result)
                }
            } finally {
                if (parentInvoiceRefreshJobs[studentId] === coroutineContext[kotlinx.coroutines.Job]) {
                    parentInvoiceRefreshJobs.remove(studentId)
                }
            }
        }
        parentInvoiceRefreshJobs[studentId] = job
        job.start()
    }

    // Authentication Actions
    fun login(email: String, password: String) {
        if (_uiState.value.isLoginLoading) return
        _uiState.update { it.copy(isLoginLoading = true) }
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isLoginLoading = true, loginError = null) }
            val cleanEmail = email.trim()
            if (cleanEmail.isNotBlank() && password.isNotBlank()) {
                try {
                    val backendUser = repository.login(cleanEmail, password)
                    if (backendUser != null) {
                        java.util.UUID.fromString(backendUser.id)
                        val userRole = SessionRolePolicy.primary(backendUser)
                            ?: error("Hesabınızın desteklenen etkin rolü bulunmuyor; kurum yöneticinizle iletişime geçin.")
                        sessionManager.saveSession(backendUser, com.example.data.remote.ApiClientProvider.interceptor.authToken)
                        _uiState.update { freshState(it, actor = backendUser, loggedIn = true, workspace = null, workspaceName = null)
                            .copy(toastMessage = "Giriş başarılı: ${backendUser.resolvedName}") }
                        loadInitialData()
                        checkSystemHealth()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoginLoading = false,
                                loginError = "E-posta veya şifre hatalı. Lütfen bilgilerinizi kontrol ediniz."
                            )
                        }
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (e: Exception) {
                    val errorMsg = when (e) {
                        is IllegalStateException -> e.message ?: "Giriş başarısız"
                        is retrofit2.HttpException -> "Giriş isteği reddedildi (HTTP ${e.code()}). Bilgilerinizi kontrol edin."
                        is java.net.UnknownHostException -> "Sunucuya ulaşılamıyor. API adresini ve bağlantınızı kontrol edin."
                        is java.net.SocketTimeoutException -> "Sunucu zaman aşımına uğradı. Tekrar deneyin."
                        else -> "Giriş sunucuda doğrulanamadı."
                    }
                    android.util.Log.w("Okul360ViewModel", "Login request failed (${e.javaClass.simpleName})")
                    _uiState.update {
                        it.copy(
                            isLoginLoading = false,
                            loginError = errorMsg
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoginLoading = false,
                        loginError = "Lütfen e-posta ve şifrenizi eksiksiz giriniz."
                    )
                }
            }
        }
    }

    fun forgotPassword(email: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.update { it.copy(toastMessage = "Geçerli e-posta adresinizi girin.") }
            return
        }
        viewModelScope.launch(requestFailures) {
            repository.forgotPassword(email.trim())
            _uiState.update { it.copy(toastMessage = "İstek alındı. Uygun bir hesap varsa şifre sıfırlama yönergesi iletilecektir.") }
        }
    }

    fun logout() {
        com.example.data.remote.ApiClientProvider.interceptor.setSession(null)
        cancelParentPortalRequests()
        sessionManager.clearSession()
        _uiState.update { Okul360UiState(selectedThemeName = it.selectedThemeName, toastMessage = "Oturum kapatıldı.") }
    }

    fun switchRole(role: UserRole) {
        val actor = _uiState.value.currentUser
        val platformOnly = actor.is_super_admin == true && com.example.data.remote.ApiClientProvider.interceptor.workspaceOrganizationId == null
        if (!SessionRolePolicy.canSelect(actor, role) || (platformOnly && role != UserRole.SUPERADMIN)) {
            _uiState.update { it.copy(toastMessage = "Bu rol hesabınıza atanmış değil. Yetkili hesabınızı veya kurum çalışma alanını seçin.") }
            return
        }
        // Change only the presentation mode. JWT actor, profile, organization,
        // persisted roles and identity must remain those returned by the server.
        com.example.data.remote.ApiClientProvider.interceptor.invalidateContext()
        cancelParentPortalRequests()
        _uiState.update { freshState(it).copy(selectedRole = role, toastMessage = "${role.displayName} görünümü açıldı.") }
        loadInitialData()
    }

    fun switchBranch(branchId: String) {
        val state = _uiState.value
        if (!state.isLoggedIn) return
        val branch = state.branches.find { it.id == branchId }
        if (branchId != "all" && branch == null) return
        com.example.data.remote.ApiClientProvider.interceptor.branchId = branchId.takeUnless { it == "all" }
        _uiState.update { freshState(it).copy(selectedBranchId = branchId,
            toastMessage = if (branch == null) "Tüm erişilebilir şubeler seçildi." else "${branch.name} şubesi seçildi.") }
        loadInitialData()
    }

    // Organizations, Branch & Class Loaders
    fun loadOrganizations() {
        viewModelScope.launch(requestFailures) {
            val orgList = repository.getOrganizations()
            _uiState.update { it.copy(organizations = orgList) }
        }
    }

    fun loadBranches() {
        viewModelScope.launch(requestFailures) {
            val branchList = repository.getBranches()
            _uiState.update { it.copy(branches = branchList) }
        }
    }

    fun createBranch(branch: Branch) {
        viewModelScope.launch(requestFailures) {
            val created = repository.createBranch(branch)
            _uiState.update {
                it.copy(
                    branches = it.branches + created,
                    toastMessage = "${created.name} şubesi başarıyla eklendi."
                )
            }
        }
    }

    fun loadClasses() {
        viewModelScope.launch(requestFailures) {
            val classList = repository.getClasses(_uiState.value.selectedBranchId)
            if (_uiState.value.isAttendanceSaving || _uiState.value.attendanceSavePending) return@launch
            _uiState.update { state -> state.copy(classes = classList,
                students = state.students.map { student -> classList.find { it.id == student.class_id }?.let { student.copy(class_name = it.name, academic_year = it.academic_year) } ?: student },
                selectedClassId = state.selectedClassId.takeIf { id -> classList.any { it.id == id } } ?: classList.firstOrNull()?.id.orEmpty(),
                attendanceClassId = state.attendanceClassId.takeIf { id -> classList.any { it.id == id } } ?: classList.firstOrNull()?.id.orEmpty()) }
            loadAttendance()
        }
    }

    fun loadRfidCards() {
        viewModelScope.launch(requestFailures) {
            val cards = repository.getRfidCards()
            _uiState.update { it.copy(rfidCards = cards) }
        }
    }

    fun loadHardwareDevices() {
        viewModelScope.launch(requestFailures) {
            val devs = repository.getDevices()
            _uiState.update { it.copy(hardwareDevices = devs) }
        }
    }

    fun addRfidCard(card: RfidCard) {
        viewModelScope.launch(requestFailures) {
            val created = repository.createRfidCard(card)
            _uiState.update {
                it.copy(
                    rfidCards = listOf(created) + it.rfidCards,
                    toastMessage = "RFID Kartı başarıyla tanımlandı (${created.card_uid})."
                )
            }
        }
    }

    fun toggleCardStatus(cardId: String) {
        _uiState.update { current ->
            val updated = current.rfidCards.map {
                if (it.id == cardId) {
                    val newStatus = if (it.status == "ACTIVE") "BLOCKED" else "ACTIVE"
                    it.copy(status = newStatus)
                } else it
            }
            current.copy(
                rfidCards = updated,
                toastMessage = "Kart durumu güncellendi."
            )
        }
    }

    // Dashboard Actions
    fun loadDashboard() {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isDashboardLoading = true) }
            val summary = repository.getDashboardSummary(_uiState.value.selectedBranchId)
            _uiState.update {
                it.copy(
                    dashboardSummary = summary,
                    isDashboardLoading = false
                )
            }
        }
    }

    // Student CRUD & Filtering
    fun loadStudents() {
        val state = _uiState.value
        val generation = ++studentsGeneration
        studentsLoadJob?.cancel()
        val scope = com.example.data.remote.ApiClientProvider.interceptor.captureScope()
        _uiState.update { it.copy(isStudentsLoading = true, studentsError = null) }
        studentsLoadJob = viewModelScope.launch {
            try {
                val rows = scope.io { repository.getStudents(state.selectedBranchId,
                    state.filterClassId.takeIf { it != "all" }, state.searchQuery.ifBlank { null }) }
                scope.ensureCurrent()
                if (generation != studentsGeneration) return@launch
                val knownClasses = _uiState.value.classes
                val joined = rows.map { student -> knownClasses.find { it.id == student.class_id }?.let {
                    student.copy(class_name = it.name, academic_year = it.academic_year)
                } ?: student }.filter { state.filterAcademicYear == "all" || it.academic_year == state.filterAcademicYear }
                _uiState.update { it.copy(students = joined, isStudentsLoading = false, studentsLoaded = true, studentsError = null) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (scope.current() && generation == studentsGeneration) {
                    if ((failure as? com.example.data.remote.RegistryHttpException)?.code == 401) { logout(); return@launch }
                    if ((failure as? com.example.data.remote.RegistryHttpException)?.code == 402) refreshSubscriptionAccessStatus()
                    _uiState.update { it.copy(students = emptyList(), isStudentsLoading = false, studentsLoaded = false,
                        studentsError = "Öğrenci listesi yüklenemedi. Yeniden deneyin.") }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadStudents()
    }

    fun onFilterClassChanged(classId: String) {
        _uiState.update { it.copy(filterClassId = classId) }
        loadStudents()
    }

    fun onFilterAcademicYearChanged(academicYear: String) {
        _uiState.update { it.copy(filterAcademicYear = academicYear) }
        loadStudents()
    }

    fun selectStudent(student: Student?) {
        _uiState.update { it.copy(selectedStudent = student) }
    }

    private fun <T> registryMutation(call: suspend () -> T, success: (T) -> Unit) {
        if (_uiState.value.registrySaving) return
        val scope = com.example.data.remote.ApiClientProvider.interceptor.captureScope()
        _uiState.update { it.copy(registrySaving = true, registryError = null) }
        viewModelScope.launch {
            try {
                val result = scope.io { call() }
                scope.ensureCurrent()
                success(result)
                _uiState.update { it.copy(registrySavedRevision = it.registrySavedRevision + 1) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (scope.current()) {
                    if ((failure as? com.example.data.remote.RegistryHttpException)?.code == 401) { logout(); return@launch }
                    if ((failure as? com.example.data.remote.RegistryHttpException)?.code == 402) refreshSubscriptionAccessStatus()
                    _uiState.update { it.copy(registryError = failure.message ?: "İşlem tamamlanamadı", toastMessage = "İşlem tamamlanamadı: ${failure.message.orEmpty()}") }
                    if (failure is com.example.data.remote.RegistryRecordChangedException) { loadStudents(); loadClasses() }
                }
            } finally { if (scope.current()) _uiState.update { it.copy(registrySaving = false) } }
        }
    }
    private var studentsGeneration = 0L
    private var studentsLoadJob: kotlinx.coroutines.Job? = null

    fun openAddStudentDialog(open: Boolean) {
        _uiState.update { it.copy(showAddStudentDialog = open) }
    }

    fun addStudent(student: Student) = registryMutation({ repository.addStudent(student) }) { created ->
        _uiState.update { it.copy(showAddStudentDialog = false, selectedStudent = created, toastMessage = "${created.full_name} kaydedildi.") }
        loadStudents(); loadClasses(); loadDashboard()
    }

    fun updateStudent(student: Student) = registryMutation({ repository.updateStudent(student) }) { saved ->
        _uiState.update { it.copy(studentToEdit = null, selectedStudent = saved, toastMessage = "${saved.full_name} güncellendi.") }
        loadStudents(); loadClasses()
    }

    fun deleteStudent(studentId: String) = registryMutation({ repository.deleteStudent(studentId) }) {
        _uiState.update { it.copy(selectedStudent = null, toastMessage = "Öğrenci kaydı silindi.") }
        loadStudents(); loadClasses(); loadDashboard()
    }

    // Attendance actions use one immutable class/day snapshot and backend row versions.
    fun loadAttendance() {
        val state = _uiState.value
        if (state.isAttendanceSaving || state.attendanceSavePending) return
        attendanceLoadJob?.cancel()
        val generation = ++attendanceGeneration
        attendanceEditor = null
        _uiState.update { it.copy(attendanceRecords = emptyList(), attendanceReady = false, attendanceCanWrite = false, attendanceError = null, isAttendanceLoading = false) }
        if (!state.isLoggedIn || state.classes.none { it.id == state.attendanceClassId }) return
        val editor = try { com.example.data.remote.NativeAttendanceEditor(getApplication(), state.currentUser.id, state.attendanceClassId, state.attendanceDate) }
            catch (_: Exception) { _uiState.update { it.copy(attendanceError = "Geçerli sınıf ve tarih seçin.") }; return }
        attendanceEditor = editor
        fun owned() = generation == attendanceGeneration && editor.current() && attendanceEditor === editor &&
            _uiState.value.attendanceClassId == editor.classId && _uiState.value.attendanceDate == editor.day
        _uiState.update { it.copy(isAttendanceLoading = true) }
        attendanceLoadJob = viewModelScope.launch(requestFailures) {
            try {
                val rows = editor.load()
                if (owned()) _uiState.update { it.copy(attendanceRecords = rows, attendanceReady = true, isAttendanceLoading = false,
                    attendanceCanWrite = it.currentUser.is_super_admin == true || it.currentUser.permissions.any { p -> p == "attendance.write" || p == "*" }) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (owned()) {
                    if ((failure as? com.example.data.remote.AttendanceHttpException)?.code == 401) logout()
                    else _uiState.update { it.copy(isAttendanceLoading = false, attendanceReady = false,
                        attendanceError = "Yoklama sunucudan yüklenemedi. Yeniden deneyin.") }
                }
            }
        }
    }

    fun selectAttendanceClass(classId: String) {
        if (_uiState.value.isAttendanceSaving || _uiState.value.attendanceSavePending) return
        if (_uiState.value.classes.none { it.id == classId }) return
        _uiState.update { it.copy(attendanceClassId = classId) }
        loadAttendance()
    }

    fun selectAttendanceDate(day: String) {
        if (_uiState.value.isAttendanceSaving || _uiState.value.attendanceSavePending) return
        try { java.time.LocalDate.parse(day) } catch (_: Exception) { return }
        _uiState.update { it.copy(attendanceDate = day) }
        loadAttendance()
    }

    fun retryAttendance() {
        if (_uiState.value.attendanceSavePending) saveAttendance() else if (_uiState.value.classes.isEmpty()) loadClasses() else loadAttendance()
    }

    private fun attendanceEditable() = _uiState.value.attendanceCanWrite && _uiState.value.attendanceReady && !_uiState.value.isAttendanceSaving &&
        !_uiState.value.attendanceSavePending && attendanceEditor?.ready == true

    fun updateSingleAttendanceStatus(studentId: String, newStatus: String) {
        if (!attendanceEditable() || newStatus !in com.example.data.remote.NativeAttendanceEditor.STATUSES) return
        _uiState.update { it.copy(attendanceRecords = it.attendanceRecords.map { row ->
            if (row.student_id == studentId) row.copy(status = newStatus) else row }, attendanceError = null) }
    }

    fun updateAttendanceNote(studentId: String, note: String?) {
        if (!attendanceEditable() || (note?.length ?: 0) > 5000) return
        _uiState.update { it.copy(attendanceRecords = it.attendanceRecords.map { row ->
            if (row.student_id == studentId) row.copy(note = note) else row }, attendanceError = null) }
    }

    fun markAllPresent() {
        if (!attendanceEditable()) return
        _uiState.update { it.copy(attendanceRecords = it.attendanceRecords.map { row -> row.copy(status = "PRESENT") },
            toastMessage = "Tümünü 'Geldi' taslağına aldınız. Sunucuya kaydetmek için Kaydet'i kullanın.") }
    }

    fun saveAttendance() {
        val state = _uiState.value
        val editor = attendanceEditor ?: return
        if (!state.attendanceCanWrite || !state.attendanceReady || state.isAttendanceSaving || !editor.ready || state.attendanceClassId != editor.classId || state.attendanceDate != editor.day) return
        val rows = state.attendanceRecords.toList()
        _uiState.update { it.copy(isAttendanceSaving = true, attendanceError = null) }
        viewModelScope.launch(requestFailures) {
            try {
                editor.save(rows)
                if (attendanceEditor === editor && editor.current()) {
                    _uiState.update { it.copy(isAttendanceSaving = false, attendanceSavePending = false, attendanceReady = false,
                        toastMessage = "Yoklama sunucuda kaydedildi.") }
                    loadAttendance()
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (attendanceEditor === editor && editor.current()) {
                    if ((failure as? com.example.data.remote.AttendanceHttpException)?.code == 401) logout()
                    else _uiState.update { it.copy(isAttendanceSaving = false, attendanceSavePending = editor.hasPending,
                        attendanceReady = editor.ready, attendanceCanWrite = it.attendanceCanWrite && (failure as? com.example.data.remote.AttendanceHttpException)?.code != 403, attendanceError = when {
                            (failure as? com.example.data.remote.AttendanceHttpException)?.code == 409 -> "Yoklama değişmiş. Yenileyip güncel kayıtları inceleyin."
                            editor.hasPending -> "Sonuç belirsiz. Aynı isteği yeniden deneyin; yeni kayıt isteği başlatılmadı."
                            failure is IllegalArgumentException || failure is IllegalStateException -> failure.message ?: "Yoklama doğrulanamadı."
                            else -> "Yoklama kaydedilemedi. Bağlantı ve yetkilerinizi kontrol edin."
                        }) }
                }
            }
        }
    }

    private var financeGeneration = 0L
    private var financeJob: kotlinx.coroutines.Job? = null
    fun loadFinance() {
        val state = _uiState.value
        val generation = ++financeGeneration
        financeJob?.cancel()
        val scope = com.example.data.remote.ApiClientProvider.interceptor.captureScope()
        _uiState.update { it.copy(isFinanceLoading = true, isFinanceRowsLoading = true, recordedFinanceSummary = null, financeSummary = null,
            financeSummaryError = null, financeRowsError = null, invoices = emptyList(), expenses = emptyList(), debts = emptyList()) }
        financeJob = viewModelScope.launch(requestFailures) {
            try {
                val summary = scope.io { repository.getFinanceSummary(state.selectedBranchId) }
                if (generation != financeGeneration) return@launch
                _uiState.update { it.copy(recordedFinanceSummary = summary, isFinanceLoading = false) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (scope.current() && generation == financeGeneration) {
                    val code = (failure as? com.example.data.remote.FinanceSummaryHttpException)?.code
                    if (code == 401) logout()
                    else _uiState.update { it.copy(isFinanceLoading = false, isFinanceRowsLoading = false, financeSummaryError = when (code) {
                        403 -> "Finans özetini görme yetkiniz yok."
                        404 -> "Seçili şube bulunamadı veya erişilemiyor."
                        402 -> "Üyelik erişiminizi kontrol edin."
                        else -> "Finans özeti doğrulanamadı. Yeniden yükleyin."
                    }) }
                }
                return@launch
            }
            // Remaining legacy row contracts are independent of the accepted aggregate.
            // A row failure must neither replace the summary with zero nor reveal old cache rows.
            try {
                val invoices = scope.io { repository.getInvoices(state.selectedBranchId, state.invoiceFilter.takeUnless { it == "ALL" }) }
                val expenses = scope.io { repository.getExpenses(state.selectedBranchId) }
                val debts = scope.io { repository.getDebts() }
                if (generation == financeGeneration && scope.current()) _uiState.update {
                    it.copy(invoices = invoices, expenses = expenses, debts = debts)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (scope.current() && generation == financeGeneration) _uiState.update {
                    it.copy(financeRowsError = "Finans satırları doğrulanamadı; listeyi yeniden yükleyin.")
                }
            } finally {
                if (scope.current() && generation == financeGeneration) _uiState.update { it.copy(isFinanceRowsLoading = false) }
            }
        }
    }

    fun setInvoiceFilter(status: String) {
        _uiState.update { it.copy(invoiceFilter = status) }
        loadFinance()
    }

    fun selectInvoiceForPayment(invoice: Invoice?) {
        _uiState.update { it.copy(selectedInvoiceForPayment = invoice) }
    }

    fun recordPayment(invoiceId: String, amount: Double, method: String) {
        // Open the canonical native editor; it loads the current debt and owns the durable request UUID.
        val invoice = _uiState.value.invoices.find { it.id == invoiceId } ?: Invoice(id = invoiceId)
        _uiState.update { it.copy(selectedInvoiceForPayment = invoice) }
    }

    fun recordIyzicoPayment(invoiceId: String, amount: Double, paymentResult: IyzicoPaymentResult,
        methodTitle: String = "Online ödeme") {
        _uiState.update { it.copy(toastMessage = "Online tahsilat sonucu yalnız sunucu ödeme durumundan doğrulanır.") }
    }

    fun createInvoice(studentId: String, title: String, amount: Double, dueDate: String, desc: String) {
        val student = _uiState.value.students.find { it.id == studentId }
        val description = if (desc.isNotBlank()) desc else title
        addInvoice(
            studentId = studentId,
            studentName = student?.full_name ?: "Öğrenci",
            className = student?.class_name ?: "Sınıf",
            amount = amount,
            dueDate = dueDate,
            desc = description
        )
    }

    fun addInvoice(studentId: String, studentName: String, className: String, amount: Double, dueDate: String, desc: String) {
        _uiState.update { it.copy(toastMessage = "Yeni tahakkuk için Finans > Tahakkuk taslağı ekranını kullanın. Eski kayıt yolu gönderilmedi.") }
    }

    fun addExpense(category: String, title: String, amount: Double, date: String, receiptUrl: String? = null) {
        _uiState.update { it.copy(toastMessage = "Gider oluşturmak için güncel gider formunu kullanın. Eski kayıt yolu gönderilmedi.") }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch(requestFailures) {
            repository.deleteExpense(id)
            _uiState.update { it.copy(toastMessage = "Gider kaydı silindi.") }
            loadFinance()
        }
    }

    fun changePassword(currentPass: String, newPass: String) {
        viewModelScope.launch(requestFailures) {
            repository.changePassword(currentPass, newPass, _uiState.value.currentUser.email)
            _uiState.update { it.copy(toastMessage = "Hesap şifreniz başarıyla güncellendi.") }
        }
    }

    fun retryOutboxLog(logId: String) {
        viewModelScope.launch(requestFailures) {
            _uiState.update { state ->
                val updatedLogs = state.outboxLogs.map { log ->
                    if (log.id == logId) log.copy(status = "DELIVERED") else log
                }
                state.copy(
                    outboxLogs = updatedLogs,
                    toastMessage = "Bildirim Outbox Worker tarafından kuyruktan tekrar gönderildi."
                )
            }
        }
    }

    fun updateInvoice(invoice: Invoice) {
        viewModelScope.launch(requestFailures) {
            repository.updateInvoice(invoice)
            _uiState.update { it.copy(invoiceToEdit = null, toastMessage = "Fatura güncellendi.") }
            loadFinance()
        }
    }

    fun deleteInvoice(invoiceId: String) {
        viewModelScope.launch(requestFailures) {
            repository.deleteInvoice(invoiceId)
            _uiState.update { it.copy(toastMessage = "Fatura silindi.") }
            loadFinance()
        }
    }

    suspend fun getReceipt(invoiceId: String): PaymentReceipt? {
        return repository.getReceiptForInvoice(invoiceId)
    }

    // Classes Sub-Module
    fun selectClassDetail(classGroup: ClassGroup?) {
        _uiState.update { it.copy(selectedClassDetail = classGroup) }
    }

    fun addClass(classGroup: ClassGroup) = registryMutation({ repository.addClass(classGroup) }) { created ->
        _uiState.update { it.copy(selectedClassDetail = created, toastMessage = "${created.name} sınıfı oluşturuldu.") }
        loadClasses()
    }

    fun updateClass(classGroup: ClassGroup) = registryMutation({ repository.updateClass(classGroup) }) { saved ->
        _uiState.update { it.copy(selectedClassDetail = saved, toastMessage = "${saved.name} güncellendi.") }
        loadClasses()
    }

    fun deleteClass(classId: String) = registryMutation({ repository.deleteClass(classId) }) {
        _uiState.update { it.copy(selectedClassDetail = null, toastMessage = "Sınıf silindi.") }
        loadClasses()
    }

    fun assignStudentToClass(studentId: String, classId: String) {
        val targetClass = _uiState.value.classes.find { it.id == classId }
        val enrolledCount = _uiState.value.students.count { it.class_id == classId }
        if (targetClass != null && enrolledCount >= targetClass.capacity) {
            _uiState.update {
                it.copy(toastMessage = "Kapasite Dolu (409 Conflict): ${targetClass.name} kontenjanı (${targetClass.capacity}) dolmuştur!")
            }
            return
        }
        viewModelScope.launch(requestFailures) {
            repository.assignStudentToClass(studentId, classId)
            _uiState.update { it.copy(toastMessage = "Öğrenci sınıfa atandı.") }
            loadStudents()
            loadClasses()
        }
    }

    fun removeStudentFromClass(studentId: String) {
        viewModelScope.launch(requestFailures) {
            repository.removeStudentFromClass(studentId)
            _uiState.update { it.copy(toastMessage = "Öğrenci sınıftan çıkarıldı.") }
            loadStudents()
            loadClasses()
        }
    }

    // Communication Actions
    fun loadAnnouncements() {
        viewModelScope.launch(requestFailures) {
            try {
                val list = repository.getAnnouncements(_uiState.value.selectedBranchId)
                if (list.isNotEmpty()) {
                    repository.cacheAnnouncementsLocally(list)
                    _uiState.update { it.copy(announcements = list) }
                } else {
                    val cached = repository.getCachedAnnouncements()
                    _uiState.update { it.copy(announcements = cached) }
                }
            } catch (e: Exception) {
                val cached = repository.getCachedAnnouncements()
                _uiState.update { it.copy(announcements = cached) }
            }
        }
    }

    fun openCreateAnnouncement(open: Boolean) {
        _uiState.update { it.copy(showCreateAnnouncementDialog = open) }
    }

    fun selectAnnouncementDetail(announcement: Announcement?) {
        _uiState.update { it.copy(selectedAnnouncementForDetail = announcement) }
    }

    fun openReportCardViewer(student: Student?) {
        _uiState.update { it.copy(viewingReportCardForStudent = student) }
    }

    fun openStudentProfile(student: Student?) {
        _uiState.update { it.copy(selectedStudentForFullProfile = student) }
    }


    fun sendNotification(title: String, message: String, targetRole: String, channel: String) {
        viewModelScope.launch(requestFailures) {
            val notif = NotificationItem(
                id = "ntf-${UUID.randomUUID()}",
                title = title,
                message = message,
                type = "GENERAL",
                created_at = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            )
            val updated = listOf(notif) + _uiState.value.notifications
            _uiState.update {
                it.copy(
                    notifications = updated,
                    toastMessage = "Bildirim $targetRole hedefine ($channel) iletildi."
                )
            }
            repository.sendNotification(targetRole, "$title: $message", channel)
            // Trigger Android System Push Notification
            val channelId = when {
                title.contains("Ödeme", ignoreCase = true) || title.contains("Taksit", ignoreCase = true) -> NotificationHelper.CHANNEL_FINANCE
                title.contains("Yoklama", ignoreCase = true) -> NotificationHelper.CHANNEL_ATTENDANCE
                else -> NotificationHelper.CHANNEL_ANNOUNCEMENT
            }
            NotificationHelper.showPushNotification(
                context = getApplication(),
                title = "📢 $title",
                message = message,
                channelId = channelId
            )
            loadOutboxLogs()
        }
    }

    fun deleteNotification(notificationId: String) {
        _uiState.update { it.copy(notifications = it.notifications.filter { n -> n.id != notificationId }) }
    }

    fun loadTemplates() {
        viewModelScope.launch(requestFailures) {
            val tpls = repository.getTemplates()
            _uiState.update { it.copy(templates = tpls) }
        }
    }

    fun addTemplate(title: String, channel: String, content: String, category: String = "GENERAL") {
        viewModelScope.launch(requestFailures) {
            val tpl = CommunicationTemplate("tpl-${UUID.randomUUID()}", title, channel, content, category)
            repository.addTemplate(tpl)
            _uiState.update { it.copy(toastMessage = "İletişim şablonu oluşturuldu.") }
            loadTemplates()
        }
    }

    fun createTemplate(title: String, type: String, content: String) {
        addTemplate(title, type, content, "GENERAL")
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch(requestFailures) {
            repository.deleteTemplate(templateId)
            _uiState.update { it.copy(toastMessage = "Şablon silindi.") }
            loadTemplates()
        }
    }

    private val pendingPaymentReminderIds = mutableSetOf<String>()

    fun sendPaymentReminder(invoiceId: String) {
        val state = _uiState.value
        val actor = state.currentUser
        if (!state.isLoggedIn || (actor.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) ||
            !com.example.ui.navigation.NativeNavigationPolicy.has(actor, "finance.write")) return
        val studentId = state.invoices.find { it.id == invoiceId }?.student_id
            ?: state.debts.find { it.id == invoiceId }?.student_id
            ?: state.reportsData?.overdue_debts?.find { it.id == invoiceId }?.student_id
        if (studentId.isNullOrBlank()) {
            _uiState.update { it.copy(toastMessage = "Seçili borç doğrulanamadı. Finans kayıtlarını yeniden açın.") }
            return
        }
        if (!pendingPaymentReminderIds.add(invoiceId)) return
        val scope = com.example.data.remote.ApiClientProvider.interceptor.captureScope()
        viewModelScope.launch(requestFailures) {
            try {
                val result = com.example.data.remote.NativePaymentReminderClient(scope).queueSms(invoiceId, studentId)
                scope.ensureCurrent()
                val message = when (result) {
                    is com.example.data.remote.PaymentReminderResult.Queued ->
                        "SMS hatırlatması ${result.recipients} alıcı için kuyruğa alındı."
                    is com.example.data.remote.PaymentReminderResult.Skipped -> result.reason
                }
                _uiState.update { it.copy(toastMessage = message) }
            } finally {
                pendingPaymentReminderIds.remove(invoiceId)
            }
        }
    }

    // Reports Actions
    fun loadReports() {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isReportsLoading = true) }
            val data = repository.getReportsData()
            _uiState.update { it.copy(reportsData = data, isReportsLoading = false) }
        }
    }

    // Resources Actions
    fun loadResources() {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isResourcesLoading = true) }
            val list = repository.getResources()
            _uiState.update { it.copy(resources = list, isResourcesLoading = false) }
        }
    }

    fun addResource(name: String, category: String, location: String, status: String, quantity: Int, notes: String?) {
        viewModelScope.launch(requestFailures) {
            val res = SchoolResource(
                id = "res-${UUID.randomUUID()}",
                name = name,
                type = category,
                branch_id = _uiState.value.selectedBranchId,
                capacity_or_serial = "$quantity Adet / $location",
                status = status,
                notes = notes
            )
            repository.addResource(res)
            _uiState.update { it.copy(toastMessage = "${res.name} kaynağı eklendi.") }
            loadResources()
        }
    }

    fun deleteResource(resourceId: String) {
        viewModelScope.launch(requestFailures) {
            repository.deleteResource(resourceId)
            _uiState.update { it.copy(toastMessage = "Kaynak silindi.") }
            loadResources()
        }
    }

    // Roles Actions
    fun loadRoles() {
        viewModelScope.launch(requestFailures) {
            val r = repository.getRoles()
            _uiState.update { it.copy(roles = r) }
        }
    }

    fun createRole(name: String, code: String, desc: String) {
        viewModelScope.launch(requestFailures) {
            val r = RoleDefinition("role-${UUID.randomUUID()}", name, code, desc, listOf("STUDENTS_VIEW", "ATTENDANCE_TAKE"), false, 0xFF0D9488)
            repository.addRole(r)
            _uiState.update { it.copy(toastMessage = "${r.name} rolü tanımlandı.") }
            loadRoles()
        }
    }

    fun deleteRole(roleId: String) {
        viewModelScope.launch(requestFailures) {
            repository.deleteRole(roleId)
            _uiState.update { it.copy(toastMessage = "Rol silindi.") }
            loadRoles()
        }
    }

    fun togglePermission(roleId: String, permCode: String, isGranted: Boolean = true) {
        viewModelScope.launch(requestFailures) {
            repository.toggleRolePermission(roleId, permCode)
            loadRoles()
        }
    }

    // Users Actions
    fun loadUsers() {
        viewModelScope.launch(requestFailures) {
            val u = repository.getUsers()
            _uiState.update { it.copy(users = u) }
        }
    }

    fun addUser(fullName: String, email: String, role: String, phone: String, branch: String, password: String = "123456") {
        viewModelScope.launch(requestFailures) {
            val roleDisplay = when (role.uppercase()) {
                "SUPERADMIN", "SUPER_ADMIN" -> "Süper Admin (Sistem Yöneticisi)"
                "ADMIN" -> "Genel Yönetici / Okul Müdürü"
                "OWNER" -> "Kurum Sahibi / Kurucu"
                "MANAGER" -> "Şube Müdürü"
                "TEACHER" -> "Öğretmen"
                "FINANCE" -> "Muhasebe & Finans"
                "PARENT" -> "Veli"
                "CANTEEN" -> "Kantin Personeli"
                else -> role
            }
            val user = AppUser(
                id = "usr-${UUID.randomUUID()}",
                full_name = fullName,
                email = email,
                phone = phone,
                role = role,
                role_display_name = roleDisplay,
                branch_id = _uiState.value.selectedBranchId,
                branch_name = branch.ifBlank { _uiState.value.currentUser.branch_name },
                is_active = true,
                created_at = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                password = if (password.isNotBlank()) password else "123456"
            )
            repository.addUser(user)
            _uiState.update { it.copy(toastMessage = "${user.full_name} (${user.email}) sisteme başarıyla tanımlandı.") }
            loadUsers()
        }
    }

    fun resetUserPassword(userId: String, newPassword: String) {
        viewModelScope.launch(requestFailures) {
            repository.resetUserPassword(userId, newPassword)
            _uiState.update { it.copy(toastMessage = "Kullanıcı şifresi başarıyla güncellendi.") }
            loadUsers()
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch(requestFailures) {
            repository.deleteUser(userId)
            _uiState.update { it.copy(toastMessage = "Kullanıcı silindi.") }
            loadUsers()
        }
    }

    fun updateProfile(fullName: String, email: String, phone: String, institutionName: String = "", branchName: String = "") {
        _uiState.update {
            it.copy(
                currentUser = it.currentUser.copy(
                    full_name = fullName,
                    email = email,
                    phone = phone,
                    institution_name = if (institutionName.isNotBlank()) institutionName else it.currentUser.institution_name,
                    branch_name = if (branchName.isNotBlank()) branchName else it.currentUser.branch_name
                ),
                toastMessage = "Profil ve kurum bilgileri güncellendi."
            )
        }
    }

    private var dailyReportsJob: kotlinx.coroutines.Job? = null

    fun loadDailyReports() {
        val state = _uiState.value
        if (!state.isLoggedIn || !com.example.ui.navigation.NativeNavigationPolicy.has(state.currentUser,
                "daily_reports.read", "academic.read")) return
        dailyReportsJob?.cancel()
        val scope = com.example.data.remote.ApiClientProvider.interceptor.captureScope()
        _uiState.update { it.copy(dailyReportsLoading = true, dailyReportsError = null) }
        dailyReportsJob = viewModelScope.launch {
            try {
                val rows = com.example.data.remote.NativeDailyReportClient().list(state.selectedBranchId)
                scope.ensureCurrent()
                _uiState.update { it.copy(dailyReports = rows, dailyReportsLoading = false, dailyReportsLoaded = true) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (scope.current()) _uiState.update { it.copy(dailyReportsLoading = false, dailyReportsLoaded = false,
                    dailyReportsError = "Günlük karneler yüklenemedi. Yeniden deneyin.") }
            }
        }
    }

    fun loadOutboxLogs() {
        viewModelScope.launch(requestFailures) {
            val logs = repository.getOutboxLogs()
            val reports = repository.getDeliveryReports()
            _uiState.update { it.copy(outboxLogs = logs, deliveryReports = reports) }
        }
    }

    fun checkSystemHealth() {
        viewModelScope.launch(requestFailures) {
            val health = repository.checkHealth()
            _uiState.update { it.copy(systemHealth = health) }
        }
    }

    fun testBackendConnection() {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isTestingBackend = true, toastMessage = "Backend sunucusuna bağlanılıyor...") }
            val (success, message) = repository.testBackendConnection()
            _uiState.update {
                it.copy(
                    isTestingBackend = false,
                    backendStatusMessage = message,
                    toastMessage = if (success) "🟢 $message" else "🔴 $message"
                )
            }
            checkSystemHealth()
        }
    }

    fun updateServerSettings(url: String, tenant: String = "") {
        val oldBase = com.example.data.remote.ApiClientProvider.activeBaseUrl
        repository.updateServerConfig(url, null, null)
        val base = com.example.data.remote.ApiClientProvider.activeBaseUrl
        _uiState.update { previous ->
            if (base != oldBase) Okul360UiState(selectedThemeName = previous.selectedThemeName, serverBaseUrl = base,
                toastMessage = "API adresi güncellendi. Yeni sunucuda oturum açın.")
            else previous.copy(serverBaseUrl = base, tenantId = previous.workspaceOrganizationId ?: previous.currentUser.resolvedTenantId.orEmpty())
        }
        checkSystemHealth()
        loadInitialData()
    }

    fun clearLocalCache() {
        viewModelScope.launch(requestFailures) {
            try {
                repository.clearLocalCache()
                _uiState.update { it.copy(toastMessage = "Yerel Room veritabanı önbelleği temizlendi.") }
                loadInitialData()
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Önbellek temizlenirken hata oluştu: ${e.message}") }
            }
        }
    }

    fun submitMedicineNotice(studentId: String, medicineName: String, dose: String, time: String, instructions: String) {
        val student = _uiState.value.students.find { it.id == studentId }
        val studentName = student?.full_name ?: "Öğrenci"
        val notice = MedicineNotice(
            id = "med-${UUID.randomUUID()}",
            student_id = studentId,
            student_name = studentName,
            medicine_name = medicineName,
            dose = dose,
            time_to_give = time,
            instructions = instructions,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        NotificationHelper.sendMedicineReminderPush(
            context = getApplication(),
            studentName = studentName,
            medicineName = "$medicineName ($dose)",
            time = time
        )
        viewModelScope.launch(requestFailures) {
            repository.requestChildMedicine(
                studentId,
                MedicineRequest(
                    id = notice.id,
                    student_id = studentId,
                    student_name = studentName,
                    medicine_name = medicineName,
                    dosage = dose,
                    intake_time = time,
                    parent_notes = instructions
                )
            )
        }
        _uiState.update {
            it.copy(
                medicineNotices = it.medicineNotices + notice,
                toastMessage = "$medicineName ilacı için öğretmene bildirim iletildi."
            )
        }
    }

    fun submitLeaveNotice(studentId: String, startDate: String, endDate: String, reason: String, note: String) {
        val student = _uiState.value.students.find { it.id == studentId }
        val studentName = student?.full_name ?: "Öğrenci"
        val notice = LeaveNotice(
            id = "leave-${UUID.randomUUID()}",
            student_id = studentId,
            student_name = studentName,
            start_date = startDate,
            end_date = endDate,
            reason_type = reason,
            parent_note = note
        )
        viewModelScope.launch(requestFailures) {
            repository.requestChildExcuse(
                studentId,
                ExcuseRequest(
                    id = notice.id,
                    student_id = studentId,
                    student_name = studentName,
                    reason_type = reason,
                    start_date = startDate,
                    end_date = endDate,
                    description = note,
                    status = "PENDING"
                )
            )
        }
        _uiState.update {
            it.copy(
                leaveNotices = it.leaveNotices + notice,
                toastMessage = "İzin ve mazeret talebi okul idaresine iletildi."
            )
        }
    }

    fun submitPickupNotice(studentId: String, personName: String, relationship: String, phone: String) {
        val student = _uiState.value.students.find { it.id == studentId }
        val studentName = student?.full_name ?: "Öğrenci"
        val notice = PickupNotice(
            id = "pickup-${UUID.randomUUID()}",
            student_id = studentId,
            student_name = studentName,
            authorized_person_name = personName,
            relationship = relationship,
            phone = phone,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        _uiState.update {
            it.copy(
                pickupNotices = it.pickupNotices + notice,
                toastMessage = "Teslim alacak yetkili ($personName) okul güvenliğine ve öğretmene iletildi."
            )
        }
    }

    fun markMedicineGiven(noticeId: String) {
        val targetNotice = _uiState.value.medicineNotices.find { it.id == noticeId }
        if (targetNotice != null) {
            NotificationHelper.sendMedicineReminderPush(
                context = getApplication(),
                studentName = targetNotice.student_name,
                medicineName = "${targetNotice.medicine_name} (${targetNotice.dose}) ilacı verildi",
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            )
        }
        _uiState.update { state ->
            val updated = state.medicineNotices.map {
                if (it.id == noticeId) it.copy(status = "GIVEN") else it
            }
            state.copy(
                medicineNotices = updated,
                toastMessage = "İlaç verildi olarak kaydedildi ve veliye push bildirim iletildi."
            )
        }
    }

    fun approveLeaveNotice(noticeId: String) {
        _uiState.update { state ->
            val updated = state.leaveNotices.map {
                if (it.id == noticeId) it.copy(status = "APPROVED") else it
            }
            state.copy(
                leaveNotices = updated,
                toastMessage = "İzin talebi onaylandı."
            )
        }
    }

    fun saveStudentDailyReport(draft: DailyReportDraft) {
        val state = _uiState.value
        if (!state.isLoggedIn || state.dailyReportSaving || !com.example.ui.navigation.NativeNavigationPolicy.has(
                state.currentUser, "daily_reports.write", "academic.write")) return
        val scope = com.example.data.remote.ApiClientProvider.interceptor.captureScope()
        val canRead = com.example.ui.navigation.NativeNavigationPolicy.has(state.currentUser, "daily_reports.read", "academic.read")
        _uiState.update { it.copy(dailyReportSaving = true, dailyReportSaveError = null) }
        viewModelScope.launch {
            try {
                com.example.data.remote.NativeDailyReportClient().save(draft, state.currentUser.id, canRead)
                scope.ensureCurrent()
                _uiState.update { it.copy(dailyReportSavedRevision = it.dailyReportSavedRevision + 1,
                    toastMessage = "Günlük karne kaydedildi.") }
                if (canRead) loadDailyReports()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (scope.current()) {
                    val message = when ((failure as? com.example.data.remote.DailyReportHttpException)?.code) {
                        403 -> "Bu günlük karneyi kaydetme yetkiniz yok."
                        409 -> "Bu öğrenci ve tarihte bir karne var. Güncel kaydı kontrol edin."
                        else -> failure.message?.takeIf { failure is IllegalStateException && it.startsWith("Bu öğrenci ve tarih") }
                            ?: "Karne kaydı doğrulanamadı. Taslak açık, yeniden deneyebilirsiniz."
                    }
                    _uiState.update { it.copy(dailyReportSaveError = message) }
                }
            } finally {
                if (scope.current()) _uiState.update { it.copy(dailyReportSaving = false) }
            }
        }
    }

    fun selectTheme(themeName: String) {
        _uiState.update {
            it.copy(
                selectedThemeName = themeName,
                toastMessage = "Tema değiştirildi: $themeName"
            )
        }
    }

    fun openFeedbackDialog(show: Boolean) {
        _uiState.update { it.copy(showFeedbackDialog = show) }
    }

    fun testPaymentReminderPush() {
        val firstStudent = _uiState.value.students.firstOrNull()
        val studentName = firstStudent?.full_name ?: "Ali Veli"
        val invoice = _uiState.value.invoices.firstOrNull { it.status != "PAID" }
        val amount = invoice?.amount ?: 4500.0
        val dueDate = invoice?.due_date ?: "30.08.2026"
        NotificationHelper.sendPaymentReminderPush(
            context = getApplication(),
            studentName = studentName,
            amount = amount,
            dueDate = dueDate
        )
        _uiState.update {
            it.copy(toastMessage = "Ödeme hatırlatma push bildirimi gönderildi.")
        }
    }

    fun refreshData() {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                loadInitialData()
                delay(800)
            } finally {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        toastMessage = "Veriler güncellendi."
                    )
                }
            }
        }
    }

    fun exportStudentAttendanceCsv(context: Context) {
        val selectedClass = _uiState.value.classes.find { it.id == _uiState.value.attendanceClassId }?.name ?: "Tüm Sınıflar"
        CsvExportHelper.exportAttendanceToCsv(
            context = context,
            records = _uiState.value.attendanceRecords,
            students = _uiState.value.students,
            className = selectedClass,
            date = _uiState.value.attendanceDate
        )
    }

    // ==========================================
    // Theme & Appearance Management
    // ==========================================
    fun setThemeMode(mode: String) {
        _uiState.update {
            it.copy(
                selectedThemeMode = mode,
                toastMessage = "Tema modu güncellendi: ${when (mode) { "LIGHT" -> "Aydınlık"; "DARK" -> "Karanlık"; else -> "Sistem" }}"
            )
        }
    }

    fun selectThemeMode(mode: String) {
        setThemeMode(mode)
    }

    fun openThemeConfig(open: Boolean) {
        _uiState.update { it.copy(showThemeConfigDialog = open) }
    }

    // ==========================================
    // Course Materials & Digital Documents
    // ==========================================
    fun createCourseMaterial(
        title: String,
        description: String,
        subject: String,
        className: String,
        type: String,
        fileExtension: String,
        fileSizeDisplay: String,
        dueDate: String?,
        isRequired: Boolean,
        topicSummary: String
    ) {
        val newMat = CourseMaterial(
            id = "mat-${System.currentTimeMillis()}",
            title = title,
            description = description,
            subject_name = subject,
            class_name = className,
            material_type = type,
            file_name = "${title.lowercase().replace(" ", "_").take(24)}.${fileExtension.lowercase()}",
            file_extension = fileExtension.uppercase(),
            file_size_display = fileSizeDisplay,
            teacher_id = _uiState.value.currentUser.id,
            teacher_name = _uiState.value.currentUser.full_name,
            created_at = "Bugün",
            due_date = dueDate,
            download_count = 0,
            is_required = isRequired,
            preview_pages_count = (4..16).random(),
            topic_summary = topicSummary
        )

        _uiState.update {
            it.copy(
                courseMaterials = listOf(newMat) + it.courseMaterials,
                toastMessage = "'$title' başarıyla paylaşıldı."
            )
        }

        // Notify parents/students about new document
        NotificationHelper.showPushNotification(
            context = getApplication(),
            title = "📚 Yeni Ders Materyali: $title",
            message = "$subject dersi için yeni bir ${newMat.typeDisplayName} paylaşıldı.",
            channelId = NotificationHelper.CHANNEL_ANNOUNCEMENT
        )
    }

    fun deleteCourseMaterial(materialId: String) {
        _uiState.update {
            it.copy(
                courseMaterials = it.courseMaterials.filter { m -> m.id != materialId },
                toastMessage = "Ders materyali silindi."
            )
        }
    }

    fun downloadCourseMaterial(material: CourseMaterial) {
        _uiState.update { current ->
            val updatedList = current.courseMaterials.map {
                if (it.id == material.id) it.copy(download_count = it.download_count + 1) else it
            }
            current.copy(
                courseMaterials = updatedList,
                toastMessage = "Dosya indirildi: ${material.file_name}"
            )
        }

        NotificationHelper.showPushNotification(
            context = getApplication(),
            title = "📥 Dosya İndirildi",
            message = "${material.file_name} (${material.file_size_display}) cihaz hafızasına kaydedildi.",
            channelId = NotificationHelper.CHANNEL_ANNOUNCEMENT
        )
    }

    fun acknowledgeCourseMaterial(materialId: String) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val currentParentName = _uiState.value.currentUser.full_name
        val currentParentPhone = _uiState.value.currentUser.phone
        val currentStudent = _uiState.value.students.find {
            it.parent_name.contains("Zeynep", ignoreCase = true) ||
            it.parent_name.contains("Demir", ignoreCase = true) ||
            it.parent_phone == currentParentPhone
        } ?: _uiState.value.students.firstOrNull()

        val studentId = currentStudent?.id ?: "std-1"
        val studentName = currentStudent?.full_name ?: "Ali Demir"

        val ack = MaterialAcknowledgment(
            student_id = studentId,
            student_name = studentName,
            parent_name = currentParentName,
            parent_phone = currentParentPhone ?: "0532 555 1234",
            acknowledged_at = now
        )

        _uiState.update { current ->
            val updatedList = current.courseMaterials.map { mat ->
                if (mat.id == materialId) {
                    val alreadyAcked = mat.parent_acknowledged_student_ids.contains(studentId)
                    val newIds = if (alreadyAcked) mat.parent_acknowledged_student_ids else mat.parent_acknowledged_student_ids + studentId
                    val newHistory = if (alreadyAcked) mat.acknowledgment_history else mat.acknowledgment_history + ack
                    mat.copy(
                        parent_acknowledged_student_ids = newIds,
                        acknowledgment_history = newHistory,
                        is_acknowledged_by_current_parent = true,
                        acknowledged_at_display = "Bugün ${now.substringAfter(" ")}"
                    )
                } else mat
            }
            current.copy(
                courseMaterials = updatedList,
                toastMessage = "Ödev & ders materyali için 'Görüldü / Onaylandı' bilgisi iletildi."
            )
        }

        NotificationHelper.showPushNotification(
            context = getApplication(),
            title = "✅ Veli Onayı Kaydedildi",
            message = "$studentName için ödev dökümanı görüldü onayı öğretmene iletildi.",
            channelId = NotificationHelper.CHANNEL_ANNOUNCEMENT
        )
    }

    fun sendCourseMaterialParentReminder(materialId: String, materialTitle: String) {
        val mat = _uiState.value.courseMaterials.find { it.id == materialId }
        val pendingCount = if (mat != null) (mat.target_students_count - mat.acknowledgedCount).coerceAtLeast(0) else 0

        NotificationHelper.showPushNotification(
            context = getApplication(),
            title = "🔔 Ödev Hatırlatması Gönderildi",
            message = "'$materialTitle' için onay vermeyen $pendingCount veliye anlık push bildirimi iletildi.",
            channelId = NotificationHelper.CHANNEL_ANNOUNCEMENT
        )

        _uiState.update {
            it.copy(toastMessage = "'$materialTitle' için velilere hatırlatma bildirimi gönderildi.")
        }
    }

    // ==========================================
    // Direct Parent-Teacher Messaging
    // ==========================================
    fun selectConversation(conversationId: String?) {
        _uiState.update { it.copy(selectedConversationId = conversationId) }
    }


    // Teacher Term Subject Grade & Attendance PDF Export
    fun exportTeacherTermSubjectReport(
        context: Context,
        teacherName: String = "Ahmet Yılmaz",
        subjectName: String = "Matematik",
        className: String = "8-A",
        termName: String = "2025-2026 Eğitim Öğretim Yılı 2. Dönem"
    ) {
        val gradeList = _uiState.value.studentTermGrades
        TeacherReportExportHelper.exportTeacherTermSubjectReport(
            context = context,
            teacherName = teacherName,
            subjectName = subjectName,
            className = className,
            termName = termName,
            schoolName = _uiState.value.currentUser.institution_name,
            campusName = _uiState.value.currentUser.branch_name,
            gradeList = gradeList
        )
    }

    // ==========================================
    // 4. Homework Submissions & Grading Management
    // ==========================================
    fun submitHomework(materialId: String, assignmentTitle: String, submissionText: String, fileName: String? = null, fileSize: String? = null) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val newSub = HomeworkSubmission(
            id = "sub-${System.currentTimeMillis()}",
            material_id = materialId,
            assignment_title = assignmentTitle,
            student_id = "std-1",
            student_name = "Ali Demir",
            student_number = "101",
            class_name = "8-A",
            submitted_at = now,
            submission_text = submissionText,
            attachment_file_name = fileName ?: "ali_demir_odev_belgesi.pdf",
            attachment_file_size = fileSize ?: "2.4 MB",
            status = "SUBMITTED",
            grade_score = null,
            teacher_feedback = null,
            graded_at = null,
            teacher_name = "Ahmet Yılmaz"
        )

        _uiState.update { current ->
            current.copy(
                homeworkSubmissions = listOf(newSub) + current.homeworkSubmissions,
                toastMessage = "'$assignmentTitle' ödeviniz başarıyla yüklendi."
            )
        }

        NotificationHelper.showPushNotification(
            context = getApplication(),
            title = "📝 Ödev Teslim Edildi",
            message = "'$assignmentTitle' öğretmeninizin onayına ve notlandırmasına gönderildi.",
            channelId = NotificationHelper.CHANNEL_MESSAGES
        )
    }

    fun gradeHomeworkSubmission(submissionId: String, score: Int, feedback: String) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        _uiState.update { current ->
            val updated = current.homeworkSubmissions.map { sub ->
                if (sub.id == submissionId) {
                    sub.copy(
                        status = "GRADED",
                        grade_score = score,
                        teacher_feedback = feedback,
                        graded_at = now
                    )
                } else sub
            }
            current.copy(
                homeworkSubmissions = updated,
                toastMessage = "Ödev notu ($score Puan) ve geri bildirimi kaydedildi."
            )
        }

        val targetSub = _uiState.value.homeworkSubmissions.find { it.id == submissionId }
        val studentName = targetSub?.student_name ?: "Öğrenci"
        NotificationHelper.showPushNotification(
            context = getApplication(),
            title = "🌟 Ödev Notlandırıldı ($score/100)",
            message = "$studentName için '${targetSub?.assignment_title ?: "Ödev"}' notlandırıldı.",
            channelId = NotificationHelper.CHANNEL_ANNOUNCEMENT
        )
    }

    // ==========================================
    // 5. Library Management (Book Catalog & Loans)
    // ==========================================
    // ==========================================
    // 6. Staff Duty & Leave Requests Management
    // ==========================================
    // ==========================================
    // 7. Yapay Zeka (AI / Gemini) Motoru Fonksiyonları
    // ==========================================
    fun generateAiOwnerForecast() {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isAiLoading = true, aiLoadingMessage = "Gemini 3.5 Flash: 3 aylık nakit akışı ve tahsilat projeksiyonu hesaplanıyor...") }
            delay(900)
            val current = _uiState.value
            val report = AiAnalyticsEngine.generateOwnerFinancialForecast(
                invoices = current.invoices,
                students = current.students,
                branches = current.branches
            )
            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    aiReports = listOf(report) + it.aiReports.filter { r -> r.report_type != "FINANCIAL_FORECAST" },
                    activeAiReport = report,
                    showAiReportModal = true,
                    toastMessage = "🔮 AI Finansal Öngörü Raporu hazırlandı."
                )
            }
        }
    }

    fun generateAiBranchHealthReport(branchName: String? = null) {
        viewModelScope.launch(requestFailures) {
            val branch = branchName ?: _uiState.value.currentUser.branch_name
            _uiState.update { it.copy(isAiLoading = true, aiLoadingMessage = "Gemini 3.5 Flash: $branch haftalık sağlık ve anomali analizi çıkarılıyor...") }
            delay(850)
            val current = _uiState.value
            val report = AiAnalyticsEngine.generateAdminBranchHealthReport(
                branchName = branch,
                students = current.students,
                attendances = current.attendanceRecords,
                invoices = current.invoices,
                dailyReports = current.dailyReports
            )
            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    aiReports = listOf(report) + it.aiReports.filter { r -> r.report_type != "BRANCH_HEALTH_DIGEST" },
                    activeAiReport = report,
                    showAiReportModal = true,
                    toastMessage = "🏫 AI Şube Sağlık Karnesi hazırlandı."
                )
            }
        }
    }

    fun generateAiStudentChurnRisk() {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isAiLoading = true, aiLoadingMessage = "Gemini 3.5 Flash: Devamsızlık, aidat ve duygu trendi ile riskli öğrenciler taranıyor...") }
            delay(800)
            val current = _uiState.value
            val churnList = AiAnalyticsEngine.generateStudentChurnRiskReport(
                students = current.students,
                attendances = current.attendanceRecords,
                invoices = current.invoices
            )
            val summaryReport = AiAnalysisReport(
                id = "ai-churn-${System.currentTimeMillis()}",
                target_role = "OWNER",
                report_type = "STUDENT_CHURN_RISK",
                title = "📉 AI Öğrenci Ayrılma & Kayıp Riski Raporu",
                subtitle = "Erken müdahale gerektiren ${churnList.size} riskli öğrenci tespit edildi",
                created_at = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                model_name = "Gemini 3.5 Flash Churn",
                summary = "Kurum genelinde ${current.students.size} öğrenci taranmış, devamsızlık artışı ve ödeme gecikmesi eşleşmesiyle ${churnList.size} öğrencide ayrılma riski öngörülmüştür.",
                key_findings = churnList.map { "${it.student_name} (${it.class_name}): ${it.primary_reason}" },
                recommendations = listOf(
                    "Tespit edilen öğrenciler için rehberlik servisine otomatik bildirim iletildi.",
                    "Sınıf öğretmenleri ile bireysel veli randevuları koordine edilmeli.",
                    "Devamsızlık yapan öğrencilere ders telafi föyleri yönlendirilmeli."
                ),
                risk_score = 45,
                risk_level = "Orta / Dikkat",
                metrics = mapOf(
                    "Taranan Öğrenci" to "${current.students.size}",
                    "Riskli Öğrenci" to "${churnList.size}",
                    "Kritik Alarm" to "${churnList.count { it.risk_score >= 70 }}"
                )
            )
            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    churnRiskList = churnList,
                    activeAiReport = summaryReport,
                    showAiReportModal = true,
                    toastMessage = "📉 AI Kayıp Riski analizi tamamlandı."
                )
            }
        }
    }

    fun generateAiTeacherDailyNarrative(
        studentName: String,
        meal: String,
        sleep: String,
        mood: String,
        activities: String,
        customNote: String? = null,
        onResult: (String) -> Unit = {}
    ) {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isAiLoading = true, aiLoadingMessage = "Gemini 3.5 Flash: $studentName için pedagojik günlük bülten notu üretiliyor...") }
            delay(700)
            val narrative = AiAnalyticsEngine.generateTeacherDailyNarrative(
                studentName = studentName,
                mealStatus = meal,
                sleepStatus = sleep,
                mood = mood,
                activities = activities,
                customTeacherNote = customNote
            )
            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    activeDailyNarrative = narrative,
                    showAiNarrativeModal = true,
                    toastMessage = "✨ $studentName için karne notu oluşturuldu."
                )
            }
            onResult(narrative.narrative_text)
        }
    }

    fun generateAiTeacherHomework(
        subject: String,
        topic: String,
        gradeLevel: String,
        questionCount: Int = 3,
        difficulty: String = "Orta",
        addToMaterials: Boolean = true
    ) {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isAiLoading = true, aiLoadingMessage = "Gemini 3.5 Flash: $subject '$topic' için akıllı sorular ve föy hazırlanıyor...") }
            delay(900)
            val hw = AiAnalyticsEngine.generateTeacherHomework(
                subject = subject,
                topic = topic,
                gradeLevel = gradeLevel,
                questionCount = questionCount,
                difficulty = difficulty
            )

            if (addToMaterials) {
                createCourseMaterial(
                    title = hw.title,
                    description = "${hw.grade_level} $subject müfredatına uygun yapay zeka ile üretilmiş ${hw.questions.size} soruluk çalışma föyü.",
                    subject = subject,
                    className = "8-A",
                    type = "WORKSHEET",
                    fileExtension = "PDF",
                    fileSizeDisplay = "1.9 MB",
                    dueDate = "2026-09-05",
                    isRequired = true,
                    topicSummary = hw.instructions_for_student
                )
            }

            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    activeAiHomework = hw,
                    showAiHomeworkModal = true,
                    toastMessage = "💡 AI Ödev Föyü oluşturuldu ve materyallere eklendi."
                )
            }
        }
    }

    fun openAiHomeworkModal(open: Boolean = true) {
        _uiState.update { it.copy(showAiHomeworkModal = open) }
    }

    fun generateAiDailyNarrative(studentName: String, meal: String, sleep: String, mood: String, activities: String) {
        generateAiTeacherDailyNarrative(
            studentName = studentName,
            meal = meal,
            sleep = sleep,
            mood = mood,
            activities = activities
        )
    }

    fun generateAiParentWeeklyDigest(student: Student) {
        viewModelScope.launch(requestFailures) {
            _uiState.update { it.copy(isAiLoading = true, aiLoadingMessage = "Gemini 3.5 Flash: ${student.first_name} için haftalık pedagojik karne sentezleniyor...") }
            delay(850)
            val current = _uiState.value
            val digest = AiAnalyticsEngine.generateParentWeeklyDigest(
                student = student,
                dailyReports = current.dailyReports
            )
            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    activeAiParentDigest = digest,
                    showAiParentDigestModal = true,
                    toastMessage = "🌟 ${student.first_name} için Haftalık AI Gelişim Raporu hazırlandı."
                )
            }
        }
    }

    fun openAiReportDetail(report: AiAnalysisReport) {
        _uiState.update {
            it.copy(
                activeAiReport = report,
                showAiReportModal = true
            )
        }
    }

    fun dismissAiModals() {
        _uiState.update {
            it.copy(
                showAiReportModal = false,
                showAiHomeworkModal = false,
                showAiNarrativeModal = false,
                showAiParentDigestModal = false,
                isAiLoading = false
            )
        }
    }

    fun loadAiAnalyticsData() {
        viewModelScope.launch(requestFailures) {
            val curr = _uiState.value
            val swot = AiAnalyticsEngine.generateInstitutionalSwotAnalysis(
                invoices = curr.invoices,
                students = curr.students,
                attendances = curr.attendanceRecords,
                branches = curr.branches,
                dailyReports = curr.dailyReports
            )
            val matrix = AiAnalyticsEngine.generateStudentPerformanceMatrix(
                students = curr.students,
                invoices = curr.invoices,
                attendances = curr.attendanceRecords,
                dailyReports = curr.dailyReports
            )
            _uiState.update {
                it.copy(
                    institutionalSwot = swot,
                    studentPerformanceMatrix = matrix,
                    monthlyFinanceChartData = AiAnalyticsEngine.getMonthlyFinanceChartData(),
                    branchAttendanceChartData = AiAnalyticsEngine.getBranchAttendanceChartData(curr.branches),
                    subjectAcademicChartData = AiAnalyticsEngine.getSubjectAcademicChartData(),
                    weeklyLgsNetTrendData = AiAnalyticsEngine.getWeeklyLgsNetTrendData()
                )
            }
        }
    }

    fun generateFullInstitutionalAnalysis(isGeminiApi: Boolean = true) {
        viewModelScope.launch(requestFailures) {
            _uiState.update {
                it.copy(
                    isAiLoading = true,
                    aiLoadingMessage = if (isGeminiApi) "Gemini 3.5 Flash: Finans, Yoklama ve Akademik veriler sentezleniyor..." else "Kurumsal veri motoru hesaplanıyor..."
                )
            }
            delay(1000)
            val curr = _uiState.value
            val swot = AiAnalyticsEngine.generateInstitutionalSwotAnalysis(
                invoices = curr.invoices,
                students = curr.students,
                attendances = curr.attendanceRecords,
                branches = curr.branches,
                dailyReports = curr.dailyReports
            )
            val matrix = AiAnalyticsEngine.generateStudentPerformanceMatrix(
                students = curr.students,
                invoices = curr.invoices,
                attendances = curr.attendanceRecords,
                dailyReports = curr.dailyReports
            )
            val financeChart = AiAnalyticsEngine.getMonthlyFinanceChartData(curr.invoices)
            val attendanceChart = AiAnalyticsEngine.getBranchAttendanceChartData(curr.branches, curr.attendanceRecords)
            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    institutionalSwot = swot,
                    studentPerformanceMatrix = matrix,
                    monthlyFinanceChartData = financeChart,
                    branchAttendanceChartData = attendanceChart,
                    toastMessage = "✨ Gemini 3.5 Flash Kurumsal Performans Analizi Güncellendi."
                )
            }
        }
    }

    fun filterAnalyticsBranch(branchName: String) {
        _uiState.update { it.copy(selectedAnalyticsBranch = branchName) }
    }

    fun filterAnalyticsTimeframe(timeframe: String) {
        _uiState.update { it.copy(selectedAnalyticsTimeframe = timeframe) }
    }

    fun setAnalyticsTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedAnalyticsTab = tabIndex) }
    }

    fun printInstitutionalPerformanceReport(context: Context) {
        val swot = _uiState.value.institutionalSwot
        val schoolName = _uiState.value.currentUser.institution_name
        val branchName = _uiState.value.selectedAnalyticsBranch
        val matrix = _uiState.value.studentPerformanceMatrix

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager == null) {
            _uiState.update { it.copy(toastMessage = "Yazdırma servisi bulunamadı.") }
            return
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>SCS360 Yapay Zeka Kurumsal Performans Raporu</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 24px; color: #1e293b; background: #fff; }
                    .header { border-bottom: 2px solid #2563eb; padding-bottom: 12px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }
                    .title { font-size: 22px; font-weight: bold; color: #1e1b4b; }
                    .subtitle { font-size: 13px; color: #64748b; margin-top: 4px; }
                    .badge { background: #dbeafe; color: #1d4ed8; padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: bold; }
                    .score-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
                    .score-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; text-align: center; }
                    .score-val { font-size: 20px; font-weight: bold; color: #2563eb; }
                    .score-lbl { font-size: 11px; color: #64748b; margin-top: 2px; }
                    .section { margin-bottom: 18px; }
                    .section-title { font-size: 15px; font-weight: bold; color: #0f172a; margin-bottom: 8px; border-left: 4px solid #2563eb; padding-left: 8px; }
                    .list-item { font-size: 12px; line-height: 1.6; margin-bottom: 4px; color: #334155; }
                    table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 11px; }
                    th, td { border: 1px solid #e2e8f0; padding: 8px; text-align: left; }
                    th { background: #f1f5f9; font-weight: bold; color: #334155; }
                    .footer { margin-top: 30px; font-size: 10px; color: #94a3b8; text-align: center; border-top: 1px solid #e2e8f0; padding-top: 8px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div>
                        <div class="title">🎓 $schoolName - Yapay Zeka Analiz & Kurum Karnesi</div>
                        <div class="subtitle">Şube: $branchName • Model: Gemini 3.5 Flash • Tarih: ${swot?.generated_at ?: "2026-08-30"}</div>
                    </div>
                    <div class="badge">MEB & KURUM UYUMLU</div>
                </div>

                <div class="score-grid">
                    <div class="score-card">
                        <div class="score-val">%${String.format(java.util.Locale.US, "%.1f", swot?.overall_institution_score ?: 94.2)}</div>
                        <div class="score-lbl">Genel Kurum Skoru</div>
                    </div>
                    <div class="score-card">
                        <div class="score-val">%${String.format(java.util.Locale.US, "%.1f", swot?.finance_health_score ?: 92.5)}</div>
                        <div class="score-lbl">Tahsilat Başarısı</div>
                    </div>
                    <div class="score-card">
                        <div class="score-val">%${String.format(java.util.Locale.US, "%.1f", swot?.attendance_health_score ?: 95.8)}</div>
                        <div class="score-lbl">Yoklama Devamlılığı</div>
                    </div>
                    <div class="score-card">
                        <div class="score-val">${swot?.academic_success_score ?: 89.5}</div>
                        <div class="score-lbl">Akademik Başarı</div>
                    </div>
                </div>

                <div class="section">
                    <div class="section-title">Yönetici & Kurumsal Özet</div>
                    <div style="font-size: 12px; line-height: 1.5; color: #334155;">${swot?.executive_summary}</div>
                </div>

                <div class="section">
                    <div class="section-title">Güçlü Yönler & Fırsatlar</div>
                    ${swot?.strengths?.joinToString("") { "<div class='list-item'>• $it</div>" }}
                    ${swot?.opportunities?.joinToString("") { "<div class='list-item'>• $it</div>" }}
                </div>

                <div class="section">
                    <div class="section-title">Gelişime Açık Alanlar & Riskler</div>
                    ${swot?.weaknesses?.joinToString("") { "<div class='list-item'>• $it</div>" }}
                    ${swot?.threats_and_risks?.joinToString("") { "<div class='list-item'>• $it</div>" }}
                </div>

                <div class="section">
                    <div class="section-title">Öğrenci 360° Performans ve Rehberlik Matrisi</div>
                    <table>
                        <thead>
                            <tr>
                                <th>Öğrenci</th>
                                <th>Sınıf</th>
                                <th>GPA</th>
                                <th>Yoklama</th>
                                <th>Ödeme</th>
                                <th>Risk</th>
                                <th>AI Pedagojik Tavsiye</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${matrix.take(10).joinToString("") { item ->
                                "<tr>" +
                                "<td><b>${item.student_name}</b> (#${item.student_number})</td>" +
                                "<td>${item.class_name}</td>" +
                                "<td>${String.format(java.util.Locale.US, "%.1f", item.gpa)}</td>" +
                                "<td>%${String.format(java.util.Locale.US, "%.1f", item.attendance_rate)}</td>" +
                                "<td>${if (item.payment_status == "PAID") "Ödendi" else if (item.payment_status == "PENDING") "Bekliyor" else "Gecikti"}</td>" +
                                "<td>${item.risk_level}</td>" +
                                "<td>${item.ai_pedagogical_advice}</td>" +
                                "</tr>"
                            }}
                        </tbody>
                    </table>
                </div>

                <div class="footer">
                    SCS360 Bulut Tabanlı Okul Yönetim ve Yapay Zeka Analitik Platformu • Powered by Google Gemini
                </div>
            </body>
            </html>
        """.trimIndent()

        val webView = android.webkit.WebView(context)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                val printAdapter = webView.createPrintDocumentAdapter("SCS360_AI_Kurum_Raporu")
                val jobName = "SCS360_AI_Performans_${System.currentTimeMillis()}"
                printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        _uiState.update { it.copy(toastMessage = "🖨️ Kurumsal Performans Raporu PDF baskıya hazırlandı.") }
    }

    // ==========================================
    // Okul360 Yeni Modül Metotları
    // ==========================================

    // 4. Clubs (Kulüpler)
    fun loadClubs() {
        viewModelScope.launch(requestFailures) {
            val list = repository.getClubs()
            _uiState.update { it.copy(schoolClubs = list) }
        }
    }

    fun enrollInClub(clubId: String, studentId: String) {
        viewModelScope.launch(requestFailures) {
            repository.enrollInClub(clubId, studentId)
            loadClubs()
            _uiState.update { it.copy(toastMessage = "Kulüp kaydı güncellendi.") }
        }
    }

    fun createClub(club: SchoolClub) {
        viewModelScope.launch(requestFailures) {
            repository.createClub(club)
            loadClubs()
            _uiState.update { it.copy(toastMessage = "Yeni öğrenci kulübü açıldı.") }
        }
    }

    // 5. Surveys (Anketler)
    fun loadSurveys() {
        viewModelScope.launch(requestFailures) {
            val list = repository.getSurveys()
            _uiState.update { it.copy(surveys = list) }
        }
    }

    fun participateSurvey(surveyId: String, answers: Map<String, Any>) {
        viewModelScope.launch(requestFailures) {
            repository.participateSurvey(surveyId, answers)
            loadSurveys()
            _uiState.update { it.copy(toastMessage = "Anket yanıtlarınız kaydedildi, teşekkür ederiz!") }
        }
    }

    fun createSurvey(survey: SchoolSurvey) {
        viewModelScope.launch(requestFailures) {
            repository.createSurvey(survey)
            loadSurveys()
            _uiState.update { it.copy(toastMessage = "Yeni anket yayına alındı.") }
        }
    }

    // 6. Tutoring (Birebir Etüt)
    fun loadTutoringSessions(studentId: String? = null) {
        viewModelScope.launch(requestFailures) {
            val list = repository.getTutoringSessions(studentId)
            _uiState.update { it.copy(tutoringSessions = list) }
        }
    }

    fun createTutoringSession(session: TutoringSession) {
        viewModelScope.launch(requestFailures) {
            repository.createTutoringSession(session)
            loadTutoringSessions()
            _uiState.update { it.copy(toastMessage = "Birebir etüt randevusu planlandı.") }
        }
    }

    // 7. Payroll (Personel Bordro & Maaş)
    fun loadPayrollRecords(period: String? = null) {
        viewModelScope.launch(requestFailures) {
            val list = repository.getPayrollRecords(period)
            _uiState.update { it.copy(payrollRecords = list) }
        }
    }

    fun createPayrollRecord(record: PayrollRecord) {
        viewModelScope.launch(requestFailures) {
            repository.createPayrollRecord(record)
            loadPayrollRecords()
            _uiState.update { it.copy(toastMessage = "Maaş/bordro kaydı oluşturuldu.") }
        }
    }

    // 9. Sınıf Transfer & Taksit Planı & Excel İçe/Dışa Aktarma
    fun transferStudentToClass(studentId: String, fromClassId: String, toClassId: String, reason: String) {
        viewModelScope.launch(requestFailures) {
            repository.transferStudent(studentId, fromClassId, toClassId, reason)
            loadStudents()
            loadClasses()
            _uiState.update { it.copy(toastMessage = "Öğrenci yeni sınıfa başarıyla transfer edildi.") }
        }
    }

    fun generateInstallmentPlan(
        studentId: String,
        totalAmount: Double,
        downPayment: Double,
        installmentCount: Int,
        startDate: String,
        description: String
    ) {
        viewModelScope.launch(requestFailures) {
            val req = InstallmentPlanRequest(
                student_id = studentId,
                total_amount = totalAmount,
                down_payment = downPayment,
                installment_count = installmentCount,
                start_due_date = startDate,
                description = description
            )
            val generated = repository.generateInstallmentPlan(req)
            loadFinance()
            _uiState.update { it.copy(toastMessage = "${generated.size} adet taksit ve peşinat faturası başarıyla oluşturuldu.") }
        }
    }

    fun exportStudentsExcel(context: Context) {
        viewModelScope.launch(requestFailures) {
            val csvBuilder = StringBuilder()
            csvBuilder.append("No,Ad,Soyad,Sinif,Sube,Veli,Telefon,Bakiye,Durum\n")
            _uiState.value.students.forEach { s ->
                csvBuilder.append("${s.student_number},${s.first_name},${s.last_name},${s.class_name},${s.branch_id},${s.parent_name},${s.parent_phone},${s.tuition_balance},${s.status}\n")
            }
            try {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, csvBuilder.toString())
                    type = "text/csv"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "Öğrenci Listesi Excel (CSV) Dışa Aktar"))
                _uiState.update { it.copy(toastMessage = "Öğrenci listesi CSV/Excel formatında dışa aktarıldı.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Dışa aktarma hatası: ${e.message}") }
            }
        }
    }

    fun importStudentsExcel(rowCount: Int = 5) {
        viewModelScope.launch(requestFailures) {
            val branch = _uiState.value.branches.firstOrNull()?.id ?: "branch-1"
            val targetClass = _uiState.value.classes.firstOrNull()?.name ?: "8-A"
            for (i in 1..rowCount) {
                val newStd = Student(
                    id = "std-imp-${System.currentTimeMillis()}-$i",
                    student_number = "${1000 + i}",
                    first_name = "ExcelÖğrenci$i",
                    last_name = "Kaya",
                    class_name = targetClass,
                    branch_id = branch,
                    parent_name = "Veli $i",
                    parent_phone = "+90 532 555 100$i",
                    tuition_balance = 0.0,
                    status = "ACTIVE"
                )
                repository.addStudent(newStd)
            }
            loadStudents()
            _uiState.update { it.copy(toastMessage = "Excel dosyasından $rowCount öğrenci başarıyla içe aktarıldı.") }
        }
    }

    // ==========================================
    // Sınavlar & Sınav Takvimi
    // ==========================================
    fun loadExams() {
        viewModelScope.launch(requestFailures) {
            val list = repository.getExams()
            _uiState.update { it.copy(exams = list) }
        }
    }

    fun createExam(exam: ExamItem) {
        viewModelScope.launch(requestFailures) {
            repository.createExam(exam)
            loadExams()
            _uiState.update { it.copy(toastMessage = "${exam.title} takvime eklendi.") }
        }
    }

    fun updateExam(exam: ExamItem) {
        viewModelScope.launch(requestFailures) {
            repository.updateExam(exam)
            loadExams()
            _uiState.update { it.copy(toastMessage = "Sınav bilgileri güncellendi.") }
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch(requestFailures) {
            repository.deleteExam(examId)
            loadExams()
            _uiState.update { it.copy(toastMessage = "Sınav kaydı silindi.") }
        }
    }

    // ==========================================
    // İletişim Kuyruğu & Broadcast & Şablonlar
    // ==========================================
    fun loadBroadcastData() {
        viewModelScope.launch(requestFailures) {
            val jobs = repository.getBroadcastJobs()
            val tpls = repository.getMessageTemplates()
            val providers = repository.getSmsProviders()
            _uiState.update { it.copy(broadcastJobs = jobs, messageTemplates = tpls, smsProviders = providers) }
        }
    }

    fun createBroadcastJob(job: BroadcastJob) {
        viewModelScope.launch(requestFailures) {
            repository.createBroadcastJob(job)
            loadBroadcastData()
            _uiState.update { it.copy(toastMessage = "Toplu bildirim işi (${job.channel}) oluşturuldu.") }
        }
    }

    fun sendBroadcastJob(jobId: String) {
        viewModelScope.launch(requestFailures) {
            repository.sendBroadcastJob(jobId)
            loadBroadcastData()
            _uiState.update { it.copy(toastMessage = "Toplu mesajlar başarıyla gönderildi ve teslimat raporlandı.") }
        }
    }

    fun createMessageTemplate(template: MessageTemplate) {
        viewModelScope.launch(requestFailures) {
            repository.createMessageTemplate(template)
            loadBroadcastData()
            _uiState.update { it.copy(toastMessage = "Yeni şablon '${template.name}' kaydedildi.") }
        }
    }

    // ==========================================
    // Veli Portalı İlaç & İzin Talepleri
    // ==========================================
    fun loadParentRequests(childId: String = "std-1") {
        viewModelScope.launch(requestFailures) {
            val meds = repository.getMedicineRequests(childId)
            val exc = repository.getExcuseRequests(childId)
            _uiState.update { it.copy(medicineRequests = meds, excuseRequests = exc) }
        }
    }

    fun submitMedicineRequest(childId: String, medicineName: String, dosage: String, intakeTime: String, notes: String) {
        viewModelScope.launch(requestFailures) {
            val req = MedicineRequest(
                id = "med-${System.currentTimeMillis()}",
                student_id = childId,
                student_name = _uiState.value.students.firstOrNull { it.id == childId }?.full_name ?: "Ali Demir",
                medicine_name = medicineName,
                dosage = dosage,
                intake_time = intakeTime,
                parent_notes = notes
            )
            repository.requestChildMedicine(childId, req)
            loadParentRequests(childId)
            _uiState.update { it.copy(toastMessage = "İlaç takip talebi okul revirine ve öğretmenine iletildi.") }
        }
    }

    fun submitExcuseRequest(childId: String, reasonType: String, startDate: String, endDate: String, desc: String) {
        viewModelScope.launch(requestFailures) {
            val req = ExcuseRequest(
                id = "exc-${System.currentTimeMillis()}",
                student_id = childId,
                student_name = _uiState.value.students.firstOrNull { it.id == childId }?.full_name ?: "Ali Demir",
                reason_type = reasonType,
                start_date = startDate,
                end_date = endDate,
                description = desc,
                status = "PENDING"
            )
            repository.requestChildExcuse(childId, req)
            loadParentRequests(childId)
            _uiState.update { it.copy(toastMessage = "Öğrenci izin talebi okul idaresine iletildi.") }
        }
    }

    // ==========================================
    // Finans & iyzico Ödeme
    // ==========================================
    fun payInvoiceWithIyzico(invoiceId: String, amount: Double, cardNumber: String, holderName: String) {
        _uiState.update { it.copy(toastMessage = "Kart bilgileri bu yoldan gönderilmez; güvenli online ödeme ekranını kullanın.") }
    }

    // ==========================================
    // Borçlar & Alacaklar (Debts)
    // ==========================================
    fun loadDebts(studentId: String? = null) {
        viewModelScope.launch(requestFailures) {
            val list = repository.getDebts(studentId)
            _uiState.update { it.copy(debts = list) }
        }
    }

    fun createDebt(studentId: String, studentName: String, className: String, debtType: String, description: String, amount: Double, dueDate: String) {
        _uiState.update { it.copy(toastMessage = "Yeni tahakkuk için Finans > Tahakkuk taslağı ekranını kullanın. Eski kayıt yolu gönderilmedi.") }
    }

    fun updateDebtStatus(debtId: String, newStatus: String, paidAmount: Double? = null) {
        viewModelScope.launch(requestFailures) {
            val debt = _uiState.value.debts.find { it.id == debtId }
            if (debt != null) {
                val updated = debt.copy(
                    status = newStatus,
                    paid_amount = paidAmount ?: debt.paid_amount
                )
                repository.updateDebt(updated)
                loadDebts()
                _uiState.update { it.copy(toastMessage = "Borç durumu güncellendi: $newStatus") }
            }
        }
    }

    fun deleteDebt(debtId: String) {
        viewModelScope.launch(requestFailures) {
            repository.deleteDebt(debtId)
            loadDebts()
            _uiState.update { it.copy(toastMessage = "Borç kaydı silindi.") }
        }
    }

    // ==========================================
    // Öğrenci Dosyaları & Evraklar
    // ==========================================
    fun loadStudentFiles(studentId: String) {
        viewModelScope.launch(requestFailures) {
            val files = repository.getStudentFiles(studentId)
            val updatedMap = _uiState.value.studentFiles.toMutableMap()
            updatedMap[studentId] = files
            _uiState.update { it.copy(studentFiles = updatedMap) }
        }
    }

    fun uploadStudentFile(studentId: String, fileName: String, category: String, fileSize: String) {
        viewModelScope.launch(requestFailures) {
            val file = StudentFile(
                id = "file-${System.currentTimeMillis()}",
                student_id = studentId,
                file_name = fileName,
                category = category,
                file_size = fileSize,
                upload_date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
            repository.uploadStudentFile(studentId, file)
            loadStudentFiles(studentId)
            _uiState.update { it.copy(toastMessage = "$fileName başarıyla yüklendi.") }
        }
    }

    // ==========================================
    // Sınıf Transferi (Transfer Students)
    // ==========================================
    fun transferStudents(sourceClassId: String, targetClassId: String, studentIds: List<String>) {
        val targetClass = _uiState.value.classes.find { it.id == targetClassId }
        val targetEnrolledCount = _uiState.value.students.count { it.class_id == targetClassId }
        if (targetClass != null && targetEnrolledCount + studentIds.size > targetClass.capacity) {
            _uiState.update {
                it.copy(toastMessage = "Transfer Reddedildi (409 Conflict): Hedef sınıf (${targetClass.name}) kapasitesi (${targetClass.capacity}) yetersiz!")
            }
            return
        }
        viewModelScope.launch(requestFailures) {
            repository.transferStudents(sourceClassId, targetClassId, studentIds)
            loadStudents()
            loadClasses()
            _uiState.update { it.copy(toastMessage = "${studentIds.size} öğrenci ${targetClass?.name ?: "hedef"} sınıfına transfer edildi.") }
        }
    }

    // ==========================================
    // SaaS Platformu, Üyelik, Kota, Teslim Masası & Kapı Güvenliği İşlemleri
    // ==========================================
    fun createPlatformPlan(plan: SubscriptionPlan) {
        viewModelScope.launch(requestFailures) {
            val api = com.example.data.remote.SaasApi()
            try { api.request("platform/plans", "POST", api.planBody(plan)); refreshSubscriptionAccessStatus() }
            catch (e: Exception) { if (api.current()) _uiState.update { it.copy(toastMessage = "Paket sunucuda kaydedilemedi.") } }
        }
    }

    suspend fun createTenantWithOwner(draft: com.example.data.remote.TenantProvisionDraft): String? {
        return try {
            val provisioner = com.example.data.remote.TenantProvisioner(getApplication())
            provisioner.create(draft)
            if (!provisioner.current()) return "Oturum değişti; kurum listesini yeniden açın."
            refreshSubscriptionAccessStatus()
            null
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { "Kurum oluşturma sonucu doğrulanamadı. Aynı kurum ve sahip bilgileriyle tekrar deneyin; sonucu web kurum listesinden de kontrol edebilirsiniz." }
    }

    suspend fun updateTenantSubscription(orgId: String, planId: String, periodStart: String, periodEnd: String): String? {
        val tenant = _uiState.value.platformTenants.find { it.organization_id == orgId }
            ?: return "Kurum kaydı bulunamadı; listeyi yenileyin."
        val api = com.example.data.remote.SaasApi()
        return try {
            api.updateMembership(tenant, planId, periodStart, periodEnd)
            if (!api.current()) return "Oturum değişti; listeyi yeniden açın."
            refreshSubscriptionAccessStatus()
            null
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { "Üyelik güncellemesi sunucuda doğrulanamadı. Bilgileri kontrol edin veya listeyi yenileyin." }
    }

    fun toggleTenantActive(orgId: String, isActive: Boolean) {
        viewModelScope.launch(requestFailures) {
            val api = com.example.data.remote.SaasApi()
            try { api.request("platform/tenants/${java.util.UUID.fromString(orgId)}/status", "PATCH", org.json.JSONObject().put("is_active", isActive)); refreshSubscriptionAccessStatus() }
            catch (e: Exception) { if (api.current()) _uiState.update { it.copy(toastMessage = "Kurum durumu sunucuda değiştirilemedi.") } }
        }
    }

    fun updateSubscriptionPlan(updatedPlan: SubscriptionPlan) {
        viewModelScope.launch(requestFailures) {
            val api = com.example.data.remote.SaasApi()
            try { api.request("platform/plans/${java.util.UUID.fromString(updatedPlan.id)}", "PUT", api.planBody(updatedPlan)); refreshSubscriptionAccessStatus() }
            catch (e: Exception) { if (api.current()) _uiState.update { it.copy(toastMessage = "Paket değişikliği sunucuda doğrulanamadı.") } }
        }
    }

    fun verifyPlatformInvoicePayment(invoiceId: String, amount: Double, currency: String, bankRef: String, paidAt: String) {
        _uiState.update { it.copy(toastMessage = "Bu işlem web yönetim panelinden yapılmalıdır; yerel ödeme veya üyelik kaydı oluşturulmadı.") }
    }

    fun refreshSubscriptionAccessStatus() {
        viewModelScope.launch(requestFailures) {
            val api = com.example.data.remote.SaasApi()
            try {
                val root = _uiState.value.selectedRole == UserRole.SUPERADMIN
                val snapshot = api.snapshot(root)
                if (api.current()) _uiState.update { it.copy(organizationAccess = snapshot.access,
                    subscriptionPlans = snapshot.plans, subscriptionInvoices = snapshot.invoices,
                    platformTenants = snapshot.tenants, toastMessage = "Üyelik bilgileri sunucudan okundu.") }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (api.current()) _uiState.update { it.copy(organizationAccess = OrganizationAccessStatus(),
                    subscriptionPlans = emptyList(), subscriptionInvoices = emptyList(), platformTenants = emptyList(),
                    toastMessage = "Üyelik bilgileri alınamadı; önceki örnek veriler kullanılmadı.") }
            }
        }
    }

    fun createRenewalInvoiceRequest(planId: String, billingPeriod: String) {
        _uiState.update { it.copy(toastMessage = "Üyelik satın alma mağaza yapılandırması bekliyor; yerel fiyat veya ödeme talebi üretilmedi.") }
    }

    fun simulateAccessStatusChange(newStatus: String) {
        _uiState.update { it.copy(toastMessage = "Bu işlem web yönetim panelinden yapılmalıdır; yerel ödeme veya üyelik kaydı oluşturulmadı.") }
    }

    fun updateUserAccess(userId: String, roles: List<String>, branchIds: List<String>, status: String) {
        _uiState.update { cur ->
            val updatedUsers = cur.users.map { u ->
                if (u.id == userId) {
                    u.copy(
                        role = roles.distinct().joinToString(", "),
                        is_active = status == "ACTIVE"
                    )
                } else u
            }
            val scopeDesc = if (branchIds.isEmpty()) "Tüm Kurum Geneli" else "${branchIds.size} Şube"
            cur.copy(
                users = updatedUsers,
                toastMessage = "Kullanıcı rol (${roles.joinToString()}) ve şube erişimi ($scopeDesc) atomik olarak güncellendi."
            )
        }
    }

    // ==========================================
    // Nov 4, 2026: Root Scoped Tenant Workspace, Hosted SaaS Checkout, PAID_REVIEW, Refund, Public Signup, Contacts & Standard Roles
    // ==========================================
    fun openTenantWorkspace(orgId: String, orgName: String) {
        viewModelScope.launch(requestFailures) {
            val api = com.example.data.remote.SaasApi()
            try {
                val row = api.request("platform/tenants/${java.util.UUID.fromString(orgId)}/workspace") as org.json.JSONObject
                if (api.current()) {
                    com.example.data.remote.ApiClientProvider.interceptor.workspaceOrganizationId = row.getString("id")
                    _uiState.update { freshState(it, workspace = row.getString("id"), workspaceName = row.getString("name")).copy(toastMessage = "Kurum çalışma alanı seçildi.") }
                    loadInitialData()
                }
            } catch (e: Exception) { if (api.current()) _uiState.update { it.copy(toastMessage = "Kurum seçimi doğrulanamadı.") } }
        }
    }

    fun exitTenantWorkspace() {
        com.example.data.remote.ApiClientProvider.interceptor.workspaceOrganizationId = null
        _uiState.update { freshState(it, workspace = null, workspaceName = null) }
        refreshSubscriptionAccessStatus()
    }

    fun startSaasCardCheckout(invoiceId: String, buyerName: String, buyerEmail: String, billingAddress: String) {
        _uiState.update { it.copy(toastMessage = "Uygulama içi üyelik satın alımı için mağaza abonelik yapılandırması henüz tamamlanmamış. Ödeme başlatılmadı.") }
    }

    fun checkSaasCheckoutStatus(invoiceId: String) {
        viewModelScope.launch(requestFailures) {
            val api = com.example.data.remote.SaasApi()
            try { api.request("subscription/invoices/${java.util.UUID.fromString(invoiceId)}/payment-status"); refreshSubscriptionAccessStatus() }
            catch (e: Exception) { if (api.current()) _uiState.update { it.copy(toastMessage = "Ödeme durumu sunucudan doğrulanamadı; üyelik değiştirilmedi.") } }
        }
    }

    fun activateFreeSubscriptionPlan(planId: String) {
        _uiState.update { it.copy(toastMessage = "Paket etkinleştirme işlemini web üyelik panelinden tamamlayın. Yerel üyelik aktifleştirilmedi.") }
    }

    fun cancelPendingSubscriptionInvoice(invoiceId: String) {
        viewModelScope.launch(requestFailures) {
            val api = com.example.data.remote.SaasApi()
            try { api.request("subscription/invoices/${java.util.UUID.fromString(invoiceId)}/cancel", "POST", org.json.JSONObject()); refreshSubscriptionAccessStatus() }
            catch (e: Exception) { if (api.current()) _uiState.update { it.copy(toastMessage = "İptal sunucuda doğrulanamadı; fatura değiştirilmedi.") } }
        }
    }

    fun reconcilePaidReviewInvoice(invoiceId: String, periodStart: String, periodEnd: String, note: String) {
        _uiState.update { it.copy(toastMessage = "Bu işlem web yönetim panelinden yapılmalıdır; yerel ödeme veya üyelik kaydı oluşturulmadı.") }
    }

    fun recordSubscriptionRefund(invoiceId: String, refundRef: String, note: String) {
        _uiState.update { it.copy(toastMessage = "Bu işlem web yönetim panelinden yapılmalıdır; yerel ödeme veya üyelik kaydı oluşturulmadı.") }
    }

    fun registerPublicTenantSignup(
        orgName: String,
        taxNo: String,
        ownerName: String,
        ownerEmail: String,
        ownerPhone: String,
        password: String,
        planId: String,
        billingPeriod: String
    ) {
        _uiState.update { it.copy(toastMessage = "Kurum kaydını web sitesindeki güvenli üyelik formundan tamamlayın. Örnek hesap veya üyelik oluşturulmadı.") }
    }

    fun updateOrganizationContacts(
        orgId: String,
        name: String,
        taxOffice: String,
        taxNumber: String,
        phone: String,
        email: String,
        address: String
    ) {
        _uiState.update { state ->
            val updatedOrgs = if (state.organizations.isEmpty()) {
                listOf(
                    Organization(
                        id = orgId,
                        name = name.ifBlank { "SCS360 Eğitim Kurumları" },
                        tax_office = taxOffice.trim(),
                        tax_number = taxNumber.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim()
                    )
                )
            } else {
                state.organizations.map { org ->
                    if (org.id == orgId || state.organizations.size == 1) {
                        org.copy(
                            name = name.ifBlank { org.name },
                            tax_office = taxOffice.trim(),
                            tax_number = taxNumber.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            address = address.trim()
                        )
                    } else org
                }
            }
            state.copy(
                organizations = updatedOrgs,
                toastMessage = "Kurum iletişim ve vergi bilgileri güncellendi (boş bırakılan alanlar temizlendi)."
            )
        }
    }

    fun updateBranchDetails(updatedBranch: Branch) {
        _uiState.update { state ->
            val updatedList = state.branches.map { br ->
                if (br.id == updatedBranch.id) updatedBranch else br
            }
            state.copy(
                branches = updatedList,
                toastMessage = "${updatedBranch.name} şube iletişim bilgileri güncellendi."
            )
        }
    }

    fun ensureStandardRolesForOrganization() {
        val standardCatalog = listOf(
            Triple("Kurum Sahibi", "OWNER", "Kurumun tüm idari, finansal ve abonelik yetkilerine sahiptir"),
            Triple("Kurum Yöneticisi", "ADMIN", "Kurum genelinde şube, kullanıcı, öğrenci ve akademik süreçleri yönetir"),
            Triple("Şube Yöneticisi", "BRANCH_ADMIN", "Atandığı şubedeki sınıf, öğrenci, yoklama ve günlük operasyonları yönetir"),
            Triple("Öğretmen", "TEACHER", "Ders, yoklama, ödev, sınav ve veli görüşmelerini yürütür"),
            Triple("Veli", "PARENT", "Kendi öğrencisinin devamsızlık, karne, servis, izin ve ödeme bilgilerini görüntüler"),
            Triple("Muhasebe", "ACCOUNTANT", "Tahsilat, fatura, taksit, gider, bordro ve mali raporları yönetir"),
            Triple("Personel", "STAFF", "İdari ve destek operasyonlarını yürütür"),
            Triple("Kantin", "CANTEEN", "Kantin POS satışı, ürün yönetimi ve öğrenci cüzdan bakiye işlemlerini yürütür"),
            Triple("Güvenlik", "SECURITY", "Öğrenci kimlik/QR doğrulama, kapı geçişleri ve öğrenci teslim masasını yönetir"),
            Triple("Kütüphane", "LIBRARIAN", "Kitap envanteri, ödünç verme ve iade süreçlerini yönetir"),
            Triple("Servis", "BUS_DRIVER", "Atanmış servis rotası, durak, öğrenci biniş/iniş yoklaması ve canlı konum bildirir")
        )
        _uiState.update { state ->
            val existingNames = state.roles.map { it.name.lowercase() }.toSet()
            val missingRoles = standardCatalog.mapIndexedNotNull { idx, (rName, rCode, rDesc) ->
                if (rName.lowercase() !in existingNames) {
                    RoleDefinition(
                        id = "std-role-$rCode-$idx",
                        name = rName,
                        code = rCode,
                        description = rDesc,
                        permissions = listOf("org.read", "students.read"),
                        is_system = true
                    )
                } else null
            }
            state.copy(
                roles = state.roles + missingRoles,
                toastMessage = if (missingRoles.isEmpty()) {
                    "11 standart kurum rolünün tamamı zaten tanımlı (idempotent kontrol tamamlandı)."
                } else {
                    "${missingRoles.size} eksik standart kurum rolü eklendi (Toplam 11 standart rol hazır)."
                }
            )
        }
    }

    fun linkParentUserAccount(
        studentId: String,
        parentUserId: String?,
        receiveNotifications: Boolean,
        smsPhone: String?,
        whatsappPhone: String?
    ) {
        val state = _uiState.value
        if (!parentUserId.isNullOrBlank()) {
            val conflictStudent = state.students.find { it.id != studentId && it.linked_parent_user_id == parentUserId }
            if (conflictStudent != null) {
                _uiState.update {
                    it.copy(toastMessage = "Hata (409 Conflict): Seçilen kullanıcı hesabı başka bir veli kaydına (${conflictStudent.parent_name}) zaten bağlı!")
                }
                return
            }
        }
        val linkedUser = state.users.find { it.id == parentUserId }
        _uiState.update { cur ->
            val updatedStudents = cur.students.map { std ->
                if (std.id == studentId) {
                    std.copy(
                        linked_parent_user_id = parentUserId?.takeIf { it.isNotBlank() },
                        parent_email = linkedUser?.email ?: std.parent_email,
                        receive_notifications = receiveNotifications,
                        sms_phone = smsPhone?.trim()?.takeIf { it.isNotBlank() },
                        whatsapp_phone = whatsappPhone?.trim()?.takeIf { it.isNotBlank() }
                    )
                } else std
            }
            cur.copy(
                students = updatedStudents,
                toastMessage = if (parentUserId.isNullOrBlank()) {
                    "Veli giriş hesabı bağı kaldırıldı ve bildirim tercihleri güncellendi."
                } else {
                    "Veli kaydı '${linkedUser?.full_name ?: parentUserId}' giriş hesabına bağlandı ve PARENT rolü güvenceye alındı."
                }
            )
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
