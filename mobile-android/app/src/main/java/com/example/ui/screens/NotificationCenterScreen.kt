package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Announcement
import com.example.data.model.NotificationItem
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

/**
 * Bildirim Merkezi (Notification Center) Screen
 * Displays historical announcements, system notifications, attendance alerts, 
 * finance/tuition reminders, and academic grade notices with unread state emphasis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    state: Okul360UiState,
    onBack: () -> Unit,
    onAnnouncementClick: (Announcement) -> Unit,
    onOpenFinance: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenReportCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Sample Notification Items combining Announcements & System events
    var notificationsList by remember {
        mutableStateOf(
            listOf(
                NotificationItem(
                    id = "n-1",
                    title = "🔔 1. Dönem Karneleri Yayınlandı!",
                    message = "2025-2026 Eğitim Öğretim Yılı 1. Dönem resmi karne ve takdir/teşekkür belgeleri sisteme yüklendi.",
                    type = "ACADEMIC",
                    created_at = "10:30 (Bugün)",
                    is_read = false
                ),
                NotificationItem(
                    id = "n-2",
                    title = "✅ Günlük Yoklama Bildirimi",
                    message = "Öğrenciniz saat 08:42'de okul turnikesinden başarıyla giriş yaptı ve sınıfta mevcuttur.",
                    type = "ATTENDANCE",
                    created_at = "08:45 (Bugün)",
                    is_read = false
                ),
                NotificationItem(
                    id = "n-3",
                    title = "📢 Bilim Fuarı ve Robotik Sergisi",
                    message = "Tüm öğrenci ve velilerimiz Cuma günü saat 14:00'te konferans salonunda düzenlenecek STEM fuarına davetlidir.",
                    type = "ANNOUNCEMENT",
                    created_at = "Dün 16:15",
                    is_read = false
                ),
                NotificationItem(
                    id = "n-4",
                    title = "💳 Ekim Ayı Eğitim Taksiti Hatırlatması",
                    message = "Ekim ayı servis ve eğitim taksitinizin son ödeme tarihi 5 gündür. Online ödeme için tıklayın.",
                    type = "FINANCE",
                    created_at = "2 gün önce",
                    is_read = true
                ),
                NotificationItem(
                    id = "n-5",
                    title = "📝 Matematik 2. Yazılı Sınav Sonuçları",
                    message = "8-A sınıfı Matematik 2. Yazılı sınavı değerlendirilmiş olup notlar sisteme girilmiştir.",
                    type = "ACADEMIC",
                    created_at = "3 gün önce",
                    is_read = true
                ),
                NotificationItem(
                    id = "n-6",
                    title = "💊 Sağlık & İlaç Bildirimi Alındı",
                    message = "Öğrenci velisi tarafından iletilen öğle saati antibiyotik kullanım talebi okul hemşiresine iletildi.",
                    type = "GENERAL",
                    created_at = "4 gün önce",
                    is_read = true
                )
            )
        )
    }

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, UNREAD, ANNOUNCEMENT, ACADEMIC, ATTENDANCE, FINANCE
    var searchQuery by remember { mutableStateOf("") }

    val unreadCount = notificationsList.count { !it.is_read }

    val filteredList = remember(notificationsList, selectedFilter, searchQuery) {
        notificationsList.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "UNREAD" -> !item.is_read
                "ANNOUNCEMENT" -> item.type == "ANNOUNCEMENT"
                "ACADEMIC" -> item.type == "ACADEMIC"
                "ATTENDANCE" -> item.type == "ATTENDANCE"
                "FINANCE" -> item.type == "FINANCE"
                else -> true
            }

            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.message.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Bildirim Merkezi",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = BrandNavy
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AccentRose
                                ) {
                                    Text(
                                        text = "$unreadCount Okunmamış",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Duyurular, karne, yoklama ve finans uyarıları",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = PrimaryBlue)
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                notificationsList = notificationsList.map { it.copy(is_read = true) }
                            }
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tümünü Oku", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Bildirim veya duyuru ara...", fontSize = 13.5.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Temizle", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Category Filter Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    Triple("ALL", "Tümü (${notificationsList.size})", Icons.Default.AllInbox),
                    Triple("UNREAD", "Okunmamış ($unreadCount)", Icons.Default.MarkEmailUnread),
                    Triple("ANNOUNCEMENT", "Duyurular", Icons.Default.Campaign),
                    Triple("ACADEMIC", "Not & Karne", Icons.Default.WorkspacePremium),
                    Triple("ATTENDANCE", "Yoklama", Icons.Default.FactCheck),
                    Triple("FINANCE", "Finans & Aidat", Icons.Default.AccountBalanceWallet)
                )

                items(filters) { (key, label, icon) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFilter = key
                        },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) null else FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Notifications List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryBlueContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Bildirim Bulunamadı", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandNavy)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedFilter == "UNREAD") "Okunmamış tüm bildirimleri tamamladınız." else "Seçtiğiniz filtreye uygun bildirim kaydı yok.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        RoleContextualAiCard(
                            screenName = "NOTIFICATIONS",
                            state = state
                        )
                    }

                    items(filteredList, key = { it.id }) { item ->
                        NotificationCardItem(
                            item = item,
                            onMarkAsRead = {
                                notificationsList = notificationsList.map {
                                    if (it.id == item.id) it.copy(is_read = true) else it
                                }
                            },
                            onActionClick = {
                                notificationsList = notificationsList.map {
                                    if (it.id == item.id) it.copy(is_read = true) else it
                                }
                                when (item.type) {
                                    "ANNOUNCEMENT" -> {
                                        val sampleAnn = state.announcements.firstOrNull() ?: Announcement(
                                            id = "ann-sample",
                                            title = item.title,
                                            content = item.message
                                        )
                                        onAnnouncementClick(sampleAnn)
                                    }
                                    "ACADEMIC" -> onOpenReportCard()
                                    "ATTENDANCE" -> onOpenAttendance()
                                    "FINANCE" -> onOpenFinance()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    item: NotificationItem,
    onMarkAsRead: () -> Unit,
    onActionClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isUnread = !item.is_read

    val (icon, iconColor, containerBg) = when (item.type) {
        "ANNOUNCEMENT" -> Triple(Icons.Default.Campaign, Color(0xFF2563EB), Color(0xFFEFF6FF))
        "ACADEMIC" -> Triple(Icons.Default.WorkspacePremium, Color(0xFF7C3AED), Color(0xFFF5F3FF))
        "ATTENDANCE" -> Triple(Icons.Default.FactCheck, Color(0xFF059669), Color(0xFFECFDF5))
        "FINANCE" -> Triple(Icons.Default.AccountBalanceWallet, Color(0xFFD97706), Color(0xFFFFFBEB))
        else -> Triple(Icons.Default.Notifications, Color(0xFF4B5563), Color(0xFFF3F4F6))
    }

    val actionButtonText = when (item.type) {
        "ANNOUNCEMENT" -> "Duyuruyu Oku"
        "ACADEMIC" -> "Karneyi Görüntüle"
        "ATTENDANCE" -> "Yoklamayı Aç"
        "FINANCE" -> "Taksitleri İncele"
        else -> "Detay"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onActionClick()
            },
        shape = RoundedCornerShape(16.dp),
        color = if (isUnread) containerBg else Color.White,
        tonalElevation = if (isUnread) 3.dp else 1.dp,
        shadowElevation = if (isUnread) 2.dp else 1.dp,
        border = if (isUnread) BorderStroke(1.5.dp, iconColor.copy(alpha = 0.5f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Icon Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            fontWeight = if (isUnread) FontWeight.Black else FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = BrandNavy,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isUnread) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(iconColor)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.message,
                        fontSize = 12.5.sp,
                        color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Time and Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.created_at,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isUnread) {
                                TextButton(
                                    onClick = onMarkAsRead,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Okundu", fontSize = 12.sp, color = iconColor, fontWeight = FontWeight.SemiBold, softWrap = false)
                                }
                            }

                            Button(
                                onClick = onActionClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = iconColor),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(actionButtonText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, softWrap = false)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
