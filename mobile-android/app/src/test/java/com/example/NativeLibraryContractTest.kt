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
class NativeLibraryContractTest {
    private fun id(n: Int) = UUID(81, n.toLong()).toString()
    private data class Reply(val code: Int = 200, val value: Any = JSONObject.NULL)
    private fun book() = JSONObject().put("id", id(1)).put("title", "Actual book").put("author", "Actual author").put("category", "Category").put("total_copies", 2).put("available_copies", 2)
    private fun loan(returned: Boolean = false, student: String = id(2)) = JSONObject().put("id", id(3)).put("book_id", id(1)).put("book_title", "Actual book")
        .put("student_id", student).put("student_name", "Server Student").put("student_number", JSONObject.NULL).put("borrow_date", "2027-01-01").put("due_date", "2027-01-15")
        .put("return_date", if (returned) "2027-01-10" else JSONObject.NULL).put("status", if (returned) "RETURNED" else "BORROWED")
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
    @Test fun canonicalCatalogBorrowAndPutReturnUseServerRecordsWithoutLocalStock() = runBlocking {
        var returned = false; val writes = mutableListOf<Pair<String, JSONObject>>()
        server({ e ->
            val path = e.requestURI.path
            if (e.requestMethod != "GET") writes.add("${e.requestMethod} $path" to JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8)))
            Reply(value = when {
                path.endsWith("/books") && e.requestMethod == "POST" -> book()
                path.endsWith("/books") -> JSONArray().put(book())
                path.endsWith("/borrow") -> JSONObject().put("id", id(3)).put("status", "BORROWED").put("due_date", "2027-01-15")
                path.endsWith("/return") -> { returned = true; JSONObject().put("id", id(3)).put("status", "RETURNED") }
                else -> JSONArray().put(loan(returned))
            })
        }) {
            val api = NativeLibraryClient(); val created = api.createBook(JSONObject().put("title", "Actual book").put("author", "Actual author").put("category", "Category").put("total_copies", 2))
            assertEquals("", created.publisher); assertEquals("", created.shelf_location); assertEquals(0, created.page_count)
            val saved = api.borrow(created.id, Student(id = id(2), first_name = "Input", last_name = "Name"), 14)
            assertEquals("Server Student", saved.student_name); assertEquals("", saved.parent_phone); assertEquals("", saved.class_name)
            assertTrue(api.returnBook(saved.id).is_returned)
            assertEquals(listOf("POST /api/library/books", "POST /api/library/loans/borrow", "PUT /api/library/loans/${id(3)}/return"), writes.map { it.first })
            assertEquals(setOf("book_id","student_id","student_name","student_number","class_name","duration_days"), writes[1].second.keys().asSequence().toSet())
            assertEquals(id(2), writes[1].second.getString("student_id")); assertEquals(14, writes[1].second.getInt("duration_days"))
        }
    }
    @Test fun invalidDisplayFieldsIdsDurationAndStaleScopeCannotWrite() = runBlocking {
        var calls = 0
        server({ _ -> calls++; Reply() }) {
            val api = NativeLibraryClient()
            try { api.createBook(JSONObject().put("publisher", "Sample")); fail("Display model") } catch (_: IllegalArgumentException) { }
            try { api.borrow(id(1), Student(id = "std-1"), 14); fail("Synthetic student ID") } catch (_: IllegalArgumentException) { }
            try { api.borrow(id(1), Student(id = id(2)), 0); fail("Invalid duration") } catch (_: IllegalArgumentException) { }
            assertEquals(0, calls); ApiClientProvider.interceptor.invalidateContext()
            try { api.returnBook(id(3)); fail("Stale write") } catch (_: kotlinx.coroutines.CancellationException) { }
            assertEquals(0, calls)
        }
    }
    @Test fun wrongStudentReadbackDoesNotTurnBorrowAcknowledgementIntoSuccess() = runBlocking {
        server({ e -> Reply(value = if (e.requestMethod == "POST") JSONObject().put("id", id(3)).put("status", "BORROWED").put("due_date", "2027-01-15") else JSONArray().put(loan(student = id(99)))) }) {
            try { NativeLibraryClient().borrow(id(1), Student(id = id(2)), 14); fail("Mismatched student") } catch (_: IllegalStateException) { }
        }
    }
    @Test fun forbiddenAndAlreadyReturnedRemainExplicitServerFailures() = runBlocking {
        var code = 403
        server({ _ -> Reply(code) }) {
            val api = NativeLibraryClient()
            try { api.books(); fail("Forbidden") } catch (failure: RegistryHttpException) { assertEquals(403, failure.code) }
            code = 409
            try { api.returnBook(id(3)); fail("Duplicate return") } catch (failure: RegistryHttpException) { assertEquals(409, failure.code) }
        }
    }
    @Test fun lostBorrowAcknowledgementIsNotAutomaticallySentAgain() = runBlocking {
        var posts = 0
        server({ e -> e.requestBody.readBytes(); posts++; Reply(0) }) {
            try { NativeLibraryClient().borrow(id(1), Student(id = id(2)), 14); fail("Lost ACK") } catch (_: java.io.IOException) { }
            assertEquals(1, posts)
        }
    }
    @Test fun dueDatesUseRecordedCalendarDaysAndEmptyModelsInventNothing() {
        val today = LocalDate.of(2027,1,10)
        assertEquals("Kalan: 5 Gün", BookLoanRecord(due_date = "2027-01-15").dueLabel(today))
        assertEquals("Gecikti (9 Gün)", BookLoanRecord(due_date = "2027-01-01").dueLabel(today))
        assertEquals("Bugün teslim", BookLoanRecord(due_date = "2027-01-10").dueLabel(today))
        assertEquals("Son tarih belirtilmedi", BookLoanRecord().dueLabel(today))
        assertEquals("", BookLoanRecord().student_id); assertEquals("", BookLoanRecord().parent_phone); assertEquals("", LibraryBook().title)
    }
}
