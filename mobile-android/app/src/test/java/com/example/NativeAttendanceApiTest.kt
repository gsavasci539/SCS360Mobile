package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.*
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeAttendanceApiTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val actor = UUID(1, 1).toString()
    private val classroom = UUID(2, 2).toString()
    private val day = "2026-11-10"
    private fun student(i: Int) = UUID(3, i.toLong()).toString()
    private fun roster(count: Int) = JSONArray().apply { repeat(count) { i -> put(JSONObject()
        .put("id", student(i)).put("class_id", classroom).put("first_name", "Synthetic$i").put("last_name", "Student")) } }
    private fun row(i: Int, version: String = "a".repeat(64)) = JSONObject().put("id", UUID(4, i.toLong()).toString())
        .put("student_id", student(i)).put("class_id", classroom).put("attendance_date", day)
        .put("status", if (i == 0) "HALF_DAY" else "ABSENT").put("notes", "Original note").put("version", version)
    private fun reply(e: HttpExchange, value: Any, code: Int = 200) {
        val bytes = JSONObject().put("success", code == 200).put("data", value).toString().toByteArray()
        e.responseHeaders.add("Content-Type", "application/json")
        e.sendResponseHeaders(code, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
    }
    private suspend fun server(handler: (HttpExchange) -> Unit, work: suspend () -> Unit) {
        val base = ApiClientProvider.activeBaseUrl; val token = ApiClientProvider.interceptor.authToken
        val workspace = ApiClientProvider.interceptor.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/", handler); server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        ApiClientProvider.interceptor.setSession("synthetic-attendance-token")
        try { work() } finally { server.stop(0); ApiClientProvider.createService(base); ApiClientProvider.interceptor.setSession(token, workspace = workspace) }
    }
    @Before fun clearNonces() { context.getSharedPreferences("attendance_requests_v1", Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun loadsEveryPageAndLeavesMissingRecordsUnmarkedWithoutInventedMeasurements() = runBlocking {
        val queries = mutableListOf<String>()
        server({ e ->
            if (e.requestURI.path.endsWith("/students")) reply(e, roster(102))
            else {
                val q = e.requestURI.rawQuery; queries.add(q)
                val page = if (q.contains("page=2&")) 2 else 1
                reply(e, JSONObject().put("total", 101).put("items", JSONArray().apply {
                    for (i in if (page == 1) 0..99 else 100..100) put(row(i))
                }))
            }
        }) {
            val editor = NativeAttendanceEditor(context, actor, classroom, day)
            val rows = editor.load()
            assertEquals(102, rows.size); assertTrue(editor.ready)
            assertEquals("HALF_DAY", rows.first { it.student_id == student(0) }.status)
            val missing = rows.first { it.student_id == student(101) }
            assertEquals("UNRECORDED", missing.status); assertEquals("", missing.id)
            assertNull(missing.version); assertNull(missing.check_in_time); assertNull(missing.temperature_celsius); assertNull(missing.parent_notified)
            assertEquals(2, queries.size)
            assertTrue(queries.all { it.contains("attendance_date=$day") && !it.contains("&date=") })
        }
    }

    @Test fun retriesIdenticalBytesAndReconstructsTheOpaqueNonceWithoutPersistingNotes() = runBlocking {
        val bodies = mutableListOf<String>()
        server({ e -> when {
            e.requestURI.path.endsWith("/students") -> reply(e, roster(2))
            e.requestMethod == "GET" -> reply(e, JSONObject().put("total", 1).put("items", JSONArray().put(row(0))))
            else -> {
                val text = e.requestBody.bufferedReader().readText(); bodies.add(text)
                if (bodies.size == 1) reply(e, JSONObject(), 503)
                else {
                    val input = JSONObject(text).getJSONArray("records")
                    reply(e, JSONObject().put("created", 1).put("updated", 1).put("unchanged", 0)
                        .put("items", JSONArray().apply { repeat(input.length()) { i -> put(JSONObject()
                            .put("id", UUID(7, i.toLong()).toString()).put("student_id", input.getJSONObject(i).getString("student_id"))
                            .put("version", "b".repeat(64))) } }))
                }
            }
        } }) {
            val editor = NativeAttendanceEditor(context, actor, classroom, day)
            val original = editor.load()
            val draft = original.map { it.copy(status = if (it.version == null) "EXCUSED" else "LATE", note = "Sensitive synthetic note") }
            try { editor.save(draft); fail("Unavailable response must not succeed") } catch (_: AttendanceHttpException) { }
            assertTrue(editor.hasPending)
            try { editor.save(draft.map { it.copy(note = "Different note") }); fail("Pending body cannot be changed") } catch (_: IllegalStateException) { }
            assertEquals(1, bodies.size)
            val restarted = NativeAttendanceEditor(context, actor, classroom, day)
            restarted.load()
            restarted.save(draft)
            assertEquals(bodies[0], bodies[1]); assertFalse(restarted.hasPending)
            val payload = JSONObject(bodies[0]); assertEquals(day, payload.getString("attendance_date")); UUID.fromString(payload.getString("request_id"))
            val record = payload.getJSONArray("records").getJSONObject(0)
            assertFalse(record.has("temperature_celsius")); assertFalse(record.has("check_in_time")); assertTrue(record.has("notes")); assertTrue(record.has("expected_version"))
            val prefs = context.getSharedPreferences("attendance_requests_v1", Context.MODE_PRIVATE).all
            assertFalse(prefs.toString().contains("Sensitive")); assertFalse(prefs.toString().contains("synthetic-attendance-token"))
            prefs.forEach { (key, value) -> assertEquals(64, key.length); UUID.fromString(value as String) }
        }
    }

    @Test fun conflictRequiresReloadAndAnOldDraftCannotAdoptTheReloadedVersion() = runBlocking {
        var changed = false
        var puts = 0
        server({ e -> when {
            e.requestURI.path.endsWith("/students") -> reply(e, roster(1))
            e.requestMethod == "GET" -> reply(e, JSONObject().put("total", 1).put("items", JSONArray().put(row(0, if (changed) "b".repeat(64) else "a".repeat(64)))))
            else -> { puts++; changed = true; reply(e, JSONObject(), 409) }
        } }) {
            val editor = NativeAttendanceEditor(context, actor, classroom, day)
            val old = editor.load().map { it.copy(status = "LATE") }
            try { editor.save(old); fail("Stale version must fail") } catch (failure: AttendanceHttpException) { assertEquals(409, failure.code) }
            assertFalse(editor.ready); assertFalse(editor.hasPending)
            editor.load()
            try { editor.save(old); fail("A stale draft must not reuse the fresh version") } catch (_: IllegalStateException) { }
            assertEquals(1, puts)
        }
    }

    @Test fun incompletePagesAndInvalidDaysNeverProduceASavableSnapshot() = runBlocking {
        server({ e -> if (e.requestURI.path.endsWith("/students")) reply(e, roster(1))
            else reply(e, JSONObject().put("total", 101).put("items", JSONArray())) }) {
            val editor = NativeAttendanceEditor(context, actor, classroom, day)
            try { editor.load(); fail("Partial data must not become ready") } catch (_: IllegalStateException) { }
            assertFalse(editor.ready)
            try { NativeAttendanceEditor(context, actor, classroom, "2026-02-30"); fail("Invalid day") } catch (_: Exception) { }
        }
    }
}
