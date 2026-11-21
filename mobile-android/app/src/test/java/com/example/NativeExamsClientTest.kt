package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.ExamGradeDraft
import com.example.data.remote.NativeExamsClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
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
class NativeExamsClientTest {
    @Test fun savedZeroNeedsAnExactPersistedStudentResult() {
        val org = UUID(86, 1).toString()
        val branch = UUID(86, 2).toString()
        val exam = UUID(86, 3).toString()
        val student = UUID(86, 4).toString()
        val result = UUID(86, 5).toString()
        val verified = AtomicBoolean(false)
        val requests = CopyOnWriteArrayList<String>()
        val auth = ApiClientProvider.interceptor
        val originalUrl = ApiClientProvider.activeBaseUrl
        val originalToken = auth.authToken
        val originalTenant = auth.tenantId
        val originalBranch = auth.branchId
        val originalWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/exams") { request ->
            requests += request.requestURI.toString()
            val row = JSONObject().put("id", result).put("exam_id", exam).put("student_id", student)
                .put("score", if (request.requestMethod == "POST" || verified.get()) 0 else 1)
                .put("grade_scale", JSONObject.NULL).put("notes", JSONObject.NULL)
            val data = if (request.requestMethod == "POST") JSONObject().put("items", JSONArray().put(row)) else JSONArray().put(row)
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-exam", org, branch)
            runBlocking {
                val client = NativeExamsClient(branch)
                val grades = mapOf(student to ExamGradeDraft("0.00", "", ""))
                assertTrue(runCatching { client.grades(exam, grades) }.isFailure)
                verified.set(true)
                client.grades(exam, grades)
            }
            assertTrue(requests.size == 4 && requests.all { "branch_id=$branch" in it })
        } finally {
            ApiClientProvider.createService(originalUrl)
            auth.setSession(originalToken, originalTenant, originalBranch, originalWorkspace)
            server.stop(0)
        }
    }
}
