package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.data.remote.ApiClientProvider
import com.example.ui.dialogs.FinanceExpenseDeleteDialog
import com.example.ui.screens.FinancePortalScreen
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.sun.net.httpserver.HttpServer
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36],qualifiers="w360dp-h780dp-mdpi")
class NativeExpenseDeleteUiTest {
    @get:Rule val compose=createComposeRule()
    private fun id(n:Int)=UUID(10,n.toLong()).toString()
    private val first=Expense(id=id(2),title="First actual ledger row",category="Supplies",amount=10.01,date="2026-11-12")
    private val second=first.copy(id=id(3),title="Selected second ledger row",amount=100.01)
    private val state=Okul360UiState(isLoggedIn=true,currentUser=User(id=id(1),permissions=listOf("finance.read","finance.write")),selectedRole=UserRole.FINANCE,expenses=listOf(first,second))
    private lateinit var server:HttpServer; private lateinit var oldBase:String
    private var token:String?=null; private var branch:String?=null; private var workspace:String?=null
    private val paths=CopyOnWriteArrayList<String>(); private val status=AtomicInteger(200)
    private var hold:CountDownLatch?=null
    @Before fun start() {
        val i=ApiClientProvider.interceptor; oldBase=ApiClientProvider.activeBaseUrl; token=i.authToken; branch=i.branchId; workspace=i.workspaceOrganizationId
        server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/") { e ->
            paths.add("${e.requestMethod} ${e.requestURI.path}"); hold?.await(15,TimeUnit.SECONDS)
            val code=status.get(); val bytes="""{"success":${code==200},"data":null}""".toByteArray()
            runCatching { e.responseHeaders.add("Content-Type","application/json"); e.sendResponseHeaders(code,bytes.size.toLong()); e.responseBody.use { it.write(bytes) } }; e.close()
        }; server.start(); ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/"); i.setSession("synthetic-expense-delete-ui")
    }
    @After fun stop() { hold?.countDown(); server.stop(0); ApiClientProvider.createService(oldBase); ApiClientProvider.interceptor.setSession(token,branch=branch,workspace=workspace) }
    private fun scroll(tag:String)=compose.onNodeWithTag("expense_delete_list").performScrollToNode(hasTestTag(tag))
    @Test fun portalDeletesOnlySelectedRowAndWaitsForServerAcknowledgementBeforeReload() {
        var reload=0; status.set(500)
        compose.setContent { Okul360Theme { FinancePortalScreen(state,{ _,_,_-> },{ _,_,_,_,_-> },{ _,_-> },onLogout={},onRetry={ reload++ }) } }
        compose.onNodeWithText("Giderler").performClick()
        compose.onNodeWithTag("finance_expenses_list").performScrollToNode(hasTestTag("finance_delete_expense_${second.id}"))
        compose.onNodeWithTag("finance_delete_expense_${second.id}").performClick()
        compose.onNodeWithTag("expense_delete_title").assertTextEquals(second.title); assertEquals(0,paths.size)
        scroll("expense_delete_submit"); compose.onNodeWithTag("expense_delete_submit").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("expense_delete_error").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("DELETE /api/expenses/${second.id}"),paths.toList()); assertEquals(0,reload)
        compose.onNodeWithTag("expense_delete_ack").assertDoesNotExist(); compose.onNodeWithTag("expense_delete_title").assertTextEquals(second.title)
        status.set(200); scroll("expense_delete_submit"); compose.onNodeWithTag("expense_delete_submit").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("expense_delete_ack").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0,reload); assertEquals(2,paths.size)
        scroll("expense_delete_done"); compose.onNodeWithTag("expense_delete_done").performClick()
        assertEquals(1,reload); assertTrue(paths.all { it=="DELETE /api/expenses/${second.id}" })
        compose.onNodeWithTag("expense_delete_list").assertDoesNotExist()
    }
    @Test @Config(qualifiers="w780dp-h360dp-land-mdpi") fun readonlyLargeFontConfirmationRemainsScrollableAndNeverDeletes() {
        val old=RuntimeEnvironment.getFontScale(); RuntimeEnvironment.setFontScale(2f)
        try {
            compose.setContent { Okul360Theme { FinanceExpenseDeleteDialog(second.copy(title="Long scoped expense ".repeat(15)),state.copy(currentUser=state.currentUser.copy(permissions=listOf("finance.read"))),{}, { fail("Readonly cannot delete") }) } }
            scroll("expense_delete_submit"); val button=compose.onNodeWithTag("expense_delete_submit").assertIsDisplayed().assertIsNotEnabled().fetchSemanticsNode()
            assertTrue(button.boundsInRoot.top>=0 && button.boundsInRoot.bottom<=360f && button.boundsInRoot.height>=48f)
            scroll("expense_delete_close"); compose.onNodeWithTag("expense_delete_close").assertIsDisplayed(); assertEquals(0,paths.size)
        } finally { RuntimeEnvironment.setFontScale(old) }
    }
    @Test fun uncertainDeleteCanRecheckPortalListWithoutAnotherDeleteOrFalseAcknowledgement() {
        var reload=0; status.set(500)
        compose.setContent { Okul360Theme { FinancePortalScreen(state,{ _,_,_-> },{ _,_,_,_,_-> },{ _,_-> },onLogout={},onRetry={ reload++ }) } }
        compose.onNodeWithText("Giderler").performClick()
        compose.onNodeWithTag("finance_expenses_list").performScrollToNode(hasTestTag("finance_delete_expense_${second.id}"))
        compose.onNodeWithTag("finance_delete_expense_${second.id}").performClick()
        scroll("expense_delete_submit"); compose.onNodeWithTag("expense_delete_submit").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("expense_delete_error").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("expense_delete_ack").assertDoesNotExist(); assertEquals(0,reload)
        scroll("expense_delete_check"); compose.onNodeWithTag("expense_delete_check").assertIsDisplayed().performClick()
        assertEquals(1,reload); assertEquals(listOf("DELETE /api/expenses/${second.id}"),paths.toList())
        compose.onNodeWithTag("expense_delete_list").assertDoesNotExist()
    }
    @Test fun staleWorkspaceResponseDoesNotAcknowledgeOrReloadAnotherWorkspace() {
        hold=CountDownLatch(1); var reload=0; var closed=0
        var current by mutableStateOf(state)
        compose.setContent { Okul360Theme { FinanceExpenseDeleteDialog(second,current,{ closed++ },{ reload++ }) } }
        scroll("expense_delete_submit"); compose.onNodeWithTag("expense_delete_submit").performClick()
        compose.waitUntil(10000) { paths.size==1 }
        compose.runOnIdle { ApiClientProvider.interceptor.setSession("synthetic-new-scope",workspace=id(9)); current=current.copy(workspaceOrganizationId=id(9)) }
        hold!!.countDown()
        compose.waitUntil(10000) { closed>0 }
        compose.onNodeWithTag("expense_delete_ack").assertDoesNotExist(); assertEquals(0,reload)
        assertEquals(listOf("DELETE /api/expenses/${second.id}"),paths.toList())
    }
}
