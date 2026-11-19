package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.viewmodel.Okul360UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatbotMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
    val intent: String? = null,
    val includeInHistory: Boolean = true,
    val roleContext: UserRole? = null,
    val suggestedActions: List<ChatbotAction> = emptyList()
)

data class ChatbotAction(
    val label: String,
    val icon: ImageVector,
    val actionTag: String
)

object Okul360ChatbotEngine {
    fun getInitialGreeting(role: UserRole, userName: String) = ChatbotMessage(
        text = "Merhaba $userName. Okul asistanına sorunuzu yazın.",
        isUser = false,
        includeInHistory = false,
        roleContext = role,
    )

    fun getDefaultSuggestionsForRole(role: UserRole): List<String> = when (role) {
        UserRole.PARENT -> listOf("Öğrencimin ders durumu", "Ödevleri göster", "Ödeme durumum")
        UserRole.TEACHER -> listOf("Sınıf yoklaması", "Ödevleri göster")
        UserRole.FINANCE, UserRole.OWNER -> listOf("Geciken ödemeler", "Tahsilat özeti")
        UserRole.CANTEEN -> listOf("Kantin satış özeti")
        UserRole.ADMIN, UserRole.MANAGER, UserRole.SUPERADMIN -> listOf("Bugünkü yoklama", "Şube raporu")
        UserRole.CUSTOM -> emptyList()
    }
}

@Composable
fun Okul360ChatbotModal(
    state: Okul360UiState,
    onDismiss: () -> Unit,
    onNavigateAction: ((actionTag: String, studentId: String?) -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        com.example.ui.screens.AiChatbotScreen(
            state = state,
            onNavigateAction = { actionTag, studentId ->
                onNavigateAction?.invoke(actionTag, studentId)
            },
            onBack = onDismiss
        )
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatbotMessage,
    roleGradient: List<Color>,
    onActionClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onSpeakClick: ((String) -> Unit)? = null,
    isSpeaking: Boolean = false
) {
    if (message.isUser) {
        // User Message (Right aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = roleGradient.first(),
                modifier = Modifier.widthIn(max = 290.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message.timestamp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 9.5.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    } else {
        // AI Assistant Message (Left aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // AI Avatar Icon
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(roleGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f, fill = false).widthIn(max = 320.dp)) {
                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = message.text,
                            color = Color(0xFF1E293B),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )

                        // Action Buttons inside Chat Bubble
                        if (message.suggestedActions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                message.suggestedActions.forEach { act ->
                                    OutlinedButton(
                                        onClick = { onActionClick(act.actionTag) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, roleGradient.first().copy(alpha = 0.4f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = roleGradient.first())
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(imageVector = act.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = act.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        // Message Footer: Timestamp, TTS, and Copy Icon
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "✨ SCS360 AI • ${message.timestamp}",
                                fontSize = 9.5.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (onSpeakClick != null) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                        contentDescription = "Seslendir",
                                        tint = if (isSpeaking) roleGradient.first() else Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clip(CircleShape)
                                            .clickable { onSpeakClick(message.text) }
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Kopyala",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .clickable { onCopyClick(message.text) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator(roleGradient: List<Color>) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(roleGradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Yanıt oluşturuluyor...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B).copy(alpha = alpha)
                )
            }
        }
    }
}

/**
 * Global Floating Chatbot FAB Component for AppShell and Portal Screens
 */
