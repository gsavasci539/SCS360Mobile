package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal enum class TutoringRecords(val path: String, val label: String) {
    SESSIONS("sessions", "Dersler"), REQUESTS("requests", "Talepler"), AVAILABILITY("availability", "Müsaitlik")
}

internal class NativeTutoringClient(selectedBranch: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val branch = selectedBranch.takeUnless { it.isBlank() || it == "all" }?.let(::id)
    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONArray.objects() = (0 until length()).map(::getJSONObject)

    private suspend fun call(path: String, query: Map<String, String> = emptyMap(), payload: JSONObject? = null,
        method: String = if (payload == null) "GET" else "POST"): Any = scope.io {
        val url = (base + "tutoring/" + path).toHttpUrl().newBuilder().apply {
            branch?.let { addQueryParameter("branch_id", it) }
            query.filterValues(String::isNotBlank).forEach { (name, value) -> addQueryParameter(name, value) }
        }.build()
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope)
            .method(method, payload?.toString()?.toRequestBody("application/json".toMediaType())).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val data = JSONObject(requireNotNull(response.body).string())
            check(data.getBoolean("success"))
            data.opt("data") ?: JSONObject.NULL
        }
    }

    suspend fun list(kind: TutoringRecords, page: Int, student: String, teacher: String): List<JSONObject> =
        (call(kind.path, mapOf("limit" to "21", "offset" to (20 * page).toString(), "student_id" to student,
            "teacher_id" to teacher)) as JSONArray).objects().onEach {
            id(it.getString("id"))
            check(kind == TutoringRecords.AVAILABILITY || student.isBlank() || it.getString("student_id") == student)
            check(teacher.isBlank() || it.getString("teacher_id") == teacher)
        }

    suspend fun options(student: String = "", teacher: String = "", search: String = "", teacherSearch: String = "") =
        call("options", mapOf("student_id" to student, "teacher_id" to teacher, "search" to search,
            "teacher_search" to teacherSearch)) as JSONObject

    suspend fun summary(student: String, teacher: String) =
        call("summary", mapOf("student_id" to student, "teacher_id" to teacher)) as JSONObject

    suspend fun detail(kind: TutoringRecords, identifier: String): JSONObject =
        (call(kind.path, mapOf("record_id" to id(identifier), "limit" to "1")) as JSONArray).objects().single()
            .also { check(it.getString("id") == identifier) }

    private suspend fun confirm(kind: TutoringRecords, expected: String, receipt: JSONObject, fields: JSONObject) {
        check(receipt.getString("id") == expected)
        val stored = detail(kind, expected)
        check(fields.keys().asSequence().all { stored.tutoringText(it) == fields.tutoringText(it) })
    }

    suspend fun save(kind: TutoringRecords, fields: JSONObject, request: UUID, existing: String?) {
        check(existing == null || kind == TutoringRecords.SESSIONS)
        val expected = existing?.let(::id) ?: request.toString()
        val body = JSONObject(fields.toString())
        if (existing == null) body.put("request_id", expected)
        val path = if (existing == null) kind.path else "${kind.path}/$expected"
        val receipt = call(path, payload = body, method = if (existing == null) "POST" else "PUT") as JSONObject
        val verified = JSONObject(fields.toString())
        when (kind) {
            TutoringRecords.SESSIONS -> verified.put("status", "SCHEDULED")
            TutoringRecords.REQUESTS -> verified.put("status", "PENDING")
            TutoringRecords.AVAILABILITY -> verified.put("is_available", true)
        }
        confirm(kind, expected, receipt, verified)
    }

    suspend fun action(kind: TutoringRecords, row: JSONObject, action: String, fields: JSONObject = JSONObject()) {
        val identifier = id(row.getString("id"))
        val base = "${kind.path}/$identifier"
        val expected = JSONObject(fields.toString())
        val receipt = when (action) {
            "disable" -> {
                check(kind == TutoringRecords.AVAILABILITY)
                expected.put("is_available", false)
                call(base, method = "DELETE")
            }
            else -> {
                expected.put("status", when (action) {
                    "cancel" -> "CANCELLED"
                    "attendance" -> "COMPLETED"
                    "approve" -> "APPROVED"
                    "reject" -> "REJECTED"
                    else -> error("Etüt işlemi tanımlanmadı.")
                })
                call("$base/$action", payload = fields)
            }
        } as JSONObject
        if (kind != TutoringRecords.AVAILABILITY) expected.put("student_id", row.getString("student_id"))
        confirm(kind, identifier, receipt, expected)
    }
}

internal fun JSONObject.tutoringText(name: String) = if (isNull(name)) "" else getString(name)
