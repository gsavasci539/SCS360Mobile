package com.example.ui.screens

import com.example.ui.components.ResponsiveTableScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HardwareDevice
import com.example.data.model.RfidCard
import com.example.ui.components.CompactResponsiveCard
import com.example.ui.components.CompactTableRow
import com.example.ui.components.DataTableViewMode
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.StickyTableHeaderRow
import com.example.ui.components.TableColumnSpec
import com.example.ui.components.isCompactCardMode
import com.example.ui.components.rememberResolvedTableMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RfidDevicesScreen(
    state: Okul360UiState,
    onBack: () -> Unit,
    onAddCard: (RfidCard) -> Unit,
    onToggleCardStatus: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("RFID Kartlar", "Donanım Cihazları")
    var showAddCardDialog by remember { mutableStateOf(false) }
    val tableModeState = rememberResolvedTableMode()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kartlar & Cihazlar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = "/rfid-cards • /devices",
                            fontSize = 11.sp,
                            color = NeutralGrayDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_rfid_back")
                    ) {
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
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddCardDialog = true },
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    text = { Text("Yeni Kart Eşle") },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_rfid_card")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = PrimaryBlue
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (index == 0) Icons.Outlined.Contactless else Icons.Outlined.Router,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                RoleContextualAiCard(
                    screenName = "RFID_DEVICES",
                    state = state
                )
            }

            if (selectedTabIndex == 0) {
                RfidCardsTab(
                    cards = state.rfidCards,
                    tableModeState = tableModeState,
                    onToggleStatus = onToggleCardStatus
                )
            } else {
                HardwareDevicesTab(
                    devices = state.hardwareDevices,
                    tableModeState = tableModeState
                )
            }
        }
    }

    if (showAddCardDialog) {
        AddRfidCardDialog(
            onDismiss = { showAddCardDialog = false },
            onConfirm = { card ->
                onAddCard(card)
                showAddCardDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RfidCardsTab(
    cards: List<RfidCard>,
    tableModeState: MutableState<DataTableViewMode>,
    onToggleStatus: (String) -> Unit
) {
    val isCardMode = isCompactCardMode(tableModeState.value)

    ResponsiveTableScope() {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stickyHeader {
                StickyTableHeaderRow(
                    columns = listOf(
                        TableColumnSpec("Kart Sahibi / Sınıf", 1.4f),
                        TableColumnSpec("Kart UID / Son Geçiş", 1.25f),
                        TableColumnSpec("Bakiye / Durum", 1.05f, TextAlign.End)
                    ),
                    totalCount = cards.size,
                    viewModeState = tableModeState
                )
            }
    
            itemsIndexed(cards, key = { _, it -> it.id }) { index, card ->
                val isActive = card.status == "ACTIVE"
                if (isCardMode) {
                    CompactResponsiveCard(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("card_rfid_${card.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) PrimaryBlue.copy(alpha = 0.12f) else StatusRed.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contactless,
                                    contentDescription = null,
                                    tint = if (isActive) PrimaryBlue else StatusRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = card.holder_name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${card.class_or_dept} • UID: ${card.card_uid}",
                                    fontSize = 11.sp,
                                    color = NeutralGrayDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "₺${card.balance.toInt()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            SuggestionChip(
                                onClick = { onToggleStatus(card.id) },
                                label = { Text(if (isActive) "AKTİF" else "BLOKE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isActive) StatusGreen.copy(alpha = 0.12f) else StatusRed.copy(alpha = 0.12f),
                                    labelColor = if (isActive) StatusGreen else StatusRed
                                ),
                                border = null
                            )
                        }
                        Text(
                            text = "Son Hareket: ${card.last_tap_time}",
                            fontSize = 10.5.sp,
                            color = NeutralGrayDark
                        )
                    }
                } else {
                    CompactTableRow(
                        index = index,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("card_rfid_${card.id}")
                    ) {
                        Column(modifier = Modifier.weight(1.4f).padding(end = 6.dp)) {
                            Text(card.holder_name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = BrandNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(card.class_or_dept, fontSize = 10.5.sp, color = NeutralGrayDark, maxLines = 1)
                        }
                        Column(modifier = Modifier.weight(1.25f).padding(end = 6.dp)) {
                            Text(card.card_uid, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(card.last_tap_time, fontSize = 10.sp, color = NeutralGrayDark, maxLines = 1)
                        }
                        Row(
                            modifier = Modifier.weight(1.05f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("₺${card.balance.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            SuggestionChip(
                                onClick = { onToggleStatus(card.id) },
                                label = { Text(if (isActive) "AKTİF" else "BLOKE", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isActive) StatusGreen.copy(alpha = 0.12f) else StatusRed.copy(alpha = 0.12f),
                                    labelColor = if (isActive) StatusGreen else StatusRed
                                ),
                                border = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HardwareDevicesTab(
    devices: List<HardwareDevice>,
    tableModeState: MutableState<DataTableViewMode>
) {
    val isCardMode = isCompactCardMode(tableModeState.value)

    ResponsiveTableScope() {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stickyHeader {
                StickyTableHeaderRow(
                    columns = listOf(
                        TableColumnSpec("Cihaz / Şube", 1.45f),
                        TableColumnSpec("IP / MAC / FW", 1.3f),
                        TableColumnSpec("Durum / Ping", 0.95f, TextAlign.End)
                    ),
                    totalCount = devices.size,
                    viewModeState = tableModeState
                )
            }
    
            itemsIndexed(devices, key = { _, it -> it.id }) { index, dev ->
                val isOnline = dev.status == "ONLINE"
                if (isCardMode) {
                    CompactResponsiveCard(
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isOnline) StatusGreen.copy(alpha = 0.12f) else StatusRed.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (dev.device_type) {
                                        "TURNSTILE" -> Icons.Default.SensorDoor
                                        "CANTEEN_POS" -> Icons.Default.PointOfSale
                                        else -> Icons.Default.TabletMac
                                    },
                                    contentDescription = null,
                                    tint = if (isOnline) StatusGreen else StatusRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${dev.branch_name} • IP: ${dev.ip_address}",
                                    fontSize = 11.sp,
                                    color = NeutralGrayDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) StatusGreen else StatusRed)
                                )
                                Text(
                                    text = if (isOnline) "ONLINE" else "OFFLINE",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) StatusGreen else StatusRed
                                )
                            }
                        }
    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "MAC: ${dev.mac_address} • FW: ${dev.fw_version}", fontSize = 10.5.sp, color = NeutralGrayDark)
                            Text(text = "Ping: ${dev.last_ping_at}", fontSize = 10.5.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    CompactTableRow(
                        index = index,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.45f).padding(end = 6.dp)) {
                            Text(dev.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = BrandNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(dev.branch_name, fontSize = 10.5.sp, color = NeutralGrayDark, maxLines = 1)
                        }
                        Column(modifier = Modifier.weight(1.3f).padding(end = 6.dp)) {
                            Text("IP: ${dev.ip_address}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("${dev.mac_address} • ${dev.fw_version}", fontSize = 10.sp, color = NeutralGrayDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(modifier = Modifier.weight(0.95f), horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isOnline) "● ONLINE" else "● OFFLINE",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) StatusGreen else StatusRed
                            )
                            Text(dev.last_ping_at, fontSize = 10.sp, color = PrimaryBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRfidCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (RfidCard) -> Unit
) {
    var cardUid by remember { mutableStateOf("E2:80:" + (10..99).random() + ":" + (10..99).random()) }
    var holderName by remember { mutableStateOf("") }
    var classOrDept by remember { mutableStateOf("") }
    var holderType by remember { mutableStateOf("STUDENT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni RFID Kartı Eşle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = cardUid,
                    onValueChange = { cardUid = it },
                    label = { Text("Kart UID (Okuyucudan Gelen)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text("Kart Sahibi Ad Soyad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = classOrDept,
                    onValueChange = { classOrDept = it },
                    label = { Text("Sınıf veya Görev / Departman") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = holderType == "STUDENT",
                        onClick = { holderType = "STUDENT" },
                        label = { Text("Öğrenci") }
                    )
                    FilterChip(
                        selected = holderType == "STAFF",
                        onClick = { holderType = "STAFF" },
                        label = { Text("Personel") }
                    )
                    FilterChip(
                        selected = holderType == "VISITOR",
                        onClick = { holderType = "VISITOR" },
                        label = { Text("Ziyaretçi") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (holderName.isNotBlank() && cardUid.isNotBlank()) {
                        val newCard = RfidCard(
                            id = "rfid-${System.currentTimeMillis()}",
                            card_uid = cardUid,
                            holder_type = holderType,
                            holder_id = "std-${System.currentTimeMillis()}",
                            holder_name = holderName,
                            class_or_dept = classOrDept,
                            balance = 0.0,
                            status = "ACTIVE",
                            last_tap_time = "Yeni Tanımlandı",
                            issued_date = "2026-09-08"
                        )
                        onConfirm(newCard)
                    }
                },
                enabled = holderName.isNotBlank() && cardUid.isNotBlank()
            ) {
                Text("Eşle & Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
