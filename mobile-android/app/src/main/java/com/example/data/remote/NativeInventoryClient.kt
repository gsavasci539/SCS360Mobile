package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal enum class InventoryRecords(val route: String, val label: String) {
    ITEMS("items", "Demirbaşlar"),
    LOCATIONS("locations", "Konumlar"),
    ASSIGNMENTS("assignments", "Zimmetler"),
    MAINTENANCE("maintenance-requests", "Bakım talepleri")
}

internal data class InventoryChoice(val id: String, val name: String, val code: String, val assigned: Boolean)
internal data class InventoryChoices(
    val branches: List<InventoryChoice>, val locations: List<InventoryChoice>,
    val items: List<InventoryChoice>, val users: List<InventoryChoice>,
    val moreItems: Boolean, val moreUsers: Boolean
)

internal class NativeInventoryClient(selectedBranch: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    val activeBranch = selectedBranch.takeUnless { it.isBlank() || it == "all" }?.let(::uuid)

    private fun uuid(value: String) = UUID.fromString(value).toString()
    private fun JSONArray.rows() = (0 until length()).map(::getJSONObject)

    private suspend fun call(path: String, query: Map<String, String?> = emptyMap(), body: JSONObject? = null): Any = scope.io {
        val url = (base + path).toHttpUrl().newBuilder().apply {
            query.forEach { (key, value) -> if (!value.isNullOrBlank()) addQueryParameter(key, value) }
        }.build()
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).apply {
            if (body != null) post(body.toString().toRequestBody("application/json".toMediaType()))
        }.build()
        http.newCall(request).execute().use { reply ->
            if (!reply.isSuccessful) throw RegistryHttpException(reply.code)
            val envelope = JSONObject(requireNotNull(reply.body).string())
            check(envelope.getBoolean("success"))
            envelope.get("data")
        }
    }

    private fun branch(value: String?) = value?.let(::uuid).also {
        require(activeBranch == null || it == activeBranch) { "Seçili şube değişti" }
    }

    suspend fun list(kind: InventoryRecords, page: Int) = records(kind, activeBranch, null, 21, page * 20)

    private suspend fun records(kind: InventoryRecords, branch: String?, record: String?, limit: Int, offset: Int): List<JSONObject> =
        (call("inventory/${kind.route}", mapOf("branch_id" to branch(branch), "record_id" to record,
            "limit" to limit.toString(), "offset" to offset.toString())) as JSONArray).rows().onEach {
            uuid(it.getString("id"))
            check(branch == null || it.getString("branch_id") == branch) { "Kayıt şubesi doğrulanamadı" }
        }

    suspend fun summary(): JSONObject =
        (call("ai/inventory-analysis", mapOf("branch_id" to activeBranch)) as JSONObject).also {
            check(it.getString("source") == "DATABASE_AND_RULES" && it.has("branch_id") && it.text("branch_id") == activeBranch.orEmpty())
            check(listOf("total_items", "assigned_count", "available_count", "pending_maintenance").all { field -> it.getInt(field) >= 0 })
        }

    suspend fun options(selected: String?, itemSearch: String, userSearch: String): InventoryChoices {
        val scopedBranch = branch(selected ?: activeBranch)
        val data = call("inventory/options", mapOf("branch_id" to scopedBranch, "item_search" to itemSearch,
            "user_search" to userSearch)) as JSONObject
        fun choices(key: String) = data.getJSONArray(key).rows().map {
            InventoryChoice(uuid(it.getString("id")), it.getString("name"), it.text("code"), it.optBoolean("is_assigned"))
        }
        val branches = choices("branches").filter { activeBranch == null || it.id == activeBranch }
        check(scopedBranch == null || branches.any { it.id == scopedBranch })
        return InventoryChoices(branches, choices("locations"), choices("items"), choices("users"),
            data.optBoolean("more_items"), data.optBoolean("more_users"))
    }

    private suspend fun exact(kind: InventoryRecords, branch: String, record: String): JSONObject =
        records(kind, branch, uuid(record), 1, 0).single().also { check(it.getString("id") == record) }

    suspend fun save(kind: InventoryRecords, selectedBranch: String, form: JSONObject) {
        val branch = requireNotNull(branch(selectedBranch))
        if (kind == InventoryRecords.ITEMS || kind == InventoryRecords.LOCATIONS) {
            require(form.getString("branch_id") == branch)
        }
        val ack = call("inventory/${kind.route}", body = form) as JSONObject
        val saved = exact(kind, branch, ack.getString("id"))
        check(form.keys().asSequence().all { saved.has(it) && saved.text(it) == form.text(it) }) {
            "Kaydedilen envanter alanları doğrulanamadı"
        }
        val expectedStatus = when (kind) {
            InventoryRecords.ITEMS -> "ACTIVE"
            InventoryRecords.ASSIGNMENTS -> "ASSIGNED"
            InventoryRecords.MAINTENANCE -> "PENDING"
            InventoryRecords.LOCATIONS -> null
        }
        if (expectedStatus != null) check(saved.getString("status") == expectedStatus)
    }

    suspend fun finish(kind: InventoryRecords, row: JSONObject) {
        val (action, status, date) = when (kind) {
            InventoryRecords.ASSIGNMENTS -> Triple("return", "RETURNED", "return_date")
            InventoryRecords.MAINTENANCE -> Triple("resolve", "RESOLVED", "resolved_at")
            else -> error("Bu kayıt türünde işlem yok")
        }
        val id = uuid(row.getString("id"))
        val branch = requireNotNull(branch(row.getString("branch_id")))
        val ack = call("inventory/${kind.route}/$id/$action", body = JSONObject()) as JSONObject
        check(ack.getString("id") == id && ack.getString("status") == status && ack.text(date).isNotBlank())
        val saved = exact(kind, branch, id)
        check(saved.getString("item_id") == row.getString("item_id") && saved.getString("status") == status && saved.text(date) == ack.text(date))
    }

    private fun JSONObject.text(key: String) = if (isNull(key)) "" else getString(key)
}
