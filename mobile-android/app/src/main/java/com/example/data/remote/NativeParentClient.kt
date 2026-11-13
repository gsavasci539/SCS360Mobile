package com.example.data.remote

import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder
import java.util.UUID

data class NativeParent(val id: String, val organization: String, val fields: String) {
    fun value(key: String) = JSONObject(fields).let { if (it.isNull(key)) "" else it.optString(key) }
    val name get() = "${value("first_name")} ${value("last_name")}".trim()
}

/** Explicit contact/account/student links; no display model or inferred identity is written. */
class NativeParentClient(private val organization: String) {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val fields = listOf("first_name", "last_name", "relation_type", "phone", "sms_phone", "whatsapp_phone", "email", "address", "receive_notifications")
    init { UUID.fromString(organization) }
    fun current() = scope.current()
    private fun id(value: String) = UUID.fromString(value).toString()
    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Any = scope.io {
        val request = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (method != "GET") request.method(method, body?.toString()?.toRequestBody("application/json".toMediaType()))
        http.newCall(request.build()).execute().use {
            if (!it.isSuccessful) throw RegistryHttpException(it.code)
            val value = JSONTokener(it.body?.string() ?: "null").nextValue()
            if (value is JSONObject && value.has("success")) {
                check(value.getBoolean("success")) { "Sunucu işlemi onaylamadı" }; value.opt("data") ?: JSONObject.NULL
            } else value
        }
    }
    private fun parent(row: JSONObject): NativeParent {
        check(row.getString("organization_id") == organization) { "Veli kurumu eşleşmiyor" }
        return NativeParent(id(row.getString("id")), organization, row.toString())
    }
    suspend fun list(search: String = ""): List<NativeParent> {
        val rows = mutableListOf<NativeParent>(); val seen = mutableSetOf<String>()
        while (true) {
            val page = call("parents?skip=${rows.size}&limit=100&search=${URLEncoder.encode(search.trim(), "UTF-8")}") as JSONArray
            repeat(page.length()) { val row = parent(page.getJSONObject(it)); check(seen.add(row.id)); rows.add(row) }
            check(rows.size <= 100000)
            if (page.length() < 100) return rows
        }
    }
    suspend fun get(parentId: String) = parent(call("parents/${id(parentId)}") as JSONObject)
    private fun write(input: JSONObject): JSONObject {
        require(input.keys().asSequence().all { it in fields }) { "Desteklenmeyen veli alanı" }
        val result = JSONObject(input.toString())
        for (key in fields) if (result.has(key) && key != "receive_notifications" && !result.isNull(key)) {
            val value = result.getString(key).trim()
            result.put(key, if (value.isEmpty() && key !in listOf("first_name", "last_name", "relation_type")) JSONObject.NULL else value)
        }
        for (key in listOf("first_name", "last_name")) if (result.has(key)) require(result.getString(key).length in 2..100) { "Ad ve soyad gerekli" }
        if (result.has("relation_type")) require(result.getString("relation_type") in setOf("MOTHER", "FATHER", "GUARDIAN", "OTHER"))
        return result
    }
    suspend fun save(input: JSONObject, original: NativeParent? = null): NativeParent {
        val body = write(input)
        val result = if (original == null) {
            require(body.has("first_name") && body.has("last_name"))
            parent(call("parents", "POST", body.put("organization_id", organization)) as JSONObject)
        } else {
            val fresh = get(original.id)
            val old = JSONObject(original.fields); val actual = JSONObject(fresh.fields)
            for (key in fields) check(old.opt(key)?.toString() == actual.opt(key)?.toString()) { "Veli kaydı değişti; güncel formu yeniden açın" }
            val delta = JSONObject()
            body.keys().forEach { key -> if (body.opt(key)?.toString() != actual.opt(key)?.toString()) delta.put(key, body.get(key)) }
            if (delta.length() == 0) fresh else parent(call("parents/${original.id}", "PUT", delta) as JSONObject)
        }
        body.keys().forEach { key -> check(body.opt(key)?.toString() == JSONObject(result.fields).opt(key)?.toString()) { "Veli kaydı sunucuda doğrulanamadı; listeyi yenileyin" } }
        return result
    }
    suspend fun delete(parentId: String) { call("parents/${id(parentId)}", "DELETE") }
    suspend fun accounts(parentId: String): List<RegistryChoice> {
        val all = mutableListOf<RegistryChoice>(); val seen = mutableSetOf<String>()
        while (true) {
            val page = (call("parents/${id(parentId)}/account-options?offset=${all.size}&limit=100") as JSONObject).getJSONArray("users")
            repeat(page.length()) { val row = page.getJSONObject(it); val key = id(row.getString("id")); check(seen.add(key)); all.add(RegistryChoice(key, "${row.getString("name")} · ${row.getString("email")}")) }
            if (page.length() < 100) return all
            check(all.size <= 100000)
        }
    }
    suspend fun linkAccount(parentId: String, userId: String?) {
        val result = call("parents/${id(parentId)}/account", "PUT", JSONObject().put("user_id", userId?.let(::id) ?: JSONObject.NULL)) as JSONObject
        check(result.getString("parent_id") == parentId && (if (result.isNull("user_id")) null else result.getString("user_id")) == userId)
    }
    suspend fun studentParents(studentId: String): List<String> {
        val rows = call("students/${id(studentId)}/parents") as JSONArray
        return (0 until rows.length()).map { id(rows.getJSONObject(it).getString("id")) }.also { check(it.distinct().size == it.size) }
    }
    suspend fun linkStudent(studentId: String, parentId: String, primary: Boolean, emergency: Boolean) {
        val value = call("students/${id(studentId)}/parents", "POST", JSONObject().put("parent_id", id(parentId)).put("is_primary", primary).put("is_emergency", emergency)) as JSONObject
        check(value.getString("id") == parentId)
    }
    suspend fun unlinkStudent(studentId: String, parentId: String) { call("students/${id(studentId)}/parents/${id(parentId)}", "DELETE") }
}
