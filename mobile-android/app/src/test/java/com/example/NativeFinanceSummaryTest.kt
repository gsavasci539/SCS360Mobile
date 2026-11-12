package com.example

import com.example.data.remote.*
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeFinanceSummaryTest {
    private val branch = UUID(1, 1).toString()
    private fun summary(branchId: String? = null) = JSONObject().put("analysis_type", "recorded_finance")
        .put("branch_id", branchId ?: JSONObject.NULL).put("forecast_available", false).put("as_of", "2026-11-12")
        .put("total_debt", "100.01").put("total_paid", "25.01").put("outstanding_debt", "75.00")
        .put("overdue_amount", "50.00").put("collected_total", "25.01").put("collection_rate", "25.0")
        .put("debt_count", 101).put("payment_count", 1).put("requires_review", false)
        .put("historical_monthly", JSONArray().put(JSONObject().put("month", "2026-11")
            .put("collected_amount", "25.01").put("payment_count", 1)))
    private fun reply(e: HttpExchange, data: JSONObject, code: Int = 200, success: Boolean = true) {
        val bytes = JSONObject().put("success", success).put("data", data).toString().toByteArray()
        e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(code, bytes.size.toLong())
        e.responseBody.use { it.write(bytes) }
    }
    private suspend fun server(handler: (HttpExchange) -> Unit, work: suspend () -> Unit) {
        val oldBase = ApiClientProvider.activeBaseUrl; val oldToken = ApiClientProvider.interceptor.authToken
        val oldBranch = ApiClientProvider.interceptor.branchId; val oldOrg = ApiClientProvider.interceptor.workspaceOrganizationId
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0); s.createContext("/", handler); s.start()
        ApiClientProvider.createService("http://127.0.0.1:${s.address.port}/api/")
        ApiClientProvider.interceptor.setSession("synthetic-finance-token")
        try { work() } finally { s.stop(0); ApiClientProvider.createService(oldBase)
            ApiClientProvider.interceptor.setSession(oldToken, branch = oldBranch, workspace = oldOrg) }
    }
    @Test fun recordedSummaryPreservesCentsHistoryAndExplicitBranch() = runBlocking {
        server({ e ->
            assertEquals("/api/ai/payment-forecast", e.requestURI.path)
            assertEquals("months=6&branch_id=$branch", e.requestURI.rawQuery)
            reply(e, summary(branch))
        }) {
            ApiClientProvider.interceptor.branchId = branch
            val value = NativeFinanceSummaryClient().load(branch)
            assertEquals(BigDecimal("100.01"), value.totalDebt); assertEquals(BigDecimal("75.00"), value.outstandingDebt)
            assertEquals(101, value.debtCount); assertEquals("2026-11", value.historicalMonthly.single().month)
            assertEquals(BigDecimal("25.01"), value.historicalMonthly.single().amount)
        }
    }
    @Test fun reviewedUnknownAmountsRemainUnknownAndEmptyHistoryDoesNotCreateMonths() = runBlocking {
        val data = summary().put("total_debt", JSONObject.NULL).put("total_paid", JSONObject.NULL)
            .put("outstanding_debt", JSONObject.NULL).put("collection_rate", JSONObject.NULL)
            .put("requires_review", true).put("historical_monthly", JSONArray())
        server({ reply(it, data) }) {
            val value = NativeFinanceSummaryClient().load("all")
            assertNull(value.totalDebt); assertNull(value.totalPaid); assertNull(value.outstandingDebt)
            assertNull(value.collectionRate); assertTrue(value.requiresReview && value.historicalMonthly.isEmpty())
        }
    }
    @Test fun serverFailureAndNegativeEnvelopeNeverBecomeZeroBalance() = runBlocking {
        var calls = 0
        server({ e -> calls++; reply(e, summary(), if (calls == 1) 503 else 200, success = calls == 1) }) {
            try { NativeFinanceSummaryClient().load(null); fail("503 must propagate") }
            catch (e: FinanceSummaryHttpException) { assertEquals(503, e.code) }
            try { NativeFinanceSummaryClient().load(null); fail("Negative envelope must propagate") }
            catch (_: java.io.IOException) { }
            assertEquals(2, calls)
        }
    }
    @Test fun staleIdentityOrMismatchedBranchCannotSendRequest() = runBlocking {
        var calls = 0
        server({ calls++; reply(it, summary()) }) {
            val stale = NativeFinanceSummaryClient(); ApiClientProvider.interceptor.setSession("new-actor")
            try { stale.load(null); fail("Stale actor must fail") } catch (_: kotlinx.coroutines.CancellationException) { }
            try { NativeFinanceSummaryClient().load(branch); fail("Branch must agree with active context") }
            catch (_: IllegalArgumentException) { }
            assertEquals(0, calls)
        }
    }
    @Test fun wrongBranchMissingFieldsAndInvalidMoneyAreRejected() = runBlocking {
        var data = summary(branch)
        server({ reply(it, data) }) {
            for (invalid in listOf(summary(branch), summary().apply { remove("collected_total") },
                summary().put("total_debt", "NaN"), summary().put("total_debt", "1.001"), summary().put("total_debt", "-1"))) {
                data = invalid
                try { NativeFinanceSummaryClient().load(null); fail("Invalid aggregate must fail") } catch (_: Exception) { }
            }
        }
    }
}
