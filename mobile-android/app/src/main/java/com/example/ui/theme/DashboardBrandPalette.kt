package com.example.ui.theme

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Institutional Brand Color Palette for SCS360 Dashboard Widgets.
 * Enables customizing widget accent colors, gradient headers, badges and chart tones
 * to match the school / institution brand guidelines.
 */
data class DashboardBrandPalette(
    val id: String,
    val name: String,
    val schoolType: String,
    val primary: Color,
    val primaryDark: Color,
    val secondary: Color,
    val accent: Color,
    val containerLight: Color,
    val containerBorder: Color,
    val onPrimary: Color = Color.White
) {
    val gradient: Brush
        get() = Brush.linearGradient(listOf(primary, secondary))

    val softGradient: Brush
        get() = Brush.linearGradient(listOf(primary.copy(alpha = 0.15f), secondary.copy(alpha = 0.05f)))

    companion object {
        val CLASSIC_NAVY = DashboardBrandPalette(
            id = "navy",
            name = "Kurumsal Lacivert",
            schoolType = "Köklü Kolejler & Üniversiteler",
            primary = Color(0xFF005A9C),
            primaryDark = Color(0xFF003E6B),
            secondary = Color(0xFF3B82F6),
            accent = Color(0xFF60A5FA),
            containerLight = Color(0xFFEFF6FF),
            containerBorder = Color(0xFFBFDBFE)
        )

        val EMERALD_GREEN = DashboardBrandPalette(
            id = "emerald",
            name = "Zümrüt Doğa",
            schoolType = "Doğa & Çevre Odaklı Kampüsler",
            primary = Color(0xFF047857),
            primaryDark = Color(0xFF064E3B),
            secondary = Color(0xFF10B981),
            accent = Color(0xFF34D399),
            containerLight = Color(0xFFECFDF5),
            containerBorder = Color(0xFFA7F3D0)
        )

        val ROYAL_PURPLE = DashboardBrandPalette(
            id = "purple",
            name = "Kraliyet Moru",
            schoolType = "Akademi & Sanat Kolejleri",
            primary = Color(0xFF6D28D9),
            primaryDark = Color(0xFF4C1D95),
            secondary = Color(0xFF8B5CF6),
            accent = Color(0xFFA78BFA),
            containerLight = Color(0xFFF5F3FF),
            containerBorder = Color(0xFFDDD6FE)
        )

        val BURGUNDY_WINE = DashboardBrandPalette(
            id = "burgundy",
            name = "Asil Bordo",
            schoolType = "Fen Liseleri & Özel Enstitüler",
            primary = Color(0xFF9F1239),
            primaryDark = Color(0xFF700C26),
            secondary = Color(0xFFE11D48),
            accent = Color(0xFFFB7185),
            containerLight = Color(0xFFFFF1F2),
            containerBorder = Color(0xFFFECDD3)
        )

        val AMBER_TERRACOTTA = DashboardBrandPalette(
            id = "amber",
            name = "Sıcak Kehribar",
            schoolType = "Teknoloji & İnovasyon Kampüsleri",
            primary = Color(0xFFC2410C),
            primaryDark = Color(0xFF7C2D12),
            secondary = Color(0xFFF97316),
            accent = Color(0xFFFBBF24),
            containerLight = Color(0xFFFFF7ED),
            containerBorder = Color(0xFFFED7AA)
        )

        val OCEAN_TEAL = DashboardBrandPalette(
            id = "teal",
            name = "Okyanus Teal",
            schoolType = "Uluslararası & Dil Okulları",
            primary = Color(0xFF0F766E),
            primaryDark = Color(0xFF134E4A),
            secondary = Color(0xFF14B8A6),
            accent = Color(0xFF2DD4BF),
            containerLight = Color(0xFFF0FDFA),
            containerBorder = Color(0xFF99F6E4)
        )

        val MIDNIGHT_SLATE = DashboardBrandPalette(
            id = "midnight",
            name = "Gece Mavisi",
            schoolType = "Kurumsal Yönetim & Prestij",
            primary = Color(0xFF1E293B),
            primaryDark = Color(0xFF0F172A),
            secondary = Color(0xFF475569),
            accent = Color(0xFF64748B),
            containerLight = Color(0xFFF8FAFC),
            containerBorder = Color(0xFFCBD5E1)
        )

        val RUBY_RED = DashboardBrandPalette(
            id = "ruby",
            name = "Yakut Kırmızı",
            schoolType = "Spor & Liderlik Kolejleri",
            primary = Color(0xFFB91C1C),
            primaryDark = Color(0xFF7F1D1D),
            secondary = Color(0xFFEF4444),
            accent = Color(0xFFF87171),
            containerLight = Color(0xFFFEF2F2),
            containerBorder = Color(0xFFFECACA)
        )

        val PRESET_PALETTES = listOf(
            CLASSIC_NAVY,
            EMERALD_GREEN,
            ROYAL_PURPLE,
            BURGUNDY_WINE,
            AMBER_TERRACOTTA,
            OCEAN_TEAL,
            MIDNIGHT_SLATE,
            RUBY_RED
        )

        fun defaultPalette(): DashboardBrandPalette = CLASSIC_NAVY

        fun fromHex(hexString: String, name: String = "Özel Kurum Rengi"): DashboardBrandPalette {
            val cleanHex = hexString.trim().removePrefix("#")
            val color = try {
                val fullHex = if (cleanHex.length == 6) "FF$cleanHex" else cleanHex
                Color(fullHex.toLong(16))
            } catch (e: Exception) {
                CLASSIC_NAVY.primary
            }

            // Derive secondary (slightly lighter/warmer) and container colors
            val red = color.red
            val green = color.green
            val blue = color.blue

            val secondary = Color(
                red = (red * 0.75f + 0.25f).coerceIn(0f, 1f),
                green = (green * 0.75f + 0.25f).coerceIn(0f, 1f),
                blue = (blue * 0.75f + 0.25f).coerceIn(0f, 1f),
                alpha = 1f
            )

            val accent = Color(
                red = (red * 0.6f + 0.4f).coerceIn(0f, 1f),
                green = (green * 0.6f + 0.4f).coerceIn(0f, 1f),
                blue = (blue * 0.6f + 0.4f).coerceIn(0f, 1f),
                alpha = 1f
            )

            val primaryDark = Color(
                red = (red * 0.7f).coerceIn(0f, 1f),
                green = (green * 0.7f).coerceIn(0f, 1f),
                blue = (blue * 0.7f).coerceIn(0f, 1f),
                alpha = 1f
            )

            return DashboardBrandPalette(
                id = "custom_${cleanHex.lowercase()}",
                name = name,
                schoolType = "Özel Kurumsal Renk (#${cleanHex.uppercase()})",
                primary = color,
                primaryDark = primaryDark,
                secondary = secondary,
                accent = accent,
                containerLight = color.copy(alpha = 0.08f),
                containerBorder = color.copy(alpha = 0.25f)
            )
        }

        fun getPaletteById(id: String, customHex: String? = null): DashboardBrandPalette {
            if (id.startsWith("custom") && !customHex.isNullOrBlank()) {
                return fromHex(customHex)
            }
            return PRESET_PALETTES.firstOrNull { it.id == id } ?: defaultPalette()
        }
    }
}

val LocalDashboardBrandPalette = compositionLocalOf { DashboardBrandPalette.defaultPalette() }

/**
 * Color picker dialog allowing institution administrators to customize
 * the visual color identity of all Dashboard widgets.
 */
@Composable
fun DashboardBrandColorPickerDialog(
    currentPalette: DashboardBrandPalette,
    onPaletteSelected: (DashboardBrandPalette) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf(currentPalette.id) }
    var customHexText by rememberSaveable {
        mutableStateOf(
            if (currentPalette.id.startsWith("custom")) {
                String.format("#%06X", 0xFFFFFF and currentPalette.primary.toArgb())
            } else "#005A9C"
        )
    }
    var isCustomActive by rememberSaveable { mutableStateOf(currentPalette.id.startsWith("custom")) }

    // Real-time preview palette
    val previewPalette = remember(selectedId, customHexText, isCustomActive) {
        if (isCustomActive) {
            DashboardBrandPalette.fromHex(customHexText, "Seçilen Özel Kurum Rengi")
        } else {
            DashboardBrandPalette.PRESET_PALETTES.firstOrNull { it.id == selectedId } ?: currentPalette
        }
    }

    val quickColors = listOf(
        "#005A9C" to "Kurumsal Mavi",
        "#1E3A8A" to "Derin Lacivert",
        "#0D9488" to "Teal",
        "#059669" to "Zümrüt",
        "#16A34A" to "Yeşil",
        "#65A30D" to "Fıstık",
        "#CA8A04" to "Altın Sarısı",
        "#D97706" to "Kehribar",
        "#EA580C" to "Turuncu",
        "#DC2626" to "Kırmızı",
        "#BE123C" to "Bordo",
        "#7C3AED" to "Mor",
        "#9333EA" to "Eflatun",
        "#C026D3" to "Fuşya",
        "#DB2777" to "Pembe",
        "#0284C7" to "Gök Mavisi",
        "#334155" to "Grafit",
        "#0F172A" to "Gece Mavisi"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_brand_color_picker_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(previewPalette.gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Kurumsal Renk Paleti",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Dashboard widget'larını kurumunuzun kurumsal kimliğine uyarlayın",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. LIVE PREVIEW CARD (Canlı Önizleme Kartı)
                Text(
                    text = "CANLI WIDGET ÖNİZLEMESİ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = previewPalette.primary,
                    letterSpacing = 0.8.sp
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.2.dp, previewPalette.containerBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(previewPalette.gradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Kurum Performansı",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = previewPalette.name,
                                        fontSize = 10.5.sp,
                                        color = previewPalette.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = previewPalette.containerLight
                            ) {
                                Text(
                                    text = "%96 Başarı",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = previewPalette.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mini progress bar styled with selected palette
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(previewPalette.containerLight)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(previewPalette.gradient)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aktif Tema: ${previewPalette.schoolType}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = previewPalette.primary,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = "Aksiyon Butonu",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 2. PRESET PALETTES (Hazır Kurumsal Temalar)
                Text(
                    text = "HAZIR KURUMSAL PALETLER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DashboardBrandPalette.PRESET_PALETTES.forEach { palette ->
                        val isSelected = !isCustomActive && selectedId == palette.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) palette.containerLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) palette.primary else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    isCustomActive = false
                                    selectedId = palette.id
                                }
                                .testTag("palette_preset_${palette.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Palette Color Swatches Circle
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(palette.gradient)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = palette.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) palette.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = palette.schoolType,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Seçildi",
                                        tint = palette.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 3. CUSTOM COLOR PICKER (Özel Kurum Rengi & Hex Girişi)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ÖZEL KURUM HEX RENK KODU",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    if (isCustomActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = previewPalette.containerLight
                        ) {
                            Text(
                                text = "Özel Aktif",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = previewPalette.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Quick Color Swatches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickColors.forEach { (hex, title) ->
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                        val isPicked = isCustomActive && customHexText.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isPicked) 2.5.dp else 1.dp,
                                    color = if (isPicked) MaterialTheme.colorScheme.primary else Color.White,
                                    shape = CircleShape
                                )
                                .clickable {
                                    isCustomActive = true
                                    customHexText = hex
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPicked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = title,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Hex text input field
                OutlinedTextField(
                    value = customHexText,
                    onValueChange = { input ->
                        val formatted = if (!input.startsWith("#") && input.isNotBlank()) "#$input" else input
                        customHexText = formatted.take(7)
                        isCustomActive = true
                    },
                    label = { Text("Kurumsal Renk Kodu (Örn: #005A9C veya #800000)") },
                    leadingIcon = {
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(customHexText))
                        } catch (e: Exception) {
                            Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(parsedColor)
                                .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        )
                    },
                    trailingIcon = {
                        if (isCustomActive) {
                            IconButton(onClick = {
                                isCustomActive = false
                                selectedId = DashboardBrandPalette.CLASSIC_NAVY.id
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Sıfırla", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_hex_color_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPaletteSelected(previewPalette)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = previewPalette.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("apply_brand_palette_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Uygula ve Kaydet", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onPaletteSelected(DashboardBrandPalette.defaultPalette())
                        onDismiss()
                    }
                ) {
                    Text("Varsayılan")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismiss) {
                    Text("Vazgeç")
                }
            }
        }
    )
}
