package com.example.data.remote

import com.example.data.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/** Actual platform records only; no sample catalog, locally paid invoice or period arithmetic. */
class SaasApi {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val token = ApiClientProvider.interceptor.authToken
    private val workspace = ApiClientProvider.interceptor.workspaceOrganizationId
    private val base = ApiClientProvider.activeBaseUrl
    fun current() = scope.current()
    private val client = ApiClientProvider.createOkHttpClient().newBuilder().addInterceptor { chain ->
        check(current() && !token.isNullOrBlank()) { "Oturum değişti" }
        val builder = chain.request().newBuilder().header("Authorization", "Bearer $token")
        if (workspace == null) builder.removeHeader("X-SCS360-Organization-Id") else builder.header("X-SCS360-Organization-Id", workspace)
        chain.proceed(builder.build())
    }.build()
    suspend fun request(path: String, method: String = "GET", body: JSONObject? = null): Any = scope.io {
        check(current()) { "Oturum değişti" }
        val builder = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (method != "GET") builder.method(method, (body ?: JSONObject()).toString().toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use { response ->
            check(response.isSuccessful) { "Üyelik isteği doğrulanamadı (HTTP ${response.code})" }
            val result = JSONTokener(response.body?.string() ?: "null").nextValue()
            check(current()) { "Oturum değişti" }
            if (result is JSONObject && result.optBoolean("success") && result.has("data")) result.get("data") else result
        }
    }
    private suspend fun pages(path: String): List<JSONObject> {
        val rows = mutableListOf<JSONObject>()
        while (true) {
            val page = request("$path?limit=100&offset=${rows.size}") as JSONArray
            for (i in 0 until page.length()) rows.add(page.getJSONObject(i))
            if (page.length() < 100) return rows
            check(rows.size < 100000) { "Üyelik listesi tamamlanamadı" }
        }
    }
    data class Snapshot(val access: OrganizationAccessStatus, val plans: List<SubscriptionPlan>, val invoices: List<SubscriptionInvoice>, val tenants: List<PlatformTenantSummary>)
    suspend fun snapshot(root: Boolean): Snapshot {
        val raw = request("subscription/access") as JSONObject
        val manager = raw.optBoolean("can_manage")
        val plans = if (root || manager) request(if (root) "platform/plans" else "subscription/plans") as JSONArray else JSONArray()
        return Snapshot(access(raw), (0 until plans.length()).map { plan(plans.getJSONObject(it)) },
            if (root || manager) pages(if (root) "platform/invoices" else "subscription/invoices").map(::invoice) else emptyList(),
            if (root) pages("platform/tenants").map(::tenant) else emptyList())
    }
    fun planBody(plan: SubscriptionPlan) = JSONObject().put("code", plan.code).put("name", plan.name).put("price", plan.price)
        .put("currency", plan.currency).put("period_months", if (plan.billing_period == "YEARLY") 12 else 1)
        .put("branch_limit", plan.branch_limit).put("student_limit", plan.student_limit).put("user_limit", plan.user_limit).put("is_active", plan.is_active)
    suspend fun updateMembership(tenant: PlatformTenantSummary, planId: String, start: String, end: String): JSONObject {
        val organization = UUID.fromString(tenant.organization_id)
        val body = membershipBody(tenant, planId, start, end)
        return request("platform/tenants/$organization/subscription", "PUT", body) as JSONObject
    }
    companion object {
        private fun membershipInstant(value: String): Instant {
            val text = value.trim()
            return if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(text)) LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC)
                else OffsetDateTime.parse(text).toInstant()
        }
        fun membershipBody(tenant: PlatformTenantSummary, planId: String, start: String, end: String): JSONObject {
            val selectedPlan = UUID.fromString(planId)
            val from = membershipInstant(start)
            val until = membershipInstant(end)
            require(until > from) { "Bitiş tarihi başlangıçtan sonra olmalıdır." }
            // Editing a period must not reopen an administratively suspended institution.
            val status = if (!tenant.is_active || tenant.access_status == "SUSPENDED") "SUSPENDED" else "ACTIVE"
            return JSONObject().put("plan_id", selectedPlan.toString()).put("period_start", from.toString())
                .put("period_end", until.toString()).put("admin_status", status)
        }
        private fun period(row: JSONObject) = if (row.optInt("period_months") == 12) "YEARLY" else "MONTHLY"
        fun plan(row: JSONObject) = SubscriptionPlan(id = row.getString("id"), code = row.getString("code"), name = row.getString("name"),
            price = row.getString("price").toDouble(), currency = row.getString("currency"), billing_period = period(row),
            branch_limit = row.getInt("branch_limit"), student_limit = row.getInt("student_limit"), user_limit = row.getInt("user_limit"), is_active = row.getBoolean("is_active"))
        fun invoice(row: JSONObject) = SubscriptionInvoice(id = row.getString("id"), invoice_no = row.getString("id"), organization_id = row.getString("organization_id"),
            organization_name = row.optString("organization_name", ""), plan_id = row.getString("plan_id"), plan_name = row.optString("package_name", ""),
            amount = row.getString("amount").toDouble(), currency = row.getString("currency"), status = row.getString("status"),
            valid_until = row.optString("valid_until", ""), paid_at = if (row.isNull("paid_at")) null else row.optString("paid_at"), created_at = row.optString("created_at", ""))
        fun access(row: JSONObject): OrganizationAccessStatus {
            val state = row.getJSONObject("access")
            val usage = row.optJSONObject("usage") ?: JSONObject()
            val pack = row.optJSONObject("package") ?: JSONObject()
            val status = if (state.optBoolean("restricted")) (if (state.optString("status") == "SUSPENDED") "SUSPENDED" else "RESTRICTED") else state.getString("status")
            return OrganizationAccessStatus(organization_id = if (row.isNull("organization_id")) "" else row.optString("organization_id"), organization_name = "Kurum üyeliği",
                status = status, plan_id = pack.optString("id", ""), plan_name = pack.optString("name", ""), billing_period = period(pack),
                period_end = if (state.isNull("period_end")) "" else state.optString("period_end"), grace_until = if (state.isNull("grace_end")) "" else state.optString("grace_end"),
                remaining_grace_days = if (state.optString("status") == "GRACE") state.optInt("days_remaining") else 0,
                active_branches = usage.optInt("branch_limit"), active_students = usage.optInt("student_limit"), active_users = usage.optInt("user_limit"),
                branch_limit = pack.optInt("branch_limit"), student_limit = pack.optInt("student_limit"), user_limit = pack.optInt("user_limit"),
                saas_iyzico_enabled = false, is_billing_authorized = row.optBoolean("can_manage"))
        }
        fun tenant(row: JSONObject): PlatformTenantSummary {
            val usage = row.optJSONObject("usage") ?: JSONObject()
            val pack = row.optJSONObject("package") ?: JSONObject()
            val state = row.getJSONObject("access")
            return PlatformTenantSummary(organization_id = row.getString("id"), organization_name = row.getString("name"), is_active = row.getBoolean("is_active"),
                access_status = state.getString("status"), plan_id = pack.optString("id", ""), plan_name = pack.optString("name", ""), billing_period = period(pack),
                period_end = if (state.isNull("period_end")) "" else state.optString("period_end"), grace_until = if (state.isNull("grace_end")) "" else state.optString("grace_end"),
                active_branches = usage.optInt("branch_limit"), active_students = usage.optInt("student_limit"), active_users = usage.optInt("user_limit"),
                branch_limit = pack.optInt("branch_limit"), student_limit = pack.optInt("student_limit"), user_limit = pack.optInt("user_limit"),
                period_start = if (row.isNull("period_start")) "" else row.optString("period_start"))
        }
    }
}
