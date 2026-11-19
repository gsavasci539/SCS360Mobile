package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.ChatFile
import com.example.data.remote.DirectMessageDraft
import com.example.data.remote.NativeMessagingClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeMessageAttachmentTest {
    @Test fun attachmentUsesMultipartAndVerifiedReceiptAndRejectsCorruptDownload() = runBlocking {
        val actor = UUID(942, 1).toString()
        val recipient = UUID(942, 2).toString()
        val branch = UUID(942, 3).toString()
        val id = UUID(942, 4)
        val file = ChatFile("Rapor.pdf", "%PDF-1.4\nSynthetic file".toByteArray())
        val draft = DirectMessageDraft(recipient, "", id, file)
        val altered = AtomicBoolean(false)
        val paths = CopyOnWriteArrayList<String>()
        val forms = CopyOnWriteArrayList<Pair<String, String>>()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val session = listOf(auth.authToken, auth.tenantId, auth.branchId, auth.workspaceOrganizationId)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/messages/") { call ->
            try {
                paths += "${call.requestMethod} ${call.requestURI}"
                val bytes = when (call.requestURI.path) {
                    "/api/messages/send-file" -> {
                        forms += call.requestHeaders.getFirst("Content-Type") to call.requestBody.readBytes().toString(Charsets.UTF_8)
                        return@createContext
                    }
                    "/api/messages/sent/$id" -> {
                        val row = JSONObject().put("id", id.toString()).put("sender_id", actor).put("recipient_id", recipient)
                            .put("content", "").put("sent_at", "2026-11-19T09:00:00Z").put("is_read", false)
                            .put("attachment_url", "/api/messages/$id/attachment").put("attachment_name", file.name)
                            .put("attachment_sha256", if (altered.get()) "0".repeat(64) else file.digest)
                        JSONObject().put("success", true).put("data", row).toString().toByteArray()
                    }
                    "/api/messages/$id/attachment" -> if (altered.get()) "Changed bytes".toByteArray() else file.bytes
                    else -> error("Unexpected test route")
                }
                call.sendResponseHeaders(200, bytes.size.toLong())
                call.responseBody.use { it.write(bytes) }
            } finally {
                call.close()
            }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        auth.setSession("isolated-attachment-test")
        try {
            val client = NativeMessagingClient(actor, branch)
            try {
                client.send(draft)
                fail("Expected a lost write response")
            } catch (_: IOException) { }
            val confirmed = client.receipt(draft, "Synthetic recipient")
            assertEquals(file.name, confirmed.attachment_name)
            assertEquals(file.digest, confirmed.attachment_sha256)
            assertArrayEquals(file.bytes, client.download(confirmed).bytes)
            altered.set(true)
            assertTrue(runCatching { client.receipt(draft, "Synthetic recipient") }.isFailure)
            assertTrue(runCatching { client.download(confirmed) }.isFailure)
            val (type, body) = forms.single()
            assertTrue(type.startsWith("multipart/form-data; boundary="))
            assertTrue(body.contains("name=\"request_id\"") && body.contains(id.toString()))
            assertTrue(body.contains("name=\"recipient_id\"") && body.contains(recipient))
            assertTrue(body.contains("name=\"branch_id\"") && body.contains(branch))
            assertTrue(body.contains("name=\"file\"; filename=\"Rapor.pdf\""))
            assertTrue(body.contains("%PDF-1.4"))
            assertTrue(paths.all { it.endsWith("?branch_id=$branch") })
            assertEquals(1, paths.count { it.startsWith("POST") })
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            auth.setSession(session[0], session[1], session[2], session[3])
        }
    }
}
