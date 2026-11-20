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
class NativeInfirmaryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun scopedVisitAndFollowUpRefreshFromServerWithoutSendingANotification() {
        fun id(suffix: Long) = UUID(99, suffix).toString()
        val requests = CopyOnWriteArrayList<String>()
        val draft = AtomicReference<JSONObject>()
        val followUp = AtomicReference<JSONObject>()
        val previousBase = ApiClientProvider.activeBaseUrl
        val previousToken = ApiClientProvider.interceptor.authToken
        val previousFont = RuntimeEnvironment.getFontScale()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val uri = exchange.requestURI
            requests.add("${exchange.requestMethod} $uri")
            val payload: Any = when {
                uri.path == "/api/infirmary/options" -> JSONObject()
                    .put("students", JSONArray().put(JSONObject().put("id", id(3)).put("branch_id", id(2)).put("name", "Test Öğrencisi")))
                    .put("today", "2026-11-20").put("time", "10:30").put("more_students", false)
                uri.path == "/api/infirmary/infirmary-visits" && exchange.requestMethod == "POST" -> {
                    val body = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                    draft.set(body)
                    JSONObject(body.toString()).put("id", id(5)).put("examined_by", id(4))
                }
                uri.path == "/api/infirmary/infirmary-visits" -> JSONArray().apply {
                    draft.get()?.let {
                        put(JSONObject(it.toString()).put("id", id(5)).put("student_name", "Test Öğrencisi")
                            .put("follow_up_required", followUp.get() == null)
                            .put("follow_up_notes", followUp.get()?.getString("follow_up_notes")))
                    }
                }
                uri.path == "/api/infirmary/infirmary-visits/${id(5)}/follow-up" -> {
                    val body = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                    followUp.set(body)
                    JSONObject(body.toString()).put("id", id(5)).put("student_id", id(3)).put("branch_id", id(2)).put("follow_up_required", false)
                }
                else -> {
                    exchange.sendResponseHeaders(404, -1)
                    exchange.close()
                    return@createContext
                }
            }
            val response = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        try {
            RuntimeEnvironment.setFontScale(2f)
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            ApiClientProvider.interceptor.setSession("infirmary-test", id(1))
            val user = User(id = id(4), organization_id = id(1), permissions = listOf("health.read", "health.write"))
            compose.setContent { Okul360Theme { InfirmaryScreen(Okul360UiState(isLoggedIn = true, currentUser = user, selectedBranchId = id(2))) } }
            node("infirmary_visits", "Öğrenci seç").assertIsEnabled().performClick()
            node("infirmary_students", "Test Öğrencisi").performClick()
            node("infirmary_visits", "Revir ziyareti ekle").performClick()
            node("infirmary_create", "Şikâyet", field = true).performTextInput("İzole test ziyareti")
            node("infirmary_create", "Takip gerekli").performClick()
            node("infirmary_create", "Revir ziyaretini kaydet").performClick()
            node("infirmary_visits", "Ziyareti incele").performClick()
            node("infirmary_detail", "Takip sonuç notu", field = true).performTextInput("İzole takip notu")
            node("infirmary_detail", "Takibi tamamla").performClick()
            node("infirmary_visits", "Ziyareti incele").performClick()
            node("infirmary_detail", "Takip notu · İzole takip notu").assertIsDisplayed()
            assertFalse(draft.get().getBoolean("parent_notified"))
            assertFalse(draft.get().has("examined_by"))
            assertEquals(id(2), draft.get().getString("branch_id"))
            assertEquals(id(3), draft.get().getString("student_id"))
            assertEquals(listOf("POST /api/infirmary/infirmary-visits", "POST /api/infirmary/infirmary-visits/${id(5)}/follow-up"),
                requests.filter { it.startsWith("POST ") })
            assertTrue(requests.any { it.startsWith("GET /api/infirmary/options?") && "branch_id=${id(2)}" in it })
            assertTrue(requests.count { it.startsWith("GET /api/infirmary/infirmary-visits?") &&
                "student_id=${id(3)}" in it && "branch_id=${id(2)}" in it && "start_date=2026-11-20" in it } >= 2)
        } finally {
            server.stop(0)
            RuntimeEnvironment.setFontScale(previousFont)
            ApiClientProvider.createService(previousBase)
            ApiClientProvider.interceptor.setSession(previousToken)
        }
    }

    private fun node(container: String, label: String, field: Boolean = false): SemanticsNodeInteraction {
        val target = if (field) hasText(label) and hasSetTextAction() else hasText(label)
        compose.waitUntil(15_000) {
            runCatching {
                compose.onNodeWithTag(container).performScrollToNode(target)
                if (label == "Öğrenci seç" && container == "infirmary_visits") compose.onNode(target).assertIsEnabled()
            }.isSuccess
        }
        return compose.onNode(target).performScrollTo().assertIsDisplayed()
    }
}
