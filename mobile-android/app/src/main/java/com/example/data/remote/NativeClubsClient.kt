package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal enum class ClubRecords(val path: String, val title: String) {
    CLUBS("", "Kulüpler"), MEMBERS("/memberships", "Üyelikler"), EVENTS("/events", "Etkinlikler"),
    CERTIFICATES("/certificates", "Öğrenci belgeleri")
}

internal data class ClubChoice(val id: String, val name: String, val number: String = "")
internal data class ClubChoices(val branches: List<ClubChoice>, val students: List<ClubChoice>,
    val teachers: List<ClubChoice>, val studentCount: Int)

internal class NativeClubsClient {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private fun id(value: String) = UUID.fromString(value).toString()
    fun ensureCurrent() = scope.ensureCurrent()

    private fun JSONArray.objects() = (0 until length()).map(::getJSONObject)

    private suspend fun call(path: String, query: Map<String, String> = emptyMap(), body: JSONObject? = null,
        method: String = if (body == null) "GET" else "POST", pdf: Boolean = false): Any = scope.io {
        val url = (base + path).toHttpUrl().newBuilder().apply {
            query.filterValues(String::isNotBlank).forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope)
            .method(method, body?.toString()?.toRequestBody("application/json".toMediaType())).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val contents = requireNotNull(response.body)
            if (pdf) {
                check(response.header("Content-Type")?.startsWith("application/pdf") == true)
                val stream = contents.source()
                check(!stream.request(10L * 1024 * 1024 + 1))
                return@use stream.readByteArray().also { check(it.take(5).toByteArray().decodeToString() == "%PDF-") }
            }
            val document = JSONObject(contents.string())
            check(document.getBoolean("success"))
            document.opt("data") ?: JSONObject.NULL
        }
    }

    private fun records(kind: ClubRecords) = "clubs${kind.path}"
    private fun branch(value: String?) = value?.takeUnless { it.isBlank() || it == "all" }?.let(::id).orEmpty()

    suspend fun list(kind: ClubRecords, selectedBranch: String?, page: Int, search: String = "",
        club: String = "", limit: Int = 21): List<JSONObject> {
        val query = mutableMapOf("branch_id" to branch(selectedBranch), "offset" to (page * 20).toString(), "limit" to limit.toString())
        if (kind == ClubRecords.CLUBS || kind == ClubRecords.EVENTS) query["search"] = search
        if (kind == ClubRecords.MEMBERS) query["status"] = "ACTIVE"
        if (club.isNotBlank()) query["club_id"] = id(club)
        return (call(records(kind), query) as JSONArray).objects().onEach { row ->
            id(row.getString("id"))
            if (kind == ClubRecords.CLUBS && branch(selectedBranch).isNotBlank()) check(row.getString("branch_id") == branch(selectedBranch))
            if (club.isNotBlank()) check(row.getString("club_id") == club)
        }
    }

    suspend fun options(selectedBranch: String?, search: String = ""): ClubChoices {
        val value = call("clubs/options", mapOf("branch_id" to branch(selectedBranch), "search" to search, "limit" to "100")) as JSONObject
        fun choices(label: String) = value.getJSONArray(label).objects().map {
            ClubChoice(id(it.getString("id")), it.getString("name"), it.clubText("student_number"))
        }
        return ClubChoices(choices("branches"), choices("students"), choices("teachers"), value.getInt("student_count"))
    }

    private suspend fun record(kind: ClubRecords, identifier: String, selectedBranch: String): JSONObject =
        (call(records(kind), mapOf("branch_id" to branch(selectedBranch), "record_id" to id(identifier), "limit" to "1")) as JSONArray)
            .objects().single().also { check(it.getString("id") == identifier) }

    suspend fun save(kind: ClubRecords, selectedBranch: String, payload: JSONObject, request: UUID, edited: String? = null) {
        check(branch(selectedBranch).isNotBlank())
        check(edited == null || kind == ClubRecords.CLUBS)
        val path = if (kind == ClubRecords.CLUBS) "clubs/clubs" else records(kind)
        val body = JSONObject(payload.toString())
        if (edited == null && kind != ClubRecords.MEMBERS) body.put("request_id", request.toString())
        val ack = if (edited == null) call(path, body = body) else call("$path/${id(edited)}", body = body, method = "PUT")
        val identifier = id((ack as JSONObject).getString("id"))
        check(identifier == (edited ?: request.toString()) || kind == ClubRecords.MEMBERS)
        val saved = record(kind, identifier, selectedBranch)
        for (key in payload.keys()) {
            check(saved.clubText(key) == payload.clubText(key)) { "Kulüp kaydı sunucudan doğrulanamadı." }
        }
        if (kind == ClubRecords.MEMBERS) check(saved.getString("status") == "ACTIVE")
    }

    suspend fun leave(row: JSONObject, selectedBranch: String) {
        val identifier = id(row.getString("id"))
        val ack = call("clubs/memberships/$identifier", method = "DELETE") as JSONObject
        check(ack.getString("id") == identifier && ack.getString("status") == "INACTIVE")
        val saved = record(ClubRecords.MEMBERS, identifier, selectedBranch)
        check(saved.getString("status") == "INACTIVE" && saved.getString("student_id") == row.getString("student_id") &&
            saved.getString("club_id") == row.getString("club_id"))
    }

    suspend fun parentCertificates(student: String, page: Int): List<JSONObject> =
        (call("parent/child/${id(student)}/certificates", mapOf("limit" to "21", "offset" to (20 * page).toString())) as JSONArray)
            .objects()

    suspend fun parentActivities(student: String): List<JSONObject> =
        (call("parent/child/${id(student)}/activities") as JSONArray).objects()

    suspend fun certificatePdf(row: JSONObject, student: String? = null): ByteArray {
        val identifier = id(row.getString("id"))
        val path = if (student == null) "clubs/certificates/$identifier/pdf"
        else "parent/child/${id(student)}/certificates/$identifier/pdf"
        check(row.getString("pdf_url") == "/api/$path")
        return call(path, pdf = true) as ByteArray
    }
}

internal fun JSONObject.clubText(key: String) = if (isNull(key)) "" else getString(key)
