package com.example.data.remote

import com.example.data.model.*
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate
import java.util.UUID

/** Canonical server records. Display fields never create local loan identities or stock. */
class NativeLibraryClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    fun current() = scope.current()
    private fun id(value: String) = UUID.fromString(value).toString()
    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Any = scope.io {
        val request = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (method != "GET") request.method(method, (body ?: JSONObject()).toString().toRequestBody("application/json".toMediaType()))
        http.newCall(request.build()).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val result = JSONTokener(response.body?.string() ?: "null").nextValue()
            if (result is JSONObject && result.has("success")) {
                check(result.getBoolean("success")) { "Sunucu işlemi onaylamadı" }; result.opt("data") ?: JSONObject.NULL
            } else result
        }
    }
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else optString(key)
    private fun book(row: JSONObject): LibraryBook {
        val total = row.getInt("total_copies"); val available = row.getInt("available_copies")
        check(total >= 0 && available in 0..total) { "Kitap stok bilgisi doğrulanamadı" }
        return LibraryBook(id = id(row.getString("id")), title = row.getString("title"), author = row.text("author"),
            category = row.text("category"), isbn = row.text("isbn"), publisher = row.text("publisher"),
            page_count = row.optInt("page_count", 0), shelf_location = row.text("shelf_location"), total_copies = total, available_copies = available)
    }
    private fun <T> rows(value: Any, parse: (JSONObject) -> T): List<T> {
        val array = value as JSONArray; val seen = mutableSetOf<String>()
        return (0 until array.length()).map { index -> val row = array.getJSONObject(index); check(seen.add(id(row.getString("id")))); parse(row) }
    }
    suspend fun books() = rows(call("library/books"), ::book)
    suspend fun createBook(input: JSONObject): LibraryBook {
        val allowed = setOf("title", "author", "category", "isbn", "barcode", "total_copies")
        require(input.keys().asSequence().all { it in allowed }) { "Desteklenmeyen kitap alanı" }
        val body = JSONObject(input.toString())
        for (key in listOf("title", "author", "category")) { val value = body.getString(key).trim(); require(value.isNotEmpty()); body.put(key, value) }
        require(body.getInt("total_copies") in 1..100000)
        val result = book(call("library/books", "POST", body) as JSONObject)
        for (key in listOf("title", "author", "category")) check(body.getString(key) == when (key) { "title" -> result.title; "author" -> result.author; else -> result.category })
        check(result.total_copies == body.getInt("total_copies") && result.available_copies == result.total_copies)
        return result
    }
    suspend fun loans() = rows(call("library/loans")) { row ->
        val status = row.getString("status"); check(status in setOf("BORROWED", "OVERDUE", "RETURNED"))
        val start = LocalDate.parse(row.getString("borrow_date")); val due = LocalDate.parse(row.getString("due_date"))
        check(!due.isBefore(start)); val returned = row.text("return_date").ifBlank { null }
        check((status == "RETURNED") == (returned != null)); returned?.let(LocalDate::parse)
        BookLoanRecord(id = id(row.getString("id")), book_id = id(row.getString("book_id")), book_title = row.getString("book_title"),
            book_author = row.text("book_author"), student_id = id(row.getString("student_id")), student_name = row.getString("student_name"),
            student_number = row.text("student_number"), class_name = row.text("class_name"), borrowed_at = start.toString(), due_date = due.toString(),
            returned_at = returned, is_returned = status == "RETURNED", is_overdue = status == "OVERDUE", parent_phone = row.text("parent_phone"))
    }
    suspend fun borrow(bookId: String, student: Student, duration: Int): BookLoanRecord {
        require(duration in 1..365)
        val body = JSONObject().put("book_id", id(bookId)).put("student_id", id(student.id)).put("student_name", student.full_name)
            .put("student_number", student.student_number).put("class_name", student.class_name).put("duration_days", duration)
        val ack = call("library/loans/borrow", "POST", body) as JSONObject
        val loanId = id(ack.getString("id")); check(ack.getString("status") == "BORROWED")
        return loans().single { it.id == loanId }.also {
            check(it.book_id == bookId && it.student_id == student.id && !it.is_returned && it.due_date == ack.getString("due_date")) { "Ödünç kaydı sunucuda doğrulanamadı" }
        }
    }
    suspend fun returnBook(loanId: String): BookLoanRecord {
        val key = id(loanId); val ack = call("library/loans/$key/return", "PUT") as JSONObject
        check(ack.getString("id") == key && ack.getString("status") == "RETURNED")
        return loans().single { it.id == key }.also { check(it.is_returned) { "İade kaydı sunucuda doğrulanamadı" } }
    }
}
