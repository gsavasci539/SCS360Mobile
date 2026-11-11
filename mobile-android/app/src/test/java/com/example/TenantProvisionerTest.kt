package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.model.User
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.TenantProvisionDraft
import com.example.data.remote.TenantProvisioner
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class TenantProvisionerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val plan = "00000000-0000-4000-8000-000000000001"
    private val actor = "00000000-0000-4000-8000-000000000002"
    private val draft = TenantProvisionDraft("Synthetic institution", "Synthetic", "Owner", "owner@example.test", "Synthetic password", plan)
    private fun login(id: String) { ApiClientProvider.interceptor.authToken = "synthetic-session"; SessionManager(context).saveSession(User(id = id, role = "SUPERADMIN", is_super_admin = true)) }

    private fun withServer(test: suspend (HttpServer, MutableList<JSONObject>) -> Unit) = runBlocking {
        val oldBase = ApiClientProvider.activeBaseUrl
        val oldToken = ApiClientProvider.interceptor.authToken
        val oldWorkspace = ApiClientProvider.interceptor.workspaceOrganizationId
        val rows = mutableListOf<JSONObject>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/platform/tenants") { exchange ->
            assertEquals("POST", exchange.requestMethod)
            assertEquals("Bearer synthetic-session", exchange.requestHeaders.getFirst("Authorization"))
            assertNull(exchange.requestHeaders.getFirst("X-SCS360-Organization-Id"))
            val body = JSONObject(exchange.requestBody.bufferedReader().use { it.readText() })
            rows.add(body)
            // The first reply is deliberately ambiguous; the next attempt must
            // reuse the durable id even with a completely new client object.
            val status = if (rows.size == 1) 500 else if (body.getString("owner_email") != draft.email) 409 else 200
            val result = JSONObject().put("success", true).put("data", JSONObject()
                .put("organization_id", "00000000-0000-4000-8000-000000000003")
                .put("owner_user_id", "00000000-0000-4000-8000-000000000004")
                .put("owner_email", draft.email).put("period_end", "2026-12-11T00:00:00+00:00"))
                .toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, result.size.toLong())
            exchange.responseBody.use { it.write(result) }
        }
        context.getSharedPreferences("tenant_provision_v1", Context.MODE_PRIVATE).edit().clear().commit()
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            ApiClientProvider.interceptor.authToken = "synthetic-session"
            ApiClientProvider.interceptor.workspaceOrganizationId = null
            login(actor)
            test(server, rows)
        } finally {
            server.stop(0)
            context.getSharedPreferences("tenant_provision_v1", Context.MODE_PRIVATE).edit().clear().commit()
            SessionManager(context).clearSession()
            ApiClientProvider.createService(oldBase)
            ApiClientProvider.interceptor.authToken = oldToken
            ApiClientProvider.interceptor.workspaceOrganizationId = oldWorkspace
        }
    }

    @Test fun ambiguousRetryAndReopenedFormUseOneRequestIdAndCanonicalBody() = withServer { _, rows ->
        try { TenantProvisioner(context).create(draft); fail("Ambiguous reply must not close the form") }
        catch (_: IllegalStateException) { }
        TenantProvisioner(context).create(draft)
        TenantProvisioner(context).create(draft)
        assertEquals(3, rows.size)
        assertEquals(1, rows.map { it.getString("request_id") }.toSet().size)
        assertEquals(setOf("name", "owner_first_name", "owner_last_name", "owner_email", "owner_password", "plan_id", "request_id"), rows[0].keys().asSequence().toSet())
        assertEquals(draft.password, rows[0].getString("owner_password"))
        assertEquals(plan, rows[0].getString("plan_id"))
        try { TenantProvisioner(context).create(draft.copy(email = "changed@example.test")); fail("Changed retry must not invent success") }
        catch (_: IllegalStateException) { }
        assertEquals(rows[0].getString("request_id"), rows.last().getString("request_id"))
        val stored = context.getSharedPreferences("tenant_provision_v1", Context.MODE_PRIVATE).all
        assertEquals(1, stored.size)
        assertTrue(stored.keys.single().matches(Regex("[a-f0-9]{64}")))
        UUID.fromString(stored.values.single() as String)
        assertFalse(stored.toString().contains(draft.password))
        assertFalse(stored.toString().contains(draft.email))
        assertFalse(stored.toString().contains(draft.name))
        assertFalse(draft.toString().contains(draft.password))
    }

    @Test fun actorAndServerScopesAreIsolatedAndChangedSessionsCannotSend() = withServer { server, rows ->
        try { TenantProvisioner(context).create(draft) } catch (_: IllegalStateException) { }
        val initial = rows.last().getString("request_id")
        login("00000000-0000-4000-8000-000000000005")
        TenantProvisioner(context).create(draft)
        assertNotEquals(initial, rows.last().getString("request_id"))
        login(actor)
        ApiClientProvider.createService("http://localhost:${server.address.port}/api/")
        login(actor)
        TenantProvisioner(context).create(draft)
        assertNotEquals(initial, rows.last().getString("request_id"))
        val stale = TenantProvisioner(context)
        ApiClientProvider.interceptor.authToken = "new-session"
        try { stale.create(draft); fail("Changed actor token must be rejected") } catch (_: IllegalStateException) { }
        assertEquals(3, rows.size)
        ApiClientProvider.interceptor.workspaceOrganizationId = "synthetic-workspace"
        try { TenantProvisioner(context); fail("Provisioning requires the platform workspace") } catch (_: IllegalStateException) { }
    }

    @Test fun invalidOwnerDataCannotPersistANonceOrSend() = withServer { _, rows ->
        for (invalid in listOf(draft.copy(password = "short"), draft.copy(email = "invalid"), draft.copy(lastName = ""), draft.copy(planId = "plan-1"))) {
            try { TenantProvisioner(context).create(invalid); fail("Invalid data must be rejected") } catch (_: IllegalArgumentException) { }
        }
        assertEquals(0, rows.size)
        assertTrue(context.getSharedPreferences("tenant_provision_v1", Context.MODE_PRIVATE).all.isEmpty())
    }
}
