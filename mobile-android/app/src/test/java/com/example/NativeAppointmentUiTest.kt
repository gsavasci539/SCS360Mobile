package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.ParentTeacherAppointment
import com.example.ui.screens.AppointmentCard
import com.example.ui.theme.Okul360Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w640dp-h360dp-mdpi")
class NativeAppointmentUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun parentCardShowsOnlyCancellationAndRecordedDetailsAtLargeFont() {
        val font = RuntimeEnvironment.getFontScale()
        RuntimeEnvironment.setFontScale(1.5f)
        var cancellations = 0
        try {
            compose.setContent { Okul360Theme {
                AppointmentCard(
                    ParentTeacherAppointment(
                        id = "recorded", teacher_name = "Kayıtlı öğretmen", student_name = "Kayıtlı öğrenci",
                        date = "2027-01-04", time_slot = "14:00", status = "PENDING",
                    ),
                    canManage = false, canCancel = true, onCardClick = {},
                    onApprove = { error("Parent cannot approve") }, onReject = { error("Parent cannot reject") },
                    onComplete = { error("Parent cannot complete") }, onCancel = { cancellations++ },
                    onAddFeedback = { error("Parent cannot edit teacher notes") }, onOpenMeet = { error("Missing link") },
                )
            } }
            compose.onNodeWithText("Kayıtlı öğretmen").assertExists()
            compose.onNodeWithText("Öğrenci: Kayıtlı öğrenci").assertExists()
            compose.onNodeWithText("Onayla").assertDoesNotExist()
            compose.onNodeWithText("Reddet").assertDoesNotExist()
            compose.onNodeWithText("Meet'e Katıl").assertDoesNotExist()
            compose.onNodeWithText("İptal et").assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(1, cancellations) }
        } finally {
            RuntimeEnvironment.setFontScale(font)
        }
    }
}
