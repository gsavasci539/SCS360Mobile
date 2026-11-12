package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeCashCollectionBackendAcceptance {
    @Test fun lostRealApiAckReplaysOnceAndAnotherExplicitCollectionIsPartial() = runBlocking {
        fun env(key: String) = requireNotNull(System.getenv(key))
        val context = ApplicationProvider.getApplicationContext<Context>()
        val oldBase = ApiClientProvider.activeBaseUrl; val oldToken = ApiClientProvider.interceptor.authToken
        try {
            ApiClientProvider.createService(env("NATIVE_TEST_API_URL"))
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_FINANCE_TOKEN"))
            fun editor() = NativeCashCollection(context, env("NATIVE_TEST_FINANCE_ACTOR"), env("NATIVE_TEST_DEBT"))
            val first = editor(); assertEquals(BigDecimal("100.01"), first.load().remaining)
            try { first.collect("30.01", "CASH"); fail("Transport drops the first committed ACK") }
            catch (_: java.io.IOException) { }
            assertTrue(first.hasPending)
            val restored = editor(); assertEquals(BigDecimal("70.00"), restored.load().remaining)
            val ack = restored.collect(restored.pendingAmount()!!, restored.pendingMethod()!!)
            assertEquals(BigDecimal("70.00"), ack.remainingStudentDebt); assertTrue(ack.receiptNumber.startsWith("RCP-")); restored.finish()
            val next = editor(); next.load(); val second = next.collect("10.00", "EFT")
            assertNotEquals(ack.paymentId, second.paymentId); assertEquals(BigDecimal("60.00"), second.remainingStudentDebt); next.finish()
            val receipts = NativeReceiptClient(env("NATIVE_TEST_DEBT"))
            val documents = receipts.load(); assertEquals(2, documents.size)
            assertEquals(setOf(ack.paymentId, second.paymentId), documents.map { it.paymentId }.toSet())
            val pdf = receipts.pdf(documents.first()); assertTrue(pdf.copyOfRange(0, 5).contentEquals("%PDF-".toByteArray()))
            val fresh = editor(); assertEquals(BigDecimal("60.00"), fresh.load().remaining)
            try { fresh.collect("61.00", "CASH"); fail("Overpayment must fail") } catch (_: IllegalArgumentException) { }
            ApiClientProvider.interceptor.branchId = env("NATIVE_TEST_OTHER_BRANCH")
            try { editor().load(); fail("Selected branch must match the debt metadata") } catch (_: IllegalStateException) { }
            ApiClientProvider.interceptor.branchId = null
            try { NativeCashCollection(context, env("NATIVE_TEST_FINANCE_ACTOR"), env("NATIVE_TEST_FOREIGN_DEBT")).load(); fail("Foreign debt denied") }
            catch (e: CashCollectionHttpException) { assertEquals(404, e.code) }
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_PARENT_TOKEN"))
            try { NativeCashCollection(context, env("NATIVE_TEST_PARENT_ACTOR"), env("NATIVE_TEST_DEBT")).load(); fail("Staff ledger denied to parent") }
            catch (e: CashCollectionHttpException) { assertEquals(403, e.code) }
        } finally { ApiClientProvider.createService(oldBase); ApiClientProvider.interceptor.setSession(oldToken) }
    }
}
