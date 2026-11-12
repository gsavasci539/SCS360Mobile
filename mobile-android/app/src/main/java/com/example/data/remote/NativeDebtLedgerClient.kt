package com.example.data.remote

import com.example.data.model.Debt
import com.example.data.model.Invoice
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class DebtLedgerHttpException(val code: Int) : java.io.IOException("Tahakkuk işlemi HTTP $code")

/** Canonical debt ledger; display models never become HTTP payloads or optimistic Room writes. */
class NativeDebtLedgerClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val branch = ApiClientProvider.interceptor.branchId
    private val workspace = ApiClientProvider.interceptor.workspaceOrganizationId
    private val client = ApiClientProvider.createOkHttpClient()

    private suspend fun request(path: String, method: String = "GET", body: JSONObject? = null): JSONObject = scope.io {
        val builder = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (method == "PUT") builder.put(checkNotNull(body).toString().toRequestBody("application/json".toMediaType()))
        if (method == "DELETE") builder.delete()
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw DebtLedgerHttpException(response.code)
            val value = JSONObject(response.body?.string() ?: "null")
            // The shared authenticated interceptor already unwraps non-null data.
            if (value.has("success")) {
                check(value.getBoolean("success")) { "Sunucu işlemi doğrulamadı" }
                value.optJSONObject("data") ?: JSONObject()
            } else value
        }
    }
    private fun parse(row: JSONObject): Debt {
        fun uuid(key: String) = UUID.fromString(row.getString(key)).toString()
        fun text(key: String) = if (row.isNull(key)) "" else row.getString(key)
        fun money(key: String) = row.getString(key).toBigDecimal().also {
            check(it >= BigDecimal.ZERO && it <= BigDecimal("9999999999.99") && it.stripTrailingZeros().scale() <= 2)
        }
        val organization = uuid("organization_id"); val rowBranch = uuid("branch_id")
        check(workspace.isNullOrBlank() || organization == workspace) { "Tahakkuk kurumu eşleşmedi" }
        check(branch == null || rowBranch == branch) { "Tahakkuk şubesi eşleşmedi" }
        val amount = money("amount"); val paid = money("paid_amount")
        check(amount > BigDecimal.ZERO && paid <= amount && money("remaining_amount").compareTo(amount - paid) == 0)
        val status = row.getString("status"); val type = row.getString("debt_type")
        check(status in setOf("UNPAID", "PARTIALLY_PAID", "PAID", "OVERDUE", "CANCELLED"))
        check(type in setOf("EDUCATION", "FOOD", "SERVICE", "STATIONERY", "ACTIVITY", "OTHER"))
        return Debt(id=uuid("id"), organization_id=organization, branch_id=rowBranch, student_id=uuid("student_id"),
            student_name=text("student_name"), class_name=text("class_name"), debt_type=type, description=text("description"),
            amount=amount.toDouble(), paid_amount=paid.toDouble(), status=status,
            due_date=LocalDate.parse(row.getString("due_date")).toString(), created_at=text("created_at"))
    }
    private fun invoice(row: Debt) = Invoice(id=row.id, invoice_number="", student_id=row.student_id,
        student_name=row.student_name, class_name=row.class_name, amount=row.amount, paid_amount=row.paid_amount,
        due_date=row.due_date, issue_date=row.created_at.take(10),
        status=if (row.status in setOf("UNPAID", "PARTIALLY_PAID")) "PENDING" else row.status,
        description=row.description, payment_method=null)

    suspend fun debts(studentId: String? = null): List<Debt> {
        val student = studentId?.let { UUID.fromString(it).toString() }
        val rows = mutableListOf<Debt>(); var page = 1; var total: Int? = null
        while (true) {
            val data = request("debts?page=$page&page_size=100" + (branch?.let { "&branch_id=${UUID.fromString(it)}" } ?: "")
                + (student?.let { "&student_id=$it" } ?: ""))
            val count = data.getInt("total"); check(count in 0..100000 && (total == null || total == count)); total = count
            val items = data.getJSONArray("items")
            for (i in 0 until items.length()) {
                val row = parse(items.getJSONObject(i)); check(student == null || row.student_id == student); rows.add(row)
            }
            check(rows.map { it.id }.distinct().size == rows.size && rows.size <= count) { "Tahakkuk sayfaları tutarsız" }
            if (rows.size == count) break
            check(items.length() > 0) { "Eksik tahakkuk sayfası" }; page++
        }
        return rows
    }
    suspend fun invoices(branchId: String?, status: String?): List<Invoice> {
        val selected = branchId?.takeUnless { it.isBlank() || it == "all" } ?: branch
        require(selected == branch) { "Şube seçimi değişti; yeniden yükleyin" }
        val filter = status?.takeUnless { it.isBlank() || it == "ALL" }
        require(filter == null || filter in setOf("PENDING", "PAID", "OVERDUE", "CANCELLED"))
        return debts().map(::invoice).filter { filter == null || it.status == filter }
    }
    suspend fun update(draft: Invoice): Invoice {
        val id = UUID.fromString(draft.id).toString()
        val current = parse(request("debts/$id")); val shown = invoice(current)
        require(draft.student_id == current.student_id && draft.paid_amount == current.paid_amount && draft.status == shown.status) {
            "Öğrenci, tahsilat ve borç durumu bu formdan değiştirilemez; güncel kaydı yükleyin"
        }
        val amount = BigDecimal.valueOf(draft.amount)
        require(amount > BigDecimal.ZERO && amount <= BigDecimal("9999999999.99") && amount.stripTrailingZeros().scale() <= 2)
        val body = JSONObject().put("due_date", LocalDate.parse(draft.due_date).toString()).put("description", draft.description)
        if (draft.amount != current.amount) body.put("amount", amount.setScale(2).toPlainString())
        val ack = request("debts/$id", "PUT", body)
        check(UUID.fromString(ack.getString("id")).toString() == id)
        return invoice(parse(request("debts/$id")))
    }
    suspend fun delete(id: String) { request("debts/${UUID.fromString(id)}", "DELETE") }
}
