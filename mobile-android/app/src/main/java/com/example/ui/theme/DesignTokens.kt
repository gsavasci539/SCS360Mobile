package com.example.ui.theme

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Global Design Tokens for Spacing, Padding, Margins, and Gaps.
 * Mirrors CSS Custom Properties (--scs-space-*) on an 8pt/4pt rhythmic grid
 * for consistent institutional layout density across mobile and tablet viewports.
 */
data class SpacingTokens(
    val none: Dp = 0.dp,
    val xxSmall: Dp = 2.dp,
    val xSmall: Dp = 4.dp,      // --scs-space-1 (4px)
    val small: Dp = 8.dp,       // --scs-space-2 (8px)
    val smMedium: Dp = 10.dp,   // --scs-space-2-5 (10px)
    val medium: Dp = 12.dp,     // --scs-space-3 (12px)
    val mdLarge: Dp = 14.dp,    // --scs-space-3-5 (14px)
    val large: Dp = 16.dp,      // --scs-space-4 (16px)
    val xLarge: Dp = 20.dp,     // --scs-space-5 (20px)
    val xxLarge: Dp = 24.dp,    // --scs-space-6 (24px)
    val xxxLarge: Dp = 32.dp,   // --scs-space-8 (32px)
    val huge: Dp = 48.dp,       // --scs-space-12 (48px)
    // Semantic Layout Tokens
    val screenHorizontal: Dp = 16.dp,
    val screenVertical: Dp = 14.dp,
    val sectionGap: Dp = 16.dp,
    val widgetGridGap: Dp = 12.dp,
    val cardInnerPadding: Dp = 16.dp,
    val pillGap: Dp = 8.dp
) {
    // CSS Variable Scale Aliases (--space-1 .. --space-12)
    val space1: Dp get() = xSmall
    val space2: Dp get() = small
    val space3: Dp get() = medium
    val space4: Dp get() = large
    val space5: Dp get() = xLarge
    val space6: Dp get() = xxLarge
    val space8: Dp get() = xxxLarge
    val space12: Dp get() = huge
}

/**
 * Global Design Tokens for Component Metrics, Radii & Accessibility.
 */
data class ComponentTokens(
    val touchTargetMinimum: Dp = 48.dp,    // Minimum 48dp touch target (WCAG 2.2 AA)
    val buttonFullHeight: Dp = 48.dp,      // Primary full-width / submit / login buttons
    val buttonStandardHeight: Dp = 44.dp,  // Standard form & primary action buttons
    val buttonCompactHeight: Dp = 40.dp,   // Compact card / row action buttons
    val buttonSmallHeight: Dp = 38.dp,     // Inline table / list card action buttons
    val buttonMinHeight: Dp = 38.dp,       // Global accessible button min height
    val iconButtonSize: Dp = 44.dp,        // Standard touch-friendly icon button size
    val iconButtonCompactSize: Dp = 36.dp, // Compact icon button size
    val inputFieldMinHeight: Dp = 48.dp,   // Accessible text input target
    val chipMinHeight: Dp = 36.dp,         // Filter / Status chip height
    val cardRadiusSmall: Dp = 10.dp,
    val cardRadiusMedium: Dp = 14.dp,
    val cardRadiusLarge: Dp = 18.dp,
    val cardRadiusXLarge: Dp = 24.dp,
    val borderStrokeSubtle: Dp = 1.dp,
    val borderStrokeFocus: Dp = 1.5.dp
)

val DefaultSpacingTokens = SpacingTokens()
val DefaultComponentTokens = ComponentTokens()

val LocalSpacingTokens = compositionLocalOf { DefaultSpacingTokens }
val LocalComponentTokens = compositionLocalOf { DefaultComponentTokens }

object DesignTokens {
    val spacing: SpacingTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacingTokens.current

    val components: ComponentTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalComponentTokens.current

    val touchTargetMinimum: Dp = 48.dp
}

// -------------------------------------------------------------
// Interactive Element Modifiers (CSS min-height / touch target rules)
// -------------------------------------------------------------

/**
 * Enforces a minimum touch target size (at least 48dp) for buttons and interactive elements,
 * ensuring high accessibility compliance on mobile touchscreens.
 */
fun Modifier.minTouchTarget(
    minWidth: Dp = DesignTokens.touchTargetMinimum,
    minHeight: Dp = DesignTokens.touchTargetMinimum
): Modifier = this.defaultMinSize(
    minWidth = minWidth,
    minHeight = minHeight
)

/**
 * Box sizing normalization: ensures elements constrain their inner content properly
 * mimicking 'box-sizing: border-box'.
 */
fun Modifier.borderBox(padding: Dp = 0.dp): Modifier =
    if (padding > 0.dp) this.padding(padding) else this

/**
 * Standard Accessible Button Size Modifiers
 */
fun Modifier.buttonFull(): Modifier =
    this.defaultMinSize(minHeight = 48.dp)

fun Modifier.buttonStandard(): Modifier =
    this.defaultMinSize(minHeight = 44.dp)

fun Modifier.buttonCompact(): Modifier =
    this.defaultMinSize(minHeight = 40.dp)

fun Modifier.buttonSmall(): Modifier =
    this.defaultMinSize(minHeight = 38.dp)
