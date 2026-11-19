package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.example.data.model.Student
import com.example.data.remote.ApiClientProvider
import com.example.ui.screens.AddDailyReportDialog
import com.example.ui.screens.DailyReportDetailsDialog
import com.example.ui.theme.Okul360Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.UUID

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

    @Test fun readOnlyDetailShowsRecordedNoteWithoutMutationActions() {
        val id = UUID(28, 1).toString()
        val base = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val previous = listOf(interceptor.authToken, interceptor.tenantId, interceptor.branchId, interceptor.workspaceOrganizationId)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/daily-reports/$id") { exchange ->
            val body = """{"success":true,"data":{"id":"$id","student_id":"${UUID(28, 2)}","class_id":"${UUID(28, 3)}","date":"2026-11-19","student_name":"Ada Test","teacher_notes":"Kayıtlı not"}}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-read-only")
        try {
            compose.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Okul360Theme { DailyReportDetailsDialog(id, "reader", false, onDismiss = {}, onChanged = { error("Yazma işlemi") }) }
                }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Kayıtlı not").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Kayıtlı not").assertIsDisplayed()
            compose.onNodeWithText("Düzenle").assertDoesNotExist()
            compose.onNodeWithText("Sil").assertDoesNotExist()
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            interceptor.setSession(previous[0], previous[1], previous[2], previous[3])
        }
    }
}
