package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.PlatformTenantSummary
import com.example.data.model.SubscriptionPlan
import com.example.ui.screens.MembershipEditorDialog
import com.example.ui.theme.Okul360Theme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MembershipEditorDialogTest {
    @get:Rule val compose = createComposeRule()
    private val plan = SubscriptionPlan(id = "00000000-0000-4000-8000-000000000001", name = "Synthetic catalog")
    private val tenant = PlatformTenantSummary(organization_id = "00000000-0000-4000-8000-000000000002", organization_name = "Synthetic institution",
        plan_id = plan.id, period_start = "2026-11-01", period_end = "2026-12-01", is_active = false, access_status = "SUSPENDED")
    @Test fun failedSaveKeepsTheDraftAndSuccessfulRetryClosesOnlyAfterAcknowledgement() {
        var attempts = 0
        var dismissed = false
        val request = CompletableDeferred<String?>()
        compose.setContent { Okul360Theme {
            MembershipEditorDialog(tenant, listOf(plan), onSave = { org, selected, start, end ->
                assertEquals(tenant.organization_id, org)
                assertEquals(plan.id, selected)
                assertEquals("2026-11-01", start)
                assertEquals("2026-12-05", end)
                attempts++
                if (attempts == 1) request.await() else null
            }, onDismiss = { dismissed = true })
        } }
        compose.onNodeWithTag("membership_end").performTextReplacement("2026-12-05")
        compose.onNodeWithTag("membership_save").performClick()
        compose.onNodeWithTag("membership_save").assertIsNotEnabled()
        compose.onNodeWithTag("membership_close").assertIsNotEnabled()
        compose.onNodeWithTag("membership_end").assertIsNotEnabled()
        assertFalse(dismissed)
        request.complete("Synthetic rejection")
        compose.waitForIdle()
        compose.onNodeWithTag("membership_error").assertTextEquals("Synthetic rejection")
        compose.onNodeWithTag("membership_end").assertTextContains("2026-12-05")
        assertFalse(dismissed)
        compose.onNodeWithTag("membership_save").performClick()
        compose.waitForIdle()
        assertEquals(2, attempts)
        assertTrue(dismissed)
    }
    @Test fun invalidDatesAreRejectedBeforeCallingTheApi() {
        var calls = 0
        compose.setContent { Okul360Theme { MembershipEditorDialog(tenant, listOf(plan), onSave = { _, _, _, _ -> calls++; null }, onDismiss = {}) } }
        compose.onNodeWithTag("membership_end").performTextReplacement("2026-10-01")
        compose.onNodeWithTag("membership_save").performClick()
        compose.onNodeWithTag("membership_error").assertExists()
        assertEquals(0, calls)
    }
}
