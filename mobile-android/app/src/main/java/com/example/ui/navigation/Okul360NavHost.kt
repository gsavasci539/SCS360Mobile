package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.data.model.Invoice
import com.example.data.model.UserRole
import com.example.ui.screens.*
import com.example.ui.viewmodel.Okul360UiState
import com.example.ui.viewmodel.Okul360ViewModel

/**
 * Jetpack Compose NavigationHost managing seamless animated transitions
 * between student list, finance overview, announcement screens, and all management portals.
 */
@Composable
fun Okul360NavHost(
    navController: NavHostController,
    state: Okul360UiState,
    viewModel: Okul360ViewModel,
    modifier: Modifier = Modifier,
    onViewReceipt: (Invoice) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        // 1. Dashboard Destination
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                state = state,
                onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                onNavigateToStudents = { navController.navigate(Screen.StudentList.route) },
                onNavigateToFinance = { navController.navigate(Screen.FinanceOverview.route) },
                onNavigateToAnnouncements = { navController.navigate(Screen.Announcements.route) },
                onNavigateToDailyReports = { navController.navigate(Screen.DailyReports.route) },
                onNavigateToClasses = { navController.navigate(Screen.Classes.route) },
                onNavigateToCommunication = { navController.navigate(Screen.Communication.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                onNavigateToResources = { navController.navigate(Screen.Resources.route) },
                onNavigateToRoles = { navController.navigate(Screen.Roles.route) },
                onNavigateToUsers = { navController.navigate(Screen.Users.route) },
                onOpenAddStudent = { viewModel.openAddStudentDialog(true) },
                onOpenCreateAnnouncement = { viewModel.openCreateAnnouncement(true) },
                onNavigateToDigitalId = { navController.navigate(Screen.DigitalIdCard.route) },
                onNavigateToSchedule = { navController.navigate(Screen.WeeklySchedule.route) },
                onNavigateToAppointments = { navController.navigate(Screen.Appointments.route) },
                onNavigateToTransport = { navController.navigate(Screen.Transport.route) },
                onNavigateToCanteen = { navController.navigate(Screen.CanteenWallet.route) },
                onNavigateToHomework = { navController.navigate(Screen.HomeworkGrading.route) },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToStaff = { navController.navigate(Screen.StaffManagement.route) }
            )
        }

        // 2. Student List Destination
        composable(Screen.StudentList.route) {
            StudentsScreen(
                state = state,
                onSearchChange = { viewModel.onSearchQueryChanged(it) },
                onFilterClassChange = { viewModel.onFilterClassChanged(it) },
                onFilterAcademicYearChange = { viewModel.onFilterAcademicYearChanged(it) },
                onSelectStudent = { viewModel.selectStudent(it) },
                onOpenAddStudent = { viewModel.openAddStudentDialog(true) },
                onUpdateStudent = { viewModel.updateStudent(it) },
                onDeleteStudent = { viewModel.deleteStudent(it) },
                                onRefresh = { viewModel.loadStudents() }
                            )
        }

        // 3. Attendance Destination
        composable(Screen.Attendance.route) {
            AttendanceScreen(
                state = state,
                onClassSelected = { viewModel.selectAttendanceClass(it) },
                onStatusChanged = { stdId, status -> viewModel.updateSingleAttendanceStatus(stdId, status) },
                onMarkAllPresent = { viewModel.markAllPresent() },
                onSaveAttendance = { viewModel.saveAttendance() },
                                onDateSelected = { viewModel.selectAttendanceDate(it) },
                                onRetry = { viewModel.retryAttendance() },
                                onNoteChanged = { id, note -> viewModel.updateAttendanceNote(id, note) }
            )
        }

        // 4. Finance Overview Destination
        composable(Screen.FinanceOverview.route) {
            FinanceScreen(
                                onRetry = { viewModel.loadFinance() },
                state = state,
                onFilterChanged = { viewModel.setInvoiceFilter(it) },
                onSelectInvoiceForPayment = { viewModel.selectInvoiceForPayment(it) },
                onAddInvoice = { stdId, stdName, clsName, amt, due, desc ->
                    viewModel.addInvoice(stdId, stdName, clsName, amt, due, desc)
                },
                onUpdateInvoice = { viewModel.updateInvoice(it) },
                onDeleteInvoice = { viewModel.deleteInvoice(it) },
                onViewReceipt = onViewReceipt,
                onAddExpense = { cat, title, amt, date, rec ->
                    viewModel.addExpense(cat, title, amt, date, rec)
                },
                onDeleteExpense = { viewModel.deleteExpense(it) }
            )
        }

        // 5. Announcements / Communication Destination
        composable(Screen.Announcements.route) {
            CommunicationScreen(
                state = state,
                onCreateAnnouncement = { title, content, cat, tgt ->
                    viewModel.createAnnouncement(title, content, cat, tgt)
                },
                onDeleteAnnouncement = { id ->
                    viewModel.deleteAnnouncement(id)
                },
                onSendNotification = { title, msg, tgt, chan ->
                    viewModel.sendNotification(title, msg, tgt, chan)
                },
                onDeleteNotification = { id ->
                    viewModel.deleteNotification(id)
                },
                onCreateTemplate = { title, type, content ->
                    viewModel.createTemplate(title, type, content)
                },
                onDeleteTemplate = { id ->
                    viewModel.deleteTemplate(id)
                },
                onTriggerPushTest = { title, msg, chan ->
                    viewModel.triggerCustomPush(title, msg, chan)
                },
                onTriggerDelayedPushTest = { delaySec, title, msg, chan ->
                    viewModel.triggerDelayedPush(delaySec, title, msg, chan)
                },
                onOpenNotificationSettings = {
                    viewModel.openDeviceNotificationSettings()
                }
            )
        }

        // Communication Route Alias
        composable(Screen.Communication.route) {
            CommunicationScreen(
                state = state,
                onCreateAnnouncement = { title, content, cat, tgt ->
                    viewModel.createAnnouncement(title, content, cat, tgt)
                },
                onDeleteAnnouncement = { id ->
                    viewModel.deleteAnnouncement(id)
                },
                onSendNotification = { title, msg, tgt, chan ->
                    viewModel.sendNotification(title, msg, tgt, chan)
                },
                onDeleteNotification = { id ->
                    viewModel.deleteNotification(id)
                },
                onCreateTemplate = { title, type, content ->
                    viewModel.createTemplate(title, type, content)
                },
                onDeleteTemplate = { id ->
                    viewModel.deleteTemplate(id)
                },
                onTriggerPushTest = { title, msg, chan ->
                    viewModel.triggerCustomPush(title, msg, chan)
                },
                onTriggerDelayedPushTest = { delaySec, title, msg, chan ->
                    viewModel.triggerDelayedPush(delaySec, title, msg, chan)
                },
                onOpenNotificationSettings = {
                    viewModel.openDeviceNotificationSettings()
                }
            )
        }

        // 6. Classes Management Destination
        composable(Screen.Classes.route) {
            ClassesScreen(
                state = state,
                onAddClass = { viewModel.addClass(it) },
                onUpdateClass = { cls ->
                    viewModel.updateClass(cls)
                },
                onDeleteClass = { clsId ->
                    viewModel.deleteClass(clsId)
                },
                onAssignStudent = { stdId, clsId ->
                    viewModel.assignStudentToClass(stdId, clsId)
                },
                onRemoveStudent = { stdId ->
                    viewModel.removeStudentFromClass(stdId)
                },
                onSelectClassDetail = { cls ->
                    viewModel.selectClassDetail(cls)
                }
            )
        }

        // 7. Reports & Analytics Destination
        composable(Screen.Reports.route) {
            ReportsScreen(
                state = state,
                onSendReminder = { invId, stdName ->
                    viewModel.sendPaymentReminder(invId, stdName)
                }
            )
        }

        // 8. Resources & Inventory Destination
        composable(Screen.Resources.route) {
            ResourcesScreen(
                state = state,
                onAddResource = { name, cat, loc, status, qty, notes ->
                    viewModel.addResource(name, cat, loc, status, qty, notes)
                },
                onDeleteResource = { id ->
                    viewModel.deleteResource(id)
                }
            )
        }

        // 9. Roles & Permissions Destination
        composable(Screen.Roles.route) {
            RolesScreen(
                state = state,
                onRoleSelected = { viewModel.switchRole(it) },
                onTogglePermission = { roleId, permCode, isGranted ->
                    viewModel.togglePermission(roleId, permCode, isGranted)
                },
                onCreateRole = { name, code, desc ->
                    viewModel.createRole(name, code, desc)
                },
                onDeleteRole = { id ->
                    viewModel.deleteRole(id)
                }
            )
        }

        // 10. Staff & Users Management Destination
        composable(Screen.Users.route) {
            UsersScreen(
                state = state,
                onAddUser = { full, email, role, phone, branch, password ->
                    viewModel.addUser(full, email, role, phone, branch, password)
                },
                onDeleteUser = { id ->
                    viewModel.deleteUser(id)
                },
                onResetPassword = { id, newPass ->
                    viewModel.resetUserPassword(id, newPass)
                }
            )
        }

        // 11. Daily Reports Destination
        composable(Screen.DailyReports.route) {
            DailyReportsScreen(
                state = state
            )
        }

        // 12. Digital Student/Parent ID Card Destination
        composable(Screen.DigitalIdCard.route) {
            DigitalIdCardScreen(
                state = state,
                onBack = { navController.popBackStack() }
            )
        }

        // 13. Weekly Schedule Destination
        composable(Screen.WeeklySchedule.route) {
            WeeklyScheduleScreen(
                state = state,
                onBack = { navController.popBackStack() }
            )
        }

        // 14. Appointments (PTC) Destination
        composable(Screen.Appointments.route) {
            AppointmentsScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onBookAppointment = { teacherId, teacherName, subject, date, timeSlot, type, notes ->
                    viewModel.bookAppointment(teacherId, teacherName, subject, date, timeSlot, type, notes)
                },
                onUpdateStatus = { aptId, newStatus, feedback ->
                    viewModel.updateAppointmentStatus(aptId, newStatus, feedback)
                }
            )
        }

        // 15. Transport & Bus Tracking Destination
        composable(Screen.Transport.route) {
            TransportScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onStudentCheckIn = { routeId, studentId, isMorning, isBoarded ->
                    viewModel.updateBusStudentCheckIn(routeId, studentId, isMorning, isBoarded)
                },
                onUpdateTelemetry = { routeId, lat, lng, speed, passedStop, nextStop, eta ->
                    viewModel.updateBusLiveTelemetry(routeId, lat, lng, speed, passedStop, nextStop, eta)
                }
            )
        }

        // 16. Canteen & Digital Wallet Destination
        composable(Screen.CanteenWallet.route) {
            CanteenWalletScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onTopup = { amount, method ->
                    viewModel.topupCanteenWallet(amount, method)
                },
                onUpdateRestrictions = { limit, categories, allergens ->
                    viewModel.updateCanteenRestrictions(limit, categories, allergens)
                },
                onProcessPurchase = { product, quantity ->
                    viewModel.processCanteenPOSPurchase(product, quantity)
                }
            )
        }

        // 17. Homework Grading & Submissions Destination
        composable(Screen.HomeworkGrading.route) {
            HomeworkGradingScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onSubmitHomework = { matId, title, text, fileName, fileSize ->
                    viewModel.submitHomework(matId, title, text, fileName, fileSize)
                },
                onGradeSubmission = { subId, score, feedback ->
                    viewModel.gradeHomeworkSubmission(subId, score, feedback)
                }
            )
        }

        // 18. Library Catalog & Book Loans Destination
        composable(Screen.Library.route) {
            NativeLibraryScreen(state, onNavigateBack = { navController.popBackStack() })
        }

        // 19. Staff Management & Duty Roster Destination
        composable(Screen.StaffManagement.route) {
            StaffManagementScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onSubmitLeaveRequest = { type, start, end, days, reason ->
                    viewModel.submitTeacherLeaveRequest(type, start, end, days, reason)
                },
                onReviewLeaveRequest = { id, approved, comment ->
                    viewModel.reviewTeacherLeaveRequest(id, approved, comment)
                }
            )
        }

        // 20. AI Analytics & Intelligence Hub Destination
        composable(Screen.AiAnalytics.route) {
            AiAnalyticsHubScreen(
                state = state,
                onGenerateOwnerForecast = { viewModel.generateAiOwnerForecast() },
                onGenerateBranchHealth = { viewModel.generateAiBranchHealthReport() },
                onGenerateChurnRisk = { viewModel.generateAiStudentChurnRisk() },
                onOpenHomeworkGenerator = {
                    viewModel.generateAiTeacherHomework(
                        subject = "Matematik",
                        topic = "Birinci Dereceden Denklemler",
                        gradeLevel = "8. Sınıf",
                        questionCount = 3,
                        difficulty = "Orta"
                    )
                },
                onOpenNarrativeGenerator = {
                    viewModel.generateAiTeacherDailyNarrative(
                        studentName = "Ali Demir",
                        meal = "Tam Bitirdi",
                        sleep = "1.5 Saat Uyudu",
                        mood = "Neşeli & Paylaşımcı",
                        activities = "Robotik Kodlama ve Resim"
                    )
                },
                onOpenParentDigest = {
                    val student = state.students.firstOrNull() ?: com.example.data.model.Student(id = "std-1", first_name = "Ali", last_name = "Demir", student_number = "101", class_name = "8-A")
                    viewModel.generateAiParentWeeklyDigest(student)
                },
                onSelectReport = { report ->
                    viewModel.openAiReportDetail(report)
                }
            )
        }

        // 22. AI Chat Audit & LLM Training Destination
        composable(Screen.AiChatAudit.route) {
            AiChatAuditScreen(
                state = state,
                onNavigateBack = { navController.popBackStack() },
                onSwitchToSuperAdmin = { viewModel.switchRole(UserRole.SUPERADMIN) }
            )
        }

        // 21. Settings Destination
        composable(Screen.Settings.route) {
            SettingsScreen(
                state = state,
                onRoleSelected = { viewModel.switchRole(it) },
                onTestBackendConnection = { viewModel.testBackendConnection() },
                onUpdateServerConfig = { url, tenant -> viewModel.updateServerSettings(url, tenant) },
                onOpenNotificationSettings = { viewModel.openDeviceNotificationSettings() },
                onTestNotification = {
                    viewModel.triggerCustomPush("Push Bildirim Testi", "Android push bildirim sistemi başarıyla çalışıyor.", "ANNOUNCEMENT")
                },
                onClearLocalCache = { viewModel.clearLocalCache() },
                onLogout = {
                    viewModel.logout()
                }
            )
        }
    }
}
