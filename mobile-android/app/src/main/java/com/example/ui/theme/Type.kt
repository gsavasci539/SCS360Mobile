package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp
import com.example.R

// Default platform text style preventing unneeded font padding while keeping Turkish diacritics safe
private val DefaultPlatformStyle = PlatformTextStyle(
    includeFontPadding = false
)

// Clean line breaking for mobile & tablet stability
private val DefaultLineBreak = LineBreak.Simple

/**
 * Bundled Google Fonts installed via font-util:
 * - Plus Jakarta Sans: Institutional display, headlines, section titles, and badges
 * - Inter: High-legibility body copy, tables, financial figures, and form inputs
 */
val ScsDisplayFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Black)
)

val ScsBodyFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

/**
 * Responsive Enterprise Typography Scale (--scs-font-scale-*)
 * Uses a modular ratio with generous line heights (1.3x - 1.48x) for crisp readability.
 */
fun createResponsiveTypography(scale: Float = 1.0f): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (34 * scale).sp,
            lineHeight = (42 * scale).sp,
            letterSpacing = (-0.5).sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        displayMedium = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28 * scale).sp,
            lineHeight = (36 * scale).sp,
            letterSpacing = (-0.35).sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        displaySmall = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24 * scale).sp,
            lineHeight = (31 * scale).sp,
            letterSpacing = (-0.25).sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        headlineLarge = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (22 * scale).sp,
            lineHeight = (29 * scale).sp,
            letterSpacing = (-0.2).sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        headlineMedium = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (19 * scale).sp,
            lineHeight = (25 * scale).sp,
            letterSpacing = (-0.15).sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        headlineSmall = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (17 * scale).sp,
            lineHeight = (23 * scale).sp,
            letterSpacing = (-0.1).sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        titleLarge = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (17 * scale).sp,
            lineHeight = (23 * scale).sp,
            letterSpacing = (-0.1).sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        titleMedium = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15 * scale).sp,
            lineHeight = (21 * scale).sp,
            letterSpacing = 0.sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        titleSmall = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13.5 * scale).sp,
            lineHeight = (19 * scale).sp,
            letterSpacing = 0.05.sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        bodyLarge = TextStyle(
            fontFamily = ScsBodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15 * scale).sp,
            lineHeight = (22 * scale).sp,
            letterSpacing = 0.sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        bodyMedium = TextStyle(
            fontFamily = ScsBodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.05.sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        bodySmall = TextStyle(
            fontFamily = ScsBodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp,
            lineHeight = (17 * scale).sp,
            letterSpacing = 0.1.sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        labelLarge = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13.5f * scale).coerceAtLeast(12.5f).sp,
            lineHeight = (19f * scale).coerceAtLeast(17f).sp,
            letterSpacing = 0.1.sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        labelMedium = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (12f * scale).coerceAtLeast(11.5f).sp,
            lineHeight = (16f * scale).coerceAtLeast(15f).sp,
            letterSpacing = 0.15.sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        ),
        labelSmall = TextStyle(
            fontFamily = ScsDisplayFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * scale).coerceAtLeast(10.5f).sp,
            lineHeight = (15f * scale).coerceAtLeast(14f).sp,
            letterSpacing = 0.2.sp,
            platformStyle = DefaultPlatformStyle,
            lineBreak = DefaultLineBreak
        )
    )
}

// Default Institutional Typography Instance
val Typography = createResponsiveTypography(1.0f)
