package com.example.ui.dialogs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Announcement
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

/**
 * Announcement Detail Dialog Component (Duyuru Detay Paneli)
 * Shows complete announcement content, metadata, author, audience, and share options.
 */
@Composable
fun AnnouncementDetailDialog(
    announcement: Announcement,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val categoryLabel = when (announcement.category.uppercase()) {
        "OKUL", "BRANCH", "INSTITUTION" -> "Okul Genel Duyurusu"
        "SINIF", "CLASS", "ACADEMIC", "DERS" -> "Akademik / Sınıf Duyurusu"
        "GENEL", "GENERAL", "EVENT" -> "Genel Bilgilendirme & Etkinlik"
        "EMERGENCY", "ACIL" -> "🚨 Acil Durum Duyurusu"
        "FINANCIAL", "MALI" -> "Mali & Aidat Duyurusu"
        else -> announcement.category
    }

    val categoryBadgeColor = when (announcement.category.uppercase()) {
        "EMERGENCY", "ACIL" -> AccentRose
        "SINIF", "ACADEMIC", "CLASS", "DERS" -> PrimaryBlue
        "OKUL", "BRANCH", "INSTITUTION" -> SecondaryEmerald
        "FINANCIAL", "MALI" -> AccentAmber
        else -> PrimaryBlue
    }

    val audienceLabel = when (announcement.target_audience.uppercase()) {
        "ALL" -> "Tüm Kurum (Öğretmenler & Veliler)"
        "PARENTS" -> "Sadece Veliler"
        "TEACHERS" -> "Sadece Öğretmen & Personel"
        "BRANCH" -> "Şube Genel"
        else -> announcement.target_audience
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        label = categoryLabel,
                        color = categoryBadgeColor
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", modifier = Modifier.size(18.dp))
                    }
                }

                if (announcement.is_pinned) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = AccentAmber.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bu duyuru panoda sabitlenmiştir",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = BrandNavy
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Metadata Pill Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Yayınlayan: ${announcement.author_name} (${announcement.author_role})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandNavy
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hedef Kitle: $audienceLabel",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = SecondaryEmerald, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tarih: ${announcement.created_at}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Content
                Text(
                    text = announcement.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons (Paylaş, Kapat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, announcement.title)
                                putExtra(Intent.EXTRA_TEXT, "📢 *${announcement.title}*\n\n${announcement.content}\n\n— ${announcement.author_name} (${announcement.created_at})")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Duyuruyu Paylaş"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paylaş", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Tamam", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
