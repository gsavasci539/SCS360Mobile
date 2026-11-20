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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h640dp-port-mdpi")
class NativeInfirmaryHealthFileTest {
    @get:Rule val compose = createComposeRule()

    @Test fun editingScopedFileSendsOnlyChangedFieldsAndReloadsPersistedValue() {
        fun id(suffix: Long) = UUID(83, suffix).toString()
        val reads = AtomicInteger()
        val submitted = AtomicReference<JSONObject>()
        val previousBase = ApiClientProvider.activeBaseUrl
        val previousToken = ApiClientProvider.interceptor.authToken
        val previousFont = RuntimeEnvironment.getFontScale()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        fun record(value: String) = JSONObject().put("id", id(5)).put("student_id", id(3))
            .put("updated_by", id(4)).put("allergies", value).put("chronic_conditions", "Korunacak test notu")
        server.createContext("/") { exchange ->
            val uri = exchange.requestURI
            val method = exchange.requestMethod
            val data: Any = when {
                uri.path == "/api/infirmary/options" -> JSONObject().put("students", JSONArray()
                    .put(JSONObject().put("id", id(3)).put("branch_id", id(2)).put("name", "Dosya Test Öğrencisi")))
                    .put("today", "2026-11-20").put("time", "10:30").put("more_students", false)
                uri.path == "/api/infirmary/infirmary-visits" -> JSONArray()
                uri.path == "/api/infirmary/health-records" && method == "GET" && uri.query.contains("student_id=${id(3)}") -> {
                    reads.incrementAndGet()
                    JSONArray().put(record(submitted.get()?.getString("allergies") ?: "Önceki test değeri"))
                }
                uri.path == "/api/infirmary/health-records/${id(3)}" && method == "PUT" -> {
                    val payload = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                    submitted.set(payload)
                    record(payload.getString("allergies"))
                }
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
            ApiClientProvider.interceptor.setSession("health-file-test", id(1))
            val user = User(id = id(4), organization_id = id(1), permissions = listOf("health.read", "health.write"))
            compose.setContent { Okul360Theme { InfirmaryScreen(Okul360UiState(isLoggedIn = true, currentUser = user, selectedBranchId = id(2))) } }
            node("infirmary_visits", "Sağlık dosyaları").performClick()
            node("infirmary_visits", "Öğrenci seç", waitEnabled = true).performClick()
            node("infirmary_students", "Dosya Test Öğrencisi").performClick()
            node("infirmary_visits", "Önceki test değeri").assertIsDisplayed()
            node("infirmary_visits", "Sağlık dosyasını düzenle").performClick()
            node("infirmary_file", "Alerji bilgisi", field = true).performTextReplacement("Güncel test değeri")
            node("infirmary_file", "Sağlık dosyasını kaydet").performClick()
            node("infirmary_visits", "Güncel test değeri").assertIsDisplayed()
            assertEquals(2, reads.get())
            assertEquals(setOf("student_id", "allergies"), submitted.get().keys().asSequence().toSet())
            assertEquals(id(3), submitted.get().getString("student_id"))
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
