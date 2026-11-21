package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.SaasApi
import com.example.ui.screens.platformPlanDraft
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
class NativePlatformPlanTest {
    @Test fun explicitPriceAndCapacitySurviveCreateAndEditReadback() {
        val id = UUID(95, 1).toString()
        fun draft(price: String, students: String = "18", existing: String = "") =
            platformPlanDraft(existing, "package_a", " Girilen paket ", price, "EUR", "YEARLY", "3", students, "12", false)
        assertTrue(runCatching { draft("") }.isFailure)
        assertTrue(runCatching { draft("4,567") }.isFailure)
        assertTrue(runCatching { draft("4,56", "") }.isFailure)
        assertEquals(0.0, draft("0").price, 0.0)
        val updated = draft("4,56", existing = id)
        val api = ApiClientProvider.interceptor
        val previousUrl = ApiClientProvider.activeBaseUrl
        val previous = listOf(api.authToken, api.tenantId, api.branchId, api.workspaceOrganizationId)
        val stored = AtomicReference<JSONObject>()
        val stale = AtomicBoolean(true)
        val paths = CopyOnWriteArrayList<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/platform/plans") { request ->
            paths += "${request.requestMethod} ${request.requestURI.path}"
            val payload = if (request.requestMethod == "GET") {
                JSONArray().put(JSONObject(stored.get().toString()).apply { if (stale.get()) put("price", "4.00") })
            } else {
                JSONObject(request.requestBody.bufferedReader().use { it.readText() }).put("id", id).also(stored::set)
            }
            val bytes = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            request.responseHeaders.set("Content-Type", "application/json")
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            api.setSession("isolated-plan")
            runBlocking {
                val client = SaasApi()
                assertTrue(runCatching { client.savePlan(updated) }.isFailure)
                stale.set(false)
                assertEquals(updated, client.savePlan(updated))
                assertEquals(updated, client.savePlan(draft("4.56")))
            }
            assertEquals("PACKAGE_A", stored.get().getString("code"))
            assertEquals(12, stored.get().getInt("period_months"))
            assertEquals(18, stored.get().getInt("student_limit"))
            assertEquals(listOf("PUT /api/platform/plans/$id", "GET /api/platform/plans",
                "PUT /api/platform/plans/$id", "GET /api/platform/plans",
                "POST /api/platform/plans", "GET /api/platform/plans"), paths.toList())
        } finally {
            ApiClientProvider.createService(previousUrl)
            api.setSession(previous[0], previous[1], previous[2], previous[3])
            server.stop(0)
        }
    }
}
