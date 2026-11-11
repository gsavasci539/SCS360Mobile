package com.example.ui.dialogs.iyzico

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.NumberFormat

private val IyzicoPrimaryBlue = Color(0xFF1E64FF)
private val IyzicoDarkNavy = Color(0xFF132035)
private val IyzicoTextDark = Color(0xFF1F2937)
private val IyzicoCardBorder = Color(0xFFE5E7EB)

@Composable
fun IyzicoPaySection(
    userPhone: String,
    onUserPhoneChange: (String) -> Unit,
    walletBalance: Double,
    selectedSource: String, // "WALLET"
    onSelectedSourceChange: (String) -> Unit,
    selectedInstallment: Int,
    onInstallmentChange: (Int) -> Unit,
    totalToPay: Double,
    currencyFormatter: NumberFormat
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Hero Blue Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = IyzicoPrimaryBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "iyzico Hesabınızla Güvenli Ödeyin",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "iyzico bakiyeniz ile hızlı ve komisyonsuz ödeme gerçekleştirin",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Phone Input / Account verification
        Text(
            text = "Kayıtlı Cep Telefonu Numaranız",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = IyzicoDarkNavy
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = userPhone,
            onValueChange = { onUserPhoneChange(it) },
            placeholder = { Text("05XX XXX XX XX", fontSize = 13.sp, color = Color.Gray) },
            leadingIcon = {
                Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = IyzicoPrimaryBlue)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IyzicoPrimaryBlue,
                unfocusedBorderColor = IyzicoCardBorder
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Ödeme Yöntemi:",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = IyzicoDarkNavy
        )

        Spacer(modifier = Modifier.height(8.dp))

        // iyzico Cüzdan / Bakiye Seçeneği
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(
                if (selectedSource == "WALLET") 2.dp else 1.dp,
                if (selectedSource == "WALLET") IyzicoPrimaryBlue else IyzicoCardBorder
            ),
            color = if (selectedSource == "WALLET") Color(0xFFEFF6FF) else Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectedSourceChange("WALLET") }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedSource == "WALLET",
                    onClick = { onSelectedSourceChange("WALLET") },
                    colors = RadioButtonDefaults.colors(selectedColor = IyzicoPrimaryBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "iyzico Bakiye / Cüzdan",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = IyzicoTextDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Komisyonsuz",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "iyzico hesabınızdaki bakiye üzerinden doğrudan tahsil edilir.",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                Text(
                    text = "₺",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = IyzicoPrimaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // iyzico Korumalı Alışveriş Bloğu
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, IyzicoCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = IyzicoPrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "iyzico Korumalı Alışveriş Güvencesi",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = IyzicoDarkNavy
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• 7/24 Canlı Destek & Kolay İptal/İade Süreci\n• Hesap ve kart bilgilerinizi satıcıyla paylaşmadan güvenli ödeme\n• Anında SMS OTP şifresiyle korumalı onay",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun IyzicoOtpVerificationDialog(
    phoneNumber: String,
    amount: Double,
    currencyFormatter: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableStateOf(120) }

    LaunchedEffect(Unit) {
        while (timerSeconds > 0) {
            delay(1000)
            timerSeconds--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = IyzicoPrimaryBlue,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "iyzico",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("iyzico SMS Onayı", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IyzicoDarkNavy)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (phoneNumber.isNotBlank()) {
                        "$phoneNumber nolu telefonunuza tek kullanımlık SMS onay kodu iletilmiştir."
                    } else {
                        "iyzico hesabınıza bağlı telefonunuza tek kullanımlık SMS onay kodu iletilmiştir."
                    },
                    fontSize = 12.sp,
                    color = IyzicoTextDark,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Onaylanacak Tutar:", fontSize = 11.sp, color = Color(0xFF1E40AF))
                        Text(currencyFormatter.format(amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IyzicoPrimaryBlue)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it.filter { c -> c.isDigit() }.take(6) },
                    placeholder = { Text("6 Haneli SMS Kodu") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Süre: ${timerSeconds / 60}:${(timerSeconds % 60).toString().padStart(2, '0')}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = otpCode.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = IyzicoPrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Doğrula ve Öde", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", color = Color.Gray)
            }
        }
    )
}
