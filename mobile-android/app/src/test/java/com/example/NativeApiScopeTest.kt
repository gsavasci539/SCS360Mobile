package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.data.remote.*
import com.example.data.repository.Okul360Repository
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeApiScopeTest {
    private suspend fun withServer(handler: (com.sun.net.httpserver.HttpExchange) -> Unit, test: suspend (String) -> Unit) {
        val base = ApiClientProvider.activeBaseUrl
        val token = ApiClientProvider.interceptor.authToken
        val workspace = ApiClientProvider.interceptor.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/", handler)
        server.start()
        try { test("http://127.0.0.1:${server.address.port}/api/") }
        finally { server.stop(0); ApiClientProvider.createService(base); ApiClientProvider.interceptor.setSession(token, workspace = workspace) }
    }
    private fun reply(exchange: com.sun.net.httpserver.HttpExchange, status: Int, text: String) {
        val bytes = text.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    @Test fun canonicalLoginPreservesPasswordAndFetchesTheRealRoleProfile() = runBlocking {
        val calls = mutableListOf<String>()
        withServer({ e ->
            calls.add(e.requestURI.path)
            if (e.requestURI.path == "/api/auth/login") {
                val body = JSONObject(e.requestBody.bufferedReader().use { it.readText() })
                assertEquals("user@example.test", body.getString("email"))
                assertEquals(" synthetic password ", body.getString("password"))
                reply(e, 200, """{"success":true,"data":{"access_token":"synthetic-token"}}""")
            } else {
                assertEquals("Bearer synthetic-token", e.requestHeaders.getFirst("Authorization"))
                assertNull(e.requestHeaders.getFirst("X-SCS360-Organization-Id"))
                reply(e, 200, """{"success":true,"data":{"id":"00000000-0000-4000-8000-000000000001","email":"user@example.test","roles":[{"slug":"teacher","is_active":true}],"permissions":["attendance.read"]}}""")
            }
        }) { url ->
            val user = NativeAuthentication(ApiClientProvider.createService(url)).login(" user@example.test ", " synthetic password ")
            assertEquals(UserRole.TEACHER, SessionRolePolicy.primary(user))
            assertNull(user.phone)
            assertEquals("", user.branch_id)
            assertEquals(listOf("/api/auth/login", "/api/auth/me"), calls)
        }
    }

    @Test fun rejectedLoginDoesNotTryLegacyEndpointsOrInventAnIdentity() = runBlocking {
        val calls = AtomicInteger()
        withServer({ e -> calls.incrementAndGet(); reply(e, 401, """{"detail":"synthetic rejection"}""") }) { url ->
            val auth = NativeAuthentication(ApiClientProvider.createService(url))
            try { auth.login("user@example.test", "synthetic-password"); fail("Rejected login must fail") } catch (_: retrofit2.HttpException) { }
            assertEquals(1, calls.get())
            assertNull(ApiClientProvider.interceptor.authToken)
        }
    }

    @Test fun changedWorkspaceAndLogoutDiscardDelayedRealClientResponses() = runBlocking {
        for (change in listOf("workspace", "logout", "branch", "permissions")) {
            val received = CountDownLatch(1); val release = CountDownLatch(1)
            withServer({ e -> received.countDown(); release.await(5, TimeUnit.SECONDS); reply(e, 200, "[]") }) { url ->
                val api = ApiClientProvider.createService(url)
                ApiClientProvider.interceptor.setSession("synthetic-token", workspace = "workspace-A")
                val result = async(Dispatchers.IO) { runCatching { api.getBranches() } }
                assertTrue(received.await(5, TimeUnit.SECONDS))
                when (change) {
                    "logout" -> ApiClientProvider.interceptor.setSession(null)
                    "branch" -> ApiClientProvider.interceptor.branchId = "branch-B"
                    "permissions" -> ApiClientProvider.interceptor.invalidateContext()
                    else -> ApiClientProvider.interceptor.workspaceOrganizationId = "workspace-B"
                }
                release.countDown()
                assertTrue(result.await().isFailure)
            }
        }
    }

    @Test fun oldApiObjectCannotSendNewServerCredentialsToThePreviousServer() = runBlocking {
        val calls = AtomicInteger()
        withServer({ e -> calls.incrementAndGet(); reply(e, 200, "[]") }) { url ->
            val old = ApiClientProvider.createService(url)
            ApiClientProvider.interceptor.setSession("old-synthetic-token")
            ApiClientProvider.createService(url.replace("127.0.0.1", "localhost"))
            assertNull(ApiClientProvider.interceptor.authToken)
            ApiClientProvider.interceptor.setSession("new-synthetic-token")
            try { old.getBranches(); fail("Old origin must never receive the new credentials") } catch (_: java.io.IOException) { }
            assertEquals(0, calls.get())
        }
    }

    @Test fun failedWritesAndReadsNeverReturnLocalSuccessOrSampleConsents() = runBlocking {
        withServer({ e -> reply(e, 503, """{"detail":"synthetic unavailable"}""") }) { url ->
            ApiClientProvider.createService(url)
            ApiClientProvider.interceptor.setSession("synthetic-token")
            val context = ApplicationProvider.getApplicationContext<Context>()
            val repo = Okul360Repository(context)
            var failures = 0
            val operations: List<suspend () -> Any?> = listOf(
                { repo.getConsents("synthetic-student") },
                { repo.createGuidanceRecord(GuidanceRecord()) },
                { repo.approveConsent("synthetic-consent", "Synthetic parent") },
                { repo.createInfirmaryVisit(InfirmaryVisit()) },
                { repo.participateSurvey("synthetic-survey", emptyMap()) },
                { repo.forgotPassword("user@example.test") }
            )
            for (operation in operations) {
                try { operation(); fail("Unavailable API must not produce a local success") } catch (_: retrofit2.HttpException) { failures++ }
            }
            assertEquals(operations.size, failures)
        }
    }
}
