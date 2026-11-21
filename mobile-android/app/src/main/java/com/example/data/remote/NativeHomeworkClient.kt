package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.math.BigDecimal
import java.util.UUID

internal data class HomeworkClassChoice(val id: String, val name: String)
internal data class HomeworkStudentChoice(val id: String, val name: String)
internal data class RecordedHomework(val id: String, val title: String, val subject: String, val classId: String, val className: String,
    val day: String, val description: String, val status: String, val attachment: String)
internal data class HomeworkDraft(val title: String = "", val subject: String = "", val classroom: String = "",
    val day: String = "", val description: String = "", val status: String = "ACTIVE", val attachment: String = "")
internal data class RecordedHomeworkSubmission(val id: String, val studentId: String, val student: String, val text: String, val status: String,
    val score: BigDecimal?, val feedback: String, val time: String, val fileName: String, val fileUrl: String)
internal data class HomeworkFile(val name: String, val bytes: ByteArray)
internal const val HOMEWORK_FILE_LIMIT = 10L * 1024 * 1024

internal class NativeHomeworkClient(selectedBranch: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val branch = selectedBranch.takeUnless { it == "all" || it.isBlank() }?.let(::id)
    val requiresClass get() = branch != null
    fun ensureCurrent() = scope.ensureCurrent()

    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else getString(key)
    private fun JSONArray.rows() = (0 until length()).map(::getJSONObject)
    private fun JSONObject.wire() = toString().toRequestBody("application/json".toMediaType())

    private suspend fun call(path: String, query: Map<String, String?> = emptyMap(), body: RequestBody? = null,
        method: String = if (body == null) "GET" else "POST", binary: Boolean = false): Any = scope.io {
        val url = (base + "homework" + path).toHttpUrl().newBuilder().apply {
            branch?.let { addQueryParameter("branch_id", it) }
            query.forEach { (key, value) -> value?.let { addQueryParameter(key, it) } }
        }.build()
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).method(method, body).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val contents = requireNotNull(response.body)
            if (binary) {
                val stream = contents.source()
                check(!stream.request(HOMEWORK_FILE_LIMIT + 1))
                return@use stream.readByteArray().also { check(it.isNotEmpty()) }
            }
            val parsed = JSONTokener(contents.string()).nextValue()
            if (parsed is JSONObject && parsed.has("success")) {
                check(parsed.getBoolean("success")) { "Ödev işlemi sunucudan doğrulanamadı." }
                parsed.opt("data") ?: JSONObject.NULL
            } else parsed
        }
    }

    private fun record(item: JSONObject) = RecordedHomework(id(item.getString("id")), item.getString("title"), item.text("subject"),
        item.text("class_id"), item.text("class_name"), item.text("due_date"), item.text("description"), item.getString("status"),
        item.text("attachment_url"))

    suspend fun classes(): List<HomeworkClassChoice> = (call("/classes") as JSONArray).rows().map {
        check(branch == null || it.getString("branch_id") == branch)
        HomeworkClassChoice(id(it.getString("id")), it.getString("name"))
    }

    suspend fun list(search: String, page: Int): Pair<List<RecordedHomework>, Int> {
        val data = call("", mapOf("search" to search, "page" to page.toString(), "page_size" to "20")) as JSONObject
        val pages = data.getInt("total_pages")
        return data.getJSONArray("items").rows().map(::record) to pages
    }

    suspend fun detail(selected: String) = record(call("/${id(selected)}") as JSONObject).also { check(it.id == selected) }

    suspend fun save(draft: HomeworkDraft, existing: String?, request: UUID) {
        val body = JSONObject().put("title", draft.title).put("subject", draft.subject.ifBlank { null } ?: JSONObject.NULL)
            .put("class_id", draft.classroom.ifBlank { null }?.let(::id) ?: JSONObject.NULL)
            .put("due_date", draft.day.ifBlank { null } ?: JSONObject.NULL).put("description", draft.description.ifBlank { null } ?: JSONObject.NULL)
            .put("attachment_url", draft.attachment.ifBlank { null } ?: JSONObject.NULL)
            .put("status", draft.status).put("request_id", request.toString())
        val path = existing?.let { "/${id(it)}" }.orEmpty()
        val ack = call(path, body = body.wire(), method = if (existing == null) "POST" else "PUT") as JSONObject
        val saved = existing ?: request.toString()
        check(ack.getString("id") == saved)
        val stored = detail(saved)
        check(stored.title == draft.title && stored.subject == draft.subject && stored.classId == draft.classroom &&
            stored.day == draft.day && stored.description == draft.description && stored.status == draft.status &&
            stored.attachment == draft.attachment)
    }

    suspend fun delete(selected: String) {
        call("/${id(selected)}", method = "DELETE")
        try {
            detail(selected)
            error("Ödev kaydının silindiği doğrulanamadı.")
        } catch (failure: RegistryHttpException) {
            if (failure.code != 404) throw failure
        }
    }

    suspend fun students(homework: String, search: String): Pair<List<HomeworkStudentChoice>, Boolean> {
        val data = call("/${id(homework)}/students", mapOf("search" to search)) as JSONObject
        return data.getJSONArray("students").rows().map {
            HomeworkStudentChoice(id(it.getString("id")), it.getString("name"))
        } to data.getBoolean("more_students")
    }

    suspend fun submissions(homework: String, limit: Int, offset: Int, receipt: String? = null): List<RecordedHomeworkSubmission> =
        (call("/submissions", mapOf("homework_id" to id(homework), "limit" to limit.toString(), "offset" to offset.toString(),
            "record_id" to receipt?.let(::id))) as JSONArray).rows().map {
            val identifier = id(it.getString("id"))
            check(it.getString("homework_id") == homework && (receipt == null || identifier == receipt))
            val fileUrl = it.text("attachment_url")
            check(fileUrl.isEmpty() || fileUrl == "/homework/submissions/$identifier/attachment")
            RecordedHomeworkSubmission(identifier, id(it.getString("student_id")), it.getString("student_name"),
                it.text("submission_text"), it.getString("status"), it.text("score").toBigDecimalOrNull(), it.text("teacher_feedback"),
                it.getString("submitted_at"), it.text("attachment_file_name"), fileUrl)
        }

    suspend fun submit(homework: String, student: String, text: String, file: HomeworkFile?) {
        val target = "/${id(homework)}"
        val ack = (if (file == null) call("$target/submit", body = JSONObject().put("student_id", id(student))
            .put("homework_id", id(homework)).put("submission_text", text).wire())
        else {
            check(file.bytes.isNotEmpty() && file.bytes.size.toLong() <= HOMEWORK_FILE_LIMIT)
            val form = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("student_id", id(student))
                .addFormDataPart("submission_text", text).addFormDataPart("file", file.name, file.bytes.toRequestBody()).build()
            call("$target/submit-file", body = form)
        }) as JSONObject
        check(ack.getString("status") == "SUBMITTED")
        val stored = submissions(homework, 1, 0, id(ack.getString("id"))).single()
        check(stored.studentId == student && stored.text == text && stored.status == "SUBMITTED" &&
            (file == null || stored.fileName == file.name && stored.fileUrl.isNotBlank()))
    }

    suspend fun grade(homework: String, submission: RecordedHomeworkSubmission, score: BigDecimal, feedback: String) {
        val body = JSONObject().put("score", score.toPlainString()).put("teacher_feedback", feedback.ifBlank { null } ?: JSONObject.NULL)
        val ack = call("/submissions/${id(submission.id)}/grade", body = body.wire(), method = "PUT") as JSONObject
        check(ack.getString("id") == submission.id && ack.getString("status") == "GRADED")
        val stored = submissions(homework, 1, 0, submission.id).single()
        check(stored.studentId == submission.studentId && stored.score?.compareTo(score) == 0 &&
            stored.feedback == feedback && stored.status == "GRADED")
    }

    suspend fun download(submission: RecordedHomeworkSubmission): ByteArray {
        check(submission.fileUrl == "/homework/submissions/${id(submission.id)}/attachment")
        return call("/submissions/${id(submission.id)}/attachment", binary = true) as ByteArray
    }
}
