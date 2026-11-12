package com.example.data.remote

import android.content.Context
import com.example.data.model.Expense
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID

class ExpenseHttpException(val code: Int) : java.io.IOException("Gider işlemi HTTP $code")
data class ExpenseDraft(val branchId: String?, val title: String, val category: String, val amount: String, val date: String)

class NativeExpenseClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    val base = ApiClientProvider.activeBaseUrl
    val branch = ApiClientProvider.interceptor.branchId
    val workspace = ApiClientProvider.interceptor.workspaceOrganizationId
    private val client = ApiClientProvider.createOkHttpClient()
    fun current() = scope.current()
    fun ensureCurrent() = scope.ensureCurrent()
    suspend fun request(path: String, body: String? = null, delete: Boolean = false): JSONObject = scope.io {
        val builder = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (body != null) builder.post(body.toRequestBody("application/json".toMediaType()))
        if (delete) builder.delete()
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw ExpenseHttpException(response.code)
            val value = JSONObject(response.body?.string() ?: "null")
            if (value.has("success")) { check(value.getBoolean("success")); value.optJSONObject("data") ?: JSONObject() } else value
        }
    }
    fun parse(row: JSONObject): Expense {
        fun uuid(key: String) = UUID.fromString(row.getString(key)).toString()
        fun text(key: String) = if (row.isNull(key)) "" else row.getString(key)
        val org = uuid("organization_id"); val selected = if (row.isNull("branch_id")) "" else uuid("branch_id")
        check(workspace.isNullOrBlank() || workspace == org) { "Gider kurumu eşleşmedi" }
        check(branch == null || branch == selected) { "Gider şubesi eşleşmedi" }
        val amount = money(row.getString("amount"))
        val title = row.getString("title"); val category = row.getString("category")
        check(title.isNotBlank() && category.isNotBlank())
        return Expense(id=uuid("id"), branch_id=selected, category=category, title=title, amount=amount.toDouble(),
            date=LocalDate.parse(row.getString("date")).toString(), receipt_url=null, created_by=text("created_by"))
    }
    suspend fun load(branchId: String?): List<Expense> {
        val selected = branchId?.takeUnless { it.isBlank() || it == "all" } ?: branch
        require(selected == branch) { "Şube seçimi değişti; yeniden yükleyin" }
        val rows = mutableListOf<Expense>(); var page = 1; var total: Int? = null
        while (true) {
            val data = request("expenses?page=$page&page_size=100" + (branch?.let { "&branch_id=${UUID.fromString(it)}" } ?: ""))
            val count = data.getInt("total"); check(count in 0..100000 && (total == null || total == count)); total = count
            val items = data.getJSONArray("items")
            for (i in 0 until items.length()) rows.add(parse(items.getJSONObject(i)))
            check(rows.map { it.id }.distinct().size == rows.size && rows.size <= count)
            if (rows.size == count) return rows
            check(items.length() > 0); page++
        }
    }
    suspend fun delete(id: String) { request("expenses/${UUID.fromString(id)}", delete=true) }
    companion object {
        fun money(value: String): BigDecimal = value.trim().replace(',', '.').toBigDecimal().also {
            require(it > BigDecimal.ZERO && it <= BigDecimal("9999999999.99") && it.stripTrailingZeros().scale() <= 2) { "Geçerli kuruşlu tutar girin" }
        }
    }
}

/** Durable identity survives a lost acknowledgement and never becomes a local expense row. */
class NativeExpenseCreateSession(context: Context, actorId: String) {
    private val api = NativeExpenseClient()
    private val actor = UUID.fromString(actorId).toString()
    private val key = MessageDigest.getInstance("SHA-256").digest("${api.base}|$actor|${api.workspace}|${api.branch}".toByteArray()).joinToString("") { "%02x".format(it) }
    private val prefs = context.getSharedPreferences("expense_create_requests_v1", Context.MODE_PRIVATE)
    private val mutex = locks.getOrPut(key) { Mutex() }
    private companion object { val locks = java.util.concurrent.ConcurrentHashMap<String, Mutex>() }
    private var body: String? = null
    private var acknowledged: Expense? = null
    private var branches = emptySet<String>()
    var allowsOrganizationWide = false; private set
    val selectedBranch get() = api.branch
    val hasPending get() = body != null && acknowledged == null
    fun current() = api.current()
    fun pendingDraft(): ExpenseDraft? = body?.let { val row=JSONObject(it)
        ExpenseDraft(if (row.isNull("branch_id")) null else row.getString("branch_id"), row.getString("title"), row.getString("category"), row.getString("amount"), row.getString("date")) }
    suspend fun load(): List<RegistryChoice> = mutex.withLock {
        api.ensureCurrent(); body=prefs.getString(key,null)
        body?.let { UUID.fromString(JSONObject(it).getString("request_id")) }
        val rows=mutableListOf<RegistryChoice>(); var page=1; var total:Int?=null; var wide:Boolean?=null
        while (true) {
            val data=api.request("expenses/branch-options?page=$page&page_size=100"+(api.branch?.let { "&branch_id=${UUID.fromString(it)}" } ?: ""))
            val count=data.getInt("total"); val allowed=data.getBoolean("allows_organization_wide")
            check(count in 0..100000 && (total==null || total==count) && (wide==null || wide==allowed)); total=count; wide=allowed
            val items=data.getJSONArray("items")
            for (i in 0 until items.length()) { val row=items.getJSONObject(i); rows.add(RegistryChoice(UUID.fromString(row.getString("id")).toString(),row.getString("name"))) }
            check(rows.map { it.id }.distinct().size==rows.size && rows.size<=count)
            if (rows.size==count) break
            check(items.length()>0); page++
        }
        branches=rows.map { it.id }.toSet(); allowsOrganizationWide=wide==true && api.branch==null; rows
    }
    suspend fun create(draft: ExpenseDraft): Expense = mutex.withLock {
        api.ensureCurrent(); acknowledged?.let { return@withLock it }
        val amount=NativeExpenseClient.money(draft.amount).setScale(2).toPlainString()
        val branch=draft.branchId?.let { UUID.fromString(it).toString() }
        require(api.branch==null || api.branch==branch)
        val title=draft.title.trim(); val category=draft.category.trim(); val date=LocalDate.parse(draft.date).toString()
        require(title.isNotEmpty() && title.length<=500 && category.isNotEmpty() && category.length<=100)
        val canonical=JSONObject().put("branch_id",branch ?: JSONObject.NULL).put("title",title).put("category",category).put("amount",amount).put("date",date)
        if (body==null) body=prefs.getString(key,null)
        if (body==null) {
            check(if(branch==null) allowsOrganizationWide else branch in branches) { "Yetkili kapsamı seçin" }
            val pending=canonical.put("request_id",UUID.randomUUID().toString()).toString()
            check(prefs.edit().putString(key,pending).commit()) { "İstek kimliği saklanamadı; gider gönderilmedi" }; body=pending
        } else {
            val saved=JSONObject(body!!)
            for (field in listOf("branch_id","title","category","amount","date")) check(saved.get(field).toString()==canonical.get(field).toString()) { "Bekleyen gider içeriği değiştirilemez" }
        }
        val result=api.parse(api.request("expenses",body)); check(result.id==JSONObject(body!!).getString("request_id"))
        check(result.branch_id==branch.orEmpty() && result.title==title && result.category==category && result.amount.toBigDecimal().compareTo(amount.toBigDecimal())==0 && result.date==date)
        acknowledged=result; result
    }
    fun finish() {
        api.ensureCurrent(); val id=checkNotNull(acknowledged).id; val saved=prefs.getString(key,null)
        if (saved!=null && JSONObject(saved).getString("request_id")==id) check(prefs.edit().remove(key).commit())
        body=null
    }
}
