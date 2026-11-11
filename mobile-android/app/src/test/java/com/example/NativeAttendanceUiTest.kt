package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.screens.AttendanceScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeAttendanceUiTest {
    @get:Rule val compose = createComposeRule()
    private val classroom = UUID(2, 2).toString()
    private val student = UUID(3, 1).toString()
    private val base = Okul360UiState(attendanceCanWrite = true, attendanceClassId = classroom, attendanceDate = "2026-11-10",
        classes = listOf(ClassGroup(id = classroom, name = "Sentetik uzun sınıf adı")),
        attendanceRecords = listOf(AttendanceRecord(student_id = student, student_name = "Sentetik öğrenci adı ve soyadı",
            class_id = classroom, date = "2026-11-10")))

    @Test fun anUnloadedOrUnmarkedSnapshotCannotBeSavedAndNoSmsSuccessIsPromised() {
        var state by mutableStateOf(base)
        var saves = 0
        compose.setContent { Okul360Theme { AttendanceScreen(state, {}, { _, _ -> }, {}, { saves++ }) } }
        compose.onNodeWithTag("attendance_list").performScrollToNode(hasTestTag("attendance_save"))
        compose.onNodeWithTag("attendance_save").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { state = state.copy(attendanceReady = true) }
        compose.onNodeWithTag("attendance_save").assertIsNotEnabled()
        compose.onAllNodes(hasText("SMS", substring = true)).assertCountEquals(0)
        compose.onAllNodes(hasText("36.5", substring = true)).assertCountEquals(0)
        assertEquals(0, saves)
    }

    @Test fun explicitHalfDayAndNotesStayDraftsUntilSaveAndPendingWritesLockEditing() {
        var state by mutableStateOf(base.copy(attendanceReady = true))
        var saves = 0
        compose.setContent { Okul360Theme { AttendanceScreen(state, {}, { id, status ->
            state = state.copy(attendanceRecords = state.attendanceRecords.map { if (it.student_id == id) it.copy(status = status) else it })
        }, {}, { saves++ }, onNoteChanged = { id, note ->
            state = state.copy(attendanceRecords = state.attendanceRecords.map { if (it.student_id == id) it.copy(note = note) else it })
        }) } }
        compose.onNodeWithTag("attendance_list").performScrollToNode(hasTestTag("attendance_status_${student}_HALF_DAY"))
        compose.onNodeWithTag("attendance_status_${student}_HALF_DAY").performClick()
        compose.onNodeWithTag("attendance_note_$student").performClick()
        compose.onNodeWithTag("attendance_note_field").performTextInput("Türkçe sentetik not")
        compose.onNodeWithText("Taslağa ekle").performClick()
        compose.runOnIdle {
            assertEquals("HALF_DAY", state.attendanceRecords.single().status)
            assertEquals("Türkçe sentetik not", state.attendanceRecords.single().note)
            assertEquals(0, saves)
            state = state.copy(attendanceSavePending = true)
        }
        compose.onNodeWithTag("attendance_note_$student").assertIsNotEnabled()
        compose.onNodeWithTag("attendance_list").performScrollToNode(hasTestTag("attendance_save"))
        compose.onNodeWithTag("attendance_save").assertIsEnabled().performClick()
        assertEquals(1, saves)
    }

    @OptIn(ExperimentalTestApi::class)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [36], qualifiers = "w360dp-h780dp-mdpi")
    @Test fun saveRemainsReachableAtLargeFontInPortraitAndLandscape() {
        var size by mutableStateOf(DpSize(360.dp, 780.dp))
        val state = base.copy(attendanceReady = true, attendanceRecords = base.attendanceRecords.map { it.copy(status = "ABSENT") })
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size) then DeviceConfigurationOverride.FontScale(2f)) {
                Okul360Theme { AppContentWrapper { key(size) { AttendanceScreen(state, {}, { _, _ -> }, {}, {}) } } }
            }
        }
        for (window in listOf(DpSize(360.dp, 780.dp), DpSize(780.dp, 360.dp), DpSize(320.dp, 600.dp))) {
            compose.runOnIdle { size = window }
            compose.onNodeWithTag("attendance_list").performScrollToNode(hasTestTag("attendance_save"))
            compose.onNodeWithTag("attendance_save").performScrollTo()
                .assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            compose.onRoot().captureRoboImage("../../output/native-attendance-${window.width.value.toInt()}x${window.height.value.toInt()}-font2-20261111.png")
        }
    }
}
