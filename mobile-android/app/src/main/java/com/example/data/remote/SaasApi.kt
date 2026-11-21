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
    suspend fun savePlan(draft: SubscriptionPlan): SubscriptionPlan {
        val identifier = draft.id.takeIf(String::isNotBlank)?.let(UUID::fromString)
        val path = identifier?.let { "platform/plans/$it" } ?: "platform/plans"
        val written = plan(request(path, if (identifier == null) "POST" else "PUT", planBody(draft)) as JSONObject)
        val actualId = UUID.fromString(written.id)
        val expected = draft.copy(id = actualId.toString())
        check((identifier == null || identifier == actualId) && written == expected) { "Paket yanıtı doğrulanamadı." }
        val rows = request("platform/plans") as JSONArray
        val matching = (0 until rows.length()).map(rows::getJSONObject).filter { it.getString("id") == actualId.toString() }
        val saved = plan(matching.single())
        check(saved == expected) { "Kaydedilen paket doğrulanamadı." }
        return saved
    }
    suspend fun saveInvoiceOperation(target: SubscriptionInvoice, draft: PlatformInvoiceDraft): SubscriptionInvoice {
        val id = UUID.fromString(target.id)
        val organization = UUID.fromString(target.organization_id)
        val endpoint = if (draft.operation == PlatformInvoiceOperation.SETTLE) "settle" else "review"
        val acknowledged = invoice(request("platform/invoices/$id/$endpoint", "POST", draft.payload(target)) as JSONObject)
        val expectedStatus = when (draft.operation) {
            PlatformInvoiceOperation.SETTLE -> acknowledged.status == "PAID"
            PlatformInvoiceOperation.APPLY -> acknowledged.status == "PAID" && acknowledged.checkout_status == "PAID"
            PlatformInvoiceOperation.REFUND_RECORDED ->
                acknowledged.status in setOf("PAID", "REFUNDED") && acknowledged.checkout_status == "REFUNDED"
        }
        check(sameInvoice(acknowledged, target) && expectedStatus) { "Fatura işlem yanıtı doğrulanamadı." }
        val saved = readConfirmedInvoice(acknowledged, platform = true)
        if (draft.operation != PlatformInvoiceOperation.SETTLE) {
            check(saved.checkout_status == acknowledged.checkout_status && saved.checkout_id == acknowledged.checkout_id) {
                "Kaydedilen ödeme oturumu doğrulanamadı."
            }
        }
        return saved
    }

    private fun sameInvoice(left: SubscriptionInvoice, right: SubscriptionInvoice): Boolean =
        left.id == right.id && left.organization_id == right.organization_id &&
            left.plan_id == right.plan_id && left.amount.isFinite() && left.amount == right.amount &&
            left.currency.isNotBlank() && left.currency == right.currency

    private suspend fun readConfirmedInvoice(written: SubscriptionInvoice, platform: Boolean = false): SubscriptionInvoice {
        val id = UUID.fromString(written.id)
        val organization = UUID.fromString(written.organization_id)
        val scopePath = if (platform) "platform" else "subscription"
        val organizationFilter = if (platform) "&organization_id=$organization" else ""
        val rows = request("$scopePath/invoices?record_id=$id$organizationFilter&limit=1") as JSONArray
        check(rows.length() == 1) { "Kaydedilen fatura yeniden okunamadı." }
        val saved = invoice(rows.getJSONObject(0))
        check(sameInvoice(saved, written) && saved.status == written.status) {
            "Kaydedilen fatura durumu doğrulanamadı."
        }
        return saved
    }
    suspend fun requestFreePackage(organizationId: String, plan: SubscriptionPlan, requestId: String): SubscriptionInvoice {
        val organization = UUID.fromString(organizationId).toString()
        val planId = UUID.fromString(plan.id).toString()
        require(plan.is_active && plan.price == 0.0) { "Etkin ve sıfır tutarlı bir paket seçin." }
        val body = JSONObject().put("plan_id", planId).put("request_id", UUID.fromString(requestId).toString()).put("free_only", true)
        val written = invoice(request("subscription/invoices", "POST", body) as JSONObject)
        check(written.organization_id == organization && written.plan_id == planId &&
            written.amount == 0.0 && written.currency == plan.currency && written.status in setOf("PENDING", "PAID")) {
            "Ücretsiz paket talebi doğrulanamadı."
        }
        return readConfirmedInvoice(written)
    }

    suspend fun changeOwnerInvoice(target: SubscriptionInvoice, activateFree: Boolean): SubscriptionInvoice {
        require(target.status == "PENDING" && target.can_cancel) { "Ödeme talebi bu işleme açık değil." }
        require(!activateFree || target.amount == 0.0) { "Yalnız sıfır tutarlı paket etkinleştirilebilir." }
        val id = UUID.fromString(target.id)
        val action = if (activateFree) "activate-free" else "cancel"
        val response = request("subscription/invoices/$id/$action", "POST") as JSONObject
        val written = invoice(if (activateFree) response.getJSONObject("invoice") else response)
        check(sameInvoice(written, target) && written.status == (if (activateFree) "PAID" else "CANCELLED")) {
            "Üyelik işlemi doğrulanamadı."
        }
        return readConfirmedInvoice(written)
    }

    suspend fun checkInvoiceStatus(target: SubscriptionInvoice, platform: Boolean): SubscriptionInvoice {
        val id = UUID.fromString(target.id)
        val scopePath = if (platform) "platform" else "subscription"
        val response = request("$scopePath/invoices/$id/payment-status", "POST") as JSONObject
        val written = invoice(response.getJSONObject("invoice"))
        val status = response.getString("status")
        check(response.has("checkout_id") && sameInvoice(written, target)) { "Ödeme durum yanıtı doğrulanamadı." }
        val checkoutId = if (response.isNull("checkout_id")) null else UUID.fromString(response.getString("checkout_id")).toString()
        check(target.checkout_id == null || target.checkout_id == checkoutId) { "Ödeme oturumu değişti. Listeyi yenileyin." }
        val saved = readConfirmedInvoice(written, platform)
        check(saved.checkout_id == checkoutId && if (checkoutId == null) status == saved.status && saved.checkout_status == null
            else saved.checkout_status == status) {
            "Kaydedilen ödeme oturumu doğrulanamadı."
        }
        return saved
    }
    suspend fun tenantRecord(organizationId: String): JSONObject {
        val organization = UUID.fromString(organizationId).toString()
        val rows = request("platform/tenants?record_id=$organization&limit=1") as JSONArray
        check(rows.length() == 1) { "Kurum kaydı yeniden okunamadı." }
        return rows.getJSONObject(0).also { check(it.getString("id") == organization) { "Kurum kimliği doğrulanamadı." } }
    }

    suspend fun tenantBillingUsers(organizationId: String): List<TenantBillingUser> {
        val uuid = UUID.fromString(organizationId)
        val rows = request("platform/tenants/$uuid/users") as JSONArray
        return (0 until rows.length()).map(rows::getJSONObject).map {
            TenantBillingUser(UUID.fromString(it.getString("id")).toString(), it.getString("name"), it.getString("email"))
        }.distinctBy(TenantBillingUser::id)
    }

    suspend fun updateMembership(tenant: PlatformTenantSummary, draft: TenantMembershipDraft): PlatformTenantSummary {
        val organization = UUID.fromString(tenant.organization_id)
        val body = membershipBody(tenant, draft.planId, draft.periodStart, draft.periodEnd, draft.adminStatus, draft.billingOwnerId)
        val written = request("platform/tenants/$organization/subscription", "PUT", body) as JSONObject
        val expectedStart = body.getString("period_start")
        val expectedEnd = body.getString("period_end")
        val writtenOwner = if (written.isNull("owner_user_id")) null else written.getString("owner_user_id")
        check(written.getString("organization_id") == organization.toString() && written.getString("plan_id") == draft.planId &&
            written.getString("admin_status") == draft.adminStatus && writtenOwner == draft.billingOwnerId &&
            membershipInstant(written.getString("period_start")) == membershipInstant(expectedStart) &&
            membershipInstant(written.getString("period_end")) == membershipInstant(expectedEnd)) {
            "Üyelik kayıt yanıtı doğrulanamadı."
        }
        val saved = tenant(tenantRecord(organization.toString()))
        check(saved.plan_id == draft.planId && saved.admin_status == draft.adminStatus && saved.billing_owner_id == draft.billingOwnerId &&
            membershipInstant(saved.period_start) == membershipInstant(expectedStart) &&
            membershipInstant(saved.period_end) == membershipInstant(expectedEnd)) { "Sunucudan okunan üyelik doğrulanamadı." }
        return saved
    }

    suspend fun setTenantActive(organizationId: String, active: Boolean): PlatformTenantSummary {
        val organization = UUID.fromString(organizationId).toString()
        val body = JSONObject().put("is_active", active)
        val written = request("platform/tenants/$organization/status", "PATCH", body) as JSONObject
        check(written.getString("id") == organization && written.getBoolean("is_active") == active) {
            "Kurum durum yanıtı doğrulanamadı."
        }
        return tenant(tenantRecord(organization)).also { check(it.is_active == active) { "Kurum durumu doğrulanamadı." } }
    }
    companion object {
        internal fun membershipInstant(value: String): Instant {
            val text = value.trim()
            return if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(text)) LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC)
                else OffsetDateTime.parse(text).toInstant()
        }
        fun membershipBody(tenant: PlatformTenantSummary, planId: String, start: String, end: String,
            adminStatus: String = tenant.admin_status ?: if (!tenant.is_active || tenant.access_status == "SUSPENDED") "SUSPENDED" else "ACTIVE",
            billingOwnerId: String? = tenant.billing_owner_id): JSONObject {
            val selectedPlan = UUID.fromString(planId)
            val from = membershipInstant(start)
            val until = membershipInstant(end)
            require(until > from) { "Bitiş tarihi başlangıçtan sonra olmalıdır." }
            require(adminStatus in setOf("ACTIVE", "SUSPENDED")) { "Üyelik durumunu seçin." }
            return JSONObject().put("plan_id", selectedPlan.toString()).put("period_start", from.toString())
                .put("period_end", until.toString()).put("admin_status", adminStatus).apply {
                    if (billingOwnerId != tenant.billing_owner_id) {
                        put("owner_user_id", billingOwnerId?.let { UUID.fromString(it).toString() } ?: JSONObject.NULL)
                    }
                }
        }
        private fun period(row: JSONObject) = if (row.optInt("period_months") == 12) "YEARLY" else "MONTHLY"
        fun plan(row: JSONObject) = SubscriptionPlan(id = row.getString("id"), code = row.getString("code"), name = row.getString("name"),
            price = row.getString("price").toDouble(), currency = row.getString("currency"), billing_period = period(row),
            branch_limit = row.getInt("branch_limit"), student_limit = row.getInt("student_limit"), user_limit = row.getInt("user_limit"), is_active = row.getBoolean("is_active"))
        fun invoice(row: JSONObject) = SubscriptionInvoice(id = row.getString("id"), invoice_no = row.getString("id"), organization_id = row.getString("organization_id"),
            organization_name = row.optString("organization_name", ""), plan_id = row.getString("plan_id"), plan_name = row.optString("package_name", ""),
            amount = row.getString("amount").toDouble(), currency = row.getString("currency"), status = row.getString("status"),
            valid_until = row.optString("valid_until", ""), paid_at = if (row.isNull("paid_at")) null else row.optString("paid_at"),
            provider_payment_id = if (row.isNull("provider_payment_id")) null else row.optString("provider_payment_id"),
            created_at = row.optString("created_at", ""), checkout_status = if (row.isNull("checkout_status")) null else row.optString("checkout_status"),
            can_cancel = row.optBoolean("can_cancel"), checkout_id = if (row.isNull("checkout_id")) null else row.optString("checkout_id"))
        fun access(row: JSONObject): OrganizationAccessStatus {
            val state = row.getJSONObject("access")
            val usage = row.optJSONObject("usage") ?: JSONObject()
            val pack = row.optJSONObject("package") ?: JSONObject()
            val accessState = state.getString("status")
            val status = if (state.optBoolean("restricted") && accessState != "SUSPENDED") "RESTRICTED" else accessState
            return OrganizationAccessStatus(organization_id = if (row.isNull("organization_id")) "" else row.optString("organization_id"), organization_name = "Kurum üyeliği",
                status = status, plan_id = pack.optString("id", ""), plan_name = pack.optString("name", ""), billing_period = period(pack),
                period_end = if (state.isNull("period_end")) "" else state.optString("period_end"), grace_until = if (state.isNull("grace_end")) "" else state.optString("grace_end"),
                remaining_grace_days = if (state.optString("status") == "GRACE") state.optInt("days_remaining") else 0,
                active_branches = usage.optInt("branch_limit"), active_students = usage.optInt("student_limit"), active_users = usage.optInt("user_limit"),
                branch_limit = pack.optInt("branch_limit"), student_limit = pack.optInt("student_limit"), user_limit = pack.optInt("user_limit"),
                saas_iyzico_enabled = false, is_billing_authorized = row.optBoolean("can_manage"),
                accepts_payment_requests = accessState in setOf("ACTIVE", "GRACE", "PAST_DUE", "PENDING_PAYMENT", "LEGACY"),
                warning = if (state.isNull("warning")) null else state.getString("warning"))
        }
        fun tenant(row: JSONObject): PlatformTenantSummary {
            val usage = row.optJSONObject("usage") ?: JSONObject()
            val pack = row.optJSONObject("package") ?: JSONObject()
            val state = row.getJSONObject("access")
            return PlatformTenantSummary(organization_id = row.getString("id"), organization_name = row.getString("name"), is_active = row.getBoolean("is_active"),
                access_status = state.getString("status"), plan_id = pack.optString("id", ""), plan_name = pack.optString("name", ""), billing_period = period(pack),
                period_end = if (!row.isNull("period_end")) row.getString("period_end") else if (state.isNull("period_end")) "" else state.optString("period_end"),
                grace_until = if (state.isNull("grace_end")) "" else state.optString("grace_end"),
                active_branches = usage.optInt("branch_limit"), active_students = usage.optInt("student_limit"), active_users = usage.optInt("user_limit"),
                branch_limit = pack.optInt("branch_limit"), student_limit = pack.optInt("student_limit"), user_limit = pack.optInt("user_limit"),
                period_start = if (row.isNull("period_start")) "" else row.optString("period_start"),
                admin_status = if (row.isNull("admin_status")) null else row.getString("admin_status"),
                billing_owner_id = if (row.isNull("owner_user_id")) null else row.getString("owner_user_id"))
        }
    }
}
