package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.remote.ApiClientProvider
import com.example.ui.dialogs.NativeCashCollectionDialog
import com.example.ui.theme.Okul360Theme
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
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w320dp-h600dp-mdpi")
class NativeCashCollectionUiTest {
    @get:Rule val compose = createComposeRule()
    private val actor = UUID(1, 1).toString(); private val debt = UUID(2, 2).toString()
    private lateinit var server: HttpServer; private lateinit var oldBase: String
    private var oldToken: String? = null; private var oldFont = 1f
    private val posts = CopyOnWriteArrayList<String>()
    @Before fun start() {
        oldFont = RuntimeEnvironment.getFontScale(); RuntimeEnvironment.setFontScale(2f)
        oldBase = ApiClientProvider.activeBaseUrl; oldToken = ApiClientProvider.interceptor.authToken
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { e ->
            var code = 200
            val data = if (e.requestMethod == "GET") JSONObject().put("id", debt).put("amount", "100.00").put("paid_amount", "0.00").put("status", "UNPAID")
            else {
                val body = e.requestBody.readBytes().toString(Charsets.UTF_8); posts.add(body)
                if (posts.size == 1) code = 503
                JSONObject().put("payment_id", JSONObject(body).getString("request_id")).put("receipt_id", UUID(3, 3).toString())
                    .put("receipt_number", "RCP-SYNTHETIC").put("remaining_debt", "0.00")
            }
            val bytes = JSONObject().put("success", code == 200).put("data", data).toString().toByteArray()
            e.responseHeaders.add("Content-Type", "application/json"); e.sendResponseHeaders(code, bytes.size.toLong()); e.responseBody.use { it.write(bytes) }
        }; server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/"); ApiClientProvider.interceptor.setSession("synthetic-cash-ui-token")
    }
    @After fun stop() { server.stop(0); ApiClientProvider.createService(oldBase); ApiClientProvider.interceptor.setSession(oldToken); RuntimeEnvironment.setFontScale(oldFont) }
    @Test fun draftAndRetryRemainUntilAckAndCompletionIsExplicit() {
        var completed = 0
        compose.setContent { Okul360Theme { NativeCashCollectionDialog(debt, actor, {}, { completed++ }, canCollect = true) } }
        compose.waitUntil(10000) { compose.onNodeWithTag("cash_list").fetchSemanticsNode().config[SemanticsProperties.StateDescription] == "Hazır" }
        compose.onNodeWithTag("cash_list").performScrollToNode(hasTestTag("cash_save"))
        compose.onNodeWithTag("cash_save").performScrollTo().assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f)).performClick()
        compose.waitUntil(10000) { compose.onNodeWithTag("cash_list").fetchSemanticsNode().config[SemanticsProperties.StateDescription] == "Bekleyen işlem" }
        compose.onNodeWithTag("cash_list").performScrollToNode(hasTestTag("cash_amount"))
        compose.onNodeWithTag("cash_amount").assertIsNotEnabled(); assertEquals(0, completed)
        compose.onNodeWithTag("cash_list").performScrollToNode(hasTestTag("cash_save"))
        compose.onNodeWithTag("cash_save").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onNodeWithTag("cash_list").fetchSemanticsNode().config[SemanticsProperties.StateDescription] == "Kaydedildi" }
        assertEquals(posts[0], posts[1]); assertEquals(0, completed)
        compose.onNodeWithTag("cash_list").performScrollToNode(hasTestTag("cash_done"))
        compose.onNodeWithTag("cash_done").performScrollTo().assertIsDisplayed().performClick(); assertEquals(1, completed)
    }
    @Test @Config(qualifiers = "w780dp-h360dp-land-mdpi")
    fun landscapeLargeFontKeepsSaveInsideViewport() {
        compose.setContent { Okul360Theme { NativeCashCollectionDialog(debt, actor, {}, {}, canCollect = true) } }
        compose.waitUntil(10000) { compose.onNodeWithTag("cash_list").fetchSemanticsNode().config[SemanticsProperties.StateDescription] == "Hazır" }
        compose.onNodeWithTag("cash_list").performScrollToNode(hasTestTag("cash_save"))
        compose.onNodeWithTag("cash_save").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
        val bounds = compose.onNodeWithTag("cash_save").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.top >= 0 && bounds.bottom <= 360f && bounds.left >= 0 && bounds.right <= 780f)
    }
    @Test fun readonlyActorHasNoManualCollectionAction() {
        compose.setContent { Okul360Theme { NativeCashCollectionDialog(debt, actor, {}, {}, canCollect = false) } }
        compose.waitUntil(10000) { compose.onNodeWithTag("cash_list").fetchSemanticsNode().config[SemanticsProperties.StateDescription] == "Hazır" }
        compose.onNodeWithTag("cash_list").performScrollToNode(hasTestTag("cash_save"))
        compose.onNodeWithTag("cash_save").assertIsNotEnabled(); assertTrue(posts.isEmpty())
    }

}
