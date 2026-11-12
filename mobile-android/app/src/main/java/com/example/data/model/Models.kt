package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class UserRole(val displayName: String, val badgeColorHex: Long) {
    SUPERADMIN("Süper Admin", 0xFF6D28D9),
    OWNER("Kurum Sahibi", 0xFF4338CA),
    ADMIN("Yönetici / Okul Müdürü", 0xFF1E40AF),
    MANAGER("Şube Müdürü", 0xFF0D9488),
    TEACHER("Öğretmen", 0xFF059669),
    FINANCE("Muhasebe & Finans", 0xFFD97706),
    PARENT("Veli", 0xFF7C3AED),
    CANTEEN("Kantin Personeli", 0xFFEA580C);

    val title: String get() = displayName
}

@JsonClass(generateAdapter = true)
data class UserRoleInfo(val slug: String = "", val is_active: Boolean = true)

@JsonClass(generateAdapter = true)
data class User(
    val id: String = "",
    val full_name: String = "Kullanıcı",
    val first_name: String? = null,
    val last_name: String? = null,
    val email: String = "",
    val role: String = "",
    val is_super_admin: Boolean? = null,
    val tenant_id: String? = null,
    val organization_id: String? = null,
    val institution_name: String = "",
    val branch_id: String = "",
    val branch_name: String = "",
    val branch_ids: List<String>? = null,
    val phone: String? = null,
    val avatar_url: String? = null,
    val roles: List<UserRoleInfo> = emptyList(),
    val permissions: List<String> = emptyList()
) {
    val resolvedName: String
        get() = when {
            full_name.isNotBlank() && full_name != "Kullanıcı" -> full_name
            !first_name.isNullOrBlank() || !last_name.isNullOrBlank() -> "${first_name.orEmpty()} ${last_name.orEmpty()}".trim()
            email.isNotBlank() -> email.substringBefore("@")
            else -> "Kullanıcı"
        }

    val resolvedEmail: String
        get() = email

    val resolvedRole: String
        get() {
            if (is_super_admin == true) return "SUPERADMIN"
            val slugs = roles.filter { it.is_active }.map { it.slug.lowercase(java.util.Locale.ROOT) }
            val known = listOf("owner" to "OWNER", "admin" to "ADMIN", "branch_manager" to "MANAGER", "teacher" to "TEACHER", "finance" to "FINANCE", "parent" to "PARENT", "canteen" to "CANTEEN")
            return known.firstOrNull { it.first in slugs }?.second ?: role.trim().uppercase(java.util.Locale.ROOT).ifBlank { "UNASSIGNED" }
        }

    val resolvedBranchId: String
        get() = branch_id.ifBlank { branch_ids?.firstOrNull().orEmpty() }

    val resolvedBranchName: String
        get() = branch_name.ifBlank { "Şube seçilmedi" }

    val resolvedInstitutionName: String
        get() = institution_name.ifBlank { "Kurum bilgisi yüklenmedi" }

    val resolvedTenantId: String?
        get() = tenant_id ?: organization_id
}

@JsonClass(generateAdapter = true)
data class AuthData(
    val access_token: String? = null,
    val accessToken: String? = null,
    val token: String? = null,
    val jwt: String? = null,
    val user: User? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String? = null,
    val username: String? = null,
    val password: String = ""
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val access_token: String? = null,
    val accessToken: String? = null,
    val token: String? = null,
    val jwt: String? = null,
    val auth_token: String? = null,
    val refresh_token: String? = null,
    val refreshToken: String? = null,
    val token_type: String? = "bearer",
    val user: User? = null,
    val data: AuthData? = null,
    val result: AuthData? = null,
    val message: String? = null,
    val detail: String? = null,
    val status: String? = null
) {
    val resolvedToken: String?
        get() = access_token ?: accessToken ?: token ?: jwt ?: auth_token ?: data?.access_token ?: data?.accessToken ?: data?.token ?: data?.jwt ?: result?.access_token ?: result?.accessToken ?: result?.token ?: result?.jwt

    val resolvedUser: User?
        get() = user ?: data?.user ?: result?.user
}

@JsonClass(generateAdapter = true)
data class Branch(
    val id: String = "",
    val organization_id: String = "",
    val name: String = "",
    val code: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String? = null,
    val student_count: Int = 0,
    val class_count: Int = 0,
    val is_active: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ClassGroup(
    val id: String = "",
    val name: String = "",
    val grade_level: String = "",
    val branch_id: String = "",
    val teacher_name: String = "",
    val student_count: Int = 0,
    val capacity: Int = 0,
    val room_number: String = "",
    val teacher_id: String? = null,
    val is_active: Boolean = true,
    val organization_id: String = "",
    val code: String = "",
    val academic_year: String = "",
    val description: String? = null,
    val registry_snapshot: String? = null
)

enum class AttendanceStatus(val label: String, val shortCode: String, val colorHex: Long) {
    PRESENT("Geldi", "G", 0xFF059669),
    ABSENT("Gelmedi", "Y", 0xFFDC2626),
    LATE("Geç Kaldı", "GK", 0xFFD97706),
    EXCUSED("İzinli / Raporlu", "İ", 0xFF2563EB),
    HALF_DAY("Yarım Gün", "YG", 0xFF7C3AED)
}

@JsonClass(generateAdapter = true)
data class AttendanceRecord(
    val id: String = "",
    val student_id: String = "",
    val student_name: String = "",
    val class_id: String = "",
    @Json(name = "attendance_date") val date: String = "",
    val status: String = "UNRECORDED",
    @Json(name = "notes") val note: String? = null,
    val version: String? = null,
    val check_in_time: String? = null,
    val check_out_time: String? = null,
    val temperature_celsius: Double? = null,
    val parent_notified: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class BulkAttendanceSubmit(
    @Json(name = "attendance_date") val date: String = "",
    val class_id: String = "",
    val records: List<AttendanceRecordSubmit> = emptyList(),
    val request_id: String = ""
)

@JsonClass(generateAdapter = true)
data class AttendanceRecordSubmit(
    val student_id: String = "",
    val status: String = "PRESENT",
    @Json(name = "notes") val note: String? = null,
    val expected_version: String? = null
)

@JsonClass(generateAdapter = true)
data class Student(
    val id: String = "",
    val student_number: String = "",
    val first_name: String = "",
    val last_name: String = "",
    val class_id: String = "",
    val class_name: String = "",
    val branch_id: String = "",
    val academic_year: String = "",
    val birth_date: String = "",
    val enrollment_date: String? = null,
    val gender: String = "",
    val address: String? = null,
    val notes: String? = null,
    val health_notes: String? = null,
    val parent_name: String = "",
    val parent_phone: String = "",
    val parent_email: String? = null,
    val linked_parent_user_id: String? = null,
    val receive_notifications: Boolean = true,
    val sms_phone: String? = null,
    val whatsapp_phone: String? = null,
    val status: String = "ACTIVE", // ACTIVE, INACTIVE, TRIAL
    val allergies_or_diet: String? = null,
    val blood_type: String? = null,
    val avatar_color_hex: Long = 0xFF3B82F6,
    val tuition_balance: Double = 0.0,
    val today_attendance_status: String? = null,
    val organization_id: String = "",
    val registry_snapshot: String? = null
) {
    val full_name: String get() = if (first_name.isNotBlank() || last_name.isNotBlank()) "$first_name $last_name".trim() else "Öğrenci"
}

enum class InvoiceStatus(val label: String, val colorHex: Long) {
    PAID("Ödendi", 0xFF059669),
    PENDING("Bekliyor", 0xFFD97706),
    OVERDUE("Gecikmede", 0xFFDC2626)
}

@JsonClass(generateAdapter = true)
data class Invoice(
    val id: String = "",
    val invoice_number: String = "",
    val student_id: String = "",
    val student_name: String = "",
    val class_name: String = "",
    val amount: Double = 0.0,
    val paid_amount: Double = 0.0,
    val due_date: String = "",
    val issue_date: String = "",
    val status: String = "",
    val description: String = "",
    val payment_method: String? = null // CREDIT_CARD, BANK_TRANSFER, CASH
)

@JsonClass(generateAdapter = true)
data class RecordPaymentRequest(
    val invoice_id: String = "",
    val amount: Double = 0.0,
    val payment_method: String = "CREDIT_CARD",
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class FinanceSummary(
    val total_invoiced_monthly: Double = 0.0,
    val total_collected_monthly: Double = 0.0,
    val pending_balance: Double = 0.0,
    val overdue_balance: Double = 0.0,
    val collection_rate_percentage: Double = 0.0,
    val student_paying_count: Int = 0,
    val total_expenses_monthly: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class Expense(
    val id: String = "exp-1",
    val branch_id: String = "branch-1",
    val category: String = "Kira", // Kira, Mutfak, Kırtasiye, Fatura, Maaş, Diğer
    val title: String = "",
    val amount: Double = 0.0,
    val date: String = "2026-08-20",
    val receipt_url: String? = null,
    val created_by: String = "Muhasebe"
)

@JsonClass(generateAdapter = true)
data class Announcement(
    val id: String = "ann-1",
    val title: String = "",
    val content: String = "",
    val category: String = "GENERAL", // GENERAL, ACADEMIC, EVENT, FINANCIAL, EMERGENCY
    val target_audience: String = "ALL", // ALL, PARENTS, TEACHERS, BRANCH
    val author_name: String = "Yönetim",
    val author_role: String = "Yönetici",
    val created_at: String = "2026-08-20",
    val is_pinned: Boolean = false,
    val is_draft: Boolean = false,
    val draft_last_modified: String? = null,
    val read_count: Int = 0
)

@JsonClass(generateAdapter = true)
data class NotificationItem(
    val id: String = "ntf-1",
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL", // ATTENDANCE, FINANCE, GENERAL, EVENT
    val created_at: String = "09:00",
    val is_read: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CreateAnnouncementRequest(
    val title: String = "",
    val content: String = "",
    val category: String = "GENERAL",
    val target_audience: String = "ALL",
    val is_pinned: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DailyReport(
    val id: String = "dr-1",
    val student_id: String = "",
    val student_name: String = "",
    val class_name: String = "",
    val date: String = "2026-08-20",
    val breakfast_status: String = "Tümünü Yedi",
    val lunch_status: String = "Tümünü Yedi",
    val snack_status: String = "Tümünü Yedi",
    val sleep_duration_minutes: Int = 90,
    val mood_level: String = "Çok Neşeli & Uyumlu",
    val activities_done: List<String> = listOf("Robotik Kodlama", "Resim & El Becerisi", "Bahçe Oyunları"),
    val teacher_note: String = "Bugün etkinliklere çok hevesli katıldı, arkadaşlarıyla paylaşımları harikaydı.",
    val teacher_name: String = "Ayşe Öğretmen"
)

@JsonClass(generateAdapter = true)
data class DashboardSummary(
    val institution_name: String = "SCS360",
    val branch_name: String = "Tüm Şubeler",
    val active_students_count: Int = 0,
    val active_students: Int? = null,
    val total_students: Int? = null,
    val present_today_count: Int = 0,
    val present_today: Int? = null,
    val absent_today_count: Int = 0,
    val absent_today: Int? = null,
    val today_absent: Int? = null,
    val late_today_count: Int = 0,
    val attendance_rate: Double = 0.0,
    val staff_count: Int = 0,
    val total_branches_count: Int = 0,
    val total_branches: Int? = null,
    val total_classes: Int? = null,
    val monthly_revenue: Double = 0.0,
    val monthly_collection: String? = null,
    val total_payments: Double? = null,
    val pending_payments_count: Int = 0,
    val pending_debt: String? = null,
    val overdue_payments_count: Int = 0,
    val overdue_debt: String? = null,
    val total_debt: Double? = null,
    val outbox_notifications_sent_today: Int = 0,
    val recent_announcements: List<Announcement> = emptyList()
) {
    val resolvedActiveStudents: Int
        get() = active_students ?: total_students ?: active_students_count

    val resolvedPresentToday: Int
        get() = present_today ?: present_today_count

    val resolvedAbsentToday: Int
        get() = today_absent ?: absent_today ?: absent_today_count

    val resolvedTotalBranches: Int
        get() = total_branches ?: total_branches_count

    val resolvedTotalClasses: Int
        get() = total_classes ?: 0

    val resolvedMonthlyRevenue: Double
        get() = monthly_collection?.toDoubleOrNull() ?: total_payments ?: monthly_revenue

    val resolvedPendingDebt: Double
        get() = pending_debt?.toDoubleOrNull() ?: total_debt ?: 0.0

    val resolvedOverdueDebt: Double
        get() = overdue_debt?.toDoubleOrNull() ?: 0.0
}

@JsonClass(generateAdapter = true)
data class OutboxNotificationLog(
    val id: String = "log-1",
    val channel: String = "SMS", // SMS, PUSH, EMAIL
    val recipient: String = "",
    val subject: String = "",
    val status: String = "SENT", // SENT, QUEUED, DELIVERED, FAILED
    val sent_at: String = "2026-08-20 09:00",
    val tenant_id: String? = null
)

@JsonClass(generateAdapter = true)
data class SystemHealth(
    val api_status: String = "ONLINE",
    val database_connection: String = "PostgreSQL Connected (Pool: 12/20)",
    val latency_ms: Long = 28,
    val outbox_worker_status: String = "RUNNING (Active Jobs: 0)",
    val tenant_id: String? = null,
    val subscription_tier: String = "Enterprise Franchise Edition",
    val server_time: String = "2026-08-20 09:37:42"
)

@JsonClass(generateAdapter = true)
data class SchoolResource(
    val id: String = "res-1",
    val name: String = "",
    val type: String = "ROOM", // ROOM, VEHICLE, DEVICE, LAB, SPORT
    val branch_id: String = "branch-1",
    val capacity_or_serial: String = "",
    val status: String = "ACTIVE", // ACTIVE, MAINTENANCE, INACTIVE
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class RoleDefinition(
    val id: String = "role-1",
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val permissions: List<String> = emptyList(),
    val is_system: Boolean = false,
    val badge_color_hex: Long = 0xFF1E40AF
)

@JsonClass(generateAdapter = true)
data class AppUser(
    val id: String = "usr-1",
    val full_name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "TEACHER",
    val role_display_name: String = "Öğretmen",
    val branch_id: String = "branch-1",
    val branch_name: String = "Merkez Kampüs",
    val is_active: Boolean = true,
    val created_at: String = "2026-01-15",
    val password: String = "123456"
)

@JsonClass(generateAdapter = true)
data class CommunicationTemplate(
    val id: String = "tmpl-1",
    val title: String = "",
    val channel: String = "SMS", // SMS, PUSH, EMAIL
    val content: String = "",
    val category: String = "GENERAL" // ATTENDANCE, PAYMENT, EVENT, GENERAL
)

@JsonClass(generateAdapter = true)
data class PaymentReceipt(
    val receipt_number: String = "",
    val invoice_id: String = "",
    val invoice_number: String = "",
    val student_id: String = "",
    val student_name: String = "",
    val student_number: String = "",
    val class_name: String = "",
    val parent_name: String = "",
    val parent_phone: String = "",
    val amount: Double = 0.0,
    val payment_method: String = "",
    val payment_date: String = "",
    val description: String = "",
    val collected_by: String = "",
    val institution_name: String = "",
    val branch_name: String = "",
    val qr_code_text: String = ""
)

data class ReportsData(
    val monthly_collections: List<MonthlyCollectionItem> = emptyList(),
    val debtors: List<DebtorItem> = emptyList(),
    val overdue_debts: List<Invoice> = emptyList(),
    val attendance_rates: List<AttendanceRateItem> = emptyList(),
    val class_occupancies: List<ClassOccupancyItem> = emptyList(),
    val branch_performances: List<BranchPerformanceItem> = emptyList()
) {
    val total_collected: Double get() = monthly_collections.sumOf { it.collected }
    val total_pending: Double get() = monthly_collections.sumOf { (it.invoiced - it.collected).coerceAtLeast(0.0) }
    val overall_attendance_rate: Double get() = if (attendance_rates.isNotEmpty()) attendance_rates.map { it.attendance_rate }.average() / 100.0 else 0.95
    val class_occupancy_rate: Double get() = if (class_occupancies.isNotEmpty()) class_occupancies.map { it.occupancy_rate }.average() / 100.0 else 0.88
}

data class MonthlyCollectionItem(val month: String = "", val invoiced: Double = 0.0, val collected: Double = 0.0)
data class DebtorItem(val student_name: String = "", val class_name: String = "", val debt_amount: Double = 0.0, val days_overdue: Int = 0, val parent_phone: String = "")
data class AttendanceRateItem(val class_name: String = "", val attendance_rate: Double = 0.0, val present_count: Int = 0, val total_count: Int = 0)
data class ClassOccupancyItem(val class_name: String = "", val current_students: Int = 0, val capacity: Int = 20, val occupancy_rate: Double = 0.0)
data class BranchPerformanceItem(val branch_name: String = "", val student_count: Int = 0, val collection_rate: Double = 0.0, val attendance_rate: Double = 0.0)

@JsonClass(generateAdapter = true)
data class MedicineNotice(
    val id: String = "med-1",
    val student_id: String = "",
    val student_name: String = "",
    val medicine_name: String = "",
    val dose: String = "",
    val time_to_give: String = "13:00",
    val instructions: String = "",
    val date: String = "2026-08-20",
    val status: String = "PENDING", // PENDING, GIVEN
    val given_by: String? = null
)

@JsonClass(generateAdapter = true)
data class LeaveNotice(
    val id: String = "leave-1",
    val student_id: String = "",
    val student_name: String = "",
    val start_date: String = "2026-08-20",
    val end_date: String = "2026-08-21",
    val reason_type: String = "ILLNESS", // ILLNESS, VACATION, SPECIAL_REASON
    val parent_note: String = "",
    val status: String = "APPROVED" // APPROVED, PENDING, REJECTED
)

@JsonClass(generateAdapter = true)
data class PickupNotice(
    val id: String = "pk-1",
    val student_id: String = "",
    val student_name: String = "",
    val authorized_person_name: String = "",
    val relationship: String = "ANNE", // ANNE, BABA, BUYUKANNE, SERVIS, DIGER
    val phone: String = "",
    val date: String = "2026-08-20",
    val note: String? = null
)

data class MealMenuItem(
    val meal_type: String = "Sabah Kahvaltısı", // Sabah Kahvaltısı, Öğle Yemeği, İkindi Beslenmesi
    val items: List<String> = emptyList(),
    val time_range: String = "09:00 - 09:30",
    val calories: String = "320 kcal"
)

@JsonClass(generateAdapter = true)
data class ExamItem(
    val id: String = "exam-1",
    val title: String = "",
    val course_name: String = "",
    val exam_type: String = "YAZILI", // YAZILI, DENEME, QUIZ, PROJE, ODEV
    val date: String = "2026-08-25",
    val time: String = "09:20 - 10:00",
    val classroom: String = "Derslik 101",
    val class_name: String = "8-A",
    val teacher_name: String = "Öğretmen",
    val duration_minutes: Int = 40,
    val topics: List<String> = emptyList(),
    val weight_percentage: Int = 40,
    val is_completed: Boolean = false
) {
    val typeDisplayName: String get() = when (exam_type.uppercase()) {
        "YAZILI" -> "Yazılı Sınav"
        "DENEME" -> "Deneme Sınavı"
        "QUIZ" -> "Quiz / Kısa Sınav"
        "PROJE" -> "Proje Teslimi"
        "ODEV" -> "Ödev Teslimi"
        else -> "Sınav"
    }

    val typeColorHex: Long get() = when (exam_type.uppercase()) {
        "YAZILI" -> 0xFFDC2626 // Red
        "DENEME" -> 0xFF2563EB // Blue
        "QUIZ" -> 0xFFD97706 // Amber
        "PROJE" -> 0xFF7C3AED // Purple
        "ODEV" -> 0xFF059669 // Emerald
        else -> 0xFF475569
    }
}

@JsonClass(generateAdapter = true)
data class TimetableSlot(
    val id: String = "slot-1",
    val period_number: Int = 1,
    val day_of_week: Int = 1, // 1: Pazartesi, 2: Salı, 3: Çarşamba, 4: Perşembe, 5: Cuma, 6: Cumartesi
    val day_name: String = "Pazartesi",
    val start_time: String = "08:30",
    val end_time: String = "09:10",
    val subject_name: String = "Matematik",
    val subject_code: String = "MAT",
    val teacher_name: String = "Mustafa Yılmaz",
    val room_name: String = "Derslik 101",
    val color_hex: Long = 0xFF2563EB,
    val class_id: String = "class-1",
    val class_name: String = "5-A",
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class MaterialAcknowledgment(
    val student_id: String = "",
    val student_name: String = "",
    val parent_name: String = "",
    val parent_phone: String = "",
    val acknowledged_at: String = "2026-08-25 14:30"
)

@JsonClass(generateAdapter = true)
data class CourseMaterial(
    val id: String = "mat-1",
    val title: String = "",
    val description: String = "",
    val subject_name: String = "Matematik",
    val class_name: String = "8-A",
    val material_type: String = "NOTE", // NOTE, HOMEWORK, WORKSHEET, PRESENTATION, EXAM_PREP
    val file_name: String = "matematik_cebir_konu_anlatimi.pdf",
    val file_extension: String = "PDF", // PDF, DOCX, PPTX, MP4, ZIP
    val file_size_display: String = "2.4 MB",
    val teacher_id: String = "t-1",
    val teacher_name: String = "Ahmet Yılmaz",
    val created_at: String = "2026-08-22",
    val due_date: String? = null,
    val download_count: Int = 28,
    val is_required: Boolean = false,
    val preview_pages_count: Int = 8,
    val topic_summary: String = "1. Dereceden Denklemler, Eşitsizlikler ve Koordinat Sistemi Özet Föyü",
    val parent_acknowledged_student_ids: List<String> = emptyList(),
    val acknowledgment_history: List<MaterialAcknowledgment> = emptyList(),
    val is_acknowledged_by_current_parent: Boolean = false,
    val acknowledged_at_display: String? = null,
    val target_students_count: Int = 24
) {
    val acknowledgedCount: Int get() = parent_acknowledged_student_ids.size

    val typeDisplayName: String get() = when (material_type.uppercase()) {
        "NOTE" -> "📚 Ders Notu"
        "HOMEWORK" -> "📝 Ödev Dökümanı"
        "WORKSHEET" -> "📄 Çalışma Föyü"
        "PRESENTATION" -> "🔬 Sunum & Slayt"
        "EXAM_PREP" -> "🎯 Sınav Hazırlık Föyü"
        else -> "📁 Materyal"
    }

    val typeColorHex: Long get() = when (material_type.uppercase()) {
        "NOTE" -> 0xFF2563EB // Blue
        "HOMEWORK" -> 0xFFD97706 // Amber
        "WORKSHEET" -> 0xFF059669 // Emerald
        "PRESENTATION" -> 0xFF7C3AED // Purple
        "EXAM_PREP" -> 0xFFDC2626 // Red
        else -> 0xFF475569
    }
}

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val id: String = "msg-1",
    val conversation_id: String = "conv-1",
    val sender_id: String = "parent-1",
    val sender_name: String = "Mehmet Demir",
    val sender_role: String = "PARENT", // PARENT, TEACHER, MANAGER
    val recipient_id: String = "teacher-1",
    val recipient_name: String = "Ahmet Yılmaz (Sınıf Öğretmeni)",
    val text: String = "",
    val timestamp: String = "14:25",
    val date: String = "Bugün",
    val is_me: Boolean = false,
    val is_delivered: Boolean = true,
    val is_read: Boolean = true,
    val read_timestamp: String? = "14:26",
    val attachment_name: String? = null,
    val attachment_type: String? = null, // PDF, IMAGE, AUDIO
    val audio_duration_seconds: Int? = null
)

@JsonClass(generateAdapter = true)
data class ChatConversation(
    val id: String = "conv-1",
    val participant_id: String = "t-1",
    val participant_name: String = "Ahmet Yılmaz",
    val participant_role: String = "Matematik & 8-A Sınıf Öğretmeni",
    val student_context: String = "Ali Demir (8-A)",
    val last_message: String = "Ödev teslimi için teşekkürler, inceleyip notları sisteme gireceğim.",
    val last_message_time: String = "14:25",
    val unread_count: Int = 0,
    val is_online: Boolean = true,
    val office_hours: String = "08:30 - 17:30",
    val is_within_office_hours: Boolean = true,
    val avatar_color_hex: Long = 0xFF2563EB
)

@JsonClass(generateAdapter = true)
data class StudentTermSubjectGrade(
    val student_id: String = "std-1",
    val student_number: String = "101",
    val student_name: String = "Ali Demir",
    val exam1: Double = 88.0,
    val exam2: Double = 92.0,
    val quiz_or_homework: Double = 95.0,
    val participation: Double = 90.0,
    val project: Double = 92.0,
    val total_attendance_days: Int = 45,
    val present_days: Int = 44,
    val absent_days: Int = 1,
    val late_days: Int = 0,
    val attendance_rate: Double = 97.8
) {
    val termAverage: Double get() = (exam1 + exam2 + quiz_or_homework + participation + project) / 5.0
    val letterGrade: String get() = when {
        termAverage >= 85.0 -> "AA"
        termAverage >= 75.0 -> "BA"
        termAverage >= 70.0 -> "BB"
        termAverage >= 60.0 -> "CB"
        termAverage >= 50.0 -> "CC"
        termAverage >= 40.0 -> "DC"
        else -> "FF"
    }
}

// ==========================================
// 1. Veli - Öğretmen Randevu Sistemi (PTC)
// ==========================================
enum class AppointmentType(val label: String, val iconEmoji: String) {
    FACE_TO_FACE("Yüz Yüze Görüşme", "🏫"),
    ONLINE_MEET("Online Video Görüşme", "💻"),
    PHONE_CALL("Telefonla Görüşme", "📞")
}

enum class AppointmentStatus(val label: String, val colorHex: Long) {
    PENDING("Onay Bekliyor", 0xFFD97706),
    APPROVED("Onaylandı", 0xFF059669),
    REJECTED("Reddedildi", 0xFFDC2626),
    COMPLETED("Tamamlandı", 0xFF2563EB),
    CANCELLED("İptal Edildi", 0xFF64748B)
}

@JsonClass(generateAdapter = true)
data class TeacherAppointmentSlot(
    val id: String = "slot-1",
    val teacher_id: String = "t-1",
    val teacher_name: String = "Ahmet Yılmaz",
    val subject: String = "Matematik",
    val day_name: String = "Salı",
    val date: String = "2026-08-28",
    val time_slot: String = "15:30 - 16:00",
    val is_booked: Boolean = false,
    val booked_by_parent_name: String? = null
)

@JsonClass(generateAdapter = true)
data class ParentTeacherAppointment(
    val id: String = "apt-1",
    val teacher_id: String = "t-1",
    val teacher_name: String = "Ahmet Yılmaz",
    val teacher_subject: String = "Matematik",
    val parent_id: String = "p-1",
    val parent_name: String = "Mehmet Demir",
    val parent_phone: String = "+90 532 555 1234",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val class_name: String = "8-A",
    val date: String = "2026-08-28",
    val time_slot: String = "15:30 - 16:00",
    val appointment_type: String = "FACE_TO_FACE", // FACE_TO_FACE, ONLINE_MEET, PHONE_CALL
    val meeting_link: String? = "https://meet.google.com/okul-360-ptc",
    val location_room: String? = "2. Kat Görüşme Odası 204",
    val status: String = "APPROVED", // PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED
    val parent_topic_notes: String = "Ali'nin 1. dönem LGS matematik soru çözüm stratejisi ve deneme netleri hakkında.",
    val teacher_feedback_notes: String? = null,
    val created_at: String = "2026-08-25 10:15"
)

// ==========================================
// 2. Servis & Ulaşım Takip Modülü (Transport)
// ==========================================
enum class BusStatus(val label: String, val colorHex: Long) {
    IDLE("Garajda / Parkta", 0xFF64748B),
    MORNING_ROUTE("Sabah Servisinde", 0xFF059669),
    EVENING_ROUTE("Akşam Dönüşünde", 0xFFD97706),
    COMPLETED("Güzergah Tamamlandı", 0xFF2563EB)
}

@JsonClass(generateAdapter = true)
data class BusStudentEntry(
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val class_name: String = "8-A",
    val stop_name: String = "Ataşehir Palmiye Konutları",
    val stop_order: Int = 3,
    val morning_boarded: Boolean = true,
    val morning_boarded_at: String? = "07:42",
    val evening_boarded: Boolean = false,
    val evening_boarded_at: String? = null,
    val parent_name: String = "Mehmet Demir",
    val parent_phone: String = "0532 555 1234"
)

@JsonClass(generateAdapter = true)
data class BusRoute(
    val id: String = "bus-1",
    val route_name: String = "1. Bölge Ataşehir - Batı Ataşehir Servisi",
    val plate_number: String = "34 OKL 360",
    val driver_name: String = "Kemal Usta",
    val driver_phone: String = "+90 533 111 2233",
    val hostess_name: String = "Fatma Hanım",
    val hostess_phone: String = "+90 535 444 5566",
    val capacity: Int = 18,
    val current_status: String = "MORNING_ROUTE", // IDLE, MORNING_ROUTE, EVENING_ROUTE, COMPLETED
    val current_latitude: Double = 40.9928,
    val current_longitude: Double = 29.1125,
    val current_speed_kmh: Int = 38,
    val last_stop_passed: String = "Kamelya Sokak Girişi",
    val next_stop_name: String = "Ataşehir Palmiye Konutları",
    val eta_minutes_to_next_stop: Int = 4,
    val students: List<BusStudentEntry> = emptyList()
) {
    val boardedCount: Int get() = students.count { it.morning_boarded || it.evening_boarded }
    val totalStudents: Int get() = students.size
}

// ==========================================
// 3. Dijital Kantin & Öğrenci Cüzdanı (Canteen)
// ==========================================
@JsonClass(generateAdapter = true)
data class CanteenProduct(
    val id: String = "prd-1",
    val name: String = "Kaşarlı Tost",
    val category: String = "Sıcak Yiyecek", // Sıcak Yiyecek, İçecek, Meyve & Atıştırmalık, Tatlı
    val price: Double = 45.0,
    val is_healthy: Boolean = true,
    val allergens: List<String> = listOf("Gluten", "Laktoz"),
    val calories: Int = 320,
    val stock_count: Int = 50
)

@JsonClass(generateAdapter = true)
data class CanteenTransaction(
    val id: String = "ctx-1",
    val student_id: String = "std-1",
    val amount: Double = 45.0,
    val transaction_type: String = "PURCHASE", // TOPUP, PURCHASE, REFUND
    val items_summary: String = "1x Kaşarlı Tost + 1x Ayran",
    val date_time: String = "Bugün 12:35",
    val balance_after: Double = 355.0
)

@JsonClass(generateAdapter = true)
data class CanteenWallet(
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val current_balance: Double = 400.0,
    val daily_spending_limit: Double = 100.0,
    val today_spent: Double = 45.0,
    val is_wallet_active: Boolean = true,
    val restricted_categories: List<String> = listOf("Gazlı İçecek", "Aşırı Şekerli"),
    val allergen_warnings: List<String> = listOf("Fıstık Alerjisi", "Balık"),
    val transactions: List<CanteenTransaction> = emptyList()
) {
    val remainingDailyLimit: Double get() = (daily_spending_limit - today_spent).coerceAtLeast(0.0)
}

// ==========================================
// 4. Öğrenci Ödev Teslim & Puanlama (Homework)
// ==========================================
enum class SubmissionStatus(val label: String, val colorHex: Long) {
    SUBMITTED("Teslim Edildi", 0xFF2563EB),
    GRADED("Notlandırıldı", 0xFF059669),
    LATE("Gecikmeli Teslim", 0xFFD97706),
    RETURNED("Revize İstendi", 0xFFDC2626)
}

@JsonClass(generateAdapter = true)
data class HomeworkSubmission(
    val id: String = "sub-1",
    val material_id: String = "mat-1",
    val assignment_title: String = "1. Dereceden Denklemler Çalışma Soruları (Fasikül 3)",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val student_number: String = "101",
    val class_name: String = "8-A",
    val submitted_at: String = "2026-08-24 16:40",
    val submission_text: String = "Hocam 15 sorunun tamamı çözüldü, 12. sorudaki alternatif kök bulma adımı föy arkasına eklendi.",
    val attachment_file_name: String? = "ali_demir_matematik_odev_fasikul3.pdf",
    val attachment_file_size: String? = "3.1 MB",
    val status: String = "GRADED", // SUBMITTED, GRADED, LATE, RETURNED
    val grade_score: Int? = 95, // 0-100
    val teacher_feedback: String? = "Çözüm adımların çok net ve düzenli. 12. sorudaki çözüm yaklaşımın harika!",
    val graded_at: String? = "2026-08-25 11:20",
    val teacher_name: String = "Ahmet Yılmaz"
)

// ==========================================
// 5. Kütüphane & Barkodlu Kitap Takip (Library)
// ==========================================
@JsonClass(generateAdapter = true)
data class LibraryBook(
    val id: String = "book-1",
    val isbn: String = "978-975-07-1853-2",
    val title: String = "Küçük Prens",
    val author: String = "Antoine de Saint-Exupéry",
    val publisher: String = "Can Çocuk",
    val category: String = "Dünya Klasikleri",
    val page_count: Int = 112,
    val shelf_location: String = "A Blok - Raf 3 / Sıra B",
    val total_copies: Int = 6,
    val available_copies: Int = 4,
    val cover_color_hex: Long = 0xFF1E40AF
)

@JsonClass(generateAdapter = true)
data class BookLoanRecord(
    val id: String = "loan-1",
    val book_id: String = "book-1",
    val book_title: String = "Küçük Prens",
    val book_author: String = "Antoine de Saint-Exupéry",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val student_number: String = "101",
    val class_name: String = "8-A",
    val borrowed_at: String = "2026-08-15",
    val due_date: String = "2026-08-29",
    val returned_at: String? = null,
    val is_returned: Boolean = false,
    val is_overdue: Boolean = false,
    val parent_phone: String = "0532 555 1234"
) {
    val daysRemainingOrOverdue: String get() = if (is_returned) "İade Edildi" else if (is_overdue) "Gecikti (2 Gün)" else "Kalan: 3 Gün"
}

// ==========================================
// 6. Personel İK, Nöbet & İzin Yönetimi (Staff HR)
// ==========================================
enum class DutyLocation(val label: String, val emoji: String) {
    GARDEN("Bahçe & Giriş Kapısı", "🌳"),
    FLOOR_1("1. Kat İlkokul Koridoru", "🏢"),
    FLOOR_2("2. Kat Ortaokul Koridoru", "🏫"),
    CAFETERIA("Yemekhane", "🍽️"),
    CANTEEN("Kantin Çevresi", "🥪"),
    KINDERGARTEN("Anaokulu Oyun Alanı", "🧸")
}

@JsonClass(generateAdapter = true)
data class TeacherDutyRoster(
    val id: String = "duty-1",
    val day_name: String = "Çarşamba",
    val date: String = "2026-08-26",
    val duty_location: String = "GARDEN", // GARDEN, FLOOR_1, FLOOR_2, CAFETERIA, CANTEEN, KINDERGARTEN
    val teacher_id: String = "t-1",
    val teacher_name: String = "Ahmet Yılmaz",
    val teacher_branch: String = "Matematik",
    val shift_hours: String = "08:15 - 16:30",
    val is_active_today: Boolean = true,
    val notes: String = "Giriş ve teneffüs saatlerinde bahçe kontrolü."
)

enum class LeaveStatus(val label: String, val colorHex: Long) {
    PENDING("Onay Bekliyor", 0xFFD97706),
    APPROVED("Onaylandı", 0xFF059669),
    REJECTED("Reddedildi", 0xFFDC2626)
}

@JsonClass(generateAdapter = true)
data class TeacherLeaveRequest(
    val id: String = "leave-1",
    val teacher_id: String = "t-1",
    val teacher_name: String = "Ahmet Yılmaz",
    val branch: String = "Matematik",
    val leave_type: String = "Mazeret İzni", // Mazeret İzni, Sevk / Sağlık Raporu, İdari İzin, Yıllık İzin
    val start_date: String = "2026-09-02",
    val end_date: String = "2026-09-03",
    val total_days: Int = 2,
    val reason: String = "Ailevi mazeret dolayısıyla 2 günlük mazeret izni talebi.",
    val status: String = "APPROVED", // PENDING, APPROVED, REJECTED
    val admin_response: String? = "Ders programı telafisi Zümre Başkanı koordinasyonunda yapılmak üzere onaylanmıştır.",
    val created_at: String = "2026-08-24"
)

// ==========================================
// 7. Yapay Zeka (AI / Gemini) Analiz & Raporlama Modelleri
// ==========================================
enum class AiReportType(val title: String, val roleTarget: String, val iconEmoji: String) {
    FINANCIAL_FORECAST("Tahmini Nakit Akışı & Tahsilat Projeksiyonu", "OWNER / FINANCE", "🔮"),
    STUDENT_CHURN_RISK("Öğrenci Kayıp / Ayrılma Riski Tespiti", "OWNER / ADMIN", "📉"),
    BRANCH_HEALTH_DIGEST("Şube Haftalık Sağlık & Performans Karnesi", "ADMIN", "🏫"),
    ATTENDANCE_ANOMALY("Devamsızlık & Akademik Anomali Analizi", "ADMIN / TEACHER", "⚡"),
    TEACHER_DAILY_NARRATIVE("Otomatik Pedagojik Günlük Karne Metni", "TEACHER", "📝"),
    LEARNING_GAP_ANALYSIS("Kazanım Boşluğu & Öğrenme Güçlüğü Raporu", "TEACHER", "📚"),
    HOMEWORK_GENERATOR("Akıllı Soru & Ödev Föyü Üretici", "TEACHER", "💡"),
    PARENT_WEEKLY_DIGEST("Haftalık Pedagojik Çocuk Gelişim Karnesi", "PARENT", "🌟"),
    PARENT_HOMEWORK_COACH("Evde Ders & Ödev Çalışma Koçu", "PARENT", "🎯")
}

@JsonClass(generateAdapter = true)
data class AiAnalysisReport(
    val id: String = "ai-rep-1",
    val target_role: String = "ADMIN", // OWNER, ADMIN, TEACHER, PARENT, FINANCE
    val report_type: String = "BRANCH_HEALTH_DIGEST",
    val title: String = "Haftalık Şube Sağlık ve Performans Karnesi",
    val subtitle: String = "Gemini 3.5 Flash ile otomatik veri analizi ve stratejik içgörüler",
    val created_at: String = "2026-08-30 08:30",
    val model_name: String = "Gemini 3.5 Flash",
    val summary: String = "",
    val key_findings: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val risk_score: Int? = 18, // 0-100 (düşük iyi veya düşük risk)
    val risk_level: String = "Düşük Risk", // Düşük, Orta, Yüksek, Kritik
    val metrics: Map<String, String> = emptyMap(),
    val is_pinned: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AiDailyNarrative(
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val date: String = "2026-08-30",
    val narrative_text: String = "Ali bugün grup oyunlarında oldukça paylaşımcı ve neşeliydi. Öğle yemeğinde sebze çorbasını severek bitirdi. Sanat etkinliğinde renk uyumu ve odaklanması takdire şayandı.",
    val tone: String = "Pedagojik & Şefkatli",
    val mood_detected: String = "Mutlu & Dingin",
    val suggested_activities: List<String> = listOf("Akşam serbest boyama", "Birlikte masal dinleme")
)

@JsonClass(generateAdapter = true)
data class AiHomeworkQuestion(
    val question_number: Int = 1,
    val question_text: String = "",
    val options: List<String>? = null,
    val correct_answer: String? = null,
    val explanation: String = "",
    val difficulty: String = "Orta" // Temel, Orta, İleri / Yeni Nesil
)

@JsonClass(generateAdapter = true)
data class AiGeneratedHomework(
    val id: String = "ai-hw-1",
    val title: String = "1. Dereceden Denklemler Pratik ve Pekiştirme Föyü",
    val subject: String = "Matematik",
    val topic: String = "Birinci Dereceden Bir Bilinmeyenli Denklemler",
    val grade_level: String = "8. Sınıf",
    val estimated_duration_minutes: Int = 35,
    val questions: List<AiHomeworkQuestion> = emptyList(),
    val instructions_for_student: String = "Her adımı işlem basamaklarını göstererek çözünüz. Takıldığınız soruları boş bırakıp çözüm föyünden destek alabilirsiniz.",
    val teacher_rubric: String = "İşlem hatası yapmadan denklem kurma ve kök bulma becerisini ölçer."
)

@JsonClass(generateAdapter = true)
data class MetricPair(
    val label: String,
    val value: String
)

@JsonClass(generateAdapter = true)
data class PageAiInsight(
    val title: String,
    val subtitle: String,
    val summary: String,
    val keyMetrics: List<MetricPair> = emptyList(),
    val highlights: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val quickActionTitle: String? = null,
    val quickActionType: String? = null,
    val riskLevel: String = "HEALTHY", // "HEALTHY", "GOOD", "ATTENTION", "HIGH_RISK"
    val confidenceScore: Int = 96,
    val modelName: String = "Gemini 3.5 Flash",
    val roleBadge: String = "Yönetici Özel"
)

@JsonClass(generateAdapter = true)
data class AiParentDigest(
    val student_id: String = "std-1",
    val student_name: String = "Emir Kaya",
    val week_range: String = "24 - 30 Ağustos 2026",
    val overall_wellness_score: Int = 94,
    val social_emotional_summary: String = "Emir bu hafta sınıfta arkadaşlarıyla çok uyumlu bir iletişim sergiledi. Paylaşım becerileri ve sınıf içi grup aktivitelerine katılımı belirgin şekilde arttı.",
    val nutrition_and_energy: String = "Yemek saatlerinde düzenli beslendi, öğle uykusu rutinlerine tam uyum sağladı. Enerji düzeyi gün boyu yüksek ve dengeliydi.",
    val academic_progress: String = "Haftalık ritmik sayma ve görsel eşleme etkinliklerini başarıyla tamamladı.",
    val positive_highlights: List<String> = listOf(
        "Arkadaşlarına yardımcı olma ve sıra bekleme konusunda örnek davranış sergiledi.",
        "Öğle yemeklerinin tamamını kendi başına ve iştahla tüketti.",
        "El-göz koordinasyonu gerektiren hamur oyunlarında yaratıcı şekiller üretti."
    ),
    val growth_areas: List<String> = listOf(
        "Etkinlik sonrası materyalleri toplarken biraz daha teşvik edilebilir."
    ),
    val recommended_home_activities: List<String> = listOf(
        "🧩 Evde 20-30 parçalık yapboz veya blok eşleme oyunu oynayabilirsiniz.",
        "📖 Uyku öncesinde duygularını anlatan resimli bir hikaye kitabı okuyup 'Bugün seni en çok ne mutlu etti?' sorusunu sorabilirsiniz.",
        "🎨 Parmak boyası veya serbest hamur oyunu ile motor becerilerini pekiştirebilirsiniz."
    )
)

@JsonClass(generateAdapter = true)
data class AiFinancialForecast(
    val forecast_period: String = "Eylül - Kasım 2026 (3 Aylık Projeksiyon)",
    val projected_collection_rate: Double = 93.4,
    val projected_inflow_tl: Double = 1450000.0,
    val overdue_risk_amount_tl: Double = 84000.0,
    val high_risk_students_count: Int = 4,
    val executive_summary: String = "Geçmiş 6 aylık tahsilat döngüleri ve eylül ayı erken kayıt/taksit ödeme eğilimleri modellendiğinde, kurum genelinde %93.4 tahsilat başarısı öngörülmektedir.",
    val action_items: List<String> = listOf(
        "Vadesi 15 günden fazla geciken 4 veli için kişiselleştirilmiş nazik ödeme kolaylığı SMS'i planlandı.",
        "Ekim ayı başında okul servisi ve yemek taksitlerinin toplu tahsilatı için e-posta hatırlatıcısı önerilmektedir.",
        "Kadıköy şubesindeki erken ödeme indirim kampanyası tahsilat hızını %12 artırmıştır."
    )
)

@JsonClass(generateAdapter = true)
data class AiChurnRiskItem(
    val student_id: String = "std-6",
    val student_name: String = "Can Aksoy",
    val class_name: String = "8-A",
    val branch_name: String = "Ataşehir Merkez Kampüs",
    val risk_score: Int = 78, // 0-100 (yüksek = riskli)
    val risk_level: String = "YÜKSEK RİSK",
    val primary_reason: String = "Son 3 haftada 4 gün devamsızlık ve 1 ödeme gecikmesi kaydedildi.",
    val suggested_action: String = "Rehberlik öğretmeni ile veli görüşmesi planlanması ve ders takip föyü verilmesi önerilir."
)

@JsonClass(generateAdapter = true)
data class InstitutionalSwotAnalysis(
    val executive_summary: String = "",
    val overall_institution_score: Double = 94.2,
    val finance_health_score: Double = 91.5,
    val attendance_health_score: Double = 96.0,
    val academic_success_score: Double = 88.7,
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val opportunities: List<String> = emptyList(),
    val threats_and_risks: List<String> = emptyList(),
    val strategic_action_roadmap: List<String> = emptyList(),
    val generated_at: String = "2026-08-30 09:00",
    val ai_model_used: String = "Gemini 3.5 Flash"
)

@JsonClass(generateAdapter = true)
data class StudentPerformanceMatrixItem(
    val student_id: String = "",
    val student_name: String = "",
    val student_number: String = "",
    val class_name: String = "",
    val branch_name: String = "Merkez Kampüs",
    val gpa: Double = 85.0,
    val attendance_rate: Double = 95.0,
    val absent_days: Int = 1,
    val payment_status: String = "PAID", // PAID, PENDING, OVERDUE
    val wellness_score: Int = 90,
    val risk_level: String = "DÜŞÜK", // DÜŞÜK, ORTA, YÜKSEK
    val primary_strength: String = "Matematik & Problem Çözme",
    val improvement_area: String = "Sınav zaman yönetimi",
    val ai_pedagogical_advice: String = "Konu hakimiyeti yüksek, çoktan seçmeli sorularda süre kontrolü alıştırması yapılmalı.",
    val recommended_action: String = "Veliye başarı tebriği ve hafta sonu pekiştirme föyü iletildi."
)

@JsonClass(generateAdapter = true)
data class MonthlyFinanceChartData(
    val month_name: String = "Ağu",
    val target_amount: Double = 250000.0,
    val collected_amount: Double = 238000.0,
    val pending_amount: Double = 12000.0,
    val collection_percentage: Double = 95.2
)

@JsonClass(generateAdapter = true)
data class BranchAttendanceChartData(
    val branch_name: String = "Merkez Kampüs",
    val attendance_percentage: Double = 96.4,
    val present_count: Int = 240,
    val absent_count: Int = 8,
    val late_count: Int = 3
)

@JsonClass(generateAdapter = true)
data class SubjectAcademicChartData(
    val subject_name: String = "Matematik",
    val class_average: Double = 79.5,
    val target_benchmark: Double = 85.0,
    val highest_score: Double = 98.0,
    val topic_mastery_percentage: Double = 82.0
)

@JsonClass(generateAdapter = true)
data class WeeklyLgsNetTrendData(
    val trial_title: String = "Deneme 1",
    val date: String = "01 Ağu",
    val total_net: Double = 72.5,
    val math_net: Double = 15.0,
    val science_net: Double = 16.5,
    val turkish_net: Double = 18.0
)

// ==========================================
// 8. Sistem, Donanım & RFID Modelleri
// ==========================================
@JsonClass(generateAdapter = true)
data class Organization(
    val id: String = "org-1",
    val name: String = "SCS360 Eğitim Kurumları",
    val code: String = "SCS360",
    val tax_office: String = "Ataşehir Vergi Dairesi",
    val tax_number: String = "3409182390",
    val phone: String = "+90 216 555 0100",
    val email: String = "yonetim@scs360.com",
    val address: String = "Barbaros Mah. Mor Sümbül Sok. No: 12 Ataşehir / İstanbul",
    val branch_count: Int = 4,
    val student_count: Int = 1250,
    val is_active: Boolean = true
)

@JsonClass(generateAdapter = true)
data class RfidCard(
    val id: String = "rfid-1",
    val card_uid: String = "E2:80:68:10:4F:92",
    val holder_type: String = "STUDENT", // STUDENT, STAFF, VISITOR
    val holder_id: String = "std-1",
    val holder_name: String = "Ali Demir",
    val class_or_dept: String = "8-A",
    val balance: Double = 420.0,
    val status: String = "ACTIVE", // ACTIVE, BLOCKED, LOST
    val last_tap_time: String = "Bugün 08:35 (Ana Giriş Turnikesi)",
    val issued_date: String = "2026-01-10"
)

@JsonClass(generateAdapter = true)
data class HardwareDevice(
    val id: String = "dev-1",
    val name: String = "Turnike Giriş 1 (RFID Reader)",
    val device_type: String = "TURNSTILE", // TURNSTILE, CANTEEN_POS, BIOMETRIC, TABLET
    val branch_id: String = "branch-1",
    val branch_name: String = "Ataşehir Merkez Kampüs",
    val ip_address: String = "192.168.1.120",
    val mac_address: String = "00:1A:2B:3C:4D:5E",
    val status: String = "ONLINE", // ONLINE, OFFLINE, MAINTENANCE
    val last_ping_at: String = "1 dk önce",
    val fw_version: String = "v2.4.1"
)

// ==========================================
// 7. Veli İzin & Muvafakatnameler (Consents)
// ==========================================
@JsonClass(generateAdapter = true)
data class StudentConsent(
    val id: String = "cst-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val consent_type: String = "EXCURSION", // EXCURSION, KVKK, PHOTO_VIDEO, SPORTS_HEALTH, CLUB
    val title: String = "Yıl Sonu Bilim Müzesi Gezisi İzin Belgesi",
    val description: String = "15 Mayıs 2026 tarihindeki İstanbul Bilim Müzesi eğitim gezisine katılım ve servis ulaşım onayı.",
    val is_approved: Boolean = false,
    val approved_at: String? = null,
    val approved_by_parent: String? = null,
    val expiry_date: String = "2026-06-15"
)

// ==========================================
// 8. Rehberlik & Psikolojik Danışmanlık (Guidance / PDR)
// ==========================================
@JsonClass(generateAdapter = true)
data class GuidanceRecord(
    val id: String = "gdn-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val class_name: String = "8-A",
    val counselor_name: String = "Zeynep Kaya (PDR)",
    val session_type: String = "ACADEMIC_COACHING", // INDIVIDUAL, ACADEMIC_COACHING, BEHAVIORAL, FAMILY_INTERVIEW
    val date: String = "2026-05-10",
    val notes: String = "LGS deneme sınavı kaygısı üzerine görüşüldü. Zaman yönetimi ve odaklanma teknikleri aktarıldı.",
    val recommendations: String = "Günde 30 dk kitap okuma ve haftalık soru hedefi çizelgesi takip edilecek.",
    val follow_up_date: String? = "2026-05-24",
    val is_confidential: Boolean = false
)

// ==========================================
// 9. Okul Reviri, Sağlık & İlaç (Infirmary / Health)
// ==========================================
@JsonClass(generateAdapter = true)
data class InfirmaryVisit(
    val id: String = "inf-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val class_name: String = "8-A",
    val visit_time: String = "Bugün 10:15",
    val complaint: String = "Hafif baş ağrısı ve halsizlik",
    val intervention_treatment: String = "Ateş ölçümü yapıldı, 15 dk istirahat ettirildi, su verildi.",
    val medicine_given: String? = "Parol 500mg (Veli izinli)",
    val fever_temperature: Double = 36.8,
    val parent_notified: Boolean = true,
    val discharged_to: String = "CLASSROOM" // CLASSROOM, HOME, HOSPITAL
)

// ==========================================
// 10. Öğrenci Kulüpleri & Sosyal Etkinlikler (Clubs)
// ==========================================
@JsonClass(generateAdapter = true)
data class SchoolClub(
    val id: String = "clb-1",
    val name: String = "Robotik Kodlama ve Yapay Zeka Kulübü",
    val category: String = "ROBOTICS", // ROBOTICS, CHESS, MUSIC, DRAMA, SPORTS, SCIENCE
    val description: String = "Arduino, LEGO Education ve temel Python ile algoritmik düşünme becerileri.",
    val teacher_supervisor: String = "Murat Yıldız",
    val capacity: Int = 20,
    val enrolled_count: Int = 18,
    val schedule_day: String = "Çarşamba 15:45 - 17:00",
    val room_number: String = "Robotik Laboratuvarı - Zemin Kat",
    val is_enrolled: Boolean = false
)

// ==========================================
// 11. Anketler & Memnuniyet Değerlendirme (Surveys)
// ==========================================
@JsonClass(generateAdapter = true)
data class SchoolSurvey(
    val id: String = "srv-1",
    val title: String = "2025-2026 Eğitim Öğretim Yılı 2. Dönem Veli Memnuniyet Anketi",
    val target_audience: String = "PARENTS", // PARENTS, STUDENTS, TEACHERS
    val questions_count: Int = 10,
    val total_responses: Int = 142,
    val start_date: String = "2026-05-01",
    val end_date: String = "2026-05-30",
    val is_active: Boolean = true,
    val satisfaction_score: Double = 4.8,
    val is_participated: Boolean = false
)

// ==========================================
// 12. Birebir Etüt & Takviye Dersler (Tutoring)
// ==========================================
@JsonClass(generateAdapter = true)
data class TutoringSession(
    val id: String = "tut-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val teacher_id: String = "t-1",
    val teacher_name: String = "Ahmet Yılmaz",
    val subject: String = "Matematik",
    val topic: String = "Üslü ve Köklü İfadeler Soru Çözümü",
    val date: String = "2026-05-18",
    val start_time: String = "16:00",
    val end_time: String = "16:45",
    val room: String = "Etüt Odası 1",
    val status: String = "SCHEDULED" // SCHEDULED, COMPLETED, CANCELLED
)

// ==========================================
// 13. Personel Maaş & Bordro Takibi (Payroll)
// ==========================================
@JsonClass(generateAdapter = true)
data class PayrollRecord(
    val id: String = "pay-1",
    val staff_id: String = "stf-1",
    val staff_name: String = "Ahmet Yılmaz",
    val role_title: String = "Matematik Öğretmeni",
    val period_month: String = "Mayıs 2026",
    val base_salary: Double = 42500.0,
    val bonus_allowance: Double = 4800.0,
    val deductions: Double = 1200.0,
    val net_salary: Double = 46100.0,
    val status: String = "PAID", // PAID, PENDING, PROCESSING
    val payment_date: String = "2026-05-05"
)

// ==========================================
// 14. Sistem Denetim & Güvenlik Logları (Audit Logs)
// ==========================================
@JsonClass(generateAdapter = true)
data class AuditLogEntry(
    val id: String = "log-1",
    val timestamp: String = "Bugün 14:22:10",
    val user_name: String = "Admin (Müdür)",
    val user_role: String = "ADMIN",
    val action: String = "PAYMENT_RECORDED", // CREATE, UPDATE, DELETE, LOGIN, PAYMENT, PERMISSION_CHANGE
    val module: String = "FINANCE",
    val entity_id: String = "INV-2026-003",
    val ip_address: String = "192.168.1.45",
    val details: String = "Ali Demir adına ₺5.000 kredi kartı tahsilatı kaydedildi."
)

// ==========================================
// 15. Taksit Planı ve Transfer Yardımcıları
// ==========================================
@JsonClass(generateAdapter = true)
data class InstallmentPlanRequest(
    val student_id: String = "",
    val total_amount: Double = 0.0,
    val down_payment: Double = 0.0,
    val installment_count: Int = 10,
    val start_due_date: String = "2026-09-15",
    val description: String = "Eğitim-Öğretim Yıllık Kayıt Ücreti"
)

@JsonClass(generateAdapter = true)
data class StudentTransferRequest(
    val student_id: String = "",
    val from_class_id: String = "",
    val to_class_id: String = "",
    val reason: String = "Şube mevcudu dengeleme ve seviye uyumu"
)

// ==========================================
// 16. İletişim Kuyruğu & Şablonlar (Outbox & Broadcast)
// ==========================================
@JsonClass(generateAdapter = true)
data class BroadcastJob(
    val id: String = "bc-1",
    val title: String = "",
    val channel: String = "SMS", // SMS, WHATSAPP, EMAIL
    val target_audience: String = "ALL_PARENTS", // ALL_PARENTS, CLASS_PARENTS, SPECIFIC
    val content: String = "",
    val total_recipients: Int = 0,
    val delivered_count: Int = 0,
    val failed_count: Int = 0,
    val status: String = "SENT", // DRAFT, SCHEDULED, SENT, CANCELLED
    val created_at: String = "Bugün 09:30",
    val scheduled_at: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageTemplate(
    val id: String = "tpl-1",
    val code: String = "ATTENDANCE_ABSENT",
    val name: String = "Devamsızlık Bilgilendirme",
    val channel: String = "SMS",
    val subject: String = "Devamsızlık Uyarısı",
    val body_template: String = "Sayın Velimiz, öğrencimiz {student_name} bugün {date} tarihinde derslere katılmamıştır.",
    val is_active: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SmsProvider(
    val id: String = "sp-1",
    val name: String = "Netgsm",
    val balance_credits: Int = 4520,
    val is_active: Boolean = true,
    val sender_header: String = "SCS360"
)

// ==========================================
// 17. Öğrenci Belgeleri / Dosyaları
// ==========================================
@JsonClass(generateAdapter = true)
data class StudentFile(
    val id: String = "file-1",
    val student_id: String = "std-1",
    val file_name: String = "Nufus_Cuzdani_Onlu_Arkali.pdf",
    val category: String = "IDENTITY", // IDENTITY, HEALTH_REPORT, REGISTRATION_CONTRACT, PAYMENT_CONTRACT, PHOTO, OTHER
    val file_size: String = "1.2 MB",
    val upload_date: String = "2026-09-01"
)

// ==========================================
// 18. Veli Portalı İlaç & İzin Talepleri
// ==========================================
@JsonClass(generateAdapter = true)
data class MedicineRequest(
    val id: String = "med-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val medicine_name: String = "Ventolin Şurup",
    val dosage: String = "1 Ölçek (5 ml)",
    val intake_time: String = "Öğle Yemeği Sonrası (12:45)",
    val storage_location: String = "Revir İlaç Dolabı",
    val status: String = "PENDING", // PENDING, GIVEN, CANCELLED
    val given_at: String? = null,
    val parent_notes: String = "Kullanmadan önce çalkalanmalı."
)

@JsonClass(generateAdapter = true)
data class ExcuseRequest(
    val id: String = "exc-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val reason_type: String = "HEALTH", // HEALTH, FAMILY, TRAVEL, OTHER
    val start_date: String = "2026-05-15",
    val end_date: String = "2026-05-16",
    val description: String = "Doktor randevusu ve diş tedavisi sebebiyle.",
    val status: String = "APPROVED", // PENDING, APPROVED, REJECTED
    val parent_name: String = "Mehmet Demir",
    val document_attached: Boolean = true
)

// ==========================================
// 19. Borçlar & Alacak Yönetimi (Debts)
// ==========================================
@JsonClass(generateAdapter = true)
data class Debt(
    val id: String = "",
    val organization_id: String = "",
    val branch_id: String = "",
    val student_id: String = "",
    val student_name: String = "",
    val class_name: String = "",
    val debt_type: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val paid_amount: Double = 0.0,
    val status: String = "",
    val due_date: String = "",
    val created_at: String = ""
)

// ==========================================
// 20. Teslimat Raporları (Delivery Reports)
// ==========================================
@JsonClass(generateAdapter = true)
data class DeliveryReport(
    val id: String = "del-1",
    val notification_job_id: String = "job-1",
    val recipient_name: String = "Mehmet Demir",
    val recipient_address: String = "+90 532 111 2233",
    val channel: String = "SMS", // SMS, EMAIL, WHATSAPP, PUSH
    val status: String = "DELIVERED", // PENDING, SENT, DELIVERED, FAILED
    val sent_at: String = "2026-09-22 14:30",
    val error_message: String? = null
)

// ==========================================
// 21. SaaS Platformu, Üyelik, Kota ve Fatura Katmanı
// ==========================================
@JsonClass(generateAdapter = true)
data class SubscriptionPlan(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val currency: String = "",
    val billing_period: String = "", // MONTHLY, YEARLY
    val branch_limit: Int = 0,
    val student_limit: Int = 0,
    val user_limit: Int = 0,
    val is_active: Boolean = true
)

@JsonClass(generateAdapter = true)
data class OrganizationAccessStatus(
    val organization_id: String = "",
    val organization_name: String = "",
    val status: String = "UNAVAILABLE", // ACTIVE, GRACE, RESTRICTED, SUSPENDED, UNCONFIGURED, LEGACY
    val plan_id: String = "",
    val plan_name: String = "",
    val billing_period: String = "",
    val period_start: String = "",
    val period_end: String = "",
    val grace_until: String = "",
    val remaining_grace_days: Int = 0,
    val active_branches: Int = 0,
    val branch_limit: Int = 0,
    val active_students: Int = 0,
    val student_limit: Int = 0,
    val active_users: Int = 0,
    val user_limit: Int = 0,
    val saas_iyzico_enabled: Boolean = false,
    val is_billing_authorized: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SubscriptionInvoice(
    val id: String = "",
    val invoice_no: String = "",
    val organization_id: String = "",
    val organization_name: String = "",
    val plan_id: String = "",
    val plan_name: String = "",
    val billing_period: String = "",
    val amount: Double = 0.0,
    val currency: String = "",
    val status: String = "UNAVAILABLE", // OPEN, PENDING_VERIFICATION, PAID, PAID_REVIEW, REFUNDED, EXPIRED, CANCELLED
    val valid_until: String = "",
    val bank_reference: String? = null,
    val paid_at: String? = null,
    val checkout_url: String? = null,
    val provider_payment_id: String? = null,
    val refund_reference: String? = null,
    val refund_note: String? = null,
    val reconcile_note: String? = null,
    val created_at: String = ""
)

@JsonClass(generateAdapter = true)
data class PlatformTenantSummary(
    val organization_id: String = "",
    val organization_name: String = "",
    val tax_number: String = "",
    val owner_name: String = "",
    val owner_email: String = "",
    val is_active: Boolean = true,
    val access_status: String = "UNAVAILABLE", // ACTIVE, GRACE, RESTRICTED, SUSPENDED, UNCONFIGURED, LEGACY
    val plan_id: String = "",
    val plan_name: String = "",
    val billing_period: String = "",
    val period_end: String = "",
    val grace_until: String = "",
    val active_branches: Int = 0,
    val branch_limit: Int = 0,
    val active_students: Int = 0,
    val student_limit: Int = 0,
    val active_users: Int = 0,
    val user_limit: Int = 0,
    val period_start: String = ""
)

// ==========================================
// 22. Öğrenci Teslim Masası & Kapı Geçiş Güvenliği
// ==========================================
@JsonClass(generateAdapter = true)
data class PickupAuthorization(
    val id: String = "pauth-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val class_name: String = "8-A",
    val authorized_person_name: String = "Mehmet Demir",
    val relation: String = "Baba (Veli)",
    val national_id: String = "12345678902",
    val phone: String = "+90 532 111 2233",
    val valid_from: String = "2026-09-01",
    val valid_until: String = "2027-06-30",
    val status: String = "ACTIVE", // ACTIVE, CANCELLED, EXPIRED
    val created_by: String = "Veli Portalı / İdare",
    val nonce: String = "nonce-init-1"
)

@JsonClass(generateAdapter = true)
data class DismissalLog(
    val id: String = "dlog-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val class_name: String = "8-A",
    val authorization_id: String = "pauth-1",
    val pickup_person_name: String = "Mehmet Demir",
    val national_id_masked: String = "123*****902",
    val relation: String = "Baba (Veli)",
    val verified_by: String = "Güvenlik Amiri - Kemal Kaya",
    val dismissed_at_utc: String = "2026-11-03 15:40 UTC",
    val status: String = "COMPLETED", // COMPLETED, REJECTED_MISMATCH
    val notes: String = "Kimlik doğrulaması yapıldı ve imza alındı."
)

@JsonClass(generateAdapter = true)
data class StudentIdCardRecord(
    val id: String = "idcard-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val student_number: String = "1042",
    val class_name: String = "8-A",
    val barcode: String = "SCS360-8A-1042-9F4A",
    val qr_record_id: String = "qr-rec-1042",
    val status: String = "ACTIVE", // ACTIVE, REVOKED
    val issued_at: String = "2026-09-05 09:00 UTC",
    val issued_by: String = "Kurum İdaresi"
)

@JsonClass(generateAdapter = true)
data class GateAccessLog(
    val id: String = "gate-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val class_name: String = "8-A",
    val direction: String = "IN", // IN, OUT
    val verification_method: String = "QR", // QR, BARCODE, MANUAL
    val gate_name: String = "Ana Kampüs Turnike 1",
    val actor_name: String = "Kemal Kaya (Güvenlik)",
    val timestamp_utc: String = "2026-11-03 08:18 UTC",
    val adult_pickup_verified: Boolean = false,
    val bus_permission_verified: Boolean = false,
    val requires_anomaly_review: Boolean = false
)

// ==========================================
// 23. Revir 4 Sekmeli Sağlık Dosyası, İlaç Programı & Doz Kayıtları
// ==========================================
@JsonClass(generateAdapter = true)
data class StudentHealthFile(
    val id: String = "hfile-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val class_name: String = "8-A",
    val blood_type: String = "A Rh+",
    val chronic_conditions: String = "Mevsimsel alerjik rinit",
    val allergies: String = "Penisilin, Fıstık",
    val emergency_contact_name: String = "Mehmet Demir",
    val emergency_contact_phone: String = "+90 532 111 2233",
    val insurance_info: String = "SGK + Özel Tamamlayıcı Sağlık",
    val updated_at: String = "2026-10-12"
)

@JsonClass(generateAdapter = true)
data class MedicationSchedule(
    val id: String = "msched-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val class_name: String = "8-A",
    val medication_name: String = "Ventolin İnhaler / Şurup",
    val dosage: String = "1 Ölçek (5 ml)",
    val planned_times: List<String> = listOf("12:45"),
    val start_date: String = "2026-11-01",
    val end_date: String = "2026-11-15",
    val approval_status: String = "APPROVED", // PENDING, APPROVED, REJECTED
    val approved_by: String? = "Hemşire Seda Yılmaz",
    val parent_notes: String = "Tok karnına verilmeli."
)

@JsonClass(generateAdapter = true)
data class MedicationDoseLog(
    val id: String = "mdose-1",
    val schedule_id: String = "msched-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val medication_name: String = "Ventolin İnhaler / Şurup",
    val scheduled_date: String = "2026-11-03",
    val planned_time: String = "12:45",
    val status: String = "ADMINISTERED", // ADMINISTERED, SKIPPED, REFUSED
    val administered_by: String = "Hemşire Seda Yılmaz",
    val administered_at_utc: String = "2026-11-03 09:47 UTC",
    val note: String = "Sorunsuz içildi, veli bilgi notu işlendi."
)

// ==========================================
// 24. Etüt Talepleri & Öğretmen Müsaitlikleri, Rehberlik Olayları, Kulüp Etkinlikleri, Envanter Zimmet & Bakım, Bordro Puantaj & Ek Ders
// ==========================================
@JsonClass(generateAdapter = true)
data class TutoringRequest(
    val id: String = "treq-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val class_name: String = "8-A",
    val teacher_id: String? = "usr-teacher",
    val teacher_name: String = "Ayşe Kaya",
    val subject: String = "Matematik",
    val topic: String = "Üslü ve Köklü İfadeler Soru Çözümü",
    val requested_date: String = "2026-11-06",
    val requested_time: String = "15:30 - 16:15",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED, SCHEDULED
    val decided_by: String? = null,
    val decided_at: String? = null,
    val decision_note: String? = null
)

@JsonClass(generateAdapter = true)
data class TeacherAvailabilitySlot(
    val id: String = "tslot-1",
    val teacher_id: String = "usr-teacher",
    val teacher_name: String = "Ayşe Kaya",
    val subject: String = "Matematik",
    val day_of_week: String = "Perşembe",
    val time_slot: String = "15:30 - 16:15",
    val room_name: String = "Etüt Odası B-1",
    val capacity: Int = 1,
    val is_open: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GuidanceIncident(
    val id: String = "ginc-1",
    val student_id: String = "std-1",
    val student_name: String = "Ali Demir",
    val class_name: String = "8-A",
    val reported_by: String = "Ayşe Kaya (Öğretmen)",
    val counselor_name: String = "Psk. Dan. Zeynep Aydın",
    val title: String = "Akran İletişim Uyuşmazlığı",
    val severity: String = "MEDIUM", // LOW, MEDIUM, HIGH, CRITICAL
    val is_confidential: Boolean = false,
    val status: String = "RESOLVED", // OPEN, IN_REVIEW, RESOLVED
    val resolution_note: String? = "Taraflarla birebir arabuluculuk görüşmesi yapıldı ve uzlaşı sağlandı.",
    val resolved_at: String? = "2026-11-02 14:10 UTC",
    val created_at: String = "2026-11-01"
)

@JsonClass(generateAdapter = true)
data class ClubEventRecord(
    val id: String = "cevt-1",
    val club_id: String = "clb-1",
    val club_name: String = "Robotik & Yapay Zeka Kulübü",
    val title: String = "TEKNOFEST Prototip Atölyesi",
    val event_date: String = "2026-11-12",
    val location: String = "STEM Laboratuvarı 2",
    val participant_count: Int = 18,
    val attendance_completed: Boolean = true,
    val certificate_ready: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AssetAssignmentRecord(
    val id: String = "asgn-1",
    val resource_id: String = "res-1",
    val resource_name: String = "Akıllı Tahta (Promethean 75\")",
    val location_name: String = "A Blok - 8A Dersliği",
    val assigned_staff_name: String = "Ayşe Kaya",
    val assigned_at: String = "2026-09-01",
    val status: String = "ACTIVE", // ACTIVE, RETURNED
    val condition_note: String = "Kalem seti ve kumanda ile eksiksiz teslim edildi."
)

@JsonClass(generateAdapter = true)
data class AssetMaintenanceRecord(
    val id: String = "maint-1",
    val resource_id: String = "res-1",
    val resource_name: String = "Kimya Laboratuvar Mikroskobu",
    val issue_description: String = "Mercek kalibrasyonu ve aydınlatma LED değişimi",
    val technician_Or_vendor: String = "Teknik Servis Birimi",
    val cost: Double = 1850.0,
    val scheduled_date: String = "2026-11-05",
    val status: String = "IN_PROGRESS" // OPEN, IN_PROGRESS, COMPLETED
)

@JsonClass(generateAdapter = true)
data class StaffTimesheetRecord(
    val id: String = "ts-1",
    val staff_id: String = "stf-1",
    val staff_name: String = "Ahmet Yılmaz",
    val branch_name: String = "Merkez Kampüs",
    val work_date: String = "2026-11-03",
    val check_in_utc: String = "2026-11-03 05:25 UTC (08:25 TR)",
    val check_out_utc: String = "2026-11-03 14:10 UTC (17:10 TR)",
    val total_hours: Double = 8.75,
    val status: String = "APPROVED" // DRAFT, APPROVED
)

@JsonClass(generateAdapter = true)
data class StaffExtraLessonRecord(
    val id: String = "exl-1",
    val staff_id: String = "stf-1",
    val staff_name: String = "Ahmet Yılmaz",
    val period_month: String = "2026-11",
    val lesson_date: String = "2026-11-02",
    val class_name: String = "8-A LGS Kampı",
    val hours: Double = 4.0,
    val hourly_rate: Double = 450.0,
    val total_amount: Double = 1800.0,
    val status: String = "APPROVED", // DRAFT, APPROVED
    val approved_at_utc: String? = "2026-11-02 16:00 UTC"
)
