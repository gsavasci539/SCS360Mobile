package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.remote.DirectMessageDraft
import com.example.data.remote.MessageRecipient
import com.example.data.remote.NativeMessagingClient
import com.example.data.remote.RegistryHttpException
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun DirectMessagingScreen(
    state: Okul360UiState,
    onSelectConversation: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    key(financePresentationScope(state)) {
        if (state.isLoggedIn && NativeNavigationPolicy.has(state.currentUser, "messaging.read")) {
            ScopedMessaging(state, onSelectConversation, modifier)
        } else {
            Text("Mesajları görüntüleme yetkiniz yok.", Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun ScopedMessaging(state: Okul360UiState, select: (String?) -> Unit, modifier: Modifier) {
    val canSend = NativeNavigationPolicy.has(state.currentUser, "messaging.send")
    val api = remember { NativeMessagingClient(state.currentUser.id, state.selectedBranchId.takeUnless { it == "all" }) }
    val scope = rememberCoroutineScope()
    var conversations by remember { mutableStateOf<List<ChatConversation>?>(null) }
    var listError by remember { mutableStateOf(false) }
    var listRevision by remember { mutableIntStateOf(0) }
    var directoryOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ChatConversation?>(null) }
    val drafts = remember { mutableStateMapOf<String, String>() }
    val pending = remember { mutableStateMapOf<String, DirectMessageDraft>() }
    val retryAvailable = remember { mutableStateMapOf<String, Boolean>() }
    var busy by remember { mutableStateOf(false) }
    val partner = selected?.takeIf { it.id == state.selectedConversationId }
    var messages by remember(partner?.id) { mutableStateOf<List<ChatMessage>?>(null) }
    var olderAvailable by remember(partner?.id) { mutableStateOf(false) }
    var historyBusy by remember(partner?.id) { mutableStateOf(false) }
    var historyError by remember(partner?.id) { mutableStateOf(false) }
    var writeError by remember(partner?.id) { mutableStateOf("") }

    fun open(conversation: ChatConversation) {
        selected = conversation
        select(conversation.id)
        directoryOpen = false
    }

    suspend fun loadHistory(conversation: ChatConversation, before: String? = null) {
        historyBusy = true
        historyError = false
        try {
            val page = api.history(conversation, before)
            if (!api.current()) return
            val retained = if (before == null) emptyList() else messages.orEmpty()
            messages = (page + retained).distinctBy { it.id }
            olderAvailable = page.size == NativeMessagingClient.HISTORY_PAGE_SIZE
            listRevision++
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (api.current()) historyError = true
        } finally {
            historyBusy = false
        }
    }

    LaunchedEffect(Unit) { if (state.selectedConversationId != null) select(null) }
    LaunchedEffect(listRevision) {
        listError = false
        try {
            val rows = api.conversations()
            if (api.current()) conversations = rows
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (api.current()) listError = true
        }
    }
    LaunchedEffect(partner?.id) { partner?.let { loadHistory(it) } }

    if (!api.current()) return
    if (partner == null) {
        DirectConversationsList(conversations, listError, canSend && !busy, modifier,
            onRefresh = { listRevision++ }, onNew = { directoryOpen = true }, onOpen = ::open)
        if (directoryOpen && canSend) {
            MessageDirectory(api, onClose = { directoryOpen = false }) { recipient ->
                open(conversations.orEmpty().find { it.id == recipient.id } ?: ChatConversation(
                    id = recipient.id, participant_id = recipient.id, participant_name = recipient.name,
                    participant_role = recipient.roles.joinToString(", "),
                ))
            }
        }
        return
    }

    fun submit(recheck: Boolean, retry: Boolean = false) {
        if (!canSend || busy || historyBusy) return
        val unconfirmed = pending[partner.id]
        val text = drafts[partner.id].orEmpty().trim()
        if (unconfirmed == null && (recheck || retry || text.isBlank() || text.length > 10000)) return
        val draft = unconfirmed ?: DirectMessageDraft(partner.id, text)
        busy = true
        writeError = ""
        scope.launch {
            var acknowledged = false
            try {
                if (!recheck) {
                    api.send(draft)
                    acknowledged = true
                }
                val stored = api.receipt(draft, partner.participant_name)
                if (!api.current()) return@launch
                messages = (messages.orEmpty() + stored).distinctBy { it.id }
                drafts.remove(partner.id)
                pending.remove(partner.id)
                retryAvailable.remove(partner.id)
                listRevision++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (!api.current()) return@launch
                val code = (error as? RegistryHttpException)?.code
                if (unconfirmed == null && !acknowledged && !recheck && code in setOf(401, 403, 404, 409, 422)) {
                    writeError = "Mesaj kaydedilemedi. Alıcı ve gönderim yetkisini kontrol edin."
                } else {
                    pending[partner.id] = draft
                    retryAvailable[partner.id] = recheck && code == 404
                    writeError = "Gönderim sonucu doğrulanamadı."
                }
            } finally {
                busy = false
            }
        }
    }

    DirectChatRoom(
        partner, messages, drafts[partner.id].orEmpty(), historyBusy, historyError, writeError,
        canSend, busy, pending.containsKey(partner.id), retryAvailable[partner.id] == true,
        olderAvailable,
        onBack = { select(null) },
        onRefresh = { if (!historyBusy && !busy) scope.launch { loadHistory(partner) } },
        onOlder = { messages?.firstOrNull()?.let { first ->
            if (!historyBusy && !busy) scope.launch { loadHistory(partner, first.id) }
        } },
        onInput = { drafts[partner.id] = it },
        onSend = { submit(recheck = false) },
        onVerify = { submit(recheck = true) },
        onRetry = { submit(recheck = false, retry = true) },
    )
}

@Composable
private fun DirectConversationsList(
    rows: List<ChatConversation>?, failed: Boolean, canNew: Boolean, modifier: Modifier,
    onRefresh: () -> Unit, onNew: () -> Unit, onOpen: (ChatConversation) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val visible = rows.orEmpty().filter {
        it.participant_name.contains(search, ignoreCase = true) || it.last_message.contains(search, ignoreCase = true)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (canNew) ExtendedFloatingActionButton(
                onClick = onNew, icon = { Icon(Icons.Default.AddComment, contentDescription = null) },
                text = { Text("Yeni mesaj") }, modifier = Modifier.testTag("fab_start_direct_chat"),
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 84.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(search, { search = it }, Modifier.weight(1f).testTag("search_conversations_input"),
                        label = { Text("Sohbet ara") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) })
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Sohbetleri yenile") }
                }
            }
            if (failed) item { Text("Sohbetler yüklenemedi.", color = MaterialTheme.colorScheme.error) }
            else if (rows == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            else if (visible.isEmpty()) item {
                Text(if (search.isBlank()) "Henüz sohbet yok." else "Aramanızla eşleşen sohbet yok.")
            }
            items(visible, key = { it.id }) { conversation ->
                ConversationItemCard(conversation) { onOpen(conversation) }
            }
        }
    }
}

@Composable
private fun ConversationItemCard(conversation: ChatConversation, open: () -> Unit) {
    ElevatedCard(onClick = open, modifier = Modifier.fillMaxWidth().testTag("conversation_item_${conversation.id}")) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(conversation.participant_name.take(2).uppercase(), Modifier.padding(9.dp), fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(conversation.participant_name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(conversation.last_message, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall)
                Text(conversation.last_message_time, style = MaterialTheme.typography.labelSmall)
            }
            if (conversation.unread_count > 0) Badge { Text(conversation.unread_count.toString()) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DirectChatRoom(
    partner: ChatConversation, messages: List<ChatMessage>?, text: String, historyBusy: Boolean, historyError: Boolean,
    writeError: String, canSend: Boolean, busy: Boolean, pending: Boolean, retry: Boolean, olderAvailable: Boolean,
    onBack: () -> Unit, onRefresh: () -> Unit, onOlder: () -> Unit, onInput: (String) -> Unit, onSend: () -> Unit,
    onVerify: () -> Unit, onRetry: () -> Unit,
) {
    val inputEnabled = canSend && !busy && !pending
    val sendEnabled = inputEnabled && !historyBusy && text.isNotBlank()
    Column(Modifier.fillMaxSize().imePadding()) {
        TopAppBar(
            title = { Text(partner.participant_name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Sohbetlere dön") } },
            actions = { IconButton(enabled = !historyBusy && !busy, onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Mesajları yenile")
            } },
        )
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
            reverseLayout = true, verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)) {
            items(messages.orEmpty().asReversed(), key = { it.id }) { MessageBubble(it) }
            if (historyBusy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (historyError) item { Text("Mesaj geçmişi yüklenemedi.", color = MaterialTheme.colorScheme.error) }
            if (olderAvailable) item { TextButton(enabled = !historyBusy && !busy, onClick = onOlder) { Text("Daha eski mesajlar") } }
            if (messages?.isEmpty() == true && !historyBusy && !historyError) item { Text("Bu sohbette henüz mesaj yok.") }
        }
        if (canSend) {
            Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    val narrow = LocalConfiguration.current.screenWidthDp < 440
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (writeError.isNotEmpty()) Text(writeError, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                    if (pending) {
                        TextButton(enabled = !busy && !historyBusy, onClick = onVerify) { Text("Sonucu kontrol et") }
                        if (retry) TextButton(enabled = !busy && !historyBusy, onClick = onRetry) { Text("Aynı isteği yeniden gönder") }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(text, { if (it.length <= 10000) onInput(it) },
                            Modifier.weight(1f).testTag("chat_message_input"),
                            enabled = inputEnabled, label = { Text("Mesaj") }, maxLines = if (narrow) 2 else 3,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { if (sendEnabled) onSend() }))
                        IconButton(enabled = sendEnabled, onClick = onSend,
                            modifier = Modifier.size(48.dp).testTag("send_message_button")) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Gönder")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.is_me) Alignment.End else Alignment.Start) {
        Surface(shape = MaterialTheme.shapes.medium,
            color = if (message.is_me) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(0.83f)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!message.is_me) Text(message.sender_name, style = MaterialTheme.typography.labelMedium)
                if (message.attachment_name != null) Text(message.attachment_name, style = MaterialTheme.typography.labelMedium)
                Text(message.text)
                val read = if (message.is_me && message.is_read) " · Okundu" else ""
                Text(message.timestamp + read, Modifier.align(Alignment.End), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MessageDirectory(api: NativeMessagingClient, onClose: () -> Unit, onOpen: (MessageRecipient) -> Unit) {
    var rows by remember { mutableStateOf<List<MessageRecipient>?>(null) }
    var search by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(revision) {
        failed = false
        try {
            val contacts = api.recipients()
            if (api.current()) rows = contacts
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (api.current()) failed = true
        }
    }
    val visible = rows.orEmpty().filter { it.name.contains(search, ignoreCase = true) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(0.94f).widthIn(max = 560.dp)
            .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.imePadding().padding(12.dp)) {
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Text("Alıcı seçin", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Alıcı ara") }, singleLine = true)
                    }
                    if (failed) item {
                        Text("Alıcılar yüklenemedi.")
                        TextButton(onClick = { revision++ }) { Text("Tekrar dene") }
                    } else if (rows == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    else if (visible.isEmpty()) item { Text("Uygun alıcı bulunamadı.") }
                    items(visible, key = { it.id }) { recipient ->
                        ElevatedCard(onClick = { onOpen(recipient) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Column(Modifier.padding(10.dp)) {
                                Text(recipient.name, fontWeight = FontWeight.SemiBold)
                                if (recipient.roles.isNotEmpty()) Text(recipient.roles.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                TextButton(onClick = onClose) { Text("Kapat") }
            }
        }
    }
}
