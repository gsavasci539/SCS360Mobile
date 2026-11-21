package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeCanteenClient
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
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeCanteenClientTest {
    @Test fun saleShowsRefreshedBalanceOnlyAfterExactOrderAndLedgerProof() {
        val school = UUID(25, 1).toString()
        val branch = UUID(25, 2).toString()
        val student = UUID(25, 3).toString()
        val product = UUID(25, 4).toString()
        val entry = UUID(25, 5).toString()
        val order = UUID(25, 6).toString()
        val request = UUID(25, 7)
        val confirmed = AtomicBoolean(false)
        val payload = AtomicReference<JSONObject>()
        val reads = CopyOnWriteArrayList<String>()
        val base = ApiClientProvider.activeBaseUrl
        val auth = ApiClientProvider.interceptor
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/canteen/") { exchange ->
            val uri = exchange.requestURI
            reads += "${exchange.requestMethod} $uri"
            val value: Any = when {
                uri.path.endsWith("/pos/checkout") -> {
                    payload.set(JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() }))
                    JSONObject().put("authorized", true).put("operation_id", request.toString()).put("student_id", student)
                        .put("branch_id", branch).put("amount", "8.25").put("transaction_id", entry).put("order_id", order)
                }
                uri.path.endsWith("/products") -> JSONArray().put(JSONObject().put("id", product).put("name", "İzole ürün")
                    .put("price", "8.25").put("branch_id", branch).put("is_available", true).put("allergens", JSONArray()))
                uri.path.endsWith("/transactions") -> JSONArray().put(JSONObject().put("id", entry).put("student_id", student)
                    .put("amount", "8.25").put("transaction_type", "DEBIT").put("payment_method", "WALLET")
                    .put("date", "2026-11-21T05:00:00Z").put("status", "COMPLETED"))
                uri.path.endsWith("/orders") -> JSONArray().apply {
                    if (confirmed.get()) put(JSONObject().put("id", order).put("student_id", student).put("branch_id", branch)
                        .put("product_id", product).put("quantity", 1).put("status", "COMPLETED").put("total_price", "8.25"))
                }
                else -> JSONObject().put("student_id", student).put("branch_id", branch).put("student_name", "İzole öğrenci")
                    .put("balance", if (confirmed.get()) "11.75" else "20.00").put("daily_spent", "0.00")
                    .put("dietary_restrictions", JSONArray()).put("restricted_categories", JSONArray()).put("daily_limit", JSONObject.NULL)
            }
            val bytes = JSONObject().put("success", true).put("data", value).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-canteen", school, branch)
            runBlocking {
                val client = NativeCanteenClient(branch)
                val wallet = client.wallet(student)
                val item = client.products(wallet, 10, 0).single()
                assertTrue(runCatching { client.sell(wallet, item, 1, "WALLET", request) }.isFailure)
                assertEquals(1, reads.count { "/wallet?" in it })
                confirmed.set(true)
                assertEquals("11.75", client.sell(wallet, item, 1, "WALLET", request).balance.toPlainString())
            }
            assertEquals("8.25", payload.get().getString("expected_unit_price"))
            assertEquals(branch, payload.get().getString("branch_id"))
            assertTrue(reads.any { "record_id=$entry" in it && "branch_id=$branch" in it })
            assertTrue(reads.any { "record_id=$order" in it && "student_id=$student" in it })
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
