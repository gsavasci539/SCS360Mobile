package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.InstitutionRole
import com.example.data.remote.NativeInstitutionRoleClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeInstitutionRoleClientTest {
    @Test fun permissionWriteNeedsExactReadbackAndIncludesTheOriginalSet() {
        val org = UUID(84, 1).toString()
        val roleId = UUID(84, 2).toString()
        val previous = UUID(84, 3).toString()
        val added = UUID(84, 4).toString()
        val frames = CopyOnWriteArrayList<JSONObject>()
        val current = AtomicBoolean(false)
        val auth = ApiClientProvider.interceptor
        val originalUrl = ApiClientProvider.activeBaseUrl
        val originalToken = auth.authToken
        val originalTenant = auth.tenantId
        val originalBranch = auth.branchId
        val originalWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/roles/$roleId/permissions") { request ->
            val reply = if (request.requestMethod == "PUT") {
                frames += JSONObject(request.requestBody.bufferedReader().use { it.readText() })
                JSONObject().put("role_id", roleId).put("permission_ids", JSONArray(listOf(previous, added)))
            } else {
                val ids = if (current.get()) listOf(previous, added) else listOf(previous)
                JSONArray(ids.map { JSONObject().put("id", it) })
            }
            val bytes = JSONObject().put("success", true).put("data", reply).toString().toByteArray()
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-role", org)
            runBlocking {
                val client = NativeInstitutionRoleClient(org)
                val role = InstitutionRole(roleId, "Test role", "test_role", null, org, false)
                val desired = setOf(previous, added)
                assertTrue(runCatching { client.replace(role, setOf(previous), desired) }.isFailure)
                current.set(true)
                assertEquals(desired, client.replace(role, setOf(previous), desired))
            }
            assertEquals(2, frames.size)
            assertTrue(frames.all { it.getJSONArray("expected_permission_ids").getString(0) == previous })
        } finally {
            ApiClientProvider.createService(originalUrl)
            auth.setSession(originalToken, originalTenant, originalBranch, originalWorkspace)
            server.stop(0)
        }
    }
}
