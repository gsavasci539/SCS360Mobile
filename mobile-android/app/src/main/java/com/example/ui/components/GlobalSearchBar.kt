package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ClassGroup
import com.example.data.model.Student
import com.example.ui.navigation.MainTab
import com.example.ui.navigation.SubScreen
import com.example.ui.theme.*
import java.util.Locale

/**
 * Metadata definition for a specific system report accessible via Global Search.
 */
data class GlobalReportItem(
    val id: String,
    val aliasId: String,
    val title: String,
    val category: String,
    val description: String,
    val icon: ImageVector,
    val badgeColor: Color,
    val subScreen: SubScreen,
    val mainTab: MainTab? = null,
    val keywords: List<String> = emptyList()
)

/**
 * Filter categories for global search
 */
enum class SearchFilter(val title: String) {
    ALL("Tümü"),
    STUDENTS("Öğrenciler"),
    CLASSES("Sınıflar"),
    REPORTS("Raporlar")
}

/**
 * Pre-defined list of institutional reports with their unique IDs and navigation targets.
 */
val SYSTEM_GLOBAL_REPORTS = listOf(
    GlobalReportItem(
        id = "REP-01",
        aliasId = "rep-karne",
        title = "Resmi Öğrenci Karnesi (PDF)",
        category = "Akademik",
        description = "Mühürlü, barkodlu ve resmi not dökümlü A4 karne çıktısı ve not çizelgesi.",
        icon = Icons.Default.Description,
        badgeColor = PrimaryBlue,
        subScreen = SubScreen.DIGITAL_REPORT_CARD,
        keywords = listOf("karne", "not", "pdf", "yazdır", "resmi", "notlar", "puan", "dönem sonu", "belge")
    ),
    GlobalReportItem(
        id = "REP-02",
        aliasId = "rep-mali",
        title = "Mali Durum & Tahsilat Raporu",
        category = "Finans",
        description = "Aylık gelir/gider, tahsilat oranları, şube finansal özeti ve kasa hareketleri.",
        icon = Icons.Default.AccountBalanceWallet,
        badgeColor = SecondaryEmerald,
        subScreen = SubScreen.REPORTS,
        mainTab = MainTab.FINANCE,
        keywords = listOf("mali", "tahsilat", "gelir", "gider", "kasa", "ödeme", "muhasebe", "finans", "ciro")
    ),
    GlobalReportItem(
        id = "REP-03",
        aliasId = "rep-borc",
        title = "Gecikmiş Ödemeler & Borçlular Raporu",
        category = "Finans",
        description = "Günü geçen taksitler, gecikmiş borçlu veli listesi ve otomatik ödeme hatırlatmaları.",
        icon = Icons.Default.Warning,
        badgeColor = AccentRose,
        subScreen = SubScreen.REPORTS,
        keywords = listOf("borç", "gecikmiş", "vadesi", "alacak", "ödenmemiş", "hatırlatma", "fatura", "gecikme")
    ),
    GlobalReportItem(
        id = "REP-04",
        aliasId = "rep-yoklama",
        title = "Yoklama & Devamsızlık Analizi",
        category = "Yoklama",
        description = "Şube ve sınıf bazlı katılım oranları, devamsızlık ısı haritası ve mazeret kayıtları.",
        icon = Icons.Default.FactCheck,
        badgeColor = AccentAmber,
        subScreen = SubScreen.REPORTS,
        mainTab = MainTab.ATTENDANCE,
        keywords = listOf("yoklama", "devamsızlık", "katılım", "gelmedi", "izinli", "oran", "devam", "raporlu")
    ),
    GlobalReportItem(
        id = "REP-05",
        aliasId = "rep-ai",
        title = "Yapay Zeka (AI) Kurumsal Analiz & Nakit Akışı",
        category = "Yapay Zeka",
        description = "3 aylık nakit akışı tahmini, churn risk skoru ve yapay zeka destekli büyüme projeksiyonu.",
        icon = Icons.Default.AutoAwesome,
        badgeColor = Color(0xFF7C3AED),
        subScreen = SubScreen.AI_ANALYTICS,
        keywords = listOf("ai", "yapay zeka", "tahmin", "risk", "churn", "nakit akışı", "büyüme", "projeksiyon")
    ),
    GlobalReportItem(
        id = "REP-06",
        aliasId = "rep-gunluk",
        title = "Günlük Karne & Öğrenci Gelişim Raporu",
        category = "Gelişim",
        description = "Beslenme, uyku, aktivite katılımı, duygu durumu ve günlük öğretmen gelişim notları.",
        icon = Icons.Default.Assignment,
        badgeColor = Color(0xFF0D9488),
        subScreen = SubScreen.DAILY_REPORTS,
        keywords = listOf("günlük", "gelişim", "yemek", "uyku", "duygu", "aktivite", "öğretmen notu", "karne")
    ),
    GlobalReportItem(
        id = "REP-07",
        aliasId = "rep-sinav",
        title = "Sınav Takvimi & Başarı Not Analizi",
        category = "Akademik",
        description = "Dönemlik sınavlar, sınıf başarı istatistikleri ve şube genel not ortalamaları.",
        icon = Icons.Default.Grading,
        badgeColor = PrimaryBlue,
        subScreen = SubScreen.EXAMS,
        keywords = listOf("sınav", "deneme", "lgs", "yks", "başarı", "ortalama", "takvim", "puan", "notlar")
    ),
    GlobalReportItem(
        id = "REP-08",
        aliasId = "rep-bordro",
        title = "Personel Bordro & Maaş Raporu",
        category = "İdari",
        description = "Öğretmen ve çalışan maaş hakedişleri, primler, ek ders ve resmi bordro dökümleri.",
        icon = Icons.Default.Payments,
        badgeColor = Color(0xFF4338CA),
        subScreen = SubScreen.PAYROLL,
        keywords = listOf("bordro", "maaş", "personel", "öğretmen", "hakediş", "ücret", "prim")
    ),
    GlobalReportItem(
        id = "REP-09",
        aliasId = "rep-guvenlik",
        title = "Sistem Güvenlik & Denetim Logları",
        category = "Güvenlik",
        description = "Kullanıcı giriş-çıkışları, veri değişiklikleri, yetki güncellemeleri ve denetim izleri.",
        icon = Icons.Default.Security,
        badgeColor = BrandNavy,
        subScreen = SubScreen.SECURITY,
        keywords = listOf("güvenlik", "log", "denetim", "audit", "giriş", "kullanıcı", "işlem geçmişi")
    ),
    GlobalReportItem(
        id = "REP-10",
        aliasId = "rep-kutuphane",
        title = "Kütüphane Kitap Ödünç Raporu",
        category = "Kaynak",
        description = "Ödünçteki kitaplar, teslim tarihi geçen yayınlar ve öğrenci kitap okuma sayıları.",
        icon = Icons.Default.MenuBook,
        badgeColor = Color(0xFFB45309),
        subScreen = SubScreen.LIBRARY,
        keywords = listOf("kütüphane", "kitap", "ödünç", "iade", "yayın", "okuma")
    ),
    GlobalReportItem(
        id = "REP-11",
        aliasId = "rep-servis",
        title = "Servis & Ulaşım Güzergah Raporu",
        category = "Ulaşım",
        description = "Okul servis araçları, durak biniş/iniş telemetrisi ve güzergah öğrenci listeleri.",
        icon = Icons.Default.DirectionsBus,
        badgeColor = Color(0xFF0284C7),
        subScreen = SubScreen.TRANSPORT,
        keywords = listOf("servis", "ulaşım", "güzergah", "biniş", "iniş", "durak", "araç")
    ),
    GlobalReportItem(
        id = "REP-12",
        aliasId = "rep-kantin",
        title = "Kantin & Cüzdan Harcama Raporu",
        category = "Kantin",
        description = "Öğrenci cüzdan bakiyeleri, günlük harcama limitleri ve POS ürün tüketim analizi.",
        icon = Icons.Default.Storefront,
        badgeColor = Color(0xFFEA580C),
        subScreen = SubScreen.CANTEEN,
        keywords = listOf("kantin", "cüzdan", "harcama", "bakiye", "pos", "limit", "beslenme", "ciro", "kazanç", "kazanc")
    ),
    GlobalReportItem(
        id = "REP-13",
        aliasId = "rep-kantin-pos",
        title = "Kantin Satış, Ciro ve Günlük Kazanç Raporu",
        category = "Kantin & Finans",
        description = "Kantin günlük ciro, net kazanç, satılan ürünler, POS hasılatı ve bakiye yüklemeleri.",
        icon = Icons.Default.PointOfSale,
        badgeColor = Color(0xFF10B981),
        subScreen = SubScreen.CANTEEN_PORTAL,
        keywords = listOf("kantin", "kazanç", "kazanc", "kazancı", "ciro", "gelir", "hasılat", "satış", "pos", "kasa", "kar", "mali")
    ),
    GlobalReportItem(
        id = "REP-14",
        aliasId = "rep-ai-chat-audit",
        title = "AI Chat Analitik & LLM Eğitim Merkezi",
        category = "Yapay Zeka & LLM",
        description = "SuperAdmin diyalog denetimi, soru/cevap analizi ve LLM fine-tuning eğitim veri seti hazırlama.",
        icon = Icons.Default.Psychology,
        badgeColor = Color(0xFF7C3AED),
        subScreen = SubScreen.AI_CHAT_AUDIT,
        keywords = listOf("chat", "diyalog", "analiz", "llm", "eğitim", "fine tuning", "finetuning", "prompt", "mesajlar", "sorular", "cevaplar", "veli", "öğretmen", "admin", "model")
    ),
    GlobalReportItem(
        id = "REP-15",
        aliasId = "rep-platform-saas",
        title = "Platform & SaaS Üyelik Yönetimi (/platform/subscriptions)",
        category = "SaaS & Platform",
        description = "SuperAdmin SaaS paket tanımlama, yeni kurum/owner açılışı, kota ve banka havale onayı.",
        icon = Icons.Default.Dns,
        badgeColor = ScsPetrolGreen,
        subScreen = SubScreen.PLATFORM_SAAS,
        keywords = listOf("platform", "saas", "superadmin", "paket", "tenant", "kurum", "havale", "onay", "abonelik")
    ),
    GlobalReportItem(
        id = "REP-16",
        aliasId = "rep-subscription-billing",
        title = "Kurum Üyelik, Kota & Ödeme (/subscription)",
        category = "SaaS & Finans",
        description = "Şube, öğrenci ve kullanıcı kota çubukları, GRACE/RESTRICTED durum takibi ve yenileme faturası.",
        icon = Icons.Default.CreditScore,
        badgeColor = SecondaryEmerald,
        subScreen = SubScreen.SUBSCRIPTION_BILLING,
        keywords = listOf("üyelik", "uyelik", "kota", "abonelik", "grace", "restricted", "yenileme", "fatura", "limit")
    ),
    GlobalReportItem(
        id = "REP-17",
        aliasId = "rep-dismissal",
        title = "Öğrenci Teslim Masası & TCKN Doğrulama (/dismissal)",
        category = "Güvenlik & Teslim",
        description = "Yetkili yetişkin teslim alma tanımları, T.C. Kimlik doğrulama ve tarihsel teslim kayıtları.",
        icon = Icons.Default.SupervisorAccount,
        badgeColor = Color(0xFF0F766E),
        subScreen = SubScreen.DISMISSAL,
        keywords = listOf("teslim", "dismissal", "tckn", "kimlik", "veli teslim", "yetki", "kapı", "çıkış")
    ),
    GlobalReportItem(
        id = "REP-18",
        aliasId = "rep-infirmary",
        title = "Revir, Sağlık Dosyası & İlaç Doz Takibi",
        category = "Sağlık",
        description = "Revir vizitleri, takip kapatma, öğrenci sağlık profilleri, ilaç takvimleri ve verilen dozlar.",
        icon = Icons.Default.MedicalServices,
        badgeColor = Color(0xFFDC2626),
        subScreen = SubScreen.INFIRMARY,
        keywords = listOf("revir", "sağlık", "saglik", "ilaç", "ilac", "doz", "ateş", "muayene", "alerji", "kan grubu")
    ),
    GlobalReportItem(
        id = "REP-19",
        aliasId = "rep-tutoring",
        title = "Birebir Etüt, Talep & Öğretmen Müsaitlik",
        category = "Akademik",
        description = "Etüt talepleri, onay/ret akışı, çakışma kontrolü ve öğretmen müsaitlik takvimi.",
        icon = Icons.Default.EventAvailable,
        badgeColor = Color(0xFF0284C7),
        subScreen = SubScreen.TUTORING,
        keywords = listOf("etüt", "etut", "birebir", "takviye", "özel ders", "müsaitlik", "talep")
    ),
    GlobalReportItem(
        id = "REP-20",
        aliasId = "rep-guidance",
        title = "Rehberlik (PDR), Gizli Görüşme & Olay Kayıtları",
        category = "Rehberlik",
        description = "Gizlilik kontrollü PDR görüşmeleri, sınıf içi gözlemler ve disiplin/olay çözüm takibi.",
        icon = Icons.Default.Psychology,
        badgeColor = Color(0xFF7C3AED),
        subScreen = SubScreen.GUIDANCE,
        keywords = listOf("rehberlik", "pdr", "danışman", "görüşme", "gözlem", "olay", "gizli", "psikolojik")
    ),
    GlobalReportItem(
        id = "REP-21",
        aliasId = "rep-clubs",
        title = "Öğrenci Kulüpleri, Etkinlik & PDF Sertifika",
        category = "Sosyal & Kulüp",
        description = "Kulüp kontenjanları, etkinlik yoklaması ve Türkçe karakter destekli PDF sertifika indirme.",
        icon = Icons.Default.Groups,
        badgeColor = Color(0xFFC2410C),
        subScreen = SubScreen.CLUBS,
        keywords = listOf("kulüp", "kulup", "etkinlik", "sertifika", "pdf", "robotik", "münazara")
    ),
    GlobalReportItem(
        id = "REP-22",
        aliasId = "rep-resources",
        title = "Demirbaş, Envanter, Zimmet & Bakım",
        category = "İdari & Envanter",
        description = "Okul demirbaşları, personel zimmet kayıtları ve teknik bakım/arıza iş emirleri.",
        icon = Icons.Default.Inventory2,
        badgeColor = Color(0xFF92400E),
        subScreen = SubScreen.RESOURCES,
        keywords = listOf("demirbaş", "envanter", "zimmet", "bakım", "arıza", "kaynak", "laboratuvar")
    ),
    GlobalReportItem(
        id = "REP-23",
        aliasId = "rep-consents",
        title = "Veli İzin Belgeleri & SHA-256 Muvafakatnameler",
        category = "Hukuk & Onay",
        description = "Belge sürümü ve SHA-256 metin özeti doğrulamalı veli izin ve muvafakatname onayları.",
        icon = Icons.Default.FactCheck,
        badgeColor = Color(0xFF15803D),
        subScreen = SubScreen.CONSENTS,
        keywords = listOf("izin", "onay", "muvafakatname", "veli onayı", "kvkk", "gezi", "belge")
    ),
    GlobalReportItem(
        id = "REP-24",
        aliasId = "rep-surveys",
        title = "Anketler & Anonim Katılım Defteri",
        category = "İletişim",
        description = "Veli ve personel memnuniyet anketleri, anonim yanıt koruması ve canlı sonuç istatistikleri.",
        icon = Icons.Default.Poll,
        badgeColor = Color(0xFFBE185D),
        subScreen = SubScreen.SURVEYS,
        keywords = listOf("anket", "memnuniyet", "oylama", "anonim", "geri bildirim")
    ),
    GlobalReportItem(
        id = "REP-25",
        aliasId = "rep-users",
        title = "Kullanıcılar, Atomik Rol & Şube Erişimi",
        category = "Yönetim",
        description = "Personel ve kullanıcı hesapları, çoklu rol ve çoklu şube atomik erişim ataması.",
        icon = Icons.Default.ManageAccounts,
        badgeColor = Color(0xFF0369A1),
        subScreen = SubScreen.USERS,
        keywords = listOf("kullanıcı", "personel", "hesap", "yetki", "şube erişimi", "rol atama")
    )
)

/**
 * Normalizes text for Turkish case-insensitive search.
 */
fun normalizeSearchText(text: String): String {
    return text.lowercase(Locale("tr", "TR"))
        .replace('ı', 'i')
        .replace('ğ', 'g')
        .replace('ü', 'u')
        .replace('ş', 's')
        .replace('ö', 'o')
        .replace('ç', 'c')
        .trim()
}

/**
 * Compact search bar trigger designed for AppShell TopBrandHeader.
 */
@Composable
fun GlobalSearchBarTrigger(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("global_search_bar_trigger"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Ara",
                tint = PrimaryBlue,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = "Öğrenci, sınıf veya rapor ara... (İsim veya ID)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Category tag badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimaryBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Hızlı Bul",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
            }
        }
    }
}

/**
 * Full interactive Global Search Dialog allowing instant navigation to:
 * - Students (by name, student number, student ID, class)
 * - Classes (by name, class ID, teacher, grade level, room)
 * - Reports (by report name, report ID, category, keywords)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchDialog(
    students: List<Student>,
    classes: List<ClassGroup>,
    onSelectStudent: (Student) -> Unit,
    onSelectClass: (ClassGroup) -> Unit,
    onSelectReport: (GlobalReportItem) -> Unit,
    onDismiss: () -> Unit,
    initialFilter: SearchFilter = SearchFilter.ALL
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(initialFilter) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val normalizedQuery = remember(searchQuery) { normalizeSearchText(searchQuery) }

    // Filter students
    val matchingStudents = remember(normalizedQuery, students) {
        if (normalizedQuery.isBlank()) {
            emptyList()
        } else {
            students.filter { student ->
                val fullNameNorm = normalizeSearchText(student.full_name)
                val idNorm = normalizeSearchText(student.id)
                val numNorm = normalizeSearchText(student.student_number)
                val classNorm = normalizeSearchText(student.class_name)
                val parentNorm = normalizeSearchText(student.parent_name)

                fullNameNorm.contains(normalizedQuery) ||
                        idNorm.contains(normalizedQuery) ||
                        numNorm.contains(normalizedQuery) ||
                        classNorm.contains(normalizedQuery) ||
                        parentNorm.contains(normalizedQuery)
            }
        }
    }

    // Filter classes
    val matchingClasses = remember(normalizedQuery, classes) {
        if (normalizedQuery.isBlank()) {
            emptyList()
        } else {
            classes.filter { cls ->
                val nameNorm = normalizeSearchText(cls.name)
                val idNorm = normalizeSearchText(cls.id)
                val gradeNorm = normalizeSearchText(cls.grade_level)
                val teacherNorm = normalizeSearchText(cls.teacher_name)
                val roomNorm = normalizeSearchText(cls.room_number)

                nameNorm.contains(normalizedQuery) ||
                        idNorm.contains(normalizedQuery) ||
                        gradeNorm.contains(normalizedQuery) ||
                        teacherNorm.contains(normalizedQuery) ||
                        roomNorm.contains(normalizedQuery)
            }
        }
    }

    // Filter reports
    val matchingReports = remember(normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            emptyList()
        } else {
            SYSTEM_GLOBAL_REPORTS.filter { report ->
                val titleNorm = normalizeSearchText(report.title)
                val idNorm = normalizeSearchText(report.id)
                val aliasNorm = normalizeSearchText(report.aliasId)
                val catNorm = normalizeSearchText(report.category)
                val descNorm = normalizeSearchText(report.description)
                val keywordMatch = report.keywords.any { normalizeSearchText(it).contains(normalizedQuery) }

                titleNorm.contains(normalizedQuery) ||
                        idNorm.contains(normalizedQuery) ||
                        aliasNorm.contains(normalizedQuery) ||
                        catNorm.contains(normalizedQuery) ||
                        descNorm.contains(normalizedQuery) ||
                        keywordMatch
            }
        }
    }

    val totalMatchingCount = matchingStudents.size + matchingClasses.size + matchingReports.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .fillMaxHeight(0.92f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("global_search_dialog"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Search Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("global_search_text_field"),
                        placeholder = {
                            Text(
                                text = "Öğrenci, sınıf veya rapor adı/ID ara...",
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("global_search_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Temizle",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .testTag("global_search_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipItem(
                        title = "Tümü",
                        count = if (searchQuery.isNotBlank()) totalMatchingCount else null,
                        isSelected = selectedFilter == SearchFilter.ALL,
                        onClick = { selectedFilter = SearchFilter.ALL },
                        testTag = "search_filter_chip_all"
                    )

                    FilterChipItem(
                        title = "Öğrenciler",
                        count = if (searchQuery.isNotBlank()) matchingStudents.size else students.size,
                        isSelected = selectedFilter == SearchFilter.STUDENTS,
                        onClick = { selectedFilter = SearchFilter.STUDENTS },
                        testTag = "search_filter_chip_students"
                    )

                    FilterChipItem(
                        title = "Sınıflar",
                        count = if (searchQuery.isNotBlank()) matchingClasses.size else classes.size,
                        isSelected = selectedFilter == SearchFilter.CLASSES,
                        onClick = { selectedFilter = SearchFilter.CLASSES },
                        testTag = "search_filter_chip_classes"
                    )

                    FilterChipItem(
                        title = "Raporlar",
                        count = if (searchQuery.isNotBlank()) matchingReports.size else SYSTEM_GLOBAL_REPORTS.size,
                        isSelected = selectedFilter == SearchFilter.REPORTS,
                        onClick = { selectedFilter = SearchFilter.REPORTS },
                        testTag = "search_filter_chip_reports"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Results Content
                if (searchQuery.isBlank()) {
                    // Empty Query State: Show Suggestions & Quick Navigation
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            Text(
                                text = "Önerilen Hızlı Aramalar",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Karne", "8-A", "101", "Tahsilat", "Devamsızlık", "AI Analiz", "Bordro").forEach { suggestion ->
                                    SuggestionChip(
                                        onClick = { searchQuery = suggestion },
                                        label = { Text(suggestion, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }

                        // Popular Reports Quick Access
                        item {
                            Text(
                                text = "Sık Kullanılan Raporlar",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Rapor adına veya ID'sine göre doğrudan erişin (Örn: REP-01, REP-02)",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(SYSTEM_GLOBAL_REPORTS.take(4)) { report ->
                            ReportSearchResultCard(
                                report = report,
                                onSelect = {
                                    onSelectReport(report)
                                    onDismiss()
                                }
                            )
                        }

                        // Sample Classes Quick Access
                        if (classes.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Kayıtlı Sınıflar & Şubeler",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandNavy
                                )
                            }

                            items(classes.take(3)) { cls ->
                                ClassSearchResultCard(
                                    classGroup = cls,
                                    onSelect = {
                                        onSelectClass(cls)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                } else if (totalMatchingCount == 0) {
                    // No Results Found
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "\"$searchQuery\" için sonuç bulunamadı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Öğrenci adı, okul numarası (ör. 101), sınıf adı (ör. 8-A) veya rapor ID'si (ör. REP-01) yazarak tekrar deneyin.",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Active Search Results
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        // 1. STUDENTS RESULTS
                        if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.STUDENTS) && matchingStudents.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "ÖĞRENCİLER",
                                    count = matchingStudents.size,
                                    icon = Icons.Default.School,
                                    color = PrimaryBlue
                                )
                            }

                            items(matchingStudents, key = { it.id }) { student ->
                                StudentSearchResultCard(
                                    student = student,
                                    onSelect = {
                                        onSelectStudent(student)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // 2. CLASSES RESULTS
                        if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.CLASSES) && matchingClasses.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "SINIFLAR & ŞUBELER",
                                    count = matchingClasses.size,
                                    icon = Icons.Default.MeetingRoom,
                                    color = Color(0xFF0D9488)
                                )
                            }

                            items(matchingClasses, key = { it.id }) { cls ->
                                ClassSearchResultCard(
                                    classGroup = cls,
                                    onSelect = {
                                        onSelectClass(cls)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // 3. REPORTS RESULTS
                        if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.REPORTS) && matchingReports.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "RAPORLAR & ANALİZLER",
                                    count = matchingReports.size,
                                    icon = Icons.Default.Analytics,
                                    color = Color(0xFF7C3AED)
                                )
                            }

                            items(matchingReports, key = { it.id }) { report ->
                                ReportSearchResultCard(
                                    report = report,
                                    onSelect = {
                                        onSelectReport(report)
                                        onDismiss()
                                    }
                                )
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
private fun FilterChipItem(
    title: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
                )
                if (count != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
            selectedLabelColor = PrimaryBlue
        ),
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 0.5.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count Sonuç",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Result Card for a Student match
 */
@Composable
fun StudentSearchResultCard(
    student: Student,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .testTag("search_result_student_${student.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            val initials = remember(student.first_name, student.last_name) {
                val f = student.first_name.firstOrNull()?.uppercaseChar() ?: 'Ö'
                val l = student.last_name.firstOrNull()?.uppercaseChar() ?: ' '
                "$f$l".trim()
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(student.avatar_color_hex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = student.full_name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryBlue.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "No: ${student.student_number}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${student.class_name} Sınıfı",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "ID: ${student.id}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (!student.parent_name.isNullOrBlank()) {
                        Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "Veli: ${student.parent_name}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryBlue)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Profili Aç",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Result Card for a Class match
 */
@Composable
fun ClassSearchResultCard(
    classGroup: ClassGroup,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .testTag("search_result_class_${classGroup.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0D9488)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${classGroup.name} Sınıfı",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0D9488).copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "ID: ${classGroup.id}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D9488)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = classGroup.grade_level,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "Öğrt: ${classGroup.teacher_name}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (classGroup.room_number.isNotBlank()) {
                        Text(text = "•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "Derslik: ${classGroup.room_number}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D9488))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Sınıfı Aç",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF0D9488),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Result Card for a specific Report match
 */
@Composable
fun ReportSearchResultCard(
    report: GlobalReportItem,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .testTag("search_result_report_${report.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(report.badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = report.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = report.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(report.badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = report.id,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = report.badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = report.description,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Kategori: ${report.category}",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(report.badgeColor)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Raporu Aç",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = report.badgeColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
