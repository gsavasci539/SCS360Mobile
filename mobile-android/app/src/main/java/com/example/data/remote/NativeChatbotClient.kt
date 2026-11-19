package com.example.data.remote

import com.example.data.model.UserRole
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChatbotTurn(val text: String, val isUser: Boolean, val intent: String? = null)
data class ChatbotAnswer(val text: String, val intent: String, val actions: List<Pair<String, String>>)

class NativeChatbotClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient().newBuilder().retryOnConnectionFailure(false).build()

    suspend fun ask(query: String, role: UserRole, studentId: String?, history: List<ChatbotTurn>): ChatbotAnswer = scope.io {
        val route = if (role == UserRole.PARENT) "parent/chatbot" else "v1/chatbot/process-query"
        val turns = JSONArray()
        for (turn in history.filter { it.text.isNotBlank() }.takeLast(6)) {
            turns.put(JSONObject().put("role", if (turn.isUser) "user" else "assistant")
                .put("content", turn.text.take(2000)).put("intent", turn.intent?.take(64) ?: JSONObject.NULL))
        }
        val payload = JSONObject().put("query", query).put("role", role.name)
            .put("student_id", studentId?.let { UUID.fromString(it).toString() } ?: JSONObject.NULL)
            .put("conversation_history", turns)
        val request = Request.Builder().url(base + route).tag(NativeRequestScope::class.java, scope)
            .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val envelope = JSONObject(response.body?.string().orEmpty())
            check(envelope.getBoolean("success"))
            val result = envelope.getJSONObject("data")
            val actions = result.optJSONArray("suggested_actions") ?: JSONArray()
            ChatbotAnswer(result.getString("response_markdown"), result.getString("intent"),
                (0 until actions.length()).map { actions.getJSONObject(it) }.mapNotNull {
                    val tag = it.optString("action_tag")
                    val label = it.optString("label")
                    if (tag.isBlank() || label.isBlank()) null else tag to label
                })
        }
    }
}
