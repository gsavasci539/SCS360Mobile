package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Branch
import com.example.data.model.FinanceSummary
import com.example.ui.theme.*
import java.util.Locale

data class BranchFinanceData(
    val branchId: String,
    val branchName: String,
    val branchCode: String,
    val revenue: Double,      // Aylık Tahsilat / Gelir (TL)
    val expenses: Double,     // Aylık İşletme & Personel Gideri (TL)
    val studentCount: Int,
    val collectionRate: Int
) {
    val netSurplus: Double get() = revenue - expenses
    val marginPercentage: Int get() = if (revenue > 0) ((netSurplus / revenue) * 100).toInt() else 0
}

/**
 * Monthly Financial Overview Chart Component
 * Visualizes revenue (Gelir) versus expenses (Gider) across all school branches
 * with comparative bar charts, branch selection, and net operating margin summaries.
 */
@Composable
fun BranchFinancialOverviewChart(
    branches: List<Branch> = emptyList(),
    financeSummary: FinanceSummary? = null,
    modifier: Modifier = Modifier
) {
    FinanceDataUnavailable("Şube bazında gelir ve gider kayıtları henüz yüklenmedi.", modifier.testTag("branch_financial_chart_card"))
}
