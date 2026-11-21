package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeSchoolMerchantClient
import com.example.data.remote.newRequest
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeSchoolMerchantClientTest {
    @Test fun replaysTheSameConfigurationRequestAndRequiresItsPersistedRevision() {
        val organization = UUID(92, 1).toString()
        val account = UUID(92, 2).toString()
        val oldRevision = "a".repeat(64)
        val newRevision = "b".repeat(64)
        val auth = ApiClientProvider.interceptor
        val originalUrl = ApiClientProvider.activeBaseUrl
        val originalToken = auth.authToken
        val originalTenant = auth.tenantId
        val originalBranch = auth.branchId
        val originalWorkspace = auth.workspaceOrganizationId
        val recorded = CopyOnWriteArrayList<JSONObject>()
        val scopes = CopyOnWriteArrayList<String>()
        val matching = AtomicBoolean(false)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/payments/merchant") { request ->
            val changed = request.requestMethod == "PUT"
            if (request.requestURI.rawQuery != "organization_id=$organization") {
                request.sendResponseHeaders(403, -1)
                request.close()
                return@createContext
            }
            if (changed) recorded += JSONObject(request.requestBody.bufferedReader().use { it.readText() })
            scopes += request.requestHeaders.getFirst("X-SCS360-Organization-Id").orEmpty()
            val provider = JSONObject().put("id", "paytr").put("name", "PayTR")
                .put("credential_fields", JSONArray(listOf("merchant_id", "merchant_key", "merchant_salt")))
            val data = JSONObject().put("can_manage", true).put("available", false).put("account_id", account)
                .put("revision", if (changed || matching.get()) newRevision else oldRevision)
                .put("environment", "SANDBOX").put("provider", "paytr").put("providers", JSONArray().put(provider))
                .put("enabled", false).put("configured", true).put("callback_configured", false)
                .put("portal_configured", false).put("encryption_configured", true).put("production_compatible", true)
                .put("bank_pos_note", "").put("credential_status", JSONObject().put("merchant_id", true))
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            request.responseHeaders.set("Content-Type", "application/json")
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-merchant", null, null, organization)
            runBlocking {
                val client = NativeSchoolMerchantClient(organization)
                val current = client.status()
                val provider = current.providers.single()
                assertTrue(runCatching { current.newRequest(provider, "LIVE", false, false, emptyMap()) }.isFailure)
                val credentials = mapOf("merchant_key" to "temporary-input")
                val update = current.newRequest(provider, "SANDBOX", false, false, credentials)
                assertFalse(update.toString().contains("temporary-input"))
                assertTrue(runCatching { client.save(update) }.isFailure)
                matching.set(true)
                assertEquals(newRevision, client.save(update).revision)
            }
            assertEquals(2, recorded.size)
            assertEquals(recorded.first().toString(), recorded.last().toString())
            assertEquals(oldRevision, recorded.first().getString("expected_revision"))
            assertEquals(1, recorded.first().getJSONObject("credentials").length())
            assertTrue(scopes.all { it == organization })
        } finally {
            ApiClientProvider.createService(originalUrl)
            auth.setSession(originalToken, originalTenant, originalBranch, originalWorkspace)
            server.stop(0)
        }
    }
}
