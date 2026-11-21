package com.example

import com.example.data.model.OrganizationAccessStatus
import com.example.data.model.Student
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.remote.ApiClientProvider
import com.example.data.remote.ChatbotTurn
import com.example.data.remote.NativeChatbotClient
import com.example.ui.components.ChatbotMessage
import com.example.ui.context.ChatContextState
import com.example.ui.navigation.CHATBOT_MESSAGING
import com.example.ui.navigation.ChatbotDestination
import com.example.ui.navigation.SubScreen
import com.example.ui.navigation.chatbotDestination
import com.example.ui.screens.ParentNavTab
import com.example.ui.viewmodel.Okul360UiState
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeChatbotContractTest {
    @Test fun requestsUseWebEndpointsSelectedStudentAndBoundedHistory() = runBlocking {
        val studentId = UUID(941, 1).toString()
        val requests = CopyOnWriteArrayList<Pair<String, JSONObject>>()
        val auth = ApiClientProvider.interceptor
        val base = ApiClientProvider.activeBaseUrl
        val session = listOf(auth.authToken, auth.tenantId, auth.branchId, auth.workspaceOrganizationId)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/") { call ->
            try {
                requests += call.requestURI.path to JSONObject(call.requestBody.readBytes().toString(Charsets.UTF_8))
                val bytes = """{"success":true,"data":{"response_markdown":"Sunucudan kayıtlı yanıt","intent":"GENERAL","suggested_actions":[{"action_tag":"NAV_MESSAGING","label":"Sohbetleri aç"}]}}""".toByteArray()
                call.responseHeaders.set("Content-Type", "application/json")
                call.sendResponseHeaders(200, bytes.size.toLong())
                call.responseBody.use { it.write(bytes) }
            } finally {
                call.close()
            }
        }
        server.start()
        ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
        auth.setSession("isolated-chatbot-test")
        try {
            val client = NativeChatbotClient()
            val turns = (1..8).map { ChatbotTurn("x".repeat(2100), it % 2 == 0, "GENERAL") }
            val answer = client.ask("Kayıtlı sohbeti aç", UserRole.PARENT, studentId, turns)
            assertEquals("Sunucudan kayıtlı yanıt", answer.text)
            assertEquals(listOf(CHATBOT_MESSAGING to "Sohbetleri aç"), answer.actions)
            client.ask("Sohbetler", UserRole.TEACHER, null, emptyList())
            assertEquals(listOf("/api/parent/chatbot", "/api/v1/chatbot/process-query"), requests.map { it.first })
            val parent = requests.first().second
            assertEquals(studentId, parent.getString("student_id"))
            assertEquals("PARENT", parent.getString("role"))
            val history = parent.getJSONArray("conversation_history")
            assertEquals(6, history.length())
            assertEquals("assistant", history.getJSONObject(0).getString("role"))
            assertEquals(2000, history.getJSONObject(0).getString("content").length)
            assertTrue(requests.last().second.isNull("student_id"))
        } finally {
            server.stop(0)
            ApiClientProvider.createService(base)
            auth.setSession(session[0], session[1], session[2], session[3])
        }
    }

    @Test fun messagingNavigationUsesRealParentChildAndNeverDispatchesSideEffects() {
        val parent = Okul360UiState(isLoggedIn = true, selectedRole = UserRole.PARENT,
            currentUser = User(permissions = listOf("parent.send_message")),
            parentChildren = listOf(Student(id = "owned")))
        assertEquals(ChatbotDestination.Parent(ParentNavTab.MESSAGING, "owned"),
            chatbotDestination(parent, CHATBOT_MESSAGING, "owned"))
        assertNull(chatbotDestination(parent, CHATBOT_MESSAGING, "foreign"))
        assertNull(chatbotDestination(parent.copy(currentUser = User()), CHATBOT_MESSAGING, "owned"))
        assertNull(chatbotDestination(parent, "ACTION_SEND_REMINDER", "owned"))
        assertNull(chatbotDestination(parent.copy(organizationAccess = OrganizationAccessStatus(status = "RESTRICTED")), CHATBOT_MESSAGING))
        val teacher = parent.copy(selectedRole = UserRole.TEACHER, currentUser = User(permissions = listOf("messaging.read")))
        assertEquals(ChatbotDestination.Screen(SubScreen.DIRECT_MESSAGING), chatbotDestination(teacher, CHATBOT_MESSAGING))
        assertNull(chatbotDestination(teacher, "CONNECT_TEACHER_Example"))
    }

    @Test fun changingSelectedStudentClearsChatTranscript() {
        val history = ChatContextState(UserRole.PARENT, "Test veli")
        history.selectStudent("student-one")
        history.addMessage(ChatbotMessage(text = "Önceki öğrencinin yanıtı", isUser = false))
        history.selectStudent("student-two")
        assertEquals("student-two", history.selectedStudentId)
        assertEquals(1, history.messages.size)
        assertFalse(history.messages.single().includeInHistory)
    }
}
