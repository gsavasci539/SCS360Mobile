package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.data.remote.*
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun NativeLibraryScreen(state: Okul360UiState, onNavigateBack: () -> Unit) {
    key(financePresentationScope(state)) { NativeLibraryContent(state, onNavigateBack) }
}

@Composable
private fun NativeLibraryContent(state: Okul360UiState, onNavigateBack: () -> Unit) {
    val api = remember { NativeLibraryClient() }
    var books by remember { mutableStateOf<List<LibraryBook>>(emptyList()) }
    var loans by remember { mutableStateOf<List<BookLoanRecord>>(emptyList()) }
    var revision by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<LibraryBook?>(null) }
    var selectedLoan by remember { mutableStateOf<BookLoanRecord?>(null) }
    val canRead = NativeNavigationPolicy.has(state.currentUser, "library.read")
    val canWrite = NativeNavigationPolicy.has(state.currentUser, "library.write")
    LaunchedEffect(revision, canRead) {
        if (!canRead) { loading = false; return@LaunchedEffect }
        loading = true; error = ""
        try {
            val nextBooks = api.books(); val nextLoans = api.loans()
            if (api.current()) { books = nextBooks; loans = nextLoans }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { if (api.current()) error = failure.message ?: "Kütüphane listesi alınamadı" }
        finally { if (api.current()) loading = false }
    }
    if (!api.current()) return
    Column(Modifier.fillMaxSize().testTag("native_library")) {
        if (!canRead) {
            Text("Kütüphane kayıtlarını görme yetkiniz yok.", Modifier.padding(16.dp))
            TextButton(onClick = onNavigateBack) { Text("Geri") }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(enabled = !loading, onClick = { revision++ }, modifier = Modifier.heightIn(min = 48.dp).testTag("library_refresh")) { Text("Listeyi yenile") }
                if (loading) CircularProgressIndicator(Modifier.padding(8.dp).size(28.dp))
            }
            if (error.isNotBlank()) Text("$error. Listeyi yenileyin; işlemler kapalı.", Modifier.padding(12.dp).testTag("library_error"))
            if ((loading || error.isNotBlank()) && books.isEmpty() && loans.isEmpty()) {
                if (loading) Text("Kütüphane kayıtları yükleniyor.", Modifier.padding(12.dp))
                TextButton(onClick = onNavigateBack) { Text("Geri") }
            } else LibraryScreen(state.copy(libraryBooks = books, bookLoans = loans), onNavigateBack,
                onBorrowBook = { selectedBook = it; action = "BORROW" },
                onBookDetails = { selectedBook = it; action = "DETAIL" },
                onReturnBook = { key -> selectedLoan = loans.singleOrNull { it.id == key }; if (selectedLoan != null) action = "RETURN" },
                canWrite = canWrite && !loading && error.isBlank(), onCreateBook = { action = "CREATE" })
        }
    }
    if (action.isNotBlank()) NativeLibraryMutationDialog(state, action, selectedBook, selectedLoan,
        onClose = { action = ""; selectedBook = null; selectedLoan = null; revision++ })
}

@Composable
internal fun NativeLibraryMutationDialog(state: Okul360UiState, action: String, book: LibraryBook?, loan: BookLoanRecord?, onClose: () -> Unit) {
    val api = remember { NativeLibraryClient() }; val coroutine = rememberCoroutineScope()
    val canWrite = NativeNavigationPolicy.has(state.currentUser, "library.write")
    val canStudents = NativeNavigationPolicy.has(state.currentUser, "student.read", "students.read")
    var currentAction by remember(action, book?.id, loan?.id) { mutableStateOf(action) }
    var detailBook by remember(book?.id) { mutableStateOf<LibraryBook?>(null) }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var student by remember { mutableStateOf<Student?>(null) }
    var query by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("14") }
    val fields = remember(currentAction, book?.id) { mutableStateMapOf("title" to "", "author" to "", "category" to "", "isbn" to "", "barcode" to "", "publisher" to "", "publication_year" to "", "total_copies" to "1", "description" to "", "location" to "") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var acknowledgement by remember { mutableStateOf("") }
    var uncertain by remember { mutableStateOf(false) }
    LaunchedEffect(currentAction, book?.id) {
        if (currentAction in setOf("DETAIL", "EDIT") && book != null) {
            busy = true; error = ""
            try {
                val loaded = api.book(book.id)
                if (api.current()) {
                    detailBook = loaded
                    for ((key, value) in mapOf(
                        "title" to loaded.title, "author" to loaded.author, "category" to loaded.category,
                        "isbn" to loaded.isbn, "barcode" to loaded.barcode, "publisher" to loaded.publisher,
                        "publication_year" to (loaded.publication_year?.toString() ?: ""),
                        "total_copies" to loaded.total_copies.toString(), "description" to loaded.description,
                        "location" to loaded.shelf_location
                    )) fields[key] = value
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (api.current()) error = failure.message ?: "Kitap ayrıntısı alınamadı" }
            finally { if (api.current()) busy = false }
        } else if (currentAction == "BORROW" && canWrite && canStudents) {
            busy = true
            try { students = NativeRegistryClient().students(state.selectedBranchId.takeUnless { it.isBlank() || it == "all" }, null, null) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (api.current()) error = failure.message ?: "Öğrenci listesi alınamadı" }
            finally { if (api.current()) busy = false }
        }
    }
    val shownStudents = students.filter { it.full_name.contains(query, true) || it.student_number.contains(query, true) }
    val selected = student; val ack = acknowledgement
    fun save() {
        if (busy || uncertain || ack.isNotBlank() || !canWrite || !api.current()) return
        busy = true; error = ""
        coroutine.launch {
            try {
                acknowledgement = when (currentAction) {
                    "CREATE" -> {
                        val result = api.createBook(JSONObject().apply {
                            for (key in listOf("title", "author", "category", "isbn", "barcode", "total_copies")) {
                                val value = fields[key].orEmpty().trim()
                                put(key, if (key == "total_copies") value.toInt() else value.ifBlank { null })
                            }
                        })
                        "${result.title}: kitap kaydı sunucuda oluşturuldu."
                    }
                    "EDIT" -> {
                        val current = requireNotNull(detailBook)
                        val body = JSONObject().apply {
                            put("title", fields["title"].orEmpty().trim()); put("author", fields["author"].orEmpty().trim())
                            put("category", fields["category"].orEmpty().trim())
                            put("isbn", fields["isbn"].orEmpty().trim().let { if (it.isBlank()) JSONObject.NULL else it })
                            put("publisher", fields["publisher"].orEmpty().trim().let { if (it.isBlank()) JSONObject.NULL else it })
                            put("publication_year", fields["publication_year"].orEmpty().trim().toIntOrNull() ?: JSONObject.NULL)
                            put("total_copies", fields["total_copies"].orEmpty().toInt())
                            put("description", fields["description"].orEmpty().trim().let { if (it.isBlank()) JSONObject.NULL else it })
                            put("location", fields["location"].orEmpty().trim().let { if (it.isBlank()) JSONObject.NULL else it })
                        }
                        val updated = api.updateBook(current.id, current.updated_at, body)
                        detailBook = updated
                        "${updated.title}: kitap kaydı sunucuda güncellendi."
                    }
                    "BORROW" -> {
                        check(canStudents); val result = api.borrow(requireNotNull(book).id, requireNotNull(selected), duration.toInt())
                        "${result.book_title}: ${result.student_name} için ödünç kaydı doğrulandı. Son tarih: ${result.due_date}"
                    }
                    "RETURN" -> { val result = api.returnBook(requireNotNull(loan).id); "${result.book_title}: iade sunucuda doğrulandı." }
                    "DELETE_CONFIRM" -> {
                        api.deleteBook(requireNotNull(detailBook).id)
                        "${detailBook?.title.orEmpty()}: kitap sunucudan silindi."
                    }
                    else -> throw IllegalArgumentException("Desteklenmeyen işlem")
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (api.current()) {
                    uncertain = failure !is IllegalArgumentException && (failure as? RegistryHttpException)?.code !in listOf(400,403,404,409,422)
                    error = if (uncertain) "İşlemin sonucu doğrulanamadı. Yeniden göndermeden önce kapatıp sunucu listesini kontrol edin." else failure.message ?: "İşlem reddedildi"
                }
            } finally { if (api.current()) busy = false }
        }
    }
    if (!api.current()) return
    Dialog(onDismissRequest = { if (!busy) onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 600.dp).fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .9f).dp).imePadding()) {
            key(ack.isNotBlank()) {
                LazyColumn(Modifier.padding(16.dp).testTag("library_form"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text(when (currentAction) { "CREATE" -> "Yeni kitap"; "EDIT" -> "Kitabı düzenle"; "DETAIL" -> "Kitap ayrıntısı"; "DELETE_CONFIRM" -> "Kitabı sil"; "BORROW" -> "Kitap ödünç ver"; else -> "Kitap iadesi" }, style = MaterialTheme.typography.titleLarge) }
                    if (ack.isNotBlank()) item { Text(ack, Modifier.testTag("library_ack")) }
                    else {
                        if (!canWrite && currentAction in setOf("CREATE", "EDIT", "DELETE_CONFIRM", "BORROW", "RETURN")) item { Text("Kütüphanede işlem yapma yetkiniz yok.") }
                        when (currentAction) {
                            "DETAIL" -> {
                                if (busy && detailBook == null) item { CircularProgressIndicator(Modifier.testTag("library_detail_loading")) }
                                detailBook?.let { row ->
                                    val details = listOf("Yazar" to row.author, "Kategori" to row.category, "ISBN" to row.isbn,
                                        "Barkod" to row.barcode, "Yayınevi" to row.publisher,
                                        "Yayın yılı" to (row.publication_year?.toString() ?: "—"),
                                        "Toplam / mevcut" to "${row.total_copies} / ${row.available_copies}",
                                        "Raf" to row.shelf_location, "Açıklama" to row.description)
                                    items(details) { (label, value) -> Text("$label: ${value.ifBlank { "—" }}", Modifier.testTag("library_detail_$label")) }
                                    if (canWrite) item {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(enabled = !busy, onClick = { currentAction = "EDIT" }, modifier = Modifier.heightIn(min = 48.dp).testTag("library_edit")) { Text("Düzenle") }
                                            TextButton(enabled = !busy, onClick = { currentAction = "DELETE_CONFIRM" }, modifier = Modifier.heightIn(min = 48.dp).testTag("library_delete")) { Text("Sil") }
                                        }
                                    }
                                }
                            }
                            "EDIT" -> {
                                if (busy && detailBook == null) item { CircularProgressIndicator(Modifier.testTag("library_edit_loading")) }
                                if (detailBook != null) items(listOf("title", "author", "category", "isbn", "publisher", "publication_year", "total_copies", "location", "description")) { key ->
                                    OutlinedTextField(fields[key].orEmpty(), { fields[key] = it }, enabled = !busy,
                                        label = { Text(when(key) { "title" -> "Kitap adı"; "author" -> "Yazar"; "category" -> "Kategori"; "isbn" -> "ISBN"; "publisher" -> "Yayınevi"; "publication_year" -> "Yayın yılı"; "total_copies" -> "Toplam kopya"; "location" -> "Raf"; else -> "Açıklama" }) },
                                        modifier = Modifier.fillMaxWidth().testTag("library_$key"))
                                }
                            }
                            "DELETE_CONFIRM" -> {
                                item { Text("Ödünç geçmişi bulunan kitaplar silinemez. Sunucu silme isteğini reddederse kayıt ve geçmiş korunur.") }
                                detailBook?.let { item { Text(it.title, Modifier.testTag("library_delete_title")) } }
                            }
                            "CREATE" -> items(listOf("title", "author", "category", "isbn", "barcode", "total_copies")) { key -> OutlinedTextField(fields[key].orEmpty(), { fields[key] = it }, enabled = !busy, label = { Text(when(key) { "title" -> "Kitap adı"; "author" -> "Yazar"; "category" -> "Kategori"; "isbn" -> "ISBN"; "barcode" -> "Barkod"; else -> "Kopya sayısı" }) }, modifier = Modifier.fillMaxWidth().testTag("library_$key")) }
                            "BORROW" -> {
                                item { Text(book?.title.orEmpty()) }
                                if (!canStudents) item { Text("Öğrenci seçmek için öğrenci okuma yetkisi gerekir.") }
                                item { OutlinedTextField(query, { query = it }, enabled = !busy, label = { Text("Öğrenci ara") }, modifier = Modifier.fillMaxWidth().testTag("library_student_search")) }
                                item { Text(selected?.let { "Seçilen: ${it.full_name}" } ?: "Öğrenci seçilmedi", Modifier.testTag("library_selected_student")) }
                                items(shownStudents, key = { it.id }) { row -> TextButton(enabled = !busy, modifier = Modifier.heightIn(min = 48.dp).testTag("library_student_${row.id}"), onClick = { student = row }) { Text(listOf(row.full_name, row.student_number).filter { it.isNotBlank() }.joinToString(" · ")) } }
                                item { OutlinedTextField(duration, { duration = it }, enabled = !busy, label = { Text("Ödünç süresi (1–365 gün)") }, modifier = Modifier.fillMaxWidth().testTag("library_duration")) }
                            }
                            "RETURN" -> item { Text("${loan?.book_title.orEmpty()} — ${loan?.student_name.orEmpty()} için iade alınacak.") }
                        }
                        item { if (error.isNotBlank()) Text(error, Modifier.testTag("library_mutation_error")) }
                        item {
                            val ready = when (currentAction) {
                                "BORROW" -> canStudents && selected != null && duration.toIntOrNull() in 1..365
                                "CREATE" -> listOf("title", "author", "category").all { !fields[it].isNullOrBlank() } && fields["total_copies"]?.toIntOrNull() in 1..100000
                                "EDIT" -> detailBook != null && fields["title"]?.isNotBlank() == true && fields["author"]?.isNotBlank() == true && fields["category"]?.isNotBlank() == true && fields["total_copies"]?.toIntOrNull() in 1..100000 && (fields["publication_year"].isNullOrBlank() || fields["publication_year"]?.toIntOrNull() != null)
                                "DELETE_CONFIRM" -> detailBook != null
                                "RETURN" -> loan != null && !loan.is_returned
                                else -> false
                            }
                            if (currentAction !in setOf("DETAIL")) Button(enabled = canWrite && !busy && !uncertain && ready, onClick = ::save, modifier = Modifier.heightIn(min = 48.dp).testTag(if (currentAction == "DELETE_CONFIRM") "library_delete_confirm" else "library_save")) { Text(if (currentAction == "EDIT") "Değişiklikleri kaydet" else if (currentAction == "DELETE_CONFIRM") "Kitabı sil" else "Onayla") }
                        }
                    }
                    item { TextButton(enabled = !busy, onClick = onClose, modifier = Modifier.heightIn(min = 48.dp).testTag("library_close")) { Text("Kapat / listeyi yenile") } }
                }
            }
        }
    }
}
