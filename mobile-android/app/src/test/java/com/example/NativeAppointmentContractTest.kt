package com.example

import com.example.data.remote.*
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeAppointmentContractTest {
    @Test fun lostParentBookingResponseIsRecoveredByReadingRealRowsAndCancellationUsesCanonicalPatch() = runBlocking {
        val student = UUID(92, 1).toString()
        val teacher = UUID(92, 2).toString()
        val parent = UUID(92, 3).toString()
        val branch = UUID(92, 4).toString()
        val requestId = UUID(92, 5)
        val availabilityId = UUID(92, 6).toString()
        val requests = CopyOnWriteArrayList<String>()
        val submitted = CopyOnWriteArrayList<JSONObject>()
        var status = "PENDING"
        val storedNote = "Recorded teacher note"
        var note: Any = storedNote
        val edits = CopyOnWriteArrayList<JSONObject>()
        var availability = false
        var deleted = false
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val token = auth.authToken
        val tenant = auth.tenantId
        val originalBranch = auth.branchId
        val workspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { request ->
            try {
                val path = request.requestURI.toString()
                requests += "${request.requestMethod} $path"
                val data = when {
                    request.requestMethod == "POST" && path == "/api/appointments/slots" -> {
                        submitted += JSONObject(request.requestBody.readBytes().toString(Charsets.UTF_8))
                        availability = true
                        JSONObject().put("id", availabilityId)
                    }
                    request.requestMethod == "DELETE" && path == "/api/appointments/slots/$availabilityId" -> {
                        availability = false
                        JSONObject()
                    }
                    request.requestMethod == "DELETE" && path == "/api/appointments/$requestId" -> {
                        deleted = true
                        JSONObject.NULL
                    }
                    path == "/api/appointments/slots?date=2027-01-04" ->
                        JSONArray().also { rows ->
                            if (availability) rows.put(JSONObject().put("id", availabilityId).put("teacher_id", teacher)
                                .put("date", "2027-01-04").put("start_time", "15:00").put("end_time", "15:30")
                                .put("is_available", true))
                        }
                    request.requestMethod == "POST" -> {
                        submitted += JSONObject(request.requestBody.readBytes().toString(Charsets.UTF_8))
                        return@createContext
                    }
                    request.requestMethod == "PATCH" -> {
                        val body = JSONObject(request.requestBody.readBytes().toString(Charsets.UTF_8))
                        edits += body
                        status = body.getString("status")
                        if (body.has("feedback_notes")) note = body.get("feedback_notes")
                        JSONObject().put("id", requestId.toString()).put("status", status)
                    }
                    path.startsWith("/api/appointments/options") -> {
                        val item = if (path.contains("student_id=")) AppointmentChoice(teacher, "Recorded teacher")
                            else AppointmentChoice(student, "Recorded student")
                        JSONObject().put("page", 1).put("total_pages", 1)
                            .put("items", JSONArray().put(JSONObject().put("id", item.id).put("name", item.name)))
                    }
                    path.contains("/teacher-slots/") -> JSONArray().put(JSONObject().put("teacher_id", teacher)
                        .put("date", "2027-01-04").put("start_time", "14:00").put("end_time", "14:30")
                        .put("duration_minutes", 30))
                    deleted -> JSONArray()
                    else -> JSONArray().put(JSONObject().put("id", requestId.toString()).put("teacher_id", teacher)
                        .put("teacher_name", "Recorded teacher").put("parent_id", parent).put("student_id", student)
                        .put("student_name", "Recorded student").put("appointment_date", "2027-01-04")
                        .put("appointment_time", "14:00").put("status", status).put("notes", "Recorded note")
                        .put("feedback_notes", note).put("duration_minutes", 30))
                }
                val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
                request.sendResponseHeaders(200, bytes.size.toLong())
                request.responseBody.use { it.write(bytes) }
            } finally {
                request.close()
            }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        auth.setSession("synthetic-appointment")
        try {
            val api = NativeAppointmentClient(branch)
            val child = api.options().single()
            val instructor = api.options(child.id).single()
            val slot = api.slots(instructor.id, "2027-01-04").single()
            val draft = AppointmentDraft(instructor, child, "2027-01-04", slot.start, "Recorded note", slot.duration, requestId)
            try {
                api.create(draft, fromParent = true)
                fail("Lost response")
            } catch (_: java.io.IOException) {}
            val row = api.records().single()
            assertTrue(draft.matches(row))
            assertFalse(draft.copy(duration = 45).matches(row))
            assertEquals("", row.parent_phone)
            assertNull(row.meeting_link)
            val decision = AppointmentDecision(row.id, "CANCELLED", null)
            api.decide(decision)
            assertTrue(decision.matches(api.records().single()))
            assertFalse(edits.single().has("feedback_notes"))
            assertEquals(storedNote, api.records().single().teacher_feedback_notes)
            val clear = AppointmentDecision(row.id, "COMPLETED", "")
            api.decide(clear)
            assertTrue(edits.last().has("feedback_notes"))
            assertTrue(edits.last().isNull("feedback_notes"))
            assertTrue(clear.matches(api.records().single()))
            assertEquals("2027-01-04T14:00", submitted.single().getString("slot_datetime"))
            assertEquals(requestId.toString(), submitted.single().getString("request_id"))
            assertEquals(1, requests.count { it == "POST /api/appointments/request" })
            assertTrue(requests.contains("PATCH /api/appointments/$requestId/status"))
            assertTrue(requests.contains("GET /api/appointments?page=1&page_size=100&branch_id=$branch"))
            assertTrue(api.managedSlots(teacher, "2027-01-04").isEmpty())
            api.createSlot(teacher, "2027-01-04", "15:00", "15:30")
            assertEquals("15:30", submitted.last().getString("end_time"))
            assertEquals(ManagedAppointmentSlot(availabilityId, "15:00", "15:30", true),
                api.managedSlots(teacher, "2027-01-04").single())
            api.deleteSlot(availabilityId)
            assertTrue(api.managedSlots(teacher, "2027-01-04").isEmpty())
            api.delete(requestId.toString())
            assertTrue(api.records().isEmpty())
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            auth.setSession(token, tenant, originalBranch, workspace)
        }
    }
}
