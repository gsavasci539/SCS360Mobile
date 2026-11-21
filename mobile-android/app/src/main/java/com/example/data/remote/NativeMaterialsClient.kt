package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

internal data class MaterialClass(val id: String, val name: String)
internal data class MaterialDraft(val title: String = "", val type: String = "PDF", val visibility: String = "PUBLIC",
    val classId: String = "", val description: String = "", val topic: String = "",
    val start: String = "", val end: String = "")
internal data class RecordedMaterial(val id: String, val fileName: String, val draft: MaterialDraft,
    val className: String, val size: Long?, val downloads: Int?, val canWrite: Boolean,
    val teacherId: String = "", val subjectId: String = "")
internal const val MATERIALS_FILE_LIMIT = 10L * 1024 * 1024

internal class NativeMaterialsClient(selectedBranch: String, private val student: String? = null) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val http = ApiClientProvider.createOkHttpClient()
    private val base = ApiClientProvider.activeBaseUrl
    private val branch = selectedBranch.takeUnless { student != null || it == "all" || it.isBlank() }?.let(::id)
    val requiresClass get() = branch != null
    fun ensureCurrent() = scope.ensureCurrent()

    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else getString(key)
    private fun JSONArray.objects() = (0 until length()).map(::getJSONObject)
    private fun filePath(material: String) = student?.let { "parent/child/${id(it)}/materials/${id(material)}/download" }
        ?: "materials/${id(material)}/file"

    private suspend fun call(path: String, query: Map<String, String> = emptyMap(), body: RequestBody? = null,
        method: String = if (body == null) "GET" else "POST", binary: Boolean = false): Any = scope.io {
        val url = (base + path).toHttpUrl().newBuilder().apply {
            branch?.let { addQueryParameter("branch_id", it) }
            query.forEach { (name, value) -> addQueryParameter(name, value) }
        }.build()
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).method(method, body).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val bodyData = requireNotNull(response.body)
            if (binary) {
                val stream = bodyData.source()
                check(!stream.request(MATERIALS_FILE_LIMIT + 1))
                return@use stream.readByteArray().also { check(it.isNotEmpty()) }
            }
            val responseJson = JSONObject(bodyData.string())
            check(responseJson.getBoolean("success"))
            responseJson.opt("data") ?: JSONObject.NULL
        }
    }

    private fun material(row: JSONObject, actor: String, broadWrite: Boolean): RecordedMaterial {
        val identifier = id(row.getString("id"))
        check(row.getString("file_url") == "/api/${filePath(identifier)}")
        val classId = row.text("class_id")
        val teacher = row.text("teacher_id")
        return RecordedMaterial(identifier, row.text("file_name"), MaterialDraft(row.getString("title"),
            row.getString(if (student == null) "material_type" else "type"), row.text("visibility"), classId,
            row.text("description"), row.text(if (student == null) "unit_topic" else "subject"),
            row.text("visible_from"), row.text("visible_until")), row.text("class_name"),
            row.text("file_size").toLongOrNull(), row.text("download_count").toIntOrNull(), student == null &&
                (branch == null || classId.isNotBlank()) && (broadWrite || teacher == actor || row.text("created_by") == actor),
            teacher, row.text("subject_id"))
    }

    suspend fun list(page: Int, actor: String, broadWrite: Boolean): List<RecordedMaterial> {
        val childPath = student?.let { "parent/child/${id(it)}/materials" }
        val query = if (student == null) mapOf("limit" to "21", "offset" to (page * 20).toString()) else emptyMap()
        return (call(childPath ?: "materials", query) as JSONArray).objects().map { material(it, actor, broadWrite) }
    }

    suspend fun classes(): List<MaterialClass> = (call("materials/classes") as JSONArray).objects().map {
        check(branch == null || it.getString("branch_id") == branch)
        MaterialClass(id(it.getString("id")), it.getString("name"))
    }

    private fun query(draft: MaterialDraft): Map<String, String> = mapOf(
        "title" to draft.title, "material_type" to draft.type, "visibility" to draft.visibility,
        "class_id" to draft.classId, "description" to draft.description, "unit_topic" to draft.topic,
        "visible_from" to draft.start, "visible_until" to draft.end,
    ).filterValues(String::isNotBlank)

    private suspend fun exact(identifier: String): RecordedMaterial? =
        (call("materials", mapOf("record_id" to id(identifier), "limit" to "1")) as JSONArray).objects()
            .singleOrNull()?.let { material(it, "", false) }?.also { check(it.id == identifier) }

    suspend fun upload(draft: MaterialDraft, file: HomeworkFile, request: UUID) {
        check(student == null && file.bytes.isNotEmpty() && file.bytes.size.toLong() <= MATERIALS_FILE_LIMIT)
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.bytes.toRequestBody()).build()
        val expected = request.toString()
        val ack = call("materials/upload", query(draft) + ("request_id" to expected), body) as JSONObject
        check(ack.getString("id") == expected)
        val saved = checkNotNull(exact(expected))
        check(saved.draft == draft && saved.fileName == file.name && saved.size == file.bytes.size.toLong())
    }

    suspend fun update(draft: MaterialDraft, record: RecordedMaterial) {
        check(student == null && draft.type == record.draft.type)
        val fields = JSONObject()
        for ((name, value) in query(draft)) fields.put(name, value)
        fields.put("teacher_id", record.teacherId.ifBlank { null } ?: JSONObject.NULL)
        fields.put("subject_id", record.subjectId.ifBlank { null } ?: JSONObject.NULL)
        val body = fields.toString().toRequestBody("application/json".toMediaType())
        val ack = call("materials/${id(record.id)}", body = body, method = "PUT") as JSONObject
        check(ack.getString("id") == record.id && exact(record.id)?.draft == draft)
    }

    suspend fun delete(identifier: String) {
        check(student == null)
        call("materials/${id(identifier)}", method = "DELETE")
        check(exact(identifier) == null)
    }

    suspend fun download(record: RecordedMaterial): ByteArray = (call(filePath(record.id), binary = true) as ByteArray).also {
        check(record.size == null || it.size.toLong() == record.size)
    }
}

internal fun validMaterialDates(start: String, end: String): Boolean = runCatching {
    val from = start.takeIf(String::isNotBlank)?.let(LocalDate::parse)
    val until = end.takeIf(String::isNotBlank)?.let(LocalDate::parse)
    from == null || until == null || !until.isBefore(from)
}.getOrDefault(false)
