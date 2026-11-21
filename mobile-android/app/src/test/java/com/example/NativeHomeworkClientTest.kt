package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeHomeworkClient
import com.example.data.remote.RecordedHomeworkSubmission
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
import java.math.BigDecimal
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeHomeworkClientTest {
    @Test fun gradeVerifiesTheExactSubmissionAndBranchBeforeSuccess() {
        val branch = UUID(22, 1).toString()
        val homework = UUID(22, 2).toString()
        val student = UUID(22, 3).toString()
        val submission = UUID(22, 4).toString()
        val score = BigDecimal("87.25")
        val feedback = "İzole değerlendirme"
        val confirmed = AtomicBoolean(false)
        val mutation = AtomicReference<JSONObject>()
        val requests = CopyOnWriteArrayList<String>()
        val base = ApiClientProvider.activeBaseUrl
        val auth = ApiClientProvider.interceptor
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/homework/submissions") { exchange ->
            requests += "${exchange.requestMethod} ${exchange.requestURI}"
            val payload = if (exchange.requestMethod == "PUT") {
                mutation.set(JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() }))
                JSONObject().put("id", submission).put("status", "GRADED")
            } else {
                JSONArray().put(JSONObject().put("id", submission).put("homework_id", homework)
                    .put("student_id", student).put("student_name", "İzole öğrenci")
                    .put("status", "GRADED").put("score", score.toPlainString())
                    .put("teacher_feedback", if (confirmed.get()) feedback else "Önceki kayıt")
                    .put("submitted_at", "2026-11-21T03:00:00Z"))
            }
            val bytes = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-homework", UUID(22, 5).toString(), branch)
            runBlocking {
                val client = NativeHomeworkClient(branch)
                val row = RecordedHomeworkSubmission(submission, student, "İzole öğrenci", "", "SUBMITTED",
                    null, "", "2026-11-21T03:00:00Z", "", "")
                assertTrue(runCatching { client.grade(homework, row, score, feedback) }.isFailure)
                confirmed.set(true)
                client.grade(homework, row, score, feedback)
            }
            assertEquals("87.25", mutation.get().getString("score"))
            assertEquals(feedback, mutation.get().getString("teacher_feedback"))
            assertEquals(4, requests.size)
            assertTrue(requests.all { "branch_id=$branch" in it })
            assertTrue(requests.filter { it.startsWith("GET ") }.all { "record_id=$submission" in it && "homework_id=$homework" in it })
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
