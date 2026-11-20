package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.RecordedCollectionMonth
import com.example.data.model.RecordedFinanceSummary
import com.example.data.model.User
import com.example.data.remote.ApiClientProvider
import com.example.ui.components.DashboardApprovalsWidget
import com.example.ui.components.DashboardReceivablesWidget
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h640dp-port-mdpi")
class NativeDashboardApprovalsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun staleBranchAmountsAndLegacyEstimateDoNotAppearInTheReceivablesCard() {
        val current = User(id = "finance-reader", permissions = listOf("finance.read"))
        val amount = BigDecimal("123.45")
        val recorded = RecordedFinanceSummary("2026-11-21", "branch-one", null, null, amount, null, amount, null,
            1, 1, false, listOf(RecordedCollectionMonth("2026-11", amount, 1)))
        val view = mutableStateOf(Okul360UiState(isLoggedIn = true, currentUser = current,
            selectedBranchId = "branch-two", recordedFinanceSummary = recorded))
        compose.setContent { Okul360Theme { DashboardReceivablesWidget(view.value, {}, {}, { _, _ -> }, {}) } }
        compose.onNodeWithText("Finans özeti alınamadı.").assertIsDisplayed()
        compose.onNodeWithText("123,45", substring = true).assertDoesNotExist()
        compose.onNodeWithText("185.000", substring = true).assertDoesNotExist()
        compose.runOnIdle { view.value = view.value.copy(selectedBranchId = "branch-one") }
        compose.onNodeWithText("Kalan borç", substring = true).assertIsDisplayed()
        compose.onNodeWithText("2026-11", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Hızlı tahsilatı aç").assertDoesNotExist()
    }

    @Test fun pendingExcuseUsesBranchAndPersistsBeforeReadingTheListAgain() {
        fun id(suffix: Long) = UUID(87, suffix).toString()
        val requests = CopyOnWriteArrayList<String>()
        val reviewed = AtomicReference<JSONObject>()
        val oldBase = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val oldToken = interceptor.authToken
        val oldTenant = interceptor.tenantId
        val oldBranch = interceptor.branchId
        val oldWorkspace = interceptor.workspaceOrganizationId
        val oldScale = RuntimeEnvironment.getFontScale()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        fun excuse() = JSONObject().put("id", id(5)).put("student_id", id(3)).put("student_name", "İzole öğrenci")
            .put("start_date", "2026-11-20").put("end_date", "2026-11-21").put("description", "İzole mazeret")
        server.createContext("/api/dashboard") { exchange ->
            val uri = exchange.requestURI
            requests.add("${exchange.requestMethod} $uri")
            val data = when {
                uri.path == "/api/dashboard/pending-actions" && uri.query == "branch_id=${id(2)}" -> {
                    val pending = JSONArray()
                    if (reviewed.get() == null) pending.put(excuse().put("status", "PENDING"))
                    JSONObject().put("branch_id", id(2)).put("medicine_requests", JSONObject().put("count", 3))
                        .put("excuse_requests", JSONObject().put("count", pending.length()).put("items", pending))
                }
                uri.path == "/api/dashboard/excuse-requests/${id(5)}" && exchange.requestMethod == "PUT" -> {
                    val body = JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() })
                    reviewed.set(body)
                    excuse().put("status", body.getString("status")).put("review_comment", body.getString("comment"))
                        .put("reviewed_by", id(4))
                }
                else -> {
                    exchange.sendResponseHeaders(404, -1)
                    exchange.close()
                    return@createContext
                }
            }
            val output = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, output.size.toLong())
            exchange.responseBody.use { it.write(output) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            interceptor.setSession("dashboard-approvals-test", id(1), id(2))
            RuntimeEnvironment.setFontScale(2f)
            val user = User(id = id(4), organization_id = id(1),
                permissions = listOf("dashboard.view", "health.read", "students.write", "finance.read"))
            val amount = BigDecimal("123.45")
            val finance = RecordedFinanceSummary("2026-11-21", id(2), null, null, null, null, amount, null,
                0, 1, false, listOf(RecordedCollectionMonth("2026-11", amount, 1)))
            val state = Okul360UiState(isLoggedIn = true, currentUser = user, selectedBranchId = id(2), recordedFinanceSummary = finance)
            compose.setContent {
                Okul360Theme {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        DashboardApprovalsWidget(state, {}, {}, {})
                    }
                }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Bekleyen ilaç talebi · 3").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Bekleyen mazeret · 1").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Mazeretleri göster").performScrollTo().performClick()
            compose.onNodeWithText("Mazereti incele").performScrollTo().performClick()
            val dialog = compose.onNodeWithTag("dashboard_excuse_review")
            val note = hasText("Değerlendirme notu") and hasSetTextAction()
            dialog.performScrollToNode(note)
            compose.onNode(note).performTextInput("İzole karar notu")
            dialog.performScrollToNode(hasText("Talebi onayla"))
            compose.onNodeWithText("Talebi onayla").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Bekleyen mazeret · 0").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Mazeret kararı kaydedildi.").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Tahsilat").performScrollTo().performClick()
            compose.onNodeWithText("123,45", substring = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Kantin POS").assertDoesNotExist()
            assertEquals("APPROVED", reviewed.get().getString("status"))
            assertEquals("İzole karar notu", reviewed.get().getString("comment"))
            assertEquals(listOf("GET /api/dashboard/pending-actions?branch_id=${id(2)}", "PUT /api/dashboard/excuse-requests/${id(5)}",
                "GET /api/dashboard/pending-actions?branch_id=${id(2)}"), requests)
        } finally {
            server.stop(0)
            RuntimeEnvironment.setFontScale(oldScale)
            ApiClientProvider.createService(oldBase)
            interceptor.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
        }
    }
}
