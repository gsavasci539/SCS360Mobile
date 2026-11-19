package com.example.ui.context

import androidx.compose.runtime.*
import com.example.data.model.UserRole
import com.example.ui.components.ChatbotMessage
import com.example.ui.components.Okul360ChatbotEngine

/**
 * ChatContext manages local conversation history, multi-turn memory,
 * and prompt context for the AI Chatbot Assistant.
 *
 * Allows the chatbot to reference previous user questions and AI responses
 * for coherent, contextual conversations across the app lifecycle.
 */
class ChatContextState(
    initialRole: UserRole = UserRole.PARENT,
    initialUserName: String = "Veli"
) {
    private val _messages = mutableStateListOf<ChatbotMessage>()
    val messages: List<ChatbotMessage> get() = _messages

    var activeRole by mutableStateOf(initialRole)
    var activeUserName by mutableStateOf(initialUserName)
    var selectedStudentId by mutableStateOf<String?>(null)
        private set

    init {
        if (_messages.isEmpty()) {
            _messages.add(Okul360ChatbotEngine.getInitialGreeting(initialRole, initialUserName))
        }
    }

    /**
     * Updates active role/user and refreshes greeting if history is empty.
     */
    fun updateRoleContext(role: UserRole, userName: String) {
        if (activeRole != role || activeUserName != userName) {
            activeRole = role
            activeUserName = userName
            selectedStudentId = null
            clearHistory()
        }
    }

    fun selectStudent(studentId: String?) {
        if (selectedStudentId != studentId) {
            selectedStudentId = studentId
            clearHistory()
        }
    }

    /**
     * Appends a new user or AI message to the conversation history.
     */
    fun addMessage(message: ChatbotMessage) {
        _messages.add(message)
    }

    /**
     * Clears history and re-adds initial greeting.
     */
    fun clearHistory() {
        _messages.clear()
        _messages.add(Okul360ChatbotEngine.getInitialGreeting(activeRole, activeUserName))
    }

}

/**
 * CompositionLocal key for ChatContextState.
 */
val LocalChatContext = staticCompositionLocalOf<ChatContextState> {
    error("LocalChatContext not provided! Ensure ChatProvider wraps your component tree.")
}

/**
 * ChatProvider Component that wraps the UI hierarchy and maintains local chat message history.
 */
@Composable
fun ChatProvider(
    role: UserRole = UserRole.PARENT,
    userName: String = "Veli",
    scope: Any? = null,
    content: @Composable () -> Unit
) {
    val chatState = remember(scope) { ChatContextState(role, userName) }

    LaunchedEffect(role, userName) {
        chatState.updateRoleContext(role, userName)
    }

    CompositionLocalProvider(LocalChatContext provides chatState) {
        content()
    }
}

/**
 * Convenient Composable helper to access ChatContextState inside any Composable.
 */
@Composable
fun rememberChatContext(): ChatContextState {
    return LocalChatContext.current
}
