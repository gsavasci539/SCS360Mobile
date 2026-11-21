package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.InventoryRecords
import com.example.data.remote.NativeInventoryClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
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
class NativeInventoryClientTest {
    @Test fun recreatingEditorReusesCreationIdentityAfterAnUnverifiedReply() {
        val branch = UUID(72, 10).toString()
        val organization = UUID(72, 11).toString()
        val actor = UUID(72, 12).toString()
        val creationIds = CopyOnWriteArrayList<String>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/inventory/locations") { request ->
            val id = if (request.requestMethod == "POST") {
                val form = JSONObject(request.requestBody.bufferedReader().use { it.readText() })
                form.getString("request_id").also(creationIds::add)
            } else creationIds.last()
            val record = JSONObject().put("id", id).put("branch_id", branch)
                .put("location_name", if (creationIds.size == 1) "Different room" else "Verified room")
            val payload = if (request.requestMethod == "POST") record else JSONArray().put(record)
            val bytes = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-inventory", organization, branch)
            runBlocking {
                val form = JSONObject().put("branch_id", branch).put("location_name", "Verified room")
                val first = NativeInventoryClient(branch, context, actor, organization, null)
                assertTrue(runCatching { first.save(InventoryRecords.LOCATIONS, branch, form) }.isFailure)
                val reloaded = NativeInventoryClient(branch, context, actor, organization, null)
                reloaded.save(InventoryRecords.LOCATIONS, branch, form)
            }
            assertEquals(2, creationIds.size)
            assertEquals(1, creationIds.toSet().size)
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }

    @Test fun returnConfirmsExactAssetAndSelectedBranchBeforeCompleting() {
        val branch = UUID(72, 1).toString()
        val assignment = UUID(72, 2).toString()
        val asset = UUID(72, 3).toString()
        val confirmed = AtomicBoolean(false)
        val requests = CopyOnWriteArrayList<String>()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/inventory/assignments") { request ->
            requests += "${request.requestMethod} ${request.requestURI}"
            val status = JSONObject().put("id", assignment).put("status", "RETURNED").put("return_date", "2026-11-21")
            val data = if (request.requestMethod == "POST") status else JSONArray().put(
                status.put("branch_id", branch).put("item_id", if (confirmed.get()) asset else UUID(72, 4).toString())
            )
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            val organization = UUID(72, 5).toString()
            auth.setSession("isolated-inventory", organization, branch)
            runBlocking {
                val client = NativeInventoryClient(branch, ApplicationProvider.getApplicationContext<Context>(),
                    UUID(72, 6).toString(), organization, null)
                val row = JSONObject().put("id", assignment).put("branch_id", branch).put("item_id", asset)
                assertTrue(runCatching { client.finish(InventoryRecords.ASSIGNMENTS, row) }.isFailure)
                confirmed.set(true)
                client.finish(InventoryRecords.ASSIGNMENTS, row)
            }
            assertEquals(4, requests.size)
            assertTrue(requests.filter { it.startsWith("POST") }.all { it.endsWith("$assignment/return") })
            assertTrue(requests.filter { it.startsWith("GET") }.all { "record_id=$assignment" in it && "branch_id=$branch" in it })
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
