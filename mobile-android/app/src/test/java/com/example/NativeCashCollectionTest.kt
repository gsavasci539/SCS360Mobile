package com.example

import com.example.data.remote.*
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeCashCollectionTest {
    private val actor = UUID(1, 1).toString(); private val debt = UUID(2, 2).toString()
    private fun row(paid: String = "0.00") = JSONObject().put("id", debt).put("amount", "100.00").put("paid_amount", paid).put("status", "UNPAID")
    private fun ack(body: String) = JSONObject().put("payment_id", JSONObject(body).getString("request_id"))
        .put("receipt_id", UUID(3, 3).toString()).put("receipt_number", "RCP-SYNTHETIC").put("remaining_debt", "69.99")
    private fun reply(e: HttpExchange, data: JSONObject, code: Int = 200) {
        val bytes = JSONObject().put("success", code < 400).put("data", data).toString().toByteArray()
        e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(code, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
    }
    private suspend fun server(handler: (HttpExchange) -> Unit, work: suspend () -> Unit) {
        val oldBase = ApiClientProvider.activeBaseUrl; val i = ApiClientProvider.interceptor
        val oldToken = i.authToken; val oldBranch = i.branchId; val oldWorkspace = i.workspaceOrganizationId
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0); s.createContext("/", handler); s.start()
        ApiClientProvider.createService("http://127.0.0.1:${s.address.port}/api/"); i.setSession("synthetic-cash-token")
        try { work() } finally { s.stop(0); ApiClientProvider.createService(oldBase); i.setSession(oldToken, branch = oldBranch, workspace = oldWorkspace) }
    }
    private fun session() = NativeCashCollection(RuntimeEnvironment.getApplication(), actor, debt)
    @Test fun lostAckSurvivesRecreationAndReusesExactCanonicalPayload() = runBlocking {
        val posts = mutableListOf<String>()
        server({ e -> if (e.requestMethod == "GET") reply(e, row(if (posts.isEmpty()) "0.00" else "30.01")) else {
            posts.add(e.requestBody.readBytes().toString(Charsets.UTF_8))
            if (posts.size == 1) e.close() else reply(e, ack(posts.last()))
        } }) {
            val first = session(); first.load()
            try { first.collect("30.01", "CASH"); fail("Dropped ACK must be uncertain") } catch (_: java.io.IOException) { }
            assertTrue(first.hasPending); assertEquals(1, posts.size)
            val restored = session(); restored.load(); assertTrue(restored.hasPending)
            assertEquals("30.01", restored.pendingAmount()); assertEquals("CASH", restored.pendingMethod())
            try { restored.collect("31.00", "CASH"); fail("Pending amount cannot change") } catch (_: IllegalStateException) { }
            val result = restored.collect("30.01", "CASH")
            assertEquals(posts[0], posts[1]); assertEquals("RCP-SYNTHETIC", result.receiptNumber)
            assertEquals(setOf("debt_id", "amount", "payment_method", "request_id"), JSONObject(posts[0]).keys().asSequence().toSet())
            assertFalse(restored.hasPending); assertEquals(result, restored.collect("30.01", "CASH")); assertEquals(2, posts.size)
            restored.finish(); val done = session(); done.load(); assertFalse(done.hasPending)
        }
    }
    @Test fun twoEditorsCannotCreateDifferentRequestIdsForTheSamePendingCollection() = runBlocking {
        val posts = mutableListOf<String>()
        server({ e -> if (e.requestMethod == "GET") reply(e, row()) else {
            val body = e.requestBody.readBytes().toString(Charsets.UTF_8); posts.add(body); reply(e, ack(body))
        } }) {
            val a = session(); val b = session(); a.load(); b.load()
            val first = a.collect("30.01", "CASH"); val second = b.collect("30.01", "CASH")
            assertEquals(first.paymentId, second.paymentId); assertEquals(posts[0], posts[1])
        }
    }
    @Test fun changedDebtInvalidAmountAndUnsupportedMethodsNeverPost() = runBlocking {
        var gets = 0; var posts = 0
        server({ e -> if (e.requestMethod == "GET") { gets++; reply(e, row(if (gets == 1) "0.00" else "1.00")) }
            else { posts++; reply(e, JSONObject(), 422) } }) {
            val s = session(); s.load()
            for (amount in listOf("NaN", "-1", "1.001", "0")) {
                try { s.collect(amount, "CASH"); fail("Invalid amount must fail") } catch (_: IllegalArgumentException) { }
            }
            try { s.collect("1", "İyzico Sandbox (Ref: fake)"); fail("Display text cannot be a ledger method") } catch (_: IllegalArgumentException) { }
            try { s.collect("1", "CASH"); fail("Changed debt must be reloaded") } catch (_: IllegalStateException) { }
            assertEquals(0, posts); assertFalse(s.hasPending)
        }
    }
    @Test fun rejectionAndStaleIdentityDoNotClaimSuccessOrDiscardPendingIdentity() = runBlocking {
        var posts = 0
        server({ e -> if (e.requestMethod == "GET") reply(e, row()) else { posts++; reply(e, JSONObject(), 403) } }) {
            val s = session(); s.load()
            try { s.collect("30.01", "BANK_TRANSFER"); fail("403 must propagate") }
            catch (failure: CashCollectionHttpException) { assertEquals(403, failure.code) }
            assertTrue(s.hasPending)
            try { s.finish(); fail("Unacknowledged collection cannot finish") } catch (_: IllegalStateException) { }
            ApiClientProvider.interceptor.setSession("new-actor")
            try { s.collect("30.01", "BANK_TRANSFER"); fail("Stale actor must not post") } catch (_: kotlinx.coroutines.CancellationException) { }
            assertEquals(1, posts)
        }
    }
}
