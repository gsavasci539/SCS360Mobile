package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.data.remote.ApiClientProvider
import com.example.ui.dialogs.FinanceExpenseEditDialog
import com.example.ui.screens.FinancePortalScreen
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.sun.net.httpserver.HttpServer
import org.json.JSONObject
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36],qualifiers="w360dp-h780dp-mdpi")
class NativeExpenseEditUiTest {
    @get:Rule val compose=createComposeRule()
    private fun id(n:Int)=UUID(12,n.toLong()).toString()
    private val state=Okul360UiState(isLoggedIn=true,currentUser=User(id=id(1),organization_id=id(2),permissions=listOf("finance.read","finance.write")),selectedRole=UserRole.FINANCE,
        expenses=listOf(Expense(id=id(3),title="Stale list title",category="Supplies",amount=99.0,date="2027-01-14")))
    private lateinit var server:HttpServer;private lateinit var oldBase:String
    private var token:String?=null;private var branch:String?=null;private var workspace:String?=null
    private val puts=CopyOnWriteArrayList<String>();private var hold:CountDownLatch?=null
    private var firstFailure=true
    private var row=JSONObject()
    @Before fun start() {
        row=JSONObject().put("id",id(3)).put("organization_id",id(2)).put("branch_id",JSONObject.NULL).put("created_by",id(1))
            .put("title","Fresh server title").put("description","Fresh server title").put("category","Supplies").put("amount","100.01")
            .put("date","2027-01-15").put("expense_date","2027-01-15T12:34:56+00:00").put("notes","Stored ref")
        val i=ApiClientProvider.interceptor;oldBase=ApiClientProvider.activeBaseUrl;token=i.authToken;branch=i.branchId;workspace=i.workspaceOrganizationId
        server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/") { e ->
            hold?.await(15,TimeUnit.SECONDS);var code=200
            if(e.requestMethod=="PUT") {
                val body=e.requestBody.readBytes().toString(Charsets.UTF_8);puts.add(body);val patch=JSONObject(body)
                for(k in patch.keys()) { row.put(k,patch.get(k));if(k=="description")row.put("title",patch.get(k)) }
                if(firstFailure) { code=500;firstFailure=false }
            }
            val bytes=JSONObject().put("success",code==200).put("data",row).toString().toByteArray()
            runCatching { e.responseHeaders.add("Content-Type","application/json");e.sendResponseHeaders(code,bytes.size.toLong());e.responseBody.use { it.write(bytes) } };e.close()
        };server.start();ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/");i.setSession("synthetic-expense-editor")
    }
    @After fun stop() { hold?.countDown();server.stop(0);ApiClientProvider.createService(oldBase);ApiClientProvider.interceptor.setSession(token,branch=branch,workspace=workspace) }
    private fun scroll(tag:String)=compose.onNodeWithTag("expense_edit_list").performScrollToNode(hasTestTag(tag))
    private fun ready() { compose.waitUntil(10000) { compose.onAllNodesWithTag("expense_edit_amount").fetchSemanticsNodes().isNotEmpty() } }
    @Test fun actualPortalReadsCurrentDetailPreservesDraftCentsNotesAndWaitsForAckBeforeClosing() {
        var reload=0
        compose.setContent { Okul360Theme { FinancePortalScreen(state,{ _,_,_-> },{ _,_,_,_,_-> },{ _,_-> },onLogout={},onRetry={ reload++ }) } }
        compose.onNodeWithText("Giderler").performClick();compose.onNodeWithTag("finance_expenses_list").performScrollToNode(hasTestTag("finance_edit_expense_${id(3)}"))
        compose.onNodeWithTag("finance_edit_expense_${id(3)}").performClick();ready()
        scroll("expense_edit_title");compose.onNodeWithTag("expense_edit_title").assertTextContains("Fresh server title")
        scroll("expense_edit_amount");compose.onNodeWithTag("expense_edit_amount").assertTextContains("100.01").performTextReplacement("100.001")
        scroll("expense_edit_save");compose.onNodeWithTag("expense_edit_save").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("expense_edit_error").fetchSemanticsNodes().isNotEmpty() };assertEquals(0,puts.size);assertEquals(0,reload)
        scroll("expense_edit_amount");compose.onNodeWithTag("expense_edit_amount").performTextReplacement("110.02")
        scroll("expense_edit_notes");compose.onNodeWithTag("expense_edit_notes").assertTextContains("Stored ref").performTextReplacement("New ref")
        scroll("expense_edit_save");compose.onNodeWithTag("expense_edit_save").performClick()
        compose.waitUntil(10000) { puts.size==1 && compose.onAllNodesWithTag("expense_edit_error").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0,reload);compose.onNodeWithTag("expense_edit_ack").assertDoesNotExist()
        scroll("expense_edit_save");compose.onNodeWithTag("expense_edit_save").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("expense_edit_ack").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(puts[0],puts[1]);assertEquals(0,reload);assertEquals(setOf("amount","notes"),JSONObject(puts[0]).keys().asSequence().toSet())
        scroll("expense_edit_done");compose.onNodeWithTag("expense_edit_done").performClick();assertEquals(1,reload)
        compose.onNodeWithTag("expense_edit_list").assertDoesNotExist()
    }
    @Test @Config(qualifiers="w780dp-h360dp-land-mdpi") fun readonlyLargeFontEditorKeepsActionsVisibleAndNeverWrites() {
        val old=RuntimeEnvironment.getFontScale();RuntimeEnvironment.setFontScale(2f)
        try {
            compose.setContent { Okul360Theme { FinanceExpenseEditDialog(id(3),state.copy(currentUser=state.currentUser.copy(permissions=listOf("finance.read"))),{},{ fail("Readonly") }) } }
            scroll("expense_edit_save");val button=compose.onNodeWithTag("expense_edit_save").assertIsDisplayed().assertIsNotEnabled().fetchSemanticsNode()
            assertTrue(button.boundsInRoot.top>=0 && button.boundsInRoot.bottom<=360f && button.boundsInRoot.height>=48f)
            scroll("expense_edit_close");compose.onNodeWithTag("expense_edit_close").assertIsDisplayed();assertEquals(0,puts.size)
        } finally { RuntimeEnvironment.setFontScale(old) }
    }
    @Test fun scopeChangeDiscardsLateReadAndNeverOpensAnEditableForeignRecord() {
        hold=CountDownLatch(1);var current by mutableStateOf(state);var closed=0
        compose.setContent { Okul360Theme { FinanceExpenseEditDialog(id(3),current,{ closed++ },{ fail("Stale") }) } }
        compose.runOnIdle { current=current.copy(workspaceOrganizationId=id(50)) };hold!!.countDown()
        compose.waitUntil(10000) { closed>0 };compose.onNodeWithTag("expense_edit_list").assertDoesNotExist();assertEquals(0,puts.size)
    }
}
