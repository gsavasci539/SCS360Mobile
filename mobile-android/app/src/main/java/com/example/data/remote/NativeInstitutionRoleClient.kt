package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class InstitutionRole(
    val id: String, val name: String, val slug: String, val description: String?, val organization: String?, val system: Boolean
)

internal data class InstitutionPermission(
    val id: String, val code: String, val name: String, val module: String, val assignable: Boolean
)

internal class NativeInstitutionRoleClient(organizationId: String) {
    private val organization = uuid(organizationId)
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()

    private fun uuid(value: String) = UUID.fromString(value).toString()
    private fun JSONArray.objects() = (0 until length()).map(::getJSONObject)
    private fun JSONArray.strings() = (0 until length()).map(::getString)
    private fun JSONObject.text(key: String) = if (isNull(key)) null else getString(key)

    private suspend fun call(path: String, query: Map<String, String> = emptyMap(), body: JSONObject? = null, method: String = "POST"): Any = scope.io {
        val url = (base + path).toHttpUrl().newBuilder().apply {
            query.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).apply {
            if (body != null) method(method, body.toString().toRequestBody("application/json".toMediaType()))
        }.build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val envelope = JSONObject(requireNotNull(response.body).string())
            check(envelope.getBoolean("success"))
            envelope.get("data")
        }
    }

    private fun role(row: JSONObject): InstitutionRole {
        val tenant = row.text("organization_id")?.let(::uuid)
        val system = row.getBoolean("is_system")
        check(tenant == organization || tenant == null && system)
        return InstitutionRole(uuid(row.getString("id")), row.getString("name"), row.getString("slug"),
            row.text("description"), tenant, system)
    }

    suspend fun roles(): List<InstitutionRole> = pages("roles").map(::role)

    suspend fun permissions(): List<InstitutionPermission> = pages("permissions").map {
        InstitutionPermission(uuid(it.getString("id")), it.getString("code"), it.getString("name"), it.getString("module"),
            it.getBoolean("assignable"))
    }

    private suspend fun pages(path: String): List<JSONObject> {
        val unique = linkedMapOf<String, JSONObject>()
        var offset = 0
        do {
            val page = (call(path, mapOf("limit" to "100", "offset" to offset.toString())) as JSONArray).objects()
            check(page.size <= 100)
            page.forEach { check(unique.put(uuid(it.getString("id")), it) == null) }
            offset += page.size
        } while (page.size == 100)
        return unique.values.toList()
    }

    suspend fun assigned(roleId: String): Set<String> = (call("roles/${uuid(roleId)}/permissions") as JSONArray).objects()
        .map { uuid(it.getString("id")) }.toSet()

    suspend fun create(name: String, slug: String, description: String?, requestId: UUID): InstitutionRole {
        val id = requestId.toString()
        val form = JSONObject().put("name", name).put("slug", slug).put("description", description ?: JSONObject.NULL).put("request_id", id)
        val acknowledgement = role(call("roles", body = form) as JSONObject)
        check(acknowledgement.id == id)
        val stored = role(call("roles/$id") as JSONObject)
        check(stored.id == id && !stored.system && stored.organization == organization && stored.name == name &&
            stored.slug == slug && stored.description == description)
        return stored
    }

    suspend fun replace(role: InstitutionRole, expected: Set<String>, desired: Set<String>): Set<String> {
        require(role.organization == organization && !role.system)
        val id = uuid(role.id)
        val payload = JSONObject().put("permission_ids", JSONArray(desired.sorted().map(::uuid)))
            .put("expected_permission_ids", JSONArray(expected.sorted().map(::uuid)))
        val acknowledgement = call("roles/$id/permissions", body = payload, method = "PUT") as JSONObject
        check(acknowledgement.getString("role_id") == id &&
            acknowledgement.getJSONArray("permission_ids").strings().map(::uuid).toSet() == desired)
        return assigned(id).also { check(it == desired) }
    }

    suspend fun completeCatalogue(): Int {
        val ack = call("users/role-catalog", body = JSONObject().put("organization_id", organization)) as JSONObject
        check(ack.getString("organization_id") == organization)
        val slugs = ack.getJSONArray("role_slugs").strings().toSet()
        val created = ack.getInt("created_count")
        check(slugs.isNotEmpty() && created in 0..slugs.size)
        val options = call("users/options", mapOf("organization_id" to organization)) as JSONObject
        val persisted = options.getJSONArray("roles").objects().filter { it.text("organization_id") == organization }
        check(persisted.map { it.getString("slug") }.containsAll(slugs))
        persisted.forEach { uuid(it.getString("id")) }
        return created
    }
}
