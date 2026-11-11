package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

import androidx.compose.foundation.clickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

import com.example.ui.dialogs.ChangePasswordDialog

@Composable
fun SettingsScreen(
    state: Okul360UiState,
    onRoleSelected: (UserRole) -> Unit,
    onToggleDemoMode: ((Boolean) -> Unit)? = null,
    onTestBackendConnection: () -> Unit,
    onUpdateServerConfig: (url: String, tenant: String) -> Unit,
    onThemeSelected: ((String) -> Unit)? = null,
    onOpenFeedback: (() -> Unit)? = null,
    onOpenNotificationSettings: (() -> Unit)? = null,
    onTestNotification: (() -> Unit)? = null,
    onTestPaymentReminder: (() -> Unit)? = null,
    onClearLocalCache: (() -> Unit)? = null,
    onChangePassword: ((String, String) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var serverUrl by remember(state.serverBaseUrl) { mutableStateOf(state.serverBaseUrl) }
    var tenantId by remember(state.tenantId) { mutableStateOf(state.tenantId) }
    var showClearCacheConfirmDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val health = state.systemHealth
    val currentUser = state.currentUser

    if (showClearCacheConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = StatusError,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Yerel Önbelleği Temizle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Cihazınızda kayıtlı tüm yerel Room veritabanı kayıtları (öğrenci listesi, yoklama kayıtları, faturalar ve duyurular) silinecek ve veriler sunucudan sıfırdan yeniden eşitlenecektir. Senkronizasyon sorunlarını çözer.\n\nDevam etmek istiyor musunuz?",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheConfirmDialog = false
                        onClearLocalCache?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Önbelleği Temizle", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearCacheConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("İptal")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // 1. Profil Bilgileri & Oturum Yönetimi
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser.full_name.take(2).uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = PrimaryBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentUser.full_name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BrandNavy
                                )
                                Text(
                                    text = currentUser.email,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusBadge(
                            label = state.selectedRole.displayName,
                            color = Color(state.selectedRole.badgeColorHex)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileDetailRow(icon = Icons.Default.Business, label = "Kurum", value = currentUser.institution_name)
                    ProfileDetailRow(icon = Icons.Default.LocationOn, label = "Aktif Şube", value = currentUser.branch_name)
                    ProfileDetailRow(icon = Icons.Default.Phone, label = "Telefon", value = currentUser.phone ?: "Tanımlanmamış")
                    ProfileDetailRow(icon = Icons.Default.LockClock, label = "Oturum Durumu", value = "🟢 Sürekli Aktif (Kalıcı Giriş)")

                    Spacer(modifier = Modifier.height(14.dp))

                    if (onLogout != null) {
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Oturumu Kapat (Güvenli Çıkış)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Sistem & Güvenlik Yapay Zeka Özeti
        item {
            RoleContextualAiCard(
                screenName = "SETTINGS",
                state = state
            )
        }

        // 2. FastAPI Backend & Sunucu Yapılandırması & Test
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Dns, contentDescription = null, tint = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "FastAPI Backend Sunucu Ayarları",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BrandNavy
                                )
                                Text(
                                    text = "Canlı REST API uç noktası ve Multi-Tenant",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusBadge(
                            label = health?.api_status ?: "ONLINE",
                            color = SecondaryEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("FastAPI Sunucu URL (Canlı)") },
                        placeholder = { Text("https://scs360.com.tr/api") },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryBlue) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tenantId,
                        onValueChange = { tenantId = it },
                        label = { Text("Tenant ID (Opsiyonel - Boş Bırakabilirsiniz)") },
                        placeholder = { Text("Kullanılmıyor (Boş)") },
                        leadingIcon = { Icon(Icons.Default.Domain, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ℹ️ Tenant ID kullanılmıyorsa boş bırakınız. Doğrudan API istekleri gönderilir.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestBackendConnection,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !state.isTestingBackend
                        ) {
                            if (state.isTestingBackend) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test ediliyor...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bağlantıyı Test Et", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { onUpdateServerConfig(serverUrl, tenantId) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kaydet & Yenile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    state.backendStatusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlueContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    fontSize = 11.5.sp,
                                    color = BrandNavy
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2.5. Kurumsal Tema & Marka Kimliği (Brand Theme Customization)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Kurumsal Tema & Renk Kimliği",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BrandNavy
                                )
                                Text(
                                    text = "Okulunuzun kurumsal kimliğine uygun birincil renk",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusBadge(
                            label = state.selectedThemeName,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Aşağıdaki renk paletlerinden birini seçerek uygulama genelindeki butonlar, başlıklar ve göstergelerin temasını anında özelleştirin:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Theme Color Options Grid
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(AppColorTheme.entries.toList()) { themeItem ->
                            val isSelected = state.selectedThemeName.equals(themeItem.name, ignoreCase = true)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onThemeSelected?.invoke(themeItem.name)
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(themeItem.primaryColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) BrandNavy else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = themeItem.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BrandNavy else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Android Push Bildirim & Sistem İzinleri
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Firebase Cloud Messaging (FCM)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BrandNavy
                            )
                        }
                        StatusBadge(label = "FCM Aktif", color = Color(0xFF059669))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Gerçek zamanlı FCM push bildirimleri; okul duyuruları, taksit hatırlatmaları ve acil bildirimler için veli ve personele iletilir.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onOpenNotificationSettings?.invoke() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sistem İzinleri", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onTestNotification?.invoke() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Duyuru Push", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onTestPaymentReminder?.invoke() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentAmber)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ödeme / Taksit Push Testi Gönder", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 3.5. Geri Bildirim & Hata Bildirimi (Feedback & Bug Report)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentRose.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = AccentRose)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Geri Bildirim & Hata Raporu",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BrandNavy
                                )
                                Text(
                                    text = "Destek ekibine doğrudan hata veya özellik talebi iletin",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Uygulama deneyiminizi iyileştirmek için önerilerinizi veya karşılaştığınız teknik sorunları tek dokunuşla e-posta desteğine aktarabilirsiniz.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenFeedback?.invoke()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Geri Bildirim / Hata Raporu Gönder", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3.6. Hesap Güvenliği & Şifre Değiştirme
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Hesap Güvenliği & Şifre",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BrandNavy
                                )
                                Text(
                                    text = "Giriş şifrenizi güncelleyin ve oturum güvenliğini sağlayın",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showChangePasswordDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Şifre Değiştir", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 4. Yerel Veritabanı & Çevrimdışı Önbellek (Room DB)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StatusWarning.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = StatusWarning
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Yerel Önbellek & Eşitleme",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Room SQLite Database Caching",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusBadge(
                            label = "${state.students.size} Kayıt",
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Çevrimdışı kullanım için cihazda saklanan öğrenci, yoklama, fatura ve duyuru veritabanı önbelleğini temizleyebilirsiniz. Senkronizasyon veya veri tutarsızlığı yaşandığında sunucuyla sıfırdan eşitleme sağlar.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Önbellek Durumu: Senkronize",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Öğrenci: ${state.students.size} • Fatura: ${state.invoices.size} • Duyuru: ${state.announcements.size}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showClearCacheConfirmDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Önbelleği Temizle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. Rol Değiştirme (Simülasyon)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kullanıcı Oturum Rolü & Yetkileri",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(state.selectedRole.badgeColorHex).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(state.selectedRole.badgeColorHex).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "🔒 ${state.selectedRole.displayName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(state.selectedRole.badgeColorHex),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Oturum yetkileriniz sunucu kimlik doğrulaması (JWT Token / Backend hesabı) ile kilitlenmiştir.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }


        // 5. SaaS Platform Health & Metrics
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sistem & Altyapı Durumu",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        StatusBadge(
                            label = health?.api_status ?: "ONLINE",
                            color = SecondaryEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SystemMetricRow(label = "Çalışma Modu", value = if (state.isDemoMode) "🧪 Demo Modu" else "⚡ Canlı Backend")
                    SystemMetricRow(label = "Abonelik Paketi", value = health?.subscription_tier ?: "SCS360 Enterprise")
                    SystemMetricRow(label = "Veritabanı", value = health?.database_connection ?: "PostgreSQL 16 Multi-Tenant")
                    SystemMetricRow(label = "Outbox Worker", value = health?.outbox_worker_status ?: "Aktif (Async SMS/Push)")
                    SystemMetricRow(label = "API Yanıt Süresi", value = "${health?.latency_ms ?: 24} ms")
                    SystemMetricRow(label = "Tenant Kimliği", value = if (state.tenantId.isNotBlank()) state.tenantId else "Kullanılmıyor (Doğrudan Tekil Kurum)")
                }
            }
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { curr, newP ->
                onChangePassword?.invoke(curr, newP)
                showChangePasswordDialog = false
            }
        )
    }
}

@Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandNavy
        )
    }
}

@Composable
fun SystemMetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
