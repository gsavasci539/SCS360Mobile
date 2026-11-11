package com.example.ui.screens

import com.example.ui.components.ResponsiveTableScope
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LibraryBook
import com.example.ui.components.CompactResponsiveCard
import com.example.ui.components.CompactRowActionIcon
import com.example.ui.components.CompactTableRow
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.StickyTableHeaderRow
import com.example.ui.components.TableColumnSpec
import com.example.ui.components.isCompactCardMode
import com.example.ui.components.rememberResolvedTableMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit,
    onBorrowBook: (String, String, String, String, String, Int) -> Unit,
    onReturnBook: (String) -> Unit,
    onSendOverdueReminder: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("CATALOG") } // CATALOG, LOANS
    var searchQuery by remember { mutableStateOf("") }
    var showBorrowDialogForBook by remember { mutableStateOf<LibraryBook?>(null) }
    val tableModeState = rememberResolvedTableMode()
    val isCardMode = isCompactCardMode(tableModeState.value)

    val filteredBooks = remember(state.libraryBooks, searchQuery) {
        if (searchQuery.isBlank()) state.libraryBooks
        else state.libraryBooks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kütüphane & Kitap Takibi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Katalog • Ödünç & İade Yönetimi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("library_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = if (selectedTab == "CATALOG") 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == "CATALOG",
                    onClick = { selectedTab = "CATALOG" },
                    text = { Text("Kitap Kataloğu (${state.libraryBooks.size})") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == "LOANS",
                    onClick = { selectedTab = "LOANS" },
                    text = { Text("Ödünç / Zimmet (${state.bookLoans.count { !it.is_returned }})") },
                    icon = { Icon(Icons.Default.AssignmentReturn, contentDescription = null) }
                )
            }

            if (selectedTab == "CATALOG") {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Kitap adı, yazar veya kategori ara...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                ResponsiveTableScope() {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            RoleContextualAiCard(
                                screenName = "LIBRARY",
                                state = state,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
    
                        if (filteredBooks.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            "Kayıtlı kitap bulunamadı.",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else {
                            stickyHeader {
                                StickyTableHeaderRow(
                                    columns = listOf(
                                        TableColumnSpec("Kitap / Yazar", 1.55f),
                                        TableColumnSpec("Kategori / Raf", 1.1f),
                                        TableColumnSpec("Stok / İşlem", 1.05f, TextAlign.End)
                                    ),
                                    totalCount = filteredBooks.size,
                                    viewModeState = tableModeState
                                )
                            }
    
                            itemsIndexed(filteredBooks, key = { _, it -> it.id }) { index, book ->
                                if (isCardMode) {
                                    CompactResponsiveCard(
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f).padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(book.cover_color_hex).copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Book, contentDescription = null, tint = Color(book.cover_color_hex), modifier = Modifier.size(18.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(book.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("${book.author} • ${book.category} • Raf: ${book.shelf_location}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
    
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (book.available_copies > 0) EmeraldPrimary.copy(alpha = 0.15f) else CrimsonPrimary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        "${book.available_copies}/${book.total_copies}",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (book.available_copies > 0) EmeraldPrimary else CrimsonPrimary
                                                    )
                                                }
                                                Button(
                                                    onClick = { showBorrowDialogForBook = book },
                                                    enabled = book.available_copies > 0,
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Ödünç Ver", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    CompactTableRow(
                                        index = index,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1.55f).padding(end = 6.dp)) {
                                            Text(book.title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(book.author, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                        Column(modifier = Modifier.weight(1.1f).padding(end = 6.dp)) {
                                            Text(book.category, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                            Text("Raf: ${book.shelf_location}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Row(
                                            modifier = Modifier.weight(1.05f),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${book.available_copies}/${book.total_copies}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (book.available_copies > 0) EmeraldPrimary else CrimsonPrimary
                                            )
                                            Button(
                                                onClick = { showBorrowDialogForBook = book },
                                                enabled = book.available_copies > 0,
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Zimmet", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Loans List
                ResponsiveTableScope() {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (state.bookLoans.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.AssignmentReturn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            "Ödünç kitap kaydı bulunamadı.",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else {
                            stickyHeader {
                                StickyTableHeaderRow(
                                    columns = listOf(
                                        TableColumnSpec("Kitap / Öğrenci", 1.45f),
                                        TableColumnSpec("Alınma / Vade", 1.15f),
                                        TableColumnSpec("Durum / İade", 1.1f, TextAlign.End)
                                    ),
                                    totalCount = state.bookLoans.size,
                                    viewModeState = tableModeState
                                )
                            }
    
                            itemsIndexed(state.bookLoans, key = { _, it -> it.id }) { index, loan ->
                                if (isCardMode) {
                                    CompactResponsiveCard(
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                                Text(loan.book_title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${loan.student_name} (${loan.class_name}) • No: ${loan.student_number}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
    
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (loan.is_returned) EmeraldPrimary.copy(alpha = 0.15f)
                                                else if (loan.is_overdue) CrimsonPrimary.copy(alpha = 0.2f)
                                                else AmberPrimary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    loan.daysRemainingOrOverdue,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (loan.is_returned) EmeraldPrimary else if (loan.is_overdue) CrimsonPrimary else AmberPrimary
                                                )
                                            }
                                        }
    
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Alınma: ${loan.borrowed_at} • Son: ${loan.due_date}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                if (!loan.is_returned) {
                                                    if (loan.is_overdue) {
                                                        CompactRowActionIcon(
                                                            icon = Icons.Default.NotificationsActive,
                                                            contentDescription = "Veliye Hatırlat",
                                                            tint = CrimsonPrimary,
                                                            containerColor = CrimsonPrimary.copy(alpha = 0.14f),
                                                            onClick = { onSendOverdueReminder(loan.id) }
                                                        )
                                                    }
    
                                                    Button(
                                                        onClick = { onReturnBook(loan.id) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("İade Al", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    Text("İade (${loan.returned_at})", fontSize = 10.5.sp, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    CompactTableRow(
                                        index = index,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1.45f).padding(end = 6.dp)) {
                                            Text(loan.book_title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${loan.student_name} (${loan.class_name})", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                        Column(modifier = Modifier.weight(1.15f).padding(end = 6.dp)) {
                                            Text(loan.borrowed_at, fontSize = 11.sp)
                                            Text("Vade: ${loan.due_date}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(
                                            modifier = Modifier.weight(1.1f),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                loan.daysRemainingOrOverdue,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (loan.is_returned) EmeraldPrimary else if (loan.is_overdue) CrimsonPrimary else AmberPrimary
                                            )
                                            if (!loan.is_returned) {
                                                CompactRowActionIcon(
                                                    icon = Icons.Default.Check,
                                                    contentDescription = "İade Al",
                                                    tint = EmeraldPrimary,
                                                    containerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                                    onClick = { onReturnBook(loan.id) }
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
        }
    }

    // Borrow Dialog
    showBorrowDialogForBook?.let { book ->
        var studentName by remember { mutableStateOf(state.students.firstOrNull()?.let { "${it.first_name} ${it.last_name}" } ?: "") }
        var studentNo by remember { mutableStateOf(state.students.firstOrNull()?.student_number ?: "") }
        var className by remember { mutableStateOf(state.students.firstOrNull()?.class_name ?: "") }

        AlertDialog(
            onDismissRequest = { showBorrowDialogForBook = null },
            title = { Text("Kitap Zimmetle / Ödünç Ver") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kitap: ${book.title} (${book.author})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Öğrenci Ad Soyad") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = studentNo,
                            onValueChange = { studentNo = it },
                            label = { Text("Öğrenci No") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = className,
                            onValueChange = { className = it },
                            label = { Text("Sınıf") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text("Ödünç Süresi: 14 Gün (2 Hafta)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBorrowBook(book.id, "std-1", studentName, studentNo, className, 14)
                        showBorrowDialogForBook = null
                    }
                ) {
                    Text("Zimmetle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBorrowDialogForBook = null }) {
                    Text("İptal")
                }
            }
        )
    }
}
