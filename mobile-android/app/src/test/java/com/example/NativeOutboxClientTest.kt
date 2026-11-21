package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeOutboxClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
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
class NativeOutboxClientTest {
    @Test fun retryConfirmsSameJobAndShowsActualProviderStatus() {
        val branch = UUID(29, 1).toString()
        val id = UUID(29, 2).toString()
        val job = UUID(29, 3).toString()
        val queued = AtomicBoolean(false)
        val requests = CopyOnWriteArrayList<String>()
        val base = ApiClientProvider.activeBaseUrl
        val auth = ApiClientProvider.interceptor
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/outbox/") { request ->
            requests += "${request.requestMethod} ${request.requestURI}"
            val post = request.requestMethod == "POST"
            val status = if (post) "PENDING" else if (queued.get()) "FAILED" else "CANCELLED"
            val record = JSONObject().put("id", id).put("job_id", job).put("branch_id", branch)
                .put("status", status).put("channel", "EMAIL").put("retry_count", 0)
            if (post) queued.set(true)
            val bytes = JSONObject().put("success", true).put("data", record).toString().toByteArray()
            request.responseHeaders.set("Content-Type", "application/json")
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-native-outbox", UUID(29, 4).toString(), branch)
            runBlocking {
                val client = NativeOutboxClient(branch)
                assertEquals("FAILED", client.retry(client.message(id)).status)
            }
            assertTrue(requests.all { "branch_id=$branch" in it })
            assertEquals(1, requests.count { it.startsWith("POST /api/outbox/retry/$id") })
            assertEquals(2, requests.count { it.startsWith("GET /api/outbox/$id") })
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
