package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

internal data class TransportRoute(val id: String, val branch: String, val branchName: String, val name: String,
    val number: String, val plate: String, val driver: String, val capacity: Int, val status: String, val description: String,
    val driverId: String, val updatedAt: String)
internal data class TransportStop(val id: String, val name: String, val order: Int, val eta: String,
    val latitude: String, val longitude: String, val version: String)
internal data class TransportAttendance(val id: String, val studentId: String, val name: String, val day: LocalDate,
    val pickup: String, val dropoff: String, val status: String, val stop: String, val notes: String)
internal data class TransportTelemetry(val time: String, val latitude: Double?, val longitude: Double?, val speed: Double?)
internal data class TransportStudent(val id: String, val name: String, val active: Boolean)
internal data class TransportOptions(val today: LocalDate, val students: List<TransportStudent>, val more: Boolean)
internal data class TransportChoice(val id: String, val name: String)
internal data class TransportChoices(val branches: List<TransportChoice>, val drivers: List<TransportChoice>, val moreDrivers: Boolean)
internal data class TransportRouteDraft(val branch: String, val driver: String = "", val name: String = "", val number: String = "",
    val plate: String = "", val capacity: String = "40", val status: String = "ACTIVE", val description: String = "")
internal data class TransportStopDraft(val name: String = "", val order: String = "", val latitude: String = "",
    val longitude: String = "", val arrival: String = "")
internal data class TransportSummary(val routes: Int, val active: Int, val capacity: Long)

internal class NativeTransportClient(selectedBranch: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val base = ApiClientProvider.activeBaseUrl
    private val http = ApiClientProvider.createOkHttpClient()
    private val branch = selectedBranch.takeUnless { it == "all" || it.isBlank() }?.let(::id)
    val fixedBranch get() = branch

    private fun id(value: String) = UUID.fromString(value).toString()
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else getString(key)
    private fun JSONArray.rows() = (0 until length()).map(::getJSONObject)

    private suspend fun call(path: String, query: Map<String, String?> = emptyMap(), body: JSONObject? = null,
        method: String = if (body == null) "GET" else "POST"): Any = scope.io {
        val url = (base + "bus/" + path).toHttpUrl().newBuilder().apply {
            query.forEach { (key, value) -> value?.let { addQueryParameter(key, it) } }
        }.build()
        val wire = body?.toString()?.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).tag(NativeRequestScope::class.java, scope).method(method, wire).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RegistryHttpException(response.code)
            val decoded = JSONTokener(response.body?.string().orEmpty()).nextValue()
            if (decoded is JSONObject && decoded.has("success")) {
                check(decoded.getBoolean("success")) { "Servis sunucu işlemi doğrulanamadı." }
                decoded.get("data")
            } else decoded
        }
    }

    private fun route(row: JSONObject): TransportRoute {
        val result = TransportRoute(id(row.getString("id")), id(row.getString("branch_id")), row.text("branch_name"),
            row.getString("route_name"), row.text("route_number"), row.text("vehicle_plate"), row.text("driver_name"),
            row.getInt("capacity"), row.getString("status"), row.text("description"), row.text("driver_id"), row.text("updated_at"))
        check(branch == null || result.branch == branch)
        return result
    }

    suspend fun routes(search: String, limit: Int, offset: Int): List<TransportRoute> =
        (call("routes", mapOf("branch_id" to branch, "search" to search, "limit" to limit.toString(), "offset" to offset.toString()))
            as JSONArray).rows().map(::route)

    suspend fun detail(selected: String): TransportRoute =
        route(call("routes/${id(selected)}") as JSONObject).also { check(it.id == selected) }

    suspend fun stops(selected: TransportRoute, limit: Int, offset: Int, record: String? = null): List<TransportStop> =
        (call("stops", mapOf("route_id" to id(selected.id), "limit" to limit.toString(), "offset" to offset.toString(),
            "record_id" to record?.let(::id))) as JSONArray)
            .rows().map {
                check(it.getString("route_id") == selected.id && (record == null || it.getString("id") == record))
                TransportStop(id(it.getString("id")), it.getString("stop_name"), it.getInt("stop_order"), it.text("estimated_arrival"),
                    it.text("latitude"), it.text("longitude"), it.text("version"))
            }

    suspend fun summary(): TransportSummary {
        val result = call("summary", mapOf("branch_id" to branch)) as JSONObject
        return TransportSummary(result.getInt("total_routes"), result.getInt("active_routes"), result.getLong("total_capacity"))
    }

    suspend fun routeChoices(branchId: String, driverSearch: String, driverId: String, routeId: String?): TransportChoices {
        val chosen = branchId.ifBlank { null }?.let(::id)
        check(branch == null || chosen == branch)
        val result = call("options", mapOf("branch_id" to chosen, "driver_search" to driverSearch,
            "route_id" to routeId?.let(::id), "driver_id" to driverId.ifBlank { null }?.let(::id))) as JSONObject
        if (chosen != null) check(result.getString("branch_id") == chosen)
        fun map(key: String) = result.getJSONArray(key).rows().map { TransportChoice(id(it.getString("id")), it.getString("name")) }
        return TransportChoices(map("branches").filter { branch == null || it.id == branch }, map("drivers"), result.getBoolean("more_drivers"))
    }

    suspend fun saveRoute(draft: TransportRouteDraft, existing: TransportRoute?, request: UUID): TransportRoute {
        check(branch == null || draft.branch == branch)
        val body = JSONObject().put("branch_id", id(draft.branch)).put("driver_id", draft.driver.ifBlank { null }?.let(::id) ?: JSONObject.NULL)
            .put("route_name", draft.name).put("route_number", draft.number.ifBlank { null } ?: JSONObject.NULL)
            .put("vehicle_plate", draft.plate.ifBlank { null } ?: JSONObject.NULL).put("capacity", draft.capacity.toInt())
            .put("description", draft.description.ifBlank { null } ?: JSONObject.NULL).put("status", draft.status)
            .put("request_id", request.toString())
        val path = if (existing == null) "routes" else "routes/${id(existing.id)}"
        if (existing != null) {
            check(existing.updatedAt.isNotBlank())
            body.put("expected_updated_at", existing.updatedAt)
        }
        val ack = call(path, body = body, method = if (existing == null) "POST" else "PUT") as JSONObject
        val expected = existing?.id ?: request.toString()
        check(ack.getString("id") == expected)
        val stored = detail(expected)
        check(stored.branch == draft.branch && stored.name == draft.name && stored.number == draft.number && stored.plate == draft.plate &&
            stored.driverId == draft.driver && stored.capacity == draft.capacity.toInt() && stored.description == draft.description &&
            stored.status == draft.status && (existing == null || stored.updatedAt == ack.getString("updated_at")))
        return stored
    }

    suspend fun saveStop(selected: TransportRoute, draft: TransportStopDraft, existing: TransportStop?, request: UUID) {
        detail(selected.id)
        fun coordinate(value: String) = value.ifBlank { null }?.toBigDecimal()?.setScale(8, RoundingMode.HALF_UP)
        val latitude = coordinate(draft.latitude)
        val longitude = coordinate(draft.longitude)
        val arrival = draft.arrival.ifBlank { null }?.let(LocalTime::parse)
        val body = JSONObject().put("route_id", id(selected.id)).put("stop_name", draft.name).put("stop_order", draft.order.toInt())
            .put("latitude", latitude?.toPlainString() ?: JSONObject.NULL).put("longitude", longitude?.toPlainString() ?: JSONObject.NULL)
            .put("estimated_arrival", draft.arrival.ifBlank { null } ?: JSONObject.NULL).put("request_id", request.toString())
        val path = if (existing == null) "stops" else "stops/${id(existing.id)}"
        if (existing != null) {
            check(existing.version.matches(Regex("[0-9a-f]{64}")))
            body.put("expected_version", existing.version)
        }
        val ack = call(path, body = body, method = if (existing == null) "POST" else "PUT") as JSONObject
        val expected = existing?.id ?: request.toString()
        check(ack.getString("id") == expected)
        val stored = stops(selected, 1, 0, expected).single()
        check(stored.name == draft.name && stored.order == draft.order.toInt() && coordinate(stored.latitude) == latitude &&
            coordinate(stored.longitude) == longitude && stored.eta.ifBlank { null }?.let(LocalTime::parse) == arrival &&
            stored.version.matches(Regex("[0-9a-f]{64}")) && (existing == null || stored.version == ack.getString("version")))
    }

    suspend fun latest(selected: TransportRoute): TransportTelemetry? {
        val data = call("telemetry/${id(selected.id)}/latest")
        if (data == JSONObject.NULL) return null
        val row = data as JSONObject
        check(row.getString("route_id") == selected.id)
        fun coordinate(key: String) = if (row.isNull(key)) null else row.getDouble(key).takeIf(Double::isFinite)
        return TransportTelemetry(row.getString("recorded_at"), coordinate("latitude"), coordinate("longitude"), coordinate("speed"))
    }

    suspend fun options(selected: TransportRoute, search: String): TransportOptions {
        val row = call("options", mapOf("route_id" to id(selected.id), "branch_id" to id(selected.branch), "search" to search)) as JSONObject
        check(row.getString("branch_id") == selected.branch)
        return TransportOptions(LocalDate.parse(row.getString("today")), row.getJSONArray("students").rows().map {
            TransportStudent(id(it.getString("id")), it.getString("name"), it.getBoolean("active"))
        }, row.getBoolean("more_students"))
    }

    suspend fun attendance(selected: TransportRoute, day: LocalDate, limit: Int, offset: Int, student: String? = null): List<TransportAttendance> =
        (call("attendance", mapOf("route_id" to id(selected.id), "attendance_date" to day.toString(), "limit" to limit.toString(),
            "offset" to offset.toString(), "student_id" to student?.let(::id))) as JSONArray).rows().map {
            val result = TransportAttendance(id(it.getString("id")), id(it.getString("student_id")), it.text("student_name"),
                LocalDate.parse(it.getString("date")), it.text("pickup_time"), it.text("dropoff_time"), it.getString("status"),
                it.text("stop_name"), it.text("notes"))
            check(it.getString("route_id") == selected.id && result.day == day && (student == null || student == result.studentId))
            result
        }

    suspend fun checkIn(selected: TransportRoute, student: String, event: String, time: LocalDateTime, request: UUID) {
        detail(selected.id)
        val ack = call("check-in", mapOf("branch_id" to id(selected.branch)), JSONObject().put("route_id", id(selected.id)).put("student_id", id(student))
            .put("event", event).put("timestamp", time.toString()).put("request_id", request.toString())) as JSONObject
        check(ack.getString("route_id") == selected.id && ack.getString("student_id") == student && ack.getString("event") == event)
        val day = LocalDate.parse(ack.getString("timestamp").take(10))
        check(day == time.toLocalDate())
        val stored = attendance(selected, day, 2, 0, student).single()
        val pickup = event == "BOARDED"
        val expected = ack.getString(if (pickup) "pickup_time" else "dropoff_time").take(5)
        check(stored.id == ack.getString("id") && expected.isNotBlank() && (if (pickup) stored.pickup else stored.dropoff) == expected)
    }

    suspend fun saveAttendance(selected: TransportRoute, student: String, day: LocalDate, status: String, notes: String) {
        detail(selected.id)
        val ack = call("attendance", mapOf("branch_id" to id(selected.branch)), JSONObject().put("route_id", id(selected.id)).put("student_id", id(student))
            .put("attendance_date", day.toString()).put("status", status).put("notes", notes.ifBlank { null } ?: JSONObject.NULL)) as JSONObject
        check(ack.getString("date") == day.toString() && ack.getString("status") == status)
        val stored = attendance(selected, day, 2, 0, student).single()
        check(stored.id == ack.getString("id") && stored.status == status && stored.notes == notes)
    }
}
