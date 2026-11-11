package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.context.rememberChatContext
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Data Model for AI Chatbot Conversation Log & Fine-Tuning Training Pair
 */
data class AiChatAuditLog(
    val id: String,
    val sessionId: String,
    val senderName: String,
    val senderRole: UserRole,
    val userQuery: String,
    val aiResponse: String,
    val topicCategory: String,
    val timestamp: String,
    val sentiment: String = "POSITIVE", // POSITIVE, NEUTRAL, QUESTION, CRITICAL
    val qualityRating: Int = 5, // 1 to 5
    val feedbackStatus: String = "VERIFIED", // VERIFIED, CORRECTED, NEEDS_REVIEW
    val correctedResponse: String? = null,
    val tokenCount: Int = 340,
    val latencyMs: Long = 285L,
    val isFineTuningReady: Boolean = true
)

/**
 * Pre-populated realistic and comprehensive multi-role training dataset
 */
val SampleAiChatAuditLogs = listOf(
    AiChatAuditLog(
        id = "LOG-2026-001",
        sessionId = "SES-PAR-882",
        senderName = "Meltem Yılmaz (Veli)",
        senderRole = UserRole.PARENT,
        userQuery = "Ahmet'in 2. dönem notları ve başarı durumu nasıl?",
        aiResponse = """📊 **Ahmet Demir (8-A • No: 104) — Akademik Not Çizelgesi:**
• **Genel Not Ortalaması (GNO):** 88.50 / 100 🏆 (Takdir Belgesi Seviyesi)
• **Sınıf Sıralaması:** 2. / 28 Öğrenci
• **Ders Notları:** Türkçe: 92 (AA) • Matematik: 88 (BA) • Fen Bilimleri: 94 (AA) • İngilizce: 95 (AA)
• **Devamsızlık:** 1.5 Gün İzinli / 0 Gün Özürsüz.""",
        topicCategory = "Akademik & Karne",
        timestamp = "27.09.2026 14:15",
        sentiment = "QUESTION",
        qualityRating = 5,
        feedbackStatus = "VERIFIED",
        tokenCount = 380,
        latencyMs = 260
    ),
    AiChatAuditLog(
        id = "LOG-2026-002",
        sessionId = "SES-ADM-109",
        senderName = "Selim Arslan (Yönetici)",
        senderRole = UserRole.ADMIN,
        userQuery = "kantin kazancı ve bugünkü ciro ne kadar?",
        aiResponse = """🥪 **Kantin & POS Satış Hasılatı ve Günlük Kazanç Raporu:**
💰 **Günlük Kazanç & Ciro Performansı:**
• **Bugünkü Toplam Kantin Kazancı (Ciro):** ₺1.420,00
• **Toplam İşlem / Fiş Sayısı:** 68 Adet Satış (Ortalama Fiş: ₺20.88)
• **Tahmini Net Kar:** ₺511,20 (Brüt Kar Marjı: %36)
💳 **Tahsilat Türü:** Temassız RFID Cüzdan: ₺1.050,80 (%74) • Nakit/Kredi Kartı: ₺369,20 (%26)
🏆 **Top Satanlar:** Kaşarlı Tost (32 Adet • ₺1.440), Tam Buğday Sandviç (25 Adet • ₺1.250).""",
        topicCategory = "Kantin & Finans",
        timestamp = "27.09.2026 13:48",
        sentiment = "POSITIVE",
        qualityRating = 5,
        feedbackStatus = "VERIFIED",
        tokenCount = 420,
        latencyMs = 240
    ),
    AiChatAuditLog(
        id = "LOG-2026-003",
        sessionId = "SES-PAR-904",
        senderName = "Ayşe Kaya (Veli)",
        senderRole = UserRole.PARENT,
        userQuery = "Zeynep için kalan borç ve taksit durumu nedir?",
        aiResponse = """💰 **Zeynep Kaya (7-B • No: 182) — Borç ve Bakiye Raporu:**
• **Kalan Toplam Borç:** ₺4.500,00 (FAT-2026-182 • 2026 Nisan Taksiti • Vade: 25 Nisan 2026)
• **Geçmiş Tahsilatlar:** ₺36.000,00 başarıyla tahsil edilmiştir.
💳 **Ödeme Kolaylığı:** iyzico Sanal POS ile 3D Secure kredi kartına 3/6 taksit imkanı mevcuttur.""",
        topicCategory = "Borç & Tahsilat",
        timestamp = "27.09.2026 12:30",
        sentiment = "NEUTRAL",
        qualityRating = 5,
        feedbackStatus = "VERIFIED",
        tokenCount = 310,
        latencyMs = 295
    ),
    AiChatAuditLog(
        id = "LOG-2026-004",
        sessionId = "SES-TCH-312",
        senderName = "Ahmet Yılmaz (Öğretmen)",
        senderRole = UserRole.TEACHER,
        userQuery = "8-A sınıfı bugün devamsızlık durumu ve gelmeyen öğrenciler kimler?",
        aiResponse = """📋 **8-A Sınıfı Canlı Yoklama Raporu (27 Eylül 2026):**
• **Toplam Mevcut:** 26 / 28 Öğrenci (%92.8 Katılım)
• **Gelmeyen / Devamsız:** 1 Öğrenci (Caner Kurt • Veli Bilgilendirildi)
• **İzinli / Raporlu:** 1 Öğrenci (Ece Yıldız • Sağlık Raporu Onaylı)
• **Turnike Giriş:** Sınıfın %90'ı 08:35 öncesi ana turnikeden geçiş yaptı.""",
        topicCategory = "Yoklama & Turnike",
        timestamp = "27.09.2026 09:12",
        sentiment = "QUESTION",
        qualityRating = 5,
        feedbackStatus = "VERIFIED",
        tokenCount = 340,
        latencyMs = 210
    ),
    AiChatAuditLog(
        id = "LOG-2026-005",
        sessionId = "SES-PAR-651",
        senderName = "Hakan Çelik (Veli)",
        senderRole = UserRole.PARENT,
        userQuery = "Matematik öğretmeni Ahmet Hoca ile ne zaman görüşebilirim randevu var mı?",
        aiResponse = """📅 **Ahmet Yılmaz (Matematik / Rehber Öğretmen) — Veli Görüşme Saatleri:**
• **Müsait Günler:** Çarşamba 14:00 - 15:30 (2 Boş Slot) • Cuma 10:30 - 11:30 (1 Boş Slot)
• **Görüşme Yeri:** Zemin Kat Veli Görüşme Odası 1 veya Online Zoom Linki
Aşağıdaki 'Randevu Al' butonuna basarak randevunuzu anında onaylayabilirsiniz.""",
        topicCategory = "Veli & Randevu",
        timestamp = "27.09.2026 11:05",
        sentiment = "QUESTION",
        qualityRating = 5,
        feedbackStatus = "VERIFIED",
        tokenCount = 290,
        latencyMs = 230
    ),
    AiChatAuditLog(
        id = "LOG-2026-006",
        sessionId = "SES-CNT-041",
        senderName = "Murat Usta (Kantin Personeli)",
        senderRole = UserRole.CANTEEN,
        userQuery = "Kritik seviyeye inen kantin ürünleri ve kalan stoklar neler?",
        aiResponse = """🛒 **Kantin Ürünleri, Stok ve Kritik Envanter Kontrolü:**
⚠️ **Kritik / Azalan Ürünler (Tedarik Gerekli):**
• Organik Günlük Süt (200ml): Kalan: **4 Adet** (Kritik Eşik: 10)
• Tam Buğday Sandviç: Kalan: **6 Adet** (Kritik Eşik: 10)
📦 **Güvenli Stoklar:** Doğal Kaynak Suyu: 120 Adet • Ayran: 80 Adet • Kaşarlı Tost Ekmeği: 50 Adet.""",
        topicCategory = "Kantin & Finans",
        timestamp = "27.09.2026 10:20",
        sentiment = "POSITIVE",
        qualityRating = 5,
        feedbackStatus = "VERIFIED",
        tokenCount = 360,
        latencyMs = 270
    ),
    AiChatAuditLog(
        id = "LOG-2026-007",
        sessionId = "SES-PAR-331",
        senderName = "Fatma Şahin (Veli)",
        senderRole = UserRole.PARENT,
        userQuery = "Öğrencimin revirde ilacı içirildi mi sağlık durumu nasıl?",
        aiResponse = """💊 **Emre Şahin (6-C) — Canlı İlaç & Revir Sağlık Takibi:**
• **Kayıtlı İlaç:** Klacid 250mg Süspansiyon (1x1 Doz • Saat: 13:00)
• **Uygulama:** Saat 13:02'de Ayten Hemşire tarafından içirildi ✅
• **Vücut Sıcaklığı / Vital:** 36.5 °C (Normal) • Bilinen Gıda Alerjisi: Fıstık (Kantin POS'ta bloke).""",
        topicCategory = "Revir & Sağlık",
        timestamp = "27.09.2026 13:10",
        sentiment = "POSITIVE",
        qualityRating = 5,
        feedbackStatus = "VERIFIED",
        tokenCount = 310,
        latencyMs = 250
    ),
    AiChatAuditLog(
        id = "LOG-2026-008",
        sessionId = "SES-OWN-002",
        senderName = "Kemal Erdem (Kurum Sahibi)",
        senderRole = UserRole.OWNER,
        userQuery = "3 aylık nakit akışı tahmini ve şube tahsilat karşılaştırması",
        aiResponse = """📈 **SCS360 Yönetim Kurulu Canlı Nakit Akışı & Şube Karşılaştırması:**
• **Toplam Tahakkuk:** ₺4.850.000 • **Gerçekleşen Tahsilat:** ₺4.480.000 (%92.4)
• **3 Aylık Tahmini Net Giriş:** ₺1.450.000
🏢 **Şube Performansı:** Ataşehir Merkez: %94.2 Tahsilat • Kadıköy Anadolu: %89.5 • Beşiktaş Fen: %92.0
💡 **AI Stratejik Tavsiye:** Kadıköy şubesinde erken kayıt avantajı ve otomatik SMS hatırlatma döngüsü önerilir.""",
        topicCategory = "Strateji & Finans",
        timestamp = "27.09.2026 08:45",
        sentiment = "POSITIVE",
        qualityRating = 5,
        feedbackStatus = "VERIFIED",
        tokenCount = 450,
        latencyMs = 310
    )
)

enum class AuditTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LOGS("Diyalog Denetimi", Icons.Default.Chat),
    ANALYTICS("Konu & Niyet Analizi", Icons.Default.Analytics),
    DATASET_EXPORT("LLM Fine-Tuning Veri Seti", Icons.Default.DataObject),
    PROMPT_TEMPLATES("Sistem Promptları", Icons.Default.Psychology),
    PLAYGROUND("Canlı LLM Simülatörü", Icons.Default.Science)
}

enum class ExportFormat(val title: String, val extension: String) {
    OPENAI_GEMINI_JSONL("OpenAI / Gemini JSONL (Chat)", ".jsonl"),
    ALPACA_JSON("Alpaca Instruction JSON", ".json"),
    LLAMA3_CHAT("Llama-3 / Mistral Template", ".txt"),
    CSV_TABULAR("CSV / Excel Veri Tablosu", ".csv")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatAuditScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit,
    onSwitchToSuperAdmin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val chatContext = rememberChatContext()

    val isAuthorized = state.selectedRole == UserRole.SUPERADMIN

    var activeTab by remember { mutableStateOf(AuditTab.LOGS) }
    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var selectedTopicFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedExportFormat by remember { mutableStateOf(ExportFormat.OPENAI_GEMINI_JSONL) }

    // Dynamic Audit Log List initialized with samples and live user turns
    var auditLogs by remember {
        mutableStateOf(SampleAiChatAuditLogs.toMutableList())
    }

    // Active dialog editing for Human-in-the-Loop RLHF annotation
    var editingLog by remember { mutableStateOf<AiChatAuditLog?>(null) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var exportedTextSnippet by remember { mutableStateOf("") }

    // Synchronize current chat session messages if any
    LaunchedEffect(chatContext.messages) {
        val userMessages = chatContext.messages.filter { it.isUser }
        userMessages.forEachIndexed { index, uMsg ->
            val aiMsg = chatContext.messages.getOrNull(chatContext.messages.indexOf(uMsg) + 1)
            if (aiMsg != null && !aiMsg.isUser) {
                val exists = auditLogs.any { it.userQuery == uMsg.text }
                if (!exists) {
                    val dynamicTopic = when {
                        uMsg.text.contains("kantin", true) || uMsg.text.contains("ciro", true) || uMsg.text.contains("kazanç", true) -> "Kantin & Finans"
                        uMsg.text.contains("not", true) || uMsg.text.contains("karne", true) || uMsg.text.contains("sınav", true) -> "Akademik & Karne"
                        uMsg.text.contains("borç", true) || uMsg.text.contains("taksit", true) || uMsg.text.contains("fatura", true) -> "Borç & Tahsilat"
                        uMsg.text.contains("yoklama", true) || uMsg.text.contains("turnike", true) -> "Yoklama & Turnike"
                        uMsg.text.contains("öğretmen", true) || uMsg.text.contains("randevu", true) -> "Veli & Randevu"
                        uMsg.text.contains("ilaç", true) || uMsg.text.contains("revir", true) -> "Revir & Sağlık"
                        else -> "Genel Okul Danışmanlığı"
                    }
                    auditLogs.add(
                        0,
                        AiChatAuditLog(
                            id = "LIVE-${System.currentTimeMillis() % 10000}",
                            sessionId = "SES-LIVE-${chatContext.activeRole.name.take(3)}",
                            senderName = "${chatContext.activeUserName} (${chatContext.activeRole.displayName})",
                            senderRole = chatContext.activeRole,
                            userQuery = uMsg.text,
                            aiResponse = aiMsg.text,
                            topicCategory = dynamicTopic,
                            timestamp = "Bugün ${java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())}",
                            sentiment = "POSITIVE",
                            qualityRating = 5,
                            feedbackStatus = "VERIFIED",
                            tokenCount = (uMsg.text.length + aiMsg.text.length) / 4 + 120,
                            latencyMs = 275
                        )
                    )
                }
            }
        }
    }

    val topics = remember(auditLogs) {
        listOf("Tümü") + auditLogs.map { it.topicCategory }.distinct()
    }

    val filteredLogs = remember(auditLogs, selectedRoleFilter, selectedTopicFilter, searchQuery) {
        auditLogs.filter { log ->
            val matchesRole = selectedRoleFilter == null || log.senderRole == selectedRoleFilter
            val matchesTopic = selectedTopicFilter == null || selectedTopicFilter == "Tümü" || log.topicCategory == selectedTopicFilter
            val matchesSearch = searchQuery.isBlank() ||
                    log.userQuery.contains(searchQuery, ignoreCase = true) ||
                    log.aiResponse.contains(searchQuery, ignoreCase = true) ||
                    log.senderName.contains(searchQuery, ignoreCase = true) ||
                    log.topicCategory.contains(searchQuery, ignoreCase = true)
            matchesRole && matchesTopic && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "AI Chatbot Analitik & LLM Eğitim Merkezi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AccentAmber
                            ) {
                                Text(
                                    text = "SuperAdmin",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BrandNavy,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Konuşma Analitiği, Niyet Tespiti & LLM Fine-Tuning Veri Seti",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val jsonl = generateFineTuningDataset(auditLogs, ExportFormat.OPENAI_GEMINI_JSONL)
                            clipboardManager.setText(AnnotatedString(jsonl))
                            exportedTextSnippet = jsonl.take(450) + "...\n\n[Toplam ${auditLogs.size} Diyalog Panoya Kopyalandı]"
                            showExportSuccessDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Veri Setini Kopyala",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandNavy
                )
            )
        }
    ) { innerPadding ->
        if (!isAuthorized) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BackgroundLight)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3E8FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Yetki Gerekli",
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "🔒 Yalnızca Süper Admin Yetkisi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )

                        Text(
                            text = "LLM İnce Ayarı (Fine-Tuning), kullanıcı yazışma denetimleri, niyet analizi ve prompt modelleri veritabanı güvenliği gereği yalnızca **Süper Admin** (Sistem Yöneticisi) yetkisine açıktır.",
                            fontSize = 13.sp,
                            color = NeutralGrayDark,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Mevcut Oturum Rolü: ${state.selectedRole.displayName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                                Text("Gerekli İzin: SUPERADMIN_ALL • AI_CHAT_AUDIT", fontSize = 11.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Medium)
                            }
                        }

                        if (onSwitchToSuperAdmin != null) {
                            Button(
                                onClick = onSwitchToSuperAdmin,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("👑 Süper Admin Olarak Yetkilendir / Giriş Yap", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Geri Dön")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BackgroundLight)
            ) {
            // Tab Navigation
            ScrollableTabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    if (activeTab.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                            color = PrimaryBlue
                        )
                    }
                }
            ) {
                AuditTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (activeTab == tab) PrimaryBlue else NeutralGrayDark
                                )
                                Text(
                                    text = tab.title,
                                    fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp,
                                    color = if (activeTab == tab) PrimaryBlue else NeutralGrayDark
                                )
                            }
                        }
                    )
                }
            }

            // Tab Content
            when (activeTab) {
                AuditTab.LOGS -> {
                    AuditLogsTabContent(
                        logs = filteredLogs,
                        totalCount = auditLogs.size,
                        selectedRole = selectedRoleFilter,
                        onSelectRole = { selectedRoleFilter = it },
                        selectedTopic = selectedTopicFilter,
                        onSelectTopic = { selectedTopicFilter = it },
                        topics = topics,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onEditLog = { editingLog = it },
                        onVerifyLog = { log ->
                            val index = auditLogs.indexOfFirst { it.id == log.id }
                            if (index != -1) {
                                auditLogs[index] = log.copy(feedbackStatus = "VERIFIED", isFineTuningReady = true)
                            }
                        },
                        onCopyLog = { log ->
                            val json = """{"messages": [{"role": "system", "content": "Sen SCS360 kurumsal zeka asistanısın."}, {"role": "user", "content": "${log.userQuery}"}, {"role": "assistant", "content": "${log.aiResponse.replace("\n", "\\n")}"}]}"""
                            clipboardManager.setText(AnnotatedString(json))
                        }
                    )
                }
                AuditTab.ANALYTICS -> {
                    TopicAnalyticsTabContent(logs = auditLogs)
                }
                AuditTab.DATASET_EXPORT -> {
                    DatasetExportTabContent(
                        logs = auditLogs,
                        selectedFormat = selectedExportFormat,
                        onSelectFormat = { selectedExportFormat = it },
                        onCopyAll = { format ->
                            val content = generateFineTuningDataset(auditLogs, format)
                            clipboardManager.setText(AnnotatedString(content))
                            exportedTextSnippet = content.take(600) + "\n\n... (${auditLogs.size} Kayıt Başarıyla Kopyalandı)"
                            showExportSuccessDialog = true
                        }
                    )
                }
                AuditTab.PROMPT_TEMPLATES -> {
                    PromptTemplatesTabContent()
                }
                AuditTab.PLAYGROUND -> {
                    PlaygroundTabContent(
                        onAddToDataset = { query, response, role, topic ->
                            auditLogs.add(
                                0,
                                AiChatAuditLog(
                                    id = "SIM-${System.currentTimeMillis() % 10000}",
                                    sessionId = "SES-SIM-LAB",
                                    senderName = "SuperAdmin Simülasyonu",
                                    senderRole = role,
                                    userQuery = query,
                                    aiResponse = response,
                                    topicCategory = topic,
                                    timestamp = "Bugün ${java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())}",
                                    sentiment = "POSITIVE",
                                    qualityRating = 5,
                                    feedbackStatus = "VERIFIED",
                                    tokenCount = (query.length + response.length) / 4 + 100,
                                    latencyMs = 210
                                )
                            )
                        }
                    )
                }
            }
        }
    }
    }

    // Human-in-the-Loop RLHF Annotation & Response Correction Dialog
    if (editingLog != null) {
        val currentLog = editingLog!!
        var editedResponse by remember { mutableStateOf(currentLog.correctedResponse ?: currentLog.aiResponse) }
        var editedTopic by remember { mutableStateOf(currentLog.topicCategory) }
        var rating by remember { mutableIntStateOf(currentLog.qualityRating) }

        AlertDialog(
            onDismissRequest = { editingLog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = PrimaryBlue)
                    Text("LLM Yanıtını Düzelt & Doğrula (RLHF)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Öğrenci/Veli/Öğretmen Sorusu (Prompt):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NeutralGrayDark
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentLog.userQuery,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandNavy,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Text(
                        text = "İdeal LLM Yanıtı (Fine-Tuning Hedefi):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NeutralGrayDark
                    )
                    OutlinedTextField(
                        value = editedResponse,
                        onValueChange = { editedResponse = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kalite Puanı:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { rating = star },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (star <= rating) AccentAmber else NeutralGrayDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = auditLogs.indexOfFirst { it.id == currentLog.id }
                        if (index != -1) {
                            auditLogs[index] = currentLog.copy(
                                aiResponse = editedResponse,
                                correctedResponse = editedResponse,
                                topicCategory = editedTopic,
                                qualityRating = rating,
                                feedbackStatus = "CORRECTED",
                                isFineTuningReady = true
                            )
                        }
                        editingLog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Kaydet & LLM Veri Setine Aktar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingLog = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Export Success Feedback Dialog
    if (showExportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(40.dp)) },
            title = { Text("LLM Eğitim Veri Seti Kopyalandı", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tüm konuşma kayıtları OpenAI & Google Gemini Fine-Tuning JSONL formatına dönüştürüldü ve panoya kopyalandı.", fontSize = 13.sp)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exportedTextSnippet,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(10.dp),
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExportSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Tamam")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// TAB 1: Diyalog Denetimi & İnceleme (Conversation Logs & Annotation)
// -----------------------------------------------------------------------------
@Composable
private fun AuditLogsTabContent(
    logs: List<AiChatAuditLog>,
    totalCount: Int,
    selectedRole: UserRole?,
    onSelectRole: (UserRole?) -> Unit,
    selectedTopic: String?,
    onSelectTopic: (String?) -> Unit,
    topics: List<String>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onEditLog: (AiChatAuditLog) -> Unit,
    onVerifyLog: (AiChatAuditLog) -> Unit,
    onCopyLog: (AiChatAuditLog) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary KPI Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Dataset, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(22.dp))
                            Text("LLM Eğitim Havuzu & Kalite Skorları", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = StatusGreen.copy(alpha = 0.25f)) {
                            Text("%98.6 Doğruluk", color = StatusGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiMiniBadge("Toplam Diyalog", "$totalCount Kayıt", Icons.Default.Chat, Modifier.weight(1f))
                        KpiMiniBadge("Doğrulanmış (RLHF)", "${logs.count { it.feedbackStatus == "VERIFIED" }} Çift", Icons.Default.Verified, Modifier.weight(1f))
                        KpiMiniBadge("Ortalama Token", "345 Token", Icons.Default.Memory, Modifier.weight(1f))
                        KpiMiniBadge("Ort. Yanıt Hızı", "265 ms", Icons.Default.Speed, Modifier.weight(1f))
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Sorularda veya AI cevaplarında ara... (Örn: kantin kazancı, veli, borç)", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Temizle")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )
        }

        // Role Filter Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Kullanıcı Rolü Filtresi:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeutralGrayDark)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedRole == null,
                            onClick = { onSelectRole(null) },
                            label = { Text("Tüm Roller (${totalCount})") }
                        )
                    }
                    items(UserRole.entries) { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { onSelectRole(if (selectedRole == role) null else role) },
                            label = { Text(role.displayName) }
                        )
                    }
                }
            }
        }

        // Topic Filter Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Konu & Niyet Kategorisi:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeutralGrayDark)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(topics) { topic ->
                        val isSelected = (selectedTopic == null && topic == "Tümü") || selectedTopic == topic
                        SuggestionChip(
                            onClick = { onSelectTopic(if (topic == "Tümü") null else topic) },
                            label = { Text(topic, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) PrimaryBlueContainer else MaterialTheme.colorScheme.surface,
                                labelColor = if (isSelected) PrimaryBlue else NeutralGrayDark
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }
        }

        // Log Cards List
        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = NeutralGrayDark, modifier = Modifier.size(48.dp))
                        Text("Filtrelere uygun diyalog kaydı bulunamadı.", fontWeight = FontWeight.Bold, color = NeutralGrayDark)
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                ChatAuditLogCard(
                    log = log,
                    onEdit = { onEditLog(log) },
                    onVerify = { onVerifyLog(log) },
                    onCopy = { onCopyLog(log) }
                )
            }
        }
    }
}

@Composable
private fun ChatAuditLogCard(
    log: AiChatAuditLog,
    onEdit: () -> Unit,
    onVerify: () -> Unit,
    onCopy: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: User Name + Role + Topic Badge + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(log.senderRole.badgeColorHex).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = log.senderRole.displayName.take(1),
                            color = Color(log.senderRole.badgeColorHex),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Column {
                        Text(text = log.senderName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandNavy)
                        Text(text = "${log.sessionId} • ${log.timestamp}", fontSize = 10.5.sp, color = NeutralGrayDark)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = log.topicCategory,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // User Prompt Section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                    Text("Kullanıcı Sorusu (Prompt):", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = NeutralGrayDark)
                }
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = log.userQuery,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // AI Completion Section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(14.dp))
                        Text("Chatbot Yanıtı (Completion):", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = NeutralGrayDark)
                    }
                    if (log.feedbackStatus == "CORRECTED") {
                        Surface(shape = RoundedCornerShape(4.dp), color = AccentAmber.copy(alpha = 0.2f)) {
                            Text("Düzeltildi (RLHF)", color = Color(0xFFB45309), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Surface(
                    color = Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (expanded) log.aiResponse else log.aiResponse.take(220) + (if (log.aiResponse.length > 220) "..." else ""),
                        fontSize = 12.5.sp,
                        color = Color(0xFF3B0764),
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Metadata & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚡ ${log.latencyMs}ms", fontSize = 10.5.sp, color = NeutralGrayDark)
                    Text("•", fontSize = 10.5.sp, color = NeutralGrayDark)
                    Text("🔤 ${log.tokenCount} Token", fontSize = 10.5.sp, color = NeutralGrayDark)
                    Text("•", fontSize = 10.5.sp, color = NeutralGrayDark)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(12.dp))
                        Text(" ${log.qualityRating}.0", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AccentAmber)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (log.aiResponse.length > 220) {
                        TextButton(
                            onClick = { expanded = !expanded },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(if (expanded) "Küçült" else "Devamı", fontSize = 11.sp)
                        }
                    }

                    IconButton(onClick = onCopy, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", modifier = Modifier.size(15.dp), tint = NeutralGrayDark)
                    }

                    FilledTonalButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = PrimaryBlue.copy(alpha = 0.12f), contentColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Düzelt (RLHF)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 2: Konu & Niyet Analitiği (Topic & Intent Analytics)
// -----------------------------------------------------------------------------
@Composable
private fun TopicAnalyticsTabContent(logs: List<AiChatAuditLog>) {
    val topicDistribution = remember(logs) {
        logs.groupBy { it.topicCategory }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    val roleDistribution = remember(logs) {
        logs.groupBy { it.senderRole }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Topic Breakdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯 En Çok Sorulan Konular ve Niyet Dağılımı", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                        Text("${logs.size} Soru", fontSize = 12.sp, color = NeutralGrayDark, fontWeight = FontWeight.Bold)
                    }

                    topicDistribution.forEach { (topic, count) ->
                        val ratio = if (logs.isNotEmpty()) count.toFloat() / logs.size else 0f
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(topic, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = BrandNavy)
                                Text("$count Soru (%${(ratio * 100).toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = PrimaryBlue,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Role Breakdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("👥 Kullanıcı Rollerine Göre Chatbot Kullanım Oranı", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)

                    roleDistribution.forEach { (role, count) ->
                        val ratio = if (logs.isNotEmpty()) count.toFloat() / logs.size else 0f
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(role.badgeColorHex)))
                                    Text(role.displayName, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                }
                                Text("$count Etkileşim (%${(ratio * 100).toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(role.badgeColorHex))
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(role.badgeColorHex),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // AI Training Readiness Advice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("LLM Fine-Tuning Önerisi:", fontWeight = FontWeight.Bold, color = Color(0xFF14532D), fontSize = 13.sp)
                        Text(
                            text = "Kantin cirosu ve akademik karne sorguları yüksek doğrulukla (%100) yanıtlanmaktadır. Veli-öğretmen randevu teyitlerinde birkaç prompt varyasyonu eklenerek veri çeşitliliği zenginleştirilebilir.",
                            fontSize = 12.sp,
                            color = Color(0xFF166534),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 3: LLM Fine-Tuning Veri Seti Dışa Aktarma (Dataset Export)
// -----------------------------------------------------------------------------
@Composable
private fun DatasetExportTabContent(
    logs: List<AiChatAuditLog>,
    selectedFormat: ExportFormat,
    onSelectFormat: (ExportFormat) -> Unit,
    onCopyAll: (ExportFormat) -> Unit
) {
    val exportCodeSnippet = remember(logs, selectedFormat) {
        generateFineTuningDataset(logs, selectedFormat)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandNavy),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📦 LLM Model Eğitimi & Fine-Tuning Veri Çıktısı", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text(
                        text = "Bu ekran, kullanıcılarınızın sorduğu gerçek soruları ve kurumsal yanıtları OpenAI GPT-4o, Google Gemini Flash, Llama-3 veya Mistral modellerine özel eğitim (fine-tuning) veri seti olarak hazırlar.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onCopyAll(selectedFormat) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BrandNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tüm Veri Setini Kopyala", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Format Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Eğitim Formatını Seçin:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeutralGrayDark)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.entries.forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { onSelectFormat(format) },
                            label = { Text(format.title, fontSize = 11.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Code Preview Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Canlı Veri Seti Önizleme (${selectedFormat.extension})", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("${logs.size} Örnek", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = Color(0xFF334155))
                    Text(
                        text = exportCodeSnippet.take(1500) + if (exportCodeSnippet.length > 1500) "\n\n... [${logs.size} adet örnek veri]" else "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 4: Sistem Promptları & Roller (System Prompts)
// -----------------------------------------------------------------------------
@Composable
private fun PromptTemplatesTabContent() {
    val systemPrompts = listOf(
        Triple("Veli Asistanı Sistem Rolü", "PARENT", "Sen SCS360 Veli Asistanısın. Velinin kayıtlı öğrencisi hakkında not, devamsızlık, yemekhane menüsü, ilaç saati ve öğretmen randevusu bilgilerini nazik, güven verici ve net bir dille aktar."),
        Triple("Öğretmen Asistanı Sistem Rolü", "TEACHER", "Sen SCS360 Öğretmen Asistanısın. Ders programı, e-yoklama listeleri, nöbet çizelgeleri ve öğrenci akademik performans kayıtlarını hızlı aksiyon butonlarıyla sun."),
        Triple("Yönetici & Müdür Sistem Rolü", "ADMIN", "Sen SCS360 Yönetim Merkezi Asistanısın. Kayıt kontenjanları, kantin hasılatı, RFID turnike sağlığı, borçlu veli listesi ve resmi bildirimleri kurumsal formatta raporla."),
        Triple("Kantin & POS Satış Asistanı", "CANTEEN", "Sen SCS360 Kantin ve İaşe Asistanısın. Günlük ciro, temassız RFID harcamaları, kritik azalan stoklar ve besin alerjen filtrelerini anlık denetle.")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(systemPrompts) { (title, role, prompt) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = BrandNavy)
                        Surface(shape = RoundedCornerShape(4.dp), color = PrimaryBlue.copy(alpha = 0.15f)) {
                            Text(role, color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = prompt, fontSize = 12.sp, color = Color(0xFF334155), modifier = Modifier.padding(10.dp), lineHeight = 17.sp)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 5: Canlı Model Test & Simülatörü (Playground)
// -----------------------------------------------------------------------------
@Composable
private fun PlaygroundTabContent(
    onAddToDataset: (query: String, response: String, role: UserRole, topic: String) -> Unit
) {
    var promptInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.PARENT) }
    var simulatedResponse by remember { mutableStateOf("") }
    var isSimulating by remember { mutableStateOf(false) }
    var addedToDataset by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚡ Canlı Model Çıkarım & Eğitim Simülatörü", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserRole.entries.take(4).forEach { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                label = { Text(role.displayName, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Modeli test etmek için soru girin... (Örn: 'Kantin cirosu', 'Ali'nin notları')") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                isSimulating = true
                                addedToDataset = false
                                simulatedResponse = "🤖 Model simülasyonu çalışıyor..."
                                simulatedResponse = when {
                                    promptInput.contains("kantin", true) || promptInput.contains("ciro", true) -> """🥪 **Kantin ve Günlük Kazanç Raporu:**
• Bugünkü Toplam Ciro: ₺1.420,00 (68 İşlem)
• Tahmini Net Kâr: ₺511,20 (%36 Marj)
• Temassız RFID Oranı: %74"""
                                    promptInput.contains("not", true) || promptInput.contains("karne", true) -> """📊 **Öğrenci Not Çizelgesi:**
• GNO: 88.50 / 100 🏆 (Takdir Seviyesi)
• Başarı Sırası: 2. / 28 Öğrenci"""
                                    else -> "SCS360 kurumsal veritabanından başarıyla yanıt üretildi."
                                }
                                isSimulating = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Model Yanıtını Test Et")
                    }

                    if (simulatedResponse.isNotBlank()) {
                        Surface(
                            color = Color(0xFFFAF5FF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Model Çıktısı (Completion):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF6B21A8))
                                Text(simulatedResponse, fontSize = 12.5.sp, color = Color(0xFF3B0764))

                                Button(
                                    onClick = {
                                        onAddToDataset(promptInput, simulatedResponse, selectedRole, "Eğitim Simülasyonu")
                                        addedToDataset = true
                                    },
                                    enabled = !addedToDataset,
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (addedToDataset) "Eğitim Setine Eklendi ✅" else "Fine-Tuning Setine Ekle", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Helper Methods & Formatters for Fine-Tuning
// -----------------------------------------------------------------------------
private fun generateFineTuningDataset(logs: List<AiChatAuditLog>, format: ExportFormat): String {
    return when (format) {
        ExportFormat.OPENAI_GEMINI_JSONL -> {
            logs.joinToString(separator = "\n") { log ->
                val cleanUser = log.userQuery.replace("\"", "\\\"").replace("\n", " ")
                val cleanAi = log.aiResponse.replace("\"", "\\\"").replace("\n", "\\n")
                """{"messages": [{"role": "system", "content": "Sen SCS360 kurumsal okul yönetim ve veli asistanısın. Türkçe, nazik ve doğru veriyle cevap ver."}, {"role": "user", "content": "$cleanUser"}, {"role": "assistant", "content": "$cleanAi"}]}"""
            }
        }
        ExportFormat.ALPACA_JSON -> {
            val items = logs.joinToString(separator = ",\n") { log ->
                val cleanUser = log.userQuery.replace("\"", "\\\"")
                val cleanAi = log.aiResponse.replace("\"", "\\\"").replace("\n", "\\n")
                """  {
    "instruction": "SCS360 asistanı olarak aşağıdaki kullanıcı sorusunu yanıtla.",
    "input": "$cleanUser",
    "output": "$cleanAi"
  }"""
            }
            "[\n$items\n]"
        }
        ExportFormat.LLAMA3_CHAT -> {
            logs.joinToString(separator = "\n---\n") { log ->
                """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
Sen SCS360 kurumsal zeka asistanısın.<|eot_id|><|start_header_id|>user<|end_header_id|>
${log.userQuery}<|eot_id|><|start_header_id|>assistant<|end_header_id|>
${log.aiResponse}<|eot_id|>"""
            }
        }
        ExportFormat.CSV_TABULAR -> {
            val rows = logs.joinToString(separator = "\n") { log ->
                "\"${log.id}\",\"${log.senderRole.name}\",\"${log.topicCategory}\",\"${log.userQuery.replace("\"", "\"\"")}\",\"${log.aiResponse.replace("\"", "\"\"")}\",\"${log.feedbackStatus}\""
            }
            "id,role,topic,prompt,completion,status\n$rows"
        }
    }
}

@Composable
private fun KpiMiniBadge(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                Text(title, fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
            }
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }
    }
}
