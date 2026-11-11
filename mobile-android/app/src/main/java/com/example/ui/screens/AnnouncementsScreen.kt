package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Announcement
import com.example.data.model.OutboxNotificationLog
import com.example.ui.components.AnnouncementEmptyState
import com.example.ui.components.Okul360CircularLoadingIndicator
import com.example.ui.components.PullRefreshWrapper
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@Composable
fun AnnouncementsScreen(
    state: Okul360UiState,
    onOpenCreateAnnouncement: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    onAnnouncementClick: ((Announcement) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Duyurular, 1: Outbox Bildirim Kuyruğu
    var isFabExpanded by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") } // ALL, OKUL, SINIF, GENEL
    var searchQuery by remember { mutableStateOf("") }

    val filteredAnnouncements = remember(state.announcements, selectedCategoryFilter, searchQuery) {
        state.announcements.filter { ann ->
            val matchesCategory = when (selectedCategoryFilter) {
                "ALL" -> true
                "OKUL" -> ann.category.equals("OKUL", ignoreCase = true) ||
                        ann.category.equals("BRANCH", ignoreCase = true) ||
                        ann.category.equals("INSTITUTION", ignoreCase = true) ||
                        ann.target_audience.equals("BRANCH", ignoreCase = true)
                "SINIF" -> ann.category.equals("SINIF", ignoreCase = true) ||
                        ann.category.equals("CLASS", ignoreCase = true) ||
                        ann.category.equals("ACADEMIC", ignoreCase = true) ||
                        ann.category.equals("DERS", ignoreCase = true)
                "GENEL" -> ann.category.equals("GENEL", ignoreCase = true) ||
                        ann.category.equals("GENERAL", ignoreCase = true) ||
                        ann.category.equals("EVENT", ignoreCase = true) ||
                        ann.target_audience.equals("ALL", ignoreCase = true)
                else -> true
            }

            val matchesSearch = searchQuery.isBlank() ||
                    ann.title.contains(searchQuery, ignoreCase = true) ||
                    ann.content.contains(searchQuery, ignoreCase = true) ||
                    ann.author_name.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }
    }

    // Category count calculators
    val countAll = state.announcements.size
    val countOkul = state.announcements.count {
        it.category.equals("OKUL", ignoreCase = true) ||
                it.category.equals("BRANCH", ignoreCase = true) ||
                it.category.equals("INSTITUTION", ignoreCase = true) ||
                it.target_audience.equals("BRANCH", ignoreCase = true)
    }
    val countSinif = state.announcements.count {
        it.category.equals("SINIF", ignoreCase = true) ||
                it.category.equals("CLASS", ignoreCase = true) ||
                it.category.equals("ACADEMIC", ignoreCase = true) ||
                it.category.equals("DERS", ignoreCase = true)
    }
    val countGenel = state.announcements.count {
        it.category.equals("GENEL", ignoreCase = true) ||
                it.category.equals("GENERAL", ignoreCase = true) ||
                it.category.equals("EVENT", ignoreCase = true) ||
                it.target_audience.equals("ALL", ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    expanded = isFabExpanded,
                    onClick = {
                        if (!isFabExpanded) {
                            isFabExpanded = true
                        } else {
                            onOpenCreateAnnouncement()
                            isFabExpanded = false
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (isFabExpanded) Icons.Default.AddComment else Icons.Default.Add,
                            contentDescription = "Duyuru Yayınla"
                        )
                    },
                    text = { Text("Duyuru Yayınla", fontWeight = FontWeight.Bold) },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Kurum Duyuruları (${state.announcements.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Outbox Worker (${state.outboxLogs.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                // Category Filter Bar & Search Box
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        // Search Box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Duyurularda ara...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Filter Chips
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filterOptions = listOf(
                                Triple("ALL", "Tümü ($countAll)", Icons.Default.AllInclusive),
                                Triple("OKUL", "Okul ($countOkul)", Icons.Default.School),
                                Triple("SINIF", "Sınıf ($countSinif)", Icons.Default.Class),
                                Triple("GENEL", "Genel ($countGenel)", Icons.Default.Public)
                            )

                            items(filterOptions) { (filterKey, label, icon) ->
                                val isSelected = selectedCategoryFilter == filterKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategoryFilter = filterKey },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
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
                } else if (filteredAnnouncements.isEmpty()) {
                    val isFiltering = searchQuery.isNotBlank() || selectedCategoryFilter != "ALL"
                    AnnouncementEmptyState(
                        isSearchingOrFiltering = isFiltering,
                        onResetFilters = {
                            searchQuery = ""
                            selectedCategoryFilter = "ALL"
                        },
                        onCreateAnnouncement = onOpenCreateAnnouncement
                    )
                } else {
                    PullRefreshWrapper(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { onRefresh?.invoke() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                RoleContextualAiCard(
                                    screenName = "ANNOUNCEMENTS",
                                    state = state,
                                    onQuickActionClick = { actionType ->
                                        if (actionType == "DRAFT_ANNOUNCEMENT_AI") {
                                            onOpenCreateAnnouncement()
                                        }
                                    }
                                )
                            }

                            items(filteredAnnouncements) { announcement ->
                                FullAnnouncementCard(
                                    announcement = announcement,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAnnouncementClick?.invoke(announcement)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryBlueContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SCS360 Outbox Worker: Yoklama, ödeme ve duyurular otomatik olarak asenkron SMS ve Push bildirim servisine iletilir.",
                                    fontSize = 12.sp,
                                    color = OnPrimaryBlueContainer
                                )
                            }
                        }
                    }

                    items(state.outboxLogs) { log ->
                        OutboxLogCard(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun FullAnnouncementCard(
    announcement: Announcement,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val categoryLabel = when (announcement.category.uppercase()) {
        "OKUL", "BRANCH", "INSTITUTION" -> "Okul"
        "SINIF", "CLASS", "ACADEMIC", "DERS" -> "Sınıf"
        "GENEL", "GENERAL", "EVENT" -> "Genel"
        "EMERGENCY", "ACIL" -> "Acil Durum"
        "FINANCIAL", "MALI" -> "Mali Bilgi"
        else -> announcement.category
    }

    val categoryBadgeColor = when (announcement.category.uppercase()) {
        "EMERGENCY", "ACIL" -> AccentRose
        "SINIF", "ACADEMIC", "CLASS", "DERS" -> PrimaryBlue
        "OKUL", "BRANCH", "INSTITUTION" -> SecondaryEmerald
        "FINANCIAL", "MALI" -> AccentAmber
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    label = categoryLabel,
                    color = categoryBadgeColor
                )

                if (announcement.is_pinned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Sabitlendi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentAmber)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${announcement.author_name} (${announcement.author_role})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = announcement.created_at,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun OutboxLogCard(
    log: OutboxNotificationLog,
    modifier: Modifier = Modifier
) {
    val channelColor = when (log.channel) {
        "PUSH" -> SecondaryEmerald
        "SMS" -> PrimaryBlue
        else -> AccentAmber
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(channelColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = log.channel,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    color = channelColor
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.subject,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                )
                Text(
                    text = "Alıcı: ${log.recipient} • Saat: ${log.sent_at}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            StatusBadge(
                label = if (log.status == "DELIVERED") "İletildi" else "Kuyrukta",
                color = if (log.status == "DELIVERED") SecondaryEmerald else AccentAmber
            )
        }
    }
}
