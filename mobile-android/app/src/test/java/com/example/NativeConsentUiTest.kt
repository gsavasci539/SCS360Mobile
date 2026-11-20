package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.User
import com.example.data.model.Student
import com.example.data.model.UserRole
import com.example.data.remote.ApiClientProvider
import com.example.ui.screens.ConsentsScreen
import com.example.ui.screens.ParentNavTab
import com.example.ui.screens.ParentPortalScreen
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
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-mdpi")
class NativeConsentUiTest {
    @get:Rule val compose = createComposeRule()
    private fun id(value: Long) = UUID(74, value).toString()
    private val paths = CopyOnWriteArrayList<String>()
    private val documents = "/api/consents/documents"
    private val decisions = "/api/consents/student-consents"
    private val options = "/api/consents/options"
    private lateinit var server: HttpServer
    private lateinit var originalBase: String
    private var originalToken: String? = null
    private var originalFont = 1f
    @Volatile private var decisionBody: JSONObject? = null
    @Volatile private var decisionSaved = false
    @Volatile private var documentBody: JSONObject? = null
    @Volatile private var guardianBody: JSONObject? = null
    private var useWrongDocumentHash = false
    private val guardianPath get() = "/api/parent/child/${id(3)}/consents"
    private val guardianText = "İzole test belge metni."
    private val guardianHash = MessageDigest.getInstance("SHA-256").digest(guardianText.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    @Before fun start() {
        originalBase = ApiClientProvider.activeBaseUrl
        originalToken = ApiClientProvider.interceptor.authToken
        originalFont = RuntimeEnvironment.getFontScale()
        RuntimeEnvironment.setFontScale(2f)
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val method = exchange.requestMethod
            val uri = exchange.requestURI
            paths.add("$method $uri")
            val data: Any = when {
                method == "GET" && uri.path == documents -> JSONArray().put(JSONObject()
                    .put("id", id(1)).put("title", "Test izin belgesi").put("version", "1.3")
                    .put("document_type", "EXCURSION").put("content", "İzole test belge metni.").put("effective_date", "2026-11-20"))
                method == "POST" && uri.path == documents -> JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                    .also { documentBody = JSONObject(it.toString()) }.put("id", id(7))
                method == "GET" && uri.path == options -> {
                    check(uri.query.contains("organization_id=${id(2)}"))
                    val parents = if (uri.query.contains("student_id=${id(3)}")) {
                        JSONArray().put(JSONObject().put("id", id(4)).put("name", "Test Velisi"))
                    } else JSONArray()
                    JSONObject().put("more_students", false).put("parents", parents)
                        .put("students", JSONArray().put(JSONObject().put("id", id(3)).put("name", "Test Öğrencisi")))
                }
                method == "POST" && uri.path == decisions -> {
                    decisionBody = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                    decisionSaved = true
                    JSONObject().put("id", id(5)).put("consent_document_id", id(1)).put("student_id", id(3)).put("consent_status", "REJECTED")
                }
                method == "GET" && uri.path == decisions -> if (!decisionSaved) JSONArray() else JSONArray().put(JSONObject()
                    .put("id", id(5)).put("student_name", "Test Öğrencisi").put("document_title", "Test izin belgesi")
                    .put("document_version", "1.3").put("parent_name", "Test Velisi")
                    .put("consent_status", "REJECTED").put("consent_method", "MANUAL"))
                method == "GET" && uri.path == guardianPath -> JSONArray().put(JSONObject()
                    .put("id", id(1)).put("title", "Veli belge testi").put("version", "1.3")
                    .put("document_type", "TRIP_CONSENT").put("content", guardianText).put("effective_date", "2026-11-20")
                    .put("consent_status", if (guardianBody == null) "PENDING" else "APPROVED").put("can_decide", true)
                    .put("content_sha256", if (useWrongDocumentHash) "0".repeat(64) else guardianHash))
                method == "POST" && uri.path == "$guardianPath/${id(1)}" -> {
                    guardianBody = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                    JSONObject().put("id", id(8)).put("consent_status", "APPROVED").put("consented_at", "2026-11-20T15:00:00Z")
                }
                else -> {
                    exchange.sendResponseHeaders(404, -1)
                    exchange.close()
                    return@createContext
                }
            }
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        ApiClientProvider.interceptor.setSession("test-consent", id(2))
    }

    @After fun stop() {
        server.stop(0)
        RuntimeEnvironment.setFontScale(originalFont)
        ApiClientProvider.createService(originalBase)
        ApiClientProvider.interceptor.setSession(originalToken)
    }

    private fun open(canWrite: Boolean) {
        val permissions = if (canWrite) listOf("consents.read", "consents.write") else listOf("consents.read")
        val user = User(id = id(6), organization_id = id(2), permissions = permissions)
        compose.setContent { Okul360Theme { ConsentsScreen(Okul360UiState(isLoggedIn = true, currentUser = user)) } }
        clickIn("consent_records", "Belgeyi incele")
    }

    private fun clickIn(container: String, text: String) {
        val match = hasText(text)
        val list = compose.onNodeWithTag(container)
        try {
            compose.waitUntil(15_000) { runCatching { list.performScrollToNode(match) }.isSuccess }
        } catch (failure: ComposeTimeoutException) {
            throw AssertionError("Missing $text after $paths.", failure)
        }
        compose.onNode(match).performScrollTo().assertIsDisplayed().performClick()
    }

    @Test fun readonlyRoleReadsDocumentsWithoutWriteActions() {
        open(canWrite = false)
        compose.onNodeWithText("İzole test belge metni.").assertIsDisplayed()
        compose.onNodeWithText("Veli kararı kaydet").assertDoesNotExist()
        compose.onNodeWithText("Arşive al").assertDoesNotExist()
        assertEquals(listOf("GET $documents?is_active=true&offset=0&limit=21"), paths)
    }

    @Test fun rootNeedsAnExplicitWorkspaceBeforeAnyConsentRequest() {
        val root = User(id = id(6), is_super_admin = true)
        compose.setContent { Okul360Theme { ConsentsScreen(Okul360UiState(isLoggedIn = true, currentUser = root)) } }
        compose.onNodeWithText("İzin belgeleri için bir kurum çalışma alanı seçin.").assertIsDisplayed()
        assertTrue(paths.isEmpty())
    }

    @Test fun createdDocumentSendsTheCanonicalTypeVersionDateAndOrganization() {
        val user = User(id = id(6), organization_id = id(2), permissions = listOf("consents.read", "consents.write"))
        compose.setContent { Okul360Theme { ConsentsScreen(Okul360UiState(isLoggedIn = true, currentUser = user)) } }
        clickIn("consent_records", "Yeni izin belgesi")
        val form = compose.onNodeWithTag("consent_create")
        for ((label, value) in listOf("Belge başlığı" to "Test izin", "Belge metni" to "İzole metin",
            "Yürürlük tarihi (YYYY-AA-GG)" to "2026-11-20")) {
            val match = hasText(label) and hasSetTextAction()
            form.performScrollToNode(match)
            compose.onNode(match).performTextInput(value)
        }
        form.performScrollToNode(hasText("Kaydet"))
        compose.onNodeWithText("Kaydet").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(15_000) { paths.count { it.startsWith("GET $documents?") } == 2 }
        val body = requireNotNull(documentBody)
        assertEquals(id(2), body.getString("organization_id"))
        assertEquals("TRIP_CONSENT", body.getString("document_type"))
        assertEquals("1.0", body.getString("version"))
        assertEquals("2026-11-20", body.getString("effective_date"))
        assertEquals("İzole metin", body.getString("content"))
        assertEquals(listOf("POST $documents"), paths.filter { it.startsWith("POST ") })
    }

    @Test fun manualDecisionUsesTheSelectedStudentGuardianAndDocument() {
        open(canWrite = true)
        clickIn("consent_document", "Veli kararı kaydet")
        clickIn("consent_decision", "Test Öğrencisi")
        clickIn("consent_decision", "Test Velisi")
        clickIn("consent_decision", "Ret")
        assertNull(decisionBody)
        compose.onNodeWithTag("consent_decision").performScrollToNode(hasText("Kararı kaydet"))
        compose.onNodeWithText("Kararı kaydet").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(15_000) { paths.any { it.startsWith("GET $decisions?") } }
        val row = requireNotNull(decisionBody)
        assertEquals(id(1), row.getString("consent_document_id"))
        assertEquals(id(3), row.getString("student_id"))
        assertEquals(id(4), row.getString("parent_id"))
        assertEquals("REJECTED", row.getString("consent_status"))
        assertEquals("MANUAL", row.getString("consent_method"))
        assertEquals(listOf("POST $decisions"), paths.filter { it.startsWith("POST ") })
    }

    private fun openGuardianConsent(canDecide: Boolean) {
        val child = Student(id = id(3), first_name = "Test Öğrencisi")
        val permissions = if (canDecide) listOf("parent.view_children", "parent.give_consent") else listOf("parent.view_children")
        val user = User(id = id(6), organization_id = id(2), permissions = permissions)
        val state = Okul360UiState(isLoggedIn = true, currentUser = user, selectedRole = UserRole.PARENT, parentChildren = listOf(child))
        compose.setContent {
            Okul360Theme {
                ParentPortalScreen(state, {}, { _, _, _ -> }, { _, _, _, _, _ -> }, { _, _, _, _, _ -> }, { _, _, _, _ -> },
                    chatbotTab = ParentNavTab.CONSENTS, chatbotStudentId = child.id, onLogout = {})
            }
        }
        clickIn("parent_consent_records", "Belgenin tam metnini oku")
    }

    @Test fun guardianAcknowledgesAndSignsTheVerifiedDocumentForTheSelectedChild() {
        openGuardianConsent(canDecide = true)
        val form = compose.onNodeWithTag("parent_consent_document")
        form.performScrollToNode(hasText("Onaylıyorum"))
        compose.onNodeWithText("Onaylıyorum").assertIsNotEnabled()
        form.performScrollToNode(hasText("Adınız ve soyadınız"))
        compose.onNode(hasText("Adınız ve soyadınız") and hasSetTextAction()).performTextInput("Test Velisi")
        form.performScrollToNode(hasTestTag("parent_consent_ack"))
        compose.onNodeWithTag("parent_consent_ack").performScrollTo().performClick()
        clickIn("parent_consent_document", "Onaylıyorum")
        compose.waitUntil(15_000) { paths.count { it.startsWith("GET $guardianPath?") } == 2 }
        val saved = requireNotNull(guardianBody)
        assertEquals("APPROVED", saved.getString("consent_status"))
        assertEquals("1.3", saved.getString("document_version"))
        assertEquals(guardianHash, saved.getString("content_sha256"))
        assertEquals("Test Velisi", saved.getString("signed_name"))
        assertTrue(saved.getBoolean("acknowledged"))
        assertEquals(listOf("POST $guardianPath/${id(1)}"), paths.filter { it.startsWith("POST ") })
    }

    @Test fun guardianCanReadButCannotSignADocumentWithUnverifiedContent() {
        useWrongDocumentHash = true
        openGuardianConsent(canDecide = true)
        compose.onNodeWithText(guardianText).assertIsDisplayed()
        compose.onNodeWithText("Onaylıyorum").assertDoesNotExist()
        compose.onNodeWithText("Onay vermiyorum").assertDoesNotExist()
        assertTrue(paths.none { it.startsWith("POST ") })
    }
}
