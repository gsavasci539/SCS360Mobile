package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

internal data class InfirmaryStudent(val id: String, val name: String, val branch: String?)
internal data class InfirmaryOptions(val students: List<InfirmaryStudent>, val today: LocalDate, val time: LocalTime, val more: Boolean)
internal data class NativeInfirmaryVisit(
    val id: String, val studentId: String, val studentName: String?, val branch: String?, val date: LocalDate,
    val time: LocalTime, val severity: String, val complaint: String?, val temperature: String?, val treatment: String?,
    val followUpRequired: Boolean, val followUpNotes: String?, val examiner: String?, val parentNotified: Boolean, val notificationMethod: String?,
)
internal data class InfirmaryVisitDraft(
    val student: InfirmaryStudent, val date: LocalDate, val time: LocalTime, val severity: String, val complaint: String,
    val temperature: Double?, val treatment: String, val followUpRequired: Boolean, val followUpNotes: String,
    val parentNotified: Boolean, val notificationMethod: String,
)
internal enum class HealthFileField(val key: String, val label: String, val maxLength: Int) {
    BLOOD_TYPE("blood_type", "Kan grubu", 5),
    ALLERGIES("allergies", "Alerji bilgisi", 1000),
    CONDITIONS("chronic_conditions", "Kronik durum bilgisi", 1000),
    CONTACT_NAME("emergency_contact_name", "Acil kişi adı", 255),
    CONTACT_PHONE("emergency_contact_phone", "Acil kişi telefonu", 20),
    CONTACT_RELATION("emergency_contact_relation", "Yakınlık", 50),
    CHECKUP_DATE("last_checkup_date", "Son kontrol tarihi (YYYY-AA-GG)", 10),
}
internal data class InfirmaryHealthFile(val id: String, val values: Map<HealthFileField, String>)
internal data class InfirmaryMedication(
    val id: String, val studentId: String, val name: String, val dosage: String, val frequency: String, val status: String,
    val start: LocalDate, val end: LocalDate?, val instructions: String?, val reviewer: String?,
)
internal data class InfirmaryMedicationDraft(
    val name: String, val dosage: String, val frequency: String, val instruction: String,
    val start: LocalDate, val end: LocalDate?, val note: String,
)
internal data class InfirmaryDose(
    val id: String, val name: String?, val scheduledDate: LocalDate?, val scheduledTime: LocalTime,
    val givenTime: String?, val status: String, val notes: String?, val recorder: String?,
)

internal class NativeInfirmaryClient(selectedBranch: String, private val actorId: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val branch = selectedBranch.takeUnless { it.isBlank() || it == "all" }?.let(::id)

    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(field: String) = if (isNull(field)) null else getString(field)
    private fun JSONArray.rows() = (0 until length()).map(::getJSONObject)

    private suspend fun call(resource: String, query: Map<String, String?> = emptyMap(), body: JSONObject? = null,
        method: String = "POST"): Any =
        scope.io {
            val url = (base + "infirmary/" + resource).toHttpUrl().newBuilder().apply {
                query.forEach { (key, value) -> if (value != null) addQueryParameter(key, value) }
            }.build()
            val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope)
            if (body != null) request.method(method, body.toString().toRequestBody("application/json".toMediaType()))
            http.newCall(request.build()).execute().use { response ->
                if (!response.isSuccessful) throw RegistryHttpException(response.code)
                val result = JSONTokener(response.body?.string().orEmpty()).nextValue()
                if (result is JSONObject && result.has("success")) {
                    check(result.getBoolean("success")) { "Sunucu revir işlemini doğrulamadı." }
                    result.get("data")
                } else result
            }
        }

    suspend fun options(search: String = ""): InfirmaryOptions {
        val result = call("options", mapOf("branch_id" to branch, "search" to search.takeIf { it.isNotBlank() })) as JSONObject
        val students = result.getJSONArray("students").rows().map { row ->
            val studentBranch = row.text("branch_id")?.let(::id)
            check(branch == null || branch == studentBranch) { "Öğrenci seçili şubeyle eşleşmiyor." }
            InfirmaryStudent(id(row.getString("id")), row.getString("name"), studentBranch)
        }
        return InfirmaryOptions(students, LocalDate.parse(result.getString("today")), LocalTime.parse(result.getString("time")), result.getBoolean("more_students"))
    }

    suspend fun visits(student: InfirmaryStudent?, date: LocalDate, offset: Int, limit: Int): List<NativeInfirmaryVisit> {
        val query = mapOf("student_id" to student?.id, "branch_id" to branch, "start_date" to date.toString(),
            "end_date" to date.toString(), "offset" to offset.toString(), "limit" to limit.toString())
        return (call("infirmary-visits", query) as JSONArray).rows().map { row ->
            val studentId = id(row.getString("student_id"))
            val receivedBranch = row.text("branch_id")?.let(::id)
            val visitDate = LocalDate.parse(row.getString("visit_date"))
            check((student == null || student.id == studentId) &&
                (branch == null || branch == receivedBranch) && visitDate == date) { "Ziyaret seçili öğrenci, şube veya tarihle eşleşmiyor." }
            NativeInfirmaryVisit(id(row.getString("id")), studentId, row.text("student_name"), receivedBranch, visitDate,
                LocalTime.parse(row.getString("visit_time")), row.getString("severity"), row.text("complaint"), row.text("temperature"),
                row.text("treatment_given"), row.getBoolean("follow_up_required"), row.text("follow_up_notes"),
                row.text("examined_by_name"), row.getBoolean("parent_notified"), row.text("notification_method"))
        }
    }

    suspend fun create(draft: InfirmaryVisitDraft): String {
        val body = JSONObject().put("student_id", draft.student.id).put("branch_id", draft.student.branch)
            .put("visit_date", draft.date.toString()).put("visit_time", draft.time.toString()).put("severity", draft.severity)
            .put("complaint", draft.complaint).put("temperature", draft.temperature).put("treatment_given", draft.treatment.ifBlank { null })
            .put("follow_up_required", draft.followUpRequired).put("follow_up_notes", draft.followUpNotes.ifBlank { null })
            .put("parent_notified", draft.parentNotified)
            .put("notification_method", draft.notificationMethod.takeIf { draft.parentNotified })
        val saved = call("infirmary-visits", body = body) as JSONObject
        check(saved.getString("student_id") == draft.student.id && saved.text("branch_id") == draft.student.branch &&
            saved.getString("examined_by") == id(actorId) && saved.getString("visit_date") == draft.date.toString() &&
            LocalTime.parse(saved.getString("visit_time")) == draft.time && saved.getString("severity") == draft.severity &&
            saved.getBoolean("follow_up_required") == draft.followUpRequired && saved.getBoolean("parent_notified") == draft.parentNotified) {
            "Kaydedilen ziyaret gönderilen öğrenci, şube ve ziyaret bilgileriyle eşleşmiyor."
        }
        return id(saved.getString("id"))
    }

    suspend fun complete(visit: NativeInfirmaryVisit, notes: String): Boolean {
        val saved = call("infirmary-visits/${id(visit.id)}/follow-up", body = JSONObject().put("follow_up_notes", notes)) as JSONObject
        check(saved.getString("id") == visit.id && saved.getString("student_id") == visit.studentId &&
            saved.text("branch_id") == visit.branch && !saved.getBoolean("follow_up_required")) {
            "Sunucu yanıtı seçili ziyaret ile eşleşmiyor."
        }
        return saved.text("follow_up_notes") == notes
    }

    suspend fun healthFile(student: InfirmaryStudent): InfirmaryHealthFile? {
        val query = mapOf("student_id" to id(student.id), "limit" to "2")
        val rows = (call("health-records", query) as JSONArray).rows()
        check(rows.all { it.getString("student_id") == student.id }) { "Sağlık dosyası seçili öğrenciyle eşleşmiyor." }
        check(rows.size <= 1) { "Öğrencinin birden fazla eski sağlık dosyası var. Kayıtları Web revir ekranında inceleyin." }
        return rows.singleOrNull()?.let { row ->
            InfirmaryHealthFile(id(row.getString("id")), HealthFileField.entries.associateWith { row.text(it.key).orEmpty() })
        }
    }

    suspend fun saveHealthFile(student: InfirmaryStudent, existing: InfirmaryHealthFile?, changes: Map<HealthFileField, String>) {
        require(changes.isNotEmpty())
        val body = JSONObject().put("student_id", id(student.id))
        changes.forEach { (field, value) -> body.put(field.key, value.ifBlank { JSONObject.NULL }) }
        val saved = call("health-records/${id(student.id)}", body = body, method = "PUT") as JSONObject
        val savedId = id(saved.getString("id"))
        check(saved.getString("student_id") == student.id && saved.getString("updated_by") == id(actorId) &&
            (existing == null || savedId == existing.id) && changes.all { (field, value) ->
                saved.has(field.key) && saved.text(field.key).orEmpty() == value
            }) { "Sunucudaki sağlık dosyası gönderilen değişikliklerle eşleşmiyor." }
    }

    suspend fun medications(student: InfirmaryStudent, offset: Int, limit: Int): List<InfirmaryMedication> {
        val query = mapOf("student_id" to id(student.id), "offset" to offset.toString(), "limit" to limit.toString())
        return (call("medicine-requests", query) as JSONArray).rows().map { row ->
            val studentId = id(row.getString("student_id"))
            check(studentId == student.id) { "İlaç programı seçili öğrenciyle eşleşmiyor." }
            InfirmaryMedication(id(row.getString("id")), studentId, row.getString("medicine_name"), row.getString("dosage"),
                row.getString("time_to_give"), row.getString("status"), LocalDate.parse(row.getString("start_date")),
                row.text("end_date")?.let(LocalDate::parse), row.text("instructions"), row.text("updated_by_name"))
        }
    }

    suspend fun createMedication(student: InfirmaryStudent, draft: InfirmaryMedicationDraft) {
        val body = JSONObject().put("student_id", id(student.id)).put("medication_name", draft.name)
            .put("dosage", draft.dosage).put("frequency", draft.frequency).put("instruction", draft.instruction)
            .put("start_date", draft.start.toString()).put("end_date", draft.end?.toString())
            .put("parent_notes", draft.note.ifBlank { null })
        val saved = call("medication-schedules", body = body) as JSONObject
        id(saved.getString("id"))
        check(saved.getString("student_id") == student.id && saved.getString("created_by") == id(actorId) &&
            saved.getString("status") == "APPROVED" && saved.getString("medication_name") == draft.name &&
            saved.getString("dosage") == draft.dosage && saved.getString("frequency") == draft.frequency &&
            saved.getString("start_date") == draft.start.toString() && saved.text("end_date") == draft.end?.toString()) {
            "Sunucudaki ilaç programı gönderilen kayıtla eşleşmiyor."
        }
    }

    suspend fun decideMedication(program: InfirmaryMedication, status: String): Boolean {
        val saved = call("medicine-requests/${id(program.id)}", body = JSONObject().put("status", status), method = "PUT") as JSONObject
        check(saved.getString("id") == program.id && saved.getString("student_id") == program.studentId &&
            saved.getString("status") == status) { "Sunucudaki karar seçili ilaç talebiyle eşleşmiyor." }
        return saved.text("updated_by") == id(actorId)
    }

    suspend fun doses(student: InfirmaryStudent, day: LocalDate, offset: Int, limit: Int): List<InfirmaryDose> {
        val query = mapOf("student_id" to id(student.id), "date" to day.toString(), "offset" to offset.toString(), "limit" to limit.toString())
        return (call("medication-logs", query) as JSONArray).rows().map { row ->
            val scheduledDay = row.text("scheduled_date")?.let(LocalDate::parse)
            check(row.getString("student_id") == student.id && (scheduledDay == null || scheduledDay == day)) {
                "Doz kaydı seçili öğrenci veya günle eşleşmiyor."
            }
            InfirmaryDose(id(row.getString("id")), row.text("medication_name"), scheduledDay, LocalTime.parse(row.getString("scheduled_time")),
                row.text("given_time"), row.getString("status"), row.text("notes"), row.text("given_by_name"))
        }
    }

    suspend fun recordDose(program: InfirmaryMedication, day: LocalDate, time: LocalTime, status: String, notes: String): Boolean {
        val body = JSONObject().put("student_id", id(program.studentId)).put("schedule_id", id(program.id))
            .put("scheduled_date", day.toString()).put("scheduled_time", time.toString()).put("status", status)
            .put("notes", notes.ifBlank { null })
        val saved = call("medication/mark-given", body = body) as JSONObject
        id(saved.getString("id"))
        check(saved.getString("student_id") == program.studentId && saved.getString("schedule_id") == program.id &&
            saved.getString("scheduled_date") == day.toString() && LocalTime.parse(saved.getString("scheduled_time")) == time &&
            saved.getString("status") == status) { "Sunucudaki doz kaydı seçili öğrenci ve programla eşleşmiyor." }
        return saved.text("given_by") == id(actorId) && saved.text("notes").orEmpty() == notes
    }
}
