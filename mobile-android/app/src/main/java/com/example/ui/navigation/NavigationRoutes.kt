package com.example.ui.navigation

/**
 * Screen Route definitions for Jetpack Compose NavigationHost
 */
sealed class Screen(val route: String, val title: String) {
    data object Login : Screen("login", "Giriş")
    data object Dashboard : Screen("dashboard", "Genel Bakış")
    data object StudentList : Screen("student_list", "Öğrenci Listesi")
    data object Attendance : Screen("attendance", "Yoklama & Devamsızlık")
    data object FinanceOverview : Screen("finance_overview", "Finans & Aidat Yönetimi")
    data object Announcements : Screen("announcements", "Duyurular & İletişim")
    data object Classes : Screen("classes", "Sınıflar & Şubeler")
    data object Communication : Screen("communication", "İletişim & Bildirim Merkezi")
    data object Reports : Screen("reports", "Raporlar & Analitik")
    data object Resources : Screen("resources", "Kaynaklar & Envanter")
    data object Roles : Screen("roles", "Roller & Yetki Matrisi")
    data object Users : Screen("users", "Kullanıcılar & Personel")
    data object DailyReports : Screen("daily_reports", "Günlük Karne & Gelişim")
    data object DigitalIdCard : Screen("digital_id_card", "Dijital Kimlik Kartı")
    data object WeeklySchedule : Screen("weekly_schedule", "Haftalık Ders Programı")
    data object Appointments : Screen("appointments", "Randevu & Veli Görüşmesi")
    data object Transport : Screen("transport", "Servis & Ulaşım Takibi")
    data object CanteenWallet : Screen("canteen_wallet", "Kantin & Öğrenci Cüzdanı")
    data object HomeworkGrading : Screen("homework_grading", "Ödev & Notlandırma")
    data object Library : Screen("library", "Kütüphane & Kitap Takibi")
    data object StaffManagement : Screen("staff_management", "Personel İK & Nöbet Çizelgesi")
    data object AiAnalytics : Screen("ai_analytics", "Yapay Zeka (AI) Analiz & Raporlar")
    data object AiChatAudit : Screen("ai_chat_audit", "AI Chat Analitik & LLM Eğitim Merkezi")
    data object PlatformSaas : Screen("platform_saas", "Platform & SaaS Üyelik Yönetimi")
    data object SubscriptionBilling : Screen("subscription_billing", "Üyelik, Kota & Ödeme")
    data object Dismissal : Screen("dismissal", "Öğrenci Teslim Masası")
    data object Settings : Screen("settings", "Sistem & Backend Ayarları")
}
