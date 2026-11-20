package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID

internal data class NativeAnnouncement(
    val id: String, val title: String, val content: String, val createdAt: String,
)

internal class NativeAnnouncementClient {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()

    suspend fun published(selectedBranch: String, limit: Int): List<NativeAnnouncement> = scope.io {
        val branch = selectedBranch.takeUnless { it == "all" || it.isBlank() }?.let { UUID.fromString(it).toString() }
        val url = (base + "announcements").toHttpUrl().newBuilder()
            .addQueryParameter("status", "SENT").addQueryParameter("limit", limit.toString())
        branch?.let { url.addQueryParameter("branch_id", it) }
        val request = Request.Builder().url(url.build()).tag(NativeRequestScope::class.java, scope).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val payload = JSONTokener(response.body?.string().orEmpty()).nextValue()
            val rows = if (payload is JSONObject) {
                check(payload.getBoolean("success")) { "Duyuru listesi doğrulanamadı." }
                payload.getJSONArray("data")
            } else payload as JSONArray
            check(rows.length() <= limit) { "Duyuru listesi sınırı aşıldı." }
            (0 until rows.length()).map { index ->
                val row = rows.getJSONObject(index)
                check(row.has("branch_id") && row.getString("status") == "SENT" &&
                    (branch == null || row.isNull("branch_id") || row.getString("branch_id") == branch)) {
                    "Duyuru seçili şube veya yayın durumuyla eşleşmiyor."
                }
                NativeAnnouncement(UUID.fromString(row.getString("id")).toString(), row.getString("title"),
                    row.getString("content"), row.getString("created_at"))
            }
        }
    }
}
