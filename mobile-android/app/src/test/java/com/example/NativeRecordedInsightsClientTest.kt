package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeRecordedInsightsClient
import com.example.data.remote.RecordedInsight
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeRecordedInsightsClientTest {
    @Test fun gradeSummaryRejectsPredictionsAndDoesNotRequestOrganizationSummaryForSelectedBranch() {
        val branch = UUID(71, 15).toString()
        val organization = UUID(71, 16).toString()
        val predictions = AtomicBoolean(true)
        val count = AtomicInteger()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/ai/academic-performance") { request ->
            count.incrementAndGet()
            val data = JSONObject().put("analysis_kind", "RECORDED_GRADES")
                .put("prediction_available", predictions.get()).put("ai_model", JSONObject.NULL)
                .put("valid_grade_count", 3)
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            runBlocking {
                auth.setSession("isolated-insights", organization, branch)
                assertTrue(runCatching { NativeRecordedInsightsClient().load(RecordedInsight.GRADES) }.isFailure)
                assertEquals(0, count.get())
                auth.setSession("isolated-insights", organization, null)
                val client = NativeRecordedInsightsClient()
                assertTrue(runCatching { client.load(RecordedInsight.GRADES) }.isFailure)
                predictions.set(false)
                assertEquals(3, client.load(RecordedInsight.GRADES).getInt("valid_grade_count"))
                assertEquals(2, count.get())
            }
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
