package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.FinanceSummary
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Data Model for Monthly Income vs. Expense Trend (Yıl Bazlı Gelir-Gider)
 * Contains comprehensive date and amount details for interactive tooltips.
 */
data class MonthFinancialMetric(
    val monthIndex: Int,                      // 0 to 11
    val monthName: String,                     // "Ocak", "Şubat", ...
    val monthShort: String,                    // "Oca", "Şub", ...
    val dateRangeText: String = "",            // "01 - 31 Ocak"
    val quarterLabel: String = "Q1",           // "Q1 • 1. Çeyrek"
    val income: Double,                        // Gelir (TL)
    val expense: Double,                       // Gider (TL)
    val collectionRate: Int = 92,              // % Tahsilat Başarısı
    val previousMonthIncome: Double? = null,   // Önceki Ayın Geliri
    val previousMonthExpense: Double? = null   // Önceki Ayın Gideri
) {
    val netSurplus: Double get() = income - expense
    val profitMarginPercent: Int get() = if (income > 0) ((netSurplus / income) * 100).toInt() else 0
    val incomeDiffFromPrev: Double get() = previousMonthIncome?.let { income - it } ?: 0.0
    val expenseDiffFromPrev: Double get() = previousMonthExpense?.let { expense - it } ?: 0.0
    val incomeGrowthPercent: Int? get() = previousMonthIncome?.let { prev ->
        if (prev > 0) (((income - prev) / prev) * 100).toInt() else null
    }
}

/**
 * Repository / Mock Provider for Multi-Year Financial Trends
 */
object FinancialTrendDataProvider {

    fun getTrendsForYear(year: Int, financeSummary: FinanceSummary? = null): List<MonthFinancialMetric> {
        val months = listOf(
            "Ocak" to "Oca",
            "Şubat" to "Şub",
            "Mart" to "Mar",
            "Nisan" to "Nis",
            "Mayıs" to "May",
            "Haziran" to "Haz",
            "Temmuz" to "Tem",
            "Ağustos" to "Ağu",
            "Eylül" to "Eyl",
            "Ekim" to "Eki",
            "Kasım" to "Kas",
            "Aralık" to "Ara"
        )

        val dateRanges = listOf(
            "01 - 31 Ocak",
            "01 - 28 Şubat",
            "01 - 31 Mart",
            "01 - 30 Nisan",
            "01 - 31 Mayıs",
            "01 - 30 Haziran",
            "01 - 31 Temmuz",
            "01 - 31 Ağustos",
            "01 - 30 Eylül",
            "01 - 31 Ekim",
            "01 - 30 Kasım",
            "01 - 31 Aralık"
        )

        val quarters = listOf(
            "Q1 • 1. Çeyrek", "Q1 • 1. Çeyrek", "Q1 • 1. Çeyrek",
            "Q2 • 2. Çeyrek", "Q2 • 2. Çeyrek", "Q2 • 2. Çeyrek",
            "Q3 • 3. Çeyrek", "Q3 • 3. Çeyrek", "Q3 • 3. Çeyrek",
            "Q4 • 4. Çeyrek", "Q4 • 4. Çeyrek", "Q4 • 4. Çeyrek"
        )

        val multiplier = when (year) {
            2026 -> 1.0
            2025 -> 0.82
            2024 -> 0.68
            else -> 1.0
        }

        val hasSummary = financeSummary != null && (financeSummary.total_collected_monthly > 0 || financeSummary.total_expenses_monthly > 0)
        val baseIncVal = if (hasSummary) financeSummary!!.total_collected_monthly else 550000.0
        val baseExpVal = if (hasSummary) financeSummary!!.total_expenses_monthly else 350000.0
        val baseCollRate = if (hasSummary) financeSummary!!.collection_rate_percentage.toInt().coerceIn(0, 100) else 93

        // Seasonal weight curve for school operations across 12 months
        val seasonalWeights = listOf(
            Pair(0.89, 0.88), // Ocak
            Pair(0.94, 0.92), // Şubat
            Pair(0.91, 0.94), // Mart
            Pair(0.99, 0.97), // Nisan
            Pair(1.04, 1.01), // Mayıs
            Pair(0.96, 0.91), // Haziran
            Pair(0.76, 0.84), // Temmuz
            Pair(1.16, 1.20), // Ağustos
            Pair(1.29, 1.28), // Eylül
            Pair(1.11, 1.08), // Ekim
            Pair(1.07, 1.04), // Kasım
            Pair(1.15, 1.12)  // Aralık
        )

        return months.mapIndexed { index, (fullName, shortName) ->
            val (wInc, wExp) = seasonalWeights[index]
            val inc = baseIncVal * wInc * multiplier
            val exp = baseExpVal * wExp * multiplier
            val rate = when (index) {
                7, 8 -> (baseCollRate + 3).coerceAtMost(100)
                6 -> (baseCollRate - 5).coerceAtLeast(60)
                else -> (baseCollRate + (index % 3)).coerceAtMost(100)
            }
            val prevInc = if (index > 0) baseIncVal * seasonalWeights[index - 1].first * multiplier else null
            val prevExp = if (index > 0) baseExpVal * seasonalWeights[index - 1].second * multiplier else null

            MonthFinancialMetric(
                monthIndex = index,
                monthName = fullName,
                monthShort = shortName,
                dateRangeText = dateRanges[index],
                quarterLabel = quarters[index],
                income = inc,
                expense = exp,
                collectionRate = rate,
                previousMonthIncome = prevInc,
                previousMonthExpense = prevExp
            )
        }
    }
}

/**
 * Recharts-Inspired Interactive Dual Line & Area Chart for Jetpack Compose
 * Visualizes Year-over-Year Income (Gelir) vs Expenses (Gider) with:
 * - Smooth Monotone Cubic Bézier Splines
 * - Gradient Area Fills (Emerald for Income, Rose for Expenses)
 * - Interactive Hover, Touch & Drag Detection (Haptic feedback)
 * - Dynamic Floating & Anchor Tooltip with Rich Date and Amount Details:
 *     - Exact date range (01 - 31 Mayıs 2026) and fiscal quarter
 *     - Income (Gelir) with month-over-month growth amount and percentage
 *     - Expenses (Gider) breakdown and ratio
 *     - Net profit margin and collection success rate
 * - Year Selector Tabs (2026 Güncel, 2025, 2024)
 * - Period Filters (Tüm Yıl, 1. Yarıyıl, 2. Yarıyıl)
 * - Annual KPI Summary Badges
 */
@Composable
fun YearlyFinancialTrendRechartsView(
    financeSummary: FinanceSummary? = null,
    onNavigateToFinance: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var selectedYear by remember { mutableIntStateOf(2026) }
    var selectedPeriod by remember { mutableIntStateOf(0) } // 0: Tüm Yıl (12 Ay), 1: 1. Dönem, 2: 2. Dönem

    val fullYearMetrics = remember(selectedYear, financeSummary) {
        FinancialTrendDataProvider.getTrendsForYear(selectedYear, financeSummary)
    }

    val displayedMetrics = remember(fullYearMetrics, selectedPeriod) {
        when (selectedPeriod) {
            1 -> fullYearMetrics.subList(0, 6)   // Oca - Haz
            2 -> fullYearMetrics.subList(6, 12)  // Tem - Ara
            else -> fullYearMetrics              // 12 Ay
        }
    }

    // Interactive tooltip selection & pointer tracking
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var hoverPointX by remember { mutableFloatStateOf(0f) }
    var hoverPointY by remember { mutableFloatStateOf(0f) }
    var isHoverActive by remember { mutableStateOf(false) }

    // Chart entry animation
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedYear, selectedPeriod) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    // Aggregations
    val totalIncome = remember(displayedMetrics) { displayedMetrics.sumOf { it.income } }
    val totalExpense = remember(displayedMetrics) { displayedMetrics.sumOf { it.expense } }
    val totalNetSurplus = totalIncome - totalExpense
    val overallMargin = if (totalIncome > 0) ((totalNetSurplus / totalIncome) * 100).toInt() else 0

    // Selected point (defaults to highlighted active month)
    val activePoint = selectedIndex?.let { idx ->
        if (idx in displayedMetrics.indices) displayedMetrics[idx] else null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("yearly_financial_trend_recharts_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title & Recharts Badge & Year Selector
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
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Yıllık Gelir & Gider Trendi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = "Recharts Tooltip",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Grafiğin üzerine gelerek tarih & miktar detaylarını inceleyin",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Year selector pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(2026, 2025, 2024).forEach { yr ->
                        val isSelected = selectedYear == yr
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) PrimaryBlue else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (selectedYear != yr) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedYear = yr
                                        selectedIndex = null
                                    }
                                }
                                .testTag("trend_year_selector_$yr")
                        ) {
                            Text(
                                text = yr.toString(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recharts-Style Annual Metric Badges & Legend Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gelir Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Column {
                        Text(
                            text = "Toplam Gelir",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "₺${formatCurrencyMillions(totalIncome)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                }

                // Gider Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF43F5E))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Column {
                        Text(
                            text = "Toplam Gider",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "₺${formatCurrencyMillions(totalExpense)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBE123C)
                        )
                    }
                }

                // Net Kâr Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Column {
                        Text(
                            text = "Net Kâr (%$overallMargin)",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "+₺${formatCurrencyMillions(totalNetSurplus)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-period Filter Tabs: [Tüm Yıl (12 Ay)] | [1. Yarıyıl] | [2. Yarıyıl]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Tüm Yıl", "1. Yarıyıl", "2. Yarıyıl").forEachIndexed { index, label ->
                        val isSelected = selectedPeriod == index
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFE2E8F0) else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (selectedPeriod != index) {
                                        selectedPeriod = index
                                        selectedIndex = null
                                    }
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (activePoint != null) "Grafikte geziniyorsunuz" else "Üzerine gelin veya dokunun",
                        fontSize = 9.5.sp,
                        fontWeight = if (activePoint != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (activePoint != null) PrimaryBlue else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // =========================================================================
            // CUSTOMIZED RECHARTS INTERACTIVE TOOLTIP CARD (Tarih ve Miktar Bazlı Detay)
            // =========================================================================
            AnimatedVisibility(
                visible = activePoint != null,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                if (activePoint != null) {
                    RechartsCustomizedTooltipCard(
                        metric = activePoint,
                        selectedYear = selectedYear,
                        onClose = { selectedIndex = null }
                    )
                }
            }

            // =========================================================================
            // RECHARTS CANVAS DUAL LINE & AREA WITH HOVER AND TOUCH TRACKING
            // =========================================================================
            val maxAmount = remember(displayedMetrics) {
                val maxVal = displayedMetrics.maxOfOrNull { maxOf(it.income, it.expense) } ?: 700000.0
                (maxVal * 1.15).coerceAtLeast(100000.0)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag("recharts_line_canvas_box")
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        // 1. Detect Hover Events (mouse, trackpad, cursor moving over chart)
                        .pointerInput(displayedMetrics) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Move, PointerEventType.Enter, PointerEventType.Press -> {
                                            val pos = event.changes.firstOrNull()?.position
                                            if (pos != null) {
                                                val count = displayedMetrics.size
                                                if (count > 0) {
                                                    val stepX = size.width / (count - 1).coerceAtLeast(1)
                                                    val idx = ((pos.x + (stepX / 2f)) / stepX)
                                                        .toInt()
                                                        .coerceIn(0, displayedMetrics.lastIndex)
                                                    if (selectedIndex != idx) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        selectedIndex = idx
                                                    }
                                                    hoverPointX = pos.x
                                                    hoverPointY = pos.y
                                                    isHoverActive = true
                                                }
                                            }
                                        }
                                        PointerEventType.Exit -> {
                                            isHoverActive = false
                                        }
                                    }
                                }
                            }
                        }
                        // 2. Detect Drag & Scrubbing Gestures for touchscreens
                        .pointerInput(displayedMetrics) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val count = displayedMetrics.size
                                    if (count > 0) {
                                        val stepX = size.width / (count - 1).coerceAtLeast(1)
                                        val idx = ((offset.x + (stepX / 2f)) / stepX)
                                            .toInt()
                                            .coerceIn(0, displayedMetrics.lastIndex)
                                        selectedIndex = idx
                                        hoverPointX = offset.x
                                        hoverPointY = offset.y
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            ) { change, _ ->
                                change.consume()
                                val count = displayedMetrics.size
                                if (count > 0) {
                                    val stepX = size.width / (count - 1).coerceAtLeast(1)
                                    val idx = ((change.position.x + (stepX / 2f)) / stepX)
                                        .toInt()
                                        .coerceIn(0, displayedMetrics.lastIndex)
                                    if (selectedIndex != idx) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedIndex = idx
                                    }
                                    hoverPointX = change.position.x
                                    hoverPointY = change.position.y
                                }
                            }
                        }
                        // 3. Detect Tap Gestures to toggle selection
                        .pointerInput(displayedMetrics) {
                            detectTapGestures { offset ->
                                val count = displayedMetrics.size
                                if (count > 0) {
                                    val stepX = size.width / (count - 1).coerceAtLeast(1)
                                    val tappedIdx = ((offset.x + (stepX / 2f)) / stepX)
                                        .toInt()
                                        .coerceIn(0, displayedMetrics.lastIndex)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedIndex = if (selectedIndex == tappedIdx) null else tappedIdx
                                    hoverPointX = offset.x
                                    hoverPointY = offset.y
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 24.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val chartHeight = height - bottomPadding - topPadding

                    val count = displayedMetrics.size
                    if (count < 2) return@Canvas

                    val stepX = width / (count - 1).coerceAtLeast(1)

                    // 1. Draw horizontal dashed Cartesian grid lines (Recharts style)
                    val gridSteps = 4
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (i in 0..gridSteps) {
                        val y = topPadding + (chartHeight * (i.toFloat() / gridSteps))
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    // 2. Map coordinates for Income and Expense
                    val progress = animProgress.value

                    val incomePoints = displayedMetrics.mapIndexed { i, m ->
                        val x = i * stepX
                        val normalized = (m.income / maxAmount).toFloat().coerceIn(0f, 1f) * progress
                        val y = topPadding + chartHeight * (1f - normalized)
                        Offset(x, y)
                    }

                    val expensePoints = displayedMetrics.mapIndexed { i, m ->
                        val x = i * stepX
                        val normalized = (m.expense / maxAmount).toFloat().coerceIn(0f, 1f) * progress
                        val y = topPadding + chartHeight * (1f - normalized)
                        Offset(x, y)
                    }

                    // Helper to build smooth cubic Bézier curves (Monotone Splines)
                    fun buildSplinePath(points: List<Offset>): Path {
                        val path = Path()
                        if (points.isEmpty()) return path
                        path.moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlX = (p0.x + p1.x) / 2f
                            path.cubicTo(
                                controlX, p0.y,
                                controlX, p1.y,
                                p1.x, p1.y
                            )
                        }
                        return path
                    }

                    val incomePath = buildSplinePath(incomePoints)
                    val expensePath = buildSplinePath(expensePoints)

                    // 3. Draw Gradient Area Fills beneath the curves (Recharts AreaChart effect)
                    val incomeAreaPath = Path().apply {
                        addPath(incomePath)
                        lineTo(incomePoints.last().x, topPadding + chartHeight)
                        lineTo(incomePoints.first().x, topPadding + chartHeight)
                        close()
                    }
                    drawPath(
                        path = incomeAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF10B981).copy(alpha = 0.22f),
                                Color(0xFF10B981).copy(alpha = 0.01f)
                            ),
                            startY = topPadding,
                            endY = topPadding + chartHeight
                        )
                    )

                    val expenseAreaPath = Path().apply {
                        addPath(expensePath)
                        lineTo(expensePoints.last().x, topPadding + chartHeight)
                        lineTo(expensePoints.first().x, topPadding + chartHeight)
                        close()
                    }
                    drawPath(
                        path = expenseAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF43F5E).copy(alpha = 0.16f),
                                Color(0xFFF43F5E).copy(alpha = 0.01f)
                            ),
                            startY = topPadding,
                            endY = topPadding + chartHeight
                        )
                    )

                    // 4. Draw the actual curved line strokes
                    drawPath(
                        path = incomePath,
                        color = Color(0xFF10B981),
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = expensePath,
                        color = Color(0xFFF43F5E),
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 5. Active Cursor Line & Tooltip Points with Highlighting Crosshairs
                    selectedIndex?.let { idx ->
                        if (idx in incomePoints.indices) {
                            val cursorX = incomePoints[idx].x
                            val incPt = incomePoints[idx]
                            val expPt = expensePoints[idx]

                            // Vertical crosshair guide line
                            drawLine(
                                color = Color(0xFF475569),
                                start = Offset(cursorX, 0f),
                                end = Offset(cursorX, topPadding + chartHeight),
                                strokeWidth = 1.3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                            )

                            // Horizontal subtle crosshair line to the nearest hovered point
                            val targetY = if (hoverPointY > (incPt.y + expPt.y) / 2f) expPt.y else incPt.y
                            drawLine(
                                color = Color(0xFF94A3B8).copy(alpha = 0.6f),
                                start = Offset(0f, targetY),
                                end = Offset(width, targetY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
                            )

                            // Income active halo dot
                            drawCircle(
                                color = Color(0xFF10B981).copy(alpha = 0.28f),
                                radius = 10.dp.toPx(),
                                center = incPt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 5.5.dp.toPx(),
                                center = incPt
                            )
                            drawCircle(
                                color = Color(0xFF10B981),
                                radius = 3.8.dp.toPx(),
                                center = incPt
                            )

                            // Expense active halo dot
                            drawCircle(
                                color = Color(0xFFF43F5E).copy(alpha = 0.28f),
                                radius = 10.dp.toPx(),
                                center = expPt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 5.5.dp.toPx(),
                                center = expPt
                            )
                            drawCircle(
                                color = Color(0xFFF43F5E),
                                radius = 3.8.dp.toPx(),
                                center = expPt
                            )
                        }
                    }

                    // 6. Draw regular circular dots on each data point
                    incomePoints.forEachIndexed { i, pt ->
                        if (i != selectedIndex) {
                            drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = pt)
                            drawCircle(
                                color = Color(0xFF10B981),
                                radius = 3.5.dp.toPx(),
                                center = pt,
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                        }
                    }

                    expensePoints.forEachIndexed { i, pt ->
                        if (i != selectedIndex) {
                            drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = pt)
                            drawCircle(
                                color = Color(0xFFF43F5E),
                                radius = 3.5.dp.toPx(),
                                center = pt,
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                        }
                    }
                }

                // Floating On-Canvas Mini Tooltip Marker Tag (Recharts Hover Tag)
                if (activePoint != null && selectedIndex != null) {
                    val count = displayedMetrics.size
                    val stepRatio = (selectedIndex!!.toFloat() / (count - 1).coerceAtLeast(1))
                    val biasAlignment = when {
                        stepRatio < 0.25f -> Alignment.TopStart
                        stepRatio > 0.75f -> Alignment.TopEnd
                        else -> Alignment.TopCenter
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(biasAlignment)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .zIndex(10f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.92f),
                            shadowElevation = 3.dp,
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${activePoint.monthShort} • Net: +₺${formatShort(activePoint.netSurplus)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // X-Axis Month Labels along the bottom of the canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    displayedMetrics.forEachIndexed { index, metric ->
                        val isSelected = selectedIndex == index
                        Text(
                            text = metric.monthShort,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) PrimaryBlue else Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedIndex = if (selectedIndex == index) null else index
                                }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer / Insight Bar & Link to Finance Module
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoGraph,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ort. Tahsilat: %93 • En Yüksek Gelir: Eylül",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                }

                if (onNavigateToFinance != null) {
                    Text(
                        text = "Detaylı Raporlar →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier
                            .clickable { onNavigateToFinance() }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Customized Recharts Tooltip Component (Tarih ve Miktar Bazlı Detay Kartı)
 * Shows precise date span, quarterly context, exact income amount, expense breakdown,
 * and net surplus margin with visual distribution indicator.
 */
@Composable
fun RechartsCustomizedTooltipCard(
    metric: MonthFinancialMetric,
    selectedYear: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .testTag("recharts_active_point_tooltip")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            // Tooltip Header: Tarih Bilgisi (Date Range, Quarter & Close Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${metric.dateRangeText} $selectedYear",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = metric.quarterLabel,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF065F46)
                    ) {
                        Text(
                            text = "%${metric.collectionRate} Tahsilat",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(8.dp))

            // Tooltip Body: Miktar Bazlı Detaylar (Gelir vs Gider vs Net Sonuç)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Gelir (Income)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tahsil Edilen Gelir",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₺${String.format(Locale.getDefault(), "%,.0f", metric.income)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = Color(0xFF34D399)
                    )
                    val growth = metric.incomeGrowthPercent
                    if (growth != null) {
                        val isPositive = growth >= 0
                        Text(
                            text = "${if (isPositive) "+" else ""}$growth% vs geçen ay",
                            fontSize = 9.sp,
                            color = if (isPositive) Color(0xFF34D399) else Color(0xFFFB7185)
                        )
                    }
                }

                // 2. Gider (Expense)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF43F5E))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Operasyonel Gider",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₺${String.format(Locale.getDefault(), "%,.0f", metric.expense)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = Color(0xFFFB7185)
                    )
                    Text(
                        text = "Personel, servis & mutfak",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. Net Kâr (Surplus)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Net Fazla / Kâr",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "+₺${String.format(Locale.getDefault(), "%,.0f", metric.netSurplus)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "%${metric.profitMarginPercent} Faaliyet Marjı",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7DD3FC)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Proportional Distribution Indicator Bar (Expense ratio vs Net Margin)
            val expenseRatio = (metric.expense / metric.income.coerceAtLeast(1.0)).toFloat().coerceIn(0.1f, 0.9f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(expenseRatio)
                        .background(Color(0xFFF43F5E))
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight((1f - expenseRatio).coerceAtLeast(0.05f))
                        .background(Color(0xFF10B981))
                )
            }
        }
    }
}

private fun formatCurrencyMillions(amount: Double): String {
    return if (amount >= 1_000_000) {
        String.format(Locale.getDefault(), "%.2f M", amount / 1_000_000.0)
    } else {
        String.format(Locale.getDefault(), "%,.0f k", amount / 1_000.0)
    }
}

private fun formatShort(amount: Double): String {
    return if (amount >= 1_000_000) {
        String.format(Locale.getDefault(), "%.1fM", amount / 1_000_000.0)
    } else {
        String.format(Locale.getDefault(), "%,.0fk", amount / 1_000.0)
    }
}
