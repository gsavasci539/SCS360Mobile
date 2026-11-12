package com.example.data.remote

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID

data class DebtCreateDraft(val studentId: String, val type: String, val amount: String, val due: String, val description: String)
class DebtCreateHttpException(val code: Int) : java.io.IOException("Tahakkuk isteği HTTP $code")

/** One durable create operation per actor/workspace/branch; display objects never become payloads. */
class NativeDebtCreateSession(context: Context, actorId: String) {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val actor = UUID.fromString(actorId).toString()
    private val workspace = ApiClientProvider.interceptor.workspaceOrganizationId.orEmpty()
    private val branch = ApiClientProvider.interceptor.branchId.orEmpty()
    private val prefs = context.getSharedPreferences("debt_create_requests_v1", Context.MODE_PRIVATE)
    private val key = MessageDigest.getInstance("SHA-256").digest("$base|$actor|$workspace|$branch".toByteArray()).joinToString("") { "%02x".format(it) }
    private val mutex = locks.getOrPut(key) { Mutex() }
    private companion object { val locks = java.util.concurrent.ConcurrentHashMap<String, Mutex>() }
    private val client = ApiClientProvider.createOkHttpClient()
    private var body: String? = null
    private var acknowledged: String? = null
    private var students = emptySet<String>()
    val hasPending get() = body != null && acknowledged == null
    fun current() = scope.current()
    fun pendingDraft(): DebtCreateDraft? = body?.let { val row = JSONObject(it)
        DebtCreateDraft(row.getString("student_id"), row.getString("debt_type"), row.getString("amount"), row.getString("due_date"), row.getString("description")) }
    private suspend fun request(path: String, payload: String? = null): JSONObject = scope.io {
        val builder = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (payload != null) builder.post(payload.toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw DebtCreateHttpException(response.code)
            val value = JSONObject(response.body?.string() ?: "null")
            if (value.has("success")) { check(value.getBoolean("success")); value.getJSONObject("data") } else value
        }
    }
    suspend fun load(): List<RegistryChoice> = mutex.withLock {
        scope.ensureCurrent(); body = prefs.getString(key, null)
        body?.let { UUID.fromString(JSONObject(it).getString("request_id")) }
        val rows = mutableListOf<RegistryChoice>(); var page = 1; var total: Int? = null
        while (true) {
            val value = request("debts/student-options?page=$page&page_size=100" + if (branch.isBlank()) "" else "&branch_id=${UUID.fromString(branch)}")
            val count = value.getInt("total"); check(count in 0..100000 && (total == null || total == count)); total = count
            val items = value.getJSONArray("items")
            for (i in 0 until items.length()) { val row = items.getJSONObject(i)
                rows.add(RegistryChoice(UUID.fromString(row.getString("id")).toString(), row.getString("name"))) }
            check(rows.map { it.id }.distinct().size == rows.size && rows.size <= count)
            if (rows.size == count) break
            check(items.length() > 0); page++
        }
        students = rows.map { it.id }.toSet(); rows
    }
    suspend fun create(draft: DebtCreateDraft): String = mutex.withLock {
        scope.ensureCurrent(); acknowledged?.let { return@withLock it }
        val amount = draft.amount.trim().replace(',', '.').toBigDecimal()
        require(amount > BigDecimal.ZERO && amount <= BigDecimal("9999999999.99") && amount.stripTrailingZeros().scale() <= 2) { "Geçerli kuruşlu tutar girin" }
        require(draft.type in setOf("EDUCATION", "FOOD", "SERVICE", "STATIONERY", "ACTIVITY", "OTHER"))
        val student = UUID.fromString(draft.studentId).toString(); val due = LocalDate.parse(draft.due).toString()
        require(draft.description.trim().isNotEmpty()) { "Açıklama girin" }
        val canonical = JSONObject().put("student_id", student).put("debt_type", draft.type).put("amount", amount.setScale(2).toPlainString())
            .put("due_date", due).put("description", draft.description.trim())
        if (body == null) body = prefs.getString(key, null)
        if (body == null) {
            check(student in students) { "Güncel yetkili öğrenci listesinden seçim yapın" }
            val pending = canonical.put("request_id", UUID.randomUUID().toString()).toString()
            check(prefs.edit().putString(key, pending).commit()) { "İstek kimliği saklanamadı; işlem gönderilmedi" }; body = pending
        } else {
            val saved = JSONObject(body!!)
            for (field in listOf("student_id", "debt_type", "amount", "due_date", "description"))
                check(saved.getString(field) == canonical.getString(field)) { "Bekleyen isteğin içeriği değiştirilemez; aynı isteği doğrulayın" }
        }
        val result = request("debts", body)
        val id = UUID.fromString(result.getString("id")).toString()
        check(id == JSONObject(body!!).getString("request_id")) { "Sunucu tahakkuk kimliği eşleşmedi" }
        acknowledged = id; id
    }
    fun finish() {
        scope.ensureCurrent(); val id = checkNotNull(acknowledged)
        val saved = prefs.getString(key, null)
        // An older dialog must never delete a newer operation's durable identity.
        if (saved != null && JSONObject(saved).getString("request_id") == id) check(prefs.edit().remove(key).commit())
        body = null
    }
}
