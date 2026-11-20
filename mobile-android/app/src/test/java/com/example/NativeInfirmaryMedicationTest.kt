package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.User
import com.example.data.remote.ApiClientProvider
import com.example.ui.screens.InfirmaryScreen
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w640dp-h360dp-land-mdpi")
class NativeInfirmaryMedicationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun parentRequestDecisionAndDoseUseTheSameScopedServerRecord() {
        fun id(suffix: Long) = UUID(84, suffix).toString()
        val requests = CopyOnWriteArrayList<String>()
        val status = AtomicReference("PENDING")
        val dose = AtomicReference<JSONObject>()
        val previousBase = ApiClientProvider.activeBaseUrl
        val previousToken = ApiClientProvider.interceptor.authToken
        val previousFont = RuntimeEnvironment.getFontScale()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        fun program() = JSONObject().put("id", id(5)).put("student_id", id(3)).put("medicine_name", "İzole ilaç kaydı")
            .put("dosage", "Test doz talimatı").put("time_to_give", "10:30").put("status", status.get())
            .put("start_date", "2026-11-20").put("updated_by", id(4))
        fun storedDose(payload: JSONObject) = JSONObject(payload.toString()).put("id", id(6))
            .put("given_by", id(4)).put("medication_name", "İzole ilaç kaydı")
        server.createContext("/") { exchange ->
            val uri = exchange.requestURI
            val method = exchange.requestMethod
            requests.add("$method $uri")
            val data: Any = when {
                uri.path == "/api/infirmary/options" -> JSONObject().put("students", JSONArray()
                    .put(JSONObject().put("id", id(3)).put("branch_id", id(2)).put("name", "İlaç Test Öğrencisi")))
                    .put("today", "2026-11-20").put("time", "10:30").put("more_students", false)
                uri.path == "/api/infirmary/infirmary-visits" -> JSONArray()
                uri.path == "/api/infirmary/medicine-requests" && uri.query?.contains("student_id=${id(3)}") == true ->
                    JSONArray().put(program())
                uri.path == "/api/infirmary/medicine-requests/${id(5)}" && method == "PUT" -> {
                    status.set(JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() }).getString("status"))
                    program()
                }
                uri.path == "/api/infirmary/medication/mark-given" && method == "POST" -> {
                    val payload = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                    dose.set(payload)
                    storedDose(payload)
                }
                uri.path == "/api/infirmary/medication-logs" &&
                    uri.query?.contains("student_id=${id(3)}") == true && uri.query.contains("date=2026-11-20") ->
                    JSONArray().apply { dose.get()?.let { put(storedDose(it)) } }
                else -> {
                    exchange.sendResponseHeaders(404, -1)
                    exchange.close()
                    return@createContext
                }
            }
            val output = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, output.size.toLong())
            exchange.responseBody.use { it.write(output) }
        }
        server.start()
        try {
            RuntimeEnvironment.setFontScale(2f)
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            ApiClientProvider.interceptor.setSession("medication-test", id(1))
            val user = User(id = id(4), organization_id = id(1), permissions = listOf("health.read", "health.write"))
            compose.setContent { Okul360Theme { InfirmaryScreen(Okul360UiState(isLoggedIn = true, currentUser = user, selectedBranchId = id(2))) } }
            node("infirmary_visits", "İlaç programları ve talepler").performClick()
            node("infirmary_visits", "Öğrenci seç", waitEnabled = true).performClick()
            node("infirmary_students", "İlaç Test Öğrencisi").performClick()
            node("infirmary_visits", "İlaç kaydını incele").performClick()
            node("infirmary_medication_detail", "Talebi onayla").performClick()
            node("infirmary_visits", "Onaylı").assertIsDisplayed()
            node("infirmary_visits", "İlaç kaydını incele").performClick()
            node("infirmary_medication_detail", "Planlanan doz saati (SS:DD)", field = true).performTextInput("10:30")
            node("infirmary_medication_detail", "Atlandı").performClick()
            node("infirmary_medication_detail", "Doz kayıt notu", field = true).performTextInput("İzole doz sonucu")
            node("infirmary_medication_detail", "Doz sonucunu kaydet").performClick()
            node("infirmary_visits", "İzole doz sonucu").assertIsDisplayed()
            val actualDose = requireNotNull(dose.get())
            assertEquals("SKIPPED", actualDose.getString("status"))
            assertEquals(id(3), actualDose.getString("student_id"))
            assertEquals(id(5), actualDose.getString("schedule_id"))
            assertEquals("10:30", actualDose.getString("scheduled_time"))
            assertFalse(actualDose.has("given_time"))
            assertFalse(actualDose.has("given_by"))
            assertEquals(listOf("PUT /api/infirmary/medicine-requests/${id(5)}", "POST /api/infirmary/medication/mark-given"),
                requests.filterNot { it.startsWith("GET ") })
        } finally {
            server.stop(0)
            RuntimeEnvironment.setFontScale(previousFont)
            ApiClientProvider.createService(previousBase)
            ApiClientProvider.interceptor.setSession(previousToken)
        }
    }

    private fun node(container: String, label: String, field: Boolean = false, waitEnabled: Boolean = false): SemanticsNodeInteraction {
        val target = if (field) hasText(label) and hasSetTextAction() else hasText(label)
        compose.waitUntil(15_000) { runCatching {
            compose.onNodeWithTag(container).performScrollToNode(target)
            if (waitEnabled) compose.onNode(target).assertIsEnabled()
        }.isSuccess }
        return compose.onNode(target).performScrollTo().assertIsDisplayed()
    }
}
