package com.example

import com.example.data.model.OrganizationAccessStatus
import com.example.data.model.SubscriptionInvoice
import com.example.data.model.SubscriptionPlan
import com.example.data.model.User
import com.example.data.model.UserRoleInfo
import com.example.data.model.PlatformTenantSummary
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.SaasApi
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class SaasApiTest {
    @Test fun membershipDatesRequireAnExplicitPeriodAndPreserveAdministrativeSuspension() {
        val plan = "00000000-0000-4000-8000-000000000001"
        val active = PlatformTenantSummary(is_active = true, access_status = "PAST_DUE")
        val body = SaasApi.membershipBody(active, plan, "2026-11-01", "2026-12-01T03:00:00+03:00")
        assertEquals("2026-11-01T00:00:00Z", body.getString("period_start"))
        assertEquals("2026-12-01T00:00:00Z", body.getString("period_end"))
        assertEquals("ACTIVE", body.getString("admin_status"))
        for (tenant in listOf(active.copy(is_active = false), active.copy(access_status = "SUSPENDED"))) {
            assertEquals("SUSPENDED", SaasApi.membershipBody(tenant, plan, "2026-11-01", "2026-12-01").getString("admin_status"))
        }
        for ((start, end) in listOf("" to "2026-12-01", "2026-12-01" to "2026-11-01", "2026-11-01T00:00:00" to "2026-12-01")) {
            try { SaasApi.membershipBody(active, plan, start, end); fail("Invalid period must be rejected") }
            catch (_: IllegalArgumentException) { }
            catch (_: java.time.format.DateTimeParseException) { }
        }
    }
    @Test fun membershipUpdateUsesCanonicalPutWithoutChangingTheOwnerOrInventingPayment() = runBlocking {
        val originalBase = ApiClientProvider.activeBaseUrl
        val originalToken = ApiClientProvider.interceptor.authToken
        val originalWorkspace = ApiClientProvider.interceptor.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val organization = "00000000-0000-4000-8000-000000000002"
        val plan = "00000000-0000-4000-8000-000000000001"
        var captured: JSONObject? = null
        var method = ""
        var header = ""
        var reject = false
        server.createContext("/api/platform/tenants/$organization/subscription") { exchange ->
            method = exchange.requestMethod
            header = exchange.requestHeaders.getFirst("Authorization")
            captured = JSONObject(exchange.requestBody.bufferedReader().use { it.readText() })
            val body = """{"success":true,"data":{"status":"SUSPENDED"}}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(if (reject) 403 else 200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            ApiClientProvider.interceptor.authToken = "synthetic-session"
            ApiClientProvider.interceptor.workspaceOrganizationId = null
            val tenant = PlatformTenantSummary(organization_id = organization, is_active = false, access_status = "SUSPENDED", period_end = "old-server-value")
            val api = SaasApi()
            val result = api.updateMembership(tenant, plan, "2026-11-01", "2026-12-01")
            assertEquals("SUSPENDED", result.getString("status"))
            assertEquals("PUT", method)
            assertEquals("Bearer synthetic-session", header)
            assertEquals(setOf("plan_id", "period_start", "period_end", "admin_status"), captured!!.keys().asSequence().toSet())
            assertEquals(plan, captured!!.getString("plan_id"))
            assertEquals("SUSPENDED", captured!!.getString("admin_status"))
            reject = true
            try { api.updateMembership(tenant, plan, "2026-11-01", "2026-12-01"); fail("A rejected update must never become success") }
            catch (_: IllegalStateException) { }
            assertEquals("old-server-value", tenant.period_end)
        } finally {
            server.stop(0)
            ApiClientProvider.createService(originalBase)
            ApiClientProvider.interceptor.authToken = originalToken
            ApiClientProvider.interceptor.workspaceOrganizationId = originalWorkspace
        }
    }
    @Test fun rolesAreTakenFromTheAuthenticatedBackendProfile() {
        assertEquals("UNASSIGNED", User().resolvedRole)
        assertEquals("OWNER", User(roles = listOf(UserRoleInfo("owner"))).resolvedRole)
        assertEquals("TEACHER", User(roles = listOf(UserRoleInfo("teacher"), UserRoleInfo("parent"))).resolvedRole)
        assertEquals("UNASSIGNED", User(roles = listOf(UserRoleInfo("admin", false))).resolvedRole)
        assertEquals("SUPERADMIN", User(is_super_admin = true).resolvedRole)
        val previous = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
            assertEquals("FINANCE", User(role = "finance").resolvedRole)
            assertEquals("ADMIN", User(roles = listOf(UserRoleInfo("ADMIN"))).resolvedRole)
        } finally { java.util.Locale.setDefault(previous) }
    }
    @Test fun catalogUsesActualServerPriceCurrencyAndPeriod() {
        val row = SaasApi.plan(JSONObject("""{"id":"synthetic-plan","code":"SYNTHETIC","name":"Synthetic catalog","price":"91.25",
          "currency":"EUR","period_months":12,"branch_limit":3,"student_limit":20,"user_limit":8,"is_active":true}"""))
        assertEquals(91.25, row.price, 0.001)
        assertEquals("EUR", row.currency)
        assertEquals("YEARLY", row.billing_period)
    }
    @Test fun defaultsAreUnavailableRatherThanPaidSampleAccounts() {
        assertEquals("", SubscriptionPlan().name)
        assertEquals("UNAVAILABLE", OrganizationAccessStatus().status)
        assertFalse(OrganizationAccessStatus().saas_iyzico_enabled)
        assertFalse(OrganizationAccessStatus().is_billing_authorized)
        assertEquals("UNAVAILABLE", SubscriptionInvoice().status)
        assertNull(SubscriptionInvoice().paid_at)
    }
    @Test fun canonicalAccessKeepsServerQuotasAndRestrictions() {
        val value = JSONObject("""{"organization_id":"synthetic-school","can_manage":true,
          "access":{"status":"PAST_DUE","restricted":true,"period_end":"2026-01-01","grace_end":"2026-01-11"},
          "usage":{"branch_limit":2,"student_limit":17,"user_limit":5},
          "package":{"id":"synthetic-plan","name":"Synthetic catalog","period_months":12,"branch_limit":3,"student_limit":20,"user_limit":8}}""")
        val row = SaasApi.access(value)
        assertEquals("RESTRICTED", row.status)
        assertEquals(17, row.active_students)
        assertEquals(20, row.student_limit)
        assertFalse(row.saas_iyzico_enabled)
        assertEquals(0, row.remaining_grace_days)
    }
    @Test fun invoiceReviewAndCurrencyComeOnlyFromTheServer() {
        val row = SaasApi.invoice(JSONObject("""{"id":"synthetic-invoice","organization_id":"synthetic-school","plan_id":"synthetic-plan",
          "package_name":"Synthetic catalog","amount":"91.25","currency":"EUR","status":"PAID_REVIEW","paid_at":null}"""))
        assertEquals("PAID_REVIEW", row.status)
        assertEquals("EUR", row.currency)
        assertEquals(91.25, row.amount, 0.001)
        assertNull(row.paid_at)
        assertNull(row.checkout_url)
    }
    @Test fun authenticatedRequestsUnwrapResponsesAndRejectChangedSessions() = runBlocking {
        val originalBase = ApiClientProvider.activeBaseUrl
        val originalToken = ApiClientProvider.interceptor.authToken
        val originalWorkspace = ApiClientProvider.interceptor.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var requests = 0
        server.createContext("/api/subscription/access") { exchange ->
            requests++
            assertEquals("Bearer synthetic-session", exchange.requestHeaders.getFirst("Authorization"))
            assertEquals("synthetic-workspace", exchange.requestHeaders.getFirst("X-SCS360-Organization-Id"))
            val body = """{"success":true,"data":{"access":{"status":"GRACE"}}}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            ApiClientProvider.interceptor.authToken = "synthetic-session"
            ApiClientProvider.interceptor.workspaceOrganizationId = "synthetic-workspace"
            val api = SaasApi()
            assertEquals("GRACE", (api.request("subscription/access") as JSONObject).getJSONObject("access").getString("status"))
            ApiClientProvider.interceptor.authToken = "new-session"
            try { api.request("subscription/access"); fail("Changed session must be rejected before requesting") }
            catch (_: IllegalStateException) { }
            assertEquals(1, requests)
        } finally {
            server.stop(0)
            ApiClientProvider.createService(originalBase)
            ApiClientProvider.interceptor.authToken = originalToken
            ApiClientProvider.interceptor.workspaceOrganizationId = originalWorkspace
        }
    }
}
