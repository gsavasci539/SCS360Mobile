package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.FinancialSummaryCard
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import com.example.util.AiAnalyticsEngine
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerPortalScreen(
    state: Okul360UiState,
    onRoleSelected: (UserRole) -> Unit,
    onGenerateAiForecast: () -> Unit,
    onOpenBranchManagement: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSubscriptionBilling: () -> Unit = {},
    onOpenChatbot: (() -> Unit)? = null,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRoleSelector by remember { mutableStateOf(false) }
    var selectedOwnerTab by remember { mutableIntStateOf(0) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("tr", "TR")) }

    // Consolidated metrics calculation across all branches
    val totalStudents = state.students.size
    val totalInvoiced = state.invoices.sumOf { it.amount }
    val totalCollected = state.invoices.filter {
        it.status.equals("PAID", ignoreCase = true) || it.status.equals("ODENDI", ignoreCase = true) || it.paid_amount >= it.amount
    }.sumOf { if (it.paid_amount > 0) it.paid_amount else it.amount }

    val collectionRate = if (totalInvoiced > 0) (totalCollected / totalInvoiced * 100).toInt() else 0
    val totalCapacity = 450 // Total capacity across campuses
    val occupancyRate = if (totalCapacity > 0) ((totalStudents.toDouble() / totalCapacity) * 100).toInt() else 0
    val staffCount = 28 // Total educators and administration

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kurum Sahibi Paneli",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = Color(0xFF0F172A)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF4338CA).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "KONSOLİDE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4338CA),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${state.currentUser.institution_name} • Tüm Kampüsler",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B), fontSize = 11.5.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showRoleSelector = true },
                        modifier = Modifier.testTag("owner_role_switch_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Rol Değiştir", tint = Color(0xFF4338CA))
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("owner_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Çıkış Yap", tint = Color(0xFF64748B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = selectedOwnerTab == 0,
                    onClick = { selectedOwnerTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Özet") },
                    label = { Text("Özet", fontSize = 10.5.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenBranchManagement,
                    icon = { Icon(Icons.Default.CorporateFare, contentDescription = "Şubeler") },
                    label = { Text("Şubeler", fontSize = 10.5.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenFinance,
                    icon = { Icon(Icons.Default.AttachMoney, contentDescription = "Finans") },
                    label = { Text("Finans", fontSize = 10.5.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onOpenChatbot?.invoke() },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Asistan") },
                    label = { Text("AI Asistan", fontSize = 10.5.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color(0xFF4338CA),
                        unselectedTextColor = Color(0xFF4338CA)
                    )
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. Kurucu Hoşgeldiniz & Hızlı Özet
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF312E81), // Deep Indigo
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Konsolide Kurum Özeti",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "2025-2026 Eğitim-Öğretim Yılı Gerçekleşen",
                                    color = Color(0xFFC7D2FE),
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Toplam Ciro", color = Color(0xFFA5B4FC), fontSize = 11.sp)
                                Text(
                                    currencyFormat.format(totalInvoiced),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Tahsilat Oranı", color = Color(0xFFA5B4FC), fontSize = 11.sp)
                                Text(
                                    "%$collectionRate",
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Tahsilat", color = Color(0xFFA5B4FC), fontSize = 11.sp)
                                Text(
                                    currencyFormat.format(totalCollected),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Operasyonel KPI Kartları (Öğrenci, Personel, Doluluk, Şube)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Öğrenci", fontSize = 11.5.sp, color = Color(0xFF64748B))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$totalStudents Kayıtlı", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text("Tüm şubeler toplamı", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Doluluk", fontSize = 11.5.sp, color = Color(0xFF64748B))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("%$occupancyRate Dolu", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text("Kapasite: $totalCapacity", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Personel", fontSize = 11.5.sp, color = Color(0xFF64748B))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$staffCount Aktif", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text("Öğretmen & İdari", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }

            // 3. AI Nakit Akışı, Kapasite & Stratejik Yönetim Kartı
            item {
                RoleContextualAiCard(
                    screenName = "DASHBOARD",
                    state = state,
                    initialExpanded = true,
                    onOpenAiHubClick = onGenerateAiForecast
                )
            }

            // 4. Şubeler Arası Karşılaştırma Grafikleri (Branch Metrics)
            item {
                Text(
                    text = "Şubeler Arası Metrik Karşılaştırması",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
            }

            items(state.branches) { branch ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = branch.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = branch.address,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            StatusBadge(label = "AKTİF LİSANS", color = SecondaryEmerald)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Branch stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Öğrenci", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("180 / 250", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Tahsilat", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("%88", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SecondaryEmerald)
                            }
                            Column {
                                Text("Aylık Ciro", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("₺640.000", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Öğretmen", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("14 Kişi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 5. Kurum Yönetim Kısayolları
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenBranchManagement,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CorporateFare, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Şube Yönetimi", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = onOpenFinance,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Finans & Muhasebe", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenSubscriptionBilling,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("owner_open_subscription_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Üyelik, Kota & Ödeme", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = onOpenReports,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("owner_open_reports_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Analitik Raporlar", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }

    if (showRoleSelector) {
        AlertDialog(
            onDismissRequest = { showRoleSelector = false },
            title = {
                Text(
                    text = "Görünüm ve Rol Değiştir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sistemi farklı kullanıcı rollerinin gözünden test edebilirsiniz:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    UserRole.values().forEach { role ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showRoleSelector = false
                                    onRoleSelected(role)
                                },
                            color = if (state.selectedRole == role) Color(role.badgeColorHex).copy(alpha = 0.15f) else Color(0xFFF1F5F9)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(role.badgeColorHex))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = role.displayName,
                                    fontWeight = if (state.selectedRole == role) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (state.selectedRole == role) Color(role.badgeColorHex) else Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleSelector = false }) {
                    Text("Kapat")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
