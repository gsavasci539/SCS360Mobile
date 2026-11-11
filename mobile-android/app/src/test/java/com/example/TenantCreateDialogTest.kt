package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.SubscriptionPlan
import com.example.ui.screens.TenantCreateDialog
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
class TenantCreateDialogTest {
    @get:Rule val compose = createComposeRule()
    private val plan = SubscriptionPlan(id = "00000000-0000-4000-8000-000000000001", name = "Synthetic catalog", is_active = true)
    private fun fill(tag: String, value: String) = compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(value)
    @Test fun failedCreationKeepsDraftAndLocksDuplicateSubmissionUntilAcknowledgement() {
        var attempts = 0
        var dismissed = false
        val pending = CompletableDeferred<String?>()
        compose.setContent { Okul360Theme { TenantCreateDialog(listOf(plan), onCreate = { draft ->
            assertEquals("Synthetic institution", draft.name)
            assertEquals("Synthetic password", draft.password)
            assertEquals(plan.id, draft.planId)
            attempts++
            if (attempts == 1) pending.await() else null
        }, onDismiss = { dismissed = true }) } }
        compose.onNodeWithTag("tenant_create").assertIsNotEnabled()
        fill("tenant_name", "Synthetic institution")
        fill("tenant_first", "Synthetic")
        fill("tenant_last", "Owner")
        fill("tenant_email", "owner@example.test")
        fill("tenant_password", "Synthetic password")
        compose.onNodeWithTag("tenant_plan_${plan.id}").performScrollTo().performClick()
        compose.onNodeWithTag("tenant_create").performClick()
        compose.onNodeWithTag("tenant_create").assertIsNotEnabled()
        compose.onNodeWithTag("tenant_close").assertIsNotEnabled()
        compose.onNodeWithTag("tenant_email").assertIsNotEnabled()
        assertFalse(dismissed)
        pending.complete("Synthetic rejection")
        compose.waitForIdle()
        compose.onNodeWithTag("tenant_error").assertTextEquals("Synthetic rejection")
        compose.onNodeWithTag("tenant_name").assertTextContains("Synthetic institution")
        assertFalse(dismissed)
        compose.onNodeWithTag("tenant_create").performClick()
        compose.waitForIdle()
        assertEquals(2, attempts)
        assertTrue(dismissed)
    }
    @Test fun unavailablePlansAndInvalidOwnerDataCannotCreate() {
        var calls = 0
        compose.setContent { Okul360Theme { TenantCreateDialog(listOf(plan, plan.copy(id = "inactive", is_active = false)), onCreate = { calls++; null }, onDismiss = {}) } }
        compose.onNodeWithTag("tenant_plan_inactive").assertDoesNotExist()
        compose.onNodeWithTag("tenant_create").assertIsNotEnabled()
        compose.onNodeWithTag("tenant_plan_${plan.id}").performScrollTo().performClick()
        compose.onNodeWithTag("tenant_create").performClick()
        compose.onNodeWithTag("tenant_error").assertExists()
        assertEquals(0, calls)
    }
}
