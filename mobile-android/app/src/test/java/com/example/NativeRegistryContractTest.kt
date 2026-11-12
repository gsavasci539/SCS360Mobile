package com.example

import com.example.data.model.*
import com.example.data.remote.*
import com.sun.net.httpserver.HttpExchange
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeRegistryContractTest {
    private val org = UUID(1, 1).toString(); private val branch = UUID(2, 2).toString(); private val classroom = UUID(3, 3).toString()
    private fun id(i: Int) = UUID(4, i.toLong()).toString()
    private fun row(i: Int) = JSONObject().put("id", id(i)).put("organization_id", org).put("branch_id", branch)
        .put("class_id", classroom).put("first_name", "Synthetic$i").put("last_name", "Student").put("student_number", JSONObject.NULL)
        .put("birth_date", JSONObject.NULL).put("gender", JSONObject.NULL).put("enrollment_date", "2026-11-12").put("status", "ACTIVE")
    private fun reply(e: HttpExchange, value: Any, code: Int = 200, success: Boolean = true) {
        val bytes = JSONObject().put("success", success).put("data", value).toString().toByteArray()
        e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(code, bytes.size.toLong())
        e.responseBody.use { it.write(bytes) }
    }
    private suspend fun server(handler: (HttpExchange) -> Unit, work: suspend () -> Unit) {
        val oldBase = ApiClientProvider.activeBaseUrl; val oldToken = ApiClientProvider.interceptor.authToken
        val oldWorkspace = ApiClientProvider.interceptor.workspaceOrganizationId
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0); s.createContext("/", handler); s.start()
        ApiClientProvider.createService("http://127.0.0.1:${s.address.port}/api/"); ApiClientProvider.interceptor.setSession("synthetic-registry-token")
        try { work() } finally { s.stop(0); ApiClientProvider.createService(oldBase); ApiClientProvider.interceptor.setSession(oldToken, workspace = oldWorkspace) }
    }
    @Test fun completePagedRosterPreservesUnknownFieldsAndEscapesSearch() = runBlocking {
        val queries = mutableListOf<String>()
        server({ e ->
            queries.add(e.requestURI.rawQuery)
            val second = "page=2&" in e.requestURI.rawQuery
            val items = JSONArray().apply { for (i in if (second) 100..100 else 0..99) put(row(i)) }
            reply(e, JSONObject().put("total", 101).put("items", items))
        }) {
            val rows = NativeRegistryClient().students(branch, classroom, "A& ?")
            assertEquals(101, rows.size); assertEquals(2, queries.size)
            assertTrue(queries.all { "search=A%26+%3F" in it && "page_size=100" in it })
            assertTrue(rows.all { it.student_number.isBlank() && it.gender.isBlank() && it.birth_date.isBlank() &&
                it.address == null && it.blood_type == null && it.parent_name.isBlank() && it.parent_phone.isBlank() && it.today_attendance_status == null })
        }
    }
    @Test fun patchesOnlyChangedFieldsIncludingExplicitNullWithoutReplacingHealth() = runBlocking {
        var patch: JSONObject? = null
        val original = row(0).put("notes", "Before").put("health_notes", "Private synthetic history")
        server({ e ->
            if (e.requestMethod == "PUT") {
                patch = JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8))
                reply(e, row(0).put("last_name", "Edited").put("class_id", JSONObject.NULL).put("health_notes", "Private synthetic history"))
            } else if (e.requestURI.path.endsWith("/form")) reply(e, original)
            else reply(e, JSONObject().put("total", 1).put("items", JSONArray().put(original)))
        }) {
            val client = NativeRegistryClient(); val before = client.students(null, null, null).single()
            val after = client.updateStudent(before.copy(last_name = "Edited", class_id = "", notes = null))
            assertEquals(setOf("last_name", "class_id", "notes"), patch!!.keys().asSequence().toSet())
            assertTrue(patch!!.isNull("class_id") && patch!!.isNull("notes")); assertEquals("Private synthetic history", after.health_notes)
            try { client.updateStudent(before.copy(tuition_balance = 50.0)); fail("Unrelated mutation must be rejected") }
            catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun failedCreateIsNotRetriedAndNegativeSuccessEnvelopeIsRejected() = runBlocking {
        var posts = 0; var payload: JSONObject? = null
        server({ e -> when {
            e.requestMethod == "POST" -> { posts++; payload = JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8)); reply(e, JSONObject(), 503) }
            e.requestMethod == "DELETE" -> reply(e, JSONObject.NULL, success = false)
            else -> reply(e, JSONObject().put("id", branch).put("organization_id", org).put("name", "Synthetic branch").put("is_active", true))
        } }) {
            val client = NativeRegistryClient()
            try { client.createStudent(Student(organization_id = org, branch_id = branch, first_name = "Real", last_name = "Draft")); fail("503 must fail") }
            catch (failure: RegistryHttpException) { assertEquals(503, failure.code) }
            assertEquals(1, posts); assertFalse(payload!!.has("id")); assertEquals(org, payload!!.getString("organization_id"))
            assertTrue(payload!!.isNull("student_number") && payload!!.isNull("birth_date") && payload!!.isNull("gender"))
            assertFalse(payload!!.has("blood_type") || payload!!.has("parent_phone") || payload!!.has("today_attendance_status"))
            try { client.deleteStudent(id(0)); fail("Negative envelope must fail") } catch (_: IllegalStateException) { }
        }
    }
    @Test fun changedRemoteRecordsCannotBeOverwrittenByAnOlderLoadedDraft() = runBlocking {
        var puts = 0
        val person = row(0)
        val room = JSONObject().put("id", classroom).put("organization_id", org).put("branch_id", branch)
            .put("name", "Synthetic classroom").put("capacity", 20).put("is_active", true).put("code", "Before")
        server({ e ->
            if (e.requestMethod == "PUT") { puts++; reply(e, JSONObject(), 500) }
            else when {
                e.requestURI.path.endsWith("/form") -> reply(e, person)
                e.requestURI.path.endsWith("/$classroom") -> reply(e, room)
                e.requestURI.path.endsWith("/classes") -> reply(e, JSONObject().put("total", 1).put("items", JSONArray().put(room)))
                else -> reply(e, JSONObject().put("total", 1).put("items", JSONArray().put(person)))
            }
        }) {
            val client = NativeRegistryClient(); val oldStudent = client.students(null, null, null).single(); val oldClass = client.classes(null).single()
            person.put("notes", "External change"); room.put("code", "External")
            try { client.updateStudent(oldStudent.copy(last_name = "My edit")); fail("Stale student must fail") } catch (_: RegistryRecordChangedException) { }
            try { client.updateClass(oldClass.copy(name = "My edit")); fail("Stale class must fail") } catch (_: RegistryRecordChangedException) { }
            assertEquals(0, puts)
        }
    }
    @Test fun occupiedClassIsNotDeletedEvenWhenLocalRosterIsAbsent() = runBlocking {
        var deletes = 0
        server({ e ->
            if (e.requestMethod == "DELETE") deletes++
            reply(e, JSONObject().put("student_count", 1))
        }) {
            try { NativeRegistryClient().deleteClass(classroom); fail("Occupied class must fail") } catch (_: IllegalArgumentException) { }
            assertEquals(0, deletes)
        }
    }
    @Test fun staleActorCannotDispatchARegistryOperationAndIncompletePagesFail() = runBlocking {
        var received = 0
        server({ e -> received++; reply(e, JSONObject().put("total", 2).put("items", JSONArray())) }) {
            val stale = NativeRegistryClient(); ApiClientProvider.interceptor.setSession("another-synthetic-actor")
            try { stale.students(null, null, null); fail("Stale scope must fail") } catch (_: kotlinx.coroutines.CancellationException) { }
            assertEquals(0, received)
            try { NativeRegistryClient().students(null, null, null); fail("Partial list must fail") } catch (_: IllegalStateException) { }
            assertEquals(1, received)
        }
    }
}
