package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.data.remote.ApiClientProvider
import com.example.ui.components.financePresentationScope
import com.example.ui.dialogs.VerifiedReceiptDialog
import com.example.ui.screens.FinancePortalScreen
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-mdpi")
class FinanceScopeUiTest {
    @get:Rule val compose = createComposeRule()
    private val initial = Okul360UiState(isLoggedIn = true,
        currentUser = User(id = UUID(1, 1).toString(), organization_id = UUID(2, 2).toString(), permissions = listOf("finance.write")),
        selectedRole = UserRole.FINANCE)
    @Test fun branchChangeClosesOldInvoiceDraftAndExpandedActions() {
        var state by mutableStateOf(initial)
        compose.setContent { Okul360Theme { FinancePortalScreen(state, { _, _, _ -> }, { _, _, _, _, _ -> }, { _, _ -> }, onLogout = {}) } }
        compose.onNodeWithTag("finance_speed_dial_fab").performClick()
        compose.onNodeWithTag("finance_fab_action_invoice").performClick()
        compose.onNodeWithText("📄 Yeni Fatura / Taksit Tanımla").assertExists()
        compose.runOnIdle { state = state.copy(selectedBranchId = UUID(3, 3).toString()) }
        compose.onNodeWithText("📄 Yeni Fatura / Taksit Tanımla").assertDoesNotExist()
        compose.onNodeWithTag("finance_fab_action_invoice").assertDoesNotExist()
    }
    @Test fun allAccessIdentityChangesResetPresentationButDataRefreshDoesNot() {
        val original = financePresentationScope(initial)
        assertEquals(original, financePresentationScope(initial.copy(invoices = listOf(Invoice(id = UUID(4, 4).toString())))))
        val variants = listOf(initial.copy(isLoggedIn = false), initial.copy(workspaceOrganizationId = UUID(5, 5).toString()),
            initial.copy(currentUser = initial.currentUser.copy(id = UUID(6, 6).toString())),
            initial.copy(currentUser = initial.currentUser.copy(permissions = emptyList())), initial.copy(selectedRole = UserRole.PARENT))
        variants.forEach { assertNotEquals(original, financePresentationScope(it)) }
    }
    @Test fun receiptWindowHidesImmediatelyWhenCapturedSessionIsInvalidated() {
        val oldBase = ApiClientProvider.activeBaseUrl
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e -> e.sendResponseHeaders(503, -1); e.close() }; server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        val oldToken = ApiClientProvider.interceptor.authToken
        ApiClientProvider.interceptor.setSession("synthetic-scope-test")
        var revision by mutableIntStateOf(0); var closed = false
        try {
            compose.setContent {
                @Suppress("UNUSED_VARIABLE") val read = revision
                Okul360Theme { VerifiedReceiptDialog(UUID(7, 7).toString()) { closed = true } }
            }
            compose.onNodeWithText("Sunucu makbuzları").assertExists()
            compose.runOnIdle { ApiClientProvider.interceptor.invalidateContext(); revision++ }
            compose.onNodeWithText("Sunucu makbuzları").assertDoesNotExist()
            compose.runOnIdle { assertTrue(closed) }
        } finally { server.stop(0); ApiClientProvider.createService(oldBase); ApiClientProvider.interceptor.setSession(oldToken) }
    }
}
