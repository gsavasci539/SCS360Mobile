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
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var student by remember { mutableStateOf<Student?>(null) }
    var query by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("14") }
    val fields = remember { mutableStateMapOf("title" to "", "author" to "", "category" to "", "isbn" to "", "barcode" to "", "total_copies" to "1") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var acknowledgement by remember { mutableStateOf("") }
    var uncertain by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (action == "BORROW" && canWrite && canStudents) {
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
                acknowledgement = when (action) {
                    "CREATE" -> {
                        val result = api.createBook(JSONObject().apply { fields.forEach { (key, value) -> put(key, if (key == "total_copies") value.toInt() else value.trim().ifBlank { null }) } })
                        "${result.title}: kitap kaydı sunucuda oluşturuldu."
                    }
                    "BORROW" -> {
                        check(canStudents); val result = api.borrow(requireNotNull(book).id, requireNotNull(selected), duration.toInt())
                        "${result.book_title}: ${result.student_name} için ödünç kaydı doğrulandı. Son tarih: ${result.due_date}"
                    }
                    "RETURN" -> { val result = api.returnBook(requireNotNull(loan).id); "${result.book_title}: iade sunucuda doğrulandı." }
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
                    item { Text(when (action) { "CREATE" -> "Yeni kitap"; "BORROW" -> "Kitap ödünç ver"; else -> "Kitap iadesi" }, style = MaterialTheme.typography.titleLarge) }
                    if (ack.isNotBlank()) item { Text(ack, Modifier.testTag("library_ack")) }
                    else {
                        if (!canWrite) item { Text("Kütüphanede işlem yapma yetkiniz yok.") }
                        when (action) {
                            "CREATE" -> items(fields.keys.toList()) { key -> OutlinedTextField(fields[key].orEmpty(), { fields[key] = it }, enabled = !busy, label = { Text(when(key) { "title" -> "Kitap adı"; "author" -> "Yazar"; "category" -> "Kategori"; "isbn" -> "ISBN"; "barcode" -> "Barkod"; else -> "Kopya sayısı" }) }, modifier = Modifier.fillMaxWidth().testTag("library_$key")) }
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
                            val ready = when (action) { "BORROW" -> canStudents && selected != null && duration.toIntOrNull() in 1..365; "CREATE" -> listOf("title","author","category").all { !fields[it].isNullOrBlank() } && fields["total_copies"]?.toIntOrNull() in 1..100000; "RETURN" -> loan != null && !loan.is_returned; else -> false }
                            Button(enabled = canWrite && !busy && !uncertain && ready, onClick = ::save, modifier = Modifier.heightIn(min = 48.dp).testTag("library_save")) { Text("Onayla") }
                        }
                    }
                    item { TextButton(enabled = !busy, onClick = onClose, modifier = Modifier.heightIn(min = 48.dp).testTag("library_close")) { Text("Kapat / listeyi yenile") } }
                }
            }
        }
    }
}
