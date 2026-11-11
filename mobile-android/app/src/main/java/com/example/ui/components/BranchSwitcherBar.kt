package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Branch
import com.example.ui.theme.*

/**
 * Multi-Branch Upper Switcher Component (Çoklu Şube & Franchise Hızlı Geçiş Menüsü)
 * Allows corporate executives and administrators with multiple branches to seamlessly switch
 * active branch context with immediate feedback, branch health stats, and consolidated options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchSwitcherBar(
    branches: List<Branch>,
    selectedBranchId: String,
    onBranchSelected: (String) -> Unit,
    onOpenConsolidatedReport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showBranchSheet by remember { mutableStateOf(false) }

    val currentBranch = branches.find { it.id == selectedBranchId } ?: branches.firstOrNull()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showBranchSheet = true
            },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Branch Icon with gradient
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryBlue, Color(0xFF0284C7))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = "Şube",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentBranch?.name ?: "Tüm Şubeler",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            color = BrandNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SecondaryEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "AKTİF ŞUBE",
                                color = SecondaryEmerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${currentBranch?.student_count ?: 0} Kayıtlı Öğrenci • ${currentBranch?.class_count ?: 0} Sınıf / Şube",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Switch Action Affordance
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PrimaryBlueContainer.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Şube Değiştir",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Branch Switcher Modal Bottom Sheet
    if (showBranchSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBranchSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🏢 Kurum Şubeleri & Franchise Yönetimi",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = "Geçiş yapmak istediğiniz aktif yerleşkeyi seçin",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showBranchSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(branches) { branch ->
                        val isSelected = branch.id == selectedBranchId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onBranchSelected(branch.id)
                                    showBranchSheet = false
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) PrimaryBlueContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) BorderStroke(2.dp, PrimaryBlue) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) PrimaryBlue else Color(0xFF64748B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = branch.code,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = branch.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = BrandNavy
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("${branch.student_count} Öğrenci", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("•", color = MaterialTheme.colorScheme.outline)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("${branch.class_count} Sınıf", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Text(
                                            text = branch.address,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = PrimaryBlue,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, contentDescription = "Seçili", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (onOpenConsolidatedReport != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            showBranchSheet = false
                            onOpenConsolidatedReport()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                        border = BorderStroke(1.5.dp, PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📊 Tüm Şubeler Konsolide Karşılaştırma Raporu", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}
