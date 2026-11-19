package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import com.example.data.model.DailyReportDraft
import com.example.data.model.User
import com.example.data.remote.ApiClientProvider
import com.example.ui.screens.DailyReportAnalysisPanel
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h620dp-mdpi")
class NativeDailyReportUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun reportAnalysisShowsMeasuredZeroAndResetsOnBranchChange() {
        val branch = mutableStateOf(UUID(40, 1).toString())
        val initial = branch.value
        val base = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val previous = listOf(interceptor.authToken, interceptor.tenantId, interceptor.branchId, interceptor.workspaceOrganizationId)
        val requests = ConcurrentLinkedQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/ai/ai-daily-reports-analysis") { exchange ->
            val query = exchange.requestURI.rawQuery
            requests.add(query)
            val body = """{"branch_id":"${query.substringAfter("branch_id=")}","report_stats":{"total":2,"mood_observations":1,"mood_rate":0,"sleep_observations":0,"sleep_rate":null,"meal_observations":3,"meal_rate":66.7},"students_needing_attention":[],"recommendations":[]}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-analysis-reader", branch = initial)
        try {
            compose.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Okul360Theme {
                        DailyReportAnalysisPanel(Okul360UiState(isLoggedIn = true, selectedBranchId = branch.value,
                            currentUser = User(id = "reader", permissions = listOf("daily_reports.read"))))
                    }
                }
            }
            val open = "Karne analizini göster"
            val measured = "Mutlu oranı · %0 · 1 gözlem"
            compose.onNodeWithText(open).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText(measured).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Uyudu oranı · Gözlem yok").assertIsDisplayed()
            compose.onNodeWithText("Tam yedi oranı · %66,7 · 3 gözlem").assertIsDisplayed()
            assertEquals(listOf("branch_id=$initial"), requests.toList())
            val next = UUID(40, 2).toString()
            compose.runOnIdle { interceptor.branchId = next; branch.value = next }
            compose.onNodeWithText(measured).assertDoesNotExist()
            compose.onNodeWithText(open).performClick()
            compose.waitUntil(10_000) { "branch_id=$next" in requests }
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            interceptor.setSession(previous[0], previous[1], previous[2], previous[3])
        }
    }

    @Test fun filteredListRefreshesAfterChangesAndDiscardsAnOldBranch() {
        val oldBranch = UUID(37, 1).toString()
        val newBranch = UUID(37, 2).toString()
        val report = UUID(37, 3).toString()
        val version = "a".repeat(64)
        val branch = mutableStateOf(oldBranch)
        val saved = mutableLongStateOf(0L)
        val removed = AtomicBoolean(false)
        val detailReads = AtomicInteger()
        val deleteVersion = AtomicReference<String>()
        val requests = ConcurrentLinkedQueue<String>()
        val releaseOldBranch = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val workers = Executors.newCachedThreadPool()
        server.executor = workers
        val base = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val previous = listOf(interceptor.authToken, interceptor.tenantId, interceptor.branchId, interceptor.workspaceOrganizationId)

        fun row(name: String) = """{"id":"$report","student_id":"${UUID(37, 4)}","class_id":"${UUID(37, 5)}","date":"2026-11-18","version":"$version","student_name":"$name","teacher_notes":"Kayıtlı gözlem"}"""
        server.createContext("/api/daily-reports") { exchange ->
            val query = exchange.requestURI.rawQuery.orEmpty()
            val payload = when {
                exchange.requestMethod == "DELETE" -> {
                    deleteVersion.set(exchange.requestHeaders.getFirst("If-Match"))
                    removed.set(true)
                    "null"
                }
                exchange.requestURI.path.endsWith("/$report") -> {
                    detailReads.incrementAndGet()
                    if (removed.get()) {
                        exchange.sendResponseHeaders(404, -1)
                        exchange.close()
                        return@createContext
                    }
                    row("Filtreli karne")
                }
                else -> {
                    requests.add(query)
                    val delayed = query.contains("search=Geciken")
                    if (delayed) releaseOldBranch.await(10, TimeUnit.SECONDS)
                    val name = when {
                        query.contains(newBranch) -> "Yeni şube karnesi"
                        delayed -> "Eski şubeden geciken karne"
                        removed.get() -> null
                        query.contains("search=Ada+%26+7") -> "Filtreli karne"
                        else -> "İlk karne"
                    }
                    val total = if (name == null) 0 else 1
                    """{"page":1,"total":$total,"total_pages":$total,"items":[${name?.let(::row).orEmpty()}]}"""
                }
            }
            val body = """{"success":true,"data":$payload}""".toByteArray()
            try {
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            } catch (_: java.io.IOException) {
                exchange.close()
            }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-report-editor", branch = oldBranch)
        try {
            lateinit var clearFocus: () -> Unit
            compose.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Okul360Theme {
                        val focus = LocalFocusManager.current
                        clearFocus = { focus.clearFocus() }
                        DailyReportsScreen(Okul360UiState(isLoggedIn = true, selectedBranchId = branch.value,
                            currentUser = User(id = UUID(37, 6).toString(), permissions = listOf("daily_reports.read", "daily_reports.write")),
                            dailyReportSavedRevision = saved.longValue), onNavigateBack = {})
                    }
                }
            }
            val list = compose.onNodeWithTag("daily_report_list")
            val search = compose.onNodeWithTag("daily_report_search_input")
            list.performScrollToNode(hasTestTag("daily_report_search_input"))
            search.performTextReplacement("Ada & 7")
            list.performScrollToNode(hasTestTag("daily_report_filter_date"))
            compose.onNodeWithTag("daily_report_filter_date").performTextReplacement("2026-11-18")
            val filtered = "page=1&page_size=20&branch_id=$oldBranch&search=Ada+%26+7&date=2026-11-18"
            compose.waitUntil(10_000) { filtered in requests }
            compose.runOnIdle { saved.longValue++ }
            compose.waitUntil(10_000) { requests.count { it == filtered } >= 2 }

            compose.waitUntil(10_000) { compose.onAllNodesWithText("Filtreli karne").fetchSemanticsNodes().isNotEmpty() }
            compose.runOnIdle { clearFocus() }
            list.performScrollToNode(hasText("Karneyi aç"))
            compose.onNodeWithText("Karneyi aç").assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick) { assertTrue(it()) }
            assertTrue(compose.onAllNodesWithText("Karne ayrıntısı").fetchSemanticsNodes().isNotEmpty())
            compose.waitUntil(10_000) { detailReads.get() > 0 }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Sil").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Sil").performClick()
            compose.onNodeWithText("Karneyi sil").performClick()
            compose.waitUntil(10_000) { requests.count { it == filtered } >= 3 }
            assertEquals("\"$version\"", deleteVersion.get())

            list.performScrollToNode(hasTestTag("daily_report_search_input"))
            search.performTextReplacement("Geciken")
            compose.waitUntil(10_000) { requests.any { it.contains(oldBranch) && it.contains("search=Geciken") } }
            compose.runOnIdle {
                interceptor.branchId = newBranch
                branch.value = newBranch
            }
            compose.waitUntil(10_000) { "page=1&page_size=20&branch_id=$newBranch" in requests }
            releaseOldBranch.countDown()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Yeni şube karnesi").fetchSemanticsNodes().isNotEmpty() }
            list.performScrollToNode(hasText("Yeni şube karnesi"))
            compose.onNodeWithText("Yeni şube karnesi").assertIsDisplayed()
            compose.onNodeWithText("Eski şubeden geciken karne").assertDoesNotExist()
        } finally {
            releaseOldBranch.countDown()
            server.stop(0)
            workers.shutdownNow()
            ApiClientProvider.createService(base)
            interceptor.setSession(previous[0], previous[1], previous[2], previous[3])
        }
    }

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
            compose.onNodeWithText("Karne analizini göster").assertDoesNotExist()
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
