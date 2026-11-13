package com.example

import com.example.data.model.*
import com.example.data.remote.*
import com.example.ui.navigation.*
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeMobileParityContractTest {
    private fun id(n: Int) = UUID(26, n.toLong()).toString()
    private fun row(n: Int) = JSONObject().put("id", id(n)).put("organization_id", id(500)).put("first_name", "Contact$n").put("last_name", "Synthetic")
        .put("relation_type", "GUARDIAN").put("phone", JSONObject.NULL).put("sms_phone", JSONObject.NULL).put("whatsapp_phone", JSONObject.NULL)
        .put("email", JSONObject.NULL).put("address", JSONObject.NULL).put("receive_notifications", true).put("user_id", JSONObject.NULL)
    private suspend fun server(handler: (com.sun.net.httpserver.HttpExchange) -> Any, work: suspend () -> Unit) {
        val base = ApiClientProvider.activeBaseUrl; val auth = ApiClientProvider.interceptor; val token = auth.authToken
        val tenant = auth.tenantId; val branch = auth.branchId; val workspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e ->
            try {
                val result = handler(e)
                val bytes = JSONObject().put("success", true).put("data", result).toString().toByteArray()
                e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(200, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
            } finally { e.close() }
        }; server.start(); ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/"); auth.setSession("synthetic-parity", workspace = id(500))
        try { work() } finally { server.stop(0); ApiClientProvider.createService(base); auth.setSession(token, tenant, branch, workspace) }
    }
    @Test fun parentPaginationCanonicalDeltasAndAccountStudentLinksUseActualIds() = runBlocking {
        val queries = mutableListOf<String>(); val writes = mutableListOf<Pair<String, JSONObject>>()
        var contact = row(1); var linked = false
        server({ e ->
            val path = e.requestURI.path.removePrefix("/api/"); val body = if (e.requestMethod in listOf("POST", "PUT")) JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8)) else JSONObject()
            when {
                path == "parents" && e.requestMethod == "GET" -> { queries.add(e.requestURI.query); val offset = if (e.requestURI.query.contains("skip=100")) 100 else 0; JSONArray().apply { for (n in offset until minOf(offset + 100, 101)) put(if (n == 0) contact else row(n + 1)) } }
                path.endsWith("account-options") -> { val offset = if (e.requestURI.query.contains("offset=100")) 100 else 0; JSONObject().put("users", JSONArray().apply { for (n in offset until minOf(offset + 100, 101)) put(JSONObject().put("id", id(1000 + n)).put("name", "Synthetic$n").put("email", "u$n@example.test")) }) }
                path.endsWith("/account") -> { writes.add(path to body); JSONObject().put("parent_id", id(1)).put("user_id", body.get("user_id")) }
                path == "students/${id(2000)}/parents" && e.requestMethod == "GET" -> JSONArray().apply { if (linked) put(JSONObject().put("id", id(1))) }
                path == "students/${id(2000)}/parents" -> { writes.add(path to body); linked = true; JSONObject().put("id", id(1)) }
                path.startsWith("students/") && e.requestMethod == "DELETE" -> { linked = false; JSONObject.NULL }
                e.requestMethod == "PUT" -> { writes.add(path to body); body.keys().forEach { contact.put(it, body.get(it)) }; contact }
                e.requestMethod == "POST" -> { writes.add(path to body); contact = row(1).apply { body.keys().forEach { put(it, body.get(it)) } }; contact }
                else -> contact
            }
        }) {
            val api = NativeParentClient(id(500)); assertEquals(101, api.list("Synthetic").size); assertTrue(queries[1].contains("skip=100"))
            val original = api.get(id(1)); val updated = api.save(JSONObject().put("first_name", "Updated"), original)
            assertEquals("Updated", updated.value("first_name")); assertEquals(setOf("first_name"), writes.last().second.keys().asSequence().toSet())
            assertEquals(101, api.accounts(id(1)).size); api.linkAccount(id(1), id(1000)); api.linkAccount(id(1), null)
            api.linkStudent(id(2000), id(1), true, false); assertEquals(listOf(id(1)), api.studentParents(id(2000)))
            assertEquals(setOf("parent_id", "is_primary", "is_emergency"), writes.last().second.keys().asSequence().toSet())
            api.unlinkStudent(id(2000), id(1)); assertTrue(api.studentParents(id(2000)).isEmpty())
            try { api.save(JSONObject().put("user_id", id(1)), updated); fail("Account bypass") } catch (_: IllegalArgumentException) { }
            contact.put("first_name", "Concurrent")
            try { api.save(JSONObject().put("first_name", "Stale"), updated); fail("Stale overwrite") } catch (_: IllegalStateException) { }
        }
    }
    @Test fun foreignContactOrStaleSessionCannotSupplyAnotherWorkspacesRows() = runBlocking {
        var calls = 0
        server({ _ -> calls++; JSONArray().put(row(1).put("organization_id", id(501))) }) {
            val api = NativeParentClient(id(500))
            try { api.list(); fail("Foreign organization") } catch (_: IllegalStateException) { }
            val before = calls; ApiClientProvider.interceptor.invalidateContext()
            try { api.list(); fail("Stale scope") } catch (_: kotlinx.coroutines.CancellationException) { }
            assertEquals(before, calls)
        }
    }
    @Test fun parentPortalUsesCanonicalChildrenAndDebtIdsAndRejectsMismatchedChild() = runBlocking {
        var wrong = false
        server({ e ->
            if (e.requestURI.path.endsWith("my-children")) JSONArray().put(JSONObject().put("student_id", id(7)).put("first_name", "Synthetic").put("last_name", "Child").put("class_id", JSONObject.NULL).put("class_name", JSONObject.NULL).put("student_number", JSONObject.NULL).put("status", "ACTIVE"))
            else JSONObject().put("student_id", id(if (wrong) 8 else 7)).put("invoices", JSONArray().put(JSONObject().put("debt_id", id(10)).put("amount", "100.10").put("paid_amount", "30.01").put("remaining_amount", "70.09").put("status", "PARTIAL").put("description", "Synthetic debt").put("due_date", JSONObject.NULL)))
        }) {
            val api = NativeParentPortalClient(); val children = api.children(); assertEquals(id(7), children.single().id)
            val invoice = api.invoices(children.single()).single(); assertEquals(id(10), invoice.id); assertEquals(30.01, invoice.paid_amount, .0001); assertEquals("", invoice.invoice_number)
            wrong = true
            try { api.invoices(children.single()); fail("Wrong child ACK") } catch (_: IllegalStateException) { }
        }
    }
    @Test fun customRoleLoginKeepsServerSlugsAndOnlyGrantedNavigation() = runBlocking {
        server({ e ->
            if (e.requestURI.path.endsWith("login")) JSONObject().put("access_token", "synthetic-custom-token")
            else JSONObject().put("id", id(20)).put("roles", JSONArray().put(JSONObject().put("slug", "library_assistant").put("is_active", true)))
                .put("permissions", JSONArray(listOf("library.read", "parents.read")))
        }) {
            val user = NativeAuthentication(ApiClientProvider.createService(ApiClientProvider.activeBaseUrl)).login("user@example.test", "synthetic-password")
            assertEquals(UserRole.CUSTOM, SessionRolePolicy.primary(user)); assertEquals("library_assistant", user.roles.single().slug)
            assertTrue(NativeNavigationPolicy.screen(user, SubScreen.LIBRARY)); assertTrue(NativeNavigationPolicy.screen(user, SubScreen.PARENTS))
            assertFalse(NativeNavigationPolicy.tab(user, MainTab.FINANCE)); assertFalse(NativeNavigationPolicy.screen(user, SubScreen.USERS)); assertFalse(NativeNavigationPolicy.screen(user, SubScreen.PLATFORM_SAAS))
            assertFalse(SessionRolePolicy.canSelect(user, UserRole.ADMIN)); assertFalse(SessionRolePolicy.canSelect(user, UserRole.SUPERADMIN))
        }
    }
}
