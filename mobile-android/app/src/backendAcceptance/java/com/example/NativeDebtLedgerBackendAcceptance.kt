package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.Okul360Database
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
class NativeDebtLedgerBackendAcceptance {
    @Test fun realLedgerPagesEditsCancellationAndAuthorizationNeverPrewriteRoom() = runBlocking {
        fun env(key: String) = requireNotNull(System.getenv(key))
        val context = ApplicationProvider.getApplicationContext<Context>()
        val i = ApiClientProvider.interceptor; val oldBase=ApiClientProvider.activeBaseUrl
        val token=i.authToken; val branch=i.branchId; val workspace=i.workspaceOrganizationId
        try {
            ApiClientProvider.createService(env("NATIVE_TEST_API_URL"))
            i.setSession(env("NATIVE_TEST_FINANCE_TOKEN"),branch=env("NATIVE_TEST_BRANCH"))
            context.getSharedPreferences("scs360_api_connection",0).edit().remove("base_url").commit()
            val repo=Okul360Repository(context); val dao=Okul360Database.getDatabase(context).invoiceDao()
            val before=dao.getAllInvoicesList(); val rows=repo.getInvoices(env("NATIVE_TEST_BRANCH"),null)
            assertEquals(102,rows.size); assertEquals(102,rows.map { it.id }.distinct().size)
            assertTrue(rows.all { it.student_name.isNotBlank() && it.class_name.isNotBlank() && it.invoice_number.isBlank() })
            assertEquals(before,dao.getAllInvoicesList())
            val edit=rows.single { it.id==env("NATIVE_TEST_EDIT_DEBT") }
            val updated=repo.updateInvoice(edit.copy(amount=110.02,due_date="2027-01-16",description="Synthetic updated ledger note"))
            assertEquals(110.02,updated.amount,0.0); assertEquals("2027-01-16",updated.due_date)
            assertEquals("Synthetic updated ledger note",updated.description)
            val paid=rows.single { it.id==env("NATIVE_TEST_PAID_DEBT") }
            try { repo.updateInvoice(paid.copy(amount=120.02)); fail("Paid amount change") }
            catch (e: DebtLedgerHttpException) { assertEquals(422,e.code) }
            try { repo.deleteInvoice(paid.id); fail("Paid debt cancellation") }
            catch (e: DebtLedgerHttpException) { assertEquals(422,e.code) }
            assertTrue(repo.deleteInvoice(env("NATIVE_TEST_DELETE_DEBT")))
            val after=repo.getInvoices(env("NATIVE_TEST_BRANCH"),null)
            assertEquals(101,after.size); assertFalse(after.any { it.id==env("NATIVE_TEST_DELETE_DEBT") })
            assertEquals(101,repo.getDebts(env("NATIVE_TEST_STUDENT")).size)
            assertEquals(before,dao.getAllInvoicesList())
            i.branchId=env("NATIVE_TEST_FOREIGN_BRANCH")
            try { repo.getDebts(); fail("Foreign branch") } catch(e: DebtLedgerHttpException) { assertEquals(404,e.code) }
            i.setSession(env("NATIVE_TEST_PARENT_TOKEN"))
            try { repo.getDebts(); fail("Parent staff ledger") } catch(e: DebtLedgerHttpException) { assertEquals(403,e.code) }
            assertEquals(before,dao.getAllInvoicesList())
        } finally { ApiClientProvider.createService(oldBase); i.setSession(token,branch=branch,workspace=workspace) }
    }
}
