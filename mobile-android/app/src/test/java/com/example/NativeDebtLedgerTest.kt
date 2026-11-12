package com.example

import com.example.data.remote.*
import com.example.data.repository.Okul360Repository
import com.example.data.local.Okul360Database
import com.example.data.local.InvoiceEntity
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeDebtLedgerTest {
    private fun id(i: Int) = UUID(4, i.toLong()).toString()
    private fun row(i: Int = 0, amount: String = "100.01") = JSONObject().put("id", id(i))
        .put("organization_id", id(200)).put("branch_id", id(201)).put("student_id", id(202))
        .put("student_name", "Synthetic Student").put("class_name", "Synthetic Class").put("debt_type", "EDUCATION")
        .put("description", "Ledger note").put("amount", amount).put("paid_amount", "0.00").put("remaining_amount", amount)
        .put("status", "UNPAID").put("due_date", "2027-01-15").put("created_at", "2026-11-12T10:00:00+00:00")
    private fun reply(e: HttpExchange, data: JSONObject, code: Int = 200) {
        val bytes = JSONObject().put("success", code < 400).put("data", data).toString().toByteArray()
        e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(code, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
    }
    private suspend fun server(handler: (HttpExchange) -> Unit, work: suspend () -> Unit) {
        val oldBase = ApiClientProvider.activeBaseUrl; val i = ApiClientProvider.interceptor
        val token = i.authToken; val branch = i.branchId; val workspace = i.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0); server.createContext("/", handler); server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        i.setSession("synthetic-ledger", branch=id(201), workspace=id(200))
        try { work() } finally { server.stop(0); ApiClientProvider.createService(oldBase); i.setSession(token, branch=branch, workspace=workspace) }
    }

    @Test fun bothNativeListsReadAllPagesAndRejectDuplicateOrStaleScope() = runBlocking {
        val queries = mutableListOf<String>(); var duplicate = false
        server({ e ->
            queries.add(e.requestURI.rawQuery.orEmpty()); val second = "page=2&" in e.requestURI.rawQuery.orEmpty()
            reply(e, JSONObject().put("total",101).put("items", JSONArray().apply {
                for (n in if (second) 100..100 else 0..99) put(row(if (second && duplicate) 0 else n)) }))
        }) {
            val context = RuntimeEnvironment.getApplication()
            context.getSharedPreferences("scs360_api_connection",0).edit().remove("base_url").commit()
            val repo = Okul360Repository(context); val dao = Okul360Database.getDatabase(context).invoiceDao()
            val before = dao.getAllInvoicesList()
            val invoices = repo.getInvoices(id(201),"PENDING"); assertEquals(101,invoices.size)
            assertEquals("",invoices[0].invoice_number); assertEquals("2026-11-12",invoices[0].issue_date)
            assertEquals("Synthetic Student",invoices[0].student_name); assertEquals(100.01,invoices[0].amount,0.0)
            val debts = repo.getDebts(id(202)); assertEquals(101,debts.size); assertEquals("UNPAID",debts[0].status)
            assertEquals(4,queries.size); assertTrue(queries.all { "branch_id=${id(201)}" in it })
            assertTrue(queries.takeLast(2).all { "student_id=${id(202)}" in it }); assertEquals(before,dao.getAllInvoicesList())
            duplicate = true
            try { repo.getInvoices(id(201),null); fail("Duplicate pages cannot become a partial ledger") } catch (_: IllegalStateException) { }
            val stale = NativeDebtLedgerClient(); val beforeStale = queries.size
            ApiClientProvider.interceptor.setSession("different-actor")
            try { stale.debts(); fail("Stale identity") } catch (_: kotlinx.coroutines.CancellationException) { }
            assertEquals(beforeStale,queries.size)
        }
    }

    @Test fun editUsesWhitelistAndFailedMutationsNeverTouchRoom() = runBlocking {
        var allowed = false; var recorded = row(); val bodies = mutableListOf<JSONObject>(); var deletes = 0
        server({ e ->
            when(e.requestMethod) {
                "GET" -> reply(e,recorded)
                "PUT" -> { val body=JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8)); bodies.add(body)
                    if (allowed) { recorded.put("amount",body.getString("amount")).put("remaining_amount",body.getString("amount"))
                        .put("due_date",body.getString("due_date")).put("description",body.getString("description"))
                        reply(e,JSONObject().put("id",id(0))) }
                    else reply(e,JSONObject(),422) }
                "DELETE" -> { deletes++; reply(e,JSONObject(),if (allowed) 200 else 422) }
            }
        }) {
            val context = RuntimeEnvironment.getApplication()
            context.getSharedPreferences("scs360_api_connection",0).edit().remove("base_url").commit()
            val dao = Okul360Database.getDatabase(context).invoiceDao()
            dao.insertInvoice(InvoiceEntity(id(0),"",id(202),"Synthetic","",100.01,0.0,"2027-01-15","2026-11-12","PENDING","Ledger note"))
            val before=dao.getAllInvoicesList(); val repo=Okul360Repository(context)
            val draft=com.example.data.model.Invoice(id=id(0),student_id=id(202),amount=110.02,paid_amount=0.0,status="PENDING",due_date="2027-01-16",description="Updated note")
            try { repo.updateInvoice(draft); fail("Rejected update") } catch (e: DebtLedgerHttpException) { assertEquals(422,e.code) }
            try { repo.deleteInvoice(id(0)); fail("Rejected delete") } catch (e: DebtLedgerHttpException) { assertEquals(422,e.code) }
            assertEquals(before,dao.getAllInvoicesList())
            allowed=true; val updated=repo.updateInvoice(draft); assertEquals(110.02,updated.amount,0.0)
            assertEquals("2027-01-16",updated.due_date); assertEquals("Updated note",updated.description)
            assertTrue(repo.deleteInvoice(id(0))); assertEquals(2,deletes); assertEquals(before,dao.getAllInvoicesList())
            assertTrue(bodies.all { it.keys().asSequence().toSet()==setOf("amount","due_date","description") })
            assertTrue(bodies.all { it.getString("amount")=="110.02" })
        }
    }
}
