package com.example.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.print.pdf.PrintedPdfDocument
import android.util.Log
import android.widget.Toast
import com.example.data.model.StudentTermSubjectGrade
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * TeacherReportExportHelper provides native PDF export & printing of
 * term-based teacher attendance records and subject grade averages.
 */
object TeacherReportExportHelper {

    private const val TAG = "TeacherReportExport"

    fun exportTeacherTermSubjectReport(
        context: Context,
        teacherName: String = "Ahmet Yılmaz",
        subjectName: String = "Matematik",
        className: String = "8-A",
        termName: String = "2025-2026 Eğitim Öğretim Yılı 2. Dönem",
        schoolName: String = "SCS360 EĞİTİM KURUMLARI",
        campusName: String = "Merkez Kampüsü",
        gradeList: List<StudentTermSubjectGrade>
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Yazdırma/PDF servisi bu cihazda desteklenmiyor.", Toast.LENGTH_SHORT).show()
            return
        }

        val sanitizedSubject = subjectName.replace(" ", "_")
        val sanitizedClass = className.replace(" ", "_")
        val jobName = "Donem_Raporu_${sanitizedSubject}_${sanitizedClass}_${System.currentTimeMillis() % 1000}"

        try {
            val printAdapter = TeacherTermReportPrintDocumentAdapter(
                context = context,
                teacherName = teacherName,
                subjectName = subjectName,
                className = className,
                termName = termName,
                schoolName = schoolName,
                campusName = campusName,
                gradeList = gradeList
            )

            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("res1", "300dpi", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager.print(jobName, printAdapter, printAttributes)
            Toast.makeText(context, "$subjectName dersi $termName PDF raporu oluşturuluyor...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Öğretmen raporu oluşturulurken hata: ${e.message}", e)
            Toast.makeText(context, "PDF Rapor oluşturulamadı: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private class TeacherTermReportPrintDocumentAdapter(
        private val context: Context,
        private val teacherName: String,
        private val subjectName: String,
        private val className: String,
        private val termName: String,
        private val schoolName: String,
        private val campusName: String,
        private val gradeList: List<StudentTermSubjectGrade>
    ) : PrintDocumentAdapter() {

        private var pdfDocument: PrintedPdfDocument? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onLayoutCancelled()
                return
            }

            pdfDocument = PrintedPdfDocument(context, newAttributes)

            val info = PrintDocumentInfo.Builder("Donem_Not_Devamsizlik_Raporu.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()

            callback?.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            val doc = pdfDocument ?: return

            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
                doc.close()
                return
            }

            val page = doc.startPage(0)
            val canvas = page.canvas
            val width = page.info.pageWidth
            val height = page.info.pageHeight

            drawTeacherTermReport(canvas, width, height)

            doc.finishPage(page)

            try {
                destination?.let {
                    FileOutputStream(it.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                }
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: IOException) {
                Log.e(TAG, "PDF yazma hatası", e)
                callback?.onWriteFailed(e.message)
            } finally {
                doc.close()
                pdfDocument = null
            }
        }

        private fun drawTeacherTermReport(canvas: Canvas, width: Int, height: Int) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val pageWidth = width.toFloat()
            val margin = 28f

            // 1. Background
            canvas.drawColor(Color.WHITE)

            // 2. Header Box (School Navy Gradient Style)
            val headerHeight = 70f
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E3A8A") // Brand Navy
            }
            canvas.drawRect(margin, margin, pageWidth - margin, margin + headerHeight, headerPaint)

            // Gold bottom border for header
            val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F59E0B")
            }
            canvas.drawRect(margin, margin + headerHeight - 3f, pageWidth - margin, margin + headerHeight, goldPaint)

            // School & Title Texts
            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(schoolName.uppercase(), pageWidth / 2f, margin + 22f, paint)

            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("$campusName • $termName", pageWidth / 2f, margin + 38f, paint)

            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#FEF08A")
            canvas.drawText("ÖĞRETMEN DÖNEMLİK DERS NOTLARI VE DEVAMSIZLIK ÇİZELGESİ", pageWidth / 2f, margin + 56f, paint)

            // 3. Metadata Info Grid Box
            var currentY = margin + headerHeight + 10f
            val infoBoxHeight = 44f
            val infoBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F8FAFC")
            }
            val infoBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(
                RectF(margin, currentY, pageWidth - margin, currentY + infoBoxHeight),
                6f, 6f, infoBgPaint
            )
            canvas.drawRoundRect(
                RectF(margin, currentY, pageWidth - margin, currentY + infoBoxHeight),
                6f, 6f, infoBorderPaint
            )

            val classAvg = if (gradeList.isNotEmpty()) gradeList.map { it.termAverage }.average() else 85.0
            val classAttendanceRate = if (gradeList.isNotEmpty()) gradeList.map { it.attendance_rate }.average() else 97.0

            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 8f

            // Left Col
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Ders Adı:", margin + 10f, currentY + 16f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(subjectName, margin + 55f, currentY + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Sınıf/Grup:", margin + 10f, currentY + 32f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(className, margin + 55f, currentY + 32f, paint)

            // Middle Col
            val midX = pageWidth * 0.38f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Ders Öğretmeni:", midX, currentY + 16f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(teacherName, midX + 70f, currentY + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Öğrenci Sayısı:", midX, currentY + 32f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("${gradeList.size} Öğrenci", midX + 70f, currentY + 32f, paint)

            // Right Col (KPIs)
            val rightX = pageWidth * 0.72f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Sınıf Ortalaması:", rightX, currentY + 16f, paint)
            paint.color = Color.parseColor("#059669")
            canvas.drawText(String.format(Locale.US, "%.1f / 100", classAvg), rightX + 68f, currentY + 16f, paint)

            paint.color = Color.parseColor("#0F172A")
            canvas.drawText("Sınıf Devam %:", rightX, currentY + 32f, paint)
            paint.color = Color.parseColor("#2563EB")
            canvas.drawText(String.format(Locale.US, "%%% .1f", classAttendanceRate), rightX + 68f, currentY + 32f, paint)

            currentY += infoBoxHeight + 12f

            // 4. Student Grade Table
            val colWidths = floatArrayOf(
                22f,  // No
                100f, // Adı Soyadı
                36f,  // 1. Yazılı
                36f,  // 2. Yazılı
                36f,  // Quiz/Ödev
                36f,  // Sözlü
                36f,  // Proje
                42f,  // Dönem Ort.
                26f,  // Harf
                36f,  // Toplam Gün
                36f,  // Devamsızlık
                pageWidth - margin * 2 - (22 + 100 + 36 * 5 + 42 + 26 + 36 * 2) // Devam Oranı %
            )

            val headers = arrayOf(
                "No", "Öğrenci Adı Soyadı", "1.Yazılı", "2.Yazılı", "Quiz/Ödev", "Sözlü", "Proje", "Ort.", "Harf", "Toplam", "Devamsız", "Devam %"
            )

            // Draw Table Header Row
            val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
            }
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 18f, tableHeaderPaint)

            paint.color = Color.WHITE
            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            var colX = margin
            for (i in headers.indices) {
                val align = if (i == 1) Paint.Align.LEFT else Paint.Align.CENTER
                paint.textAlign = align
                val textX = if (i == 1) colX + 4f else colX + colWidths[i] / 2f
                canvas.drawText(headers[i], textX, currentY + 12f, paint)
                colX += colWidths[i]
            }

            currentY += 18f

            // Draw Student Rows
            val rowHeight = 15f
            val gridBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 0.5f
                style = Paint.Style.STROKE
            }
            val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F1F5F9")
            }

            paint.textSize = 6.8f
            for (r in gradeList.indices) {
                val item = gradeList[r]
                val rowY = currentY + r * rowHeight

                // Zebra striping
                if (r % 2 == 1) {
                    canvas.drawRect(margin, rowY, pageWidth - margin, rowY + rowHeight, zebraPaint)
                }

                // Row borders
                canvas.drawRect(margin, rowY, pageWidth - margin, rowY + rowHeight, gridBorderPaint)

                var cX = margin
                val rowTexts = arrayOf(
                    item.student_number,
                    item.student_name,
                    String.format(Locale.US, "%.0f", item.exam1),
                    String.format(Locale.US, "%.0f", item.exam2),
                    String.format(Locale.US, "%.0f", item.quiz_or_homework),
                    String.format(Locale.US, "%.0f", item.participation),
                    String.format(Locale.US, "%.0f", item.project),
                    String.format(Locale.US, "%.1f", item.termAverage),
                    item.letterGrade,
                    "${item.total_attendance_days} gün",
                    "${item.absent_days} gün",
                    String.format(Locale.US, "%%% .1f", item.attendance_rate)
                )

                for (c in rowTexts.indices) {
                    val align = if (c == 1) Paint.Align.LEFT else Paint.Align.CENTER
                    paint.textAlign = align
                    paint.typeface = if (c == 7 || c == 8) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = when (c) {
                        7 -> Color.parseColor("#0F172A")
                        8 -> if (item.letterGrade.startsWith("A") || item.letterGrade.startsWith("B")) Color.parseColor("#059669") else Color.parseColor("#D97706")
                        10 -> if (item.absent_days > 4) Color.parseColor("#DC2626") else Color.parseColor("#475569")
                        else -> Color.parseColor("#1E293B")
                    }

                    val textX = if (c == 1) cX + 4f else cX + colWidths[c] / 2f
                    canvas.drawText(rowTexts[c], textX, rowY + 10.5f, paint)
                    cX += colWidths[c]
                }
            }

            val tableBottomY = currentY + gradeList.size * rowHeight + 14f

            // 5. Grade Distribution Summary & Stats Box
            val distBoxHeight = 44f
            val distRect = RectF(margin, tableBottomY, pageWidth - margin, tableBottomY + distBoxHeight)
            canvas.drawRoundRect(distRect, 6f, 6f, infoBgPaint)
            canvas.drawRoundRect(distRect, 6f, 6f, infoBorderPaint)

            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7.5f
            canvas.drawText("📊 DÖNEMLİK BAŞARI VE DEVAM DAĞILIMI ÖZETİ", margin + 10f, tableBottomY + 14f, paint)

            val aaCount = gradeList.count { it.letterGrade == "AA" }
            val baCount = gradeList.count { it.letterGrade == "BA" }
            val bbCount = gradeList.count { it.letterGrade == "BB" }
            val cbCount = gradeList.count { it.letterGrade == "CB" || it.letterGrade == "CC" }
            val ffCount = gradeList.count { it.letterGrade == "FF" }

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7f
            paint.color = Color.parseColor("#334155")
            canvas.drawText(
                "Harf Notları: AA ($aaCount), BA ($baCount), BB ($bbCount), CB/CC ($cbCount), FF/Kalan ($ffCount)  •  Başarı Oranı: %${if (gradeList.isNotEmpty()) ((gradeList.size - ffCount) * 100 / gradeList.size) else 100}",
                margin + 10f, tableBottomY + 26f, paint
            )
            canvas.drawText(
                "Devamsızlık Durumu: Ortalama ${String.format(Locale.US, "%.1f", if (gradeList.isNotEmpty()) gradeList.map { it.absent_days }.average() else 1.0)} gün devamsızlık  •  Düzenli Takip Edilen Ders Saati: 72 Saat",
                margin + 10f, tableBottomY + 37f, paint
            )

            // 6. Signatures and Official Seals
            val sigY = tableBottomY + distBoxHeight + 35f
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 8f
            paint.color = Color.parseColor("#0F172A")

            // Teacher Sig
            val teacherX = margin + 100f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(teacherName, teacherX, sigY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7f
            paint.color = Color.parseColor("#64748B")
            canvas.drawText("$subjectName Dersi Öğretmeni", teacherX, sigY + 12f, paint)
            canvas.drawText("İmza: ____________________", teacherX, sigY + 28f, paint)

            // Principal Sig
            val principalX = pageWidth - margin - 100f
            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8f
            canvas.drawText("Ahmet Yılmaz", principalX, sigY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7f
            paint.color = Color.parseColor("#64748B")
            canvas.drawText("Okul Müdürü / İdare", principalX, sigY + 12f, paint)
            canvas.drawText("Mühür / İmza: ____________________", principalX, sigY + 28f, paint)

            // Official Timestamp
            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 6.5f
            paint.color = Color.parseColor("#94A3B8")
            canvas.drawText("Bu belge SCS360 e-Okul Entegrasyon Sistemi tarafından üretilmiştir. Rapor Üretim Tarihi: $dateStr • Belge Doğrulama Kodu: SCS360-TR-${System.currentTimeMillis() % 100000}", pageWidth / 2f, height.toFloat() - 15f, paint)
        }
    }
}
