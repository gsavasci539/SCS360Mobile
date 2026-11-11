package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PaymentReceipt
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.PrimaryBlue

@Composable
fun ReceiptDialog(
    receipt: PaymentReceipt,
    onDismiss: () -> Unit,
    onPrintOrShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Top Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFECFDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TAHSİLAT MAKBUZU",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = BrandNavy
                            )
                            Text(
                                text = receipt.receipt_number,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Institution Info
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = receipt.institution_name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = receipt.branch_name,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Yetkili: ${receipt.collected_by}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Details Grid
                ReceiptRow(label = "Öğrenci Adı Soyadı", value = receipt.student_name)
                ReceiptRow(label = "Öğrenci No / Sınıf", value = "${receipt.student_number} • ${receipt.class_name}")
                ReceiptRow(label = "Veli Bilgisi", value = "${receipt.parent_name} (${receipt.parent_phone})")
                ReceiptRow(label = "Fatura Numarası", value = receipt.invoice_number)
                ReceiptRow(label = "Açıklama", value = receipt.description)
                ReceiptRow(label = "Ödeme Yöntemi", value = receipt.payment_method)
                ReceiptRow(label = "Tahsilat Tarihi", value = receipt.payment_date)

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Box
                Surface(
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tahsil Edilen Tutar",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3A8A),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "₺${String.format("%,.2f", receipt.amount)}",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E40AF),
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // QR Code Verification Placeholder & Digital Stamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Doğrulama",
                            tint = BrandNavy,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "E-Makbuz Karekodu",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "E-İmza Onaylı",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SCS360 Dijital Arşiv",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Kapat")
                    }
                    Button(
                        onClick = onPrintOrShare,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paylaş / Yazdır")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandNavy,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
fun ReceiptDialog(
    invoice: com.example.data.model.Invoice,
    onDismiss: () -> Unit,
    onPrintOrShare: () -> Unit = {}
) {
    val receipt = PaymentReceipt(
        receipt_number = "MAK-${invoice.invoice_number.takeLast(6)}",
        invoice_id = invoice.id,
        invoice_number = invoice.invoice_number,
        student_id = invoice.student_id,
        student_name = invoice.student_name,
        student_number = "2026-${(100..999).random()}",
        class_name = invoice.class_name,
        parent_name = "Veli",
        parent_phone = "+90 532 555 0101",
        amount = invoice.amount,
        payment_method = invoice.payment_method ?: "Kredi Kartı / Havale",
        payment_date = invoice.issue_date,
        description = invoice.description,
        collected_by = "Gökhan Savaşçı (Muhasebe)",
        institution_name = "SCS360 Eğitim Kurumları",
        branch_name = "Ataşehir Merkez Kampüs",
        qr_code_text = "SCS360-RECEIPT-${invoice.id}-${invoice.amount}"
    )
    ReceiptDialog(receipt = receipt, onDismiss = onDismiss, onPrintOrShare = onPrintOrShare)
}

