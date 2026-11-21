package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeSecurityClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
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
class NativeSecurityClientTest {
    @Test fun gateRequiresMatchingBranchAndPersistentReceipt() {
        val child = UUID(8, 1).toString()
        val branch = UUID(8, 2).toString()
        val request = UUID(8, 3)
        val historyMatches = AtomicBoolean(false)
        val writes = CopyOnWriteArrayList<JSONObject>()
        val requests = CopyOnWriteArrayList<String>()
        val base = ApiClientProvider.activeBaseUrl
        val auth = ApiClientProvider.interceptor
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/security/") { exchange ->
            val uri = exchange.requestURI
            requests += uri.toString()
            val payload = if (uri.path.endsWith("/verify-qr")) {
                val sent = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                writes += sent
                JSONObject().put("authorized", sent.getString("token") == "isolated-input")
                    .put("gate_log_id", request.toString()).put("student_id", child).put("branch_id", branch)
                    .put("verification_scope", "STUDENT_ID_ONLY")
            } else JSONArray().apply {
                put(JSONObject().put("id", request.toString()).put("student_id", child).put("student_name", "İzole öğrenci")
                    .put("branch_id", branch).put("direction", if (historyMatches.get()) "IN" else "OUT")
                    .put("status", "AUTHORIZED").put("verification_method", "BARCODE").put("created_at", "2026-11-21T05:00:00Z"))
            }
            val bytes = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-gate", UUID(8, 7).toString(), branch)
            runBlocking {
                val client = NativeSecurityClient(branch)
                assertNull(client.verify("unmatched", "IN", "BARCODE", "", "", request))
                assertEquals(1, requests.size)
                assertTrue(runCatching { client.verify("isolated-input", "IN", "BARCODE", "Gate", "", request) }.isFailure)
                historyMatches.set(true)
                assertEquals(request.toString(), client.verify("isolated-input", "IN", "BARCODE", "Gate", "", request)?.id)
            }
            assertEquals(setOf("token", "direction", "verification_method", "gate_location", "notes", "request_id"),
                writes.last().keys().asSequence().toSet())
            assertTrue(requests.all { "branch_id=$branch" in it })
            assertTrue(requests.any { "record_id=$request" in it })
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
