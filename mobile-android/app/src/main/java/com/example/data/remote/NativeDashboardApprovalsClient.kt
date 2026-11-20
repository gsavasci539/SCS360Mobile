package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

internal data class DashboardExcuse(
    val id: String, val studentId: String, val studentName: String, val start: LocalDate, val end: LocalDate, val description: String,
)
internal data class DashboardApprovals(val medicineCount: Int, val excuseCount: Int, val excuses: List<DashboardExcuse>)

internal class NativeDashboardApprovalsClient {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private fun id(raw: String) = UUID.fromString(raw).toString()

    private suspend fun call(path: String, branch: String? = null, body: JSONObject? = null): JSONObject = scope.io {
        val url = (base + "dashboard/" + path).toHttpUrl().newBuilder()
        branch?.let { url.addQueryParameter("branch_id", id(it)) }
        val request = Request.Builder().url(url.build()).tag(NativeRequestScope::class.java, scope)
        if (body != null) request.put(body.toString().toRequestBody("application/json".toMediaType()))
        http.newCall(request.build()).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val payload = JSONObject(response.body?.string().orEmpty())
            if (payload.has("success")) {
                check(payload.getBoolean("success")) { "Pano yanıtı doğrulanamadı." }
                payload.getJSONObject("data")
            } else payload
        }
    }

    suspend fun pending(branch: String, readMedicine: Boolean, readExcuses: Boolean): DashboardApprovals {
        val payload = call("pending-actions", branch)
        check(id(payload.getString("branch_id")) == branch) { "Bekleyen işlemler seçili şubeyle eşleşmiyor." }
        fun count(category: JSONObject) = category.getInt("count").also { check(it >= 0) }
        val medicineCount = if (readMedicine) count(payload.getJSONObject("medicine_requests")) else 0
        if (!readExcuses) return DashboardApprovals(medicineCount, 0, emptyList())
        val excuses = payload.getJSONObject("excuse_requests")
        val values = excuses.getJSONArray("items")
        val rows = (0 until values.length()).map { index ->
            val row = values.getJSONObject(index)
            check(row.getString("status") == "PENDING") { "Mazeret talebi bekleyen işlemlerle eşleşmiyor." }
            DashboardExcuse(id(row.getString("id")), id(row.getString("student_id")), row.getString("student_name"),
                LocalDate.parse(row.getString("start_date")), LocalDate.parse(row.getString("end_date")), row.getString("description"))
        }
        val excuseCount = count(excuses)
        check(excuseCount >= rows.size)
        return DashboardApprovals(medicineCount, excuseCount, rows)
    }

    suspend fun review(excuse: DashboardExcuse, actorId: String, status: String, comment: String) {
        val body = JSONObject().put("status", status).put("comment", comment.ifBlank { JSONObject.NULL })
        val saved = call("excuse-requests/${id(excuse.id)}", body = body)
        check(saved.getString("id") == excuse.id && saved.getString("student_id") == excuse.studentId &&
            saved.getString("status") == status && saved.getString("reviewed_by") == id(actorId) &&
            (if (saved.isNull("review_comment")) "" else saved.getString("review_comment")) == comment) {
            "Sunucudaki değerlendirme seçili mazeretle eşleşmiyor."
        }
    }
}
