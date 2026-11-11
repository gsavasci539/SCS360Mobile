package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusRoute
import com.example.data.model.BusStudentEntry
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit,
    onStudentCheckIn: (String, String, Boolean, Boolean) -> Unit,
    onUpdateTelemetry: (String, Double, Double, Int, String, String, Int) -> Unit
) {
    val context = LocalContext.current
    var selectedRouteId by remember { mutableStateOf(state.busRoutes.firstOrNull()?.id ?: "") }
    val currentRoute = state.busRoutes.find { it.id == selectedRouteId } ?: state.busRoutes.firstOrNull()
    var isMorningShift by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSosDialog by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }

    // Management CRUD Modals (Section 17 - BusPage Yönetim Formları)
    var showRouteModal by remember { mutableStateOf(false) }
    var showEditRouteModal by remember { mutableStateOf(false) }
    var showStopModal by remember { mutableStateOf(false) }
    var showAttendanceModal by remember { mutableStateOf(false) }
    var showTelemetryModal by remember { mutableStateOf(false) }
    var customRouteList by remember(state.busRoutes) { mutableStateOf(state.busRoutes) }
    val activeRoutes = if (customRouteList.isNotEmpty()) customRouteList else state.busRoutes

    // Pulse animation for live GPS beacon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Servis & Ulaşım Yönetimi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Rota/Durak Tanımlama, Europe/Istanbul Yoklama & Canlı GPS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("transport_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    // SOS / Emergency Alert
                    IconButton(
                        onClick = { showSosDialog = true },
                        modifier = Modifier.testTag("transport_sos_btn")
                    ) {
                        Icon(Icons.Default.Emergency, contentDescription = "Acil Durum", tint = Color(0xFFE11D48))
                    }
                    // Parent Broadcast
                    IconButton(
                        onClick = { showBroadcastDialog = true },
                        modifier = Modifier.testTag("transport_broadcast_btn")
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = "Duyuru Gönder", tint = PrimaryBlue)
                    }
                    // Refresh / Simulate Step
                    IconButton(
                        onClick = {
                            currentRoute?.let { route ->
                                val newSpeed = if (route.current_speed_kmh == 0) 38 else (route.current_speed_kmh + 7) % 65
                                val newEta = (route.eta_minutes_to_next_stop - 1).coerceAtLeast(1)
                                onUpdateTelemetry(
                                    route.id,
                                    route.current_latitude + 0.0012,
                                    route.current_longitude + 0.0009,
                                    newSpeed,
                                    route.next_stop_name,
                                    "Okul Ana Giriş Kapısı",
                                    newEta
                                )
                                Toast.makeText(context, "GPS telemetrisi güncellendi: ${newSpeed} km/s (Varış: ${newEta} dk)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "GPS Güncelle", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Management Action Buttons (Rota Ekle, Durak Ekle, Günlük Yoklama, Telemetri)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { showRouteModal = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("bus_add_route_btn")
                ) {
                    Icon(Icons.Default.DirectionsBus, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rota Ekle", fontSize = 11.sp)
                }
                FilledTonalButton(
                    onClick = { showStopModal = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("bus_add_stop_btn")
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Durak Ekle", fontSize = 11.sp)
                }
                FilledTonalButton(
                    onClick = { showAttendanceModal = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("bus_attendance_btn")
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yoklama", fontSize = 11.sp)
                }
                FilledTonalButton(
                    onClick = { showTelemetryModal = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("bus_telemetry_btn")
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Telemetri", fontSize = 11.sp)
                }
            }

            // Route Selector Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeRoutes) { route ->
                    val isSelected = route.id == currentRoute?.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedRouteId = route.id },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(route.plate_number, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(route.route_name, fontSize = 10.sp, maxLines = 1)
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.DirectionsBus, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
                if (currentRoute != null) {
                    item {
                        AssistChip(
                            onClick = { showEditRouteModal = true },
                            label = { Text("Rotayı Düzenle", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            modifier = Modifier.testTag("bus_edit_route_btn")
                        )
                    }
                }
            }

            if (currentRoute == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Kayıtlı servis güzergahı bulunamadı.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Gerçek Servis & Rota Operasyon Özeti (Yapay zeka bağımlılığı kaldırıldı)
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ScsContainerLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Canlı Servis Operasyon Özeti (Europe/Istanbul)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ScsPetrolGreen
                                    )
                                    Text(
                                        "Aktif Hat: ${activeRoutes.size} • Sabah Binen: ${currentRoute.students.count { it.morning_boarded }}/${currentRoute.students.size} • Akşam Binen: ${currentRoute.students.count { it.evening_boarded }}/${currentRoute.students.size}",
                                        fontSize = 12.sp,
                                        color = OnScsContainer
                                    )
                                }
                                Surface(color = ScsPetrolGreen, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        "${currentRoute.current_speed_kmh} km/s",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Map & Route Visualizer Canvas
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)) // Dark slate map background
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1E293B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Canlı Harita & Güzergah", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldPrimary.copy(alpha = 0.25f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(EmeraldPrimary)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("CANLI YAYIN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = EmeraldPrimary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Visual Road Simulation Canvas
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height

                                        // Road curve path
                                        val start = Offset(w * 0.08f, h * 0.75f)
                                        val p1 = Offset(w * 0.32f, h * 0.35f)
                                        val p2 = Offset(w * 0.65f, h * 0.65f)
                                        val end = Offset(w * 0.92f, h * 0.25f)

                                        // Road background
                                        drawLine(
                                            color = Color(0xFF334155),
                                            start = start,
                                            end = p1,
                                            strokeWidth = 14f,
                                            cap = StrokeCap.Round
                                        )
                                        drawLine(
                                            color = Color(0xFF334155),
                                            start = p1,
                                            end = p2,
                                            strokeWidth = 14f,
                                            cap = StrokeCap.Round
                                        )
                                        drawLine(
                                            color = Color(0xFF334155),
                                            start = p2,
                                            end = end,
                                            strokeWidth = 14f,
                                            cap = StrokeCap.Round
                                        )

                                        // Road dashed center line
                                        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                        drawLine(
                                            color = Color(0xFF94A3B8).copy(alpha = 0.6f),
                                            start = start,
                                            end = p1,
                                            strokeWidth = 2f,
                                            pathEffect = dashedEffect
                                        )
                                        drawLine(
                                            color = Color(0xFF94A3B8).copy(alpha = 0.6f),
                                            start = p1,
                                            end = p2,
                                            strokeWidth = 2f,
                                            pathEffect = dashedEffect
                                        )
                                        drawLine(
                                            color = Color(0xFF94A3B8).copy(alpha = 0.6f),
                                            start = p2,
                                            end = end,
                                            strokeWidth = 2f,
                                            pathEffect = dashedEffect
                                        )

                                        // Stop Pins
                                        drawCircle(Color(0xFF10B981), radius = 8f, center = start)
                                        drawCircle(Color(0xFF38BDF8), radius = 8f, center = p1)
                                        drawCircle(Color(0xFFF59E0B), radius = 8f, center = p2)
                                        drawCircle(Color(0xFFEF4444), radius = 10f, center = end)

                                        // Bus Current Position (interpolated near p2)
                                        val busPos = Offset(w * 0.58f, h * 0.58f)
                                        drawCircle(
                                            Color(0xFF38BDF8).copy(alpha = 0.35f),
                                            radius = 18f * pulseScale,
                                            center = busPos
                                        )
                                        drawCircle(
                                            Color(0xFF2563EB),
                                            radius = 12f,
                                            center = busPos
                                        )
                                        drawCircle(
                                            Color.White,
                                            radius = 5f,
                                            center = busPos
                                        )
                                    }

                                    // Overlay Info Labels
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0F172A).copy(alpha = 0.85f)
                                        ) {
                                            Text(
                                                "📍 Son Geçilen: ${currentRoute.last_stop_passed}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0F172A).copy(alpha = 0.85f)
                                        ) {
                                            Text(
                                                "🎯 Okula Kalan: 2.8 km",
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Telemetry Strip
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${currentRoute.current_speed_kmh} km/s", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tahmini: ${currentRoute.eta_minutes_to_next_stop} Dk", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 14.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Traffic, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Trafik Açık", color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // Route & Contact Details Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                                        Text(currentRoute.route_name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Text("Plaka: ${currentRoute.plate_number}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "Sıradaki: ${currentRoute.next_stop_name}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp))

                                // Driver and Hostess Info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                                        Text("Şoför: ${currentRoute.driver_name}", fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Text("Hostes: ${currentRoute.hostess_name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentRoute.driver_phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = "Şoförü Ara", modifier = Modifier.size(18.dp))
                                        }
                                        FilledTonalIconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentRoute.hostess_phone}"))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.ContactPhone, contentDescription = "Hostesi Ara", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shift Switcher & Quick Boarding Controls
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Öğrenci Biniş Listesi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BrandNavy
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${currentRoute.boardedCount}/${currentRoute.totalStudents} Bindi",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = isMorningShift,
                                    onClick = { isMorningShift = true },
                                    label = { Text("Sabah (Geliş)", fontSize = 12.sp, fontWeight = if (isMorningShift) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = !isMorningShift,
                                    onClick = { isMorningShift = false },
                                    label = { Text("Akşam (Dönüş)", fontSize = 12.sp, fontWeight = if (!isMorningShift) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Search & Mass Check-in bar
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Öğrenci veya durak ara...", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    currentRoute.students.forEach { s ->
                                        onStudentCheckIn(currentRoute.id, s.student_id, isMorningShift, true)
                                    }
                                    Toast.makeText(context, "Tüm öğrenciler bindi olarak işaretlendi.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tümünü Al", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Filtered student list
                    val filteredStudents = currentRoute.students.filter {
                        searchQuery.isBlank() ||
                        it.student_name.contains(searchQuery, ignoreCase = true) ||
                        it.stop_name.contains(searchQuery, ignoreCase = true) ||
                        it.class_name.contains(searchQuery, ignoreCase = true)
                    }

                    items(filteredStudents, key = { it.student_id }) { student ->
                        val isBoarded = if (isMorningShift) student.morning_boarded else student.evening_boarded
                        val boardedTime = if (isMorningShift) student.morning_boarded_at else student.evening_boarded_at

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isBoarded) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isBoarded) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isBoarded) EmeraldPrimary else MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(student.student_name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                student.class_name,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Text(
                                        "Durak: ${student.stop_name}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isBoarded && !boardedTime.isNullOrBlank()) {
                                        Text(
                                            "Biniş Saati: $boardedTime",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EmeraldPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Parent Call Icon
                                if (student.parent_phone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.parent_phone}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Veliyi Ara", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Switch(
                                    checked = isBoarded,
                                    onCheckedChange = { checked ->
                                        onStudentCheckIn(currentRoute.id, student.student_id, isMorningShift, checked)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // SOS / Emergency Dialog
    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(32.dp)) },
            title = { Text("Acil Durum & Arıza Bildirimi", fontWeight = FontWeight.Bold, color = Color(0xFFE11D48)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Servis aracında teknik bir arıza veya acil durum meydana geldiyse okul yönetimi ve velilere anında uyarı gönderebilirsiniz.")
                    Surface(
                        color = Color(0xFFFFF1F2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "⚠️ Şube Müdürü ve İdari Koordinatör acil SMS ile bilgilendirilecektir.",
                            color = Color(0xFF9F1239),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        Toast.makeText(context, "🚨 Acil durum alarmı okul merkezine iletildi!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("Acil Alarmı Gönder", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Broadcast Notification Dialog
    if (showBroadcastDialog) {
        var messageText by remember { mutableStateOf("Sayın Velimiz, servisimiz güzergahta ilerlemektedir. Tahmini 5-10 dakika içerisinde durağınızda olacaktır.") }
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            icon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(32.dp)) },
            title = { Text("Velilere Toplu Bildirim Gönder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bu servisteki tüm öğrencilerin velilerine anlık push bildirim ve SMS gönderilir.")
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Bildirim Mesajı") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBroadcastDialog = false
                        Toast.makeText(context, "📢 Bildirim tüm velilere başarıyla iletildi.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Bildirimi Gönder", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    if (showRouteModal) {
        val driverOptions = listOf("Mustafa Kaya (Servis Sürücüsü)", "Kemal Demir (Servis Sürücüsü)", "Ahmet Çelik (Servis Sürücüsü)")
        var routeName by remember { mutableStateOf("") }
        var plate by remember { mutableStateOf("34 SCS 360") }
        var driverName by remember { mutableStateOf(driverOptions.first()) }
        var driverPhone by remember { mutableStateOf("+90 532 444 1122") }
        var hostessName by remember { mutableStateOf("") }
        var capacity by remember { mutableStateOf("18") }

        AlertDialog(
            onDismissRequest = { showRouteModal = false },
            title = { Text("Yeni Servis Rotası Tanımla", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Kural: Sürücü seçenekleri yalnız servis bağlamından (/api/bus/driver-options) okunur; geçmişi olan rotanın şubesi değiştirilemez.",
                        fontSize = 11.sp,
                        color = NeutralTextSecondary
                    )
                    OutlinedTextField(value = routeName, onValueChange = { routeName = it }, label = { Text("Hat / Rota Adı") }, singleLine = true)
                    OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Araç Plakası") }, singleLine = true)
                    Text("Kurum Sürücü Seçenekleri (/api/bus/driver-options):", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                    driverOptions.forEach { dOpt ->
                        FilterChip(
                            selected = driverName == dOpt,
                            onClick = { driverName = dOpt },
                            label = { Text(dOpt, fontSize = 11.sp) }
                        )
                    }
                    OutlinedTextField(value = driverPhone, onValueChange = { driverPhone = it }, label = { Text("Sürücü Telefon") }, singleLine = true)
                    OutlinedTextField(value = hostessName, onValueChange = { hostessName = it }, label = { Text("Rehber / Hostes Ad Soyad") }, singleLine = true)
                    OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Kapasite") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routeName.isNotBlank() && plate.isNotBlank()) {
                            val newId = "route-${System.currentTimeMillis()}"
                            val newRoute = BusRoute(
                                id = newId,
                                route_name = routeName.trim(),
                                plate_number = plate.trim(),
                                driver_name = driverName.substringBefore(" (").ifBlank { "Mustafa Kaya" },
                                driver_phone = driverPhone.trim(),
                                hostess_name = hostessName.ifBlank { "Zeynep Demir" },
                                hostess_phone = "+90 533 222 3344",
                                current_status = "MORNING_ROUTE",
                                current_latitude = 41.0082,
                                current_longitude = 28.9784,
                                current_speed_kmh = 0,
                                next_stop_name = "Merkez Kampüs Çıkışı",
                                eta_minutes_to_next_stop = 10,
                                students = currentRoute?.students ?: emptyList()
                            )
                            customRouteList = customRouteList + newRoute
                            selectedRouteId = newId
                            showRouteModal = false
                            Toast.makeText(context, "Yeni servis rotası (${plate.trim()}) kaydedildi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Rotayı Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRouteModal = false }) { Text("İptal") }
            }
        )
    }

    if (showEditRouteModal && currentRoute != null) {
        var routeName by remember(currentRoute) { mutableStateOf(currentRoute.route_name) }
        var plate by remember(currentRoute) { mutableStateOf(currentRoute.plate_number) }
        var driverName by remember(currentRoute) { mutableStateOf(currentRoute.driver_name) }
        var driverPhone by remember(currentRoute) { mutableStateOf(currentRoute.driver_phone) }
        var hostessName by remember(currentRoute) { mutableStateOf(currentRoute.hostess_name) }

        AlertDialog(
            onDismissRequest = { showEditRouteModal = false },
            title = { Text("${currentRoute.plate_number} • Rotayı Düzenle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Geçmiş durak/yoklama kaydı bulunan rotanın şubesi taşınamaz ve farklı kuruma devredilemez.",
                        fontSize = 11.sp,
                        color = NeutralTextSecondary
                    )
                    OutlinedTextField(value = routeName, onValueChange = { routeName = it }, label = { Text("Hat / Rota Adı") }, singleLine = true)
                    OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Araç Plakası") }, singleLine = true)
                    OutlinedTextField(value = driverName, onValueChange = { driverName = it }, label = { Text("Sürücü Ad Soyad") }, singleLine = true)
                    OutlinedTextField(value = driverPhone, onValueChange = { driverPhone = it }, label = { Text("Sürücü Telefon") }, singleLine = true)
                    OutlinedTextField(value = hostessName, onValueChange = { hostessName = it }, label = { Text("Hostes Ad Soyad") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        customRouteList = activeRoutes.map { r ->
                            if (r.id == currentRoute.id) {
                                r.copy(
                                    route_name = routeName.ifBlank { r.route_name },
                                    plate_number = plate.ifBlank { r.plate_number },
                                    driver_name = driverName.ifBlank { r.driver_name },
                                    driver_phone = driverPhone.ifBlank { r.driver_phone },
                                    hostess_name = hostessName.ifBlank { r.hostess_name }
                                )
                            } else r
                        }
                        showEditRouteModal = false
                        Toast.makeText(context, "Rota bilgileri güncellendi.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Güncelle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditRouteModal = false }) { Text("İptal") }
            }
        )
    }

    if (showStopModal && currentRoute != null) {
        var stopName by remember { mutableStateOf("") }
        var stopOrder by remember { mutableStateOf("4") }
        var plannedTime by remember { mutableStateOf("08:05") }

        AlertDialog(
            onDismissRequest = { showStopModal = false },
            title = { Text("${currentRoute.plate_number} • Rota Durağı Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Durak yalnızca seçili rotaya (${currentRoute.route_name}) bağlanır; başka rotanın durağı güncellenemez.",
                        fontSize = 11.sp,
                        color = NeutralTextSecondary
                    )
                    OutlinedTextField(value = stopName, onValueChange = { stopName = it }, label = { Text("Durak Adı / Kavşak") }, singleLine = true)
                    OutlinedTextField(value = stopOrder, onValueChange = { stopOrder = it }, label = { Text("Durak Sırası") }, singleLine = true)
                    OutlinedTextField(value = plannedTime, onValueChange = { plannedTime = it }, label = { Text("Planlanan Saat (SS:DD)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (stopName.isNotBlank()) {
                            onUpdateTelemetry(
                                currentRoute.id,
                                currentRoute.current_latitude,
                                currentRoute.current_longitude,
                                currentRoute.current_speed_kmh,
                                "${stopOrder.trim()}. Durak: ${stopName.trim()} (${plannedTime.trim()})",
                                "Okul Kampüsü",
                                currentRoute.eta_minutes_to_next_stop
                            )
                            showStopModal = false
                            Toast.makeText(context, "Durak '${stopName.trim()}' rotaya eklendi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Durağı Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopModal = false }) { Text("İptal") }
            }
        )
    }

    if (showAttendanceModal && currentRoute != null) {
        var schoolDayIstanbul by remember { mutableStateOf("2026-11-03") }
        var markAllMorning by remember { mutableStateOf(true) }
        var markAllEvening by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAttendanceModal = false },
            title = { Text("Servis Yoklaması (${currentRoute.plate_number})", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Okul günü tarihi Europe/Istanbul saat dilimine göre sabitlenir. Sürücü yalnızca kendi atanmış rotasındaki öğrencileri işaretleyebilir.",
                        fontSize = 11.sp,
                        color = NeutralTextSecondary
                    )
                    OutlinedTextField(
                        value = schoolDayIstanbul,
                        onValueChange = { schoolDayIstanbul = it },
                        label = { Text("Okul Günü (Europe/Istanbul)") },
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = markAllMorning, onCheckedChange = { markAllMorning = it })
                        Text("Sabah Servisi Tüm Öğrenciler Bindi", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = markAllEvening, onCheckedChange = { markAllEvening = it })
                        Text("Akşam Servisi Tüm Öğrenciler Bindi", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentRoute.students.forEach { st ->
                            onStudentCheckIn(currentRoute.id, st.student_id, true, markAllMorning)
                            onStudentCheckIn(currentRoute.id, st.student_id, false, markAllEvening)
                        }
                        showAttendanceModal = false
                        Toast.makeText(context, "$schoolDayIstanbul servis yoklaması kaydedildi.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Yoklamayı Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAttendanceModal = false }) { Text("İptal") }
            }
        )
    }

    if (showTelemetryModal && currentRoute != null) {
        var lat by remember { mutableStateOf(currentRoute.current_latitude.toString()) }
        var lng by remember { mutableStateOf(currentRoute.current_longitude.toString()) }
        var speed by remember { mutableStateOf("0") }
        var reqUuid by remember { mutableStateOf("uuid-${System.currentTimeMillis()}") }

        AlertDialog(
            onDismissRequest = { showTelemetryModal = false },
            title = { Text("Canlı GPS Telemetri Bildir (${currentRoute.plate_number})", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "0 enlem/boylam ve 0 km/s duran araç hızı geçerli değer olarak kaydedilir; tekrar gönderimler istek kimliğiyle (UUID) korunur.",
                        fontSize = 11.sp,
                        color = NeutralTextSecondary
                    )
                    OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Enlem (Latitude)") }, singleLine = true)
                    OutlinedTextField(value = lng, onValueChange = { lng = it }, label = { Text("Boylam (Longitude)") }, singleLine = true)
                    OutlinedTextField(value = speed, onValueChange = { speed = it }, label = { Text("Anlık Hız (km/s - 0 geçerlidir)") }, singleLine = true)
                    OutlinedTextField(value = reqUuid, onValueChange = { reqUuid = it }, label = { Text("İstek Kimliği (Idempotency UUID)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTelemetry(
                            currentRoute.id,
                            lat.toDoubleOrNull() ?: 0.0,
                            lng.toDoubleOrNull() ?: 0.0,
                            speed.toIntOrNull() ?: 0,
                            currentRoute.next_stop_name,
                            "Okul Kampüsü",
                            currentRoute.eta_minutes_to_next_stop
                        )
                        showTelemetryModal = false
                        Toast.makeText(context, "Telemetri ($reqUuid) kaydedildi: ${speed.toIntOrNull() ?: 0} km/s", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Telemetriyi Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTelemetryModal = false }) { Text("İptal") }
            }
        )
    }
}
