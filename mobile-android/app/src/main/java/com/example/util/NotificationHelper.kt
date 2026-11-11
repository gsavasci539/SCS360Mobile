package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.atomic.AtomicInteger

object NotificationHelper {

    const val CHANNEL_ANNOUNCEMENT = "scs360_announcements"
    const val CHANNEL_ATTENDANCE = "scs360_attendance"
    const val CHANNEL_FINANCE = "scs360_finance"
    const val CHANNEL_MESSAGES = "scs360_messages"
    const val CHANNEL_HEALTH_DAILY = "scs360_health_daily"
    const val CHANNEL_ACADEMIC = "scs360_academic"

    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
    const val EXTRA_TARGET_SCREEN = "extra_target_screen"
    const val EXTRA_PAYLOAD_ID = "extra_payload_id"

    private val notificationIdCounter = AtomicInteger(1001)

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", context.applicationInfo.uid)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // 1. Announcements Channel
            val announcementChannel = NotificationChannel(
                CHANNEL_ANNOUNCEMENT,
                "Duyuru & Genel Bildirimler",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Okul ve şube duyuruları, etkinlikler ve acil bilgilendirmeler"
                enableLights(true)
                lightColor = Color.parseColor("#1E40AF")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Attendance Channel
            val attendanceChannel = NotificationChannel(
                CHANNEL_ATTENDANCE,
                "Yoklama & Giriş-Çıkış Güvenliği",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Öğrenci okula giriş, çıkış ve devamsızlık anlık bildirimleri"
                enableLights(true)
                lightColor = Color.parseColor("#059669")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. Finance & Invoices Channel
            val financeChannel = NotificationChannel(
                CHANNEL_FINANCE,
                "Ödeme & Taksit Hatırlatmaları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ödeme makbuzları, taksit vade bildirimleri ve gecikme uyarıları"
                enableLights(true)
                lightColor = Color.parseColor("#D97706")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 4. Messages Channel
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Öğretmen & Veli Mesajları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Birebir veli-öğretmen mesajlaşması ve acil notlar"
                enableLights(true)
                lightColor = Color.parseColor("#7C3AED")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 250)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 5. Health & Daily Reports Channel
            val healthChannel = NotificationChannel(
                CHANNEL_HEALTH_DAILY,
                "Günlük Bülten, Beslenme & İlaç",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Yemek menüsü tüketimi, ilaç verme teyidi ve günlük aktivite bülteni"
                enableLights(true)
                lightColor = Color.parseColor("#0284C7")
                enableVibration(true)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 6. Academic & Grades Channel (Yeni Not & Sınav Bildirimleri)
            val academicChannel = NotificationChannel(
                CHANNEL_ACADEMIC,
                "Sınav Notları & Akademik Değerlendirme",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Yazılı sınavlar, deneme sonuçları, karne notları ve öğretmen akademik değerlendirmeleri"
                enableLights(true)
                lightColor = Color.parseColor("#7C3AED")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 300)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(
                listOf(announcementChannel, attendanceChannel, financeChannel, messagesChannel, healthChannel, academicChannel)
            )
        }
    }

    fun showPushNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = CHANNEL_ANNOUNCEMENT,
        targetScreen: String? = null,
        payloadId: String? = null,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt() + notificationIdCounter.incrementAndGet()
    ) {
        // Ensure channels exist
        createNotificationChannels(context)

        // Intent to launch app when notification clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_SCREEN, targetScreen)
            putExtra(EXTRA_PAYLOAD_ID, payloadId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setOnlyAlertOnce(false)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            notificationManagerCompat.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Android 13+ permission not yet granted or denied
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Convenience Trigger Methods for School Events
    fun initFirebaseMessaging(context: Context) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("announcements")
            FirebaseMessaging.getInstance().subscribeToTopic("general_events")
            FirebaseMessaging.getInstance().subscribeToTopic("grades")
            FirebaseMessaging.getInstance().subscribeToTopic("academic_updates")
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        val token = task.result
                        val session = com.example.data.local.SessionManager(context)
                        session.saveFcmToken(token)
                        android.util.Log.d("NotificationHelper", "FCM Token initialized: $token")
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun subscribeToBranchTopic(branchId: String) {
        try {
            if (branchId.isNotBlank()) {
                FirebaseMessaging.getInstance().subscribeToTopic("branch_$branchId")
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun sendAttendancePush(context: Context, studentName: String, statusText: String, className: String) {
        val title = "🔔 Yoklama Bildirimi: $studentName"
        val message = "$studentName ($className) için durum güncellendi: $statusText."
        showPushNotification(
            context = context,
            title = title,
            message = message,
            channelId = CHANNEL_ATTENDANCE,
            targetScreen = "ATTENDANCE"
        )
    }

    fun sendAnnouncementPush(context: Context, title: String, content: String, author: String = "Okul Yönetimi") {
        val notificationTitle = "📢 Yeni Duyuru: $title"
        val message = "$author tarafından yeni bir duyuru yayınlandı: $content"
        showPushNotification(
            context = context,
            title = notificationTitle,
            message = message,
            channelId = CHANNEL_ANNOUNCEMENT,
            targetScreen = "ANNOUNCEMENTS"
        )
    }

    fun sendPaymentReminderPush(context: Context, studentName: String, amount: Double, dueDate: String) {
        val title = "💳 Ödeme Hatırlatması: $studentName"
        val formattedAmount = "${amount.toInt()} ₺"
        val message = "$studentName için $formattedAmount tutarındaki taksit son ödeme tarihi: $dueDate."
        showPushNotification(
            context = context,
            title = title,
            message = message,
            channelId = CHANNEL_FINANCE,
            targetScreen = "FINANCE"
        )
    }

    fun sendFinanceReminderPush(context: Context, studentName: String, amount: Double, dueDate: String, description: String = "Eğitim Taksiti") {
        val title = "💳 Ödeme Hatırlatması: $studentName"
        val formattedAmount = "${amount.toInt()} ₺"
        val message = "$studentName için $description ($formattedAmount) son ödeme tarihi: $dueDate. Lütfen gecikmeye düşmemesi için kontrol ediniz."
        showPushNotification(
            context = context,
            title = title,
            message = message,
            channelId = CHANNEL_FINANCE,
            targetScreen = "FINANCE"
        )
    }

    fun sendTeacherMessagePush(context: Context, senderName: String, messageText: String) {
        val title = "💬 Yeni Mesaj: $senderName"
        showPushNotification(
            context = context,
            title = title,
            message = messageText,
            channelId = CHANNEL_MESSAGES,
            targetScreen = "MESSAGES"
        )
    }

    fun sendDailyReportPush(context: Context, studentName: String, summary: String) {
        val title = "📝 Günlük Bülten Paylaşıldı: $studentName"
        showPushNotification(
            context = context,
            title = title,
            message = summary,
            channelId = CHANNEL_HEALTH_DAILY,
            targetScreen = "DAILY_REPORT"
        )
    }

    fun sendMedicineReminderPush(context: Context, studentName: String, medicineName: String, time: String) {
        val title = "💊 İlaç Takip Bildirimi"
        val message = "$studentName için saat $time $medicineName ilacı başarıyla verildi."
        showPushNotification(
            context = context,
            title = title,
            message = message,
            channelId = CHANNEL_HEALTH_DAILY,
            targetScreen = "MEDICINE"
        )
    }

    fun sendGradeEntryPush(
        context: Context,
        studentName: String,
        subjectName: String,
        examType: String,
        score: Int,
        teacherName: String = "Ders Öğretmeni"
    ) {
        val title = "📊 Yeni Not Girişi: $studentName - $subjectName"
        val message = "$studentName için $subjectName dersi $examType sınav/değerlendirme notu ($score puan) $teacherName tarafından sisteme girildi."
        showPushNotification(
            context = context,
            title = title,
            message = message,
            channelId = CHANNEL_ACADEMIC,
            targetScreen = "REPORT_CARD"
        )
    }

    fun sendBusApproachPush(
        context: Context,
        studentName: String,
        routePlate: String,
        etaMinutes: Int
    ) {
        val title = "🚌 Okul Servisi Yaklaşıyor!"
        val message = "$studentName için $routePlate plakalı servis aracınız durağınıza yaklaşık $etaMinutes dakika içinde varacaktır."
        showPushNotification(
            context = context,
            title = title,
            message = message,
            channelId = CHANNEL_ATTENDANCE,
            targetScreen = "TRANSPORT"
        )
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra("notification_id", -1)
        if (notificationId != -1) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(notificationId)
        }
    }
}
