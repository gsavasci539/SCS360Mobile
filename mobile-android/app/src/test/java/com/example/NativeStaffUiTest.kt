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
}
