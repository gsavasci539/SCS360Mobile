package com.example.data.remote

import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

data class MessageRecipient(val id: String, val name: String, val roles: List<String>)

data class DirectMessageDraft(
    val recipientId: String,
    val content: String,
    val id: UUID = UUID.randomUUID(),
    val file: ChatFile? = null,
)

class NativeMessagingClient(actorId: String, branchId: String?) {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient(unwrapApiData = false).newBuilder()
        .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()
    private val actor = uuid(actorId)
    private val branch = branchId?.let(::uuid)
    private val schoolZone = ZoneId.of("Europe/Istanbul")
    private val timestampDisplay = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(schoolZone)

    fun current() = scope.current()

    private suspend fun get(path: String, vararg params: Pair<String, String>): Any = exchange(path, null, *params)

    private fun request(path: String, vararg params: Pair<String, String>): Request.Builder {
        val url = (base + "messages/" + path).toHttpUrl().newBuilder()
        branch?.let { url.addQueryParameter("branch_id", it) }
        for ((key, value) in params) url.addQueryParameter(key, value)
        return Request.Builder().url(url.build()).tag(NativeRequestScope::class.java, scope)
    }

    private suspend fun exchange(path: String, body: RequestBody?, vararg params: Pair<String, String>): Any = scope.io {
        val request = request(path, *params)
        if (body != null) request.post(body)
        http.newCall(request.build()).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val envelope = JSONObject(response.body?.string().orEmpty())
            check(envelope.getBoolean("success")) { "Sunucu mesaj kaydını doğrulamadı" }
            envelope.get("data")
        }
    }

    suspend fun conversations(): List<ChatConversation> =
        (get("conversations") as JSONArray).objects().map { row ->
            val partner = uuid(row.getString("user_id"))
            val name = row.getString("name")
            val message = row.getJSONObject("last_message").toMessage(partner, name)
            ChatConversation(id = partner, participant_id = partner, participant_name = name,
                participant_role = if (message.is_me) "" else message.sender_role,
                last_message = message.text.ifBlank { message.attachment_name.orEmpty() }, last_message_time = message.timestamp,
                unread_count = row.getInt("unread_count"))
        }

    suspend fun recipients(): List<MessageRecipient> {
        val contacts = linkedMapOf<String, MessageRecipient>()
        var page = 1
        do {
            val result = get("recipients", "page" to page.toString(), "page_size" to "100") as JSONObject
            check(result.getInt("page") == page)
            for (row in result.getJSONArray("items").objects()) {
                val key = uuid(row.getString("id"))
                check(key != actor)
                val roles = row.getJSONArray("roles")
                contacts[key] = MessageRecipient(key, row.getString("name"), (0 until roles.length()).map(roles::getString))
            }
            page++
        } while (page <= result.getInt("total_pages"))
        return contacts.values.toList()
    }

    suspend fun history(partner: ChatConversation, before: String? = null): List<ChatMessage> {
        val params = mutableListOf("limit" to HISTORY_PAGE_SIZE.toString())
        if (before != null) params += "before_id" to uuid(before)
        val id = uuid(partner.id)
        return (get("conversation/$id", *params.toTypedArray()) as JSONArray).objects()
            .map { it.toMessage(id, partner.participant_name) }
    }

    suspend fun send(draft: DirectMessageDraft) {
        val file = draft.file
        if (file != null) {
            val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("request_id", draft.id.toString())
                .addFormDataPart("recipient_id", uuid(draft.recipientId))
                .addFormDataPart("content", draft.content)
                .addFormDataPart("file", file.name, file.bytes.toRequestBody(file.type.toMediaType()))
            branch?.let { form.addFormDataPart("branch_id", it) }
            verify(exchange("send-file", form.build()) as JSONObject, draft)
            return
        }
        val payload = JSONObject().put("request_id", draft.id.toString())
            .put("recipient_id", uuid(draft.recipientId)).put("content", draft.content)
        branch?.let { payload.put("branch_id", it) }
        verify(exchange("send", payload.toString().toRequestBody("application/json".toMediaType())) as JSONObject, draft)
    }

    suspend fun receipt(draft: DirectMessageDraft, recipientName: String): ChatMessage =
        verify(get("sent/${draft.id}") as JSONObject, draft).toMessage(uuid(draft.recipientId), recipientName)

    private fun verify(row: JSONObject, draft: DirectMessageDraft): JSONObject {
        check(uuid(row.getString("id")) == draft.id.toString() && uuid(row.getString("sender_id")) == actor &&
            uuid(row.getString("recipient_id")) == uuid(draft.recipientId) && row.getString("content") == draft.content)
        val attachment = row.attachment(draft.id.toString())
        check(attachment?.first == draft.file?.name && attachment?.second == draft.file?.digest)
        return row
    }

    suspend fun download(message: ChatMessage): ChatFile = scope.io {
        val name = requireNotNull(message.attachment_name)
        val digest = requireNotNull(message.attachment_sha256)
        val id = uuid(message.id)
        http.newCall(request("$id/attachment").build()).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val file = ChatFile(name, requireNotNull(response.body).byteStream().use { it.readChatFile() })
            check(file.digest == digest) { "Dosya özeti sunucu kaydıyla eşleşmiyor" }
            file
        }
    }

    private fun JSONObject.toMessage(partner: String, name: String): ChatMessage {
        val sender = uuid(getString("sender_id"))
        val recipient = uuid(getString("recipient_id"))
        check((sender == actor && recipient == partner) || (sender == partner && recipient == actor))
        val raw = getString("sent_at")
        val instantly = runCatching { Instant.parse(raw) }.getOrElse {
            runCatching { java.time.OffsetDateTime.parse(raw).toInstant() }.getOrElse {
                LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC)
            }
        }
        val mine = sender == actor
        val role = when (optString("sender_role")) {
            "TEACHER" -> "Öğretmen"
            "PARENT" -> "Veli"
            "STAFF" -> "Personel"
            else -> ""
        }
        val id = uuid(getString("id"))
        val attached = attachment(id)
        return ChatMessage(id = id, conversation_id = partner, sender_id = sender,
            recipient_id = recipient, sender_name = if (mine) "" else name, sender_role = role,
            recipient_name = if (mine) name else "", text = getString("content"),
            timestamp = timestampDisplay.format(instantly), date = instantly.atZone(schoolZone).toLocalDate().toString(),
            is_me = mine, is_read = getBoolean("is_read"),
            attachment_name = attached?.first, attachment_sha256 = attached?.second)
    }

    private fun JSONObject.attachment(id: String): Pair<String, String>? {
        if (optString("attachment_url") != "/api/messages/$id/attachment") return null
        val name = runCatching { chatFileName(getString("attachment_name")) }.getOrNull() ?: return null
        val digest = optString("attachment_sha256")
        return if (digest.matches(Regex("[a-f0-9]{64}"))) name to digest else null
    }

    private fun uuid(value: String) = UUID.fromString(value).toString()
    private fun JSONArray.objects() = (0 until length()).map(::getJSONObject)

    companion object {
        const val HISTORY_PAGE_SIZE = 50
    }
}
