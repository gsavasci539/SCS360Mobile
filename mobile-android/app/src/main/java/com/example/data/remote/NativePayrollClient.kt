package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

internal enum class PayrollRecords(val path: String, val label: String) {
    RECORDS("records", "Bordrolar"), TIMESHEETS("timesheets", "Puantaj"), ADDITIONAL("additional-hours", "Ek ders")
}

internal class NativePayrollClient(selectedBranch: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val http = ApiClientProvider.createOkHttpClient()
    private val base = ApiClientProvider.activeBaseUrl
    private val branch = selectedBranch.takeUnless { it.isBlank() || it == "all" }?.let(::id)
    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONArray.objects() = (0 until length()).map(::getJSONObject)

    private suspend fun call(path: String, query: Map<String, String> = emptyMap(), payload: JSONObject? = null): Any = scope.io {
        val url = (base + "payroll/" + path).toHttpUrl().newBuilder().apply {
            branch?.let { addQueryParameter("branch_id", it) }
            query.filterValues(String::isNotBlank).forEach { (name, value) -> addQueryParameter(name, value) }
        }.build()
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).apply {
            if (payload != null) post(payload.toString().toRequestBody("application/json".toMediaType()))
        }.build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val result = JSONObject(requireNotNull(response.body).string())
            check(result.getBoolean("success"))
            result.opt("data") ?: JSONObject.NULL
        }
    }

    private fun periodQuery(period: String, month: YearMonth, staff: String) =
        mapOf("period" to period, "year" to month.year.toString(), "month" to month.monthValue.toString(), "staff_id" to staff)

    suspend fun options(search: String = "", staff: String = "") =
        call("options", mapOf("search" to search, "staff_id" to staff)) as JSONObject

    suspend fun summary(period: String, month: YearMonth, staff: String) =
        call("summary", periodQuery(period, month, staff)) as JSONObject

    suspend fun list(kind: PayrollRecords, page: Int, period: String, month: YearMonth, staff: String): List<JSONObject> {
        val query = if (kind == PayrollRecords.TIMESHEETS) mapOf("staff_id" to staff, "start_date" to month.atDay(1).toString(),
            "end_date" to month.plusMonths(if (period == "QUARTERLY") 2 else 0).atEndOfMonth().toString())
        else periodQuery(period, month, staff)
        return (call(kind.path, query + mapOf("limit" to "21", "offset" to (page * 20).toString())) as JSONArray).objects().onEach {
            id(it.getString("id"))
            check(staff.isBlank() || it.getString("staff_id") == staff)
        }
    }

    private suspend fun exact(kind: PayrollRecords, identifier: String) =
        (call(kind.path, mapOf("record_id" to id(identifier), "limit" to "1")) as JSONArray).objects().single()
            .also { check(it.getString("id") == identifier) }

    suspend fun save(kind: PayrollRecords, form: JSONObject) {
        val ack = call(kind.path, payload = form) as JSONObject
        val stored = exact(kind, ack.getString("id"))
        check(form.keys().asSequence().all { key ->
            val expected = form.payrollText(key)
            val actual = stored.payrollText(if (key == "timesheet_date") "date" else key)
            when (key) {
                "check_in_time", "check_out_time" -> expected.isBlank() && actual.isBlank() ||
                    expected.isNotBlank() && actual.isNotBlank() && OffsetDateTime.parse(expected).toInstant() == OffsetDateTime.parse(actual).toInstant()
                "work_hours", "total_hours", "hourly_rate", "base_salary", "additional_course_payment", "bonus", "advance_payment", "deductions" ->
                    expected.isEmpty() && actual.isEmpty() || expected.toBigDecimalOrNull()?.compareTo(actual.toBigDecimalOrNull() ?: return@all false) == 0
                else -> expected == actual
            }
        })
        if (kind == PayrollRecords.RECORDS) check(stored.getString("status") == "DRAFT")
        val amountKey = when (kind) {
            PayrollRecords.RECORDS -> "net_salary"
            PayrollRecords.ADDITIONAL -> "total_amount"
            else -> null
        }
        if (amountKey != null) check(ack.payrollText(amountKey).toBigDecimal().compareTo(stored.payrollText(amountKey).toBigDecimal()) == 0)
    }

    suspend fun approve(row: JSONObject) {
        val identifier = id(row.getString("id"))
        val ack = call("records/$identifier/approve", payload = JSONObject()) as JSONObject
        check(ack.getString("id") == identifier && ack.getString("status") == "APPROVED")
        val saved = exact(PayrollRecords.RECORDS, identifier)
        check(saved.getString("staff_id") == row.getString("staff_id") && saved.getString("status") == "APPROVED" &&
            saved.payrollText("approved_by").isNotBlank() && saved.payrollText("approved_by") == ack.payrollText("approved_by") &&
            saved.payrollText("approved_at") == ack.payrollText("approved_at") &&
            saved.payrollText("net_salary").toBigDecimal().compareTo(row.payrollText("net_salary").toBigDecimal()) == 0)
    }
}

internal fun JSONObject.payrollText(key: String) = if (isNull(key)) "" else getString(key)
