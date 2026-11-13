package com.example

import com.example.data.remote.*
import com.example.data.model.Expense
import com.example.data.repository.Okul360Repository
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36],manifest=Config.NONE)
class NativeExpenseClientTest {
    private fun id(n:Int)=UUID(8,n.toLong()).toString()
    private fun row(n:Int=0)=JSONObject().put("id",id(n)).put("organization_id",id(200)).put("branch_id",id(201))
        .put("title","Synthetic expense").put("category","Synthetic category").put("amount","100.01").put("date","2027-01-15").put("created_by",id(202))
    private fun reply(e:HttpExchange,data:JSONObject,code:Int=200) {
        val bytes=JSONObject().put("success",code<400).put("data",data).toString().toByteArray()
        e.responseHeaders.add("Content-Type","application/json"); e.sendResponseHeaders(code,bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
    }
    private suspend fun server(handler:(HttpExchange)->Unit,work:suspend()->Unit) {
        val oldBase=ApiClientProvider.activeBaseUrl; val i=ApiClientProvider.interceptor
        val token=i.authToken; val branch=i.branchId; val workspace=i.workspaceOrganizationId
        val s=HttpServer.create(InetSocketAddress("127.0.0.1",0),0); s.createContext("/",handler); s.start()
        ApiClientProvider.createService("http://127.0.0.1:${s.address.port}/api/"); i.setSession("synthetic-expense",workspace=id(200))
        try { work() } finally { s.stop(0); ApiClientProvider.createService(oldBase); i.setSession(token,branch=branch,workspace=workspace) }
    }
    @Test fun durableExpenseIdentitySurvivesLostAckAndLegacyWritesCannotInventRows()=runBlocking {
        val bodies=mutableListOf<String>(); var empty=false
        server({ e ->
            if(e.requestURI.path.endsWith("branch-options")) reply(e,JSONObject().put("total",1).put("allows_organization_wide",true)
                .put("items",JSONArray().put(JSONObject().put("id",id(201)).put("name","Synthetic Branch"))))
            else if(e.requestMethod=="POST") {
                bodies.add(e.requestBody.readBytes().toString(Charsets.UTF_8)); if(bodies.size==1) { e.close(); return@server }
                reply(e,row().put("id",JSONObject(bodies.last()).getString("request_id")))
            } else reply(e,JSONObject().put("total",if(empty) 0 else 1).put("items",if(empty) JSONArray() else JSONArray().put(row())))
        }) {
            val context=RuntimeEnvironment.getApplication(); val actor=id(202)
            val first=NativeExpenseCreateSession(context,actor); assertEquals(1,first.load().size)
            val draft=ExpenseDraft(id(201),"Synthetic expense","Synthetic category","100,01","2027-01-15")
            try { first.create(draft.copy(amount="100.001")); fail("Invalid cents") } catch(_:IllegalArgumentException) { }
            assertEquals(0,bodies.size)
            try { first.create(draft); fail("Lost committed ACK") } catch(_:java.io.IOException) { }
            val restored=NativeExpenseCreateSession(context,actor); restored.load(); assertEquals("100.01",restored.pendingDraft()!!.amount)
            try { restored.create(draft.copy(title="Changed pending")); fail("Changed pending body") } catch(_:IllegalStateException) { }
            val created=restored.create(restored.pendingDraft()!!); assertEquals(created,restored.create(draft))
            assertEquals(bodies[0],bodies[1]); assertEquals(setOf("request_id","branch_id","title","category","amount","date"),JSONObject(bodies[0]).keys().asSequence().toSet())
            restored.finish()
            val next=NativeExpenseCreateSession(context,actor); next.load(); val second=next.create(draft)
            restored.finish(); val recovered=NativeExpenseCreateSession(context,actor); recovered.load(); assertTrue(recovered.hasPending)
            assertEquals(second.id,recovered.create(recovered.pendingDraft()!!).id); recovered.finish()
            context.getSharedPreferences("scs360_api_connection",0).edit().remove("base_url").commit()
            val repo=Okul360Repository(context); assertEquals(1,repo.getExpenses().size)
            empty=true; assertTrue(repo.getExpenses().isEmpty())
            try { repo.createExpense(Expense()); fail("Legacy fake write") } catch(_:IllegalStateException) { }
            assertTrue(repo.getExpenses().isEmpty())
        }
    }
    @Test fun notesRecoverWithExactIdentityRejectWrongAckAndPreserveLegacySixFieldRequests()=runBlocking {
        val bodies=mutableListOf<String>();var wrongNote=true
        server({ e ->
            if(e.requestURI.path.endsWith("branch-options")) reply(e,JSONObject().put("total",1).put("allows_organization_wide",true)
                .put("items",JSONArray().put(JSONObject().put("id",id(201)).put("name","Synthetic Branch"))))
            else {
                val raw=e.requestBody.readBytes().toString(Charsets.UTF_8);bodies.add(raw);val body=JSONObject(raw)
                val note=if(body.has("receipt_url"))body.getString("receipt_url") else null
                reply(e,row().put("id",body.getString("request_id")).put("notes",if(wrongNote && note!=null) "Wrong note" else note ?: JSONObject.NULL).put("status","pending"))
            }
        }) {
            val context=RuntimeEnvironment.getApplication();val actor=id(202);val first=NativeExpenseCreateSession(context,actor);first.load()
            val draft=ExpenseDraft(id(201),"Synthetic expense","Synthetic category","100.01","2027-01-15","Creation ref")
            try { first.create(draft.copy(notes="x".repeat(2001)));fail("Long note") } catch(_:IllegalArgumentException) { };assertTrue(bodies.isEmpty())
            try { first.create(draft);fail("Mismatched note ACK") } catch(_:IllegalStateException) { };assertTrue(first.hasPending)
            val restored=NativeExpenseCreateSession(context,actor);restored.load();assertEquals("Creation ref",restored.pendingDraft()!!.notes)
            try { restored.create(draft.copy(notes="Changed pending"));fail("Changed note") } catch(_:IllegalStateException) { };assertEquals(1,bodies.size)
            wrongNote=false;val result=restored.create(restored.pendingDraft()!!);assertEquals("Creation ref",result.notes);assertEquals("pending",result.status);assertNull(result.receipt_url)
            assertEquals(bodies[0],bodies[1]);assertEquals(setOf("request_id","branch_id","title","category","amount","date","receipt_url"),JSONObject(bodies[0]).keys().asSequence().toSet());restored.finish()
            val api=NativeExpenseClient();val key=java.security.MessageDigest.getInstance("SHA-256").digest("${api.base}|$actor|${api.workspace}|${api.branch}".toByteArray()).joinToString("") { "%02x".format(it) }
            val legacy=JSONObject().put("branch_id",id(201)).put("title",draft.title).put("category",draft.category).put("amount","100.01").put("date",draft.date).put("request_id",id(300)).toString()
            context.getSharedPreferences("expense_create_requests_v1",0).edit().putString(key,legacy).commit()
            val upgraded=NativeExpenseCreateSession(context,actor);upgraded.load();assertNull(upgraded.pendingDraft()!!.notes)
            val old=upgraded.create(upgraded.pendingDraft()!!);assertEquals(id(300),old.id);assertNull(old.notes);assertEquals(legacy,bodies.last());upgraded.finish()
            assertNull(api.parse(row()).status);assertEquals("external-raw",api.parse(row().put("status","external-raw")).status)
        }
    }
    @Test fun fullExpensePagingRejectsDuplicatePagesAndStaleActors()=runBlocking {
        val paths=mutableListOf<String>(); var duplicate=false; var deletion=false
        server({ e ->
            paths.add(e.requestURI.toString())
            if(e.requestMethod=="DELETE") reply(e,JSONObject(),if(deletion) 200 else 422)
            else { val second="page=2&" in e.requestURI.rawQuery.orEmpty()
                reply(e,JSONObject().put("total",101).put("items",JSONArray().apply { for(n in if(second) 100..100 else 0..99) put(row(if(second && duplicate) 0 else n)) })) }
        }) {
            ApiClientProvider.interceptor.branchId=id(201)
            val api=NativeExpenseClient(); val rows=api.load(id(201)); assertEquals(101,rows.size)
            assertEquals(101,rows.map { it.id }.distinct().size); assertTrue(paths.all { "branch_id=${id(201)}" in it })
            assertEquals("Synthetic category",rows[0].category); assertEquals("2027-01-15",rows[0].date)
            try { api.delete(id(0)); fail("Rejected delete") } catch(e:ExpenseHttpException) { assertEquals(422,e.code) }
            deletion=true; api.delete(id(0)); duplicate=true
            try { api.load(id(201)); fail("Duplicate pages") } catch(_:IllegalStateException) { }
            val before=paths.size; ApiClientProvider.interceptor.setSession("new-actor")
            try { api.load(id(201)); fail("Stale actor") } catch(_:kotlinx.coroutines.CancellationException) { }
            assertEquals(before,paths.size)
        }
    }
}
