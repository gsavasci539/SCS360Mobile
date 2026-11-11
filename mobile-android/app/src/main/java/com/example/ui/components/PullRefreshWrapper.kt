package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern pull-to-refresh container for list-based screens in SCS360.
 * Allows users to manually drag down to trigger data synchronization with tactile haptic feedback.
 */
@Composable
fun PullRefreshWrapper(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var dragOffset by remember { mutableStateOf(0f) }
    val refreshThreshold = 180f

    val animatedOffset by animateFloatAsState(
        targetValue = if (isRefreshing) 120f else dragOffset.coerceIn(0f, 240f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pullOffset"
    )

    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshing) 720f else (dragOffset * 2f).coerceIn(0f, 360f),
        animationSpec = if (isRefreshing) infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ) else spring(),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isRefreshing) {
                if (!isRefreshing) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (dragAmount > 0 || dragOffset > 0) {
                                dragOffset = (dragOffset + dragAmount * 0.5f).coerceAtLeast(0f)
                                if (dragOffset >= refreshThreshold && dragOffset - dragAmount * 0.5f < refreshThreshold) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        onDragEnd = {
                            if (dragOffset >= refreshThreshold) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRefresh()
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            dragOffset = 0f
                        }
                    )
                }
            }
    ) {
        // Main content shifted down slightly when pulled
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (animatedOffset * 0.25f).dp)
        ) {
            content()
        }

        // Pull to refresh bubble indicator
        if (animatedOffset > 10f || isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (animatedOffset * 0.4f).dp)
                    .size(44.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Yenilemek için Çekin",
                        tint = if (dragOffset >= refreshThreshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotationAngle)
                    )
                }
            }
        }
    }
}
