package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.Okul360Database
import com.example.data.model.Invoice
import com.example.data.remote.*
import com.example.data.repository.Okul360Repository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeDebtCreateBackendAcceptance {
    @Test fun realOptionsLostAckReplayAndNoLocalLedgerWrites() = runBlocking {
        fun env(key: String) = requireNotNull(System.getenv(key))
        val context = ApplicationProvider.getApplicationContext<Context>()
        val oldBase = ApiClientProvider.activeBaseUrl; val oldToken = ApiClientProvider.interceptor.authToken
        try {
            ApiClientProvider.createService(env("NATIVE_TEST_API_URL"))
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_FINANCE_TOKEN"), branch = env("NATIVE_TEST_BRANCH"))
            val dao = Okul360Database.getDatabase(context).invoiceDao(); val before = dao.getAllInvoicesList()
            fun editor() = NativeDebtCreateSession(context, env("NATIVE_TEST_FINANCE_ACTOR"))
            val first = editor(); val options = first.load(); assertEquals(102, options.size)
            val draft = DebtCreateDraft(env("NATIVE_TEST_STUDENT"), "EDUCATION", "100,01", "2027-01-15", "Synthetic tuition")
            try { first.create(draft); fail("First committed ACK is deliberately dropped") } catch (_: java.io.IOException) { }
            assertTrue(first.hasPending); assertEquals(before, dao.getAllInvoicesList())
            val restored = editor(); restored.load(); val identifier = restored.create(restored.pendingDraft()!!)
            assertEquals(identifier, restored.create(draft)); restored.finish()
            assertEquals(before, dao.getAllInvoicesList())
            context.getSharedPreferences("scs360_api_connection", Context.MODE_PRIVATE).edit().remove("base_url").commit()
            try { Okul360Repository(context).addInvoice(Invoice(id = identifier)); fail("Legacy write cannot bypass canonical create") }
            catch (_: IllegalStateException) { }
            assertEquals(before, dao.getAllInvoicesList())
            ApiClientProvider.interceptor.branchId = env("NATIVE_TEST_FOREIGN_BRANCH")
            try { editor().load(); fail("Foreign branch options must be denied") } catch (e: DebtCreateHttpException) { assertEquals(404, e.code) }
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_PARENT_TOKEN"))
            try { NativeDebtCreateSession(context, env("NATIVE_TEST_PARENT_ACTOR")).load(); fail("Parent cannot create staff debt") }
            catch (e: DebtCreateHttpException) { assertEquals(403, e.code) }
        } finally { ApiClientProvider.createService(oldBase); ApiClientProvider.interceptor.setSession(oldToken) }
    }
}
