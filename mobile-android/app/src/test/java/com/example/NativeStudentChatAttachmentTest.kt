package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.ChatFile
import com.example.data.remote.NativeParentPortalClient
import com.example.data.remote.StudentChatDraft
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CancellationException
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
class NativeStudentChatAttachmentTest {
    @Test fun scopedMultipartReceiptAndAuthenticatedDownload() = runBlocking {
        val child = UUID(943, 1).toString()
        val guardian = UUID(943, 2).toString()
        val teacher = UUID(943, 3).toString()
        val id = UUID(943, 4)
        val file = ChatFile("Rapor.pdf", "%PDF-1.4\nSynthetic report".toByteArray())
        val draft = StudentChatDraft("Belge", file, id)
        val path = "/api/parent/child/$child/chat/$teacher"
        val altered = AtomicBoolean(false)
        val forms = CopyOnWriteArrayList<Pair<String, String>>()
        val calls = CopyOnWriteArrayList<Triple<String, String, String>>()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val session = listOf(auth.authToken, auth.tenantId, auth.branchId, auth.workspaceOrganizationId)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext(path) { request ->
            try {
                calls += Triple(request.requestMethod, request.requestURI.path, request.requestHeaders.getFirst("Authorization").orEmpty())
                val response = when (request.requestURI.path) {
                    "$path/send-file" -> {
                        forms += request.requestHeaders.getFirst("Content-Type") to request.requestBody.readBytes().toString(Charsets.UTF_8)
                        return@createContext
                    }
                    "$path/sent/$id" -> {
                        val row = JSONObject().put("id", id.toString()).put("sender_id", guardian).put("receiver_id", teacher)
                            .put("is_current_user", true).put("content", draft.content).put("attachment_name", file.name)
                            .put("attachment_sha256", if (altered.get()) "0".repeat(64) else file.digest)
                        JSONObject().put("success", true).put("data", row).toString().toByteArray()
                    }
                    "$path/attachments/$id" -> if (altered.get()) "Changed bytes".toByteArray() else file.bytes
                    else -> error("Unexpected test endpoint")
                }
                request.sendResponseHeaders(200, response.size.toLong())
                request.responseBody.use { it.write(response) }
            } finally {
                request.close()
            }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        auth.setSession("isolated-student-chat")
        try {
            val client = NativeParentPortalClient()
            try {
                client.sendStudentChat(child, teacher, guardian, draft)
                fail("Expected a lost send response")
            } catch (_: IOException) { }
            val stored = client.studentChatReceipt(child, teacher, guardian, draft)
            assertEquals(id.toString(), stored.getString("id"))
            assertArrayEquals(file.bytes, client.downloadStudentChat(child, teacher, stored).bytes)
            altered.set(true)
            assertTrue(runCatching { client.studentChatReceipt(child, teacher, guardian, draft) }.isFailure)
            assertTrue(runCatching { client.downloadStudentChat(child, teacher, stored) }.isFailure)
            val (type, form) = forms.single()
            assertTrue(type.startsWith("multipart/form-data; boundary="))
            assertTrue(form.contains("name=\"request_id\"") && form.contains(id.toString()))
            assertTrue(form.contains("name=\"message\"") && form.contains(draft.content))
            assertTrue(form.contains("name=\"file\"; filename=\"Rapor.pdf\""))
            assertTrue(form.contains("%PDF-1.4"))
            assertEquals(1, calls.count { it.first == "POST" })
            assertTrue(calls.all { it.second.startsWith(path) && it.third == "Bearer isolated-student-chat" })

            val before = calls.size
            auth.invalidateContext()
            try {
                client.downloadStudentChat(child, teacher, stored)
                fail("Stale recipient context")
            } catch (_: CancellationException) { }
            assertEquals(before, calls.size)
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            auth.setSession(session[0], session[1], session[2], session[3])
        }
    }
}
