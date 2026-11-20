package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID

internal data class NativeAnnouncement(
    val id: String, val title: String, val content: String, val createdAt: String, val status: String,
    val audience: String, val channels: List<String>, val branch: String?, val classroom: String?, val student: String?,
)
internal data class AnnouncementTargetChoice(val id: String, val name: String)
internal data class AnnouncementTargets(val choices: List<AnnouncementTargetChoice>, val more: Boolean)
internal data class NativeAnnouncementDraft(
    val title: String, val content: String, val audience: String, val channels: List<String>,
    val branch: String?, val classroom: String?, val student: String?,
)
internal data class AnnouncementQueue(val jobs: Int, val alreadyQueued: Boolean)

internal class NativeAnnouncementClient {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()

    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(key: String): String? = if (isNull(key)) null else getString(key)

    private suspend fun call(path: String, query: List<Pair<String, String>> = emptyList(),
        method: String = "GET", body: JSONObject? = null): Any = scope.io {
        val url = (base + path).toHttpUrl().newBuilder().apply {
            query.forEach { (key, value) -> addQueryParameter(key, value) }
        }
        val content = body?.toString()?.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url.build()).tag(NativeRequestScope::class.java, scope).method(method, content).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val payload = JSONTokener(response.body?.string().orEmpty().ifBlank { "{}" }).nextValue()
            if (payload is JSONObject && payload.has("success")) {
                check(payload.getBoolean("success")) { "Duyuru işlemi doğrulanamadı." }
                payload.opt("data") ?: JSONObject.NULL
            } else payload
        }
    }

    private fun record(row: JSONObject): NativeAnnouncement {
        val channels = row.optJSONArray("channels")
        return NativeAnnouncement(id(row.getString("id")), row.getString("title"), row.getString("content"),
            row.getString("created_at"), row.getString("status"), row.text("audience").orEmpty(),
            if (channels == null) emptyList() else (0 until channels.length()).map(channels::getString),
            row.text("branch_id")?.let(::id), row.text("class_id")?.let(::id), row.text("student_id")?.let(::id))
    }

    suspend fun published(selectedBranch: String, limit: Int) = list(selectedBranch, "SENT", limit)

    suspend fun list(selectedBranch: String, status: String?, limit: Int, offset: Int = 0): List<NativeAnnouncement> {
        val branch = selectedBranch.takeUnless { it == "all" || it.isBlank() }?.let { UUID.fromString(it).toString() }
        val query = buildList {
            status?.let { add("status" to it) }
            add("limit" to limit.toString())
            branch?.let { add("branch_id" to it) }
            if (offset > 0) add("offset" to offset.toString())
        }
        val rows = call("announcements", query) as JSONArray
        check(rows.length() <= limit) { "Duyuru listesi sınırı aşıldı." }
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val item = record(row)
            check(row.has("branch_id") && (status == null || item.status == status) &&
                (branch == null || item.branch == null || item.branch == branch)) {
                "Duyuru seçili şube veya yayın durumuyla eşleşmiyor."
            }
            item
        }
    }

    suspend fun detail(announcementId: String): NativeAnnouncement {
        val value = record(call("announcements/${id(announcementId)}") as JSONObject)
        check(value.id == announcementId) { "Duyuru ayrıntısı seçili kayıtla eşleşmiyor." }
        return value
    }

    suspend fun targets(kind: String, search: String, selected: String?): AnnouncementTargets {
        val query = buildList {
            add("kind" to kind)
            add("search" to search)
            selected?.let { add("selected_id" to id(it)) }
        }
        val value = call("announcements/targets", query) as JSONObject
        val items = value.getJSONArray("items")
        val choices = (0 until items.length()).map { index ->
            val row = items.getJSONObject(index)
            AnnouncementTargetChoice(id(row.getString("id")), row.getString("name"))
        }
        return AnnouncementTargets(choices, value.getBoolean("more"))
    }

    suspend fun save(draft: NativeAnnouncementDraft, editing: String?) {
        val body = JSONObject().put("title", draft.title).put("content", draft.content).put("audience", draft.audience)
            .put("channels", JSONArray(draft.channels)).put("branch_id", draft.branch ?: JSONObject.NULL)
            .put("class_id", draft.classroom ?: JSONObject.NULL).put("student_id", draft.student ?: JSONObject.NULL)
        val path = if (editing == null) "announcements" else "announcements/${id(editing)}"
        val response = call(path, method = if (editing == null) "POST" else "PUT", body = body) as JSONObject
        val savedId = id(response.getString("id"))
        check(editing == null || savedId == editing) { "Duyuru kimliği gönderilen kayıtla eşleşmiyor." }
        val saved = detail(savedId)
        check(saved.status == "DRAFT" && saved.title == draft.title && saved.content == draft.content &&
            saved.audience == draft.audience && saved.channels.toSet() == draft.channels.toSet() &&
            (draft.branch == null || saved.branch == draft.branch) && saved.classroom == draft.classroom && saved.student == draft.student) {
            "Sunucudaki taslak gönderilen bilgilerle eşleşmiyor."
        }
    }

    suspend fun enqueue(announcementId: String): AnnouncementQueue {
        val result = call("announcements/${id(announcementId)}/send", method = "POST", body = JSONObject()) as JSONObject
        check(result.getString("announcement_id") == announcementId) { "Kuyruk kaydı seçili duyuruyla eşleşmiyor." }
        return AnnouncementQueue(result.getInt("jobs_created").also { check(it > 0) }, result.getBoolean("already_queued"))
    }

    suspend fun delete(announcementId: String) {
        call("announcements/${id(announcementId)}", method = "DELETE")
        try {
            detail(announcementId)
        } catch (error: RegistryHttpException) {
            if (error.code == 404) return
            throw error
        }
        error("Duyuru sunucuda hâlâ görünüyor.")
    }
}
