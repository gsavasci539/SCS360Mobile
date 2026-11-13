package com.example.ui.navigation

import com.example.data.model.User

/** Same canonical permission codes as web navigation. Role names grant no powers. */
object NativeNavigationPolicy {
    fun has(user: User, vararg codes: String) = user.is_super_admin == true || codes.any { it in user.permissions }
    fun tab(user: User, tab: MainTab): Boolean = when (tab) {
        MainTab.HOME, MainTab.OTHER -> true
        MainTab.STUDENTS -> has(user, "students.read", "student.read")
        MainTab.ATTENDANCE -> has(user, "attendance.read")
        MainTab.NOTIFICATIONS -> has(user, "notification.read", "announcement.read")
        MainTab.FINANCE -> has(user, "finance.read")
        MainTab.AI_CHATBOT -> user.is_super_admin == true
    }
    fun screen(user: User, screen: SubScreen): Boolean = when (screen) {
        SubScreen.NONE, SubScreen.SUBSCRIPTION_BILLING -> true
        SubScreen.PARENTS -> has(user, "parents.read")
        SubScreen.CLASSES, SubScreen.SCHEDULE -> has(user, "classes.read", "class.read")
        SubScreen.COMMUNICATION -> has(user, "announcement.read")
        SubScreen.NOTIFICATIONS -> has(user, "notification.read")
        SubScreen.DIGITAL_REPORT_CARD, SubScreen.EXAMS -> has(user, "exam.read", "exam.write")
        SubScreen.COURSE_MATERIALS -> has(user, "material.read")
        SubScreen.DIRECT_MESSAGING -> has(user, "messaging.read")
        SubScreen.REPORTS -> has(user, "report.read")
        SubScreen.RESOURCES -> has(user, "inventory.read")
        SubScreen.ROLES, SubScreen.USERS, SubScreen.STAFF -> has(user, "users.read")
        SubScreen.DAILY_REPORTS -> has(user, "daily_reports.read")
        SubScreen.DIGITAL_ID_CARD -> has(user, "security.read")
        SubScreen.APPOINTMENTS -> has(user, "appointments.read")
        SubScreen.TRANSPORT -> has(user, "bus.read")
        SubScreen.CANTEEN, SubScreen.CANTEEN_PORTAL -> has(user, "canteen.read")
        SubScreen.HOMEWORK -> has(user, "homework.read")
        SubScreen.LIBRARY -> has(user, "library.read")
        SubScreen.ORGANIZATIONS -> has(user, "org.read", "branch.read")
        SubScreen.RFID_DEVICES, SubScreen.SETTINGS -> has(user, "settings.read")
        SubScreen.CONSENTS -> has(user, "consents.read")
        SubScreen.GUIDANCE -> has(user, "guidance.read")
        SubScreen.INFIRMARY -> has(user, "health.read")
        SubScreen.CLUBS -> has(user, "clubs.read")
        SubScreen.SURVEYS -> has(user, "surveys.read", "surveys.respond")
        SubScreen.TUTORING -> has(user, "tutoring.read")
        SubScreen.PAYROLL -> has(user, "payroll.read")
        SubScreen.SECURITY -> has(user, "audit.read")
        SubScreen.OUTBOX -> has(user, "notification.read")
        SubScreen.DISMISSAL -> has(user, "dismissal.read")
        SubScreen.AI_ANALYTICS, SubScreen.AI_CHAT_AUDIT, SubScreen.PLATFORM_SAAS -> user.is_super_admin == true
    }
}
