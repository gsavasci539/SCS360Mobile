package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime
import java.util.UUID

internal data class ExamsPage<T>(val rows: List<T>, val more: Boolean)
internal data class ExamClassChoice(val id: String, val name: String, val branch: String)
internal data class ExamGradeDraft(val score: String, val scale: String, val notes: String)

internal class NativeExamsClient(selectedBranch: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val branch = selectedBranch.takeUnless { it.isBlank() || it == "all" }?.let(::uuid)

    private fun uuid(value: String) = UUID.fromString(value).toString()
    private fun JSONArray.records() = (0 until length()).map(::getJSONObject)

    private suspend fun call(path: String, query: Map<String, String?> = emptyMap(), method: String = "GET",
        form: JSONObject? = null): Any = scope.io {
        val url = (base + "exams" + path).toHttpUrl().newBuilder().apply {
            branch?.let { addQueryParameter("branch_id", it) }
            query.forEach { (key, value) -> if (!value.isNullOrBlank()) addQueryParameter(key, value) }
        }.build()
        val body = form?.toString()?.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).method(method, body).build()
        http.newCall(request).execute().use { reply ->
            if (!reply.isSuccessful) throw RegistryHttpException(reply.code)
            val envelope = JSONObject(requireNotNull(reply.body).string())
            check(envelope.getBoolean("success"))
            envelope.get("data")
        }
    }

    private fun page(data: JSONObject, page: Int): ExamsPage<JSONObject> {
        check(data.getInt("page") == page)
        return ExamsPage(data.getJSONArray("items").records().onEach { uuid(it.getString("id")) },
            page < data.getInt("total_pages"))
    }

    suspend fun list(page: Int, search: String, status: String): ExamsPage<JSONObject> =
        page(call("", mapOf("page" to page.toString(), "page_size" to "25", "search" to search.trim(), "status" to status))
            as JSONObject, page).also { records -> records.rows.forEach(::validateExamBranch) }

    private fun validateExamBranch(row: JSONObject) {
        check(branch == null || row.getString("branch_id") == branch)
    }

    suspend fun classes(page: Int, search: String, examId: String?): ExamsPage<ExamClassChoice> {
        val values = page(call("/class-options", mapOf("page" to page.toString(), "page_size" to "25",
            "search" to search.trim(), "exam_id" to examId?.let(::uuid))) as JSONObject, page)
        return ExamsPage(values.rows.map {
            val classroomBranch = uuid(it.getString("branch_id"))
            check(branch == null || branch == classroomBranch)
            ExamClassChoice(it.getString("id"), it.getString("name"), classroomBranch)
        }, values.more)
    }

    suspend fun exact(identifier: String): JSONObject {
        val id = uuid(identifier)
        return (call("", mapOf("record_id" to id, "limit" to "1")) as JSONArray).records().single().also {
            check(it.getString("id") == id)
            validateExamBranch(it)
        }
    }

    suspend fun save(existing: JSONObject?, expectedClass: ExamClassChoice, requestId: UUID, values: JSONObject) {
        val draft = JSONObject(values.toString())
        check(draft.getString("class_id") == expectedClass.id)
        val id = existing?.getString("id")?.let(::uuid) ?: requestId.toString()
        if (existing == null) draft.put("request_id", id)
        else existing.examText("updated_at")?.let { draft.put("expected_updated_at", it) }
        val ack = call(if (existing == null) "" else "/$id", method = if (existing == null) "POST" else "PUT", form = draft) as JSONObject
        check(ack.getString("id") == id)
        val stored = exact(id)
        check(stored.getString("branch_id") == expectedClass.branch && stored.getString("status") != "CANCELLED")
        check(values.keys().asSequence().all { name ->
            val actual = stored.examText(name)
            val wanted = values.examText(name)
            stored.has(name) && when (name) {
                "start_time" -> actual?.let(LocalTime::parse) == wanted?.let(LocalTime::parse)
                "duration_minutes", "max_score", "weight" -> actual?.toBigDecimalOrNull()?.compareTo(requireNotNull(wanted).toBigDecimal()) == 0
                else -> actual == wanted
            }
        })
    }

    suspend fun cancel(identifier: String) {
        val id = uuid(identifier)
        val ack = call("/$id", method = "DELETE") as JSONObject
        check(ack.getString("id") == id && ack.getString("status") == "CANCELLED")
        check(exact(id).getString("status") == "CANCELLED")
    }

    suspend fun roster(exam: String, page: Int, search: String): ExamsPage<JSONObject> =
        page(call("/${uuid(exam)}/roster", mapOf("page" to page.toString(), "page_size" to "20", "search" to search.trim()))
            as JSONObject, page)

    suspend fun grades(exam: String, drafts: Map<String, ExamGradeDraft>) {
        val examId = uuid(exam)
        val sent = drafts.map { (student, draft) -> JSONObject().put("student_id", uuid(student))
            .put("score", draft.score.ifBlank { null } ?: JSONObject.NULL)
            .put("grade_scale", draft.scale.trim().ifBlank { null } ?: JSONObject.NULL)
            .put("notes", draft.notes.trim().ifBlank { null } ?: JSONObject.NULL) }
        val form = JSONObject().put("exam_id", examId).put("grades", JSONArray(sent))
        val ack = call("/bulk-grade-entry", method = "POST", form = form) as JSONObject
        val acknowledged = ack.getJSONArray("items").records()
        check(acknowledged.size == sent.size)
        val stored = (call("/$examId/results") as JSONArray).records().groupBy { it.getString("id") }
        sent.forEach { grade ->
            val receipt = acknowledged.single { it.getString("student_id") == grade.getString("student_id") }
            check(matchesGrade(receipt, examId, grade))
            val row = stored[uuid(receipt.getString("id"))]?.single()
            check(row != null && matchesGrade(row, examId, grade))
        }
    }

    private fun matchesGrade(data: JSONObject, exam: String, grade: JSONObject): Boolean =
        data.getString("exam_id") == exam && data.getString("student_id") == grade.getString("student_id") &&
            data.examText("grade_scale") == grade.examText("grade_scale") && data.examText("notes") == grade.examText("notes") &&
            if (grade.isNull("score")) data.has("score") && data.isNull("score")
            else data.examText("score")?.toBigDecimalOrNull()?.compareTo(requireNotNull(grade.examText("score")).toBigDecimal()) == 0

    suspend fun cards(student: String): List<JSONObject> {
        val id = uuid(student)
        return (call("/report-cards/$id") as JSONArray).records().onEach {
            check(uuid(it.getString("student_id")) == id)
            uuid(it.getString("id"))
        }
    }

    suspend fun generate(student: String, values: JSONObject) {
        val id = uuid(student)
        val form = JSONObject(values.toString()).put("student_id", id)
        val ack = call("/report-cards/generate", method = "POST", form = form) as JSONObject
        val card = uuid(ack.getString("id"))
        check(ack.getString("student_id") == id && !ack.getBoolean("is_published"))
        val saved = cards(id).single { it.getString("id") == card }
        check(!saved.getBoolean("is_published") && form.keys().asSequence().all { saved.has(it) && saved.examText(it) == form.examText(it) })
    }

    suspend fun publish(student: String, card: String) {
        val id = uuid(card)
        val studentId = uuid(student)
        val reply = call("/report-cards/$id/publish", method = "PUT", form = JSONObject()) as JSONObject
        check(reply.getString("id") == id && reply.getString("student_id") == studentId && reply.getBoolean("is_published"))
        check(cards(studentId).single { it.getString("id") == id }.getBoolean("is_published"))
    }
}

internal fun JSONObject.examText(key: String): String? = if (isNull(key)) null else getString(key)
