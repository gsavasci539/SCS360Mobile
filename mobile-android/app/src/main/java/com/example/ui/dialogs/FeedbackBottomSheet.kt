package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.*

enum class FeedbackCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color
) {
    BUG("Hata Bildirimi", Icons.Default.BugReport, AccentRose),
    FEATURE("Öneri & İstek", Icons.Default.Lightbulb, AccentAmber),
    SUPPORT("Destek Talebi", Icons.Default.HelpCenter, PrimaryBlue),
    GENERAL("Genel Görüş", Icons.Default.RateReview, SecondaryEmerald)
}

enum class FeedbackUrgency(
    val title: String,
    val color: Color
) {
    LOW("Düşük", Color(0xFF64748B)),
    NORMAL("Normal", PrimaryBlue),
    HIGH("Yüksek", AccentAmber),
    CRITICAL("Kritik", AccentRose)
}

/**
 * Modern Material 3 Bottom Sheet Dialog for reporting bugs, submitting suggestions,
 * and requesting support directly to the institutional help desk via standard Email intent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    currentUser: User,
    selectedRole: UserRole,
    tenantId: String,
    onDismiss: () -> Unit,
    supportEmail: String = "destek@scs360.com"
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedCategory by remember { mutableStateOf(FeedbackCategory.BUG) }
    var selectedUrgency by remember { mutableStateOf(FeedbackUrgency.NORMAL) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var includeDeviceInfo by remember { mutableStateOf(true) }
    var userEmailInput by remember { mutableStateOf(currentUser.email) }

    var subjectError by remember { mutableStateOf(false) }
    var messageError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Geri Bildirim & Destek",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = BrandNavy
                            )
                        )
                        Text(
                            text = "Hata raporu ve önerilerinizi doğrudan bize iletin",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector Chips
            Text(
                text = "Bildirim Türü",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(FeedbackCategory.entries.toTypedArray()) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategory = cat
                            }
                            .testTag("feedback_cat_${cat.name.lowercase()}"),
                        color = if (isSelected) cat.color else cat.color.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) cat.color else cat.color.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else cat.color,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.title,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else cat.color
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Urgency / Priority Selection
            Text(
                text = "Öncelik Seviyesi",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FeedbackUrgency.entries.toTypedArray()) { urgency ->
                    val isSelected = selectedUrgency == urgency
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedUrgency = urgency
                            },
                        color = if (isSelected) urgency.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isSelected) urgency.color else Color.Transparent
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = urgency.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) urgency.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subject TextField
            OutlinedTextField(
                value = subject,
                onValueChange = {
                    subject = it
                    if (it.isNotBlank()) subjectError = false
                },
                label = { Text("Konu Başlığı") },
                placeholder = {
                    Text(
                        when (selectedCategory) {
                            FeedbackCategory.BUG -> "Örn: Yoklama kaydedilirken ekran donuyor"
                            FeedbackCategory.FEATURE -> "Örn: Servis güzergahı harita takibi eklensin"
                            FeedbackCategory.SUPPORT -> "Örn: Fatura PDF'ini indiremiyorum"
                            FeedbackCategory.GENERAL -> "Örn: SCS360 arayüzü ve hız değerlendirmesi"
                        }
                    )
                },
                isError = subjectError,
                supportingText = if (subjectError) {
                    { Text("Lütfen bir konu başlığı yazınız.", color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_subject_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description / Message TextField
            OutlinedTextField(
                value = message,
                onValueChange = {
                    message = it
                    if (it.isNotBlank()) messageError = false
                },
                label = { Text("Açıklama / Mesajınız") },
                placeholder = {
                    Text(
                        when (selectedCategory) {
                            FeedbackCategory.BUG -> "Karşılaştığınız hatayı, oluştuğu ekranı ve adımları detaylıca açıklayınız..."
                            FeedbackCategory.FEATURE -> "Uygulamada görmek istediğiniz özelliği ve size nasıl fayda sağlayacağını yazınız..."
                            FeedbackCategory.SUPPORT -> "Yardım almak istediğiniz konuyu ve ilgili öğrenci/fatura bilgilerini belirtiniz..."
                            FeedbackCategory.GENERAL -> "Deneyiminizi ve önerilerinizi bizimle paylaşınız..."
                        }
                    )
                },
                isError = messageError,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (messageError) {
                            Text("Lütfen açıklama giriniz.", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Minimum 10 karakter önerilir.")
                        }
                        Text("${message.length} karakter")
                    }
                },
                minLines = 4,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_message_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Reply Email (Optional contact confirmation)
            OutlinedTextField(
                value = userEmailInput,
                onValueChange = { userEmailInput = it },
                label = { Text("Yanıt Alınacak E-posta") },
                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = PrimaryBlue) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Include Device Info Toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            includeDeviceInfo = !includeDeviceInfo
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PermDeviceInformation,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cihaz ve Sistem Tanılarını Ekle",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Model: ${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} • v1.0.0",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = includeDeviceInfo,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            includeDeviceInfo = it
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    if (subject.isBlank()) {
                        subjectError = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        return@Button
                    }
                    if (message.isBlank()) {
                        messageError = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        return@Button
                    }

                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    sendFeedbackEmail(
                        context = context,
                        supportEmail = supportEmail,
                        category = selectedCategory,
                        urgency = selectedUrgency,
                        subject = subject.trim(),
                        message = message.trim(),
                        replyEmail = userEmailInput.trim(),
                        currentUser = currentUser,
                        selectedRole = selectedRole,
                        tenantId = tenantId,
                        includeDeviceInfo = includeDeviceInfo
                    )

                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_feedback_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "E-posta Uygulamasıyla Gönder",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Builds and dispatches an Android Intent to send feedback via email client
 */
private fun sendFeedbackEmail(
    context: Context,
    supportEmail: String,
    category: FeedbackCategory,
    urgency: FeedbackUrgency,
    subject: String,
    message: String,
    replyEmail: String,
    currentUser: User,
    selectedRole: UserRole,
    tenantId: String,
    includeDeviceInfo: Boolean
) {
    val emailSubject = "[SCS360 ${category.title}] [${urgency.title}] $subject"

    val bodyBuilder = StringBuilder()
    bodyBuilder.append("=== SCS360 KULLANICI GERİ BİLDİRİMİ ===\n\n")
    bodyBuilder.append("Bildirim Türü: ${category.title}\n")
    bodyBuilder.append("Öncelik: ${urgency.title}\n")
    bodyBuilder.append("Konu: $subject\n")
    bodyBuilder.append("Gönderen: ${currentUser.full_name} (${selectedRole.title})\n")
    bodyBuilder.append("Yanıt E-postası: $replyEmail\n\n")
    bodyBuilder.append("--- MESAJ & DETAYLAR ---\n")
    bodyBuilder.append(message)
    bodyBuilder.append("\n\n")

    if (includeDeviceInfo) {
        bodyBuilder.append("--- SİSTEM & CİHAZ BİLGİLERİ ---\n")
        bodyBuilder.append("Uygulama: SCS360 Mobil v1.0.0 (Build 2026.08)\n")
        bodyBuilder.append("Cihaz Modeli: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})\n")
        bodyBuilder.append("Android Sürümü: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        bodyBuilder.append("Kullanıcı ID: ${currentUser.id}\n")
        bodyBuilder.append("Tenant Kimliği: ${tenantId.ifBlank { "default_tenant" }}\n")
        bodyBuilder.append("Tarih / Saat: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
    }

    val emailBody = bodyBuilder.toString()

    try {
        val mailtoUri = Uri.parse("mailto:$supportEmail")
        val emailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            putExtra(Intent.EXTRA_SUBJECT, emailSubject)
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }

        val chooser = Intent.createChooser(emailIntent, "Geri bildirimi e-posta ile gönder...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        Toast.makeText(context, "E-posta istemcisi açılıyor...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        // Fallback to standard ACTION_SEND
        try {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                putExtra(Intent.EXTRA_TEXT, emailBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "E-posta gönder..."))
        } catch (err: Exception) {
            Toast.makeText(context, "E-posta istemcisi bulunamadı: $supportEmail adresine yazabilirsiniz.", Toast.LENGTH_LONG).show()
        }
    }
}
