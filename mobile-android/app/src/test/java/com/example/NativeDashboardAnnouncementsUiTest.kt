package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.User
import com.example.data.remote.ApiClientProvider
import com.example.ui.components.AnnouncementsSummaryWidget
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h640dp-port-mdpi")
class NativeDashboardAnnouncementsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun confirmsBranchAndPublicationStatusAndDropsOldAnnouncementDetails() {
        fun id(number: Long) = UUID(95, number).toString()
        val requests = CopyOnWriteArrayList<String>()
        val oldBase = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val oldToken = interceptor.authToken
        val oldTenant = interceptor.tenantId
        val oldBranch = interceptor.branchId
        val oldWorkspace = interceptor.workspaceOrganizationId
        val oldScale = RuntimeEnvironment.getFontScale()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/announcements") { exchange ->
            requests.add("${exchange.requestMethod} ${exchange.requestURI}")
            val first = exchange.requestURI.query.contains("branch_id=${id(2)}")
            val row = JSONObject().put("id", id(6)).put("status", if (first) "SENT" else "DRAFT")
                .put("branch_id", if (first) id(2) else id(3))
                .put("title", if (first) "Yayımlanmış test kaydı" else "Gizli test taslağı")
                .put("content", "Sunucudaki metin").put("created_at", "2026-11-21T07:30:00Z")
            val body = JSONObject().put("success", true).put("data", JSONArray().put(row)).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            interceptor.setSession("announcement-widget-test", id(1), id(2))
            RuntimeEnvironment.setFontScale(2f)
            val user = User(id = id(4), organization_id = id(1), permissions = listOf("announcement.read"))
            val state = mutableStateOf(Okul360UiState(isLoggedIn = true, currentUser = user, selectedBranchId = id(2)))
            compose.setContent { Okul360Theme { AnnouncementsSummaryWidget(state.value, {}) } }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Yayımlanmış test kaydı").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Yayımlanmış test kaydı").performClick()
            compose.onNodeWithTag("dashboard_announcement_details").assertIsDisplayed()
            compose.runOnIdle { state.value = state.value.copy(selectedBranchId = id(3)) }
            compose.onNodeWithTag("dashboard_announcement_details").assertDoesNotExist()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Duyuru seçili şube veya yayın durumuyla eşleşmiyor.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Yayımlanmış test kaydı").assertDoesNotExist()
            compose.onNodeWithText("Gizli test taslağı").assertDoesNotExist()
            assertEquals(listOf("GET /api/announcements?status=SENT&limit=4&branch_id=${id(2)}",
                "GET /api/announcements?status=SENT&limit=4&branch_id=${id(3)}"), requests)
        } finally {
            server.stop(0)
            RuntimeEnvironment.setFontScale(oldScale)
            ApiClientProvider.createService(oldBase)
            interceptor.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
        }
    }
}
