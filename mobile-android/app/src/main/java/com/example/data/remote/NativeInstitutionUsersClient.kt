package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class UserAccessChoice(val id: String, val name: String, val assignable: Boolean = true)
internal data class InstitutionAccount(
    val id: String, val firstName: String, val lastName: String, val email: String, val phone: String?,
    val position: String?, val department: String?, val status: String, val roleNames: List<String>, val roleSlugs: List<String>
) {
    val name: String get() = "$firstName $lastName"
}

internal data class UserAccess(val roleIds: Set<String>, val branchIds: Set<String>)
internal data class UserAccessOptions(val roles: List<UserAccessChoice>, val branches: List<UserAccessChoice>, val allowAllBranches: Boolean)

internal class NativeInstitutionUsersClient(organizationId: String, selectedBranch: String) {
    private val organization = uuid(organizationId)
    private val branch = selectedBranch.takeUnless { it.isBlank() || it == "all" }?.let(::uuid)
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()

    private fun uuid(value: String) = UUID.fromString(value).toString()
    private fun JSONArray.objects() = (0 until length()).map(::getJSONObject)
    private fun JSONArray.identifiers() = (0 until length()).map { uuid(getString(it)) }.toSet()
    private fun JSONObject.text(key: String) = if (isNull(key)) null else getString(key)

    private suspend fun call(path: String, query: Map<String, String?> = emptyMap(), body: JSONObject? = null, verb: String = "GET"): Any = scope.io {
        val url = (base + path).toHttpUrl().newBuilder().apply {
            query.forEach { (name, value) -> if (!value.isNullOrBlank()) addQueryParameter(name, value) }
        }.build()
        val payload = body?.toString()?.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).method(verb, payload).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val envelope = JSONObject(requireNotNull(response.body).string())
            check(envelope.getBoolean("success"))
            envelope.opt("data") ?: JSONObject.NULL
        }
    }

    private fun row(data: JSONObject): InstitutionAccount {
        check(data.getString("organization_id") == organization && !data.getBoolean("is_super_admin"))
        val roles = data.getJSONArray("roles").objects()
        return InstitutionAccount(uuid(data.getString("id")), data.getString("first_name"), data.getString("last_name"),
            data.getString("email"), data.text("phone"), data.text("position"), data.text("department"),
            data.getString("status"), roles.map { it.getString("name") }, roles.map { it.getString("slug") })
    }

    suspend fun list(page: Int, search: String): Pair<List<InstitutionAccount>, Boolean> {
        val data = call("users", mapOf("page" to page.toString(), "page_size" to "20",
            "search" to search.trim(), "branch_id" to branch)) as JSONObject
        check(data.getInt("page") == page)
        return data.getJSONArray("items").objects().map(::row) to (data.getInt("total_pages") > page)
    }

    suspend fun read(userId: String): InstitutionAccount = row(call("users/${uuid(userId)}") as JSONObject)
        .also { check(it.id == userId) }

    suspend fun access(userId: String): UserAccess {
        val path = "users/${uuid(userId)}"
        fun identifiers(data: Any) = (data as JSONArray).objects().map { uuid(it.getString("id")) }.toSet()
        return UserAccess(identifiers(call("$path/roles")), identifiers(call("$path/branches")))
    }

    suspend fun options(): UserAccessOptions {
        val data = call("users/options", mapOf("organization_id" to organization)) as JSONObject
        val roles = data.getJSONArray("roles").objects().map {
            check(it.text("organization_id") in setOf(null, organization))
            UserAccessChoice(uuid(it.getString("id")), it.getString("name"), it.getBoolean("assignable"))
        }
        val branches = data.getJSONArray("branches").objects().map { UserAccessChoice(uuid(it.getString("id")), it.getString("name")) }
        return UserAccessOptions(roles, branches, data.getBoolean("allows_institution_scope"))
    }

    suspend fun replaceAccess(userId: String, original: UserAccess, desired: UserAccess) {
        val id = uuid(userId)
        val form = JSONObject().put("role_ids", JSONArray(desired.roleIds.sorted().map(::uuid)))
            .put("branch_ids", JSONArray(desired.branchIds.sorted().map(::uuid)))
            .put("expected_role_ids", JSONArray(original.roleIds.sorted().map(::uuid)))
            .put("expected_branch_ids", JSONArray(original.branchIds.sorted().map(::uuid)))
        val ack = call("users/$id/access", body = form, verb = "PUT") as JSONObject
        check(ack.getString("user_id") == id && ack.getJSONArray("role_ids").identifiers() == desired.roleIds &&
            ack.getJSONArray("branch_ids").identifiers() == desired.branchIds)
        check(access(id) == desired) { "Kalıcı kullanıcı kapsamı doğrulanamadı" }
    }

    suspend fun save(profile: JSONObject, userId: String?): InstitutionAccount {
        val expected = JSONObject(profile.toString())
        val id = userId?.let(::uuid)
        val path = if (id == null) "users" else "users/$id"
        if (id == null) expected.put("organization_id", organization)
        val ack = call(path, body = expected, verb = if (id == null) "POST" else "PUT") as JSONObject
        val storedId = uuid(ack.getString("id"))
        check(id == null || id == storedId)
        check(ack.getString("organization_id") == organization && !ack.getBoolean("is_super_admin"))
        val saved = call("users/$storedId") as JSONObject
        check(saved.getString("id") == storedId)
        check(expected.keys().asSequence().filter { it !in setOf("password", "branch_id") }.all { key ->
            saved.has(key) && saved.text(key) == expected.text(key)
        })
        if (id == null && !expected.isNull("branch_id")) {
            check(access(storedId).branchIds == setOf(expected.getString("branch_id")))
        }
        return row(saved)
    }

    suspend fun deactivate(userId: String) {
        val id = uuid(userId)
        call("users/$id", verb = "DELETE")
        try {
            read(id)
        } catch (failure: RegistryHttpException) {
            if (failure.code == 404) return
            throw failure
        }
        error("Kullanıcının pasife alınması doğrulanamadı")
    }
}
