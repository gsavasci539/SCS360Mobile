package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.example.data.model.UserRole
import com.example.data.remote.ChatbotTurn
import com.example.data.remote.NativeChatbotClient
import com.example.ui.components.ChatBubbleItem
import com.example.ui.components.ChatbotAction
import com.example.ui.components.ChatbotMessage
import com.example.ui.components.Okul360ChatbotEngine
import com.example.ui.components.ThinkingIndicator
import com.example.ui.context.rememberChatContext
import com.example.ui.navigation.CHATBOT_MESSAGING
import com.example.ui.navigation.chatbotDestination
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * SCS360 Birinci Sınıf Tam Ekran AI Asistan ve Küresel DB Sorgulama Ekranı.
 * MainTab.AI_CHATBOT ve tüm rollerin portal Chatbot butonları altında entegre olarak çalışır.
 */
@Composable
fun AiChatbotScreen(
    state: Okul360UiState,
    onNavigateAction: (String, String?) -> Unit,
    onBack: (() -> Unit)? = null
) {
    key(com.example.ui.components.financePresentationScope(state)) {
        ScopedAiChatbotScreen(state, onNavigateAction, onBack)
    }
}

@Composable
private fun ScopedAiChatbotScreen(
    state: Okul360UiState,
    onNavigateAction: (String, String?) -> Unit,
    onBack: (() -> Unit)?,
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    val role = state.selectedRole
    val client = remember { NativeChatbotClient() }
    val parentChildren = if (role == UserRole.PARENT) state.parentChildren else emptyList()

    val chatContext = rememberChatContext()
    val messages = chatContext.messages
    var selectedStudentId by remember(parentChildren.map { it.id }) {
        mutableStateOf(chatContext.selectedStudentId?.takeIf { id -> parentChildren.any { it.id == id } }
            ?: parentChildren.singleOrNull()?.id)
    }
    var studentMenu by remember { mutableStateOf(false) }
    LaunchedEffect(selectedStudentId) { chatContext.selectStudent(selectedStudentId) }

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("tr", "TR")
                }
            }
            ttsEngine = tts
        } catch (_: Exception) {}

        onDispose {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (_: Exception) {}
        }
    }

    val roleGradient = remember(role) {
        when (role) {
            UserRole.CUSTOM -> listOf(Color(0xFF334155), Color(0xFF475569))
            UserRole.SUPERADMIN -> listOf(Color(0xFF4C1D95), Color(0xFF6D28D9), Color(0xFF8B5CF6))
            UserRole.OWNER -> listOf(Color(0xFF312E81), Color(0xFF4338CA), Color(0xFF6366F1))
            UserRole.ADMIN -> listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF3B82F6))
            UserRole.MANAGER -> listOf(Color(0xFF134E4A), Color(0xFF0D9488), Color(0xFF14B8A6))
            UserRole.TEACHER -> listOf(Color(0xFF064E3B), Color(0xFF059669), Color(0xFF10B981))
            UserRole.FINANCE -> listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFF59E0B))
            UserRole.PARENT -> listOf(Color(0xFF581C87), Color(0xFF7C3AED), Color(0xFF8B5CF6))
            UserRole.CANTEEN -> listOf(Color(0xFF7C2D12), Color(0xFFEA580C), Color(0xFFF97316))
        }
    }

    val suggestions = remember(role) {
        Okul360ChatbotEngine.getDefaultSuggestionsForRole(role)
    }

    fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || isThinking) return

        val studentId = selectedStudentId
        val history = messages.filter { it.includeInHistory }.map { ChatbotTurn(it.text, it.isUser, it.intent) }
        val userMsg = ChatbotMessage(text = trimmed, isUser = true)
        chatContext.addMessage(userMsg)
        inputText = ""
        isThinking = true

        coroutineScope.launch {
            try {
                val answer = client.ask(trimmed, role, studentId, history)
                val actions = answer.actions.distinctBy { it.first }.mapNotNull { (tag, label) ->
                    if (chatbotDestination(state, tag, studentId) == null) null
                    else ChatbotAction(label, Icons.Default.OpenInNew, tag)
                }
                chatContext.addMessage(ChatbotMessage(text = answer.text, isUser = false,
                    intent = answer.intent, roleContext = role, suggestedActions = actions))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                chatContext.addMessage(ChatbotMessage(text = "Okul asistanından yanıt alınamadı. Tekrar deneyebilirsiniz.",
                    isUser = false, includeInHistory = false, roleContext = role))
            } finally {
                isThinking = false
            }
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Üst Bilgi Başlığı (Top Banner)
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Geri",
                                    tint = Color(0xFF1E293B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(roleGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("SCS360 Okul Asistanı", fontWeight = FontWeight.Bold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    // Temizle Butonu
                    IconButton(
                        enabled = !isThinking,
                        onClick = { chatContext.clearHistory() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Sohbeti Temizle",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

        if (parentChildren.size > 1 || chatbotDestination(state, CHATBOT_MESSAGING, selectedStudentId) != null) {
            Row(Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (parentChildren.size > 1) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(enabled = !isThinking, onClick = { studentMenu = true }) {
                            Text(parentChildren.find { it.id == selectedStudentId }?.full_name ?: "Öğrenci seçin",
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(expanded = studentMenu && !isThinking, onDismissRequest = { studentMenu = false },
                            modifier = Modifier.heightIn(max = 240.dp)) {
                            parentChildren.forEach { child ->
                                DropdownMenuItem(text = { Text(child.full_name) }, onClick = {
                                    studentMenu = false
                                    selectedStudentId = child.id
                                    chatContext.selectStudent(child.id)
                                    inputText = ""
                                })
                            }
                        }
                    }
                }
                if (chatbotDestination(state, CHATBOT_MESSAGING, selectedStudentId) != null) {
                    TextButton(onClick = { onNavigateAction(CHATBOT_MESSAGING, selectedStudentId) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("chatbot_open_messaging")) {
                        Text(if (role == UserRole.PARENT) "Öğretmenle iletişim" else "Sohbetleri aç")
                    }
                }
            }
        }

        // Mesaj Listesi
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 840.dp)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    message = msg,
                    roleGradient = roleGradient,
                    onActionClick = { actionTag -> onNavigateAction(actionTag, selectedStudentId) },
                    onCopyClick = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Yanıt metni panoya kopyalandı", Toast.LENGTH_SHORT).show()
                    },
                    onSpeakClick = { text ->
                        val cleanText = text.replace("*", "").replace("•", "").replace("✅", "").replace("⚠️", "")
                        if (speakingMessageId == msg.id) {
                            ttsEngine?.stop()
                            speakingMessageId = null
                        } else {
                            ttsEngine?.stop()
                            speakingMessageId = msg.id
                            ttsEngine?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, msg.id)
                        }
                    },
                    isSpeaking = (speakingMessageId == msg.id)
                )
            }

            if (isThinking) {
                item {
                    ThinkingIndicator(roleGradient = roleGradient)
                }
            }
        }

        // Sık Sorulan Öneri Çipleri (Suggestion Chips) & Mesaj Yazma Alanı
        Surface(
            color = Color.White,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp)
                        .padding(top = 8.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = { sendMessage(suggestion) },
                                label = {
                                    Text(
                                        text = suggestion,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Mesaj Yazma Alanı (Bottom Input Bar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp, max = 120.dp)
                                .testTag("ai_screen_input_field"),
                            placeholder = {
                                Text(
                                    text = "Herhangi bir öğrenci, sınıf, öğretmen, borç veya DB sorusu...",
                                    fontSize = 12.5.sp,
                                    color = Color.Gray
                                )
                            },
                            singleLine = false,
                            maxLines = 3,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = roleGradient.first(),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputText.isNotBlank()) {
                                    sendMessage(inputText)
                                    focusManager.clearFocus()
                                }
                            })
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    sendMessage(inputText)
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = inputText.isNotBlank() && !isThinking,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank() && !isThinking) Brush.horizontalGradient(roleGradient)
                                    else Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
                                )
                                .testTag("ai_screen_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gönder",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
