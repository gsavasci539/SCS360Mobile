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
import com.example.ui.components.ChatBubbleItem
import com.example.ui.components.ChatbotAction
import com.example.ui.components.ChatbotMessage
import com.example.ui.components.Okul360ChatbotEngine
import com.example.ui.components.ThinkingIndicator
import com.example.ui.context.rememberChatContext
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * SCS360 Birinci Sınıf Tam Ekran AI Asistan ve Küresel DB Sorgulama Ekranı.
 * MainTab.AI_CHATBOT ve tüm rollerin portal Chatbot butonları altında entegre olarak çalışır.
 */
@Composable
fun AiChatbotScreen(
    state: Okul360UiState,
    onNavigateAction: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    val role = state.selectedRole
    val userName = state.currentUser.resolvedName

    val chatContext = rememberChatContext()
    val messages = chatContext.messages

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

        val userMsg = ChatbotMessage(text = trimmed, isUser = true)
        chatContext.addMessage(userMsg)
        inputText = ""
        isThinking = true

        coroutineScope.launch {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
            delay(200)
            val historyTranscript = chatContext.getRecentHistoryTranscript()
            val historyPairs = messages.takeLast(6).map { it.text to it.isUser }
            val aiResponse = Okul360ChatbotEngine.processUserQueryAsync(
                query = trimmed,
                role = role,
                state = state,
                conversationHistory = historyTranscript,
                messageHistory = historyPairs
            )
            chatContext.addMessage(aiResponse)
            isThinking = false
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SCS360 Yapay Zeka Danışmanı",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Canlı DB",
                                        color = Color(0xFF166534),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${state.currentUser.resolvedRole} Modu • 30+ Veritabanı Tablosu Aktif",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Temizle Butonu
                    IconButton(
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

        // Canlı DB Metrik Çubuğu
        Surface(
            color = Color(0xFFF1F5F9),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickMetricBadge(label = "Öğrenci", value = "${state.students.size}", color = Color(0xFF2563EB)) {
                    sendMessage("öğrenci listesi")
                }
                QuickMetricBadge(label = "Sınıf", value = "${state.classes.size}", color = Color(0xFF0D9488)) {
                    sendMessage("sınıflar")
                }
                QuickMetricBadge(label = "Katılım", value = "%95.0", color = Color(0xFF16A34A)) {
                    sendMessage("yoklama durumu")
                }
                QuickMetricBadge(label = "Turnike", value = "Aktif", color = Color(0xFF4F46E5)) {
                    sendMessage("turnike logları")
                }
                QuickMetricBadge(label = "Servis", value = "4 Hat", color = Color(0xFFD97706)) {
                    sendMessage("servis filosu")
                }
                QuickMetricBadge(label = "Kasa", value = "₺${String.format(Locale.US, "%,.0f", state.invoices.sumOf { it.paid_amount })}", color = Color(0xFF059669)) {
                    sendMessage("kasa ve tahsilat")
                }
                QuickMetricBadge(label = "Tüm Tablolar", value = "DB Özeti", color = Color(0xFF7C3AED)) {
                    sendMessage("tüm veritabanı tabloları")
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
                    onActionClick = { actionTag ->
                        onNavigateAction(actionTag)
                    },
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

@Composable
private fun QuickMetricBadge(
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$label: ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B)
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
