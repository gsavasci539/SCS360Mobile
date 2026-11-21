package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativePayrollClient
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
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativePayrollClientTest {
    @Test fun approvalRequiresTheExactRecordStaffAndNetAmountFromTheServer() {
        val branch = UUID(71, 1).toString()
        val staff = UUID(71, 2).toString()
        val recordId = UUID(71, 3).toString()
        val amount = AtomicReference("10000.11")
        val savedStaff = AtomicReference(staff)
        val requests = CopyOnWriteArrayList<String>()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/payroll/records") { request ->
            requests += "${request.requestMethod} ${request.requestURI}"
            val approval = JSONObject().put("id", recordId).put("status", "APPROVED")
                .put("approved_by", UUID(71, 5).toString()).put("approved_at", "2026-11-21T09:00:00Z")
            val payload = if (request.requestMethod == "POST") approval else JSONArray().put(
                approval.put("staff_id", savedStaff.get()).put("net_salary", amount.get())
            )
            val bytes = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            request.sendResponseHeaders(200, bytes.size.toLong())
            request.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-payroll", UUID(71, 4).toString(), branch)
            runBlocking {
                val client = NativePayrollClient(branch)
                val row = JSONObject().put("id", recordId).put("staff_id", staff).put("net_salary", "10000.12")
                assertTrue(runCatching { client.approve(row) }.isFailure)
                amount.set("10000.120")
                savedStaff.set(UUID(71, 6).toString())
                assertTrue(runCatching { client.approve(row) }.isFailure)
                savedStaff.set(staff)
                client.approve(row)
            }
            assertEquals(6, requests.size)
            assertTrue(requests.all { "branch_id=$branch" in it })
            assertTrue(requests.filter { it.startsWith("GET") }.all { "record_id=$recordId" in it })
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
