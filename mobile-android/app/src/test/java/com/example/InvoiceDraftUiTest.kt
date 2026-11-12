package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.ui.dialogs.FinanceInvoiceDraftDialog
import com.example.ui.screens.FinancePortalScreen
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-mdpi")
class InvoiceDraftUiTest {
    @get:Rule val compose = createComposeRule()
    private val student = Student(id = UUID(1, 1).toString(), first_name = "Synthetic", last_name = "Student")
    private val state = Okul360UiState(isLoggedIn = true, currentUser = User(id = UUID(2, 2).toString(), permissions = listOf("finance.write")),
        selectedRole = UserRole.FINANCE, students = listOf(student))
    private lateinit var server: com.sun.net.httpserver.HttpServer
    private lateinit var oldBase: String
    private var oldToken: String? = null
    private val posts = java.util.concurrent.atomic.AtomicInteger()
    @Before fun server() {
        oldBase = com.example.data.remote.ApiClientProvider.activeBaseUrl
        oldToken = com.example.data.remote.ApiClientProvider.interceptor.authToken
        server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e ->
            if (e.requestMethod == "POST") posts.incrementAndGet()
            val bytes = "{\"success\":true,\"data\":{\"total\":0,\"items\":[]}}".toByteArray()
            e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(200, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
        }; server.start()
        com.example.data.remote.ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        com.example.data.remote.ApiClientProvider.interceptor.setSession("synthetic-debt-ui")
    }
    @After fun stop() { server.stop(0); com.example.data.remote.ApiClientProvider.createService(oldBase); com.example.data.remote.ApiClientProvider.interceptor.setSession(oldToken) }
    @Test fun blankDraftValidationDoesNotSubmitAndDoesNotClose() {
        var calls = 0; var closes = 0
        compose.setContent { Okul360Theme { FinanceInvoiceDraftDialog(state, { closes++ }, { calls++ }) } }
        compose.onNodeWithTag("invoice_draft_list").performScrollToNode(hasTestTag("invoice_amount"))
        compose.onNodeWithTag("invoice_amount").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        compose.onNodeWithTag("invoice_draft_list").performScrollToNode(hasTestTag("invoice_due"))
        compose.onNodeWithTag("invoice_due").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        compose.onNodeWithTag("invoice_draft_list").performScrollToNode(hasTestTag("invoice_submit"))
        compose.onNodeWithTag("invoice_submit").performClick()
        assertEquals(0, calls); assertEquals(0, closes); assertEquals(0, posts.get())
        compose.onNodeWithTag("invoice_submit").assertIsEnabled()
    }
    @Test fun actualPortalScopeChangeDisposesInvoiceDialog() {
        var current by mutableStateOf(state)
        compose.setContent { Okul360Theme { FinancePortalScreen(current, { _, _, _ -> }, { _, _, _, _, _ -> }, { _, _ -> }, onLogout = {}) } }
        compose.onNodeWithTag("finance_speed_dial_fab").performClick()
        compose.onNodeWithTag("finance_fab_action_invoice").performClick()
        compose.onNodeWithText("Tahakkuk taslağı").assertExists()
        compose.runOnIdle { current = current.copy(workspaceOrganizationId = UUID(3, 3).toString()) }
        compose.onNodeWithText("Tahakkuk taslağı").assertDoesNotExist()
    }
    @Test @Config(qualifiers = "w780dp-h360dp-land-mdpi")
    fun largeFontLandscapeKeepsActionsVisibleAndReadonlyBlocked() {
        val old = RuntimeEnvironment.getFontScale(); RuntimeEnvironment.setFontScale(2f)
        try {
            compose.setContent { Okul360Theme { FinanceInvoiceDraftDialog(state.copy(currentUser = state.currentUser.copy(permissions = emptyList())), {}, { fail("Readonly") }) } }
            compose.onNodeWithTag("invoice_draft_list").performScrollToNode(hasTestTag("invoice_submit"))
            val node = compose.onNodeWithTag("invoice_submit").assertIsNotEnabled().assertIsDisplayed().fetchSemanticsNode()
            assertTrue(node.boundsInRoot.top >= 0 && node.boundsInRoot.bottom <= 360f && node.boundsInRoot.height >= 48f)
            compose.onNodeWithTag("invoice_draft_list").performScrollToNode(hasTestTag("invoice_close"))
            compose.onNodeWithTag("invoice_close").assertIsDisplayed()
        } finally { RuntimeEnvironment.setFontScale(old) }
    }
}
