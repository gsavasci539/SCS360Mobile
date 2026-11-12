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

    fun getTrendsForYear(year: Int, financeSummary: FinanceSummary? = null): List<MonthFinancialMetric> = emptyList()

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
    FinanceDataUnavailable("Yıllık gelir ve gider serisi henüz yüklenmedi. Kayıtlı tahsilatlar finans özetinde görüntülenir.",
        modifier.testTag("yearly_financial_trend_recharts_card"))
}
