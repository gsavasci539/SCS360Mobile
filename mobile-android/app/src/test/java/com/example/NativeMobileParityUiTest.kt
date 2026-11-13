package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.SessionManager
import com.example.data.model.*
import com.example.data.remote.ApiClientProvider
import com.example.ui.dialogs.iyzico.HostedPaymentDialog
import com.example.ui.navigation.*
import com.example.ui.screens.*
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.sun.net.httpserver.HttpServer
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-mdpi")
class NativeMobileParityUiTest {
    @get:Rule val compose = createComposeRule()
    private fun id(n: Int) = UUID(33, n.toLong()).toString()
    private lateinit var server: HttpServer
    private lateinit var base: String
    private var token: String? = null
    private var oldFont = 1f
    private val calls = CopyOnWriteArrayList<String>()
    @Volatile private var paid = false
    @Volatile private var sentMessage: String? = null
    @Before fun start() {
        oldFont = RuntimeEnvironment.getFontScale(); RuntimeEnvironment.setFontScale(2f)
        base = ApiClientProvider.activeBaseUrl; token = ApiClientProvider.interceptor.authToken
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e ->
            calls.add("${e.requestMethod} ${e.requestURI.path}")
            val data: Any = when {
                e.requestURI.path.contains("/online/debts/") -> JSONObject().put("id", id(2)).put("remaining_amount", if (paid) "0.00" else "100.00").put("online_payment_available", false)
                e.requestURI.path.contains("/checkouts/") -> JSONObject().put("id", id(3)).put("debt_id", id(2)).put("amount", "100.00").put("status", if (paid) "PAID" else "PENDING").put("payment_id", if (paid) id(4) else JSONObject.NULL)
                e.requestURI.path.endsWith("/medicine-requests") -> JSONObject().put("id", id(8)).put("student_id", id(7)).put("status", "PENDING")
                e.requestURI.path.endsWith("/today-status") -> JSONObject().put("student_id", id(7)).put("date", "2026-11-13").put("attendance", JSONObject.NULL).put("daily_report", JSONObject.NULL).put("today_schedule", JSONArray())
                e.requestURI.path.endsWith("/teachers") -> JSONArray().put(JSONObject().put("id", id(9)).put("name", "Synthetic Teacher"))
                e.requestURI.path.contains("/chat/") -> {
                    if (e.requestMethod == "POST") {
                        sentMessage = JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8)).getString("message")
                        JSONObject().put("id", id(10)).put("sender_id", id(1)).put("receiver_id", id(9)).put("content", sentMessage)
                    } else JSONArray().apply { if (sentMessage != null) put(JSONObject().put("id", id(10)).put("sender_name", "Synthetic Parent").put("content", sentMessage).put("sent_at", "2026-11-13T10:00:00Z")) }
                }
                else -> JSONArray()
            }
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(200, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }; e.close()
        }; server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/"); ApiClientProvider.interceptor.setSession("synthetic-hosted")
        val context = RuntimeEnvironment.getApplication()
        SessionManager(context).saveSession(User(id = id(1), roles = listOf(UserRoleInfo("parent"))))
        val raw = "${ApiClientProvider.activeBaseUrl}|${id(1)}|null|null|${id(2)}"
        val key = java.security.MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
        context.getSharedPreferences("school_checkout_v1", 0).edit().clear().putString(key, JSONObject().put("request_id", id(3)).put("debt_id", id(2)).put("amount", "100.00").toString()).commit()
    }
    @After fun stop() { server.stop(0); ApiClientProvider.createService(base); ApiClientProvider.interceptor.setSession(token); RuntimeEnvironment.setFontScale(oldFont) }
    @Test fun hostedServerReconciliationAndCloseReloadWithoutManualPayment() {
        var reloads = 0; var closed = 0
        compose.setContent { Okul360Theme { HostedPaymentDialog(id(2), { closed++ }, { reloads++ }) } }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("hosted_status").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("hosted_status").assertTextEquals("PENDING")
        paid = true
        compose.onNodeWithTag("hosted_payment").performScrollToNode(hasTestTag("hosted_refresh"))
        compose.onNodeWithTag("hosted_refresh").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(15000) { reloads == 1 }
        compose.onNodeWithTag("hosted_status").assertTextEquals("PAID")
        compose.onNodeWithTag("hosted_close").assertIsDisplayed().performClick()
        assertEquals(1, closed); assertEquals(2, reloads)
        assertTrue(calls.all { it.contains("/payments/online/") })
        assertTrue(calls.none { it.startsWith("POST") && !it.endsWith("/refresh") })
    }
    @Test @Config(qualifiers = "w780dp-h360dp-land-mdpi")
    fun closeFitsLargeFontLandscapeAndStaleWorkspaceNeverReloads() {
        var reloads = 0; var closed = 0; var revision by mutableIntStateOf(0)
        compose.setContent { @Suppress("UNUSED_VARIABLE") val read = revision; Okul360Theme { HostedPaymentDialog(id(2), { closed++ }, { reloads++ }) } }
        compose.onNodeWithTag("hosted_payment").performScrollToNode(hasTestTag("hosted_close"))
        compose.onNodeWithTag("hosted_close").assertIsDisplayed().assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
        compose.runOnIdle { ApiClientProvider.interceptor.invalidateContext(); revision++ }
        compose.onNodeWithTag("hosted_payment").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, closed); assertEquals(0, reloads) }
    }
    @Test fun customPortalContainsOnlyServerGrantedModulesAndRetainsActor() {
        val user = User(id = id(1), roles = listOf(UserRoleInfo("contact_reader")), permissions = listOf("parents.read"))
        var chosen: SubScreen? = null
        compose.setContent { Okul360Theme { CustomRolePortalScreen(user, {}, { chosen = it }, {}) } }
        compose.onNodeWithTag("custom_role_portal").performScrollToNode(hasTestTag("custom_screen_PARENTS"))
        compose.onNodeWithTag("custom_screen_PARENTS").performClick(); assertEquals(SubScreen.PARENTS, chosen)
        compose.onNodeWithTag("custom_tab_FINANCE").assertDoesNotExist()
        compose.onNodeWithTag("custom_screen_USERS").assertDoesNotExist()
        assertEquals(id(1), user.id); assertEquals(UserRole.CUSTOM, SessionRolePolicy.primary(user))
    }
    @Test fun parentPortalDoesNotTreatLegacyNameOrPhoneMatchesAsChildren() {
        val user = User(id = id(1), full_name = "Synthetic Parent", roles = listOf(UserRoleInfo("parent")))
        val foreign = Student(id = id(7), first_name = "Foreign child", parent_name = user.full_name)
        compose.setContent { Okul360Theme { ParentPortalScreen(Okul360UiState(isLoggedIn = true, currentUser = user, selectedRole = UserRole.PARENT, students = listOf(foreign)), {}, { _, _, _ -> }, { _, _, _, _, _ -> }, { _, _, _, _, _ -> }, { _, _, _, _ -> }, { _, _ -> }, onLogout = {}) } }
        try { compose.onNodeWithText("Kayıtlı Öğrenci Bulunamadı").assertExists() }
        catch (failure: AssertionError) { println(compose.onRoot(useUnmergedTree = true).printToString()); throw failure }
        compose.onAllNodesWithText("Foreign child", substring = true).assertCountEquals(0)
    }
    @Test fun missingParentDailyRecordsNeverBecomeInventedMealsHealthOrScores() {
        compose.setContent { Okul360Theme { ParentVerifiedRecordsPane(id(7), "today-status") } }
        compose.waitUntil(15000) { compose.onAllNodesWithText("Bugün için yoklama kaydı yok.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("parent_verified_today-status").performScrollToNode(hasText("Bugün için günlük rapor kaydı yok."))
        compose.onNodeWithText("Bugün için günlük rapor kaydı yok.").assertExists()
        compose.onAllNodesWithText("Neşeli", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("omlet", substring = true).assertCountEquals(0)
    }
    @Test fun readonlyParentManagementCannotCreateContacts() {
        val actor = User(id = id(1), organization_id = id(5), permissions = listOf("parents.read"))
        compose.setContent { Okul360Theme { ParentManagementScreen(Okul360UiState(isLoggedIn = true, currentUser = actor, selectedRole = UserRole.CUSTOM)) } }
        compose.onNodeWithTag("parent_management").performScrollToNode(hasTestTag("parent_new"))
        compose.onNodeWithTag("parent_new").assertIsNotEnabled()
        assertTrue(calls.none { it.startsWith("POST") || it.startsWith("PUT") || it.startsWith("DELETE") })
    }
    @Test fun medicineRequestShowsServerAcknowledgementBeforeReloadAndNeverClaimsAdministration() {
        var reloaded = 0; var closed = 0
        val actor = User(id = id(1), permissions = listOf("parent.medicine_request"))
        compose.setContent { Okul360Theme { com.example.ui.dialogs.NativeParentRequestDialog(id(7), actor, "MEDICINE", { closed++ }, { reloaded++ }) } }
        for ((key, value) in listOf("medicine_name" to "Synthetic request", "dosage" to "Recorded dose", "time_to_give" to "10:00")) {
            compose.onNodeWithTag("parent_request").performScrollToNode(hasTestTag("parent_request_$key"))
            compose.onNodeWithTag("parent_request_$key").performTextInput(value)
        }
        compose.onNodeWithTag("parent_request").performScrollToNode(hasTestTag("parent_request_save"))
        compose.onNodeWithTag("parent_request_save").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("parent_request_ack").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("parent_request_ack").assertIsDisplayed(); assertEquals(0, reloaded)
        compose.onAllNodesWithText("İlaç verildi", substring = true).assertCountEquals(0)
        compose.onNodeWithTag("parent_request").performScrollToNode(hasText("Kapat"))
        compose.onNodeWithText("Kapat").performClick(); assertEquals(1, reloaded); assertEquals(1, closed)
        assertEquals(1, calls.count { it.startsWith("POST") })
    }

    @Test fun anEmptyRealDebtListNeverCreatesSampleInvoices() {
        val child = Student(id = id(7), first_name = "Synthetic", last_name = "Child")
        compose.setContent { Okul360Theme { ParentPaymentsTab(child, Okul360UiState(invoices = emptyList()), {}, {}) } }
        compose.onNodeWithText("Kayıtlı borç bulunamadı").assertExists()
        compose.onAllNodesWithText("FAT-2026", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("4500", substring = true).assertCountEquals(0)
    }
    @Test fun parentMessagingReadsAuthorizedTeachersAndServerHistoryAfterAcknowledgement() {
        val actor = User(id = id(1), permissions = listOf("parent.send_message"))
        compose.setContent { Okul360Theme { ParentVerifiedMessagingPane(id(7), actor) } }
        try { compose.waitUntil(15000) { compose.onAllNodesWithTag("parent_teacher_${id(9)}").fetchSemanticsNodes().isNotEmpty() } }
        catch (failure: Exception) { println("Teacher requests: $calls"); println(compose.onRoot(useUnmergedTree = true).printToString()); throw failure }
        compose.onNodeWithTag("parent_teacher_${id(9)}").performClick()
        compose.onNodeWithTag("parent_messages").performScrollToNode(hasTestTag("parent_message_text"))
        compose.onNodeWithTag("parent_message_text").performTextInput("Synthetic stored message")
        compose.onNodeWithTag("parent_messages").performScrollToNode(hasTestTag("parent_message_send"))
        compose.onNodeWithTag("parent_message_send").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("parent_message_${id(10)}").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("parent_messages").performScrollToNode(hasTestTag("parent_message_${id(10)}"))
        compose.onNodeWithText("Synthetic stored message").assertExists()
        assertEquals(1, calls.count { it.startsWith("POST") })
        assertTrue(calls.count { it.startsWith("GET") && it.contains("/chat/") } >= 2)
    }

}
