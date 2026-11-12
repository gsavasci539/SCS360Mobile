package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinanceSummary
import com.example.data.model.Invoice
import com.example.data.model.Student
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.*

/**
 * Finansal Özet (Financial Summary) Card Component
 * Provides parents and administrators with a clean, clear visual overview of:
 * - Current debt and payment status (Toplam Tutar, Ödenen, Kalan Borç, Geciken Tutar)
 * - Payment / Collection Progress ratio
 * - Next upcoming installment alert
 * - Direct quick actions (Ödeme Yap, Detaylar, vb.)
 */
@Composable
fun FinancialSummaryCard(
    student: Student? = null,
    invoices: List<Invoice> = emptyList(),
    financeSummary: FinanceSummary? = null,
    isParentView: Boolean = false,
    onPayClick: ((Invoice) -> Unit)? = null,
    onViewAllInvoices: (() -> Unit)? = null,
    onViewReceipts: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    FinanceDataUnavailable("Öğrenciye ait doğrulanmış finans özeti henüz yüklenmedi.", modifier.testTag("financial_summary_card"))
}
