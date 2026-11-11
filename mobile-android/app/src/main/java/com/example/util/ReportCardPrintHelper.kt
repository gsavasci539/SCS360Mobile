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
import com.example.data.model.Student
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * ReportCardPrintHelper provides PDF generation and printing functionality for Student Report Cards (Karne)
 * utilizing the Android PrintManager API.
 */
object ReportCardPrintHelper {

    private const val TAG = "ReportCardPrintHelper"

    /**
     * Initiates the native Android PrintManager workflow to generate and print or save the Report Card as a PDF.
     */
    fun printStudentReportCard(
        context: Context,
        student: Student,
        schoolName: String = "SCS360 EĞİTİM KURUMLARI",
        campusName: String = "Kadıköy Kampüsü",
        termName: String = "2025-2026 Eğitim Öğretim Yılı 2. Dönem",
        teacherName: String = "Zeynep Kaya",
        principalName: String = "Ahmet Yılmaz"
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Yazdırma servisi bu cihazda desteklenmiyor.", Toast.LENGTH_SHORT).show()
            return
        }

        val jobName = "Karne_${student.student_number}_${student.first_name}_${student.last_name}"

        try {
            val printAdapter = ReportCardPrintDocumentAdapter(
                context = context,
                student = student,
                schoolName = schoolName,
                campusName = campusName,
                termName = termName,
                teacherName = teacherName,
                principalName = principalName
            )

            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("res1", "300dpi", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager.print(jobName, printAdapter, printAttributes)
            Toast.makeText(context, "${student.full_name} için karne PDF oluşturuldu. Yazdırma / Kaydetme ekranı açılıyor...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Yazdırma başlatılırken hata oluştu: ${e.message}", e)
            Toast.makeText(context, "Karne PDF oluşturulamadı: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Batch print report cards for an entire class list
     */
    fun printClassReportCards(
        context: Context,
        students: List<Student>,
        className: String,
        schoolName: String = "SCS360 EĞİTİM KURUMLARI",
        campusName: String = "Kadıköy Kampüsü"
    ) {
        if (students.isEmpty()) {
            Toast.makeText(context, "Seçili sınıfta yazdırılacak öğrenci kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Yazdırma servisi bu cihazda desteklenmiyor.", Toast.LENGTH_SHORT).show()
            return
        }

        val jobName = "Toplu_Karneler_${className}_${System.currentTimeMillis() % 1000}"

        try {
            val printAdapter = BatchReportCardsPrintDocumentAdapter(
                context = context,
                students = students,
                className = className,
                schoolName = schoolName,
                campusName = campusName
            )

            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("res1", "300dpi", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager.print(jobName, printAdapter, printAttributes)
            Toast.makeText(context, "$className sınıfı (${students.size} öğrenci) için toplu karne hazırlanıyor...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Toplu yazdırma başlatılırken hata: ${e.message}", e)
            Toast.makeText(context, "Toplu karne PDF oluşturulamadı: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Custom PrintDocumentAdapter for rendering a single Student Report Card (Karne).
     */
    class ReportCardPrintDocumentAdapter(
        private val context: Context,
        private val student: Student,
        private val schoolName: String,
        private val campusName: String,
        private val termName: String,
        private val teacherName: String,
        private val principalName: String
    ) : PrintDocumentAdapter() {

        private var pdfDocument: PrintedPdfDocument? = null
        private val pageCount = 1

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

            val info = PrintDocumentInfo.Builder("Karne_${student.student_number}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pageCount)
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
            val page = doc.startPage(0)

            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
                doc.close()
                return
            }

            drawReportCard(page.canvas, page.info.pageWidth, page.info.pageHeight, student)
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

        private fun drawReportCard(canvas: Canvas, width: Int, height: Int, student: Student) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val margin = 36f
            val contentWidth = width - (2 * margin)

            // Background Canvas
            paint.color = Color.WHITE
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Outer Decorative Border (Double Border)
            paint.color = Color.parseColor("#1E3A8A") // Dark Navy
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3.5f
            canvas.drawRoundRect(
                RectF(margin, margin, width - margin, height - margin),
                12f, 12f, paint
            )

            paint.color = Color.parseColor("#93C5FD") // Light Blue Inner Border
            paint.strokeWidth = 1f
            canvas.drawRoundRect(
                RectF(margin + 5f, margin + 5f, width - margin - 5f, height - margin - 5f),
                8f, 8f, paint
            )

            paint.style = Paint.Style.FILL

            // 1. Header Banner
            var yPos = margin + 30f

            paint.color = Color.parseColor("#1E40AF")
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            paint.textSize = 15f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("T.C. MİLLÎ EĞİTİM BAKANLIĞI", width / 2f, yPos, paint)

            yPos += 18f
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 18f
            canvas.drawText("$schoolName - $campusName", width / 2f, yPos, paint)

            yPos += 16f
            paint.color = Color.parseColor("#059669") // Emerald
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ÖĞRENCİ GELİŞİM VE DEĞERLENDİRME KARNESİ", width / 2f, yPos, paint)

            yPos += 14f
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val currentDate = SimpleDateFormat("dd MMMM yyyy", Locale("tr")).format(Date())
            canvas.drawText("$termName  •  Düzenleme Tarihi: $currentDate", width / 2f, yPos, paint)

            // Divider Line
            yPos += 12f
            paint.color = Color.parseColor("#CBD5E1")
            paint.strokeWidth = 1.2f
            canvas.drawLine(margin + 16f, yPos, width - margin - 16f, yPos, paint)

            // 2. Student Info Card Box
            yPos += 16f
            val cardLeft = margin + 16f
            val cardRight = width - margin - 16f
            val cardHeight = 70f
            val cardRect = RectF(cardLeft, yPos, cardRight, yPos + cardHeight)

            paint.color = Color.parseColor("#F8FAFC")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(cardRect, 8f, 8f, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(cardRect, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            // Student Avatar Badge Box
            val avatarSize = 48f
            val avatarLeft = cardLeft + 12f
            val avatarTop = yPos + 11f
            paint.color = Color.parseColor("#1D4ED8")
            canvas.drawRoundRect(RectF(avatarLeft, avatarTop, avatarLeft + avatarSize, avatarTop + avatarSize), 8f, 8f, paint)

            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val initials = "${student.first_name.firstOrNull() ?: 'Ö'}${student.last_name.firstOrNull() ?: 'K'}"
            canvas.drawText(initials, avatarLeft + (avatarSize / 2f), avatarTop + 32f, paint)

            // Student Info Columns
            paint.textAlign = Paint.Align.LEFT
            val col1Left = avatarLeft + avatarSize + 16f

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(student.full_name, col1Left, yPos + 26f, paint)

            paint.color = Color.parseColor("#475569")
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Okul No: ${student.student_number}   |   Sınıf: ${student.class_name}", col1Left, yPos + 44f, paint)
            canvas.drawText("Veli Adı: ${student.parent_name}   |   Telefon: ${student.parent_phone}", col1Left, yPos + 58f, paint)

            // Right side of info card
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.parseColor("#059669")
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Dönem Sonu Durumu: BAŞARILI (TAKDIR)", cardRight - 14f, yPos + 26f, paint)
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Ağırlıklı Not Ortalaması: 94.8 / 100", cardRight - 14f, yPos + 44f, paint)

            // 3. Academic Courses & Grade Table
            yPos += cardHeight + 20f

            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#1E3A8A")
            paint.textSize = 12.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("📚 DERS VE AKADEMİK BAŞARI DEĞERLENDİRMESİ", cardLeft, yPos, paint)

            yPos += 8f
            val tableLeft = cardLeft
            val tableRight = cardRight
            val rowHeight = 22f

            // Table Header Row
            val headerRect = RectF(tableLeft, yPos, tableRight, yPos + rowHeight)
            paint.color = Color.parseColor("#1E40AF")
            canvas.drawRoundRect(headerRect, 4f, 4f, paint)

            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Ders Adı", tableLeft + 10f, yPos + 15f, paint)

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("1. Sınav", tableLeft + 220f, yPos + 15f, paint)
            canvas.drawText("2. Sınav", tableLeft + 280f, yPos + 15f, paint)
            canvas.drawText("Proje & Katılım", tableLeft + 360f, yPos + 15f, paint)
            canvas.drawText("Dönem Sonu", tableLeft + 440f, yPos + 15f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Derece", tableRight - 12f, yPos + 15f, paint)

            yPos += rowHeight

            // Course Rows Data
            val courses = listOf(
                CourseGrade("Türkçe", "95", "92", "100", "96", "Pekiyi"),
                CourseGrade("Matematik", "90", "94", "95", "93", "Pekiyi"),
                CourseGrade("Fen Bilimleri", "92", "88", "95", "92", "Pekiyi"),
                CourseGrade("Sosyal Bilgiler", "98", "96", "100", "98", "Pekiyi"),
                CourseGrade("Yabancı Dil (İngilizce)", "95", "98", "100", "98", "Pekiyi"),
                CourseGrade("Bilişim Teknolojileri & Robotik", "100", "98", "100", "99", "Pekiyi"),
                CourseGrade("Görsel Sanatlar & Tasarım", "100", "100", "100", "100", "Pekiyi"),
                CourseGrade("Müzik ve Ritim", "95", "95", "100", "97", "Pekiyi"),
                CourseGrade("Beden Eğitimi ve Spor", "100", "100", "100", "100", "Pekiyi"),
                CourseGrade("Rehberlik ve Değerler Eğitimi", "100", "100", "100", "100", "Pekiyi")
            )

            courses.forEachIndexed { index, course ->
                val isEven = index % 2 == 0
                val rowRect = RectF(tableLeft, yPos, tableRight, yPos + rowHeight)

                paint.color = if (isEven) Color.parseColor("#F1F5F9") else Color.WHITE
                canvas.drawRect(rowRect, paint)

                paint.color = Color.parseColor("#0F172A")
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(course.name, tableLeft + 10f, yPos + 15f, paint)

                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(course.exam1, tableLeft + 220f, yPos + 15f, paint)
                canvas.drawText(course.exam2, tableLeft + 280f, yPos + 15f, paint)
                canvas.drawText(course.project, tableLeft + 360f, yPos + 15f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.parseColor("#1D4ED8")
                canvas.drawText(course.finalScore, tableLeft + 440f, yPos + 15f, paint)

                paint.textAlign = Paint.Align.RIGHT
                paint.color = Color.parseColor("#059669")
                canvas.drawText(course.status, tableRight - 12f, yPos + 15f, paint)

                // Bottom row border
                paint.color = Color.parseColor("#E2E8F0")
                paint.strokeWidth = 0.6f
                canvas.drawLine(tableLeft, yPos + rowHeight, tableRight, yPos + rowHeight, paint)

                yPos += rowHeight
            }

            // 4. Social & Behavior / Attendance Summary Box
            yPos += 14f
            val summaryWidth = (contentWidth - 40f) / 2f

            // Social Development
            val socialLeft = tableLeft
            val socialRight = socialLeft + summaryWidth
            val summaryHeight = 85f

            paint.color = Color.parseColor("#F8FAFC")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(socialLeft, yPos, socialRight, yPos + summaryHeight), 6f, 6f, paint)
            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(RectF(socialLeft, yPos, socialRight, yPos + summaryHeight), 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#1E3A8A")
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("🌟 Sosyal & Kişisel Gelişim", socialLeft + 10f, yPos + 18f, paint)

            paint.color = Color.parseColor("#334155")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("• Sorumluluk Bilinci ve Görev Alma: Çok İyi", socialLeft + 10f, yPos + 34f, paint)
            canvas.drawText("• Arkadaşlık ve Takım Çalışması: Çok İyi", socialLeft + 10f, yPos + 48f, paint)
            canvas.drawText("• Nezaket, Saygı ve Çevre Bilinci: Çok İyi", socialLeft + 10f, yPos + 62f, paint)
            canvas.drawText("• Derse Hazırlık ve İletişim: Çok İyi", socialLeft + 10f, yPos + 76f, paint)

            // Attendance Summary Box (Right)
            val attendLeft = socialRight + 12f
            val attendRight = tableRight

            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(RectF(attendLeft, yPos, attendRight, yPos + summaryHeight), 6f, 6f, paint)
            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(RectF(attendLeft, yPos, attendRight, yPos + summaryHeight), 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#059669")
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("📅 Devam - Devamsızlık Durumu", attendLeft + 10f, yPos + 18f, paint)

            paint.color = Color.parseColor("#334155")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("• Toplam İş Günü: 180 Gün", attendLeft + 10f, yPos + 34f, paint)
            canvas.drawText("• Devam Edilen Gün: 178 Gün", attendLeft + 10f, yPos + 48f, paint)
            canvas.drawText("• İzinli / Özürlü Devamsızlık: 2 Gün", attendLeft + 10f, yPos + 62f, paint)
            canvas.drawText("• Özürsüz Devamsızlık: 0 Gün (Devam Oranı: %98.8)", attendLeft + 10f, yPos + 76f, paint)

            // 5. Teacher Note Box
            yPos += summaryHeight + 12f
            val noteRect = RectF(tableLeft, yPos, tableRight, yPos + 45f)
            paint.color = Color.parseColor("#EFF6FF")
            canvas.drawRoundRect(noteRect, 6f, 6f, paint)
            paint.color = Color.parseColor("#BFDBFE")
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(noteRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#1D4ED8")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("📝 Sınıf Rehber Öğretmeni ve Okul Yönetimi Görüşü:", tableLeft + 10f, yPos + 16f, paint)

            paint.color = Color.parseColor("#1E3A8A")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            val teacherOpinion = "Öğrencimiz dönem boyunca sergilediği yüksek motivasyon, bilimsel merakı, arkadaşlarına olan saygısı ve ders başarılarıyla takdiri hak etmiştir. Tebrik eder, başarılarının hayat boyu devamını dileriz."
            canvas.drawText(teacherOpinion, tableLeft + 10f, yPos + 32f, paint)

            // 6. Signatures and Official Seal Block
            yPos += 58f

            val sigWidth = contentWidth / 3f

            // Teacher Signature
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(teacherName, tableLeft + (sigWidth / 2f), yPos, paint)
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Sınıf Rehber Öğretmeni", tableLeft + (sigWidth / 2f), yPos + 12f, paint)
            canvas.drawText("İmza: ________________", tableLeft + (sigWidth / 2f), yPos + 30f, paint)

            // Official Stamp / Seal
            val sealCenter = tableLeft + (contentWidth / 2f)
            paint.color = Color.parseColor("#DC2626") // Red Seal
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawCircle(sealCenter, yPos + 14f, 22f, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("RESMÎ MÜHÜR", sealCenter, yPos + 12f, paint)
            canvas.drawText("SCS360", sealCenter, yPos + 22f, paint)

            // Principal Signature
            val princCenter = tableRight - (sigWidth / 2f)
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(principalName, princCenter, yPos, paint)
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Okul Müdürü", princCenter, yPos + 12f, paint)
            canvas.drawText("İmza: ________________", princCenter, yPos + 30f, paint)
        }
    }

    /**
     * PrintDocumentAdapter for Batch Class Report Cards (Toplu Sınıf Karneleri)
     */
    class BatchReportCardsPrintDocumentAdapter(
        private val context: Context,
        private val students: List<Student>,
        private val className: String,
        private val schoolName: String,
        private val campusName: String
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

            val info = PrintDocumentInfo.Builder("Karneler_${className}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(students.size)
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

            for (i in students.indices) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    doc.close()
                    return
                }

                val student = students[i]
                val page = doc.startPage(i)

                val singleAdapter = ReportCardPrintDocumentAdapter(
                    context = context,
                    student = student,
                    schoolName = schoolName,
                    campusName = campusName,
                    termName = "2025-2026 Eğitim Öğretim Yılı 2. Dönem",
                    teacherName = "Zeynep Kaya",
                    principalName = "Ahmet Yılmaz"
                )

                // Use reflection or invoke drawing
                val drawMethod = ReportCardPrintDocumentAdapter::class.java.getDeclaredMethod(
                    "drawReportCard",
                    Canvas::class.java,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Student::class.java
                )
                drawMethod.isAccessible = true
                drawMethod.invoke(singleAdapter, page.canvas, page.info.pageWidth, page.info.pageHeight, student)

                doc.finishPage(page)
            }

            try {
                destination?.let {
                    FileOutputStream(it.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                }
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: IOException) {
                Log.e(TAG, "Toplu PDF yazma hatası", e)
                callback?.onWriteFailed(e.message)
            } finally {
                doc.close()
                pdfDocument = null
            }
        }
    }

    private data class CourseGrade(
        val name: String,
        val exam1: String,
        val exam2: String,
        val project: String,
        val finalScore: String,
        val status: String
    )
}
