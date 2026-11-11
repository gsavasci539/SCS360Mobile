package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClubEventRecord
import com.example.data.model.SchoolClub
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@Composable
fun ClubsScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit = {},
    onEnrollClub: (String) -> Unit = {},
    onCreateClub: (SchoolClub) -> Unit = {}
) {
    val clubs = state.schoolClubs
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    var clubEvents by remember {
        mutableStateOf(
            listOf(
                ClubEventRecord(
                    id = "cevt-1",
                    club_id = "clb-1",
                    club_name = "Robotik & Yapay Zeka Kulübü",
                    title = "TEKNOFEST Otonom Araç Prototip Atölyesi",
                    event_date = "2026-11-12",
                    location = "STEM Laboratuvarı 2",
                    participant_count = 18,
                    attendance_completed = true,
                    certificate_ready = true
                ),
                ClubEventRecord(
                    id = "cevt-2",
                    club_id = "clb-2",
                    club_name = "Münazara & Hitabet Kulübü",
                    title = "Kampüsler Arası Türkçe Münazara Turnuvası",
                    event_date = "2026-11-18",
                    location = "Konferans Salonu",
                    participant_count = 24,
                    attendance_completed = true,
                    certificate_ready = true
                )
            )
        )
    }

    val filtered = remember(clubs, searchQuery) {
        clubs.filter {
            searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.teacher_supervisor.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                text = { Text("Yeni Kulüp Aç", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_club_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NeutralLightBackground)
                .padding(paddingValues)
        ) {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Kulüp adı, kategori veya danışman öğretmen ara...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Öğrenci Kulüpleri (${filtered.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Etkinlikler & Sertifika PDF (${clubEvents.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { club ->
                            val fillRatio = (club.enrolled_count.toFloat() / club.capacity.coerceAtLeast(1)).coerceIn(0f, 1f)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.EmojiEvents,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = club.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Danışman: ${club.teacher_supervisor} • ${club.schedule_day}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NeutralTextSecondary
                                                )
                                            }
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${club.enrolled_count}/${club.capacity} Kontenjan",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = club.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeutralTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { fillRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (fillRatio >= 0.9f) AccentAmber else StatusSuccess
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Konum: ${club.room_number}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeutralTextSecondary
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    Toast.makeText(
                                                        context,
                                                        "Yetkili İndirme: '${club.name}' Türkçe karakter destekli Katılım Sertifikası PDF hazırlandı.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Sertifika PDF", fontSize = 12.sp)
                                            }
                                            Button(
                                                onClick = { onEnrollClub(club.id) },
                                                enabled = club.enrolled_count < club.capacity,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("enroll_club_${club.id}")
                                            ) {
                                                Icon(Icons.Default.PersonAddAlt1, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(if (club.enrolled_count < club.capacity) "Öğrenci Kaydet" else "Kontenjan Dolu")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ScsContainerLight),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Kulüp etkinlikleri ve katılım yoklaması tamamlanan öğrenciler için yetkili uçtan Türkçe karakter (ğ, ü, ş, ı, ö, ç, İ) destekli özel sertifika PDF'i indirilebilir.",
                                    fontSize = 12.sp,
                                    color = OnScsContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        items(clubEvents, key = { it.id }) { evt ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(evt.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${evt.club_name} • ${evt.event_date} • ${evt.location}", fontSize = 12.sp, color = NeutralTextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Katılımcı: ${evt.participant_count} Öğrenci • Yoklama: Tamamlandı", fontSize = 12.sp, color = StatusSuccess, fontWeight = FontWeight.SemiBold)
                                        Button(
                                            onClick = {
                                                Toast.makeText(
                                                    context,
                                                    "${evt.title} - Türkçe Karakter Destekli Özel Sertifika PDF indirildi.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Sertifika PDF İndir", fontSize = 12.sp)
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

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var instructor by remember { mutableStateOf("") }
        var schedule by remember { mutableStateOf("Çarşamba 15:30 - 17:00") }
        var quota by remember { mutableStateOf("25") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Yeni Öğrenci Kulübü Aç", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Kulüp Adı (Örn: Robotik & Kodlama)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = instructor,
                        onValueChange = { instructor = it },
                        label = { Text("Danışman Öğretmen") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = schedule,
                        onValueChange = { schedule = it },
                        label = { Text("Toplantı / Etkinlik Günü ve Saati") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quota,
                        onValueChange = { quota = it },
                        label = { Text("Kontenjan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Kulüp Açıklaması") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreateClub(
                                SchoolClub(
                                    id = "clb-${System.currentTimeMillis()}",
                                    name = name.trim(),
                                    teacher_supervisor = instructor.ifBlank { "Burak Aydın" },
                                    schedule_day = schedule.trim(),
                                    capacity = quota.toIntOrNull() ?: 25,
                                    enrolled_count = 1,
                                    description = description.ifBlank { "Uygulamalı proje ve atölye çalışmaları." }
                                )
                            )
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Oluştur")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
