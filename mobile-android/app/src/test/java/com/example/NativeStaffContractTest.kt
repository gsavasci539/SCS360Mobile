package com.example

import com.example.data.model.*
import com.example.data.remote.*
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeStaffContractTest {
    private fun id(n: Int) = UUID(91, n.toLong()).toString()
    private data class Reply(val code: Int = 200, val value: Any = JSONObject.NULL)
    private fun leave(status: String = "PENDING", actor: String = id(1)) = JSONObject()
        .put("id", id(2)).put("staff_id", actor).put("staff_name", "Server Actor")
        .put("leave_type", "Annual").put("start_date", "2027-01-01").put("end_date", "2027-01-03")
        .put("days_count", 3).put("reason", "Recorded reason").put("status", status).put("admin_comment", "")
    private suspend fun server(handler: (com.sun.net.httpserver.HttpExchange) -> Reply, work: suspend () -> Unit) {
        val base = ApiClientProvider.activeBaseUrl; val auth = ApiClientProvider.interceptor
        val token = auth.authToken; val tenant = auth.tenantId; val branch = auth.branchId; val workspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e -> try {
            val reply = handler(e)
            if (reply.code > 0) {
                val bytes = JSONObject().put("success", reply.code < 400).put("data", reply.value).toString().toByteArray(Charsets.UTF_8)
                e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(reply.code, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
            }
        } finally { e.close() } }
        server.start(); ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/"); auth.setSession("synthetic-library")
        try { work() } finally { server.stop(0); ApiClientProvider.createService(base); auth.setSession(token, tenant, branch, workspace) }
    }

    @Test fun canonicalLeaveAndReviewRequireServerReadback() = runBlocking {
        var status = "PENDING"; val writes = mutableListOf<Pair<String, JSONObject>>()
        server({ e ->
            if(e.requestMethod != "GET") {
                val body=JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8))
                writes.add(e.requestMethod + " " + e.requestURI.path to body)
                if(e.requestMethod == "PUT") status=body.getString("status")
            }
            Reply(value=if(e.requestMethod == "GET") JSONArray().put(leave(status)) else leave(status))
        }) {
            val api=NativeStaffClient()
            val saved=api.requestLeave(User(id=id(1)), "Annual", "2027-01-01", "2027-01-03", "Recorded reason")
            assertEquals("Server Actor", saved.teacher_name); assertEquals(3, saved.total_days)
            assertEquals("APPROVED", api.reviewLeave(saved.id,true,"").status)
            assertEquals(setOf("staff_id","staff_name","leave_type","start_date","end_date","days_count","reason"), writes[0].second.keys().asSequence().toSet())
            assertEquals(3,writes[0].second.getInt("days_count"))
            assertEquals("PUT /api/staff/leave-requests/${id(2)}/review",writes[1].first)
        }
    }
    @Test fun wrongReadbackCannotAcknowledgeLeave() = runBlocking {
        server({ e -> Reply(value=if(e.requestMethod == "GET") JSONArray().put(leave(actor=id(9))) else leave()) }) {
            try { NativeStaffClient().requestLeave(User(id=id(1)),"Annual","2027-01-01","2027-01-03","Recorded reason"); fail("Wrong actor") } catch (_: IllegalStateException) {}
        }
    }
    @Test fun lostAcknowledgementNeverRepeatsWrite() = runBlocking {
        var posts=0
        server({ e -> e.requestBody.readBytes(); posts++; Reply(0) }) {
            try { NativeStaffClient().requestLeave(User(id=id(1)),"Annual","2027-01-01","2027-01-03","Recorded reason"); fail("Lost ACK") } catch (_: java.io.IOException) {}
            assertEquals(1,posts)
        }
    }
    @Test fun invalidDatesAndStaleScopeCannotWrite() = runBlocking {
        var calls=0
        server({ _ -> calls++; Reply(403) }) {
            val api=NativeStaffClient()
            try { api.requestLeave(User(id=id(1)),"Annual","2027-01-03","2027-01-01","Reason"); fail("Dates") } catch (_: IllegalArgumentException) {}
            ApiClientProvider.interceptor.invalidateContext()
            try { api.leaves(); fail("Stale scope") } catch (_: kotlinx.coroutines.CancellationException) {}
            assertEquals(0,calls)
        }
    }
    @Test fun dutyListUsesCanonicalFieldsWithoutInventedDateOrSubject() = runBlocking {
        val duty=JSONObject().put("id",id(3)).put("teacher_id",id(1)).put("teacher_name","Server Actor")
            .put("day_of_week","MONDAY").put("duty_location","GARDEN").put("start_time","08:00").put("end_time","16:00")
        server({ _ -> Reply(value=JSONArray().put(duty)) }) {
            val row=NativeStaffClient().duties().single()
            assertEquals("MONDAY",row.day_name); assertEquals("08:00 - 16:00",row.shift_hours)
            assertEquals("",row.date); assertEquals("",row.teacher_branch); assertFalse(row.is_active_today)
        }
    }
}
