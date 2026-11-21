package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.Okul360ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

object SecurityUtils {
    fun hashPassword(password: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(password.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            password
        }
    }

    fun verifyPassword(inputPassword: String, storedPasswordOrHash: String?): Boolean {
        if (storedPasswordOrHash.isNullOrBlank()) return false
        val trimmedInput = inputPassword.trim()
        val trimmedStored = storedPasswordOrHash.trim()
        if (trimmedInput == trimmedStored) return true
        val hashed = hashPassword(trimmedInput)
        return hashed.equals(trimmedStored, ignoreCase = true)
    }
}

class Okul360Repository(private val context: Context) {

    private val database = Okul360Database.getDatabase(context)
    private val studentDao = database.studentDao()
    private val attendanceDao = database.attendanceDao()
    private val invoiceDao = database.invoiceDao()
    private val announcementDao = database.announcementDao()

    private val connectionPrefs = context.getSharedPreferences("scs360_api_connection", Context.MODE_PRIVATE)
    private var baseUrl: String = connectionPrefs.getString("base_url", ApiClientProvider.activeBaseUrl) ?: ApiClientProvider.activeBaseUrl
    private var apiService: Okul360ApiService = ApiClientProvider.createService(baseUrl)

    private val defaultSystemRoles = listOf(
        RoleDefinition(
            id = "role-superadmin",
            name = "Süper Admin (Sistem Yöneticisi)",
            code = "SUPERADMIN",
            description = "Tüm kurumlar, şubeler, AI model eğitimi, LLM chat analitiği, sistem ayarları ve tam yetki.",
            permissions = listOf("SUPERADMIN_ALL", "AI_CHAT_AUDIT", "LLM_FINE_TUNING", "ALL_BRANCHES", "FINANCE_FULL", "ACADEMIC_FULL", "SETTINGS_FULL", "USERS_FULL", "REPORTS_FULL"),
            is_system = true,
            badge_color_hex = 0xFF6D28D9
        ),
        RoleDefinition(
            id = "role-owner",
            name = "Kurum Sahibi / Kurucu",
            code = "OWNER",
            description = "Tüm şubeler, finansal veriler, kurum ayarları ve tam sistem yetkisi.",
            permissions = listOf("ALL_BRANCHES", "FINANCE_FULL", "ACADEMIC_FULL", "SETTINGS_FULL", "USERS_FULL", "REPORTS_FULL"),
            is_system = true,
            badge_color_hex = 0xFF4338CA
        ),
        RoleDefinition(
            id = "role-admin",
            name = "Genel Yönetici / Okul Müdürü",
            code = "ADMIN",
            description = "Şube yönetimi, öğrenci ve personel takibi, akademik onay ve raporlama.",
            permissions = listOf("BRANCH_MANAGE", "STUDENT_MANAGE", "STAFF_MANAGE", "ACADEMIC_MANAGE", "ANNOUNCEMENT_MANAGE"),
            is_system = true,
            badge_color_hex = 0xFF1E40AF
        ),
        RoleDefinition(
            id = "role-manager",
            name = "Şube Müdürü",
            code = "MANAGER",
            description = "İlgili şubenin operasyonel, idari ve sınıf içi süreçlerinin yönetimi.",
            permissions = listOf("BRANCH_OPERATIONS", "CLASS_MANAGE", "ATTENDANCE_VIEW", "ANNOUNCEMENT_CREATE"),
            is_system = true,
            badge_color_hex = 0xFF0D9488
        ),
        RoleDefinition(
            id = "role-teacher",
            name = "Öğretmen",
            code = "TEACHER",
            description = "Yoklama alma, ders programı, ödev ve sınav değerlendirme, karne girişi.",
            permissions = listOf("ATTENDANCE_TAKE", "GRADE_ENTRY", "HOMEWORK_ASSIGN", "EXAM_ENTRY"),
            is_system = true,
            badge_color_hex = 0xFF059669
        ),
        RoleDefinition(
            id = "role-finance",
            name = "Muhasebe & Finans",
            code = "FINANCE",
            description = "Aidat ve fatura takibi, kasa/banka, gider kayıtları ve ödeme tahsilatı.",
            permissions = listOf("INVOICE_MANAGE", "PAYMENT_COLLECT", "EXPENSE_MANAGE", "FINANCE_REPORTS"),
            is_system = true,
            badge_color_hex = 0xFFD97706
        ),
        RoleDefinition(
            id = "role-parent",
            name = "Veli",
            code = "PARENT",
            description = "Öğrenci karnesi, yoklama durumu, ödevler, duyurular ve kantin harcamaları.",
            permissions = listOf("STUDENT_VIEW_OWN", "ATTENDANCE_VIEW_OWN", "PAYMENT_PAY_OWN", "ANNOUNCEMENT_VIEW"),
            is_system = true,
            badge_color_hex = 0xFF7C3AED
        ),
        RoleDefinition(
            id = "role-canteen",
            name = "Kantin Personeli",
            code = "CANTEEN",
            description = "Öğrenci kartı ve QR ile kantin bakiyesi harcama, ürün satışı ve bakiye yükleme.",
            permissions = listOf("CANTEEN_POS", "WALLET_CHARGE", "MENU_VIEW"),
            is_system = true,
            badge_color_hex = 0xFFEA580C
        )
    )

    private val defaultSystemUsers = emptyList<AppUser>()

    private val localOrganizations = mutableListOf<Organization>()
    private val localClasses = mutableListOf<ClassGroup>()
    private val localRfidCards = mutableListOf<RfidCard>()
    private val localDevices = mutableListOf<HardwareDevice>()
    private val localAnnouncements = mutableListOf<Announcement>()
    private val localConsents = mutableListOf<StudentConsent>()
    private val localGuidance = mutableListOf<GuidanceRecord>()
    private val localAuditLogs = mutableListOf<AuditLogEntry>()

    suspend fun ensureLocalSeedData() = requestContext {
        // No-op: zero mock seed data
    }

    suspend fun login(email: String, password: String): User? =
        com.example.data.remote.NativeAuthentication(apiService).login(email, password)

    suspend fun restoreSession(token: String): User =
        com.example.data.remote.NativeAuthentication(apiService).restore(token)

    suspend fun forgotPassword(email: String) = requestContext {
        apiService.forgotPassword(mapOf("email" to email.trim()))
        Unit
    }

    private suspend fun <T> requestContext(block: suspend kotlinx.coroutines.CoroutineScope.() -> T): T {
        val scope = com.example.data.remote.NativeRequestScope.operation()
        try {
            val result = scope.io(block)
            scope.ensureCurrent()
            return result
        } catch (failure: Exception) {
            scope.ensureCurrent()
            throw failure
        }
    }

    fun updateServerConfig(newUrl: String, tenantId: String?, branchId: String?) {
        val service = ApiClientProvider.createService(newUrl)
        check(connectionPrefs.edit().putString("base_url", ApiClientProvider.activeBaseUrl).commit()) { "API adresi kaydedilemedi" }
        baseUrl = ApiClientProvider.activeBaseUrl
        apiService = service
        // Institution/branch authorization comes from the JWT and the explicit
        // Root workspace endpoint, never arbitrary connection-form headers.
    }

    suspend fun checkHealth(): SystemHealth = requestContext {
        com.example.data.remote.NativeSystemHealth(apiService).load()
    }

    suspend fun getDashboardSummary(branchId: String): DashboardSummary = requestContext {
        try {
            val raw = apiService.getDashboardSummary(branchId.takeIf { it != "all" && it.isNotBlank() })
            raw.copy(
                active_students_count = raw.resolvedActiveStudents,
                present_today_count = raw.resolvedPresentToday,
                absent_today_count = raw.resolvedAbsentToday,
                total_branches_count = raw.resolvedTotalBranches,
                monthly_revenue = raw.resolvedMonthlyRevenue
            )
        } catch (failure: Exception) { throw failure }
    }

    suspend fun getBranches(): List<Branch> = requestContext {
        com.example.data.remote.NativeRegistryClient().branches()
    }


    suspend fun createBranch(branch: Branch): Branch = requestContext {
        com.example.data.remote.NativeRegistryClient().saveBranch(null, branch)
    }

    suspend fun getOrganizations(): List<Organization> = requestContext {
        com.example.data.remote.NativeRegistryClient().organizations()
    }

    suspend fun updateOrganization(original: Organization, draft: Organization): Organization = requestContext {
        com.example.data.remote.NativeRegistryClient().saveOrganization(original, draft)
    }

    suspend fun updateBranch(original: Branch, draft: Branch): Branch = requestContext {
        com.example.data.remote.NativeRegistryClient().saveBranch(original, draft)
    }

    suspend fun getClasses(branchId: String?): List<ClassGroup> = requestContext {
        com.example.data.remote.NativeRegistryClient().classes(branchId?.takeIf { it != "all" && it.isNotBlank() })
    }
    suspend fun getClassTeacherOptions(org: String, branch: String): List<com.example.data.remote.RegistryChoice> = requestContext {
        com.example.data.remote.NativeRegistryClient().teachers(org, branch)
    }


    suspend fun addClass(classGroup: ClassGroup): ClassGroup = requestContext {
        com.example.data.remote.NativeRegistryClient().createClass(classGroup)
    }


    suspend fun updateClass(classGroup: ClassGroup): ClassGroup = requestContext {
        com.example.data.remote.NativeRegistryClient().updateClass(classGroup)
    }


    suspend fun deleteClass(classId: String): Boolean = requestContext {
        com.example.data.remote.NativeRegistryClient().deleteClass(classId); true
    }


    suspend fun assignStudentToClass(studentId: String, classId: String): Boolean = requestContext {
        com.example.data.remote.NativeRegistryClient().assignStudent(studentId, classId); true
    }


    suspend fun removeStudentFromClass(studentId: String): Boolean = requestContext {
        com.example.data.remote.NativeRegistryClient().unassignStudent(studentId); true
    }


    suspend fun getStudents(branchId: String?, classId: String?, search: String?): List<Student> = requestContext {
        com.example.data.remote.NativeRegistryClient().students(
            branchId?.takeIf { it != "all" && it.isNotBlank() }, classId?.takeIf { it != "all" && it.isNotBlank() }, search?.takeIf { it.isNotBlank() })
    }


    suspend fun addStudent(student: Student): Student = requestContext {
        com.example.data.remote.NativeRegistryClient().createStudent(student)
    }


    suspend fun updateStudent(student: Student): Student = requestContext {
        com.example.data.remote.NativeRegistryClient().updateStudent(student)
    }


    suspend fun deleteStudent(studentId: String): Boolean = requestContext {
        com.example.data.remote.NativeRegistryClient().deleteStudent(studentId); true
    }


    suspend fun getAttendance(classId: String, date: String): List<AttendanceRecord> = requestContext {
        val id = classId.takeIf { it != "all" && it.isNotBlank() }
        java.time.LocalDate.parse(date)
        val result = mutableListOf<AttendanceRecord>()
        var page = 1
        while (true) {
            val rows = apiService.getAttendance(classId = id, date = date, page = page)
            check(rows.all { it.date == date } && result.size + rows.size <= 100000) { "Yoklama listesi doğrulanamadı" }
            result.addAll(rows)
            check(result.map { it.student_id to it.date }.toSet().size == result.size) { "Yoklama sayfalaması değişti" }
            if (rows.size < 100) break
            page++
        }
        result
    }

    suspend fun getAttendanceRecords(date: String, classId: String): List<AttendanceRecord> = getAttendance(classId, date)
    suspend fun submitBulkAttendance(submit: BulkAttendanceSubmit): Boolean = saveBulkAttendance(submit)
    suspend fun saveBulkAttendance(submit: BulkAttendanceSubmit): Boolean = requestContext {
        UUID.fromString(submit.request_id)
        UUID.fromString(submit.class_id)
        java.time.LocalDate.parse(submit.date)
        apiService.submitBulkAttendance(submit)
        true
    }

    suspend fun getFinanceSummary(branchId: String?): RecordedFinanceSummary = requestContext {
        com.example.data.remote.NativeFinanceSummaryClient().load(branchId)
    }

    suspend fun getInvoices(branchId: String?, status: String?): List<Invoice> = requestContext {
        com.example.data.remote.NativeDebtLedgerClient().invoices(branchId, status)
    }

    suspend fun addInvoice(invoice: Invoice): Invoice = requestContext {
        throw IllegalStateException("Tahakkuku kalıcı işlem kimliği kullanan güncel tahakkuk formundan oluşturun")
    }

    suspend fun updateInvoice(invoice: Invoice): Invoice = requestContext {
        com.example.data.remote.NativeDebtLedgerClient().update(invoice)
    }

    suspend fun deleteInvoice(invoiceId: String): Boolean = requestContext {
        com.example.data.remote.NativeDebtLedgerClient().delete(invoiceId)
        true
    }

    suspend fun getExpenses(branchId: String? = null): List<Expense> = requestContext {
        com.example.data.remote.NativeExpenseClient().load(branchId)
    }

    suspend fun createExpense(expense: Expense): Expense = requestContext {
        throw IllegalStateException("Gideri kalıcı işlem kimliği kullanan güncel gider formundan oluşturun")
    }

    suspend fun deleteExpense(id: String): Boolean = requestContext {
        com.example.data.remote.NativeExpenseClient().delete(id)
        true
    }

    suspend fun changePassword(accountId: String, currentPass: String, newPass: String) = requestContext {
        com.example.data.remote.NativeAccountClient(apiService).changePassword(accountId, currentPass, newPass)
    }

    suspend fun updateOwnProfile(accountId: String, firstName: String, lastName: String, email: String,
        phone: String, currentPassword: String?): User = requestContext {
        com.example.data.remote.NativeAccountClient(apiService)
            .updateProfile(accountId, firstName, lastName, email, phone, currentPassword)
    }

    suspend fun recordPayment(request: RecordPaymentRequest): Boolean = requestContext {
        throw IllegalStateException("Tahsilatı güncel borç ve kalıcı işlem kimliğiyle tahsilat ekranından kaydedin")
    }

    suspend fun getReceiptForInvoice(invoiceId: String): PaymentReceipt? = requestContext {
        throw IllegalStateException("Makbuzları gerçek tahsilat bağlantısıyla sunucu makbuz ekranından açın")
    }

    suspend fun getAnnouncements(branchId: String?): List<Announcement> = requestContext {
        val bId = branchId?.takeIf { it != "all" && it.isNotBlank() }
        try {
            apiService.getAnnouncements(bId)
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createAnnouncement(request: CreateAnnouncementRequest): Announcement = requestContext {
        try {
            val created = apiService.createAnnouncement(request)
            localAnnouncements.add(0, created)
            created
        } catch (failure: Exception) { throw failure }
    }

    suspend fun updateAnnouncement(announcement: Announcement): Announcement = requestContext {
        try {
            val updated = apiService.updateAnnouncement(announcement.id, announcement)
            val idx = localAnnouncements.indexOfFirst { it.id == announcement.id }
            if (idx >= 0) localAnnouncements[idx] = updated else localAnnouncements.add(updated)
            updated
        } catch (failure: Exception) { throw failure }
    }

    suspend fun deleteAnnouncement(announcementId: String): Boolean = requestContext {
        try {
            apiService.deleteAnnouncement(announcementId)
            localAnnouncements.removeAll { it.id == announcementId }
            true
        } catch (failure: Exception) { throw failure }
    }

    suspend fun getDailyReports(date: String?, classId: String?): List<DailyReport> = requestContext {
        val cId = classId?.takeIf { it != "all" && it.isNotBlank() }
        try {
            apiService.getDailyReports(date, cId)
        } catch (failure: Exception) { throw failure }
    }

    suspend fun addDailyReport(report: DailyReport): DailyReport = requestContext {
        apiService.createDailyReport(report)
    }

    suspend fun updateDailyReport(report: DailyReport): DailyReport = requestContext {
        apiService.updateDailyReport(report.id, report)
    }

    suspend fun deleteDailyReport(reportId: String): Boolean = requestContext {
        try {
            apiService.deleteDailyReport(reportId)
            true
        } catch (failure: Exception) { throw failure }
    }

    // Communication: Notifications & Outbox
    suspend fun sendNotification(recipient: String, subject: String, channel: String): OutboxNotificationLog = requestContext {
        try {
            apiService.sendNotification(
                mapOf(
                    "recipient" to recipient,
                    "subject" to subject,
                    "channel" to channel
                )
            )
        } catch (failure: Exception) { throw failure }
        OutboxNotificationLog(
            id = "ntf-${UUID.randomUUID()}",
            channel = channel,
            recipient = recipient,
            subject = subject,
            status = "DELIVERED",
            sent_at = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            tenant_id = ApiClientProvider.interceptor.tenantId
        )
    }

    suspend fun getOutboxLogs(): List<OutboxNotificationLog> = requestContext {
        try {
            apiService.getNotifications()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun resendNotification(logId: String): Boolean = requestContext {
        try {
            apiService.retryNotificationJob(logId)
            true
        } catch (failure: Exception) { throw failure }
    }

    suspend fun deleteNotification(logId: String): Boolean = requestContext {
        try {
            apiService.deleteNotification(logId)
            true
        } catch (failure: Exception) { throw failure }
    }

    // Communication Templates
    suspend fun getTemplates(): List<CommunicationTemplate> = requestContext {
        try {
            apiService.getNotificationTemplates()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun addTemplate(template: CommunicationTemplate): CommunicationTemplate = requestContext {
        apiService.createTemplate(template)
    }

    suspend fun updateTemplate(template: CommunicationTemplate): CommunicationTemplate = requestContext {
        apiService.updateTemplate(template.id, template)
    }

    suspend fun deleteTemplate(templateId: String): Boolean = requestContext {
        try {
            apiService.deleteTemplate(templateId)
            true
        } catch (failure: Exception) { throw failure }
    }

    // Reports Data Aggregation via API
    suspend fun getReportsData(): ReportsData = requestContext {
        val overdue = try { apiService.getOverdueDebtsReport() } catch (failure: Exception) { throw failure }
        val classes = getClasses(null)
        val branches = getBranches()
        val debts = getInvoices(null, null)

        val debtors = debts.filter { it.status != "PAID" }.map { inv ->
            DebtorItem(
                student_name = inv.student_name,
                class_name = inv.class_name,
                debt_amount = inv.amount,
                days_overdue = if (inv.status == "OVERDUE") 7 else 0,
                parent_phone = "+90 532 555 0101"
            )
        }

        val attendances = classes.map { cls ->
            AttendanceRateItem(
                class_name = cls.name,
                attendance_rate = 95.0,
                present_count = cls.student_count,
                total_count = cls.student_count.coerceAtLeast(1)
            )
        }

        val occupancies = classes.map { cls ->
            val count = cls.student_count
            val cap = cls.capacity.coerceAtLeast(1)
            ClassOccupancyItem(
                class_name = cls.name,
                current_students = count,
                capacity = cap,
                occupancy_rate = ((count.toDouble() / cap.toDouble()) * 1000).toInt() / 10.0
            )
        }

        val branchPerfs = branches.map { br ->
            BranchPerformanceItem(
                branch_name = br.name,
                student_count = br.student_count,
                collection_rate = 90.0,
                attendance_rate = 95.0
            )
        }

        ReportsData(
            monthly_collections = emptyList(),
            debtors = debtors,
            overdue_debts = overdue.ifEmpty { debts.filter { it.status == "OVERDUE" } },
            attendance_rates = attendances,
            class_occupancies = occupancies,
            branch_performances = branchPerfs
        )
    }

    // Institution user directory
    suspend fun getUsers(): List<AppUser> = requestContext {
        val authorization = ApiClientProvider.interceptor
        val organization = requireNotNull(authorization.workspaceOrganizationId ?: authorization.tenantId)
        val client = com.example.data.remote.NativeInstitutionUsersClient(organization, authorization.branchId.orEmpty())
        val records = mutableListOf<AppUser>()
        var page = 1
        do {
            val (rows, more) = client.list(page, "")
            records.addAll(rows.map { row ->
                AppUser(id = row.id, full_name = row.name, email = row.email, phone = row.phone.orEmpty(),
                    role = row.roleSlugs.joinToString(", "), role_display_name = row.roleNames.joinToString(", "),
                    branch_id = "", branch_name = "", is_active = row.status == "ACTIVE", created_at = "", password = "")
            })
            page++
        } while (more)
        records
    }

    // RFID Cards Management
    suspend fun getRfidCards(): List<RfidCard> = requestContext {
        try {
            apiService.getRfidCards()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createRfidCard(card: RfidCard): RfidCard = requestContext {
        try {
            val created = apiService.createRfidCard(card)
            localRfidCards.add(created)
            created
        } catch (failure: Exception) { throw failure }
    }

    // Hardware Devices Management
    suspend fun getDevices(): List<HardwareDevice> = requestContext {
        try {
            apiService.getDevices()
        } catch (failure: Exception) { throw failure }
    }

    // =========================================================
    // Room Local Database Caching & Offline Synchronization
    // =========================================================

    suspend fun cacheStudentsLocally(students: List<Student>) = requestContext {
        if (students.isNotEmpty()) {
            val entities = students.map { it.toEntity() }
            studentDao.insertStudents(entities)
        }
    }

    suspend fun getCachedStudents(classId: String? = null, search: String? = null): List<Student> = emptyList() // Legacy disk rows lack actor/tenant scope; preserve them without displaying them.

    suspend fun cacheAttendanceLocally(records: List<AttendanceRecord>, date: String) = requestContext {
        if (records.isNotEmpty()) {
            val entities = records.map {
                AttendanceEntity(
                    id = it.id,
                    student_id = it.student_id,
                    student_name = it.student_name,
                    class_id = it.class_id,
                    date = date,
                    status = it.status,
                    note = it.note,
                    check_in_time = it.check_in_time,
                    temperature_celsius = it.temperature_celsius,
                    is_synced = true
                )
            }
            attendanceDao.insertAttendance(entities)
        }
    }

    suspend fun getCachedAttendance(classId: String? = null, date: String): List<AttendanceRecord> = emptyList() // Legacy disk rows lack actor/tenant scope; preserve them without displaying them.

    suspend fun cacheInvoicesLocally(invoices: List<Invoice>) = requestContext {
        if (invoices.isNotEmpty()) {
            val entities = invoices.map {
                InvoiceEntity(
                    id = it.id,
                    invoice_number = it.invoice_number,
                    student_id = it.student_id,
                    student_name = it.student_name,
                    class_name = it.class_name,
                    amount = it.amount,
                    paid_amount = it.paid_amount,
                    due_date = it.due_date,
                    issue_date = it.issue_date,
                    status = it.status,
                    description = it.description
                )
            }
            invoiceDao.insertInvoices(entities)
        }
    }

    suspend fun getCachedInvoices(status: String? = null): List<Invoice> = emptyList() // Legacy disk rows lack actor/tenant scope; preserve them without displaying them.

    suspend fun cacheAnnouncementsLocally(announcements: List<Announcement>) = requestContext {
        if (announcements.isNotEmpty()) {
            val entities = announcements.map {
                AnnouncementEntity(
                    id = it.id,
                    title = it.title,
                    content = it.content,
                    category = it.category,
                    target_audience = it.target_audience,
                    author_name = it.author_name,
                    author_role = it.author_role,
                    created_at = it.created_at,
                    is_pinned = it.is_pinned
                )
            }
            announcementDao.insertAnnouncements(entities)
        }
    }

    suspend fun getCachedAnnouncements(): List<Announcement> = emptyList() // Legacy disk rows lack actor/tenant scope; preserve them without displaying them.

    // ==========================================
    // SCS360 İlave Modül Repository Metotları
    // ==========================================

    // 1. Muvafakatname & İzin Belgeleri (Consents)
    suspend fun getConsents(studentId: String? = null): List<StudentConsent> = requestContext {
        try {
            val remote = apiService.getConsents(studentId)
            localConsents.clear()
            localConsents.addAll(remote)
            localConsents.toList()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createConsent(consent: StudentConsent): StudentConsent = requestContext {
        try {
            val res = apiService.createConsent(consent)
            localConsents.add(res)
            res
        } catch (failure: Exception) { throw failure }
    }

    suspend fun approveConsent(id: String, parentName: String): Boolean = requestContext {
        try {
            apiService.approveConsent(id, mapOf("parent_name" to parentName))
        } catch (failure: Exception) { throw failure }
        val idx = localConsents.indexOfFirst { it.id == id }
        if (idx >= 0) {
            localConsents[idx] = localConsents[idx].copy(
                is_approved = true,
                approved_at = "Bugün",
                approved_by_parent = parentName
            )
        }
        true
    }

    // 2. Rehberlik & PDR (Guidance)
    suspend fun getGuidanceRecords(studentId: String? = null): List<GuidanceRecord> = requestContext {
        try {
            val remote = apiService.getGuidanceRecords(studentId)
            localGuidance.clear()
            localGuidance.addAll(remote)
            localGuidance.toList()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createGuidanceRecord(record: GuidanceRecord): GuidanceRecord = requestContext {
        try {
            val res = apiService.createGuidanceRecord(record)
            localGuidance.add(0, res)
            res
        } catch (failure: Exception) { throw failure }
    }

    suspend fun deleteGuidanceRecord(id: String): Boolean = requestContext {
        try { apiService.deleteGuidanceRecord(id) } catch (failure: Exception) { throw failure }
        localGuidance.removeAll { it.id == id }
        true
    }

    // 8. Sistem Güvenlik & Denetim Logları (Security & Audit)
    suspend fun getAuditLogs(): List<AuditLogEntry> = requestContext {
        try {
            val remote = apiService.getAuditLogs()
            localAuditLogs.clear()
            localAuditLogs.addAll(remote)
            localAuditLogs.toList()
        } catch (failure: Exception) { throw failure }
    }

    // 9. Sınıf Transfer & Taksit Planı İşlemleri
    suspend fun transferStudent(studentId: String, fromClassId: String, toClassId: String, reason: String): Boolean = requestContext {
        try {
            apiService.transferStudents(fromClassId, mapOf("student_id" to studentId, "target_class_id" to toClassId, "reason" to reason))
        } catch (failure: Exception) { throw failure }
        val std = studentDao.getStudentByIdDirect(studentId)
        if (std != null) {
            studentDao.insertStudent(std.copy(class_id = toClassId))
        }
        true
    }

    suspend fun generateInstallmentPlan(req: InstallmentPlanRequest): List<Invoice> = requestContext {
        val invoices = mutableListOf<Invoice>()
        val remaining = (req.total_amount - req.down_payment).coerceAtLeast(0.0)
        val perInstallment = if (req.installment_count > 0) remaining / req.installment_count else remaining

        if (req.down_payment > 0) {
            invoices.add(
                Invoice(
                    id = "inv-down-${System.currentTimeMillis()}",
                    student_id = req.student_id,
                    invoice_number = "PEŞINAT-${System.currentTimeMillis() % 10000}",
                    description = "${req.description} (Peşinat)",
                    amount = req.down_payment,
                    paid_amount = 0.0,
                    due_date = req.start_due_date,
                    status = "PENDING"
                )
            )
        }

        for (i in 1..req.installment_count) {
            invoices.add(
                Invoice(
                    id = "inv-inst-$i-${System.currentTimeMillis()}",
                    student_id = req.student_id,
                    invoice_number = "TAKSIT-$i/${req.installment_count}",
                    description = "${req.description} ($i/${req.installment_count}. Taksit)",
                    amount = perInstallment,
                    paid_amount = 0.0,
                    due_date = "2026-0${(9 + (i - 1)) % 12 + 1}-15",
                    status = "PENDING"
                )
            )
        }

        invoices.forEach { invoiceDao.insertInvoice(it.toEntity()) }
        try {
            apiService.generateInstallmentPlan(req)
        } catch (failure: Exception) { throw failure }

        invoices
    }

    // ==========================================
    // Veli Portalı İlaç & İzin Talepleri
    // ==========================================
    private val localMedicineRequests = mutableListOf<MedicineRequest>()
    private val localExcuseRequests = mutableListOf<ExcuseRequest>()

    suspend fun getMedicineRequests(childId: String): List<MedicineRequest> = requestContext {
        try {
            val res = apiService.getChildMedicineRequests(childId)
            localMedicineRequests.clear()
            localMedicineRequests.addAll(res)
            res
        } catch (failure: Exception) { throw failure }
    }

    suspend fun requestChildMedicine(childId: String, req: MedicineRequest): MedicineRequest = requestContext {
        try {
            apiService.requestChildMedicine(childId, req)
        } catch (failure: Exception) { throw failure }
        localMedicineRequests.add(0, req)
        req
    }

    suspend fun requestChildExcuse(childId: String, req: ExcuseRequest): ExcuseRequest = requestContext {
        try {
            apiService.requestChildExcuse(childId, req)
        } catch (failure: Exception) { throw failure }
        localExcuseRequests.add(0, req)
        req
    }

    suspend fun getExcuseRequests(childId: String): List<ExcuseRequest> = requestContext {
        try {
            val res = apiService.getChildExcuseRequests(childId)
            localExcuseRequests.clear()
            localExcuseRequests.addAll(res)
            res
        } catch (failure: Exception) { throw failure }
    }

    // ==========================================
    // iyzico Sanal POS
    // ==========================================
    suspend fun initializeIyzicoPayment(invoiceId: String, amount: Double, cardNumber: String, holderName: String): Map<String, Any> = requestContext {
        throw IllegalStateException("Kart bilgileri bu yoldan gönderilemez; güvenli sağlayıcı ödeme sayfasını kullanın")
    }

    suspend fun clearLocalCache() = requestContext {
        studentDao.clearAll()
        attendanceDao.clearAll()
        invoiceDao.clearAll()
        announcementDao.clearAll()
    }

    // Debts & Collections (/api/debts)

    suspend fun getDebts(studentId: String? = null): List<Debt> = requestContext {
        com.example.data.remote.NativeDebtLedgerClient().debts(studentId)
    }

    suspend fun createDebt(debt: Debt): Debt = requestContext {
        throw IllegalStateException("Tahakkuku kalıcı işlem kimliği kullanan güncel tahakkuk formundan oluşturun")
    }

    suspend fun updateDebt(debt: Debt): Debt = requestContext {
        throw IllegalStateException("Borç durumu tahsilat ve vadeden hesaplanır; tahakkuk düzenleme/tahsilat/iptal ekranını kullanın")
    }

    suspend fun deleteDebt(id: String): Boolean = requestContext {
        com.example.data.remote.NativeDebtLedgerClient().delete(id)
        true
    }

    // Delivery Reports (/api/communications/delivery-reports)
    private val localDeliveryReports = mutableListOf<DeliveryReport>()

    suspend fun getDeliveryReports(broadcastJobId: String? = null): List<DeliveryReport> = requestContext {
        try {
            val res = apiService.getDeliveryReports(broadcastJobId)
            localDeliveryReports.clear()
            localDeliveryReports.addAll(res)
            res
        } catch (failure: Exception) { throw failure }
    }

    // Student Files (/api/students/{id}/files)
    private val localStudentFiles = mutableMapOf<String, MutableList<StudentFile>>()

    suspend fun getStudentFiles(studentId: String): List<StudentFile> = requestContext {
        try {
            val res = apiService.getStudentFiles(studentId)
            localStudentFiles[studentId] = res.toMutableList()
            res
        } catch (failure: Exception) { throw failure }
    }

    suspend fun uploadStudentFile(studentId: String, file: StudentFile): StudentFile = requestContext {
        try {
            apiService.uploadStudentFile(studentId, file)
        } catch (failure: Exception) { throw failure }
        val list = localStudentFiles.getOrPut(studentId) { mutableListOf() }
        list.add(0, file)
        file
    }

    // Student Transfer between classes (/api/classes/{class_id}/transfer-students)
    suspend fun transferStudents(classId: String, targetClassId: String, studentIds: List<String>): Boolean = requestContext {
        try {
            apiService.transferStudents(classId, mapOf("target_class_id" to targetClassId, "student_ids" to studentIds))
        } catch (failure: Exception) { throw failure }
        studentDao.getAllStudentsList().forEach { entity ->
            if (entity.id in studentIds) {
                studentDao.insertStudent(entity.copy(class_id = targetClassId))
            }
        }
        true
    }
}

// Extension mappers
private fun Student.toEntity(): StudentEntity = StudentEntity(
    id = id,
    student_number = student_number,
    first_name = first_name,
    last_name = last_name,
    class_id = class_id,
    class_name = class_name,
    branch_id = branch_id,
    academic_year = academic_year,
    parent_name = parent_name,
    parent_phone = parent_phone,
    parent_email = parent_email,
    status = status,
    allergies_or_diet = allergies_or_diet,
    blood_type = blood_type,
    avatar_color_hex = avatar_color_hex,
    tuition_balance = tuition_balance,
    today_attendance_status = today_attendance_status
)

private fun StudentEntity.toModel(): Student = Student(
    id = id,
    student_number = student_number,
    first_name = first_name,
    last_name = last_name,
    class_id = class_id,
    class_name = class_name,
    branch_id = branch_id,
    academic_year = academic_year,
    parent_name = parent_name,
    parent_phone = parent_phone,
    parent_email = parent_email,
    status = status,
    allergies_or_diet = allergies_or_diet,
    blood_type = blood_type,
    avatar_color_hex = avatar_color_hex,
    tuition_balance = tuition_balance,
    today_attendance_status = today_attendance_status
)

val DEFAULT_SEEDED_STUDENTS: List<Student> = emptyList()
val DEFAULT_SEEDED_INVOICES: List<Invoice> = emptyList()
val DEFAULT_SEEDED_ORGANIZATIONS: List<Organization> = emptyList()
val DEFAULT_SEEDED_BRANCHES: List<Branch> = emptyList()
val DEFAULT_SEEDED_CLASSES: List<ClassGroup> = emptyList()
val DEFAULT_SEEDED_ANNOUNCEMENTS: List<Announcement> = emptyList()
val DEFAULT_SEEDED_RFID_CARDS: List<RfidCard> = emptyList()
val DEFAULT_SEEDED_DEVICES: List<HardwareDevice> = emptyList()
val DEFAULT_SEEDED_EXPENSES: List<Expense> = emptyList()
