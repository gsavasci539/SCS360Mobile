package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.util.Locale

enum class AiRoleFilter(val title: String, val emoji: String) {
    ALL("Tüm Raporlar", "🌐"),
    OWNER("Kurum Sahibi", "👑"),
    ADMIN("Yönetici & Müdür", "🏫"),
    TEACHER("Öğretmen", "👩‍🏫"),
    PARENT("Veli", "👨‍👩‍👧"),
    FINANCE("Finans", "💳")
}

enum class AnalyticsMainTab(val title: String, val emoji: String) {
    SWOT_OVERVIEW("Kurum Karnesi & SWOT", "🏛️"),
    FINANCE_CHARTS("Finansal Trendler", "💰"),
    ATTENDANCE_CHARTS("Yoklama Analitiği", "📊"),
    ACADEMIC_CHARTS("Akademik Başarı", "🎓"),
    STUDENT_MATRIX("Öğrenci Performans Analizi", "👥"),
    CUSTOM_REPORTS("Özel AI Raporları", "📑")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAnalyticsHubScreen(
    state: Okul360UiState,
    onGenerateOwnerForecast: () -> Unit,
    onGenerateBranchHealth: () -> Unit,
    onGenerateChurnRisk: () -> Unit,
    onOpenHomeworkGenerator: () -> Unit,
    onOpenNarrativeGenerator: () -> Unit,
    onOpenParentDigest: () -> Unit,
    onSelectReport: (AiAnalysisReport) -> Unit,
    onRefreshAiAnalysis: () -> Unit = {},
    onFilterBranch: (String) -> Unit = {},
    onFilterTimeframe: (String) -> Unit = {},
    onSetAnalyticsTab: (Int) -> Unit = {},
    onPrintPdfReport: () -> Unit = {},
    onSendStudentAction: (StudentPerformanceMatrixItem, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val allowedTabs = remember(state.selectedRole) {
        when (state.selectedRole) {
            UserRole.SUPERADMIN, UserRole.OWNER, UserRole.ADMIN, UserRole.MANAGER -> AnalyticsMainTab.entries
            UserRole.FINANCE -> listOf(
                AnalyticsMainTab.FINANCE_CHARTS,
                AnalyticsMainTab.CUSTOM_REPORTS
            )
            UserRole.TEACHER, UserRole.PARENT -> listOf(
                AnalyticsMainTab.ATTENDANCE_CHARTS,
                AnalyticsMainTab.ACADEMIC_CHARTS,
                AnalyticsMainTab.STUDENT_MATRIX,
                AnalyticsMainTab.CUSTOM_REPORTS
            )
            UserRole.CANTEEN -> listOf(
                AnalyticsMainTab.CUSTOM_REPORTS
            )
        }
    }
    var selectedTab by remember(allowedTabs) { mutableStateOf(allowedTabs.first()) }
    var selectedRoleFilter by remember(state.selectedRole) {
        mutableStateOf(
            when (state.selectedRole) {
                UserRole.TEACHER -> AiRoleFilter.TEACHER
                UserRole.PARENT -> AiRoleFilter.PARENT
                UserRole.FINANCE -> AiRoleFilter.FINANCE
                else -> AiRoleFilter.ALL
            }
        )
    }
    var studentSearchQuery by remember { mutableStateOf("") }
    var studentRiskFilter by remember { mutableStateOf("TÜMÜ") } // TÜMÜ, RISKLI, BASARILI

    val swot = state.institutionalSwot ?: remember {
        com.example.util.AiAnalyticsEngine.generateInstitutionalSwotAnalysis(
            invoices = state.invoices,
            students = state.students,
            attendances = state.attendanceRecords,
            branches = state.branches,
            dailyReports = state.dailyReports
        )
    }

    val studentMatrix = remember(state.studentPerformanceMatrix, studentSearchQuery, studentRiskFilter) {
        val baseList = if (state.studentPerformanceMatrix.isNotEmpty()) {
            state.studentPerformanceMatrix
        } else {
            com.example.util.AiAnalyticsEngine.generateStudentPerformanceMatrix(
                students = state.students,
                invoices = state.invoices,
                attendances = state.attendanceRecords,
                dailyReports = state.dailyReports
            )
        }

        baseList.filter { item ->
            val matchesQuery = studentSearchQuery.isEmpty() ||
                    item.student_name.contains(studentSearchQuery, ignoreCase = true) ||
                    item.student_number.contains(studentSearchQuery, ignoreCase = true) ||
                    item.class_name.contains(studentSearchQuery, ignoreCase = true)

            val matchesFilter = when (studentRiskFilter) {
                "RISKLI" -> item.risk_level == "YÜKSEK" || item.risk_level == "ORTA"
                "BASARILI" -> item.gpa >= 88.0 && item.risk_level == "DÜŞÜK"
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    val filteredReports = remember(state.aiReports, selectedRoleFilter) {
        when (selectedRoleFilter) {
            AiRoleFilter.ALL -> state.aiReports
            AiRoleFilter.OWNER -> state.aiReports.filter { it.target_role == "OWNER" }
            AiRoleFilter.ADMIN -> state.aiReports.filter { it.target_role == "ADMIN" }
            AiRoleFilter.TEACHER -> state.aiReports.filter { it.target_role == "TEACHER" }
            AiRoleFilter.PARENT -> state.aiReports.filter { it.target_role == "PARENT" }
            AiRoleFilter.FINANCE -> state.aiReports.filter { it.target_role == "FINANCE" || it.target_role == "OWNER" }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // 1. Hero Banner: Gemini 3.5 Flash Central Hub
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0F172A), // Slate 900
                                    Color(0xFF1E1B4B), // Deep Indigo
                                    Color(0xFF2563EB)  // Modern Blue
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Yapay Zeka Analiz Raporları",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Google Gemini 3.5 Flash Zeka Motoru",
                                        fontSize = 12.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Color(0xFF34D399))
                            ) {
                                Text(
                                    text = "CANLI VERİ",
                                    color = Color(0xFF6EE7B7),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Finansal nakit akışı, sınıf bazlı yoklama trendleri ve akademik sınav başarıları Gemini API ile çapraz analiz edilerek kurumsal gelişim stratejisi oluşturulmuştur.",
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRefreshAiAnalysis()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "AI Yenile",
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPrintPdfReport()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PDF Rapor",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    clipboardManager.setText(AnnotatedString(swot.executive_summary))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Kopyala",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. High Level KPI Scorecards (Filtered by AnalysisPermission domain access)
        // ==========================================
        item {
            val canViewSwot = AnalyticsMainTab.SWOT_OVERVIEW in allowedTabs
            val canViewFinance = AnalyticsMainTab.FINANCE_CHARTS in allowedTabs
            val canViewAttendance = AnalyticsMainTab.ATTENDANCE_CHARTS in allowedTabs
            val canViewAcademic = AnalyticsMainTab.ACADEMIC_CHARTS in allowedTabs

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canViewSwot) {
                    KpiScoreCard(
                        title = "Genel Kurum",
                        value = "%${String.format(Locale.US, "%.1f", swot.overall_institution_score)}",
                        badge = "A+",
                        badgeColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (canViewFinance) {
                    KpiScoreCard(
                        title = "Tahsilat Sağlığı",
                        value = "%${String.format(Locale.US, "%.1f", swot.finance_health_score)}",
                        badge = "Nakit A",
                        badgeColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (canViewAttendance) {
                    KpiScoreCard(
                        title = "Yoklama Oranı",
                        value = "%${String.format(Locale.US, "%.1f", swot.attendance_health_score)}",
                        badge = "Devam",
                        badgeColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (canViewAcademic) {
                    KpiScoreCard(
                        title = "Akademik GPA",
                        value = "${swot.academic_success_score}",
                        badge = "Başarı",
                        badgeColor = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ==========================================
        // 3. Main Navigation Tabs (Only tabs where all required domains are permitted)
        // ==========================================
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allowedTabs) { tab ->
                    val isSelected = selectedTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = tab
                        },
                        label = {
                            Text(
                                text = "${tab.emoji} ${tab.title}",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // ==========================================
        // Loading State Banner
        // ==========================================
        if (state.isAiLoading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlueContainer.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = PrimaryBlue,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Gemini API Analiz Yapıyor...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BrandNavy
                            )
                            Text(
                                text = state.aiLoadingMessage,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // TAB CONTENTS
        // ==========================================
        when (selectedTab) {
            AnalyticsMainTab.SWOT_OVERVIEW -> {
                // 1. SWOT & Kurumsal Gelişim Önerileri
                item {
                    SwotAndInstitutionalGrowthCard(swot = swot)
                }

                item {
                    StrategicActionRoadmapCard(roadmap = swot.strategic_action_roadmap)
                }
            }

            AnalyticsMainTab.FINANCE_CHARTS -> {
                // 2. Finansal Trendler ve Recharts Tarzı Bar Grafiği
                item {
                    FinanceBarChartCard(
                        dataList = state.monthlyFinanceChartData,
                        invoices = state.invoices
                    )
                }

                item {
                    FinancialFlowProjectionCard(state = state)
                }
            }

            AnalyticsMainTab.ATTENDANCE_CHARTS -> {
                // 3. Yoklama Analitiği & Şubeler Arası Karşılaştırma Grafiği
                item {
                    AttendanceBranchChartCard(dataList = state.branchAttendanceChartData)
                }

                item {
                    DayOfWeekAttendanceTrendCard()
                }
            }

            AnalyticsMainTab.ACADEMIC_CHARTS -> {
                // 4. Akademik Ders Başarı ve Sınav Net Grafikleri
                item {
                    SubjectAcademicChartCard(dataList = state.subjectAcademicChartData)
                }

                item {
                    LgsNetTrendChartCard(dataList = state.weeklyLgsNetTrendData)
                }
            }

            AnalyticsMainTab.STUDENT_MATRIX -> {
                // 5. 'Öğrenci ve Kurum Performans Analizi' Bileşeni
                item {
                    StudentMatrixFilterBar(
                        searchQuery = studentSearchQuery,
                        onSearchChange = { studentSearchQuery = it },
                        selectedFilter = studentRiskFilter,
                        onFilterChange = { studentRiskFilter = it }
                    )
                }

                if (studentMatrix.isEmpty()) {
                    item {
                        EmptyStateCard(message = "Arama kriterine uygun öğrenci bulunamadı.")
                    }
                } else {
                    items(studentMatrix) { item ->
                        StudentPerformanceMatrixRow(
                            item = item,
                            onActionClick = { actionType ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSendStudentAction(item, actionType)
                            }
                        )
                    }
                }
            }

            AnalyticsMainTab.CUSTOM_REPORTS -> {
                // 6. Özel AI Rapor Listesi ve Filtreleme
                item {
                    Text(
                        text = "🚀 Hızlı AI Asistanı & Analiz Üreticileri",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BrandNavy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onGenerateOwnerForecast()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    "📈 Kurucu Tahmini",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onGenerateBranchHealth()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    "🏥 Şube Sağlığı",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onGenerateChurnRisk()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    "⚠️ Risk & Kayıp",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenHomeworkGenerator()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    "✍️ AI Ödev Üret",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "📑 Hazırlanan Rol Bazlı Yapay Zeka Raporları",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BrandNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AiRoleFilter.entries) { filter ->
                            val isSel = selectedRoleFilter == filter
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedRoleFilter = filter
                                },
                                label = { Text("${filter.emoji} ${filter.title}", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (filteredReports.isEmpty() && !state.isAiLoading) {
                    item {
                        EmptyStateCard(message = "Bu kategoride henüz oluşturulmuş rapor bulunmuyor.")
                    }
                } else {
                    items(filteredReports) { report ->
                        AiReportCard(
                            report = report,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectReport(report)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS & RECHARTS-STYLE VISUALIZERS
// ==========================================

@Composable
fun KpiScoreCard(
    title: String,
    value: String,
    badge: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = BrandNavy
            )
        }
    }
}

@Composable
fun SwotAndInstitutionalGrowthCard(swot: InstitutionalSwotAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏛️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kurumsal Gelişim & SWOT Analizi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandNavy
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Gemini 3.5 Flash",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = swot.executive_summary,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SWOT 4-Quadrant Visual Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Strengths
                SwotQuadrantBox(
                    title = "GÜÇLÜ YÖNLER (STRENGTHS)",
                    items = swot.strengths,
                    color = Color(0xFF10B981),
                    bgColor = Color(0xFFECFDF5),
                    icon = "🟢"
                )

                // Weaknesses
                SwotQuadrantBox(
                    title = "GELİŞİME AÇIK ALANLAR (WEAKNESSES)",
                    items = swot.weaknesses,
                    color = Color(0xFFD97706),
                    bgColor = Color(0xFFFFFBEB),
                    icon = "🟡"
                )

                // Opportunities
                SwotQuadrantBox(
                    title = "STRATEJİK FIRSATLAR (OPPORTUNITIES)",
                    items = swot.opportunities,
                    color = Color(0xFF2563EB),
                    bgColor = Color(0xFFEFF6FF),
                    icon = "🔵"
                )

                // Threats
                SwotQuadrantBox(
                    title = "KRİTİK RİSKLER & TEHDİTLER (THREATS)",
                    items = swot.threats_and_risks,
                    color = Color(0xFFDC2626),
                    bgColor = Color(0xFFFEF2F2),
                    icon = "🔴"
                )
            }
        }
    }
}

@Composable
fun SwotQuadrantBox(
    title: String,
    items: List<String>,
    color: Color,
    bgColor: Color,
    icon: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$icon $title",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = color
            )
            Spacer(modifier = Modifier.height(6.dp))
            items.forEach { item ->
                Text(
                    text = item,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
fun StrategicActionRoadmapCard(roadmap: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎯", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Öncelikli Yönetim & Zümre Aksiyon Yol Haritası",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandNavy
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            roadmap.forEachIndexed { idx, action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = action,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ==========================================
// FINANCE CHARTS (Recharts Visualizer)
// ==========================================

@Composable
fun FinanceBarChartCard(
    dataList: List<MonthlyFinanceChartData>,
    invoices: List<Invoice>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "💰 Aylık Tahsilat & Hedef Grafiği",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BrandNavy
                    )
                    Text(
                        text = "Recharts Çoklu Seri Tahsilat Modeli",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Ort. %95.3",
                        color = Color(0xFF047857),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (dataList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aylık tahsilat verisi bulunamadı",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Interactive Bar Graph Canvas in Jetpack Compose
                val maxAmount = 350000.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dataList.forEach { item ->
                    val collectedFraction = (item.collected_amount / maxAmount).toFloat().coerceIn(0.1f, 1f)
                    val targetFraction = (item.target_amount / maxAmount).toFloat().coerceIn(0.1f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "%${item.collection_percentage.toInt()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.height(110.dp)
                        ) {
                            // Target bar (grey/slate)
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxHeight(targetFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0xFFCBD5E1))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            // Collected bar (primary blue)
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(collectedFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.month_name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).background(Color(0xFFCBD5E1), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Hedef Tahakkuk", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(10.dp).background(PrimaryBlue, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Gerçekleşen Tahsilat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        }
    }
}

@Composable
fun FinancialFlowProjectionCard(state: Okul360UiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔮 90 Günlük AI Nakit Akışı Projeksiyonu",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BrandNavy
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Tahmini Nakit Girişi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "₺1.450.000", fontSize = 16.sp, fontWeight = FontWeight.Black, color = PrimaryBlue)
                    }
                    Column {
                        Text(text = "Riskli Alacak", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "₺84.000", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                    }
                    Column {
                        Text(text = "Güven Endeksi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "%94.2", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                    }
                }
            }
        }
    }
}

// ==========================================
// ATTENDANCE CHARTS (Recharts Visualizer)
// ==========================================

@Composable
fun AttendanceBranchChartCard(dataList: List<BranchAttendanceChartData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 Şubeler Arası Devamlılık Oranları",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BrandNavy
            )
            Text(
                text = "Şube bazında katılım, devamsızlık ve geç kalma metrikleri",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (dataList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Şube yoklama verisi bulunamadı",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                dataList.forEach { branch ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = branch.branch_name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                            Text(
                                text = "%${String.format(Locale.US, "%.1f", branch.attendance_percentage)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        // Progress Bar
                        val fraction = (branch.attendance_percentage / 100.0).toFloat().coerceIn(0.5f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF10B981), Color(0xFF059669))
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Mevcut: ${branch.present_count} öğrenci",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Devamsız: ${branch.absent_count} • Geç: ${branch.late_count}",
                                fontSize = 10.sp,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayOfWeekAttendanceTrendCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📅 Haftanın Günlerine Göre Devamsızlık Isı Eğrisi",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BrandNavy
            )
            Spacer(modifier = Modifier.height(10.dp))

            val days = listOf("Pazartesi" to 96.5, "Salı" to 97.2, "Çarşamba" to 96.8, "Perşembe" to 95.9, "Cuma" to 91.8)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                days.forEach { (day, rate) ->
                    val isFriday = day == "Cuma"
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFriday) Color(0xFFFEE2E2) else Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, if (isFriday) Color(0xFFFCA5A5) else Color(0xFFA7F3D0))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = day.take(3), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "%$rate",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isFriday) Color(0xFFDC2626) else Color(0xFF059669)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠️ AI Uyarısı: Cuma öğleden sonra devamsızlık %4.7 artış gösteriyor. Son iki ders için proje/etkinlik bazlı programlama önerilir.",
                fontSize = 11.sp,
                color = Color(0xFFB45309),
                lineHeight = 15.sp
            )
        }
    }
}

// ==========================================
// ACADEMIC CHARTS (Recharts Visualizer)
// ==========================================

@Composable
fun SubjectAcademicChartCard(dataList: List<SubjectAcademicChartData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎓 Ders Bazlı Başarı & Kazanım Ortalamaları",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BrandNavy
            )
            Text(
                text = "Zümre hedefleri ve sınıf genel ortalamaları",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (dataList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ders başarı ortalaması verisi bulunamadı",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                dataList.forEach { subj ->
                    val fraction = (subj.class_average / 100.0).toFloat().coerceIn(0.2f, 1f)
                    val isBelowTarget = subj.class_average < subj.target_benchmark

                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subj.subject_name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ort: ${String.format(Locale.US, "%.1f", subj.class_average)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBelowTarget) Color(0xFFDC2626) else Color(0xFF059669)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(Hedef: ${subj.target_benchmark})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        if (isBelowTarget) Color(0xFFF59E0B) else PrimaryBlue
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LgsNetTrendChartCard(dataList: List<WeeklyLgsNetTrendData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 LGS / YKS Deneme Net Trendi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BrandNavy
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "+9.7 Net Artış",
                        color = Color(0xFF047857),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (dataList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Deneme sınavı net verisi bulunamadı",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Line chart visually in Compose Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    val stepX = size.width / (dataList.size - 1).coerceAtLeast(1)
                    val minNet = 60f
                    val maxNet = 85f

                    val points = dataList.mapIndexed { idx, item ->
                        val x = idx * stepX
                        val y = size.height - ((item.total_net.toFloat() - minNet) / (maxNet - minNet)) * size.height
                        Offset(x, y.coerceIn(8f, size.height - 8f))
                    }

                    val path = Path()
                    points.forEachIndexed { i, pt ->
                        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }

                    drawPath(
                        path = path,
                        color = Color(0xFF2563EB),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    points.forEach { pt ->
                        drawCircle(Color.White, radius = 5.dp.toPx(), center = pt)
                        drawCircle(Color(0xFF2563EB), radius = 3.5.dp.toPx(), center = pt)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dataList.forEach { item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = item.trial_title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${item.total_net} Net", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// STUDENT PERFORMANCE MATRIX (360° Matrix Component)
// ==========================================

@Composable
fun StudentMatrixFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Öğrenci adı, no veya sınıf ara...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "TÜMÜ",
                onClick = { onFilterChange("TÜMÜ") },
                label = { Text("Tüm Öğrenciler", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "RISKLI",
                onClick = { onFilterChange("RISKLI") },
                label = { Text("⚠️ Riskli / Devamsız", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFDC2626),
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedFilter == "BASARILI",
                onClick = { onFilterChange("BASARILI") },
                label = { Text("🌟 Yüksek Başarılı", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF10B981),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun StudentPerformanceMatrixRow(
    item: StudentPerformanceMatrixItem,
    onActionClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.student_name.take(2).uppercase(),
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.student_name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = "#${item.student_number} • Sınıf: ${item.class_name}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Risk Badge
                val riskColor = when (item.risk_level) {
                    "YÜKSEK" -> Color(0xFFDC2626)
                    "ORTA" -> Color(0xFFD97706)
                    else -> Color(0xFF10B981)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${item.risk_level} RİSK",
                        color = riskColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini KPI Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(text = "GPA Not", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = String.format(Locale.US, "%.1f", item.gpa), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(text = "Yoklama", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "%${String.format(Locale.US, "%.0f", item.attendance_rate)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(text = "Aidat", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (item.payment_status == "PAID") "Ödendi" else if (item.payment_status == "PENDING") "Bekliyor" else "Gecikti",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.payment_status == "OVERDUE") Color(0xFFDC2626) else BrandNavy
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(text = "İyilik Skoru", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${item.wellness_score}/100", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "💡 AI Rehberlik Notu: ${item.ai_pedagogical_advice}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onActionClick("PARENT_NOTE") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "Veliye Not", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = { onActionClick("HOMEWORK") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "AI Föy Ata", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🤖", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AiReportCard(
    report: AiAnalysisReport,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📑", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = report.target_role,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = report.summary,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Oluşturulma: ${report.created_at}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Detayları Gör →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }
    }
}

