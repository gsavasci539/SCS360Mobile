package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.InstitutionIntegration
import com.example.data.remote.NativeIntegrationSettings
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
class NativeIntegrationSettingsTest {
    @Test fun verifiesExactProviderWithoutEchoingMaskedOrUntouchedFields() {
        val organization = UUID(85, 1).toString()
        val auth = ApiClientProvider.interceptor
        val originalUrl = ApiClientProvider.activeBaseUrl
        val originalToken = auth.authToken
        val originalTenant = auth.tenantId
        val originalBranch = auth.branchId
        val originalWorkspace = auth.workspaceOrganizationId
        val queries = CopyOnWriteArrayList<String>()
        val workspaceHeaders = CopyOnWriteArrayList<String>()
        val writes = CopyOnWriteArrayList<JSONObject>()
        val matching = AtomicBoolean(false)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/settings/integrations") { request ->
            workspaceHeaders += request.requestHeaders.getFirst("X-SCS360-Organization-Id").orEmpty()
            val data = JSONObject().put("id", "smtp-one").put("provider", "SMTP").put("organization_id", organization)
            val payload = if (request.requestMethod == "PUT") {
                writes += JSONObject(request.requestBody.bufferedReader().use { it.readText() })
                data
            } else {
                queries += request.requestURI.rawQuery.orEmpty()
                val config = JSONObject().put("host", if (matching.get()) "next.example.invalid" else "old.example.invalid")
                    .put("password", "***").put("port", 465).put("username", JSONObject().put("legacy", true))
                JSONArray().put(data.put("is_active", false).put("config", config))
            }
            val bytes = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            request.responseHeaders.set("Content-Type", "application/json")
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-integration", null, null, organization)
            runBlocking {
                val client = NativeIntegrationSettings(organization)
                val provider = InstitutionIntegration.SMTP
                val old = client.load().getValue(provider)
                assertFalse(old.publicFields.containsKey("password"))
                assertTrue("username" in old.blockedFields)
                assertTrue(runCatching { client.save(provider, old, mapOf("username" to "")) }.isFailure)
                val patch = mapOf("host" to "next.example.invalid")
                assertTrue(runCatching { client.save(provider, old, patch) }.isFailure)
                matching.set(true)
                val saved = client.save(provider, old, patch).getValue(provider)
                assertEquals("next.example.invalid", saved.publicFields["host"])
                assertFalse(saved.active)
            }
            assertEquals(2, writes.size)
            assertTrue(writes.all { it.getJSONObject("config").length() == 1 && !it.has("is_active") })
            assertEquals("old.example.invalid", writes.first().getJSONObject("expected_config").getString("host"))
            assertTrue(queries.all { it == "organization_id=$organization" })
            assertTrue(workspaceHeaders.all { it == organization })
        } finally {
            ApiClientProvider.createService(originalUrl)
            auth.setSession(originalToken, originalTenant, originalBranch, originalWorkspace)
            server.stop(0)
        }
    }
}
