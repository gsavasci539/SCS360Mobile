package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ResponsiveLayoutTest {
    @get:Rule val compose = createComposeRule()
    private data class Window(val width: Int, val height: Int, val font: Float)
    private val windows = listOf(320 to 780, 360 to 780, 393 to 852, 412 to 915, 600 to 960, 780 to 360, 840 to 1100, 1280 to 800)
        .flatMap { (w, h) -> listOf(1f, 1.5f, 2f).map { Window(w, h, it) } }
    private val columns = listOf(TableColumnSpec("Öğrenci / Numara", 1.4f), TableColumnSpec("Sınıf ve veli", 1.2f),
        TableColumnSpec("Durum", .75f), TableColumnSpec("Hızlı işlemler", 1.15f, minWidth = 180.dp))

    @OptIn(ExperimentalTestApi::class)
    @Composable private fun WindowHost(window: Window, content: @Composable () -> Unit) {
        DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(window.width.dp, window.height.dp)) then
            DeviceConfigurationOverride.FontScale(window.font)) {
            Okul360Theme { AppContentWrapper { content() } }
        }
    }

    @Test fun tableColumnsStayAlignedAndScrollTogetherAcross24WindowAndFontCombinations() {
        var window by mutableStateOf(windows.first())
        compose.setContent {
            WindowHost(window) {
                key(window) {
                    ResponsiveTableScope {
                        Column(Modifier.fillMaxWidth().testTag("page_viewport")) {
                            val mode = rememberResolvedTableMode(DataTableViewMode.TABLE)
                            StickyTableHeaderRow(columns, 123456, mode)
                            CompactTableRow {
                                columns.forEachIndexed { i, col -> Text("Uzun içerik $i", Modifier.weight(col.weight).testTag("cell_$i"), maxLines = 1) }
                            }
                        }
                    }
                }
            }
        }
        for (candidate in windows) {
            compose.runOnIdle { window = candidate }
            compose.waitForIdle()
            val pageBefore = compose.onNodeWithTag("page_viewport").getUnclippedBoundsInRoot()
            val firstBefore = compose.onNodeWithTag("table_heading_0").getUnclippedBoundsInRoot().left.value
            fun aligned() {
                columns.indices.forEach { i ->
                    val heading = compose.onNodeWithTag("table_heading_$i").getUnclippedBoundsInRoot()
                    val cell = compose.onNodeWithTag("cell_$i").getUnclippedBoundsInRoot()
                    assertEquals("$candidate column $i left", heading.left.value, cell.left.value, 1f)
                    assertEquals("$candidate column $i width", (heading.right - heading.left).value, (cell.right - cell.left).value, 1f)
                }
            }
            aligned()
            compose.onNodeWithTag("table_row_viewport_0").performTouchInput { swipeLeft() }
            compose.waitForIdle()
            aligned()
            val pageAfter = compose.onNodeWithTag("page_viewport").getUnclippedBoundsInRoot()
            assertEquals("The page must not pan with the table: $candidate", pageBefore, pageAfter)
            val firstAfter = compose.onNodeWithTag("table_heading_0").getUnclippedBoundsInRoot().left.value
            if (candidate.width < 780 || candidate.font > 1f) assertTrue("Table must scroll at $candidate", firstAfter < firstBefore)
        }
    }

    @Test fun automaticCardsFollowActualContentWidthAndLargeFontsAcross24Combinations() {
        var window by mutableStateOf(windows.first())
        var contentWidth = 0f
        compose.setContent { WindowHost(window) {
            val density = LocalDensity.current
            Box(Modifier.fillMaxWidth().onGloballyPositioned { contentWidth = it.size.width / density.density }) {
                Text(if (isCompactCardMode(DataTableViewMode.AUTO)) "CARD" else "TABLE", Modifier.testTag("automatic_mode"))
            }
        } }
        for (candidate in windows) {
            compose.runOnIdle { window = candidate }
            compose.waitForIdle()
            assertEquals("Content must stay capped and fit its window: $candidate", minOf(candidate.width, 840).toFloat(), contentWidth, 2f)
            val card = minOf(candidate.width, 840) / candidate.font < 600
            compose.onNodeWithTag("automatic_mode").assertTextEquals(if (card) "CARD" else "TABLE")
        }
    }
}
