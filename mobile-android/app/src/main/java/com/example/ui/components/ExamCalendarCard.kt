package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ExamItem
import com.example.ui.theme.*

@Composable
fun ExamCalendarCard(
    exams: List<ExamItem>,
    onViewFullSchedule: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, YAZILI, DENEME, PROJE_ODEV
    var selectedExamForDetail by remember { mutableStateOf<ExamItem?>(null) }
    var isExpanded by remember { mutableStateOf(false) }

    val filteredExams = remember(exams, selectedFilter) {
        val list = when (selectedFilter) {
            "ALL" -> exams
            "YAZILI" -> exams.filter { it.exam_type.equals("YAZILI", ignoreCase = true) }
            "DENEME" -> exams.filter { it.exam_type.equals("DENEME", ignoreCase = true) }
            "PROJE_ODEV" -> exams.filter {
                it.exam_type.equals("PROJE", ignoreCase = true) ||
                        it.exam_type.equals("ODEV", ignoreCase = true) ||
                        it.exam_type.equals("QUIZ", ignoreCase = true)
            }
            else -> exams
        }
        // Sort chronologically by date
        list.sortedBy { it.date }
    }

    val displayList = if (isExpanded) filteredExams else filteredExams.take(3)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Sınav & Teslim Takvimi",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BrandNavy
                            )
                        )
                        Text(
                            text = "Yaklaşan sınav ve ödev teslim tarihleri",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentRose.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${filteredExams.size} Yaklaşan",
                        color = AccentRose,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = listOf(
                    "ALL" to "Tümü",
                    "YAZILI" to "Yazılılar",
                    "DENEME" to "Denemeler",
                    "PROJE_ODEV" to "Ödev / Proje"
                )

                filters.forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chronological Exam Items List
            if (filteredExams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bu kategoride yaklaşan sınav bulunmuyor.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayList.forEach { exam ->
                        ExamTimelineRowItem(
                            exam = exam,
                            onClick = { selectedExamForDetail = exam }
                        )
                    }
                }
            }

            if (filteredExams.size > 3) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Text(
                            text = if (isExpanded) "Daha Az Göster" else "Tümünü Göster (${filteredExams.size} Sınav)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PrimaryBlue
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Detail Modal for Exam Topics
    selectedExamForDetail?.let { exam ->
        ExamDetailDialog(
            exam = exam,
            onDismiss = { selectedExamForDetail = null }
        )
    }
}

@Composable
fun ExamTimelineRowItem(
    exam: ExamItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countdownText = remember(exam.date) {
        when {
            exam.date.contains("08-25") -> "4 Gün Kaldı"
            exam.date.contains("08-28") -> "7 Gün Kaldı"
            exam.date.contains("09-02") -> "12 Gün Kaldı"
            exam.date.contains("09-08") -> "18 Gün Kaldı"
            exam.date.contains("09-12") -> "22 Gün Kaldı"
            else -> "Yakında"
        }
    }

    val typeColor = Color(exam.typeColorHex)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Date Pill
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(typeColor.copy(alpha = 0.12f))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val dateParts = exam.date.split("-")
                    val day = dateParts.getOrNull(2) ?: "25"
                    val month = when (dateParts.getOrNull(1)) {
                        "08" -> "AĞU"
                        "09" -> "EYL"
                        "10" -> "EKİ"
                        "11" -> "KAS"
                        else -> "AY"
                    }
                    Text(
                        text = day,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = typeColor
                    )
                    Text(
                        text = month,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = typeColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle: Subject & Title & Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = exam.course_name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandNavy
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = exam.typeDisplayName,
                            color = typeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = exam.title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = exam.time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text(text = "•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = exam.classroom, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right: Countdown Badge & Arrow
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimaryBlue.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = countdownText,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Detay",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ExamDetailDialog(
    exam: ExamItem,
    onDismiss: () -> Unit
) {
    val typeColor = Color(exam.typeColorHex)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(typeColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = exam.typeDisplayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = typeColor
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = exam.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = BrandNavy
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Info Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Tarih & Saat:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${exam.date} • ${exam.time}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Sınıf & Salon:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${exam.class_name} • ${exam.classroom}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Sorumlu Öğretmen:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = exam.teacher_name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Süre & Karne Ağırlığı:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${exam.duration_minutes} Dk • %${exam.weight_percentage} Etki", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Sınav / Ödev Kapsamı (Konular):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (exam.topics.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        exam.topics.forEach { topic ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SecondaryEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = topic,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Tüm dönem başı ünitelerini kapsamaktadır.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kapat", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
