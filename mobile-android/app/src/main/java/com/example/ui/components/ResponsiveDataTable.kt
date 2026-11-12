package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.LocalAdaptiveMetrics
import com.example.ui.theme.PrimaryBlue

/**
 * Column specification for sticky table headers and proportional responsive rows.
 * Shared minimum column widths preserve alignment in a bounded table viewport.
 */
data class TableColumnSpec(
    val title: String,
    val weight: Float = 1f,
    val alignment: TextAlign = TextAlign.Start,
    val minWidth: Dp = 128.dp
)

/**
 * Responsive display mode for data tables across the application:
 * - AUTO: Automatically selects CARD on mobile phones (< 600dp) to eliminate horizontal scrolling,
 *         and TABLE on tablets / wide screens (>= 600dp).
 * - CARD: High-density compact card layout tailored for vertical mobile screens.
 * - TABLE: Multi-column tabular layout with reduced cell padding and zebra striping.
 */
enum class DataTableViewMode {
    AUTO,
    CARD,
    TABLE
}

/**
 * Resolves whether the current table should render in Compact Card mode (true on mobile by default)
 * or Compact Table mode (true on tablets or when toggled by user).
 */
@Composable
fun rememberResolvedTableMode(
    initialMode: DataTableViewMode = DataTableViewMode.AUTO
): MutableState<DataTableViewMode> {
    return remember { mutableStateOf(initialMode) }
}

@Composable
fun isCompactCardMode(mode: DataTableViewMode): Boolean {
    val metrics = LocalAdaptiveMetrics.current
    return when (mode) {
        DataTableViewMode.CARD -> true
        DataTableViewMode.TABLE -> false
        DataTableViewMode.AUTO -> metrics.screenWidthDp / LocalDensity.current.fontScale.coerceAtLeast(1f) < 600f
    }
}

/**
 * Sticky Column Header Bar designed to be placed inside `LazyColumn { stickyHeader { ... } }`.
 * Pins column titles to the top of the viewport while scrolling, displays total record count,
 * and allows switching between mobile cards and a separately scrollable table.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StickyTableHeaderRow(
    columns: List<TableColumnSpec>,
    totalCount: Int? = null,
    viewModeState: MutableState<DataTableViewMode>? = null,
    modifier: Modifier = Modifier
) {
    val isCard = isCompactCardMode(viewModeState?.value ?: DataTableViewMode.AUTO)
    val viewport = LocalTableViewport.current
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val weight = columns.sumOf { it.weight.toDouble() }.toFloat()
    val minimum = ((columns.maxOfOrNull { it.minWidth.value / it.weight } ?: 0f) * weight * fontScale +
        16f + (columns.size - 1).coerceAtLeast(0) * 6f).dp
    SideEffect { viewport?.minimumWidth = minimum }
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp, shadowElevation = 2.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))) {
        Column(Modifier.fillMaxWidth()) {
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (totalCount != null) Text("$totalCount kayıt", modifier = Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                if (viewModeState != null) {
                    OutlinedButton(modifier = Modifier.testTag("table_view_toggle"),
                        onClick = { viewModeState.value = if (isCard) DataTableViewMode.TABLE else DataTableViewMode.CARD }) {
                        Icon(if (isCard) Icons.Default.TableRows else Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isCard) "Tabloya geç" else "Kartlara geç", maxLines = 2)
                    }
                }
            }
            if (!isCard) {
                TableScrollRow(Modifier.background(BrandNavy.copy(alpha = 0.045f)).testTag("table_header_viewport")) {
                    columns.forEachIndexed { index, col ->
                        Text(col.title, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = BrandNavy.copy(alpha = 0.85f), textAlign = col.alignment,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(col.weight).testTag("table_heading_$index"))
                    }
                }
                TableScrollIndicator()
                if (viewport != null && viewport.scroll.maxValue in 1 until Int.MAX_VALUE) {
                    Text("Tüm sütunlar için tabloyu yana kaydırın", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Compact Table Row container for TABLE mode (with tight cell padding, zebra striping, and hairline border).
 * Columns use the same weights, padding and scroll position as the header.
 */
@Composable
fun CompactTableRow(
    index: Int = 0,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val bgColor = if (index % 2 == 1) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        TableScrollRow(modifier = Modifier.testTag("table_row_viewport_$index"), content = content)
    }
}

/**
 * Compact Responsive Card container for Mobile CARD mode.
 * Replaces oversized cards and horizontal scrolling tables on mobile with a high-density,
 * structured card layout with minimal padding (`horizontal = 10.dp, vertical = 8.dp`).
 */
@Composable
fun CompactResponsiveCard(
    onClick: (() -> Unit)? = null,
    accentColor: Color? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 0.5.dp,
        border = BorderStroke(
            width = 0.8.dp,
            color = accentColor?.copy(alpha = 0.28f) ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            content = content
        )
    }
}

/**
 * Compact key-value metadata pill used inside mobile responsive cards to show table columns
 * side-by-side without horizontal scrolling.
 */
@Composable
fun CompactMetaCell(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 11.sp
        )
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

/**
 * Compact icon action button (30.dp visual pill with accessible hit target) for dense table/card rows.
 */
@Composable
fun CompactRowActionIcon(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    containerColor: Color = tint.copy(alpha = 0.12f),
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(7.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Surface(modifier = Modifier.size(30.dp), color = containerColor, shape = RoundedCornerShape(7.dp)) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else tint.copy(alpha = .38f),
                modifier = Modifier.size(15.dp)
            )
          }
        }
    }
}
