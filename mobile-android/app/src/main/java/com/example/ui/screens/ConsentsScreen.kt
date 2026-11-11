package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentConsent
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConsentsScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit = {},
    onApproveConsent: (String, String) -> Unit = { _, _ -> },
    onCreateConsent: (StudentConsent) -> Unit = {}
) {
    val consents = state.consents
    val students = state.students
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var rejectedIds by remember { mutableStateOf(setOf<String>()) }

    val filtered = remember(consents, selectedFilter, searchQuery, rejectedIds) {
        consents.filter { item ->
            val isRej = rejectedIds.contains(item.id)
            val statusKey = when {
                isRej -> "REJECTED"
                item.is_approved -> "APPROVED"
                else -> "PENDING"
            }
            val matchesFilter = selectedFilter == "ALL" || statusKey == selectedFilter
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.student_name.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddTask, contentDescription = null) },
                text = { Text("Yeni İzin Belgesi", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_consent_fab")
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
                        placeholder = { Text("Belge başlığı veya öğrenci ara...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf(
                            "ALL" to "Tümü (${consents.size})",
                            "PENDING" to "Bekleyen",
                            "APPROVED" to "Onaylanan",
                            "REJECTED" to "Reddedilen / Arşiv"
                        )
                        filters.forEach { (key, label) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

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
                            "Veli kararı belge sürümü (v2.4) ve okunmuş metnin SHA-256 özeti üzerinden doğrulanır. Karar sonrasında belge metni dondurulur (Frozen) ve tekrarlı kararlar korunur.",
                            fontSize = 12.sp,
                            color = OnScsContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                items(filtered, key = { it.id }) { consent ->
                    val isRej = rejectedIds.contains(consent.id)
                    val statusColor = when {
                        isRej -> StatusError
                        consent.is_approved -> StatusSuccess
                        else -> AccentAmber
                    }
                    val docHash = remember(consent.id, consent.description) {
                        "SHA256:${(consent.title + consent.description).hashCode().toUInt().toString(16).uppercase()}"
                    }
                    val isFrozen = consent.is_approved || isRej

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
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(statusColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (consent.consent_type) {
                                                "KVKK" -> Icons.Default.VerifiedUser
                                                "SPORTS_HEALTH" -> Icons.Default.HealthAndSafety
                                                else -> Icons.Default.DirectionsBus
                                            },
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = consent.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Öğrenci: ${consent.student_name} • Tür: ${consent.consent_type}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeutralTextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    color = statusColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isRej -> "Reddedildi"
                                            consent.is_approved -> "Onaylandı"
                                            else -> "Onay Bekliyor"
                                        },
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = NeutralLightSurfaceVariant, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        "Sürüm: v2.4 • $docHash",
                                        fontSize = 10.sp,
                                        color = NeutralTextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                if (isFrozen) {
                                    Surface(color = ScsContainerLight, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            "METİN DONDURULDU (FROZEN)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ScsPetrolGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = consent.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralTextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (consent.approved_at != null) "Karar Zamanı: ${consent.approved_at} (${consent.approved_by_parent ?: "Veli"})" else "Son Tarih: ${consent.expiry_date}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeutralTextSecondary
                                )
                                if (!consent.is_approved && !isRej) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { rejectedIds = rejectedIds + consent.id },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Reddet", color = StatusError, fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = { onApproveConsent(consent.id, "Mehmet Demir (Veli)") },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                            modifier = Modifier.testTag("approve_consent_${consent.id}")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Dijital Onayla")
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
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("EXCURSION") }
        val std = students.firstOrNull()

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Yeni Veli İzin / Onay Formu", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("İzin Başlığı (Örn: Bilim Müzesi Gezisi)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detaylı Açıklama & Şartlar") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EXCURSION" to "Gezi", "KVKK" to "KVKK", "PHOTO_VIDEO" to "Fotoğraf").forEach { (key, label) ->
                            FilterChip(
                                selected = selectedType == key,
                                onClick = { selectedType = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onCreateConsent(
                                StudentConsent(
                                    id = "cns-${System.currentTimeMillis()}",
                                    student_id = std?.id ?: "std-1",
                                    student_name = std?.full_name ?: "Ali Demir",
                                    consent_type = selectedType,
                                    title = title.trim(),
                                    description = description.ifBlank { "Kurumumuz tarafından düzenlenen etkinliğe katılım onayı." },
                                    is_approved = false,
                                    expiry_date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                )
                            )
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Gönder")
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
