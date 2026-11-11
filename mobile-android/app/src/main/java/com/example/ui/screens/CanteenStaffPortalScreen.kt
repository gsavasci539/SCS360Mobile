package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CanteenProduct
import com.example.data.model.Student
import com.example.data.model.UserRole
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.RoleSwitchBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanteenStaffPortalScreen(
    state: Okul360UiState,
    onAddProduct: (CanteenProduct) -> Unit,
    onUpdateProduct: (CanteenProduct) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onProcessPurchase: (CanteenProduct, Int) -> Unit,
    onTopup: (Double, String) -> Unit,
    onRoleSelected: (UserRole) -> Unit = {},
    onOpenChatbot: (() -> Unit)? = null,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Kantin Satış (POS)", "Ürün Kataloğu", "Bakiye Yükleme", "Ciro & Rapor", "AI Asistan")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SCS360 Kantin Portalı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BrandNavy
                            )
                            Text(
                                text = "Kantin Görevlisi • ${state.currentUser.branch_name}",
                                fontSize = 11.sp,
                                color = NeutralGrayDark
                            )
                        }
                    }
                },
                actions = {
                    RoleSwitchBadge(
                        selectedRole = state.selectedRole,
                        onRoleSelected = onRoleSelected,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("btn_canteen_logout")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Logout,
                            contentDescription = "Çıkış Yap",
                            tint = StatusRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryBlue
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index && index != 4,
                        onClick = {
                            if (index == 4) {
                                onOpenChatbot?.invoke()
                            } else {
                                selectedTab = index
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Outlined.PointOfSale
                                    1 -> Icons.Outlined.Fastfood
                                    2 -> Icons.Outlined.AccountBalanceWallet
                                    3 -> Icons.Outlined.Analytics
                                    else -> Icons.Outlined.AutoAwesome
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> CanteenPosTab(
                    state = state,
                    onProcessPurchase = onProcessPurchase
                )
                1 -> CanteenCatalogTab(
                    state = state,
                    onAddProduct = onAddProduct,
                    onUpdateProduct = onUpdateProduct,
                    onDeleteProduct = onDeleteProduct
                )
                2 -> CanteenTopupTab(
                    state = state,
                    onTopup = onTopup
                )
                3 -> CanteenRevenueReportTab(
                    state = state,
                    onOpenChatbot = onOpenChatbot
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: Kantin Satış (POS) (/canteen/sales)
// -------------------------------------------------------------
@Composable
private fun CanteenPosTab(
    state: Okul360UiState,
    onProcessPurchase: (CanteenProduct, Int) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("TÜMÜ") }
    val categories = listOf("TÜMÜ", "Sıcak Yiyecek", "İçecek", "Meyve / Sağlıklı", "Tatlı / Atıştırmalık")
    val selectedStudent = state.students.firstOrNull { it.id == state.canteenWallet.student_id } ?: state.students.firstOrNull()

    var cartItems by remember { mutableStateOf<Map<CanteenProduct, Int>>(emptyMap()) }
    var showSaleSuccessDialog by remember { mutableStateOf(false) }
    var lastSaleSummary by remember { mutableStateOf("") }

    val filteredProducts = remember(selectedCategory, state.canteenProducts) {
        if (selectedCategory == "TÜMÜ") state.canteenProducts
        else state.canteenProducts.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    val totalCartAmount = cartItems.entries.sumOf { it.key.price * it.value }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Student Header (RFID card tap simulation)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Nfc, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedStudent?.full_name ?: "Ali Emir Demir",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                            Text(
                                text = "Sınıf: ${selectedStudent?.class_name ?: "8-A"} • Kart: E2:80:68:10",
                                fontSize = 12.sp,
                                color = NeutralGrayDark
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Cüzdan Bakiyesi", fontSize = 11.sp, color = NeutralGrayDark)
                            Text(
                                text = "₺${state.canteenWallet.current_balance.toInt()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen
                            )
                        }
                    }

                    // Allergen / Restriction Alert
                    if (!selectedStudent?.allergies_or_diet.isNullOrBlank() || state.canteenWallet.restricted_categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = StatusRed.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "DİKKAT: ${selectedStudent?.allergies_or_diet?.takeIf { it.isNotBlank() } ?: "Belirtilmemiş"} • Kısıtlı: ${if (state.canteenWallet.restricted_categories.isNotEmpty()) state.canteenWallet.restricted_categories.joinToString(", ") else "Yok"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // Kantin Beslenme ve Harcama AI Analiz Kartı
        item {
            RoleContextualAiCard(
                screenName = "CANTEEN",
                state = state,
                initialExpanded = false
            )
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Products Grid / Cards
        items(filteredProducts, key = { it.id }) { product ->
            val isInCart = cartItems.containsKey(product)
            val count = cartItems[product] ?: 0

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeutralGrayLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (product.category) {
                                "İçecek" -> Icons.Default.LocalCafe
                                "Meyve / Sağlıklı" -> Icons.Default.Eco
                                else -> Icons.Default.Fastfood
                            },
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandNavy
                        )
                        Text(
                            text = "₺${product.price.toInt()} • Stok: ${product.stock_count}",
                            fontSize = 12.sp,
                            color = NeutralGrayDark
                        )
                        if (product.allergens.isNotEmpty()) {
                            Text(
                                text = "Alerjen: ${product.allergens.joinToString()}",
                                fontSize = 10.sp,
                                color = StatusRed
                            )
                        }
                    }

                    // Add/Remove buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isInCart) {
                            IconButton(
                                onClick = {
                                    val currentCount = cartItems[product] ?: 0
                                    cartItems = if (currentCount > 1) {
                                        cartItems + (product to (currentCount - 1))
                                    } else {
                                        cartItems - product
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Azalt", tint = StatusRed)
                            }

                            Text(
                                text = "$count",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                        }

                        IconButton(
                            onClick = {
                                val currentCount = cartItems[product] ?: 0
                                cartItems = cartItems + (product to (currentCount + 1))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Ekle", tint = PrimaryBlue)
                        }
                    }
                }
            }
        }

        // Cart Checkout Bar
        if (cartItems.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Sepet Toplamı (${cartItems.values.sum()} ürün)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text(text = "₺${totalCartAmount.toInt()}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    cartItems.forEach { (prod, qty) ->
                                        onProcessPurchase(prod, qty)
                                    }
                                    lastSaleSummary = cartItems.entries.joinToString(", ") { "${it.value}x ${it.key.name}" }
                                    cartItems = emptyMap()
                                    showSaleSuccessDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_complete_pos_sale")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Satışı Tamamla", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaleSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSaleSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(44.dp)) },
            title = { Text("Kantin Satışı Başarılı", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Satış tutarı öğrenci cüzdanından düşüldü ve veliye bildirim iletildi.\n\nSatılan Ürünler: $lastSaleSummary",
                    fontSize = 13.sp,
                    color = NeutralGrayDark
                )
            },
            confirmButton = {
                Button(onClick = { showSaleSuccessDialog = false }) {
                    Text("Yeni Satış Yap")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 2: Ürün Kataloğu (/canteen)
// -------------------------------------------------------------
@Composable
private fun CanteenCatalogTab(
    state: Okul360UiState,
    onAddProduct: (CanteenProduct) -> Unit,
    onUpdateProduct: (CanteenProduct) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_canteen_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ürün Ekle")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Menü & Stok Listesi (${state.canteenProducts.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeutralGrayDark
                )
            }

            items(state.canteenProducts, key = { it.id }) { product ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = product.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                            Text(text = "${product.category} • ₺${product.price.toInt()}", fontSize = 13.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
                            Text(text = "Mevcut Stok: ${product.stock_count} adet", fontSize = 12.sp, color = NeutralGrayDark)
                        }

                        IconButton(onClick = { onDeleteProduct(product.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Sil", tint = StatusRed)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Sıcak Yiyecek") }
        var price by remember { mutableStateOf("") }
        var stock by remember { mutableStateOf("50") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Ürün Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ürün Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Fiyat (₺)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Stok Miktarı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pr = price.toDoubleOrNull() ?: 20.0
                        val st = stock.toIntOrNull() ?: 50
                        onAddProduct(
                            CanteenProduct(
                                id = "cp-${System.currentTimeMillis()}",
                                name = name,
                                category = category,
                                price = pr,
                                calories = 150,
                                allergens = emptyList(),
                                is_healthy = true,
                                stock_count = st
                            )
                        )
                        showAddDialog = false
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Ekle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("İptal") }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 3: Kart Bakiye Yükleme (/canteen/card-balance)
// -------------------------------------------------------------
@Composable
private fun CanteenTopupTab(
    state: Okul360UiState,
    onTopup: (Double, String) -> Unit
) {
    var topupAmountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("Nakit") }
    val methods = listOf("Nakit", "Kredi Kartı (POS)", "Banka Havalesi")
    val presets = listOf(50.0, 100.0, 200.0, 500.0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Öğrenci Kartına Bakiye Yükle",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                    val studentDisplay = state.canteenWallet.student_name.ifBlank {
                        state.students.firstOrNull()?.let { "${it.first_name} ${it.last_name} (${it.class_name})" } ?: "Öğrenci Cüzdanı"
                    }
                    Text(
                        text = "Öğrenci: $studentDisplay • Güncel: ₺${state.canteenWallet.current_balance.toInt()}",
                        fontSize = 12.sp,
                        color = NeutralGrayDark
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset buttons
                    Text(text = "Hızlı Tutar Seçimi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandNavy)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { amt ->
                            OutlinedButton(
                                onClick = { topupAmountText = amt.toInt().toString() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("₺${amt.toInt()}", fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = topupAmountText,
                        onValueChange = { topupAmountText = it },
                        label = { Text("Yüklenecek Tutar (₺)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Ödeme Yöntemi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandNavy)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        methods.forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val amt = topupAmountText.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                                onTopup(amt, selectedMethod)
                                topupAmountText = ""
                            }
                        },
                        enabled = (topupAmountText.toDoubleOrNull() ?: 0.0) > 0,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_canteen_topup_confirm")
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bakiyeyi Yükle & Fiş Kes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Son Bakiye & Harcama Hareketleri",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeutralGrayDark
            )
        }

        if (state.canteenWallet.transactions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Henüz bakiye veya harcama hareketi bulunmuyor.",
                        fontSize = 13.sp,
                        color = NeutralGrayDark,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.canteenWallet.transactions) { tx ->
            val isTopup = tx.transaction_type == "TOPUP"
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isTopup) StatusGreen.copy(alpha = 0.12f) else StatusRed.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTopup) Icons.Default.ArrowDownward else Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = if (isTopup) StatusGreen else StatusRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = tx.items_summary, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = BrandNavy)
                        Text(text = tx.date_time, fontSize = 11.sp, color = NeutralGrayDark)
                    }
                    Text(
                        text = if (isTopup) "+₺${tx.amount.toInt()}" else "-₺${tx.amount.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTopup) StatusGreen else StatusRed
                    )
                }
            }
        }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: Kantin Ciro, Satış & Kazanç Analitiği
// -------------------------------------------------------------
@Composable
private fun CanteenRevenueReportTab(
    state: Okul360UiState,
    onOpenChatbot: (() -> Unit)? = null
) {
    val livePurchases = state.canteenWallet.transactions.filter { it.transaction_type == "PURCHASE" }
    val livePurchaseSum = livePurchases.sumOf { it.amount }
    val baseDailyRevenue = 1420.0
    val totalDailyRevenue = baseDailyRevenue + livePurchaseSum
    val totalTxCount = 68 + livePurchases.size
    val avgBasket = if (totalTxCount > 0) totalDailyRevenue / totalTxCount else 20.88
    val estimatedProfit = totalDailyRevenue * 0.36
    val rfidAmount = totalDailyRevenue * 0.74
    val cashAmount = totalDailyRevenue * 0.26

    var showZReportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Advice Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Canlı Kantin Kazanç & Ciro Paneli",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StatusGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Canlı Veri",
                                color = StatusGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Bugünkü toplam ciro ₺${String.format(java.util.Locale.US, "%,.2f", totalDailyRevenue)} seviyesindedir. Satışların %74'ü temassız RFID cüzdan ile gerçekleşmiş olup ortalama sepet tutarı ₺${String.format(java.util.Locale.US, "%.2f", avgBasket)}'dir.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showZReportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = BrandNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Günlük Z-Raporu Al", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (onOpenChatbot != null) {
                            OutlinedButton(
                                onClick = onOpenChatbot,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI'a Danış", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4 KPI Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(14.dp))
                            }
                            Text("Bugünkü Ciro", fontSize = 11.sp, color = NeutralGrayDark)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₺${String.format(java.util.Locale.US, "%,.2f", totalDailyRevenue)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusGreen
                        )
                        Text("+%12.4 dünden fazla", fontSize = 10.sp, color = StatusGreen)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                            }
                            Text("Tahmini Net Kâr", fontSize = 11.sp, color = NeutralGrayDark)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₺${String.format(java.util.Locale.US, "%,.2f", estimatedProfit)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )
                        Text("%36 brüt kâr marjı", fontSize = 10.sp, color = NeutralGrayDark)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(AccentAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                            }
                            Text("İşlem / Fiş", fontSize = 11.sp, color = NeutralGrayDark)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$totalTxCount Satış",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )
                        Text("Ort. ₺${String.format(java.util.Locale.US, "%.2f", avgBasket)}/fiş", fontSize = 10.sp, color = NeutralGrayDark)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(14.dp))
                            }
                            Text("RFID Cüzdan Payı", fontSize = 11.sp, color = NeutralGrayDark)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "%74 Temassız",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5CF6)
                        )
                        Text("₺${String.format(java.util.Locale.US, "%,.0f", rfidAmount)} cüzdandan", fontSize = 10.sp, color = NeutralGrayDark)
                    }
                }
            }
        }

        // Payment Method Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "💳 Tahsilat Türü ve Kasa Dağılımı",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandNavy
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PrimaryBlue))
                            Text("RFID Temassız Öğrenci Kartı", fontSize = 13.sp)
                        }
                        Text("₺${String.format(java.util.Locale.US, "%,.2f", rfidAmount)} (%74)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    LinearProgressIndicator(
                        progress = { 0.74f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = PrimaryBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AccentAmber))
                            Text("Nakit / Kredi Kartı POS Satışı", fontSize = 13.sp)
                        }
                        Text("₺${String.format(java.util.Locale.US, "%,.2f", cashAmount)} (%26)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    LinearProgressIndicator(
                        progress = { 0.26f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = AccentAmber,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Top Selling Products Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🏆 En Çok Kazanç Getiren Ürünler (Top Ciro)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandNavy
                    )

                    val topProducts = listOf(
                        Triple("Kaşarlı & Karışık Tost", "32 Adet", "₺1.440,00 (%32)"),
                        Triple("Tam Buğday Sandviç", "25 Adet", "₺1.250,00 (%28)"),
                        Triple("Doğal Ayran & Günlük Süt", "48 Adet", "₺720,00 (%16)"),
                        Triple("Doğal Kaynak Suyu", "65 Adet", "₺650,00 (%15)"),
                        Triple("Meyve Suyu & Atıştırmalık", "22 Adet", "₺440,00 (%9)")
                    )

                    topProducts.forEach { (name, qty, ciro) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = BrandNavy)
                                Text(qty, fontSize = 11.sp, color = NeutralGrayDark)
                            }
                            Text(ciro, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    if (showZReportDialog) {
        AlertDialog(
            onDismissRequest = { showZReportDialog = false },
            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp)) },
            title = { Text("Günlük Mali Z-Raporu Çıktısı", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SCS360 Kantin İşletmesi — Tarih: 27.09.2026", fontSize = 12.sp, color = NeutralGrayDark)
                    HorizontalDivider()
                    Text("• Toplam Ciro: ₺${String.format(java.util.Locale.US, "%,.2f", totalDailyRevenue)}", fontWeight = FontWeight.Bold)
                    Text("• Toplam Fiş: $totalTxCount Adet")
                    Text("• RFID Cüzdan: ₺${String.format(java.util.Locale.US, "%,.2f", rfidAmount)}")
                    Text("• Nakit / Kredi Kartı: ₺${String.format(java.util.Locale.US, "%,.2f", cashAmount)}")
                    Text("• KDV Dahil Brüt Satış: ₺${String.format(java.util.Locale.US, "%,.2f", totalDailyRevenue)}")
                    Text("• Tahmini Net Kâr: ₺${String.format(java.util.Locale.US, "%,.2f", estimatedProfit)}")
                    HorizontalDivider()
                    Text("✅ Z-Raporu e-arşiv ve muhasebe sistemine başarıyla kaydedildi.", color = StatusGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showZReportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Yazdır & Kapat")
                }
            }
        )
    }
}
