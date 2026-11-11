package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CanteenProduct
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.dialogs.ceppos.CepPosPaymentDialog
import com.example.ui.dialogs.iyzico.IyzicoPaymentDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CanteenWalletScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit,
    onTopup: (Double, String) -> Unit,
    onUpdateRestrictions: (Double, List<String>, List<String>) -> Unit,
    onProcessPurchase: (CanteenProduct, Int) -> Unit
) {
    val wallet = state.canteenWallet
    var showTopupDialog by remember { mutableStateOf(false) }
    var showRestrictionsDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val filteredProducts = remember(state.canteenProducts, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") state.canteenProducts
        else state.canteenProducts.filter { it.category.contains(selectedCategoryFilter, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kantin & Öğrenci Cüzdanı", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${wallet.student_name.ifBlank { "Öğrenci Cüzdanı" }} • Dijital Bakiye & Harcama Kontrolü",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("canteen_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showRestrictionsDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Limit & Alerjen Ayarları", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sade & Zarif Bakiye Kartı
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Cüzdan Bakiyesi",
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "₺${String.format("%.2f", wallet.current_balance)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Button(
                                onClick = { showTopupDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bakiye Yükle", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Günlük Limit & Kalan Göstergesi
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Günlük Limit: ₺${wallet.daily_spending_limit.toInt()}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Kalan: ₺${wallet.remainingDailyLimit.toInt()}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // AI Analiz & Beslenme Dengesi Kartı
            item {
                RoleContextualAiCard(
                    screenName = "CANTEEN",
                    state = state,
                    onQuickActionClick = { actionType ->
                        if (actionType == "TOPUP_WALLET") {
                            showTopupDialog = true
                        }
                    }
                )
            }

            // Sade Alerjen ve Kısıt Bilgi Şeridi
            if (wallet.allergen_warnings.isNotEmpty() || wallet.restricted_categories.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (wallet.allergen_warnings.isNotEmpty()) {
                                    Text(
                                        "Alerjen: ${wallet.allergen_warnings.joinToString(", ")}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CrimsonPrimary
                                    )
                                }
                                if (wallet.restricted_categories.isNotEmpty()) {
                                    Text(
                                        "Kısıtlı Ürün: ${wallet.restricted_categories.joinToString(", ")}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Kantin Menüsü Başlığı
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kantin Menüsü", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "${filteredProducts.size} Ürün",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Products Grid / List
            if (filteredProducts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Kantin menüsünde ürün bulunamadı.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                val isRestricted = wallet.restricted_categories.any { it.equals(product.category, ignoreCase = true) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRestricted) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (isRestricted) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = CrimsonPrimary.copy(alpha = 0.2f)) {
                                        Text("Kısıtlı", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.sp, color = CrimsonPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text("${product.category} • ${product.calories} kcal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (product.allergens.isNotEmpty()) {
                                Text("Alerjen: ${product.allergens.joinToString(", ")}", fontSize = 10.sp, color = AmberPrimary)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("₺${product.price.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { onProcessPurchase(product, 1) },
                                enabled = !isRestricted,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(38.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Satın Al", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            }

            // Recent Transactions Statement
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Harcama & Bakiye Ekstresi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (wallet.transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Kayıtlı harcama veya bakiye hareketi bulunmuyor.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(wallet.transactions, key = { it.id }) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (tx.transaction_type == "TOPUP") EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (tx.transaction_type == "TOPUP") Icons.Default.AddCard else Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = if (tx.transaction_type == "TOPUP") EmeraldPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tx.items_summary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(tx.date_time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val prefix = if (tx.transaction_type == "TOPUP") "+₺" else "-₺"
                            val color = if (tx.transaction_type == "TOPUP") EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                            Text("$prefix${tx.amount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                            Text("Bakiye: ₺${tx.balance_after.toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            }
        }
    }

    // Top-up Dialog with Cep POS and İyzico
    if (showTopupDialog) {
        var topupAmount by remember { mutableStateOf(200.0) }
        var showIyzicoModal by remember { mutableStateOf(false) }
        var showCepPosModal by remember { mutableStateOf(false) }
        var selectedProvider by remember { mutableStateOf("CEPPOS") } // CEPPOS or IYZICO

        if (showCepPosModal) {
            CepPosPaymentDialog(
                canteenTopupAmount = topupAmount,
                studentName = wallet.student_name,
                paymentDescription = "Kantin Cüzdanı ₺${topupAmount.toInt()} Bakiye Yükleme",
                onDismiss = {
                    showCepPosModal = false
                    showTopupDialog = false
                },
                onPaymentSuccess = { result, methodTitle ->
                    onTopup(topupAmount, "$methodTitle (Ref: ${result.rrn})")
                    showCepPosModal = false
                    showTopupDialog = false
                }
            )
        } else if (showIyzicoModal) {
            IyzicoPaymentDialog(
                canteenTopupAmount = topupAmount,
                studentName = wallet.student_name,
                paymentDescription = "Kantin Cüzdanı ₺${topupAmount.toInt()} Bakiye Yükleme",
                onDismiss = {
                    showIyzicoModal = false
                    showTopupDialog = false
                },
                onPaymentSuccess = { result, methodTitle ->
                    onTopup(topupAmount, "$methodTitle (Ref: ${result.paymentId})")
                    showIyzicoModal = false
                    showTopupDialog = false
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showTopupDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (selectedProvider == "CEPPOS") Color(0xFF059669) else Color(0xFF1E3A8A),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                if (selectedProvider == "CEPPOS") "Cep POS" else "iyzico",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("Kantin Cüzdanına Bakiye Yükle", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Yüklenecek Tutarı Seçin:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(100.0, 200.0, 500.0, 1000.0).forEach { amt ->
                                val isSelected = topupAmount == amt
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { topupAmount = amt }
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            "₺${amt.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Text("Ödeme / Yükleme Kanalı:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedProvider == "CEPPOS") Color(0xFFECFDF5) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedProvider == "CEPPOS") Color(0xFF059669) else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedProvider = "CEPPOS" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedProvider == "CEPPOS", onClick = { selectedProvider = "CEPPOS" })
                                    Text("Cep POS (NFC)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedProvider == "IYZICO") Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedProvider == "IYZICO") PrimaryBlue else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedProvider = "IYZICO" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedProvider == "IYZICO", onClick = { selectedProvider = "IYZICO" })
                                    Text("İyzico 3DS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                }
                            }
                        }

                        Surface(
                            color = if (selectedProvider == "CEPPOS") Color(0xFFF0FDF4) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (selectedProvider == "CEPPOS") "⚡ Cep POS: Kartı telefonun arkasına dokundurarak anında temassız yükleyin." else "🔒 İyzico Sandbox: Kredi kartı bilgileri ve 3D Secure SMS doğrulaması ile yükleyin.",
                                fontSize = 11.sp,
                                color = if (selectedProvider == "CEPPOS") Color(0xFF065F46) else Color(0xFFB45309),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedProvider == "CEPPOS") {
                                showCepPosModal = true
                            } else {
                                showIyzicoModal = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedProvider == "CEPPOS") Color(0xFF059669) else SecondaryEmerald
                        )
                    ) {
                        Text(
                            if (selectedProvider == "CEPPOS") "₺${topupAmount.toInt()} Cep POS ile Yükle" else "₺${topupAmount.toInt()} İyzico ile Yükle",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTopupDialog = false }) {
                        Text("İptal")
                    }
                }
            )
        }
    }

    // Restrictions Dialog
    if (showRestrictionsDialog) {
        var limitInput by remember { mutableStateOf(wallet.daily_spending_limit.toInt().toString()) }
        var selectedRestrictedCategories by remember { mutableStateOf(wallet.restricted_categories.toSet()) }
        var selectedAllergens by remember { mutableStateOf(wallet.allergen_warnings.toSet()) }

        val availableCategories = listOf("Gazlı İçecek", "Tatlı", "Meyve & Atıştırmalık", "Sıcak Yiyecek")
        val availableAllergens = listOf("Gluten", "Laktoz", "Ceviz", "Fındık", "Yumurta", "Fıstık", "Soya")

        AlertDialog(
            onDismissRequest = { showRestrictionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kantin Harcama & Alerjen Limiti", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Günlük Harcama Limiti (₺):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(50, 100, 150, 250).forEach { preset ->
                                val isSelected = limitInput == preset.toString()
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { limitInput = preset.toString() }
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            "₺$preset",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = limitInput,
                            onValueChange = { limitInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Özel Günlük Limit (₺)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🚫 Yasaklı / Kısıtlı Ürün Kategorileri:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                        Text("Öğrencinin bu kategorideki ürünleri satın alması engellenir.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableCategories.forEach { cat ->
                                val isRestricted = selectedRestrictedCategories.contains(cat)
                                FilterChip(
                                    selected = isRestricted,
                                    onClick = {
                                        selectedRestrictedCategories = if (isRestricted) {
                                            selectedRestrictedCategories - cat
                                        } else {
                                            selectedRestrictedCategories + cat
                                        }
                                    },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    leadingIcon = if (isRestricted) {
                                        { Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp), tint = CrimsonPrimary) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CrimsonPrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = CrimsonPrimary
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("⚠️ Öğrenci Alerjen Uyarıları:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberPrimary)
                        Text("İçeriğinde bu alerjenler bulunan ürünlerde kantin görevlisine sesli/görsel uyarı verilir.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableAllergens.forEach { allergen ->
                                val isSelected = selectedAllergens.contains(allergen)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedAllergens = if (isSelected) {
                                            selectedAllergens - allergen
                                        } else {
                                            selectedAllergens + allergen
                                        }
                                    },
                                    label = { Text(allergen, fontSize = 11.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberPrimary) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = Color(0xFF92400E)
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newLimit = limitInput.toDoubleOrNull() ?: wallet.daily_spending_limit
                        onUpdateRestrictions(newLimit, selectedRestrictedCategories.toList(), selectedAllergens.toList())
                        showRestrictionsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Ayarları Kaydet", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictionsDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }
}
