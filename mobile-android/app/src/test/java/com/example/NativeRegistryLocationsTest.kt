package com.example

import com.example.data.model.Branch
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeRegistryClient
import com.example.data.remote.RegistryRecordChangedException
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeRegistryLocationsTest {
    @Test fun siteMutationsUseCanonicalFieldsAndReadBackTheScopedRecords() {
        val organizationId = UUID(98, 1).toString()
        val branchId = UUID(98, 2).toString()
        val org = JSONObject().put("id", organizationId).put("name", "Synthetic institution")
            .put("country", "Türkiye").put("email", "info@example.test").put("is_active", true)
        val orgRow = AtomicReference(JSONObject(org.toString()))
        val branchRow = AtomicReference(JSONObject())
        val stale = AtomicBoolean(false)
        val writes = CopyOnWriteArrayList<Pair<String, JSONObject>>()
        val api = ApiClientProvider.interceptor
        val previousUrl = ApiClientProvider.activeBaseUrl
        val previous = listOf(api.authToken, api.tenantId, api.branchId, api.workspaceOrganizationId)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/") { exchange ->
            val path = exchange.requestURI.path
            val method = exchange.requestMethod
            val branch = path.startsWith("/api/branches")
            val target = if (branch) branchRow else orgRow
            val body = if (method != "GET") JSONObject(exchange.requestBody.bufferedReader().use { it.readText() }) else null
            val data = when {
                body != null -> {
                    writes += "$method $path" to body
                    val next = JSONObject(target.get().toString())
                    for (key in body.keys()) next.put(key, body.get(key))
                    if (branch) next.put("id", branchId).put("student_count", 0).put("organization_id", organizationId)
                    if (!stale.get()) target.set(next)
                    next
                }
                path == "/api/organizations" -> JSONObject().put("total", 1).put("items", JSONArray().put(target.get()))
                else -> target.get()
            }
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            api.setSession("isolated-registry", organizationId)
            runBlocking {
                val client = NativeRegistryClient()
                val original = client.organizations().single()
                val desired = original.copy(email = "", tax_number = "Synthetic tax identity")
                stale.set(true)
                assertTrue(runCatching { client.saveOrganization(original, desired) }.isFailure)
                stale.set(false)
                assertEquals(desired, client.saveOrganization(original, desired))
                val changed = runCatching { client.saveOrganization(original, desired) }.exceptionOrNull()
                assertTrue(changed is RegistryRecordChangedException)
                val saved = client.saveBranch(null, Branch(organization_id = organizationId, name = "Synthetic branch", capacity = 48))
                assertEquals(branchId, saved.id)
                assertEquals(48, saved.capacity)
                val updated = client.saveBranch(saved, saved.copy(city = "Ankara", is_active = false))
                assertFalse(updated.is_active)
                assertEquals("Ankara", updated.city)
            }
            assertEquals(4, writes.size)
            assertEquals(setOf("email", "tax_number"), writes.first().second.keys().asSequence().toSet())
            assertTrue(writes.first().second.isNull("email"))
            val create = writes[2]
            assertEquals("POST /api/branches", create.first)
            assertEquals(organizationId, create.second.getString("organization_id"))
            assertFalse(create.second.has("id"))
            assertTrue(create.second.isNull("code"))
            assertEquals("PUT /api/branches/$branchId", writes[3].first)
            assertEquals(setOf("city", "is_active"), writes[3].second.keys().asSequence().toSet())
        } finally {
            ApiClientProvider.createService(previousUrl)
            api.setSession(previous[0], previous[1], previous[2], previous[3])
            server.stop(0)
        }
    }
}
