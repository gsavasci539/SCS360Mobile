package com.example.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ClassGroup
import com.example.data.model.Invoice
import com.example.data.model.Student
import com.example.data.model.User
import com.example.ui.components.AttendanceHeatmapCalendar
import com.example.ui.components.MonthlyAttendanceBarChart
import com.example.ui.components.StatusBadge
import com.example.ui.dialogs.ceppos.CepPosPaymentDialog
import com.example.ui.dialogs.iyzico.IyzicoPaymentDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentDialog(
    classes: List<ClassGroup>,
    onDismiss: () -> Unit,
    onConfirm: (firstName: String, lastName: String, classId: String, parentName: String, parentPhone: String, allergies: String?) -> Unit
) {
    val selectableClasses = remember(classes) {
        classes.filter { it.is_active }
    }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedClassId by remember(selectableClasses) {
        mutableStateOf(
            selectableClasses.firstOrNull { it.student_count < it.capacity }?.id
                ?: selectableClasses.firstOrNull()?.id
                ?: ""
        )
    }
    var parentName by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var classSearchQuery by remember { mutableStateOf("") }

    val selectedClass = selectableClasses.find { it.id == selectedClassId } ?: classes.find { it.id == selectedClassId }
    val isSelectedClassFull = selectedClass != null && selectedClass.student_count >= selectedClass.capacity

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Yeni Öğrenci Kaydı",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Text(
                    text = "Öğrenci ve veli bilgilerini doldurarak sisteme dahil edin.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Öğrenci Adı") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Öğrenci Soyadı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Class selection with active/capacity filter & search
                ExposedDropdownMenuBox(
                    expanded = classDropdownExpanded,
                    onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClass?.let { "${it.name} (${it.student_count}/${it.capacity})" } ?: "Sınıf Seçiniz",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kayıt Edilecek Aktif Sınıf & Kontenjan") },
                        supportingText = if (isSelectedClassFull) {
                            { Text("Seçili sınıf kontenjanı dolu (${selectedClass?.student_count}/${selectedClass?.capacity}).", color = StatusError) }
                        } else null,
                        isError = isSelectedClassFull,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false }
                    ) {
                        OutlinedTextField(
                            value = classSearchQuery,
                            onValueChange = { classSearchQuery = it },
                            placeholder = { Text("Sınıf ara (100+ sınıf filtreleme)...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        val filteredClasses = selectableClasses.filter {
                            classSearchQuery.isBlank() ||
                                it.name.contains(classSearchQuery, ignoreCase = true) ||
                                it.grade_level.contains(classSearchQuery, ignoreCase = true)
                        }
                        filteredClasses.forEach { cls ->
                            val isFull = cls.student_count >= cls.capacity
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${cls.name} (${cls.grade_level})", fontWeight = FontWeight.Medium)
                                        Text(
                                            text = if (isFull) "${cls.student_count}/${cls.capacity} • DOLU" else "${cls.student_count}/${cls.capacity}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFull) StatusError else SecondaryEmerald
                                        )
                                    }
                                },
                                enabled = !isFull,
                                onClick = {
                                    if (!isFull) {
                                        selectedClassId = cls.id
                                        classDropdownExpanded = false
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = parentName,
                    onValueChange = { parentName = it },
                    label = { Text("Veli Adı & Soyadı (Örn: Mehmet Kaya - Baba)") },
                    leadingIcon = { Icon(Icons.Default.SupervisorAccount, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = { parentPhone = it },
                    label = { Text("Veli Telefon Numarası") },
                    placeholder = { Text("+90 5XX XXX XX XX") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Alerji / Özel Sağlık Durumu (Opsiyonel)") },
                    placeholder = { Text("Örn: Laktoz intoleransı, Astım") },
                    leadingIcon = { Icon(Icons.Default.HealthAndSafety, contentDescription = null) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (firstName.isNotBlank() && lastName.isNotBlank() && parentName.isNotBlank()) {
                            onConfirm(firstName, lastName, selectedClassId, parentName, parentPhone, allergies)
                        }
                    },
                    enabled = firstName.isNotBlank() && lastName.isNotBlank() && parentName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Öğrenciyi Kaydet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onConfirm: (paymentMethod: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("CEPPOS") }
    var showIyzicoModal by remember { mutableStateOf(false) }
    var showCepPosModal by remember { mutableStateOf(false) }

    val methods = listOf(
        Triple("CEPPOS", "Cep POS (NFC Temassız / QR SoftPOS)", Icons.Default.Nfc),
        Triple("IYZICO", "İyzico Sandbox Online Kredi Kartı (3D Secure)", Icons.Default.CreditCard),
        Triple("BANK_TRANSFER", "Banka Havalesi / EFT", Icons.Default.AccountBalance),
        Triple("CASH", "Nakit Tahsilat", Icons.Default.Payments)
    )

    if (showCepPosModal) {
        CepPosPaymentDialog(
            invoice = invoice,
            studentName = invoice.student_name,
            paymentDescription = "${invoice.invoice_number} - ${invoice.description}",
            onDismiss = {
                showCepPosModal = false
                onDismiss()
            },
            onPaymentSuccess = { result, methodTitle ->
                onConfirm("$methodTitle (Ref: ${result.rrn})")
                showCepPosModal = false
            }
        )
    } else if (showIyzicoModal) {
        IyzicoPaymentDialog(
            invoice = invoice,
            studentName = invoice.student_name,
            paymentDescription = "${invoice.invoice_number} - ${invoice.description}",
            onDismiss = {
                showIyzicoModal = false
                onDismiss()
            },
            onPaymentSuccess = { result, methodTitle ->
                onConfirm("$methodTitle (Ref: ${result.paymentId})")
                showIyzicoModal = false
            }
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tahsilat Kaydı",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = invoice.student_name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${invoice.class_name} • Fatura: ${invoice.invoice_number}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Ödenecek Tutar:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "₺${invoice.amount.toInt()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = SecondaryEmerald
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Ödeme Yöntemi Seçin",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    methods.forEach { (key, title, icon) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedMethod = key },
                            color = if (selectedMethod == key) PrimaryBlueContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedMethod == key) PrimaryBlue else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedMethod == key,
                                    onClick = { selectedMethod = key }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            when (selectedMethod) {
                                "CEPPOS" -> showCepPosModal = true
                                "IYZICO" -> showIyzicoModal = true
                                else -> onConfirm(selectedMethod)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMethod == "CEPPOS") Color(0xFF059669) else SecondaryEmerald
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (selectedMethod) {
                                "CEPPOS" -> "Cep POS Terminalini Aç (NFC)"
                                "IYZICO" -> "İyzico Ödeme Ekranına Geç"
                                else -> "Ödemeyi Onayla & Makbuz Üret"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnouncementDialog(
    initialTitle: String = "",
    initialContent: String = "",
    initialCategory: String = "GENERAL",
    initialTarget: String = "ALL",
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, category: String, target: String) -> Unit,
    onSaveDraft: ((title: String, content: String, category: String, target: String) -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedTarget by remember { mutableStateOf(initialTarget) }

    val categories = listOf(
        "GENERAL" to "Genel Duyuru",
        "ACADEMIC" to "Akademik / Ders",
        "EVENT" to "Etkinlik & Gezi",
        "FINANCIAL" to "Mali Bilgilendirme",
        "EMERGENCY" to "Acil Durum"
    )

    val targets = listOf(
        "ALL" to "Tüm Kurum (Veli & Öğretmen)",
        "PARENTS" to "Sadece Veliler",
        "TEACHERS" to "Sadece Öğretmenler"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (initialTitle.isNotBlank()) "Duyuruyu Düzenle / Yayınla" else "Yeni Duyuru Yayınla",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Duyuru Başlığı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Duyuru Kategorisi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { (cat, label) ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(label, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Hedef Kitle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(targets) { (tgt, label) ->
                        FilterChip(
                            selected = selectedTarget == tgt,
                            onClick = { selectedTarget = tgt },
                            label = { Text(label, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Duyuru Metni") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onConfirm(title, content, selectedCategory, selectedTarget)
                            }
                        },
                        enabled = title.isNotBlank() && content.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Duyuruyu Yayınla", fontWeight = FontWeight.Bold)
                    }

                    if (onSaveDraft != null) {
                        OutlinedButton(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSaveDraft(title, content, selectedCategory, selectedTarget)
                                }
                            },
                            enabled = title.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Drafts, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Taslak Olarak Kaydet", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailBottomSheet(
    student: Student,
    onDismiss: () -> Unit,
    onViewReportCard: ((Student) -> Unit)? = null,
    onOpenProfile: ((Student) -> Unit)? = null
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(student.avatar_color_hex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${student.first_name.take(1)}${student.last_name.take(1)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.full_name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = "No: ${student.student_number} • ${student.class_name}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        StatusBadge(
                            label = if (student.status == "ACTIVE") "Aktif Kayıt" else "Aday",
                            color = SecondaryEmerald
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(
                            label = "Kan: ${student.blood_type ?: "Bilinmiyor"}",
                            color = PrimaryBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row (Detaylı Profil & Karne)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onOpenProfile != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenProfile(student)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("👤 Detaylı Öğrenci Profilini Aç", fontWeight = FontWeight.Bold)
                    }
                }

                // Karne & Not Görüntüleyici Quick Action
                if (onViewReportCard != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onViewReportCard(student)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7C3AED)),
                        border = BorderStroke(1.dp, Color(0xFF7C3AED))
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📜 Dönem Notları & Karne Görüntüle", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Veli Contact Section with Call & WhatsApp
            Text(
                text = "Veli İletişim Bilgileri",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = student.parent_name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = student.parent_phone,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${student.parent_phone}")
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ara", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val cleanPhone = student.parent_phone.replace("[^0-9]".toRegex(), "")
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/$cleanPhone")
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar-based Attendance Heatmap Visualization
            AttendanceHeatmapCalendar(
                student = student,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Monthly Attendance Bar Chart (Devam-Devamsızlık Veri Görselleştirmesi)
            MonthlyAttendanceBarChart(
                student = student,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sağlık ve Alerji Bilgisi
            Text(
                text = "Sağlık & Beslenme Notları",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (student.allergies_or_diet == "Yok" || student.allergies_or_diet == null)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else AccentRoseContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = if (student.allergies_or_diet == "Yok" || student.allergies_or_diet == null) PrimaryBlue else AccentRose
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = student.allergies_or_diet ?: "Belirtilen alerji veya diyet kısıtı bulunmuyor.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Finans & Bakiye Bilgisi
            Text(
                text = "Eğitim Ücreti & Bakiye Durumu",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Kalan Tahsilat Bakiyesi:", fontSize = 13.sp)
                    Text(
                        text = if (student.tuition_balance <= 0) "Borcu Yok (₺0)" else "₺${student.tuition_balance.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (student.tuition_balance <= 0) SecondaryEmerald else AccentRose
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentDialog(
    student: Student,
    classes: List<ClassGroup>,
    onDismiss: () -> Unit,
    onConfirm: (Student) -> Unit
) {
    var firstName by remember { mutableStateOf(student.first_name) }
    var lastName by remember { mutableStateOf(student.last_name) }
    var selectedClassId by remember { mutableStateOf(student.class_id) }
    var enrollmentDate by remember { mutableStateOf(student.enrollment_date ?: "") }
    var address by remember { mutableStateOf(student.address ?: "") }
    var notes by remember { mutableStateOf(student.notes ?: "") }
    var healthNotes by remember { mutableStateOf(student.health_notes ?: "") }
    var parentName by remember { mutableStateOf(student.parent_name) }
    var parentPhone by remember { mutableStateOf(student.parent_phone) }
    var allergies by remember { mutableStateOf(student.allergies_or_diet ?: "") }
    var classDropdownExpanded by remember { mutableStateOf(false) }

    val selectableClasses = remember(classes, student.class_id) {
        classes.filter { it.is_active || it.id == student.class_id }
    }
    var classSearchQuery by remember { mutableStateOf("") }
    val selectedClass = selectableClasses.find { it.id == selectedClassId } ?: classes.find { it.id == selectedClassId }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Öğrenciyi Düzenle",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Kapat") }
                }

                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Adı") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Soyadı") }, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(
                    expanded = classDropdownExpanded,
                    onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClass?.let { "${it.name} (${it.student_count}/${it.capacity})" } ?: "Sınıf Seçiniz",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sınıf & Kontenjan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false }
                    ) {
                        OutlinedTextField(
                            value = classSearchQuery,
                            onValueChange = { classSearchQuery = it },
                            placeholder = { Text("Sınıf ara (100+ filtreleme)...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        val filteredClasses = selectableClasses.filter {
                            classSearchQuery.isBlank() ||
                                it.name.contains(classSearchQuery, ignoreCase = true) ||
                                it.grade_level.contains(classSearchQuery, ignoreCase = true)
                        }
                        filteredClasses.forEach { cls ->
                            val isCurrentClass = cls.id == student.class_id
                            val isFullForTransfer = !isCurrentClass && cls.student_count >= cls.capacity
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isCurrentClass) "${cls.name} (Mevcut)" else cls.name,
                                            fontWeight = if (isCurrentClass) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = if (isFullForTransfer) "${cls.student_count}/${cls.capacity} • DOLU" else "${cls.student_count}/${cls.capacity}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFullForTransfer) StatusError else SecondaryEmerald
                                        )
                                    }
                                },
                                enabled = !isFullForTransfer,
                                onClick = {
                                    if (!isFullForTransfer) {
                                        selectedClassId = cls.id
                                        classDropdownExpanded = false
                                    }
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = enrollmentDate,
                    onValueChange = { enrollmentDate = it },
                    label = { Text("Kayıt Tarihi (YYYY-MM-DD • Boş bırakılırsa temizlenir)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Ev Adresi (Boş bırakılırsa temizlenir)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Genel Öğrenci Notu (Boş bırakılırsa temizlenir)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = healthNotes,
                    onValueChange = { healthNotes = it },
                    label = { Text("Sağlık Notu (health.write yetkisiyle • Boş bırakılırsa temizlenir)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = parentName, onValueChange = { parentName = it }, label = { Text("Veli Adı & Soyadı") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = parentPhone, onValueChange = { parentPhone = it }, label = { Text("Veli Telefonu") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = allergies, onValueChange = { allergies = it }, label = { Text("Alerji / Diyet") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val updated = student.copy(
                            first_name = firstName,
                            last_name = lastName,
                            class_id = selectedClassId,
                            class_name = selectedClass?.name ?: student.class_name,
                            enrollment_date = enrollmentDate.trim().takeIf { it.isNotBlank() },
                            address = address.trim().takeIf { it.isNotBlank() },
                            notes = notes.trim().takeIf { it.isNotBlank() },
                            health_notes = healthNotes.trim().takeIf { it.isNotBlank() },
                            parent_name = parentName,
                            parent_phone = parentPhone,
                            allergies_or_diet = allergies.trim().takeIf { it.isNotBlank() }
                        )
                        onConfirm(updated)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Değişiklikleri Kaydet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRose) },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
            ) {
                Text("Evet, Sil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvoiceDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onConfirm: (studentId: String, studentName: String, className: String, amount: Double, dueDate: String, desc: String) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
    var amountText by remember { mutableStateOf("4500") }
    var dueDate by remember { mutableStateOf("2026-08-30") }
    var description by remember { mutableStateOf("Eğitim ve Yemek Hizmeti") }
    var studentDropdownExpanded by remember { mutableStateOf(false) }

    val selectedStudent = students.find { it.id == selectedStudentId }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Yeni Ödeme / Fatura Ekle", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }

                ExposedDropdownMenuBox(
                    expanded = studentDropdownExpanded,
                    onExpandedChange = { studentDropdownExpanded = !studentDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedStudent?.full_name ?: "Öğrenci Seçiniz",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Öğrenci") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        students.forEach { std ->
                            DropdownMenuItem(
                                text = { Text("${std.full_name} (${std.class_name})") },
                                onClick = {
                                    selectedStudentId = std.id
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Tutar (₺)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Son Ödeme Tarihi (YYYY-AA-GG)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Açıklama / Kalem") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (selectedStudent != null && amount > 0) {
                            onConfirm(selectedStudent.id, selectedStudent.full_name, selectedStudent.class_name, amount, dueDate, description)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Faturayı Oluştur", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditInvoiceDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onConfirm: (Invoice) -> Unit
) {
    var amountText by remember { mutableStateOf(invoice.amount.toInt().toString()) }
    var dueDate by remember { mutableStateOf(invoice.due_date) }
    var description by remember { mutableStateOf(invoice.description) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Faturayı Düzenle", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }

                Text("Fatura No: ${invoice.invoice_number}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PrimaryBlue)
                Text("Öğrenci: ${invoice.student_name} (${invoice.class_name})", fontSize = 13.sp)

                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Tutar (₺)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Son Ödeme Tarihi") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: invoice.amount
                        onConfirm(invoice.copy(amount = amount, due_date = dueDate, description = description))
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Değişiklikleri Kaydet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddClassDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, gradeLevel: String, teacherName: String, capacity: Int, roomNumber: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("Anaokulu") }
    var teacherName by remember { mutableStateOf("") }
    var capacityText by remember { mutableStateOf("20") }
    var roomNumber by remember { mutableStateOf("Derslik 101") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Yeni Sınıf Ekle", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Sınıf Adı (Örn: Kelebekler Sınıfı)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = gradeLevel, onValueChange = { gradeLevel = it }, label = { Text("Seviye / Kademe (Örn: Anaokulu, 1. Sınıf)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = teacherName, onValueChange = { teacherName = it }, label = { Text("Sınıf Öğretmeni Adı") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = capacityText, onValueChange = { capacityText = it }, label = { Text("Öğrenci Kontenjanı") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = roomNumber, onValueChange = { roomNumber = it }, label = { Text("Derslik / Oda No") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name, gradeLevel, teacherName, capacityText.toIntOrNull() ?: 20, roomNumber)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sınıfı Oluştur", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditClassDialog(
    classGroup: ClassGroup,
    onDismiss: () -> Unit,
    onConfirm: (ClassGroup) -> Unit
) {
    var name by remember { mutableStateOf(classGroup.name) }
    var gradeLevel by remember { mutableStateOf(classGroup.grade_level) }
    var teacherName by remember { mutableStateOf(classGroup.teacher_name) }
    var capacityText by remember { mutableStateOf(classGroup.capacity.toString()) }
    var roomNumber by remember { mutableStateOf(classGroup.room_number) }
    var isActive by remember { mutableStateOf(classGroup.is_active) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sınıfı Düzenle (Kapasite Korumalı)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Sınıf Adı") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = gradeLevel, onValueChange = { gradeLevel = it }, label = { Text("Kademe") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("Birincil Sınıf Öğretmeni (Boş bırakılırsa öğretmen ataması kaldırılır)") },
                    trailingIcon = {
                        if (teacherName.isNotBlank()) {
                            IconButton(onClick = { teacherName = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Öğretmeni Kaldır")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it },
                    label = { Text("Kapasite (Mevcut öğrenci sayısının altına düşürülemez)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = roomNumber, onValueChange = { roomNumber = it }, label = { Text("Oda No") }, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sınıf Aktif Durumu (is_active)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        onConfirm(
                            classGroup.copy(
                                name = name,
                                grade_level = gradeLevel,
                                teacher_id = teacherName.trim().takeIf { it.isNotBlank() }?.let { "usr-teacher" },
                                teacher_name = teacherName.trim().ifBlank { "Atanmadı" },
                                capacity = capacityText.toIntOrNull() ?: classGroup.capacity,
                                room_number = roomNumber,
                                is_active = isActive
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Güncelle", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignStudentDialog(
    students: List<Student>,
    currentClassId: String,
    onDismiss: () -> Unit,
    onConfirm: (studentId: String) -> Unit
) {
    val unassignedOrOther = students.filter { it.class_id != currentClassId }
    var selectedStudentId by remember { mutableStateOf(unassignedOrOther.firstOrNull()?.id ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedStudent = students.find { it.id == selectedStudentId }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sınıfa Öğrenci Ata", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }

                if (unassignedOrOther.isEmpty()) {
                    Text("Atanabilecek başka öğrenci bulunmamaktadır.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedStudent?.full_name ?: "Öğrenci Seçin",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Atanacak Öğrenci") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            unassignedOrOther.forEach { std ->
                                DropdownMenuItem(
                                    text = { Text("${std.full_name} (${std.class_name})") },
                                    onClick = {
                                        selectedStudentId = std.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (selectedStudentId.isNotBlank()) {
                                onConfirm(selectedStudentId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Öğrenciyi Sınıfa Ekle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentEmail: String,
    currentPhone: String,
    onDismiss: () -> Unit,
    onConfirm: (fullName: String, email: String, phone: String) -> Unit
) {
    var fullName by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    var phone by remember { mutableStateOf(currentPhone) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Profili Düzenle", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }

                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Ad Soyad") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (fullName.isNotBlank()) {
                            onConfirm(fullName, email, phone)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Kaydet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, phone: String, institution: String, branch: String) -> Unit
) {
    var fullName by remember { mutableStateOf(user.full_name) }
    var email by remember { mutableStateOf(user.email) }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var instName by remember { mutableStateOf(user.institution_name) }
    var branchName by remember { mutableStateOf(user.branch_name) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kullanıcı Profilini Düzenle", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }

                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Ad Soyad") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefon Numarası") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = instName, onValueChange = { instName = it }, label = { Text("Kurum Adı") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = branchName, onValueChange = { branchName = it }, label = { Text("Şube Adı") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (fullName.isNotBlank()) {
                            onConfirm(fullName, email, phone, instName, branchName)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Değişiklikleri Kaydet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPaymentSelectionBottomSheet(
    invoices: List<Invoice>,
    onDismiss: () -> Unit,
    onSelectInvoice: (Invoice) -> Unit,
    onNavigateToFinance: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val pendingInvoices = remember(invoices, searchQuery) {
        invoices.filter {
            val isUnpaid = !it.status.equals("PAID", ignoreCase = true) && !it.status.equals("ODENDI", ignoreCase = true) && (it.amount - it.paid_amount > 0)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                it.student_name.contains(searchQuery, ignoreCase = true) ||
                it.class_name.contains(searchQuery, ignoreCase = true) ||
                it.invoice_number.contains(searchQuery, ignoreCase = true)
            }
            isUnpaid && matchesSearch
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hızlı Tahsilat / Ödeme Al",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = "Tahsilat yapılacak öğrenci veya faturayı seçin",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Öğrenci Adı, Sınıf veya Fatura No") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (pendingInvoices.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SecondaryEmerald,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bekleyen Ödeme Bulunamadı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Tüm aidat ve faturalar tahsil edilmiş veya arama kriterine uygun kayıt yok.",
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                onNavigateToFinance()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Finans Sayfasına Git")
                        }
                    }
                }
            } else {
                Text(
                    text = "Tahsilat Bekleyen Faturalar (${pendingInvoices.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = pendingInvoices, key = { it.id }) { inv ->
                        val remaining = inv.amount - inv.paid_amount
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectInvoice(inv)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (inv.status.equals("OVERDUE", ignoreCase = true))
                                                    Color(0xFFFEE2E2)
                                                else
                                                    Color(0xFFFEF3C7)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (inv.status.equals("OVERDUE", ignoreCase = true))
                                                Icons.Default.Warning
                                            else
                                                Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = if (inv.status.equals("OVERDUE", ignoreCase = true))
                                                AccentRose
                                            else
                                                Color(0xFFD97706),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = inv.student_name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${inv.class_name} • ${inv.description}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₺${remaining.toInt()}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = SecondaryEmerald
                                    )
                                    Text(
                                        text = "Ödeme Al →",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}



