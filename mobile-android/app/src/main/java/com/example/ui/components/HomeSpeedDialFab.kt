package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Data model representing an action item in the Speed Dial FAB menu
 */
data class SpeedDialAction(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color = Color.White,
    val testTag: String,
    val onClick: () -> Unit
)

/**
 * Modern Material 3 Generic Role-Based Speed-Dial Floating Action Button (FAB) Menu.
 * Positioned at the bottom-right of the user portal screens with bouncy spring animations,
 * high-contrast pills, and haptic feedback.
 */
@Composable
fun RoleSpeedDialFab(
    actions: List<SpeedDialAction>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    fabColor: Color = PrimaryBlue,
    fabLabel: String = "Hızlı Aksiyon",
    showLabelPill: Boolean = false,
    testTag: String = "role_speed_dial_fab"
) {
    val haptic = LocalHapticFeedback.current

    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 135f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "FabRotation"
    )

    val fabScale by animateFloatAsState(
        targetValue = if (expanded) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "FabScale"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .wrapContentSize()
            .padding(bottom = 12.dp, end = 6.dp)
    ) {
        // Speed dial expanded action items with staggered animation
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(200)) + expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + scaleIn(initialScale = 0.85f, animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(160)) + shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            ) + scaleOut(targetScale = 0.85f, animationSpec = tween(160))
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                actions.forEachIndexed { index, action ->
                    SpeedDialActionItem(
                        action = action,
                        index = index
                    )
                }
            }
        }

        // Primary Speed Dial Floating Action Button ("Hızlı Aksiyon")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Optional expanded helper pill
            if (showLabelPill) {
                AnimatedVisibility(
                    visible = !expanded,
                    enter = fadeIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BrandNavy,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onExpandedChange(true)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = fabLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExpandedChange(!expanded)
                },
                shape = CircleShape,
                containerColor = if (expanded) MaterialTheme.colorScheme.surfaceVariant else fabColor,
                contentColor = if (expanded) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = if (expanded) 8.dp else 6.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier
                    .size(60.dp)
                    .scale(fabScale)
                    .testTag(testTag)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (expanded) "Hızlı Aksiyon Menüsünü Kapat" else "Hızlı Aksiyon Menüsünü Aç",
                    modifier = Modifier
                        .size(30.dp)
                        .rotate(rotationAngle)
                )
            }
        }
    }
}

/**
 * Enhanced Speed-Dial FAB Menu for Admin & Executive Quick Operations
 */
@Composable
fun HomeSpeedDialFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTakeAttendance: () -> Unit,
    onPostAnnouncement: () -> Unit,
    onReceivePayment: () -> Unit,
    onAddStudent: () -> Unit = {},
    onOpenDailyReportOrHomework: () -> Unit = {},
    onOpenConsentsOrApprovals: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val actions = remember(
        onTakeAttendance,
        onPostAnnouncement,
        onReceivePayment,
        onAddStudent,
        onOpenDailyReportOrHomework,
        onOpenConsentsOrApprovals
    ) {
        listOf(
            SpeedDialAction(
                id = "take_attendance",
                title = "Hızlı Yoklama Al",
                description = "Sınıf seçimi ile anında yoklama kaydet",
                icon = Icons.Default.FactCheck,
                containerColor = SecondaryEmerald,
                testTag = "fab_action_take_attendance",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExpandedChange(false)
                    onTakeAttendance()
                }
            ),
            SpeedDialAction(
                id = "receive_payment",
                title = "Hızlı Tahsilat / Ödeme Ekle",
                description = "Fatura seçimi, Kredi Kartı veya Nakit tahsilat",
                icon = Icons.Default.Payments,
                containerColor = Color(0xFF2563EB),
                testTag = "fab_action_receive_payment",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExpandedChange(false)
                    onReceivePayment()
                }
            ),
            SpeedDialAction(
                id = "add_student",
                title = "Yeni Öğrenci Kaydı",
                description = "Hızlı öğrenci ve veli kaydı oluştur",
                icon = Icons.Default.PersonAdd,
                containerColor = Color(0xFF0284C7),
                testTag = "fab_action_add_student",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExpandedChange(false)
                    onAddStudent()
                }
            ),
            SpeedDialAction(
                id = "add_announcement",
                title = "Hızlı Duyuru Gönder",
                description = "Veli ve personele anlık bildirim yayınla",
                icon = Icons.Default.Campaign,
                containerColor = Color(0xFFF59E0B),
                testTag = "fab_action_add_announcement",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExpandedChange(false)
                    onPostAnnouncement()
                }
            ),
            SpeedDialAction(
                id = "daily_report_homework",
                title = "Günlük Karne & Ödev",
                description = "Öğrenci gelişim karnesi ve ödev girişi",
                icon = Icons.Default.Assignment,
                containerColor = Color(0xFFBE185D),
                testTag = "fab_action_daily_report",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExpandedChange(false)
                    onOpenDailyReportOrHomework()
                }
            ),
            SpeedDialAction(
                id = "approvals_consents",
                title = "İzin & Belge Onayları",
                description = "Bekleyen veli izin ve muvafakatnameler",
                icon = Icons.Default.VerifiedUser,
                containerColor = Color(0xFF7C3AED),
                testTag = "fab_action_approvals",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExpandedChange(false)
                    onOpenConsentsOrApprovals()
                }
            )
        )
    }

    RoleSpeedDialFab(
        actions = actions,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        fabColor = PrimaryBlue,
        fabLabel = "Hızlı İşlem",
        showLabelPill = true,
        testTag = "dashboard_quick_action_fab",
        modifier = modifier
    )
}

@Composable
private fun SpeedDialActionItem(
    action: SpeedDialAction,
    index: Int
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .wrapContentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = action.onClick
            )
            .testTag(action.testTag)
    ) {
        // Label Pill
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            ),
            modifier = Modifier.padding(end = 10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = action.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = action.description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Small FAB Icon Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(action.containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = action.contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
