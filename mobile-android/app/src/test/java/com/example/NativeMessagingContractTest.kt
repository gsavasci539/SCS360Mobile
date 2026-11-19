package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.DirectMessageDraft
import com.example.data.remote.NativeMessagingClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeMessagingContractTest {
    @Test fun selectedBranchAndLostResponseRecoverOnePersistedMessage() = runBlocking {
        val actor = UUID(940, 1).toString()
        val partner = UUID(940, 2).toString()
        val branch = UUID(940, 3).toString()
        val storedId = UUID(940, 4)
        val oldId = UUID(940, 5)
        val draft = DirectMessageDraft(partner, "Geçici mesaj", storedId)
        val old = JSONObject().put("id", oldId.toString()).put("sender_id", partner).put("recipient_id", actor)
            .put("sender_role", "TEACHER").put("content", "Kayıtlı geçmiş").put("sent_at", "2026-11-19T08:00:00")
            .put("is_read", true)
        val stored = JSONObject().put("id", storedId.toString()).put("sender_id", actor).put("recipient_id", partner)
            .put("sender_role", "STAFF").put("content", draft.content).put("sent_at", "2026-11-19T08:02:00Z")
            .put("is_read", false)
        val paths = CopyOnWriteArrayList<String>()
        val writes = CopyOnWriteArrayList<JSONObject>()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val token = auth.authToken
        val tenant = auth.tenantId
        val previousBranch = auth.branchId
        val workspace = auth.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/messages/") { call ->
            try {
                val url = call.requestURI
                paths += "${call.requestMethod} $url"
                val data = when (url.path.removePrefix("/api/messages/")) {
                    "conversations" -> JSONArray().put(JSONObject().put("user_id", partner).put("name", "Kayıtlı kişi")
                        .put("last_message", old).put("unread_count", 1))
                    "recipients" -> JSONObject().put("page", 1).put("total_pages", 1)
                        .put("items", JSONArray().put(JSONObject().put("id", partner).put("name", "Kayıtlı kişi")
                            .put("roles", JSONArray().put("Öğretmen"))))
                    "conversation/$partner" -> JSONArray().put(old)
                    "send" -> {
                        writes += JSONObject(call.requestBody.readBytes().toString(Charsets.UTF_8))
                        return@createContext
                    }
                    "sent/$storedId" -> stored
                    else -> error("Unexpected test route")
                }
                val bytes = JSONObject().put("success", true).put("data", data).toString().toByteArray()
                call.sendResponseHeaders(200, bytes.size.toLong())
                call.responseBody.use { it.write(bytes) }
            } finally {
                call.close()
            }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        auth.setSession("isolated-messaging-test")
        try {
            val api = NativeMessagingClient(actor, branch)
            val conversation = api.conversations().single()
            assertEquals(partner, conversation.id)
            assertEquals("19.11 11:00", conversation.last_message_time)
            assertEquals(listOf("Öğretmen"), api.recipients().single().roles)
            val previous = api.history(conversation, oldId.toString()).single()
            assertEquals("Kayıtlı geçmiş", previous.text)
            assertFalse(previous.is_me)
            try {
                api.send(draft)
                fail("Expected missing response")
            } catch (_: IOException) { }
            val confirmed = api.receipt(draft, conversation.participant_name)
            assertEquals(draft.content, confirmed.text)
            assertTrue(confirmed.is_me)
            assertFalse(confirmed.is_read)
            val write = writes.single()
            assertEquals(branch, write.getString("branch_id"))
            assertEquals(partner, write.getString("recipient_id"))
            assertEquals(storedId.toString(), write.getString("request_id"))
            assertFalse(write.has("attachment_url"))
            assertTrue(paths.all { it.contains("branch_id=$branch") })
            assertTrue(paths.any { it.contains("before_id=$oldId") })
            assertTrue(paths.last().startsWith("GET /api/messages/sent/$storedId"))
            auth.setSession("different-isolated-session")
            assertFalse(api.current())
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            auth.setSession(token, tenant, previousBranch, workspace)
        }
    }
}
