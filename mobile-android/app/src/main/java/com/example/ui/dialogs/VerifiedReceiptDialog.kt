package com.example.ui.dialogs

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.print.*
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.*
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

@Composable
fun VerifiedReceiptDialog(debtId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val client = remember(debtId) { runCatching { NativeReceiptClient(debtId) }.getOrNull() }
    var rows by remember { mutableStateOf<List<VerifiedReceipt>?>(null) }
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }; val scope = rememberCoroutineScope()
    val currency = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("tr-TR")) }
    LaunchedEffect(client, revision) {
        busy = true; error = null; rows = null
        try { rows = checkNotNull(client).load() }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Makbuz kayıtları doğrulanamadı. Yetki ve bağlantınızı kontrol edin." }
        finally { busy = false }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 600.dp).fillMaxWidth()
            .heightIn(max = (LocalConfiguration.current.screenHeightDp * .9f).dp), shape = MaterialTheme.shapes.large) {
            LazyColumn(Modifier.padding(16.dp).testTag("receipt_list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Sunucu makbuzları", style = MaterialTheme.typography.titleLarge) }
                if (busy) item { Text("Yükleniyor…") }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                if (!busy && rows == null) item { Button(onClick = { revision++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Yeniden yükle") } }
                if (rows?.isEmpty() == true) item { Text("Bu borca bağlı makbuz bulunmuyor. Belge oluşturulmadı.") }
                rows?.forEach { receipt -> item(key = receipt.id) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Makbuz: ${receipt.number}")
                        Text("Düzenlenme: ${receipt.issuedAt}")
                        Text("Durum: ${if (receipt.status == "CANCELLED") "İptal edilmiş" else "Aktif"}")
                        Text("Tahsilat: ${currency.format(receipt.paymentAmount)}")
                        Text("Makbuz anındaki öğrenci toplam kalan borcu: ${currency.format(receipt.remainingStudentDebt)}")
                        Button(enabled = !busy, modifier = Modifier.heightIn(min = 48.dp).testTag("receipt_pdf_${receipt.id}"), onClick = {
                            busy = true; error = null
                            scope.launch {
                                try { val bytes = client!!.pdf(receipt); check(client.current()); printVerifiedPdf(context, bytes) }
                                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                catch (_: Exception) { error = "Sunucu PDF'si açılamadı. Yazdırma veya indirme tamamlanmış sayılmaz." }
                                finally { busy = false }
                            }
                        }) { Text("Sunucu PDF'sini aç / yazdır") }
                    }
                } }
                item { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Kapat") } }
            }
        }
    }
}

/** Android's print UI can save the exact server bytes as PDF; no fabricated local document. */
private fun printVerifiedPdf(context: Context, bytes: ByteArray) {
    var owner = context
    while (owner is ContextWrapper && owner !is Activity) owner = owner.baseContext
    check(owner is Activity)
    val printer = owner.getSystemService(Context.PRINT_SERVICE) as PrintManager
    printer.print("Makbuz", object : PrintDocumentAdapter() {
        override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback, extras: Bundle?) {
            if (cancellationSignal.isCanceled) callback.onLayoutCancelled()
            else callback.onLayoutFinished(PrintDocumentInfo.Builder("makbuz.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true)
        }
        override fun onWrite(pages: Array<out PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal, callback: WriteResultCallback) {
            if (cancellationSignal.isCanceled) { callback.onWriteCancelled(); return }
            try { FileOutputStream(destination.fileDescriptor).use { it.write(bytes) }; callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES)) }
            catch (_: Exception) { callback.onWriteFailed("PDF yazılamadı") }
        }
    }, null)
}
