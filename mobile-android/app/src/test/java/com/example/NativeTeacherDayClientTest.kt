package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeTeacherDayClient
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.InetSocketAddress
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeTeacherDayClientTest {
    private val classId = UUID(23, 1).toString()
    private val institution = UUID(23, 2).toString()

    private suspend fun server(handler: (HttpExchange) -> Unit, test: suspend () -> Unit) {
        val oldBase = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val oldToken = interceptor.authToken
        val oldTenant = interceptor.tenantId
        val oldBranch = interceptor.branchId
        val oldWorkspace = interceptor.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/", handler)
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-teacher", workspace = institution)
        try {
            test()
        } finally {
            server.stop(0)
            ApiClientProvider.createService(oldBase)
            interceptor.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
        }
    }

    private fun HttpExchange.respond(data: String, status: Int = 200) {
        val bytes = """{"success":${status == 200},"data":$data}""".toByteArray()
        responseHeaders.add("Content-Type", "application/json")
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    @Test fun classesTodayWeekAndTasksUseTheRecordedApiRows() = runBlocking {
        val requests = mutableListOf<String>()
        server({ exchange ->
            val path = exchange.requestURI.path
            requests += path
            if (exchange.requestHeaders.getFirst("Authorization") != "Bearer synthetic-teacher" ||
                exchange.requestHeaders.getFirst("X-SCS360-Organization-Id") != institution) {
                exchange.respond("null", 403)
            } else {
                when (path) {
                    "/api/teacher/my-classes" -> exchange.respond("""[{"id":"$classId","name":"Bilim","student_count":3}]""")
                    "/api/teacher/today-schedule" -> exchange.respond("""[{"subject":"Kimya","start_time":"09:00","end_time":"09:40","class_name":"Bilim","room_number":null}]""")
                    "/api/teacher/pending-tasks" -> exchange.respond("""{"date":"2026-11-19","tasks":[{"class_name":"Bilim","task_type":"attendance","remaining_count":2,"due_date":"2026-11-19"}]}""")
                    "/api/classes/$classId/schedule" -> exchange.respond("""[{"day":"Perşembe","periods":[{"subject":"Fizik","time":"13:00 - 13:45"}]}]""")
                    else -> exchange.respond("null", 404)
                }
            }
        }) {
            val client = NativeTeacherDayClient()
            val classroom = client.classes().single()
            assertEquals(3, classroom.studentCount)
            assertEquals("09:00 - 09:40", client.today().single().time)
            assertEquals(2, client.pending().single().remaining)
            assertEquals("Fizik", client.week(classroom.id).single().lessons.single().subject)
            assertEquals(listOf("/api/teacher/my-classes", "/api/teacher/today-schedule", "/api/teacher/pending-tasks",
                "/api/classes/$classId/schedule"), requests)
        }
    }

    @Test fun authorizationFailureDoesNotReturnAnEmptySchedule() = runBlocking {
        server({ it.respond("null", 403) }) {
            val result = runCatching { NativeTeacherDayClient().today() }
            assertTrue(result.exceptionOrNull() is IOException)
        }
    }
}
