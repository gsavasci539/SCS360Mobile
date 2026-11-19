package com.example

import com.example.data.model.DailyReportDraft
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.DailyReportEdit
import com.example.data.remote.NativeDailyReportClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeDailyReportClientTest {
    @Test fun lostSaveResponseRecoversExactRecordAndDoesNotWriteAgain() = runBlocking {
        val student = UUID(22, 1).toString()
        val classroom = UUID(22, 2).toString()
        val actor = UUID(22, 3).toString()
        val key = UUID(22, 4).toString()
        val draft = DailyReportDraft(student, classroom, LocalDate.parse("2026-11-19"),
            breakfast = "Yarısını Yedi", sleepMinutes = 0, note = "Kayıtlı gözlem")
        val payload = AtomicReference<JSONObject>()
        val writes = AtomicInteger()
        val saved = """{"id":"$key","student_id":"$student","class_id":"$classroom","recorded_by":"$actor","date":"2026-11-19","meal_status":"Kahvaltı: Yarısını Yedi","sleep_status":"Uyumadı","teacher_notes":"Kayıtlı gözlem"}"""
        val base = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val previous = listOf(interceptor.authToken, interceptor.tenantId, interceptor.branchId, interceptor.workspaceOrganizationId)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/daily-reports") { exchange ->
            if (exchange.requestMethod == "POST") {
                payload.set(JSONObject(exchange.requestBody.bufferedReader().readText()))
                writes.incrementAndGet()
                exchange.close()
            } else {
                val rows = if (writes.get() == 0) "[]" else "[$saved]"
                val data = """{"success":true,"data":{"page":1,"total_pages":1,"items":$rows}}""".toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, data.size.toLong())
                exchange.responseBody.use { it.write(data) }
            }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-report")
        try {
            assertEquals(key, NativeDailyReportClient().save(draft, actor, true).id)
            assertEquals(key, NativeDailyReportClient().save(draft, actor, true).id)
            assertTrue(runCatching { NativeDailyReportClient().save(draft.copy(note = "Farklı not"), actor, true) }.isFailure)
            assertEquals(1, writes.get())
            assertEquals(setOf("student_id", "class_id", "date", "meal_breakfast", "sleep_status", "teacher_notes"),
                payload.get().keys().asSequence().toSet())
            assertEquals("Uyumadı", payload.get().getString("sleep_status"))
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            interceptor.setSession(previous[0], previous[1], previous[2], previous[3])
        }
    }

    @Test fun detailAndLostMutationResponsesUseTheVerifiedRecord() = runBlocking {
        val id = UUID(24, 1).toString()
        val actor = UUID(24, 2).toString()
        val row = JSONObject().put("id", id).put("student_id", UUID(24, 3).toString())
            .put("class_id", UUID(24, 4).toString()).put("date", "2026-11-19").put("recorded_by", actor)
            .put("teacher_notes", "İlk gözlem")
        val existing = AtomicReference(row)
        val updateBody = AtomicReference<JSONObject>()
        val deletes = AtomicInteger()
        val interceptor = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val previous = listOf(interceptor.authToken, interceptor.tenantId, interceptor.branchId, interceptor.workspaceOrganizationId)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/daily-reports/$id") { exchange ->
            when (exchange.requestMethod) {
                "PUT" -> {
                    val body = JSONObject(exchange.requestBody.bufferedReader().readText())
                    updateBody.set(body)
                    val changed = JSONObject(row.toString())
                    for (key in body.keys()) changed.put(key, body.get(key))
                    existing.set(changed)
                    exchange.close()
                }
                "DELETE" -> { deletes.incrementAndGet(); existing.set(null); exchange.close() }
                else -> {
                    val current = existing.get()
                    if (current == null) {
                        exchange.sendResponseHeaders(404, -1)
                        exchange.close()
                    } else {
                        val data = JSONObject().put("success", true).put("data", current).toString().toByteArray()
                        exchange.responseHeaders.add("Content-Type", "application/json")
                        exchange.sendResponseHeaders(200, data.size.toLong())
                        exchange.responseBody.use { it.write(data) }
                    }
                }
            }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-report-edit")
        try {
            val client = NativeDailyReportClient()
            val first = client.detail(id)
            assertEquals("İlk gözlem", first.teacher_note)
            val updated = client.update(first, DailyReportEdit.from(first).copy(note = "Yeni gözlem"), actor)
            assertEquals("Yeni gözlem", updated.teacher_note)
            assertEquals(setOf("meal_status", "sleep_status", "mood", "activities", "teacher_notes"),
                updateBody.get().keys().asSequence().toSet())
            assertTrue(runCatching { client.delete(first) }.isFailure)
            assertEquals(0, deletes.get())
            client.delete(updated)
            client.delete(updated)
            assertEquals(1, deletes.get())
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            interceptor.setSession(previous[0], previous[1], previous[2], previous[3])
        }
    }
}
