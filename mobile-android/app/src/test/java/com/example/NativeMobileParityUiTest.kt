package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.SessionManager
import com.example.data.model.*
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeParentPortalClient
import com.example.data.remote.ParentInvoiceSnapshot
import com.example.ui.dialogs.iyzico.HostedPaymentDialog
import com.example.ui.navigation.*
import com.example.ui.screens.*
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.example.ui.viewmodel.ParentInvoiceLoadState
import com.example.ui.viewmodel.withParentInvoiceResult
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    @Volatile private var sentMessage: JSONObject? = null
    @Volatile private var dropMessageResponse = false
    @Volatile private var pagedHistory = false
    @Volatile private var parentInvoiceResponse: JSONObject? = null
    @Before fun start() {
        oldFont = RuntimeEnvironment.getFontScale(); RuntimeEnvironment.setFontScale(2f)
        base = ApiClientProvider.activeBaseUrl; token = ApiClientProvider.interceptor.authToken
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e ->
            calls.add("${e.requestMethod} ${e.requestURI.path}")
            val data: Any = when {
                e.requestURI.path.endsWith("/financial-invoices") -> parentInvoiceResponse ?: JSONObject()
                    .put("student_id", id(7))
                    .put("online_payment_available", true)
                    .put("invoices", JSONArray().put(JSONObject().put("debt_id", id(2))
                        .put("amount", "100.00").put("paid_amount", if (paid) "100.00" else "0.00")
                        .put("remaining_amount", if (paid) "0.00" else "100.00").put("status", if (paid) "PAID" else "UNPAID")))
                e.requestURI.path.contains("/online/debts/") -> JSONObject().put("id", id(2)).put("remaining_amount", if (paid) "0.00" else "100.00").put("online_payment_available", false)
                e.requestURI.path.contains("/checkouts/") -> JSONObject().put("id", id(3)).put("debt_id", id(2)).put("amount", "100.00").put("status", if (paid) "PAID" else "PENDING").put("payment_id", if (paid) id(4) else JSONObject.NULL)
                e.requestURI.path.endsWith("/medicine-requests") -> JSONObject().put("id", id(8)).put("student_id", id(7)).put("status", "PENDING")
                e.requestURI.path.endsWith("/today-status") -> JSONObject().put("student_id", id(7)).put("date", "2026-11-13").put("attendance", JSONObject.NULL).put("daily_report", JSONObject.NULL).put("today_schedule", JSONArray())
                e.requestURI.path.endsWith("/teachers") -> JSONArray().put(JSONObject().put("id", id(9)).put("name", "Synthetic Teacher"))
                e.requestURI.path.endsWith("/chat-contacts") -> {
                    check(e.requestURI.path.startsWith("/api/teacher/"))
                    check(e.requestURI.query.contains("branch_id=${id(8)}"))
                    val contact = JSONObject().put("student_id", id(7)).put("student_name", "Fixture learner")
                        .put("parent_id", id(9)).put("parent_name", "Fixture guardian")
                        .put("class_name", "Fixture class").put("branch_id", id(8))
                    JSONObject().put("items", JSONArray().put(contact)).put("page", 1).put("total_pages", 1)
                }
                e.requestURI.path.contains("/chat/") -> {
                    if (e.requestURI.path.startsWith("/api/teacher/")) {
                        check(e.requestURI.query.contains("parent_id=${id(9)}"))
                        check(e.requestURI.query.contains("branch_id=${id(8)}"))
                    }
                    if (pagedHistory) {
                        val query = e.requestURI.query.orEmpty()
                        check(query.contains("limit=50"))
                        val items = if (query.contains("before_id=${id(100)}")) listOf(90) else (100..149).toList()
                        JSONArray().apply { items.forEach { number ->
                            put(JSONObject().put("id", id(number)).put("sender_id", id(9)).put("receiver_id", id(1))
                                .put("is_current_user", false).put("content", "Fixture message $number"))
                        } }
                    } else if (e.requestMethod == "POST") {
                        val draft = JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8))
                        val row = JSONObject().put("id", draft.getString("request_id")).put("sender_id", id(1))
                            .put("receiver_id", id(9)).put("content", draft.getString("message")).put("is_current_user", true)
                            .put("sender_name", "Synthetic Parent").put("sent_at", "2026-11-13T10:00:00Z")
                        sentMessage = row
                        if (dropMessageResponse) { e.close(); return@createContext }
                        row
                    } else if (e.requestURI.path.contains("/sent/")) {
                        requireNotNull(sentMessage).also { check(e.requestURI.path.endsWith(it.getString("id"))) }
                    } else JSONArray().apply { sentMessage?.let { put(it) } }
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

    @Test fun parentCheckoutRefreshKeepsTheSelectedChildAndDialog() {
        val sibling = Student(id = id(6), first_name = "Birinci", class_name = "A")
        val child = Student(id = id(7), first_name = "İkinci", class_name = "A")
        val siblingInvoice = Invoice(id = id(5), student_id = sibling.id, amount = 50.0)
        val user = User(id = id(1), permissions = listOf("parent.view_children", "parent.view_finances", "parent.make_payment"))
        val snapshot = runBlocking { NativeParentPortalClient().invoiceSnapshot(child) }
        var state by mutableStateOf(Okul360UiState(
            isLoggedIn = true, currentUser = user, selectedRole = UserRole.PARENT, parentChildren = listOf(sibling, child),
            invoices = listOf(siblingInvoice, Invoice(id = id(2), student_id = child.id, amount = 100.0, paid_amount = 0.0, status = "UNPAID")),
            parentInvoiceStates = mapOf(sibling.id to ParentInvoiceLoadState.LOADED, child.id to ParentInvoiceLoadState.LOADED),
            parentInvoiceSnapshots = mapOf(child.id to snapshot),
        ))
        var portalReloads = 0
        val refreshedChildren = mutableListOf<String>()
        compose.setContent {
            val scope = rememberCoroutineScope()
            Okul360Theme {
                ParentPortalScreen(state, {}, { _, _, _ -> }, { _, _, _, _, _ -> }, { _, _, _, _, _ -> }, { _, _, _, _ -> },
                    chatbotTab = ParentNavTab.PAYMENTS, chatbotStudentId = child.id, onRefresh = { portalReloads++ },
                    onRefreshInvoices = { studentId ->
                        refreshedChildren.add(studentId)
                        state = state.copy(parentInvoiceStates = state.parentInvoiceStates + (studentId to ParentInvoiceLoadState.LOADING))
                        scope.launch {
                            val refreshed = NativeParentPortalClient().invoiceSnapshot(child)
                            state = state.withParentInvoiceResult(studentId, Result.success(refreshed))
                        }
                    }, onLogout = {})
            }
        }
        compose.onNodeWithTag("parent_invoices").performScrollToNode(hasText("Online Öde"))
        compose.onNodeWithTag("parent_invoices").performTouchInput { swipeUp() }
        compose.onNodeWithText("Online Öde").assertIsDisplayed().performClick()
        compose.onNodeWithTag("hosted_payment").assertExists()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("hosted_status").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("hosted_status").assertTextEquals("PENDING")
        paid = true
        compose.onNodeWithTag("hosted_payment").performScrollToNode(hasTestTag("hosted_refresh"))
        compose.onNodeWithTag("hosted_refresh").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(15_000) { state.invoices.any { it.student_id == child.id && it.status == "PAID" } }
        compose.onNodeWithTag("hosted_status").assertTextEquals("PAID")
        compose.onNodeWithTag("hosted_payment").assertExists()
        assertEquals(0, portalReloads)
        assertEquals(listOf(child.id), refreshedChildren)
        assertTrue(siblingInvoice in state.invoices)
        assertTrue(calls.any { it == "POST /api/parent/payments/checkouts/${id(3)}/refresh" })
        assertTrue(calls.none { it.contains("/payments/online/") || it.contains("/my-children") || it.contains(sibling.id) })
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
        compose.setContent { Okul360Theme { ParentPortalScreen(Okul360UiState(isLoggedIn = true, currentUser = user, selectedRole = UserRole.PARENT, students = listOf(foreign)), {}, { _, _, _ -> }, { _, _, _, _, _ -> }, { _, _, _, _, _ -> }, { _, _, _, _ -> }, onLogout = {}) } }
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
        val state = Okul360UiState(currentUser = User(permissions = listOf("parent.view_finances")),
            parentInvoiceStates = mapOf(child.id to ParentInvoiceLoadState.LOADED),
            parentInvoiceSnapshots = mapOf(child.id to ParentInvoiceSnapshot(emptyList(), false)))
        compose.setContent { Okul360Theme { ParentPaymentsTab(child, state, {}, {}) } }
        compose.onNodeWithText("Kayıtlı borç bulunamadı").assertExists()
        compose.onAllNodesWithText("FAT-2026", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("4500", substring = true).assertCountEquals(0)
    }

    @Test fun parentInvoiceRowsUseRecordedCentsStatusAndOnlineAvailability() {
        val child = Student(id = id(7), first_name = "Deniz")
        val user = User(permissions = listOf("parent.view_finances", "parent.make_payment"))
        val apiData = JSONObject("""{
            "student_id":"${child.id}","online_payment_available":false,"invoices":[
                {"debt_id":"${id(2)}","amount":125.35,"paid_amount":25.10,"remaining_amount":100.25,
                 "status":"PARTIALLY_PAID","description":"Kasım ödemesi"},
                {"debt_id":"${id(5)}","amount":1.4,"paid_amount":0.0,"remaining_amount":1.4,
                 "status":"CANCELLED","description":"İptal edilen kayıt"}]
        }""")
        parentInvoiceResponse = apiData
        var state by mutableStateOf(Okul360UiState(
            currentUser = user,
            parentInvoiceStates = mapOf(child.id to ParentInvoiceLoadState.LOADED),
            parentInvoiceSnapshots = mapOf(child.id to runBlocking { NativeParentPortalClient().invoiceSnapshot(child) }),
        ))
        var openedDebt: String? = null
        compose.setContent { Okul360Theme { ParentPaymentsTab(child, state, { openedDebt = it.id }, {}) } }
        val list = compose.onNodeWithTag("parent_invoices")
        val partial = hasTestTag("parent_invoice_${id(2)}")
        val cancelled = hasTestTag("parent_invoice_${id(5)}")
        list.performScrollToNode(partial)
        compose.onNodeWithText("Kısmi ödendi").assertExists()
        compose.onNodeWithText("125,35", substring = true).assertExists()
        compose.onNodeWithText("Kalan: ₺100,25").assertExists()
        compose.onAllNodesWithText("Online Öde").assertCountEquals(0)
        compose.onAllNodesWithText("Hatırlatıcı").assertCountEquals(0)

        parentInvoiceResponse = JSONObject(apiData.toString()).put("online_payment_available", true)
        val available = runBlocking { NativeParentPortalClient().invoiceSnapshot(child) }
        compose.runOnIdle { state = state.copy(parentInvoiceSnapshots = mapOf(child.id to available)) }
        list.performScrollToNode(cancelled)
        compose.onNodeWithText("İptal").assertExists()
        compose.onAllNodes(hasText("Online Öde") and hasAnyAncestor(cancelled)).assertCountEquals(0)
        list.performScrollToNode(hasText("Online Öde"))
        compose.onNodeWithText("Online Öde").assert(hasAnyAncestor(partial)).performClick()
        assertEquals(id(2), openedDebt)
        compose.runOnIdle { state = state.copy(currentUser = user.copy(permissions = listOf("parent.view_finances"))) }
        compose.onAllNodesWithText("Online Öde").assertCountEquals(0)
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
        compose.waitUntil(15000) { calls.any { it.startsWith("GET") && it.contains("/sent/") } }
        val row = "parent_message_${requireNotNull(sentMessage).getString("id")}"
        compose.waitUntil(15000) { compose.onAllNodesWithTag(row).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("parent_messages").performScrollToNode(hasTestTag(row))
        compose.onNodeWithText("Synthetic stored message").assertExists()
        assertEquals(1, calls.count { it.startsWith("POST") })
        assertTrue(calls.count { it.startsWith("GET") && it.contains("/chat/") } >= 2)
    }

    @Test @Config(qualifiers = "w360dp-h780dp-mdpi")
    fun parentMessagingChecksLostResponseWithoutAnotherPost() {
        dropMessageResponse = true
        val actor = User(id = id(1), permissions = listOf("parent.send_message"))
        compose.setContent { Okul360Theme { ParentVerifiedMessagingPane(id(7), actor) } }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("parent_teacher_${id(9)}").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("parent_teacher_${id(9)}").performClick()
        val list = compose.onNodeWithTag("parent_messages")
        list.performScrollToNode(hasTestTag("parent_message_text"))
        compose.onNodeWithTag("parent_message_text").performTextInput("Synthetic recoverable message")
        list.performScrollToNode(hasTestTag("parent_message_choose_file"))
        compose.onNodeWithTag("parent_message_choose_file").assertIsDisplayed()
            .assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
        list.performScrollToNode(hasTestTag("parent_message_send"))
        compose.onNodeWithTag("parent_message_send").performClick()
        compose.waitUntil(15000) {
            list.performScrollToIndex(4)
            compose.onAllNodesWithTag("parent_message_check").fetchSemanticsNodes().isNotEmpty()
        }
        list.performScrollToNode(hasTestTag("parent_message_check"))
        compose.onNodeWithTag("parent_message_check").performClick()
        compose.waitUntil(15000) { calls.any { it.startsWith("GET") && it.contains("/sent/") } }
        val row = "parent_message_${requireNotNull(sentMessage).getString("id")}"
        compose.waitUntil(15000) { compose.onAllNodesWithTag(row).fetchSemanticsNodes().isNotEmpty() }
        list.performScrollToNode(hasTestTag(row))
        assertEquals(1, calls.count { it.startsWith("POST") })
    }

    @Test @Config(qualifiers = "w640dp-h360dp-land-mdpi")
    fun teacherMessagingOpensScopedContactAndHidesComposerWithoutWritePermission() {
        val actor = User(id = id(1), organization_id = id(40), permissions = listOf("students.read", "messaging.read"))
        val state = Okul360UiState(isLoggedIn = true, currentUser = actor, selectedRole = UserRole.TEACHER,
            selectedBranchId = id(8))
        compose.setContent { Okul360Theme { DirectMessagingScreen(state, {}) } }
        compose.onNodeWithTag("open_staff_student_chat").assertIsDisplayed().performClick()
        val contact = "staff_chat_contact_${id(7)}_${id(9)}"
        compose.waitUntil(15000) { compose.onAllNodesWithTag(contact).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(contact).performClick()
        val history = "GET /api/teacher/chat/${id(7)}"
        compose.waitUntil(15000) { calls.contains(history) }
        compose.onNodeWithTag("parent_messages").performScrollToNode(hasText("Bu hesapla gönderim yapılamaz."))
        compose.onNodeWithTag("parent_message_send").assertDoesNotExist()
        compose.onNodeWithTag("parent_message_choose_file").assertDoesNotExist()
        assertFalse(calls.any { it.startsWith("POST ") })
    }

    @Test fun parentMessagingLoadsOlderPage() {
        pagedHistory = true
        val actor = User(id = id(1), permissions = listOf("parent.send_message"))
        compose.setContent { Okul360Theme { ParentVerifiedMessagingPane(id(7), actor) } }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("parent_teacher_${id(9)}").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("parent_teacher_${id(9)}").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("student_chat_older").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("student_chat_older").performClick()
        val list = compose.onNodeWithTag("parent_messages")
        compose.waitUntil(15000) {
            list.performScrollToIndex(0)
            compose.onAllNodesWithTag("parent_message_${id(90)}").fetchSemanticsNodes().isNotEmpty()
        }
        list.performScrollToNode(hasTestTag("parent_message_${id(149)}"))
        compose.onNodeWithText("Fixture message 149").assertExists()
        assertFalse(calls.any { it.startsWith("POST ") })
    }

    @Test fun parentChatbotShortcutOpensTheSelectedChildWithoutSending() {
        val actor = User(id = id(1), permissions = listOf("parent.send_message"))
        val state = Okul360UiState(isLoggedIn = true, currentUser = actor, selectedRole = UserRole.PARENT,
            parentChildren = listOf(Student(id = id(6), first_name = "First"), Student(id = id(7), first_name = "Selected")))
        var opened = 0
        compose.setContent {
            Okul360Theme {
                ParentPortalScreen(state, {}, { _, _, _ -> }, { _, _, _, _, _ -> }, { _, _, _, _, _ -> }, { _, _, _, _ -> },
                    chatbotTab = ParentNavTab.MESSAGING, chatbotStudentId = id(7),
                    onChatbotDestinationOpened = { opened++ }, onLogout = {})
            }
        }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("parent_teacher_${id(9)}").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("parent_messages").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, opened) }
        assertTrue(calls.any { it == "GET /api/parent/child/${id(7)}/teachers" })
        assertFalse("Unexpected messaging request: $calls", calls.any {
            (it.startsWith("POST") && (it.contains("/chat/") || it.contains("/messages/"))) ||
                it.contains("/child/${id(6)}/teachers")
        })
    }

}
