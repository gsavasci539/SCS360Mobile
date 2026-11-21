package com.example

import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.SessionManager
import com.example.data.model.*
import com.example.data.payment.SchoolCheckout
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    @Volatile private var omitPaymentId = false
    @Volatile private var sentMessage: JSONObject? = null
    @Volatile private var dropMessageResponse = false
    @Volatile private var pagedHistory = false
    @Volatile private var parentInvoiceResponse: JSONObject? = null
    @Volatile private var rejectCheckout = false
    @Volatile private var changeBalanceOnInitialize = false
    private val releaseBrowserStatus = CountDownLatch(1)
    @Volatile private var holdBrowserStatus = false
    @Volatile private var failBrowserStatus = false
    @Volatile private var missingSavedCheckout = false
    @Volatile private var nextCheckout: JSONObject? = null
    @Before fun start() {
        oldFont = RuntimeEnvironment.getFontScale(); RuntimeEnvironment.setFontScale(2f)
        base = ApiClientProvider.activeBaseUrl; token = ApiClientProvider.interceptor.authToken
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e ->
            calls.add("${e.requestMethod} ${e.requestURI.path}")
            if (missingSavedCheckout && e.requestMethod == "GET" && e.requestURI.path.contains("/checkouts/")) {
                e.sendResponseHeaders(404, -1)
                e.close()
                return@createContext
            }
            if (failBrowserStatus && e.requestMethod == "GET" && e.requestURI.path.contains("/checkouts/")) {
                failBrowserStatus = false
                e.sendResponseHeaders(503, -1)
                e.close()
                return@createContext
            }
            if (rejectCheckout && e.requestMethod == "POST" && e.requestURI.path == "/api/parent/payments/initialize") {
                nextCheckout = JSONObject(e.requestBody.use { it.readBytes().decodeToString() })
                parentInvoiceResponse = JSONObject("""{
                    "student_id":"${id(7)}","online_payment_available":true,"invoices":[
                        {"debt_id":"${id(2)}","amount":"100.00","paid_amount":"0.00","remaining_amount":"100.00","status":"UNPAID",
                         "payment_in_progress":true,"open_checkout":null}]
                }""")
                val row = parentInvoiceResponse!!.getJSONArray("invoices").getJSONObject(0)
                if (changeBalanceOnInitialize) {
                    row.put("paid_amount", "25.00").put("remaining_amount", "75.00").put("status", "PARTIALLY_PAID")
                        .put("payment_in_progress", false)
                }
                val code = if (changeBalanceOnInitialize) "SCHOOL_CHECKOUT_BALANCE_CHANGED" else "SCHOOL_CHECKOUT_IN_PROGRESS"
                val conflict = """{"success":false,"errors":[{"code":"$code"}]}""".toByteArray()
                e.responseHeaders.add("Content-Type", "application/json")
                e.sendResponseHeaders(409, conflict.size.toLong())
                e.responseBody.use { it.write(conflict) }
                e.close()
                return@createContext
            }
            val data: Any = when {
                e.requestURI.path == "/api/parent/child/${id(7)}/debts/${id(2)}/receipts" ->
                    JSONObject().put("student_id", id(7)).put("debt_id", id(2)).put("total", 1)
                        .put("items", JSONArray().put(JSONObject().put("id", id(12)).put("payment_id", id(4))
                            .put("receipt_number", "TEST-MAKBUZ").put("amount", "25.10").put("remaining_debt", "100.25")
                            .put("issued_at", "2026-11-20T12:00:00+03:00").put("status", "ACTIVE")))
                e.requestURI.path.endsWith("/financial-invoices") -> parentInvoiceResponse ?: JSONObject()
                    .put("student_id", id(7))
                    .put("online_payment_available", true)
                    .put("invoices", JSONArray().put(JSONObject().put("debt_id", id(2))
                        .put("amount", "100.00").put("paid_amount", if (paid) "100.00" else "0.00")
                        .put("remaining_amount", if (paid) "0.00" else "100.00").put("status", if (paid) "PAID" else "UNPAID")))
                e.requestURI.path.contains("/online/debts/") -> JSONObject().put("id", id(2)).put("remaining_amount", if (paid) "0.00" else "100.00").put("online_payment_available", false)
                e.requestURI.path == "/api/parent/payments/initialize" -> {
                    val request = JSONObject(e.requestBody.use { it.readBytes().decodeToString() })
                    nextCheckout = request
                    JSONObject().put("id", request.getString("request_id")).put("debt_id", request.getString("debt_id"))
                        .put("amount", request.getString("amount")).put("status", "PENDING")
                }
                e.requestURI.path.contains("/checkouts/") -> {
                    if (holdBrowserStatus && e.requestMethod == "GET") releaseBrowserStatus.await(30, TimeUnit.SECONDS)
                    JSONObject().put("id", id(3)).put("debt_id", id(2)).put("amount", "100.00")
                        .put("status", if (paid) "COMPLETED" else "PENDING")
                        .put("payment_id", if (paid && !omitPaymentId) id(4) else JSONObject.NULL)
                        .put("can_resume", !paid)
                        .put("checkout_url", "https://sandbox-api.iyzipay.com/checkoutform/fixture")
                }
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
        compose.onNodeWithTag("hosted_status").assertTextEquals("COMPLETED")
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
                ParentPortalScreen(state, {}, { _, _, _ -> },
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
        val dialog = compose.onNodeWithTag("hosted_payment")
        dialog.assertExists()
        compose.waitUntil(15_000) { runCatching { dialog.performScrollToNode(hasTestTag("hosted_status")) }.isSuccess }
        compose.onNodeWithTag("hosted_status").assertTextEquals("PENDING")
        assertEquals(1, calls.count { it == "GET /api/parent/payments/checkouts/${id(3)}" })
        assertTrue(calls.none { it.startsWith("POST ") })
        paid = true
        compose.onNodeWithTag("hosted_payment").performScrollToNode(hasTestTag("hosted_refresh"))
        compose.onNodeWithTag("hosted_refresh").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(15_000) { state.invoices.any { it.student_id == child.id && it.status == "PAID" } }
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("hosted_status") and hasText("COMPLETED")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("hosted_status").assertTextEquals("COMPLETED")
        compose.onNodeWithTag("hosted_payment").assertExists()
        assertEquals(0, portalReloads)
        assertEquals(listOf(child.id), refreshedChildren)
        assertTrue(siblingInvoice in state.invoices)
        assertTrue(calls.any { it == "POST /api/parent/payments/checkouts/${id(3)}/refresh" })
        assertTrue(calls.none { it.contains("/payments/online/") || it.contains("/my-children") || it.contains(sibling.id) })
        assertTrue(calls.none { it.startsWith("POST") && !it.endsWith("/refresh") })
    }

    @Test fun parentRemainingPaymentOpensNewFormWithVerifiedBalance() {
        verifyRemainingPaymentForm(changeSession = false)
    }

    @Test fun parentRemainingPaymentDoesNotOpenFormAfterSessionChange() {
        verifyRemainingPaymentForm(changeSession = true)
    }

    @Test @Config(qualifiers = "w640dp-h360dp-land-mdpi")
    fun parentRemainingPaymentCanScrollAndSubmitInLandscape() {
        verifyRemainingPaymentForm(changeSession = false, checkLandscapeLayout = true)
    }

    private fun verifyRemainingPaymentForm(changeSession: Boolean, checkLandscapeLayout: Boolean = false) {
        paid = true
        parentInvoiceResponse = JSONObject("""{
            "student_id":"${id(7)}","online_payment_available":true,"invoices":[
                {"debt_id":"${id(2)}","paid_amount":"25.00","amount":"100.00","remaining_amount":"75.00","status":"PARTIALLY_PAID"}]
        }""")
        var revision by mutableIntStateOf(0)
        val dismissals = CopyOnWriteArrayList<Int>()
        compose.setContent {
            val sessionRevision = revision
            Okul360Theme { HostedPaymentDialog(id(2), { dismissals.add(sessionRevision) }, parentStudentId = id(7)) }
        }
        val dialog = compose.onNodeWithTag("hosted_payment")
        val checkBalance = hasText("Kalan borcu kontrol et")
        compose.waitUntil(15_000) { runCatching { dialog.performScrollToNode(checkBalance) }.isSuccess }
        holdBrowserStatus = changeSession
        compose.onNode(checkBalance).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        try {
            compose.waitUntil(15_000) { calls.count { it == "GET /api/parent/payments/checkouts/${id(3)}" } == 2 }
            if (changeSession) {
                compose.runOnIdle { ApiClientProvider.interceptor.invalidateContext(); revision++ }
                releaseBrowserStatus.countDown()
                compose.waitUntil(15_000) { dismissals.isNotEmpty() }
                dialog.assertDoesNotExist()
                assertEquals(1, calls.count { it.endsWith("/financial-invoices") })
                assertNull(nextCheckout)
                return
            }
            val identity = "Kimlik / pasaport numarası"
            compose.waitUntil(15_000) { runCatching { dialog.performScrollToNode(hasText(identity)) }.isSuccess }
            assertNull(nextCheckout)
            for ((label, value) in listOf(identity to "TEST-001", "Telefon" to "5555555555",
                "Fatura adresi" to "Synthetic test address", "Şehir" to "Test", "Ülke" to "TR")) {
                dialog.performScrollToNode(hasText(label))
                val field = compose.onNode(hasText(label) and hasSetTextAction())
                if (checkLandscapeLayout) field.performScrollTo().assertIsDisplayed()
                field.performTextInput(value)
            }
            dialog.performScrollToNode(hasTestTag("hosted_prepare"))
            val submit = compose.onNodeWithTag("hosted_prepare").performScrollTo().assertIsEnabled()
            if (checkLandscapeLayout) submit.assertIsDisplayed().performClick()
            else submit.performSemanticsAction(SemanticsActions.OnClick)
            compose.waitUntil(15_000) { nextCheckout != null }
            val request = requireNotNull(nextCheckout)
            assertEquals("75.00", request.getString("amount"))
            assertEquals(id(2), request.getString("debt_id"))
            assertNotEquals(id(3), request.getString("request_id"))
            assertEquals(1, calls.count { it == "POST /api/parent/payments/initialize" })
        } finally {
            releaseBrowserStatus.countDown()
        }
    }

    @Test fun parentCanStartRemainingPaymentOnlyAfterVerifyingTheCompletedCheckout() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val service = SchoolCheckout(context, id(2), id(7))
        paid = true
        assertTrue(runCatching { service.prepareRemainingPayment() }.isFailure)
        assertEquals(id(3), service.saved()?.getString("request_id"))
        parentInvoiceResponse = JSONObject("""{
            "student_id":"${id(7)}","online_payment_available":true,"invoices":[
                {"debt_id":"${id(2)}","amount":"100.00","paid_amount":"25.00","remaining_amount":"75.00","status":"PARTIALLY_PAID"}]
        }""")
        omitPaymentId = true
        assertTrue(runCatching { service.prepareRemainingPayment() }.isFailure)
        assertEquals(id(3), service.saved()?.getString("request_id"))
        omitPaymentId = false
        val balance = service.prepareRemainingPayment()
        assertNull(service.saved())
        assertTrue(calls.none { it.startsWith("POST") })
        service.start(balance.getString("remaining_amount"), JSONObject())
        val request = requireNotNull(nextCheckout)
        assertEquals("75.00", request.getString("amount"))
        assertNotEquals(id(3), request.getString("request_id"))
        assertEquals(listOf("POST /api/parent/payments/initialize"), calls.filter { it.startsWith("POST") })
    }

    @Test fun parentBrowserReturnChecksTheSameCheckoutOnce() {
        verifyParentBrowserReturn(changeSession = false)
    }

    @Test fun parentBrowserReturnDiscardsAResultAfterSessionChange() {
        verifyParentBrowserReturn(changeSession = true)
    }

    @Test fun parentBrowserRetryChecksTheSameCheckout() {
        verifyParentBrowserReturn(changeSession = false, retry = true)
    }

    @Test fun parentBrowserRetryDiscardsTheResultAfterSessionChange() {
        verifyParentBrowserReturn(changeSession = true, retry = true)
    }

    private fun verifyParentBrowserReturn(changeSession: Boolean, retry: Boolean = false) {
        val host = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this)
        }
        val launches = CopyOnWriteArrayList<Intent>()
        val reloads = CopyOnWriteArrayList<String>()
        val dismissals = CopyOnWriteArrayList<Int>()
        var revision by mutableIntStateOf(0)
        compose.runOnUiThread { host.lifecycle.currentState = Lifecycle.State.RESUMED }
        compose.setContent {
            val sessionRevision = revision
            val context = LocalContext.current
            val browser = remember(context) {
                object : ContextWrapper(context) {
                    override fun startActivity(intent: Intent) { launches.add(intent) }
                }
            }
            CompositionLocalProvider(LocalContext provides browser, LocalLifecycleOwner provides host) {
                Okul360Theme {
                    HostedPaymentDialog(id(2), { dismissals.add(sessionRevision) }, { reloads.add(id(7)) },
                        parentStudentId = id(7))
                }
            }
        }
        val checkoutPath = "/api/parent/payments/checkouts/${id(3)}"
        val dialog = compose.onNodeWithTag("hosted_payment")
        val open = hasText("Güvenli ödeme sayfasını aç")
        compose.waitUntil(15_000) { runCatching { dialog.performScrollToNode(open) }.isSuccess }
        compose.onNode(open).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(15_000) { launches.isNotEmpty() }
        assertEquals(Intent.ACTION_VIEW, launches.single().action)
        assertEquals(1, calls.count { it == "GET $checkoutPath" })
        val beforeReturn = calls.size
        failBrowserStatus = retry
        holdBrowserStatus = changeSession && !retry
        paid = true
        compose.runOnIdle {
            host.lifecycle.currentState = Lifecycle.State.STARTED
            host.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        try {
            compose.waitUntil(15_000) { calls.drop(beforeReturn).contains("GET $checkoutPath") }
            if (retry) {
                val retryAction = hasText("Ödeme durumunu yeniden sorgula")
                compose.waitUntil(15_000) {
                    runCatching { dialog.performScrollToNode(retryAction); compose.onNode(retryAction).assertIsEnabled() }.isSuccess
                }
                assertTrue(reloads.isEmpty())
                compose.onNodeWithTag("hosted_refresh").assertDoesNotExist()
                compose.onNodeWithTag("hosted_prepare").assertDoesNotExist()
                holdBrowserStatus = changeSession
                compose.onNode(retryAction).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
                compose.waitUntil(15_000) { calls.drop(beforeReturn).count { it == "GET $checkoutPath" } == 2 }
            }
            if (changeSession) {
                compose.runOnIdle { ApiClientProvider.interceptor.invalidateContext(); revision++ }
                releaseBrowserStatus.countDown()
                compose.waitUntil(15_000) { dismissals.isNotEmpty() }
                dialog.assertDoesNotExist()
                assertTrue(reloads.isEmpty())
            } else {
                compose.waitUntil(15_000) { reloads.isNotEmpty() }
                dialog.performScrollToNode(hasTestTag("hosted_status"))
                compose.onNodeWithTag("hosted_status").assertTextEquals("COMPLETED")
                compose.runOnIdle {
                    host.lifecycle.currentState = Lifecycle.State.STARTED
                    host.lifecycle.currentState = Lifecycle.State.RESUMED
                }
                assertEquals(listOf(id(7)), reloads)
            }
            assertEquals(List(if (retry) 2 else 1) { "GET $checkoutPath" }, calls.drop(beforeReturn))
        } finally {
            releaseBrowserStatus.countDown()
        }
    }

    @Test fun parentRetriesMissingCheckoutWithTheSavedIdentifier() {
        missingSavedCheckout = true
        compose.setContent { Okul360Theme { HostedPaymentDialog(id(2), {}, parentStudentId = id(7)) } }
        val dialog = compose.onNodeWithTag("hosted_payment")
        val identity = "Kimlik / pasaport numarası"
        compose.waitUntil(15_000) {
            runCatching { dialog.performScrollToNode(hasText("Ödeme kaydı sunucuda bulunamadı.", substring = true)) }.isSuccess
        }
        assertTrue(calls.none { it.startsWith("POST ") })
        for ((label, value) in listOf(identity to "TEST-001", "Telefon" to "5555555555",
            "Fatura adresi" to "Synthetic test address", "Şehir" to "Test", "Ülke" to "TR")) {
            dialog.performScrollToNode(hasText(label))
            compose.onNode(hasText(label) and hasSetTextAction()).performTextInput(value)
        }
        dialog.performScrollToNode(hasTestTag("hosted_prepare"))
        compose.onNodeWithTag("hosted_prepare").performScrollTo().assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(15_000) { nextCheckout != null }
        assertEquals(id(3), requireNotNull(nextCheckout).getString("request_id"))
        assertEquals(listOf("POST /api/parent/payments/initialize"), calls.filter { it.startsWith("POST ") })
    }

    @Test fun parentCheckoutConflictReloadsOnlyItsDebtList() {
        verifyParentCheckoutRejection(balanceChanged = false)
    }

    @Test fun parentCheckoutBalanceChangeShowsNoticeAndReloadsCurrentAmount() {
        verifyParentCheckoutRejection(balanceChanged = true)
    }

    private fun verifyParentCheckoutRejection(balanceChanged: Boolean) {
        val child = Student(id = id(7), first_name = "Deniz")
        val user = User(id = id(1), permissions = listOf("parent.view_finances", "parent.make_payment"))
        val preferences = RuntimeEnvironment.getApplication().getSharedPreferences("school_checkout_v1", 0)
        assertTrue(preferences.edit().clear().commit())
        rejectCheckout = true
        changeBalanceOnInitialize = balanceChanged
        val snapshot = runBlocking { NativeParentPortalClient().invoiceSnapshot(child) }
        var state by mutableStateOf(Okul360UiState(isLoggedIn = true, currentUser = user, selectedRole = UserRole.PARENT,
            parentChildren = listOf(child), parentInvoiceStates = mapOf(child.id to ParentInvoiceLoadState.LOADED),
            parentInvoiceSnapshots = mapOf(child.id to snapshot)))
        val refreshed = mutableListOf<String>()
        compose.setContent {
            val scope = rememberCoroutineScope()
            Okul360Theme {
                ParentPortalScreen(state, {}, { _, _, _ -> },
                    onRefreshInvoices = { studentId ->
                        refreshed.add(studentId)
                        state = state.copy(parentInvoiceStates = state.parentInvoiceStates + (studentId to ParentInvoiceLoadState.LOADING))
                        scope.launch {
                            state = state.withParentInvoiceResult(studentId, Result.success(NativeParentPortalClient().invoiceSnapshot(child)))
                        }
                    }, onLogout = {})
            }
        }
        val list = compose.onNodeWithTag("parent_invoices")
        list.performScrollToNode(hasText("Online Öde"))
        compose.onNodeWithText("Online Öde").performScrollTo().performClick()
        val dialog = compose.onNodeWithTag("hosted_payment")
        for ((label, value) in listOf(
            "Kimlik / pasaport numarası" to "TEST-001", "Telefon" to "5555555555",
            "Fatura adresi" to "Synthetic test address", "Şehir" to "Test", "Ülke" to "TR",
        )) {
            dialog.performScrollToNode(hasText(label))
            compose.onNode(hasText(label) and hasSetTextAction()).performTextInput(value)
        }
        dialog.performScrollToNode(hasTestTag("hosted_prepare"))
        compose.onNodeWithTag("hosted_prepare").performScrollTo().assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick)
        try {
            compose.waitUntil(20_000) {
                state.parentInvoiceSnapshots[child.id]?.rows?.single()?.let {
                    if (balanceChanged) it.remaining.toPlainString() == "75.00" else it.paymentInProgress
                } == true
            }
        } catch (failure: ComposeTimeoutException) {
            val dialogState = runCatching { dialog.printToString(maxDepth = 6) }.getOrDefault("Dialog closed")
            throw AssertionError("Checkout reload timed out. Calls: $calls. Reloaded: $refreshed. $dialogState", failure)
        }
        compose.onNodeWithTag("hosted_payment").assertDoesNotExist()
        if (balanceChanged) {
            list.performScrollToNode(hasText("Kalan borç değişti. Güncel tutarı kontrol edin."))
            list.performScrollToNode(hasText("Online Öde"))
            assertEquals("100.00", nextCheckout?.optString("expected_remaining_amount"))
            assertEquals(listOf(child.id), refreshed)
            assertTrue(preferences.all.isEmpty())
            assertEquals(1, calls.count { it.startsWith("POST") })
            return
        }
        list.performScrollToNode(hasText("Bu borç için açık bir ödeme işlemi var."))
        compose.onNodeWithText("Online Öde").assertDoesNotExist()
        compose.onNodeWithText("Ödeme sonucunu kontrol et").assertDoesNotExist()
        assertEquals(listOf(child.id), refreshed)
        val refreshedRow = state.parentInvoiceSnapshots[child.id]!!.rows.single()
        assertEquals(snapshot.rows.single().remaining, refreshedRow.remaining)
        assertNull(refreshedRow.openCheckout)
        assertEquals(listOf("POST /api/parent/payments/initialize"), calls.filter { it.startsWith("POST") })
        assertTrue(calls.none { it.contains("/checkouts/") })
        assertTrue(preferences.all.isEmpty())

        parentInvoiceResponse!!.getJSONArray("invoices").getJSONObject(0).put("payment_in_progress", false)
        val reload = hasText("Borçları yenile")
        list.performScrollToNode(reload)
        compose.onNode(reload).performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(20_000) {
            state.parentInvoiceStates[child.id] == ParentInvoiceLoadState.LOADED &&
                state.parentInvoiceSnapshots[child.id]?.rows?.single()?.paymentInProgress == false
        }
        list.performScrollToNode(hasText("Online Öde"))
        compose.onNodeWithText("Online Öde").assertExists()
        assertEquals(listOf(child.id, child.id), refreshed)
        assertEquals(4, calls.count { it == "GET /api/parent/child/${child.id}/financial-invoices" })
        assertEquals(1, calls.count { it.startsWith("POST") })
    }

    @Test fun parentCanReadSavedCheckoutWhileOnlinePaymentIsUnavailable() {
        verifyParentReadsCheckout(fromServer = false)
    }

    @Test fun parentFindsOpenCheckoutOnAnotherDevice() {
        verifyParentReadsCheckout(fromServer = true, hasLocalAttempt = false)
    }

    @Test fun parentCanContinueOpenCheckoutOnOriginalDevice() {
        verifyParentReadsCheckout(fromServer = true)
    }

    private fun verifyParentReadsCheckout(fromServer: Boolean, hasLocalAttempt: Boolean = true) {
        val child = Student(id = id(7), first_name = "Deniz")
        val user = User(id = id(1), permissions = listOf("parent.view_finances", "parent.make_payment"))
        val preferences = RuntimeEnvironment.getApplication().getSharedPreferences("school_checkout_v1", 0)
        if (!hasLocalAttempt) assertTrue(preferences.edit().clear().commit())
        parentInvoiceResponse = JSONObject("""{
            "student_id":"${child.id}","online_payment_available":$fromServer,"invoices":[
                {"debt_id":"${id(5)}","amount":50,"paid_amount":0,"remaining_amount":50,"status":"CANCELLED"},
                {"debt_id":"${id(2)}","amount":100,"paid_amount":0,"remaining_amount":100,"status":"UNPAID"}]
        }""").apply {
            if (fromServer) getJSONArray("invoices").getJSONObject(1).put("open_checkout",
                JSONObject().put("request_id", id(3)).put("debt_id", id(2)).put("amount", "100.00"))
        }
        val snapshot = runBlocking { NativeParentPortalClient().invoiceSnapshot(child) }
        assertEquals(fromServer, snapshot.rows.last().openCheckout != null)
        val state = Okul360UiState(isLoggedIn = true, currentUser = user, selectedRole = UserRole.PARENT,
            parentChildren = listOf(child), parentInvoiceStates = mapOf(child.id to ParentInvoiceLoadState.LOADED),
            parentInvoiceSnapshots = mapOf(child.id to snapshot))
        val refreshed = mutableListOf<String>()
        compose.setContent {
            Okul360Theme {
                ParentPortalScreen(state, {}, { _, _, _ -> },
                    onRefreshInvoices = { refreshed.add(it) }, onLogout = {})
            }
        }
        val list = compose.onNodeWithTag("parent_invoices")
        if (fromServer && hasLocalAttempt) {
            val continuation = hasText("Ödeme işlemine dön")
            list.performScrollToNode(continuation)
            compose.onNode(continuation).assertIsDisplayed()
            return
        }
        val recovery = hasText("Ödeme sonucunu kontrol et")
        list.performScrollToNode(hasTestTag("parent_invoice_${id(5)}"))
        compose.onAllNodes(recovery and hasAnyAncestor(hasTestTag("parent_invoice_${id(5)}"))).assertCountEquals(0)
        list.performScrollToNode(recovery)
        compose.onAllNodesWithText("Online Öde").assertCountEquals(0)
        compose.onNode(recovery).assertIsDisplayed().performClick()
        compose.waitUntil(20_000) { calls.contains("GET /api/parent/payments/checkouts/${id(3)}") }
        compose.waitUntil(20_000) {
            runCatching {
                compose.onNodeWithTag("hosted_payment").performScrollToNode(hasTestTag("hosted_status"))
            }.isSuccess
        }
        compose.onNodeWithTag("hosted_status").assertTextEquals("PENDING")
        compose.onNodeWithText("Kimlik / pasaport numarası").assertDoesNotExist()
        compose.onNodeWithText("Ödeme sayfasını hazırla / tekrar dene").assertDoesNotExist()
        paid = true
        compose.onNodeWithTag("hosted_refresh").assertIsDisplayed().performClick()
        compose.waitUntil(15_000) { refreshed.size == 1 }
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("hosted_status") and hasText("COMPLETED")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("hosted_status").assertTextEquals("COMPLETED")
        assertEquals(listOf(child.id), refreshed)
        assertEquals(2, calls.count { it == "GET /api/parent/payments/checkouts/${id(3)}" })
        assertTrue(calls.none { it.startsWith("POST") || it.contains("/payments/online/") })
        if (!hasLocalAttempt) assertTrue(preferences.all.isEmpty())
    }
    @Test fun completedStatusWithoutRecordedPaymentNeverShowsSuccess() {
        paid = true
        omitPaymentId = true
        var reloads = 0
        compose.setContent { Okul360Theme { HostedPaymentDialog(id(2), {}, { reloads++ }) } }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Tahsilat doğrulanamadı").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("hosted_status").assertDoesNotExist()
        compose.onNodeWithText("Tahsilat doğrulandı · Kapat").assertDoesNotExist()
        assertEquals(0, reloads)
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
        compose.setContent { Okul360Theme { ParentPortalScreen(Okul360UiState(isLoggedIn = true, currentUser = user, selectedRole = UserRole.PARENT, students = listOf(foreign)), {}, { _, _, _ -> }, onLogout = {}) } }
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
    @Test fun parentCanOpenReceiptForAPartialPayment() {
        val child = Student(id = id(7), first_name = "Deniz")
        parentInvoiceResponse = JSONObject("""{
            "student_id":"${child.id}","online_payment_available":false,"invoices":[
                {"debt_id":"${id(2)}","amount":125.35,"paid_amount":25.10,"remaining_amount":100.25,"status":"PARTIALLY_PAID"}]
        }""")
        val snapshot = runBlocking { NativeParentPortalClient().invoiceSnapshot(child) }
        val state = Okul360UiState(isLoggedIn = true, selectedRole = UserRole.PARENT, parentChildren = listOf(child),
            currentUser = User(id = id(1), permissions = listOf("parent.view_finances")),
            parentInvoiceStates = mapOf(child.id to ParentInvoiceLoadState.LOADED),
            parentInvoiceSnapshots = mapOf(child.id to snapshot))
        compose.setContent {
            Okul360Theme {
                ParentPortalScreen(state, {}, { _, _, _ -> }, onLogout = {})
            }
        }
        compose.onNodeWithTag("parent_invoices").performScrollToNode(hasText("Makbuz İncele"))
        compose.onNodeWithText("Makbuz İncele").assertIsDisplayed().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("receipt_pdf_${id(12)}").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Makbuz: TEST-MAKBUZ").assertExists()
        val prefix = "/api/parent/child/${child.id}/debts/${id(2)}/receipts"
        assertTrue(calls.contains("GET $prefix"))
        assertTrue(calls.all { it.startsWith("GET /api/parent/") })
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
                ParentPortalScreen(state, {}, { _, _, _ -> },
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
