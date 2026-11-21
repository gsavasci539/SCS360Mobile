package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeTutoringClient
import com.example.data.remote.TutoringRecords
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeTutoringClientTest {
    @Test fun attendanceRequiresTheExactSavedStudentStatusAndProgressScore() {
        val branch = UUID(30, 1).toString()
        val student = UUID(30, 2).toString()
        val session = UUID(30, 3).toString()
        val confirmed = AtomicBoolean(false)
        val requests = CopyOnWriteArrayList<String>()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/tutoring/sessions") { request ->
            requests += "${request.requestMethod} ${request.requestURI}"
            val record = JSONObject().put("id", session).put("status", "COMPLETED").put("student_id", student)
                .put("attendance_status", "PRESENT").put("progress_score", if (confirmed.get()) 87 else 80)
            val payload = if (request.requestMethod == "POST") record else JSONArray().put(record)
            val bytes = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            request.responseHeaders.set("Content-Type", "application/json")
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-tutoring", UUID(30, 4).toString(), branch)
            runBlocking {
                val client = NativeTutoringClient(branch)
                val row = JSONObject().put("id", session).put("student_id", student)
                val form = JSONObject().put("attendance_status", "PRESENT").put("progress_score", 87)
                assertTrue(runCatching { client.action(TutoringRecords.SESSIONS, row, "attendance", form) }.isFailure)
                confirmed.set(true)
                client.action(TutoringRecords.SESSIONS, row, "attendance", form)
            }
            assertEquals(4, requests.size)
            assertTrue(requests.all { "branch_id=$branch" in it })
            assertTrue(requests.filter { it.startsWith("GET") }.all { "record_id=$session" in it })
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
