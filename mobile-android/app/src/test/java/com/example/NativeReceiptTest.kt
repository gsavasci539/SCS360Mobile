package com.example

import com.example.data.remote.*
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeReceiptTest {
    private val debt = UUID(1, 1).toString()
    private fun id(i: Int) = UUID(2, i.toLong()).toString()
    private suspend fun serve(mode: String, work: suspend (NativeReceiptClient, MutableList<String>) -> Unit) {
        val old = ApiClientProvider.activeBaseUrl; val token = ApiClientProvider.interceptor.authToken
        val hits = mutableListOf<String>(); val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e ->
            hits.add(e.requestURI.toString())
            var code = if (mode == "403") 403 else 200
            val pdf = e.requestURI.path.endsWith("/pdf")
            val data = if (e.requestURI.path.endsWith("/payments")) {
                val second = "page=2&" in e.requestURI.rawQuery
                val rows = JSONArray()
                if (mode != "empty") for (i in if (second) 100..100 else 0..99) rows.put(JSONObject().put("id", id(i)).put("debt_id", debt)
                    .put("receipt_id", id(1000 + i)).put("amount", "10.01"))
                JSONObject().put("total", if (mode == "empty") 0 else 101).put("items", rows)
            } else {
                val n = e.requestURI.path.substringAfterLast('/').toString()
                val receiptId = if (pdf) e.requestURI.path.split('/').let { it[it.size - 2] } else n
                val i = UUID.fromString(receiptId).leastSignificantBits.toInt() - 1000
                JSONObject().put("id", receiptId).put("payment_id", if (mode == "wrong") id(999) else id(i))
                    .put("receipt_number", "RCP-$i").put("issued_at", "2026-11-12T12:00:00").put("status", "ACTIVE").put("remaining_debt", "20.02")
            }
            val bytes = if (pdf) (if (mode == "badpdf") "<html>" else "%PDF-1.7\nsynthetic bytes").toByteArray()
                else JSONObject().put("success", true).put("data", data).toString().toByteArray()
            e.responseHeaders.add("Content-Type", if (pdf) "application/pdf" else "application/json")
            e.sendResponseHeaders(code, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
        }; server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/"); ApiClientProvider.interceptor.setSession("synthetic-receipt-token")
        try { work(NativeReceiptClient(debt), hits) } finally { server.stop(0); ApiClientProvider.createService(old); ApiClientProvider.interceptor.setSession(token) }
    }
    @Test fun fullPaginationAndPaymentReceiptLinkBeforeExactPdf() = runBlocking {
        serve("valid") { c, hits ->
            val rows = c.load(); assertEquals(101, rows.size); assertEquals("10.01", rows.first().paymentAmount.toPlainString())
            assertTrue(hits.any { "page=2&" in it }); assertTrue(c.pdf(rows.first()).toString(Charsets.UTF_8).startsWith("%PDF-"))
            val before = hits.size; ApiClientProvider.interceptor.setSession("new-actor")
            try { c.pdf(rows.first()); fail("Old actor cannot fetch PDF") } catch (_: kotlinx.coroutines.CancellationException) { }
            assertEquals(before, hits.size)
        }
    }
    @Test fun emptyAndUnauthorizedNeverGenerateReceipt() = runBlocking {
        serve("empty") { c, _ -> assertTrue(c.load().isEmpty()) }
        serve("403") { c, _ -> try { c.load(); fail("403 must propagate") } catch (e: ReceiptHttpException) { assertEquals(403, e.code) } }
    }
    @Test fun mismatchedPaymentAndNonPdfBytesAreRejected() = runBlocking {
        serve("wrong") { c, _ -> try { c.load(); fail("Wrong payment link") } catch (_: IllegalStateException) { } }
        serve("badpdf") { c, _ -> val receipt = c.load().first()
            try { c.pdf(receipt); fail("HTML cannot be a PDF") } catch (_: IllegalStateException) { } }
    }
}
