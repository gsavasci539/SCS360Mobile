package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.data.remote.ApiClientProvider
import com.example.ui.dialogs.FinanceDebtEditDialog
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360UiState
import com.sun.net.httpserver.HttpServer
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36],qualifiers="w360dp-h780dp-mdpi")
class FinanceDebtEditUiTest {
    @get:Rule val compose=createComposeRule()
    private fun id(n:Int)=UUID(7,n.toLong()).toString()
    private val invoice=Invoice(id=id(1),student_id=id(2),student_name="Synthetic Student",class_name="Synthetic Class",
        amount=100.01,paid_amount=0.0,status="PENDING",due_date="2027-01-15",description="Synthetic note")
    private val state=Okul360UiState(isLoggedIn=true,currentUser=User(id=id(3),permissions=listOf("finance.write")),selectedRole=UserRole.FINANCE)
    private lateinit var server:HttpServer
    private lateinit var oldBase:String
    private var token:String?=null; private var branch:String?=null; private var workspace:String?=null
    private val requests=AtomicInteger(); private val puts=AtomicInteger(); private val accept=AtomicBoolean(false)
    private val release=CountDownLatch(1); private val bodies=java.util.concurrent.CopyOnWriteArrayList<JSONObject>()
    private var due="2027-01-15"; private var description="Synthetic note"
    @Before fun start() {
        val i=ApiClientProvider.interceptor; oldBase=ApiClientProvider.activeBaseUrl; token=i.authToken; branch=i.branchId; workspace=i.workspaceOrganizationId
        server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/") { e ->
            requests.incrementAndGet(); var code=200
            val data=if(e.requestMethod=="PUT") {
                val body=JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8)); bodies.add(body); puts.incrementAndGet()
                if(!accept.get()) { code=422; JSONObject() }
                else { release.await(15,TimeUnit.SECONDS); due=body.getString("due_date"); description=body.getString("description"); JSONObject().put("id",id(1)) }
            } else JSONObject().put("id",id(1)).put("organization_id",id(4)).put("branch_id",id(5)).put("student_id",id(2))
                .put("student_name","Synthetic Student").put("class_name","Synthetic Class").put("debt_type","EDUCATION")
                .put("description",description).put("amount","100.01").put("paid_amount","0.00").put("remaining_amount","100.01")
                .put("status","UNPAID").put("due_date",due).put("created_at","2026-11-12T12:00:00")
            val bytes=JSONObject().put("success",code<400).put("data",data).toString().toByteArray()
            e.responseHeaders.add("Content-Type","application/json"); e.sendResponseHeaders(code,bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
        }; server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/"); i.setSession("synthetic-debt-edit-ui")
    }
    @After fun stop() { release.countDown(); server.stop(0); ApiClientProvider.createService(oldBase); ApiClientProvider.interceptor.setSession(token,branch=branch,workspace=workspace) }
    private fun scroll(tag:String)=compose.onNodeWithTag("debt_edit_list").performScrollToNode(hasTestTag(tag))
    @Test fun centsValidationFailureAndPendingAckPreserveDraft() {
        var recorded=0; var closed=0; var shown by mutableStateOf(true)
        compose.setContent { Okul360Theme { if(shown) FinanceDebtEditDialog(state,invoice,{ closed++; shown=false },{ recorded++; shown=false }) } }
        scroll("debt_edit_amount")
        compose.onNodeWithTag("debt_edit_amount").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText,androidx.compose.ui.text.AnnotatedString("100.01")))
        compose.onNodeWithTag("debt_edit_amount").performTextReplacement("100.009"); scroll("debt_edit_save"); compose.onNodeWithTag("debt_edit_save").performClick()
        assertEquals(0,requests.get()); assertEquals(0,recorded); assertEquals(0,closed)
        scroll("debt_edit_amount"); compose.onNodeWithTag("debt_edit_amount").performTextReplacement("100,01")
        scroll("debt_edit_due"); compose.onNodeWithTag("debt_edit_due").performTextReplacement("2027-01-16")
        scroll("debt_edit_description"); compose.onNodeWithTag("debt_edit_description").performTextReplacement("Synthetic updated note")
        scroll("debt_edit_save"); compose.onNodeWithTag("debt_edit_save").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("debt_edit_error").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0,recorded); assertEquals(0,closed)
        scroll("debt_edit_amount"); compose.onNodeWithTag("debt_edit_amount").assertTextContains("100,01")
        accept.set(true); scroll("debt_edit_save"); compose.onNodeWithTag("debt_edit_save").performClick()
        compose.waitUntil(10000) { puts.get()==2 }; compose.onNodeWithTag("debt_edit_save").assertIsNotEnabled()
        assertEquals(0,recorded); assertEquals(0,closed); release.countDown()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("debt_edit_ack").fetchSemanticsNodes().isNotEmpty() }
        scroll("debt_edit_done"); compose.onNodeWithTag("debt_edit_done").performClick(); assertEquals(1,recorded); assertEquals(0,closed)
        assertTrue(bodies.all { it.keys().asSequence().toSet()==setOf("due_date","description") })
        assertTrue(bodies.all { it.getString("due_date")=="2027-01-16" && it.getString("description")=="Synthetic updated note" })
    }
    @Test @Config(qualifiers="w780dp-h360dp-land-mdpi") fun largeFontReadonlyAndWorkspaceChangeNeverSubmit() {
        val old=RuntimeEnvironment.getFontScale(); RuntimeEnvironment.setFontScale(2f)
        try {
            var current by mutableStateOf(state.copy(currentUser=state.currentUser.copy(permissions=emptyList())))
            var closed=0
            compose.setContent { Okul360Theme { FinanceDebtEditDialog(current,invoice,{ closed++ },{ fail("Readonly") }) } }
            scroll("debt_edit_save")
            val node=compose.onNodeWithTag("debt_edit_save").assertIsNotEnabled().assertIsDisplayed().fetchSemanticsNode()
            assertTrue(node.boundsInRoot.top>=0 && node.boundsInRoot.bottom<=360f && node.boundsInRoot.height>=48f)
            scroll("debt_edit_close"); compose.onNodeWithTag("debt_edit_close").assertIsDisplayed()
            compose.runOnIdle { current=current.copy(workspaceOrganizationId=id(9)) }
            compose.onNodeWithText("Tahakkuku düzenle").assertDoesNotExist(); assertEquals(1,closed); assertEquals(0,requests.get())
        } finally { RuntimeEnvironment.setFontScale(old) }
    }
}
