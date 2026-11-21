package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.remote.ApiClientProvider
import com.example.ui.screens.OrganizationPreferencesCard
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w640dp-h360dp-land-mdpi")
class NativeOrganizationPreferencesUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun scrollsToTheUploadLimitAndVerifiesSavedValueWithLargeText() {
        val organization = UUID(83, 1).toString()
        val auth = ApiClientProvider.interceptor
        val originalUrl = ApiClientProvider.activeBaseUrl
        val originalToken = auth.authToken
        val originalTenant = auth.tenantId
        val originalBranch = auth.branchId
        val originalWorkspace = auth.workspaceOrganizationId
        val oldScale = RuntimeEnvironment.getFontScale()
        val maximum = AtomicInteger(3)
        val calls = CopyOnWriteArrayList<String>()
        val writes = CopyOnWriteArrayList<JSONObject>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/settings/preferences") { request ->
            if (request.requestMethod == "PUT") {
                val payload = JSONObject(request.requestBody.bufferedReader().use { it.readText() })
                writes += payload
                maximum.set(payload.getJSONObject("changes").getInt("max_upload_size_mb"))
            }
            calls += request.requestMethod
            val values = JSONObject().put("school_name", "Kurum").put("academic_year", JSONObject.NULL)
                .put("default_currency", "TRY").put("max_upload_size_mb", maximum.get()).put("allowed_file_types", "pdf,png")
                .put("receipt_email_enabled", JSONObject.NULL).put("overdue_reminders_enabled", JSONObject.NULL)
                .put("failed_notification_alerts_enabled", JSONObject.NULL)
            val policy = JSONObject().put("max_size_mb", 3).put("allowed_extensions", JSONArray(listOf("pdf", "png")))
            val data = JSONObject().put("organization_id", organization).put("values", values).put("upload_policy", policy)
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            request.responseHeaders.set("Content-Type", "application/json")
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            RuntimeEnvironment.setFontScale(2f)
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-settings", organization)
            compose.setContent { MaterialTheme { OrganizationPreferencesCard(organization, canWrite = true) } }
            compose.onNodeWithText("Kurum tercihlerini aç").performClick()
            val editor = compose.onNodeWithTag("student_document_max_size")
            compose.waitUntil(15_000) { calls.isNotEmpty() }
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("student_document_max_size").fetchSemanticsNodes().isNotEmpty() }
            editor.performScrollTo().assertIsDisplayed().performTextReplacement("2")
            compose.onNodeWithText("Kaydet").performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Kurum tercihleri kaydedildi.").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Kurum tercihleri kaydedildi.").performScrollTo().assertIsDisplayed()
            assertEquals(listOf("GET", "PUT", "GET"), calls.toList())
            assertEquals(organization, writes.single().getString("organization_id"))
            assertEquals(3, writes.single().getJSONObject("expected").getInt("max_upload_size_mb"))
            assertEquals(2, maximum.get())
        } finally {
            RuntimeEnvironment.setFontScale(oldScale)
            ApiClientProvider.createService(originalUrl)
            auth.setSession(originalToken, originalTenant, originalBranch, originalWorkspace)
            server.stop(0)
        }
    }
}
