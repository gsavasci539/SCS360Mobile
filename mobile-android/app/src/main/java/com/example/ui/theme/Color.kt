package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// SCS360 Institutional Color System & WCAG AA/AAA High-Contrast Tokens
// Equivalent to Enterprise CSS Custom Properties (--scs-color-*)
// ============================================================================

// 1. Brand Primary & Sapphire / Petrol Tones (WCAG AAA on Light Surfaces)
val BrandNavy = Color(0xFF0B1324)               // --scs-brand-navy (17.4:1 contrast on white)
val PrimaryBlue = Color(0xFF005A9C)             // --scs-primary-blue (7.2:1 contrast on white)
val PrimaryBlueVariant = Color(0xFF004477)      // --scs-primary-blue-hover (9.8:1)
val PrimaryBlueLight = Color(0xFF1D4ED8)        // --scs-primary-blue-accent (6.7:1)
val PrimaryBlueContainer = Color(0xFFD8E9FF)    // --scs-primary-container
val OnPrimaryBlueContainer = Color(0xFF001B33)  // --scs-on-primary-container (15.8:1)

// 2. Secondary Emerald & Teal Accent (Success, Attendance, Health)
val SecondaryEmerald = Color(0xFF0B7A70)        // --scs-secondary-emerald (5.4:1 contrast on white)
val SecondaryEmeraldVariant = Color(0xFF0F5E56) // --scs-secondary-emerald-dark (7.8:1)
val SecondaryEmeraldContainer = Color(0xFFCCFBF1)
val OnSecondaryEmeraldContainer = Color(0xFF083B37)

// 3. Tertiary & Warm Semantic Accents (WCAG AA+ Verified)
val AccentAmber = Color(0xFFB45309)             // --scs-accent-amber (5.1:1 contrast on white)
val AccentAmberContainer = Color(0xFFFEF3C7)
val OnAccentAmberContainer = Color(0xFF5C2804)

val AccentRose = Color(0xFFBE123C)              // --scs-accent-rose (6.4:1 contrast on white)
val AccentRoseContainer = Color(0xFFFFE4E6)
val OnAccentRoseContainer = Color(0xFF690B22)

val AccentIndigo = Color(0xFF4338CA)            // --scs-accent-indigo (7.9:1 contrast on white)
val AccentIndigoContainer = Color(0xFFE0E7FF)
val OnAccentIndigoContainer = Color(0xFF1E1B4B)

// 4. Institutional Management Semantic Status Colors
val StatusSuccess = Color(0xFF047857)           // --scs-status-success (5.6:1)
val StatusSuccessContainer = Color(0xFFDCFCE7)
val StatusError = Color(0xFFB91C1C)             // --scs-status-error (6.5:1)
val StatusErrorContainer = Color(0xFFFEE2E2)
val StatusWarning = Color(0xFFB45309)           // --scs-status-warning (5.1:1)
val StatusWarningContainer = Color(0xFFFEF3C7)
val StatusInfo = Color(0xFF0369A1)              // --scs-status-info (6.1:1)
val StatusInfoContainer = Color(0xFFE0F2FE)

// 5. Attendance Semantic Colors
val AttendancePresent = Color(0xFF047857)
val AttendanceAbsent = Color(0xFFB91C1C)
val AttendanceLate = Color(0xFFB45309)
val AttendanceExcused = Color(0xFF1D4ED8)

// 6. Finance Semantic Colors
val FinanceCollected = Color(0xFF047857)
val FinancePending = Color(0xFFB45309)
val FinanceOverdue = Color(0xFFB91C1C)
val FinanceBalance = Color(0xFF4338CA)

// 7. Surface, Elevation & Text Hierarchy (WCAG AA/AAA Contrast Calibrated)
val NeutralLightBackground = Color(0xFFF4F7FB)      // --scs-bg-canvas
val NeutralLightSurface = Color(0xFFFFFFFF)         // --scs-surface-0
val NeutralLightSurfaceVariant = Color(0xFFEEF2F7)  // --scs-surface-variant
val SurfaceContainerLowest = Color(0xFFFFFFFF)      // --scs-surface-lowest
val SurfaceContainerLow = Color(0xFFF8FAFC)         // --scs-surface-low
val SurfaceContainer = Color(0xFFF1F5F9)            // --scs-surface-container
val SurfaceContainerHigh = Color(0xFFE2E8F0)        // --scs-surface-high
val NeutralLightBorder = Color(0xFFCBD5E1)          // --scs-border-subtle (Improved contrast)
val NeutralLightBorderStrong = Color(0xFF94A3B8)    // --scs-border-strong

// Text Contrast Ratios on #FFFFFF:
// NeutralTextPrimary (#0B1324)   -> 17.4:1 (WCAG AAA)
// NeutralTextSecondary (#334155) -> 10.1:1 (WCAG AAA)
// NeutralTextMuted (#526277)     ->  6.2:1 (WCAG AA+)
val NeutralTextPrimary = Color(0xFF0B1324)
val NeutralTextSecondary = Color(0xFF334155)
val NeutralTextMuted = Color(0xFF526277)

// Skeleton Shimmer Palette (--scs-skeleton-*)
val SkeletonBaseColor = Color(0xFFE2E8F0)
val SkeletonHighlightColor = Color(0xFFF8FAFC)
val SkeletonSubtleColor = Color(0xFFEDF2F7)

// Module Semantic Theme Constants
val EmeraldPrimary = Color(0xFF047857)
val AmberPrimary = Color(0xFFB45309)
val CrimsonPrimary = Color(0xFFB91C1C)
val CrimsonAlert = Color(0xFFB91C1C)
val OceanPrimary = Color(0xFF0369A1)

val StatusGreen = Color(0xFF047857)
val StatusRed = Color(0xFFB91C1C)
val NeutralGrayDark = Color(0xFF475569)
val NeutralGrayLight = Color(0xFFF1F5F9)
val BackgroundLight = Color(0xFFF4F7FB)
val AccentTeal = Color(0xFF0B7A70)

// 8. Official SCS360 Brand Palette (SCS360_MARKA.md)
val ScsPetrolGreen = Color(0xFF143636)          // --scs-brand-petrol (13.2:1 on white)
val ScsAccentGreen = Color(0xFF146354)          // --scs-brand-emerald (7.1:1 on white)
val ScsOrbitGold = Color(0xFFE5A94E)            // --scs-brand-orbit-gold
val ScsMintPage = Color(0xFF84CBB4)
val ScsCreamPage = Color(0xFFFBF7E9)
val ScsContainerLight = Color(0xFFDDF3EC)
val OnScsContainer = Color(0xFF0A2424)
