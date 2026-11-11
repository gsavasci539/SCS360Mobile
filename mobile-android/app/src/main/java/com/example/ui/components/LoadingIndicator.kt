package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DesignTokens
import com.example.ui.theme.NeutralLightBorder
import com.example.ui.theme.NeutralLightSurface
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryEmerald
import com.example.ui.theme.SkeletonBaseColor
import com.example.ui.theme.SkeletonHighlightColor
import com.example.ui.theme.SkeletonSubtleColor

/**
 * Animated Shimmer Brush for Skeleton Loading states across Dashboard Widgets, Cards, and Tables.
 * Eliminates harsh empty flashes and preserves exact card geometry while data hydrates.
 */
@Composable
fun rememberScsShimmerBrush(
    baseColor: Color = SkeletonBaseColor,
    highlightColor: Color = SkeletonHighlightColor
): Brush {
    val transition = rememberInfiniteTransition(label = "ScsSkeletonShimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerOffset"
    )

    return Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset(translateAnim - 250f, translateAnim - 250f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * Reusable Skeleton primitive box with rounded corners and animated shimmer sweep.
 */
@Composable
fun ScsSkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    width: Dp? = null,
    cornerRadius: Dp = 8.dp,
    brush: Brush = rememberScsShimmerBrush()
) {
    val sizedModifier = if (width != null) {
        modifier.size(width = width, height = height)
    } else {
        modifier.fillMaxWidth().height(height)
    }
    Box(
        modifier = sizedModifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

/**
 * Reusable Circular Skeleton placeholder for avatars and circular progress rings.
 */
@Composable
fun ScsSkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier,
    brush: Brush = rememberScsShimmerBrush()
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
    )
}

/**
 * 1. Dashboard Greeting Header Skeleton (`DashboardHeroHeaderSkeleton`)
 * Matches the avatar + greeting title + institution subtitle + "+ Öğrenci" button row.
 */
@Composable
fun DashboardHeroHeaderSkeleton(
    modifier: Modifier = Modifier
) {
    val brush = rememberScsShimmerBrush()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = DesignTokens.spacing.xSmall)
            .testTag("skeleton_dashboard_hero_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            ScsSkeletonCircle(size = 38.dp, brush = brush)
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScsSkeletonBox(modifier = Modifier.fillMaxWidth(0.65f), height = 16.dp, cornerRadius = 6.dp, brush = brush)
                ScsSkeletonBox(modifier = Modifier.fillMaxWidth(0.85f), height = 12.dp, cornerRadius = 5.dp, brush = brush)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        ScsSkeletonBox(
            width = 82.dp,
            height = 34.dp,
            cornerRadius = DesignTokens.components.cardRadiusMedium,
            brush = brush
        )
    }
}

/**
 * 2. Quick Shortcut Pill Skeleton (`QuickShortcutPillSkeleton` & `QuickShortcutStripSkeleton`)
 */
@Composable
fun QuickShortcutPillSkeleton(
    width: Dp = 124.dp,
    modifier: Modifier = Modifier
) {
    val brush = rememberScsShimmerBrush()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SkeletonSubtleColor,
        border = BorderStroke(1.dp, NeutralLightBorder.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScsSkeletonBox(width = 17.dp, height = 17.dp, cornerRadius = 5.dp, brush = brush)
            Spacer(modifier = Modifier.width(6.dp))
            ScsSkeletonBox(width = width - 47.dp, height = 12.dp, cornerRadius = 4.dp, brush = brush)
        }
    }
}

@Composable
fun QuickShortcutStripSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("skeleton_quick_shortcut_strip"),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.spacing.pillGap)
    ) {
        QuickShortcutPillSkeleton(width = 112.dp)
        QuickShortcutPillSkeleton(width = 136.dp)
        QuickShortcutPillSkeleton(width = 122.dp)
        QuickShortcutPillSkeleton(width = 128.dp)
        QuickShortcutPillSkeleton(width = 148.dp)
    }
}

/**
 * 3. Bespoke Skeleton for WIDGET 1: Today's Attendance Rate (`TodayAttendanceWidgetSkeleton`)
 * Matches the 42dp icon box, title + "Canlı" pill, subtitle, 46dp circular progress ring, and 4 mini stat chips.
 */
@Composable
fun TodayAttendanceWidgetSkeleton(
    modifier: Modifier = Modifier,
    testTag: String = "skeleton_widget_attendance"
) {
    val brush = rememberScsShimmerBrush()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = NeutralLightSurface,
        border = BorderStroke(1.dp, NeutralLightBorder.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ScsSkeletonBox(width = 42.dp, height = 42.dp, cornerRadius = 12.dp, brush = brush)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ScsSkeletonBox(width = 110.dp, height = 14.dp, cornerRadius = 5.dp, brush = brush)
                            Spacer(modifier = Modifier.width(6.dp))
                            ScsSkeletonBox(width = 34.dp, height = 16.dp, cornerRadius = 6.dp, brush = brush)
                        }
                        ScsSkeletonBox(modifier = Modifier.fillMaxWidth(0.85f), height = 11.dp, cornerRadius = 4.dp, brush = brush)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Circular ring skeleton
                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ScsSkeletonCircle(size = 46.dp, brush = brush)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NeutralLightSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            ScsSkeletonBox(width = 22.dp, height = 10.dp, cornerRadius = 3.dp, brush = brush)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    ScsSkeletonBox(width = 18.dp, height = 18.dp, cornerRadius = 4.dp, brush = brush)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 AttendanceMiniStatChip placeholders (Geldi, Gelmedi, Geç, İzinli)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(4) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = SkeletonSubtleColor
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ScsSkeletonBox(width = 24.dp, height = 14.dp, cornerRadius = 4.dp, brush = brush)
                            ScsSkeletonBox(width = 38.dp, height = 10.dp, cornerRadius = 3.dp, brush = brush)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Bespoke Skeleton for WIDGET 2: Uncollected Fees (`UncollectedFeesWidgetSkeleton`)
 * Matches the 42dp icon box, title + overdue badge, right currency balance, and linear progress bar strip.
 */
@Composable
fun UncollectedFeesWidgetSkeleton(
    modifier: Modifier = Modifier,
    testTag: String = "skeleton_widget_finance"
) {
    val brush = rememberScsShimmerBrush()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = NeutralLightSurface,
        border = BorderStroke(1.dp, NeutralLightBorder.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ScsSkeletonBox(width = 42.dp, height = 42.dp, cornerRadius = 12.dp, brush = brush)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ScsSkeletonBox(width = 144.dp, height = 14.dp, cornerRadius = 5.dp, brush = brush)
                            Spacer(modifier = Modifier.width(6.dp))
                            ScsSkeletonBox(width = 54.dp, height = 16.dp, cornerRadius = 6.dp, brush = brush)
                        }
                        ScsSkeletonBox(width = 176.dp, height = 11.dp, cornerRadius = 4.dp, brush = brush)
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ScsSkeletonBox(width = 72.dp, height = 15.dp, cornerRadius = 5.dp, brush = brush)
                    ScsSkeletonBox(width = 52.dp, height = 10.dp, cornerRadius = 4.dp, brush = brush)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Collection Progress Bar & Summary Strip placeholder
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ScsSkeletonBox(width = 116.dp, height = 11.dp, cornerRadius = 4.dp, brush = brush)
                    ScsSkeletonBox(width = 96.dp, height = 11.dp, cornerRadius = 4.dp, brush = brush)
                }
                ScsSkeletonBox(height = 7.dp, cornerRadius = 4.dp, brush = brush)
            }
        }
    }
}

/**
 * 5. Bespoke Skeleton for WIDGET 3: Daily Cash & Pending Approvals (`DailyCashAndApprovalsWidgetSkeleton`)
 * Matches the 42dp icon box, title + pending badge, right Net Kasa amount, and 2-tab segmented pill bar.
 */
@Composable
fun DailyCashAndApprovalsWidgetSkeleton(
    modifier: Modifier = Modifier,
    testTag: String = "skeleton_widget_approvals"
) {
    val brush = rememberScsShimmerBrush()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = NeutralLightSurface,
        border = BorderStroke(1.dp, NeutralLightBorder.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ScsSkeletonBox(width = 42.dp, height = 42.dp, cornerRadius = 12.dp, brush = brush)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ScsSkeletonBox(width = 136.dp, height = 14.dp, cornerRadius = 5.dp, brush = brush)
                            Spacer(modifier = Modifier.width(6.dp))
                            ScsSkeletonBox(width = 56.dp, height = 16.dp, cornerRadius = 6.dp, brush = brush)
                        }
                        ScsSkeletonBox(width = 172.dp, height = 11.dp, cornerRadius = 4.dp, brush = brush)
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ScsSkeletonBox(width = 64.dp, height = 14.dp, cornerRadius = 5.dp, brush = brush)
                    ScsSkeletonBox(width = 44.dp, height = 10.dp, cornerRadius = 4.dp, brush = brush)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2-Tab Segmented Selector placeholder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SkeletonSubtleColor)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeutralLightSurface,
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ScsSkeletonBox(width = 96.dp, height = 11.dp, cornerRadius = 4.dp, brush = brush)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ScsSkeletonBox(width = 88.dp, height = 11.dp, cornerRadius = 4.dp, brush = brush)
                }
            }
        }
    }
}

/**
 * 6. Bespoke Skeleton for WIDGET 4: Critical Risk & Campus Pulse (`CriticalRiskAndCampusPulseWidgetSkeleton`)
 * Matches the 42dp icon box, title + bus badge, subtitle, and 2 side-by-side status summary boxes.
 */
@Composable
fun CriticalRiskAndCampusPulseWidgetSkeleton(
    modifier: Modifier = Modifier,
    testTag: String = "skeleton_widget_risk"
) {
    val brush = rememberScsShimmerBrush()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = NeutralLightSurface,
        border = BorderStroke(1.dp, NeutralLightBorder.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ScsSkeletonBox(width = 42.dp, height = 42.dp, cornerRadius = 12.dp, brush = brush)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ScsSkeletonBox(width = 142.dp, height = 14.dp, cornerRadius = 5.dp, brush = brush)
                            Spacer(modifier = Modifier.width(6.dp))
                            ScsSkeletonBox(width = 48.dp, height = 16.dp, cornerRadius = 6.dp, brush = brush)
                        }
                        ScsSkeletonBox(width = 184.dp, height = 11.dp, cornerRadius = 4.dp, brush = brush)
                    }
                }
                ScsSkeletonBox(width = 18.dp, height = 18.dp, cornerRadius = 4.dp, brush = brush)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2 Side-by-Side Summary Boxes (Yakın Takip / Risk & Canlı Servis GPS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(2) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SkeletonSubtleColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScsSkeletonCircle(size = 18.dp, brush = brush)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ScsSkeletonBox(width = 68.dp, height = 12.dp, cornerRadius = 4.dp, brush = brush)
                                ScsSkeletonBox(width = 82.dp, height = 10.dp, cornerRadius = 3.dp, brush = brush)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 7. RoleContextualAiCard Skeleton (`RoleContextualAiCardSkeleton`)
 * Matches the AI analysis banner card on the Dashboard and role portals.
 */
@Composable
fun RoleContextualAiCardSkeleton(
    modifier: Modifier = Modifier,
    testTag: String = "skeleton_role_ai_card"
) {
    val brush = rememberScsShimmerBrush()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = NeutralLightSurface,
        border = BorderStroke(1.dp, NeutralLightBorder.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                ScsSkeletonCircle(size = 30.dp, brush = brush)
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScsSkeletonBox(width = 132.dp, height = 13.dp, cornerRadius = 5.dp, brush = brush)
                        Spacer(modifier = Modifier.width(6.dp))
                        ScsSkeletonBox(width = 26.dp, height = 14.dp, cornerRadius = 4.dp, brush = brush)
                    }
                    ScsSkeletonBox(width = 190.dp, height = 11.dp, cornerRadius = 4.dp, brush = brush)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScsSkeletonCircle(size = 30.dp, brush = brush)
                ScsSkeletonBox(width = 64.dp, height = 26.dp, cornerRadius = 14.dp, brush = brush)
            }
        }
    }
}

/**
 * 8. SimplifiedActionTile Skeleton (`SimplifiedActionTileSkeleton`)
 * Matches the 4x2 Quick Action Module tiles on the Dashboard.
 */
@Composable
fun SimplifiedActionTileSkeleton(
    modifier: Modifier = Modifier,
    testTag: String = "skeleton_action_tile"
) {
    val brush = rememberScsShimmerBrush()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = NeutralLightSurface,
        border = BorderStroke(1.dp, NeutralLightBorder.copy(alpha = 0.6f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScsSkeletonBox(width = 38.dp, height = 38.dp, cornerRadius = 10.dp, brush = brush)
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ScsSkeletonBox(width = 84.dp, height = 13.dp, cornerRadius = 4.dp, brush = brush)
                ScsSkeletonBox(width = 68.dp, height = 11.dp, cornerRadius = 4.dp, brush = brush)
            }
        }
    }
}

/**
 * 9. MetricStatCard Skeleton (`MetricStatCardSkeleton`)
 * Matches KPI summary cards across Dashboard and Role Portals.
 */
@Composable
fun MetricStatCardSkeleton(
    modifier: Modifier = Modifier,
    testTag: String = "skeleton_metric_stat_card"
) {
    val brush = rememberScsShimmerBrush()
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).testTag(testTag),
        color = NeutralLightSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, NeutralLightBorder.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScsSkeletonBox(width = 82.dp, height = 12.dp, cornerRadius = 4.dp, brush = brush)
                ScsSkeletonBox(width = 32.dp, height = 32.dp, cornerRadius = 8.dp, brush = brush)
            }
            Spacer(modifier = Modifier.height(10.dp))
            ScsSkeletonBox(width = 64.dp, height = 20.dp, cornerRadius = 6.dp, brush = brush)
            Spacer(modifier = Modifier.height(5.dp))
            ScsSkeletonBox(width = 96.dp, height = 11.dp, cornerRadius = 4.dp, brush = brush)
        }
    }
}

/**
 * 10. AnnouncementCard Skeleton (`AnnouncementCardSkeleton`)
 */
@Composable
fun AnnouncementCardSkeleton(
    modifier: Modifier = Modifier,
    testTag: String = "skeleton_announcement_card"
) {
    val brush = rememberScsShimmerBrush()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(DesignTokens.components.cardRadiusLarge),
        color = NeutralLightSurface,
        border = BorderStroke(1.dp, NeutralLightBorder.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.spacing.cardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScsSkeletonBox(width = 38.dp, height = 38.dp, cornerRadius = 10.dp, brush = brush)
            Spacer(modifier = Modifier.width(DesignTokens.spacing.medium))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScsSkeletonBox(width = 160.dp, height = 14.dp, cornerRadius = 6.dp, brush = brush)
                ScsSkeletonBox(width = 220.dp, height = 11.dp, cornerRadius = 5.dp, brush = brush)
            }
            ScsSkeletonBox(width = 16.dp, height = 16.dp, cornerRadius = 4.dp, brush = brush)
        }
    }
}

/**
 * Generic fallback Skeleton Card for custom widget containers.
 */
@Composable
fun DashboardWidgetSkeletonCard(
    modifier: Modifier = Modifier,
    testTag: String = "dashboard_widget_skeleton_card"
) {
    TodayAttendanceWidgetSkeleton(modifier = modifier, testTag = testTag)
}

/**
 * Full 4-Widget Responsive Skeleton Grid using each widget's bespoke Skeleton anatomy.
 */
@Composable
fun DashboardWidgetsSkeletonGrid(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_widgets_skeleton_grid")
    ) {
        val isWide = maxWidth >= 600.dp
        val gap = DesignTokens.spacing.widgetGridGap

        if (isWide) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    TodayAttendanceWidgetSkeleton(
                        modifier = Modifier.weight(1f),
                        testTag = "skeleton_widget_attendance"
                    )
                    UncollectedFeesWidgetSkeleton(
                        modifier = Modifier.weight(1f),
                        testTag = "skeleton_widget_finance"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    DailyCashAndApprovalsWidgetSkeleton(
                        modifier = Modifier.weight(1f),
                        testTag = "skeleton_widget_approvals"
                    )
                    CriticalRiskAndCampusPulseWidgetSkeleton(
                        modifier = Modifier.weight(1f),
                        testTag = "skeleton_widget_risk"
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                TodayAttendanceWidgetSkeleton(testTag = "skeleton_widget_attendance")
                UncollectedFeesWidgetSkeleton(testTag = "skeleton_widget_finance")
                DailyCashAndApprovalsWidgetSkeleton(testTag = "skeleton_widget_approvals")
                CriticalRiskAndCampusPulseWidgetSkeleton(testTag = "skeleton_widget_risk")
            }
        }
    }
}

/**
 * Custom animated circular loading indicator with dual-ring gradient sweep
 * and center pulse effect for polished modern UX across Login and Data views.
 */
@Composable
fun Okul360CircularLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
    primaryColor: Color = PrimaryBlue,
    secondaryColor: Color = SecondaryEmerald,
    showCenterIcon: Boolean = false,
    label: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SCS360Loader")

    // Rotation angle animation
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    // Pulse scale animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            // Track background ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = strokeWidth.toPx()
                val radius = (this.size.minDimension - strokePx) / 2f
                val center = Offset(this.size.width / 2f, this.size.height / 2f)

                drawCircle(
                    color = primaryColor.copy(alpha = 0.12f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokePx)
                )
            }

            // Animated gradient sweeping arc
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = strokeWidth.toPx()
                val pad = strokePx / 2f
                val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)

                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to primaryColor.copy(alpha = 0.1f),
                        0.5f to secondaryColor,
                        1.0f to primaryColor
                    ),
                    startAngle = angle,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            if (showCenterIcon) {
                Box(
                    modifier = Modifier
                        .size(size * 0.45f * pulseScale)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(size * 0.28f)
                    )
                }
            }
        }

        if (!label.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Full-screen or container loading overlay with smooth semi-transparent backdrop
 * and custom circular progress indicator.
 */
@Composable
fun Okul360LoadingOverlay(
    message: String = "Veriler yükleniyor...",
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Okul360CircularLoadingIndicator(
                        size = 56.dp,
                        strokeWidth = 5.dp,
                        showCenterIcon = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lütfen bekleyiniz",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
