package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Management & Education Design System Shapes
val Okul360Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(14.dp)
)

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = OnPrimaryBlueContainer,
    secondary = SecondaryEmerald,
    onSecondary = Color.White,
    secondaryContainer = SecondaryEmeraldContainer,
    onSecondaryContainer = OnSecondaryEmeraldContainer,
    tertiary = AccentAmber,
    onTertiary = Color.White,
    tertiaryContainer = AccentAmberContainer,
    onTertiaryContainer = OnAccentAmberContainer,
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorContainer,
    onErrorContainer = Color(0xFF991B1B),
    background = NeutralLightBackground,
    onBackground = NeutralTextPrimary,
    surface = NeutralLightSurface,
    onSurface = NeutralTextPrimary,
    surfaceVariant = NeutralLightSurfaceVariant,
    onSurfaceVariant = NeutralTextSecondary,
    outline = NeutralLightBorder,
    outlineVariant = NeutralLightBorder.copy(alpha = 0.6f),
  )

// Primary Color Themes configuration (Institutional Palette)
enum class AppColorTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color
) {
    SCS360(
        id = "SCS360",
        title = "SCS360 Kurumsal Yeşil",
        subtitle = "Petrol Yeşili & Yörünge Altını",
        primaryColor = ScsPetrolGreen,
        primaryContainer = ScsContainerLight,
        onPrimaryContainer = OnScsContainer
    ),
    BLUE(
        id = "BLUE",
        title = "Safir Mavi",
        subtitle = "SCS360 Klasik Kurumsal",
        primaryColor = PrimaryBlue,
        primaryContainer = PrimaryBlueContainer,
        onPrimaryContainer = OnPrimaryBlueContainer
    ),
    EMERALD(
        id = "EMERALD",
        title = "Zümrüt Yeşili",
        subtitle = "Doğa & Prestij Kampüsü",
        primaryColor = SecondaryEmerald,
        primaryContainer = SecondaryEmeraldContainer,
        onPrimaryContainer = OnSecondaryEmeraldContainer
    ),
    INDIGO(
        id = "INDIGO",
        title = "Kraliyet İndigo",
        subtitle = "Bilim & Teknoloji Koleji",
        primaryColor = AccentIndigo,
        primaryContainer = AccentIndigoContainer,
        onPrimaryContainer = OnAccentIndigoContainer
    ),
    BURGUNDY(
        id = "BURGUNDY",
        title = "Asil Bordo",
        subtitle = "Akademi & Fen Lisesi",
        primaryColor = Color(0xFF991B1B),
        primaryContainer = Color(0xFFFEE2E2),
        onPrimaryContainer = Color(0xFF7F1D1D)
    ),
    TEAL(
        id = "TEAL",
        title = "Okyanus Turkuaz",
        subtitle = "Modern İnovasyon Okulu",
        primaryColor = Color(0xFF0284C7),
        primaryContainer = Color(0xFFE0F2FE),
        onPrimaryContainer = Color(0xFF0369A1)
    ),
    AMBER(
        id = "AMBER",
        title = "Güneş Kehribar",
        subtitle = "Gelişim & Anaokulu",
        primaryColor = AccentAmber,
        primaryContainer = AccentAmberContainer,
        onPrimaryContainer = OnAccentAmberContainer
    ),
    CORAL(
        id = "CORAL",
        title = "Mercan & Gül",
        subtitle = "Sanat & Kültür Koleji",
        primaryColor = AccentRose,
        primaryContainer = AccentRoseContainer,
        onPrimaryContainer = OnAccentRoseContainer
    )
}

fun getColorScheme(themeName: String): androidx.compose.material3.ColorScheme {
    val theme = AppColorTheme.entries.find { it.name.equals(themeName, ignoreCase = true) || it.id.equals(themeName, ignoreCase = true) } 
        ?: AppColorTheme.SCS360

    return LightColorScheme.copy(
        primary = theme.primaryColor,
        primaryContainer = theme.primaryContainer,
        onPrimaryContainer = theme.onPrimaryContainer
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun Okul360Theme(
  themeName: String = "SCS360",
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val adaptiveMetrics = rememberAdaptiveMetrics()
  val responsiveTypography = createResponsiveTypography(adaptiveMetrics.fontScaleRatio)

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        dynamicLightColorScheme(context)
      }
      else -> getColorScheme(themeName)
    }

  CompositionLocalProvider(
      LocalAdaptiveMetrics provides adaptiveMetrics,
      LocalSpacingTokens provides DefaultSpacingTokens,
      LocalComponentTokens provides DefaultComponentTokens
  ) {
      MaterialTheme(
          colorScheme = colorScheme,
          typography = responsiveTypography,
          shapes = Okul360Shapes,
          content = content
      )
  }
}

@Composable
fun MyApplicationTheme(
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  Okul360Theme(dynamicColor = dynamicColor, content = content)
}



