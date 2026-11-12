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
    @Test fun blankDraftValidationDoesNotSubmitAndDoesNotClose() {
        var calls = 0; var closes = 0
        compose.setContent { Okul360Theme { FinanceInvoiceDraftDialog(state, { closes++ }, { _, _, _, _, _ -> calls++ }) } }
        compose.onNodeWithTag("invoice_amount").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        compose.onNodeWithTag("invoice_due").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        compose.onNodeWithTag("invoice_draft_list").performScrollToNode(hasTestTag("invoice_submit"))
        compose.onNodeWithTag("invoice_submit").performClick()
        assertEquals(0, calls); assertEquals(0, closes)
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
            compose.setContent { Okul360Theme { FinanceInvoiceDraftDialog(state.copy(currentUser = state.currentUser.copy(permissions = emptyList())), {}, { _, _, _, _, _ -> fail("Readonly") }) } }
            compose.onNodeWithTag("invoice_draft_list").performScrollToNode(hasTestTag("invoice_submit"))
            val node = compose.onNodeWithTag("invoice_submit").assertIsNotEnabled().assertIsDisplayed().fetchSemanticsNode()
            assertTrue(node.boundsInRoot.top >= 0 && node.boundsInRoot.bottom <= 360f && node.boundsInRoot.height >= 48f)
            compose.onNodeWithTag("invoice_draft_list").performScrollToNode(hasTestTag("invoice_close"))
            compose.onNodeWithTag("invoice_close").assertIsDisplayed()
        } finally { RuntimeEnvironment.setFontScale(old) }
    }
}
