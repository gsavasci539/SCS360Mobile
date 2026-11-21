package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeInstitutionUsersClient
import com.example.data.remote.UserAccess
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
class NativeInstitutionUsersClientTest {
    @Test fun accessSuccessRequiresBothVerifiedListsAndSendsTheLoadedSnapshot() {
        val org = UUID(85, 1).toString()
        val account = UUID(85, 2).toString()
        val role = UUID(85, 3).toString()
        val previousBranch = UUID(85, 4).toString()
        val branch = UUID(85, 5).toString()
        val writes = CopyOnWriteArrayList<JSONObject>()
        val current = AtomicBoolean(false)
        val auth = ApiClientProvider.interceptor
        val originalUrl = ApiClientProvider.activeBaseUrl
        val originalToken = auth.authToken
        val originalTenant = auth.tenantId
        val originalBranch = auth.branchId
        val originalWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/users/$account") { request ->
            val answer = when {
                request.requestMethod == "PUT" -> {
                    writes += JSONObject(request.requestBody.bufferedReader().use { it.readText() })
                    JSONObject().put("user_id", account).put("role_ids", JSONArray(listOf(role)))
                        .put("branch_ids", JSONArray(listOf(branch)))
                }
                request.requestURI.path.endsWith("/branches") ->
                    JSONArray().put(JSONObject().put("id", if (current.get()) branch else previousBranch))
                else -> JSONArray().put(JSONObject().put("id", role))
            }
            val bytes = JSONObject().put("success", true).put("data", answer).toString().toByteArray()
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-access", org, branch)
            runBlocking {
                val client = NativeInstitutionUsersClient(org, branch)
                val baseline = UserAccess(emptySet(), setOf(previousBranch))
                val desired = UserAccess(setOf(role), setOf(branch))
                assertTrue(runCatching { client.replaceAccess(account, baseline, desired) }.isFailure)
                current.set(true)
                client.replaceAccess(account, baseline, desired)
            }
            assertEquals(2, writes.size)
            assertTrue(writes.all { it.getJSONArray("expected_role_ids").length() == 0 &&
                it.getJSONArray("expected_branch_ids").getString(0) == previousBranch })
        } finally {
            ApiClientProvider.createService(originalUrl)
            auth.setSession(originalToken, originalTenant, originalBranch, originalWorkspace)
            server.stop(0)
        }
    }
}
