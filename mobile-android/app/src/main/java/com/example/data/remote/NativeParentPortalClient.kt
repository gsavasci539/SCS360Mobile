package com.example.data.remote

import com.example.data.model.Invoice
import com.example.data.model.Student
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.math.BigDecimal
import java.util.UUID

/** Only explicit server-side parent/child relationships; never match names or phones. */
class NativeParentPortalClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val client = ApiClientProvider.createOkHttpClient()
    private suspend fun get(path: String): Any = scope.io {
        client.newCall(Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope).build()).execute().use {
            if (!it.isSuccessful) throw RegistryHttpException(it.code)
            val value = JSONTokener(it.body?.string() ?: "null").nextValue()
            if (value is JSONObject && value.has("success")) {
                check(value.getBoolean("success")) { "Veli verileri doğrulanamadı" }; value.get("data")
            } else value
        }
    }
    suspend fun children(): List<Student> {
        val values = get("parent/my-children") as JSONArray
        val seen = mutableSetOf<String>()
        return (0 until values.length()).map {
            val row = values.getJSONObject(it); val id = UUID.fromString(row.getString("student_id")).toString()
            check(seen.add(id)) { "Öğrenci ilişkileri tutarsız" }
            fun text(key: String) = if (row.isNull(key)) "" else row.getString(key)
            Student(id = id, first_name = row.getString("first_name"), last_name = row.getString("last_name"),
                student_number = text("student_number"), class_id = text("class_id"), class_name = text("class_name"), status = text("status"))
        }
    }
    suspend fun invoices(child: Student): List<Invoice> {
        val id = UUID.fromString(child.id).toString()
        val value = get("parent/child/$id/financial-invoices") as JSONObject
        check(value.getString("student_id") == id) { "Borçlar seçili öğrenciyle eşleşmiyor" }
        val rows = value.getJSONArray("invoices"); val seen = mutableSetOf<String>()
        return (0 until rows.length()).map {
            val row = rows.getJSONObject(it); val debt = UUID.fromString(row.getString("debt_id")).toString()
            check(seen.add(debt)) { "Borç listesi tutarsız" }
            val amount = BigDecimal(row.getString("amount")); val paid = BigDecimal(row.getString("paid_amount"))
            val remaining = BigDecimal(row.getString("remaining_amount"))
            check(amount.signum() >= 0 && paid.signum() >= 0 && remaining.signum() >= 0 && (amount - paid).compareTo(remaining) == 0) { "Borç tutarları doğrulanamadı" }
            Invoice(id = debt, student_id = id, student_name = child.full_name, class_name = child.class_name,
                amount = amount.toDouble(), paid_amount = paid.toDouble(), status = row.getString("status"),
                description = if (row.isNull("description")) "" else row.getString("description"),
                due_date = if (row.isNull("due_date")) "" else row.getString("due_date"))
        }
    }
}
