package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.remote.ApiClientProvider
import com.example.ui.dialogs.RegistryClassDialog
import com.example.ui.dialogs.RegistryStudentDialog
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.github.takahirom.roborazzi.captureRoboImage
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import android.os.Looper
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-mdpi")
class NativeRegistryUiTest {
    @get:Rule val compose = createComposeRule()
    private val org = UUID(1, 1).toString(); private val branch = UUID(2, 2).toString(); private val teacher = UUID(3, 3).toString()
    private val state = Okul360UiState(currentUser = User(id = UUID(4, 4).toString(), organization_id = org,
        permissions = listOf("students.write", "classes.write")), selectedBranchId = branch,
        branches = listOf(Branch(id = branch, organization_id = org, name = "Sentetik uzun şube adı")))
    private val existing = Student(id = UUID(5, 5).toString(), organization_id = org, branch_id = branch,
        first_name = "Sentetik uzun öğrenci adı", last_name = "Soyadı", enrollment_date = "2026-11-12")
    private val room = ClassGroup(id = UUID(6, 6).toString(), organization_id = org, branch_id = branch,
        name = "Sentetik uzun sınıf adı", capacity = 20, academic_year = "2026-2027")
    private lateinit var server: HttpServer
    private lateinit var oldBase: String
    private var oldToken: String? = null; private var oldWorkspace: String? = null
    private var oldFontScale = 1f
    private val hits = AtomicInteger(); @Volatile private var failFirst = false
    @Before fun server() {
        oldFontScale = RuntimeEnvironment.getFontScale()
        RuntimeEnvironment.setFontScale(2f)
        assertEquals(2f, RuntimeEnvironment.getFontScale(), .01f)
        oldBase = ApiClientProvider.activeBaseUrl; oldToken = ApiClientProvider.interceptor.authToken; oldWorkspace = ApiClientProvider.interceptor.workspaceOrganizationId
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e ->
            val isTeacher = e.requestURI.path.contains("teacher-options")
            val status = if (isTeacher && hits.incrementAndGet() == 1 && failFirst) 503 else 200
            val options = when {
                e.requestURI.path.contains("branch-options") -> JSONArray().put(JSONObject().put("id", branch).put("name", "Sentetik uzun şube adı"))
                e.requestURI.path.contains("class-options") -> JSONArray()
                else -> JSONArray().put(JSONObject().put("id", teacher).put("name", "Sentetik Öğretmen"))
            }
            val bytes = JSONObject().put("success", status == 200).put("data", JSONObject().put("total", options.length()).put("items", options)).toString().toByteArray()
            e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(status, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
        }
        server.start(); ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        ApiClientProvider.interceptor.setSession("synthetic-ui-registry-token")
    }
    @After fun stop() { RuntimeEnvironment.setFontScale(oldFontScale); server.stop(0); ApiClientProvider.createService(oldBase); ApiClientProvider.interceptor.setSession(oldToken, workspace = oldWorkspace) }

    @Test fun unloadedStudentDetailsDoNotInventDebtOrAttendanceHistory() {
        compose.setContent { Okul360Theme { com.example.ui.dialogs.StudentDetailBottomSheet(existing, {}) } }
        compose.onNodeWithTag("student_finance_unloaded").performScrollTo().assertTextEquals("Yüklenmedi")
        compose.onAllNodes(hasText("Borcu Yok", substring = true)).assertCountEquals(0)
        compose.onNodeWithTag("student_attendance_unloaded").assertExists()
        compose.onNodeWithText("WhatsApp").assertIsNotEnabled()
    }
    @Test fun failedRosterLoadIsAnErrorWithWorkingRetryRatherThanAnEmptyCount() {
        var retries = 0
        val failed = state.copy(studentsError = "Sentetik bağlantı hatası", students = emptyList())
        compose.setContent { Okul360Theme { com.example.ui.screens.StudentsScreen(failed, {}, {},
            onSelectStudent = {}, onOpenAddStudent = {}, onUpdateStudent = {}, onDeleteStudent = {}, onRefresh = { retries++ }) } }
        compose.onNodeWithTag("students_load_error").assertExists()
        compose.onNodeWithTag("students_retry").performClick()
        assertEquals(1, retries)
    }
    @Test fun readOnlyRosterDisablesMutationTargetsAndHidesEmptyActionMenu() {
        var creates = 0
        val readonly = state.copy(currentUser = state.currentUser.copy(permissions = emptyList()), students = listOf(existing))
        compose.setContent { Okul360Theme { com.example.ui.theme.AppContentWrapper {
            com.example.ui.screens.StudentsScreen(readonly, {}, {}, onSelectStudent = {}, onOpenAddStudent = { creates++ }, onUpdateStudent = {}, onDeleteStudent = {})
        } } }
        compose.onNodeWithContentDescription("Düzenle").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Sil").assertIsNotEnabled()
        compose.onAllNodesWithTag("students_speed_dial_fab").assertCountEquals(0)
        assertEquals(0, creates)
    }
    @Test fun unknownFieldsStayBlankAndFailedSaveKeepsDraftUntilAcknowledged() {
        var current by mutableStateOf(state)
        var captured: Student? = null; val dismissed = AtomicInteger()
        compose.setContent { Okul360Theme { RegistryStudentDialog(null, current, { dismissed.incrementAndGet() }, { captured = it }) } }
        compose.onNodeWithTag("registry_student_form").performScrollToNode(hasTestTag("student_first"))
        compose.onNodeWithTag("student_first").performTextInput("Sentetik")
        compose.onNodeWithTag("student_last").performTextInput("Taslak")
        compose.onNodeWithTag("registry_student_form").performScrollToNode(hasTestTag("registry_student_save"))
        compose.waitUntil(10000) { runCatching { compose.onNodeWithTag("registry_student_save").assertIsEnabled(); true }.getOrDefault(false) }
        compose.onNodeWithTag("registry_student_save").performScrollTo().assertIsEnabled().performClick()
        assertNotNull(captured); assertTrue(captured!!.birth_date.isBlank() && captured!!.gender.isBlank() && captured!!.student_number.isBlank())
        assertNull(captured!!.blood_type); assertNull(captured!!.address); assertEquals(0, dismissed.get())
        compose.runOnIdle { current = current.copy(registryError = "Sentetik sunucu hatası") }
        compose.onNodeWithTag("registry_student_form").performScrollToNode(hasTestTag("registry_error"))
        compose.onNodeWithTag("registry_error").assertTextEquals("Sentetik sunucu hatası")
        compose.onAllNodesWithTag("student_health").assertCountEquals(0)
        compose.runOnIdle { current = current.copy(registrySaving = true) }
        compose.onNodeWithTag("registry_student_form").performScrollToNode(hasTestTag("registry_student_save"))
        compose.onNodeWithTag("registry_student_save").assertIsNotEnabled()
        compose.runOnIdle { current = current.copy(registrySaving = false, registrySavedRevision = 1) }
        compose.waitForIdle()
        compose.waitUntil(10000) { dismissed.get() > 0 }
        assertEquals(1, dismissed.get())
    }
    @Test fun teacherOptionsRetryUsesActualIdAndReadOnlyStateCannotSave() {
        failFirst = true
        var current by mutableStateOf(state); var captured: ClassGroup? = null
        compose.setContent { Okul360Theme { RegistryClassDialog(room, current, {}, { captured = it }) } }
        compose.waitUntil(10000) { hits.get() >= 1 }
        compose.onNodeWithTag("registry_class_form").performScrollToNode(hasText("Yeniden dene"))
        compose.onNodeWithText("Yeniden dene").performClick()
        compose.onNodeWithTag("registry_class_form").performScrollToNode(hasTestTag("class_teacher"))
        compose.waitUntil(10000) { runCatching { compose.onNodeWithTag("class_teacher").assertIsEnabled(); true }.getOrDefault(false) }
        compose.onNodeWithTag("class_teacher").performClick(); compose.onNodeWithText("Sentetik Öğretmen").performClick()
        compose.onNodeWithTag("registry_class_form").performScrollToNode(hasTestTag("registry_class_save"))
        compose.onNodeWithTag("registry_class_save").performScrollTo().performClick()
        assertEquals(teacher, captured!!.teacher_id); assertEquals("2026-2027", captured!!.academic_year)
        compose.runOnIdle { current = current.copy(currentUser = current.currentUser.copy(permissions = emptyList())) }
        compose.onNodeWithTag("registry_class_save").assertIsNotEnabled()
    }
    @Test fun phoneFormsKeepSaveReachableAtLargeFont() = formsReachable()
    @Test @Config(sdk = [36], qualifiers = "w780dp-h360dp-land-mdpi")
    fun landscapeFormsKeepSaveReachableAtLargeFont() = formsReachable()
    @Test @Config(sdk = [36], qualifiers = "w320dp-h600dp-mdpi")
    fun narrowFormsKeepSaveReachableAtLargeFont() = formsReachable()
    @OptIn(ExperimentalTestApi::class)
    private fun formsReachable() {
        var kind by mutableIntStateOf(0)
        compose.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
            Okul360Theme { key(kind) { if (kind == 0) RegistryStudentDialog(existing, state, {}, {}) else RegistryClassDialog(room, state, {}, {}) } }
        } }
        for (form in 0..1) {
            compose.runOnIdle { kind = form }
            val prefix = if (form == 0) "student" else "class"
            compose.onNodeWithTag("registry_${prefix}_form").performScrollToNode(hasTestTag("registry_${prefix}_save"))
            compose.onNodeWithText("Kapat").performScrollTo()
            compose.onNodeWithTag("registry_${prefix}_save").assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            assertSaveInsideViewport(prefix)
            val bounds = compose.onNodeWithTag("registry_${prefix}_form").getUnclippedBoundsInRoot()
            assertTrue(bounds.right.value - bounds.left.value <= 600f)
        }
    }
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test fun nativeStudentPhoneRendersWithSystemLargeFont() { captureForm(true) }
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test fun nativeClassPhoneRendersWithSystemLargeFont() { captureForm(false) }
    private fun captureForm(student: Boolean) {
        compose.setContent { Okul360Theme {
            if (student) RegistryStudentDialog(existing, state, {}, {}) else RegistryClassDialog(room, state, {}, {})
        } }
        val prefix = if (student) "student" else "class"
        val list = compose.onNodeWithTag("registry_${prefix}_form")
        val selector = if (student) "student_branch" else "class_teacher"
        if (!student) list.performScrollToNode(hasTestTag(selector))
        compose.waitUntil(10000) { runCatching { compose.onNodeWithTag(selector).assertIsEnabled(); true }.getOrDefault(false) }
        list.performScrollToIndex(0)
        captureReady(prefix, "top")
        list.performScrollToNode(hasTestTag("registry_${prefix}_save"))
        compose.onNodeWithText("Kapat").performScrollTo()
        list.performTouchInput { swipeUp() }
        compose.onNodeWithTag("registry_${prefix}_save").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        assertSaveInsideViewport(prefix)
        captureReady(prefix, "save")
    }
    private fun assertSaveInsideViewport(prefix: String) {
        val viewport = compose.onNodeWithTag("registry_${prefix}_form").getUnclippedBoundsInRoot()
        val save = compose.onNodeWithTag("registry_${prefix}_save").getUnclippedBoundsInRoot()
        assertTrue("Save clipped above viewport", save.top.value >= viewport.top.value - .5f)
        assertTrue("Save clipped below viewport", save.bottom.value <= viewport.bottom.value + .5f)
    }
    private fun captureReady(prefix: String, part: String) {
        compose.mainClock.advanceTimeBy(64)
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        compose.onNodeWithTag("registry_${prefix}_form").captureRoboImage("../../output/native-registry-${prefix}-${part}-font2-20261112.png")
    }
}
