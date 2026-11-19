package com.example.ui.navigation

import com.example.data.model.UserRole
import com.example.ui.screens.ParentNavTab
import com.example.ui.viewmodel.Okul360UiState

internal sealed interface ChatbotDestination {
    data class Parent(val tab: ParentNavTab, val studentId: String?) : ChatbotDestination
    data class Screen(val screen: SubScreen) : ChatbotDestination
    data class Tab(val tab: MainTab) : ChatbotDestination
}

internal const val CHATBOT_MESSAGING = "NAV_MESSAGING"

internal fun chatbotDestination(state: Okul360UiState, tag: String, studentId: String? = null): ChatbotDestination? {
    val actor = state.currentUser
    if (!state.isLoggedIn || state.organizationAccess.status in setOf("RESTRICTED", "SUSPENDED")) return null
    if (tag == CHATBOT_MESSAGING && actor.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) return null
    if (state.selectedRole == UserRole.PARENT) {
        if (studentId != null && state.parentChildren.none { it.id == studentId }) return null
        val parent = when (tag) {
            CHATBOT_MESSAGING -> ParentNavTab.MESSAGING.takeIf { NativeNavigationPolicy.has(actor, "parent.send_message") }
            "VIEW_PAYMENTS", "VIEW_DEBTS" -> ParentNavTab.PAYMENTS.takeIf { NativeNavigationPolicy.has(actor, "parent.view_finances", "parent.view_finance") }
            "VIEW_HOMEWORK" -> ParentNavTab.MATERIALS.takeIf { NativeNavigationPolicy.has(actor, "parent.view_children") }
            else -> null
        }
        return parent?.let { ChatbotDestination.Parent(it, studentId) }
    }
    val tab = when (tag) {
        "VIEW_DASHBOARD" -> MainTab.HOME
        "VIEW_STUDENTS" -> MainTab.STUDENTS
        "VIEW_ATTENDANCE", "VIEW_ATTENDANCE_REPORT" -> MainTab.ATTENDANCE
        "VIEW_DEBTS", "VIEW_PAYMENTS", "VIEW_EXPENSES" -> MainTab.FINANCE
        "VIEW_NOTIFICATIONS" -> MainTab.NOTIFICATIONS
        else -> null
    }
    if (tab != null) {
        return tab.takeIf {
            state.selectedRole in setOf(UserRole.ADMIN, UserRole.SUPERADMIN, UserRole.CUSTOM) &&
                NativeNavigationPolicy.tab(actor, it)
        }?.let { ChatbotDestination.Tab(it) }
    }
    val screen = when (tag) {
        CHATBOT_MESSAGING -> SubScreen.DIRECT_MESSAGING
        "VIEW_GRADES" -> SubScreen.DIGITAL_REPORT_CARD
        "VIEW_HOMEWORK" -> SubScreen.HOMEWORK
        "VIEW_ANNOUNCEMENTS" -> SubScreen.COMMUNICATION
        "VIEW_CANTEEN", "VIEW_CANTEEN_BALANCE" -> SubScreen.CANTEEN
        "VIEW_REPORTS" -> SubScreen.REPORTS
        "VIEW_USERS" -> SubScreen.USERS
        else -> null
    }
    return screen?.takeIf { NativeNavigationPolicy.screen(actor, it) }?.let { ChatbotDestination.Screen(it) }
}
