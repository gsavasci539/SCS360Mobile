package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlin.math.max

internal class TableViewportState(val scroll: ScrollState) {
    var minimumWidth by mutableStateOf(0.dp)
}
internal val LocalTableViewport = compositionLocalOf<TableViewportState?> { null }

/** One scope per table/list, never shared by unrelated pages or dialogs. */
@Composable
fun ResponsiveTableScope(scopeKey: Any? = Unit, content: @Composable () -> Unit) {
    key(scopeKey) {
        val scroll = rememberScrollState()
        val viewport = remember { TableViewportState(scroll) }
        CompositionLocalProvider(LocalTableViewport provides viewport, content = content)
    }
}

@Composable
internal fun TableScrollRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val viewport = LocalTableViewport.current
    val scroll = viewport?.scroll ?: rememberScrollState()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = maxOf(maxWidth, viewport?.minimumWidth ?: 0.dp)
        Row(
            Modifier.horizontalScroll(scroll).width(width).padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
internal fun TableScrollIndicator() {
    val viewport = LocalTableViewport.current ?: return
    val scroll = viewport.scroll
    if (scroll.maxValue <= 0 || scroll.maxValue == Int.MAX_VALUE) return
    val track = MaterialTheme.colorScheme.outlineVariant
    val thumb = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        val proportion = scroll.viewportSize.toFloat() / (scroll.viewportSize + scroll.maxValue)
        val width = max(size.width * proportion, 24.dp.toPx()).coerceAtMost(size.width)
        val left = (size.width - width) * scroll.value.toFloat() / scroll.maxValue
        drawRoundRect(track, cornerRadius = CornerRadius(size.height))
        drawRoundRect(thumb, Offset(left, 0f), Size(width, size.height), CornerRadius(size.height))
    }
}
