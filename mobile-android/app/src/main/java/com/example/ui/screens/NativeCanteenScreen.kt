package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.*
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Composable
internal fun NativeCanteenScreen(state: Okul360UiState, onBack: (() -> Unit)? = null, onLogout: (() -> Unit)? = null) {
    val user = state.currentUser
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, "canteen.read") ||
        (user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank())) {
        Column(Modifier.padding(16.dp)) {
            Text("Kantin için yetkili kurum çalışma alanını açın.")
            onBack?.let { TextButton(onClick = it) { Text("Geri") } }
            onLogout?.let { TextButton(onClick = it) { Text("Çıkış yap") } }
        }
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl) {
        val client = remember { NativeCanteenClient(state.selectedBranchId) }
        CanteenWorkspace(client, NativeNavigationPolicy.has(user, "canteen.write"), onBack, onLogout)
    }
}

@Composable
private fun CanteenWorkspace(client: NativeCanteenClient, canWrite: Boolean, onBack: (() -> Unit)?, onLogout: (() -> Unit)?) {
    var choosing by remember { mutableStateOf(false) }
    var student by remember { mutableStateOf<CanteenPerson?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var topUp by remember { mutableStateOf(false) }
    var changingLimits by remember { mutableStateOf(false) }
    var product by remember { mutableStateOf<RecordedCanteenProduct?>(null) }
    var catalogBranch by remember { mutableStateOf<CanteenPerson?>(null) }
    var choosingBranch by remember { mutableStateOf(false) }
    var catalogAvailable by remember { mutableStateOf(true) }
    var managingProduct by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<RecordedCanteenProduct?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val coroutine = rememberCoroutineScope()
    val walletResponse by canteenLoad(student?.id, revision) { student?.let { client.wallet(it.id) } }
    val wallet = walletResponse?.getOrNull()
    val products by canteenLoad(wallet?.student, wallet?.branch, tab, page, revision) {
        if (wallet != null && tab == 0) client.products(wallet, 21, page * 20) else emptyList()
    }
    val history by canteenLoad(wallet?.student, wallet?.branch, tab, page, revision) {
        if (wallet != null && tab == 1) client.history(wallet, 21, page * 20) else emptyList()
    }
    val branch = catalogBranch?.id ?: client.fixedBranch
    val catalog by canteenLoad(branch, catalogAvailable, tab, page, revision) {
        if (branch != null && tab == 2) client.catalog(branch, catalogAvailable, 21, page * 20) else emptyList()
    }
    fun write(action: suspend () -> String) {
        if (!canWrite || busy) return
        busy = true
        message = null
        coroutine.launch {
            try {
                message = action()
                revision++
                page = 0
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                message = "Kantin işlemi doğrulanamadı. ${failure.message.orEmpty()}"
            } finally { busy = false }
        }
    }

    LazyColumn(Modifier.fillMaxSize().testTag("native_canteen_workspace"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Kantin ve Öğrenci Cüzdanı", style = MaterialTheme.typography.headlineSmall)
            onBack?.let { TextButton(enabled = !busy, onClick = it) { Text("Geri") } }
            onLogout?.let { TextButton(onClick = it) { Text("Çıkış yap") } }
            if (tab != 2) Button(enabled = !busy, onClick = { choosing = true }) { Text(student?.name ?: "Öğrenci seç") }
            TextButton(enabled = !busy, onClick = { revision++; message = null }) { Text("Kantin kayıtlarını yenile") }
            message?.let { Text(it) }
            TabRow(tab) {
                listOf("Satış", "Cüzdan", "Katalog").forEachIndexed { index, label ->
                    Tab(tab == index, { if (!busy) { tab = index; page = 0 } }, text = { Text(label) })
                }
            }
            if (tab != 2 && student != null && wallet == null) Text(canteenMessage(walletResponse, "Cüzdan bilgisi bulunamadı."))
            if (tab != 2 && wallet != null) {
                Text(wallet.name, style = MaterialTheme.typography.titleMedium)
                Text("Güncel bakiye ${canteenMoney(wallet.balance)}")
                Text("Bugünkü harcama ${canteenMoney(wallet.spent)}")
                wallet.limit?.let { Text("Günlük limit ${canteenMoney(it)}") }
                if (wallet.dietary.isNotEmpty()) Text("Beslenme kısıtları ${wallet.dietary.joinToString()}")
                if (wallet.categories.isNotEmpty()) Text("Kısıtlı kategoriler ${wallet.categories.joinToString()}")
                if (canWrite) {
                    TextButton(enabled = !busy, onClick = { topUp = true; message = null }) { Text("Tahsil edilmiş bakiyeyi kaydet") }
                    TextButton(enabled = !busy, onClick = { changingLimits = true; message = null }) { Text("Cüzdan kısıtlarını düzenle") }
                }
            }
        }
        if (wallet != null && tab == 0) {
            val rows = products?.getOrNull().orEmpty()
            if (rows.isEmpty()) item { Text(canteenMessage(products, "Bu şubede satışa açık ürün yok.")) }
            items(rows.take(20), key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.name)
                        Text(canteenMoney(item.price))
                        if (item.category.isNotBlank()) Text(item.category)
                        if (item.allergens.isNotEmpty()) Text("Alerjenler ${item.allergens.joinToString()}")
                        if (canWrite) TextButton(enabled = !busy, onClick = { product = item; message = null }) { Text("${item.name} satışı") }
                    }
                }
            }
            if (page > 0 || rows.size > 20) item { CanteenPager(page, rows.size, busy) { page = it } }
        }
        if (wallet != null && tab == 1) {
            val rows = history?.getOrNull().orEmpty()
            if (rows.isEmpty()) item { Text(canteenMessage(history, "Kayıtlı cüzdan hareketi yok.")) }
            items(rows.take(20), key = { it.id }) { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(when (entry.type) { "CREDIT" -> "Bakiye kaydı"; "DEBIT" -> "Cüzdan satışı"; "SALE" -> "Satış"; else -> entry.type })
                        Text(canteenMoney(entry.amount))
                        if (entry.description.isNotBlank()) Text(entry.description)
                        Text(entry.date)
                        Text(if (entry.status == "COMPLETED") "Tamamlandı" else entry.status)
                    }
                }
            }
            if (page > 0 || rows.size > 20) item { CanteenPager(page, rows.size, busy) { page = it } }
        }
        if (tab == 2) {
            item {
                if (client.fixedBranch == null) Button(enabled = !busy, onClick = { choosingBranch = true }) {
                    Text(catalogBranch?.name ?: "Katalog şubesini seç")
                }
                if (branch != null) {
                    FilterChip(catalogAvailable, { catalogAvailable = true; page = 0 }, { Text("Satışa açık") }, enabled = !busy)
                    FilterChip(!catalogAvailable, { catalogAvailable = false; page = 0 }, { Text("Satışa kapalı") }, enabled = !busy)
                    if (canWrite) TextButton(enabled = !busy, onClick = {
                        selectedProduct = null; managingProduct = true; message = null
                    }) { Text("Ürün ekle") }
                }
            }
            if (branch != null) {
                val rows = catalog?.getOrNull().orEmpty()
                if (rows.isEmpty()) item { Text(canteenMessage(catalog, "Bu şubede eşleşen ürün yok.")) }
                items(rows.take(20), key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.name)
                            Text(canteenMoney(item.price))
                            if (item.category.isNotBlank()) Text(item.category)
                            if (item.description.isNotBlank()) Text(item.description)
                            if (canWrite && item.updatedAt.isNotBlank()) TextButton(enabled = !busy, onClick = {
                                selectedProduct = item; managingProduct = true; message = null
                            }) { Text("${item.name} ürününü düzenle") }
                        }
                    }
                }
                if (page > 0 || rows.size > 20) item { CanteenPager(page, rows.size, busy) { page = it } }
            }
        }
    }
    if (choosing) {
        var search by remember { mutableStateOf("") }
        val choices by canteenLoad(search) {
            if (search.isNotBlank()) delay(250)
            client.people(search.trim())
        }
        ConsentFormDialog("Kantin öğrencisi seç", "native_canteen_students", false, { choosing = false }, fields = {
            item { OutlinedTextField(search, { search = it.take(100) }, modifier = Modifier.fillMaxWidth(), label = { Text("Öğrenci ara") }) }
            val options = choices?.getOrNull()
            if (options == null || options.students.isEmpty()) item { Text(canteenMessage(choices, "Öğrenci bulunamadı.")) }
            if (options != null) {
                items(options.students, key = { it.id }) { person ->
                    TextButton(onClick = { student = person; page = 0; choosing = false; message = null }) { Text(person.name) }
                }
                if (options.more) item { Text("Daha fazla sonuç için aramayı daraltın.") }
            }
        }, confirm = {})
    }
    if (canWrite && topUp && wallet != null) CanteenPaymentDialog(wallet, null, busy, message, { topUp = false }) { method, amount, _, reference, request ->
        write { client.topUp(wallet, requireNotNull(amount), method, reference, request); topUp = false; "Bakiye kaydı doğrulandı." }
    }
    val purchase = product
    if (canWrite && purchase != null && wallet != null) CanteenPaymentDialog(wallet, purchase, busy, message, { product = null }) { method, _, quantity, _, request ->
        write { client.sell(wallet, purchase, requireNotNull(quantity), method, request); product = null; "Kantin satışı sunucudan doğrulandı." }
    }
    if (choosingBranch) {
        val choices by canteenLoad(Unit) { client.people("").branches }
        ConsentFormDialog("Katalog şubesi seç", "native_canteen_branches", false, { choosingBranch = false }, fields = {
            val rows = choices?.getOrNull().orEmpty()
            if (rows.isEmpty()) item { Text(canteenMessage(choices, "Erişilebilir şube bulunamadı.")) }
            items(rows, key = { it.id }) { row ->
                TextButton(onClick = { catalogBranch = row; page = 0; choosingBranch = false; message = null }) { Text(row.name) }
            }
        }, confirm = {})
    }
    if (canWrite && changingLimits && wallet != null) CanteenLimitsDialog(wallet, busy, message, { changingLimits = false }) { limit, categories, allergens ->
        write {
            client.setRestrictions(wallet, limit, categories, allergens)
            changingLimits = false
            "Cüzdan kısıtları sunucudan doğrulandı."
        }
    }
    if (canWrite && managingProduct && branch != null) {
        CanteenProductEditor(selectedProduct, busy, message, { managingProduct = false }) { values, request ->
            write {
                client.saveProduct(branch, values, selectedProduct, request)
                managingProduct = false
                catalogAvailable = values.available
                "Ürün kaydı sunucudan doğrulandı."
            }
        }
    }
}

@Composable
private fun CanteenLimitsDialog(wallet: RecordedCanteenWallet, busy: Boolean, message: String?, close: () -> Unit,
    save: (BigDecimal?, List<String>, List<String>) -> Unit) {
    var amount by remember { mutableStateOf(wallet.limit?.toPlainString().orEmpty()) }
    var categories by remember { mutableStateOf(wallet.categories.joinToString()) }
    var allergens by remember { mutableStateOf(wallet.dietary.joinToString()) }
    val value = canteenAmount(amount)
    ConsentFormDialog("Cüzdan kısıtları", "native_canteen_limits", busy, close, fields = {
        item { CanteenEditorField(amount, "Günlük limit (isteğe bağlı)", busy, 11) { amount = it } }
        item { CanteenEditorField(categories, "Kısıtlı kategoriler, virgülle ayırın", busy, 1000) { categories = it } }
        item { CanteenEditorField(allergens, "Alerjen kodları, virgülle ayırın", busy, 1000) { allergens = it } }
        message?.let { item { Text(it) } }
    }, confirm = {
        Button(enabled = !busy && (amount.isBlank() || value != null), onClick = {
            save(value, canteenTokens(categories), canteenTokens(allergens))
        }) { Text("Kısıtları kaydet") }
    })
}

@Composable
private fun CanteenProductEditor(existing: RecordedCanteenProduct?, busy: Boolean, message: String?, close: () -> Unit,
    save: (CanteenProductDraft, UUID) -> Unit) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category.orEmpty()) }
    var price by remember { mutableStateOf(existing?.price?.toPlainString().orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var image by remember { mutableStateOf(existing?.image.orEmpty()) }
    var allergens by remember { mutableStateOf(existing?.allergens?.joinToString().orEmpty()) }
    var available by remember { mutableStateOf(existing?.available ?: true) }
    val value = canteenAmount(price)
    val request = remember(name, category, price, description, image, allergens, available) { UUID.randomUUID() }
    ConsentFormDialog(if (existing == null) "Kantin ürünü ekle" else "Kantin ürününü düzenle",
        "native_canteen_product", busy, close, fields = {
            item { CanteenEditorField(name, "Ürün adı", busy, 255) { name = it } }
            item { CanteenEditorField(category, "Kategori", busy, 100) { category = it } }
            item { CanteenEditorField(price, "Birim fiyat", busy, 11) { price = it } }
            item { CanteenEditorField(description, "Açıklama", busy, 1000) { description = it } }
            item { CanteenEditorField(image, "Görsel bağlantısı", busy, 500) { image = it } }
            item { CanteenEditorField(allergens, "Alerjen kodları, virgülle ayırın", busy, 1000) { allergens = it } }
            item { FilterChip(available, { available = !available }, { Text(if (available) "Satışa açık" else "Satışa kapalı") }, enabled = !busy) }
            message?.let { item { Text(it) } }
        }, confirm = {
            Button(enabled = !busy && name.isNotBlank() && value?.signum() == 1, onClick = {
                save(CanteenProductDraft(name.trim(), category.trim(), requireNotNull(value), canteenTokens(allergens),
                    description.trim(), image.trim(), available), request)
            }) { Text("Ürünü kaydet") }
        })
}

@Composable
private fun CanteenEditorField(value: String, label: String, busy: Boolean, max: Int, change: (String) -> Unit) {
    OutlinedTextField(value, { change(it.take(max)) }, enabled = !busy, singleLine = max < 1000,
        modifier = Modifier.fillMaxWidth(), label = { Text(label) })
}

private fun canteenTokens(input: String) = input.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
private fun canteenAmount(input: String) = input.trim().replace(',', '.')
    .takeIf { it.matches(Regex("\\d{1,8}(\\.\\d{1,2})?")) }?.toBigDecimalOrNull()

@Composable
private fun CanteenPaymentDialog(wallet: RecordedCanteenWallet, product: RecordedCanteenProduct?, busy: Boolean,
    message: String?, close: () -> Unit, save: (String, BigDecimal?, Int?, String, UUID) -> Unit) {
    var input by remember { mutableStateOf(if (product == null) "" else "1") }
    var method by remember { mutableStateOf(if (product == null) "CASH" else "WALLET") }
    var reference by remember { mutableStateOf("") }
    val amount = canteenAmount(input)
    val quantity = input.toIntOrNull()
    val request = remember(input, method, reference) { UUID.randomUUID() }
    val methods = if (product == null) listOf("CASH" to "Nakit", "CARD" to "Kart", "BANK_TRANSFER" to "Havale")
        else listOf("WALLET" to "Öğrenci cüzdanı", "CASH" to "Nakit", "CARD" to "Kart")
    val valid = if (product == null) amount != null && amount.signum() > 0 else quantity != null && quantity > 0
    ConsentFormDialog(if (product == null) "Bakiye kaydı" else "Kantin satışı", "native_canteen_payment", busy, close, fields = {
        item { Text(wallet.name) }
        if (product != null) item {
            Text(product.name)
            Text("Birim fiyat ${canteenMoney(product.price)}")
            if (quantity != null && quantity > 0) Text("Toplam ${canteenMoney(product.price.multiply(quantity.toBigDecimal()))}")
        }
        item {
            OutlinedTextField(input, { input = it.take(if (product == null) 11 else 10) }, modifier = Modifier.fillMaxWidth(),
                label = { Text(if (product == null) "Tahsil edilmiş tutar" else "Adet") }, singleLine = true, enabled = !busy)
        }
        methods.forEach { (value, label) ->
            item { FilterChip(method == value, { method = value }, { Text(label) }, enabled = !busy) }
        }
        if (product == null) item {
            OutlinedTextField(reference, { reference = it.take(100) }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Tahsilat referansı (isteğe bağlı)") }, singleLine = true, enabled = !busy)
        }
        if (method != "WALLET") item { Text("Yalnız tahsil edilmiş işlemi kaydedin.") }
        message?.let { item { Text(it) } }
    }, confirm = {
        Button(enabled = !busy && valid, onClick = { save(method, amount, quantity, reference.trim(), request) }) {
            Text(if (product == null) "Bakiye kaydını onayla" else "Satış kaydını onayla")
        }
    })
}

@Composable
private fun CanteenPager(page: Int, count: Int, busy: Boolean, change: (Int) -> Unit) {
    Text("Sayfa ${page + 1}")
    if (page > 0) TextButton(enabled = !busy, onClick = { change(page - 1) }) { Text("Önceki sayfa") }
    if (count > 20) TextButton(enabled = !busy, onClick = { change(page + 1) }) { Text("Sonraki sayfa") }
}

private fun canteenMoney(amount: BigDecimal) = amount.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " ₺"
private fun canteenMessage(result: Result<*>?, empty: String) = when {
    result == null -> "Kantin kayıtları yükleniyor…"
    result.isFailure -> "Kantin kayıtları yüklenemedi."
    else -> empty
}

@Composable
private fun <T> canteenLoad(vararg keys: Any?, load: suspend () -> T): State<Result<T>?> = key(keys.toList()) {
    produceState<Result<T>?>(null) {
        value = try { Result.success(load()) } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            Result.failure(failure)
        }
    }
}
