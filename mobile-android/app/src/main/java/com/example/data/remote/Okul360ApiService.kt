package com.example.data.remote

import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JvmSuppressWildcards
interface Okul360ApiService {

    // System Health & Ready
    @GET("health")
    suspend fun getHealth(): SystemHealth

    @GET("health")
    suspend fun getApiHealth(): SystemHealth

    @GET("health")
    suspend fun getApiV1Health(): SystemHealth

    @GET("ready")
    suspend fun getReady(): Map<String, Any>

    @GET("/")
    suspend fun getRoot(): Map<String, Any>

    // Authentication - JSON POST
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/login")
    suspend fun loginMap(@Body request: Map<String, String>): AuthResponse

    @POST("login")
    suspend fun loginDirect(@Body request: LoginRequest): AuthResponse

    @POST("login")
    suspend fun loginDirectMap(@Body request: Map<String, String>): AuthResponse

    @POST("auth/login")
    suspend fun loginAuth(@Body request: LoginRequest): AuthResponse

    @POST("auth/login")
    suspend fun loginAuthMap(@Body request: Map<String, String>): AuthResponse

    @POST("auth/jwt/login")
    suspend fun loginJwt(@Body request: LoginRequest): AuthResponse

    @POST("auth/jwt/login")
    suspend fun loginJwtMap(@Body request: Map<String, String>): AuthResponse

    // Authentication - Form URL Encoded
    @FormUrlEncoded
    @POST("auth/login")
    suspend fun loginFormApiAuthLogin(
        @Field("username") username: String,
        @Field("password") password: String
    ): AuthResponse

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun loginOAuth2TokenV1(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grantType: String = "password"
    ): AuthResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun loginOAuth2Token(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grantType: String = "password"
    ): AuthResponse

    @POST("auth/logout")
    suspend fun logout(): Map<String, Any>

    @GET("auth/me")
    suspend fun getCurrentUser(): User

    @GET("auth/me")
    suspend fun getCurrentUserV1(): User

    @GET("auth/me")
    suspend fun getCurrentUserAuth(): User

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): AuthResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: Map<String, String>): Map<String, Any>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>): Map<String, Any>

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: Map<String, String>): Map<String, Any>


    // Dashboard
    @GET("dashboard/summary")
    suspend fun getDashboardSummary(
        @Query("branch_id") branchId: String? = null
    ): DashboardSummary

    @GET("dashboard/summary")
    suspend fun getDashboardSummaryV1(
        @Query("branch_id") branchId: String? = null
    ): DashboardSummary

    @GET("dashboard/charts")
    suspend fun getDashboardCharts(): Map<String, Any>

    // Branches & Organizations & Schools
    @GET("branches")
    suspend fun getBranches(): List<Branch>

    @GET("branches")
    suspend fun getBranchesV1(): List<Branch>

    @GET("branches")
    suspend fun getBranchesDirect(): List<Branch>

    @GET("schools")
    suspend fun getSchools(): List<Branch>

    @GET("schools")
    suspend fun getSchoolsV1(): List<Branch>

    @GET("branches/{branch_id}")
    suspend fun getBranchById(@Path("branch_id") branchId: String): Branch

    @POST("branches")
    suspend fun createBranch(@Body branch: Branch): Branch

    @GET("organizations")
    suspend fun getOrganizationsRaw(): List<Map<String, Any>>

    @GET("organizations/{org_id}")
    suspend fun getOrganizationById(@Path("org_id") orgId: String): Map<String, Any>

    // Classes
    @GET("classes")
    suspend fun getClasses(
        @Query("branch_id") branchId: String? = null
    ): List<ClassGroup>

    @GET("classes")
    suspend fun getClassesV1(
        @Query("branch_id") branchId: String? = null
    ): List<ClassGroup>

    @GET("classes")
    suspend fun getClassesDirect(
        @Query("branch_id") branchId: String? = null
    ): List<ClassGroup>

    @GET("classes/{class_id}")
    suspend fun getClassById(@Path("class_id") classId: String): ClassGroup

    @POST("classes")
    suspend fun createClass(@Body classGroup: ClassGroup): ClassGroup

    @PUT("classes/{class_id}")
    suspend fun updateClass(
        @Path("class_id") classId: String,
        @Body classGroup: ClassGroup
    ): ClassGroup

    @DELETE("classes/{class_id}")
    suspend fun deleteClass(@Path("class_id") classId: String): Map<String, Any>

    @POST("classes/{class_id}/assign-students")
    suspend fun assignStudentsToClass(
        @Path("class_id") classId: String,
        @Body body: Map<String, Any>
    ): Map<String, Any>

    @GET("classes/{class_id}/occupancy")
    suspend fun getClassOccupancy(@Path("class_id") classId: String): Map<String, Any>

    @GET("classes/{class_id}/students")
    suspend fun getClassStudents(@Path("class_id") classId: String): List<Student>

    @GET("classes/{class_id}/students/{student_id}")
    suspend fun getClassStudentDetail(
        @Path("class_id") classId: String,
        @Path("student_id") studentId: String
    ): Student

    @GET("classes/{class_id}/teachers")
    suspend fun getClassTeachers(@Path("class_id") classId: String): List<AppUser>

    // Students
    @GET("students")
    suspend fun getStudents(
        @Query("branch_id") branchId: String? = null,
        @Query("class_id") classId: String? = null,
        @Query("search") search: String? = null
    ): List<Student>

    @GET("students")
    suspend fun getStudentsV1(
        @Query("branch_id") branchId: String? = null,
        @Query("class_id") classId: String? = null,
        @Query("search") search: String? = null
    ): List<Student>

    @GET("students")
    suspend fun getStudentsDirect(
        @Query("branch_id") branchId: String? = null,
        @Query("class_id") classId: String? = null,
        @Query("search") search: String? = null
    ): List<Student>

    @POST("students")
    suspend fun createStudent(@Body student: Student): Student

    @POST("students")
    suspend fun createStudentV1(@Body student: Student): Student

    @GET("students/{student_id}")
    suspend fun getStudentById(@Path("student_id") studentId: String): Student

    @PUT("students/{student_id}")
    suspend fun updateStudent(
        @Path("student_id") studentId: String,
        @Body student: Student
    ): Student

    @DELETE("students/{student_id}")
    suspend fun deleteStudent(@Path("student_id") studentId: String): Map<String, Any>

    @GET("students/{student_id}/attendance")
    suspend fun getStudentAttendance(@Path("student_id") studentId: String): List<AttendanceRecord>

    @GET("students/{student_id}/files")
    suspend fun getStudentFiles(@Path("student_id") studentId: String): List<StudentFile>

    @GET("students/{student_id}/parents")
    suspend fun getStudentParents(@Path("student_id") studentId: String): List<Map<String, Any>>

    @GET("students/{student_id}/payments")
    suspend fun getStudentPayments(@Path("student_id") studentId: String): List<Invoice>

    // Parents
    @GET("parents")
    suspend fun getParents(): List<Map<String, Any>>

    @GET("parents")
    suspend fun getParentsV1(): List<Map<String, Any>>

    @GET("parents/{parent_id}")
    suspend fun getParentById(@Path("parent_id") parentId: String): Map<String, Any>

    // Attendance
    @GET("attendance")
    suspend fun getAttendance(
        @Query("class_id") classId: String? = null,
        @Query("attendance_date") date: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100
    ): List<AttendanceRecord>

    @GET("attendance")
    suspend fun getAttendanceV1(
        @Query("class_id") classId: String? = null,
        @Query("date") date: String? = null
    ): List<AttendanceRecord>

    @GET("attendance")
    suspend fun getAttendanceDirect(
        @Query("class_id") classId: String? = null,
        @Query("date") date: String? = null
    ): List<AttendanceRecord>

    @POST("attendance")
    suspend fun recordSingleAttendance(@Body record: AttendanceRecord): AttendanceRecord

    @GET("attendance/{attendance_id}")
    suspend fun getAttendanceById(@Path("attendance_id") attendanceId: String): AttendanceRecord

    @PUT("attendance/bulk")
    suspend fun submitBulkAttendance(@Body request: BulkAttendanceSubmit): Map<String, Any>

    @GET("attendance/reports/monthly")
    suspend fun getMonthlyAttendanceReports(): Map<String, Any>

    // Announcements
    @GET("announcements")
    suspend fun getAnnouncements(
        @Query("branch_id") branchId: String? = null
    ): List<Announcement>

    @GET("announcements")
    suspend fun getAnnouncementsV1(
        @Query("branch_id") branchId: String? = null
    ): List<Announcement>

    @GET("announcements")
    suspend fun getAnnouncementsDirect(): List<Announcement>

    @POST("announcements")
    suspend fun createAnnouncement(@Body request: CreateAnnouncementRequest): Announcement

    @GET("announcements/{announcement_id}")
    suspend fun getAnnouncementById(@Path("announcement_id") announcementId: String): Announcement

    @PUT("announcements/{announcement_id}")
    suspend fun updateAnnouncement(
        @Path("announcement_id") announcementId: String,
        @Body announcement: Announcement
    ): Announcement

    @DELETE("announcements/{announcement_id}")
    suspend fun deleteAnnouncement(@Path("announcement_id") announcementId: String): Map<String, Any>

    @GET("announcements/{announcement_id}/results")
    suspend fun getAnnouncementResults(@Path("announcement_id") announcementId: String): Map<String, Any>

    @POST("announcements/{announcement_id}/retry")
    suspend fun retryAnnouncement(@Path("announcement_id") announcementId: String): Map<String, Any>

    @POST("announcements/{announcement_id}/send")
    suspend fun sendAnnouncement(@Path("announcement_id") announcementId: String): Map<String, Any>

    // Debts & Payments & Fees (Finance)
    @GET("debts")
    suspend fun getDebts(
        @Query("branch_id") branchId: String? = null,
        @Query("status") status: String? = null
    ): List<Invoice>

    @GET("debts")
    suspend fun getDebtsV1(
        @Query("branch_id") branchId: String? = null,
        @Query("status") status: String? = null
    ): List<Invoice>

    @GET("debts")
    suspend fun getDebtsDirect(
        @Query("branch_id") branchId: String? = null,
        @Query("status") status: String? = null
    ): List<Invoice>

    @GET("invoices")
    suspend fun getInvoices(
        @Query("branch_id") branchId: String? = null,
        @Query("status") status: String? = null
    ): List<Invoice>

    @GET("invoices")
    suspend fun getInvoicesV1(
        @Query("branch_id") branchId: String? = null,
        @Query("status") status: String? = null
    ): List<Invoice>

    @GET("invoices")
    suspend fun getInvoicesDirect(
        @Query("branch_id") branchId: String? = null,
        @Query("status") status: String? = null
    ): List<Invoice>

    @GET("fees")
    suspend fun getFees(): List<Invoice>

    @GET("fees")
    suspend fun getFeesV1(): List<Invoice>

    @POST("debts")
    suspend fun createDebt(@Body invoice: Invoice): Invoice

    @POST("debts")
    suspend fun createDebtV1(@Body invoice: Invoice): Invoice

    @POST("debts")
    suspend fun createDebtDirect(@Body invoice: Invoice): Invoice

    @GET("debts/{debt_id}")
    suspend fun getDebtById(@Path("debt_id") debtId: String): Invoice

    @PUT("debts/{debt_id}")
    suspend fun updateDebt(
        @Path("debt_id") debtId: String,
        @Body invoice: Invoice
    ): Invoice

    @DELETE("debts/{debt_id}")
    suspend fun deleteDebt(@Path("debt_id") debtId: String): Map<String, Any>

    @POST("debts/{debt_id}/recalculate-status")
    suspend fun recalculateDebtStatus(@Path("debt_id") debtId: String): Map<String, Any>

    @POST("debts/{debt_id}/pay")
    suspend fun payDebt(
        @Path("debt_id") debtId: String,
        @Body body: Map<String, Any>
    ): Map<String, Any>

    @POST("debts/{debt_id}/payments")
    suspend fun addDebtPayment(
        @Path("debt_id") debtId: String,
        @Body body: Map<String, Any>
    ): Map<String, Any>

    @GET("payments")
    suspend fun getPayments(
        @Query("branch_id") branchId: String? = null
    ): List<Invoice>

    @GET("payments")
    suspend fun getPaymentsV1(
        @Query("branch_id") branchId: String? = null
    ): List<Invoice>

    @GET("payments")
    suspend fun getPaymentsDirect(
        @Query("branch_id") branchId: String? = null
    ): List<Invoice>

    @POST("payments")
    suspend fun recordPayment(@Body request: RecordPaymentRequest): Invoice

    @POST("payments")
    suspend fun recordPaymentV1(@Body request: RecordPaymentRequest): Invoice

    @POST("payments")
    suspend fun recordPaymentDirect(@Body request: RecordPaymentRequest): Invoice

    @GET("payments/{payment_id}")
    suspend fun getPaymentById(@Path("payment_id") paymentId: String): Map<String, Any>

    @POST("payments/{payment_id}/cancel")
    suspend fun cancelPayment(@Path("payment_id") paymentId: String): Map<String, Any>

    // Receipts
    @GET("receipts")
    suspend fun getReceipts(): List<PaymentReceipt>

    @GET("receipts")
    suspend fun getReceiptsV1(): List<PaymentReceipt>

    @GET("receipts")
    suspend fun getReceiptsDirect(): List<PaymentReceipt>

    @GET("receipts/{receipt_id}")
    suspend fun getReceiptById(@Path("receipt_id") receiptId: String): PaymentReceipt

    // Expenses (Giderler)
    @GET("expenses")
    suspend fun getExpenses(
        @Query("branch_id") branchId: String? = null
    ): List<Expense>

    @GET("expenses")
    suspend fun getExpensesV1(
        @Query("branch_id") branchId: String? = null
    ): List<Expense>

    @POST("expenses")
    suspend fun createExpense(@Body expense: Expense): Expense

    @POST("expenses")
    suspend fun createExpenseV1(@Body expense: Expense): Expense

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): Map<String, Any>

    @DELETE("expenses/{id}")
    suspend fun deleteExpenseV1(@Path("id") id: String): Map<String, Any>

    @POST("receipts/{receipt_id}/send-email")
    suspend fun sendReceiptEmail(@Path("receipt_id") receiptId: String): Map<String, Any>

    @POST("receipts/{receipt_id}/send-sms")
    suspend fun sendReceiptSms(@Path("receipt_id") receiptId: String): Map<String, Any>

    @POST("receipts/{receipt_id}/send-whatsapp")
    suspend fun sendReceiptWhatsapp(@Path("receipt_id") receiptId: String): Map<String, Any>

    @GET("finance/receipts")
    suspend fun getFinanceReceipts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): List<PaymentReceipt>

    // Notifications
    @GET("notifications")
    suspend fun getNotifications(): List<OutboxNotificationLog>

    @GET("notifications")
    suspend fun getNotificationsV1(): List<OutboxNotificationLog>

    @POST("notifications/send")
    suspend fun sendNotification(@Body body: Map<String, String>): Map<String, Any>

    @DELETE("notifications/{notification_id}")
    suspend fun deleteNotification(@Path("notification_id") notificationId: String): Map<String, Any>

    @GET("notifications/{job_id}")
    suspend fun getNotificationJob(@Path("job_id") jobId: String): Map<String, Any>

    @POST("notifications/{job_id}/retry")
    suspend fun retryNotificationJob(@Path("job_id") jobId: String): Map<String, Any>

    @GET("notifications/templates")
    suspend fun getNotificationTemplates(): List<CommunicationTemplate>

    @GET("notifications/templates/{template_id}")
    suspend fun getNotificationTemplateById(@Path("template_id") templateId: String): CommunicationTemplate

    @POST("notifications/templates")
    suspend fun createTemplate(@Body template: CommunicationTemplate): CommunicationTemplate

    @PUT("notifications/templates/{template_id}")
    suspend fun updateTemplate(
        @Path("template_id") templateId: String,
        @Body template: CommunicationTemplate
    ): CommunicationTemplate

    @DELETE("notifications/templates/{template_id}")
    suspend fun deleteTemplate(@Path("template_id") templateId: String): Map<String, Any>

    // Daily Reports
    @GET("daily-reports")
    suspend fun getDailyReports(
        @Query("date") date: String? = null,
        @Query("class_id") classId: String? = null
    ): List<DailyReport>

    @GET("daily-reports")
    suspend fun getDailyReportsV1(
        @Query("date") date: String? = null,
        @Query("class_id") classId: String? = null
    ): List<DailyReport>

    @POST("daily-reports")
    suspend fun createDailyReport(@Body report: DailyReport): DailyReport

    @PUT("daily-reports/{report_id}")
    suspend fun updateDailyReport(
        @Path("report_id") reportId: String,
        @Body report: DailyReport
    ): DailyReport

    @DELETE("daily-reports/{report_id}")
    suspend fun deleteDailyReport(@Path("report_id") reportId: String): Map<String, Any>

    // School Resources
    @GET("resources")
    suspend fun getResources(
        @Query("branch_id") branchId: String? = null
    ): List<SchoolResource>

    @GET("resources")
    suspend fun getResourcesV1(
        @Query("branch_id") branchId: String? = null
    ): List<SchoolResource>

    @POST("resources")
    suspend fun createResource(@Body resource: SchoolResource): SchoolResource

    @PUT("resources/{resource_id}")
    suspend fun updateResource(
        @Path("resource_id") resourceId: String,
        @Body resource: SchoolResource
    ): SchoolResource

    @DELETE("resources/{resource_id}")
    suspend fun deleteResource(@Path("resource_id") resourceId: String): Map<String, Any>

    // Reports
    @GET("reports/attendance")
    suspend fun getAttendanceReport(): Map<String, Any>

    @GET("reports/branch-performance")
    suspend fun getBranchPerformanceReport(): Map<String, Any>

    @GET("reports/class-occupancy")
    suspend fun getClassOccupancyReport(): Map<String, Any>

    @GET("reports/debts")
    suspend fun getDebtsReport(): Map<String, Any>

    @GET("reports/monthly-payments")
    suspend fun getMonthlyPaymentsReport(): Map<String, Any>

    @GET("reports/overdue-debts")
    suspend fun getOverdueDebtsReport(): List<Invoice>

    @POST("reports/overdue-debts/remind")
    suspend fun remindOverdueDebts(@Body body: Map<String, Any>): Map<String, Any>

    // Roles, Users & Permissions
    @GET("roles")
    suspend fun getRoles(): List<RoleDefinition>

    @GET("roles")
    suspend fun getRolesV1(): List<RoleDefinition>

    @POST("roles")
    suspend fun createRole(@Body role: RoleDefinition): RoleDefinition

    @GET("roles/{role_id}")
    suspend fun getRoleById(@Path("role_id") roleId: String): RoleDefinition

    @PUT("roles/{role_id}")
    suspend fun updateRole(
        @Path("role_id") roleId: String,
        @Body role: RoleDefinition
    ): RoleDefinition

    @DELETE("roles/{role_id}")
    suspend fun deleteRole(@Path("role_id") roleId: String): Map<String, Any>

    @GET("roles/{role_id}/permissions")
    suspend fun getRolePermissions(@Path("role_id") roleId: String): List<String>

    @GET("permissions")
    suspend fun getPermissions(): List<String>

    @GET("users")
    suspend fun getUsers(): List<AppUser>

    @GET("users")
    suspend fun getUsersV1(): List<AppUser>

    @GET("users")
    suspend fun getUsersDirect(): List<AppUser>

    @GET("teachers")
    suspend fun getTeachers(): List<AppUser>

    @GET("teachers")
    suspend fun getTeachersV1(): List<AppUser>

    @POST("users")
    suspend fun createUser(@Body user: AppUser): AppUser

    @GET("users/{user_id}")
    suspend fun getUserById(@Path("user_id") userId: String): AppUser

    @PUT("users/{user_id}")
    suspend fun updateUser(
        @Path("user_id") userId: String,
        @Body user: AppUser
    ): AppUser

    @DELETE("users/{user_id}")
    suspend fun deleteUser(@Path("user_id") userId: String): Map<String, Any>

    @GET("users/{user_id}/branches")
    suspend fun getUserBranches(@Path("user_id") userId: String): List<Branch>

    @GET("users/{user_id}/roles")
    suspend fun getUserRoles(@Path("user_id") userId: String): List<RoleDefinition>

    @GET("settings")
    suspend fun getSettings(): Map<String, Any>

    @GET("settings/integrations")
    suspend fun getSettingsIntegrations(): Map<String, Any>

    // 1. Appointments (PTC)
    @GET("appointments")
    suspend fun getAppointments(
        @Query("teacher_id") teacherId: String? = null,
        @Query("parent_id") parentId: String? = null
    ): List<ParentTeacherAppointment>

    @GET("appointments")
    suspend fun getAppointmentsV1(
        @Query("teacher_id") teacherId: String? = null,
        @Query("parent_id") parentId: String? = null
    ): List<ParentTeacherAppointment>

    @POST("appointments/book")
    suspend fun bookAppointment(@Body appointment: ParentTeacherAppointment): ParentTeacherAppointment

    @PUT("appointments/{id}/status")
    suspend fun updateAppointmentStatus(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): ParentTeacherAppointment

    @GET("appointments/slots")
    suspend fun getTeacherAppointmentSlots(
        @Query("teacher_id") teacherId: String? = null
    ): List<TeacherAppointmentSlot>

    // 2. Transport & Bus Tracking
    @GET("transport/routes")
    suspend fun getBusRoutes(): List<BusRoute>

    @GET("transport/routes")
    suspend fun getBusRoutesV1(): List<BusRoute>

    @GET("transport/routes/{id}")
    suspend fun getBusRouteById(@Path("id") id: String): BusRoute

    @POST("transport/check-in")
    suspend fun submitBusStudentCheckIn(@Body body: Map<String, Any>): Map<String, Any>

    // 3. Canteen & Digital Wallet
    @GET("canteen/wallets/{student_id}")
    suspend fun getCanteenWallet(@Path("student_id") studentId: String): CanteenWallet

    @POST("canteen/wallets/{student_id}/topup")
    suspend fun topupCanteenWallet(
        @Path("student_id") studentId: String,
        @Body body: Map<String, Any>
    ): CanteenWallet

    @PUT("canteen/wallets/{student_id}/limits")
    suspend fun updateCanteenLimits(
        @Path("student_id") studentId: String,
        @Body body: Map<String, Any>
    ): CanteenWallet

    @GET("canteen/products")
    suspend fun getCanteenProducts(): List<CanteenProduct>

    @POST("canteen/pos/checkout")
    suspend fun processCanteenCheckout(@Body body: Map<String, Any>): CanteenTransaction

    // 4. Homework Submissions & Grading
    @GET("homework/submissions")
    suspend fun getHomeworkSubmissions(
        @Query("material_id") materialId: String? = null,
        @Query("student_id") studentId: String? = null
    ): List<HomeworkSubmission>

    @POST("homework/submissions")
    suspend fun submitHomework(@Body submission: HomeworkSubmission): HomeworkSubmission

    @PUT("homework/submissions/{id}/grade")
    suspend fun gradeHomeworkSubmission(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): HomeworkSubmission

    // Library wire operations are scoped and validated by NativeLibraryClient.

    // 6. Staff Duty Roster & Leave Requests
    @GET("staff/duty-rosters")
    suspend fun getTeacherDutyRosters(): List<TeacherDutyRoster>

    @POST("staff/duty-rosters")
    suspend fun createDutyRoster(@Body duty: TeacherDutyRoster): TeacherDutyRoster

    @GET("staff/leave-requests")
    suspend fun getStaffLeaveRequests(): List<TeacherLeaveRequest>

    @POST("staff/leave-requests")
    suspend fun submitStaffLeaveRequest(@Body request: TeacherLeaveRequest): TeacherLeaveRequest

    @PUT("staff/leave-requests/{id}/approval")
    suspend fun approveOrRejectLeaveRequest(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): TeacherLeaveRequest

    // Organizations
    @GET("organizations")
    suspend fun getOrganizations(): List<Organization>

    // RFID Cards & Hardware Devices
    @GET("rfid-cards")
    suspend fun getRfidCards(): List<RfidCard>

    @POST("rfid-cards")
    suspend fun createRfidCard(@Body card: RfidCard): RfidCard

    @GET("devices")
    suspend fun getDevices(): List<HardwareDevice>

    // Parent Portal Endpoints
    @GET("parent/my-children")
    suspend fun getParentChildren(): List<Student>

    @GET("parent/child/{student_id}/today-status")
    suspend fun getChildTodayStatus(@Path("student_id") studentId: String): Map<String, Any>

    @GET("parent/child/{student_id}/timeline")
    suspend fun getChildTimeline(@Path("student_id") studentId: String): List<Map<String, Any>>

    @GET("parent/child/{student_id}/attendance")
    suspend fun getChildAttendance(@Path("student_id") studentId: String): List<AttendanceRecord>

    @GET("parent/child/{student_id}/grades")
    suspend fun getChildGrades(@Path("student_id") studentId: String): List<Map<String, Any>>

    @GET("parent/child/{student_id}/schedule")
    suspend fun getChildSchedule(@Path("student_id") studentId: String): List<Map<String, Any>>

    @GET("parent/child/{student_id}/homework")
    suspend fun getChildHomework(@Path("student_id") studentId: String): List<HomeworkSubmission>

    @GET("parent/child/{student_id}/financial-invoices")
    suspend fun getChildFinancialInvoices(@Path("student_id") studentId: String): List<Invoice>

    @POST("parent/child/{student_id}/excuse-request")
    suspend fun submitParentExcuseRequest(
        @Path("student_id") studentId: String,
        @Body body: Map<String, Any>
    ): Map<String, Any>

    @POST("parent/child/{student_id}/medicine-requests")
    suspend fun submitParentMedicineRequest(
        @Path("student_id") studentId: String,
        @Body body: Map<String, Any>
    ): Map<String, Any>

    @POST("parent/payments/checkout-session")
    suspend fun createParentCheckoutSession(@Body body: Map<String, Any>): Map<String, Any>

    // Classes & Attendance Extensions
    @POST("attendance/mark-all-present")
    suspend fun markAllAttendancePresent(@Body body: Map<String, Any>): Map<String, Any>

    @POST("classes/{class_id}/transfer-students")
    suspend fun transferStudents(
        @Path("class_id") classId: String,
        @Body body: Map<String, Any>
    ): Map<String, Any>

    // Canteen Orders & Products
    @GET("canteen/orders")
    suspend fun getCanteenOrders(@Query("student_id") studentId: String? = null): List<CanteenTransaction>

    @POST("canteen/products")
    suspend fun createCanteenProduct(@Body body: CanteenProduct): CanteenProduct

    @PUT("canteen/products/{product_id}")
    suspend fun updateCanteenProduct(
        @Path("product_id") productId: String,
        @Body body: CanteenProduct
    ): CanteenProduct

    @DELETE("canteen/products/{product_id}")
    suspend fun deleteCanteenProduct(@Path("product_id") productId: String): Map<String, Any>

    @GET("card-numbers")
    suspend fun getCardNumbers(): List<Map<String, Any>>

    // Payment & Finance Extensions
    @POST("payments/finances/payments/collect")
    suspend fun collectPayment(@Body body: Map<String, Any>): Map<String, Any>

    @GET("timetable")
    suspend fun getTimetable(@Query("class_id") classId: String? = null): List<Map<String, Any>>

    @GET("bus")
    suspend fun getBuses(): List<BusRoute>

    // --- Ek Modüller (SCS360 Eksiksiz API Seti) ---
    // 1. Veli İzin & Muvafakatnameler (Consents)
    @GET("consents")
    suspend fun getConsents(@Query("student_id") studentId: String? = null): List<StudentConsent>

    @POST("consents")
    suspend fun createConsent(@Body consent: StudentConsent): StudentConsent

    @PUT("consents/{id}")
    suspend fun updateConsent(@Path("id") id: String, @Body consent: StudentConsent): StudentConsent

    @POST("consents/{id}/approve")
    suspend fun approveConsent(@Path("id") id: String, @Body body: Map<String, String>): StudentConsent

    // 2. Rehberlik & PDR (Guidance)
    @GET("guidance")
    suspend fun getGuidanceRecords(@Query("student_id") studentId: String? = null): List<GuidanceRecord>

    @POST("guidance")
    suspend fun createGuidanceRecord(@Body record: GuidanceRecord): GuidanceRecord

    @DELETE("guidance/{id}")
    suspend fun deleteGuidanceRecord(@Path("id") id: String): Map<String, Any>

    // 3. Revir & Sağlık (Infirmary)
    @GET("infirmary")
    suspend fun getInfirmaryVisits(@Query("student_id") studentId: String? = null): List<InfirmaryVisit>

    @POST("infirmary")
    suspend fun createInfirmaryVisit(@Body visit: InfirmaryVisit): InfirmaryVisit

    // 4. Öğrenci Kulüpleri (Clubs)
    @GET("clubs")
    suspend fun getClubs(): List<SchoolClub>

    @POST("clubs")
    suspend fun createClub(@Body club: SchoolClub): SchoolClub

    @POST("clubs/{id}/enroll")
    suspend fun enrollInClub(@Path("id") clubId: String, @Body body: Map<String, String>): Map<String, Any>

    // 5. Anketler (Surveys)
    @GET("surveys")
    suspend fun getSurveys(): List<SchoolSurvey>

    @POST("surveys")
    suspend fun createSurvey(@Body survey: SchoolSurvey): SchoolSurvey

    @POST("surveys/{id}/participate")
    suspend fun participateSurvey(@Path("id") surveyId: String, @Body body: Map<String, Any>): Map<String, Any>

    // 6. Birebir Etüt (Tutoring)
    @GET("tutoring")
    suspend fun getTutoringSessions(@Query("student_id") studentId: String? = null): List<TutoringSession>

    @POST("tutoring")
    suspend fun createTutoringSession(@Body session: TutoringSession): TutoringSession

    @PUT("tutoring/{id}")
    suspend fun updateTutoringSession(@Path("id") id: String, @Body session: TutoringSession): TutoringSession

    // 7. Maaş & Bordro (Payroll)
    @GET("payroll")
    suspend fun getPayrollRecords(@Query("period") period: String? = null): List<PayrollRecord>

    @POST("payroll")
    suspend fun createPayrollRecord(@Body payroll: PayrollRecord): PayrollRecord

    // 8. Güvenlik & Denetim Logları (Security & Audit)
    @GET("security/audit-logs")
    suspend fun getAuditLogs(): List<AuditLogEntry>

    // 9. Excel İçe/Dışa Aktarma
    @POST("students/import-excel")
    suspend fun importStudentsExcel(@Body body: Map<String, Any>): Map<String, Any>

    @GET("students/export-excel")
    suspend fun exportStudentsExcel(): Map<String, Any>

    // 10. Sınavlar (Exams)
    @GET("exams")
    suspend fun getExams(): List<ExamItem>

    @POST("exams")
    suspend fun createExam(@Body exam: ExamItem): ExamItem

    @PUT("exams/{id}")
    suspend fun updateExam(@Path("id") id: String, @Body exam: ExamItem): ExamItem

    @DELETE("exams/{id}")
    suspend fun deleteExam(@Path("id") id: String): Map<String, Any>

    // 11. İletişim, Outbox & Şablonlar (Communications)
    @GET("communications/broadcast-jobs")
    suspend fun getBroadcastJobs(): List<BroadcastJob>

    @POST("communications/broadcast-jobs")
    suspend fun createBroadcastJob(@Body job: BroadcastJob): BroadcastJob

    @POST("communications/broadcast-jobs/{id}/send")
    suspend fun sendBroadcastJob(@Path("id") id: String): Map<String, Any>

    @GET("communications/message-templates")
    suspend fun getMessageTemplates(): List<MessageTemplate>

    @POST("communications/message-templates")
    suspend fun createMessageTemplate(@Body template: MessageTemplate): MessageTemplate

    @GET("communications/sms-providers")
    suspend fun getSmsProviders(): List<SmsProvider>

    // 12. Veli Portalı İlaç & İzin (Parent Requests)
    @GET("parent/child/{id}/medicine-requests")
    suspend fun getChildMedicineRequests(@Path("id") childId: String): List<MedicineRequest>

    @POST("parent/child/{id}/medicine-requests")
    suspend fun requestChildMedicine(@Path("id") childId: String, @Body req: MedicineRequest): MedicineRequest

    @GET("parent/child/{id}/excuse-requests")
    suspend fun getChildExcuseRequests(@Path("id") childId: String): List<ExcuseRequest>

    @POST("parent/child/{id}/excuse-request")
    suspend fun requestChildExcuse(@Path("id") childId: String, @Body req: ExcuseRequest): ExcuseRequest

    // 13. Finans Taksit & iyzico Ödeme
    @POST("payments/finances/invoices/generate-plan")
    suspend fun generateInstallmentPlan(@Body req: InstallmentPlanRequest): Map<String, Any>

    @POST("payments/finances/reminders/send")
    suspend fun sendPaymentReminder(@Body body: Map<String, String>): Map<String, Any>

    @POST("payments/iyzico/initialize")
    suspend fun initializeIyzicoPayment(@Body body: Map<String, Any>): Map<String, Any>

    // 14. Borçlar & Alacaklar (Debts - /api/debts)
    @GET("debts")
    suspend fun getDebts(@Query("student_id") studentId: String? = null): List<Debt>

    @POST("debts")
    suspend fun createDebtRecord(@Body debt: Debt): Debt

    @PUT("debts/{id}")
    suspend fun updateDebtRecord(@Path("id") id: String, @Body debt: Debt): Debt

    // 15. Teslimat Raporları (Delivery Reports)
    @GET("communications/delivery-reports")
    suspend fun getDeliveryReports(@Query("broadcast_job_id") broadcastJobId: String? = null): List<DeliveryReport>

    // 16. Öğrenci Evrak & Dosyaları (/api/students/{id}/files)
    @POST("students/{id}/files")
    suspend fun uploadStudentFile(@Path("id") studentId: String, @Body file: StudentFile): StudentFile

    // 17. SaaS Platform Yönetimi & Kurum Üyelikleri (/api/platform & /api/subscription)
    @GET("platform/plans")
    suspend fun getPlatformPlans(): List<SubscriptionPlan>

    @POST("platform/plans")
    suspend fun createPlatformPlan(@Body plan: SubscriptionPlan): SubscriptionPlan

    @GET("platform/tenants")
    suspend fun getPlatformTenants(): List<PlatformTenantSummary>

    @POST("platform/tenants")
    suspend fun createTenantWithOwner(@Body body: Map<String, @JvmSuppressWildcards Any>): PlatformTenantSummary

    @PUT("platform/tenants/{orgId}/subscription")
    suspend fun updateTenantSubscription(@Path("orgId") orgId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): PlatformTenantSummary

    @PATCH("platform/tenants/{orgId}/status")
    suspend fun updateTenantStatus(@Path("orgId") orgId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): PlatformTenantSummary

    @POST("platform/invoices/{invoiceId}/verify-payment")
    suspend fun verifyPlatformInvoicePayment(@Path("invoiceId") invoiceId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): SubscriptionInvoice

    @GET("subscription/status")
    suspend fun getOrganizationAccessStatus(): OrganizationAccessStatus

    @GET("subscription/invoices")
    suspend fun getSubscriptionInvoices(): List<SubscriptionInvoice>

    @POST("subscription/renewal-request")
    suspend fun createRenewalInvoiceRequest(@Body body: Map<String, @JvmSuppressWildcards Any>): SubscriptionInvoice

    // 18. Öğrenci Teslim Masası & Kapı Güvenliği (/api/dismissal & /api/security)
    @GET("dismissal/authorizations")
    suspend fun getPickupAuthorizations(): List<PickupAuthorization>

    @POST("dismissal/authorizations")
    suspend fun createPickupAuthorization(@Body auth: PickupAuthorization): PickupAuthorization

    @POST("dismissal/authorizations/{id}/cancel")
    suspend fun cancelPickupAuthorization(@Path("id") id: String): PickupAuthorization

    @GET("dismissal/logs")
    suspend fun getDismissalLogs(): List<DismissalLog>

    @POST("dismissal/handover")
    suspend fun recordStudentHandover(@Body body: Map<String, @JvmSuppressWildcards Any>): DismissalLog

    @GET("security/id-cards")
    suspend fun getStudentIdCards(): List<StudentIdCardRecord>

    @POST("security/id-cards/{studentId}/issue")
    suspend fun issueStudentIdCard(@Path("studentId") studentId: String): StudentIdCardRecord

    @POST("security/id-cards/{cardId}/revoke")
    suspend fun revokeStudentIdCard(@Path("cardId") cardId: String): StudentIdCardRecord

    @GET("security/gate-logs")
    suspend fun getGateAccessLogs(): List<GateAccessLog>

    @POST("security/gate-verify")
    suspend fun verifyGateAccess(@Body body: Map<String, @JvmSuppressWildcards Any>): GateAccessLog

    // 19. Kullanıcı Atomik Rol ve Şube Erişimi (/api/users/{id}/access)
    @PUT("users/{id}/access")
    suspend fun updateUserAccess(@Path("id") userId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): AppUser

    // 20. Veli & Aile Yönetimi (/api/parents & /api/students/{id}/parents)
    @POST("parents")
    suspend fun createParent(@Body parent: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PUT("parents/{parent_id}")
    suspend fun updateParent(@Path("parent_id") parentId: String, @Body parent: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @DELETE("parents/{parent_id}")
    suspend fun deleteParent(@Path("parent_id") parentId: String): Map<String, Any>

    @POST("students/{student_id}/parents")
    suspend fun linkStudentParent(@Path("student_id") studentId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @DELETE("students/{student_id}/parents/{parent_id}")
    suspend fun unlinkStudentParent(@Path("student_id") studentId: String, @Path("parent_id") parentId: String): Map<String, Any>

    // 21. Sınıf Öğrenci & Öğretmen Atama (/api/classes/{id}/students & /api/classes/{id}/teachers)
    @POST("classes/{class_id}/students")
    suspend fun assignStudentToClass(@Path("class_id") classId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @DELETE("classes/{class_id}/students/{student_id}")
    suspend fun removeStudentFromClass(@Path("class_id") classId: String, @Path("student_id") studentId: String): Map<String, Any>

    @POST("classes/{class_id}/teachers")
    suspend fun assignTeacherToClass(@Path("class_id") classId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    // 22. Yoklama Güncelleme & Silme (/api/attendance/{id})
    @PUT("attendance/{attendance_id}")
    suspend fun updateAttendanceRecord(@Path("attendance_id") attendanceId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): AttendanceRecord

    @DELETE("attendance/{attendance_id}")
    suspend fun deleteAttendanceRecord(@Path("attendance_id") attendanceId: String): Map<String, Any>

    // 23. Kurucu / Yönetici Özel Paneli (/api/v1/owner)
    @GET("v1/owner/dashboard/overview")
    suspend fun getOwnerDashboardOverview(): Map<String, Any>

    @GET("v1/owner/branches/metrics-comparison")
    suspend fun getBranchMetricsComparison(): List<Map<String, Any>>

    @GET("v1/owner/financial-trends")
    suspend fun getOwnerFinancialTrends(): Map<String, Any>

    @POST("v1/ai/owner/financial-forecast")
    suspend fun aiOwnerFinancialForecast(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("v1/ai/owner/churn-risk-analysis")
    suspend fun aiOwnerChurnRiskAnalysis(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("v1/ai/admin/branch-health-digest")
    suspend fun aiAdminBranchHealthDigest(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("v1/ai/admin/anomaly-detection")
    suspend fun aiAdminAnomalyDetection(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("v1/ai/teacher/generate-daily-narrative")
    suspend fun aiTeacherGenerateDailyNarrative(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("v1/ai/teacher/learning-gap-report")
    suspend fun aiTeacherLearningGapReport(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("v1/ai/teacher/generate-homework")
    suspend fun aiTeacherGenerateHomework(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("v1/ai/parent/weekly-pedagogical-summary")
    suspend fun aiParentWeeklyPedagogicalSummary(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("v1/ai/parent/homework-coach")
    suspend fun aiParentHomeworkCoach(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    // 24. Genel Dashboard & Rapor Rotaları (/api/dashboard & /api/reports/export)
    @GET("dashboard/overview")
    suspend fun getDashboardOverview(): Map<String, Any>

    @GET("dashboard/recent-activity")
    suspend fun getDashboardRecentActivity(@Query("limit") limit: Int = 10): List<Map<String, Any>>

    @GET("dashboard/upcoming-events")
    suspend fun getDashboardUpcomingEvents(@Query("limit") limit: Int = 5): Map<String, Any>

    @GET("dashboard/financial-summary")
    suspend fun getDashboardFinancialSummary(): Map<String, Any>

    @GET("dashboard/attendance-summary")
    suspend fun getDashboardAttendanceSummary(): Map<String, Any>

    @GET("reports/export/csv")
    suspend fun exportReportsCsv(): okhttp3.ResponseBody

    @GET("reports/export/excel")
    suspend fun exportReportsExcel(): okhttp3.ResponseBody

    // 25. Sistem Ayarları & Entegrasyonlar (/api/settings)
    @PUT("settings/integrations")
    suspend fun updateIntegrationsSettings(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PUT("settings")
    suspend fun updateGeneralSettings(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("settings/{key}")
    suspend fun getSettingByKey(@Path("key") key: String): Map<String, Any>

    @POST("settings")
    suspend fun createSetting(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PUT("settings/{key}")
    suspend fun updateSettingByKey(@Path("key") key: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @DELETE("settings/{key}")
    suspend fun deleteSettingByKey(@Path("key") key: String): Map<String, Any>

    // 26. Gecikmiş Borçlar (/api/debts/overdue)
    @GET("debts/overdue")
    suspend fun getOverdueDebts(): List<Invoice>

    // 27. İlaç Takip & Kantin Sipariş & Diyet Kısıtlamaları (/api/canteen & /api/parent/child)
    @POST("parent/child/{student_id}/medicine-request")
    suspend fun createChildMedicineRequest(@Path("student_id") studentId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("canteen/orders/{order_id}")
    suspend fun getCanteenOrderDetail(@Path("order_id") orderId: String): Map<String, Any>

    @POST("canteen/dietary-restrictions")
    suspend fun createCanteenDietaryRestriction(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @DELETE("canteen/dietary-restrictions/{restriction_id}")
    suspend fun deleteCanteenDietaryRestriction(@Path("restriction_id") restrictionId: String): Map<String, Any>

    @POST("canteen/student/{student_id}/wallet/top-up")
    suspend fun topUpCanteenWalletAlias(@Path("student_id") studentId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("canteen/dietary-restrictions")
    suspend fun listCanteenDietaryRestrictions(): List<Map<String, Any>>

    @GET("canteen/student/{student_id}/wallet")
    suspend fun getCanteenStudentWallet(@Path("student_id") studentId: String): Map<String, Any>

    @GET("canteen/wallet/{student_id}")
    suspend fun getCanteenWalletByStudentId(@Path("student_id") studentId: String): Map<String, Any>

    @POST("canteen/wallet/topup")
    suspend fun topUpCanteenWalletGeneral(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PUT("canteen/wallet/restrictions")
    suspend fun updateCanteenWalletRestrictions(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("canteen/pos/purchase")
    suspend fun processCanteenPosPurchase(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("canteen/transactions")
    suspend fun listCanteenTransactions(): List<Map<String, Any>>

    // 28. İyzico & Ödeme Makbuzları (/api/payments)
    @POST("payments/iyzico/callback")
    suspend fun callbackIyzicoPayment(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("payments/finances/payments/{payment_id}/receipt-pdf")
    suspend fun getFinancesPaymentReceiptPdf(@Path("payment_id") paymentId: String): okhttp3.ResponseBody

    @PUT("payments/{payment_id}")
    suspend fun updatePaymentRecord(@Path("payment_id") paymentId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @DELETE("payments/{payment_id}")
    suspend fun deletePaymentRecord(@Path("payment_id") paymentId: String): Map<String, Any>

    // 29. Toplu İletişim & SMS Sağlayıcıları (/api/communications)
    @POST("communications/sms-providers")
    suspend fun createSmsProvider(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PUT("communications/broadcast-jobs/{job_id}")
    suspend fun updateBroadcastJob(@Path("job_id") jobId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @DELETE("communications/broadcast-jobs/{job_id}")
    suspend fun deleteBroadcastJob(@Path("job_id") jobId: String): Map<String, Any>

    @GET("communications/broadcast-jobs/{job_id}/delivery-summary")
    suspend fun getBroadcastJobDeliverySummary(@Path("job_id") jobId: String): Map<String, Any>

    // 30. Servis & Ulaşım (/api/bus & /api/transport)
    @POST("bus")
    suspend fun createBusEntity(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("bus/{bus_id}")
    suspend fun getBusDetail(@Path("bus_id") busId: String): Map<String, Any>

    @POST("transport/routes")
    suspend fun createBusTransportRoute(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PUT("transport/routes/{route_id}/telemetry")
    suspend fun updateBusRouteTelemetry(@Path("route_id") routeId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("transport/routes/{route_id}/checkin")
    suspend fun recordBusStudentCheckIn(@Path("route_id") routeId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    // 31. Envanter & Ders Materyalleri & Sınavlar (/api/inventory & /api/materials & /api/exams)
    @GET("inventory")
    suspend fun listInventoryItems(): List<Map<String, Any>>

    @POST("inventory")
    suspend fun createInventoryItem(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("materials")
    suspend fun listMaterials(): List<Map<String, Any>>

    @POST("materials")
    suspend fun createMaterialItem(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("materials/{material_id}")
    suspend fun getMaterialDetail(@Path("material_id") materialId: String): Map<String, Any>

    @GET("exams/{exam_id}")
    suspend fun getExamDetail(@Path("exam_id") examId: String): Map<String, Any>

    // 32. Anlık Mesajlaşma (/api/messages)
    @GET("messages")
    suspend fun listMessages(): List<Map<String, Any>>

    @POST("messages")
    suspend fun sendDirectMessage(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    // 34. Ders Programı
    @GET("timetable/{class_id}")
    suspend fun getTimetableForClassId(@Path("class_id") classId: String): List<Map<String, Any>>

    @GET("classes/{class_id}/schedule")
    suspend fun getClassSchedule(@Path("class_id") classId: String): Map<String, Any>

    @PUT("classes/{class_id}/schedule")
    suspend fun updateClassSchedule(@Path("class_id") classId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("teachers/{teacher_id}/schedule")
    suspend fun getTeacherSchedule(@Path("teacher_id") teacherId: String): Map<String, Any>

    // 35. Dosya Yönetimi & Makbuzlar (/api/files & /api/receipts)
    @POST("files/upload")
    suspend fun uploadGenericFile(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("files/{file_path}")
    suspend fun getGenericFile(@Path("file_path", encoded = true) filePath: String): okhttp3.ResponseBody

    @DELETE("files/{file_path}")
    suspend fun deleteGenericFile(@Path("file_path", encoded = true) filePath: String): Map<String, Any>

    @DELETE("students/{student_id}/files/{file_id}")
    suspend fun deleteStudentFileAttachment(@Path("student_id") studentId: String, @Path("file_id") fileId: String): Map<String, Any>

    @GET("students/{student_id}/files/{file_id}/download")
    suspend fun downloadStudentFileAttachment(@Path("student_id") studentId: String, @Path("file_id") fileId: String): okhttp3.ResponseBody

    @GET("receipts/{receipt_id}/pdf")
    suspend fun downloadReceiptPdf(@Path("receipt_id") receiptId: String): okhttp3.ResponseBody

    @GET("receipts/{receipt_id}/print")
    suspend fun printReceiptHtml(@Path("receipt_id") receiptId: String): okhttp3.ResponseBody

    // 36. Veli-Öğretmen Randevuları (PTC) (/api/appointments)
    @POST("appointments")
    suspend fun createPtcAppointment(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PATCH("appointments/{appointment_id}/status")
    suspend fun updatePtcAppointmentStatus(@Path("appointment_id") appointmentId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    // 37. Donanım & RFID Cihaz Entegrasyonu (/api/hardware & /api/devices & /api/rfid-cards)
    @POST("hardware/card/balance")
    suspend fun checkHardwareCardBalance(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("hardware/payment/process")
    suspend fun processHardwareCardPayment(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("hardware/wifi/card-read")
    suspend fun recordHardwareWifiCardRead(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("hardware/wifi/card-reads")
    suspend fun getHardwareWifiCardReads(): List<Map<String, Any>>

    @GET("rfid-cards/number/{card_number}")
    suspend fun getRfidCardByCardNumber(@Path("card_number") cardNumber: String): RfidCard

    @GET("rfid-cards/{card_id}")
    suspend fun getRfidCardDetail(@Path("card_id") cardId: String): RfidCard

    @PUT("rfid-cards/{card_id}")
    suspend fun updateRfidCardRecord(@Path("card_id") cardId: String, @Body card: RfidCard): RfidCard

    @DELETE("rfid-cards/{card_id}")
    suspend fun deleteRfidCardRecord(@Path("card_id") cardId: String): Map<String, Any>

    @POST("devices")
    suspend fun createHardwareDeviceRecord(@Body device: HardwareDevice): HardwareDevice

    @PUT("devices/{device_id}")
    suspend fun updateHardwareDeviceRecord(@Path("device_id") deviceId: String, @Body device: HardwareDevice): HardwareDevice

    @DELETE("devices/{device_id}")
    suspend fun deleteHardwareDeviceRecord(@Path("device_id") deviceId: String): Map<String, Any>

    // 38. Ödev & Teslimat (/api/homework)
    @GET("homework")
    suspend fun listHomeworkAssignments(): List<Map<String, Any>>

    @POST("homework")
    suspend fun createHomeworkAssignment(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("homework/{homework_id}/submit")
    suspend fun submitHomeworkAssignment(@Path("homework_id") homeworkId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    // 39. Personel İzin Talepleri İnceleme (/api/staff/leave-requests)
    @PUT("staff/leave-requests/{request_id}/review")
    suspend fun reviewStaffLeaveRequest(@Path("request_id") requestId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    // 40. Toplu Günlük Rapor (/api/daily-reports/bulk)
    @POST("daily-reports/bulk")
    suspend fun createBulkDailyReports(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    // 41. Veli Bildirimleri (İlaç, İzin, Teslimat) (/api/parent/notices)
    @GET("parent/notices")
    suspend fun listParentNotices(): List<Map<String, Any>>

    @POST("parent/notices")
    suspend fun createParentNoticeItem(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PATCH("parent/notices/{notice_id}/status")
    suspend fun updateParentNoticeStatus(@Path("notice_id") noticeId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("parent/notices/medicine")
    suspend fun listParentMedicineNotices(): List<Map<String, Any>>

    @POST("parent/notices/medicine")
    suspend fun createParentMedicineNotice(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("parent/notices/leave")
    suspend fun listParentLeaveNotices(): List<Map<String, Any>>

    @POST("parent/notices/leave")
    suspend fun createParentLeaveNotice(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("parent/notices/pickup")
    suspend fun listParentPickupNotices(): List<Map<String, Any>>

    @POST("parent/notices/pickup")
    suspend fun createParentPickupNotice(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PATCH("parent/notices/{notice_type}/{notice_id}/status")
    suspend fun updateTypedParentNoticeStatus(@Path("notice_type") noticeType: String, @Path("notice_id") noticeId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    // 42. Finans Analitiği (/api/finance/analytics)
    @GET("finance/analytics")
    suspend fun getFinanceAnalytics(): Map<String, Any>

    // 43. Outbox Kuyruğu & Bildirim Yönetimi (/api/outbox & /api/notifications)
    @GET("outbox")
    suspend fun listOutboxLogs(): List<OutboxNotificationLog>

    @POST("outbox/retry/{outbox_id}")
    suspend fun retryOutboxNotification(@Path("outbox_id") outboxId: String): Map<String, Any>

    @POST("webhooks/sms-status")
    suspend fun webhookSmsStatusUpdate(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("notifications/unread-count")
    suspend fun getUnreadNotificationsCount(): Map<String, Any>

    @PATCH("notifications/mark-all-read")
    suspend fun markAllNotificationsRead(): Map<String, Any>

    @PATCH("users/{user_id}/fcm-token")
    suspend fun updateUserFcmToken(@Path("user_id") userId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PUT("users/{user_id}/avatar")
    suspend fun updateUserAvatarUrl(@Path("user_id") userId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    // 44. Rol & Yetki Modülleri (/api/users/{id}/roles & /api/permissions/modules)
    @POST("users/{user_id}/roles")
    suspend fun assignUserRoles(@Path("user_id") userId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("users/{user_id}/branches")
    suspend fun assignUserBranches(@Path("user_id") userId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @POST("roles/{role_id}/permissions")
    suspend fun assignRolePermissions(@Path("role_id") roleId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PUT("roles/{role_id}/permissions")
    suspend fun updateRolePermissionsAssignment(@Path("role_id") roleId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @GET("permissions/modules")
    suspend fun listPermissionModules(): List<Map<String, Any>>

    // 45. Organizasyon & Şube Yönetimi (/api/organizations & /api/branches)
    @POST("organizations")
    suspend fun createOrganization(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @PUT("organizations/{org_id}")
    suspend fun updateOrganization(@Path("org_id") orgId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Any>

    @DELETE("organizations/{org_id}")
    suspend fun deleteOrganization(@Path("org_id") orgId: String): Map<String, Any>

    @PUT("branches/{branch_id}")
    suspend fun updateBranchRecord(@Path("branch_id") branchId: String, @Body branch: Branch): Branch

    @DELETE("branches/{branch_id}")
    suspend fun deleteBranchRecord(@Path("branch_id") branchId: String): Map<String, Any>
}

class AuthAndTenantInterceptor : Interceptor {
    private data class Identity(val token: String? = null, val tenant: String? = null, val branch: String? = null, val workspace: String? = null) {
        override fun toString() = "RequestIdentity(redacted)"
    }
    private val identity = java.util.concurrent.atomic.AtomicReference(Identity())
    var authToken: String?
        get() = identity.get().token
        set(value) { identity.updateAndGet { it.copy(token = value) } }
    var tenantId: String?
        get() = identity.get().tenant
        set(value) { identity.updateAndGet { it.copy(tenant = value) } }
    var branchId: String?
        get() = identity.get().branch
        set(value) { identity.updateAndGet { it.copy(branch = value) } }
    var workspaceOrganizationId: String?
        get() = identity.get().workspace
        set(value) { identity.updateAndGet { it.copy(workspace = value) } }
    fun setSession(token: String?, tenant: String? = null, branch: String? = null, workspace: String? = null) {
        identity.set(Identity(token, tenant, branch, workspace))
    }
    internal fun owns(snapshot: Any) = identity.get() === snapshot
    fun invalidateContext() { identity.updateAndGet { it.copy() } }
    fun captureScope() = NativeRequestScope(this, identity.get(), ApiClientProvider.activeBaseUrl)

    override fun intercept(chain: Interceptor.Chain): Response {
        val snapshot = identity.get()
        val base = ApiClientProvider.activeBaseUrl
        val scope = chain.request().tag(NativeRequestScope::class.java) ?: captureScope()
        if (!scope.matches(snapshot, base)) throw NativeContextChangedException()
        val expected = java.net.URI(base)
        val target = chain.request().url
        val expectedPort = if (expected.port != -1) expected.port else if (expected.scheme == "https") 443 else 80
        if (!target.host.equals(expected.host, ignoreCase = true) || target.scheme != expected.scheme || target.port != expectedPort) {
            throw NativeContextChangedException()
        }
        val request = chain.request().newBuilder().header("Accept", "application/json")
            .removeHeader("Authorization").removeHeader("X-SCS360-Organization-Id")
            .removeHeader("X-Tenant-ID").removeHeader("X-Branch-ID")
        snapshot.token?.takeIf { it.isNotBlank() }?.let { request.header("Authorization", "Bearer $it") }
        snapshot.workspace?.takeIf { it.isNotBlank() }?.let { request.header("X-SCS360-Organization-Id", it) }
        val response = chain.proceed(request.build())
        if (!owns(snapshot) || base != ApiClientProvider.activeBaseUrl) {
            response.close()
            throw NativeContextChangedException()
        }
        return response
    }
}

class UnwrapApiResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!response.isSuccessful) return response

        val body = response.body ?: return response
        val contentType = body.contentType()
        val mediaTypeString = contentType?.toString() ?: ""
        if (!mediaTypeString.contains("json", ignoreCase = true)) {
            return response
        }

        val bodyString = body.string()
        if (!bodyString.startsWith("{")) {
            val newBody = okhttp3.ResponseBody.create(contentType, bodyString)
            return response.newBuilder().body(newBody).build()
        }

        try {
            val json = org.json.JSONObject(bodyString)
            if (json.has("success") && !json.optBoolean("success", false)) {
                throw java.io.IOException("Sunucu işlemi onaylamadı")
            }
            // If response is wrapped by FastAPI success_response: { "success": true, "data": ... }
            if (json.has("success") && json.has("data") && !json.isNull("data")) {
                val dataObj = json.get("data")
                val unwrappedString = dataObj.toString()
                val newBody = okhttp3.ResponseBody.create(contentType, unwrappedString)
                return response.newBuilder().body(newBody).build()
            }
        } catch (rejected: java.io.IOException) {
            throw rejected
        } catch (_: Exception) {}

        val newBody = okhttp3.ResponseBody.create(contentType, bodyString)
        return response.newBuilder().body(newBody).build()
    }
}

object ApiClientProvider {
    val DEFAULT_BASE_URL = com.example.BuildConfig.API_BASE_URL
    @Volatile var activeBaseUrl: String = DEFAULT_BASE_URL
        private set
    val interceptor = AuthAndTenantInterceptor()

    val moshi: Moshi = Moshi.Builder()
        .add(StringCoercingAdapter())
        .add(DoubleCoercingAdapter())
        .add(IntCoercingAdapter())
        .add(FlexibleListJsonAdapterFactory())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .addInterceptor(UnwrapApiResponseInterceptor())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .build()

    fun createService(baseUrl: String = DEFAULT_BASE_URL): Okul360ApiService {
        val clean = baseUrl.trim().trimEnd('/')
        val normalizedUrl = if (clean.endsWith("/api")) "$clean/" else "$clean/api/"
        val endpoint = java.net.URI(normalizedUrl)
        require(endpoint.host != null && endpoint.userInfo == null && endpoint.query == null && endpoint.fragment == null &&
            (endpoint.scheme == "https" || com.example.BuildConfig.DEBUG && endpoint.scheme == "http")) { "Geçerli HTTPS API adresi gerekli" }
        if (activeBaseUrl != normalizedUrl) interceptor.setSession(null)
        activeBaseUrl = normalizedUrl
        val client = createOkHttpClient()
        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .callFactory { request ->
                if (normalizedUrl != activeBaseUrl) throw NativeContextChangedException()
                val scope = NativeRequestScope.bound() ?: interceptor.captureScope()
                scope.ensureNetworkCurrent()
                client.newCall(request.newBuilder().tag(NativeRequestScope::class.java, scope).build())
            }
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(Okul360ApiService::class.java)
    }
}
