package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BroadcastJob
import com.example.data.model.DeliveryReport
import com.example.data.model.MessageTemplate
import com.example.data.model.SmsProvider
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboxBroadcastScreen(
    state: Okul360UiState,
    onBack: () -> Unit,
    onCreateBroadcast: (BroadcastJob) -> Unit,
    onSendBroadcast: (String) -> Unit,
    onCreateTemplate: (MessageTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Giden Kutusu", "Mesaj Şablonları", "Sağlayıcılar & Bakiye", "Teslimat Raporları")
    var showNewBroadcastDialog by remember { mutableStateOf(false) }
    var showNewTemplateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Toplu İletişim & Giden Kutusu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "SMS, WhatsApp ve E-Posta Gönderim Merkezi",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showNewBroadcastDialog = true }) {
                            Icon(Icons.Default.Send, contentDescription = "Yeni Gönderim", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else if (selectedTab == 1) {
                        IconButton(onClick = { showNewTemplateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Yeni Şablon", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showNewBroadcastDialog = true },
                    icon = { Icon(Icons.Default.Send, contentDescription = null) },
                    text = { Text("Yeni Toplu Gönderim") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            } else if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showNewTemplateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Yeni Şablon Ekle") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> BroadcastJobsTab(
                    state = state,
                    jobs = state.broadcastJobs,
                    onSend = onSendBroadcast
                )
                1 -> TemplatesTab(
                    templates = state.messageTemplates,
                    onUseTemplate = { tpl ->
                        // Switch to create broadcast with template prefilled
                        showNewBroadcastDialog = true
                    }
                )
                2 -> ProvidersTab(providers = state.smsProviders)
                3 -> DeliveryReportsTab(reports = state.deliveryReports)
            }
        }
    }

    // New Broadcast Dialog
    if (showNewBroadcastDialog) {
        var title by remember { mutableStateOf("") }
        var channel by remember { mutableStateOf("SMS") }
        var audience by remember { mutableStateOf("ALL_PARENTS") }
        var content by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewBroadcastDialog = false },
            title = { Text("Yeni Toplu Gönderim", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Görev Başlığı") },
                        placeholder = { Text("Örn: Veli Toplantısı Daveti") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = channel,
                            onValueChange = { channel = it },
                            label = { Text("Kanal (SMS/WHATSAPP/EMAIL)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = audience,
                            onValueChange = { audience = it },
                            label = { Text("Hedef Kitle") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Mesaj Metni") },
                        placeholder = { Text("Sayın Velimiz...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            onCreateBroadcast(
                                BroadcastJob(
                                    id = "bc-${System.currentTimeMillis()}",
                                    title = title,
                                    channel = channel.uppercase(),
                                    target_audience = audience,
                                    content = content,
                                    total_recipients = if (audience == "ALL_PARENTS") 340 else 45,
                                    delivered_count = 0,
                                    failed_count = 0,
                                    status = "SENT",
                                    created_at = "Şimdi"
                                )
                            )
                            showNewBroadcastDialog = false
                        }
                    }
                ) {
                    Text("Kuyruğa Ekle & Gönder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBroadcastDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // New Template Dialog
    if (showNewTemplateDialog) {
        var code by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var channel by remember { mutableStateOf("SMS") }
        var templateBody by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewTemplateDialog = false },
            title = { Text("Yeni Mesaj Şablonu", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Şablon Adı") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Kod") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = channel,
                            onValueChange = { channel = it },
                            label = { Text("Kanal") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = templateBody,
                        onValueChange = { templateBody = it },
                        label = { Text("Şablon Metni ({student_name}, {date})") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && templateBody.isNotBlank()) {
                            onCreateTemplate(
                                MessageTemplate(
                                    id = "tpl-${System.currentTimeMillis()}",
                                    code = code.ifBlank { "TPL_${System.currentTimeMillis() % 1000}" },
                                    name = name,
                                    channel = channel.uppercase(),
                                    body_template = templateBody,
                                    subject = name
                                )
                            )
                            showNewTemplateDialog = false
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTemplateDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun BroadcastJobsTab(
    state: Okul360UiState,
    jobs: List<BroadcastJob>,
    onSend: (String) -> Unit
) {
    if (jobs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Kuyrukta toplu gönderim bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                RoleContextualAiCard(
                    screenName = "OUTBOX",
                    state = state
                )
            }
            items(jobs, key = { it.id }) { job ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (job.channel) {
                                    "WHATSAPP" -> Color(0xFF25D366).copy(alpha = 0.15f)
                                    "EMAIL" -> Color(0xFFEA4335).copy(alpha = 0.15f)
                                    else -> PrimaryBlue.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    job.channel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = when (job.channel) {
                                        "WHATSAPP" -> Color(0xFF1E824C)
                                        "EMAIL" -> Color(0xFFC0392B)
                                        else -> PrimaryBlue
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (job.status == "SENT") StatusGreen.copy(alpha = 0.15f) else Color(0xFFD97706).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    if (job.status == "SENT") "İletildi" else "Kuyrukta Bekliyor",
                                    color = if (job.status == "SENT") StatusGreen else Color(0xFFD97706),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(job.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(job.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Alıcı: ${job.total_recipients} • Başarılı: ${job.delivered_count} • Hata: ${job.failed_count}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (job.status != "SENT") {
                                Button(
                                    onClick = { onSend(job.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Şimdi Gönder", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplatesTab(
    templates: List<MessageTemplate>,
    onUseTemplate: (MessageTemplate) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(templates, key = { it.id }) { tpl ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tpl.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                tpl.channel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(tpl.body_template, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kod: ${tpl.code}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        TextButton(onClick = { onUseTemplate(tpl) }) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bu Şablonla Gönder", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProvidersTab(providers: List<SmsProvider>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Aktif SMS Ağ Geçidi Entegrasyonu", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Okul velilerine anlık devamsızlık ve taksit SMS gönderimleri bu servis üzerinden yönlendirilir.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        items(providers, key = { it.id }) { provider ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(provider.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Başlık: ${provider.sender_header}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (provider.is_active) "🟢 Durum: Aktif ve Bağlı" else "⚪ Durum: Pasif",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (provider.is_active) StatusGreen else MaterialTheme.colorScheme.outline
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Kalan Kredi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${provider.balance_credits} SMS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryReportsTab(
    reports: List<DeliveryReport>,
    modifier: Modifier = Modifier
) {
    var channelFilter by remember { mutableStateOf("ALL") }

    val defaultReports = remember(reports) {
        if (reports.isNotEmpty()) reports else listOf(
            DeliveryReport("del-1", "job-1", "Mehmet Demir (Ali Veli)", "+90 532 111 2233", "SMS", "DELIVERED", "Bugün 14:30"),
            DeliveryReport("del-2", "job-1", "Ayşe Kaya (Zeynep Veli)", "+90 533 222 3344", "SMS", "DELIVERED", "Bugün 14:31"),
            DeliveryReport("del-3", "job-1", "Fatma Çelik (Burak Veli)", "+90 535 333 4455", "SMS", "FAILED", "Bugün 14:32", "Operatör hatası: Numara servise kapalı"),
            DeliveryReport("del-4", "job-2", "Kemal Şahin (Elif Veli)", "+90 542 444 5566", "WHATSAPP", "DELIVERED", "Bugün 15:00"),
            DeliveryReport("del-5", "job-2", "Selin Arslan (Can Veli)", "+90 555 555 6677", "PUSH", "DELIVERED", "Bugün 15:05"),
            DeliveryReport("del-6", "job-3", "Ahmet Koç (Kerem Veli)", "ahmet.koc@example.com", "EMAIL", "DELIVERED", "Dün 10:15")
        )
    }

    val filtered = remember(defaultReports, channelFilter) {
        if (channelFilter == "ALL") defaultReports else defaultReports.filter { it.channel.equals(channelFilter, ignoreCase = true) }
    }

    val totalCount = defaultReports.size
    val deliveredCount = defaultReports.count { it.status.equals("DELIVERED", ignoreCase = true) }
    val failedCount = defaultReports.count { it.status.equals("FAILED", ignoreCase = true) }
    val pendingCount = defaultReports.count { it.status.equals("PENDING", ignoreCase = true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Summary Cards
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Canlı Teslimat ve İletim İstatistikleri", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Toplam", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalCount", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("İletildi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$deliveredCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
                        Column {
                            Text("Başarısız", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$failedCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                        Column {
                            Text("Bekleyen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$pendingCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }
                }
            }
        }

        // Channel filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Tümü",
                    "SMS" to "SMS",
                    "WHATSAPP" to "WhatsApp",
                    "EMAIL" to "E-Posta",
                    "PUSH" to "Push"
                ).forEach { (code, label) ->
                    FilterChip(
                        selected = channelFilter == code,
                        onClick = { channelFilter = code },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Report Items
        items(filtered, key = { it.id }) { item ->
            val (statusText, statusBg, statusColor) = when (item.status) {
                "DELIVERED" -> Triple("İletildi", Color(0xFF059669).copy(alpha = 0.12f), Color(0xFF059669))
                "FAILED" -> Triple("İletilemedi", Color(0xFFDC2626).copy(alpha = 0.12f), Color(0xFFDC2626))
                else -> Triple("Gönderimde", Color(0xFFD97706).copy(alpha = 0.12f), Color(0xFFD97706))
            }

            val channelIcon = when (item.channel) {
                "WHATSAPP" -> Icons.Default.Chat
                "EMAIL" -> Icons.Default.Email
                "PUSH" -> Icons.Default.Notifications
                else -> Icons.Default.Sms
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(channelIcon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(item.recipient_name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(item.recipient_address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusBg
                        ) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (item.error_message != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                Text(item.error_message, fontSize = 11.sp, color = Color(0xFFDC2626))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "İşlem Zamanı: ${item.sent_at.ifBlank { "Bilinmiyor" }}",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
