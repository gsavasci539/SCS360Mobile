package com.example.data.remote

import android.content.Context
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID

class AttendanceHttpException(val code: Int) : java.io.IOException("Yoklama isteği reddedildi (HTTP $code)")

/** One verified class/day snapshot; no fabricated presence, measurement or notification. */
class NativeAttendanceEditor(context: Context, actorId: String, val classId: String, val day: String) {
    private val scope = NativeRequestScope.bound() ?: ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val workspace = ApiClientProvider.interceptor.workspaceOrganizationId.orEmpty()
    private val actor = UUID.fromString(actorId).toString()
    private val client = ApiClientProvider.createOkHttpClient()
    private val prefs = context.getSharedPreferences("attendance_requests_v1", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private var snapshot: List<AttendanceRecord>? = null
    private var pendingBody: String? = null
    private var pendingCanonical: String? = null
    val hasPending: Boolean get() = pendingBody != null
    val ready: Boolean get() = snapshot != null && current()
    fun current() = scope.current()

    init { UUID.fromString(classId); require(LocalDate.parse(day).toString() == day) }

    private suspend fun request(path: String, body: String? = null): Any = scope.io {
        val builder = Request.Builder().url(base + path).tag(NativeRequestScope::class.java, scope)
        if (body != null) builder.put(body.toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use { reply ->
            if (!reply.isSuccessful) throw AttendanceHttpException(reply.code)
            val value = JSONTokener(reply.body?.string() ?: "null").nextValue()
            if (value is JSONObject && value.has("success")) {
                check(value.getBoolean("success") && value.has("data")) { "Yoklama yanıtı doğrulanamadı" }
                value.get("data")
            } else value
        }
    }

    suspend fun load(): List<AttendanceRecord> = mutex.withLock {
        check(!hasPending) { "Önce bekleyen kaydetme isteğinin sonucunu kontrol edin" }
        snapshot = null
        val roster = request("classes/$classId/students") as JSONArray
        require(roster.length() <= 5000) { "Sınıf listesi tek yoklama sınırını aşıyor" }
        val rows = linkedMapOf<String, AttendanceRecord>()
        for (i in 0 until roster.length()) {
            val student = roster.getJSONObject(i)
            val id = UUID.fromString(student.getString("id")).toString()
            check(student.getString("class_id") == classId && id !in rows) { "Sınıf listesi eşleşmiyor" }
            rows[id] = AttendanceRecord(student_id = id, student_name = name(student, id), class_id = classId, date = day)
        }
        var page = 1
        var total: Int? = null
        val seen = mutableSetOf<String>()
        while (true) {
            val result = request("attendance?class_id=$classId&attendance_date=$day&page=$page&page_size=100") as JSONObject
            val count = result.getInt("total")
            check(count in 0..5000 && (total == null || count == total)) { "Yoklama listesi değişti; yeniden yükleyin" }
            total = count
            val items = result.getJSONArray("items")
            for (i in 0 until items.length()) {
                val row = items.getJSONObject(i)
                val id = UUID.fromString(row.getString("student_id")).toString()
                check(seen.add(id) && row.getString("class_id") == classId && row.getString("attendance_date") == day)
                val version = row.getString("version")
                check(VERSION.matches(version) && row.getString("status") in STATUSES)
                // Historical rows remain editable even if the student has since moved class.
                rows[id] = AttendanceRecord(id = UUID.fromString(row.getString("id")).toString(), student_id = id,
                    student_name = rows[id]?.student_name ?: name(row, id), class_id = classId, date = day,
                    status = row.getString("status"), note = nullable(row, "notes"), version = version,
                    check_in_time = nullable(row, "check_in_time"))
            }
            check(seen.size <= count)
            if (seen.size == count) break
            check(items.length() > 0) { "Yoklama sayfalaması tamamlanamadı" }
            page++
        }
        scope.ensureCurrent()
        rows.values.toList().also { snapshot = it }
    }

    suspend fun save(draft: List<AttendanceRecord>): JSONObject = mutex.withLock {
        scope.ensureCurrent()
        val original = snapshot ?: error("Önce sınıf ve günün yoklamasını yükleyin")
        check(draft.map { it.student_id }.toSet() == original.map { it.student_id }.toSet() && draft.size == original.size)
        val originalById = original.associateBy { it.student_id }
        check(draft.all { row -> originalById[row.student_id]?.let {
            row.id == it.id && row.version == it.version && row.class_id == classId && row.date == day
        } == true }) { "Taslak başka bir yoklama sürümüne ait; güncel kayıtları inceleyin" }
        val versions = original.associate { it.student_id to it.version }
        val edited = draft.sortedBy { it.student_id }.mapNotNull { row ->
            if (row.status == "UNRECORDED") {
                require(versions[row.student_id] == null && row.note.isNullOrBlank()) { "Not yazılan öğrencinin durumunu seçin" }
                null
            } else {
                require(row.status in STATUSES && (row.note?.length ?: 0) <= 5000)
                "{\"student_id\":${JSONObject.quote(row.student_id)},\"status\":${JSONObject.quote(row.status)}," +
                    "\"notes\":${quoted(row.note)},\"expected_version\":${quoted(versions[row.student_id])}}"
            }
        }
        require(edited.isNotEmpty()) { "En az bir öğrencinin durumunu işaretleyin" }
        val canonical = "{\"class_id\":${JSONObject.quote(classId)},\"attendance_date\":${JSONObject.quote(day)},\"records\":[${edited.joinToString(",")}] }"
        check(pendingCanonical == null || pendingCanonical == canonical) { "Bekleyen istek değiştirilemez; aynı isteği yeniden deneyin" }
        if (pendingBody == null) {
            val key = MessageDigest.getInstance("SHA-256").digest("$base|$actor|$workspace|$canonical".toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(java.util.Locale.ROOT, it.toInt() and 255) }
            val nonce = synchronized(nonceLock) {
                prefs.getString(key, null)?.also { UUID.fromString(it) } ?: UUID.randomUUID().toString().also {
                    check(prefs.edit().putString(key, it).commit()) { "Yoklama istek kimliği saklanamadı" }
                }
            }
            pendingBody = JSONObject(canonical).put("request_id", nonce).toString()
            pendingCanonical = canonical
        }
        try {
            val response = request("attendance/bulk", pendingBody) as JSONObject
            val expected = JSONObject(pendingBody!!).getJSONArray("records")
            check(response.getInt("created") + response.getInt("updated") + response.getInt("unchanged") == expected.length())
            val items = response.getJSONArray("items")
            val wanted = (0 until expected.length()).map { expected.getJSONObject(it).getString("student_id") }.toSet()
            val received = (0 until items.length()).map {
                val row = items.getJSONObject(it)
                UUID.fromString(row.getString("id"))
                check(VERSION.matches(row.getString("version")))
                row.getString("student_id")
            }
            check(received.size == wanted.size && received.toSet() == wanted)
            scope.ensureCurrent()
            pendingBody = null; pendingCanonical = null; snapshot = null
            response
        } catch (failure: AttendanceHttpException) {
            if (failure.code in setOf(400, 401, 403, 404, 405, 409, 413, 422)) {
                pendingBody = null; pendingCanonical = null
                if (failure.code == 409) snapshot = null
            }
            throw failure
        }
    }

    companion object {
        val STATUSES = AttendanceStatus.entries.map { it.name }.toSet()
        private val VERSION = Regex("[0-9a-f]{64}")
        private val nonceLock = Any()
        private fun nullable(row: JSONObject, key: String) = if (row.isNull(key)) null else row.optString(key)
        private fun quoted(value: String?) = value?.let(JSONObject::quote) ?: "null"
        private fun name(row: JSONObject, id: String) = listOfNotNull(nullable(row, "first_name"), nullable(row, "last_name"))
            .filter { it.isNotBlank() }.joinToString(" ").ifBlank { id }
    }
}
