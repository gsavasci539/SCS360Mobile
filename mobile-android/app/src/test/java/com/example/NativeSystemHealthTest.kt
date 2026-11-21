package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeSystemHealth
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeSystemHealthTest {
    @Test fun livenessDoesNotImplyDatabaseReadiness() {
        val originalUrl = ApiClientProvider.activeBaseUrl
        val auth = ApiClientProvider.interceptor
        val original = listOf(auth.authToken, auth.tenantId, auth.branchId, auth.workspaceOrganizationId)
        val paths = CopyOnWriteArrayList<String>()
        val healthy = AtomicBoolean(true)
        val ready = AtomicBoolean(false)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api") { exchange ->
            val path = exchange.requestURI.path
            paths += path
            val response = when {
                path == "/api/health" && healthy.get() -> 200 to """{"status":"ok","success":true,"data":{"status":"healthy"}}"""
                path == "/api/health" -> 200 to """{"success":true,"data":{"status":"unknown"}}"""
                path == "/api/ready" && ready.get() -> 200 to """{"success":true,"data":{"status":"ready","database":"connected"}}"""
                else -> 503 to """{"success":false,"data":null}"""
            }
            val bytes = response.second.toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(response.first, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val api = ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            runBlocking {
                val client = NativeSystemHealth(api)
                val notReady = client.load()
                assertTrue(notReady.apiVerified)
                assertEquals(false, notReady.databaseReady)
                ready.set(true)
                assertEquals(true, client.load().databaseReady)
                healthy.set(false)
                val unverified = client.load()
                assertFalse(unverified.apiVerified)
                assertNull(unverified.latencyMs)
                assertNull(unverified.databaseReady)
            }
            assertEquals(listOf("/api/health", "/api/ready", "/api/health", "/api/ready", "/api/health"), paths.toList())
        } finally {
            ApiClientProvider.createService(originalUrl)
            auth.setSession(original[0], original[1], original[2], original[3])
            server.stop(0)
        }
    }
}
