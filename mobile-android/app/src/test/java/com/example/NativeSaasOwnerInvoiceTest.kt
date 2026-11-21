package com.example

import com.example.data.model.SubscriptionPlan
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.SaasApi
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class NativeSaasOwnerInvoiceTest {
    @Test fun freeRequestCancelActivationAndStatusUseTheExactInvoice() {
        val org = UUID(97, 1).toString()
        val id = UUID(97, 2).toString()
        val plan = SubscriptionPlan(id = UUID(97, 3).toString(), currency = "TRY", price = 0.0, is_active = true)
        val requestId = UUID(97, 4).toString()
        val checkout = UUID(97, 5).toString()
        val row = JSONObject().put("id", id).put("organization_id", org).put("plan_id", plan.id)
            .put("amount", "0.00").put("currency", plan.currency).put("status", "PENDING").put("can_cancel", true)
        val recorded = AtomicReference(JSONObject(row.toString()))
        val stale = AtomicBoolean(false)
        val calls = CopyOnWriteArrayList<String>()
        val body = AtomicReference<JSONObject>()
        val api = ApiClientProvider.interceptor
        val previousUrl = ApiClientProvider.activeBaseUrl
        val previous = listOf(api.authToken, api.tenantId, api.branchId, api.workspaceOrganizationId)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/") { exchange ->
            val uri = exchange.requestURI
            calls += "${exchange.requestMethod} $uri"
            val key = uri.path.substringAfterLast('/')
            val output = when {
                exchange.requestMethod == "GET" -> {
                    val current = JSONObject(recorded.get().toString())
                    if (stale.get()) current.put("checkout_id", UUID(97, 6).toString())
                    JSONArray().put(current)
                }
                key == "invoices" -> {
                    body.set(JSONObject(exchange.requestBody.bufferedReader().use { it.readText() }))
                    JSONObject(row.toString()).also(recorded::set)
                }
                key == "cancel" -> JSONObject(row.toString()).put("status", "CANCELLED").put("can_cancel", false).also(recorded::set)
                key == "activate-free" -> {
                    val paid = JSONObject(row.toString()).put("status", "PAID").put("can_cancel", false).also(recorded::set)
                    JSONObject().put("invoice", paid).put("status", "PAID").put("checkout_id", JSONObject.NULL)
                }
                else -> {
                    val paid = JSONObject(row.toString()).put("status", "PAID").put("checkout_status", "PAID")
                        .put("checkout_id", checkout).put("can_cancel", false).also(recorded::set)
                    JSONObject().put("invoice", paid).put("status", "PAID").put("checkout_id", checkout)
                }
            }
            val bytes = JSONObject().put("success", true).put("data", output).toString().toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            api.setSession("isolated-owner", org)
            runBlocking {
                val client = SaasApi()
                assertTrue(runCatching { client.requestFreePackage(org, plan.copy(price = 4.0), requestId) }.isFailure)
                assertTrue(calls.isEmpty())
                val target = client.requestFreePackage(org, plan, requestId)
                assertEquals(id, target.id)
                assertEquals("CANCELLED", client.changeOwnerInvoice(target, false).status)
                assertEquals("PAID", client.changeOwnerInvoice(target, true).status)
                val online = target.copy(checkout_id = checkout, checkout_status = "PENDING", can_cancel = false)
                stale.set(true)
                assertTrue(runCatching { client.checkInvoiceStatus(online, false) }.isFailure)
                stale.set(false)
                assertEquals(checkout, client.checkInvoiceStatus(online, false).checkout_id)
                assertEquals("PAID", client.checkInvoiceStatus(online, true).status)
            }
            assertEquals(plan.id, body.get().getString("plan_id"))
            assertEquals(requestId, body.get().getString("request_id"))
            assertTrue(body.get().getBoolean("free_only"))
            assertFalse(body.get().has("amount"))
            assertEquals(5, calls.count { it == "GET /api/subscription/invoices?record_id=$id&limit=1" })
            assertTrue(calls.contains("GET /api/platform/invoices?record_id=$id&organization_id=$org&limit=1"))
            assertTrue(calls.contains("POST /api/subscription/invoices/$id/payment-status"))
            assertTrue(calls.contains("POST /api/platform/invoices/$id/payment-status"))
        } finally {
            ApiClientProvider.createService(previousUrl)
            api.setSession(previous[0], previous[1], previous[2], previous[3])
            server.stop(0)
        }
    }
}
