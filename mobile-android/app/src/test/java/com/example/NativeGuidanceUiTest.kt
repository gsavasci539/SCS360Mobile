package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.User
import com.example.data.remote.ApiClientProvider
import com.example.ui.screens.GuidanceScreen
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w640dp-h360dp-land-mdpi")
class NativeGuidanceUiTest {
    @get:Rule val compose = createComposeRule()
    private fun id(value: Long) = UUID(38, value).toString()
    private val requests = CopyOnWriteArrayList<String>()
    private lateinit var server: HttpServer
    private lateinit var base: String
    private var token: String? = null
    private var font = 1f
    @Volatile private var requestBody: JSONObject? = null
    @Volatile private var wrongAcknowledgement = false

    @Before fun start() {
        base = ApiClientProvider.activeBaseUrl
        token = ApiClientProvider.interceptor.authToken
        font = RuntimeEnvironment.getFontScale()
        RuntimeEnvironment.setFontScale(2f)
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val uri = exchange.requestURI
            requests.add("${exchange.requestMethod} $uri")
            val payload: Any = when {
                uri.path == "/api/guidance/options" -> {
                    val counselor = JSONArray()
                    if (uri.query.contains("student_id=${id(2)}")) counselor.put(JSONObject().put("id", id(3)).put("name", "Test Danışmanı"))
                    JSONObject().put("students", JSONArray().put(JSONObject().put("id", id(2)).put("name", "Test Öğrencisi")))
                        .put("counselors", counselor).put("more_students", false).put("more_counselors", false).put("today", "2026-11-20")
                }
                uri.path == "/api/guidance/sessions" && exchange.requestMethod == "POST" -> {
                    val sent = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                    requestBody = sent
                    JSONObject(sent.toString()).put("id", id(4)).apply {
                        if (wrongAcknowledgement) put("student_id", id(5))
                    }
                }
                uri.path == "/api/guidance/sessions" -> JSONArray().apply {
                    val saved = requestBody
                    if (saved != null && !wrongAcknowledgement) put(JSONObject(saved.toString()).put("id", id(4))
                        .put("student_name", "Test Öğrencisi").put("counselor_name", "Test Danışmanı"))
                }
                else -> {
                    exchange.sendResponseHeaders(404, -1)
                    exchange.close()
                    return@createContext
                }
            }
            val bytes = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        ApiClientProvider.interceptor.setSession("test-guidance", id(1))
    }

    @After fun stop() {
        server.stop(0)
        RuntimeEnvironment.setFontScale(font)
        ApiClientProvider.createService(base)
        ApiClientProvider.interceptor.setSession(token)
    }

    private fun open(canWrite: Boolean) {
        val permissions = if (canWrite) listOf("guidance.read", "guidance.write") else listOf("guidance.read")
        val user = User(id = id(3), organization_id = id(1), permissions = permissions)
        compose.setContent { Okul360Theme { GuidanceScreen(Okul360UiState(isLoggedIn = true, currentUser = user)) } }
        click("guidance_sessions", "Öğrenci seç")
        click("guidance_students", "Test Öğrencisi")
        compose.waitUntil(15_000) { requests.any { it.startsWith("GET /api/guidance/sessions?") && it.contains("student_id=${id(2)}") } }
    }

    private fun click(container: String, text: String) {
        val target = hasText(text)
        try {
            compose.waitUntil(15_000) {
                runCatching { compose.onNodeWithTag(container).performScrollToNode(target) }.isSuccess
            }
        } catch (failure: ComposeTimeoutException) {
            throw AssertionError("Missing $text after $requests.", failure)
        }
        compose.onNode(target).performScrollTo().assertIsDisplayed().performClick()
    }

    @Test fun readerSelectsStudentWithoutWriteActions() {
        open(canWrite = false)
        compose.onNodeWithText("Yeni görüşme").assertDoesNotExist()
        assertTrue(requests.none { it.startsWith("POST ") || it.contains("counselor_search=") && it.contains("student_id=") })
    }

    @Test fun createsSessionForTheVerifiedStudentAndCounselorInLandscape() {
        verifyCreate(matches = true)
    }

    @Test fun mismatchedServerAcknowledgementDoesNotShowSuccess() {
        verifyCreate(matches = false)
    }

    private fun verifyCreate(matches: Boolean) {
        wrongAcknowledgement = !matches
        open(canWrite = true)
        click("guidance_sessions", "Yeni görüşme")
        click("guidance_create", "Test Danışmanı")
        val form = compose.onNodeWithTag("guidance_create")
        val text = hasText("Görüşme özeti") and hasSetTextAction()
        form.performScrollToNode(text)
        compose.onNode(text).performTextInput("İzole test görüşmesi")
        click("guidance_create", "Görüşmeyi kaydet")
        if (matches) {
            compose.waitUntil(15_000) { requests.count { it.startsWith("GET /api/guidance/sessions?") && it.contains("student_id=${id(2)}") } == 2 }
            click("guidance_sessions", "Görüşmeyi incele")
            compose.onNodeWithText("İzole test görüşmesi").assertIsDisplayed()
        } else {
            val error = hasText("Kaydedilen görüşme seçili öğrenci, danışman ve tarihle eşleşmiyor.")
            compose.waitUntil(15_000) { runCatching { form.performScrollToNode(error) }.isSuccess }
            compose.onNodeWithText("Rehberlik görüşmesi kaydedildi.").assertDoesNotExist()
        }
        val body = requireNotNull(requestBody)
        assertEquals(id(2), body.getString("student_id"))
        assertEquals(id(3), body.getString("counselor_id"))
        assertEquals("INDIVIDUAL", body.getString("session_type"))
        assertEquals("2026-11-20", body.getString("session_date"))
        assertEquals("STANDARD", body.getString("confidentiality_level"))
        assertEquals(listOf("POST /api/guidance/sessions"), requests.filter { it.startsWith("POST ") })
    }
}
