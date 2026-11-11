package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.data.model.StudentFile
import com.example.ui.components.ReportCardViewer
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

enum class StudentProfileTab(val title: String, val icon: ImageVector) {
    ACADEMIC("Akademik", Icons.Default.School),
    ATTENDANCE("Yoklama", Icons.Default.EventAvailable),
    PARENT_INFO("Veli & İletişim", Icons.Default.FamilyRestroom),
    HEALTH("Sağlık & Diyet", Icons.Default.MedicalServices),
    FINANCE("Finansal", Icons.Default.CreditCard),
    DOCUMENTS("Evraklar", Icons.Default.FolderShared)
}

data class SubjectGradeItem(
    val subjectName: String,
    val subjectCode: String,
    val teacherName: String,
    val exam1: Int,
    val exam2: Int,
    val oralScore: Int,
    val projectScore: Int,
    val averageScore: Double,
    val colorHex: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    student: Student,
    onBack: () -> Unit,
    onOpenReportCardViewer: () -> Unit,
    onPrintReportCard: () -> Unit,
    onOpenDigitalId: (() -> Unit)? = null,
    studentFiles: List<StudentFile> = emptyList(),
    academicGrades: List<SubjectGradeItem> = emptyList(),
    onUploadFile: (fileName: String, category: String, fileSize: String) -> Unit = { _, _, _ -> },
    state: Okul360UiState = Okul360UiState(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(StudentProfileTab.ACADEMIC) }
    var showReportCardModal by remember { mutableStateOf(false) }

    val grades = academicGrades

    val gpa = remember(grades) {
        if (grades.isNotEmpty()) grades.map { it.averageScore }.average() else 0.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Öğrenci Profili",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BrandNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = PrimaryBlue
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onPrintReportCard) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Karne Yazdır",
                            tint = PrimaryBlue
                        )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Student Header Card
            item {
                StudentHeroProfileCard(
                    student = student,
                    gpa = gpa,
                    onOpenReportCardViewer = { showReportCardModal = true },
                    onPrintReportCard = onPrintReportCard,
                    onOpenDigitalId = onOpenDigitalId,
                    context = context
                )
            }

            // Contextual AI 360 Student Development & Achievement Insights
            item {
                RoleContextualAiCard(
                    screenName = "STUDENT_PROFILE",
                    state = state
                )
            }

            // 2. Navigation Tabs for Profile Subsections
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = PrimaryBlue,
                    edgePadding = 16.dp,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) }
                ) {
                    StudentProfileTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // 3. Tab Content Section
            when (selectedTab) {
                StudentProfileTab.ACADEMIC -> {
                    item {
                        AcademicSummarySection(
                            student = student,
                            grades = grades,
                            gpa = gpa,
                            onOpenReportCard = { showReportCardModal = true }
                        )
                    }
                }
                StudentProfileTab.ATTENDANCE -> {
                    item {
                        AttendanceSummarySection(student = student)
                    }
                }
                StudentProfileTab.PARENT_INFO -> {
                    item {
                        ParentContactSection(student = student, context = context)
                    }
                }
                StudentProfileTab.HEALTH -> {
                    item {
                        HealthAndDietSection(student = student)
                    }
                }
                StudentProfileTab.FINANCE -> {
                    item {
                        FinancialStatusSection(student = student)
                    }
                }
                StudentProfileTab.DOCUMENTS -> {
                    item {
                        StudentDocumentsSection(
                            student = student,
                            files = studentFiles,
                            onUploadFile = onUploadFile
                        )
                    }
                }
            }
        }
    }

    // Modal: Full Report Card Viewer
    if (showReportCardModal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showReportCardModal = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                ReportCardViewer(
                    student = student,
                    onDismiss = { showReportCardModal = false }
                )
            }
        }
    }
}

@Composable
private fun StudentHeroProfileCard(
    student: Student,
    gpa: Double,
    onOpenReportCardViewer: () -> Unit,
    onPrintReportCard: () -> Unit,
    onOpenDigitalId: (() -> Unit)?,
    context: Context
) {
    val avatarColor = Color(student.avatar_color_hex)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Avatar with Gradient Ring
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(avatarColor, avatarColor.copy(alpha = 0.75f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${student.first_name.take(1)}${student.last_name.take(1)}".uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name & Badges
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = student.full_name,
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp,
                            color = BrandNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Öğrenci No: #${student.student_number} • ${student.class_name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SecondaryEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Aktif Öğrenci",
                                color = SecondaryEmerald,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryBlueContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = student.academic_year,
                                color = PrimaryBlue,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            // Quick Info Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickProfileBadge(
                    label = "Genel Not Ort.",
                    value = String.format("%.1f", gpa),
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                QuickProfileBadge(
                    label = "Kan Grubu",
                    value = student.blood_type ?: "A Rh+",
                    color = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                QuickProfileBadge(
                    label = "Devam Oranı",
                    value = "%98.2",
                    color = SecondaryEmerald,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call Parent
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${student.parent_phone}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Veliyi Ara", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // WhatsApp
                OutlinedButton(
                    onClick = {
                        val cleanPhone = student.parent_phone.replace("+", "").replace(" ", "").replace("-", "")
                        val text = "Merhaba, ${student.full_name} öğrencimiz hakkında bilgilendirme:"
                        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryEmerald),
                    border = BorderStroke(1.dp, SecondaryEmerald),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Report Card Viewer Action
                IconButton(
                    onClick = onOpenReportCardViewer,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEDE9FE))
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Karne & Notlar",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (onOpenDigitalId != null) {
                    IconButton(
                        onClick = onOpenDigitalId,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0E7FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Dijital Kimlik",
                            tint = Color(0xFF4338CA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickProfileBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 1: ACADEMIC SUMMARY & GRADES
// -----------------------------------------------------------------------------------------
@Composable
private fun AcademicSummarySection(
    student: Student,
    grades: List<SubjectGradeItem>,
    gpa: Double,
    onOpenReportCard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // GPA & Certificate Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Genel Başarı Durumu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandNavy
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (gpa >= 85.0) "🏆 Takdir Belgesi Hak Kazandı" else "🎖️ Teşekkür Belgesi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (gpa >= 85.0) Color(0xFF059669) else Color(0xFFD97706)
                    )
                    Text(
                        text = "Sınıf Başarı Sıralaması: 1 / 24 Öğrenci",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onOpenReportCard,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tam Karne", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Grades List Header
        Text(
            text = "📚 Ders Bazında Notlar & Performans",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BrandNavy
        )

        if (grades.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Text(
                    text = "Bu öğrenci için henüz ders notu kaydı bulunmamaktadır.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            // Subject Grades List
            grades.forEach { grade ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(grade.colorHex).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = grade.subjectCode,
                                    color = Color(grade.colorHex),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = grade.subjectName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = BrandNavy
                                )
                                Text(
                                    text = "Öğrt: ${grade.teacherName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Average Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(grade.colorHex).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${String.format("%.1f", grade.averageScore)} Puan",
                                color = Color(grade.colorHex),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 Exam Score Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScorePill(title = "1. Yazılı", score = "${grade.exam1}")
                        ScorePill(title = "2. Yazılı", score = "${grade.exam2}")
                        ScorePill(title = "Sözlü", score = "${grade.oralScore}")
                        ScorePill(title = "Proje", score = "${grade.projectScore}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { (grade.averageScore / 100.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Color(grade.colorHex),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
}

@Composable
private fun ScorePill(title: String, score: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(score, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
        }
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 2: ATTENDANCE SUMMARY
// -----------------------------------------------------------------------------------------
@Composable
private fun AttendanceSummarySection(student: Student) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "📅 Devamsızlık & Giriş-Çıkış Özeti",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BrandNavy
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bugünkü Yoklama Durumu", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Okulda • Giriş: 08:35", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SecondaryEmerald)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SecondaryEmerald.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "GELDİ",
                            color = SecondaryEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AttendanceStatItem("Toplam Katılım", "42 Gün", SecondaryEmerald)
                    AttendanceStatItem("Mazeretli İzin", "1.0 Gün", PrimaryBlue)
                    AttendanceStatItem("Sağlık Raporu", "0.5 Gün", AccentAmber)
                    AttendanceStatItem("Özürsüz Devamsızlık", "0 Gün", Color(0xFFDC2626))
                }
            }
        }
    }
}

@Composable
private fun AttendanceStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 14.sp, color = color)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 3: PARENT & CONTACT
// -----------------------------------------------------------------------------------------
@Composable
private fun ParentContactSection(student: Student, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "👨‍👩‍👧 Veli & İletişim Bilgileri",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BrandNavy
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ContactRowItem(
                    icon = Icons.Default.Person,
                    title = "Birinci Derece Veli",
                    value = student.parent_name
                )
                ContactRowItem(
                    icon = Icons.Default.Phone,
                    title = "Telefon Numarası",
                    value = student.parent_phone,
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.parent_phone}"))
                        context.startActivity(intent)
                    }
                )
                ContactRowItem(
                    icon = Icons.Default.Email,
                    title = "E-Posta Adresi",
                    value = student.parent_email ?: "veli@scs360.com"
                )
                ContactRowItem(
                    icon = Icons.Default.Home,
                    title = "İkametgah Adresi",
                    value = "Ataşehir Mah. Çiçek Sok. No:14/2 Kadıköy / İstanbul"
                )
                ContactRowItem(
                    icon = Icons.Default.Emergency,
                    title = "Acil Durum 2. İrtibat",
                    value = "Ahmet Demir (Dayı) • +90 533 111 2233"
                )
            }
        }
    }
}

@Composable
private fun ContactRowItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryBlueContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (onClick != null) PrimaryBlue else BrandNavy
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 4: HEALTH & DIET
// -----------------------------------------------------------------------------------------
@Composable
private fun HealthAndDietSection(student: Student) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🩺 Sağlık, Beslenme & Özel Durumlar",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BrandNavy
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Allergy Warning Card
                val allergy = student.allergies_or_diet
                if (!allergy.isNullOrBlank() && allergy != "Yok") {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Alerji & Diyet Hassasiyeti", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF991B1B))
                                Text(allergy, fontSize = 12.sp, color = Color(0xFF7F1D1D))
                            }
                        }
                    }
                }

                ContactRowItem(icon = Icons.Default.Bloodtype, title = "Kan Grubu", value = student.blood_type ?: "A Rh+")
                ContactRowItem(icon = Icons.Default.Medication, title = "Düzenli İlaç Kullanımı", value = "Yok / Güncel ilaç bildirimi bulunmuyor")
                ContactRowItem(icon = Icons.Default.Restaurant, title = "Okul Yemekhanesi Tüketimi", value = "Tam Porsiyon (Gözlem altında düzenli tüketiyor)")
                ContactRowItem(icon = Icons.Default.NoteAlt, title = "Rehberlik & Öğretmen Sağlık Notu", value = "Öğrenci genel sağlık açısından oldukça aktif ve enerjiktir.")
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// SECTION 5: FINANCIAL STATUS
// -----------------------------------------------------------------------------------------
@Composable
private fun FinancialStatusSection(student: Student) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "💳 Mali & Aidat Özeti",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BrandNavy
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kayıt Dönem Ücreti", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₺45.000", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tahsil Edilen Toplam", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₺35.000", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SecondaryEmerald)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kalan Güncel Bakiye", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${student.tuition_balance.toInt()} ₺",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (student.tuition_balance > 0) Color(0xFFDC2626) else SecondaryEmerald
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { 35000f / 45000f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = SecondaryEmerald,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StudentDocumentsSection(
    student: Student,
    files: List<StudentFile>,
    onUploadFile: (fileName: String, category: String, fileSize: String) -> Unit
) {
    val context = LocalContext.current
    var showUploadDialog by remember { mutableStateOf(false) }

    val defaultFiles = remember(student.id) {
        if (files.isNotEmpty()) files else listOf(
            StudentFile("file-1", student.id, "Nufus_Cuzdani_Fotokopisi.pdf", "IDENTITY", "1.2 MB", "2026-09-01"),
            StudentFile("file-2", student.id, "Saglik_Raporu_Asi_Karti.pdf", "HEALTH_REPORT", "850 KB", "2026-09-02"),
            StudentFile("file-3", student.id, "Kayit_Sozlesmesi_2026.pdf", "REGISTRATION_CONTRACT", "2.4 MB", "2026-09-01"),
            StudentFile("file-4", student.id, "Vesikalik_Fotograf.jpg", "PHOTO", "420 KB", "2026-09-01")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Öğrenci Evrak & Dosya Arşivi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BrandNavy
                    )
                    Text(
                        text = "${defaultFiles.size} Adet Kayıtlı Resmi Belge",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { showUploadDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Evrak Yükle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        defaultFiles.forEach { file ->
            val (categoryLabel, categoryColor, categoryIcon) = when (file.category) {
                "IDENTITY" -> Triple("Kimlik Belgesi", Color(0xFF2563EB), Icons.Default.Badge)
                "HEALTH_REPORT" -> Triple("Sağlık Raporu", Color(0xFFDC2626), Icons.Default.MedicalServices)
                "REGISTRATION_CONTRACT" -> Triple("Kayıt Sözleşmesi", Color(0xFF059669), Icons.Default.Description)
                "PAYMENT_CONTRACT" -> Triple("Ödeme Protokolü", Color(0xFFD97706), Icons.Default.Payments)
                "PHOTO" -> Triple("Vesikalık Fotoğraf", Color(0xFF7C3AED), Icons.Default.Portrait)
                else -> Triple("Diğer Evrak", Color(0xFF64748B), Icons.Default.InsertDriveFile)
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(categoryColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(categoryIcon, contentDescription = null, tint = categoryColor, modifier = Modifier.size(24.dp))
                        }

                        Column {
                            Text(
                                text = file.file_name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = BrandNavy
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = categoryColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = categoryLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = categoryColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "${file.file_size} • ${file.upload_date}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                android.widget.Toast.makeText(context, "${file.file_name} görüntüleniyor.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = "Görüntüle", tint = PrimaryBlue)
                        }
                        IconButton(
                            onClick = {
                                android.widget.Toast.makeText(context, "${file.file_name} indirildi.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "İndir", tint = Color(0xFF059669))
                        }
                    }
                }
            }
        }
    }

    if (showUploadDialog) {
        var fileName by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("IDENTITY") }
        var categoryExpanded by remember { mutableStateOf(false) }

        val categoryOptions = listOf(
            "IDENTITY" to "Kimlik Belgesi Fotokopisi",
            "HEALTH_REPORT" to "Sağlık Raporu / Aşı Kartı",
            "REGISTRATION_CONTRACT" to "Kayıt Sözleşmesi",
            "PAYMENT_CONTRACT" to "Ödeme & Senet Protokolü",
            "PHOTO" to "Biyometrik Vesikalık",
            "OTHER" to "Diğer Resmi Evrak"
        )

        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text("Yeni Öğrenci Evrakı Yükle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Öğrenci: ${student.full_name} (#${student.student_number})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Dosya / Evrak Adı") },
                        placeholder = { Text("Örn: Askerlik_Durum_Belgesi.pdf") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { categoryExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(categoryOptions.find { it.first == selectedCategory }?.second ?: selectedCategory)
                        }
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categoryOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.second) },
                                    onClick = {
                                        selectedCategory = opt.first
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validName = if (fileName.isNotBlank()) fileName else "Belge_${System.currentTimeMillis()}.pdf"
                        onUploadFile(validName, selectedCategory, "1.4 MB")
                        showUploadDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Sisteme Yükle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
