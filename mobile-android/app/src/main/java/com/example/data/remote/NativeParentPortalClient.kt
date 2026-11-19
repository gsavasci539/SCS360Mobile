package com.example.data.remote

import com.example.data.model.Invoice
import com.example.data.model.Student
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.math.BigDecimal
import java.util.UUID

data class StudentChatDraft(val content: String, val file: ChatFile? = null, val id: UUID = UUID.randomUUID()) {
    init {
        require((content.isNotBlank() || file != null) && content.length <= 10000)
    }
}

internal data class StudentChatAttachment(val name: String, val digest: String)

internal fun JSONObject.studentChatAttachment(): StudentChatAttachment? {
    if (isNull("attachment_name") || isNull("attachment_sha256")) return null
    val name = runCatching { chatFileName(getString("attachment_name")) }.getOrNull() ?: return null
    val digest = optString("attachment_sha256")
    return if (digest.matches(Regex("[a-f0-9]{64}"))) StudentChatAttachment(name, digest) else null
}

/** Only explicit server-side parent/child relationships; never match names or phones. */
class NativeParentPortalClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val client = ApiClientProvider.createOkHttpClient()
    private val chatClient = client.newBuilder().retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).build()

    private suspend fun get(path: String): Any = call(path)

    private fun request(path: String) =
        Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)

    private suspend fun call(path: String, body: JSONObject? = null): Any =
        exchange(path, body?.jsonBody())

    private suspend fun exchange(path: String, body: RequestBody? = null, chat: Boolean = false): Any = scope.io {
        val request = request(path)
        if (body != null) request.post(body)
        val http = if (chat) chatClient else client
        http.newCall(request.build()).execute().use {
            if (!it.isSuccessful) throw RegistryHttpException(it.code)
            val value = JSONTokener(it.body?.string() ?: "null").nextValue()
            if (value is JSONObject && value.has("success")) {
                check(value.getBoolean("success")) { "Veli verileri doğrulanamadı" }; value.get("data")
            } else value
        }
    }
    suspend fun children(): List<Student> {
        val values = get("parent/my-children") as JSONArray
        val seen = mutableSetOf<String>()
        return (0 until values.length()).map {
            val row = values.getJSONObject(it); val id = UUID.fromString(row.getString("student_id")).toString()
            check(seen.add(id)) { "Öğrenci ilişkileri tutarsız" }
            fun text(key: String) = if (row.isNull(key)) "" else row.getString(key)
            Student(id = id, first_name = row.getString("first_name"), last_name = row.getString("last_name"),
                student_number = text("student_number"), class_id = text("class_id"), class_name = text("class_name"), status = text("status"))
        }
    }
    suspend fun section(studentId: String, section: String): Any {
        require(section in setOf("today-status", "timeline", "grades", "schedule", "attendance", "homework", "materials", "communications", "announcements", "medicine-requests", "excuse-requests", "teachers"))
        val id = UUID.fromString(studentId).toString()
        val value = get("parent/child/$id/$section")
        if (section in setOf("today-status", "timeline")) check((value as JSONObject).getString("student_id") == id) { "Öğrenci yanıtı eşleşmiyor" }
        return value
    }
    private fun JSONObject.jsonBody() = toString().toRequestBody("application/json".toMediaType())

    private fun chatPath(studentId: String, teacherId: String) =
        "parent/child/${UUID.fromString(studentId)}/chat/${UUID.fromString(teacherId)}"

    suspend fun chat(studentId: String, teacherId: String): JSONArray =
        exchange(chatPath(studentId, teacherId), chat = true) as JSONArray

    suspend fun sendChat(studentId: String, teacherId: String, content: String, actorId: String): JSONObject {
        val draft = StudentChatDraft(content)
        sendStudentChat(studentId, teacherId, actorId, draft)
        return studentChatReceipt(studentId, teacherId, actorId, draft)
    }

    suspend fun sendStudentChat(studentId: String, teacherId: String, actorId: String, draft: StudentChatDraft) {
        val path = chatPath(studentId, teacherId)
        val file = draft.file
        val response = if (file == null) {
            val body = JSONObject().put("message", draft.content).put("request_id", draft.id.toString())
            exchange(path, body.jsonBody(), chat = true)
        } else {
            val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("request_id", draft.id.toString())
                .addFormDataPart("message", draft.content)
                .addFormDataPart("file", file.name, file.bytes.toRequestBody(file.type.toMediaType()))
                .build()
            exchange("$path/send-file", form, chat = true)
        }
        verifyChat(response as JSONObject, teacherId, actorId, draft)
    }

    suspend fun studentChatReceipt(studentId: String, teacherId: String, actorId: String, draft: StudentChatDraft): JSONObject {
        val row = exchange("${chatPath(studentId, teacherId)}/sent/${draft.id}", chat = true) as JSONObject
        verifyChat(row, teacherId, actorId, draft)
        return row
    }

    private fun verifyChat(row: JSONObject, teacherId: String, actorId: String, draft: StudentChatDraft) {
        check(row.getString("id") == draft.id.toString() && row.getBoolean("is_current_user") &&
            row.getString("receiver_id") == UUID.fromString(teacherId).toString() &&
            row.getString("sender_id") == UUID.fromString(actorId).toString() && row.getString("content") == draft.content)
        val file = draft.file
        if (file == null) {
            check(row.isNull("attachment_name") && row.isNull("attachment_sha256"))
        } else {
            check(row.studentChatAttachment() == StudentChatAttachment(file.name, file.digest))
        }
    }

    suspend fun downloadStudentChat(studentId: String, teacherId: String, message: JSONObject): ChatFile = scope.io {
        val id = UUID.fromString(message.getString("id"))
        val attachment = requireNotNull(message.studentChatAttachment())
        val path = "${chatPath(studentId, teacherId)}/attachments/$id"
        chatClient.newCall(request(path).build()).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val file = ChatFile(attachment.name, requireNotNull(response.body).byteStream().use { it.readChatFile() })
            check(file.digest == attachment.digest) { "Dosya özeti sunucu kaydıyla eşleşmiyor" }
            file
        }
    }

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
