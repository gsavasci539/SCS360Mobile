package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.data.remote.ApiClientProvider
import com.example.ui.screens.ParentManagementScreen
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
class NativeParentBranchUiTest {
    @get:Rule val compose = createComposeRule()
    private fun id(n: Int) = UUID(72, n.toLong()).toString()
    private lateinit var server: HttpServer; private lateinit var base: String
    private var token: String? = null; private var oldFont = 1f
    private val calls = CopyOnWriteArrayList<String>()
    @Volatile private var contact: JSONObject? = null
    @Before fun start() {
        oldFont = RuntimeEnvironment.getFontScale(); RuntimeEnvironment.setFontScale(2f)
        base = ApiClientProvider.activeBaseUrl; token = ApiClientProvider.interceptor.authToken
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e ->
            calls.add("${e.requestMethod} ${e.requestURI.path}")
            val data: Any = when {
                e.requestMethod == "POST" -> {
                    contact = JSONObject(e.requestBody.readBytes().toString(Charsets.UTF_8)).put("id", id(4)).put("organization_id", id(1)); JSONObject().put("id", id(4))
                }
                e.requestURI.path == "/api/students" -> JSONArray().put(JSONObject().put("id", id(3)).put("organization_id", id(1)).put("branch_id", id(2)).put("first_name", "Scoped").put("last_name", "Student").put("enrollment_date", "2026-01-01").put("status", "ACTIVE"))
                e.requestURI.path == "/api/students/${id(3)}/parents" -> JSONArray().apply { if (contact != null) put(JSONObject().put("id", id(4))) }
                e.requestURI.path == "/api/parents/${id(4)}" -> contact ?: JSONObject.NULL
                else -> JSONArray().apply { contact?.let { put(it) } }
            }
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray(Charsets.UTF_8)
            e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(200, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }; e.close()
        }
        server.start(); ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/"); ApiClientProvider.interceptor.setSession("synthetic-branch-ui")
    }
    @After fun stop() { server.stop(0); ApiClientProvider.createService(base); ApiClientProvider.interceptor.setSession(token); RuntimeEnvironment.setFontScale(oldFont) }
    private fun actor(studentPermissions: Boolean) = User(id = id(9), organization_id = id(1), branch_ids = listOf(id(2)), roles = listOf(UserRoleInfo("contact_manager")), permissions = listOf("parents.read", "parents.write") + if (studentPermissions) listOf("student.read", "student.write") else emptyList())
    @Test fun branchFormRequiresRealStudentAndRefreshesServerContactAfterAtomicCreateAtLargeFont() {
        compose.setContent { Okul360Theme { ParentManagementScreen(Okul360UiState(isLoggedIn = true, currentUser = actor(true), selectedRole = UserRole.CUSTOM)) } }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("parent_new").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("parent_management").performScrollToNode(hasTestTag("parent_new"))
        compose.waitUntil(15000) { runCatching { compose.onNodeWithTag("parent_new").assertIsEnabled() }.isSuccess }
        compose.onNodeWithTag("parent_new").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("parent_form").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("parent_form").performScrollToNode(hasTestTag("parent_save")); compose.onNodeWithTag("parent_save").assertIsNotEnabled()
        compose.onNodeWithTag("parent_form").performScrollToNode(hasTestTag("parent_create_student_${id(3)}")); compose.onNodeWithTag("parent_create_student_${id(3)}").performClick()
        for ((key, value) in listOf("first_name" to "Branch", "last_name" to "Contact")) {
            compose.onNodeWithTag("parent_form").performScrollToNode(hasTestTag("parent_$key")); compose.onNodeWithTag("parent_$key").performTextInput(value)
        }
        compose.onNodeWithTag("parent_form").performScrollToNode(hasTestTag("parent_save")); compose.onNodeWithTag("parent_save").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("parent_form").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("parent_management").performScrollToNode(hasText("Branch Contact")); compose.onNodeWithText("Branch Contact").assertExists()
        assertEquals(1, calls.count { it.startsWith("POST") }); assertTrue(calls.contains("POST /api/students/${id(3)}/parents")); assertFalse(calls.contains("POST /api/parents"))
    }
    @Test fun branchManagerWithoutStudentPermissionsCannotCreateAnInvisibleContact() {
        compose.setContent { Okul360Theme { ParentManagementScreen(Okul360UiState(isLoggedIn = true, currentUser = actor(false), selectedRole = UserRole.CUSTOM)) } }
        compose.onNodeWithTag("parent_management").performScrollToNode(hasTestTag("parent_new")); compose.onNodeWithTag("parent_new").assertIsNotEnabled()
        assertTrue(calls.none { it.startsWith("POST") || it.startsWith("PUT") || it.startsWith("DELETE") })
    }
}
