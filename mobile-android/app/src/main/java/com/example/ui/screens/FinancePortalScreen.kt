package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.PaymentStatusPieChart
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.RoleSpeedDialFab
import com.example.ui.components.RoleSwitchBadge
import com.example.ui.components.SpeedDialAction
import com.example.ui.dialogs.ReceiptDialog
import com.example.ui.dialogs.ceppos.CepPosPaymentDialog
import com.example.ui.dialogs.iyzico.IyzicoPaymentDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class FinanceNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    SUMMARY("Kasa", Icons.Default.AccountBalance, Icons.Outlined.AccountBalance),
    INVOICES("Taksit", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong),
    DUNNING("Gecikenler", Icons.Default.WarningAmber, Icons.Outlined.WarningAmber),
    EXPENSES("Giderler", Icons.Default.Paid, Icons.Outlined.Paid),
    AI_CHATBOT("AI Asistan", Icons.Default.AutoAwesome, Icons.Outlined.AutoAwesome),
    PROFILE("Rapor", Icons.Default.Summarize, Icons.Outlined.Summarize)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancePortalScreen(
    state: Okul360UiState,
    onRecordPayment: (invoiceId: String, amount: Double, method: String) -> Unit,
    onCreateInvoice: (studentId: String, title: String, amount: Double, dueDate: String, desc: String) -> Unit,
    onSendReminder: (invoiceId: String, studentName: String) -> Unit,
    onRoleSelected: (UserRole) -> Unit = {},
    onOpenChatbot: (() -> Unit)? = null,
    onLogout: () -> Unit,
    onRetry: () -> Unit = {}
) {
    val presentationScope = com.example.ui.components.financePresentationScope(state)
    var selectedTab by remember(presentationScope) { mutableStateOf(FinanceNavTab.SUMMARY) }
    var showExpenseDialog by remember(presentationScope) { mutableStateOf(false) }
    var deletingExpense by remember(presentationScope) { mutableStateOf<Expense?>(null) }
    var editingExpense by remember(presentationScope) { mutableStateOf<String?>(null) }
    var showNewInvoiceDialog by remember(presentationScope) { mutableStateOf(false) }
    var showPaymentDialogForInvoice by remember(presentationScope) { mutableStateOf<Invoice?>(null) }
    var viewingReceipt by remember(presentationScope) { mutableStateOf<Invoice?>(null) }
    var financeFabExpanded by remember(presentationScope) { mutableStateOf(false) }

    val financeActions = remember(presentationScope, state.invoices) {
        listOf(
            SpeedDialAction(
                id = "finance_quick_collect",
                title = "Hızlı Tahsilat / POS",
                description = "Bekleyen faturadan anında tahsilat al",
                icon = Icons.Default.Payments,
                containerColor = Color(0xFF2563EB), // Blue
                testTag = "finance_fab_action_collect",
                onClick = {
                    financeFabExpanded = false
                    val firstUnpaid = state.invoices.firstOrNull { !it.status.equals("PAID", ignoreCase = true) && !it.status.equals("ODENDI", ignoreCase = true) }
                    if (firstUnpaid != null) {
                        showPaymentDialogForInvoice = firstUnpaid
                    } else {
                        selectedTab = FinanceNavTab.INVOICES
                    }
                }
            ),
            SpeedDialAction(
                id = "finance_new_invoice",
                title = "Fatura & Taksit Oluştur",
                description = "Öğrenciye yeni taksit veya ek ücret tanımla",
                icon = Icons.Default.ReceiptLong,
                containerColor = Color(0xFF059669), // Emerald
                testTag = "finance_fab_action_invoice",
                onClick = {
                    financeFabExpanded = false
                    showNewInvoiceDialog = true
                }
            ),
            SpeedDialAction(
                id = "finance_send_dunning",
                title = "Gecikenleri Hatırlat",
                description = "Vadesi geçmiş alacakları yönet ve uyar",
                icon = Icons.Default.WarningAmber,
                containerColor = Color(0xFFD97706), // Amber
                testTag = "finance_fab_action_dunning",
                onClick = {
                    financeFabExpanded = false
                    selectedTab = FinanceNavTab.DUNNING
                }
            )
        )
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("tr", "TR")).apply {
            maximumFractionDigits = 0
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(AccentAmber, Color(0xFFEA580C))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Muhasebe & Finans Portalı",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = BrandNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${state.currentUser.full_name} • ${state.currentUser.institution_name}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        RoleSwitchBadge(
                            selectedRole = state.selectedRole,
                            onRoleSelected = onRoleSelected
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                .clickable { onLogout() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Çıkış",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                FinanceNavTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab && tab != FinanceNavTab.AI_CHATBOT
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (tab == FinanceNavTab.AI_CHATBOT) {
                                onOpenChatbot?.invoke()
                            } else {
                                selectedTab = tab
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(21.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentAmber,
                            selectedTextColor = AccentAmber,
                            indicatorColor = AccentAmber.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                FinanceNavTab.SUMMARY -> FinanceSummaryTab(
                    state = state,
                    currencyFormatter = currencyFormatter,
                    onOpenNewInvoice = { showNewInvoiceDialog = true },
                    onSelectTab = { selectedTab = it },
                    onRetry = onRetry
                )
                FinanceNavTab.INVOICES -> FinanceInvoicesTab(
                    state = state,
                    currencyFormatter = currencyFormatter,
                    onOpenPayment = { showPaymentDialogForInvoice = it },
                    onOpenNewInvoice = { showNewInvoiceDialog = true },
                    onViewReceipt = { viewingReceipt = it }
                )
                FinanceNavTab.DUNNING -> FinanceDunningTab(
                    state = state,
                    currencyFormatter = currencyFormatter,
                    onSendReminder = onSendReminder
                )
                FinanceNavTab.EXPENSES -> FinanceExpensesTab(
                    state = state, currencyFormatter = currencyFormatter,
                    onNew = { showExpenseDialog = true }, onRetry = onRetry,
                    onDelete = { deletingExpense = it }, onEdit = { editingExpense=it.id }
                )
                FinanceNavTab.PROFILE -> FinanceReportsAndProfileTab(
                    state = state,
                    currencyFormatter = currencyFormatter,
                    onLogout = onLogout
                )
                FinanceNavTab.AI_CHATBOT -> {}
            }

            // Role-specific Floating Action Button for Finance
            RoleSpeedDialFab(
                actions = financeActions,
                expanded = financeFabExpanded,
                onExpandedChange = { financeFabExpanded = it },
                fabColor = Color(0xFFD97706),
                fabLabel = "Finans Hızlı Aksiyon",
                testTag = "finance_speed_dial_fab",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            )
        }
    }

    // Both finance entry points use the same canonical native collection editor.
    showPaymentDialogForInvoice?.let { invoice ->
        com.example.ui.dialogs.NativeCashCollectionDialog(invoice.id, state.currentUser.id,
            canCollect = state.currentUser.permissions.any { it in setOf("finance.write", "finance.collect_payment") } || (state.currentUser.is_super_admin == true && !state.workspaceOrganizationId.isNullOrBlank()),
            onDismiss = { showPaymentDialogForInvoice = null },
            onRecorded = { showPaymentDialogForInvoice = null; onRetry() })
    }

    // Native bounded form; no invented amounts or dates.
    if (showExpenseDialog) {
        com.example.ui.dialogs.FinanceExpenseCreateDialog(state, onDismiss = { showExpenseDialog = false },
            onRecorded = { showExpenseDialog = false; onRetry() })
    }
    deletingExpense?.let { expense ->
        com.example.ui.dialogs.FinanceExpenseDeleteDialog(expense,state,
            onDismiss={ deletingExpense=null },onRecorded={ deletingExpense=null; onRetry() },
            onCheck={ deletingExpense=null; onRetry() })
    }
    editingExpense?.let { id ->
        com.example.ui.dialogs.FinanceExpenseEditDialog(id,state,onDismiss={ editingExpense=null },onRefresh={ editingExpense=null; onRetry() })
    }
    if (showNewInvoiceDialog) {
        com.example.ui.dialogs.FinanceInvoiceDraftDialog(state,
            onDismiss = { showNewInvoiceDialog = false },
            onRecorded = { showNewInvoiceDialog = false; onRetry() })
    }

    // Receipt Modal
    viewingReceipt?.let { receipt ->
        ReceiptDialog(
            invoice = receipt,
            onDismiss = { viewingReceipt = null },
            onPrintOrShare = { viewingReceipt = null }
        )
    }
}

// -------------------------------------------------------------
// 1. FINANCE SUMMARY TAB
// -------------------------------------------------------------
@Composable
private fun FinanceSummaryTab(
    state: Okul360UiState,
    currencyFormatter: NumberFormat,
    onOpenNewInvoice: () -> Unit,
    onSelectTab: (FinanceNavTab) -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { com.example.ui.components.RecordedFinanceSummaryCard(state.recordedFinanceSummary, state.isFinanceLoading,
            state.financeSummaryError, onRetry) }
        state.financeRowsError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        item { Button(onClick = { onSelectTab(FinanceNavTab.INVOICES) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Taksit kayıtları") } }
    }
}

@Composable
private fun FinanceMiniMetric(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// -------------------------------------------------------------
// 2. FINANCE INVOICES TAB
// -------------------------------------------------------------
@Composable
private fun FinanceInvoicesTab(
    state: Okul360UiState,
    currencyFormatter: NumberFormat,
    onOpenPayment: (Invoice) -> Unit,
    onOpenNewInvoice: () -> Unit,
    onViewReceipt: (Invoice) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredInvoices = remember(state.invoices, searchQuery, selectedFilter, state.students) {
        state.invoices.filter { inv ->
            val std = state.students.find { it.id == inv.student_id }
            val parentName = std?.parent_name ?: ""
            val matchesQuery = inv.student_name.contains(searchQuery, ignoreCase = true) ||
                    parentName.contains(searchQuery, ignoreCase = true) ||
                    inv.invoice_number.contains(searchQuery, ignoreCase = true)
            val isPaid = inv.status.equals("PAID", ignoreCase = true) || inv.status.equals("ODENDI", ignoreCase = true) || (inv.paid_amount >= inv.amount && inv.amount > 0)
            val isOverdue = inv.status.equals("OVERDUE", ignoreCase = true) || inv.status.equals("GECIKTI", ignoreCase = true)
            val isPending = inv.status.equals("PENDING", ignoreCase = true) || inv.status.equals("BEKLIYOR", ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "PAID" -> isPaid
                "PENDING" -> isPending
                "OVERDUE" -> isOverdue
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("💳 Taksitler & Tahsilat Yönetimi", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandNavy)
                    Text("Tüm öğrenci taksit planları ve tahsilat kayıtları", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = onOpenNewInvoice,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yeni Taksit", fontSize = 12.sp)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Öğrenci, veli veya fatura no ara...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Filter Chips
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Pair("ALL", "Tümü (${state.invoices.size})"),
                    Pair("PENDING", "Bekleyen"),
                    Pair("OVERDUE", "Geciken"),
                    Pair("PAID", "Ödenen")
                ).forEach { (code, label) ->
                    val isSel = selectedFilter == code
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedFilter = code },
                        label = { Text(label, fontSize = 11.5.sp) }
                    )
                }
            }
        }

        // Invoices List
        items(filteredInvoices) { inv ->
            val isPaid = inv.status == "PAID"
            val std = state.students.find { it.id == inv.student_id }
            val parentName = std?.parent_name ?: "Veli"
            val parentPhone = std?.parent_phone ?: ""
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(inv.student_name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = BrandNavy)
                            Text("Veli: $parentName • $parentPhone", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            color = if (isPaid) SecondaryEmerald.copy(alpha = 0.15f) else if (inv.status == "OVERDUE") MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isPaid) "ÖDENDİ" else if (inv.status == "OVERDUE") "GECİKTİ" else "BEKLİYOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaid) SecondaryEmerald else if (inv.status == "OVERDUE") MaterialTheme.colorScheme.error else AccentAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(inv.description, fontSize = 13.sp, color = BrandNavy)
                    Text("Vade Tarihi: ${inv.due_date} • Tahakkuk: ${inv.id}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            currencyFormatter.format(inv.amount),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = BrandNavy
                        )

                        if (!isPaid) {
                            Button(
                                onClick = { onOpenPayment(inv) },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tahsilat Al", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onViewReceipt(inv) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Makbuz Gör", fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. FINANCE DUNNING TAB (Geciken Ödemeler & Hatırlatma)
// -------------------------------------------------------------
@Composable
private fun FinanceDunningTab(
    state: Okul360UiState,
    currencyFormatter: NumberFormat,
    onSendReminder: (String, String) -> Unit
) {
    val context = LocalContext.current
    val overdueInvoices = remember(state.invoices) {
        state.invoices.filter { it.status == "OVERDUE" || (it.status == "PENDING" && it.due_date < "2026-08-15") }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("⚠️ Geciken Ödemeler & Hatırlatma", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
            Text("Vadesi geçmiş borçlular ve tek tıkla SMS/WhatsApp hatırlatma servisi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (overdueInvoices.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tebrikler! Vadesi geçmiş taksit bulunmuyor.", modifier = Modifier.padding(16.dp), color = SecondaryEmerald, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            items(overdueInvoices) { inv ->
                val std = state.students.find { it.id == inv.student_id }
                val parentName = std?.parent_name ?: "Veli"
                val parentPhone = std?.parent_phone ?: "0535 000 0000"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 1.dp,
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(inv.student_name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                                Text("Veli: $parentName • $parentPhone", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                currencyFormatter.format(inv.amount),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${inv.description} • Vade: ${inv.due_date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onSendReminder(inv.id, inv.student_name) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SMS Hatırlatması", fontSize = 11.5.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val phone = parentPhone
                                    val msg = "Sayın $parentName, ${inv.student_name} için ${currencyFormatter.format(inv.amount)} tutarındaki okul taksitinizin vadesi geçmiştir. Bilginize sunarız. SCS360"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(msg)}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. FINANCE EXPENSES TAB
// -------------------------------------------------------------
@Composable
private fun FinanceExpensesTab(state: Okul360UiState, currencyFormatter: NumberFormat, onNew: () -> Unit, onRetry: () -> Unit, onDelete: (Expense) -> Unit, onEdit: (Expense) -> Unit) {
    val canWrite="finance.write" in state.currentUser.permissions || (state.currentUser.is_super_admin==true && !state.workspaceOrganizationId.isNullOrBlank())
    LazyColumn(Modifier.fillMaxSize().testTag("finance_expenses_list"), contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text("Giderler & Harcamalar",fontSize=18.sp,fontWeight=FontWeight.Bold) }
        if(canWrite) item { Button(onClick=onNew,modifier=Modifier.heightIn(min=48.dp).testTag("finance_new_expense")) { Text("Yeni gider") } }
        if(state.isFinanceLoading || state.isFinanceRowsLoading) item { Text("Gider kayıtları yükleniyor…") }
        else if(state.financeRowsError!=null || state.financeSummaryError!=null) item { Text(state.financeRowsError ?: state.financeSummaryError.orEmpty()); TextButton(onClick=onRetry,modifier=Modifier.heightIn(min=48.dp)) { Text("Yeniden yükle") } }
        else if(state.expenses.isEmpty()) item { Text("Bu kapsamda gider kaydı yok.") }
        else items(state.expenses,key={ it.id }) { expense ->
            Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),tonalElevation=1.dp) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(expense.title,fontWeight=FontWeight.Bold)
                    Text("${expense.category} • ${expense.date}")
                    Text(currencyFormatter.format(expense.amount),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.error)
                    expense.notes?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    com.example.ui.components.expenseStatusLabel(expense.status)?.let { Text("Kaydın durumu: $it") }
                    if(canWrite) TextButton(onClick={ onEdit(expense) },modifier=Modifier.heightIn(min=48.dp).testTag("finance_edit_expense_${expense.id}")) { Text("Gideri düzenle") }
                    if(canWrite) TextButton(onClick={ onDelete(expense) },modifier=Modifier.heightIn(min=48.dp).testTag("finance_delete_expense_${expense.id}")) { Text("Gideri sil") }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. FINANCE REPORTS & PROFILE TAB
// -------------------------------------------------------------
@Composable
private fun FinanceReportsAndProfileTab(
    state: Okul360UiState,
    currencyFormatter: NumberFormat,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("📄 Mali Raporlar & Hesap", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandNavy)
            Text("Resmi muhasebe raporları ve kullanıcı bilgileri", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💼 Muhasebe Sorumlusu", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                    Text("Ad Soyad: ${state.currentUser.full_name}", fontSize = 13.sp, color = BrandNavy)
                    Text("E-Posta: ${state.currentUser.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Kurum: ${state.currentUser.institution_name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Oturumu Kapat")
                    }
                }
            }
        }
    }
}
