package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.data.remote.ApiClientProvider
import com.example.ui.dialogs.FinanceExpenseCreateDialog
import com.example.ui.screens.FinancePortalScreen
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.sun.net.httpserver.HttpServer
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36],qualifiers="w360dp-h780dp-mdpi")
class NativeExpenseUiTest {
    @get:Rule val compose=createComposeRule()
    private fun id(n:Int)=UUID(9,n.toLong()).toString()
    private val state=Okul360UiState(isLoggedIn=true,currentUser=User(id=id(1),permissions=listOf("finance.read","finance.write")),selectedRole=UserRole.FINANCE)
    private lateinit var server:HttpServer; private lateinit var oldBase:String
    private var token:String?=null; private var branch:String?=null; private var workspace:String?=null
    private val requests=AtomicInteger(); private val posts=AtomicInteger()
    @Before fun start() {
        val i=ApiClientProvider.interceptor; oldBase=ApiClientProvider.activeBaseUrl; token=i.authToken; branch=i.branchId; workspace=i.workspaceOrganizationId
        server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/") { e ->
            requests.incrementAndGet(); if(e.requestMethod=="POST") posts.incrementAndGet()
            val bytes="""{"success":true,"data":{"total":0,"items":[],"allows_organization_wide":true}}""".toByteArray()
            e.responseHeaders.add("Content-Type","application/json"); e.sendResponseHeaders(200,bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
        }; server.start(); ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/"); i.setSession("synthetic-expense-ui")
    }
    @After fun stop() { server.stop(0); ApiClientProvider.createService(oldBase); ApiClientProvider.interceptor.setSession(token,branch=branch,workspace=workspace) }
    private fun scroll(tag:String)=compose.onNodeWithTag("expense_create_list").performScrollToNode(hasTestTag(tag))
    @Test fun blankScopeDateAmountAndCategoryNeverInventOrSubmitExpense() {
        var recorded=0; var closed=0
        compose.setContent { Okul360Theme { FinanceExpenseCreateDialog(state,{ closed++ },{ recorded++ }) } }
        compose.waitUntil(10000) { compose.onAllNodesWithTag("expense_scope_org").fetchSemanticsNodes().isNotEmpty() }
        for(tag in listOf("expense_title","expense_category","expense_amount","expense_date")) {
            scroll(tag); compose.onNodeWithTag(tag).assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText,androidx.compose.ui.text.AnnotatedString("")))
        }
        scroll("expense_submit"); compose.onNodeWithTag("expense_submit").performClick()
        assertEquals(0,posts.get()); assertEquals(0,recorded); assertEquals(0,closed)
    }
    @Test fun actualFinanceExpenseTabUsesOnlyServerStateAndGuardsUnknownRows() {
        var current by mutableStateOf(state.copy(expenses=listOf(Expense(id=id(2),branch_id=id(3),title="Synthetic server expense",category="Synthetic",amount=100.01,date="2027-01-15"))))
        compose.setContent { Okul360Theme { FinancePortalScreen(current,{ _,_,_-> },{ _,_,_,_,_-> },{ _,_-> },onLogout={}) } }
        compose.onNodeWithText("Giderler").performClick()
        compose.onNodeWithText("Synthetic server expense").assertExists()
        compose.onNodeWithText("Personel Maaş & SGK Ödemeleri").assertDoesNotExist()
        compose.onNodeWithText("Kampüs Kira & Bina Aidatı").assertDoesNotExist()
        compose.runOnIdle { current=current.copy(expenses=emptyList(),isFinanceRowsLoading=true) }
        compose.onNodeWithText("Gider kayıtları yükleniyor…").assertExists(); compose.onNodeWithText("Bu kapsamda gider kaydı yok.").assertDoesNotExist()
        compose.runOnIdle { current=current.copy(isFinanceRowsLoading=false,financeRowsError="Synthetic unverified rows") }
        compose.onNodeWithText("Synthetic unverified rows").assertExists(); compose.onNodeWithText("Bu kapsamda gider kaydı yok.").assertDoesNotExist()
        compose.runOnIdle { current=current.copy(financeRowsError=null) }
        compose.onNodeWithText("Bu kapsamda gider kaydı yok.").assertExists()
        compose.onNodeWithTag("finance_new_expense").performClick(); compose.onNodeWithTag("expense_create_list").assertExists()
        compose.runOnIdle { current=current.copy(workspaceOrganizationId=id(8)) }
        compose.onNodeWithTag("expense_create_list").assertDoesNotExist(); assertEquals(0,posts.get())
    }
    @Test @Config(qualifiers="w780dp-h360dp-land-mdpi") fun readonlyLargeFontLandscapeKeepsActionsInsideViewport() {
        val old=RuntimeEnvironment.getFontScale(); RuntimeEnvironment.setFontScale(2f)
        try {
            compose.setContent { Okul360Theme { FinanceExpenseCreateDialog(state.copy(currentUser=state.currentUser.copy(permissions=emptyList())),{},{ fail("Readonly") }) } }
            scroll("expense_submit"); val button=compose.onNodeWithTag("expense_submit").assertIsNotEnabled().assertIsDisplayed().fetchSemanticsNode()
            assertTrue(button.boundsInRoot.top>=0 && button.boundsInRoot.bottom<=360f && button.boundsInRoot.height>=48f)
            scroll("expense_close"); compose.onNodeWithTag("expense_close").assertIsDisplayed(); assertEquals(0,requests.get())
        } finally { RuntimeEnvironment.setFontScale(old) }
    }
}
