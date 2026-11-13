package com.example

import com.example.data.model.User
import com.example.data.model.UserRoleInfo
import com.example.data.remote.*
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
class NativeParentBranchContractTest {
    private fun id(n: Int) = UUID(71, n.toLong()).toString()
    private suspend fun server(handler: (com.sun.net.httpserver.HttpExchange) -> Any, work: suspend () -> Unit) {
        val base = ApiClientProvider.activeBaseUrl; val auth = ApiClientProvider.interceptor
        val token = auth.authToken; val tenant = auth.tenantId; val branch = auth.branchId; val workspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e ->
            try {
                val bytes = JSONObject().put("success", true).put("data", handler(e)).toString().toByteArray(Charsets.UTF_8)
                e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(200, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
            } finally { e.close() }
        }
        server.start(); ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/"); auth.setSession("synthetic-branch", workspace = id(1))
        try { work() } finally { server.stop(0); ApiClientProvider.createService(base); auth.setSession(token, tenant, branch, workspace) }
    }
    @Test fun branchAssignmentsAndExplicitBranchRequireAStudentWithoutElevatingCustomRoles() {
        assertTrue(NativeParentCreationPolicy.requiresStudent(User(branch_ids = listOf(id(2))), "all"))
        assertTrue(NativeParentCreationPolicy.requiresStudent(User(branch_id = id(2)), "all"))
        assertTrue(NativeParentCreationPolicy.requiresStudent(User(), id(2)))
        assertFalse(NativeParentCreationPolicy.requiresStudent(User(), "all"))
        assertFalse(NativeParentCreationPolicy.requiresStudent(User(is_super_admin = true, branch_ids = listOf(id(2))), "all"))
        assertTrue(NativeParentCreationPolicy.requiresStudent(User(roles = listOf(UserRoleInfo("teacher"))), "all"))
        assertFalse(NativeParentCreationPolicy.requiresStudent(User(roles = listOf(UserRoleInfo("teacher", false))), "all"))
        assertFalse(NativeParentCreationPolicy.requiresStudent(User(is_super_admin = true, roles = listOf(UserRoleInfo("teacher"))), "all"))
        assertTrue(NativeParentCreationPolicy.requiresStudent(User(roles = listOf(UserRoleInfo("parent"))), "all"))
        assertFalse(NativeParentCreationPolicy.requiresStudent(User(roles = listOf(UserRoleInfo("teacher"), UserRoleInfo("owner"))), "all"))
        assertTrue(NativeParentCreationPolicy.requiresStudent(User(branch_ids = listOf(id(2)), roles = listOf(UserRoleInfo("teacher"), UserRoleInfo("owner"))), "all"))
    }
    @Test fun branchCreationUsesOneAtomicStudentEndpointAndReadsBackContactAndLink() = runBlocking {
        val calls = mutableListOf<String>(); var submitted = JSONObject()
        server({ e ->
            calls.add("${e.requestMethod} ${e.requestURI.path}")
            when {
                e.requestMethod == "POST" -> {
                    submitted = JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8)); JSONObject().put("id", id(4))
                }
                e.requestURI.path == "/api/parents/${id(4)}" -> JSONObject(submitted.toString()).put("id", id(4)).put("organization_id", id(1))
                else -> JSONArray().put(JSONObject().put("id", id(4)))
            }
        }) {
            val result = NativeParentClient(id(1), true).save(JSONObject().put("first_name", "Branch").put("last_name", "Contact"), createStudentId = id(3))
            assertEquals(id(4), result.id)
            assertEquals(listOf("POST /api/students/${id(3)}/parents", "GET /api/parents/${id(4)}", "GET /api/students/${id(3)}/parents"), calls)
            assertEquals(setOf("first_name", "last_name"), submitted.keys().asSequence().toSet())
            assertFalse(submitted.has("organization_id"))
        }
    }
    @Test fun missingOrMalformedStudentCannotLeaveAnUnlinkedContact() = runBlocking {
        var calls = 0
        server({ _ -> calls++; JSONObject() }) {
            val api = NativeParentClient(id(1), true); val body = JSONObject().put("first_name", "Branch").put("last_name", "Contact")
            try { api.save(body); fail("Student required") } catch (_: IllegalArgumentException) { }
            try { api.save(body, createStudentId = "std-1"); fail("Real student UUID required") } catch (_: IllegalArgumentException) { }
            assertEquals(0, calls)
            ApiClientProvider.interceptor.invalidateContext()
            try { api.save(body, createStudentId = id(3)); fail("Stale scope") } catch (_: kotlinx.coroutines.CancellationException) { }
            assertEquals(0, calls)
        }
    }
    @Test fun aCreateAcknowledgementWithoutTheStudentLinkIsNotSuccess() = runBlocking {
        server({ e -> when {
            e.requestMethod == "POST" -> JSONObject().put("id", id(4))
            e.requestURI.path.startsWith("/api/parents/") -> JSONObject().put("id", id(4)).put("organization_id", id(1)).put("first_name", "Branch").put("last_name", "Contact")
            else -> JSONArray()
        } }) {
            try { NativeParentClient(id(1), true).save(JSONObject().put("first_name", "Branch").put("last_name", "Contact"), createStudentId = id(3)); fail("Missing student link") }
            catch (_: IllegalStateException) { }
        }
    }
}
