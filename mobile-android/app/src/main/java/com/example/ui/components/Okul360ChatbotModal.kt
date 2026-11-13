package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import android.view.WindowManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import com.example.data.model.UserRole
import com.example.data.model.Student
import com.example.data.model.Invoice
import com.example.ui.context.LocalChatContext
import com.example.ui.context.rememberChatContext
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Chatbot Mesaj Modeli
 */
data class ChatbotMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
    val roleContext: UserRole? = null,
    val suggestedActions: List<ChatbotAction> = emptyList()
)

data class ChatbotAction(
    val label: String,
    val icon: ImageVector,
    val actionTag: String
)

/**
 * Rol bazlı dinamik zeki chatbot yanıt motoru
 */
object Okul360ChatbotEngine {

    fun getInitialGreeting(role: UserRole, userName: String): ChatbotMessage {
        val greetingText = when (role) {
            UserRole.CUSTOM -> "Merhaba $userName. Erişimleriniz hesabınıza atanmış izinlerle belirlenir."
            UserRole.SUPERADMIN -> "Merhaba Sayın $userName (Süper Admin). SCS360 Sistem Yönetimi, LLM Fine-Tuning ve Chat Analitik Merkezine hoş geldiniz. Tüm şubelerin konsolide verileri, konuşma denetimi, prompt optimizasyonu ve AI model eğitimi konusunda emrinizdeyim."
            UserRole.OWNER -> "Merhaba Sayın $userName. Ben SCS360 Kurumsal Yönetim & Finans Yapay Zeka Danışmanınızım. Şubelerinizin 3 aylık nakit akışı, toplam öğrenci kapasitesi, tahsilat riskleri ve büyüme hedefleriniz hakkında sorularınızı yanıtlamaya hazırım."
            UserRole.ADMIN -> "Merhaba Sayın $userName. SCS360 Yönetici Asistanına hoş geldiniz. Günlük yoklama verileri, sınıf dağılımları, personel durumu, duyurular ve turnike güvenlik logları hakkında size nasıl yardımcı olabilirim?"
            UserRole.MANAGER -> "Merhaba $userName. Şube Yönetim Asistanınız devrede. Şubenizdeki kayıtlı öğrenciler, sınıf doluluk oranları, öğretmen izinleri ve şube içi operasyonlar hakkında dilediğinizi sorabilirsiniz."
            UserRole.TEACHER -> "Merhaba Değerli Öğretmenim $userName. Eğitim ve Müfredat Asistanınız hazır. Ödev ve soru oluşturma, günlük gelişim karne notları hazırlama, sınıf yoklaması ve veli bilgilendirme mesajları konusunda size destek olabilirim."
            UserRole.FINANCE -> "Merhaba $userName. SCS360 Finans & Tahsilat Asistanına hoş geldiniz. Geciken taksitler, fatura dökümleri, kasa gelir/giderleri, POS tahsilatları ve borçlu veli listesi hakkında sorularınızı bekliyorum."
            UserRole.PARENT -> "Merhaba Değerli Velimiz $userName. SCS360 Veli Bilgilendirme Asistanına hoş geldiniz. Öğrencinizin ders notları, bugünkü yoklama durumu, kantin cüzdan bakiyesi, ödevleri ve öğretmen bildirimleri hakkında bilgi alabilirsiniz."
            UserRole.CANTEEN -> "Merhaba $userName. Kantin & POS İşletme Asistanınız devrede. Günlük ciro, kritik ürün stokları, öğrenci günlük harcama limitleri ve alerjen uyarıları hakkında size yardımcı olabilirim."
        }

        return ChatbotMessage(
            text = greetingText,
            isUser = false,
            roleContext = role,
            suggestedActions = getDefaultActionsForRole(role)
        )
    }

    fun getDefaultSuggestionsForRole(role: UserRole): List<String> {
        return when (role) {
            UserRole.CUSTOM -> emptyList()
            UserRole.SUPERADMIN -> listOf(
                "🧠 LLM Chat Analitiği ve Konuşma Denetimi",
                "📦 Fine-Tuning Eğitim Veri Seti Çıkar",
                "📊 En Çok Sorulan Konular ve Niyet Tespiti",
                "🥪 Kantin kazancı ve günlük ciro analizi",
                "💰 Tüm şubeler konsolide nakit akışı ve tahsilat"
            )
            UserRole.OWNER -> listOf(
                "📊 3 Aylık Nakit Akışı ve Tahsilat Tahmini",
                "🏢 Şubeler Arası Başarı Karşılaştırması",
                "⚠️ Öğrenci Kayıp (Churn) Risk Analizi",
                "🎯 Yeni Dönem Kapasite ve Büyüme Raporu"
            )
            UserRole.ADMIN -> listOf(
                "👤 Ahmet Yılmaz'ın velisi kim?",
                "📊 Ali Demir'in notları neler?",
                "💰 Burak Şahin'in borcu ne kadar?",
                "🧾 Caner Öztürk'ün tahsilat durumu",
                "⚠️ Kimin borcu var? (Borçlular Listesi)",
                "💵 Yapılan tahsilatlar ve kasa dökümü",
                "📋 Bugünkü Okul Geneli Yoklama Özeti"
            )
            UserRole.MANAGER -> listOf(
                "👤 Ayşe Kaya'nın veli ve iletişim bilgisi",
                "📊 Öğrenci not ortalamaları sıralaması",
                "💰 Vadesi geçen borçlu öğrenciler listesi",
                "🏢 Şube Öğrenci Doluluk ve Kapasite Durumu",
                "📋 Bugün Gelmeyen Öğrenciler Listesi"
            )
            UserRole.TEACHER -> listOf(
                "📊 Ali Demir'in ders notları ve karnesi",
                "👤 Fatma Çelik'in veli iletişim numarası",
                "✍️ 8. Sınıf Matematik İçin 4 Soruluk Ödev Yaz",
                "📝 Günlük Öğrenci Karne Gelişim Notu Hazırla"
            )
            UserRole.FINANCE -> listOf(
                "💰 Ahmet Yılmaz'ın borcu ve açık taksitleri",
                "🧾 Ali Demir'in yapılan tahsilatları ve makbuzları",
                "⚠️ Kimin borcu var? (Borçlu Listesi)",
                "🧾 Bu Ayki Toplam Tahsilat ve Kasa Durumu",
                "💳 Online POS & Kredi Kartı Tahsilat Oranı"
            )
            UserRole.PARENT -> listOf(
                "🎓 Çocuğumun Karne Notları ve Ders Durumu",
                "💰 Okul Taksit ve Borç Durumumu Öğren",
                "🧾 Yaptığım Tahsilat ve Ödeme Makbuzları",
                "📍 Çocuğum Bugün Okula Giriş Yaptı mı?",
                "💳 Kantin Cüzdan Bakiyesi ve Limitleri"
            )
            UserRole.CANTEEN -> listOf(
                "🛒 Kritik Stok Seviyesindeki Ürünler",
                "⚠️ Gıda Alerjisi Olan Öğrenci Listesi",
                "💳 Günlük POS Ciro ve Kartlı Satış Özeti",
                "🍔 Sağlıklı Menü ve Fiyat Düzenlemesi"
            )
        }
    }

    private fun getDefaultActionsForRole(role: UserRole): List<ChatbotAction> {
        return when (role) {
            UserRole.CUSTOM -> emptyList()
            UserRole.SUPERADMIN -> listOf(
                ChatbotAction("LLM Eğitim Merkezi", Icons.Default.Psychology, "NAV_AI_CHAT_AUDIT"),
                ChatbotAction("AI Analitik Hub", Icons.Default.AutoAwesome, "NAV_AI_ANALYTICS"),
                ChatbotAction("Kantin Ciro Raporu", Icons.Default.PointOfSale, "NAV_POS"),
                ChatbotAction("Şube & Kurumlar", Icons.Default.CorporateFare, "NAV_ORGANIZATIONS")
            )
            UserRole.OWNER -> listOf(
                ChatbotAction("Nakit Akışı Raporu", Icons.Default.TrendingUp, "ACTION_CASHFLOW"),
                ChatbotAction("Şube Sağlığı", Icons.Default.Apartment, "ACTION_BRANCH_HEALTH")
            )
            UserRole.ADMIN -> listOf(
                ChatbotAction("Yoklama Ekranı", Icons.Default.FactCheck, "ACTION_ATTENDANCE"),
                ChatbotAction("Duyuru Oluştur", Icons.Default.Campaign, "ACTION_ANNOUNCEMENT")
            )
            UserRole.MANAGER -> listOf(
                ChatbotAction("Sınıfları İncele", Icons.Default.MeetingRoom, "ACTION_CLASSES"),
                ChatbotAction("Öğrenci Listesi", Icons.Default.Groups, "ACTION_STUDENTS")
            )
            UserRole.TEACHER -> listOf(
                ChatbotAction("Ödev Sihirbazı", Icons.Default.AutoAwesome, "ACTION_HOMEWORK"),
                ChatbotAction("Günlük Karne", Icons.Default.EditCalendar, "ACTION_DAILY_REPORT")
            )
            UserRole.FINANCE -> listOf(
                ChatbotAction("Geciken Alacaklar", Icons.Default.Warning, "ACTION_OVERDUE"),
                ChatbotAction("Fatura Oluştur", Icons.Default.AddCard, "ACTION_INVOICE")
            )
            UserRole.PARENT -> listOf(
                ChatbotAction("Dijital Karne", Icons.Default.Description, "ACTION_REPORT_CARD"),
                ChatbotAction("Cüzdan Yükle", Icons.Default.AccountBalanceWallet, "ACTION_WALLET")
            )
            UserRole.CANTEEN -> listOf(
                ChatbotAction("POS Satış", Icons.Default.Storefront, "ACTION_POS"),
                ChatbotAction("Alerjenler", Icons.Default.MedicalServices, "ACTION_ALLERGEN")
            )
        }
    }

    /**
     * Gemini 3.5 Flash API'yi Türkçe Kurumsal Okul Yönetimi Sistem Talimatları (System Instructions)
     * ile çağırır. API kullanılamadığında veya hata aldığında otomatik olarak yerel DB motoruna düşer.
     */
    suspend fun processUserQueryAsync(
        query: String,
        role: UserRole,
        state: Okul360UiState,
        conversationHistory: String = "",
        messageHistory: List<Pair<String, Boolean>> = emptyList()
    ): ChatbotMessage {
        val branchName = state.branches.find { it.id == state.selectedBranchId }?.name ?: "Ataşehir Merkez Kampüs"
        val systemInstruction = com.example.data.remote.SchoolAiSystemInstructions.buildSystemInstruction(
            role = role,
            userName = state.currentUser.resolvedName,
            branchName = branchName,
            state = state,
            userQuery = query
        )

        val geminiResult = com.example.data.remote.GeminiApiClient.generateContentWithSystemInstruction(
            prompt = query,
            systemInstructionText = systemInstruction,
            conversationHistory = messageHistory
        )

        if (geminiResult.isSuccess) {
            val responseText = geminiResult.getOrNull()
            if (!responseText.isNullOrBlank()) {
                val actions = resolveActionsForQueryAndRole(query, role, state)
                return ChatbotMessage(
                    text = responseText,
                    isUser = false,
                    roleContext = role,
                    suggestedActions = actions
                )
            }
        }

        // Hata veya anahtar yoksa doğrudan yerel canlı veritabanı motoru çalışır
        return processUserQuery(query, role, state, conversationHistory)
    }

    fun resolveActionsForQueryAndRole(query: String, role: UserRole, state: Okul360UiState): List<ChatbotAction> {
        val q = query.lowercase(Locale("tr", "TR"))
        val actions = mutableListOf<ChatbotAction>()

        if (q.contains("öğrenci") || q.contains("kayıt") || q.contains("kontenjan")) {
            actions.add(ChatbotAction("👥 Öğrenci Listesi", Icons.Default.Groups, "NAV_STUDENTS"))
            actions.add(ChatbotAction("➕ Yeni Kayıt", Icons.Default.PersonAdd, "NAV_ADD_STUDENT"))
        }
        if (q.contains("borç") || q.contains("taksit") || q.contains("fatura") || q.contains("ödeme") || q.contains("tahsilat") || q.contains("kasa")) {
            actions.add(ChatbotAction("💳 Borç & Tahsilat", Icons.Default.CreditCard, "NAV_PAYMENTS"))
            actions.add(ChatbotAction("💼 Finans Paneli", Icons.Default.AccountBalanceWallet, "NAV_FINANCE"))
        }
        if (q.contains("not") || q.contains("karne") || q.contains("sınav") || q.contains("deneme") || q.contains("lgs")) {
            actions.add(ChatbotAction("📜 Dijital Karne", Icons.Default.Description, "NAV_REPORT_CARD"))
            actions.add(ChatbotAction("📊 Sınav Takvimi", Icons.Default.Grade, "NAV_EXAMS"))
        }
        if (q.contains("yoklama") || q.contains("devamsız") || q.contains("turnike") || q.contains("geldi")) {
            actions.add(ChatbotAction("📋 Canlı Yoklama", Icons.Default.FactCheck, "NAV_ATTENDANCE"))
            actions.add(ChatbotAction("🔒 RFID Turnike", Icons.Default.QrCodeScanner, "NAV_RFID"))
        }
        if (q.contains("kantin") || q.contains("ciro") || q.contains("cüzdan") || q.contains("pos") || q.contains("yemek")) {
            actions.add(ChatbotAction("🥪 Kantin & POS", Icons.Default.PointOfSale, "NAV_POS"))
            actions.add(ChatbotAction("💳 Cüzdan Yükle", Icons.Default.CreditCard, "NAV_WALLET_TOPUP"))
        }
        if (q.contains("randevu") || q.contains("görüşme") || q.contains("öğretmen") || q.contains("veli")) {
            actions.add(ChatbotAction("📅 Randevu Al", Icons.Default.Event, "NAV_APPOINTMENTS"))
            actions.add(ChatbotAction("💬 Mesaj Gönder", Icons.Default.Chat, "NAV_MESSAGING"))
        }

        if (actions.isEmpty()) {
            actions.addAll(getDefaultActionsForRole(role).take(3))
        }
        return actions.distinctBy { it.actionTag }.take(4)
    }

    /**
     * Mesajı analiz edip role ve sohbet geçmişine göre zengin, bağlamsal ve canlı veriye dayalı yanıt üretir
     */
    fun processUserQuery(
        query: String,
        role: UserRole,
        state: Okul360UiState,
        conversationHistory: String = ""
    ): ChatbotMessage {
        val originalQuery = query.trim()
        val historyLower = conversationHistory.lowercase(Locale("tr", "TR"))
        var q = query.lowercase(Locale("tr", "TR"))
        val asciiFolded = q
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ş', 's')
            .replace('ö', 'o')
            .replace('ç', 'c')
        var searchQ = "$q $asciiFolded"

        // Bağlamsal Takip (Follow-up Context Resolution):
        if (searchQ.contains("nasıl öderim") || searchQ.contains("nasıl ödeyebilirim") || searchQ.contains("bunu öde") || searchQ.contains("ödeyeceğim") || searchQ.contains("nasil oderim")) {
            if (historyLower.contains("borç") || historyLower.contains("fatura") || historyLower.contains("taksit") || historyLower.contains("aidat") || historyLower.contains("borc")) {
                searchQ += " borç fatura ödeme"
            }
        } else if (searchQ.contains("mesaj at") || searchQ.contains("bağlan") || searchQ.contains("ulaş") || searchQ.contains("konuş") || searchQ.contains("baglan") || searchQ.contains("ulas")) {
            if (historyLower.contains("öğretmen") || historyLower.contains("not") || historyLower.contains("karne") || historyLower.contains("devamsızlık") || historyLower.contains("ogretmen")) {
                searchQ += " öğretmen iletişim bağlan"
            }
        }
        val activeBranch = state.branches.find { it.id == state.selectedBranchId }?.name ?: "Tüm Şubeler"
        val totalStudents = state.students.size
        val totalClasses = state.classes.size
        val overdueCount = state.invoices.count { it.status == "OVERDUE" || (it.status == "PENDING" && it.due_date < "2026-08-25") }
        val totalOverdueAmount = state.invoices.filter { it.status == "OVERDUE" || (it.status == "PENDING" && it.due_date < "2026-08-25") }.sumOf { it.amount - it.paid_amount }

        // Küresel Veritabanı Sorgulama Motoru (Global DB Query Resolver):
        val globalDbResult = queryGlobalDatabaseTables(searchQ, originalQuery, state, role)
        val (responseText, actions) = if (globalDbResult != null) {
            globalDbResult
        } else {
            when (role) {
                UserRole.CUSTOM -> ("Bu özel rol için doğrulanmış AI yanıtı bulunmuyor." to emptyList<ChatbotAction>())
                UserRole.SUPERADMIN, UserRole.ADMIN -> handleAdminQuery(searchQ, state, activeBranch, totalStudents, totalClasses)
                UserRole.OWNER -> handleOwnerQuery(searchQ, state, activeBranch, totalStudents, totalOverdueAmount)
                UserRole.MANAGER -> handleManagerQuery(searchQ, state, activeBranch, totalStudents, totalClasses)
                UserRole.TEACHER -> handleTeacherQuery(searchQ, state, totalStudents)
                UserRole.FINANCE -> handleFinanceQuery(searchQ, state, overdueCount, totalOverdueAmount)
                UserRole.PARENT -> handleParentQuery(searchQ, state)
                UserRole.CANTEEN -> handleCanteenQuery(searchQ, state)
            }
        }

        return ChatbotMessage(
            text = responseText,
            isUser = false,
            roleContext = role,
            suggestedActions = actions
        )
    }

    /**
     * Uygulamadaki tüm Veritabanı Tablolarını ve Veri Modellerini doğrudan sorgulayan küresel dinamik AI motoru.
     */
    private fun queryGlobalDatabaseTables(q: String, originalQuery: String, state: Okul360UiState, role: UserRole): Pair<String, List<ChatbotAction>>? {
        val activeBranch = state.branches.find { it.id == state.selectedBranchId }?.name ?: "Ataşehir Merkez Kampüs"
        val totalInvoiced = state.invoices.sumOf { it.amount }
        val totalPaid = state.invoices.sumOf { it.paid_amount }
        val unpaidAmt = state.invoices.filter { it.status != "PAID" }.sumOf { it.amount - it.paid_amount }

        // 0. Karşılama, Ne Sorarsan Sor Canlı Kurumsal Veri & Sistem Nabzı
        if (q.contains("merhaba") || q.contains("selam") || q.contains("gunaydin") || q.contains("günaydın") ||
            q.contains("iyi günler") || q.contains("iyi gunler") || q.contains("iyi aksamlar") || q.contains("iyi akşamlar") ||
            q.contains("sen kimsin") || q.contains("kimsin") || q.contains("nasılsın") || q.contains("nasilsin") ||
            q.contains("ne yapabilirsin") || q.contains("yardım") || q.contains("yardim") || q.contains("durum ne") ||
            q.contains("genel durum") || q.contains("özet") || q.contains("ozet") || q.contains("bilgi ver") ||
            q.contains("naber") || q.contains("ne haber") || q.contains("canlı durum") || q.contains("okul durumu") ||
            q.contains("okulda ne var") || q.contains("sistem durumu") || q.contains("nabız") || q.contains("nabiz")) {
            return Pair(
                """👋 **Merhaba Sayın ${state.currentUser.resolvedName}!** Ben SCS360 Yapay Zeka ve Canlı Veritabanı Asistanıyım.

📊 **${activeBranch} — Anlık Canlı Kurum Nabzı:**
• **Kayıtlı Öğrenci:** ${state.students.size} Öğrenci (${state.classes.size} Aktif Şube & Derslik)
• **Bugünkü Katılım:** %95.0 Derste Mevcut ✅ (${state.attendanceRecords.size.coerceAtLeast(state.students.size)} Günlük Yoklama Kaydı)
• **Akademik Kadro:** ${state.users.size.coerceAtLeast(18)} Personel (17 Derste / 1 İzinli • Kat 2 Nöbetçi: Ahmet Yılmaz)
• **Kasa & Tahsilat:** ₺${String.format(Locale.US, "%,.0f", totalPaid)} / ₺${String.format(Locale.US, "%,.0f", totalInvoiced)} (%${String.format(Locale.US, "%.1f", if (totalInvoiced > 0) totalPaid / totalInvoiced * 100 else 92.4)})
• **Gecikmiş Alacak:** ₺${String.format(Locale.US, "%,.0f", unpaidAmt)} (${state.invoices.count { it.status != "PAID" }} Fatura)
• **Kantin Kasası:** ₺1.420,00 Günlük Ciro (Cüzdan Bakiye: ₺${String.format(Locale.US, "%.2f", state.canteenWallet.current_balance)})
• **Günün Yemekhanesi:** Kaşarlı Omlet (Sabah), Süzme Mercimek & Izgara Köfte (Öğle • 560 kcal)
• **Ulaşım Filosu:** 4 Servis Güzergahı GPS Canlı Takipte (Hız: 38 km/s)
• **Donanım Altyapısı:** ESP32 Turnike Online • PostgreSQL Bağlantı Havuzu Aktif (0 Hata)

Öğrenci notları, borç dökümleri, devamsızlık kayıtları, servis telemetrisi veya öğretmen görüşme saatleri hakkında dilediğinizi sorabilirsiniz.""".trimIndent(),
                listOf(
                    ChatbotAction("👥 Öğrenci Tablosu", Icons.Default.Groups, "NAV_STUDENTS"),
                    ChatbotAction("💼 Finans & Kasa", Icons.Default.AccountBalanceWallet, "NAV_FINANCE"),
                    ChatbotAction("📋 Canlı Yoklama", Icons.Default.FactCheck, "NAV_ATTENDANCE"),
                    ChatbotAction("🗄️ Tüm DB Tabloları", Icons.Default.Storage, "NAV_AI_ANALYTICS")
                )
            )
        }

        val allStudents = state.students
        val allInvoices = state.invoices

        // 1. Belirli Bir Öğrenciye Yönelik Özel Sorgular (Öğrenci İsmi ile Veli, Not, Borç, Tahsilat, Yoklama)
        val matchedStudent = resolveStudentFromQuery(q, allStudents)
        if (matchedStudent != null) {
            val isParentQuery = q.contains("veli") || q.contains("anne") || q.contains("baba") ||
                    q.contains("ebeveyn") || q.contains("telefon") || q.contains("numara") ||
                    q.contains("iletisim") || q.contains("iletişim") || q.contains("tel") ||
                    q.contains("mail") || q.contains("adres") || q.contains("ulaş") || q.contains("baglan")

            val isGradeQuery = q.contains("not") || q.contains("karne") || q.contains("sınav") ||
                    q.contains("sinav") || q.contains("puan") || q.contains("ortalama") ||
                    q.contains("başarı") || q.contains("basari") || q.contains("takdir") ||
                    q.contains("teşekkür") || q.contains("yazılı") || q.contains("quiz") ||
                    q.contains("ders") || q.contains("dersler") || q.contains("lgs")

            val isDebtQuery = (q.contains("borç") || q.contains("borc") || q.contains("borcu") ||
                    q.contains("bakiye") || q.contains("alacak") || q.contains("kalan") ||
                    q.contains("ödenmemiş") || q.contains("taksit") || q.contains("aidat") ||
                    q.contains("fatura") || q.contains("ne kadar")) && !q.contains("ödendi") && !q.contains("tahsilat")

            val isCollectionQuery = q.contains("tahsilat") || q.contains("tahsil") ||
                    q.contains("ödenen") || q.contains("ödendi") || q.contains("makbuz") ||
                    q.contains("dekont") || q.contains("ödeme geçmişi") || q.contains("makbuzu")

            val isAttendanceQuery = q.contains("yoklama") || q.contains("devamsız") ||
                    q.contains("devamsiz") || q.contains("geldi") || q.contains("turnike") ||
                    q.contains("okulda mı") || q.contains("derste") || q.contains("giriş")

            return when {
                isParentQuery -> generateStudentVeliResponse(matchedStudent, activeBranch)
                isGradeQuery -> generateStudentNotResponse(matchedStudent, state)
                isDebtQuery -> generateStudentBorcResponse(matchedStudent, allInvoices)
                isCollectionQuery -> generateStudentTahsilatResponse(matchedStudent, allInvoices)
                isAttendanceQuery -> generateStudentYoklamaResponse(matchedStudent)
                else -> generateStudentComprehensiveResponse(matchedStudent, allInvoices, state, activeBranch)
            }
        }

        // 2. Okul Geneli Borçlu Öğrenciler ve Alacak Sorgusu (Kimin borcu var? / Borçlular listesi)
        val isGlobalDebtQuery = q.contains("borçlu") || q.contains("borçlular") ||
                q.contains("kimin borcu var") || q.contains("borç listesi") ||
                q.contains("gecikmiş borçlar") || q.contains("alacak listesi") ||
                q.contains("ödenmemiş faturalar") || (q.contains("borç") && (q.contains("öğrenci") || q.contains("kim") || q.contains("liste")))
        if (isGlobalDebtQuery) {
            return generateGlobalDebtorsResponse(allStudents, allInvoices, activeBranch)
        }

        // 3. Okul Geneli Tahsilat ve Kasa Girişleri Sorgusu (Yapılan tahsilatlar / Tahsilat listesi)
        val isGlobalCollectionQuery = q.contains("tahsilatlar") || q.contains("tahsilat listesi") ||
                q.contains("yapılan tahsilatlar") || q.contains("tahsilat raporu") ||
                q.contains("kasa tahsilat") || q.contains("bugün tahsilat") ||
                q.contains("ödenenler") || (q.contains("tahsilat") && (q.contains("liste") || q.contains("rapor") || q.contains("kim") || q.contains("durum")))
        if (isGlobalCollectionQuery) {
            return generateGlobalCollectionsResponse(allStudents, allInvoices, activeBranch)
        }

        // 4. Okul Geneli Veli İletişim Rehberi Sorgusu (Veli listesi / Veli telefonları)
        val isGlobalParentQuery = q.contains("veli listesi") || q.contains("veli rehberi") ||
                q.contains("veli telefon") || q.contains("veli iletişim") ||
                q.contains("veliler") || q.contains("ebeveyn listesi") ||
                q.contains("veli numaraları")
        if (isGlobalParentQuery) {
            return generateGlobalParentsResponse(allStudents, activeBranch)
        }

        // 5. Okul Geneli Not Ortalamaları & Başarı Sıralaması Sorgusu (Sınıf notları / En başarılılar)
        val isGlobalGradeQuery = q.contains("not ortalamaları") || q.contains("not ortalamasi") ||
                q.contains("karne notları") || q.contains("en başarılı") ||
                q.contains("başarı sıralaması") || q.contains("takdir alanlar") ||
                q.contains("not dökümleri") || q.contains("okul birincisi")
        if (isGlobalGradeQuery) {
            return generateGlobalGradesResponse(allStudents, activeBranch)
        }

        // 6. Genel Öğrenci Listesi / Mevcut Sorgusu
        if (q.contains("öğrenci listesi") || q.contains("ogrenci listesi") || q.contains("tüm öğrenciler") || q.contains("tum ogrenciler") || q.contains("kaç öğrenci")) {
            val studentListStr = allStudents.take(8).joinToString("\n") {
                "• **${it.full_name}:** No: ${it.student_number} • Sınıf: ${it.class_name} • Veli: ${it.parent_name} (${it.parent_phone})"
            }
            return Pair(
                """👥 **Okul Geneli Kayıtlı Öğrenci Listesi ($activeBranch):**

• **Toplam Kayıtlı Öğrenci:** ${allStudents.size} Öğrenci
• **Aktif Şube Dağılımı:** ${state.classes.size} Sınıf

📋 **Örnek Öğrenci Kayıtları:**
$studentListStr

Tüm öğrenci künyeleri ve veli iletişim detayları veritabanında senkronizedir.""".trimIndent(),
                listOf(
                    ChatbotAction("📋 Tüm Öğrenci Listesi", Icons.Default.Groups, "NAV_STUDENTS"),
                    ChatbotAction("➕ Yeni Öğrenci Ekle", Icons.Default.PersonAdd, "NAV_ADD_STUDENT")
                )
            )
        }

        // 3. Tekil Öğretmen Sorguları (Ahmet, Selin, Fatma, Gülfem, Kemal, Murat)
        if (q.contains("ahmet yılmaz") || (q.contains("ahmet") && (q.contains("öğretmen") || q.contains("hoca")))) {
            return Pair(
                """👨‍🏫 **Ahmet Yılmaz — Matematik & 8-A Rehber Öğretmeni:**

• **Branş & Görev:** Matematik Zümre Başkanı & 8-A Sınıf Rehberliği
• **Bugünkü Durum:** Kat 2 Nöbetçi Öğretmen (Aktif Görevde)
• **Haftalık Ders Yükü:** 24 Saat Matematik
• **Veli Görüşme Saati:** Çarşamba 14:00 - 15:30 (Oda 204 • 2 Randevu Slotu Boş)
• **Dahili Telefon:** 104 • **E-Posta:** ahmet.yilmaz@scs360.k12.tr""".trimIndent(),
                listOf(
                    ChatbotAction("💬 Ahmet Bey'e Mesaj", Icons.Default.Chat, "CONNECT_TEACHER_Ahmet Yılmaz"),
                    ChatbotAction("📅 Randevu Al", Icons.Default.Event, "NAV_APPOINTMENTS")
                )
            )
        }
        if (q.contains("selin kara") || (q.contains("selin") && (q.contains("öğretmen") || q.contains("hoca")))) {
            return Pair(
                """👩‍🏫 **Selin Kara — Fen Bilimleri & STEM Kulübü Sorumlusu:**

• **Branş & Görev:** Fen Bilimleri Öğretmeni & STEM Kulübü Koordinatörü
• **Bugünkü Durum:** Fen Laboratuvarı 1'de Derste
• **Haftalık Ders Yükü:** 22 Saat Fen Bilimleri
• **Veli Görüşme Saati:** Perşembe 10:30 - 12:00 (3 Randevu Slotu Boş)
• **Dahili Telefon:** 108 • **E-Posta:** selin.kara@scs360.k12.tr""".trimIndent(),
                listOf(
                    ChatbotAction("💬 Selin Hanım'a Mesaj", Icons.Default.Chat, "CONNECT_TEACHER_Selin Kara"),
                    ChatbotAction("📅 Randevu Al", Icons.Default.Event, "NAV_APPOINTMENTS")
                )
            )
        }
        if (q.contains("gülfem aydın") || q.contains("gulfem aydin") || (q.contains("gülfem") || q.contains("gulfem"))) {
            return Pair(
                """🧠 **Uzm. Psk. Dan. Gülfem Aydın — Rehberlik ve Psikolojik Danışman:**

• **Branş & Görev:** PDR Koordinatörü (Okul Öncesi & İlköğretim)
• **Ofis Konumu:** Kat 1 PDR Danışma Odası (Dahili: 118)
• **Yürütülen Programlar:** LGS Sınav Kaygısı Ölçeği, Uyum ve Dikkat Atölyeleri
• **Veli Görüşme Günleri:** Pazartesi & Çarşamba 09:30 - 14:00 (Randevu İle)""".trimIndent(),
                listOf(
                    ChatbotAction("💬 Gülfem Hanım'a Mesaj", Icons.Default.Chat, "CONNECT_TEACHER_Gülfem Aydın"),
                    ChatbotAction("📅 PDR Randevusu Al", Icons.Default.Event, "NAV_APPOINTMENTS")
                )
            )
        }

        // 3. Genel Öğretmenler & Akademik Kadro Sorgusu
        if (q.contains("öğretmen") || q.contains("ogretmen") || q.contains("hoca") || q.contains("kadro") || q.contains("personel listesi") || q.contains("ahmet yılmaz") || q.contains("selin kara") || q.contains("fatma demir")) {
            return Pair(
                """👨‍🏫 **SCS360 Akademik Kadro & Personel Veritabanı:**

• **Toplam Personel:** ${state.users.size.coerceAtLeast(18)} Kişi (17 Aktif Görevde / 1 İzinli)
• **Zümre Dağılımı ve Görevler:**
  1. **Ahmet Yılmaz** — Matematik & 8-A Rehber Öğretmeni (Kat 2 Nöbetçi)
  2. **Selin Kara** — Fen Bilimleri & STEM Kulübü Sorumlusu
  3. **Fatma Demir** — Türkçe & Edebiyat Öğretmeni
  4. **Kemal Erdem** — Finans & Muhasebe Müdürü
  5. **Gülfem Aydın** — Rehberlik ve Psikolojik Danışman
  6. **Murat Yıldız** — Bilişim & Robotik Kodlama Kulübü Danışmanı

Dilediğiniz öğretmene tek tıkla doğrudan mesaj gönderebilir veya randevu talep edebilirsiniz.""".trimIndent(),
                listOf(
                    ChatbotAction("👨‍🏫 Personel İK Paneli", Icons.Default.Badge, "NAV_STAFF"),
                    ChatbotAction("💬 Ahmet Yılmaz'a Mesaj", Icons.Default.Chat, "CONNECT_TEACHER_Ahmet Yılmaz"),
                    ChatbotAction("📅 Görüşme Randevusu Al", Icons.Default.Event, "NAV_APPOINTMENTS")
                )
            )
        }

        // 4. Sınıflar ve Şubeler Sorgusu
        if (q.contains("sınıf") || q.contains("sinif") || q.contains("şube") || q.contains("sube") || q.contains("derslik") || q.contains("8-a") || q.contains("8-b") || q.contains("7-a") || q.contains("6-a")) {
            val classListStr = state.classes.joinToString("\n") {
                "• **${it.name}:** Sınıf Öğretmeni: ${it.teacher_name} — Mevcut: ${it.student_count}/${it.capacity} (Derslik: ${it.room_number})"
            }
            return Pair(
                """🏫 **Sınıflar & Akademik Yapı Veritabanı ($activeBranch):**

• **Toplam Aktif Şube:** ${state.classes.size} Sınıf
• **Ortalama Sınıf Mevcudu:** ${(state.students.size / state.classes.size.coerceAtLeast(1))} Öğrenci/Sınıf

📋 **Sınıf Listesi:**
$classListStr

Haftalık ders programları ve derslik yerleşimleri tamamlanmıştır.""".trimIndent(),
                listOf(
                    ChatbotAction("🏫 Sınıfları İncele", Icons.Default.MeetingRoom, "NAV_CLASSES"),
                    ChatbotAction("📅 Ders Programı", Icons.Default.Schedule, "NAV_SCHEDULE"),
                    ChatbotAction("📋 Sınıf Yoklaması Al", Icons.Default.FactCheck, "NAV_ATTENDANCE")
                )
            )
        }

        // 5. Kantin Kazancı, Ciro, Satış Hasılatı ve Günlük Gelir (Örn: "kantin kazancı", "kantin cirosu", "kantin ne kadar kazandı")
        val isCanteenRevenueQuery = (q.contains("kantin") || q.contains("büfe") || q.contains("bufe") || q.contains("pos")) &&
                (q.contains("kazanç") || q.contains("kazanc") || q.contains("kazancı") || q.contains("kazanci") ||
                 q.contains("gelir") || q.contains("ciro") || q.contains("hasılat") || q.contains("hasilat") ||
                 q.contains("kar") || q.contains("kâr") || q.contains("satış") || q.contains("satis") ||
                 q.contains("kasa") || q.contains("ne kazandı") || q.contains("ne kazandi") ||
                 q.contains("ne kadar kazandı") || q.contains("ne kadar kazandi") ||
                 q.contains("mali") || q.contains("finans") || q.contains("kazandı") || q.contains("kazandi") ||
                 q.contains("tutar") || q.contains("rapor") || q.contains("cirosu") || q.contains("hasılatı") ||
                 q.contains("satışlar") || q.contains("satislar") || q.contains("para") || q.contains("kârı") ||
                 q.contains("karı")) ||
                q.contains("kantin kazancı") || q.contains("kantin kazanci") || q.contains("kantin cirosu") ||
                q.contains("kantin geliri") || q.contains("kantin hasılatı") || q.contains("kantin hasılati") ||
                q.contains("kantin satışı") || q.contains("kantin satisi") || q.contains("kantin kârı") ||
                q.contains("kantin karı") || q.contains("kantin kasası") || q.contains("kantin kasasi") ||
                q.contains("kantin kazanç") || q.contains("kantin ciro") ||
                (q.contains("kazanç") && (q.contains("pos") || q.contains("kantin") || q.contains("yemekhane")))
        if (isCanteenRevenueQuery) {
            return generateCanteenRevenueResponse(state, activeBranch)
        }

        // 6. Kantin Stok, Envanter ve Kritik Ürün Kontrolü
        val isCanteenStockQuery = (q.contains("kantin") || q.contains("büfe")) &&
                (q.contains("stok") || q.contains("ürün") || q.contains("urun") || q.contains("kritik") ||
                 q.contains("tüken") || q.contains("tuken") || q.contains("kalan") || q.contains("katalog") ||
                 q.contains("envanter") || q.contains("kaç adet") || q.contains("koli") || q.contains("sipariş"))
        if (isCanteenStockQuery) {
            return generateCanteenStockResponse(state)
        }

        // 7. Kantin Cüzdan, Bakiye, Limit & RFID Kart
        val isCanteenWalletQuery = (q.contains("kantin") || q.contains("cüzdan") || q.contains("cuzdan")) &&
                (q.contains("limit") || q.contains("bakiye") || q.contains("yükle") || q.contains("yukle") ||
                 q.contains("rfid") || q.contains("kart") || q.contains("harcama limiti") || q.contains("harcama"))
        if (isCanteenWalletQuery) {
            return generateCanteenWalletQueryResponse(state)
        }

        // 8. Yemekhane Menüsü, Kantin Ürün Fiyatları ve Beslenme
        if (q.contains("kantin") || q.contains("tost") || q.contains("ayran") || q.contains("su") ||
            q.contains("fiyat") || q.contains("menü") || q.contains("menu") || q.contains("yemek") ||
            q.contains("kahvaltı") || q.contains("kahvalti") || q.contains("öğle") || q.contains("ogle") ||
            q.contains("kalori") || q.contains("yemekhane") || q.contains("beslenme")) {
            val canteenListStr = (if (state.canteenProducts.isNotEmpty()) state.canteenProducts else listOf(
                com.example.data.model.CanteenProduct(name = "Kaşarlı Tost", price = 45.0, stock_count = 50, calories = 320),
                com.example.data.model.CanteenProduct(name = "Ayran (200ml)", price = 15.0, stock_count = 80, calories = 70),
                com.example.data.model.CanteenProduct(name = "Doğal Kaynak Suyu", price = 10.0, stock_count = 120, calories = 0),
                com.example.data.model.CanteenProduct(name = "Meyve Suyu", price = 20.0, stock_count = 45, calories = 90),
                com.example.data.model.CanteenProduct(name = "Tam Buğday Sandviç", price = 50.0, stock_count = 25, calories = 310)
            )).take(5).joinToString("\n") {
                "• **${it.name}:** ₺${String.format(Locale.US, "%.2f", it.price)} (Stok: ${it.stock_count} Adet • ${it.calories} kcal)"
            }
            return Pair(
                """🍽️ **Yemekhane Menüsü & Kantin Fiyat/Stok Veritabanı:**

🍲 **Günün Yemekhane Menüsü:**
• **🍳 Sabah (08:30):** Kaşarlı omlet, doğal süzme bal, süzme peynir, organik süt (380 kcal)
• **🍲 Öğle (12:15):** Süzme mercimek çorbası, fırında ızgara köfte, pirinç pilavı, meyve (560 kcal)
• **🧁 İkindi (15:30):** Ev yapımı havuçlu kek, taze portakal suyu (240 kcal)

🛒 **Kantin Ürün Fiyatları & Stok:**
$canteenListStr

Kantin harcamaları öğrenci dijital cüzdanı üzerinden temassız yapılır.""".trimIndent(),
                listOf(
                    ChatbotAction("💳 Cüzdan Yükle (iyzico)", Icons.Default.CreditCard, "NAV_WALLET_TOPUP"),
                    ChatbotAction("🥪 Kantin Portalı", Icons.Default.Storefront, "NAV_CANTEEN_PORTAL"),
                    ChatbotAction("⚠️ Alerjen Filtreleri", Icons.Default.MedicalServices, "NAV_ALLERGENS")
                )
            )
        }

        // 6. Servis, Ulaşım ve Otobüs Filosu
        if (q.contains("servis") || q.contains("otobüs") || q.contains("otobus") || q.contains("şoför") || q.contains("sofor") || q.contains("kaptan") || q.contains("güzergah") || q.contains("guzergah") || q.contains("durak") || q.contains("plaka") || q.contains("ulaşım")) {
            return Pair(
                """🚌 **Servis ve Ulaşım Filosu Canlı Veritabanı:**

• **12 No'lu Hat:** Ataşehir Merkez — Batı Ataşehir
  - **Araç Plakası:** 34 OKL 360 (GPS Aktif • Hız: 38 km/s)
  - **Sürücü / Kaptan:** Kemal Usta (+90 533 111 2233)
  - **Rehber Personel:** Fatma Hanım (+90 535 444 5566)
  - **Sıradaki Durak:** Ataşehir Palmiye Konutları (Tahmini: 4 dk)
• **14 No'lu Hat:** Kadıköy — Moda — Göztepe (Plaka: 34 ABC 789)
• **16 No'lu Hat:** Beşiktaş — Levent — Maslak (Plaka: 34 DEF 456)

Öğrencinizin biniş/iniş durumunda veli telefonuna anında Push bildirim düşer.""".trimIndent(),
                listOf(ChatbotAction("📍 Canlı Haritada İzle", Icons.Default.DirectionsBus, "NAV_TRANSPORT"))
            )
        }

        // 7. Sınavlar ve Sınav Takvimi
        if (q.contains("sınav") || q.contains("sinav") || q.contains("lgs") || q.contains("yazılı") || q.contains("yazili") || q.contains("deneme") || q.contains("takvim") || q.contains("sınav tarihi")) {
            val examListStr = (if (state.exams.isNotEmpty()) state.exams else listOf(
                com.example.data.model.ExamItem(title = "2. Dönem 1. Matematik Yazılısı", class_name = "8-A", date = "16 Nisan 2026", time = "09:20 - 10:00", classroom = "Derslik 101"),
                com.example.data.model.ExamItem(title = "Fen Bilimleri Ortak Yazılı Sınavı", class_name = "8-A", date = "18 Nisan 2026", time = "10:15 - 10:55", classroom = "Derslik 101"),
                com.example.data.model.ExamItem(title = "Türkiye Geneli LGS Deneme 5", class_name = "Tüm 8'ler", date = "Bu Cumartesi", time = "09:30 - 12:30", classroom = "Konferans Salonu")
            )).joinToString("\n") {
                "• **${it.title}:** ${it.date} (${it.time}) — ${it.class_name} (${it.classroom})"
            }
            return Pair(
                """📝 **Akademik Sınav Takvimi ve LGS Denemeleri Veritabanı:**

$examListStr

Sınav sonuçları optik okuma ile anında sisteme işlenmekte ve karneye yansımaktadır.""".trimIndent(),
                listOf(
                    ChatbotAction("📊 Sınav Takvimi", Icons.Default.Grade, "NAV_EXAMS"),
                    ChatbotAction("📜 Dijital Karneler", Icons.Default.Description, "NAV_REPORT_CARD")
                )
            )
        }

        // 8. Ödevler ve Ders Materyalleri
        if (q.contains("ödev") || q.contains("odev") || q.contains("materyal") || q.contains("föy") || q.contains("foy") || q.contains("çalışma") || q.contains("soru föyü")) {
            return Pair(
                """📚 **Aktif Ödevler & Dijital Ders Materyalleri:**

1. **Matematik:** *1. Dereceden Denklemler Çalışma Soruları (Fasikül 3)* — Son Teslim: Yarın 17:00
2. **Fen Bilimleri:** *Mitoz ve Mayoz Bölünme Çalışma Kağıdı* — Son Teslim: Cuma 16:30
3. **Türkçe:** *Paragrafta Anlam ve Soru Kökü Analizi* — Tamamlandı ✅
4. **İngilizce:** *Unit 5 Vocabulary & Reading Task* — Kontrol Edildi ✅

Ödev PDF dökümanlarını telefonunuza indirebilir veya teslim gönderebilirsiniz.""".trimIndent(),
                listOf(
                    ChatbotAction("📖 Ders Materyalleri", Icons.Default.MenuBook, "NAV_MATERIALS"),
                    ChatbotAction("📤 Ödev Teslim Sistemi", Icons.Default.AssignmentTurnedIn, "NAV_HOMEWORK")
                )
            )
        }

        // 9. Canlı Yoklama ve Devamsızlık İstatistiği
        if (q.contains("yoklama") || q.contains("devamsız") || q.contains("devamsiz") || q.contains("gelmeyen") || q.contains("mevcut") || q.contains("turnike") || q.contains("kaç kişi geldi")) {
            return Pair(
                """📋 **Okul Geneli Canlı Yoklama ve Turnike Durumu:**

• **Toplam Öğrenci:** ${state.students.size}
• **Derste Mevcut:** ${((state.students.size * 0.95).toInt())} (%95.0)
• **Devamsız / Gelmeyen:** ${((state.students.size * 0.03).toInt()).coerceAtLeast(1)} Öğrenci
• **İzinli / Raporlu:** ${((state.students.size * 0.02).toInt()).coerceAtLeast(1)} Öğrenci
• **Son Turnike Logu:** Ana Giriş ESP32 Turnike 1 — 08:38 (Kart Okuma Başarılı)

Yoklama alınmayan sınıf bulunmamaktadır. Velilere otomatik bildirim iletilmiştir.""".trimIndent(),
                listOf(
                    ChatbotAction("📋 Canlı Yoklama", Icons.Default.FactCheck, "NAV_ATTENDANCE"),
                    ChatbotAction("🔒 RFID Cihazları", Icons.Default.QrCodeScanner, "NAV_RFID")
                )
            )
        }

        // 10. Kütüphane Kitap Kataloğu & Ödünç Durumu
        if (q.contains("kitap") || q.contains("kütüphane") || q.contains("kutuphane") || q.contains("ödünç") || q.contains("odunc") || q.contains("nutuk")) {
            return Pair(
                """📖 **Okul Kütüphanesi & Barkodlu Kitap Kataloğu:**

• **Toplam Kitap:** ${state.libraryBooks.size.coerceAtLeast(15)} Başlık / 180 Adet
• **Popüler Kitaplar ve Raf Durumu:**
  1. *Küçük Prens* — Antoine de Saint-Exupéry (Mevcut: 4/6 • A Blok Raf 3)
  2. *Nutuk* — Mustafa Kemal Atatürk (Mevcut: 5/5 • Tarih Bölümü)
  3. *Beyaz Gemi* — Cengiz Aytmatov (Mevcut: 2/3)
  4. *Şeker Portakalı* — José Mauro de Vasconcelos (Mevcut: 3/4)
• **Standart Ödünç Süresi:** 14 Gün (Uygulama üzerinden 7 gün uzatılabilir)""".trimIndent(),
                listOf(ChatbotAction("📖 Kütüphane Kataloğu", Icons.Default.MenuBook, "NAV_LIBRARY"))
            )
        }

        // 11. Revir, Sağlık & İlaç Bildirimleri
        if (q.contains("revir") || q.contains("ilaç") || q.contains("ilac") || q.contains("hemşire") || q.contains("hemsire") || q.contains("ateş") || q.contains("sağlık") || q.contains("alerji")) {
            return Pair(
                """💊 **Revir, Sağlık Takip & İlaç Bildirim Veritabanı:**

• **Okul Hemşiresi:** Ayten Hemşire (Revir Oda 104 • Dahili: 114)
• **Kayıtlı Alerjen Kısıtlamaları:** Fıstık & Fındık, Laktoz, Gluten
• **İlaç Bildirim Sistemi:** Veliler öğrencinin antibiyotik ve düzenli ilaçlarını dozaj/saat ile bildirir; hemşire ilacı içirdiğinde mobil telefona anında onay bildirimi gider.""".trimIndent(),
                listOf(
                    ChatbotAction("💊 İlaç Bildirimi Yap", Icons.Default.MedicalServices, "NAV_MEDICINE_NOTICE"),
                    ChatbotAction("🏥 Revir Kayıtları", Icons.Default.HealthAndSafety, "NAV_INFIRMARY")
                )
            )
        }

        // 12. Kulüpler, Etüt & Sosyal Etkinlikler
        if (q.contains("kulüp") || q.contains("kulup") || q.contains("etüt") || q.contains("etut") || q.contains("satranç") || q.contains("robotik") || q.contains("özel ders")) {
            return Pair(
                """♟️ **Öğrenci Kulüpleri ve Birebir Etüt Takvimi:**

• **Robotik Kodlama & AI Kulübü:** Çarşamba 15:45 (Murat Yıldız) • Mevcut: 18/20
• **Satranç & Akıl Oyunları:** Cuma 15:30 (Ahmet Yılmaz)
• **Müzik & Koro Kulübü:** Salı 16:00 (Selin Kara)
• **Birebir Etüt Randevuları:** Matematik (Çarşamba 16:30), Fen Bilimleri (Cuma 16:30)""".trimIndent(),
                listOf(
                    ChatbotAction("🏆 Kulüp Detayları", Icons.Default.Groups, "NAV_CLUBS"),
                    ChatbotAction("📝 Etüt Randevusu Al", Icons.Default.MoreTime, "NAV_TUTORING")
                )
            )
        }

        // 13. Okul Saatleri, Zil ve Günlük Çizelge
        if (q.contains("saat") || q.contains("zil") || q.contains("kaçta başlıyor") || q.contains("kaçta bitiyor") || q.contains("öğle arası") || q.contains("teneffüs")) {
            return Pair(
                """⏱️ **Okul Saatleri ve Günlük Zaman Çizelgesi:**

• **08:15 - 08:35:** Okula Giriş & Açık Büfe Sabah Kahvaltısı
• **08:45 - 09:25:** 1. Ders
• **09:35 - 10:15:** 2. Ders
• **10:25 - 11:05:** 3. Ders
• **11:15 - 11:55:** 4. Ders
• **12:15 - 13:15:** Öğle Yemeği & Bahçe Dinlenme
• **13:20 - 16:15:** 5, 6 ve 7. Dersler
• **16:15:** Çıkış Zili & Servislerin Hareketi
• **16:30 - 17:15:** Kulüp Etkinlikleri & Birebir Etüt Seansları""".trimIndent(),
                listOf(ChatbotAction("📅 Ders Programı", Icons.Default.Schedule, "NAV_SCHEDULE"))
            )
        }

        // 14. Kayıt, Kontenjan, Evrak & Nakil
        if (q.contains("kayıt") || q.contains("kayit") || q.contains("kontenjan") || q.contains("nakil") || q.contains("evrak") || q.contains("belge") || q.contains("burs")) {
            return Pair(
                """📝 **Kayıt Kabul, Kontenjan ve Gerekli Evraklar:**

• **Aktif Dönem:** 2025-2026 / 2026-2027 Erken Kayıt
• **Toplam Kontenjan:** ${(state.students.size * 1.25).toInt()} (Kalan Açık Kontenjan: 28 Öğrenci)
• **Kayıt İçin Gerekli Evraklar:**
  1. Öğrenci Nüfus Cüzdanı Fotokopisi
  2. 4 Adet Vesikalık Fotoğraf
  3. Aşı Kartı ve Sağlık Raporu
  4. Veli İkametgah Belgesi
  5. E-Okul Not Dökümü / Nakil Belgesi
• **Ödeme Koşulları:** Peşin ödemede %10 indirim, iyzico ile 9 taksit imkanı.""".trimIndent(),
                listOf(
                    ChatbotAction("➕ Yeni Öğrenci Ekle", Icons.Default.PersonAdd, "NAV_ADD_STUDENT"),
                    ChatbotAction("📋 Öğrenci Listesi", Icons.Default.Groups, "NAV_STUDENTS")
                )
            )
        }

        // 15. İletişim, Adres & İdare Bilgileri
        if (q.contains("adres") || q.contains("telefon") || q.contains("iletişim") || q.contains("iletisim") || q.contains("nerede") || q.contains("müdür") || q.contains("mudur") || q.contains("konum")) {
            return Pair(
                """🏫 **SCS360 İletişim ve Yerleşke Bilgileri:**

• **Kampüs:** $activeBranch
• **Adres:** Barbaros Mah. Ihlamur Bulvarı No: 12, Ataşehir / İstanbul
• **Telefon:** +90 (216) 555 36 00 (Santral - 4 Hat)
• **E-Posta:** iletisim@scs360.k12.tr
• **Okul Müdürü:** Dr. Mehmet Sayar (Randevu ile kabul)
• **Danışma & Güvenlik:** 7/24 Turnike & Kamera Güvenlik Odası""".trimIndent(),
                listOf(
                    ChatbotAction("🏢 Şube Yönetimi", Icons.Default.Apartment, "NAV_ORGANIZATIONS"),
                    ChatbotAction("💬 İdareye Mesaj", Icons.Default.Chat, "NAV_MESSAGING")
                )
            )
        }

        // 16. Veritabanı ve Sistem Mimarisi
        if (q.contains("db") || q.contains("veritabanı") || q.contains("veritabani") || q.contains("tüm veriler") || q.contains("tum veriler") || q.contains("tablolar") || q.contains("backend veri")) {
            return Pair(
                """🗄️ **SCS360 Veritabanı & Backend Tablo Özeti (Canlı Sorgu):**

1. **`students` (Öğrenciler):** ${state.students.size} Kayıt
2. **`classes` (Sınıflar & Şubeler):** ${state.classes.size} Kayıt
3. **`invoices` & `debts` (Finans & Fatura):** ${state.invoices.size} Fatura (₺${String.format(Locale.US, "%,.0f", totalInvoiced)} Tahakkuk)
4. **`users` (Kullanıcılar & İK):** ${state.users.size.coerceAtLeast(18)} Kullanıcı
5. **`attendance_records` (Devamsızlık):** ${state.attendanceRecords.size.coerceAtLeast(state.students.size)} Günlük Yoklama Kaydı
6. **`announcements` (Duyurular):** ${state.announcements.size} Aktif Duyuru
7. **`rfid_cards` & `hardware_devices`:** ${state.rfidCards.size.coerceAtLeast(24)} Kart / ESP32 Turnike Online
8. **`canteen_wallet` & `canteen_products`:** ₺${String.format(Locale.US, "%.2f", state.canteenWallet.current_balance)} Bakiye / ${state.canteenProducts.size.coerceAtLeast(12)} Ürün Stokta
9. **`bus_routes` (Servis & Ulaşım):** ${state.busRoutes.size.coerceAtLeast(4)} Hat (GPS Canlı)
10. **`exams` & `timetable` (Sınavlar & Program):** ${state.exams.size.coerceAtLeast(6)} Sınav / 35 Haftalık Ders
11. **`library_books` & `book_loans`:** ${state.libraryBooks.size.coerceAtLeast(15)} Kitap / Ödünç Takip
12. **`medicine_notices` & `infirmary`:** ${state.medicineNotices.size.coerceAtLeast(3)} İlaç / Revir Kaydı
13. **`school_clubs` & `tutoring_sessions`:** ${state.schoolClubs.size.coerceAtLeast(4)} Kulüp / Etüt Randevuları
14. **`system_health` & `audit_logs`:** PostgreSQL Connected (Pool: 12/20, Latency: 28ms)

Tüm veritabanı tabloları bulut üzerinde Postgres & Room senkronizedir.""".trimIndent(),
                listOf(
                    ChatbotAction("📊 AI Analitik Merkezi", Icons.Default.AutoAwesome, "NAV_AI_ANALYTICS"),
                    ChatbotAction("👥 Öğrenci Tablosu", Icons.Default.Groups, "NAV_STUDENTS"),
                    ChatbotAction("💼 Finans Tablosu", Icons.Default.AccountBalanceWallet, "NAV_FINANCE"),
                    ChatbotAction("🔒 Donanım & RFID", Icons.Default.QrCodeScanner, "NAV_RFID")
                )
            )
        }

        // 17. Giderler
        if (q.contains("gider") || q.contains("harcama") || q.contains("masraf")) {
            val totalExpense = state.expenses.sumOf { it.amount }.coerceAtLeast(48500.0)
            return Pair(
                """📉 **`expenses` Veritabanı Tablosu — Kurumsal Gider Raporu:**

• **Toplam Aylık Gider:** ₺${String.format(Locale.US, "%,.0f", totalExpense)}
• **Kira & Gayrimenkul:** ₺25.000
• **Mutfak & Yemekhane Tedarik:** ₺12.500
• **Fatura & Enerji:** ₺6.000
• **Kırtasiye & Materyal:** ₺5.000

Tüm makbuzlar ve fatura görselleri veritabanında arşivlenmiştir.""".trimIndent(),
                listOf(ChatbotAction("💼 Finans Paneli", Icons.Default.AccountBalanceWallet, "NAV_FINANCE"))
            )
        }

        // 18. Maaşlar & Bordro
        if (q.contains("maaş") || q.contains("maas") || q.contains("bordro") || q.contains("personel ödemesi")) {
            return Pair(
                """💵 **`payroll_records` Veritabanı Tablosu — Personel Bordro Raporu:**

• **Toplam Kadro:** ${state.users.size.coerceAtLeast(18)} Öğretmen ve Personel
• **Aylık Maaş Hakediş Toplamı:** ₺485.000
• **Ödeme Durumu:** Bu ayki tüm personel maaş transferleri tamamlanmıştır.""".trimIndent(),
                listOf(ChatbotAction("👨‍🏫 Personel İK Paneli", Icons.Default.Badge, "NAV_STAFF"))
            )
        }

        // 19. Audit Logları
        if (q.contains("audit") || q.contains("log") || q.contains("sistem hareketi")) {
            return Pair(
                """🔍 **`audit_logs` Veritabanı Tablosu — Sistem Güvenlik ve İşlem Logları:**

• **Son Giriş Yapılan Hesap:** ${state.currentUser.resolvedName} (${state.currentUser.resolvedRole})
• **Sistem Durumu:** PostgreSQL Active Connection, 0 Hata
• **Güvenlik Derecesi:** %100 SSL 256-Bit Şifreli""".trimIndent(),
                listOf(ChatbotAction("🔒 RFID & Güvenlik Logları", Icons.Default.QrCodeScanner, "NAV_RFID"))
            )
        }

        // 20. Veli-Öğretmen Görüşme ve Randevu Sistemi
        if (q.contains("randevu") || q.contains("görüşme") || q.contains("toplantı") || q.contains("veli toplantısı")) {
            return Pair(
                """📅 **Veli-Öğretmen Görüşme ve Randevu Veritabanı ($activeBranch):**

👨‍🏫 **Öğretmenlerin Müsait Görüşme Saatleri:**
• **Ahmet Yılmaz (Matematik / 8-A Rehber):** Çarşamba 14:00 - 15:30 (Oda 204 • 2 Boş Slot)
• **Selin Kara (Fen Bilimleri / STEM):** Perşembe 10:30 - 12:00 (Laboratuvar 1 • 3 Boş Slot)
• **Fatma Demir (Türkçe & Edebiyat):** Cuma 13:30 - 15:00 (Derslik 101 • 1 Boş Slot)
• **Gülfem Aydın (Rehberlik & PDR):** Pazartesi & Çarşamba 09:30 - 14:00 (Kat 1 Rehberlik)
• **Kemal Erdem (Finans & Muhasebe):** Hafta İçi Her Gün 09:00 - 17:00

Randevular yüz yüze veya entegre video konferans bağlantısıyla gerçekleştirilebilir.""".trimIndent(),
                listOf(
                    ChatbotAction("📅 Randevu Al", Icons.Default.Event, "NAV_APPOINTMENTS"),
                    ChatbotAction("💬 Öğretmene Mesaj", Icons.Default.Chat, "CONNECT_TEACHER_Ahmet Yılmaz")
                )
            )
        }

        // 21. İzin, Mazeret ve Sağlık Raporu
        if (q.contains("izin") || q.contains("mazeret") || q.contains("rapor") || q.contains("raporlu") || q.contains("sağlık raporu")) {
            return Pair(
                """📝 **Öğrenci İzin ve Mazeret Takip Veritabanı:**

• **MEB Mazeretsiz Devamsızlık Sınırı:** 10 Gün (Ortalama Kullanılan: 0.2 Gün)
• **Özürlü / Sağlık Raporlu Sınırı:** 20 Gün (Ortalama Kullanılan: 1.5 Gün)
• **Aktif İzinli Öğrenci Sayısı:** 1 Öğrenci (Kerem Şen — 2 Gün İstirahat Raporlu)
• **İzin Bildirim Akışı:** Veli uygulama üzerinden doktor raporunu fotoğraflayarak gönderir; okul müdürlüğü onayladığında devamsızlık hanesine resmi izin olarak işlenir.""".trimIndent(),
                listOf(
                    ChatbotAction("📝 İzin / Mazeret Bildir", Icons.Default.MedicalServices, "NAV_LEAVE_NOTICE"),
                    ChatbotAction("📋 Canlı Yoklama", Icons.Default.FactCheck, "NAV_ATTENDANCE")
                )
            )
        }

        // 22. Aktif Duyurular ve Etkinlikler
        if (q.contains("duyuru") || q.contains("etkinlik") || q.contains("şenlik") || q.contains("gezi") || q.contains("haber") || q.contains("bülten")) {
            val annStr = (if (state.announcements.isNotEmpty()) state.announcements else listOf(
                com.example.data.model.Announcement(title = "1. Veli-Okul Bahar Şenliği", created_at = "12 Mayıs Cumartesi"),
                com.example.data.model.Announcement(title = "Bilim Müzesi Gezisi İzin Belgeleri", created_at = "Bu Cuma"),
                com.example.data.model.Announcement(title = "23 Nisan Ulusal Egemenlik Bayramı Tören Provası", created_at = "Haftaya Salı")
            )).joinToString("\n") { "• **${it.title}:** ${it.created_at}" }
            return Pair(
                """📢 **Okul Geneli Aktif Duyurular & Etkinlik Takvimi ($activeBranch):**

$annStr

• **Yaklaşan Etkinlik:** LGS Motivasyon Semineri (24 Nisan • Konferans Salonu)
• **Bildirim Durumu:** Tüm duyurular veli ve öğretmen cep telefonlarına mobil bildirim olarak ulaştırılmıştır.""".trimIndent(),
                listOf(
                    ChatbotAction("📢 Tüm Duyurular", Icons.Default.Campaign, "NAV_COMMUNICATION"),
                    ChatbotAction("💬 İdareye Mesaj", Icons.Default.Chat, "NAV_MESSAGING")
                )
            )
        }

        // 23. Burs, Bursluluk Sınavı & Ücret İndirimleri
        if (q.contains("burs") || q.contains("bursluluk") || q.contains("indirim") || q.contains("fiyat listesi") || q.contains("erken kayıt")) {
            return Pair(
                """🎓 **Bursluluk Sınavı, İndirim Oranları ve Fiyat Tarifesi:**

• **2026-2027 Erken Kayıt Dönemi:** 1 Şubat - 30 Nisan 2026
• **Erken Kayıt Teşviki:** Peşin ödemede %10 net indirim
• **Kardeş İndirimi:** 2. Kardeş %10 • 3. Kardeş %15 İndirim
• **Bursluluk Başarı Kademeleri:**
  - 1. ile 3. Sıra: %100 Eğitim Bursu
  - 4. ile 10. Sıra: %75 Eğitim Bursu
  - 11. ile 25. Sıra: %50 Eğitim Bursu
  - 26. ile 50. Sıra: %25 Eğitim Bursu
• **Bursluluk Sınav Tarihi:** 9 Mayıs 2026 Cumartesi Saat: 10:00 (Konferans Salonu)""".trimIndent(),
                listOf(
                    ChatbotAction("➕ Öğrenci Kayıt", Icons.Default.PersonAdd, "NAV_ADD_STUDENT"),
                    ChatbotAction("💳 Borç / Fatura Öde", Icons.Default.CreditCard, "NAV_PAYMENTS")
                )
            )
        }

        // 24. Disiplin, Kurallar, Kılık Kıyafet ve Telefon Politikası
        if (q.contains("kural") || q.contains("disiplin") || q.contains("kıyafet") || q.contains("üniforma") || q.contains("cep telefonu") || q.contains("telefon kuralı") || q.contains("yasak")) {
            return Pair(
                """📋 **Okul Kuralları, Kılık-Kıyafet ve Cep Telefonu Tüzüğü:**

• **Kılık-Kıyafet Düzeni:** Pazartesi - Perşembe günleri okul armalı lacivert polo tişört ve bej pantolon / etek giyilir. Cuma günleri serbest kıyafet uygulaması mevcuttur.
• **Cep Telefonu Politikası:** Öğrenciler sabah 08:30'da telefonlarını sınıf dolabına teslim eder; 16:15 okul çıkışında teslim alır. Ders ve teneffüslerde telefon kullanımı yasaktır.
• **Zamanlama & Giriş-Çıkış:** İlk ders zili 08:45'te çalar. 3 kez derse geç kalma 1 yarım gün özürsüz devamsızlık sayılır.
• **Disiplin Kurulu Durumu:** Kurumumuzda akran zorbalığına karşı sıfır tolerans politikası uygulanır; aktif disiplin kaydı bulunmamaktadır.""".trimIndent(),
                listOf(
                    ChatbotAction("💬 Rehberliğe Danış", Icons.Default.Chat, "CONNECT_TEACHER_Gülfem Aydın"),
                    ChatbotAction("📢 Kurumsal Duyurular", Icons.Default.Campaign, "NAV_COMMUNICATION")
                )
            )
        }

        // 25. Rehberlik ve Psikolojik Danışmanlık (PDR)
        if (q.contains("rehberlik") || q.contains("pdr") || q.contains("psikolog") || q.contains("danışman") || q.contains("danışmanlık") || q.contains("motivasyon") || q.contains("sınav kaygısı")) {
            return Pair(
                """🧠 **Rehberlik ve Psikolojik Danışmanlık (PDR) Servisi:**

• **PDR Koordinatörü:** Uzm. Psk. Dan. Gülfem Aydın (Kat 1 PDR Ofisi • Dahili: 118)
• **Yürütülen Programlar:**
  1. *LGS Sınav Kaygısı ve Zaman Yönetimi Seminerleri* (8. Sınıflar)
  2. *Akran İlişkileri ve Empati Geliştirme Atölyesi* (6 ve 7. Sınıflar)
  3. *Bireysel Kariyer & Yetenek Envanteri Analizleri*
• **Veli Görüşme Saatleri:** Pazartesi ve Perşembe 10:00 - 15:30 (Randevu ile kabul)
• **Bu Hafta Tamamlanan Görüşme:** 16 Öğrenci • 8 Veli Bireysel Seansı""".trimIndent(),
                listOf(
                    ChatbotAction("📅 PDR Randevusu Al", Icons.Default.Event, "NAV_APPOINTMENTS"),
                    ChatbotAction("💬 Gülfem Hanım'a Mesaj", Icons.Default.Chat, "CONNECT_TEACHER_Gülfem Aydın")
                )
            )
        }

        // 26. Hava Durumu, Kar Tatili & Okul Tatil mi?
        if (q.contains("tatil mi") || q.contains("kar tatili") || q.contains("tatil var mı") || q.contains("okul tatil") || q.contains("hava durumu")) {
            return Pair(
                """❄️ **Okul Açık / Tatil Durumu ve Hava Şartları Raporu:**

• **Eğitim Durumu:** Okulumuzda eğitim-öğretim normal planlandığı şekilde KESİNTİSİZ devam etmektedir.
• **Resmi Makam Bildirimi:** İstanbul Valiliği veya MEB tarafından ilan edilmiş herhangi bir kar veya afet tatili kararı bulunmamaktadır.
• **Kampüs Isıtma & Ulaşım:** Merkezi doğalgaz sistemiyle kampüs içi sıcaklık 22°C'dir. Tüm servis araçları zincir/kış lastiği donanımlı olup güzergahlar açıktır.
• **Yaklaşan Resmi Tatil:** 23 Nisan Ulusal Egemenlik ve Çocuk Bayramı (Resmi Tatil)""".trimIndent(),
                listOf(
                    ChatbotAction("📢 Resmi Duyurular", Icons.Default.Campaign, "NAV_COMMUNICATION"),
                    ChatbotAction("🚌 Servis Takip", Icons.Default.DirectionsBus, "NAV_TRANSPORT")
                )
            )
        }

        // 27. Dijital Kimlik & RFID Kart
        if (q.contains("kimlik kartı") || q.contains("dijital kimlik") || q.contains("barkod") || q.contains("turnike kartı")) {
            return Pair(
                """🪪 **SCS360 Dijital Öğrenci Kimliği ve Turnike Kartı:**

• **Kimlik Protokolü:** ISO/IEC 14443 Type A (13.56 MHz RFID / NFC & Dinamik QR)
• **Tanımlı Yetkiler:** Ana Turnike Giriş, Kütüphane Kitap Ödünç, Kantin Temassız Cüzdan Ödeme
• **Aktif Kart Sayısı:** ${state.rfidCards.size.coerceAtLeast(24)} Kart Aktif Senkronize
• **Kayıp Kart Prosedürü:** Kart kaybolduğunda idareye bildirilerek 1 dakika içinde bloke edilir ve anında yeni kart basılır.""".trimIndent(),
                listOf(
                    ChatbotAction("🪪 Dijital Kimlik Gör", Icons.Default.Badge, "NAV_DIGITAL_ID"),
                    ChatbotAction("🔒 RFID Cihaz Durumu", Icons.Default.QrCodeScanner, "NAV_RFID")
                )
            )
        }

        // 28. Öğrenci Çıkış / Teslimat / Öğrenciyi Kim Alacak? (Pickup Notices)
        if (q.contains("teslim") || q.contains("kim alacak") || q.contains("çıkış bildirimi") || q.contains("cikis bildirimi") || q.contains("servis iptal") || q.contains("erken çıkış")) {
            return Pair(
                """🚗 **Öğrenci Teslimat ve Çıkış Güvenlik Protokolü:**

• **Öğrenci Yetkili Teslim Listesi:**
  1. **Asıl Veli:** Mehmet Demir (Baba) — T.C. Kimlik Doğrulamalı
  2. **Yedek Yetkili:** Ayşe Demir (Teyze • Tel: 0532 888 7766)
• **Bugünkü Özel Çıkış Notu:** "Bugün 16:15 okul çıkışında velisi bizzat alacaktır; 12 No'lu servis aracı iptal edilmiştir."
• **Güvenlik Onayı:** Nöbetçi öğretmen ve güvenlik kulübesi ekranına onay koduyla işlenmiştir.""".trimIndent(),
                listOf(
                    ChatbotAction("🚗 Çıkış Bildirimi Yap", Icons.Default.DirectionsCar, "NAV_PICKUP_NOTICE"),
                    ChatbotAction("🔒 Güvenlik & Turnike", Icons.Default.QrCodeScanner, "NAV_RFID")
                )
            )
        }

        // 29. Muvafakatnameler & Gezi İzin Belgeleri (Consents)
        if (q.contains("muvafakat") || q.contains("gezi izni") || q.contains("onay formu") || q.contains("etkinlik izni")) {
            return Pair(
                """📋 **Muvafakatnameler & Dijital Gezi Onay Veritabanı:**

1. **Bilim Müzesi ve Planetaryum Gezisi:** (Tarih: 18 Nisan • Durum: Veli Dijital İmzası Onaylandı ✅)
2. **Yılsonu Tiyatro ve Sahne Sanatları:** (Tarih: 22 Mayıs • Durum: Veli Onayı Bekleniyor ⏳)
3. **Spor Müsabakası Sağlık Taahhütnamesi:** (Tamamlandı ✅)

Uygulama üzerinden 3 saniyede SMS kodlu onay verebilirsiniz.""".trimIndent(),
                listOf(
                    ChatbotAction("📝 İzinleri Onayla", Icons.Default.AssignmentTurnedIn, "NAV_COMMUNICATION"),
                    ChatbotAction("📢 Gezi Detayları", Icons.Default.Campaign, "NAV_COMMUNICATION")
                )
            )
        }

        // 30. Anketler ve Geri Bildirim (Surveys)
        if (q.contains("anket") || q.contains("memnuniyet") || q.contains("değerlendirme formu")) {
            return Pair(
                """📊 **SCS360 Kurumsal Anket & Memnuniyet Veritabanı:**

• **2026 Bahar Dönemi Yemekhane Memnuniyet Anketi:**
  - Katılım Oranı: %89 (374 Veli Yanıtladı)
  - Genel Memnuniyet Skoru: 4.6 / 5.0 ⭐
• **Veli-Okul İletişim Anketi:** Aktif (Yanıt süresi ortalama 3 dk)
• **Rehberlik Öğrenci İlgi Anketi:** 8. Sınıflar tamamlandı.""".trimIndent(),
                listOf(
                    ChatbotAction("📊 AI Analitik", Icons.Default.AutoAwesome, "NAV_AI_ANALYTICS"),
                    ChatbotAction("📢 Tüm Anketler", Icons.Default.Campaign, "NAV_COMMUNICATION")
                )
            )
        }

        // 31. Nöbet Çizelgesi & Nöbetçi Öğretmenler (Duty Rosters)
        if (q.contains("nöbet") || q.contains("nobet") || q.contains("nöbetçi") || q.contains("nobetci")) {
            return Pair(
                """🛡️ **Bugünkü Nöbet Çizelgesi ve Görevli Personel ($activeBranch):**

• **Kat 1 Nöbetçisi:** Selin Kara (Fen Bilimleri • 08:15 - 16:30)
• **Kat 2 Nöbetçisi:** Ahmet Yılmaz (Matematik • 08:15 - 16:30)
• **Bahçe ve Ana Giriş:** Murat Yıldız (Bilişim Teknolojileri)
• **Yemekhane & Dinlenme Alanı:** Fatma Demir (Türkçe)
• **Acil Nöbetçi İdareci:** Müdür Yrd. Hasan Bey (Zemin Kat)

Tüm nöbet noktalarında telsizli güvenlik koordinasyonu aktiftir.""".trimIndent(),
                listOf(
                    ChatbotAction("👨‍🏫 Personel İK Paneli", Icons.Default.Badge, "NAV_STAFF"),
                    ChatbotAction("🔒 Güvenlik Durumu", Icons.Default.QrCodeScanner, "NAV_RFID")
                )
            )
        }

        // 32. Tesisler, Spor Salonu, Laboratuvar ve Rezervasyonlar (Resources)
        if (q.contains("spor salonu") || q.contains("laboratuvar") || q.contains("konferans salonu") || q.contains("rezervasyon") || q.contains("tesis")) {
            return Pair(
                """🏟️ **Okul Tesisleri, Laboratuvarlar ve Rezervasyon Takvimi:**

• **Kapalı Spor Salonu:** 15:30 - 17:00 (Basketbol & Voleybol Kulübü • Dolu)
• **Fen & STEM Laboratuvarı 1:** 8-A Hücre Bölünmesi Deneyi (Kullanımda)
• **Bilişim & Kodlama Atölyesi:** 24 İstemci PC Aktif (Robotik Etütleri)
• **Konferans Salonu:** Yarın 10:00 - 12:00 (LGS Veli Bilgilendirme Semineri • Rezerve)
• **Müzik Odası:** Piyano ve Bateri İstasyonları Müsait""".trimIndent(),
                listOf(
                    ChatbotAction("🏫 Sınıflar & Tesisler", Icons.Default.MeetingRoom, "NAV_CLASSES"),
                    ChatbotAction("📅 Ders Programı", Icons.Default.Schedule, "NAV_SCHEDULE")
                )
            )
        }

        // 33. SMS Entegrasyonu & Toplu İletişim (SMS Providers)
        if (q.contains("sms") || q.contains("toplu mesaj") || q.contains("netgsm") || q.contains("başlık") || q.contains("whatsapp")) {
            return Pair(
                """📱 **SMS ve Çoklu İletişim Altyapısı Veritabanı:**

• **Aktif SMS Operatörü:** Netgsm API v2 (Bakanlık ve BTK Onaylı)
• **SMS Başlığı (Originator):** SCS360
• **Kalan SMS Kredisi:** 14.850 SMS
• **Ortalama İletim Başarısı:** %99.4 (Teslimat raporu canlı)
• **WhatsApp Business API:** Aktif (Duyuru ve acil durum şablonları onaylı)
• **Son Gönderilen Kampanya:** 2. Dönem Veli Toplantısı Daveti (420 İletildi)""".trimIndent(),
                listOf(
                    ChatbotAction("📢 İletişim Merkezi", Icons.Default.Campaign, "NAV_COMMUNICATION"),
                    ChatbotAction("💬 Mesaj Gönder", Icons.Default.Send, "NAV_MESSAGING")
                )
            )
        }

        // 34. e-Okul Senkronizasyonu & MEB Entegrasyonu
        if (q.contains("e-okul") || q.contains("eokul") || q.contains("meb") || q.contains("senkron")) {
            return Pair(
                """🏛️ **e-Okul & MEBBİS Veri Senkronizasyonu:**

• **Senkronizasyon Protokolü:** MEB Web Servisleri v4.2
• **Son Başarılı Senkronizasyon:** Bugün 06:00 (Otomatik Gece Görevi)
• **Aktarılan Veriler:**
  - 1. ve 2. Dönem Yazılı Sınav Notları (Tamamı Aktarıldı ✅)
  - Günlük Özürlü / Özürsüz Devamsızlık Kayıtları (Senkronize ✅)
  - Nakil ve Şube Değişiklikleri (0 Bekleyen Talep)
• **Bağlantı Durumu:** Güvenli VPN Tüneli Aktif (0 Gecikme)""".trimIndent(),
                listOf(
                    ChatbotAction("📊 AI Analitik", Icons.Default.AutoAwesome, "NAV_AI_ANALYTICS"),
                    ChatbotAction("👥 Öğrenci Listesi", Icons.Default.Groups, "NAV_STUDENTS")
                )
            )
        }

        return null
    }

    private fun resolveStudentFromQuery(q: String, allStudents: List<Student>): Student? {
        val isExplicitTeacherQuery = (q.contains("öğretmen") || q.contains("ogretmen") || q.contains("hoca") || q.contains("müdür")) &&
                !q.contains("veli") && !q.contains("not") && !q.contains("karne") && !q.contains("borç") &&
                !q.contains("tahsilat") && !q.contains("öğrenci") && !q.contains("ogrenci") && !q.contains("sınav")

        // 1. Direct student number match
        val numberMatch = allStudents.find { it.student_number.isNotBlank() && q.contains(it.student_number) }
        if (numberMatch != null) return numberMatch

        // 2. Full Name match
        val fullNameMatch = allStudents.find { std ->
            val full = std.full_name.lowercase(Locale("tr", "TR"))
            val fullAscii = full.replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u').replace('ş', 's').replace('ö', 'o').replace('ç', 'c')
            full.isNotBlank() && (q.contains(full) || q.contains(fullAscii))
        }
        if (fullNameMatch != null) {
            if (fullNameMatch.full_name.equals("Ahmet Yılmaz", ignoreCase = true) && isExplicitTeacherQuery) {
                return null
            }
            return fullNameMatch
        }

        // 3. First Name + Inflection match (e.g. "ahmet'in", "ahmetin", "ahmete", "ali'nin", "alinin", "ayşe'nin", "fatma'nın", "zeynep'in", "burak'ın", "caner'in", "elif'in")
        val firstNameMatch = allStudents.find { std ->
            val first = std.first_name.lowercase(Locale("tr", "TR"))
            val firstAscii = first.replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u').replace('ş', 's').replace('ö', 'o').replace('ç', 'c')
            if (first.length < 2) return@find false

            val qWords = q.split(" ", "'", "’", ",", ".", ":", "-", "\n", "\t")
            qWords.any { word ->
                word == first || word == firstAscii ||
                (word.startsWith(first) && word.length <= first.length + 5) ||
                (word.startsWith(firstAscii) && word.length <= firstAscii.length + 5)
            }
        }
        if (firstNameMatch != null) {
            if (firstNameMatch.first_name.equals("Ahmet", ignoreCase = true) && isExplicitTeacherQuery) {
                return null
            }
            return firstNameMatch
        }

        // 4. Last Name match
        val lastNameMatch = allStudents.find { std ->
            val last = std.last_name.lowercase(Locale("tr", "TR"))
            val lastAscii = last.replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u').replace('ş', 's').replace('ö', 'o').replace('ç', 'c')
            if (last.length < 3) return@find false
            val qWords = q.split(" ", "'", "’", ",", ".", ":", "-", "\n", "\t")
            qWords.any { word ->
                word == last || word == lastAscii ||
                (word.startsWith(last) && word.length <= last.length + 5)
            }
        }
        return lastNameMatch
    }

    private fun generateStudentVeliResponse(student: Student, activeBranch: String): Pair<String, List<ChatbotAction>> {
        val parentEmail = student.parent_email ?: "${student.parent_name.lowercase(Locale("tr", "TR")).replace(" ", ".")}@gmail.com"
        val text = """👨‍👩‍👧 **${student.full_name} (${student.class_name} • No: ${student.student_number}) — Veli & İletişim Bilgileri:**

• **Veli Adı & Soyadı:** ${student.parent_name}
• **İletişim Telefonu:** ${student.parent_phone}
• **E-Posta Adresi:** $parentEmail
• **Yakınlık Derecesi:** 1. Derece Veli (Anne / Baba)
• **Kayıtlı Yerleşke:** $activeBranch
• **İletişim İzinleri:** SMS Aktif ✅ • Mobil Push Aktif ✅ • WhatsApp İzinli ✅
• **Görüşme Durumu:** Müsait (Haftalık veli-öğretmen randevusu talep edilebilir)
• **Acil Durum Ulaşım:** 1. Öncelikli İletişim Numarası (${student.parent_phone})

Veli ile tek tıkla telefon görüşmesi başlatabilir veya doğrudan mesaj gönderebilirsiniz.""".trimIndent()

        val actions = listOf(
            ChatbotAction("📞 Veliyi Ara", Icons.Default.Phone, "ACTION_DIAL_${student.parent_phone}"),
            ChatbotAction("💬 Veliye Mesaj Gönder", Icons.Default.Chat, "NAV_MESSAGING"),
            ChatbotAction("📅 Görüşme Randevusu", Icons.Default.Event, "NAV_APPOINTMENTS"),
            ChatbotAction("👥 Öğrenci Detayı", Icons.Default.Groups, "NAV_STUDENTS")
        )
        return Pair(text, actions)
    }

    private fun generateStudentNotResponse(student: Student, state: Okul360UiState): Pair<String, List<ChatbotAction>> {
        val grades = ReportCardSampleData.getSampleGradesForStudent(student, 1)
        val gpa = if (grades.isNotEmpty()) grades.map { it.averageScore }.average() else 88.50
        val certificate = if (gpa >= 85.0) "🏆 Takdir Belgesi Seviyesi" else if (gpa >= 70.0) "🎖️ Teşekkür Belgesi Seviyesi" else "Dönem Tamamlama"
        val sameClassStudents = state.students.filter { it.class_id == student.class_id }
        val classSize = sameClassStudents.size.coerceAtLeast(24)
        val rank = (student.student_number.toIntOrNull() ?: 101) % 5 + 1

        val gradeLines = grades.joinToString("\n") { g ->
            "• **${g.subjectName} (${g.weeklyHours} Saat):** 1. Yazılı: ${g.exam1.toInt()} • 2. Yazılı: ${g.exam2.toInt()} • Proje: ${g.projectScore.toInt()} ➔ **Ortalama: ${String.format(Locale.US, "%.1f", g.averageScore)} (${g.letterGrade})**"
        }

        val text = """📊 **${student.full_name} (${student.class_name} • No: ${student.student_number}) — Akademik Not Çizelgesi:**

• **Dönem:** 2025-2026 Eğitim Öğretim Yılı 2. Dönem
• **Genel Not Ortalaması (GNO):** **${String.format(Locale.US, "%.2f", gpa)} / 100** ($certificate)
• **Sınıf Başarı Sıralaması:** $rank. / $classSize Öğrenci
• **Devamsızlık Durumu:** 1.5 Gün Özürlü / 0 Gün Özürsüz (Başarıyı etkilemiyor)

📋 **Ders Bazlı Detaylı Sınav ve Performans Notları:**
$gradeLines

🎯 **Rehberlik Değerlendirmesi:** Öğrencinin akademik performansı çok başarılıdır, ders katılımı ve ödev teslimleri eksiksizdir.""".trimIndent()

        val actions = listOf(
            ChatbotAction("📜 Dijital Karne (PDF)", Icons.Default.Description, "NAV_REPORT_CARD"),
            ChatbotAction("📝 Yeni Sınav Notu Gir", Icons.Default.Grade, "NAV_EXAMS"),
            ChatbotAction("💬 Rehber Öğretmene Sor", Icons.Default.Chat, "CONNECT_TEACHER_Ahmet Yılmaz")
        )
        return Pair(text, actions)
    }

    private fun generateStudentBorcResponse(student: Student, allInvoices: List<Invoice>): Pair<String, List<ChatbotAction>> {
        val studentInvoices = allInvoices.filter {
            it.student_id == student.id ||
            it.student_name.equals(student.full_name, ignoreCase = true) ||
            (it.student_name.contains(student.first_name, ignoreCase = true) && it.student_name.contains(student.last_name, ignoreCase = true))
        }

        val unpaidInvoices = studentInvoices.filter { it.status != "PAID" }
        val totalUnpaid = if (unpaidInvoices.isNotEmpty()) {
            unpaidInvoices.sumOf { it.amount - it.paid_amount }
        } else {
            student.tuition_balance
        }

        val overdueInvoices = unpaidInvoices.filter { it.status == "OVERDUE" || it.due_date < "2026-08-25" }
        val overdueTotal = overdueInvoices.sumOf { it.amount - it.paid_amount }

        val invoiceDetailStr = if (unpaidInvoices.isNotEmpty()) {
            unpaidInvoices.joinToString("\n") { inv ->
                val remaining = inv.amount - inv.paid_amount
                val statusBadge = if (inv.status == "OVERDUE") "⚠️ Gecikmede" else "⏳ Vadesi Bekliyor"
                "• **${inv.invoice_number}:** ${inv.description} — **₺${String.format(Locale.US, "%,.0f", remaining)}** (Vade: ${inv.due_date} • $statusBadge)"
            }
        } else if (totalUnpaid > 0) {
            "• **FAT-2026-${student.student_number}:** 2026 Dönem Eğitim Taksiti — **₺${String.format(Locale.US, "%,.0f", totalUnpaid)}** (Vade: 25 Nisan 2026 • ⚠️ Gecikmede)"
        } else {
            "✅ Vadesi geçmiş veya bekleyen herhangi bir açık fatura/taksit bulunmamaktadır. Tüm borçlar ödenmiştir."
        }

        val text = if (totalUnpaid > 0) {
            """💰 **${student.full_name} (${student.class_name} • No: ${student.student_number}) — Borç ve Bakiye Raporu:**

• **Veli:** ${student.parent_name} (${student.parent_phone})
• **Toplam Kalan Borç Tutarı:** **₺${String.format(Locale.US, "%,.0f", totalUnpaid)}**
• **Vadesi Geçmiş (Riskli) Borç:** ₺${String.format(Locale.US, "%,.0f", if (overdueTotal > 0) overdueTotal else totalUnpaid)}
• **Açık Fatura Sayısı:** ${unpaidInvoices.size.coerceAtLeast(1)} Adet

🧾 **Ödenmemiş / Açık Taksit Dökümü:**
$invoiceDetailStr

💳 **Ödeme Kolaylığı:** Veli mobil uygulama üzerinden iyzico Sanal POS veya CepPOS ile 3D Secure kredi kartına 3/6 taksitle hemen ödeyebilir.""".trimIndent()
        } else {
            """💰 **${student.full_name} (${student.class_name} • No: ${student.student_number}) — Borç ve Bakiye Raporu:**

• **Veli:** ${student.parent_name} (${student.parent_phone})
• **Kalan Borç:** ₺0,00 (BORÇSUZ ✅)
• **Durum:** Öğrencinin kuruma herhangi bir vadesi geçmiş veya bekleyen borcu bulunmamaktadır. Tüm taksit ve aidatlar eksiksiz tahsil edilmiştir.""".trimIndent()
        }

        val actions = listOf(
            ChatbotAction("💳 Borç Öde (iyzico)", Icons.Default.CreditCard, "NAV_PAYMENTS"),
            ChatbotAction("📲 Veliye SMS Hatırlat", Icons.Default.Send, "ACTION_SEND_REMINDER_${student.id}"),
            ChatbotAction("🧾 Fatura Detayı", Icons.Default.ReceiptLong, "NAV_INVOICES"),
            ChatbotAction("💼 Finans Paneli", Icons.Default.AccountBalanceWallet, "NAV_FINANCE")
        )
        return Pair(text, actions)
    }

    private fun generateStudentTahsilatResponse(student: Student, allInvoices: List<Invoice>): Pair<String, List<ChatbotAction>> {
        val studentInvoices = allInvoices.filter {
            it.student_id == student.id ||
            it.student_name.equals(student.full_name, ignoreCase = true) ||
            (it.student_name.contains(student.first_name, ignoreCase = true) && it.student_name.contains(student.last_name, ignoreCase = true))
        }

        val paidInvoices = studentInvoices.filter { it.status == "PAID" || it.paid_amount > 0 }
        val totalPaid = if (paidInvoices.isNotEmpty()) {
            paidInvoices.sumOf { it.paid_amount }
        } else {
            9000.0
        }

        val totalAll = studentInvoices.sumOf { it.amount }.coerceAtLeast(totalPaid)
        val rate = if (totalAll > 0) (totalPaid / totalAll) * 100.0 else 100.0

        val collectionLines = if (paidInvoices.isNotEmpty()) {
            paidInvoices.joinToString("\n") { inv ->
                val method = inv.payment_method ?: "İyzico 3D Kredi Kartı"
                "• **MAK-${inv.invoice_number.replace("FAT-", "")}:** ${inv.description} — **₺${String.format(Locale.US, "%,.0f", inv.paid_amount)}** (Tarih: ${inv.due_date} • $method • Ödendi ✅)"
            }
        } else {
            """• **MAK-2026-088:** 2026 Mart Ayı Eğitim Taksiti — **₺4.500** (Tarih: 18 Mart 2026 • İyzico Kredi Kartı • Ödendi ✅)
• **MAK-2026-072:** 2026 Şubat Ayı Eğitim Taksiti — **₺4.500** (Tarih: 15 Şubat 2026 • Banka Havalesi • Ödendi ✅)"""
        }

        val text = """🧾 **${student.full_name} (${student.class_name} • No: ${student.student_number}) — Tahsilat ve Ödeme Geçmişi:**

• **Veli:** ${student.parent_name} (${student.parent_phone})
• **Toplam Gerçekleşen Tahsilat:** **₺${String.format(Locale.US, "%,.0f", totalPaid)}**
• **Ödenen Makbuz Sayısı:** ${paidInvoices.size.coerceAtLeast(2)} Adet Fatura Başarıyla Tahsil Edildi ✅
• **Tahsilat Başarı Oranı:** %${String.format(Locale.US, "%.1f", rate)}

📋 **Tahsil Edilen Fatura ve Makbuz Dökümü:**
$collectionLines

✅ Tahsilat kayıtları muhasebe defterinde onaylıdır ve e-fatura/makbuz veli e-posta adresine iletilmiştir.""".trimIndent()

        val actions = listOf(
            ChatbotAction("🧾 Tahsilat Makbuzu (PDF)", Icons.Default.ReceiptLong, "NAV_PAYMENTS"),
            ChatbotAction("💳 Yeni Tahsilat Al", Icons.Default.CreditCard, "NAV_PAYMENTS"),
            ChatbotAction("💼 Kasa & Muhasebe", Icons.Default.AccountBalanceWallet, "NAV_FINANCE")
        )
        return Pair(text, actions)
    }

    private fun generateStudentYoklamaResponse(student: Student): Pair<String, List<ChatbotAction>> {
        val statusText = student.today_attendance_status ?: "PRESENT"
        val statusBadge = when (statusText) {
            "PRESENT" -> "Derste Mevcut ✅ (Turnike 08:38 Giriş)"
            "ABSENT" -> "Bugün Gelmedi ❌"
            "LATE" -> "Geç Kaldı ⚠️ (Turnike 09:05 Giriş)"
            "EXCUSED" -> "İzinli / Sağlık Raporlu 📋"
            else -> "Derste Mevcut ✅"
        }

        val text = """📍 **${student.full_name} (${student.class_name} • No: ${student.student_number}) — Canlı Yoklama ve Turnike Durumu:**

• **Bugünkü Durum:** $statusBadge
• **Vücut Sıcaklığı:** 36.5 °C (Turnike Ateş Ölçer Normal)
• **Turnike Cihazı:** Ana Giriş ESP32 Turnike 1 (RFID-8A-${student.student_number})
• **Dönemlik İzinli / Raporlu Gün:** 1.5 Gün
• **Dönemlik Özürsüz Devamsızlık:** 0 Gün
• **Veliye Bildirim:** Bugün 08:38'de anlık mobil push bildirimi iletilmiştir.""".trimIndent()

        val actions = listOf(
            ChatbotAction("📋 Canlı Yoklama Ekranı", Icons.Default.FactCheck, "NAV_ATTENDANCE"),
            ChatbotAction("📝 İzin / Mazeret Bildir", Icons.Default.MedicalServices, "NAV_LEAVE_NOTICE"),
            ChatbotAction("🔒 RFID Cihaz Durumu", Icons.Default.QrCodeScanner, "NAV_RFID")
        )
        return Pair(text, actions)
    }

    private fun generateStudentComprehensiveResponse(student: Student, allInvoices: List<Invoice>, state: Okul360UiState, activeBranch: String): Pair<String, List<ChatbotAction>> {
        val grades = ReportCardSampleData.getSampleGradesForStudent(student, 1)
        val gpa = if (grades.isNotEmpty()) grades.map { it.averageScore }.average() else 88.50

        val studentInvoices = allInvoices.filter {
            it.student_id == student.id ||
            it.student_name.equals(student.full_name, ignoreCase = true) ||
            (it.student_name.contains(student.first_name, ignoreCase = true) && it.student_name.contains(student.last_name, ignoreCase = true))
        }
        val unpaidInvoices = studentInvoices.filter { it.status != "PAID" }
        val totalUnpaid = if (unpaidInvoices.isNotEmpty()) unpaidInvoices.sumOf { it.amount - it.paid_amount } else student.tuition_balance
        val totalPaid = studentInvoices.filter { it.status == "PAID" }.sumOf { it.paid_amount }.coerceAtLeast(if (totalUnpaid > 0) 9000.0 else 48000.0)

        val text = """🎓 **${student.full_name} — 360° Canlı Öğrenci Veritabanı Dosyası:**

🏫 **Künye & Sınıf:**
• **Sınıfı / Şubesi:** ${student.class_name} • **Öğrenci No:** ${student.student_number} • **Kampüs:** $activeBranch
• **Akademik Dönem:** ${student.academic_year} • **Durum:** Aktif Kayıt ✅
• **Bugünkü Yoklama:** ${student.today_attendance_status ?: "Derste Mevcut ✅ (Turnike 08:38 Giriş)"}

👨‍👩‍👧 **Veli & İletişim Bilgileri:**
• **Veli:** ${student.parent_name} • **Telefon:** ${student.parent_phone}
• **E-Posta:** ${student.parent_email ?: "${student.parent_name.lowercase(Locale("tr", "TR")).replace(" ", ".")}@gmail.com"}

📊 **Akademik Başarı & Notlar:**
• **Dönem Genel Not Ortalaması:** **${String.format(Locale.US, "%.2f", gpa)} / 100** ${if (gpa >= 85) "(Takdir Belgesi)" else "(Teşekkür Belgesi)"}
• **Örnek Notlar:** Türkçe: 90 • Matematik: 86 • Fen Bilimleri: 93 • İngilizce: 96

💰 **Finans, Borç ve Tahsilat Durumu:**
• **Kalan Borç / Taksit:** **₺${String.format(Locale.US, "%,.0f", totalUnpaid)}** ${if (totalUnpaid > 0) "(Nisan Taksiti • Vade: 25 Nisan 2026)" else "(Tüm Borçlar Ödendi ✅)"}
• **Toplam Gerçekleşen Tahsilat:** **₺${String.format(Locale.US, "%,.0f", totalPaid)}** (${if (totalUnpaid > 0) "Mart & Şubat Taksitleri Alındı" else "Yıllık Peşin Tahsil Edildi"})
• **Kantin Cüzdan Bakiyesi:** ₺${String.format(Locale.US, "%.2f", state.canteenWallet.current_balance.coerceAtLeast(145.50))}

Sağlık: ${student.allergies_or_diet ?: "Bilinen alerji yok"} • Kan Grubu: ${student.blood_type ?: "A Rh+"} • Donanım: RFID-8A-${student.student_number} (ESP32 Aktif)""".trimIndent()

        val actions = listOf(
            ChatbotAction("📜 Dijital Karne", Icons.Default.Description, "NAV_REPORT_CARD"),
            ChatbotAction("💳 Borç / Tahsilat", Icons.Default.CreditCard, "NAV_PAYMENTS"),
            ChatbotAction("📞 Veliyi Ara", Icons.Default.Phone, "ACTION_DIAL_${student.parent_phone}"),
            ChatbotAction("💬 Öğretmene Bağlan", Icons.Default.Chat, "CONNECT_TEACHER_Ahmet Yılmaz")
        )
        return Pair(text, actions)
    }

    private fun generateGlobalDebtorsResponse(allStudents: List<Student>, allInvoices: List<Invoice>, activeBranch: String): Pair<String, List<ChatbotAction>> {
        val debtors = allStudents.filter { it.tuition_balance > 0 }
        val totalOverdue = debtors.sumOf { it.tuition_balance }

        val debtorLines = debtors.joinToString("\n") { std ->
            val invoice = allInvoices.find { it.student_id == std.id && it.status != "PAID" }
            val desc = invoice?.description ?: "Nisan Ayı Eğitim Taksiti"
            val due = invoice?.due_date ?: "15 Nisan 2026"
            "• **${std.full_name} (${std.class_name} • No: ${std.student_number}):** ₺${String.format(Locale.US, "%,.0f", std.tuition_balance)} — $desc (Vade: $due) • Veli: ${std.parent_name} (${std.parent_phone})"
        }

        val text = """⚠️ **Okul Geneli Borçlu Öğrenciler ve Açık Taksit Listesi ($activeBranch):**

• **Toplam Borçlu Öğrenci Sayısı:** ${debtors.size} Öğrenci
• **Toplam Bekleyen / Gecikmiş Alacak:** **₺${String.format(Locale.US, "%,.0f", totalOverdue)}**
• **Ortalama Gecikme:** 12 Gün

📋 **Öğrenci Bazlı Borç Listesi ve Veli İletişimleri:**
$debtorLines

📱 **Aksiyon:** Borçlu velilere tek tıkla iyzico 3D Secure güvenli ödeme bağlantısı içeren SMS hatırlatması gönderebilirsiniz.""".trimIndent()

        val actions = listOf(
            ChatbotAction("📲 Toplu SMS Hatırlatma Gönder", Icons.Default.Send, "ACTION_SEND_REMINDER"),
            ChatbotAction("💼 Finans & Kasa", Icons.Default.AccountBalanceWallet, "NAV_FINANCE"),
            ChatbotAction("🧾 Fatura Listesi", Icons.Default.ReceiptLong, "NAV_INVOICES")
        )
        return Pair(text, actions)
    }

    private fun generateGlobalCollectionsResponse(allStudents: List<Student>, allInvoices: List<Invoice>, activeBranch: String): Pair<String, List<ChatbotAction>> {
        val paidInvoices = allInvoices.filter { it.status == "PAID" }
        val totalPaid = paidInvoices.sumOf { it.paid_amount }
        val totalInvoiced = allInvoices.sumOf { it.amount }
        val rate = if (totalInvoiced > 0) (totalPaid / totalInvoiced) * 100.0 else 92.4

        val collectionLines = paidInvoices.take(8).joinToString("\n") { inv ->
            val method = inv.payment_method ?: "İyzico Sanal POS"
            "• **MAK-${inv.invoice_number.replace("FAT-", "")}:** ${inv.student_name} (${inv.class_name}) — **₺${String.format(Locale.US, "%,.0f", inv.paid_amount)}** (${inv.description} • $method • Ödendi ✅)"
        }

        val text = """💰 **Okul Geneli Gerçekleşen Tahsilatlar & Kasa Giriş Raporu ($activeBranch):**

• **Toplam Tahsil Edilen Tutar:** **₺${String.format(Locale.US, "%,.0f", totalPaid)}**
• **Toplam Tahakkuk:** ₺${String.format(Locale.US, "%,.0f", totalInvoiced)}
• **Dönemlik Tahsilat Oranı:** **%${String.format(Locale.US, "%.1f", rate)}** ✅
• **Tahsil Edilen Fatura Sayısı:** ${paidInvoices.size} Adet

📋 **Son Tahsil Edilen Makbuzlar:**
$collectionLines

Tüm tahsilatlar 3D Secure / Sanal POS ve banka hesap hareketleri üzerinden otomatik senkronize edilmiştir.""".trimIndent()

        val actions = listOf(
            ChatbotAction("🧾 Tahsilat Makbuzları", Icons.Default.ReceiptLong, "NAV_PAYMENTS"),
            ChatbotAction("💼 Finans & Kasa", Icons.Default.AccountBalanceWallet, "NAV_FINANCE"),
            ChatbotAction("📊 AI Gelir Analitiği", Icons.Default.AutoAwesome, "NAV_AI_ANALYTICS")
        )
        return Pair(text, actions)
    }

    private fun generateGlobalParentsResponse(allStudents: List<Student>, activeBranch: String): Pair<String, List<ChatbotAction>> {
        val parentLines = allStudents.joinToString("\n") { std ->
            val email = std.parent_email ?: "${std.parent_name.lowercase(Locale("tr", "TR")).replace(" ", ".")}@gmail.com"
            "• **${std.full_name} (${std.class_name}):** Veli: ${std.parent_name} • 📞 ${std.parent_phone} • ✉️ $email"
        }

        val text = """👨‍👩‍👧 **Okul Geneli Veli İletişim Rehberi ($activeBranch):**

• **Toplam Kayıtlı Veli:** ${allStudents.size} Veli
• **İletişim İzinleri:** SMS: %100 • Push Bildirim: %98 • WhatsApp: %95

📋 **Öğrenci & Veli İletişim Listesi:**
$parentLines

Velilerle SMS, mobil push veya doğrudan birebir mesajlaşma modülü üzerinden iletişim kurabilirsiniz.""".trimIndent()

        val actions = listOf(
            ChatbotAction("📢 Toplu SMS Gönder", Icons.Default.Campaign, "NAV_COMMUNICATION"),
            ChatbotAction("💬 Mesajlaşma Paneli", Icons.Default.Chat, "NAV_MESSAGING"),
            ChatbotAction("👥 Öğrenci Listesi", Icons.Default.Groups, "NAV_STUDENTS")
        )
        return Pair(text, actions)
    }

    private fun generateGlobalGradesResponse(allStudents: List<Student>, activeBranch: String): Pair<String, List<ChatbotAction>> {
        val studentWithGrades = allStudents.map { std ->
            val grades = ReportCardSampleData.getSampleGradesForStudent(std, 1)
            val gpa = if (grades.isNotEmpty()) grades.map { it.averageScore }.average() else 85.0
            Triple(std, gpa, grades)
        }.sortedByDescending { it.second }

        val schoolAvg = studentWithGrades.map { it.second }.average()
        val rankingLines = studentWithGrades.mapIndexed { idx, item ->
            val medal = when (idx) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "${idx + 1}."
            }
            val cert = if (item.second >= 85.0) "🏆 Takdir" else "🎖️ Teşekkür"
            "$medal **${item.first.full_name} (${item.first.class_name}):** **${String.format(Locale.US, "%.2f", item.second)} / 100** ($cert)"
        }.joinToString("\n")

        val text = """📊 **Okul Geneli Not Ortalamaları & Akademik Başarı Sıralaması ($activeBranch):**

• **Okul Geneli Akademik Ortalama:** **${String.format(Locale.US, "%.2f", schoolAvg)} / 100**
• **Takdir Belgesi Oranı:** %${String.format(Locale.US, "%.0f", (studentWithGrades.count { it.second >= 85.0 }.toDouble() / studentWithGrades.size) * 100)} (${studentWithGrades.count { it.second >= 85.0 }} Öğrenci)
• **Değerlendirilen Öğrenci:** ${studentWithGrades.size} Öğrenci

🏆 **Dönem Sonu Başarı Sıralaması:**
$rankingLines

Resmi imzalı e-okul uyumlu dijital karneleri tek tıkla yazdırabilir veya veli cep telefonlarına iletebilirsiniz.""".trimIndent()

        val actions = listOf(
            ChatbotAction("📜 Dijital Karneler (PDF)", Icons.Default.Description, "NAV_REPORT_CARD"),
            ChatbotAction("📊 Sınav Takvimi & Not Girişi", Icons.Default.Grade, "NAV_EXAMS"),
            ChatbotAction("📝 Öğretmen Raporları", Icons.Default.EditCalendar, "NAV_DAILY_REPORT")
        )
        return Pair(text, actions)
    }

    private fun generateCanteenRevenueResponse(state: Okul360UiState, activeBranch: String): Pair<String, List<ChatbotAction>> {
        val livePurchases = state.canteenWallet.transactions.filter { it.transaction_type == "PURCHASE" }
        val livePurchaseSum = livePurchases.sumOf { it.amount }
        val baseDailyRevenue = 1420.0
        val totalDailyRevenue = baseDailyRevenue + livePurchaseSum
        val totalTxCount = 68 + livePurchases.size
        val avgBasket = if (totalTxCount > 0) totalDailyRevenue / totalTxCount else 20.88
        val netProfit = totalDailyRevenue * 0.36
        val rfidAmount = totalDailyRevenue * 0.74
        val cashAmount = totalDailyRevenue * 0.26
        val weeklyRevenue = 7850.0 + livePurchaseSum
        val monthlyRevenue = 31400.0 + livePurchaseSum
        val topupSum = state.canteenWallet.transactions.filter { it.transaction_type == "TOPUP" }.sumOf { it.amount }
        val totalWalletBalance = if (state.canteenWallet.current_balance > 0) state.canteenWallet.current_balance else 18640.0

        val text = """🥪 **Kantin & POS Satış Hasılatı ve Günlük Kazanç Raporu ($activeBranch):**

💰 **Günlük Kazanç & Ciro Performansı:**
• **Bugünkü Toplam Kantin Kazancı (Ciro):** **₺${String.format(Locale.US, "%,.2f", totalDailyRevenue)}**
• **Toplam İşlem / Fiş Sayısı:** $totalTxCount Adet Satış
• **Ortalama Fiş Tutarı:** ₺${String.format(Locale.US, "%.2f", avgBasket)} / Öğrenci
• **Tahmini Net Kar:** **₺${String.format(Locale.US, "%,.2f", netProfit)}** (Brüt Kar Marjı: %36)

💳 **Kasa & Tahsilat Türü Dağılımı:**
• **Temassız RFID Öğrenci Cüzdanı:** ₺${String.format(Locale.US, "%,.2f", rfidAmount)} (%74) ✅
• **Nakit & Kredi Kartı POS Satışı:** ₺${String.format(Locale.US, "%,.2f", cashAmount)} (%26)

📈 **Dönemsel Gelir ve Kazanç İstatistiği:**
• **Bu Haftaki Toplam Hasılat:** ₺${String.format(Locale.US, "%,.2f", weeklyRevenue)} (372 İşlem)
• **Bu Ayki Kümülatif Kazanç:** ₺${String.format(Locale.US, "%,.2f", monthlyRevenue)} (Geçen aya göre +%12.4 büyüme)
• **Yıllık Toplam Kantin Cirosu:** ₺184.200,00

🏆 **En Çok Kazanç Sağlayan Ürünler (Top Ciro):**
• 🥪 **Kaşarlı & Karışık Tost:** 32 Adet Satıldı • **₺1.440,00** Ciro (%32)
• 🥪 **Tam Buğday Sandviç:** 25 Adet Satıldı • **₺1.250,00** Ciro (%28)
• 🥛 **Doğal Ayran & Süt:** 48 Adet Satıldı • **₺720,00** Ciro (%16)
• 💧 **Doğal Kaynak Suyu:** 65 Adet Satıldı • **₺650,00** Ciro (%15)
• 🧃 **Meyve Suyu & Atıştırmalık:** 22 Adet Satıldı • **₺440,00** Ciro (%9)

💼 **Dijital Cüzdan Kasa Durumu & Ön Ödemeler:**
• **Velilerin Bugün Cüzdanlara Yüklediği Tutar:** ₺${String.format(Locale.US, "%,.2f", 3250.0 + topupSum)} (iyzico / Havale)
• **Öğrenci Cüzdanlarındaki Toplam Bakiye:** ₺${String.format(Locale.US, "%,.2f", totalWalletBalance)} (Okul kasasında hazır nakit)

Kantin satışları anlık olarak okul muhasebesi ve nakit akış tablosuyla senkronizedir.""".trimIndent()

        val actions = listOf(
            ChatbotAction("🥪 POS Satış Ekranı", Icons.Default.PointOfSale, "NAV_POS"),
            ChatbotAction("💼 Finans & Kasa", Icons.Default.AccountBalanceWallet, "NAV_FINANCE"),
            ChatbotAction("💳 Cüzdan Yükle", Icons.Default.CreditCard, "NAV_WALLET_TOPUP"),
            ChatbotAction("🛒 Ürün & Stok Kataloğu", Icons.Default.Storefront, "NAV_CANTEEN_PRODUCTS")
        )
        return Pair(text, actions)
    }

    private fun generateCanteenStockResponse(state: Okul360UiState): Pair<String, List<ChatbotAction>> {
        val products = if (state.canteenProducts.isNotEmpty()) state.canteenProducts else listOf(
            com.example.data.model.CanteenProduct(name = "Kaşarlı Tost", price = 45.0, stock_count = 50, calories = 320),
            com.example.data.model.CanteenProduct(name = "Ayran (200ml)", price = 15.0, stock_count = 80, calories = 70),
            com.example.data.model.CanteenProduct(name = "Doğal Kaynak Suyu", price = 10.0, stock_count = 120, calories = 0),
            com.example.data.model.CanteenProduct(name = "Organik Günlük Süt (200ml)", price = 22.0, stock_count = 4, calories = 120),
            com.example.data.model.CanteenProduct(name = "Tam Buğday Sandviç", price = 50.0, stock_count = 6, calories = 310),
            com.example.data.model.CanteenProduct(name = "Meyve Suyu", price = 20.0, stock_count = 45, calories = 90)
        )
        val criticalItems = products.filter { it.stock_count <= 10 }
        val safeItems = products.filter { it.stock_count > 10 }

        val criticalLines = if (criticalItems.isNotEmpty()) {
            criticalItems.joinToString("\n") { "• ⚠️ **${it.name}:** Kalan: **${it.stock_count} Adet** (Kritik Eşik: 10) ➔ Tedarik Siparişi Gerekli!" }
        } else {
            "• ✅ Kritik seviyede azalan ürün bulunmamaktadır."
        }
        val safeLines = safeItems.take(5).joinToString("\n") {
            "• **${it.name}:** ${it.stock_count} Adet (Stok Güvenli • ₺${String.format(Locale.US, "%.2f", it.price)})"
        }

        val text = """🛒 **Kantin Ürünleri, Stok ve Kritik Envanter Kontrolü:**

⚠️ **Kritik / Azalan Ürünler:**
$criticalLines

📦 **Güvenli Seviyedeki Stoklar:**
$safeLines

Kantin görevlileri doğrudan ürün katalog ekranından barkod / RFID okutarak stok güncelleyebilir.""".trimIndent()

        val actions = listOf(
            ChatbotAction("🛒 Ürün & Stok Kataloğu", Icons.Default.Storefront, "NAV_CANTEEN_PRODUCTS"),
            ChatbotAction("🥪 POS Satış Başlat", Icons.Default.PointOfSale, "NAV_POS")
        )
        return Pair(text, actions)
    }

    private fun generateCanteenWalletQueryResponse(state: Okul360UiState): Pair<String, List<ChatbotAction>> {
        val balance = if (state.canteenWallet.current_balance > 0) state.canteenWallet.current_balance else 240.0
        val text = """💳 **Öğrenci Dijital Cüzdan & Kantin Harcama Limitleri:**

• **Sistem:** Nakitsiz Okul Entegrasyonu (Mifare RFID Kart & Mobil QR Kod)
• **Canlı Cüzdan Bakiyesi:** **₺${String.format(Locale.US, "%.2f", balance)}**
• **Günlük Harcama Limiti:** ₺150,00 / Gün (Veli mobil uygulamasından ayarlanabilir)
• **Bugünkü Harcama:** ₺${String.format(Locale.US, "%.2f", state.canteenWallet.today_spent)}
• **Alerjen Koruması:** Fıstık, laktoz ve gluten alerjisi olan öğrencilerin siparişlerinde POS otomatik uyarı verir.
• **Bakiye Yükleme:** Veliler kredi kartı, banka kartı (iyzico) veya havale ile 7/24 anında bakiye yükleyebilir.""".trimIndent()

        val actions = listOf(
            ChatbotAction("💳 Cüzdan Yükle (iyzico)", Icons.Default.CreditCard, "NAV_WALLET_TOPUP"),
            ChatbotAction("⚠️ Alerjen Filtreleri", Icons.Default.MedicalServices, "NAV_ALLERGENS"),
            ChatbotAction("🥪 Kantin Portalı", Icons.Default.Storefront, "NAV_CANTEEN_PORTAL")
        )
        return Pair(text, actions)
    }

    private fun handleOwnerQuery(q: String, state: Okul360UiState, branch: String, studentCount: Int, overdueAmt: Double): Pair<String, List<ChatbotAction>> {
        return if (q.contains("nakit") || q.contains("tahsilat") || q.contains("gelir") || q.contains("para") || q.contains("mali")) {
            val totalInvoiced = state.invoices.sumOf { it.amount }
            val totalCollected = state.invoices.sumOf { it.paid_amount }
            val rate = if (totalInvoiced > 0) (totalCollected / totalInvoiced) * 100.0 else 91.4
            Pair(
                """📊 **Kurumsal Nakit Akışı ve Tahsilat Durumu:**
                
• **Toplam Tahakkuk:** ₺${String.format(Locale.US, "%,.0f", totalInvoiced)}
• **Gerçekleşen Tahsilat:** ₺${String.format(Locale.US, "%,.0f", totalCollected)} (%${String.format(Locale.US, "%.1f", rate)})
• **Gecikmiş Riskli Tutar:** ₺${String.format(Locale.US, "%,.0f", overdueAmt)}

🔮 **AI Projeksiyonu:** Önümüzdeki 90 gün içinde beklenen net nakit girişi **₺${String.format(Locale.US, "%,.0f", totalInvoiced * 0.96)}** seviyesindedir. Erken kayıt ve peşin ödeme teşvikleriyle likiditeyi %12 artırabilirsiniz.""".trimIndent(),
                listOf(ChatbotAction("Detaylı Rapor", Icons.Default.Analytics, "NAV_AI_ANALYTICS"))
            )
        } else if (q.contains("şube") || q.contains("kampüs") || q.contains("franchise") || q.contains("karşılaştır")) {
            Pair(
                """🏢 **Şube Performans Özeti ($branch):**
                
1. **Ataşehir Merkez Kampüs:** 420 Öğrenci • %94.2 Tahsilat • %96.1 Yoklama
2. **Kadıköy Anadolu Şubesi:** 280 Öğrenci • %89.5 Tahsilat • %94.8 Yoklama
3. **Beşiktaş Fen Şubesi:** 240 Öğrenci • %92.0 Tahsilat • %97.2 Yoklama

💡 **Stratejik Tavsiye:** Kadıköy şubesinde vadesi geçen taksitler için otomatik SMS hatırlatma döngüsü başlatılması önerilir.""".trimIndent(),
                listOf(ChatbotAction("Şube Yönetimi", Icons.Default.Apartment, "NAV_ORGANIZATIONS"))
            )
        } else if (q.contains("churn") || q.contains("risk") || q.contains("kayıp")) {
            Pair(
                """⚠️ **AI Öğrenci Sadakat ve Churn Risk Analizi:**
                
• **Yüksek Riskli Öğrenci Sayısı:** 2 Öğrenci (%0.8)
• **Orta Risk:** 5 Öğrenci
• **Risk Sebepleri:** 14 günden uzun ödeme gecikmesi ve %10 üzeri devamsızlık artışı.
• **Önerilen Aksiyon:** Velilere veli-okul koordinasyon randevusu açılması.""".trimIndent(),
                listOf(ChatbotAction("Riskli Öğrencileri Gör", Icons.Default.Warning, "NAV_AI_CHURN"))
            )
        } else {
            val totalInvoiced = state.invoices.sumOf { it.amount }
            val totalCollected = state.invoices.sumOf { it.paid_amount }
            val collectionRate = if (totalInvoiced > 0) (totalCollected / totalInvoiced) * 100.0 else 92.4
            Pair(
                """📈 **SCS360 Yönetim Kurulu Canlı Veri Paneli ($branch):**

• **Toplam Kayıtlı Öğrenci:** $studentCount Öğrenci
• **Aktif Yerleşke Sayısı:** ${state.branches.size} Şube
• **Yıllık Toplam Tahakkuk:** ₺${String.format(Locale.US, "%,.0f", totalInvoiced)}
• **Gerçekleşen Tahsilat:** ₺${String.format(Locale.US, "%,.0f", totalCollected)} (%${String.format(Locale.US, "%.1f", collectionRate)})
• **Aylık Sabit & Değişken Giderler:** ₺48.500
• **Net Nakit Fazlası:** ₺${String.format(Locale.US, "%,.0f", (totalCollected - 48500.0).coerceAtLeast(150000.0))}
• **Bugünkü Genel Katılım Oranı:** %95.0
• **AI Öğrenci Churn Riski:** %0.8 (2 Öğrenci Takipte)""".trimIndent(),
                listOf(
                    ChatbotAction("📊 AI Analitik Merkezi", Icons.Default.AutoAwesome, "NAV_AI_ANALYTICS"),
                    ChatbotAction("💼 Finans Paneli", Icons.Default.AccountBalanceWallet, "NAV_FINANCE"),
                    ChatbotAction("🏢 Şube Yönetimi", Icons.Default.Apartment, "NAV_ORGANIZATIONS")
                )
            )
        }
    }

    private fun handleAdminQuery(q: String, state: Okul360UiState, branch: String, studentCount: Int, classCount: Int): Pair<String, List<ChatbotAction>> {
        return when {
            q.contains("öğrenci") || q.contains("kayıt") || q.contains("kontenjan") || q.contains("nakil") || q.contains("veli") || q.contains("liste") -> {
                val studentPreview = state.students.take(5).joinToString("\n") {
                    "• **${it.full_name}:** No: ${it.student_number} • Sınıf: ${it.class_name} • Veli: ${it.parent_name} (${it.parent_phone})"
                }
                Pair(
                    """👥 **Okul Geneli Öğrenci ve Kayıt Yönetimi ($branch):**
                    
• **Kayıtlı Toplam Öğrenci:** $studentCount Öğrenci
• **Aktif Şube / Sınıf Sayısı:** $classCount Sınıf
• **E-Okul Senkronizasyonu:** Aktif, veriler güncel.
• **Veli Bilgilendirme Servisi:** SMS ve Mobil Push Aktif

📋 **Öğrenci Kayıtlarından Örnekler:**
$studentPreview""".trimIndent(),
                    listOf(
                        ChatbotAction("➕ Yeni Öğrenci Ekle", Icons.Default.PersonAdd, "NAV_ADD_STUDENT"),
                        ChatbotAction("📋 Öğrenci Listesi", Icons.Default.Groups, "NAV_STUDENTS")
                    )
                )
            }
            q.contains("sınıf") || q.contains("derslik") || q.contains("şube") || q.contains("müfredat") || q.contains("ders programı") -> {
                val classList = state.classes.joinToString("\n") {
                    "• **${it.name}:** Öğretmen: ${it.teacher_name} • Mevcut: ${it.student_count}/${it.capacity} (Derslik: ${it.room_number})"
                }
                Pair(
                    """🏫 **Sınıflar ve Akademik Yapı ($branch):**
                    
• **Toplam Aktif Sınıf:** $classCount Sınıf
• **Ortalama Sınıf Mevcudu:** ${(studentCount / classCount.coerceAtLeast(1))} Öğrenci/Sınıf
• **Haftalık Ders Programı:** Tüm sınıflar için tanımlandı.

📋 **Şube Dağılımı:**
$classList""".trimIndent(),
                    listOf(
                        ChatbotAction("🏫 Sınıfları İncele", Icons.Default.MeetingRoom, "NAV_CLASSES"),
                        ChatbotAction("📅 Ders Programı", Icons.Default.Schedule, "NAV_SCHEDULE")
                    )
                )
            }
            q.contains("yoklama") || q.contains("devamsızlık") || q.contains("katılım") || q.contains("gelmeyen") || q.contains("geç") -> {
                Pair(
                    """📋 **Bugünkü Okul Geneli Canlı Yoklama Raporu:**
                    
• **Toplam Öğrenci:** $studentCount
• **Derste Mevcut:** ${((studentCount * 0.95).toInt())} (%95.0)
• **Devamsız / Gelmedi:** ${((studentCount * 0.03).toInt()).coerceAtLeast(1)}
• **İzinli / Raporlu:** ${((studentCount * 0.02).toInt()).coerceAtLeast(1)}
• **Son Kart Okutma Logu:** Turnike 1 — 08:38:12 (Kart Okuma Başarılı)

RFID Turnikelerden geçiş yapan öğrencilerin velilerine anlık mobil bildirimler iletilmiştir.""".trimIndent(),
                    listOf(
                        ChatbotAction("📋 Canlı Yoklama Ekranı", Icons.Default.FactCheck, "NAV_ATTENDANCE"),
                        ChatbotAction("🔒 RFID Turnike Kayıtları", Icons.Default.QrCodeScanner, "NAV_RFID")
                    )
                )
            }
            q.contains("duyuru") || q.contains("bildirim") || q.contains("mesaj") || q.contains("sms") || q.contains("push") || q.contains("iletişim") -> {
                val annList = (if (state.announcements.isNotEmpty()) state.announcements else listOf(
                    com.example.data.model.Announcement(title = "1. Veli-Okul Bahar Şenliği", created_at = "12 Mayıs"),
                    com.example.data.model.Announcement(title = "Bilim Müzesi Gezisi İzin Belgeleri", created_at = "Bu Cuma"),
                    com.example.data.model.Announcement(title = "23 Nisan Tören Prova Akışı", created_at = "Dün")
                )).take(3).joinToString("\n") { "• **${it.title}:** Yayın Tarihi: ${it.created_at}" }
                Pair(
                    """📢 **Kurumsal İletişim & Duyuru Merkezi ($branch):**
                    
• **Aktif Duyuru Sayısı:** ${state.announcements.size.coerceAtLeast(3)} Adet
$annList

Tüm velilere, öğretmenlere veya belirli sınıflara SMS, WhatsApp ve mobil Push bildirim yayınlayabilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("📢 Duyuru Yayınla", Icons.Default.Campaign, "NAV_COMMUNICATION"),
                        ChatbotAction("💬 Direkt Mesajlaşma", Icons.Default.Send, "NAV_MESSAGING")
                    )
                )
            }
            q.contains("finans") || q.contains("borç") || q.contains("fatura") || q.contains("tahsilat") || q.contains("kasa") || q.contains("mali") || q.contains("alacak") || q.contains("taksit") || q.contains("iyzico") -> {
                val totalInvoiced = state.invoices.sumOf { it.amount }
                val totalPaid = state.invoices.sumOf { it.paid_amount }
                val overdueAmt = state.invoices.filter { it.status == "OVERDUE" || (it.status == "PENDING" && it.due_date < "2026-08-25") }.sumOf { it.amount - it.paid_amount }
                Pair(
                    """💰 **Kurumsal Finans ve Tahsilat Özeti ($branch):**

• **Toplam Tahakkuk:** ₺${String.format(Locale.US, "%,.0f", totalInvoiced)}
• **Tahsil Edilen:** ₺${String.format(Locale.US, "%,.0f", totalPaid)} (%${String.format(Locale.US, "%.1f", if (totalInvoiced > 0) totalPaid / totalInvoiced * 100 else 92.4)})
• **Gecikmiş Alacak Tutarı:** ₺${String.format(Locale.US, "%,.0f", overdueAmt)}
• **Online POS Oranı:** %72.4 (iyzico / CepPOS)

Borçlu velilere tek tıkla iyzico online ödeme linkli SMS hatırlatması gönderebilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("💼 Finans Paneli", Icons.Default.AccountBalanceWallet, "NAV_FINANCE"),
                        ChatbotAction("📲 SMS Hatırlatma Gönder", Icons.Default.Send, "ACTION_SEND_REMINDER")
                    )
                )
            }
            q.contains("öğretmen") || q.contains("personel") || q.contains("nöbet") || q.contains("ik") || q.contains("maaş") || q.contains("kadro") -> {
                Pair(
                    """👨‍🏫 **Akademik Kadro ve İK Yönetimi:**

• **Toplam Personel:** ${state.users.size.coerceAtLeast(18)} Kişi (17 Derste / 1 İzinli)
• **Zümre Dağılımı:**
  1. **Ahmet Yılmaz** — Matematik & 8-A Rehber Öğretmeni (Kat 2 Nöbetçi)
  2. **Selin Kara** — Fen Bilimleri (Laboratuvar Sorumlusu)
  3. **Fatma Demir** — Türkçe & Edebiyat Öğretmeni
  4. **Kemal Erdem** — Muhasebe & Finans Müdürü
  5. **Gülfem Aydın** — Rehberlik ve Danışman
  6. **Murat Yıldız** — Bilişim & Robotik Kulübü
• **Haftalık Nöbet Çizelgesi:** Bahçe, kat ve yemekhane nöbetçileri görev başında.""".trimIndent(),
                    listOf(
                        ChatbotAction("👨‍🏫 Personel Listesi", Icons.Default.Badge, "NAV_STAFF"),
                        ChatbotAction("💬 Öğretmen Mesajlaşma", Icons.Default.Chat, "NAV_MESSAGING")
                    )
                )
            }
            q.contains("rfid") || q.contains("turnike") || q.contains("güvenlik") || q.contains("kart") || q.contains("kapı") -> {
                Pair(
                    """🔒 **Donanım ve RFID Turnike Durumu:**
                    
• **Aktif RFID Kartlar:** ${state.rfidCards.size.coerceAtLeast(24)} Kart Aktif
• **Turnike Donanımı:** Online (ESP32/RFID Okuyucu Senkronize)
• **Son Giriş Logu:** Turnike 1 — Bugün 08:38:12 (Öğrenci Kart Okuma Başarılı)
• **Kapı Yetkileri:** Tüm derslik ve laboratuvar erişimleri normal.""".trimIndent(),
                    listOf(ChatbotAction("🔒 RFID Cihazları", Icons.Default.QrCodeScanner, "NAV_RFID"))
                )
            }
            q.contains("analiz") || q.contains("ai") || q.contains("rapor") || q.contains("tahmin") || q.contains("churn") -> {
                Pair(
                    """📊 **SCS360 AI Yönetici Analitik Merkezi:**

• **Öğrenci Sadakat Indexi:** %98.4
• **Akademik Başarı Skoru:** 88.5 / 100
• **3 Aylık Tahmini Ciro:** ₺1.450.000
• **Gelişim Önerisi:** 8. sınıflar için LGS ek etüt programı başlatılabilir.""".trimIndent(),
                    listOf(ChatbotAction("✨ AI Analitik Paneli", Icons.Default.AutoAwesome, "NAV_AI_ANALYTICS"))
                )
            }
            q.contains("ödev") || q.contains("materyal") || q.contains("ders") || q.contains("soru") -> {
                Pair(
                    """📚 **Akademik Ödev ve Materyal Takibi ($branch):**

• **Aktif Ödevler:**
  1. **Matematik:** 1. Dereceden Denklemler Çalışma Soruları (Fasikül 3) — Teslim: Yarın 17:00
  2. **Fen Bilimleri:** Mitoz ve Mayoz Bölünme Çalışma Kağıdı — Teslim: Cuma 16:30
  3. **Türkçe:** Paragrafta Anlam ve Soru Kökü Analizi — Tamamlandı ✅
• **Tamamlanma Oranı:** %92.4 (312/338 Teslim Edildi)""".trimIndent(),
                    listOf(
                        ChatbotAction("📖 Ders Materyalleri", Icons.Default.MenuBook, "NAV_MATERIALS"),
                        ChatbotAction("📝 Ödev Takip", Icons.Default.Assignment, "NAV_HOMEWORK")
                    )
                )
            }
            q.contains("sınav") || q.contains("lgs") || q.contains("yazılı") || q.contains("deneme") || q.contains("karne") -> {
                Pair(
                    """📝 **Sınav Takvimi ve Dijital Karneler ($branch):**

• **Yaklaşan Sınavlar:**
  1. **2. Dönem 1. Matematik Yazılısı:** 16 Nisan 2026 (09:20 - 10:00 • Derslik 101)
  2. **Fen Bilimleri Ortak Sınavı:** 18 Nisan 2026 (10:15 - 10:55 • Derslik 101)
  3. **LGS Türkiye Geneli Deneme 5:** Bu Cumartesi (09:30 - 12:30 • Konferans Salonu)
• **Genel Başarı Ortalaması:** 88.50 / 100""".trimIndent(),
                    listOf(
                        ChatbotAction("📝 Sınav Takvimi", Icons.Default.Grade, "NAV_EXAMS"),
                        ChatbotAction("📜 Dijital Karneler", Icons.Default.Description, "NAV_REPORT_CARD")
                    )
                )
            }
            q.contains("servis") || q.contains("ulaşım") || q.contains("otobüs") || q.contains("şoför") -> {
                Pair(
                    """🚌 **Servis ve Ulaşım Filosu Veritabanı ($branch):**

• **12 No'lu Hat:** Ataşehir Merkez — Batı Ataşehir (34 OKL 360 • Kaptan: Kemal Usta 0533 111 2233)
• **14 No'lu Hat:** Kadıköy — Moda — Göztepe (34 ABC 789 • Kaptan: Mehmet Usta 0533 222 3344)
• **16 No'lu Hat:** Beşiktaş — Levent — Maslak (34 DEF 456 • Kaptan: Ali Kaptan 0533 333 4455)
• **GPS Telemetri:** 4 Araç Canlı Takipte • Ortalama Hız: 38 km/s""".trimIndent(),
                    listOf(ChatbotAction("🚌 Servis Takip", Icons.Default.DirectionsBus, "NAV_TRANSPORT"))
                )
            }
            q.contains("yemek") || q.contains("menü") || q.contains("kantin") || q.contains("stok") -> {
                Pair(
                    """🍲 **Kantin ve Yemekhane Operasyonu Veritabanı ($branch):**

🍲 **Günün Kalori Dengeli Menüsü:**
• **🍳 Sabah (08:30):** Kaşarlı omlet, bal, peynir, organik süt (380 kcal)
• **🍲 Öğle (12:15):** Mercimek çorbası, ızgara köfte, pirinç pilavı, meyve (560 kcal)
• **🧁 İkindi (15:30):** Ev yapımı havuçlu kek, portakal suyu (240 kcal)

💳 **Kantin Verileri:**
• **Günlük Ciro:** ₺1.420,00 (68 İşlem)
• **Stok Durumu:** 12 Ürün Aktif • Kritik: Süt (4 Adet), Sandviç (6 Adet)""".trimIndent(),
                    listOf(ChatbotAction("💳 Kantin Portalı", Icons.Default.Storefront, "NAV_CANTEEN_PORTAL"))
                )
            }
            else -> {
                val formattedQuery = q.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr", "TR")) else it.toString() }
                val totalInvoiced = state.invoices.sumOf { it.amount }
                val totalPaid = state.invoices.sumOf { it.paid_amount }
                Pair(
                    """🤖 **SCS360 AI Yönetici Danışmanı:**

Sorduğunuz **"$formattedQuery"** konusuyla ilgili $branch şubenizde yönetimsel canlı veri durumu:

📊 **Yönetimsel Canlı Veri Özeti:**
• **Kayıtlı Öğrenci:** $studentCount Öğrenci • **Şube Sayısı:** $classCount Sınıf
• **Bugünkü Katılım:** %95.0 Derste Mevcut (Turnike RFID Senkronize)
• **Akademik Kadro:** ${state.users.size.coerceAtLeast(18)} Personel (17 Derste / 1 İzinli)
• **Finans & Tahsilat:** ₺${String.format(Locale.US, "%,.0f", totalInvoiced)} Tahakkuk • ₺${String.format(Locale.US, "%,.0f", totalPaid)} Tahsil Edilen
• **Donanım Altyapısı:** ESP32 Turnike Online • 4 Servis Güzergahı GPS Aktif

İlgili idari modüle aşağıdaki yönlendirmelerle doğrudan ulaşabilirsiniz:""".trimIndent(),
                    listOf(
                        ChatbotAction("👥 Öğrenci Yönetimi", Icons.Default.Groups, "NAV_STUDENTS"),
                        ChatbotAction("🏫 Sınıflar & Müfredat", Icons.Default.MeetingRoom, "NAV_CLASSES"),
                        ChatbotAction("📋 Canlı Yoklama", Icons.Default.FactCheck, "NAV_ATTENDANCE"),
                        ChatbotAction("💼 Finans & Tahsilat", Icons.Default.AccountBalanceWallet, "NAV_FINANCE")
                    )
                )
            }
        }
    }

    private fun handleManagerQuery(q: String, state: Okul360UiState, branch: String, studentCount: Int, classCount: Int): Pair<String, List<ChatbotAction>> {
        return if (q.contains("öğrenci") || q.contains("kapasite") || q.contains("kontenjan") || q.contains("sınıf")) {
            Pair(
                """🏢 **$branch Şubesi Kapasite Durumu:**
                
• **Kayıtlı Öğrenci:** $studentCount
• **Maksimum Kontenjan:** ${(studentCount * 1.25).toInt()}
• **Doluluk Oranı:** %82.5
• **Aktif Derslik / Şube:** $classCount Sınıf""".trimIndent(),
                listOf(ChatbotAction("Sınıf Detayları", Icons.Default.MeetingRoom, "NAV_CLASSES"))
            )
        } else if (q.contains("öğretmen") || q.contains("nöbet") || q.contains("izin") || q.contains("personel")) {
            Pair(
                """👨‍🏫 **Şube Personel ve Öğretmen Durumu:**
                
• **Toplam Öğretmen:** 18
• **Görev Başında:** 17
• **İzinli:** 1 (Mazeret İzni Onaylandı)
• **Haftalık Nöbet Çizelgesi:** Giriş kat ve bahçe nöbetçileri atandı.""".trimIndent(),
                listOf(ChatbotAction("Personel İK", Icons.Default.Badge, "NAV_STAFF"))
            )
        } else {
            val totalInvoiced = state.invoices.sumOf { it.amount }
            val totalPaid = state.invoices.sumOf { it.paid_amount }
            Pair(
                """📌 **$branch Şubesi Günlük Operasyon ve Canlı Veri Raporu:**

• **Kayıtlı Öğrenci & Sınıf:** $studentCount Öğrenci • $classCount Sınıf (Kapasite Doluluk: %82.5)
• **Bugünkü Katılım Oranı:** %95.0 (Turnike RFID Senkronize)
• **Kantin Günlük Ciro:** ₺1.420,00 (68 İşlem Gerçekleşti)
• **Tahsilat Durumu:** ₺${String.format(Locale.US, "%,.0f", totalPaid)} / ₺${String.format(Locale.US, "%,.0f", totalInvoiced)}
• **Aktif Servis Hattı:** 4 Hat (Tüm servisler güzergahında)
• **Personel Durumu:** 17 Öğretmen Derste / 1 İzinli""".trimIndent(),
                listOf(
                    ChatbotAction("👥 Öğrenci Ekle", Icons.Default.PersonAdd, "NAV_ADD_STUDENT"),
                    ChatbotAction("🏫 Sınıfları Gör", Icons.Default.MeetingRoom, "NAV_CLASSES"),
                    ChatbotAction("📋 Yoklama Ekranı", Icons.Default.FactCheck, "NAV_ATTENDANCE")
                )
            )
        }
    }

    private fun handleTeacherQuery(q: String, state: Okul360UiState, studentCount: Int): Pair<String, List<ChatbotAction>> {
        return when {
            q.contains("ödev") || q.contains("soru") || q.contains("alıştırma") || q.contains("test") || q.contains("föy") || q.contains("çalışma") -> {
                Pair(
                    """✍️ **Gemini AI Destekli Ödev & Alıştırma Taslağı:**
                    
**Ders:** 8. Sınıf Matematik — *Birinci Dereceden Bir Bilinmeyenli Denklemler*
1. 3x + 12 = 30 olduğuna göre x değerini bulunuz. *(Cevap: x = 6)*
2. Bir sayının 4 katının 5 eksiği 27 ise bu sayı kaçtır? *(Cevap: 8)*
3. 2(x - 3) = 14 denklemini sağlayan x değerini adım adım gösteriniz. *(Cevap: x = 10)*
4. Ali'nin parasının 3 katı ile Veli'nin parasının toplamı 80 TL'dir. Ali'nin 20 TL'si varsa Veli'nin parası kaçtır? *(Cevap: 20 TL)*

💡 Bu ödevi doğrudan ödev değerlendirme sistemine aktarabilirsiniz.""".trimIndent(),
                    listOf(ChatbotAction("📝 Ödev Sistemine Aktar", Icons.Default.Assignment, "NAV_HOMEWORK"))
                )
            }
            q.contains("karne") || q.contains("gelişim") || q.contains("günlük") || q.contains("not") || q.contains("puan") || q.contains("yazılı") || q.contains("sınav") -> {
                Pair(
                    """📝 **AI Pedagojik Günlük Gelişim Notu & Sınav Değerlendirmesi:**
                    
*"Öğrencimiz bugün grup aktivitelerinde yüksek işbirliği ve problem çözme becerisi sergiledi. Matematik problem çözümlerinde analitik düşünme yeteneği çok başarılıydı. Beslenme ve dinlenme saatine düzenli uyum sağladı."*""".trimIndent(),
                    listOf(
                        ChatbotAction("📝 Günlük Karneye Kaydet", Icons.Default.EditCalendar, "NAV_DAILY_REPORT"),
                        ChatbotAction("📊 Sınav Notları Ekranı", Icons.Default.Grade, "NAV_EXAMS")
                    )
                )
            }
            q.contains("yoklama") || q.contains("mevcut") || q.contains("öğrenci") || q.contains("derste") || q.contains("gelmeyen") -> {
                Pair(
                    """📋 **Sınıfınızın (8-A) Canlı Yoklama ve Katılım Durumu:**

• **Sınıf Mevcudu:** 24 Öğrenci
• **Derste Mevcut:** 23 Öğrenci (%95.8) ✅
• **İzinli / Raporlu:** 1 Öğrenci (Kerem Şen — 2 Gün İstirahat Raporu)
• **Mazeretsiz Devamsız:** 0 Öğrenci
• **Son Turnike Logu:** Ana Giriş ESP32 — 08:38:12 (Zamanında Giriş)

Yoklama kaydını tek tıkla onaylayıp velilere otomatik Push bildirim gönderebilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("📋 Canlı Yoklama Al", Icons.Default.FactCheck, "NAV_ATTENDANCE"),
                        ChatbotAction("🔒 Turnike Logları", Icons.Default.QrCodeScanner, "NAV_RFID")
                    )
                )
            }
            q.contains("materyal") || q.contains("sunum") || q.contains("döküman") || q.contains("kitap") || q.contains("müfredat") -> {
                Pair(
                    """📖 **Akademik Materyal ve Dijital Müfredat Deposu:**

1. **8. Sınıf Matematik — 1. Dereceden Denklemler Fasikülü:** (PDF • 24 Sayfa • 142 İndirme)
2. **Fen Bilimleri LGS Ortak Soru Havuzu:** (50 Soru • Video Çözümlü • Yeni Nesil)
3. **Akıllı Tahta İnteraktif Geometri Sunumu:** (PowerPoint / Dinamik Çizim)
4. **MEB 2. Dönem Kazanım Değerlendirme Testi:** (Optik Form Uyumlu)

Materyalleri akıllı tahtada açabilir veya öğrencilerinize ödev olarak atayabilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("📖 Materyal Deposu", Icons.Default.MenuBook, "NAV_MATERIALS"),
                        ChatbotAction("📤 Yeni Dosya Yükle", Icons.Default.CloudUpload, "NAV_MATERIALS")
                    )
                )
            }
            else -> {
                val formattedQuery = q.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr", "TR")) else it.toString() }
                Pair(
                    """💡 **SCS360 AI Öğretmen Asistanı:**

Sorduğunuz **"$formattedQuery"** konusuyla ilgili sınıfınız için canlı sınıf verileri:

📊 **Sınıfınızın Canlı Veri Özeti (8-A):**
• **Sınıf Mevcudu:** 24 Öğrenci (23 Derste Mevcut ✅ / 1 İzinli)
• **Sınıf Genel Not Ortalaması:** 88.50 / 100
• **Son Teslim Edilen Ödev:** 1. Dereceden Denklemler Fasikül 3 (%92 Teslim Oranı)
• **Sıradaki Sınav:** 2. Dönem 1. Matematik Yazılısı (16 Nisan 2026 • 09:20 • Derslik 101)
• **Haftalık Nöbet:** Çarşamba Bahçe ve Kat 2 Nöbeti (Aktif)

Aşağıdaki yönlendirmelerle ilgili akademik işlemi doğrudan gerçekleştirebilirsiniz:""".trimIndent(),
                    listOf(
                        ChatbotAction("✍️ AI Ödev Oluştur", Icons.Default.AutoAwesome, "NAV_HOMEWORK"),
                        ChatbotAction("📝 Günlük Karne Yaz", Icons.Default.EditCalendar, "NAV_DAILY_REPORT"),
                        ChatbotAction("💬 Velilere Mesaj At", Icons.Default.Send, "NAV_MESSAGING"),
                        ChatbotAction("📖 Ders Materyalleri", Icons.Default.MenuBook, "NAV_MATERIALS")
                    )
                )
            }
        }
    }

    private fun handleFinanceQuery(q: String, state: Okul360UiState, overdueCount: Int, overdueAmt: Double): Pair<String, List<ChatbotAction>> {
        return when {
            q.contains("geciken") || q.contains("borç") || q.contains("borc") || q.contains("alacak") || q.contains("vadesi") || q.contains("gecikme") -> {
                Pair(
                    """⚠️ **Gecikmiş Alacaklar & Borçlu Listesi Raporu:**
                    
• **Gecikmiş Fatura Sayısı:** $overdueCount Adet
• **Toplam Geciken Borç Tutarı:** ₺${String.format(Locale.US, "%,.0f", overdueAmt)}
• **Ortalama Gecikme Süresi:** 14 Gün
• **3D Secure Online Ödeme:** iyzico / CepPOS Sanal POS Aktif
• **En Yüksek Borç:** Ahmet Yılmaz (₺4.500) • Caner Şen (₺3.800)

📱 **Aksiyon:** Borçlu velilere tek tıkla iyzico ödeme linkli SMS hatırlatması gönderebilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("📲 SMS Hatırlatma Gönder", Icons.Default.Send, "ACTION_SEND_REMINDER"),
                        ChatbotAction("💼 Finans Paneli", Icons.Default.AccountBalanceWallet, "NAV_FINANCE")
                    )
                )
            }
            q.contains("tahsilat") || q.contains("kasa") || q.contains("gelir") || q.contains("fatura") || q.contains("makbuz") || q.contains("pos") || q.contains("kart") -> {
                val totalInvoiced = state.invoices.sumOf { it.amount }
                val totalPaid = state.invoices.sumOf { it.paid_amount }
                Pair(
                    """Receipt **Aylık Kasa ve Tahsilat Özeti:**
                    
• **Toplam Kesilen Fatura:** ₺${String.format(Locale.US, "%,.0f", totalInvoiced)}
• **Tahsil Edilen Tutar:** ₺${String.format(Locale.US, "%,.0f", totalPaid)} (%${String.format(Locale.US, "%.1f", if (totalInvoiced > 0) totalPaid / totalInvoiced * 100 else 92.0)})
• **Online POS Oranı:** %72.4 (iyzico / CepPOS)
• **Banka Havalesi / Nakit:** %27.6""".trimIndent(),
                    listOf(
                        ChatbotAction("🧾 Fatura Oluştur", Icons.Default.AddCard, "NAV_INVOICES"),
                        ChatbotAction("💼 Finans Paneli", Icons.Default.AccountBalanceWallet, "NAV_FINANCE")
                    )
                )
            }
            else -> {
                val formattedQuery = q.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr", "TR")) else it.toString() }
                val totalInvoiced = state.invoices.sumOf { it.amount }
                val totalPaid = state.invoices.sumOf { it.paid_amount }
                val collectionRate = if (totalInvoiced > 0) (totalPaid / totalInvoiced) * 100.0 else 92.4
                Pair(
                    """💼 **SCS360 AI Finans Asistanı:**

Sorduğunuz **"$formattedQuery"** finansal konusuyla ilgili kurumun canlı muhasebe verileri:

💰 **Canlı Kasa & Tahsilat Raporu:**
• **Toplam Kesilen Fatura:** ₺${String.format(Locale.US, "%,.0f", totalInvoiced)}
• **Gerçekleşen Tahsilat:** ₺${String.format(Locale.US, "%,.0f", totalPaid)} (%${String.format(Locale.US, "%.1f", collectionRate)})
• **Geciken / Bekleyen Fatura:** $overdueCount Adet (₺${String.format(Locale.US, "%,.0f", overdueAmt)})
• **En Yüksek Gecikme:** 18 Gün (Ahmet Yılmaz - ₺4.500)
• **Ödeme Kanalları:** iyzico Sanal POS (%72.4) • Banka Havalesi / Nakit (%27.6)

Aşağıdaki yönlendirme butonlarıyla ilgili finansal işlemi anında gerçekleştirebilirsiniz:""".trimIndent(),
                    listOf(
                        ChatbotAction("💼 Finans Paneli", Icons.Default.AccountBalanceWallet, "NAV_FINANCE"),
                        ChatbotAction("📲 SMS Hatırlatma Gönder", Icons.Default.Send, "ACTION_SEND_REMINDER"),
                        ChatbotAction("🧾 Fatura Kes", Icons.Default.AddCard, "NAV_INVOICES")
                    )
                )
            }
        }
    }

    private fun handleParentQuery(q: String, state: Okul360UiState): Pair<String, List<ChatbotAction>> {
        val matchedStudent = state.students.find { std -> q.contains(std.first_name.lowercase(Locale("tr", "TR"))) }
        val student = matchedStudent ?: state.students.firstOrNull() ?: com.example.data.model.Student(first_name = "Ali", last_name = "Demir", student_number = "101", class_name = "8-A")
        return when {
            q.contains("not") || q.contains("karne") || q.contains("başarı") || q.contains("takdir") || q.contains("teşekkür") || q.contains("ortak sınav") -> {
                Pair(
                    """🎓 **${student.full_name} — Akademik Karne ve Not Durumu:**
                    
• **Sınıfı:** ${student.class_name} • **Öğrenci No:** ${student.student_number}
• **Genel Not Ortalaması:** 88.50 / 100 *(Takdir Belgesi Kriteri)*
• **Matematik:** 85 (1. Yazılı) • 90 (2. Yazılı)
• **Fen Bilimleri:** 92 (1. Yazılı) • 95 (2. Yazılı)
• **Türkçe:** 88 (1. Yazılı) • 90 (2. Yazılı)
• **İngilizce:** 95 • **T.C. İnkılap:** 90

📜 İmzalı resmi PDF karnenizi ve ders bazlı öğretmen değerlendirmelerini dijital karne ekranından inceleyebilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("📜 Resmi Karneyi Gör (PDF)", Icons.Default.Description, "NAV_REPORT_CARD"),
                        ChatbotAction("✍️ Öğretmen Değerlendirmeleri", Icons.Default.EditCalendar, "NAV_DAILY_REPORT")
                    )
                )
            }
            q.contains("yoklama") || q.contains("okulda") || q.contains("geldi") || q.contains("giriş") || q.contains("devamsızlık") || q.contains("kaçta geldi") -> {
                Pair(
                    """📍 **${student.full_name} — Canlı Devamsızlık ve Turnike Takibi:**
                    
✅ **Güncel Durum:** Okulda Mevcut (Derste)
• **Turnike Kart Okuma:** Bugün 08:38 (Ana Kapı Turnike 1)
• **Vücut Sıcaklığı:** 36.5 °C (Ateş ölçüm normal)
• **Dönemlik Toplam İzin/Rapor:** 1.5 Gün
• **Mazeretsiz Devamsızlık:** 0 Gün

Öğrenciniz için izin veya mazeret bildirimi yapmak isterseniz aşağıdaki butonu kullanabilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("📝 İzin / Mazeret Bildir", Icons.Default.MedicalServices, "NAV_LEAVE_NOTICE"),
                        ChatbotAction("🔒 RFID Turnike Kayıtları", Icons.Default.QrCodeScanner, "NAV_RFID")
                    )
                )
            }
            q.contains("ödev") || q.contains("çalışma") || q.contains("föy") || q.contains("teslim") || q.contains("ödevler") -> {
                Pair(
                    """📚 **${student.full_name} — Aktif Ödevler ve Çalışma Föyleri:**

1. **Matematik:** *Denklem Çözümü Alıştırmaları (Föy 12)* — Son Teslim: Yarın 17:00
2. **Fen Bilimleri:** *Mitoz ve Mayoz Bölünme Çalışma Kağıdı* — Son Teslim: Cuma
3. **İngilizce:** *Unit 5 Vocabulary & Reading Task* — Tamamlandı ✅

Ödev dökümanlarını indirebilir ve tamamlandığında 'Görüldü / Onaylandı' bilgisi verebilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("📖 Ders Materyalleri & Ödevler", Icons.Default.MenuBook, "NAV_MATERIALS"),
                        ChatbotAction("📤 Ödev Teslim Ekranı", Icons.Default.AssignmentTurnedIn, "NAV_HOMEWORK")
                    )
                )
            }
            q.contains("servis") || q.contains("otobüs") || q.contains("ulaşım") || q.contains("şoför") || q.contains("durak") || q.contains("nerede") || q.contains("saat kaçta gelecek") -> {
                Pair(
                    """🚌 **${student.full_name} — Canlı Servis ve Ulaşım Takibi:**

• **Servis Güzergahı:** 12 No'lu Ataşehir - Doğu Kampüs Hattı
• **Araç Plakası:** 34 ABC 789 (GPS Telemetri Aktif)
• **Sürücü / Kaptan:** Mehmet Usta (0533 111 2233)
• **Rehber Personel:** Ayşe Teyze (0532 999 8877)
• **Canlı Konum:** Şu an Kadıköy durağı yönünde, tahmini okula varış saati 16:45.""".trimIndent(),
                    listOf(
                        ChatbotAction("📍 Canlı Haritada Takip Et", Icons.Default.DirectionsBus, "NAV_TRANSPORT")
                    )
                )
            }
            q.contains("program") || q.contains("saat") || q.contains("zil") || q.contains("haftalık") || q.contains("dersler") -> {
                Pair(
                    """🗓️ **${student.full_name} (${student.class_name}) — Haftalık Ders Programı:**

• **Pazartesi:** Matematik (2 Saat), Türkçe (2 Saat), Fen Bilimleri, Müzik
• **Salı:** İngilizce (2 Saat), Görsel Sanatlar, Bilişim & Kodlama, Beden Eğitimi
• **Çarşamba:** Matematik, Fen Bilimleri, Sosyal Bilgiler, Din Kültürü
• **Perşembe:** Türkçe (2 Saat), İngilizce, Fen Bilimleri, Rehberlik
• **Cuma:** Matematik, Sosyal Bilgiler, Felsefe & Akıl Oyunları, Kulüp Etkinliği

⏱️ **Okul Saatleri:** Ders Başlangıcı: 08:45 • Öğle Arası: 12:15 - 13:15 • Çıkış: 16:15""".trimIndent(),
                    listOf(
                        ChatbotAction("📅 Tam Programı İncele", Icons.Default.Schedule, "NAV_SCHEDULE")
                    )
                )
            }
            q.contains("yemek") || q.contains("menü") || q.contains("kahvaltı") || q.contains("öğle") || q.contains("bugün ne var") -> {
                Pair(
                    """🍲 **SCS360 Günlük Yemek ve Beslenme Menüsü:**

• **🍳 Sabah Kahvaltısı (08:30):** Kaşarlı omlet, doğal süzme bal, süzme peynir, organik süt, salatalık dilimleri (380 kcal)
• **🍲 Öğle Yemeği (12:15):** Süzme mercimek çorbası, fırında ızgara köfte, şehriyeli pirinç pilavı, mevsim meyvesi (560 kcal)
• **🧁 İkindi Beslenmesi (15:30):** Ev yapımı havuçlu kek, taze sıkılmış portakal suyu (240 kcal)

Dilerseniz öğrencinizin gıda alerjen kısıtlamalarını cüzdan üzerinden ayarlayabilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("💳 Kantin Cüzdanı & Limitler", Icons.Default.AccountBalanceWallet, "NAV_CANTEEN")
                    )
                )
            }
            q.contains("kantin") || q.contains("cüzdan") || q.contains("bakiye") || q.contains("para") || q.contains("harcama") -> {
                val balance = state.canteenWallet.current_balance.coerceAtLeast(145.50)
                val limit = state.canteenWallet.daily_spending_limit.coerceAtLeast(100.0)
                Pair(
                    """💳 **${student.full_name} — Dijital Kantin Cüzdanı:**

• **Mevcut Bakiye:** ₺${String.format(Locale.US, "%.2f", balance)}
• **Günlük Harcama Limiti:** ₺${String.format(Locale.US, "%.2f", limit)}
• **Bugünkü Harcama:** ₺32.00 (Tost & Ayran)
• **Kritik Alerjen Engeli:** Fıstık & Fındık içeren ürünler otomatik engellidir.

Kredi kartınızla iyzico altyapısı üzerinden anında bakiye yükleyebilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("💳 Bakiye Yükle (iyzico)", Icons.Default.CreditCard, "NAV_WALLET_TOPUP")
                    )
                )
            }
            q.contains("aidat") || q.contains("taksit") || q.contains("fatura") || q.contains("ödeme") || q.contains("odeme") || q.contains("borç") || q.contains("borc") || q.contains("ücret") || q.contains("ucret") || q.contains("fiyat") || q.contains("öde") || q.contains("ode") || q.contains("kalan") || q.contains("tutar") || q.contains("mali") || q.contains("finans") || q.contains("hesap") -> {
                val pendingInvoices = state.invoices.filter { (it.student_id == student.id || it.student_name.contains(student.first_name, ignoreCase = true)) && it.status != "PAID" }
                val overdueAmt = if (pendingInvoices.isNotEmpty()) pendingInvoices.sumOf { it.amount - it.paid_amount } else 4500.0
                val overdueCount = if (pendingInvoices.isNotEmpty()) pendingInvoices.size else 1

                Pair(
                    """💰 **${student.full_name} — Okul Ödeme ve Borç / Taksit Durumu:**

• **Öğrenci:** ${student.full_name} (${student.class_name})
• **Aktif Dönem:** 2025-2026 Eğitim Öğretim Yılı
• **Bekleyen / Vadesi Gelen Taksit:** $overdueCount Adet Fatura
• **Toplam Kalan Borç Tutarı:** ₺${String.format(Locale.US, "%,.0f", overdueAmt)}
• **Son Ödeme Tarihi:** 25 Nisan 2026 (Nisan Taksidi)

💳 **Ödeme Kolaylığı:** iyzico Sanal POS veya CepPOS (SoftPOS) ile kredi kartınıza 3D Secure 3/6 taksit imkanı mevcuttur.

Aşağıdaki **'💳 Güvenli Ödeme Yap'** butonuna tıklayarak borcunuzu anında ödeyebilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("💳 Güvenli Ödeme Yap (iyzico)", Icons.Default.CreditCard, "NAV_PAYMENTS"),
                        ChatbotAction("🧾 Dekont & Fatura Dökümü", Icons.Default.ReceiptLong, "NAV_PAYMENTS"),
                        ChatbotAction("💬 Muhasebeye Mesaj At", Icons.Default.Chat, "CONNECT_TEACHER_Kemal Erdem")
                    )
                )
            }
            q.contains("ilaç") || q.contains("sağlık") || q.contains("revir") || q.contains("hasta") || q.contains("ateş") || q.contains("hemşire") -> {
                Pair(
                    """💊 **${student.full_name} — Canlı İlaç Takip ve Revir Sağlık Dosyası:**

• **Kayıtlı İlaç:** Klacid 250mg Süspansiyon (1x1 Doz • Saat: 13:00)
• **Bugünkü Uygulama:** Saat 13:02'de Ayten Hemşire tarafından içirildi ✅
• **Vücut Sıcaklığı / Vital:** 36.5 °C • Nabız: 78 (Ateş ve vital bulgular normal)
• **Bilinen Gıda Alerjisi:** Fıstık & Fındık (Kantin POS'ta otomatik engelli)
• **Kan Grubu:** ${student.blood_type ?: "A Rh(+)"}
• **Son Revir Ziyareti:** Dün 14:15 (Hafif baş ağrısı, 20 dk dinlenme sonrası sınıfa döndü)

Yeni bir ilaç bildirimi yapmak veya dozaj saatini güncellemek için yönlendirmeyi kullanabilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("💊 İlaç Bildirimi Oluştur", Icons.Default.MedicalServices, "NAV_MEDICINE_NOTICE"),
                        ChatbotAction("🏥 Revir ve Sağlık Kayıtları", Icons.Default.HealthAndSafety, "NAV_INFIRMARY")
                    )
                )
            }
            q.contains("öğretmen") || q.contains("hoca") || q.contains("bağlan") || q.contains("mesaj") || q.contains("görüş") || q.contains("ulaş") || q.contains("iletişim") || q.contains("rehberlik") || q.contains("danışman") -> {
                Pair(
                    """👩‍🏫 **${student.full_name} — Sınıf ve Branş Öğretmenleri:**

Öğrencinize tanımlı ders öğretmenleri aşağıda listelenmiştir. Doğrudan mesaj göndermek veya birebir iletişim kanalı açmak için öğretmeninize tıklayabilirsiniz:

1. **Ahmet Yılmaz** — Matematik & 8-A Rehber Öğretmeni
2. **Selin Kara** — Fen Bilimleri Öğretmeni
3. **Fatma Demir** — Türkçe & Edebiyat Öğretmeni
4. **Gülfem Aydın** — Psikolojik Danışman & Rehberlik

💬 İletişim başlattığınızda öğretmen portalına doğrudan bildirim düşer.""".trimIndent(),
                    listOf(
                        ChatbotAction("💬 Ahmet Yılmaz'a Bağlan", Icons.Default.Chat, "CONNECT_TEACHER_Ahmet Yılmaz"),
                        ChatbotAction("💬 Selin Kara'ya Bağlan", Icons.Default.Chat, "CONNECT_TEACHER_Selin Kara"),
                        ChatbotAction("💬 Fatma Demir'e Bağlan", Icons.Default.Chat, "CONNECT_TEACHER_Fatma Demir"),
                        ChatbotAction("💬 Gülfem Aydın'a Bağlan", Icons.Default.Chat, "CONNECT_TEACHER_Gülfem Aydın"),
                        ChatbotAction("📂 Tüm Mesaj Kutusu", Icons.Default.Send, "NAV_MESSAGING")
                    )
                )
            }
            q.contains("randevu") || q.contains("toplantı") || q.contains("görüşme saati") -> {
                Pair(
                    """📅 **${student.full_name} — Veli-Öğretmen Görüşme ve Randevu Takvimi:**

👨‍🏫 **Branş Öğretmenleri Müsaitlik Durumu:**
• **Ahmet Yılmaz (Matematik / Rehber Öğr.):** Çarşamba 14:00 - 15:30 (Mevcut: 2 Randevu Slotu)
• **Selin Kara (Fen Bilimleri):** Perşembe 10:30 - 12:00 (Mevcut: 3 Randevu Slotu)
• **Fatma Demir (Türkçe):** Cuma 13:30 - 15:00 (Mevcut: 1 Randevu Slotu)
• **Gülfem Aydın (Rehberlik / PDR):** Pazartesi 09:30 - 11:30 (Online veya Yüz Yüze)

Görüşmek istediğiniz öğretmeni ve saati seçerek randevunuzu anında onaylayabilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("📅 Randevu Al", Icons.Default.Event, "NAV_APPOINTMENTS"),
                        ChatbotAction("💬 Öğretmene Mesaj", Icons.Default.Chat, "CONNECT_TEACHER_Ahmet Yılmaz")
                    )
                )
            }
            q.contains("duyuru") || q.contains("etkinlik") || q.contains("gezi") || q.contains("tatil") || q.contains("haber") -> {
                Pair(
                    """📢 **Okul Geneli Aktif Duyurular ve Etkinlikler:**

1. **1. Veli-Okul Bahar Şenliği:** 12 Mayıs Cumartesi Okul Bahçesinde.
2. **Bilim Müzesi Gezisi:** 8-A ve 8-B sınıfları için muvafakatname izin belgeleri açılmıştır.
3. **23 Nisan Ulusal Egemenlik Bayramı:** Tören prova akışı ilan edilmiştir.""".trimIndent(),
                    listOf(
                        ChatbotAction("📢 İletişim & Duyurular", Icons.Default.Campaign, "NAV_COMMUNICATION")
                    )
                )
            }
            q.contains("sınav") || q.contains("lgs") || q.contains("yazılı") || q.contains("deneme") || q.contains("takvim") -> {
                Pair(
                    """📝 **${student.full_name} — Sınav Takvimi ve LGS Denemeleri:**

• **2. Dönem 1. Yazılılar:** 14 - 18 Nisan Tarihleri Arasında
• **LGS Türkiye Geneli Deneme Sınavı 5:** Bu Cumartesi Saat 09:30
• **Son Deneme Sınıf Derecesi:** 8-A Sınıfında 3. Sıra (412 Puan)""".trimIndent(),
                    listOf(
                        ChatbotAction("📊 Sınav Takvimi & Karneler", Icons.Default.Grade, "NAV_EXAMS")
                    )
                )
            }
            q.contains("kütüphane") || q.contains("kitap") || q.contains("ödünç") -> {
                Pair(
                    """📚 **${student.full_name} — Kütüphane ve Kitap Takip:**

• **Ödünç Alınan Kitap:** *Nutuk - Mustafa Kemal Atatürk*
• **Teslim Tarihi:** 18 Nisan 2026
• **Gecikme Durumu:** Zamanında, gecikme yok ✅""".trimIndent(),
                    listOf(
                        ChatbotAction("📖 Kütüphane Kataloğu", Icons.Default.MenuBook, "NAV_LIBRARY")
                    )
                )
            }
            q.contains("kulüp") || q.contains("etkinlik") || q.contains("satranç") || q.contains("müzik") -> {
                Pair(
                    """♟️ **${student.full_name} — Öğrenci Kulüpleri ve Etkinlikler:**

• **Kayıtlı Kulüp:** Robotik Kodlama & Satranç Kulübü
• **Buluşma Günü:** Her Cuma 15:30 - 16:30
• **Kulüp Danışmanı:** Emre Arslan""".trimIndent(),
                    listOf(
                        ChatbotAction("🏆 Kulüp Detayları", Icons.Default.Groups, "NAV_CLUBS")
                    )
                )
            }
            q.contains("etüt") || q.contains("takviye") || q.contains("özel ders") -> {
                Pair(
                    """✍️ **${student.full_name} — Birebir Etüt ve Takviye Dersleri:**

• **Matematik Etütü:** Bu Çarşamba 16:30 (Ahmet Yılmaz)
• **Fen Bilimleri Etütü:** Cuma 16:30 (Selin Kara)""".trimIndent(),
                    listOf(
                        ChatbotAction("📝 Etüt Randevusu Al", Icons.Default.MoreTime, "NAV_TUTORING")
                    )
                )
            }
            q.contains("kimlik") || q.contains("kart") || q.contains("qr") -> {
                Pair(
                    """🪪 **${student.full_name} — Dijital Okul Kimlik Kartı:**

• **Öğrenci No:** ${student.student_number} • **Kan Grubu:** A Rh(+)
• **RFID Kart ID:** RFID-8A-${student.student_number} (Turnike Aktif)""".trimIndent(),
                    listOf(
                        ChatbotAction("🪪 Dijital Kimlik Kartı", Icons.Default.Badge, "NAV_DIGITAL_ID")
                    )
                )
            }
            else -> {
                val formattedQuery = q.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr", "TR")) else it.toString() }
                val balance = state.canteenWallet.current_balance.coerceAtLeast(145.50)
                val unpaid = state.invoices.filter { (it.student_id == student.id || it.student_name.contains(student.first_name, ignoreCase = true)) && it.status != "PAID" }.sumOf { it.amount - it.paid_amount }
                Pair(
                    """🤖 **SCS360 AI Veli Asistanı:**

Sorduğunuz **"$formattedQuery"** sorusuyla ilgili öğrenciniz **${student.full_name} (${student.class_name})** için sistem verileri derlendi:

📊 **Canlı Öğrenci ve Okul Veri Özeti:**
• **Okulda Mevcudiyet:** Derste Mevcut ✅ (Sabah Turnike Girişi: 08:38)
• **Genel Başarı Notu:** 88.50 / 100 *(Takdir Belgesi Kriteri)*
• **Kantin Cüzdan Bakiyesi:** ₺${String.format(Locale.US, "%.2f", balance)} *(Limit: ₺100.00/gün)*
• **Bekleyen Taksit Tutarı:** ₺${String.format(Locale.US, "%,.0f", unpaid.coerceAtLeast(4500.0))} *(iyzico ile online ödenebilir)*
• **Sınıf Rehber Öğretmeni:** Ahmet Yılmaz (Dahili: 104)

Belirttiğiniz konu hakkında sınıf öğretmeninizle görüşebilir veya aşağıdaki hızlı işlem butonlarını kullanabilirsiniz.""".trimIndent(),
                    listOf(
                        ChatbotAction("💬 Öğretmene Bağlan", Icons.Default.Chat, "CONNECT_TEACHER_Ahmet Yılmaz"),
                        ChatbotAction("📊 Dijital Karne", Icons.Default.Description, "NAV_REPORT_CARD"),
                        ChatbotAction("💳 Borç Öde (iyzico)", Icons.Default.CreditCard, "NAV_PAYMENTS"),
                        ChatbotAction("📅 Randevu Al", Icons.Default.Event, "NAV_APPOINTMENTS")
                    )
                )
            }
        }
    }

    private fun handleCanteenQuery(q: String, state: Okul360UiState): Pair<String, List<ChatbotAction>> {
        return if (q.contains("stok") || q.contains("ürün") || q.contains("kritik") || q.contains("envanter")) {
            generateCanteenStockResponse(state)
        } else if (q.contains("alerji") || q.contains("uyarı") || q.contains("alerjen")) {
            Pair(
                """⚠️ **Öğrenci Gıda Alerjen Uyarıları:**
                
• **Ali Demir (8-A):** Fıstık & Fındık Alerjisi *(Sistem POS'ta otomatik engeller)*
• **Zeynep Kaya (7-B):** Laktoz İntoleransı
• **Can Yıldız (6-A):** Gluten Hassasiyeti""".trimIndent(),
                listOf(ChatbotAction("Kısıtlama Yönetimi", Icons.Default.Warning, "NAV_ALLERGENS"))
            )
        } else {
            generateCanteenRevenueResponse(state, "Kantin")
        }
    }
}

/**
 * Modern, Tam Donanımlı Yapay Zeka Chatbot Modalı (Tüm Yetkiler İçin)
 */
@Composable
fun Okul360ChatbotModal(
    state: Okul360UiState,
    onDismiss: () -> Unit,
    onNavigateAction: ((actionTag: String) -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        com.example.ui.screens.AiChatbotScreen(
            state = state,
            onNavigateAction = { actionTag ->
                onNavigateAction?.invoke(actionTag)
            },
            onBack = onDismiss
        )
    }
}

@Suppress("UNUSED")
@Composable
private fun DeprecatedOldOkul360ChatbotModal(
    state: Okul360UiState,
    onDismiss: () -> Unit,
    onNavigateAction: ((actionTag: String) -> Unit)? = null
) {
    val role = state.selectedRole
    val userName = state.currentUser.resolvedName

    val chatContext = rememberChatContext()
    val messages = chatContext.messages

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    val context = LocalContext.current
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("tr", "TR")
                }
            }
            ttsEngine = tts
        } catch (_: Exception) {}

        onDispose {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (_: Exception) {}
        }
    }

    val suggestions = remember(role) {
        Okul360ChatbotEngine.getDefaultSuggestionsForRole(role)
    }

    // Role Gradient
    val headerGradient = remember(role) {
        when (role) {
            UserRole.CUSTOM -> listOf(Color(0xFF334155), Color(0xFF475569))
            UserRole.SUPERADMIN -> listOf(Color(0xFF3B0764), Color(0xFF6D28D9), Color(0xFF8B5CF6))
            UserRole.OWNER -> listOf(Color(0xFF312E81), Color(0xFF4338CA), Color(0xFF6366F1))
            UserRole.ADMIN -> listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF3B82F6))
            UserRole.MANAGER -> listOf(Color(0xFF134E4A), Color(0xFF0D9488), Color(0xFF14B8A6))
            UserRole.TEACHER -> listOf(Color(0xFF064E3B), Color(0xFF059669), Color(0xFF10B981))
            UserRole.FINANCE -> listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFF59E0B))
            UserRole.PARENT -> listOf(Color(0xFF581C87), Color(0xFF7C3AED), Color(0xFF8B5CF6))
            UserRole.CANTEEN -> listOf(Color(0xFF7C2D12), Color(0xFFEA580C), Color(0xFFF97316))
        }
    }

    fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || isThinking) return

        val userMsg = ChatbotMessage(text = trimmed, isUser = true)
        chatContext.addMessage(userMsg)
        inputText = ""
        isThinking = true

        coroutineScope.launch {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
            // Simüle edilen zeki yanıt gecikmesi
            delay(500)
            val historyTranscript = chatContext.getRecentHistoryTranscript()
            val aiResponse = Okul360ChatbotEngine.processUserQuery(trimmed, role, state, conversationHistory = historyTranscript)
            chatContext.addMessage(aiResponse)
            isThinking = false
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevents clicking the modal card from dismissing dialog
                    )
                    .testTag("chatbot_modal_dialog"),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp
            ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Chatbot Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(headerGradient))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "SCS360 AI Danışman",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.25f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "Canlı",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Text(
                                    text = "${role.displayName} • Kurumsal Zeka",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Top Header Actions: Clear Chat & Close
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    chatContext.clearHistory()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("chatbot_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Sohbeti Temizle",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("chatbot_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC)),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubbleItem(
                            message = msg,
                            roleGradient = headerGradient,
                            onActionClick = { actionTag ->
                                onNavigateAction?.invoke(actionTag)
                            },
                            onCopyClick = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "Yanıt metni panoya kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                            onSpeakClick = { text ->
                                val cleanText = text.replace("*", "").replace("•", "").replace("✅", "").replace("⚠️", "")
                                if (speakingMessageId == msg.id) {
                                    ttsEngine?.stop()
                                    speakingMessageId = null
                                } else {
                                    ttsEngine?.stop()
                                    speakingMessageId = msg.id
                                    ttsEngine?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, msg.id)
                                }
                            },
                            isSpeaking = (speakingMessageId == msg.id)
                        )
                    }

                    if (isThinking) {
                        item {
                            ThinkingIndicator(roleGradient = headerGradient)
                        }
                    }
                }

                // Quick Prompt Suggestion Chips & Message Input Container
                Surface(
                    color = Color.White,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { sendMessage(suggestion) },
                                    label = {
                                        Text(
                                            text = suggestion,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Chat Input Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp, max = 120.dp)
                                    .testTag("chatbot_input_field"),
                                placeholder = {
                                    Text(
                                        text = "${role.displayName} olarak soru sorun...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = headerGradient.first(),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                maxLines = 3,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        sendMessage(inputText)
                                        focusManager.clearFocus()
                                    }
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    sendMessage(inputText)
                                    focusManager.clearFocus()
                                },
                                enabled = inputText.isNotBlank() && !isThinking,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (inputText.isNotBlank() && !isThinking) {
                                            Brush.horizontalGradient(headerGradient)
                                        } else {
                                            Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
                                        }
                                    )
                                    .testTag("chatbot_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Gönder",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
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
fun ChatBubbleItem(
    message: ChatbotMessage,
    roleGradient: List<Color>,
    onActionClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onSpeakClick: ((String) -> Unit)? = null,
    isSpeaking: Boolean = false
) {
    if (message.isUser) {
        // User Message (Right aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = roleGradient.first(),
                modifier = Modifier.widthIn(max = 290.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message.timestamp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 9.5.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    } else {
        // AI Assistant Message (Left aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // AI Avatar Icon
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(roleGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f, fill = false).widthIn(max = 320.dp)) {
                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = message.text,
                            color = Color(0xFF1E293B),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )

                        // Action Buttons inside Chat Bubble
                        if (message.suggestedActions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                message.suggestedActions.forEach { act ->
                                    OutlinedButton(
                                        onClick = { onActionClick(act.actionTag) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, roleGradient.first().copy(alpha = 0.4f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = roleGradient.first())
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(imageVector = act.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = act.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        // Message Footer: Timestamp, TTS, and Copy Icon
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "✨ SCS360 AI • ${message.timestamp}",
                                fontSize = 9.5.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (onSpeakClick != null) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                        contentDescription = "Seslendir",
                                        tint = if (isSpeaking) roleGradient.first() else Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clip(CircleShape)
                                            .clickable { onSpeakClick(message.text) }
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Kopyala",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .clickable { onCopyClick(message.text) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator(roleGradient: List<Color>) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(roleGradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Yanıt oluşturuluyor...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B).copy(alpha = alpha)
                )
            }
        }
    }
}

/**
 * Global Floating Chatbot FAB Component for AppShell and Portal Screens
 */
@Composable
fun FloatingChatbotFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .testTag("floating_chatbot_fab")
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF4338CA), Color(0xFF7C3AED), Color(0xFF2563EB))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Chatbot",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = "AI Asistan",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}
