package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.math.BigDecimal
import java.util.UUID

internal data class CanteenPerson(val id: String, val name: String)
internal data class CanteenPeople(val students: List<CanteenPerson>, val more: Boolean, val branches: List<CanteenPerson>)
internal data class RecordedCanteenWallet(val student: String, val branch: String, val name: String, val balance: BigDecimal,
    val spent: BigDecimal, val limit: BigDecimal?, val dietary: List<String>, val categories: List<String>)
internal data class RecordedCanteenProduct(val id: String, val name: String, val category: String, val price: BigDecimal,
    val allergens: List<String>, val branch: String, val available: Boolean, val updatedAt: String,
    val description: String, val image: String, val nutrition: JSONObject?)
internal data class CanteenProductDraft(val name: String, val category: String, val price: BigDecimal, val allergens: List<String>,
    val description: String, val image: String, val available: Boolean)
internal data class RecordedCanteenEntry(val id: String, val amount: BigDecimal, val type: String, val method: String,
    val date: String, val status: String, val description: String)

internal class NativeCanteenClient(selectedBranch: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val branch = selectedBranch.takeUnless { it == "all" || it.isBlank() }?.let(::id)
    val fixedBranch get() = branch

    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else getString(key)
    private fun JSONObject.money(key: String) = getString(key).toBigDecimal()
    private fun JSONArray.rows() = (0 until length()).map(::getJSONObject)
    private fun JSONArray.entries() = (0 until length()).map(::getString)
    private fun walletPath(student: String) = "student/${id(student)}/wallet"

    private suspend fun call(path: String, query: Map<String, String?> = emptyMap(), body: JSONObject? = null,
        method: String = if (body == null) "GET" else "POST"): Any = scope.io {
        val url = (base + "canteen/" + path).toHttpUrl().newBuilder().apply {
            query.forEach { (key, value) -> value?.let { addQueryParameter(key, it) } }
        }.build()
        val wire = body?.toString()?.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).method(method, wire).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val parsed = JSONTokener(response.body?.string().orEmpty()).nextValue()
            if (parsed is JSONObject && parsed.has("success")) {
                check(parsed.getBoolean("success")) { "Kantin işlemi sunucudan doğrulanamadı." }
                parsed.get("data")
            } else parsed
        }
    }

    suspend fun people(search: String): CanteenPeople {
        val data = call("options", mapOf("branch_id" to branch, "search" to search)) as JSONObject
        return CanteenPeople(data.getJSONArray("students").rows().filter { it.getBoolean("active") }
            .map { CanteenPerson(id(it.getString("id")), it.getString("name")) }, data.getBoolean("more_students"),
            data.getJSONArray("branches").rows().map { CanteenPerson(id(it.getString("id")), it.getString("name")) }
                .filter { branch == null || it.id == branch })
    }

    suspend fun wallet(student: String): RecordedCanteenWallet {
        val data = call(walletPath(student), mapOf("branch_id" to branch)) as JSONObject
        val result = RecordedCanteenWallet(id(data.getString("student_id")), id(data.getString("branch_id")),
            data.getString("student_name"), data.money("balance"), data.money("daily_spent"),
            data.text("daily_limit").toBigDecimalOrNull(), data.getJSONArray("dietary_restrictions").entries(),
            data.getJSONArray("restricted_categories").entries())
        check(result.student == student && (branch == null || result.branch == branch))
        return result
    }

    suspend fun products(wallet: RecordedCanteenWallet, limit: Int, offset: Int): List<RecordedCanteenProduct> =
        catalog(wallet.branch, true, limit, offset)

    suspend fun catalog(selectedBranch: String, available: Boolean, limit: Int, offset: Int,
        record: String? = null): List<RecordedCanteenProduct> {
        check(branch == null || selectedBranch == branch)
        return (call("products", mapOf("branch_id" to id(selectedBranch), "is_available" to available.toString(),
            "limit" to limit.toString(), "offset" to offset.toString(), "record_id" to record?.let(::id))) as JSONArray).rows().map {
            val result = RecordedCanteenProduct(id(it.getString("id")), it.getString("name"), it.text("category"),
                it.money("price"), it.getJSONArray("allergens").entries(), id(it.getString("branch_id")),
                it.getBoolean("is_available"), it.text("updated_at"), it.text("description"), it.text("image_url"),
                it.optJSONObject("nutritional_info"))
            check(result.branch == selectedBranch && (record == null && result.available == available || result.id == record))
            result
        }
    }

    suspend fun saveProduct(selectedBranch: String, draft: CanteenProductDraft, existing: RecordedCanteenProduct?, request: UUID) {
        check((branch == null || selectedBranch == branch) && (existing == null || existing.branch == selectedBranch))
        val body = JSONObject().put("branch_id", id(selectedBranch)).put("name", draft.name)
            .put("category", draft.category.ifBlank { null } ?: JSONObject.NULL).put("price", draft.price.toPlainString())
            .put("description", draft.description.ifBlank { null } ?: JSONObject.NULL)
            .put("image_url", draft.image.ifBlank { null } ?: JSONObject.NULL).put("allergens", JSONArray(draft.allergens))
            .put("nutritional_info", existing?.nutrition ?: JSONObject.NULL).put("is_available", draft.available)
            .put("request_id", request.toString())
        if (existing != null) {
            check(existing.updatedAt.isNotBlank())
            body.put("expected_updated_at", existing.updatedAt)
        }
        val path = if (existing == null) "products" else "products/${id(existing.id)}"
        val ack = call(path, body = body, method = if (existing == null) "POST" else "PUT") as JSONObject
        val expected = existing?.id ?: request.toString()
        check(ack.getString("id") == expected)
        val stored = catalog(selectedBranch, draft.available, 1, 0, expected).single()
        check(stored.name == draft.name && stored.category == draft.category && stored.price.compareTo(draft.price) == 0 &&
            stored.allergens == draft.allergens && stored.available == draft.available && stored.description == draft.description &&
            stored.image == draft.image && (existing == null || stored.updatedAt == ack.getString("updated_at")))
    }

    suspend fun setRestrictions(wallet: RecordedCanteenWallet, limit: BigDecimal?, categories: List<String>, allergens: List<String>) {
        val body = JSONObject().put("student_id", id(wallet.student)).put("daily_limit", limit?.toPlainString() ?: JSONObject.NULL)
            .put("restricted_categories", JSONArray(categories)).put("restricted_allergens", JSONArray(allergens))
        val ack = call("wallet/restrictions", mapOf("branch_id" to id(wallet.branch)), body, "PUT") as JSONObject
        check(ack.getString("student_id") == wallet.student && ack.getString("branch_id") == wallet.branch)
        val stored = wallet(wallet.student)
        check(stored.branch == wallet.branch && stored.categories == categories && stored.dietary == allergens &&
            (if (limit == null) stored.limit == null else stored.limit?.compareTo(limit) == 0))
    }

    suspend fun history(wallet: RecordedCanteenWallet, limit: Int, offset: Int, record: String? = null): List<RecordedCanteenEntry> =
        (call("${walletPath(wallet.student)}/transactions", mapOf("branch_id" to id(wallet.branch),
            "limit" to limit.toString(), "offset" to offset.toString(), "record_id" to record?.let(::id))) as JSONArray).rows().map {
            val entry = RecordedCanteenEntry(id(it.getString("id")), it.money("amount"), it.getString("transaction_type"),
                it.text("payment_method"), it.getString("date"), it.getString("status"), it.text("item_name"))
            check(it.getString("student_id") == wallet.student && (record == null || entry.id == record))
            entry
        }

    private suspend fun confirmedEntry(ack: JSONObject, wallet: RecordedCanteenWallet, request: UUID, type: String,
        method: String, amount: BigDecimal) {
        check(ack.getString("operation_id") == request.toString() && ack.getString("student_id") == wallet.student &&
            ack.getString("branch_id") == wallet.branch && ack.money("amount").compareTo(amount) == 0)
        val record = history(wallet, 1, 0, id(ack.getString("transaction_id"))).single()
        check(record.amount.compareTo(amount) == 0 && record.type == type && record.method == method && record.status == "COMPLETED")
    }

    suspend fun topUp(wallet: RecordedCanteenWallet, amount: BigDecimal, method: String, reference: String, request: UUID): RecordedCanteenWallet {
        val ack = call("${walletPath(wallet.student)}/top-up", mapOf("branch_id" to id(wallet.branch)),
            JSONObject().put("student_id", id(wallet.student)).put("amount", amount.toPlainString()).put("payment_method", method)
                .put("reference_number", reference.ifBlank { null } ?: JSONObject.NULL).put("request_id", request.toString())) as JSONObject
        confirmedEntry(ack, wallet, request, "CREDIT", method, amount)
        return wallet(wallet.student).also { check(it.branch == wallet.branch) }
    }

    suspend fun sell(wallet: RecordedCanteenWallet, product: RecordedCanteenProduct, quantity: Int, method: String,
        request: UUID): RecordedCanteenWallet {
        val body = JSONObject().put("student_id", id(wallet.student)).put("branch_id", id(wallet.branch))
            .put("product_id", id(product.id)).put("quantity", quantity).put("payment_method", method)
            .put("request_id", request.toString()).put("expected_unit_price", product.price.toPlainString())
        val ack = call("pos/checkout", body = body) as JSONObject
        check(ack.getBoolean("authorized"))
        val amount = product.price.multiply(quantity.toBigDecimal())
        confirmedEntry(ack, wallet, request, if (method == "WALLET") "DEBIT" else "SALE", method, amount)
        val orderId = id(ack.getString("order_id"))
        val stored = (call("orders", mapOf("student_id" to id(wallet.student), "branch_id" to id(wallet.branch),
            "record_id" to orderId, "limit" to "1")) as JSONArray).rows().single()
        check(stored.getString("id") == orderId && stored.getString("student_id") == wallet.student &&
            stored.getString("branch_id") == wallet.branch && stored.getString("product_id") == product.id &&
            stored.getInt("quantity") == quantity && stored.money("total_price").compareTo(amount) == 0 &&
            stored.getString("status") == "COMPLETED")
        return wallet(wallet.student).also { check(it.branch == wallet.branch) }
    }
}
