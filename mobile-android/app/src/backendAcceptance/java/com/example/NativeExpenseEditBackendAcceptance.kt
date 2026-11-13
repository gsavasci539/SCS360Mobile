package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.Okul360Database
import com.example.data.remote.*
import com.example.data.repository.Okul360Repository
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36],manifest=Config.NONE)
class NativeExpenseEditBackendAcceptance {
    @Test fun realNativeExpenseDeltaEditAckRecoveryPreservesNotesAndWebLedger()=runBlocking {
        fun env(key:String)=requireNotNull(System.getenv(key))
        val context=ApplicationProvider.getApplicationContext<Context>()
        val i=ApiClientProvider.interceptor; val oldBase=ApiClientProvider.activeBaseUrl
        val token=i.authToken; val branch=i.branchId; val workspace=i.workspaceOrganizationId
        suspend fun web(path:String,payload:String?=null) {
            val scope=i.captureScope()
            scope.io {
                val builder=Request.Builder().url(ApiClientProvider.activeBaseUrl+path).tag(NativeRequestScope::class.java,scope)
                if(payload==null) builder.delete() else builder.put(payload.toRequestBody("application/json".toMediaType()))
                ApiClientProvider.createOkHttpClient().newCall(builder.build()).execute().use { check(it.isSuccessful) { "Web expense HTTP ${it.code}" } }
            }
        }
        try {
            ApiClientProvider.createService(env("NATIVE_TEST_API_URL")); i.setSession(env("NATIVE_TEST_FINANCE_TOKEN"),branch=env("NATIVE_TEST_BRANCH"))
            context.getSharedPreferences("scs360_api_connection",0).edit().remove("base_url").commit()
            val repo=Okul360Repository(context); val dao=Okul360Database.getDatabase(context).invoiceDao(); val before=dao.getAllInvoicesList()
            fun editor()=NativeExpenseCreateSession(context,env("NATIVE_TEST_FINANCE_ACTOR"))
            val first=editor(); assertEquals(1,first.load().size)
            val draft=ExpenseDraft(env("NATIVE_TEST_BRANCH"),"Synthetic native expense","Synthetic category","100,01","2027-01-15")
            try { first.create(draft); fail("Lost committed ACK") } catch(_:java.io.IOException) { }
            val restored=editor(); restored.load(); val created=restored.create(restored.pendingDraft()!!); restored.finish()
            assertEquals(100.01,created.amount,0.0); assertEquals("Synthetic category",created.category); assertEquals("2027-01-15",created.date)
            assertEquals(101,repo.getExpenses(env("NATIVE_TEST_BRANCH")).size)
            web("finance/expenses/${created.id}","""{"amount":"110.02","description":"Updated from web","category":"Web category","expense_date":"2027-01-16T00:00:00+03:00"}""")
            val edited=repo.getExpenses(env("NATIVE_TEST_BRANCH")).single { it.id==created.id }
            assertEquals(110.02,edited.amount,0.0); assertEquals("Updated from web",edited.title); assertEquals("Web category",edited.category); assertEquals("2027-01-16",edited.date)
            val detail=NativeExpenseClient().request("finance/expenses/${created.id}")
            val editorSession=NativeExpenseEditSession(created.id,detail.getString("organization_id"))
            val editDraft=editorSession.load().draft().copy(amount="120.03",notes="Native ref")
            try { editorSession.save(editDraft); fail("Lost committed edit ACK") } catch(_:java.io.IOException) { }
            assertTrue(editorSession.hasPending)
            val nativeEdited=editorSession.save(editDraft); assertEquals(120.03,nativeEdited.expense.amount,0.0);assertEquals("Native ref",nativeEdited.expense.notes)
            val visibleToWeb=NativeExpenseClient().request("finance/expenses/${created.id}")
            assertEquals("120.03",visibleToWeb.getString("amount"));assertEquals("Native ref",visibleToWeb.getString("notes"))
            web("finance/expenses/${created.id}"); assertTrue(repo.deleteExpense(env("NATIVE_TEST_SEEDED_EXPENSE")))
            val remaining=repo.getExpenses(env("NATIVE_TEST_BRANCH")); assertEquals(99,remaining.size)
            for(row in remaining) web("finance/expenses/${row.id}")
            assertTrue(repo.getExpenses(env("NATIVE_TEST_BRANCH")).isEmpty()); assertEquals(before,dao.getAllInvoicesList())
            i.branchId=env("NATIVE_TEST_FOREIGN_BRANCH")
            try { repo.getExpenses(env("NATIVE_TEST_FOREIGN_BRANCH")); fail("Foreign branch") } catch(e:ExpenseHttpException) { assertEquals(404,e.code) }
            i.setSession(env("NATIVE_TEST_PARENT_TOKEN"))
            try { repo.getExpenses(); fail("Parent staff ledger") } catch(e:ExpenseHttpException) { assertEquals(403,e.code) }
            assertEquals(before,dao.getAllInvoicesList())
        } finally { ApiClientProvider.createService(oldBase); i.setSession(token,branch=branch,workspace=workspace) }
    }
}
