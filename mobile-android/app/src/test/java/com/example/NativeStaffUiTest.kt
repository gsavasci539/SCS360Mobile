package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.screens.StaffManagementScreen
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w640dp-h360dp-mdpi")
class NativeStaffUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun landscapeLargeTextFormWaitsForAcknowledgement() {
        val font = RuntimeEnvironment.getFontScale()
        RuntimeEnvironment.setFontScale(1.5f)
        try {
            var revision by mutableIntStateOf(0)
            var calls = 0
            compose.setContent { Okul360Theme {
                StaffManagementScreen(Okul360UiState(), {}, actionsEnabled = true, savedRevision = revision,
                    onSubmitLeaveRequest = { _, _, _, days, _ -> calls++; assertEquals(3, days) },
                    onReviewLeaveRequest = { _, _, _ -> error("No review") })
            } }
            compose.onNodeWithText("İzin Talepleri (0)").performClick()
            compose.onNodeWithText("İzin Talebi Oluştur", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Talebi İlet").assertIsNotEnabled()
            compose.onNode(hasSetTextAction() and hasText("Başlangıç")).performScrollTo().performTextInput("2027-01-01")
            compose.onNode(hasSetTextAction() and hasText("Bitiş")).performScrollTo().performTextInput("2027-01-03")
            compose.onNode(hasSetTextAction() and hasText("İzin Gerekçesi & Detayı")).performScrollTo().performTextInput("Recorded reason")
            compose.onNodeWithText("Talebi İlet").performClick()
            compose.runOnIdle { assertEquals(1, calls) }
            compose.onNodeWithText("Yeni İzin / Mazeret Talebi").assertExists()
            compose.runOnIdle { revision++ }
            compose.onNodeWithText("Yeni İzin / Mazeret Talebi").assertDoesNotExist()
        } finally { RuntimeEnvironment.setFontScale(font) }
    }
    @Test fun reviewRequiresExplicitConfirmationAndRetainsTypedCommentUntilServerAcknowledgement() {
        val font = RuntimeEnvironment.getFontScale()
        RuntimeEnvironment.setFontScale(1.5f)
        try {
            val request = com.example.data.model.TeacherLeaveRequest(id = "recorded-leave", teacher_id = "recorded-staff",
                teacher_name = "Recorded staff", branch = "", leave_type = "Annual", start_date = "2027-01-01",
                end_date = "2027-01-03", total_days = 3, reason = "Recorded reason", status = "PENDING", admin_response = null, created_at = "")
            var revision by mutableIntStateOf(0)
            var enabled by mutableStateOf(true)
            var failureText by mutableStateOf("")
            var sent: Triple<String, Boolean, String>? = null
            compose.setContent { Okul360Theme {
                StaffManagementScreen(Okul360UiState(leaveRequests = listOf(request)), {},
                    actionsEnabled = enabled, canReview = true, savedRevision = revision, operationError = failureText,
                    onSubmitLeaveRequest = { _, _, _, _, _ -> error("No creation") },
                    onReviewLeaveRequest = { id, approved, comment -> sent = Triple(id, approved, comment); enabled = false })
            } }
            compose.onNodeWithText("İzin Talepleri (1)").performClick()
            compose.onNode(hasText("Reddet") or hasContentDescription("Reddet")).performClick()
            compose.runOnIdle { org.junit.Assert.assertNull(sent) }
            compose.onNodeWithTag("staff_review_comment").performScrollTo().performTextInput("Yazılı değerlendirme")
            compose.onNodeWithTag("staff_review_confirm").performClick()
            compose.runOnIdle { assertEquals(Triple("recorded-leave", false, "Yazılı değerlendirme"), sent); failureText = "Sunucu yanıtı doğrulanamadı" }
            compose.onNodeWithText("İzin talebini reddet").assertExists()
            compose.onNodeWithTag("staff_review_confirm").assertIsNotEnabled()
            compose.onNodeWithTag("staff_review_comment").assertTextContains("Yazılı değerlendirme")
            compose.runOnIdle { revision++ }
            compose.onNodeWithText("İzin talebini reddet").assertDoesNotExist()
        } finally { RuntimeEnvironment.setFontScale(font) }
    }
    @Test fun readonlyStaffHasNoReviewAction() {
        val request = com.example.data.model.TeacherLeaveRequest(id = "recorded-leave", status = "PENDING")
        compose.setContent { Okul360Theme {
            StaffManagementScreen(Okul360UiState(leaveRequests = listOf(request)), {}, actionsEnabled = true, canReview = false,
                onSubmitLeaveRequest = { _, _, _, _, _ -> }, onReviewLeaveRequest = { _, _, _ -> error("Forbidden") })
        } }
        compose.onNodeWithText("İzin Talepleri (1)").performClick()
        compose.onNode(hasText("Onayla") or hasContentDescription("Onayla")).assertDoesNotExist()
        compose.onNode(hasText("Reddet") or hasContentDescription("Reddet")).assertDoesNotExist()
    }
}
