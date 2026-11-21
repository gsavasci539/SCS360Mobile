package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeSurveyClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
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
class NativeSurveyClientTest {
    @Test fun anonymousParentSurveyRequiresDurableParticipationInTheChildScope() {
        val organization = UUID(27, 1).toString()
        val child = UUID(27, 2).toString()
        val survey = UUID(27, 3).toString()
        val answer = UUID(27, 4).toString()
        val question = UUID(27, 5).toString()
        val path = "/api/parent/child/$child/surveys/$survey"
        val confirmed = AtomicBoolean(false)
        val requests = CopyOnWriteArrayList<String>()
        val written = AtomicReference<JSONObject>()
        val base = ApiClientProvider.activeBaseUrl
        val auth = ApiClientProvider.interceptor
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext(path) { exchange ->
            requests += "${exchange.requestMethod} ${exchange.requestURI.path}"
            val data = if (exchange.requestMethod == "POST") {
                written.set(JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() }))
                JSONObject().put("id", answer)
            } else {
                JSONObject().put("survey_id", survey).put("submitted", confirmed.get())
            }
            val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-parent-survey", organization, null)
            runBlocking {
                val client = NativeSurveyClient(organization, child)
                val values = JSONObject().put(question, 4)
                assertTrue(runCatching { client.submit(survey, values) }.isFailure)
                confirmed.set(true)
                client.submit(survey, values)
            }
            assertEquals(4, written.get().getJSONObject("response_data").getInt(question))
            assertEquals(setOf("response_data"), written.get().keys().asSequence().toSet())
            assertEquals(listOf("POST $path/respond", "GET $path/participation", "POST $path/respond", "GET $path/participation"), requests.toList())
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
