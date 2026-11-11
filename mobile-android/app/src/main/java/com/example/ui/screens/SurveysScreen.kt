package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SchoolSurvey
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.util.Locale

@Composable
fun SurveysScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit = {},
    onParticipate: (String, Map<String, Any>) -> Unit = { _, _ -> },
    onCreateSurvey: (SchoolSurvey) -> Unit = {}
) {
    val surveys = state.surveys
    var showCreateDialog by remember { mutableStateOf(false) }
    var localSurveys by remember(surveys) { mutableStateOf(surveys) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Poll, contentDescription = null) },
                text = { Text("Yeni Anket Oluştur", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_survey_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NeutralLightBackground)
                .padding(paddingValues),
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
                        "Anonim anketlerde yanıtı kullanıcıya bağlamayan ayrı katılım defteriyle çift gönderim engellenir ve aktör bilgisi görünümden gizlenir.",
                        fontSize = 12.sp,
                        color = OnScsContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            items(localSurveys, key = { it.id }) { survey ->
                val targetCount = 200
                val participationRatio = (survey.total_responses.toFloat() / targetCount).coerceIn(0f, 1f)
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
                                        .background(AccentIndigoContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.HowToVote,
                                        contentDescription = null,
                                        tint = AccentIndigo
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = survey.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Hedef: ${survey.target_audience} • ${survey.start_date} → ${survey.end_date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeutralTextSecondary
                                    )
                                }
                            }
                            Surface(
                                color = AccentAmberContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f/5.0", survey.satisfaction_score),
                                        color = OnAccentAmberContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = NeutralLightSurfaceVariant, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    "Durum: ${if (survey.is_active) "AKTİF (YAYINDA)" else "KAPALI / TASLAK"}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeutralTextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(color = StatusInfoContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    "Anonim Katılım Defteri • Aktör Gizli",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusInfo,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { participationRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${survey.total_responses} Yanıt • ${survey.questions_count} Soru",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeutralTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!survey.is_active) {
                                    OutlinedButton(
                                        onClick = {
                                            localSurveys = localSurveys.map {
                                                if (it.id == survey.id) it.copy(is_active = true) else it
                                            }
                                        }
                                    ) {
                                        Text("Yayımla", fontSize = 11.sp)
                                    }
                                } else {
                                    TextButton(
                                        onClick = {
                                            localSurveys = localSurveys.map {
                                                if (it.id == survey.id) it.copy(is_active = false) else it
                                            }
                                        }
                                    ) {
                                        Text("Anketi Kapat", fontSize = 11.sp, color = StatusError)
                                    }
                                    Button(
                                        onClick = { onParticipate(survey.id, mapOf("score" to 5)) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("participate_survey_${survey.id}")
                                    ) {
                                        Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ankete Katıl")
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
        var audience by remember { mutableStateOf("PARENTS") }
        var publishImmediately by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Yeni Memnuniyet Anketi Oluştur", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Anket Başlığı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PARENTS" to "Veliler", "STUDENTS" to "Öğrenciler", "TEACHERS" to "Öğretmenler").forEach { (key, label) ->
                            FilterChip(
                                selected = audience == key,
                                onClick = { audience = key },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !publishImmediately,
                            onClick = { publishImmediately = false },
                            label = { Text("Taslak Olarak Kaydet") }
                        )
                        FilterChip(
                            selected = publishImmediately,
                            onClick = { publishImmediately = true },
                            label = { Text("Hemen Yayımla") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val newSurvey = SchoolSurvey(
                                id = "srv-${System.currentTimeMillis()}",
                                title = title.trim(),
                                target_audience = audience,
                                questions_count = 8,
                                total_responses = 1,
                                is_active = publishImmediately,
                                satisfaction_score = 5.0
                            )
                            onCreateSurvey(newSurvey)
                            localSurveys = listOf(newSurvey) + localSurveys
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Kaydet")
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
