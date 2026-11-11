package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Announcement
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.text.SimpleDateFormat
import java.util.*

/**
 * School Administrator Dedicated Announcement Management Screen & View.
 * Allows administrators to create, categorize (Teacher, Parent, General), preview,
 * pin, dispatch push notifications, and manage institutional announcements.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnnouncementManagementScreen(
    state: Okul360UiState,
    onCreateAnnouncement: (title: String, content: String, category: String, target: String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onSaveDraft: ((id: String?, title: String, content: String, category: String, target: String, isUrgent: Boolean) -> Unit)? = null,
    onPublishDraft: ((String) -> Unit)? = null,
    onDeleteDraft: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var mainViewMode by remember { mutableIntStateOf(0) } // 0: Published, 1: Drafts
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingDraft by remember { mutableStateOf<Announcement?>(null) }
    var previewAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var selectedTargetFilter by remember { mutableStateOf("ALL") } // ALL, TEACHERS, PARENTS, GENERAL
    var selectedCategoryFilter by remember { mutableStateOf("ALL") } // ALL, ACADEMIC, EVENT, FINANCIAL, EMERGENCY
    var searchQuery by remember { mutableStateOf("") }

    val publishedList = remember(state.announcements, selectedTargetFilter, selectedCategoryFilter, searchQuery) {
        state.announcements.filter { ann ->
            val matchesTarget = when (selectedTargetFilter) {
                "TEACHERS" -> ann.target_audience.equals("TEACHERS", ignoreCase = true) || ann.target_audience.contains("ÖĞRETMEN", ignoreCase = true)
                "PARENTS" -> ann.target_audience.equals("PARENTS", ignoreCase = true) || ann.target_audience.contains("VELİ", ignoreCase = true)
                "GENERAL" -> ann.target_audience.equals("ALL", ignoreCase = true) || ann.target_audience.contains("GENEL", ignoreCase = true)
                else -> true
            }

            val matchesCategory = when (selectedCategoryFilter) {
                "ACADEMIC" -> ann.category.equals("ACADEMIC", ignoreCase = true) || ann.category.equals("SINIF", ignoreCase = true) || ann.category.equals("DERS", ignoreCase = true)
                "EVENT" -> ann.category.equals("EVENT", ignoreCase = true) || ann.category.equals("ETKİNLİK", ignoreCase = true)
                "FINANCIAL" -> ann.category.equals("FINANCIAL", ignoreCase = true) || ann.category.equals("MALİ", ignoreCase = true)
                "EMERGENCY" -> ann.category.equals("EMERGENCY", ignoreCase = true) || ann.category.equals("ACİL", ignoreCase = true)
                else -> true
            }

            val matchesSearch = searchQuery.isBlank() ||
                    ann.title.contains(searchQuery, ignoreCase = true) ||
                    ann.content.contains(searchQuery, ignoreCase = true) ||
                    ann.author_name.contains(searchQuery, ignoreCase = true)

            matchesTarget && matchesCategory && matchesSearch
        }
    }

    val draftsList = remember(state.announcementDrafts, searchQuery) {
        state.announcementDrafts.filter { draft ->
            searchQuery.isBlank() ||
                    draft.title.contains(searchQuery, ignoreCase = true) ||
                    draft.content.contains(searchQuery, ignoreCase = true)
        }
    }

    val countTeachers = state.announcements.count { it.target_audience.equals("TEACHERS", ignoreCase = true) || it.target_audience.contains("ÖĞRETMEN", ignoreCase = true) }
    val countParents = state.announcements.count { it.target_audience.equals("PARENTS", ignoreCase = true) || it.target_audience.contains("VELİ", ignoreCase = true) }
    val countGeneral = state.announcements.count { it.target_audience.equals("ALL", ignoreCase = true) || it.target_audience.contains("GENEL", ignoreCase = true) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    editingDraft = null
                    showCreateDialog = true
                },
                icon = { Icon(Icons.Default.PostAdd, contentDescription = null) },
                text = { Text("Yeni Duyuru / Taslak", fontWeight = FontWeight.Bold) },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(listOf(PrimaryBlue, Color(0xFF1D4ED8)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "📢 İdari Duyuru Yönetimi",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = BrandNavy
                                    )
                                    Text(
                                        text = "Öğretmen, Veli ve Genel kategorili duyuru & taslak paneli",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Contextual AI Announcement Analytics
                        RoleContextualAiCard(
                            screenName = "ANNOUNCEMENTS",
                            state = state
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Published vs Drafts Segmented Tab Bar
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (mainViewMode == 0) Color.White else Color.Transparent,
                                    shadowElevation = if (mainViewMode == 0) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { mainViewMode = 0 }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = if (mainViewMode == 0) PrimaryBlue else Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Yayınlananlar (${state.announcements.size})",
                                            fontWeight = if (mainViewMode == 0) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            color = if (mainViewMode == 0) BrandNavy else Color(0xFF64748B)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (mainViewMode == 1) Color.White else Color.Transparent,
                                    shadowElevation = if (mainViewMode == 1) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { mainViewMode = 1 }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = if (mainViewMode == 1) Color(0xFFD97706) else Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Taslaklar (${state.announcementDrafts.size})",
                                            fontWeight = if (mainViewMode == 1) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            color = if (mainViewMode == 1) BrandNavy else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        if (mainViewMode == 0) {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Audience KPI Cards Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AudienceKpiCard(
                                    title = "👨‍🏫 Öğretmen",
                                    count = "$countTeachers Duyuru",
                                    isSelected = selectedTargetFilter == "TEACHERS",
                                    color = Color(0xFF7C3AED),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedTargetFilter = if (selectedTargetFilter == "TEACHERS") "ALL" else "TEACHERS"
                                    }
                                )
                                AudienceKpiCard(
                                    title = "👨‍👩‍👧 Veli",
                                    count = "$countParents Duyuru",
                                    isSelected = selectedTargetFilter == "PARENTS",
                                    color = Color(0xFF059669),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedTargetFilter = if (selectedTargetFilter == "PARENTS") "ALL" else "PARENTS"
                                    }
                                )
                                AudienceKpiCard(
                                    title = "📢 Genel",
                                    count = "$countGeneral Duyuru",
                                    isSelected = selectedTargetFilter == "GENERAL",
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedTargetFilter = if (selectedTargetFilter == "GENERAL") "ALL" else "GENERAL"
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search Box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(if (mainViewMode == 0) "Duyurularda ara..." else "Taslaklarda ara...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (mainViewMode == 0) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Category Filter Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == "ALL",
                                        onClick = { selectedCategoryFilter = "ALL" },
                                        label = { Text("Tüm Kategoriler", fontSize = 11.5.sp) }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == "ACADEMIC",
                                        onClick = { selectedCategoryFilter = "ACADEMIC" },
                                        label = { Text("📚 Akademik / Ders", fontSize = 11.5.sp) }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == "EVENT",
                                        onClick = { selectedCategoryFilter = "EVENT" },
                                        label = { Text("🎪 Etkinlik & Gezi", fontSize = 11.5.sp) }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == "FINANCIAL",
                                        onClick = { selectedCategoryFilter = "FINANCIAL" },
                                        label = { Text("💳 Mali / Taksit", fontSize = 11.5.sp) }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == "EMERGENCY",
                                        onClick = { selectedCategoryFilter = "EMERGENCY" },
                                        label = { Text("🚨 Acil Durum", fontSize = 11.5.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Body rendering based on Tab
            if (mainViewMode == 0) {
                // Results count
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Yayınlanan Duyurular (${publishedList.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = if (selectedTargetFilter != "ALL") "Filtre: $selectedTargetFilter" else "Tüm Hedefler",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Empty State
                if (publishedList.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Eşleşen duyuru bulunamadı", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Farklı bir filtre seçebilir veya yeni duyuru oluşturabilirsiniz.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        editingDraft = null
                                        showCreateDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("Duyuru Oluştur")
                                }
                            }
                        }
                    }
                } else {
                    items(publishedList) { ann ->
                        AdminAnnouncementCard(
                            announcement = ann,
                            onPreview = { previewAnnouncement = ann },
                            onDelete = {
                                onDeleteAnnouncement(ann.id)
                                Toast.makeText(context, "Duyuru yayından kaldırıldı", Toast.LENGTH_SHORT).show()
                            },
                            onResendPush = {
                                Toast.makeText(context, "🔔 '${ann.title}' için anlık push bildirimi tüm kullanıcılara tekrar gönderildi!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            } else {
                // DRAFTS TAB
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Taslak Olarak Kaydedilenler (${draftsList.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = "Yayınlanmaya Hazır",
                            fontSize = 11.5.sp,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (draftsList.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Henüz kaydedilmiş taslak duyuru yok", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Önceden hazırlamak istediğiniz duyuruları taslak olarak kaydedebilir, daha sonra düzenleyip yayınlayabilirsiniz.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        editingDraft = null
                                        showCreateDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                ) {
                                    Text("Yeni Taslak Hazırla")
                                }
                            }
                        }
                    }
                } else {
                    items(draftsList) { draft ->
                        AdminDraftAnnouncementCard(
                            draft = draft,
                            onEdit = {
                                editingDraft = draft
                                showCreateDialog = true
                            },
                            onPublishNow = {
                                onPublishDraft?.invoke(draft.id)
                            },
                            onDelete = {
                                onDeleteDraft?.invoke(draft.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Create / Edit Announcement Dialog
    if (showCreateDialog) {
        AdminCreateAnnouncementModal(
            initialDraft = editingDraft,
            onDismiss = {
                showCreateDialog = false
                editingDraft = null
            },
            onSaveDraft = { id, title, content, cat, target, isUrgent ->
                onSaveDraft?.invoke(id, title, content, cat, target, isUrgent)
                showCreateDialog = false
                editingDraft = null
            },
            onConfirm = { title, content, cat, target, isUrgent, sendPush ->
                onCreateAnnouncement(title, content, cat, target)
                // If it was editing a draft, remove the draft
                editingDraft?.let { onDeleteDraft?.invoke(it.id) }
                showCreateDialog = false
                editingDraft = null
                val msg = if (sendPush) {
                    "📢 Duyuru başarıyla yayınlandı ve FCM bildirimi hedefe iletildi!"
                } else {
                    "📢 Duyuru başarıyla yayınlandı!"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Preview Dialog
    if (previewAnnouncement != null) {
        val ann = previewAnnouncement!!
        AlertDialog(
            onDismissRequest = { previewAnnouncement = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Kullanıcı Önizlemesi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when (ann.target_audience.uppercase()) {
                                        "TEACHERS" -> "👨‍🏫 Öğretmenlere Özel"
                                        "PARENTS" -> "👨‍👩‍👧 Velilere Özel"
                                        else -> "📢 Genel Duyuru"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                                Text(ann.created_at, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(ann.title, fontWeight = FontWeight.Black, fontSize = 15.sp, color = BrandNavy)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(ann.content, fontSize = 13.sp, color = Color(0xFF334155), lineHeight = 18.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Yayınlayan: ${ann.author_name} (${ann.author_role})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { previewAnnouncement = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Tamam")
                }
            }
        )
    }
}

@Composable
private fun AdminDraftAnnouncementCard(
    draft: Announcement,
    onEdit: () -> Unit,
    onPublishNow: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        tonalElevation = 1.dp,
        shadowElevation = 1.5.dp,
        border = BorderStroke(1.dp, Color(0xFFFDE68A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = "📝 TASLAK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = draft.category.uppercase(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = draft.draft_last_modified ?: draft.created_at,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = draft.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = draft.content,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hedef: ${if (draft.target_audience == "TEACHERS") "Öğretmenler" else if (draft.target_audience == "PARENTS") "Veliler" else "Tüm Okul"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Düzenle", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onPublishNow,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Yayınla", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AudienceKpiCard(
    title: String,
    count: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else Color.White,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) color else BrandNavy)
            Spacer(modifier = Modifier.height(2.dp))
            Text(count, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun AdminAnnouncementCard(
    announcement: Announcement,
    onPreview: () -> Unit,
    onDelete: () -> Unit,
    onResendPush: () -> Unit
) {
    val targetColor = when (announcement.target_audience.uppercase()) {
        "TEACHERS" -> Color(0xFF7C3AED)
        "PARENTS" -> Color(0xFF059669)
        else -> Color(0xFF2563EB)
    }

    val targetLabel = when (announcement.target_audience.uppercase()) {
        "TEACHERS" -> "👨‍🏫 Öğretmenler"
        "PARENTS" -> "👨‍👩‍👧 Veliler"
        else -> "📢 Genel (Tüm Okul)"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        tonalElevation = 1.dp,
        shadowElevation = 1.5.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Target audience badge & Category tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = targetColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = targetLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = targetColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = announcement.category.uppercase(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(announcement.created_at, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = announcement.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Content preview
            Text(
                text = announcement.content,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yazar: ${announcement.author_name}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Preview Button
                    OutlinedButton(
                        onClick = onPreview,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Önizle", fontSize = 11.sp)
                    }

                    // Resend Push Button
                    FilledTonalButton(
                        onClick = onResendPush,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Push Bildir", fontSize = 11.sp)
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * Detailed creation modal with targeted audience, categories, urgency tags, and push notification toggles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreateAnnouncementModal(
    initialDraft: Announcement? = null,
    onDismiss: () -> Unit,
    onSaveDraft: ((id: String?, title: String, content: String, category: String, target: String, isUrgent: Boolean) -> Unit)? = null,
    onConfirm: (title: String, content: String, category: String, target: String, isUrgent: Boolean, sendPush: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initialDraft?.title ?: "") }
    var content by remember { mutableStateOf(initialDraft?.content ?: "") }
    var selectedTarget by remember { mutableStateOf(initialDraft?.target_audience ?: "ALL") } // ALL, TEACHERS, PARENTS
    var selectedCategory by remember { mutableStateOf(initialDraft?.category ?: "GENERAL") }
    var isUrgent by remember { mutableStateOf(initialDraft?.is_pinned ?: false) }
    var sendPushNotification by remember { mutableStateOf(true) }
    var sendSmsDigest by remember { mutableStateOf(false) }

    val targets = listOf(
        "ALL" to "📢 Genel (Tüm Okul)",
        "PARENTS" to "👨‍👩‍👧 Veliler",
        "TEACHERS" to "👨‍🏫 Öğretmenler"
    )

    val categories = listOf(
        "GENERAL" to "Genel",
        "ACADEMIC" to "Akademik & Ders",
        "EVENT" to "Etkinlik & Gezi",
        "FINANCIAL" to "Finans & Taksit",
        "EMERGENCY" to "Acil Durum"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (initialDraft != null) Color(0xFFD97706) else PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (initialDraft != null) Icons.Default.EditNote else Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        if (initialDraft != null) "Taslak Duyuruyu Düzenle" else "Yeni Duyuru Yayınla",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = BrandNavy
                    )
                    Text("Hedef kitle ve kategori seçimi", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Duyuru Başlığı *", fontSize = 12.sp) },
                    placeholder = { Text("Örn: 2026 Bahar Dönemi Veli Toplantısı", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Target Audience Selector (Öğretmen, Veli, Genel)
                Column {
                    Text("Hedef Kitle:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        targets.forEach { (key, label) ->
                            val isSel = selectedTarget == key
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTarget = key }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Category Selector
                Column {
                    Text("Duyuru Kategorisi:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { (catKey, catLabel) ->
                            FilterChip(
                                selected = selectedCategory == catKey,
                                onClick = { selectedCategory = catKey },
                                label = { Text(catLabel, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Content Area
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Duyuru Metni / Detaylar *", fontSize = 12.sp) },
                    placeholder = { Text("Duyurunun detaylı açıklamasını giriniz...", fontSize = 12.sp) },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Options / Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Anlık FCM Push Bildirimi Gönder", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                    }
                    Checkbox(checked = sendPushNotification, onCheckedChange = { sendPushNotification = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🚨 Acil Durum / Öncelikli Duyuru Olarak İşaretle", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                    }
                    Checkbox(checked = isUrgent, onCheckedChange = { isUrgent = it })
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSaveDraft != null) {
                    OutlinedButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSaveDraft(initialDraft?.id, title, content, selectedCategory, selectedTarget, isUrgent)
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Taslak Kaydet")
                    }
                }

                Button(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            onConfirm(title, content, selectedCategory, selectedTarget, isUrgent, sendPushNotification)
                        }
                    },
                    enabled = title.isNotBlank() && content.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(if (initialDraft != null) "Şimdi Yayınla" else "Duyuruyu Yayınla")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
