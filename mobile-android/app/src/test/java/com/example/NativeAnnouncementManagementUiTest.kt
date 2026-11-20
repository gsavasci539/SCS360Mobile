package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.User
import com.example.data.remote.ApiClientProvider
import com.example.ui.screens.NativeAnnouncementManagementPane
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
class NativeAnnouncementManagementUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun draftUsesVerifiedStudentAndQueueUsesOnlyItsPersistedId() {
        fun id(part: Long) = UUID(96, part).toString()
        val requests = CopyOnWriteArrayList<String>()
        val draft = AtomicReference<JSONObject>()
        val wire = AtomicReference<JSONObject>()
        val status = AtomicReference("DRAFT")
        val oldBase = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val oldToken = interceptor.authToken
        val oldTenant = interceptor.tenantId
        val oldBranch = interceptor.branchId
        val oldWorkspace = interceptor.workspaceOrganizationId
        val oldScale = RuntimeEnvironment.getFontScale()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        fun readDraft() = JSONObject(requireNotNull(draft.get()).toString()).put("status", status.get())
        server.createContext("/api/announcements") { exchange ->
            val uri = exchange.requestURI
            val method = exchange.requestMethod
            requests.add("$method $uri")
            val payload: Any = when {
                uri.path == "/api/announcements/targets" -> {
                    val query = uri.query.orEmpty()
                    val choice = if ("kind=students" in query) JSONObject().put("id", id(3)).put("name", "İzole öğrenci")
                        else JSONObject().put("id", id(2)).put("name", "İzole şube")
                    JSONObject().put("items", JSONArray().put(choice)).put("more", false)
                }
                uri.path == "/api/announcements" && method == "POST" -> {
                    val sent = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                    wire.set(sent)
                    draft.set(JSONObject(sent.toString()).put("id", id(5)).put("branch_id", id(2)).put("created_at", "2026-11-21T01:00:00Z"))
                    JSONObject().put("id", id(5))
                }
                uri.path == "/api/announcements/${id(5)}" -> readDraft()
                uri.path == "/api/announcements/${id(5)}/send" && method == "POST" -> {
                    status.set("SENT")
                    JSONObject().put("announcement_id", id(5)).put("jobs_created", 1).put("already_queued", false)
                }
                uri.path == "/api/announcements" -> JSONArray().apply { if (draft.get() != null) put(readDraft()) }
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
        }
        server.start()
        try {
            RuntimeEnvironment.setFontScale(2f)
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            interceptor.setSession("announcement-manager-test", id(1), id(2))
            val user = User(id = id(4), organization_id = id(1), permissions = listOf("announcement.read", "announcement.write"))
            compose.setContent {
                Okul360Theme { NativeAnnouncementManagementPane(Okul360UiState(isLoggedIn = true, currentUser = user, selectedBranchId = id(2))) }
            }
            node("native_announcement_records", "Duyuru taslağı oluştur").performClick()
            node("native_announcement_draft", "Duyuru başlığı", field = true).performTextInput("İzole taslak")
            node("native_announcement_draft", "Duyuru içeriği", field = true).performTextInput("İzole ve kalıcı test metni")
            node("native_announcement_draft", "Tek öğrenci velisi").performClick()
            node("native_announcement_draft", "İzole öğrenci").performClick()
            node("native_announcement_draft", "E-posta").performClick()
            node("native_announcement_draft", "Taslağı kaydet", waitEnabled = true).performClick()
            node("native_announcement_records", "İzole taslak").performClick()
            node("native_announcement_detail", "Kuyruğa al").performClick()
            node("native_announcement_detail", "Kuyruğu onayla").performClick()
            node("native_announcement_records", "Yayımlanmış").assertIsDisplayed()
            assertEquals("SINGLE_STUDENT", wire.get().getString("audience"))
            assertEquals(id(3), wire.get().getString("student_id"))
            assertEquals("EMAIL", wire.get().getJSONArray("channels").getString(0))
            assertEquals(listOf("POST /api/announcements", "POST /api/announcements/${id(5)}/send"), requests.filterNot { it.startsWith("GET ") })
            assertTrue(requests.any { it.startsWith("GET /api/announcements/targets?kind=students") })
        } finally {
            server.stop(0)
            RuntimeEnvironment.setFontScale(oldScale)
            ApiClientProvider.createService(oldBase)
            interceptor.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
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
