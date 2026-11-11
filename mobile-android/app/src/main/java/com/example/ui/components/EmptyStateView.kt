package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueContainer
import com.example.ui.theme.SecondaryEmerald

/**
 * Reusable, modern Material 3 Empty State Component with clear visual hierarchy,
 * contextual icon illustration, explanatory text, and primary/secondary action buttons.
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconColor: Color = PrimaryBlue,
    primaryActionLabel: String? = null,
    primaryActionIcon: ImageVector? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    secondaryActionIcon: ImageVector? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon illustration container with soft gradient circle
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    iconColor.copy(alpha = 0.16f),
                                    iconColor.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                if (primaryActionLabel != null && onPrimaryAction != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onPrimaryAction,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iconColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        if (primaryActionIcon != null) {
                            Icon(
                                imageVector = primaryActionIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = primaryActionLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (secondaryActionIcon != null) {
                            Icon(
                                imageVector = secondaryActionIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = secondaryActionLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pre-configured empty state for Student List
 */
@Composable
fun StudentEmptyState(
    isSearchingOrFiltering: Boolean,
    onResetFilters: () -> Unit,
    onAddStudent: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSearchingOrFiltering) {
        EmptyStateView(
            icon = Icons.Outlined.People,
            title = "Aramaya Uygun Öğrenci Bulunamadı",
            description = "Girdiğiniz arama terimi veya seçili sınıf filtresine uygun öğrenci bulunamadı. Filtreleri temizleyebilir veya yeni öğrenci kaydı oluşturabilirsiniz.",
            iconColor = PrimaryBlue,
            primaryActionLabel = "Filtreleri Temizle",
            primaryActionIcon = Icons.Default.Refresh,
            onPrimaryAction = onResetFilters,
            secondaryActionLabel = "Yeni Öğrenci Ekle",
            secondaryActionIcon = Icons.Default.Add,
            onSecondaryAction = onAddStudent,
            modifier = modifier
        )
    } else {
        EmptyStateView(
            icon = Icons.Outlined.People,
            title = "Henüz Öğrenci Kaydı Bulunmuyor",
            description = "Okulunuza yeni öğrenci ekleyerek sınıf ataması, günlük yoklama ve aidat takibini hemen başlatabilirsiniz.",
            iconColor = PrimaryBlue,
            primaryActionLabel = "Yeni Öğrenci Ekle",
            primaryActionIcon = Icons.Default.Add,
            onPrimaryAction = onAddStudent,
            modifier = modifier
        )
    }
}

/**
 * Pre-configured empty state for Announcement Screen
 */
@Composable
fun AnnouncementEmptyState(
    isSearchingOrFiltering: Boolean,
    onResetFilters: () -> Unit,
    onCreateAnnouncement: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSearchingOrFiltering) {
        EmptyStateView(
            icon = Icons.Outlined.Campaign,
            title = "Filtreye Uygun Duyuru Bulunamadı",
            description = "Seçilen kategori veya arama kriterinde herhangi bir duyuru kaydı bulunamadı.",
            iconColor = SecondaryEmerald,
            primaryActionLabel = "Filtreleri Sıfırla",
            primaryActionIcon = Icons.Default.Refresh,
            onPrimaryAction = onResetFilters,
            secondaryActionLabel = "Yeni Duyuru Yayınla",
            secondaryActionIcon = Icons.Default.Add,
            onSecondaryAction = onCreateAnnouncement,
            modifier = modifier
        )
    } else {
        EmptyStateView(
            icon = Icons.Outlined.Campaign,
            title = "Henüz Bir Duyuru Paylaşılmadı",
            description = "Tüm okul veya seçili sınıflar için ilk duyurunuzu, toplantı bildirimini veya etkinlik haberini hemen yayınlayabilirsiniz.",
            iconColor = SecondaryEmerald,
            primaryActionLabel = "İlk Duyuruyu Yayınla",
            primaryActionIcon = Icons.Default.Add,
            onPrimaryAction = onCreateAnnouncement,
            modifier = modifier
        )
    }
}
