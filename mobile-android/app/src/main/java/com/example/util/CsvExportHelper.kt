package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.FinanceSummary
import com.example.data.model.Invoice
import com.example.data.model.Student
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * CsvExportHelper provides standardized CSV export functionality for institutional data
 * including Student Attendance (Yoklama), Financial Invoices (Finans & Tahsilat), and Student Directories.
 *
 * Utilizes UTF-8 with BOM and semicolon (;) delimiter for full Turkish character support
 * and seamless out-of-the-box compatibility with Microsoft Excel, LibreOffice Calc, and Google Sheets.
 */
object CsvExportHelper {

    private const val TAG = "CsvExportHelper"
    private const val DELIMITER = ";"
    private const val BOM = "\uFEFF"

    /**
     * Exports attendance records for a specific date and class group to a CSV file and opens the share sheet.
     */
    fun exportAttendanceToCsv(
        context: Context,
        records: List<AttendanceRecord>,
        students: List<Student>,
        className: String,
        date: String
    ) {
        if (records.isEmpty()) {
            Toast.makeText(context, "Dışa aktarılacak yoklama kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanDate = date.replace("-", "").replace(".", "")
            val cleanClassName = className.replace("[^a-zA-Z0-9]".toRegex(), "_").trim('_')
            val file = File(exportDir, "SCS360_Yoklama_${cleanClassName}_$cleanDate.csv")

            val studentMap = students.associateBy { it.id }

            val writer = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
            writer.write(BOM) // Excel UTF-8 recognition

            // CSV Header
            writer.write(
                listOf(
                    "Sıra",
                    "Öğrenci No",
                    "Öğrenci Adı Soyadı",
                    "Sınıf",
                    "Tarih",
                    "Yoklama Durumu",
                    "Giriş Saati",
                    "Vücut Sıcaklığı (°C)",
                    "Veli Adı",
                    "Veli Telefon",
                    "Yoklama Notu"
                ).joinToString(DELIMITER) + "\n"
            )

            // Rows
            records.forEachIndexed { index, record ->
                val student = studentMap[record.student_id]
                val statusText = when (record.status.uppercase()) {
                    "PRESENT" -> "GELDİ"
                    "ABSENT" -> "GELMEDİ (DEVAMSIZ)"
                    "LATE" -> "GEÇ GELDİ"
                    "EXCUSED" -> "İZİNLİ / RAPORLU"
                    else -> record.status
                }

                val row = listOf(
                    (index + 1).toString(),
                    escapeCsv(student?.student_number ?: "-"),
                    escapeCsv(record.student_name),
                    escapeCsv(className.ifBlank { student?.class_name ?: "-" }),
                    escapeCsv(record.date.ifBlank { date }),
                    escapeCsv(statusText),
                    escapeCsv(record.check_in_time ?: "-"),
                    escapeCsv(record.temperature_celsius?.toString() ?: "-"),
                    escapeCsv(student?.parent_name ?: "-"),
                    escapeCsv(student?.parent_phone ?: "-"),
                    escapeCsv(record.note ?: "")
                )
                writer.write(row.joinToString(DELIMITER) + "\n")
            }

            writer.flush()
            writer.close()

            shareCsvFile(
                context = context,
                file = file,
                title = "Yoklama Raporunu Paylaş / Dışa Aktar ($className - $date)",
                successMessage = "${records.size} adet yoklama kaydı CSV olarak dışa aktarıldı."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Yoklama CSV ihracında hata", e)
            Toast.makeText(context, "CSV oluşturulurken hata oluştu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Exports financial billing records and invoices to a CSV file and opens the share sheet.
     */
    fun exportFinanceToCsv(
        context: Context,
        invoices: List<Invoice>,
        summary: FinanceSummary? = null
    ) {
        if (invoices.isEmpty()) {
            Toast.makeText(context, "Dışa aktarılacak fatura/ödeme kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val file = File(exportDir, "SCS360_Finans_Raporu_$timeStamp.csv")

            val writer = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
            writer.write(BOM)

            // Header Section Summary Meta
            if (summary != null) {
                writer.write("SCS360 EĞİTİM KURUMLARI - MALİ VE TAHSİLAT RAPORU\n")
                writer.write("Rapor Tarihi${DELIMITER}${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())}\n")
                writer.write("Toplam Tahakkuk${DELIMITER}₺${String.format(Locale("tr"), "%.2f", summary.total_invoiced_monthly)}\n")
                writer.write("Tahsil Edilen${DELIMITER}₺${String.format(Locale("tr"), "%.2f", summary.total_collected_monthly)}\n")
                writer.write("Bekleyen Bakiye${DELIMITER}₺${String.format(Locale("tr"), "%.2f", summary.pending_balance)}\n")
                writer.write("Geciken Bakiye${DELIMITER}₺${String.format(Locale("tr"), "%.2f", summary.overdue_balance)}\n")
                writer.write("Tahsilat Oranı${DELIMITER}%${summary.collection_rate_percentage}\n\n")
            }

            // CSV Table Header
            writer.write(
                listOf(
                    "Sıra",
                    "Fatura No",
                    "Öğrenci Adı Soyadı",
                    "Sınıf",
                    "Açıklama",
                    "Tutar (TL)",
                    "Ödenen Tutar (TL)",
                    "Kalan Tutar (TL)",
                    "Düzenleme Tarihi",
                    "Vade Tarihi",
                    "Ödeme Durumu",
                    "Ödeme Yöntemi"
                ).joinToString(DELIMITER) + "\n"
            )

            // Invoices Rows
            invoices.forEachIndexed { index, inv ->
                val remaining = (inv.amount - inv.paid_amount).coerceAtLeast(0.0)
                val statusText = when (inv.status.uppercase()) {
                    "PAID", "ODENDI" -> "ÖDENDİ"
                    "PENDING", "BEKLIYOR" -> "BEKLİYOR"
                    "OVERDUE", "GECIKTI" -> "GECİKMEDE"
                    "CANCELLED", "IPTAL" -> "İPTAL"
                    else -> inv.status
                }

                val row = listOf(
                    (index + 1).toString(),
                    escapeCsv(inv.invoice_number),
                    escapeCsv(inv.student_name),
                    escapeCsv(inv.class_name),
                    escapeCsv(inv.description),
                    String.format(Locale("tr"), "%.2f", inv.amount),
                    String.format(Locale("tr"), "%.2f", inv.paid_amount),
                    String.format(Locale("tr"), "%.2f", remaining),
                    escapeCsv(inv.issue_date),
                    escapeCsv(inv.due_date),
                    escapeCsv(statusText),
                    escapeCsv(inv.payment_method ?: "-")
                )
                writer.write(row.joinToString(DELIMITER) + "\n")
            }

            writer.flush()
            writer.close()

            shareCsvFile(
                context = context,
                file = file,
                title = "Finans Raporunu Paylaş / Dışa Aktar (CSV)",
                successMessage = "${invoices.size} adet fatura kaydı CSV olarak dışa aktarıldı."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Finans CSV ihracında hata", e)
            Toast.makeText(context, "CSV oluşturulurken hata oluştu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Exports full student directory and parent contact list to a CSV file.
     */
    fun exportStudentsToCsv(
        context: Context,
        students: List<Student>
    ) {
        if (students.isEmpty()) {
            Toast.makeText(context, "Dışa aktarılacak öğrenci kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val file = File(exportDir, "SCS360_Ogrenci_Listesi_$timeStamp.csv")

            val writer = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
            writer.write(BOM)

            // Header
            writer.write(
                listOf(
                    "Sıra",
                    "Öğrenci No",
                    "Ad",
                    "Soyad",
                    "Tam Ad",
                    "Sınıf",
                    "Eğitim Yılı",
                    "Veli Adı",
                    "Veli Telefon",
                    "Veli E-Posta",
                    "Sağlık & Alerji Notları",
                    "Doğum Tarihi",
                    "Durum"
                ).joinToString(DELIMITER) + "\n"
            )

            // Rows
            students.forEachIndexed { index, student ->
                val row = listOf(
                    (index + 1).toString(),
                    escapeCsv(student.student_number),
                    escapeCsv(student.first_name),
                    escapeCsv(student.last_name),
                    escapeCsv(student.full_name),
                    escapeCsv(student.class_name),
                    escapeCsv(student.academic_year),
                    escapeCsv(student.parent_name),
                    escapeCsv(student.parent_phone),
                    escapeCsv(student.parent_email ?: "-"),
                    escapeCsv(student.allergies_or_diet ?: "Yok"),
                    escapeCsv(student.birth_date),
                    escapeCsv(student.status)
                )
                writer.write(row.joinToString(DELIMITER) + "\n")
            }

            writer.flush()
            writer.close()

            shareCsvFile(
                context = context,
                file = file,
                title = "Öğrenci Listesini Paylaş / Dışa Aktar (CSV)",
                successMessage = "${students.size} öğrenci kaydı CSV olarak dışa aktarıldı."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Öğrenci CSV ihracında hata", e)
            Toast.makeText(context, "CSV oluşturulurken hata oluştu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Dispatches the CSV file via Android share chooser with FileProvider uri.
     */
    private fun shareCsvFile(
        context: Context,
        file: File,
        title: String,
        successMessage: String
    ) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
                putExtra(Intent.EXTRA_TEXT, "SCS360 Kurumsal Yönetim Sistemi tarafından dışa aktarılan veri dökümüdür.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Dosya paylaşım hatası, panoya kopyalama deneniyor", e)
            try {
                val content = file.readText()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("SCS360 CSV", content)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(context, "CSV içeriği panoya kopyalandı.", Toast.LENGTH_LONG).show()
            } catch (err: Exception) {
                Toast.makeText(context, "Paylaşım başarısız: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Escapes CSV string to handle semicolons, double quotes, and line breaks according to RFC 4180.
     */
    private fun escapeCsv(value: String): String {
        var result = value.replace("\r", " ").replace("\n", " ").trim()
        if (result.contains(DELIMITER) || result.contains("\"") || result.contains(",")) {
            result = "\"" + result.replace("\"", "\"\"") + "\""
        }
        return result
    }
}
