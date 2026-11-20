package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import com.example.data.model.Student
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeParentPortalClient
import com.example.data.remote.ParentChildOptionsModule
import com.example.data.remote.ParentInvoiceSnapshot
import com.example.ui.screens.ParentVerifiedRecordsPane
import com.example.ui.screens.ParentDiaryAndGrowthTab
import com.example.ui.screens.ParentPortalScreen
import com.example.ui.screens.ParentPaymentsTab
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.example.ui.viewmodel.ParentInvoiceLoadState
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h620dp-mdpi")
class NativeParentDayUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun oneInvoiceFailureKeepsVerifiedChildrenAndTheOtherInvoiceResult() {
        val first = UUID(43, 1).toString()
        val second = UUID(43, 2).toString()
        val debt = UUID(43, 3).toString()
        val base = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val previous = listOf(interceptor.authToken, interceptor.tenantId, interceptor.branchId, interceptor.workspaceOrganizationId)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/parent/") { exchange ->
            val data = when (exchange.requestURI.path) {
                "/api/parent/my-children" ->
                    """[{"student_id":"$first","first_name":"Deniz","last_name":"Test"},{"student_id":"$second","first_name":"Ada","last_name":"Test"}]"""
                "/api/parent/child/$second/financial-invoices" ->
                    """{"student_id":"$second","invoices":[{"debt_id":"$debt","amount":"25.00","paid_amount":"0.00","remaining_amount":"25.00","status":"UNPAID"}]}"""
                else -> null
            }
            val bytes = """{"success":${data != null},"data":$data}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(if (data != null) 200 else 503, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-parent")
        try {
            var children = emptyList<Student>()
            val results = mutableMapOf<String, Result<ParentInvoiceSnapshot>>()
            runBlocking {
                NativeParentPortalClient().loadPortalRecords(null, readFinances = true, onChildren = { children = it }) { id, result ->
                    assertEquals(listOf(first, second), children.map { it.id })
                    results[id] = result
                }
            }
            assertEquals(listOf(first, second), children.map { it.id })
            assertEquals(true, results.getValue(first).isFailure)
            assertEquals(debt, results.getValue(second).getOrThrow().rows.single().invoice.id)

            val invoiceStates = results.mapValues { (_, result) ->
                if (result.isSuccess) ParentInvoiceLoadState.LOADED else ParentInvoiceLoadState.FAILED
            }
            val state = Okul360UiState(currentUser = User(permissions = listOf("parent.view_finances")),
                parentChildren = children, parentInvoiceStates = invoiceStates,
                parentInvoiceSnapshots = mapOf(second to results.getValue(second).getOrThrow()))
            compose.setContent { Okul360Theme { ParentPaymentsTab(children.first(), state, {}, {}) } }
            compose.onNodeWithText("Borçlar yüklenemedi. Yeniden deneyin.").assertIsDisplayed()
            compose.onNodeWithText("Kayıtlı borç bulunamadı").assertDoesNotExist()
            compose.onNodeWithText("Taksit & Fatura Listesi").assertDoesNotExist()
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            interceptor.setSession(previous[0], previous[1], previous[2], previous[3])
        }
    }

    @Test fun limitedParentUsesModuleChildOptionsAndShowsOnlyAuthorizedTabs() {
        val child = UUID(42, 3).toString()
        val requests = ConcurrentLinkedQueue<String>()
        val base = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val previous = listOf(interceptor.authToken, interceptor.tenantId, interceptor.branchId, interceptor.workspaceOrganizationId)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/parent/") { exchange ->
            val uri = exchange.requestURI
            requests.add(uri.toString())
            val data = when {
                uri.path.endsWith("/my-children/options") ->
                    """[{"student_id":"$child","first_name":"Deniz","last_name":"Test"}]"""
                uri.path.endsWith("/attendance") -> {
                    val params = uri.rawQuery.split("&").associate { it.substringBefore("=") to it.substringAfter("=") }
                    """{"student_id":"$child","year":${params.getValue("year")},"month":${params.getValue("month")},"calendar":{},"summary":{"present":0,"absent":0,"late":0,"excused":0,"total":0}}"""
                }
                else -> null
            }
            val bytes = """{"success":${data != null},"data":$data}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(if (data != null) 200 else 403, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-parent")
        try {
            val attendanceChildren = runBlocking { NativeParentPortalClient().children(ParentChildOptionsModule.ATTENDANCE) }
            val financeChildren = runBlocking { NativeParentPortalClient().children(ParentChildOptionsModule.FINANCES) }
            assertEquals(child, attendanceChildren.single().id)
            assertEquals("", financeChildren.single().student_number)
            val state = mutableStateOf(Okul360UiState(isLoggedIn = true, selectedRole = UserRole.PARENT,
                currentUser = User(id = "parent", permissions = listOf("parent.view_attendance")), parentChildren = attendanceChildren))
            compose.setContent {
                Okul360Theme {
                    ParentPortalScreen(state.value, {}, { _, _, _ -> }, { _, _, _, _, _ -> },
                        { _, _, _, _, _ -> }, { _, _, _, _ -> }, onLogout = {})
                }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Bu ay için yoklama kaydı yok.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Ödemeler").assertDoesNotExist()
            compose.onNodeWithText("Günlük kayıtlar").assertDoesNotExist()
            compose.runOnIdle {
                state.value = state.value.copy(
                    currentUser = state.value.currentUser.copy(permissions = listOf("parent.view_finances")),
                    parentChildren = financeChildren,
                )
            }
            compose.onNodeWithText("Ödemeler").assertIsDisplayed()
            compose.onNodeWithText("Yoklama").assertDoesNotExist()
            compose.onNodeWithText("Bugün").assertDoesNotExist()
            assertEquals(listOf("/api/parent/my-children/options?module=attendance", "/api/parent/my-children/options?module=finances"),
                requests.filter { it.contains("my-children") })
            assertFalse(requests.any { it.contains("/timeline") || it.contains("/today-status") })
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            interceptor.setSession(previous[0], previous[1], previous[2], previous[3])
        }
    }

    @Test fun attendanceUsesTheAuthorizedChildAndMonthFromTheTimeline() {
        val child = UUID(42, 1).toString()
        val user = mutableStateOf(User(id = "parent", permissions = listOf("parent.view_children")))
        val requests = ConcurrentLinkedQueue<String>()
        val base = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val previous = listOf(interceptor.authToken, interceptor.tenantId, interceptor.branchId, interceptor.workspaceOrganizationId)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/parent/child/$child/") { exchange ->
            val uri = exchange.requestURI
            requests.add(uri.toString())
            val data = if (uri.path.endsWith("/timeline")) {
                val date = uri.rawQuery?.substringAfter("date=") ?: "2026-11-20"
                """{"student_id":"$child","date":"$date","events":[{"type":"ATTENDANCE","title":"Yoklama kaydı","details":{}},{"type":"DAILY_REPORT","title":"Öğretmen günlük raporu","details":{}}]}"""
            } else {
                val october = uri.rawQuery == "year=2025&month=10"
                val day = """{"date":"2025-10-07","status":"PRESENT","check_in_time":"08:45","check_out_time":null,"note":"Kayıtlı yoklama notu"}"""
                val present = if (october) 1 else 0
                """{"student_id":"$child","year":2025,"month":${if (october) 10 else 11},"calendar":{${if (october) "\"7\":$day" else ""}},"summary":{"present":$present,"absent":0,"late":0,"excused":0,"total":$present}}"""
            }
            val bytes = """{"success":true,"data":$data}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-parent")
        try {
            val student = Student(id = child, first_name = "Deniz")
            compose.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Okul360Theme {
                        ParentDiaryAndGrowthTab(student, Okul360UiState(isLoggedIn = true, currentUser = user.value,
                            parentChildren = listOf(student)), user.value, onLogout = {})
                    }
                }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Öğretmen günlük raporu").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Yoklama kaydı").assertDoesNotExist()
            compose.onNodeWithText("Aylık yoklamayı aç").assertDoesNotExist()
            compose.onNodeWithText("Yoklama").assertDoesNotExist()
            assertFalse(requests.any { it.contains("/attendance") })
            compose.runOnIdle { user.value = user.value.copy(permissions = user.value.permissions + "parent.view_attendance") }
            compose.onNodeWithTag("parent_timeline_date").performTextReplacement("2025-10-07")
            compose.waitUntil(10_000) { "/api/parent/child/$child/timeline?date=2025-10-07" in requests }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Aylık yoklamayı aç").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Aylık yoklamayı aç").performClick()
            compose.waitUntil(10_000) { "/api/parent/child/$child/attendance?year=2025&month=10" in requests }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("2025-10-07 · Mevcut").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Giriş · 08:45").assertIsDisplayed()
            compose.onNodeWithText("Çıkış", substring = true).assertDoesNotExist()
            compose.onNodeWithText("Sonraki ay").performClick()
            compose.waitUntil(10_000) { "/api/parent/child/$child/attendance?year=2025&month=11" in requests }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Bu ay için yoklama kaydı yok.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Kayıtlı yoklama notu").assertDoesNotExist()
            compose.onNodeWithText("Günlük kayıtlar").performClick()
            compose.waitUntil(10_000) { requests.count { it == "/api/parent/child/$child/timeline?date=2025-10-07" } >= 2 }
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            interceptor.setSession(previous[0], previous[1], previous[2], previous[3])
        }
    }

    @Test fun parentDayRendersActivitiesAndDropsADelayedTimelineForThePreviousChild() {
        val first = UUID(41, 1).toString()
        val second = UUID(41, 2).toString()
        val student = mutableStateOf(first)
        val section = mutableStateOf("today-status")
        val canReadAttendance = mutableStateOf(false)
        val base = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val previous = listOf(interceptor.authToken, interceptor.tenantId, interceptor.branchId, interceptor.workspaceOrganizationId)
        val requests = ConcurrentLinkedQueue<String>()
        val release = CountDownLatch(1)
        val delivered = CountDownLatch(1)
        val workers = Executors.newCachedThreadPool()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = workers
        server.createContext("/api/parent/child/") { exchange ->
            val uri = exchange.requestURI
            val child = uri.path.substringAfter("/child/").substringBefore('/')
            val date = uri.rawQuery?.substringAfter("date=") ?: "2026-11-20"
            val delayed = child == first && date == "2026-11-18"
            requests.add(uri.toString())
            if (delayed) release.await(10, TimeUnit.SECONDS)
            val activities = when {
                delayed -> "\"Geciken etkinlik\""
                child == second -> "\"Boya\",\"Müzik\""
                else -> "\"Kitap okuma\",\"Çizim\""
            }
            val report = """{"activities":[$activities],"teacher_notes":"Kayıtlı gözlem"}"""
            val fields = if (uri.path.endsWith("/today-status"))
                """"daily_report":$report,"attendance":{"status":"PRESENT","check_in_time":"08:45"}"""
                else """"events":[{"title":"Öğretmen günlük raporu","details":$report}]"""
            val bytes = """{"success":true,"data":{"student_id":"$child","date":"$date",$fields}}""".toByteArray()
            try {
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (_: java.io.IOException) {
                exchange.close()
            } finally {
                if (delayed) delivered.countDown()
            }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-parent")
        try {
            compose.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Okul360Theme {
                        ParentVerifiedRecordsPane(student.value, section.value, canReadAttendance = canReadAttendance.value)
                    }
                }
            }
            fun awaitText(text: String) {
                compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(text).assertIsDisplayed()
            }
            awaitText("Etkinlikler: Kitap okuma, Çizim")
            compose.onNodeWithText("Yoklama: PRESENT").assertDoesNotExist()
            compose.onNodeWithText("Bugün için yoklama kaydı yok.").assertDoesNotExist()
            compose.runOnIdle { canReadAttendance.value = true }
            awaitText("Yoklama: PRESENT")
            compose.runOnIdle { section.value = "timeline" }
            awaitText("Etkinlikler: Kitap okuma, Çizim")
            compose.onNodeWithTag("parent_timeline_date").performTextReplacement("2026-11-18")
            compose.waitUntil(10_000) { "/api/parent/child/$first/timeline?date=2026-11-18" in requests }
            compose.runOnIdle { student.value = second }
            awaitText("Etkinlikler: Boya, Müzik")
            release.countDown()
            compose.waitUntil(10_000) { delivered.count == 0L }
            compose.onNodeWithText("Etkinlikler: Geciken etkinlik").assertDoesNotExist()
            compose.onNodeWithText("Etkinlikler: Kitap okuma, Çizim").assertDoesNotExist()
        } finally {
            release.countDown()
            server.stop(0)
            workers.shutdownNow()
            ApiClientProvider.createService(base)
            interceptor.setSession(previous[0], previous[1], previous[2], previous[3])
        }
    }
}
