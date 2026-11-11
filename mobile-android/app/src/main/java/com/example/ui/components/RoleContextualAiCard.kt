package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PageAiInsight
import com.example.data.model.UserRole
import com.example.ui.viewmodel.Okul360UiState
import com.example.util.AiAnalyticsEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Her sayfada ve her yetkiye özel çalışan Akıllı Yapay Zeka Analiz Kartı (RoleContextualAiCard)
 * Kullanıcı isteğine göre tamamen kapanıp (yalnızca şık başlık çubuğu) açılabilmektedir.
 */
@Composable
fun RoleContextualAiCard(
    screenName: String,
    state: Okul360UiState,
    modifier: Modifier = Modifier,
    onQuickActionClick: ((actionType: String) -> Unit)? = null,
    onOpenAiHubClick: (() -> Unit)? = null,
    initialExpanded: Boolean = false,
    isLoading: Boolean = false
) {
    if (isLoading) {
        RoleContextualAiCardSkeleton(modifier = modifier)
        return
    }
    val role = state.selectedRole
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshCounter by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Dynamic AI Insight computation based on screen, role, and current state data
    val insight: PageAiInsight = remember(screenName, role, state.selectedBranchId, state.students.size, state.attendanceRecords.size, state.invoices.size, refreshCounter) {
        AiAnalyticsEngine.getPageAiAnalysis(screenName, role, state)
    }

    // Role-specific gradient themes
    val (headerGradient, badgeBgColor, accentColor) = remember(role) {
        when (role) {
            UserRole.SUPERADMIN -> Triple(
                listOf(Color(0xFF4C1D95), Color(0xFF6D28D9), Color(0xFF8B5CF6)),
                Color(0xFF4C1D95),
                Color(0xFFA78BFA)
            )
            UserRole.OWNER -> Triple(
                listOf(Color(0xFF312E81), Color(0xFF4338CA), Color(0xFF6366F1)),
                Color(0xFF312E81),
                Color(0xFF818CF8)
            )
            UserRole.ADMIN -> Triple(
                listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF3B82F6)),
                Color(0xFF1E3A8A),
                Color(0xFF60A5FA)
            )
            UserRole.MANAGER -> Triple(
                listOf(Color(0xFF134E4A), Color(0xFF0D9488), Color(0xFF14B8A6)),
                Color(0xFF134E4A),
                Color(0xFF2DD4BF)
            )
            UserRole.TEACHER -> Triple(
                listOf(Color(0xFF064E3B), Color(0xFF059669), Color(0xFF10B981)),
                Color(0xFF064E3B),
                Color(0xFF34D399)
            )
            UserRole.FINANCE -> Triple(
                listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFF59E0B)),
                Color(0xFF78350F),
                Color(0xFFFBBF24)
            )
            UserRole.PARENT -> Triple(
                listOf(Color(0xFF581C87), Color(0xFF7C3AED), Color(0xFF8B5CF6)),
                Color(0xFF581C87),
                Color(0xFFA78BFA)
            )
            UserRole.CANTEEN -> Triple(
                listOf(Color(0xFF7C2D12), Color(0xFFEA580C), Color(0xFFF97316)),
                Color(0xFF7C2D12),
                Color(0xFFFB923C)
            )
        }
    }

    // Animation for refresh icon rotation
    val infiniteTransition = rememberInfiniteTransition(label = "ai_refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .testTag("ai_card_${screenName.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isExpanded) accentColor.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Gradient Banner (Clickable everywhere to toggle collapse / expand)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(headerGradient))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = if (isExpanded) 9.dp else 7.dp)
                    .testTag("ai_card_header_${screenName.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Role Badge & Gemini Sparkle + Preview Subtitle when collapsed
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = insight.roleBadge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "AI",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = if (isExpanded) "✨ ${insight.modelName} • Canlı Analiz" else "✨ ${insight.title}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right: Refresh button and Expand/Collapse toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable {
                                    if (!isRefreshing) {
                                        isRefreshing = true
                                        coroutineScope.launch {
                                            delay(600)
                                            refreshCounter++
                                            isRefreshing = false
                                        }
                                    }
                                }
                                .testTag("ai_refresh_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Yenile",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(17.dp)
                                    .then(if (isRefreshing) Modifier.rotate(rotation) else Modifier)
                            )
                        }

                        // Expand / Collapse Action Pill
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { isExpanded = !isExpanded }
                                .testTag("ai_expand_button"),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = if (isExpanded) "Daralt" else "Genişlet",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Daralt" else "Genişlet",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Animated Card Body: Completely collapses when isExpanded is false!
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title and Subtitle
                    Column {
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = Color(0xFF0F172A)
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = insight.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 11.5.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Summary Bubble
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = badgeBgColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(top = 1.dp)
                            )
                            Text(
                                text = insight.summary,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF1E293B),
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }

                    // Key Metrics Chips 2x2 Clean Grid
                    if (insight.keyMetrics.isNotEmpty()) {
                        val metricsChunks = insight.keyMetrics.take(4).chunked(2)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            metricsChunks.forEach { rowMetrics ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowMetrics.forEach { metric ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = accentColor.copy(alpha = 0.12f),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = metric.label,
                                                    fontSize = 10.5.sp,
                                                    color = Color(0xFF475569),
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = metric.value,
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFF0F172A),
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    if (rowMetrics.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Section: Highlights & Strategic Recommendations
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Key Findings / Highlights
                        if (insight.highlights.isNotEmpty()) {
                            Text(
                                text = "📌 Öne Çıkan AI Bulguları",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            insight.highlights.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier
                                            .size(15.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF334155),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Recommendations
                        if (insight.recommendations.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "💡 Stratejik Eylem Önerileri",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            insight.recommendations.forEach { rec ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier
                                            .size(15.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Text(
                                        text = rec,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF334155),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Action Bar: Quick Action Button & Collapse Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasQuickAction = !insight.quickActionTitle.isNullOrBlank() && !insight.quickActionType.isNullOrBlank()

                        if (hasQuickAction) {
                            Button(
                                onClick = {
                                    onQuickActionClick?.invoke(insight.quickActionType!!)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("ai_action_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = badgeBgColor,
                                    contentColor = Color.White
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = insight.quickActionTitle!!,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier
                                .then(if (hasQuickAction) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                .height(40.dp)
                                .testTag("ai_expand_toggle_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeBgColor.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = badgeBgColor
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Daralt",
                                    modifier = Modifier.size(16.dp),
                                    tint = badgeBgColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Kartı Daralt",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeBgColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
