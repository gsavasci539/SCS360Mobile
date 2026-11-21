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
import com.example.data.model.CommunicationTemplate
import com.example.data.model.NotificationItem
import com.example.ui.components.EmptyStateView
import com.example.ui.dialogs.ConfirmDeleteDialog
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationScreen(
    state: Okul360UiState,
    onSendNotification: (title: String, message: String, targetRole: String, channel: String) -> Unit,
    onDeleteNotification: (String) -> Unit,
    onCreateTemplate: (title: String, type: String, content: String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Duyurular", "Giden Bildirimler", "Mesaj Şablonları")

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
        ScrollableTabRow(
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
            0 -> NativeAnnouncementManagementPane(state)
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
        }
    }

    // Dialogs

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
