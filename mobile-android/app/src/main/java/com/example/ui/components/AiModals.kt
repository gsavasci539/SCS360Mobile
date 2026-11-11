package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*

/**
 * 1. AI Rapor Detay Modalı
 */
@Composable
fun AiReportDetailModal(
    report: AiAnalysisReport,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1E1B4B),
                                    Color(0xFF3730A3)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "🤖 ${report.model_name}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = report.title,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = report.subtitle,
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                // Body
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Risk & Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (report.risk_level.contains("Düşük") || report.risk_level.contains("Sağlıklı")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Risk Durumu", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = report.risk_level,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (report.risk_level.contains("Düşük") || report.risk_level.contains("Sağlıklı")) Color(0xFF15803D) else Color(0xFFB91C1C)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = PrimaryBlueContainer.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Hedef Rol", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = report.target_role,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = BrandNavy
                                )
                            }
                        }
                    }

                    // Key Metrics
                    if (report.metrics.isNotEmpty()) {
                        Text(
                            text = "📊 Temel Metrikler & KPI'lar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandNavy
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            report.metrics.forEach { (k, v) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = k, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        Text(text = v, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    // AI Executive Summary
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Yapay Zeka Yönetici Özeti",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = BrandNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = report.summary,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    // Key Findings
                    if (report.key_findings.isNotEmpty()) {
                        Text(
                            text = "🔍 Önemli Tespit ve Bulgular",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandNavy
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            report.key_findings.forEach { finding ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = finding,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // Strategic Recommendations
                    if (report.recommendations.isNotEmpty()) {
                        Text(
                            text = "💡 Stratejik Aksiyon & Öneriler",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandNavy
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            report.recommendations.forEach { rec ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PrimaryBlue.copy(alpha = 0.05f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = rec,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val fullText = "${report.title}\n\n${report.summary}\n\nBulgular:\n" + report.key_findings.joinToString("\n• ") + "\n\nÖneriler:\n" + report.recommendations.joinToString("\n• ")
                                clipboardManager.setText(AnnotatedString(fullText))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Metni Kopyala", fontSize = 13.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Tamam", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. AI Öğretmen: Akıllı Soru & Ödev Föyü Üretici Modalı
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHomeworkGeneratorModal(
    onDismiss: () -> Unit,
    onGenerate: (subject: String, topic: String, gradeLevel: String, count: Int, difficulty: String) -> Unit,
    activeHomework: AiGeneratedHomework?
) {
    var subject by remember { mutableStateOf("Matematik") }
    var topic by remember { mutableStateOf("Birinci Dereceden Denklemler") }
    var gradeLevel by remember { mutableStateOf("8. Sınıf") }
    var questionCount by remember { mutableStateOf(3) }
    var difficulty by remember { mutableStateOf("Orta") }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF5B21B6),
                                    Color(0xFF7C3AED)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFDE047))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Akıllı Soru & Föy Üretici",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                            }
                        }
                        Text(
                            text = "Müfredata uygun yeni nesil sorular, çözüm anahtarı ve rubrik oluşturun.",
                            fontSize = 12.sp,
                            color = Color(0xFFE9D5FF)
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Subject Selection
                    Text(text = "Ders Seçimi", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandNavy)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Matematik", "Fen Bilimleri", "Türkçe", "İngilizce").forEach { sub ->
                            val isSel = subject == sub
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    subject = sub
                                    if (sub == "Matematik") topic = "Birinci Dereceden Denklemler"
                                    if (sub == "Fen Bilimleri") topic = "DNA, Genetik Kod ve Hücre"
                                    if (sub == "Türkçe") topic = "Fiilimsiler ve Paragraf"
                                    if (sub == "İngilizce") topic = "Friendship & Teen Life"
                                },
                                label = { Text(sub, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Topic Input
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Konu / Kazanım Başlığı") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Grade Level & Difficulty
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = gradeLevel,
                            onValueChange = { gradeLevel = it },
                            label = { Text("Sınıf Düzeyi") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = difficulty,
                            onValueChange = { difficulty = it },
                            label = { Text("Zorluk") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Generate Button
                    Button(
                        onClick = {
                            onGenerate(subject, topic, gradeLevel, questionCount, difficulty)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✨ Yapay Zeka ile Soruları & Föyü Üret", fontWeight = FontWeight.Bold)
                    }

                    // Active Generated Homework Preview
                    if (activeHomework != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "📑 Üretilen Çalışma Föyü Önizlemesi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandNavy
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFAF5FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8B4FE))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = activeHomework.title,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color(0xFF581C87)
                                )
                                Text(
                                    text = "Tahmini Süre: ${activeHomework.estimated_duration_minutes} dk • Yönerge: ${activeHomework.instructions_for_student}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                activeHomework.questions.forEach { q ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "Soru ${q.question_number} (${q.difficulty}):",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF7C3AED)
                                            )
                                            Text(
                                                text = q.question_text,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = BrandNavy
                                            )
                                            if (!q.options.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                q.options.forEach { opt ->
                                                    Text(text = opt, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFDCFCE7)
                                            ) {
                                                Text(
                                                    text = "Doğru Cevap & Çözüm: ${q.correct_answer ?: ""} (${q.explanation})",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF166534),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
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
}

/**
 * 3. AI Öğretmen: Pedagojik Günlük Karne Metni Üretici Modalı
 */
@Composable
fun AiNarrativeGeneratorModal(
    onDismiss: () -> Unit,
    onGenerate: (studentName: String, meal: String, sleep: String, mood: String, activities: String, customNote: String?) -> Unit,
    activeNarrative: AiDailyNarrative?,
    onApplyToDailyReport: ((String) -> Unit)? = null
) {
    var studentName by remember { mutableStateOf("Ali Demir") }
    var meal by remember { mutableStateOf("Tam Bitirdi (Sebze ve Çorba)") }
    var sleep by remember { mutableStateOf("1.5 Saat Rahat Uyudu") }
    var mood by remember { mutableStateOf("Neşeli & Paylaşımcı") }
    var activities by remember { mutableStateOf("Parmak Boyası ve Blok İnşası") }
    var customNote by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFC2410C),
                                    Color(0xFFEA580C)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📝 AI Pedagojik Karne Metni Üretici",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Velilere hitap eden sıcak, profesyonel bülten notu üretin.",
                                fontSize = 12.sp,
                                color = Color(0xFFFFEDD5)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                        }
                    }
                }

                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Öğrenci Adı") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = meal,
                        onValueChange = { meal = it },
                        label = { Text("Yemek Durumu") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = mood,
                        onValueChange = { mood = it },
                        label = { Text("Duygu / Sosyal Durum") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = activities,
                        onValueChange = { activities = it },
                        label = { Text("Günün Etkinlikleri") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            onGenerate(studentName, meal, sleep, mood, activities, customNote.ifBlank { null })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✨ Pedagojik Günlük Notu Üret", fontWeight = FontWeight.Bold)
                    }

                    if (activeNarrative != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🌟 Üretilen Karne Metni:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF9A3412)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFFEDD5)
                                    ) {
                                        Text(
                                            text = activeNarrative.tone,
                                            fontSize = 10.sp,
                                            color = Color(0xFFC2410C),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = activeNarrative.narrative_text,
                                    fontSize = 13.sp,
                                    color = BrandNavy,
                                    lineHeight = 19.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(activeNarrative.narrative_text))
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kopyala", fontSize = 12.sp)
                                    }

                                    if (onApplyToDailyReport != null) {
                                        Button(
                                            onClick = {
                                                onApplyToDailyReport(activeNarrative.narrative_text)
                                                onDismiss()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                                        ) {
                                            Text("Rapora Ekle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

/**
 * 4. AI Veli: Haftalık Pedagojik Gelişim Karnesi Modalı
 */
@Composable
fun AiParentDigestModal(
    digest: AiParentDigest,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0E7490),
                                    Color(0xFF06B6D4)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "🌟 HAFTALIK AI PEDAGOJİK REHBER",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${digest.student_name} - Haftalık Gelişim Karnesi",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Tarih Aralığı: ${digest.week_range}",
                            fontSize = 12.sp,
                            color = Color(0xFFCFFAFE)
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Wellness Score Gauge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFECFEFF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5F3FC))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0891B2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "%${digest.overall_wellness_score}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Genel Uyum & Esenlik Skoru",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF164E63)
                                )
                                Text(
                                    text = "Sosyal katılım, beslenme düzeni ve sınıf içi adaptasyon yüksek seviyede.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Positive Highlights
                    Text(text = "✨ Haftanın Parlayan Anları", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        digest.positive_highlights.forEach { h ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = h, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
                            }
                        }
                    }

                    // Recommended Home Activities
                    Text(text = "🏡 Evde Uygulanabilecek Pedagojik İpuçları", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        digest.recommended_home_activities.forEach { act ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = act,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2))
                    ) {
                        Text("Kapat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
