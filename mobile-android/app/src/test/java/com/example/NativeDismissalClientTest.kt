package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeDismissalClient
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeDismissalClientTest {
    @Test fun verifiedDeliveryRequiresMatchingPersistedRecordAndDenialDoesNotFetchOne() {
        val child = UUID(13, 1).toString()
        val branch = UUID(13, 2).toString()
        val requestId = UUID(13, 3)
        val grantId = UUID(13, 4)
        val received = CopyOnWriteArrayList<JSONObject>()
        val historyLookups = CopyOnWriteArrayList<String>()
        val base = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val oldToken = interceptor.authToken
        val oldTenant = interceptor.tenantId
        val oldBranch = interceptor.branchId
        val oldWorkspace = interceptor.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/dismissal/") { exchange ->
            val uri = exchange.requestURI
            check("branch_id=$branch" in uri.query.orEmpty())
            val payload = if (uri.path.endsWith("/verify-pickup")) {
                val wire = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                received += wire
                if (wire.getString("pickup_person_id") == "DENIED-ID") {
                    JSONObject().put("authorized", false).put("student_id", child)
                } else JSONObject().put("authorized", true).put("student_id", child)
                    .put("authorization_id", grantId.toString()).put("pickup_log_id", wire.getString("request_id"))
            } else {
                historyLookups += uri.query.orEmpty()
                JSONArray().put(JSONObject().put("id", requestId.toString()).put("student_id", child)
                    .put("student_name", "İzole öğrenci").put("pickup_person_name", "İzole yetişkin")
                    .put("pickup_time", "2026-11-21T09:40:00Z").put("verification_method", "ID_CARD"))
            }
            val bytes = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            interceptor.setSession("isolated-dismissal", UUID(13, 9).toString(), branch)
            runBlocking {
                val client = NativeDismissalClient(branch)
                assertNull(client.verify(child, "DENIED-ID", UUID(13, 5)))
                assertTrue(historyLookups.isEmpty())
                assertEquals(requestId.toString(), client.verify(child, "MATCHED-ID", requestId)?.id)
            }
            assertEquals(setOf("student_id", "pickup_person_id", "request_id"), received.last().keys().asSequence().toSet())
            assertEquals(child, received.last().getString("student_id"))
            assertEquals(requestId.toString(), received.last().getString("request_id"))
            assertEquals(1, historyLookups.size)
            assertTrue("branch_id=$branch" in historyLookups.single())
            assertTrue("record_id=$requestId" in historyLookups.single())
        } finally {
            ApiClientProvider.createService(base)
            interceptor.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
