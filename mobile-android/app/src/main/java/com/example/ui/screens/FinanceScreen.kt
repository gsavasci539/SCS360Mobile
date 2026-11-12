package com.example.ui.screens

import com.example.ui.components.ResponsiveTableScope
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Debt
import com.example.data.model.Expense
import com.example.data.model.Invoice
import com.example.data.model.InvoiceStatus
import com.example.ui.components.CompactResponsiveCard
import com.example.ui.components.CompactRowActionIcon
import com.example.ui.components.CompactTableRow
import com.example.ui.components.FinancialSummaryCard
import com.example.ui.components.MetricStatCard
import com.example.ui.components.PaymentStatusPieChart
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.RoleSpeedDialFab
import com.example.ui.components.SpeedDialAction
import com.example.ui.components.StatusBadge
import com.example.ui.components.StickyTableHeaderRow
import com.example.ui.components.TableColumnSpec
import com.example.ui.components.isCompactCardMode
import com.example.ui.components.rememberResolvedTableMode
import com.example.ui.dialogs.*
import com.example.ui.dialogs.iyzico.IyzicoPaymentDialog
import com.example.data.payment.iyzico.IyzicoPaymentResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinanceScreen(
    state: Okul360UiState,
    onFilterChanged: (String) -> Unit,
    onSelectInvoiceForPayment: (Invoice) -> Unit,
    onAddInvoice: (studentId: String, studentName: String, className: String, amount: Double, dueDate: String, desc: String) -> Unit,
    onUpdateInvoice: (Invoice) -> Unit,
    onDeleteInvoice: (String) -> Unit,
    onViewReceipt: (Invoice) -> Unit,
    onAddExpense: (category: String, title: String, amount: Double, date: String, receiptUrl: String?) -> Unit = { _, _, _, _, _ -> },
    onDeleteExpense: (String) -> Unit = {},
    onSendPaymentReminder: (String, String) -> Unit = { _, _ -> },
    onPayWithIyzico: (String, Double, String, String) -> Unit = { _, _, _, _ -> },
    onGenerateInstallments: (studentId: String, totalAmount: Double, installmentCount: Int, downPayment: Double, startDate: String, desc: String) -> Unit = { _, _, _, _, _, _ -> },
    onCreateDebt: (studentId: String, studentName: String, className: String, debtType: String, description: String, amount: Double, dueDate: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onUpdateDebtStatus: (debtId: String, newStatus: String, paidAmount: Double?) -> Unit = { _, _, _ -> },
    onDeleteDebt: (debtId: String) -> Unit = {},
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    val presentationScope = com.example.ui.components.financePresentationScope(state)
    val summary = state.financeSummary
    var selectedFinanceTab by remember { mutableStateOf(0) } // 0: Gelir & Faturalar, 1: Borçlar & Alacaklar, 2: Giderler

    var searchQuery by remember { mutableStateOf("") }
    var showAddInvoiceDialog by remember(presentationScope) { mutableStateOf(false) }
    var showInstallmentDialog by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember(presentationScope) { mutableStateOf(false) }
    var debtFilterType by remember { mutableStateOf("ALL") }
    var invoiceForIyzicoPayment by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var expenseCategoryFilter by remember { mutableStateOf("ALL") }
    var debtToDelete by remember { mutableStateOf<Debt?>(null) }

    var isFabExpanded by remember { mutableStateOf(false) }
    val tableModeState = rememberResolvedTableMode()
    val isCardMode = isCompactCardMode(tableModeState.value)

    val totalExpenses = remember(state.expenses) { state.expenses.sumOf { it.amount } }
    val totalCollected = remember(state.invoices) {
        state.invoices.filter {
            it.status.equals("PAID", ignoreCase = true) || it.status.equals("ODENDI", ignoreCase = true) || it.paid_amount >= it.amount
        }.sumOf { if (it.paid_amount > 0) it.paid_amount else it.amount }
    }
    val netCashflow = totalCollected - totalExpenses

    val totalDebtsAmount = remember(state.debts) { state.debts.sumOf { it.amount } }
    val totalDebtsPaid = remember(state.debts) { state.debts.sumOf { it.paid_amount } }
    val totalDebtsRemaining = remember(totalDebtsAmount, totalDebtsPaid) { totalDebtsAmount - totalDebtsPaid }

    val financeActions = remember(state.invoices, onSelectInvoiceForPayment, onFilterChanged) {
        listOf(
            SpeedDialAction(
                id = "finance_screen_add_invoice",
                title = "Yeni Fatura Oluştur",
                description = "Öğrenciye aidat/hizmet faturası tanımla",
                icon = Icons.Default.AddCard,
                containerColor = Color(0xFF059669),
                testTag = "finance_screen_fab_add_invoice",
                onClick = {
                    isFabExpanded = false
                    showAddInvoiceDialog = true
                }
            ),
            SpeedDialAction(
                id = "finance_screen_installments",
                title = "Taksit Planı Sihirbazı",
                description = "Peşinat ve 1-12 taksitli ödeme planı çıkar",
                icon = Icons.Default.CalendarMonth,
                containerColor = Color(0xFF7C3AED),
                testTag = "finance_screen_fab_installments",
                onClick = {
                    isFabExpanded = false
                    showInstallmentDialog = true
                }
            ),
            SpeedDialAction(
                id = "finance_screen_add_debt",
                title = "Yeni Borç Tahakkuku",
                description = "Öğrenciye eğitim, yemek, servis borcu tanımla",
                icon = Icons.Default.AccountBalanceWallet,
                containerColor = Color(0xFFD97706),
                testTag = "finance_screen_fab_add_debt",
                onClick = {
                    isFabExpanded = false
                    showAddDebtDialog = true
                }
            ),
            SpeedDialAction(
                id = "finance_screen_add_expense",
                title = "Yeni Gider Kaydet",
                description = "Kira, fatura, mutfak vb. kasa çıkışı",
                icon = Icons.Default.ReceiptLong,
                containerColor = Color(0xFFDC2626),
                testTag = "finance_screen_fab_add_expense",
                onClick = {
                    isFabExpanded = false
                    showAddExpenseDialog = true
                }
            ),
            SpeedDialAction(
                id = "finance_screen_quick_pos",
                title = "Hızlı Tahsilat / POS",
                description = "İlk bekleyen faturadan anında tahsilat al",
                icon = Icons.Default.Payment,
                containerColor = PrimaryBlue,
                testTag = "finance_screen_fab_quick_pos",
                onClick = {
                    isFabExpanded = false
                    val firstUnpaid = state.invoices.firstOrNull { !it.status.equals("PAID", ignoreCase = true) && !it.status.equals("ODENDI", ignoreCase = true) }
                    if (firstUnpaid != null) {
                        onSelectInvoiceForPayment(firstUnpaid)
                    }
                }
            )
        )
    }

    val paidCount = remember(state.invoices) { state.invoices.count { it.status.equals("PAID", ignoreCase = true) || it.status.equals("ODENDI", ignoreCase = true) || (it.paid_amount >= it.amount && it.amount > 0) } }
    val pendingCount = remember(state.invoices) { state.invoices.count { it.status.equals("PENDING", ignoreCase = true) || it.status.equals("BEKLIYOR", ignoreCase = true) } }
    val overdueCount = remember(state.invoices) { state.invoices.count { it.status.equals("OVERDUE", ignoreCase = true) || it.status.equals("GECIKTI", ignoreCase = true) } }

    val filteredInvoices = remember(state.invoices, searchQuery, state.invoiceFilter) {
        state.invoices.filter { invoice ->
            val isPaid = invoice.status.equals("PAID", ignoreCase = true) || invoice.status.equals("ODENDI", ignoreCase = true) || (invoice.paid_amount >= invoice.amount && invoice.amount > 0)
            val isOverdue = invoice.status.equals("OVERDUE", ignoreCase = true) || invoice.status.equals("GECIKTI", ignoreCase = true)
            val isPending = invoice.status.equals("PENDING", ignoreCase = true) || invoice.status.equals("BEKLIYOR", ignoreCase = true)

            val matchesFilter = when (state.invoiceFilter) {
                "PAID" -> isPaid
                "PENDING" -> isPending
                "OVERDUE" -> isOverdue
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                invoice.student_name.contains(searchQuery, ignoreCase = true) ||
                invoice.invoice_number.contains(searchQuery, ignoreCase = true) ||
                invoice.description.contains(searchQuery, ignoreCase = true) ||
                invoice.class_name.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    val filteredExpenses = remember(state.expenses, searchQuery, expenseCategoryFilter) {
        state.expenses.filter { expense ->
            val matchesCategory = if (expenseCategoryFilter == "ALL") true else expense.category.equals(expenseCategoryFilter, ignoreCase = true)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                expense.title.contains(searchQuery, ignoreCase = true) ||
                expense.category.contains(searchQuery, ignoreCase = true) ||
                expense.created_by.contains(searchQuery, ignoreCase = true) ||
                (expense.receipt_url?.contains(searchQuery, ignoreCase = true) ?: false)
            }
            matchesCategory && matchesSearch
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ResponsiveTableScope(scopeKey = selectedFinanceTab) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Header & Tab Switcher
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "💰 Finans & Kasa Yönetimi",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = BrandNavy
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tahsilat, fatura ve operasyonel gider takibi",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
    
                            Spacer(modifier = Modifier.width(8.dp))
    
                            when (selectedFinanceTab) {
                                0 -> {
                                    Button(
                                        onClick = { showAddInvoiceDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Fatura Ekle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                1 -> {
                                    Button(
                                        onClick = { showAddDebtDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Borç Tahakkuk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = { showAddExpenseDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Gider Ekle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
    
                        Spacer(modifier = Modifier.height(14.dp))
    
                        // AI Analiz & Nakit Tahmini Kartı
                        RoleContextualAiCard(
                            screenName = "FINANCE",
                            state = state,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
    
                        // Tab Selector: Gelir / Faturalar vs Borçlar vs Giderler
                        TabRow(
                            selectedTabIndex = selectedFinanceTab,
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = PrimaryBlue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Tab(
                                selected = selectedFinanceTab == 0,
                                onClick = { selectedFinanceTab = 0 },
                                text = { Text("Faturalar (${state.invoices.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                icon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = selectedFinanceTab == 1,
                                onClick = { selectedFinanceTab = 1 },
                                text = { Text("Borçlar (${state.debts.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = selectedFinanceTab == 2,
                                onClick = { selectedFinanceTab = 2 },
                                text = { Text("Giderler (${state.expenses.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                icon = { Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
    
                        Spacer(modifier = Modifier.height(14.dp))
    
                        com.example.ui.components.RecordedFinanceSummaryCard(state.recordedFinanceSummary,
                            state.isFinanceLoading, state.financeSummaryError, onRetry)
                        state.financeRowsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
    
                // TAB 0: INVOICES & COLLECTIONS
                if (selectedFinanceTab == 0) {
                    // Search Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Ödeme / Fatura veya öğrenci ara...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
    
                    // Status Filter Chips
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val filters = listOf(
                                "ALL" to "Tümü (${state.invoices.size})",
                                "PENDING" to "Bekleyenler ($pendingCount)",
                                "OVERDUE" to "Gecikenler ($overdueCount)",
                                "PAID" to "Ödenmiş / Tahsilat ($paidCount)"
                            )
    
                            items(filters) { (key, label) ->
                                FilterChip(
                                    selected = state.invoiceFilter == key,
                                    onClick = { onFilterChanged(key) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    leadingIcon = if (state.invoiceFilter == key) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
    
                    // Empty state if no items
                    if (filteredInvoices.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "Filtreye uygun fatura bulunamadı" else "Kayıtlı fatura yok",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
    
                    if (filteredInvoices.isNotEmpty()) {
                        stickyHeader {
                            StickyTableHeaderRow(
                                columns = listOf(
                                    TableColumnSpec("Öğrenci / Sınıf", 1.35f),
                                    TableColumnSpec("Fatura / Vade", 1.1f),
                                    TableColumnSpec("Tutar", 0.85f, TextAlign.End),
                                    TableColumnSpec("Durum / İşlem", 1.15f, TextAlign.End)
                                ),
                                totalCount = filteredInvoices.size,
                                viewModeState = tableModeState
                            )
                        }
                    }
    
                    // Invoice Items
                    itemsIndexed(filteredInvoices, key = { _, it -> it.id }) { index, invoice ->
                        if (isCardMode) {
                            InvoiceCard(
                                invoice = invoice,
                                onPayClick = { onSelectInvoiceForPayment(invoice) },
                                onEditClick = { invoiceToEdit = invoice },
                                onDeleteClick = { invoiceToDelete = invoice },
                                onViewReceiptClick = { onViewReceipt(invoice) },
                                onIyzicoPayClick = { invoiceForIyzicoPayment = invoice },
                                onSendReminderClick = { onSendPaymentReminder(invoice.id, invoice.student_name) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                            )
                        } else {
                            InvoiceCompactTableRow(
                                index = index,
                                invoice = invoice,
                                onPayClick = { onSelectInvoiceForPayment(invoice) },
                                onEditClick = { invoiceToEdit = invoice },
                                onDeleteClick = { invoiceToDelete = invoice },
                                onViewReceiptClick = { onViewReceipt(invoice) },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                } else if (selectedFinanceTab == 1) {
                    // TAB 1: DEBTS & BORÇ TAKİBİ (/api/debts)
                    // Search Bar for Debts
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Öğrenci veya borç açıklaması ara...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFD97706)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
    
                    // Debt Type Filter Chips
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val types = listOf(
                                "ALL" to "Tümü",
                                "EDUCATION" to "Eğitim",
                                "FOOD" to "Yemek",
                                "SERVICE" to "Servis",
                                "STATIONERY" to "Kırtasiye"
                            )
                            items(types) { (key, label) ->
                                FilterChip(
                                    selected = debtFilterType == key,
                                    onClick = { debtFilterType = key },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
    
                    val filteredDebts = state.debts.filter { debt ->
                        val matchesQuery = searchQuery.isBlank() ||
                            debt.student_name.contains(searchQuery, ignoreCase = true) ||
                            debt.description.contains(searchQuery, ignoreCase = true) ||
                            debt.class_name.contains(searchQuery, ignoreCase = true)
                        val matchesType = debtFilterType == "ALL" || debt.debt_type.equals(debtFilterType, ignoreCase = true)
                        matchesQuery && matchesType
                    }
    
                    if (filteredDebts.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "Aramaya uygun borç kaydı bulunamadı" else "Kayıtlı borç tahakkuku bulunmuyor",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showAddDebtDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("İlk Borç Tahakkukunu Ekle")
                                    }
                                }
                            }
                        }
                    }
    
                    if (filteredDebts.isNotEmpty()) {
                        stickyHeader {
                            StickyTableHeaderRow(
                                columns = listOf(
                                    TableColumnSpec("Öğrenci / Sınıf", 1.3f),
                                    TableColumnSpec("Açıklama / Tür", 1.2f),
                                    TableColumnSpec("Tutar / Kalan", 0.9f, TextAlign.End),
                                    TableColumnSpec("Durum / İşlem", 1.1f, TextAlign.End)
                                ),
                                totalCount = filteredDebts.size,
                                viewModeState = tableModeState
                            )
                        }
                    }
    
                    // Debt Items
                    itemsIndexed(filteredDebts, key = { _, it -> it.id }) { index, debt ->
                        val remaining = maxOf(0.0, debt.amount - debt.paid_amount)
                        val isPaid = debt.status.equals("PAID", ignoreCase = true) || remaining <= 0
                        val (statusText, statusBg, statusColor) = when {
                            isPaid -> Triple("ÖDENDİ", Color(0xFF059669).copy(alpha = 0.12f), Color(0xFF059669))
                            debt.status.equals("OVERDUE", ignoreCase = true) -> Triple("GECİKMİŞ", Color(0xFFDC2626).copy(alpha = 0.12f), Color(0xFFDC2626))
                            debt.paid_amount > 0 -> Triple("KISMİ", Color(0xFF2563EB).copy(alpha = 0.12f), Color(0xFF2563EB))
                            else -> Triple("BEKLİYOR", Color(0xFFD97706).copy(alpha = 0.12f), Color(0xFFD97706))
                        }
    
                        if (isCardMode) {
                            CompactResponsiveCard(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                        Text(debt.student_name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = BrandNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${debt.class_name} • Vade: ${debt.due_date} • ${debt.debt_type}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                                        Text(
                                            text = statusText,
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
    
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(debt.description, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₺${String.format("%,.0f", debt.amount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                                            if (!isPaid && debt.paid_amount > 0) {
                                                Text("Kalan: ₺${String.format("%,.0f", remaining)}", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        CompactRowActionIcon(
                                            icon = Icons.Default.Send,
                                            contentDescription = "Hatırlat",
                                            tint = PrimaryBlue,
                                            containerColor = PrimaryBlue.copy(alpha = 0.1f),
                                            onClick = { onSendPaymentReminder(debt.id, debt.student_name) }
                                        )
                                        if (!isPaid) {
                                            CompactRowActionIcon(
                                                icon = Icons.Default.Check,
                                                contentDescription = "Ödendi Yap",
                                                tint = SecondaryEmerald,
                                                containerColor = SecondaryEmerald.copy(alpha = 0.12f),
                                                onClick = { onUpdateDebtStatus(debt.id, "PAID", debt.amount) }
                                            )
                                        }
                                        CompactRowActionIcon(
                                            icon = Icons.Default.DeleteOutline,
                                            contentDescription = "Borcu Sil",
                                            tint = Color(0xFFDC2626),
                                            containerColor = Color(0xFFDC2626).copy(alpha = 0.1f),
                                            onClick = { debtToDelete = debt }
                                        )
                                    }
                                }
                            }
                        } else {
                            CompactTableRow(
                                index = index,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1.3f).padding(end = 6.dp)) {
                                    Text(debt.student_name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = BrandNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${debt.class_name} • ${debt.due_date}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Column(modifier = Modifier.weight(1.2f).padding(end = 6.dp)) {
                                    Text(debt.description, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(debt.debt_type, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                Column(modifier = Modifier.weight(0.9f), horizontalAlignment = Alignment.End) {
                                    Text("₺${String.format("%,.0f", debt.amount)}", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = BrandNavy)
                                    if (!isPaid && debt.paid_amount > 0) {
                                        Text("Kalan: ₺${String.format("%,.0f", remaining)}", fontSize = 10.sp, color = Color(0xFFDC2626))
                                    }
                                }
                                Row(
                                    modifier = Modifier.weight(1.1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = RoundedCornerShape(5.dp), color = statusBg) {
                                        Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                    }
                                    if (!isPaid) {
                                        CompactRowActionIcon(
                                            icon = Icons.Default.Check,
                                            contentDescription = "Ödendi Yap",
                                            tint = SecondaryEmerald,
                                            containerColor = SecondaryEmerald.copy(alpha = 0.12f),
                                            onClick = { onUpdateDebtStatus(debt.id, "PAID", debt.amount) }
                                        )
                                    }
                                    CompactRowActionIcon(
                                        icon = Icons.Default.DeleteOutline,
                                        contentDescription = "Borcu Sil",
                                        tint = Color(0xFFDC2626),
                                        containerColor = Color(0xFFDC2626).copy(alpha = 0.1f),
                                        onClick = { debtToDelete = debt }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // TAB 2: EXPENSES & CASH OUTFLOW
                    // Search Bar for Expenses
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Gider ara (Kira, mutfak, kırtasiye, belge no)...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFDC2626)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
    
                    // Category Filter Chips
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val categories = listOf("ALL", "Kira", "Mutfak", "Kırtasiye", "Fatura", "Maaş", "Diğer")
    
                            items(categories) { cat ->
                                FilterChip(
                                    selected = expenseCategoryFilter == cat,
                                    onClick = { expenseCategoryFilter = cat },
                                    label = { Text(if (cat == "ALL") "Tümü (${state.expenses.size})" else cat, fontSize = 12.sp) },
                                    leadingIcon = if (expenseCategoryFilter == cat) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
    
                    if (filteredExpenses.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "Aramaya uygun gider bulunamadı" else "Kayıtlı gider bulunmuyor",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showAddExpenseDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("İlk Gideri Ekle")
                                    }
                                }
                            }
                        }
                    }
    
                    if (filteredExpenses.isNotEmpty()) {
                        stickyHeader {
                            StickyTableHeaderRow(
                                columns = listOf(
                                    TableColumnSpec("Kategori / Tarih", 1.1f),
                                    TableColumnSpec("Gider Başlığı / Kayıt", 1.6f),
                                    TableColumnSpec("Tutar", 0.9f, TextAlign.End),
                                    TableColumnSpec("İşlem", 0.5f, TextAlign.End)
                                ),
                                totalCount = filteredExpenses.size,
                                viewModeState = tableModeState
                            )
                        }
                    }
    
                    // Expense Items
                    itemsIndexed(filteredExpenses, key = { _, it -> it.id }) { index, expense ->
                        if (isCardMode) {
                            ExpenseCardItem(
                                expense = expense,
                                onDeleteClick = { expenseToDelete = expense },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                            )
                        } else {
                            val categoryColor = when (expense.category) {
                                "Kira" -> Color(0xFFDC2626)
                                "Mutfak" -> Color(0xFFD97706)
                                "Kırtasiye" -> Color(0xFF7C3AED)
                                "Fatura" -> Color(0xFF0284C7)
                                "Maaş" -> Color(0xFF059669)
                                else -> Color(0xFF64748B)
                            }
                            CompactTableRow(
                                index = index,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1.1f).padding(end = 6.dp)) {
                                    Surface(shape = RoundedCornerShape(5.dp), color = categoryColor.copy(alpha = 0.12f)) {
                                        Text(expense.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = categoryColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Text(expense.date, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(modifier = Modifier.weight(1.6f).padding(end = 6.dp)) {
                                    Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = BrandNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Kaydeden: ${expense.created_by}", fontSize = 10.5.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(
                                    text = "-₺${String.format("%,.0f", expense.amount)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(0xFFDC2626),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.9f)
                                )
                                Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.CenterEnd) {
                                    CompactRowActionIcon(
                                        icon = Icons.Default.Delete,
                                        contentDescription = "Sil",
                                        tint = MaterialTheme.colorScheme.error,
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                        onClick = { expenseToDelete = expense }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Speed Dial
        RoleSpeedDialFab(
            actions = financeActions,
            expanded = isFabExpanded,
            onExpandedChange = { isFabExpanded = it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            testTag = "finance_screen_fab_main"
        )
    }

    // Dialogs
    if (showAddInvoiceDialog) {
        FinanceInvoiceDraftDialog(state, onDismiss = { showAddInvoiceDialog = false }, onCreate = { _, _, _, _, _ -> },
            onRecorded = { showAddInvoiceDialog = false; onRetry() })
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { category, title, amount, date, receiptUrl ->
                onAddExpense(category, title, amount, date, receiptUrl)
                showAddExpenseDialog = false
            }
        )
    }

    expenseToDelete?.let { exp ->
        ConfirmDeleteDialog(
            title = "Gider Kaydını Sil",
            message = "\"${exp.title}\" başlıklı ₺${exp.amount.toInt()} tutarındaki gideri silmek istediğinize emin misiniz?",
            onDismiss = { expenseToDelete = null },
            onConfirm = {
                onDeleteExpense(exp.id)
                expenseToDelete = null
            }
        )
    }

    invoiceToEdit?.let { inv ->
        EditInvoiceDialog(
            invoice = inv,
            onDismiss = { invoiceToEdit = null },
            onConfirm = { updated ->
                onUpdateInvoice(updated)
                invoiceToEdit = null
            }
        )
    }

    invoiceToDelete?.let { inv ->
        ConfirmDeleteDialog(
            title = "Faturayı Sil",
            message = "${inv.invoice_number} numaralı faturayı silmek istediğinize emin misiniz?",
            onDismiss = { invoiceToDelete = null },
            onConfirm = {
                onDeleteInvoice(inv.id)
                invoiceToDelete = null
            }
        )
    }

    invoiceForIyzicoPayment?.let { inv ->
        IyzicoPaymentDialog(
            invoice = inv,
            studentName = inv.student_name,
            initialAmount = inv.amount,
            paymentDescription = "${inv.class_name} - ${inv.invoice_number} nolu fatura tahsilatı",
            onDismiss = { invoiceForIyzicoPayment = null },
            onPaymentSuccess = { _: IyzicoPaymentResult, _: String ->
                onPayWithIyzico(inv.id, inv.amount, "4355-****-****-0001", inv.student_name)
                invoiceForIyzicoPayment = null
            }
        )
    }

    if (showInstallmentDialog) {
        var selectedStudentId by remember { mutableStateOf(state.students.firstOrNull()?.id ?: "") }
        var totalAmountText by remember { mutableStateOf("120000") }
        var installmentCountText by remember { mutableStateOf("10") }
        var downPaymentText by remember { mutableStateOf("20000") }
        var startDateText by remember { mutableStateOf("2026-09-15") }
        var planDescription by remember { mutableStateOf("2026-2027 Öğretim Yılı Taksit Planı") }

        AlertDialog(
            onDismissRequest = { showInstallmentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Taksit Planı Sihirbazı", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Öğrenciye peşinat ve eşit taksitli ödeme planı tanımlayın.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = totalAmountText,
                        onValueChange = { totalAmountText = it },
                        label = { Text("Toplam Eğitim Ücreti (₺)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = downPaymentText,
                            onValueChange = { downPaymentText = it },
                            label = { Text("Peşinat (₺)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = installmentCountText,
                            onValueChange = { installmentCountText = it },
                            label = { Text("Taksit Sayısı") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = startDateText,
                        onValueChange = { startDateText = it },
                        label = { Text("İlk Taksit Vadesi (YYYY-AA-GG)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = planDescription,
                        onValueChange = { planDescription = it },
                        label = { Text("Açıklama") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val total = totalAmountText.toDoubleOrNull() ?: 100000.0
                        val count = installmentCountText.toIntOrNull() ?: 10
                        val down = downPaymentText.toDoubleOrNull() ?: 0.0
                        val sId = if (selectedStudentId.isNotBlank()) selectedStudentId else state.students.firstOrNull()?.id ?: "std-1"
                        onGenerateInstallments(sId, total, count, down, startDateText, planDescription)
                        showInstallmentDialog = false
                    }
                ) {
                    Text("Faturaları Oluştur")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallmentDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    if (showAddDebtDialog) {
        FinanceInvoiceDraftDialog(state, onDismiss = { showAddDebtDialog = false }, onCreate = { _, _, _, _, _ -> },
            onRecorded = { showAddDebtDialog = false; onRetry() })
    }

    debtToDelete?.let { debt ->
        ConfirmDeleteDialog(
            title = "Borç Kaydını Sil",
            message = "${debt.student_name} için tanımlanan ₺${debt.amount.toInt()} tutarındaki (${debt.debt_type}) borcu silmek istediğinize emin misiniz?",
            onDismiss = { debtToDelete = null },
            onConfirm = {
                onDeleteDebt(debt.id)
                debtToDelete = null
            }
        )
    }
}

@Composable
fun AddDebtDialog(
    students: List<com.example.data.model.Student>,
    onDismiss: () -> Unit,
    onConfirm: (studentId: String, studentName: String, className: String, debtType: String, desc: String, amount: Double, dueDate: String) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
    var selectedStudentName by remember { mutableStateOf(students.firstOrNull()?.full_name ?: "") }
    var selectedClassName by remember { mutableStateOf(students.firstOrNull()?.class_name ?: "") }
    var selectedDebtType by remember { mutableStateOf("EDUCATION") }
    var description by remember { mutableStateOf("2026-2027 Eğitim Öğretim Ücreti") }
    var amountText by remember { mutableStateOf("15000") }
    var dueDate by remember { mutableStateOf("2026-10-15") }
    var studentDropdownExpanded by remember { mutableStateOf(false) }

    val debtTypes = listOf(
        "EDUCATION" to "Eğitim / Öğrenim",
        "FOOD" to "Yemek Hizmeti",
        "SERVICE" to "Servis & Ulaşım",
        "STATIONERY" to "Kırtasiye & Kitap",
        "OTHER" to "Diğer Hizmet"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Borç Tahakkuku Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Öğrenci Seçimi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { studentDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedStudentName.isNotBlank()) "$selectedStudentName ($selectedClassName)" else "Öğrenci Seçin")
                    }
                    DropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        students.forEach { std ->
                            DropdownMenuItem(
                                text = { Text("${std.full_name} (${std.class_name})") },
                                onClick = {
                                    selectedStudentId = std.id
                                    selectedStudentName = std.full_name
                                    selectedClassName = std.class_name
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Borç Türü", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    debtTypes.take(3).forEach { (typeKey, label) ->
                        FilterChip(
                            selected = selectedDebtType == typeKey,
                            onClick = { selectedDebtType = typeKey },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    debtTypes.drop(3).forEach { (typeKey, label) ->
                        FilterChip(
                            selected = selectedDebtType == typeKey,
                            onClick = { selectedDebtType = typeKey },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Tutar (₺)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Son Ödeme / Vade Tarihi (YYYY-AA-GG)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && selectedStudentId.isNotBlank()) {
                        onConfirm(selectedStudentId, selectedStudentName, selectedClassName, selectedDebtType, description, amt, dueDate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text("Tahakkuk Oluştur")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun ExpenseCardItem(
    expense: Expense,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (expense.category) {
        "Kira" -> Color(0xFFDC2626)
        "Mutfak" -> Color(0xFFD97706)
        "Kırtasiye" -> Color(0xFF7C3AED)
        "Fatura" -> Color(0xFF0284C7)
        "Maaş" -> Color(0xFF059669)
        else -> Color(0xFF64748B)
    }

    CompactResponsiveCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = categoryColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = expense.category,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${expense.date} • ${expense.created_by}" + (if (!expense.receipt_url.isNullOrBlank()) " • Ref: ${expense.receipt_url}" else ""),
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "-₺${String.format("%,.0f", expense.amount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.5.sp,
                    color = Color(0xFFDC2626)
                )
                CompactRowActionIcon(
                    icon = Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    onClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
fun InvoiceCompactTableRow(
    index: Int,
    invoice: Invoice,
    onPayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onViewReceiptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusEnum = when (invoice.status) {
        "PAID" -> InvoiceStatus.PAID
        "OVERDUE" -> InvoiceStatus.OVERDUE
        else -> InvoiceStatus.PENDING
    }

    CompactTableRow(
        index = index,
        modifier = modifier
    ) {
        Column(modifier = Modifier.weight(1.35f).padding(end = 6.dp)) {
            Text(
                text = invoice.student_name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = BrandNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = invoice.class_name,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Column(modifier = Modifier.weight(1.1f).padding(end = 6.dp)) {
            Text(
                text = invoice.invoice_number,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Vade: ${invoice.due_date}",
                fontSize = 10.5.sp,
                color = if (invoice.status == "OVERDUE") AccentRose else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Text(
            text = "₺${invoice.amount.toInt()}",
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.85f).padding(end = 6.dp)
        )

        Row(
            modifier = Modifier.weight(1.15f),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(
                label = statusEnum.label,
                color = Color(statusEnum.colorHex)
            )
            if (invoice.status != "PAID") {
                CompactRowActionIcon(
                    icon = Icons.Default.Payment,
                    contentDescription = "Tahsilat",
                    tint = SecondaryEmerald,
                    containerColor = SecondaryEmerald.copy(alpha = 0.14f),
                    onClick = onPayClick
                )
            } else {
                CompactRowActionIcon(
                    icon = Icons.Default.Receipt,
                    contentDescription = "Makbuz",
                    tint = Color(0xFF059669),
                    containerColor = Color(0xFF059669).copy(alpha = 0.12f),
                    onClick = onViewReceiptClick
                )
            }
            CompactRowActionIcon(
                icon = Icons.Default.Edit,
                contentDescription = "Düzenle",
                tint = PrimaryBlue,
                containerColor = PrimaryBlue.copy(alpha = 0.1f),
                onClick = onEditClick
            )
            CompactRowActionIcon(
                icon = Icons.Default.Delete,
                contentDescription = "Sil",
                tint = MaterialTheme.colorScheme.error,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    onPayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onViewReceiptClick: () -> Unit,
    onIyzicoPayClick: (() -> Unit)? = null,
    onSendReminderClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusEnum = when (invoice.status) {
        "PAID" -> InvoiceStatus.PAID
        "OVERDUE" -> InvoiceStatus.OVERDUE
        else -> InvoiceStatus.PENDING
    }

    CompactResponsiveCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                Text(
                    text = invoice.student_name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${invoice.class_name} • ${invoice.invoice_number} • Son: ${invoice.due_date}",
                    fontSize = 11.sp,
                    color = if (invoice.status == "OVERDUE") AccentRose else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusBadge(
                    label = statusEnum.label,
                    color = Color(statusEnum.colorHex)
                )
                CompactRowActionIcon(
                    icon = Icons.Default.Edit,
                    contentDescription = "Düzenle",
                    tint = PrimaryBlue,
                    containerColor = PrimaryBlue.copy(alpha = 0.1f),
                    onClick = onEditClick
                )
                CompactRowActionIcon(
                    icon = Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    onClick = onDeleteClick
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "₺${invoice.amount.toInt()}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 15.5.sp,
                    color = MaterialTheme.colorScheme.primary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (invoice.status != "PAID") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (onSendReminderClick != null) {
                        CompactRowActionIcon(
                            icon = Icons.Default.Send,
                            contentDescription = "SMS Hatırlat",
                            tint = PrimaryBlue,
                            containerColor = PrimaryBlue.copy(alpha = 0.1f),
                            onClick = onSendReminderClick
                        )
                    }
                    if (onIyzicoPayClick != null) {
                        OutlinedButton(
                            onClick = onIyzicoPayClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("iyzico", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E64FF), softWrap = false)
                        }
                    }
                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Tahsilat", fontSize = 11.sp, fontWeight = FontWeight.Bold, softWrap = false)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onViewReceiptClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Makbuz",
                        fontSize = 11.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold,
                        softWrap = false
                    )
                }
            }
        }
    }
}
