package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.example.data.model.User
import com.example.data.remote.ApiClientProvider
import com.example.ui.screens.TeacherDayPane
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w320dp-h480dp-mdpi")
class NativeTeacherDayUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun scheduleOnlyShowsRecordedLessonAtLargeTextWithoutOpeningOtherSections() {
        val oldBase = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val oldToken = interceptor.authToken
        val oldTenant = interceptor.tenantId
        val oldBranch = interceptor.branchId
        val oldWorkspace = interceptor.workspaceOrganizationId
        val requests = ConcurrentLinkedQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.add(exchange.requestURI.path)
            val body = """{"success":true,"data":[{"subject":"Kayıtlı Kimya","start_time":"09:00","end_time":"09:40","class_name":"Bilim","room_number":null}]}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        interceptor.setSession("synthetic-schedule")
        try {
            compose.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Okul360Theme {
                        TeacherDayPane(Okul360UiState(isLoggedIn = true,
                            currentUser = User(id = "fixture", permissions = listOf("teacher.view_schedule"))))
                    }
                }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Kayıtlı Kimya").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Kayıtlı Kimya").assertIsDisplayed()
            compose.onNodeWithText("Sınıflarım").assertDoesNotExist()
            compose.onNodeWithText("Görevler").assertDoesNotExist()
            assertEquals(listOf("/api/teacher/today-schedule"), requests.toList())
        } finally {
            server.stop(0)
            ApiClientProvider.createService(oldBase)
            interceptor.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
        }
    }
}
