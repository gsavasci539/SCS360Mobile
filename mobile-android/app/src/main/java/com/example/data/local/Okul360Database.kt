package com.example.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// =========================================================
// Room Database Entities
// =========================================================

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val student_number: String,
    val first_name: String,
    val last_name: String,
    val class_id: String,
    val class_name: String,
    val branch_id: String,
    val academic_year: String = "2025-2026",
    val parent_name: String,
    val parent_phone: String,
    val parent_email: String?,
    val status: String,
    val allergies_or_diet: String?,
    val blood_type: String?,
    val avatar_color_hex: Long,
    val tuition_balance: Double,
    val today_attendance_status: String?
)

@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val student_id: String,
    val student_name: String,
    val class_id: String,
    val date: String,
    val status: String,
    val note: String?,
    val check_in_time: String?,
    val temperature_celsius: Double?,
    val is_synced: Boolean = true
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val invoice_number: String,
    val student_id: String,
    val student_name: String,
    val class_name: String,
    val amount: Double,
    val paid_amount: Double,
    val due_date: String,
    val issue_date: String,
    val status: String,
    val description: String
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val category: String,
    val target_audience: String,
    val author_name: String,
    val author_role: String,
    val created_at: String,
    val is_pinned: Boolean
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val full_name: String,
    val email: String,
    val role: String,
    val tenant_id: String?,
    val institution_name: String,
    val branch_id: String,
    val branch_name: String,
    val phone: String?,
    val avatar_url: String?,
    val password_hash: String = "123456"
)

// =========================================================
// 1. Student Data Access Object (DAO)
// =========================================================

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY first_name ASC, last_name ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY first_name ASC, last_name ASC")
    suspend fun getAllStudentsList(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    fun getStudentById(id: String): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentByIdDirect(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE class_id = :classId ORDER BY first_name ASC")
    fun getStudentsByClass(classId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE class_id = :classId ORDER BY first_name ASC")
    suspend fun getStudentsByClassList(classId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE first_name LIKE '%' || :query || '%' OR last_name LIKE '%' || :query || '%' OR student_number LIKE '%' || :query || '%' OR parent_name LIKE '%' || :query || '%' ORDER BY first_name ASC")
    fun searchStudents(query: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE first_name LIKE '%' || :query || '%' OR last_name LIKE '%' || :query || '%' OR student_number LIKE '%' || :query || '%' OR parent_name LIKE '%' || :query || '%' ORDER BY first_name ASC")
    suspend fun searchStudentsList(query: String): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudent(id: String)

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentCount(): Int

    @Query("DELETE FROM students")
    suspend fun clearAll()
}

// =========================================================
// 2. Attendance Data Access Object (DAO)
// =========================================================

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE class_id = :classId AND date = :date ORDER BY student_name ASC")
    fun getAttendanceForClassAndDate(classId: String, date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE class_id = :classId AND date = :date ORDER BY student_name ASC")
    suspend fun getAttendanceForClassAndDateList(classId: String, date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance_records WHERE student_id = :studentId ORDER BY date DESC")
    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    suspend fun getAllAttendanceList(): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(records: List<AttendanceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleRecord(record: AttendanceEntity)

    @Update
    suspend fun updateAttendance(record: AttendanceEntity)

    @Query("SELECT * FROM attendance_records WHERE is_synced = 0")
    suspend fun getPendingSyncRecords(): List<AttendanceEntity>

    @Query("UPDATE attendance_records SET is_synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("SELECT COUNT(*) FROM attendance_records WHERE date = :date AND status = :status")
    suspend fun getAttendanceCountByDateAndStatus(date: String, status: String): Int

    @Query("DELETE FROM attendance_records WHERE date = :date AND class_id = :classId")
    suspend fun clearAttendanceForClassAndDate(classId: String, date: String)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAll()
}

// =========================================================
// 3. Finance & Invoice Data Access Object (DAO)
// =========================================================

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY due_date DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY due_date DESC")
    suspend fun getAllInvoicesList(): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    fun getInvoiceById(id: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceByIdDirect(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE student_id = :studentId ORDER BY due_date DESC")
    fun getInvoicesByStudent(studentId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY due_date ASC")
    fun getInvoicesByStatus(status: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY due_date ASC")
    suspend fun getInvoicesByStatusList(status: String): List<InvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<InvoiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: String)

    @Query("SELECT SUM(amount - paid_amount) FROM invoices WHERE status != 'PAID'")
    suspend fun getTotalPendingAmount(): Double?

    @Query("SELECT SUM(paid_amount) FROM invoices")
    suspend fun getTotalCollectedAmount(): Double?

    @Query("DELETE FROM invoices")
    suspend fun clearAll()
}

// Type alias for FinanceDao
typealias FinanceDao = InvoiceDao

// =========================================================
// 4. Announcement Data Access Object (DAO)
// =========================================================

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements ORDER BY is_pinned DESC, created_at DESC")
    fun getAllAnnouncements(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements ORDER BY is_pinned DESC, created_at DESC")
    suspend fun getAllAnnouncementsList(): List<AnnouncementEntity>

    @Query("SELECT * FROM announcements WHERE category = :category ORDER BY created_at DESC")
    fun getAnnouncementsByCategory(category: String): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE category = :category ORDER BY created_at DESC")
    suspend fun getAnnouncementsByCategoryList(category: String): List<AnnouncementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<AnnouncementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)

    @Query("DELETE FROM announcements")
    suspend fun clearAll()
}

// =========================================================
// 5. User Data Access Object (DAO)
// =========================================================

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY full_name ASC")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET password_hash = :newPasswordHash WHERE id = :userId")
    suspend fun updatePassword(userId: String, newPasswordHash: String)

    @Query("UPDATE users SET password_hash = :newPasswordHash WHERE email = :email")
    suspend fun updatePasswordByEmail(email: String, newPasswordHash: String)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: String)

    @Query("DELETE FROM users")
    suspend fun clearAll()
}

// =========================================================
// SCS360 Database Definition
// =========================================================

@Database(
    entities = [
        StudentEntity::class,
        AttendanceEntity::class,
        InvoiceEntity::class,
        AnnouncementEntity::class,
        UserEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class Okul360Database : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun invoiceDao(): InvoiceDao
    fun financeDao(): InvoiceDao = invoiceDao()
    abstract fun announcementDao(): AnnouncementDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: Okul360Database? = null

        fun getDatabase(context: Context): Okul360Database {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Okul360Database::class.java,
                    "scs360_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
