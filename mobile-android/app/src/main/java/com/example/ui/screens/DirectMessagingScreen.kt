package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.model.UserRole
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

/**
 * Direct Messaging & 1-on-1 Parent-Teacher Communication Module.
 * Provides a secure, verified messaging channel between parents, teachers, and school administration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessagingScreen(
    state: Okul360UiState,
    onSelectConversation: (String?) -> Unit,
    onSendMessage: (conversationId: String, text: String, attachmentName: String?, attachmentType: String?) -> Unit,
    onStartNewConversation: (name: String, role: String, initialMessage: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeConversationId = state.selectedConversationId
    val activeConversation = state.chatConversations.find { it.id == activeConversationId }

    if (activeConversation != null) {
        // Individual 1-on-1 Chat Room
        DirectChatRoom(
            conversation = activeConversation,
            messages = state.chatMessages[activeConversation.id] ?: emptyList(),
            currentUserRole = state.selectedRole,
            onBack = { onSelectConversation(null) },
            onSendMessage = { text, attachmentName, attachmentType ->
                onSendMessage(activeConversation.id, text, attachmentName, attachmentType)
            }
        )
    } else {
        // Conversations List
        DirectConversationsList(
            state = state,
            conversations = state.chatConversations,
            currentUserRole = state.selectedRole,
            onSelectConversation = onSelectConversation,
            onStartNewConversation = onStartNewConversation,
            modifier = modifier
        )
    }
}

/**
 * Conversations list for parent and teacher communication
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectConversationsList(
    state: Okul360UiState,
    conversations: List<ChatConversation>,
    currentUserRole: UserRole,
    onSelectConversation: (String) -> Unit,
    onStartNewConversation: (name: String, role: String, initialMessage: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val filteredConversations = remember(conversations, searchQuery, selectedFilter) {
        conversations.filter { conv ->
            val matchQuery = searchQuery.isBlank() ||
                    conv.participant_name.contains(searchQuery, ignoreCase = true) ||
                    conv.participant_role.contains(searchQuery, ignoreCase = true) ||
                    conv.student_context.contains(searchQuery, ignoreCase = true) ||
                    conv.last_message.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilter) {
                "TEACHER" -> conv.participant_role.contains("Öğretmen", ignoreCase = true)
                "GUIDE" -> conv.participant_role.contains("Rehber", ignoreCase = true) || conv.participant_role.contains("Danışman", ignoreCase = true)
                "ADMIN" -> conv.participant_role.contains("Müdür", ignoreCase = true) || conv.participant_role.contains("İdare", ignoreCase = true)
                else -> true
            }

            matchQuery && matchFilter
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddComment, contentDescription = null) },
                text = { Text("Yeni Mesaj Başlat", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_start_direct_chat")
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            // Header Security Info Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Birebir Güvenli İletişim",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentUserRole == UserRole.PARENT)
                                        "Ders öğretmenleri ve okul rehberliğiyle doğrudan görüşün"
                                    else
                                        "Öğrenci velileri ile kurumsal ve kayıtlı mesajlaşma kanalı",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "KVKK ve MEB kurumsal iletişim protokolüne tam uyumlu",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Contextual AI Communication & Response Insights
            item {
                RoleContextualAiCard(
                    screenName = "DIRECT_MESSAGING",
                    state = state
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Öğretmen, veli veya öğrenci adı ara...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Ara", tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Temizle")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_conversations_input")
                )
            }

            // Filter Chips
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf(
                        "ALL" to "Tüm Sohbetler",
                        "TEACHER" to "Branş Öğretmenleri",
                        "GUIDE" to "Rehberlik & Danışman",
                        "ADMIN" to "İdare"
                    )
                    filters.forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Conversation Items
            if (filteredConversations.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Henüz Sohbet Bulunmuyor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "'Yeni Mesaj Başlat' butonuna tıklayarak öğretmeninizle görüşme başlatabilirsiniz.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredConversations, key = { it.id }) { conv ->
                    ConversationItemCard(
                        conversation = conv,
                        onClick = { onSelectConversation(conv.id) }
                    )
                }
            }
        }
    }

    if (showNewChatDialog) {
        StartNewChatDialog(
            onDismiss = { showNewChatDialog = false },
            onStart = { name, role, msg ->
                onStartNewConversation(name, role, msg)
                showNewChatDialog = false
            }
        )
    }
}

/**
 * Individual Conversation Card
 */
@Composable
private fun ConversationItemCard(
    conversation: ChatConversation,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("conversation_item_${conversation.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with Online dot
            Box {
                Surface(
                    shape = CircleShape,
                    color = Color(conversation.avatar_color_hex),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = conversation.participant_name.take(2).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
                if (conversation.is_online) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .border(2.dp, Color.White, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.participant_name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = conversation.last_message_time,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = conversation.participant_role,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = conversation.last_message,
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = conversation.student_context,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "⏰ ${conversation.office_hours}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Unread badge
            if (conversation.unread_count > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${conversation.unread_count}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 1-on-1 Chat Room with message bubbles, quick preset chips, and input controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectChatRoom(
    conversation: ChatConversation,
    messages: List<ChatMessage>,
    currentUserRole: UserRole,
    onBack: () -> Unit,
    onSendMessage: (text: String, attachmentName: String?, attachmentType: String?) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickResponses = listOf(
        "📝 Ödev konusu hakkında",
        "📅 Randevu talep ediyorum",
        "📄 Föyü indirdik teşekkürler",
        "💊 Sağlık durumu bildirimi"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(conversation.avatar_color_hex),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = conversation.participant_name.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                    Column {
                        Text(
                            text = conversation.participant_name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (conversation.is_online) Color(0xFF10B981) else Color(0xFF94A3B8))
                            )
                            Text(
                                text = if (conversation.is_online) "Çevrimiçi" else "Mesai: ${conversation.office_hours}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                }
            },
            actions = {
                IconButton(onClick = { /* Call shortcut */ }) {
                    Icon(Icons.Default.Phone, contentDescription = "Ara", tint = MaterialTheme.colorScheme.primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            windowInsets = WindowInsets.statusBars
        )

        // Security Note
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Görüşmeler okul politikası gereğince kayıt altındadır.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Messages Bubbles List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(message = msg)
            }
        }

        // Input Bar & Quick Presets at the bottom
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quick Action Preset Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickResponses) { chip ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable {
                                onSendMessage(chip, null, null)
                            }
                        ) {
                            Text(
                                text = chip,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Input bar row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Attach button
                    IconButton(
                        onClick = {
                            onSendMessage("📎 Ek Dosya: çalışma_raporu.pdf", "çalışma_raporu.pdf", "PDF")
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Dosya Ekle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Mesajınızı yazın...", fontSize = 13.sp) },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp, max = 100.dp)
                            .testTag("chat_message_input"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText.trim(), null, null)
                                inputText = ""
                            }
                        })
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText.trim(), null, null)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Gönder",
                            tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Message Bubble Composable
 */
@Composable
private fun MessageBubble(message: ChatMessage) {
    val isMe = message.is_me

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Sender name for other party
        if (!isMe) {
            Text(
                text = message.sender_name,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 16.dp
            ),
            color = if (isMe) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Attachment preview if present
                if (message.attachment_name != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMe) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = message.attachment_name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = message.timestamp,
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isMe) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Okundu",
                            tint = if (message.is_read) Color(0xFF93C5FD) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Start New Chat Dialog (Directory)
 */
@Composable
private fun StartNewChatDialog(
    onDismiss: () -> Unit,
    onStart: (name: String, role: String, initialMessage: String) -> Unit
) {
    var selectedStaff by remember { mutableStateOf<Pair<String, String>?>(null) }
    var initialMessage by remember { mutableStateOf("") }

    val schoolStaff = listOf(
        "Ahmet Yılmaz" to "Matematik & 8-A Rehber Öğretmeni",
        "Selin Kara" to "Fen Bilimleri Öğretmeni",
        "Fatma Demir" to "Türkçe & Edebiyat Öğretmeni",
        "John Smith" to "İngilizce Öğretmeni",
        "Emre Arslan" to "Bilişim & Kodlama Öğretmeni",
        "Gülfem Aydın" to "Psikolojik Danışman & Rehberlik",
        "Mustafa Ak" to "Sosyal Bilgiler Öğretmeni",
        "Kemal Erdem" to "Okul Müdürü & İdare"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val effectiveBottomPadding = maxOf(navBottom, imeBottom)

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .statusBarsPadding()
                .padding(bottom = effectiveBottomPadding)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Yeni Mesaj Gönder",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Öğretmen veya İdari Personel Seçin:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(schoolStaff) { staff ->
                        val isSelected = selectedStaff?.first == staff.first
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStaff = staff }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = staff.first.take(1),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Column {
                                    Text(text = staff.first, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = staff.second, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = initialMessage,
                    onValueChange = { initialMessage = it },
                    label = { Text("İlk Mesajınız") },
                    placeholder = { Text("Görüşmek istediğiniz konuyu özetleyin...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        selectedStaff?.let { staff ->
                            if (initialMessage.isNotBlank()) {
                                onStart(staff.first, staff.second, initialMessage)
                            }
                        }
                    },
                    enabled = selectedStaff != null && initialMessage.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sohbeti Başlat", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
