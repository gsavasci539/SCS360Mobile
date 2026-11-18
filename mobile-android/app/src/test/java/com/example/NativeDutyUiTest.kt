package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeStaffClient
import com.example.ui.screens.NativeDutyDialog
import com.example.ui.theme.Okul360Theme
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w640dp-h360dp-mdpi")
class NativeDutyUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun landscapeFormRequiresRealOptionsAndConfirmsAfterListReadback() {
        val branch = UUID(90, 1).toString()
        val teacher = UUID(90, 2).toString()
        val sends = AtomicInteger()
        val confirmed = AtomicReference<String>()
        val saved = AtomicReference<JSONObject>()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val token = auth.authToken
        val tenant = auth.tenantId
        val originalBranch = auth.branchId
        val workspace = auth.workspaceOrganizationId
        val font = RuntimeEnvironment.getFontScale()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { request ->
            try {
                val data = when {
                    request.requestMethod == "POST" -> {
                        sends.incrementAndGet()
                        JSONObject(request.requestBody.readBytes().toString(Charsets.UTF_8)).apply {
                            put("id", getString("request_id"))
                            put("status", "ACTIVE")
                            saved.set(this)
                        }
                    }
                    request.requestURI.path.endsWith("/options") -> {
                        val isTeacher = request.requestURI.query.contains("branch_id=")
                        val option = JSONObject().put("id", if (isTeacher) teacher else branch)
                            .put("name", if (isTeacher) "Kayıtlı öğretmen" else "Kayıtlı şube")
                        JSONObject().put("page", 1).put("total_pages", 1).put("items", JSONArray().put(option))
                    }
                    else -> JSONArray().put(saved.get())
                }
                val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray(Charsets.UTF_8)
                request.sendResponseHeaders(200, bytes.size.toLong())
                request.responseBody.use { it.write(bytes) }
            } finally {
                request.close()
            }
        }
        server.start()
        RuntimeEnvironment.setFontScale(1.5f)
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        auth.setSession("synthetic-duty")
        try {
            val api = NativeStaffClient()
            compose.setContent { Okul360Theme {
                NativeDutyDialog(api, branch, onDismiss = {}) { confirmed.set(it.single().teacher_id) }
            } }
            val fields = compose.onNodeWithTag("staff_duty_fields")
            val save = compose.onNodeWithTag("staff_duty_save")
            save.assertIsNotEnabled()
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasText("Öğretmen seçin") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Öğretmen seçin").performClick()
            compose.onNodeWithText("Kayıtlı öğretmen").performClick()
            fields.performScrollToNode(hasText("Gün seçin"))
            compose.onNodeWithText("Gün seçin").performClick()
            compose.onNodeWithText("Pazartesi").performClick()
            fields.performScrollToNode(hasText("Nöbet yeri seçin"))
            compose.onNodeWithText("Nöbet yeri seçin").performClick()
            compose.onNodeWithText("Bahçe & Giriş Kapısı").performClick()
            val start = hasSetTextAction() and hasText("Başlangıç (SS:DD)")
            val end = hasSetTextAction() and hasText("Bitiş (SS:DD)")
            fields.performScrollToNode(start)
            compose.onNode(start).performTextInput("08:00")
            fields.performScrollToNode(end)
            compose.onNode(end).performTextInput("15:30")
            assertEquals(0, sends.get())
            save.assertIsEnabled().performClick()
            compose.waitUntil(5_000) { confirmed.get() == teacher }
            assertEquals(1, sends.get())
            assertEquals(branch, saved.get().getString("branch_id"))
        } finally {
            RuntimeEnvironment.setFontScale(font)
            server.stop(0)
            ApiClientProvider.createService(base)
            auth.setSession(token, tenant, originalBranch, workspace)
        }
    }
}
