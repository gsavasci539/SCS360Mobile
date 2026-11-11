package com.example.ui.dialogs.iyzico

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat

private val IyzicoPrimaryBlue = Color(0xFF1E64FF)
private val IyzicoDarkNavy = Color(0xFF132035)
private val IyzicoTextDark = Color(0xFF1F2937)
private val IyzicoCardBorder = Color(0xFFE5E7EB)

data class BankAccountInfo(
    val id: String,
    val name: String,
    val iban: String,
    val branch: String
)

@Composable
fun IyzicoEftSection(
    selectedBank: String,
    onBankSelect: (String) -> Unit,
    transferReferenceCode: String,
    totalToPay: Double,
    currencyFormatter: NumberFormat
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val banks = remember {
        listOf(
            BankAccountInfo("IS_BANK", "İş Bankası", "TR68 0006 4000 0011 2345 6789 01", "Kadıköy / 1042"),
            BankAccountInfo("GARANTI", "Garanti BBVA", "TR56 0006 2000 0001 2987 6543 21", "Maslak / 3401"),
            BankAccountInfo("ZIRAAT", "Ziraat Bankası", "TR12 0001 0000 0019 8765 4321 00", "Ankara Kurumsal"),
            BankAccountInfo("YAPI_KREDI", "Yapı Kredi", "TR94 0006 7010 0000 0098 7654 32", "Levent / 512"),
            BankAccountInfo("AKBANK", "Akbank", "TR31 0004 6001 2345 6789 0123 45", "Sabancı Center")
        )
    }

    val currentBank = banks.find { it.id == selectedBank } ?: banks.firstOrNull() ?: BankAccountInfo("IS_BANK", "İş Bankası", "TR68 0006 4000 0011 2345 6789 01", "Kadıköy / 1042")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Hero EFT Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Havale / FAST ile Kolay Ödeme",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF22C55E),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "7/24 FAST",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Aşağıdaki banka hesabına referans kodu ile transfer yapınız",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bank Selection Horizontal Bar
        Text(
            text = "Banka Seçiniz:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = IyzicoDarkNavy
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(banks) { bank ->
                val isSelected = bank.id == selectedBank
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) IyzicoPrimaryBlue else IyzicoCardBorder
                    ),
                    color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
                    modifier = Modifier.clickable { onBankSelect(bank.id) }
                ) {
                    Text(
                        text = bank.name,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) IyzicoPrimaryBlue else IyzicoTextDark,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Official iyzico Virtual Account Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, IyzicoCardBorder),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Alıcı Adı
                EftDetailRow(
                    label = "Alıcı Unvanı (Alıcı Adı)",
                    value = "iyzi Ödeme ve Elektronik Para Hizmetleri A.Ş.",
                    onCopy = {
                        clipboardManager.setText(AnnotatedString("iyzi Ödeme ve Elektronik Para Hizmetleri A.Ş."))
                        Toast.makeText(context, "Alıcı unvanı kopyalandı", Toast.LENGTH_SHORT).show()
                    }
                )

                Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))

                // IBAN
                EftDetailRow(
                    label = "${currentBank.name} IBAN Numarası",
                    value = currentBank.iban,
                    isMonospace = true,
                    highlight = true,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(currentBank.iban.replace(" ", "")))
                        Toast.makeText(context, "IBAN kopyalandı", Toast.LENGTH_SHORT).show()
                    }
                )

                Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))

                // Transfer Reference Code (Açıklama)
                EftDetailRow(
                    label = "Açıklama / Referans Kodu (Zorunlu)",
                    value = transferReferenceCode,
                    isMonospace = true,
                    isCode = true,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(transferReferenceCode))
                        Toast.makeText(context, "Açıklama kodu kopyalandı", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Warning & instruction note
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFFBEB),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transfer yaparken Açıklama alanına '$transferReferenceCode' kodunu yazmanız ödemenizin anında otomatik onaylanması için zorunludur.",
                    fontSize = 11.sp,
                    color = Color(0xFF92400E),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun EftDetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    highlight: Boolean = false,
    isCode: Boolean = false,
    onCopy: () -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = Color(0xFF6B7280),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontSize = if (isCode) 14.sp else if (highlight) 12.5.sp else 12.sp,
                fontWeight = if (isCode || highlight) FontWeight.Bold else FontWeight.Medium,
                color = if (isCode) IyzicoPrimaryBlue else if (highlight) IyzicoDarkNavy else IyzicoTextDark,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Kopyala",
                    tint = IyzicoPrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
