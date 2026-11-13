package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.data.remote.ApiClientProvider
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
class NativeLibraryUiTest {
    @get:Rule val compose = createComposeRule()
    private fun id(n: Int) = UUID(82,n.toLong()).toString()
    private lateinit var server: HttpServer; private lateinit var base: String
    private var token: String? = null; private var font = 1f
    private val calls = CopyOnWriteArrayList<String>()
    @Volatile private var posted: JSONObject? = null
    @Volatile private var loseReturn = false
    private fun bookRow() = JSONObject().put("id",id(1)).put("title","Recorded book").put("author","Recorded author").put("category","Category").put("total_copies",2).put("available_copies",2)
    @Before fun start() {
        font=RuntimeEnvironment.getFontScale();RuntimeEnvironment.setFontScale(2f)
        base=ApiClientProvider.activeBaseUrl;token=ApiClientProvider.interceptor.authToken
        server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/") { e ->
            calls.add("${e.requestMethod} ${e.requestURI}")
            val path=e.requestURI.path
            if (loseReturn && e.requestMethod=="PUT") { e.requestBody.readBytes();e.close() }
            else {
                val data:Any=when {
                    path.endsWith("/students") -> {
                        val second=e.requestURI.query.orEmpty().split('&').contains("page=2")
                        JSONObject().put("total",101).put("items",JSONArray().apply {
                            for(n in (if(second) 200..200 else 100..199)) put(JSONObject().put("id",id(n)).put("organization_id",id(50)).put("branch_id",id(51)).put("first_name",if(n==200) "Last" else "Scoped$n").put("last_name","Student").put("enrollment_date","2026-01-01").put("status","ACTIVE"))
                        })
                    }
                    path.endsWith("/books") && e.requestMethod=="POST" -> {
                        posted=JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8))
                        bookRow().put("title",posted!!.getString("title")).put("author",posted!!.getString("author")).put("category",posted!!.getString("category")).put("total_copies",1).put("available_copies",1)
                    }
                    path.endsWith("/books") -> JSONArray().put(bookRow())
                    path.endsWith("/borrow") -> {
                        posted=JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8));JSONObject().put("id",id(3)).put("status","BORROWED").put("due_date","2027-01-15")
                    }
                    else -> JSONArray().apply { posted?.takeIf { it.has("student_id") }?.let { p -> put(JSONObject().put("id",id(3)).put("book_id",id(1)).put("book_title","Recorded book").put("student_id",p.getString("student_id")).put("student_name","Last Student").put("borrow_date","2027-01-01").put("due_date","2027-01-15").put("return_date",JSONObject.NULL).put("status","BORROWED")) } }
                }
                val bytes=JSONObject().put("success",true).put("data",data).toString().toByteArray(Charsets.UTF_8)
                e.responseHeaders.add("Content-Type","application/json");e.sendResponseHeaders(200,bytes.size.toLong());e.responseBody.use { it.write(bytes) };e.close()
            }
        }
        server.start();ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/");ApiClientProvider.interceptor.setSession("synthetic-library-ui")
    }
    @After fun stop() {server.stop(0);ApiClientProvider.createService(base);ApiClientProvider.interceptor.setSession(token);RuntimeEnvironment.setFontScale(font)}
    private fun state(write:Boolean=true)=Okul360UiState(isLoggedIn=true,selectedRole=UserRole.CUSTOM,currentUser=User(id=id(9),organization_id=id(50),roles=listOf(UserRoleInfo("librarian")),permissions=listOf("library.read","student.read")+if(write) listOf("library.write") else emptyList()))
    private fun scroll(tag:String)=compose.onNodeWithTag("library_form").performScrollToNode(hasTestTag(tag))
    @Test @Config(qualifiers="w780dp-h360dp-mdpi") fun borrowerSelectionUsesTheSecondServerPageAndAcknowledgesBeforeClosingAtLargeFont() {
        var closed=0
        compose.setContent {Okul360Theme {NativeLibraryMutationDialog(state(),"BORROW",LibraryBook(id=id(1),title="Recorded book"),null,{closed++})}}
        compose.waitUntil(15000) {calls.any {it.contains("page=2")}}
        scroll("library_student_search");compose.onNodeWithTag("library_student_search").performTextInput("Last")
        compose.waitUntil(15000) {runCatching {scroll("library_student_${id(200)}")}.isSuccess}
        scroll("library_student_${id(200)}");compose.onNodeWithTag("library_student_${id(200)}").performClick()
        scroll("library_save");compose.onNodeWithTag("library_save").performClick()
        compose.waitUntil(15000) {compose.onAllNodesWithTag("library_ack").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("library_ack").assertIsDisplayed();assertEquals(0,closed);assertEquals(id(200),posted!!.getString("student_id"))
        scroll("library_close");compose.onNodeWithTag("library_close").performClick();assertEquals(1,closed)
        assertEquals(1,calls.count {it.startsWith("POST")})
    }
    @Test fun newBookFieldsAreCanonicalAndServerAcknowledgementIsVisible() {
        compose.setContent {Okul360Theme {NativeLibraryMutationDialog(state(),"CREATE",null,null,{})}}
        for((key,value) in listOf("title" to "New recorded title","author" to "Recorded author","category" to "Category")) {scroll("library_$key");compose.onNodeWithTag("library_$key").performTextInput(value)}
        scroll("library_save");compose.onNodeWithTag("library_save").performClick()
        compose.waitUntil(15000) {compose.onAllNodesWithTag("library_ack").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("library_ack").assertIsDisplayed();assertFalse(posted!!.has("id"));assertFalse(posted!!.has("available_copies"));assertEquals(1,posted!!.getInt("total_copies"))
    }
    @Test fun readOnlyCatalogCannotCreateBorrowReturnOrClaimReminderDelivery() {
        compose.setContent {Okul360Theme {NativeLibraryScreen(state(false),{})}}
        compose.waitUntil(15000) {compose.onAllNodesWithTag("library_borrow_${id(1)}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("library_borrow_${id(1)}").assertIsNotEnabled();compose.onAllNodesWithTag("library_new_book").assertCountEquals(0)
        assertTrue(calls.none {it.startsWith("POST")||it.startsWith("PUT")||it.startsWith("DELETE")})
        compose.onAllNodesWithText("SMS",substring=true).assertCountEquals(0)
    }
    @Test fun lostReturnAcknowledgementBlocksAutomaticOrRepeatedSubmission() {
        loseReturn=true
        compose.setContent {Okul360Theme {NativeLibraryMutationDialog(state(),"RETURN",null,BookLoanRecord(id=id(3),book_title="Recorded book",student_name="Actual student"),{})}}
        scroll("library_save");compose.onNodeWithTag("library_save").performClick()
        compose.waitUntil(15000) {compose.onAllNodesWithTag("library_mutation_error").fetchSemanticsNodes().isNotEmpty()}
        scroll("library_save");compose.onNodeWithTag("library_save").assertIsNotEnabled();compose.onAllNodesWithTag("library_ack").assertCountEquals(0)
        assertEquals(1,calls.count {it.startsWith("PUT")})
    }
}
