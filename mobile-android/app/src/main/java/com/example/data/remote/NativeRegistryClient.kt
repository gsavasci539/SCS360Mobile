package com.example.data.remote

import com.example.data.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

data class RegistryChoice(val id: String, val name: String)
class RegistryHttpException(val code: Int) : IOException("İstek reddedildi (HTTP $code)")
class RegistryRecordChangedException : IllegalStateException("Kayıt değişmiş. Güncel listeyi inceleyip formu yeniden açın; taslağınız otomatik uygulanmadı.")

/** Canonical wire contracts; display models are never serialized as mutations. */
class NativeRegistryClient {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val client = ApiClientProvider.createOkHttpClient()
    private suspend fun request(path: String, method: String = "GET", body: JSONObject? = null): Any = scope.io {
        val builder = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (method != "GET") builder.method(method, body?.toString()?.toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use { reply ->
            if (!reply.isSuccessful) throw RegistryHttpException(reply.code)
            val result = JSONTokener(reply.body?.string() ?: "null").nextValue()
            if (result is JSONObject && result.has("success")) {
                check(result.getBoolean("success")) { "Sunucu işlemi onaylamadı" }
                result.opt("data") ?: JSONObject.NULL
            } else result
        }
    }
    private suspend fun pages(path: String, envelope: (JSONObject) -> Unit = {}): List<JSONObject> {
        val rows = mutableListOf<JSONObject>(); val seen = mutableSetOf<String>()
        var page = 1; var total: Int? = null
        while (true) {
            val reply = request(path + (if ('?' in path) "&" else "?") + "page=$page&page_size=100")
            val items: JSONArray
            if (reply is JSONArray) { items = reply; total = reply.length() }
            else {
                val value = reply as JSONObject; envelope(value); items = value.getJSONArray("items")
                val count = value.getInt("total")
                check(count in 0..100000 && (total == null || total == count)) { "Liste değişti; yeniden yükleyin" }
                total = count
            }
            for (i in 0 until items.length()) {
                val row = items.getJSONObject(i)
                check(seen.add(uuid(row.getString("id")))) { "Liste sayfaları tutarsız" }; rows.add(row)
            }
            check(rows.size <= total!!)
            if (rows.size == total) return rows
            check(items.length() > 0) { "Liste tamamlanamadı" }; page++
        }
    }
    suspend fun branches() = pages("branches").map(::branch)
    suspend fun classes(branchId: String?) = pages("classes" + query("branch_id" to branchId)).map(::classroom).also { rows ->
        check(branchId == null || rows.all { it.branch_id == branchId })
    }
    suspend fun students(branchId: String?, classId: String?, search: String?) = pages("students" +
        query("branch_id" to branchId, "class_id" to classId, "search" to search)).map(::student).also { rows ->
        check(branchId == null || rows.all { it.branch_id == branchId }); check(classId == null || rows.all { it.class_id == classId })
    }
    suspend fun teachers(organizationId: String, branchId: String) = pages("classes/teacher-options" +
        query("organization_id" to uuid(organizationId), "branch_id" to uuid(branchId))).map {
        RegistryChoice(uuid(it.getString("id")), it.getString("name"))
    }
    suspend fun studentBranches(org: String, currentStudent: String? = null, selected: String? = null) = choices("students/branch-options" +
        query("organization_id" to uuid(org), "student_id" to currentStudent, "selected_id" to selected))
    suspend fun studentClasses(org: String, branch: String, currentStudent: String? = null, selected: String? = null) = choices("students/class-options" +
        query("organization_id" to uuid(org), "branch_id" to uuid(branch), "student_id" to currentStudent, "selected_id" to selected))
    private suspend fun choices(path: String): List<RegistryChoice> {
        var selected: JSONObject? = null
        val rows = pages(path) { if (!it.isNull("selected")) selected = it.getJSONObject("selected") }.toMutableList()
        selected?.let { choice -> if (rows.none { it.getString("id") == choice.getString("id") }) rows.add(choice) }
        return rows.map { RegistryChoice(uuid(it.getString("id")), it.getString("name")) }
    }
    suspend fun createStudent(draft: Student): Student {
        rejectStudentExtras(draft, Student())
        val body = studentWrite(draft, creating = true).put("organization_id", uuid(draft.organization_id))
        return student(request("students", "POST", body) as JSONObject)
    }
    suspend fun updateStudent(draft: Student): Student {
        val original = student(request("students/${uuid(draft.id)}/form") as JSONObject)
        require(draft.organization_id == original.organization_id) { "Öğrencinin kurumu değiştirilemez" }
        if (draft.registry_snapshot == null || draft.registry_snapshot != original.registry_snapshot) throw RegistryRecordChangedException()
        rejectStudentExtras(draft, original)
        val body = diff(studentWrite(draft, false), studentWrite(original, false))
        if (body.has("enrollment_date")) require(!body.isNull("enrollment_date")) { "Kayıt tarihi boş bırakılamaz" }
        return if (body.length() == 0) original else student(request("students/${uuid(draft.id)}", "PUT", body) as JSONObject)
    }
    suspend fun deleteStudent(id: String) { request("students/${uuid(id)}", "DELETE") }
    suspend fun unassignStudent(id: String) { request("students/${uuid(id)}", "PUT", JSONObject().put("class_id", JSONObject.NULL)) }
    suspend fun assignStudent(id: String, classId: String) {
        request("classes/${uuid(classId)}/assign-students", "POST", JSONObject().put("student_ids", JSONArray().put(uuid(id))))
    }
    suspend fun createClass(draft: ClassGroup): ClassGroup {
        require(draft.grade_level.isBlank() && draft.room_number.isBlank()) { "Kademe/oda bu API sözleşmesinde desteklenmiyor" }
        return classroom(request("classes", "POST", classWrite(draft).put("organization_id", uuid(draft.organization_id))
            .put("branch_id", uuid(draft.branch_id))) as JSONObject)
    }
    suspend fun updateClass(draft: ClassGroup): ClassGroup {
        val original = classroom(request("classes/${uuid(draft.id)}") as JSONObject)
        require(draft.organization_id == original.organization_id && draft.branch_id == original.branch_id)
        if (draft.registry_snapshot == null || draft.registry_snapshot != original.registry_snapshot) throw RegistryRecordChangedException()
        require(draft.grade_level == original.grade_level && draft.room_number == original.room_number)
        val body = diff(classWrite(draft), classWrite(original))
        return if (body.length() == 0) original else classroom(request("classes/${uuid(draft.id)}", "PUT", body) as JSONObject)
    }
    suspend fun deleteClass(id: String) {
        val path = "classes/${uuid(id)}"
        val current = request(path) as JSONObject
        require(current.getInt("student_count") == 0) { "Sınıfta öğrenci var; önce öğrencilerin sınıf atamalarını değiştirin" }
        request(path, "DELETE")
    }

    private fun studentWrite(s: Student, creating: Boolean, validate: Boolean = true): JSONObject {
        if (validate) {
        require(s.first_name.trim().length in 1..100 && s.last_name.trim().length in 1..100) { "Ad ve soyad gerekli" }
        require(s.status in setOf("ACTIVE", "PASSIVE", "GRADUATED", "LEFT")) { "Geçerli öğrenci durumu seçin" }
        require(s.gender.isBlank() || s.gender in setOf("MALE", "FEMALE", "OTHER")) { "Geçerli cinsiyet seçin" }
        require(s.student_number.length <= 50)
        }
        return JSONObject().put("branch_id", uuid(s.branch_id)).nullable("class_id", s.class_id.takeIf { it.isNotBlank() }?.let(::uuid))
            .put("first_name", s.first_name.trim()).put("last_name", s.last_name.trim()).put("status", s.status)
            .nullable("student_number", s.student_number.trim().ifBlank { null }).nullable("gender", s.gender.ifBlank { null })
            .nullable("birth_date", day(s.birth_date)).nullable("address", s.address?.trim()?.ifBlank { null })
            .nullable("notes", s.notes?.trim()?.ifBlank { null }).nullable("health_notes", s.health_notes?.trim()?.ifBlank { null })
            .also { if (!creating || !s.enrollment_date.isNullOrBlank()) it.nullable("enrollment_date", day(s.enrollment_date)) }
    }
    private fun classWrite(c: ClassGroup, validate: Boolean = true): JSONObject {
        if (validate) {
        require(c.name.trim().length in 1..150 && c.capacity > 0) { "Sınıf adı ve pozitif kapasite gerekli" }
        require(c.code.length <= 50 && c.academic_year.length <= 20)
        }
        return JSONObject().put("name", c.name.trim()).put("capacity", c.capacity).put("is_active", c.is_active)
            .nullable("code", c.code.trim().ifBlank { null }).nullable("description", c.description?.trim()?.ifBlank { null })
            .nullable("academic_year", c.academic_year.trim().ifBlank { null }).nullable("teacher_id", c.teacher_id?.let(::uuid))
    }
    private fun rejectStudentExtras(draft: Student, original: Student) {
        require(draft.parent_name == original.parent_name && draft.parent_phone == original.parent_phone &&
            draft.parent_email == original.parent_email && draft.allergies_or_diet == original.allergies_or_diet &&
            draft.blood_type == original.blood_type && draft.tuition_balance == original.tuition_balance) {
            "Veli, ayrı sağlık alanları ve tahsilatlar kendi işlemlerinden kaydedilmelidir"
        }
    }
    private fun student(j: JSONObject): Student = Student(
        id = uuid(j.getString("id")), organization_id = uuid(j.getString("organization_id")), branch_id = uuid(j.getString("branch_id")),
        class_id = nullable(j, "class_id")?.let(::uuid).orEmpty(), student_number = nullable(j, "student_number").orEmpty(),
        first_name = j.getString("first_name"), last_name = j.getString("last_name"), gender = nullable(j, "gender").orEmpty(),
        birth_date = day(nullable(j, "birth_date") ?: nullable(j, "date_of_birth")).orEmpty(),
        enrollment_date = day(j.getString("enrollment_date")), status = j.getString("status"),
        address = nullable(j, "address"), notes = nullable(j, "notes"), health_notes = nullable(j, "health_notes")
    ).let { it.copy(registry_snapshot = fingerprint(studentWrite(it, false, false).put("id", it.id).put("organization_id", it.organization_id))) }
    private fun classroom(j: JSONObject): ClassGroup = ClassGroup(
        id = uuid(j.getString("id")), organization_id = uuid(j.getString("organization_id")), branch_id = uuid(j.getString("branch_id")),
        name = j.getString("name"), code = nullable(j, "code").orEmpty(), capacity = j.getInt("capacity"),
        academic_year = nullable(j, "academic_year").orEmpty(), description = nullable(j, "description"),
        teacher_id = nullable(j, "teacher_id")?.let(::uuid), teacher_name = nullable(j, "teacher_name").orEmpty(),
        student_count = j.optInt("student_count", 0), is_active = j.getBoolean("is_active")
    ).let { it.copy(registry_snapshot = fingerprint(classWrite(it, false).put("id", it.id).put("organization_id", it.organization_id).put("branch_id", it.branch_id))) }
    private fun branch(j: JSONObject) = Branch(id = uuid(j.getString("id")), organization_id = uuid(j.getString("organization_id")),
        name = j.getString("name"), code = nullable(j, "code").orEmpty(), address = nullable(j, "address").orEmpty(),
        phone = nullable(j, "phone").orEmpty(), email = nullable(j, "email"), is_active = j.getBoolean("is_active"))
    private fun diff(after: JSONObject, before: JSONObject) = JSONObject().apply {
        after.keys().forEach { key -> if (after.get(key) != before.opt(key)) put(key, after.get(key)) }
    }
    private fun fingerprint(fields: JSONObject): String {
        val ordered = JSONArray()
        fields.keys().asSequence().sorted().forEach { ordered.put(JSONObject().put(it, fields.get(it))) }
        return java.security.MessageDigest.getInstance("SHA-256").digest(ordered.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
    private fun JSONObject.nullable(key: String, value: String?) = put(key, value ?: JSONObject.NULL)
    private fun nullable(j: JSONObject, key: String) = if (j.isNull(key)) null else j.getString(key)
    private fun day(value: String?) = value?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it).toString() }
    private fun uuid(value: String) = UUID.fromString(value).toString()
    private fun query(vararg values: Pair<String, String?>): String {
        val pairs = values.filter { !it.second.isNullOrBlank() }.map { (k, v) ->
            if (k.endsWith("_id")) uuid(v!!)
            "$k=" + java.net.URLEncoder.encode(v, "UTF-8")
        }
        return if (pairs.isEmpty()) "" else pairs.joinToString("&", "?")
    }
}
