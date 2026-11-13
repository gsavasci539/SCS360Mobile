package com.example

import com.example.data.remote.*
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36],manifest=Config.NONE)
class NativeExpenseEditClientTest {
    private fun id(n:Int)=UUID(11,n.toLong()).toString()
    private fun row()=JSONObject().put("id",id(1)).put("organization_id",id(2)).put("branch_id",id(3)).put("created_by",id(4))
        .put("title","Server expense").put("description","Server expense").put("category","Supplies").put("amount","100.01")
        .put("date","2027-01-15").put("expense_date","2027-01-15T12:34:56.123456+00:00").put("notes","Stored ref")
    private suspend fun server(handler:(com.sun.net.httpserver.HttpExchange)->Unit,work:suspend()->Unit) {
        val i=ApiClientProvider.interceptor; val base=ApiClientProvider.activeBaseUrl; val token=i.authToken;val branch=i.branchId;val workspace=i.workspaceOrganizationId
        val s=HttpServer.create(InetSocketAddress("127.0.0.1",0),0);s.createContext("/",handler);s.start()
        ApiClientProvider.createService("http://127.0.0.1:${s.address.port}/api/");i.setSession("synthetic-edit",branch=id(3),workspace=id(2))
        try { work() } finally { s.stop(0);ApiClientProvider.createService(base);i.setSession(token,branch=branch,workspace=workspace) }
    }
    private fun reply(e:com.sun.net.httpserver.HttpExchange,row:JSONObject,code:Int=200) {
        val bytes=JSONObject().put("success",code==200).put("data",row).toString().toByteArray();e.responseHeaders.add("Content-Type","application/json");e.sendResponseHeaders(code,bytes.size.toLong());e.responseBody.use { it.write(bytes) }
    }
    @Test fun exactDeltaPreservesCentsNotesAndOriginalInstantUntilDateActuallyChanges()=runBlocking {
        var current=row();val bodies=mutableListOf<JSONObject>()
        server({ e ->
            assertEquals("/api/finance/expenses/${id(1)}",e.requestURI.path)
            if(e.requestMethod=="PUT") { val b=JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8));bodies.add(b)
                for(k in b.keys()) { current.put(k,b.get(k));if(k=="description")current.put("title",b.get(k));if(k=="expense_date")current.put("date","2027-01-16") }
            };reply(e,current)
        }) {
            val session=NativeExpenseEditSession(id(1),id(2));val original=session.load();val draft=original.draft()
            assertEquals("100.01",draft.amount);assertEquals("Stored ref",draft.notes)
            for(invalid in listOf(draft,draft.copy(amount="100.001"),draft.copy(title=" "),draft.copy(date="2027-02-30"))) {
                try { session.save(invalid);fail("Invalid/no changes") } catch(_:Exception) { }
            };assertTrue(bodies.isEmpty())
            val changed=session.save(draft.copy(amount="110,02",notes="New ref"));assertEquals(110.02,changed.expense.amount,0.0);assertEquals(original.instant,changed.instant)
            assertEquals(setOf("amount","notes"),bodies.single().keys().asSequence().toSet());assertEquals("110.02",bodies.single().getString("amount"))
            session.save(draft);assertEquals(1,bodies.size)
            val second=NativeExpenseEditSession(id(1),id(2));val fresh=second.load().draft()
            second.save(fresh.copy(title="Edited",category="Other",date="2027-01-16",notes=""))
            assertEquals(setOf("description","category","expense_date","notes"),bodies.last().keys().asSequence().toSet())
            assertEquals("2027-01-16T00:00:00+03:00",bodies.last().getString("expense_date"));assertTrue(bodies.last().isNull("notes"))
        }
    }
    @Test fun committedMissingAckKeepsExactPatchAndExplicitReadRecoversWithoutFalseSuccess()=runBlocking {
        var current=row();val bodies=mutableListOf<String>();var badAck=false
        server({ e ->
            if(e.requestMethod=="PUT") { val b=e.requestBody.readBytes().toString(Charsets.UTF_8);bodies.add(b);current.put("amount",JSONObject(b).getString("amount"))
                if(bodies.size==1) { reply(e,current,500);return@server }
                reply(e,if(badAck) JSONObject(current.toString()).put("id",id(99)) else current)
            } else reply(e,current)
        }) {
            val session=NativeExpenseEditSession(id(1),id(2));val draft=session.load().draft().copy(amount="110.02")
            try { session.save(draft);fail("Missing ACK") } catch(e:ExpenseHttpException) { assertEquals(500,e.code) };assertTrue(session.hasPending)
            try { session.save(draft.copy(amount="120.02"));fail("Changed pending") } catch(_:IllegalStateException) { };assertEquals(1,bodies.size)
            badAck=true;try { session.save(draft);fail("Wrong row ACK") } catch(_:IllegalStateException) { };assertTrue(session.hasPending)
            assertEquals(bodies[0],bodies[1]);val fresh=session.load();assertFalse(session.hasPending);assertEquals(110.02,fresh.expense.amount,0.0);assertEquals(2,bodies.size)
        }
    }
    @Test fun deniedMalformedScopeAndDefinitiveValidationNeverBecomeAnAcknowledgedEdit()=runBlocking {
        var status=200;var foreign=false;var stale=false;var puts=0
        server({ e ->
            if(stale)ApiClientProvider.interceptor.workspaceOrganizationId=id(700)
            if(e.requestMethod=="PUT")puts++
            reply(e,if(foreign)row().put("organization_id",id(700)) else row(),status)
        }) {
            foreign=true;try { NativeExpenseEditSession(id(1),id(2)).load();fail("Foreign") } catch(_:IllegalStateException) { };assertEquals(0,puts)
            foreign=false;status=404;try { NativeExpenseEditSession(id(1),id(2)).load();fail("Deleted") } catch(e:ExpenseHttpException) { assertEquals(404,e.code) }
            status=200;val session=NativeExpenseEditSession(id(1),id(2));val draft=session.load().draft();status=422
            try { session.save(draft.copy(amount="110.02"));fail("Validation") } catch(e:ExpenseHttpException) { assertEquals(422,e.code) };assertFalse(session.hasPending)
            status=200;stale=true;try { session.save(draft.copy(amount="120.02"));fail("Stale") } catch(_:kotlinx.coroutines.CancellationException) { }
        }
    }
}
