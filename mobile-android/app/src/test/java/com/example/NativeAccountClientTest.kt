package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeAccountClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeAccountClientTest {
    @Test fun profileNeedsMatchingReadbackAndPasswordNeedsMatchingAcknowledgement() {
        val auth = ApiClientProvider.interceptor
        val originalUrl = ApiClientProvider.activeBaseUrl
        val originalToken = auth.authToken
        val originalTenant = auth.tenantId
        val originalBranch = auth.branchId
        val originalWorkspace = auth.workspaceOrganizationId
        val recorded = CopyOnWriteArrayList<Pair<String, JSONObject>>()
        val matching = AtomicBoolean(false)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/auth") { request ->
            val body = request.requestBody.bufferedReader().use { it.readText() }
            val path = request.requestURI.path
            recorded += request.requestMethod to JSONObject(body.ifEmpty { "{}" })
            val data = JSONObject().put("id", "actor").put("email", "saved@example.com")
            if (request.requestMethod == "GET") {
                data.put("first_name", if (matching.get()) "Recorded" else "Old").put("last_name", "Account")
            }
            if (path.endsWith("change-password") && !matching.get()) data.put("id", "different")
            val response = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            request.responseHeaders.set("Content-Type", "application/json")
            request.sendResponseHeaders(200, response.size.toLong())
            request.responseBody.use { it.write(response) }
        }
        server.start()
        try {
            val service = ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-account")
            runBlocking {
                val client = NativeAccountClient(service)
                suspend fun save() = client.updateProfile("actor", " Recorded ", "Account", "saved@example.com", "", " old password ")
                assertTrue(runCatching { save() }.isFailure)
                assertTrue(runCatching { client.changePassword("actor", " old password ", "new password") }.isFailure)
                matching.set(true)
                assertEquals("Recorded", save().first_name)
                client.changePassword("actor", " old password ", "new password")
            }
            assertEquals(listOf("PUT", "GET", "POST", "PUT", "GET", "POST"), recorded.map { it.first })
            assertEquals(" old password ", recorded.first().second.getString("current_password"))
            assertFalse(recorded.first().second.has("organization_id"))
            assertFalse(recorded.first().second.has("branch_ids"))
        } finally {
            ApiClientProvider.createService(originalUrl)
            auth.setSession(originalToken, originalTenant, originalBranch, originalWorkspace)
            server.stop(0)
        }
    }
}
