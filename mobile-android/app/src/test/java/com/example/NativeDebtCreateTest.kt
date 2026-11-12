package com.example

import com.example.data.remote.*
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeDebtCreateTest {
    @Test fun fullOptionsAndDurableCreateRetryUseOnlyCanonicalFields() = runBlocking {
        val oldBase = ApiClientProvider.activeBaseUrl; val oldToken = ApiClientProvider.interceptor.authToken
        val posts = mutableListOf<String>(); val pages = mutableListOf<String>(); fun id(i: Int) = UUID(1, i.toLong()).toString()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e ->
            val data = if (e.requestMethod == "GET") {
                pages.add(e.requestURI.rawQuery)
                val second = "page=2&" in e.requestURI.rawQuery
                JSONObject().put("total", 101).put("items", JSONArray().apply {
                    for (i in if (second) 100..100 else 0..99) put(JSONObject().put("id", id(i)).put("name", "Synthetic $i")) })
            } else {
                posts.add(e.requestBody.readBytes().toString(Charsets.UTF_8))
                if (posts.size == 1) { e.close(); return@createContext }
                JSONObject().put("id", JSONObject(posts.last()).getString("request_id"))
            }
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(200, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
        }; server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/"); ApiClientProvider.interceptor.setSession("synthetic-debt")
        try {
            val context = RuntimeEnvironment.getApplication()
            val actor = id(200)
            val first = NativeDebtCreateSession(context, actor); assertEquals(101, first.load().size)
            val draft = DebtCreateDraft(id(0), "EDUCATION", "100,01", "2027-01-15", "Synthetic tuition")
            try { first.create(draft); fail("Lost ACK") } catch (_: java.io.IOException) { }
            assertTrue(first.hasPending); assertEquals(1, posts.size)
            val restored = NativeDebtCreateSession(context, actor); restored.load()
            try { restored.create(draft.copy(amount = "101")); fail("Pending body immutable") } catch (_: IllegalStateException) { }
            val result = restored.create(restored.pendingDraft()!!)
            assertEquals(posts[0], posts[1]); assertEquals(JSONObject(posts[0]).getString("request_id"), result)
            assertEquals(setOf("student_id", "debt_type", "amount", "due_date", "description", "request_id"), JSONObject(posts[0]).keys().asSequence().toSet())
            assertEquals("100.01", JSONObject(posts[0]).getString("amount")); restored.finish()
            assertTrue(pages.any { "page=2&" in it }); assertFalse(restored.hasPending)
            val next = NativeDebtCreateSession(context, actor); next.load()
            val nextId = next.create(draft.copy(description = "Next synthetic debt", amount = "20.00"))
            restored.finish() // Closing the old dialog again cannot clear the newer request.
            val recoverNext = NativeDebtCreateSession(context, actor); recoverNext.load(); assertTrue(recoverNext.hasPending)
            assertEquals(nextId, recoverNext.create(recoverNext.pendingDraft()!!)); recoverNext.finish()
            val stale = NativeDebtCreateSession(context, actor); ApiClientProvider.interceptor.setSession("new-actor")
            try { stale.load(); fail("Stale identity") } catch (_: kotlinx.coroutines.CancellationException) { }
        } finally { server.stop(0); ApiClientProvider.createService(oldBase); ApiClientProvider.interceptor.setSession(oldToken) }
    }
}
