package com.example.util

import android.util.Log
import com.example.data.local.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class Okul360FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "Okul360FCMService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Yeni Firebase Cloud Messaging (FCM) Cihaz Tokenı alındı: $token")
        val sessionManager = SessionManager(applicationContext)
        sessionManager.saveFcmToken(token)
        // Token backend sunucusuna kaydedilebilir
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Mesajı Geldi: from=${remoteMessage.from}, data=${remoteMessage.data}")

        // 1. Data payload
        val data = remoteMessage.data
        val type = data["type"] ?: data["category"] ?: "ANNOUNCEMENT"
        val title = data["title"] ?: remoteMessage.notification?.title ?: "SCS360 Duyurusu"
        val message = data["message"] ?: data["body"] ?: remoteMessage.notification?.body ?: "Yeni bir okul duyurusu yayınlandı."
        val targetScreen = data["target_screen"] ?: "ANNOUNCEMENTS"
        val payloadId = data["payload_id"] ?: data["announcement_id"]

        when (type.uppercase()) {
            "ANNOUNCEMENT", "DUYURU" -> {
                val author = data["author"] ?: "Okul Yönetimi"
                NotificationHelper.sendAnnouncementPush(
                    context = applicationContext,
                    title = title,
                    content = message,
                    author = author
                )
            }
            "ATTENDANCE", "YOKLAMA" -> {
                val studentName = data["student_name"] ?: "Öğrenciniz"
                val statusText = data["status"] ?: "Durum güncellendi"
                val className = data["class_name"] ?: "Sınıf"
                NotificationHelper.sendAttendancePush(
                    context = applicationContext,
                    studentName = studentName,
                    statusText = statusText,
                    className = className
                )
            }
            "FINANCE", "ODEME", "TAKSIT" -> {
                val studentName = data["student_name"] ?: "Öğrenciniz"
                val rawAmount = data["amount"]?.replace("₺", "")?.replace("TL", "")?.trim()
                val parsedAmount = rawAmount?.let {
                    if (it.contains(",") && it.contains(".")) {
                        it.replace(".", "").replace(",", ".").toDoubleOrNull()
                    } else if (it.contains(",")) {
                        it.replace(",", ".").toDoubleOrNull()
                    } else {
                        it.toDoubleOrNull()
                    }
                } ?: 0.0
                val dueDate = data["due_date"] ?: "Bu Ay"
                NotificationHelper.sendPaymentReminderPush(
                    context = applicationContext,
                    studentName = studentName,
                    amount = parsedAmount,
                    dueDate = dueDate
                )
            }
            "DAILY_REPORT", "BULLETIN", "GUNLUK", "KARNE" -> {
                val studentName = data["student_name"] ?: "Öğrenciniz"
                val summary = data["summary"] ?: message
                NotificationHelper.sendDailyReportPush(
                    context = applicationContext,
                    studentName = studentName,
                    summary = summary
                )
            }
            "MEDICINE", "ILAC", "REVIR" -> {
                val studentName = data["student_name"] ?: "Öğrenciniz"
                val medicineName = data["medicine_name"] ?: "Reçeteli İlaç"
                val time = data["time"] ?: "13:00"
                NotificationHelper.sendMedicineReminderPush(
                    context = applicationContext,
                    studentName = studentName,
                    medicineName = medicineName,
                    time = time
                )
            }
            "BUS", "SERVIS", "TRANSPORT" -> {
                val studentName = data["student_name"] ?: "Öğrenciniz"
                val routePlate = data["route_plate"] ?: "34 ABC 123"
                val etaMinutes = data["eta_minutes"]?.toIntOrNull() ?: 5
                NotificationHelper.sendBusApproachPush(
                    context = applicationContext,
                    studentName = studentName,
                    routePlate = routePlate,
                    etaMinutes = etaMinutes
                )
            }
            "CHAT", "MESSAGE", "MESAJ" -> {
                val senderName = data["sender_name"] ?: data["author"] ?: "Öğretmen"
                NotificationHelper.sendTeacherMessagePush(
                    context = applicationContext,
                    senderName = senderName,
                    messageText = message
                )
            }
            "GRADE", "NOT", "EXAM", "ACADEMIC" -> {
                val studentName = data["student_name"] ?: "Öğrenciniz"
                val subjectName = data["subject_name"] ?: data["course_name"] ?: "Ders"
                val examType = data["exam_type"] ?: "Sınav"
                val score = data["score"]?.toIntOrNull() ?: 95
                val teacherName = data["teacher_name"] ?: data["author"] ?: "Ders Öğretmeni"
                NotificationHelper.sendGradeEntryPush(
                    context = applicationContext,
                    studentName = studentName,
                    subjectName = subjectName,
                    examType = examType,
                    score = score,
                    teacherName = teacherName
                )
            }
            else -> {
                NotificationHelper.showPushNotification(
                    context = applicationContext,
                    title = title,
                    message = message,
                    channelId = NotificationHelper.CHANNEL_ANNOUNCEMENT,
                    targetScreen = targetScreen,
                    payloadId = payloadId
                )
            }
        }
    }
}
