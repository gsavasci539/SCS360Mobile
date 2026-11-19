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
import java.util.UUID

internal const val STUDENT_CHAT_PAGE_SIZE = 50

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

internal enum class StaffChatArea(val segment: String) {
    TEACHER("teacher"), ADMIN("admin"),
}

internal data class StaffChatContact(
    val studentId: String, val studentName: String, val parentId: String, val parentName: String,
    val className: String?, val branchId: String,
)

internal data class StaffChatContacts(val rows: List<StaffChatContact>, val hasMore: Boolean)

internal class NativeStudentChatClient(
    private val scope: NativeRequestScope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope(),
) {
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient().newBuilder().retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).build()

    fun current() = scope.current()

    fun parent(studentId: String, teacherId: String, actorId: String? = null): Conversation {
        val teacher = chatUuid(teacherId)
        return Conversation("parent/child/${chatUuid(studentId)}/chat/$teacher", actorId, teacher)
    }

    fun staff(area: StaffChatArea, contact: StaffChatContact, actorId: String): Conversation {
        val parent = chatUuid(contact.parentId)
        return Conversation("${area.segment}/chat/${chatUuid(contact.studentId)}", actorId, parent,
            listOf("parent_id" to parent, "branch_id" to chatUuid(contact.branchId)))
    }

    suspend fun contacts(area: StaffChatArea, branchId: String?, search: String, page: Int): StaffChatContacts {
        val params = mutableListOf("page" to page.toString(), "search" to search)
        val branch = branchId?.let(::chatUuid)
        branch?.let { params += "branch_id" to it }
        val result = exchange("${area.segment}/chat-contacts", params) as JSONObject
        check(result.getInt("page") == page)
        val rows = result.getJSONArray("items")
        return StaffChatContacts((0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val rowBranch = chatUuid(row.getString("branch_id"))
            check(branch == null || branch == rowBranch)
            StaffChatContact(chatUuid(row.getString("student_id")), row.getString("student_name"),
                chatUuid(row.getString("parent_id")), row.getString("parent_name"),
                row.optString("class_name").takeUnless { row.isNull("class_name") }, rowBranch)
        }, page < result.getInt("total_pages"))
    }

    private fun request(path: String, params: List<Pair<String, String>>): Request.Builder {
        val url = (base + path).toHttpUrl().newBuilder()
        for ((key, value) in params) url.addQueryParameter(key, value)
        return Request.Builder().url(url.build()).tag(NativeRequestScope::class.java, scope)
    }

    private suspend fun exchange(path: String, params: List<Pair<String, String>>, body: RequestBody? = null): Any = scope.io {
        val request = request(path, params)
        if (body != null) request.post(body)
        http.newCall(request.build()).execute().use { result ->
            if (!result.isSuccessful) throw RegistryHttpException(result.code)
            val value = JSONTokener(requireNotNull(result.body).string()).nextValue()
            if (value is JSONObject && value.has("success")) {
                check(value.getBoolean("success"))
                value.get("data")
            } else value
        }
    }

    internal inner class Conversation(
        private val path: String, actorId: String?, private val partnerId: String,
        private val params: List<Pair<String, String>> = emptyList(),
    ) {
        private val actor = actorId?.let(::chatUuid)

        suspend fun history(limit: Int? = null, beforeId: String? = null): JSONArray {
            val query = params.toMutableList()
            limit?.let { query += "limit" to it.toString() }
            beforeId?.let { query += "before_id" to chatUuid(it) }
            val rows = exchange(path, query) as JSONArray
            if (actor != null) {
                val seen = mutableSetOf<String>()
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    val mine = row.getBoolean("is_current_user")
                    check(row.getString("sender_id") == (if (mine) actor else partnerId) &&
                        row.getString("receiver_id") == (if (mine) partnerId else actor) &&
                        seen.add(chatUuid(row.getString("id"))))
                }
            }
            return rows
        }

        suspend fun send(draft: StudentChatDraft) {
            val file = draft.file
            val response = if (file == null) {
                val body = JSONObject().put("message", draft.content).put("request_id", draft.id.toString())
                exchange(path, params, body.toString().toRequestBody("application/json".toMediaType()))
            } else {
                val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("request_id", draft.id.toString())
                    .addFormDataPart("message", draft.content)
                    .addFormDataPart("file", file.name, file.bytes.toRequestBody(file.type.toMediaType()))
                    .build()
                exchange("$path/send-file", params, form)
            }
            verify(response as JSONObject, draft)
        }

        suspend fun receipt(draft: StudentChatDraft): JSONObject {
            val row = exchange("$path/sent/${draft.id}", params) as JSONObject
            verify(row, draft)
            return row
        }

        private fun verify(row: JSONObject, draft: StudentChatDraft) {
            check(row.getString("id") == draft.id.toString() && row.getBoolean("is_current_user") &&
                row.getString("receiver_id") == partnerId && row.getString("sender_id") == requireNotNull(actor) &&
                row.getString("content") == draft.content)
            val file = draft.file
            if (file == null) {
                check(row.isNull("attachment_name") && row.isNull("attachment_sha256"))
            } else {
                check(row.studentChatAttachment() == StudentChatAttachment(file.name, file.digest))
            }
        }

        suspend fun download(message: JSONObject): ChatFile = scope.io {
            val id = chatUuid(message.getString("id"))
            val attachment = requireNotNull(message.studentChatAttachment())
            http.newCall(request("$path/attachments/$id", params).build()).execute().use { response ->
                if (!response.isSuccessful) throw RegistryHttpException(response.code)
                val file = ChatFile(attachment.name, requireNotNull(response.body).byteStream().use { it.readChatFile() })
                check(file.digest == attachment.digest) { "Dosya özeti sunucu kaydıyla eşleşmiyor" }
                file
            }
        }
    }
}

private fun chatUuid(value: String) = UUID.fromString(value).toString()
