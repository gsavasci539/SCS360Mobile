package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeTransportClient
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
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeTransportClientTest {
    @Test fun checkInUsesSchoolDateAndReadsBackOnlySelectedStudent() {
        val branch = UUID(17, 1).toString()
        val routeId = UUID(17, 2).toString()
        val student = UUID(17, 3).toString()
        val record = UUID(17, 4).toString()
        val operation = UUID(17, 5)
        val requests = CopyOnWriteArrayList<String>()
        val mutations = CopyOnWriteArrayList<JSONObject>()
        val base = ApiClientProvider.activeBaseUrl
        val auth = ApiClientProvider.interceptor
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val route = JSONObject().put("id", routeId).put("branch_id", branch).put("route_name", "İzole rota")
            .put("capacity", 15).put("status", "ACTIVE")
        server.createContext("/api/bus/") { exchange ->
            val uri = exchange.requestURI
            requests += uri.toString()
            val payload: Any = when {
                uri.path.endsWith("/latest") -> JSONObject.NULL
                uri.path.endsWith("/check-in") -> {
                    val sent = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                    mutations += sent
                    JSONObject().put("id", record).put("route_id", routeId).put("student_id", student)
                        .put("event", "DROPPED").put("timestamp", "2026-11-21T17:15:00+03:00").put("dropoff_time", "17:15:00")
                }
                uri.path.endsWith("/attendance") -> JSONArray().put(JSONObject().put("id", record).put("route_id", routeId)
                    .put("student_id", student).put("date", "2026-11-21").put("student_name", "İzole öğrenci")
                    .put("dropoff_time", "17:15").put("status", "PRESENT"))
                else -> route
            }
            val bytes = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-transit", UUID(17, 8).toString(), branch)
            runBlocking {
                val client = NativeTransportClient(branch)
                val chosen = client.detail(routeId)
                assertNull(client.latest(chosen))
                client.checkIn(chosen, student, "DROPPED", LocalDateTime.parse("2026-11-21T17:15"), operation)
            }
            assertEquals(operation.toString(), mutations.single().getString("request_id"))
            assertEquals("2026-11-21T17:15", mutations.single().getString("timestamp"))
            assertEquals("DROPPED", mutations.single().getString("event"))
            assertTrue(requests.any { it == "/api/bus/check-in?branch_id=$branch" })
            assertTrue(requests.any { "?route_id=$routeId" in it && "attendance_date=2026-11-21" in it && "student_id=$student" in it })
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
