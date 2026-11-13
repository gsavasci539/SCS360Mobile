package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusBadge
import com.example.ui.dialogs.ProfileDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

data class MenuItemData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val badge: String? = null,
    val onClick: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuScreen(
    state: Okul360UiState,
    onNavigateToClasses: () -> Unit,
    onNavigateToCommunication: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToRoles: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToDailyReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRoleSelected: (UserRole) -> Unit,
    onUpdateServerConfig: (url: String, tenant: String) -> Unit,
    onUpdateProfile: (name: String, email: String, phone: String, institution: String, branch: String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToDigitalId: () -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToReportCard: () -> Unit = {},
    onNavigateToCourseMaterials: () -> Unit = {},
    onNavigateToDirectMessaging: () -> Unit = {},
    onOpenThemeConfig: () -> Unit = {},
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToTransport: () -> Unit = {},
    onNavigateToCanteen: () -> Unit = {},
    onNavigateToHomework: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToStaff: () -> Unit = {},
    onNavigateToAiAnalytics: () -> Unit = {},
    onNavigateToAiChatAudit: () -> Unit = {},
    onNavigateToOrganizations: () -> Unit = {},
    onNavigateToRfidDevices: () -> Unit = {},
    onNavigateToCanteenPortal: () -> Unit = {},
    onNavigateToConsents: () -> Unit = {},
    onNavigateToGuidance: () -> Unit = {},
    onNavigateToInfirmary: () -> Unit = {},
    onNavigateToClubs: () -> Unit = {},
    onNavigateToSurveys: () -> Unit = {},
    onNavigateToTutoring: () -> Unit = {},
    onNavigateToPayroll: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToExams: () -> Unit = {},
    onNavigateToOutbox: () -> Unit = {},
    onNavigateToPlatformSaas: () -> Unit = {},
    onNavigateToSubscriptionBilling: () -> Unit = {},
    onNavigateToDismissal: () -> Unit = {},
    onNavigateToParents: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showServerSettingsModal by remember { mutableStateOf(false) }
    var showOutboxLogsModal by remember { mutableStateOf(false) }
    var showProfileModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val educationItems = remember(state) {
        listOf(
            MenuItemData("Veliler", "Veli, öğrenci ve giriş hesabı bağları", Icons.Default.FamilyRestroom, Color(0xFFEDE9FE), Color(0xFF7C3AED), null, onNavigateToParents),
            MenuItemData("Sınıflar & Şubeler", "${state.classes.size} Sınıf", Icons.Default.School, Color(0xFFDBEAFE), Color(0xFF1D4ED8), "${state.classes.size}", onNavigateToClasses),
            MenuItemData("Sınavlar & Takvim", "${state.exams.size} Sınav", Icons.Default.Assessment, Color(0xFFFEE2E2), Color(0xFFDC2626), "${state.exams.size}", onNavigateToExams),
            MenuItemData("Ders Programı", "Haftalık Derslik & Saat", Icons.Default.CalendarMonth, Color(0xFFE0F2FE), Color(0xFF0284C7), null, onNavigateToSchedule),
            MenuItemData("Dijital Karne", "Notlar ve PDF Çıktı", Icons.Default.WorkspacePremium, Color(0xFFF3E8FF), Color(0xFF7C3AED), "MEB", onNavigateToReportCard),
            MenuItemData("Ders Materyalleri", "${state.courseMaterials.size} Föy & Doküman", Icons.Default.MenuBook, Color(0xFFE0E7FF), Color(0xFF4338CA), "${state.courseMaterials.size}", onNavigateToCourseMaterials),
            MenuItemData("Ödev Takibi", "Teslim ve Puanlama", Icons.Default.Assignment, Color(0xFFFCE7F3), Color(0xFFBE185D), null, onNavigateToHomework),
            MenuItemData("Kütüphane", "Kitap Zimmet & İade", Icons.Default.CollectionsBookmark, Color(0xFFFFEDD5), Color(0xFFC2410C), "${state.libraryBooks.size}", onNavigateToLibrary),
            MenuItemData("Günlük Karne", "Beslenme, Uyku, Gelişim", Icons.Default.AutoStories, Color(0xFFDCFCE7), Color(0xFF15803D), null, onNavigateToDailyReports),
            MenuItemData("İzin Belgeleri", "Muvafakatname Onayları", Icons.Default.FactCheck, Color(0xFFDCFCE7), Color(0xFF15803D), "${state.consents.size}", onNavigateToConsents),
            MenuItemData("Rehberlik & PDR", "Psikolojik Danışmanlık", Icons.Default.Psychology, Color(0xFFF3E8FF), Color(0xFF7C3AED), null, onNavigateToGuidance),
            MenuItemData("Birebir Etüt", "Kişisel Takviye Dersler", Icons.Default.EventAvailable, Color(0xFFE0F2FE), Color(0xFF0284C7), null, onNavigateToTutoring),
            MenuItemData("Kulüpler & Etkinlik", "${state.schoolClubs.size} Kulüp", Icons.Default.Groups, Color(0xFFFFEDD5), Color(0xFFC2410C), "${state.schoolClubs.size}", onNavigateToClubs)
        )
    }

    val communicationItems = remember(state) {
        listOf(
            MenuItemData("Giden Kutusu & Kuyruk", "Toplu SMS / WhatsApp", Icons.Default.Outbox, Color(0xFFE0E7FF), Color(0xFF4338CA), "${state.broadcastJobs.size}", onNavigateToOutbox),
            MenuItemData("Duyurular & Bildirim", "${state.announcements.size} Duyuru", Icons.Default.Campaign, Color(0xFFF3E8FF), Color(0xFF7E22CE), "${state.announcements.size}", onNavigateToCommunication),
            MenuItemData("Özel Mesajlaşma", "Veli-Öğretmen İletişim", Icons.Default.ChatBubbleOutline, Color(0xFFD1FAE5), Color(0xFF059669), "${state.chatConversations.size}", onNavigateToDirectMessaging),
            MenuItemData("Veli Randevuları", "Birebir Görüşmeler", Icons.Default.EventAvailable, Color(0xFFE0F2FE), Color(0xFF0369A1), "${state.appointments.size}", onNavigateToAppointments),
            MenuItemData("Anketler & Memnuniyet", "${state.surveys.size} Anket", Icons.Default.Poll, Color(0xFFFCE7F3), Color(0xFFBE185D), null, onNavigateToSurveys),
            MenuItemData("Bildirim Merkezi", "${state.notifications.count { !it.is_read }} Yeni Uyarı", Icons.Default.Notifications, Color(0xFFDBEAFE), Color(0xFF2563EB), "${state.notifications.count { !it.is_read }}", onNavigateToNotifications)
        )
    }

    val smartServicesItems = remember(state) {
        val list = mutableListOf(
            MenuItemData("Yapay Zeka (AI)", "Akıllı Raporlama & Analiz", Icons.Default.AutoAwesome, Color(0xFFFAE8FF), Color(0xFFA21CAF), "✨ AI", onNavigateToAiAnalytics)
        )
        if (state.selectedRole == UserRole.SUPERADMIN) {
            list.add(
                MenuItemData("AI Chat Analiz & LLM", "Diyalog Denetimi & Fine-Tuning", Icons.Default.Psychology, Color(0xFFF3E8FF), Color(0xFF7C3AED), "SuperAdmin", onNavigateToAiChatAudit)
            )
        }
        list.addAll(
            listOf(
                MenuItemData("Revir & Sağlık", "Ateş, İlaç, Muayene", Icons.Default.MedicalServices, Color(0xFFFEE2E2), Color(0xFFDC2626), null, onNavigateToInfirmary),
                MenuItemData("Öğrenci Teslim Masası", "Yetkili Teslim & TC Doğrulama", Icons.Default.SupervisorAccount, Color(0xFFCCFBF1), Color(0xFF0F766E), "${state.dismissalLogs.size}", onNavigateToDismissal),
                MenuItemData("Dijital Kimlik (QR)", "Kartlı & QR Güvenlik", Icons.Default.QrCode2, Color(0xFFEDE9FE), Color(0xFF6D28D9), "QR", onNavigateToDigitalId),
                MenuItemData("Servis & Ulaşım", "Canlı GPS & Biniş", Icons.Default.DirectionsBus, Color(0xFFFEF3C7), Color(0xFFB45309), "${state.busRoutes.size}", onNavigateToTransport),
                MenuItemData("Kantin Cüzdan", "Temassız Harcama & POS", Icons.Default.AccountBalanceWallet, Color(0xFFDCFCE7), Color(0xFF15803D), "₺", onNavigateToCanteen),
                MenuItemData("Kantin Görevlisi Paneli", "Satış POS & Menü", Icons.Default.Restaurant, Color(0xFFFFEDD5), Color(0xFFC2410C), "POS", onNavigateToCanteenPortal)
            )
        )
        list
    }

    val adminItems = remember(state) {
        val list = mutableListOf(
            MenuItemData("Kurumlar & Şubeler", "/organizations • /branches", Icons.Default.CorporateFare, Color(0xFFDBEAFE), Color(0xFF1D4ED8), "${state.branches.size}", onNavigateToOrganizations),
            MenuItemData("Üyelik, Kota & Ödeme", "/subscription • ${state.organizationAccess.status}", Icons.Default.CreditScore, Color(0xFFCCFBF1), Color(0xFF0F766E), state.organizationAccess.plan_name, onNavigateToSubscriptionBilling)
        )
        if (state.selectedRole == UserRole.SUPERADMIN) {
            list.add(
                MenuItemData("Platform & SaaS Üyelikler", "/platform/subscriptions", Icons.Default.Dns, Color(0xFFFEF3C7), Color(0xFF92400E), "SuperAdmin", onNavigateToPlatformSaas)
            )
            list.add(
                MenuItemData("LLM & Chat Analitiği", "Konuşma Denetimi & Veri Seti", Icons.Default.Dataset, Color(0xFFE0E7FF), Color(0xFF4338CA), "SuperAdmin", onNavigateToAiChatAudit)
            )
        }
        list.addAll(
            listOf(
                MenuItemData("RFID & Cihazlar", "Turnike POS & Cihaz Ağ", Icons.Default.Contactless, Color(0xFFEDE9FE), Color(0xFF6D28D9), "${state.rfidCards.size}", onNavigateToRfidDevices),
                MenuItemData("Raporlar & Analitik", "Mali & Katılım Çizelgesi", Icons.Default.BarChart, Color(0xFFCCFBF1), Color(0xFF0F766E), "Canlı", onNavigateToReports),
                MenuItemData("Personel & İK", "Nöbet & İzin Talepleri", Icons.Default.Badge, Color(0xFFFCE7F3), Color(0xFFBE185D), "${state.dutyRosters.size}", onNavigateToStaff),
                MenuItemData("Personel Bordro & Maaş", "Hakediş ve Ödemeler", Icons.Default.Payments, Color(0xFFE0E7FF), Color(0xFF4338CA), "${state.payrollRecords.size}", onNavigateToPayroll),
                MenuItemData("Kullanıcılar", "${state.users.size} Personel & Rol", Icons.Default.ManageAccounts, Color(0xFFE0F2FE), Color(0xFF0369A1), null, onNavigateToUsers),
                MenuItemData("Roller & Yetkiler", "Sistem İzin Matrisi", Icons.Default.Security, Color(0xFFFFEDD5), Color(0xFF9A3412), null, onNavigateToRoles),
                MenuItemData("Güvenlik & Kapı Geçişleri", "Turnike, QR & Denetim", Icons.Default.Security, Color(0xFFEDE9FE), Color(0xFF6D28D9), "${state.gateAccessLogs.size}", onNavigateToSecurity),
                MenuItemData("Kaynaklar & Envanter", "Fiziksel Demirbaşlar", Icons.Default.Inventory2, Color(0xFFFEF3C7), Color(0xFF92400E), null, onNavigateToResources)
            )
        )
        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Profil ve Aktif Rol Kartı
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProfileModal = true },
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.currentUser.full_name.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.currentUser.full_name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                        )
                        Text(
                            text = "${state.currentUser.institution_name} • ${state.currentUser.branch_name}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(
                                label = state.selectedRole.title,
                                color = Color(state.selectedRole.badgeColorHex)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { showProfileModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Düzenle",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Contextual AI Hub Card
        item {
            RoleContextualAiCard(
                screenName = "MENU",
                state = state
            )
        }

        // 2. Bölüm: 🎓 Eğitim & Öğrenci
        item {
            MenuCategorySection(
                title = "🎓 Eğitim & Öğrenci Modülleri",
                items = educationItems
            )
        }

        // 4. Bölüm: 💬 İletişim & Rehberlik
        item {
            MenuCategorySection(
                title = "💬 İletişim & Rehberlik",
                items = communicationItems
            )
        }

        // 5. Bölüm: ✨ Akıllı Hizmetler
        item {
            MenuCategorySection(
                title = "✨ Akıllı Hizmetler & AI",
                items = smartServicesItems
            )
        }

        // 6. Bölüm: 🏢 İdari & Mali Yönetim
        item {
            MenuCategorySection(
                title = "🏢 İdari & Mali Yönetim",
                items = adminItems
            )
        }

        // 7. Sistem & Ayarlar
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "⚙️ Sistem & Uygulama Ayarları",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenThemeConfig,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE11D48))
                            Spacer(Modifier.width(6.dp))
                            Text("Tema Rengi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onNavigateToSettings,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                            Spacer(Modifier.width(6.dp))
                            Text("Ayarlar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onLogout,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Güvenli Çıkış Yap", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // Profile Dialog
    if (showProfileModal) {
        ProfileDialog(
            user = state.currentUser,
            onDismiss = { showProfileModal = false },
            onConfirm = { name, email, phone, inst, branch ->
                onUpdateProfile(name, email, phone, inst, branch)
                showProfileModal = false
            }
        )
    }
}

@Composable
fun MenuCategorySection(
    title: String,
    items: List<MenuItemData>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { item.onClick() },
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(item.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = item.subtitle,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (item.badge != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = item.iconBg,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = item.badge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = item.iconTint,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
