package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Announcement
import com.example.data.model.CommunicationTemplate
import com.example.data.model.NotificationItem
import com.example.ui.components.AnnouncementEmptyState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.Okul360CircularLoadingIndicator
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.dialogs.ConfirmDeleteDialog
import com.example.ui.dialogs.CreateAnnouncementDialog
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationScreen(
    state: Okul360UiState,
    onCreateAnnouncement: (title: String, content: String, category: String, target: String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onSaveAnnouncementDraft: ((title: String, content: String, category: String, target: String) -> Unit)? = null,
    onDeleteAnnouncementDraft: ((String) -> Unit)? = null,
    onPublishAnnouncementDraft: ((String) -> Unit)? = null,
    onSendNotification: (title: String, message: String, targetRole: String, channel: String) -> Unit,
    onDeleteNotification: (String) -> Unit,
    onCreateTemplate: (title: String, type: String, content: String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onTriggerPushTest: ((title: String, message: String, channel: String) -> Unit)? = null,
    onTriggerDelayedPushTest: ((delaySeconds: Int, title: String, message: String, channel: String) -> Unit)? = null,
    onOpenNotificationSettings: (() -> Unit)? = null,
    onAnnouncementClick: ((Announcement) -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Duyurular", "Giden Bildirimler", "Mesaj Şablonları", "Push Bildirim Testi")

    var announcementViewMode by remember { mutableIntStateOf(0) } // 0: Published, 1: Drafts
    var showCreateAnnouncementDialog by remember { mutableStateOf(false) }
    var editingDraft by remember { mutableStateOf<Announcement?>(null) }
    var announcementToDelete by remember { mutableStateOf<Announcement?>(null) }
    var draftToDelete by remember { mutableStateOf<Announcement?>(null) }

    var showSendNotificationDialog by remember { mutableStateOf(false) }
    var notificationToDelete by remember { mutableStateOf<NotificationItem?>(null) }

    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<CommunicationTemplate?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryBlue
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // ANNOUNCEMENTS TAB
                Column(modifier = Modifier.fillMaxSize()) {
                    // Compact Header with Sub-filter & Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = announcementViewMode == 0,
                                onClick = { announcementViewMode = 0 },
                                label = { Text("Yayınlananlar (${state.announcements.size})", fontSize = 11.5.sp, fontWeight = if (announcementViewMode == 0) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            )
                            FilterChip(
                                selected = announcementViewMode == 1,
                                onClick = { announcementViewMode = 1 },
                                label = { Text("Taslaklar (${state.announcementDrafts.size})", fontSize = 11.5.sp, fontWeight = if (announcementViewMode == 1) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    Icon(Icons.Default.Drafts, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            )
                        }

                        Button(
                            onClick = {
                                editingDraft = null
                                showCreateAnnouncementDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Duyuru Oluştur", fontSize = 11.5.sp)
                        }
                    }

                    if (state.isDataLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Okul360CircularLoadingIndicator(
                                size = 52.dp,
                                label = "Duyurular yükleniyor..."
                            )
                        }
                    } else if (announcementViewMode == 0) {
                        // PUBLISHED ANNOUNCEMENTS
                        if (state.announcements.isEmpty()) {
                            AnnouncementEmptyState(
                                isSearchingOrFiltering = false,
                                onResetFilters = {},
                                onCreateAnnouncement = {
                                    editingDraft = null
                                    showCreateAnnouncementDialog = true
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    RoleContextualAiCard(
                                        screenName = "COMMUNICATION",
                                        state = state
                                    )
                                }
                                items(state.announcements) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(if (onAnnouncementClick != null) Modifier.clickable { onAnnouncementClick(item) } else Modifier),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                                                    Text(text = "${item.created_at} • ${item.author_name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(
                                                    onClick = { announcementToDelete = item },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = item.content, fontSize = 13.sp, color = Color(0xFF334155))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(6.dp)) {
                                                    Text(text = item.category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                                }
                                                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp)) {
                                                    Text(text = "Hedef: ${item.target_audience}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color(0xFF64748B))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ANNOUNCEMENT DRAFTS
                        if (state.announcementDrafts.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Outlined.Drafts,
                                title = "Kayıtlı Taslak Yok",
                                description = "Duyuru hazırlarken 'Taslak Olarak Kaydet' butonunu kullanarak daha sonra yayınlamak üzere taslak oluşturabilirsiniz.",
                                primaryActionLabel = "Taslak Duyuru Yaz",
                                onPrimaryAction = {
                                    editingDraft = null
                                    showCreateAnnouncementDialog = true
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.announcementDrafts) { draft ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            color = Color(0xFFFEF3C7),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "TASLAK",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFB45309),
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(text = draft.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                                                    }
                                                    Text(text = "Son Düzenleme: ${draft.created_at} • ${draft.author_name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(
                                                    onClick = { draftToDelete = draft },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Taslağı Sil", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = draft.content, fontSize = 13.sp, color = Color(0xFF475569))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(6.dp)) {
                                                        Text(text = draft.category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                                    }
                                                    Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp)) {
                                                        Text(text = "Hedef: ${draft.target_audience}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color(0xFF64748B))
                                                    }
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            editingDraft = draft
                                                            showCreateAnnouncementDialog = true
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                        modifier = Modifier.height(40.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text("Düzenle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            onPublishAnnouncementDraft?.invoke(draft.id)
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                        modifier = Modifier.height(40.dp)
                                                    ) {
                                                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(15.dp))
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text("Şimdi Yayınla", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // NOTIFICATIONS OUTBOX TAB
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Giden Bildirim Havuzu (${state.notifications.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandNavy
                    )
                    Button(
                        onClick = { showSendNotificationDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bildirim Gönder", fontSize = 12.sp)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.notifications) { notif ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (notif.type) {
                                                    "ATTENDANCE" -> Color(0xFFFEF3C7)
                                                    "FINANCE" -> Color(0xFFD1FAE5)
                                                    else -> Color(0xFFEFF6FF)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (notif.type) {
                                                "ATTENDANCE" -> Icons.Default.FactCheck
                                                "FINANCE" -> Icons.Default.Receipt
                                                else -> Icons.Default.Notifications
                                            },
                                            contentDescription = null,
                                            tint = when (notif.type) {
                                                "ATTENDANCE" -> Color(0xFFD97706)
                                                "FINANCE" -> Color(0xFF059669)
                                                else -> PrimaryBlue
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                                        Text(text = notif.message, fontSize = 12.sp, color = Color(0xFF334155))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = notif.created_at, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                                IconButton(
                                    onClick = { notificationToDelete = notif },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // TEMPLATES TAB
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMS & Mesaj Şablonları (${state.communicationTemplates.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BrandNavy
                    )
                    Button(
                        onClick = { showCreateTemplateDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Yeni Şablon", fontSize = 12.sp)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.communicationTemplates) { tpl ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            color = if (tpl.channel == "SMS") Color(0xFFFEF3C7) else Color(0xFFEFF6FF),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = tpl.channel,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = if (tpl.channel == "SMS") Color(0xFFB45309) else PrimaryBlue
                                            )
                                        }
                                        Text(text = tpl.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                                    }
                                    IconButton(
                                        onClick = { templateToDelete = tpl },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = tpl.content,
                                        modifier = Modifier.padding(10.dp),
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // PUSH NOTIFICATION SYSTEM & TEST CONTROLS
                var customPushTitle by remember { mutableStateOf("") }
                var customPushBody by remember { mutableStateOf("") }
                var selectedChannel by remember { mutableStateOf("scs360_announcements") }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        // Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Android Push Bildirim Motoru",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = BrandNavy
                                        )
                                        Text(
                                            text = "Arka Plan Sync & Heads-up Bildirimler Aktif",
                                            fontSize = 12.sp,
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Uygulama arka planda veya kapalı olsa bile Android işletim sistemi düzeyinde sesli, titreşimli ve başlık açılır pencereli (heads-up) gerçek bildirimler iletilir.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 17.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenNotificationSettings?.invoke() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Bildirim Ayarları", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            onTriggerDelayedPushTest?.invoke(
                                                3,
                                                "🚀 Arka Plan Test Bildirimi",
                                                "Bu bildirim uygulama arka plandayken veya kilit ekranındayken başarıyla iletildi!",
                                                "scs360_announcements"
                                            )
                                        },
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("3sn Sonra Arka Plan Testi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Tek Dokunuşla Hızlı Test Bildirimleri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = "Butonlara basıp uygulamayı arka plana alarak sistem bildirim tepsisinde görüntüleyebilirsiniz:",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 1. Duyuru Push Testi
                            ElevatedCard(
                                onClick = {
                                    onTriggerPushTest?.invoke(
                                        "📢 Yeni Okul Duyurusu",
                                        "2026-2027 Eğitim Öğretim Yılı Bahar Şenliği & Kermes Programı yayınlandı!",
                                        "scs360_announcements"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDBEAFE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Campaign, contentDescription = null, tint = PrimaryBlue)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Duyuru Push Testi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Kanal: Duyurular (Ses & Titreşim)", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Icon(Icons.Default.Send, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                }
                            }

                            // 2. Yoklama Push Testi
                            ElevatedCard(
                                onClick = {
                                    onTriggerPushTest?.invoke(
                                        "🔔 Yoklama: Ali Kaya",
                                        "Ali Kaya (1-A Sınıfı) saat 08:45 itibarıyla okula giriş yapmıştır.",
                                        "scs360_attendance"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFD1FAE5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color(0xFF059669))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Yoklama & Güvenlik Push Testi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Kanal: Giriş-Çıkış (Öncelikli Başlık)", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                }
                            }

                            // 3. Taksit & Ödeme Push Testi
                            ElevatedCard(
                                onClick = {
                                    onTriggerPushTest?.invoke(
                                        "💳 Taksit Hatırlatması",
                                        "Sayın Velimiz, Mart 2026 taksitinin (4.500 ₺) son ödeme gününe 2 gün kaldı.",
                                        "scs360_finance"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFEF3C7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFD97706))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Finans & Taksit Push Testi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Kanal: Ödeme & Vade Uyarıları", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                }
                            }

                            // 4. Öğretmen Mesajı Push Testi
                            ElevatedCard(
                                onClick = {
                                    onTriggerPushTest?.invoke(
                                        "💬 Öğretmen Mesajı: Ayşe Yılmaz",
                                        "Bugünkü fen deneyinde öğrencimiz çok başarılı bir katılım gösterdi.",
                                        "scs360_messages"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF3E8FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF7C3AED))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Öğretmen-Veli Mesaj Push Testi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Kanal: Anlık Mesajlaşma", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Özel Push Bildirim Oluştur & Gönder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandNavy
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = customPushTitle,
                                    onValueChange = { customPushTitle = it },
                                    label = { Text("Bildirim Başlığı") },
                                    placeholder = { Text("Örn: Acil Servis Değişikliği") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = customPushBody,
                                    onValueChange = { customPushBody = it },
                                    label = { Text("Bildirim Metni") },
                                    placeholder = { Text("Örn: Yoğun kar yağışı nedeniyle yarın okullar tatil edilmiştir.") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text("Kanal Seçimi:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = selectedChannel == "scs360_announcements",
                                        onClick = { selectedChannel = "scs360_announcements" },
                                        label = { Text("Duyuru", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = selectedChannel == "scs360_attendance",
                                        onClick = { selectedChannel = "scs360_attendance" },
                                        label = { Text("Yoklama", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = selectedChannel == "scs360_finance",
                                        onClick = { selectedChannel = "scs360_finance" },
                                        label = { Text("Finans", fontSize = 11.sp) }
                                    )
                                }

                                Button(
                                    onClick = {
                                        val titleToSend = customPushTitle.ifBlank { "SCS360 Sistem Bildirimi" }
                                        val bodyToSend = customPushBody.ifBlank { "Yeni bir bilgilendirme mesajınız var." }
                                        onTriggerPushTest?.invoke(titleToSend, bodyToSend, selectedChannel)
                                        customPushTitle = ""
                                        customPushBody = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Telefona Push Bildirimi Gönder", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateAnnouncementDialog) {
        CreateAnnouncementDialog(
            initialTitle = editingDraft?.title ?: "",
            initialContent = editingDraft?.content ?: "",
            initialCategory = editingDraft?.category ?: "GENERAL",
            initialTarget = editingDraft?.target_audience ?: "ALL",
            onDismiss = {
                showCreateAnnouncementDialog = false
                editingDraft = null
            },
            onConfirm = { title, content, cat, tgt ->
                val draft = editingDraft
                if (draft != null) {
                    onDeleteAnnouncementDraft?.invoke(draft.id)
                }
                onCreateAnnouncement(title, content, cat, tgt)
                showCreateAnnouncementDialog = false
                editingDraft = null
            },
            onSaveDraft = { title, content, cat, tgt ->
                val draft = editingDraft
                if (draft != null) {
                    onDeleteAnnouncementDraft?.invoke(draft.id)
                }
                onSaveAnnouncementDraft?.invoke(title, content, cat, tgt)
                showCreateAnnouncementDialog = false
                editingDraft = null
            }
        )
    }

    draftToDelete?.let { draft ->
        ConfirmDeleteDialog(
            title = "Taslağı Sil",
            message = "\"${draft.title}\" başlıklı duyuru taslağını silmek istiyor musunuz?",
            onDismiss = { draftToDelete = null },
            onConfirm = {
                onDeleteAnnouncementDraft?.invoke(draft.id)
                draftToDelete = null
            }
        )
    }

    announcementToDelete?.let { ann ->
        ConfirmDeleteDialog(
            title = "Duyuruyu Sil",
            message = "${ann.title} başlıklı duyuruyu silmek istiyor musunuz?",
            onDismiss = { announcementToDelete = null },
            onConfirm = {
                onDeleteAnnouncement(ann.id)
                announcementToDelete = null
            }
        )
    }

    if (showSendNotificationDialog) {
        var notifTitle by remember { mutableStateOf("") }
        var notifBody by remember { mutableStateOf("") }
        var targetRole by remember { mutableStateOf("VELI") }
        var channel by remember { mutableStateOf("PUSH") }

        AlertDialog(
            onDismissRequest = { showSendNotificationDialog = false },
            title = { Text("Yeni Bildirim Gönder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = notifTitle, onValueChange = { notifTitle = it }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notifBody, onValueChange = { notifBody = it }, label = { Text("Mesaj") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = targetRole == "VELI", onClick = { targetRole = "VELI" }, label = { Text("Veliler") })
                        FilterChip(selected = targetRole == "OGRETMEN", onClick = { targetRole = "OGRETMEN" }, label = { Text("Öğretmenler") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (notifTitle.isNotBlank() && notifBody.isNotBlank()) {
                            onSendNotification(notifTitle, notifBody, targetRole, channel)
                            showSendNotificationDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Gönder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendNotificationDialog = false }) { Text("Vazgeç") }
            }
        )
    }

    notificationToDelete?.let { notif ->
        ConfirmDeleteDialog(
            title = "Bildirimi Sil",
            message = "${notif.title} bildirimini silmek istiyor musunuz?",
            onDismiss = { notificationToDelete = null },
            onConfirm = {
                onDeleteNotification(notif.id)
                notificationToDelete = null
            }
        )
    }

    if (showCreateTemplateDialog) {
        var tplTitle by remember { mutableStateOf("") }
        var tplType by remember { mutableStateOf("SMS") }
        var tplBody by remember { mutableStateOf("Sayın Velimiz, {OGRENCI_ADI} isimli öğrencimizin...") }

        AlertDialog(
            onDismissRequest = { showCreateTemplateDialog = false },
            title = { Text("Yeni Şablon Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = tplTitle, onValueChange = { tplTitle = it }, label = { Text("Şablon Başlığı") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = tplType == "SMS", onClick = { tplType = "SMS" }, label = { Text("SMS") })
                        FilterChip(selected = tplType == "PUSH", onClick = { tplType = "PUSH" }, label = { Text("Push Bildirim") })
                        FilterChip(selected = tplType == "EMAIL", onClick = { tplType = "EMAIL" }, label = { Text("E-posta") })
                    }
                    OutlinedTextField(value = tplBody, onValueChange = { tplBody = it }, label = { Text("Şablon Metni") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tplTitle.isNotBlank() && tplBody.isNotBlank()) {
                            onCreateTemplate(tplTitle, tplType, tplBody)
                            showCreateTemplateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Oluştur")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTemplateDialog = false }) { Text("Vazgeç") }
            }
        )
    }

    templateToDelete?.let { tpl ->
        ConfirmDeleteDialog(
            title = "Şablonu Sil",
            message = "${tpl.title} şablonunu silmek istiyor musunuz?",
            onDismiss = { templateToDelete = null },
            onConfirm = {
                onDeleteTemplate(tpl.id)
                templateToDelete = null
            }
        )
    }
}
