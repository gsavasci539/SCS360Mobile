package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.ClubRecords
import com.example.data.remote.NativeClubsClient
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
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeClubsClientTest {
    @Test fun enrollmentRequiresAnExactActiveRecordForTheSelectedChild() {
        val branch = UUID(25, 1).toString()
        val club = UUID(25, 2).toString()
        val student = UUID(25, 3).toString()
        val membership = UUID(25, 4).toString()
        val wrongChild = UUID(25, 5).toString()
        val valid = AtomicBoolean(false)
        val written = AtomicReference<JSONObject>()
        val reads = CopyOnWriteArrayList<String>()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/clubs/memberships") { exchange ->
            val data = if (exchange.requestMethod == "POST") {
                written.set(JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() }))
                JSONObject().put("id", membership).put("status", "ACTIVE")
            } else {
                reads += exchange.requestURI.toString()
                JSONArray().put(JSONObject().put("id", membership).put("status", "ACTIVE").put("club_id", club)
                    .put("student_id", if (valid.get()) student else wrongChild))
            }
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-club", UUID(25, 6).toString(), branch)
            runBlocking {
                val client = NativeClubsClient()
                val body = JSONObject().put("student_id", student).put("club_id", club)
                val request = UUID(25, 7)
                assertTrue(runCatching { client.save(ClubRecords.MEMBERS, branch, body, request) }.isFailure)
                valid.set(true)
                client.save(ClubRecords.MEMBERS, branch, body, request)
            }
            assertEquals(student, written.get().getString("student_id"))
            assertTrue(reads.size == 2 && reads.all { "branch_id=$branch" in it && "record_id=$membership" in it })
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
