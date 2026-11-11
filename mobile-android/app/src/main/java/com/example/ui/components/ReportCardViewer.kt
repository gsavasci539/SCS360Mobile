package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.ui.theme.*
import com.example.util.ReportCardPrintHelper
import java.util.*

/**
 * Subject grade item representation for official school report cards.
 */
data class SubjectGradeItem(
    val id: String = UUID.randomUUID().toString(),
    val subjectName: String,
    val subjectCode: String,
    val weeklyHours: Int,
    val exam1: Double,
    val exam2: Double,
    val participation1: Double,
    val participation2: Double,
    val projectScore: Double,
    val teacherName: String,
    val icon: ImageVector = Icons.Default.MenuBook
) {
    val averageScore: Double
        get() = (exam1 + exam2 + participation1 + participation2 + projectScore) / 5.0

    val weightedScore: Double
        get() = averageScore * weeklyHours

    val letterGrade: String
        get() = when {
            averageScore >= 85.0 -> "AA"
            averageScore >= 75.0 -> "BA"
            averageScore >= 70.0 -> "BB"
            averageScore >= 60.0 -> "CB"
            averageScore >= 50.0 -> "CC"
            averageScore >= 40.0 -> "DC"
            else -> "FF"
        }

    val isPassed: Boolean
        get() = averageScore >= 50.0
}

/**
 * Report Card dataset generator helper
 */
object ReportCardSampleData {
    fun getSampleGradesForStudent(student: Student, term: Int): List<SubjectGradeItem> {
        return emptyList()
    }
}

/**
 * Karne Görüntüleyici (Report Card Viewer) Component
 * Renders an official student term report card with subject grades, exam breakdowns,
 * weighted average GPA, certificate eligibility, behavior traits, and PDF printing capability.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReportCardViewer(
    student: Student,
    onDismiss: (() -> Unit)? = null,
    onPrintPdf: ((Student) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedTerm by remember { mutableStateOf(1) } // 1: 1. Dönem, 2: 2. Dönem
    var searchQuery by remember { mutableStateOf("") }
    var expandedSubjectId by remember { mutableStateOf<String?>(null) }
    val viewModeState = rememberResolvedTableMode()
    val useCardMode = isCompactCardMode(viewModeState.value)

    val grades = remember(student.id, selectedTerm) {
        ReportCardSampleData.getSampleGradesForStudent(student, selectedTerm)
    }

    val filteredGrades = remember(grades, searchQuery) {
        if (searchQuery.isBlank()) grades
        else grades.filter {
            it.subjectName.contains(searchQuery, ignoreCase = true) ||
                    it.teacherName.contains(searchQuery, ignoreCase = true) ||
                    it.subjectCode.contains(searchQuery, ignoreCase = true)
        }
    }

    // Weighted GPA Calculation: sum(average * weeklyHours) / sum(weeklyHours)
    val totalWeeklyHours = remember(grades) { grades.sumOf { it.weeklyHours } }
    val totalWeightedScore = remember(grades) { grades.sumOf { it.weightedScore } }
    val weightedGpa = remember(totalWeightedScore, totalWeeklyHours) {
        if (totalWeeklyHours > 0) totalWeightedScore / totalWeeklyHours else 0.0
    }

    val certificateStatus = remember(weightedGpa) {
        when {
            weightedGpa >= 85.0 -> Pair("Takdir Belgesi", SecondaryEmerald)
            weightedGpa >= 70.0 -> Pair("Teşekkür Belgesi", PrimaryBlue)
            weightedGpa >= 50.0 -> Pair("Sınıfı Geçti", AccentAmber)
            else -> Pair("Geliştirilmeli", AccentRose)
        }
    }

    var aiTone by remember { mutableStateOf("PEDAGOGICAL") } // PEDAGOGICAL, ACADEMIC, COACHING
    var isAiGenerating by remember { mutableStateOf(false) }
    var generatedTeacherRemark by remember {
        mutableStateOf(
            "“${student.first_name}, ${selectedTerm}. dönem boyunca derslerdeki aktif tutumu, arkadaşlarına örnek liderliği ve sorumluluk bilinciyle üstün bir başarı sergilemiştir. Özellikle sayısal alanlardaki analitik problem çözme kabiliyeti takdire şayandır.”"
        )
    }
    var copiedToastText by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(PrimaryBlue, AccentIndigo)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "📜 Karne & Dönem Not Görüntüleyici",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = BrandNavy
                            )
                            Text(
                                text = "${student.full_name} • ${student.class_name} (No: ${student.student_number})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (onDismiss != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Term Switcher (1. Dönem / 2. Dönem)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp)
                    ) {
                        listOf(1 to "1. Dönem Karnesi", 2 to "2. Dönem Karnesi").forEach { (termNum, title) ->
                            val isSelected = selectedTerm == termNum
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryBlue else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTerm = termNum
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. High-Impact Weighted GPA & Honor Certificate Hero Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "AĞIRLIKLI DÖNEM ORTALAMASI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = String.format(Locale("tr"), "%.2f", weightedGpa),
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BrandNavy
                                        )
                                        Text(
                                            text = " / 100",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                                        )
                                    }
                                }

                                // Certificate Badge
                                Surface(
                                    color = certificateStatus.second.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        certificateStatus.second
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = certificateStatus.second,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = certificateStatus.first,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.5.sp,
                                            color = certificateStatus.second
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Key Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ReportMetricItem(
                                    label = "Haftalık Ders",
                                    value = "$totalWeeklyHours Saat",
                                    icon = Icons.Default.Schedule,
                                    color = PrimaryBlue
                                )
                                ReportMetricItem(
                                    label = "Ders Sayısı",
                                    value = "${grades.size} Ders",
                                    icon = Icons.Default.LibraryBooks,
                                    color = AccentIndigo
                                )
                                ReportMetricItem(
                                    label = "Özürsüz Devamsızlık",
                                    value = "0.5 Gün",
                                    icon = Icons.Default.EventBusy,
                                    color = SecondaryEmerald
                                )
                                ReportMetricItem(
                                    label = "Özürlü Devamsızlık",
                                    value = "1.5 Gün",
                                    icon = Icons.Default.MedicalServices,
                                    color = AccentAmber
                                )
                            }
                        }
                    }
                }



                // 3. Search & Subject Filter Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📚 Dersler ve Not Dökümü (${filteredGrades.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )

                        // PDF Export Button
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (onPrintPdf != null) {
                                    onPrintPdf(student)
                                } else {
                                    ReportCardPrintHelper.printStudentReportCard(context, student)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PrimaryBlueContainer,
                                contentColor = OnPrimaryBlueContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Karne PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Search field
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Ders veya öğretmen ara...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(18.dp))
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // 4. Subject Grades Sticky Header & List
                stickyHeader {
                    StickyTableHeaderRow(
                        columns = listOf(
                            TableColumnSpec("Ders / Öğretmen", 2.0f),
                            TableColumnSpec("Y1 / Y2 / Prj", 1.5f, TextAlign.Center),
                            TableColumnSpec("Harf / Ort.", 1.1f, TextAlign.End)
                        ),
                        totalCount = filteredGrades.size,
                        viewModeState = viewModeState
                    )
                }

                if (filteredGrades.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Bu öğrenci için henüz karne notu girilmedi.",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                itemsIndexed(filteredGrades, key = { _, it -> it.id }) { index, gradeItem ->
                    if (useCardMode) {
                        SubjectGradeCard(
                            gradeItem = gradeItem,
                            isExpanded = expandedSubjectId == gradeItem.id,
                            onToggleExpand = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                expandedSubjectId = if (expandedSubjectId == gradeItem.id) null else gradeItem.id
                            }
                        )
                    } else {
                        val scoreColor = when {
                            gradeItem.averageScore >= 85.0 -> SecondaryEmerald
                            gradeItem.averageScore >= 70.0 -> PrimaryBlue
                            gradeItem.averageScore >= 50.0 -> AccentAmber
                            else -> AccentRose
                        }
                        CompactTableRow(
                            index = index,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                expandedSubjectId = if (expandedSubjectId == gradeItem.id) null else gradeItem.id
                            }
                        ) {
                            Column(modifier = Modifier.weight(2.0f).padding(end = 6.dp)) {
                                Text(
                                    text = gradeItem.subjectName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BrandNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${gradeItem.teacherName} • ${gradeItem.weeklyHours}s",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${gradeItem.exam1.toInt()} / ${gradeItem.exam2.toInt()} / ${gradeItem.projectScore.toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandNavy,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1.5f)
                            )
                            Row(
                                modifier = Modifier.weight(1.1f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = scoreColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = gradeItem.letterGrade,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = scoreColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format(Locale("tr"), "%.1f", gradeItem.averageScore),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.5.sp,
                                    color = scoreColor
                                )
                            }
                        }
                    }
                }

                // 5. Behavior & Social Development Assessment (Davranış Değerlendirmesi)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = AccentIndigo,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "🌟 Davranış & Sosyal Beceriler",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BrandNavy
                                    )
                                }
                                Text(
                                    text = "MEB Standart",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            listOf(
                                "Ders İçi Katılım & Odaklanma" to "Çok İyi (3/3)",
                                "Okul ve Sınıf Kurallarına Uyum" to "Çok İyi (3/3)",
                                "Arkadaşlık & Takım Çalışması" to "Çok İyi (3/3)",
                                "Ödev ve Sorumluluk Bilinci" to "Çok İyi (3/3)",
                                "Sosyal & Kültürel Etkinlikler" to "İyi (2/3)"
                            ).forEach { (trait, rating) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = trait,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = SecondaryEmerald.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = rating,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SecondaryEmerald,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Teacher Remark & Official Signature Box
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "✍️ Sınıf Rehber Öğretmeni Görüşü",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = BrandNavy
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = generatedTeacherRemark,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Ayşe Kaya", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandNavy)
                                    Text("Sınıf Rehber Öğretmeni", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Ahmet Yılmaz", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandNavy)
                                    Text("Okul Müdürü • Mühürlü", fontSize = 11.sp, color = SecondaryEmerald, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectGradeCard(
    gradeItem: SubjectGradeItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val scoreColor = when {
        gradeItem.averageScore >= 85.0 -> SecondaryEmerald
        gradeItem.averageScore >= 70.0 -> PrimaryBlue
        gradeItem.averageScore >= 50.0 -> AccentAmber
        else -> AccentRose
    }

    CompactResponsiveCard(onClick = onToggleExpand) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(scoreColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = gradeItem.icon,
                        contentDescription = null,
                        tint = scoreColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = gradeItem.subjectName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${gradeItem.teacherName} • ${gradeItem.weeklyHours} Saat/Hafta",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = scoreColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = gradeItem.letterGrade,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.5.sp,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale("tr"), "%.1f", gradeItem.averageScore),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = scoreColor
                    )
                    Text(
                        text = "Ort.",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Detay",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactMetaCell(label = "1. Yazılı", value = "${gradeItem.exam1.toInt()}", modifier = Modifier.weight(1f))
                    CompactMetaCell(label = "2. Yazılı", value = "${gradeItem.exam2.toInt()}", modifier = Modifier.weight(1f))
                    CompactMetaCell(label = "1. İçi", value = "${gradeItem.participation1.toInt()}", modifier = Modifier.weight(1f))
                    CompactMetaCell(label = "2. İçi", value = "${gradeItem.participation2.toInt()}", modifier = Modifier.weight(1f))
                    CompactMetaCell(label = "Proje", value = "${gradeItem.projectScore.toInt()}", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ağırlıklı Katsayı: ${String.format(Locale("tr"), "%.1f", gradeItem.weightedScore)}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )

                    Text(
                        text = if (gradeItem.isPassed) "✓ Başarılı" else "✗ Kaldı",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gradeItem.isPassed) SecondaryEmerald else AccentRose
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreDetailItem(
    title: String,
    score: Double
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = score.toInt().toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = BrandNavy
        )
    }
}

@Composable
private fun ReportMetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandNavy)
        Text(text = label, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
