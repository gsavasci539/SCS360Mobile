package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativeMaterialsClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeMaterialsClientTest {
    @Test fun parentListsAndDownloadsOnlyThroughTheSelectedChild() {
        val student = UUID(24, 1).toString()
        val material = UUID(24, 2).toString()
        val branch = UUID(24, 3).toString()
        val path = "/api/parent/child/$student/materials"
        val attachment = "%PDF-1.4\nisolated material".toByteArray()
        val requests = CopyOnWriteArrayList<String>()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val oldToken = auth.authToken
        val oldTenant = auth.tenantId
        val oldBranch = auth.branchId
        val oldWorkspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext(path) { exchange ->
            requests += exchange.requestURI.toString()
            val bytes = if (exchange.requestURI.path.endsWith("/download")) attachment else
                JSONObject().put("success", true).put("data", JSONArray().put(JSONObject()
                    .put("id", material).put("title", "İzole materyal").put("type", "PDF")
                    .put("file_url", "$path/$material/download").put("file_name", "izole.pdf"))).toString().toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            auth.setSession("isolated-materials-parent", UUID(24, 4).toString(), branch)
            runBlocking {
                val client = NativeMaterialsClient(branch, student)
                val row = client.list(0, UUID(24, 5).toString(), false).single()
                assertEquals("izole.pdf", row.fileName)
                assertArrayEquals(attachment, client.download(row))
            }
            assertEquals(listOf(path, "$path/$material/download"), requests.toList())
        } finally {
            ApiClientProvider.createService(base)
            auth.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
            server.stop(0)
        }
    }
}
