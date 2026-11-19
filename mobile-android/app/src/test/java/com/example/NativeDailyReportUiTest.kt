package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import com.example.data.model.DailyReportDraft
import com.example.data.model.User
import com.example.data.remote.ApiClientProvider
import com.example.ui.screens.DailyReportsScreen
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.navigation.SubScreen
import com.example.ui.screens.DailyReportDetailsDialog
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h620dp-mdpi")
class NativeDailyReportUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun writeOnlyActorUsesScopedChoicesAndCannotReadTheReportList() {
        val branch = UUID(32, 1).toString()
        val actor = User(id = "writer", permissions = listOf("daily_reports.write"))
        assertTrue(NativeNavigationPolicy.screen(actor, SubScreen.DAILY_REPORTS))
        val saving = mutableStateOf(false)
        val base = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val previous = listOf(interceptor.authToken, interceptor.tenantId, interceptor.branchId, interceptor.workspaceOrganizationId)
        val requests = ConcurrentLinkedQueue<String>()
        val submitted = mutableListOf<DailyReportDraft>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.add(exchange.requestURI.toString())
            val body = """{"success":true,"data":{"page":1,"total_pages":1,"items":[{"id":"${UUID(32, 2)}","name":"Ada Test · KARNE-11","student_name":"Ada Test","class_id":"${UUID(32, 3)}"}]}}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-report-writer")
        try {
            compose.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Okul360Theme {
                        DailyReportsScreen(Okul360UiState(isLoggedIn = true, currentUser = actor,
                            selectedBranchId = branch, dailyReportSaving = saving.value),
                            onCreateReport = { submitted.add(it); saving.value = true }, onReload = { error("Okuma izni yok") })
                    }
                }
            }
            compose.onNodeWithText("Kayıtlı karneleri görüntüleme yetkiniz yok.").assertIsDisplayed()
            compose.onNodeWithTag("daily_report_fab").performClick()
            compose.onNodeWithText("Kaydet").assertIsNotEnabled()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Ada Test · KARNE-11").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Ada Test · KARNE-11").performClick()
            compose.onNodeWithText("Kaydet").assertIsNotEnabled()
            val dateField = compose.onNodeWithTag("daily_report_date")
            dateField.performScrollTo().performTextReplacement("2026-02-30")
            compose.onNodeWithText("Geçerli bir karne tarihi girin.").assertIsDisplayed()
            compose.onNodeWithText("🌟 Çok Neşeli & Uyumlu").performScrollTo().performClick()
            compose.onNodeWithText("Kaydet").assertIsNotEnabled()
            dateField.performScrollTo().performTextReplacement("2026-11-15")
            compose.onNodeWithText("Kaydet").performClick()
            compose.runOnIdle { assertEquals(LocalDate.of(2026, 11, 15), submitted.single().date) }
            compose.onNodeWithText("Kaydediliyor…").assertIsDisplayed().assertIsNotEnabled()
            compose.onNodeWithText("İptal").assertIsNotEnabled()
            assertEquals(listOf("/api/daily-reports/student-options?page=1&page_size=25&branch_id=$branch"), requests.toList())
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            interceptor.setSession(previous[0], previous[1], previous[2], previous[3])
        }
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
