package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.ChatAuditExport
import com.example.data.remote.ChatAuditStatus
import com.example.data.remote.NativeChatAuditClient
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
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeChatAuditClientTest {
    @Test fun scopedLogCorrectionAndExportUseTheServerRecord() {
        val organization = UUID(98, 1).toString()
        val foreign = UUID(98, 2).toString()
        val id = UUID(98, 3).toString()
        val auth = ApiClientProvider.interceptor
        val beforeUrl = ApiClientProvider.activeBaseUrl
        val before = listOf(auth.authToken, auth.tenantId, auth.branchId, auth.workspaceOrganizationId)
        val exact = AtomicBoolean(false)
        val foreignRead = AtomicBoolean(false)
        val writes = AtomicReference<JSONObject>()
        val requests = CopyOnWriteArrayList<String>()
        val scopes = CopyOnWriteArrayList<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/v1/chatbot/ai") { exchange ->
            val uri = exchange.requestURI
            scopes += exchange.requestHeaders.getFirst("X-SCS360-Organization-Id").orEmpty()
            requests += "${exchange.requestMethod} $uri"
            val exporting = uri.path.endsWith("export-dataset")
            val data = if (exchange.requestMethod == "PUT") {
                writes.set(JSONObject(exchange.requestBody.bufferedReader().use { it.readText() }))
                JSONObject().put("log_id", id)
            } else {
                val row = JSONObject().put("id", id).put("organization_id", if (foreignRead.get()) foreign else organization)
                    .put("sender_name", "İzole").put("sender_role", "teacher").put("user_query", "İzole soru")
                    .put("ai_response", "İzole yanıt").put("topic_category", JSONObject.NULL)
                    .put("detected_intent", JSONObject.NULL).put("sentiment", "NEUTRAL").put("created_at", "2026-11-21")
                    .put("quality_rating", if (exact.get()) 4 else 0)
                    .put("feedback_status", if (exact.get()) "CORRECTED" else "NEEDS_REVIEW")
                    .put("corrected_response", if (exact.get()) "İzole düzeltme" else JSONObject.NULL)
                    .put("is_fine_tuning_ready", false)
                JSONObject().put("page", 1).put("page_size", if ("record_id" in uri.query.orEmpty()) 1 else 20)
                    .put("total", 1).put("logs", JSONArray().put(row))
            }
            val bytes = (if (exporting) "isolated,fixture\n" else
                JSONObject().put("success", true).put("data", data).toString()).toByteArray()
            exchange.responseHeaders.set("Content-Type", if (exporting) "text/csv" else "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-chat-audit", null, null, organization)
            val client = NativeChatAuditClient(organization)
            runBlocking {
                val loaded = client.page(1, 20, "", null).entries.single()
                assertTrue(loaded.topic.isEmpty())
                foreignRead.set(true)
                assertTrue(runCatching { client.page(1, 20, "", null) }.isFailure)
                foreignRead.set(false)
                assertTrue(runCatching { client.feedback(id, 4, ChatAuditStatus.CORRECTED, "İzole düzeltme") }.isFailure)
                exact.set(true)
                assertEquals("İzole düzeltme", client.feedback(id, 4, ChatAuditStatus.CORRECTED, "İzole düzeltme").correction)
                val destination = ByteArrayOutputStream()
                client.export(ChatAuditExport.CSV, ChatAuditStatus.CORRECTED) { destination }
                assertEquals("isolated,fixture\n", destination.toString(Charsets.UTF_8.name()))
            }
            assertEquals(4, writes.get().getInt("quality_rating"))
            assertTrue(requests.any { "export-dataset?format=csv&feedback_status=CORRECTED" in it })
            assertTrue(scopes.all { it == organization })
        } finally {
            ApiClientProvider.createService(beforeUrl)
            auth.setSession(before[0], before[1], before[2], before[3])
            server.stop(0)
        }
    }
}
