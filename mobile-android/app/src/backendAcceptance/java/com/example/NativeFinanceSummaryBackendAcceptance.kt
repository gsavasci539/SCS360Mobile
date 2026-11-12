package com.example

import com.example.data.remote.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

/** Opt-in: Python owns the real FastAPI process and temporary ledger fixture. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeFinanceSummaryBackendAcceptance {
    @Test fun recordedLedgerSummaryUsesRealApiTotalsAndRejectsUnauthorizedScope() = runBlocking {
        fun env(key: String) = requireNotNull(System.getenv(key))
        val oldBase = ApiClientProvider.activeBaseUrl; val oldToken = ApiClientProvider.interceptor.authToken
        val oldBranch = ApiClientProvider.interceptor.branchId; val oldOrg = ApiClientProvider.interceptor.workspaceOrganizationId
        try {
            ApiClientProvider.createService(env("NATIVE_TEST_API_URL"))
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_FINANCE_TOKEN"))
            val all = NativeFinanceSummaryClient().load(null)
            assertEquals(102, all.debtCount); assertEquals(BigDecimal("10400.04"), all.totalDebt)
            assertEquals(BigDecimal("100.00"), all.totalPaid); assertEquals(BigDecimal("10300.04"), all.outstandingDebt)
            assertEquals(BigDecimal("100.00"), all.collectedTotal); assertEquals(1, all.paymentCount)
            assertEquals(1, all.historicalMonthly.size)
            val branch = env("NATIVE_TEST_BRANCH")
            ApiClientProvider.interceptor.branchId = branch
            val selected = NativeFinanceSummaryClient().load(branch)
            assertEquals(101, selected.debtCount); assertEquals(BigDecimal("10100.01"), selected.totalDebt)
            assertEquals(BigDecimal("10000.01"), selected.outstandingDebt)
            val foreign = env("NATIVE_TEST_FOREIGN_BRANCH")
            ApiClientProvider.interceptor.branchId = foreign
            try { NativeFinanceSummaryClient().load(foreign); fail("Foreign branch must be denied") }
            catch (failure: FinanceSummaryHttpException) { assertTrue(failure.code in listOf(403, 404)) }
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_PARENT_TOKEN"))
            try { NativeFinanceSummaryClient().load(null); fail("Parent cannot read staff summary") }
            catch (failure: FinanceSummaryHttpException) { assertEquals(403, failure.code) }
        } finally {
            ApiClientProvider.createService(oldBase)
            ApiClientProvider.interceptor.setSession(oldToken, branch = oldBranch, workspace = oldOrg)
        }
    }
}
