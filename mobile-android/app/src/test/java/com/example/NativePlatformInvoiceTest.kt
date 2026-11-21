package com.example

import com.example.data.model.SubscriptionInvoice
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.PlatformInvoiceDraft
import com.example.data.remote.PlatformInvoiceOperation
import com.example.data.remote.SaasApi
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
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativePlatformInvoiceTest {
    @Test fun receiptAndReviewRequireThePersistedScopedInvoice() {
        val id = UUID(96, 1).toString()
        val org = UUID(96, 2).toString()
        val requestId = UUID(96, 3).toString()
        val invoice = SubscriptionInvoice(id = id, organization_id = org, amount = 17.54,
            currency = "TRY", status = "PENDING", can_cancel = true)
        val receipt = PlatformInvoiceDraft(PlatformInvoiceOperation.SETTLE, requestId, "receipt-1", "17,54",
            "2026-11-17T10:30:00+03:00", "", "", "")
        val reviewed = invoice.copy(status = "PAID_REVIEW", checkout_status = "PAID_REVIEW", can_cancel = false)
        val refund = receipt.copy(operation = PlatformInvoiceOperation.REFUND_RECORDED, reference = "refund-1",
            reason = "Synthetic external refund confirmed")
        val apply = refund.copy(operation = PlatformInvoiceOperation.APPLY, periodStart = "2026-11-20", periodEnd = "2027-12-20")
        val api = ApiClientProvider.interceptor
        val previousUrl = ApiClientProvider.activeBaseUrl
        val previous = listOf(api.authToken, api.tenantId, api.branchId, api.workspaceOrganizationId)
        val requests = CopyOnWriteArrayList<Pair<String, JSONObject>>()
        val paths = CopyOnWriteArrayList<String>()
        val written = AtomicReference(JSONObject())
        val stale = AtomicBoolean(false)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/platform/invoices") { exchange ->
            val uri = exchange.requestURI
            val data = if (exchange.requestMethod == "GET") {
                paths += uri.rawQuery.orEmpty()
                val row = JSONObject(written.get().toString())
                if (stale.get()) row.put("checkout_status", "PAID_REVIEW")
                JSONArray().put(row)
            } else {
                val body = JSONObject(exchange.requestBody.bufferedReader().use { it.readText() })
                requests += uri.path to body
                val action = body.optString("action")
                JSONObject().put("id", id).put("organization_id", org).put("plan_id", UUID(96, 4))
                    .put("amount", "17.54").put("currency", "TRY")
                    .put("status", "PAID")
                    .apply {
                        if (action == "REFUND_RECORDED") put("checkout_status", "REFUNDED")
                        if (action == "APPLY") put("checkout_status", "PAID")
                    }
                    .also(written::set)
            }
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            api.setSession("isolated-invoice")
            runBlocking {
                val client = SaasApi()
                assertTrue(runCatching { client.saveInvoiceOperation(invoice, receipt.copy(reference = "")) }.isFailure)
                assertTrue(runCatching { client.saveInvoiceOperation(invoice, receipt.copy(amount = "17.55")) }.isFailure)
                assertTrue(requests.isEmpty())
                assertEquals("PAID", client.saveInvoiceOperation(invoice, receipt).status)
                stale.set(true)
                assertTrue(runCatching { client.saveInvoiceOperation(reviewed, refund) }.isFailure)
                stale.set(false)
                assertEquals("REFUNDED", client.saveInvoiceOperation(reviewed, refund).checkout_status)
                assertEquals("PAID", client.saveInvoiceOperation(reviewed, apply.copy(requestId = UUID(96, 5).toString())).checkout_status)
            }
            assertEquals("/api/platform/invoices/$id/settle", requests[0].first)
            assertEquals("17.54", requests[0].second.getString("amount"))
            assertEquals("2026-11-17T07:30:00Z", requests[0].second.getString("paid_at"))
            assertEquals("/api/platform/invoices/$id/review", requests[1].first)
            assertEquals(requestId, requests[1].second.getString("request_id"))
            assertEquals(requests[1].second.toString(), requests[2].second.toString())
            assertEquals("2027-12-20T00:00:00Z", requests[3].second.getString("period_end"))
            assertEquals(List(4) { "record_id=$id&organization_id=$org&limit=1" }, paths.toList())
        } finally {
            ApiClientProvider.createService(previousUrl)
            api.setSession(previous[0], previous[1], previous[2], previous[3])
            server.stop(0)
        }
    }
}
