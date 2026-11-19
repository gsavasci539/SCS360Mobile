package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import com.example.data.remote.ApiClientProvider
import com.example.ui.screens.ParentVerifiedRecordsPane
import com.example.ui.theme.Okul360Theme
import com.sun.net.httpserver.HttpServer
import org.junit.Rule
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

    @Test fun parentDayRendersActivitiesAndDropsADelayedTimelineForThePreviousChild() {
        val first = UUID(41, 1).toString()
        val second = UUID(41, 2).toString()
        val student = mutableStateOf(first)
        val section = mutableStateOf("today-status")
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
            val fields = if (uri.path.endsWith("/today-status")) """"daily_report":$report"""
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
                    Okul360Theme { ParentVerifiedRecordsPane(student.value, section.value) }
                }
            }
            fun awaitText(text: String) {
                compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(text).assertIsDisplayed()
            }
            awaitText("Etkinlikler: Kitap okuma, Çizim")
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
