package com.example.data.remote

import com.example.data.model.Invoice
import com.example.data.model.ParentAttendanceMonth
import com.example.data.model.Student
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class ParentChildOptionsModule(val queryValue: String) {
    ATTENDANCE("attendance"),
    FINANCES("finances")
}

/** Only explicit server-side parent/child relationships; never match names or phones. */
class NativeParentPortalClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val client = ApiClientProvider.createOkHttpClient()
    private val chat = NativeStudentChatClient(scope)

    private suspend fun get(path: String): Any = call(path)
    private suspend fun call(path: String, body: JSONObject? = null): Any = scope.io {
        val request = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (body != null) request.post(body.toString().toRequestBody("application/json".toMediaType()))
        client.newCall(request.build()).execute().use {
            if (!it.isSuccessful) throw RegistryHttpException(it.code)
            val value = JSONTokener(it.body?.string() ?: "null").nextValue()
            if (value is JSONObject && value.has("success")) {
                check(value.getBoolean("success")) { "Veli verileri doğrulanamadı" }; value.get("data")
            } else value
        }
    }
    suspend fun children(module: ParentChildOptionsModule? = null): List<Student> {
        val path = module?.let { "parent/my-children/options?module=${it.queryValue}" } ?: "parent/my-children"
        val values = get(path) as JSONArray
        val seen = mutableSetOf<String>()
        return (0 until values.length()).map {
            val row = values.getJSONObject(it); val id = UUID.fromString(row.getString("student_id")).toString()
            check(seen.add(id)) { "Öğrenci ilişkileri tutarsız" }
            fun text(key: String) = if (row.isNull(key)) "" else row.getString(key)
            Student(id = id, first_name = row.getString("first_name"), last_name = row.getString("last_name"),
                student_number = text("student_number"), class_id = text("class_id"), class_name = text("class_name"), status = text("status"))
        }
    }

    internal suspend fun loadPortalRecords(
        module: ParentChildOptionsModule?,
        readFinances: Boolean,
        onChildren: (List<Student>) -> Unit,
        onInvoices: (String, Result<List<Invoice>>) -> Unit,
    ) = coroutineScope {
        val children = children(module)
        scope.ensureCurrent()
        onChildren(children)
        if (readFinances) for (child in children) launch {
            val result = try {
                Result.success(invoices(child))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Result.failure(failure)
            }
            scope.ensureCurrent()
            onInvoices(child.id, result)
        }
    }
    suspend fun section(studentId: String, section: String, date: LocalDate? = null): Any {
        require(section in setOf("today-status", "timeline", "grades", "schedule", "homework", "materials", "communications", "announcements", "medicine-requests", "excuse-requests", "teachers"))
        require(date == null || section == "timeline")
        val id = UUID.fromString(studentId).toString()
        val query = date?.let { "?date=$it" }.orEmpty()
        val value = get("parent/child/$id/$section$query")
        if (section == "today-status" || section == "timeline") {
            val day = value as JSONObject
            check(day.getString("student_id") == id) { "Öğrenci yanıtı eşleşmiyor" }
            check(date == null || day.getString("date") == date.toString()) { "Günlük akış tarihi eşleşmiyor" }
        }
        return value
    }

    suspend fun attendance(studentId: String, period: YearMonth): ParentAttendanceMonth {
        require(period.year in 1..9999)
        val id = UUID.fromString(studentId).toString()
        val raw = get("parent/child/$id/attendance?year=${period.year}&month=${period.monthValue}") as JSONObject
        val attendance = checkNotNull(ApiClientProvider.moshi.adapter(ParentAttendanceMonth::class.java).fromJson(raw.toString()))
        check(attendance.student_id == id && attendance.year == period.year && attendance.month == period.monthValue) {
            "Yoklama öğrencisi veya dönemi eşleşmiyor."
        }
        check(attendance.calendar.all { (number, entry) ->
            val day = LocalDate.parse(entry.date)
            YearMonth.from(day) == period && number == day.dayOfMonth.toString()
        }) { "Yoklama günleri doğrulanamadı." }
        return attendance
    }
    suspend fun chat(studentId: String, teacherId: String): JSONArray =
        chat.parent(studentId, teacherId).history()

    suspend fun sendChat(studentId: String, teacherId: String, content: String, actorId: String): JSONObject {
        val draft = StudentChatDraft(content)
        val conversation = chat.parent(studentId, teacherId, actorId)
        conversation.send(draft)
        return conversation.receipt(draft)
    }

    suspend fun sendStudentChat(studentId: String, teacherId: String, actorId: String, draft: StudentChatDraft) =
        chat.parent(studentId, teacherId, actorId).send(draft)

    suspend fun studentChatReceipt(studentId: String, teacherId: String, actorId: String, draft: StudentChatDraft): JSONObject =
        chat.parent(studentId, teacherId, actorId).receipt(draft)

    suspend fun downloadStudentChat(studentId: String, teacherId: String, message: JSONObject): ChatFile =
        chat.parent(studentId, teacherId).download(message)

    suspend fun submit(studentId: String, kind: String, fields: JSONObject): JSONObject {
        val id = UUID.fromString(studentId).toString()
        val allowed = when (kind) {
            "MEDICINE" -> setOf("medicine_name", "dosage", "time_to_give", "instructions")
            "EXCUSE" -> setOf("start_date", "end_date", "reason_type", "description")
            "PICKUP" -> setOf("authorized_person_name", "authorized_person_id", "relationship", "phone", "valid_from", "valid_until", "notes", "request_id")
            else -> error("Geçersiz talep türü")
        }
        require(fields.keys().asSequence().all { it in allowed }) { "Desteklenmeyen talep alanı" }
        val body = JSONObject(fields.toString())
        val path = when (kind) {
            "MEDICINE" -> "parent/child/$id/medicine-requests"
            "EXCUSE" -> "parent/child/$id/excuse-request"
            else -> { body.put("student_id", id); "dismissal/authorizations" }
        }
        val value = call(path, body) as JSONObject
        UUID.fromString(value.getString("id"))
        check(value.getString("student_id") == id) { "Talep yanıtı öğrenciyle eşleşmiyor" }
        return value
    }
    suspend fun invoices(child: Student): List<Invoice> {
        val id = UUID.fromString(child.id).toString()
        val value = get("parent/child/$id/financial-invoices") as JSONObject
        check(value.getString("student_id") == id) { "Borçlar seçili öğrenciyle eşleşmiyor" }
        val rows = value.getJSONArray("invoices"); val seen = mutableSetOf<String>()
        return (0 until rows.length()).map {
            val row = rows.getJSONObject(it); val debt = UUID.fromString(row.getString("debt_id")).toString()
            check(seen.add(debt)) { "Borç listesi tutarsız" }
            val amount = BigDecimal(row.getString("amount")); val paid = BigDecimal(row.getString("paid_amount"))
            val remaining = BigDecimal(row.getString("remaining_amount"))
            check(amount.signum() >= 0 && paid.signum() >= 0 && remaining.signum() >= 0 && (amount - paid).compareTo(remaining) == 0) { "Borç tutarları doğrulanamadı" }
            Invoice(id = debt, student_id = id, student_name = child.full_name, class_name = child.class_name,
                amount = amount.toDouble(), paid_amount = paid.toDouble(), status = row.getString("status"),
                description = if (row.isNull("description")) "" else row.getString("description"),
                due_date = if (row.isNull("due_date")) "" else row.getString("due_date"))
        }
    }
}
