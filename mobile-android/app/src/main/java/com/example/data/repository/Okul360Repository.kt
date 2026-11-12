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
    private val localBranches = mutableListOf<Branch>()
    private val localClasses = mutableListOf<ClassGroup>()
    private val localRoles = mutableListOf<RoleDefinition>()
    private val localUsers = mutableListOf<AppUser>()
    private val localResources = mutableListOf<SchoolResource>()
    private val localRfidCards = mutableListOf<RfidCard>()
    private val localDevices = mutableListOf<HardwareDevice>()
    private val localAnnouncements = mutableListOf<Announcement>()
    private val localExpenses = mutableListOf<Expense>()
    private val localConsents = mutableListOf<StudentConsent>()
    private val localGuidance = mutableListOf<GuidanceRecord>()
    private val localInfirmary = mutableListOf<InfirmaryVisit>()
    private val localClubs = mutableListOf<SchoolClub>()
    private val localSurveys = mutableListOf<SchoolSurvey>()
    private val localTutoring = mutableListOf<TutoringSession>()
    private val localPayrolls = mutableListOf<PayrollRecord>()
    private val localAuditLogs = mutableListOf<AuditLogEntry>()

    suspend fun ensureLocalSeedData() = requestContext {
        // No-op: zero mock seed data
    }

    suspend fun testBackendConnection(): Pair<Boolean, String> = requestContext {
        val startTime = System.currentTimeMillis()
        try {
            val readyResponse = try {
                apiService.getReady()
            } catch (failure: Exception) { throw failure }
            val elapsed = System.currentTimeMillis() - startTime
            Pair(true, "Canlı Backend Bağlantısı Başarılı: HTTP 200 OK (${elapsed}ms) - $baseUrl")
        } catch (failure: Exception) { throw failure }
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
        try {
            apiService.getHealth()
        } catch (failure: Exception) { throw failure }
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
        try {
            val created = apiService.createBranch(branch)
            localBranches.add(created)
            created
        } catch (failure: Exception) { throw failure }
    }

    suspend fun getOrganizations(): List<Organization> = requestContext {
        try {
            apiService.getOrganizations()
        } catch (failure: Exception) { throw failure }
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
        val bId = branchId?.takeIf { it != "all" && it.isNotBlank() }
        val s = status?.takeIf { it != "ALL" && it.isNotBlank() }

        try {
            val list = apiService.getDebts(bId, s)
            cacheInvoicesLocally(list)
            list
        } catch (failure: Exception) { throw failure }
    }

    suspend fun addInvoice(invoice: Invoice): Invoice = requestContext {
        try {
            invoiceDao.insertInvoice(
                InvoiceEntity(
                    id = invoice.id,
                    invoice_number = invoice.invoice_number,
                    student_id = invoice.student_id,
                    student_name = invoice.student_name,
                    class_name = invoice.class_name,
                    amount = invoice.amount,
                    paid_amount = invoice.paid_amount,
                    due_date = invoice.due_date,
                    issue_date = invoice.issue_date,
                    status = invoice.status,
                    description = invoice.description
                )
            )
            apiService.createDebt(invoice)
        } catch (failure: Exception) { throw failure }
    }

    suspend fun updateInvoice(invoice: Invoice): Invoice = requestContext {
        try {
            invoiceDao.insertInvoice(
                InvoiceEntity(
                    id = invoice.id,
                    invoice_number = invoice.invoice_number,
                    student_id = invoice.student_id,
                    student_name = invoice.student_name,
                    class_name = invoice.class_name,
                    amount = invoice.amount,
                    paid_amount = invoice.paid_amount,
                    due_date = invoice.due_date,
                    issue_date = invoice.issue_date,
                    status = invoice.status,
                    description = invoice.description
                )
            )
            apiService.updateDebt(invoice.id, invoice)
        } catch (failure: Exception) { throw failure }
    }

    suspend fun deleteInvoice(invoiceId: String): Boolean = requestContext {
        try {
            invoiceDao.deleteInvoice(invoiceId)
            apiService.deleteDebt(invoiceId)
            true
        } catch (failure: Exception) { throw failure }
    }

    suspend fun getExpenses(branchId: String? = null): List<Expense> = requestContext {
        val bId = branchId?.takeIf { it != "all" && it.isNotBlank() }
        try {
            val remote = apiService.getExpenses(bId)
            if (remote.isNotEmpty()) {
                remote
            } else {
                localExpenses.filter { bId == null || it.branch_id == bId }
            }
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createExpense(expense: Expense): Expense = requestContext {
        try {
            val created = apiService.createExpense(expense)
            localExpenses.add(0, created)
            created
        } catch (failure: Exception) { throw failure }
    }

    suspend fun deleteExpense(id: String): Boolean = requestContext {
        try {
            apiService.deleteExpense(id)
            localExpenses.removeAll { it.id == id }
            true
        } catch (failure: Exception) { throw failure }
    }

    suspend fun changePassword(currentPass: String, newPass: String, userEmail: String? = null): Boolean = requestContext {
        try {
            apiService.changePassword(mapOf("current_password" to currentPass, "new_password" to newPass))
        } catch (failure: Exception) { throw failure }

        if (!userEmail.isNullOrBlank()) {
            try {
                database.userDao().updatePasswordByEmail(userEmail, newPass)
                val idx = localUsers.indexOfFirst { it.email.equals(userEmail, ignoreCase = true) }
                if (idx >= 0) {
                    localUsers[idx] = localUsers[idx].copy(password = newPass)
                }
            } catch (failure: Exception) { throw failure }
        }
        true
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

    // Resources Management
    suspend fun getResources(): List<SchoolResource> = requestContext {
        try {
            apiService.getResources()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun addResource(resource: SchoolResource): SchoolResource = requestContext {
        try {
            val created = apiService.createResource(resource)
            localResources.add(created)
            created
        } catch (failure: Exception) { throw failure }
    }

    suspend fun updateResource(resource: SchoolResource): SchoolResource = requestContext {
        try {
            val updated = apiService.updateResource(resource.id, resource)
            val idx = localResources.indexOfFirst { it.id == resource.id }
            if (idx >= 0) localResources[idx] = updated else localResources.add(updated)
            updated
        } catch (failure: Exception) { throw failure }
    }

    suspend fun deleteResource(resourceId: String): Boolean = requestContext {
        try {
            apiService.deleteResource(resourceId)
            localResources.removeAll { it.id == resourceId }
            true
        } catch (failure: Exception) { throw failure }
    }

    // Roles & Permissions Management
    suspend fun getRoles(): List<RoleDefinition> = requestContext {
        try {
            apiService.getRoles()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun addRole(role: RoleDefinition): RoleDefinition = requestContext {
        try {
            val created = apiService.createRole(role)
            localRoles.add(created)
            created
        } catch (failure: Exception) { throw failure }
    }

    suspend fun updateRole(role: RoleDefinition): RoleDefinition = requestContext {
        try {
            val updated = apiService.updateRole(role.id, role)
            val idx = localRoles.indexOfFirst { it.id == role.id }
            if (idx >= 0) localRoles[idx] = updated else localRoles.add(updated)
            updated
        } catch (failure: Exception) { throw failure }
    }

    suspend fun deleteRole(roleId: String): Boolean = requestContext {
        try {
            apiService.deleteRole(roleId)
            localRoles.removeAll { it.id == roleId }
            true
        } catch (failure: Exception) { throw failure }
    }

    suspend fun toggleRolePermission(roleId: String, permissionCode: String): Boolean = requestContext {
        try {
            val role = apiService.getRoleById(roleId)
            val currentPerms = role.permissions.toMutableList()
            if (currentPerms.contains(permissionCode)) {
                currentPerms.remove(permissionCode)
            } else {
                currentPerms.add(permissionCode)
            }
            apiService.updateRole(roleId, role.copy(permissions = currentPerms))
            val idx = localRoles.indexOfFirst { it.id == roleId }
            if (idx >= 0) localRoles[idx] = localRoles[idx].copy(permissions = currentPerms)
            true
        } catch (failure: Exception) { throw failure }
    }

    // App Users Management
    suspend fun getUsers(): List<AppUser> = requestContext {
        try {
            val remoteUsers = try {
                apiService.getUsers()
            } catch (failure: Exception) { throw failure }
            if (remoteUsers.isNotEmpty()) {
                val entities = remoteUsers.map { u ->
                    UserEntity(
                        id = u.id,
                        full_name = u.full_name,
                        email = u.email,
                        role = u.role,
                        tenant_id = "tenant-001",
                        institution_name = "SCS360 Eğitim Kurumları",
                        branch_id = u.branch_id,
                        branch_name = u.branch_name,
                        phone = u.phone,
                        avatar_url = null
                    )
                }
                database.userDao().insertUsers(entities)
                return@requestContext remoteUsers
            }
        } catch (failure: Exception) { throw failure }

        // Room Database query
        val dbUsers = database.userDao().getAllUsers()
        if (dbUsers.isNotEmpty()) {
            dbUsers.map { entity ->
                AppUser(
                    id = entity.id,
                    full_name = entity.full_name,
                    email = entity.email,
                    phone = entity.phone ?: "",
                    role = entity.role,
                    role_display_name = when (entity.role.uppercase()) {
                        "OWNER" -> "Kurum Sahibi / Kurucu"
                        "ADMIN" -> "Genel Yönetici / Okul Müdürü"
                        "MANAGER" -> "Şube Müdürü"
                        "TEACHER" -> "Öğretmen"
                        "FINANCE" -> "Muhasebe & Finans"
                        "PARENT" -> "Veli"
                        "CANTEEN" -> "Kantin Personeli"
                        else -> entity.role
                    },
                    branch_id = entity.branch_id,
                    branch_name = entity.branch_name,
                    is_active = true,
                    created_at = "2026-01-01",
                    password = entity.password_hash
                )
            }
        } else {
            localUsers.toList()
        }
    }

    suspend fun addUser(user: AppUser): AppUser = requestContext {
        val created = try {
            apiService.createUser(user)
        } catch (failure: Exception) { throw failure }

        val pass = if (created.password.isNotBlank()) created.password else "123456"

        try {
            database.userDao().insertUser(
                UserEntity(
                    id = created.id,
                    full_name = created.full_name,
                    email = created.email,
                    role = created.role,
                    tenant_id = "tenant-001",
                    institution_name = "SCS360 Eğitim Kurumları",
                    branch_id = created.branch_id,
                    branch_name = created.branch_name,
                    phone = created.phone,
                    avatar_url = null,
                    password_hash = pass
                )
            )
        } catch (failure: Exception) { throw failure }

        val userWithPass = created.copy(password = pass)
        val idx = localUsers.indexOfFirst { it.id == userWithPass.id }
        if (idx >= 0) localUsers[idx] = userWithPass else localUsers.add(userWithPass)
        userWithPass
    }

    suspend fun updateUser(user: AppUser): AppUser = requestContext {
        val updated = try {
            apiService.updateUser(user.id, user)
        } catch (failure: Exception) { throw failure }

        val pass = if (updated.password.isNotBlank()) updated.password else "123456"

        try {
            database.userDao().insertUser(
                UserEntity(
                    id = updated.id,
                    full_name = updated.full_name,
                    email = updated.email,
                    role = updated.role,
                    tenant_id = "tenant-001",
                    institution_name = "SCS360 Eğitim Kurumları",
                    branch_id = updated.branch_id,
                    branch_name = updated.branch_name,
                    phone = updated.phone,
                    avatar_url = null,
                    password_hash = pass
                )
            )
        } catch (failure: Exception) { throw failure }

        val idx = localUsers.indexOfFirst { it.id == user.id }
        if (idx >= 0) localUsers[idx] = updated else localUsers.add(updated)
        updated
    }

    suspend fun resetUserPassword(userId: String, newPass: String): Boolean = requestContext {
        try {
            database.userDao().updatePassword(userId, newPass)
        } catch (_: Exception) {}

        val idx = localUsers.indexOfFirst { it.id == userId }
        if (idx >= 0) {
            localUsers[idx] = localUsers[idx].copy(password = newPass)
        }
        true
    }

    suspend fun deleteUser(userId: String): Boolean = requestContext {
        try {
            apiService.deleteUser(userId)
        } catch (failure: Exception) { throw failure }

        try {
            database.userDao().deleteUser(userId)
        } catch (failure: Exception) { throw failure }

        localUsers.removeAll { it.id == userId }
        true
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

    // 3. Revir & Sağlık (Infirmary)
    suspend fun getInfirmaryVisits(studentId: String? = null): List<InfirmaryVisit> = requestContext {
        try {
            val remote = apiService.getInfirmaryVisits(studentId)
            localInfirmary.clear()
            localInfirmary.addAll(remote)
            localInfirmary.toList()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createInfirmaryVisit(visit: InfirmaryVisit): InfirmaryVisit = requestContext {
        try {
            val res = apiService.createInfirmaryVisit(visit)
            localInfirmary.add(0, res)
            res
        } catch (failure: Exception) { throw failure }
    }

    // 4. Öğrenci Kulüpleri (Clubs)
    suspend fun getClubs(): List<SchoolClub> = requestContext {
        try {
            val remote = apiService.getClubs()
            localClubs.clear()
            localClubs.addAll(remote)
            localClubs.toList()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun enrollInClub(clubId: String, studentId: String): Boolean = requestContext {
        try {
            apiService.enrollInClub(clubId, mapOf("student_id" to studentId))
        } catch (failure: Exception) { throw failure }
        val idx = localClubs.indexOfFirst { it.id == clubId }
        if (idx >= 0) {
            val club = localClubs[idx]
            localClubs[idx] = club.copy(
                is_enrolled = !club.is_enrolled,
                enrolled_count = if (!club.is_enrolled) club.enrolled_count + 1 else (club.enrolled_count - 1).coerceAtLeast(0)
            )
        }
        true
    }

    suspend fun createClub(club: SchoolClub): SchoolClub = requestContext {
        try {
            val res = apiService.createClub(club)
            localClubs.add(0, res)
            res
        } catch (failure: Exception) { throw failure }
    }

    // 5. Anketler & Memnuniyet (Surveys)
    suspend fun getSurveys(): List<SchoolSurvey> = requestContext {
        try {
            val remote = apiService.getSurveys()
            localSurveys.clear()
            localSurveys.addAll(remote)
            localSurveys.toList()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createSurvey(survey: SchoolSurvey): SchoolSurvey = requestContext {
        try {
            val res = apiService.createSurvey(survey)
            localSurveys.add(0, res)
            res
        } catch (failure: Exception) { throw failure }
    }

    suspend fun participateSurvey(surveyId: String, answers: Map<String, Any>): Boolean = requestContext {
        try {
            apiService.participateSurvey(surveyId, answers)
        } catch (failure: Exception) { throw failure }
        val idx = localSurveys.indexOfFirst { it.id == surveyId }
        if (idx >= 0) {
            val s = localSurveys[idx]
            localSurveys[idx] = s.copy(
                is_participated = true,
                total_responses = s.total_responses + 1
            )
        }
        true
    }

    // 6. Birebir Etüt (Tutoring)
    suspend fun getTutoringSessions(studentId: String? = null): List<TutoringSession> = requestContext {
        try {
            val remote = apiService.getTutoringSessions(studentId)
            localTutoring.clear()
            localTutoring.addAll(remote)
            localTutoring.toList()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createTutoringSession(session: TutoringSession): TutoringSession = requestContext {
        try {
            val res = apiService.createTutoringSession(session)
            localTutoring.add(0, res)
            res
        } catch (failure: Exception) { throw failure }
    }

    // 7. Personel Maaş & Bordro (Payroll)
    suspend fun getPayrollRecords(period: String? = null): List<PayrollRecord> = requestContext {
        try {
            val remote = apiService.getPayrollRecords(period)
            localPayrolls.clear()
            localPayrolls.addAll(remote)
            localPayrolls.toList()
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createPayrollRecord(record: PayrollRecord): PayrollRecord = requestContext {
        try {
            val res = apiService.createPayrollRecord(record)
            localPayrolls.add(0, res)
            res
        } catch (failure: Exception) { throw failure }
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
    // Sınavlar & Sınav Takvimi (Exams)
    // ==========================================
    private val localExams = mutableListOf<ExamItem>()

    suspend fun getExams(): List<ExamItem> = requestContext {
        try {
            val remote = apiService.getExams()
            localExams.clear()
            localExams.addAll(remote)
            remote
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createExam(exam: ExamItem): ExamItem = requestContext {
        try {
            val res = apiService.createExam(exam)
            localExams.add(0, res)
            res
        } catch (failure: Exception) { throw failure }
    }

    suspend fun updateExam(exam: ExamItem): ExamItem = requestContext {
        try {
            apiService.updateExam(exam.id, exam)
        } catch (failure: Exception) { throw failure }
        val idx = localExams.indexOfFirst { it.id == exam.id }
        if (idx >= 0) localExams[idx] = exam
        exam
    }

    suspend fun deleteExam(examId: String): Boolean = requestContext {
        try {
            apiService.deleteExam(examId)
        } catch (failure: Exception) { throw failure }
        localExams.removeAll { it.id == examId }
        true
    }

    // ==========================================
    // İletişim Kuyruğu & Toplu Gönderim (Broadcast & Outbox)
    // ==========================================
    private val localBroadcastJobs = mutableListOf<BroadcastJob>()
    private val localMessageTemplates = mutableListOf<MessageTemplate>()
    private val localSmsProviders = mutableListOf<SmsProvider>()

    suspend fun getBroadcastJobs(): List<BroadcastJob> = requestContext {
        try {
            val res = apiService.getBroadcastJobs()
            localBroadcastJobs.clear()
            localBroadcastJobs.addAll(res)
            res
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createBroadcastJob(job: BroadcastJob): BroadcastJob = requestContext {
        try {
            val res = apiService.createBroadcastJob(job)
            localBroadcastJobs.add(0, res)
            res
        } catch (failure: Exception) { throw failure }
    }

    suspend fun sendBroadcastJob(jobId: String): Boolean = requestContext {
        try {
            apiService.sendBroadcastJob(jobId)
        } catch (failure: Exception) { throw failure }
        val idx = localBroadcastJobs.indexOfFirst { it.id == jobId }
        if (idx >= 0) {
            val current = localBroadcastJobs[idx]
            localBroadcastJobs[idx] = current.copy(
                status = "SENT",
                delivered_count = (current.total_recipients * 0.98).toInt()
            )
        }
        true
    }

    suspend fun getMessageTemplates(): List<MessageTemplate> = requestContext {
        try {
            val res = apiService.getMessageTemplates()
            localMessageTemplates.clear()
            localMessageTemplates.addAll(res)
            res
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createMessageTemplate(tpl: MessageTemplate): MessageTemplate = requestContext {
        try {
            val res = apiService.createMessageTemplate(tpl)
            localMessageTemplates.add(0, res)
            res
        } catch (failure: Exception) { throw failure }
    }

    suspend fun getSmsProviders(): List<SmsProvider> = requestContext {
        try {
            val res = apiService.getSmsProviders()
            localSmsProviders.clear()
            localSmsProviders.addAll(res)
            res
        } catch (failure: Exception) { throw failure }
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
    // Ödeme Hatırlatması ve iyzico Sanal POS
    // ==========================================
    suspend fun sendPaymentReminder(invoiceId: String, studentName: String): Boolean = requestContext {
        try {
            apiService.sendPaymentReminder(mapOf("invoice_id" to invoiceId, "student_name" to studentName))
        } catch (failure: Exception) { throw failure }
        true
    }

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
    private val localDebts = mutableListOf<Debt>()

    suspend fun getDebts(studentId: String? = null): List<Debt> = requestContext {
        try {
            val res = apiService.getDebts(studentId)
            localDebts.clear()
            localDebts.addAll(res)
            res
        } catch (failure: Exception) { throw failure }
    }

    suspend fun createDebt(debt: Debt): Debt = requestContext {
        try {
            apiService.createDebtRecord(debt)
        } catch (failure: Exception) { throw failure }
        localDebts.add(0, debt)
        debt
    }

    suspend fun updateDebt(debt: Debt): Debt = requestContext {
        try {
            apiService.updateDebtRecord(debt.id, debt)
        } catch (failure: Exception) { throw failure }
        val idx = localDebts.indexOfFirst { it.id == debt.id }
        if (idx >= 0) localDebts[idx] = debt else localDebts.add(debt)
        debt
    }

    suspend fun deleteDebt(id: String): Boolean = requestContext {
        try {
            apiService.deleteDebt(id)
        } catch (failure: Exception) { throw failure }
        localDebts.removeAll { it.id == id }
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
val DEFAULT_SEEDED_ROLES: List<RoleDefinition> = emptyList()
val DEFAULT_SEEDED_USERS: List<AppUser> = emptyList()
val DEFAULT_SEEDED_ANNOUNCEMENTS: List<Announcement> = emptyList()
val DEFAULT_SEEDED_RESOURCES: List<SchoolResource> = emptyList()
val DEFAULT_SEEDED_RFID_CARDS: List<RfidCard> = emptyList()
val DEFAULT_SEEDED_DEVICES: List<HardwareDevice> = emptyList()
val DEFAULT_SEEDED_EXPENSES: List<Expense> = emptyList()
