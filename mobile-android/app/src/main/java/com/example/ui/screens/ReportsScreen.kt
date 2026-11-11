package com.example.ui.screens

import com.example.ui.components.ResponsiveTableScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BranchFinancialOverviewChart
import com.example.ui.components.CompactResponsiveCard
import com.example.ui.components.CompactTableRow
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.StickyTableHeaderRow
import com.example.ui.components.TableColumnSpec
import com.example.ui.components.isCompactCardMode
import com.example.ui.components.rememberResolvedTableMode
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryEmerald
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportsScreen(
    state: Okul360UiState,
    onSendReminder: ((invoiceId: String, studentName: String) -> Unit)? = null,
    onPrintClassReportCards: ((String) -> Unit)? = null,
    onExportAttendanceCsv: (() -> Unit)? = null,
    onExportFinanceCsv: (() -> Unit)? = null,
    onOpenAiAnalytics: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val reports = state.reportsData ?: return
    val overdueInvoices = state.invoices.filter { it.status == "OVERDUE" || (it.status == "PENDING" && it.due_date < "2026-08-18") }
    var selectedKarneClass by remember { mutableStateOf("all") }
    val viewModeState = rememberResolvedTableMode()
    val useCardMode = isCompactCardMode(viewModeState.value)

    ResponsiveTableScope() {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Raporlar & Analiz Merkezi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandNavy
                )
                Text(
                    text = "Mali durum, tahsilat, devamsızlık ve karne çıktıları",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
    
            // Role-Based AI Analytics Insight Card
            item {
                RoleContextualAiCard(
                    screenName = "REPORTS",
                    state = state,
                    onQuickActionClick = { onOpenAiAnalytics?.invoke() }
                )
            }
    
            // AI Intelligence Banner (Sade & Zarif)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenAiAnalytics?.invoke()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF5F3FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE))
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF7C3AED)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
    
                            Spacer(modifier = Modifier.width(12.dp))
    
                            Column {
                                Text(
                                    text = "✨ Akıllı Raporlama & Analitik",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF4C1D95)
                                )
                                Text(
                                    text = "3 aylık nakit akışı ve öğrenci risk tahminleri",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF6D28D9)
                                )
                            }
                        }
    
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
    
            // PDF Report Cards Export Section (Android PrintManager)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "📜 Resmi Öğrenci Karnesi (PDF)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BrandNavy
                                )
                            }
                        }
    
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Android PrintManager API kullanılarak doğrudan A4 formatında mühürlü, barkodlu ve not dökümlü resmi öğrenci karnesi PDF'i oluşturun.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
    
                        Spacer(modifier = Modifier.height(12.dp))
    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val targetClassName = if (selectedKarneClass == "all") "Tüm Sınıflar" else {
                                        state.classes.find { it.id == selectedKarneClass }?.name ?: selectedKarneClass
                                    }
                                    onPrintClassReportCards?.invoke(targetClassName)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("PDF Olarak Karne Çıkar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
    
            // CSV & Cloud Storage Export (Google Drive, Excel, File Manager Intent)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = SecondaryEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "📁 Veri & Rapor Dışa Aktar (CSV & Drive)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BrandNavy
                                )
                            }
                        }
    
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Yoklama, öğrenci ve finansal kayıtlarınızı CSV formatında dışa aktarın. Android dosya yöneticisi entegrasyonu sayesinde Google Drive, OneDrive veya Excel uygulamasına tek tıkla kaydedin.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
    
                        Spacer(modifier = Modifier.height(14.dp))
    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onExportAttendanceCsv?.invoke()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp), tint = SecondaryEmerald)
                                Spacer(Modifier.width(6.dp))
                                Text("Yoklama CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
    
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onExportFinanceCsv?.invoke()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mali Rapor CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
    
            // Branch Financial Overview Chart (Şube Gelir - Gider Karşılaştırmalı Grafiği)
            item {
                BranchFinancialOverviewChart(
                    branches = state.branches,
                    financeSummary = state.financeSummary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
    
            // Financial KPIs Cards
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💰 Tahsilat & Borç Analizi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BrandNavy
                        )
    
                        Spacer(modifier = Modifier.height(14.dp))
    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Tahsil Edilen", fontSize = 11.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Medium)
                                    Text("₺${String.format("%,.0f", reports.total_collected)}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = SecondaryEmerald)
                                }
                            }
    
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Bekleyen Borç", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Medium)
                                    Text("₺${String.format("%,.0f", reports.total_pending)}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = AccentRose)
                                }
                            }
                        }
    
                        Spacer(modifier = Modifier.height(12.dp))
    
                        // Progress bar for collection rate
                        val collectionPercent = if ((reports.total_collected + reports.total_pending) > 0) {
                            ((reports.total_collected / (reports.total_collected + reports.total_pending)) * 100).toInt()
                        } else 0
    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tahsilat Başarı Oranı", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%$collectionPercent", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
    
                        Spacer(modifier = Modifier.height(6.dp))
    
                        LinearProgressIndicator(
                            progress = { (collectionPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = SecondaryEmerald,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
    
            // Monthly Collection Chart (Custom Canvas Bar Chart)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📈 Aylık Tahsilat Trendi (2026)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandNavy
                        )
    
                        Spacer(modifier = Modifier.height(16.dp))
    
                        val months = listOf(
                            "Oca" to 42000f,
                            "Şub" to 48000f,
                            "Mar" to 51000f,
                            "Nis" to 46000f,
                            "May" to 58000f,
                            "Haz" to 64000f,
                            "Tem" to 59000f,
                            "Ağu" to 68000f
                        )
    
                        val maxVal = months.maxOf { it.second }
    
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            val barWidth = 24.dp.toPx()
                            val spacing = (size.width - (months.size * barWidth)) / (months.size + 1)
                            val chartHeight = size.height - 20.dp.toPx()
    
                            months.forEachIndexed { i, pair ->
                                val x = spacing + i * (barWidth + spacing)
                                val barH = (pair.second / maxVal) * chartHeight
                                val y = chartHeight - barH
    
                                drawRoundRect(
                                    color = if (i == months.lastIndex) PrimaryBlue else PrimaryBlue.copy(alpha = 0.4f),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barH),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }
                        }
    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            months.forEach { (m, _) ->
                                Text(text = m, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
    
            // NEW REPORT 1: Canteen & Nutrition Health Analytics Report
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "🥗 Kantin & Beslenme Sağlık Raporu",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BrandNavy
                                )
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7)) {
                                Text("Sağlıklı Okul", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            }
                        }
    
                        Spacer(modifier = Modifier.height(12.dp))
    
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Ort. Kalori/Öğrenci", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("420 kcal", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                                }
                            }
                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Sağlıklı Ürün Oranı", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("%86.4", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SecondaryEmerald)
                                }
                            }
                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Alerjen Engeli", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("18 Engelleme", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryBlue)
                                }
                            }
                        }
    
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("En Çok Tercih Edilen 3 Ürün:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf(
                            "1. Tam Buğdaylı Sandviç" to "%38 Tüketim",
                            "2. Taze Sıkma Portakal Suyu" to "%29 Tüketim",
                            "3. Organik Elma & Kuruyemiş" to "%21 Tüketim"
                        ).forEach { (item, share) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item, fontSize = 12.sp)
                                Text(share, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }
                    }
                }
            }
    
            // NEW REPORT 2: Transport & Security Performance Report
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "🚌 Servis Filosu & Güvenlik Performansı",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BrandNavy
                                )
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEFF6FF)) {
                                Text("100% Aktif GPS", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }
    
                        Spacer(modifier = Modifier.height(12.dp))
    
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = Color(0xFFF0FDF4)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Zamanında Varış", fontSize = 11.sp, color = Color(0xFF166534))
                                    Text("%98.2", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF15803D))
                                }
                            }
                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = Color(0xFFF0FDF4)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Kaza/SOS Olayı", fontSize = 11.sp, color = Color(0xFF166534))
                                    Text("0 Olay", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF15803D))
                                }
                            }
                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = Color(0xFFEFF6FF)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Ort. Biniş Süresi", fontSize = 11.sp, color = Color(0xFF1E40AF))
                                    Text("1.4 Dk", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryBlue)
                                }
                            }
                        }
                    }
                }
            }
    
            // Attendance & Occupancy Rates
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "🏫 Kapasite & Devamsızlık İstatistikleri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandNavy
                        )
    
                        // Overall Attendance Rate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color(0xFF059669))
                                Text("Bugünkü Katılım Oranı", fontSize = 13.sp)
                            }
                            Text("%${(reports.overall_attendance_rate * 100).toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
    
                        // Class Occupancy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = PrimaryBlue)
                                Text("Ortalama Sınıf Doluluğu", fontSize = 13.sp)
                            }
                            Text("%${(reports.class_occupancy_rate * 100).toInt()}", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
    
                        HorizontalDivider()
    
                        // Class Breakdown
                        Text("Sınıf Bazlı Doluluk Oranları", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    
                        state.classes.forEach { cls ->
                            val enrolled = state.students.count { it.class_id == cls.id }
                            val pct = if (cls.capacity > 0) (enrolled * 100 / cls.capacity) else 0
    
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = cls.name, fontSize = 12.sp, modifier = Modifier.weight(0.5f))
                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .weight(0.35f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (pct >= 90) AccentRose else PrimaryBlue,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                                Text(
                                    text = "%$pct",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(0.15f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
    
            // Overdue Debtors List (Geciken Borçlar)
            item {
                Text(
                    text = "⚠️ Geciken Ödemeler Takip Listesi (${overdueInvoices.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AccentRose
                )
            }
    
            if (overdueInvoices.isEmpty()) {
                item {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Harika! Şu anda gecikmiş ödeme kaydı bulunmamaktadır.",
                            modifier = Modifier.padding(14.dp),
                            fontSize = 12.5.sp,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            } else {
                stickyHeader {
                    StickyTableHeaderRow(
                        columns = listOf(
                            TableColumnSpec("Öğrenci / Sınıf", 1.8f),
                            TableColumnSpec("Vade Tarihi", 1.1f),
                            TableColumnSpec("Geciken Tutar", 1.1f, androidx.compose.ui.text.style.TextAlign.End)
                        ),
                        totalCount = overdueInvoices.size,
                        viewModeState = viewModeState
                    )
                }
    
                itemsIndexed(overdueInvoices, key = { _, it -> it.id }) { index, inv ->
                    if (useCardMode) {
                        CompactResponsiveCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = inv.student_name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = BrandNavy,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${inv.class_name} • Vade: ${inv.due_date}",
                                        fontSize = 10.5.sp,
                                        color = AccentRose,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "₺${inv.amount.toInt()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = AccentRose
                                )
                            }
                        }
                    } else {
                        CompactTableRow(index = index) {
                            Column(modifier = Modifier.weight(1.8f).padding(end = 6.dp)) {
                                Text(
                                    text = inv.student_name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BrandNavy,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = inv.class_name,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = inv.due_date,
                                fontSize = 11.sp,
                                color = AccentRose,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                text = "₺${inv.amount.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                color = AccentRose,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                modifier = Modifier.weight(1.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
