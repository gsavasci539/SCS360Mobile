package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, title: String, amount: Double, date: String, receiptUrl: String?) -> Unit
) {
    val categories = listOf("Kira", "Mutfak", "Kırtasiye", "Fatura", "Maaş", "Diğer")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var dateText by remember { mutableStateOf(today) }
    var receiptNote by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Yeni Gider (Kasa Çıkışı)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gider Kategorisi") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("expense_category_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("Gider Açıklaması / Başlık") },
                    placeholder = { Text("Örn: Kampüs Ağustos Elektrik Faturası") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Tutar (₺)") },
                    placeholder = { Text("Örn: 4500") },
                    leadingIcon = { Text("₺", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Date
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("İşlem Tarihi (YYYY-AA-GG)") },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Receipt / Doc Ref
                OutlinedTextField(
                    value = receiptNote,
                    onValueChange = { receiptNote = it },
                    label = { Text("Fiş / Fatura No veya Belge Ref (Opsiyonel)") },
                    placeholder = { Text("Örn: FTR-2026-9812") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (title.isBlank()) {
                        errorMessage = "Lütfen gider açıklamasını giriniz."
                    } else if (amount == null || amount <= 0) {
                        errorMessage = "Lütfen geçerli bir harcama tutarı giriniz."
                    } else {
                        onConfirm(
                            selectedCategory,
                            title.trim(),
                            amount,
                            dateText.trim().ifBlank { today },
                            receiptNote.trim().ifBlank { null }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_add_expense_button")
            ) {
                Text("Gideri Kaydet", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("İptal")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
