package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Student
import com.example.data.model.User
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalIdCardScreen(
    state: Okul360UiState,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var idTypeTab by remember { mutableIntStateOf(0) } // 0: Öğrenci Kimliği, 1: Veli / Personel Kimliği
    var isCardFlipped by remember { mutableStateOf(false) } // false: Ön Yüz, true: Arka Yüz
    var showLargeQrDialog by remember { mutableStateOf(false) }
    var showNfcSimulation by remember { mutableStateOf(false) }
    var showWalletSavedToast by remember { mutableStateOf(false) }

    val defaultStudent = state.students.firstOrNull() ?: Student(
        id = "std-1",
        student_number = "2026-0842",
        first_name = "Emir",
        last_name = "Kaya",
        class_name = "8-A",
        parent_name = "Ayşe Kaya",
        parent_phone = "+90 532 555 0101",
        blood_type = "A Rh+",
        allergies_or_diet = "Yok",
        birth_date = "2012-05-14"
    )

    var selectedStudentId by remember { mutableStateOf(defaultStudent.id) }
    val currentStudent = state.students.find { it.id == selectedStudentId } ?: defaultStudent

    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Dijital Kimlik Kartı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandNavy
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = PrimaryBlue
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tab Switcher (Öğrenci Kimliği vs Veli/Personel)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple(0, "Öğrenci Kimliği", Icons.Default.School),
                            Triple(1, "Veli / Personel Kartı", Icons.Default.Badge)
                        ).forEach { (index, title, icon) ->
                            val isSelected = idTypeTab == index
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue else Color.Transparent,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        idTypeTab = index
                                        isCardFlipped = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Contextual AI Telemetry & Pass Insights
            item {
                RoleContextualAiCard(
                    screenName = "DIGITAL_ID",
                    state = state
                )
            }

            // If Student ID tab and multiple students exist, show Student selector dropdown
            if (idTypeTab == 0 && state.students.size > 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Öğrenci Seçimi:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            AssistChip(
                                onClick = { expanded = true },
                                label = { Text(currentStudent.full_name, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            )

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                state.students.forEach { std ->
                                    DropdownMenuItem(
                                        text = { Text("${std.full_name} (${std.class_name})") },
                                        onClick = {
                                            selectedStudentId = std.id
                                            expanded = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.School, contentDescription = null, tint = PrimaryBlue)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Flip Card Action Hint
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = SecondaryEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NFC & Temassız Turnike Aktif",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryEmerald
                        )
                    }

                    TextButton(
                        onClick = { isCardFlipped = !isCardFlipped },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCardFlipped) "Ön Yüzü Göster" else "Arka Yüzü Göster",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }
            }

            // THE DIGITAL ID CARD ITSELF (Front or Back)
            item {
                AnimatedContent(
                    targetState = Pair(idTypeTab, isCardFlipped),
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "IdCardFlip"
                ) { (typeTab, flipped) ->
                    if (typeTab == 0) {
                        // Student Card
                        if (!flipped) {
                            StudentIdCardFront(
                                student = currentStudent,
                                institutionName = state.currentUser.institution_name,
                                branchName = currentStudent.branch_id.takeIf { it.isNotBlank() } ?: state.currentUser.branch_name,
                                onQrClick = { showLargeQrDialog = true }
                            )
                        } else {
                            StudentIdCardBack(
                                student = currentStudent,
                                institutionName = state.currentUser.institution_name,
                                branchName = state.currentUser.branch_name
                            )
                        }
                    } else {
                        // Parent / Staff Card
                        if (!flipped) {
                            ParentStaffIdCardFront(
                                user = state.currentUser,
                                students = state.students,
                                onQrClick = { showLargeQrDialog = true }
                            )
                        } else {
                            ParentStaffIdCardBack(
                                user = state.currentUser
                            )
                        }
                    }
                }
            }

            // Action Buttons Bar
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Akıllı Kimlik İşlemleri",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showLargeQrDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Turnike QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { showNfcSimulation = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SecondaryEmerald.copy(alpha = 0.15f),
                                    contentColor = SecondaryEmerald
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Contactless, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("NFC Doğrula", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { showWalletSavedToast = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dijital Cüzdana Ekle (Google / Apple Wallet)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Security & Validity Note
            item {
                Surface(
                    color = PrimaryBlueContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "T.C. MEB e-Okul Entegre Akıllı Kimlik",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandNavy
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bu kimlik kartı okul kampüs girişleri, servis yoklaması ve yemekhane geçiş turnikelerinde resmi geçerliliğe sahiptir.",
                                fontSize = 11.sp,
                                color = OnPrimaryBlueContainer.copy(alpha = 0.9f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Turnike Fullscreen Large QR Code
    if (showLargeQrDialog) {
        Dialog(onDismissRequest = { showLargeQrDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Turnike Geçiş Kodu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(onClick = { showLargeQrDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Oversized High-Contrast QR Code
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, PrimaryBlue),
                        modifier = Modifier.size(220.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DynamicQrCodeCanvas(
                                seed = if (idTypeTab == 0) currentStudent.id else state.currentUser.id,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (idTypeTab == 0) currentStudent.full_name else state.currentUser.full_name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandNavy
                    )

                    Text(
                        text = if (idTypeTab == 0) "No: ${currentStudent.student_number} • Sınıf: ${currentStudent.class_name}" else "${state.selectedRole.title} • Turnike Yetkisi Aktif",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pulse Security Indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SecondaryEmerald)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dinamik Güvenlik Şifreli (Her 30 sn'de yenilenir)",
                            fontSize = 11.sp,
                            color = SecondaryEmerald,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showLargeQrDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kapat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal: NFC Contactless Turnstile Simulation
    if (showNfcSimulation) {
        Dialog(onDismissRequest = { showNfcSimulation = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SecondaryEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SecondaryEmerald,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Turnike Geçişi Onaylandı!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SecondaryEmerald
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${if (idTypeTab == 0) currentStudent.full_name else state.currentUser.full_name} için temassız NFC akıllı kart okundu. Kampüs ana turnike kapısı açıldı.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Cihaz:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Ana Giriş Turnikesi #01", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Yoklama Durumu:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Geldi (Giriş: 08:32)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SecondaryEmerald)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { showNfcSimulation = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tamam", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal: Wallet Saved Toast
    if (showWalletSavedToast) {
        Dialog(onDismissRequest = { showWalletSavedToast = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Cüzdana Başarıyla Eklendi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "SCS360 Dijital Kimlik Kartınız telefonunuzun yerel cüzdanına (Apple Wallet / Google Wallet) entegre edildi. Kilit ekranından temassız okutabilirsiniz.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showWalletSavedToast = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Anladım")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STUDENT ID CARD FRONT FACE
// -------------------------------------------------------------
@Composable
fun StudentIdCardFront(
    student: Student,
    institutionName: String,
    branchName: String,
    onQrClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .shadow(12.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // Deep Slate Navy
                            Color(0xFF1E3A8A), // Royal Deep Blue
                            Color(0xFF1E293B)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
                .padding(16.dp)
        ) {
            // Subtle Security Watermark Pattern Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val circleRadius = 140.dp.toPx()
                drawCircle(
                    color = Color.White.copy(alpha = 0.03f),
                    radius = circleRadius,
                    center = Offset(size.width * 0.9f, size.height * 0.2f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.02f),
                    radius = circleRadius * 1.5f,
                    center = Offset(size.width * 0.9f, size.height * 0.2f)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = institutionName.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = "$branchName • ÖĞRENCİ KİMLİK KARTI",
                            color = Color(0xFF93C5FD),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Chip & Contactless Wave Icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Contactless,
                            contentDescription = "Temassız",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Gold EMV Chip Simulation
                        Surface(
                            color = Color(0xFFFBBF24),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .width(28.dp)
                                .height(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawLine(Color(0xFFB45309), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1f)
                                    drawLine(Color(0xFFB45309), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 1f)
                                }
                            }
                        }
                    }
                }

                // Middle Main Content Row: Photo + Info + QR Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Avatar / Student Photo
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E40AF))
                            .border(BorderStroke(2.dp, Color(0xFF60A5FA)), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.full_name.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Middle: Student Data Columns
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.full_name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "No: ${student.student_number} • Sınıf: ${student.class_name}",
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kan: ${student.blood_type ?: "A Rh+"}",
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "D.Tarihi: ${student.birth_date}",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right: Quick Scan QR Code Box
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .size(68.dp)
                            .clickable { onQrClick() }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DynamicQrCodeCanvas(seed = student.id, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // Bottom Footer: Holographic Valid Badge & Barcode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF059669).copy(alpha = 0.35f))
                                .border(BorderStroke(1.dp, Color(0xFF34D399)), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "2026-2027 GEÇERLİ",
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "T.C. MEB Akredite",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp
                        )
                    }

                    // Micro Barcode Simulation
                    Canvas(
                        modifier = Modifier
                            .width(90.dp)
                            .height(16.dp)
                    ) {
                        val barCount = 28
                        val step = size.width / barCount
                        for (i in 0 until barCount) {
                            val isThick = (i % 3 == 0 || i % 7 == 0)
                            drawLine(
                                color = Color.White.copy(alpha = 0.85f),
                                start = Offset(i * step, 0f),
                                end = Offset(i * step, size.height),
                                strokeWidth = if (isThick) 2.5f else 1f
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STUDENT ID CARD BACK FACE
// -------------------------------------------------------------
@Composable
fun StudentIdCardBack(
    student: Student,
    institutionName: String,
    branchName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .shadow(12.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF0F172A),
                            Color(0xFF1E3A8A)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Magnetic Stripe Simulation at top
                Surface(
                    color = Color(0xFF000000).copy(alpha = 0.65f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = "ISO/IEC 7810 SMART CARD • ID-1",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 8.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                // Emergency & Contact Info Blocks
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Veli / Acil İletişim:",
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${student.parent_name} (${student.parent_phone})",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sağlık / Alerji Durumu:",
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = student.allergies_or_diet ?: "Bilinen alerji yok",
                            color = if (student.allergies_or_diet != null && student.allergies_or_diet != "Yok") Color(0xFFFCA5A5) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kampüs & Danışma Tel:",
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "+90 (216) 555 0101",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Official Seal and Disclaimer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bu kart kurum malıdır. Bulunduğunda lütfen",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.sp
                        )
                        Text(
                            text = "$institutionName okul idaresine teslim ediniz.",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.sp
                        )
                    }

                    // Mühür / Stamp badge
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(1.5.dp, Color(0xFF60A5FA).copy(alpha = 0.5f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SCS360\nONAYLI",
                            color = Color(0xFF93C5FD),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 9.sp
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PARENT / STAFF ID CARD FRONT FACE
// -------------------------------------------------------------
@Composable
fun ParentStaffIdCardFront(
    user: User,
    students: List<Student>,
    onQrClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .shadow(12.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF14532D), // Deep Emerald Green
                            Color(0xFF0F766E), // Teal Slate
                            Color(0xFF0F172A)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = user.institution_name.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = "${user.branch_name} • YETKİLİ VELİ / PERSONEL KİMLİĞİ",
                            color = Color(0xFF86EFAC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Contactless,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Middle: User Info + QR Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF065F46))
                            .border(BorderStroke(2.dp, Color(0xFF4ADE80)), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.full_name.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.full_name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Rol: ${user.role} • Yetkili Veli",
                            color = Color(0xFFD1FAE5),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Teslim Yetkisi: ${students.firstOrNull()?.full_name ?: "Öğrenci Kayıtlı"}",
                            color = Color(0xFF86EFAC),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // QR Code
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .size(68.dp)
                            .clickable { onQrClick() }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DynamicQrCodeCanvas(seed = user.id, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // Bottom Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF047857).copy(alpha = 0.5f))
                            .border(BorderStroke(1.dp, Color(0xFF4ADE80)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TURNİKE VE ÖĞRENCİ TESLİM YETKİLİSİ",
                            color = Color(0xFFD1FAE5),
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp
                        )
                    }

                    Text(
                        text = "ID: ${user.id.take(8).uppercase()}",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun ParentStaffIdCardBack(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .shadow(12.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF064E3B),
                            Color(0xFF0F172A),
                            Color(0xFF134E4A)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = "AUTHORIZED GUARDIAN / PARENT SMART CARD",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 8.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "İletişim & Güvenlik Bilgileri",
                        color = Color(0xFF86EFAC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "E-Posta: ${user.email}",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Telefon: ${user.phone ?: "+90 532 555 0101"}",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Kurum: ${user.institution_name} (${user.branch_name})",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "Bu kart sahibi öğrenciyi kurumdan teslim almaya ve okul kampüsüne giriş yapmaya tam yetkilidir.",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// DYNAMIC QR CODE VECTOR CANVAS COMPOSABLE
// -------------------------------------------------------------
@Composable
fun DynamicQrCodeCanvas(
    seed: String,
    modifier: Modifier = Modifier,
    qrColor: Color = Color.Black
) {
    val random = remember(seed) { Random(seed.hashCode()) }
    val gridSize = 17 // 17x17 mini QR matrix grid

    Canvas(modifier = modifier) {
        val cellSize = size.width / gridSize

        // 1. Draw Finder Patterns (Top-Left, Top-Right, Bottom-Left)
        drawFinderPattern(0f, 0f, cellSize, qrColor)
        drawFinderPattern((gridSize - 5) * cellSize, 0f, cellSize, qrColor)
        drawFinderPattern(0f, (gridSize - 5) * cellSize, cellSize, qrColor)

        // 2. Draw Data Modules Matrix
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                // Skip finder pattern zones
                val inTopLeft = r < 5 && c < 5
                val inTopRight = r < 5 && c >= gridSize - 5
                val inBottomLeft = r >= gridSize - 5 && c < 5
                if (inTopLeft || inTopRight || inBottomLeft) continue

                // Pseudo random pattern based on seed
                val isFilled = ((r * 7 + c * 13 + seed.hashCode()) % 3 == 0) ||
                        ((r + c) % 2 == 0 && (r * c) % 5 == 0)

                if (isFilled) {
                    drawRoundRect(
                        color = qrColor,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize * 0.92f, cellSize * 0.92f),
                        cornerRadius = CornerRadius(cellSize * 0.15f, cellSize * 0.15f)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinderPattern(
    x: Float,
    y: Float,
    cellSize: Float,
    color: Color
) {
    val patternSize = 5 * cellSize
    // Outer Border
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(patternSize, patternSize),
        cornerRadius = CornerRadius(cellSize * 0.4f, cellSize * 0.4f),
        style = Stroke(width = cellSize * 0.9f)
    )
    // Center Solid Core
    val centerOffset = 1.6f * cellSize
    val centerSize = 1.8f * cellSize
    drawRoundRect(
        color = color,
        topLeft = Offset(x + centerOffset, y + centerOffset),
        size = Size(centerSize, centerSize),
        cornerRadius = CornerRadius(cellSize * 0.3f, cellSize * 0.3f)
    )
}
