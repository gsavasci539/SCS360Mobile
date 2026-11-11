package com.example.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Adaptive Design Metrics (CSS Media Queries Equivalent in Jetpack Compose).
 * Automatically scales paddings, margins, gaps and typography for small viewport
 * devices like Samsung Galaxy A26 (< 380dp) up to tablets (>= 600dp).
 */
data class AdaptiveMetrics(
    val screenWidthDp: Float,
    val isNarrowScreen: Boolean, // < 380dp (e.g. Samsung Galaxy A26, compact phones)
    val isTablet: Boolean,       // >= 600dp
    val screenPadding: Dp,
    val cardPadding: Dp,
    val buttonHorizontalPadding: Dp,
    val buttonVerticalPadding: Dp,
    val gapSmall: Dp,
    val gapMedium: Dp,
    val gapLarge: Dp,
    val fontScaleRatio: Float
)

val LocalAdaptiveMetrics = compositionLocalOf {
    AdaptiveMetrics(
        screenWidthDp = 390f,
        isNarrowScreen = false,
        isTablet = false,
        screenPadding = 16.dp,
        cardPadding = 14.dp,
        buttonHorizontalPadding = 12.dp,
        buttonVerticalPadding = 8.dp,
        gapSmall = 6.dp,
        gapMedium = 12.dp,
        gapLarge = 16.dp,
        fontScaleRatio = 1.0f
    )
}

object AdaptiveTheme {
    val metrics: AdaptiveMetrics
        @Composable
        @ReadOnlyComposable
        get() = LocalAdaptiveMetrics.current

    val screenPadding: Dp
        @Composable
        @ReadOnlyComposable
        get() = LocalAdaptiveMetrics.current.screenPadding

    val cardPadding: Dp
        @Composable
        @ReadOnlyComposable
        get() = LocalAdaptiveMetrics.current.cardPadding

    val isNarrowScreen: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalAdaptiveMetrics.current.isNarrowScreen
}

/**
 * Calculates adaptive metrics based on device configuration (simulating CSS media queries).
 */
@Composable
fun rememberAdaptiveMetrics(availableWidthDp: Float? = null): AdaptiveMetrics {
    val configuration = LocalConfiguration.current
    val screenWidth = availableWidthDp ?: configuration.screenWidthDp.toFloat()
    val fontScale = configuration.fontScale

    return remember(screenWidth, fontScale) {
        when {
            // Samsung Galaxy A26 & Compact/Standard Galaxy Phones (< 400dp)
            screenWidth < 400f -> {
                AdaptiveMetrics(
                    screenWidthDp = screenWidth,
                    isNarrowScreen = true,
                    isTablet = false,
                    screenPadding = 12.dp,
                    cardPadding = 12.dp,
                    buttonHorizontalPadding = 12.dp,
                    buttonVerticalPadding = 8.dp,
                    gapSmall = 4.dp,
                    gapMedium = 8.dp,
                    gapLarge = 12.dp,
                    fontScaleRatio = 1.0f
                )
            }
            // Small Tablets, Foldables & Landscape (600dp - 839dp)
            screenWidth in 600f..839f -> AdaptiveMetrics(
                screenWidthDp = screenWidth,
                isNarrowScreen = false,
                isTablet = true,
                screenPadding = 20.dp,
                cardPadding = 16.dp,
                buttonHorizontalPadding = 14.dp,
                buttonVerticalPadding = 8.dp,
                gapSmall = 6.dp,
                gapMedium = 12.dp,
                gapLarge = 18.dp,
                fontScaleRatio = 1.0f
            )
            // Tablets & Large Screens (>= 840dp)
            screenWidth >= 840f -> AdaptiveMetrics(
                screenWidthDp = screenWidth,
                isNarrowScreen = false,
                isTablet = true,
                screenPadding = 24.dp,
                cardPadding = 18.dp,
                buttonHorizontalPadding = 16.dp,
                buttonVerticalPadding = 10.dp,
                gapSmall = 8.dp,
                gapMedium = 16.dp,
                gapLarge = 24.dp,
                fontScaleRatio = 1.05f
            )
            // Standard Screens (400dp - 599dp)
            else -> AdaptiveMetrics(
                screenWidthDp = screenWidth,
                isNarrowScreen = false,
                isTablet = false,
                screenPadding = 16.dp,
                cardPadding = 14.dp,
                buttonHorizontalPadding = 12.dp,
                buttonVerticalPadding = 8.dp,
                gapSmall = 6.dp,
                gapMedium = 12.dp,
                gapLarge = 16.dp,
                fontScaleRatio = 1.0f
            )
        }
    }
}

// -------------------------------------------------------------
// Modifier Extensions: CSS Flexbox equivalents in Jetpack Compose
// -------------------------------------------------------------

/**
 * Equivalent of 'flex: 1 1 auto' and 'min-width: 0'.
 * Ensures the element flexes and shrinks properly inside Row without overflowing.
 */
fun Modifier.flexShrink(weight: Float = 1f): Modifier =
    this.defaultMinSize(minWidth = 0.dp)

/**
 * Equivalent of 'flex: 1 1 0%' and 'min-width: 0'.
 */
fun Modifier.minWidthZero(): Modifier =
    this.defaultMinSize(minWidth = 0.dp)

/**
 * Equivalent of 'max-width: 100%' with adaptive container constraint (standard 840dp).
 */
fun Modifier.adaptiveContainer(maxWidth: Dp = 840.dp): Modifier =
    this.fillMaxWidth().widthIn(max = maxWidth)

// -------------------------------------------------------------
// Global Adaptive Layout Wrappers
// -------------------------------------------------------------

/**
 * Standardized AppShell Main Content Wrapper.
 * Centers the content horizontally and restricts max width on tablets & large viewports
 * to avoid horizontal layout stretching and unexpected layout shifts.
 */
@Composable
fun AppContentWrapper(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 840.dp,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxSize()
        ) {
            val metrics = rememberAdaptiveMetrics(this.maxWidth.value)
            CompositionLocalProvider(LocalAdaptiveMetrics provides metrics) { content() }
        }
    }
}

/**
 * Responsive Action Row (equivalent to 'display: flex', 'flex-wrap: wrap', 'max-width: 100%').
 * On narrow screens like Samsung A26, items wrap or adapt spacing to prevent clipping.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdaptiveActionRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = AdaptiveTheme.metrics.gapSmall,
    verticalSpacing: Dp = AdaptiveTheme.metrics.gapSmall,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        content()
    }
}

/**
 * Globally protected Text component (implements 'word-break: break-word', 'overflow-wrap: break-word').
 * Prevents long labels or button texts from breaking layouts on narrow viewports.
 */
@Composable
fun AdaptiveText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val metrics = LocalAdaptiveMetrics.current
    val effectiveStyle = if (metrics.isNarrowScreen && fontSize == TextUnit.Unspecified && style.fontSize != TextUnit.Unspecified) {
        style.copy(
            fontSize = (style.fontSize.value * metrics.fontScaleRatio).sp,
            lineHeight = if (style.lineHeight != TextUnit.Unspecified) (style.lineHeight.value * metrics.fontScaleRatio).sp else TextUnit.Unspecified
        )
    } else {
        style
    }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = if (fontSize != TextUnit.Unspecified && metrics.isNarrowScreen) (fontSize.value * metrics.fontScaleRatio).sp else fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = effectiveStyle
    )
}
