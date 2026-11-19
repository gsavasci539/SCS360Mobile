package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.example.data.model.Student
import com.example.ui.screens.AddDailyReportDialog
import com.example.ui.theme.Okul360Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h620dp-mdpi")
class NativeDailyReportUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectingStudentLeavesObservationsBlankAndSavingLocksTheDialog() {
        val saving = mutableStateOf(false)
        val students = listOf(Student(id = "fixture", class_id = "class", first_name = "Ada", last_name = "Test", class_name = "Sınıf A"))
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                Okul360Theme {
                    AddDailyReportDialog(students, saving.value, error = null, onDismiss = {}, onConfirm = { error("Boş karne gönderildi") })
                }
            }
        }
        compose.onNodeWithText("Kaydet").assertIsNotEnabled()
        compose.onNodeWithText("Öğrenci Seçin").performClick()
        compose.onNodeWithText("Ada Test (Sınıf A)").performClick()
        compose.onNodeWithText("Kaydet").assertIsNotEnabled()
        compose.runOnIdle { saving.value = true }
        compose.onNodeWithText("Kaydediliyor…").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("İptal").assertIsNotEnabled()
    }
}
