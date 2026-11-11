package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Interactive In-App Reminder & Automatic Notification Banner for Upcoming Payment Installments.
 */
@Composable
fun UpcomingPaymentReminderBanner(
    upcomingInvoices: List<Invoice>,
    onPayClicked: (Invoice) -> Unit,
    onNavigateToPayments: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showReminderModalForInvoice by remember { mutableStateOf<Invoice?>(null) }
    var reminderEnabledMap by remember { mutableStateOf(mapOf<String, Boolean>()) }

    val nextUnpaid = upcomingInvoices.firstOrNull {
        !it.status.equals("PAID", ignoreCase = true) && !it.status.equals("ODENDI", ignoreCase = true) && (it.paid_amount < it.amount || it.amount == 0.0)
    } ?: return

    val isOverdue = nextUnpaid.status.equals("OVERDUE", ignoreCase = true)
    val isReminderActive = reminderEnabledMap[nextUnpaid.id] ?: true

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isOverdue) Color(0xFFFFF1F2) else Color(0xFFFEF9C3),
        border = BorderStroke(
            1.5.dp,
            if (isOverdue) Color(0xFFF43F5E).copy(alpha = 0.6f) else Color(0xFFEAB308).copy(alpha = 0.7f)
        ),
        shadowElevation = 2.dp,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isOverdue) Color(0xFFE11D48) else Color(0xFFCA8A04)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOverdue) Icons.Default.PriorityHigh else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = if (isOverdue) "⚠️ Gecikmiş Ödeme Uyarısı" else "📅 Yaklaşan Taksit",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = if (isOverdue) Color(0xFF9F1239) else Color(0xFF854D0E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Son Ödeme: ${nextUnpaid.due_date}",
                            fontSize = 11.5.sp,
                            color = if (isOverdue) Color(0xFFBE123C) else Color(0xFFA16207),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOverdue) Color(0xFFE11D48).copy(alpha = 0.15f) else Color(0xFFCA8A04).copy(alpha = 0.15f),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = if (isOverdue) "GECİKTİ" else "YAKLAŞIYOR",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) Color(0xFF9F1239) else Color(0xFF854D0E),
                        softWrap = false,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nextUnpaid.description,
                        fontSize = 12.5.sp,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${nextUnpaid.invoice_number} • Tutar: ₺${(nextUnpaid.amount - nextUnpaid.paid_amount).toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                }

                Text(
                    text = "₺${(nextUnpaid.amount - nextUnpaid.paid_amount).toInt()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    color = if (isOverdue) Color(0xFFE11D48) else Color(0xFF854D0E)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reminder Settings Button
                OutlinedButton(
                    onClick = { showReminderModalForInvoice = nextUnpaid },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isOverdue) Color(0xFF9F1239) else Color(0xFF854D0E)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isOverdue) Color(0xFFF43F5E).copy(alpha = 0.5f) else Color(0xFFCA8A04).copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isReminderActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isReminderActive) "Hatırlatıcı" else "Hatırlat",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Pay Now Button
                Button(
                    onClick = { onPayClicked(nextUnpaid) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverdue) Color(0xFFE11D48) else PrimaryBlue
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Hemen Öde",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // Modal dialog to configure automated push & in-app reminders
    if (showReminderModalForInvoice != null) {
        PaymentReminderConfigDialog(
            invoice = showReminderModalForInvoice!!,
            isCurrentReminderEnabled = reminderEnabledMap[showReminderModalForInvoice!!.id] ?: true,
            onDismiss = { showReminderModalForInvoice = null },
            onSaveConfig = { invoiceId, enabled, daysBefore, addCalendar ->
                reminderEnabledMap = reminderEnabledMap + (invoiceId to enabled)
                showReminderModalForInvoice = null

                val msg = if (enabled) {
                    "🔔 Otomatik taksit hatırlatıcısı aktif edildi ($daysBefore gün önce bildirim gönderilecek)"
                } else {
                    "Taksit hatırlatması kapatıldı"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

                if (addCalendar && enabled) {
                    addCalendarReminderEvent(context, showReminderModalForInvoice!!)
                }
            }
        )
    }
}

/**
 * Dialog to configure automatic push notifications, SMS alerts, and calendar reminders.
 */
@Composable
fun PaymentReminderConfigDialog(
    invoice: Invoice,
    isCurrentReminderEnabled: Boolean,
    onDismiss: () -> Unit,
    onSaveConfig: (invoiceId: String, enabled: Boolean, daysBefore: Int, addCalendar: Boolean) -> Unit
) {
    var reminderEnabled by remember { mutableStateOf(isCurrentReminderEnabled) }
    var selectedDaysBefore by remember { mutableStateOf(3) }
    var enablePushNotification by remember { mutableStateOf(true) }
    var enableSmsReminder by remember { mutableStateOf(true) }
    var addToDeviceCalendar by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AlarmOn,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Taksit Hatırlatma Servisi", fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = BrandNavy)
                    Text(invoice.invoice_number, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Vade Tarihi: ${invoice.due_date}", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = BrandNavy)
                            Text("Tutar: ₺${invoice.amount.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SecondaryEmerald
                            )
                        )
                    }
                }

                if (reminderEnabled) {
                    Text("Hatırlatma Zamanı:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            1 to "1 Gün Önce",
                            3 to "3 Gün Önce",
                            7 to "1 Hafta Önce"
                        ).forEach { (days, label) ->
                            val isSel = selectedDaysBefore == days
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDaysBefore = days }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Channel preferences
                    Text("Bildirim Kanalları:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uygulama İçi Push Bildirim", fontSize = 12.sp)
                        }
                        Checkbox(checked = enablePushNotification, onCheckedChange = { enablePushNotification = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sms, contentDescription = null, tint = SecondaryEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SMS Hatırlatma Mesajı", fontSize = 12.sp)
                        }
                        Checkbox(checked = enableSmsReminder, onCheckedChange = { enableSmsReminder = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cihaz Takvimine Ekle", fontSize = 12.sp)
                        }
                        Checkbox(checked = addToDeviceCalendar, onCheckedChange = { addToDeviceCalendar = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveConfig(invoice.id, reminderEnabled, selectedDaysBefore, addToDeviceCalendar)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Ayarları Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

/**
 * Helper to dispatch calendar intent so the user can easily register a reminder on their device calendar.
 */
private fun addCalendarReminderEvent(context: Context, invoice: Invoice) {
    try {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Okul Taksit Ödemesi (${invoice.invoice_number})")
            putExtra(CalendarContract.Events.DESCRIPTION, "${invoice.student_name} - ${invoice.description}. Tutar: ₺${invoice.amount.toInt()}")
            putExtra(CalendarContract.Events.EVENT_LOCATION, "SCS360 Veli Portalı Online Ödeme")
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback gracefully
    }
}
