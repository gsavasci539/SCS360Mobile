package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Announcement
import com.example.data.model.AttendanceStatus
import com.example.data.model.Invoice
import com.example.data.model.Student
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
/**
 * Interactive, Summary-Based Dashboard Widgets Section with Drag & Reordering Support
 * Provides customizable operational insights and inline micro-actions for:
 * 1. Today's Attendance Rate (Bugünkü Yoklama Oranı & Sınıf/Devamsız Detayı)
 * 2. Uncollected Fees (Finansal Özet & Gecikmiş Tahsilat Aksiyonu)
 * 3. Upcoming Announcements (Yaklaşan Duyurular & Pano İletişimi)
 * 4. Daily Cash & Pending Approvals (Günlük Kasa Özeti & Bekleyen İzin/İlaç/Randevu Onayları)
 * 5. Critical Risk Students & Campus/Bus Live Status (Kritik Öğrenciler & Servis Canlı Takip)
 */

enum class DashboardWidgetType(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    ATTENDANCE("attendance", "Bugün Beklenen Yoklama", "Sınıf katılım oranları ve hızlı yoklama", Icons.Default.FactCheck),
    FINANCE("finance", "Finansal Özet & Aidatlar", "Tahsilat oranı, bekleyen/gecikmiş bakiye", Icons.Default.Payments),
    ANNOUNCEMENTS("announcements", "Yaklaşan Duyurular", "Son okul ve sınıf duyuruları", Icons.Default.Campaign),
    CASH_APPROVALS("cash_approvals", "Günlük Kasa & Onaylar", "Kasa durumu, veli izin ve onaylar", Icons.Default.AccountBalanceWallet),
    CAMPUS_RISK("campus_risk", "Kritik Risk & Servis GPS", "Riskli öğrenciler ve servis canlı takibi", Icons.Default.HealthAndSafety);

    companion object {
        fun fromId(id: String): DashboardWidgetType? = values().firstOrNull { it.id == id }
        fun defaultOrder(): List<String> = listOf(
            ATTENDANCE.id,
            FINANCE.id,
            ANNOUNCEMENTS.id,
            CASH_APPROVALS.id,
            CAMPUS_RISK.id
        )
    }
}

@Composable
fun InteractiveDashboardWidgetsSection(
    state: Okul360UiState,
    onOpenQuickAttendance: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onOpenQuickPayment: () -> Unit,
    onSelectInvoiceForPayment: (Invoice) -> Unit,
    onSendPaymentReminder: (String, String) -> Unit,
    onNavigateToFinance: () -> Unit,
    onApproveLeaveNotice: (String) -> Unit,
    onMarkMedicineGiven: (String) -> Unit,
    onApproveConsent: (String, String) -> Unit,
    onNavigateToConsents: () -> Unit,
    onNavigateToTransport: () -> Unit,
    onNavigateToAiAnalytics: () -> Unit,
    onSelectStudent: (Student) -> Unit,
    onUpdateAttendanceStatus: (String, String) -> Unit,
    onNavigateToAnnouncements: () -> Unit = {},
    isSkeletonLoading: Boolean = false,
    isBackgroundRefreshing: Boolean = false,
    onRefreshClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val gridGap = DesignTokens.spacing.widgetGridGap
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("scs360_dashboard_prefs", Context.MODE_PRIVATE)
    }

    // Load persisted layout preference from local storage (survives restarts)
    val initialOrder = remember {
        val saved = prefs.getString("dashboard_widget_order", null)
        if (!saved.isNullOrBlank()) {
            val valid = saved.split(",")
                .map { it.trim() }
                .filter { DashboardWidgetType.fromId(it) != null }
                .distinct()
            val missing = DashboardWidgetType.defaultOrder().filter { !valid.contains(it) }
            valid + missing
        } else {
            DashboardWidgetType.defaultOrder()
        }
    }

    val initialHidden = remember {
        prefs.getStringSet("dashboard_hidden_widgets", null)?.toSet() ?: emptySet()
    }

    // Persistent Reordering & Customization State
    var widgetOrder by remember {
        mutableStateOf(initialOrder)
    }
    var hiddenWidgets by remember {
        mutableStateOf(initialHidden)
    }
    var isReorderingMode by rememberSaveable {
        mutableStateOf(false)
    }
    var showReorderDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // Institutional Brand Color Palette Customization State (Persisted in SharedPreferences)
    val initialPaletteId = remember {
        prefs.getString("dashboard_brand_palette_id", "navy") ?: "navy"
    }
    val initialCustomHex = remember {
        prefs.getString("dashboard_brand_custom_hex", null)
    }
    var brandPalette by remember {
        mutableStateOf(DashboardBrandPalette.getPaletteById(initialPaletteId, initialCustomHex))
    }
    var showColorPickerDialog by rememberSaveable {
        mutableStateOf(false)
    }

    fun persistPalette(palette: DashboardBrandPalette) {
        brandPalette = palette
        val hexToSave = if (palette.id.startsWith("custom")) {
            String.format("#%06X", 0xFFFFFF and palette.primary.toArgb())
        } else null
        prefs.edit()
            .putString("dashboard_brand_palette_id", palette.id)
            .putString("dashboard_brand_custom_hex", hexToSave)
            .apply()
    }

    // Color Picker Dialog for Institution Branding
    if (showColorPickerDialog) {
        DashboardBrandColorPickerDialog(
            currentPalette = brandPalette,
            onPaletteSelected = { newPalette ->
                persistPalette(newPalette)
            },
            onDismiss = { showColorPickerDialog = false }
        )
    }

    // Helper function to persist changes immediately to local storage
    fun persistLayout(order: List<String>, hidden: Set<String>) {
        prefs.edit()
            .putString("dashboard_widget_order", order.joinToString(","))
            .putStringSet("dashboard_hidden_widgets", hidden)
            .apply()
    }

    // Helper functions for reordering
    fun moveWidgetUp(id: String) {
        val currentList = widgetOrder.toMutableList()
        val index = currentList.indexOf(id)
        if (index > 0) {
            val item = currentList.removeAt(index)
            currentList.add(index - 1, item)
            widgetOrder = currentList
            persistLayout(currentList, hiddenWidgets)
        }
    }

    fun moveWidgetDown(id: String) {
        val currentList = widgetOrder.toMutableList()
        val index = currentList.indexOf(id)
        if (index in 0 until currentList.size - 1) {
            val item = currentList.removeAt(index)
            currentList.add(index + 1, item)
            widgetOrder = currentList
            persistLayout(currentList, hiddenWidgets)
        }
    }

    fun toggleWidgetVisibility(id: String) {
        val newHidden = if (hiddenWidgets.contains(id)) {
            hiddenWidgets - id
        } else {
            // Prevent hiding all widgets
            if (hiddenWidgets.size < DashboardWidgetType.values().size - 1) {
                hiddenWidgets + id
            } else hiddenWidgets
        }
        hiddenWidgets = newHidden
        persistLayout(widgetOrder, newHidden)
    }

    fun resetToDefaultOrder() {
        val defaultList = DashboardWidgetType.defaultOrder()
        val defaultHidden = emptySet<String>()
        widgetOrder = defaultList
        hiddenWidgets = defaultHidden
        persistLayout(defaultList, defaultHidden)
    }

    // Dialog for Widget Reordering & Visibility Management
    if (showReorderDialog) {
        AlertDialog(
            onDismissRequest = { showReorderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Widget Düzeni & Sıralama",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Kartların gösterim sırasını yukarı/aşağı butonlarıyla değiştirin veya göz simgesine dokunarak gizleyin.",
                        fontSize = 12.sp,
                        color = NeutralTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    widgetOrder.forEachIndexed { index, id ->
                        val type = DashboardWidgetType.fromId(id) ?: return@forEachIndexed
                        val isHidden = hiddenWidgets.contains(id)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isHidden) SurfaceContainerHigh.copy(alpha = 0.5f) else SurfaceContainerLow,
                            border = BorderStroke(1.dp, if (isHidden) NeutralLightBorder.copy(alpha = 0.5f) else NeutralLightBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Sürükle",
                                        tint = NeutralTextSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrimaryBlue.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = type.icon,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = type.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isHidden) NeutralTextSecondary else NeutralTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${index + 1}. Sıra" + if (isHidden) " (Gizli)" else "",
                                            fontSize = 11.sp,
                                            color = if (isHidden) AccentRose else NeutralTextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { moveWidgetUp(id) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Yukarı Taşı",
                                            tint = if (index > 0) PrimaryBlue else NeutralTextSecondary.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { moveWidgetDown(id) },
                                        enabled = index < widgetOrder.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Aşağı Taşı",
                                            tint = if (index < widgetOrder.size - 1) PrimaryBlue else NeutralTextSecondary.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { toggleWidgetVisibility(id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isHidden) "Göster" else "Gizle",
                                            tint = if (isHidden) AccentRose else SecondaryEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showReorderDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = brandPalette.primary)
                ) {
                    Text("Tamamla", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            showReorderDialog = false
                            showColorPickerDialog = true
                        },
                        modifier = Modifier.testTag("dialog_open_brand_color_btn")
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(15.dp), tint = brandPalette.primary)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Renk Paleti", color = brandPalette.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { resetToDefaultOrder() }
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Sıfırla")
                    }
                }
            }
        )
    }

    CompositionLocalProvider(LocalDashboardBrandPalette provides brandPalette) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag("interactive_dashboard_widgets_section"),
            verticalArrangement = Arrangement.spacedBy(gridGap)
        ) {
            // Section Header with Live Pulse Badge, Color Palette Picker & Reorder Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isBackgroundRefreshing || isSkeletonLoading) AccentAmber else brandPalette.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kurum Özeti & Canlı Metrikler",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NeutralTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Brand Color Palette Picker Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = brandPalette.containerLight,
                        border = BorderStroke(1.dp, brandPalette.containerBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showColorPickerDialog = true }
                            .testTag("dashboard_header_color_picker_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(brandPalette.gradient)
                                    .border(0.8.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Renk",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = brandPalette.primary,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // "Düzenle" / "Bitti" Mode Toggle Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isReorderingMode) brandPalette.primary else SurfaceContainer,
                        border = BorderStroke(1.dp, if (isReorderingMode) brandPalette.primary else NeutralLightBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { isReorderingMode = !isReorderingMode }
                            .testTag("dashboard_toggle_edit_mode_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isReorderingMode) Icons.Default.Check else Icons.Default.Tune,
                                contentDescription = if (isReorderingMode) "Düzenlemeyi Bitir" else "Widgetları Düzenle",
                                tint = if (isReorderingMode) Color.White else brandPalette.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isReorderingMode) "Bitti" else "Düzenle",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isReorderingMode) Color.White else brandPalette.primary,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    if (onRefreshClicked != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceContainer,
                            border = BorderStroke(1.dp, NeutralLightBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onRefreshClicked() }
                                .testTag("dashboard_skeleton_refresh_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Verileri Yenile",
                                    tint = NeutralTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSkeletonLoading || isBackgroundRefreshing) "Yükleniyor..." else "Yenile",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeutralTextSecondary,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Active "Düzenle" Mode Banner with Instructions & Actions
            AnimatedVisibility(
                visible = isReorderingMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = brandPalette.containerLight,
                    border = BorderStroke(1.dp, brandPalette.containerBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_reordering_active_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = brandPalette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Düzenleme Modu Aktif",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brandPalette.primary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Kartları taşıyabilir veya 'Renk' ile kurum renginizi özelleştirebilirsiniz.",
                                    fontSize = 10.sp,
                                    color = NeutralTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Brand Color Picker button in edit banner
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = brandPalette.primary.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showColorPickerDialog = true }
                                    .testTag("dashboard_reorder_color_picker_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = brandPalette.primary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Renk", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = brandPalette.primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = brandPalette.primary.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showReorderDialog = true }
                                    .testTag("dashboard_reorder_list_modal_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = brandPalette.primary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Liste", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = brandPalette.primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = brandPalette.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { isReorderingMode = false }
                                    .testTag("dashboard_finish_editing_btn")
                            ) {
                                Text(
                                    text = "Bitti",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

        // Smooth Crossfade between Shimmer Skeleton Grid and Hydrated Interactive Widgets (Zero UI Flicker)
        androidx.compose.animation.Crossfade(
            targetState = isSkeletonLoading,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "DashboardSkeletonCrossfade"
        ) { showSkeleton ->
            if (showSkeleton) {
                DashboardWidgetsSkeletonGrid()
            } else {
                val visibleWidgetTypes = widgetOrder
                    .mapNotNull { DashboardWidgetType.fromId(it) }
                    .filter { !hiddenWidgets.contains(it.id) }

                if (visibleWidgetTypes.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainer,
                        border = BorderStroke(1.dp, NeutralLightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = NeutralTextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tüm widget kartları gizlendi",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeutralTextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { resetToDefaultOrder() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("Varsayılan Sıralamaya Dön", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Responsive Layout: 2x2 grid on wide screens (>= 600.dp), vertical stack on compact screens
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isWide = maxWidth >= 600.dp

                        if (isWide) {
                            val chunkedRows = visibleWidgetTypes.chunked(2)
                            Column(verticalArrangement = Arrangement.spacedBy(gridGap)) {
                                chunkedRows.forEach { rowTypes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(gridGap),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        rowTypes.forEach { widgetType ->
                                            val indexInVisible = visibleWidgetTypes.indexOf(widgetType)
                                            Box(modifier = Modifier.weight(1f)) {
                                                ReorderableWidgetWrapper(
                                                    widgetType = widgetType,
                                                    index = indexInVisible,
                                                    totalCount = visibleWidgetTypes.size,
                                                    isReorderingMode = isReorderingMode,
                                                    onMoveUp = { moveWidgetUp(widgetType.id) },
                                                    onMoveDown = { moveWidgetDown(widgetType.id) },
                                                    onHide = { toggleWidgetVisibility(widgetType.id) }
                                                ) {
                                                    RenderSingleDashboardWidget(
                                                        type = widgetType,
                                                        state = state,
                                                        onOpenQuickAttendance = onOpenQuickAttendance,
                                                        onNavigateToAttendance = onNavigateToAttendance,
                                                        onOpenQuickPayment = onOpenQuickPayment,
                                                        onSelectInvoiceForPayment = onSelectInvoiceForPayment,
                                                        onSendPaymentReminder = onSendPaymentReminder,
                                                        onNavigateToFinance = onNavigateToFinance,
                                                        onApproveLeaveNotice = onApproveLeaveNotice,
                                                        onMarkMedicineGiven = onMarkMedicineGiven,
                                                        onApproveConsent = onApproveConsent,
                                                        onNavigateToConsents = onNavigateToConsents,
                                                        onNavigateToTransport = onNavigateToTransport,
                                                        onNavigateToAiAnalytics = onNavigateToAiAnalytics,
                                                        onSelectStudent = onSelectStudent,
                                                        onUpdateAttendanceStatus = onUpdateAttendanceStatus,
                                                        onNavigateToAnnouncements = onNavigateToAnnouncements,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                        if (rowTypes.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(gridGap)) {
                                visibleWidgetTypes.forEachIndexed { index, widgetType ->
                                    ReorderableWidgetWrapper(
                                        widgetType = widgetType,
                                        index = index,
                                        totalCount = visibleWidgetTypes.size,
                                        isReorderingMode = isReorderingMode,
                                        onMoveUp = { moveWidgetUp(widgetType.id) },
                                        onMoveDown = { moveWidgetDown(widgetType.id) },
                                        onHide = { toggleWidgetVisibility(widgetType.id) }
                                    ) {
                                        RenderSingleDashboardWidget(
                                            type = widgetType,
                                            state = state,
                                            onOpenQuickAttendance = onOpenQuickAttendance,
                                            onNavigateToAttendance = onNavigateToAttendance,
                                            onOpenQuickPayment = onOpenQuickPayment,
                                            onSelectInvoiceForPayment = onSelectInvoiceForPayment,
                                            onSendPaymentReminder = onSendPaymentReminder,
                                            onNavigateToFinance = onNavigateToFinance,
                                            onApproveLeaveNotice = onApproveLeaveNotice,
                                            onMarkMedicineGiven = onMarkMedicineGiven,
                                            onApproveConsent = onApproveConsent,
                                            onNavigateToConsents = onNavigateToConsents,
                                            onNavigateToTransport = onNavigateToTransport,
                                            onNavigateToAiAnalytics = onNavigateToAiAnalytics,
                                            onSelectStudent = onSelectStudent,
                                            onUpdateAttendanceStatus = onUpdateAttendanceStatus,
                                            onNavigateToAnnouncements = onNavigateToAnnouncements,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ReorderableWidgetWrapper(
    widgetType: DashboardWidgetType,
    index: Int,
    totalCount: Int,
    isReorderingMode: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onHide: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!isReorderingMode) {
        content()
    } else {
        var dragOffsetY by remember { mutableStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        val animatedElevation by animateFloatAsState(
            targetValue = if (isDragging) 8f else 1f,
            label = "widgetDragElevation"
        )
        val animatedScale by animateFloatAsState(
            targetValue = if (isDragging) 1.02f else 1f,
            label = "widgetDragScale"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(if (isDragging) 10f else 0f)
                .graphicsLayer {
                    translationY = dragOffsetY
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .shadow(
                    elevation = animatedElevation.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (isDragging) 2.dp else 1.5.dp,
                    color = if (isDragging) PrimaryBlue else PrimaryBlue.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(SurfaceContainerLow)
                .testTag("reorderable_widget_${widgetType.id}")
        ) {
            // Reorder Bar Header with Drag Handle & Actions
            Surface(
                color = if (isDragging) PrimaryBlue.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Sürükle-bırak tutamacı ve başlık (dokunup sürükleme alanı)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(index, totalCount) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        dragOffsetY = 0f
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount
                                        if (dragOffsetY > 65f && index < totalCount - 1) {
                                            onMoveDown()
                                            dragOffsetY = 0f
                                        } else if (dragOffsetY < -65f && index > 0) {
                                            onMoveUp()
                                            dragOffsetY = 0f
                                        }
                                    }
                                )
                            }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDragging) PrimaryBlue else PrimaryBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = "Sürükleyip Sıralayın",
                                    tint = if (isDragging) Color.White else PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryBlue
                        ) {
                            Text(
                                text = "#${index + 1}",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = widgetType.title,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isDragging) "Sürükleniyor... (Bırakınca yerleşir)" else "Sürükle veya oklarla taşı",
                                fontSize = 9.5.sp,
                                color = if (isDragging) AccentAmber else NeutralTextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // Move Up, Move Down, Hide micro-actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = index > 0,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("widget_move_up_${widgetType.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Yukarı Taşı",
                                tint = if (index > 0) PrimaryBlue else NeutralTextSecondary.copy(alpha = 0.25f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = index < totalCount - 1,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("widget_move_down_${widgetType.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Aşağı Taşı",
                                tint = if (index < totalCount - 1) PrimaryBlue else NeutralTextSecondary.copy(alpha = 0.25f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onHide,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("widget_hide_${widgetType.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Gizle",
                                tint = AccentRose,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun RenderSingleDashboardWidget(
    type: DashboardWidgetType,
    state: Okul360UiState,
    onOpenQuickAttendance: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onOpenQuickPayment: () -> Unit,
    onSelectInvoiceForPayment: (Invoice) -> Unit,
    onSendPaymentReminder: (String, String) -> Unit,
    onNavigateToFinance: () -> Unit,
    onApproveLeaveNotice: (String) -> Unit,
    onMarkMedicineGiven: (String) -> Unit,
    onApproveConsent: (String, String) -> Unit,
    onNavigateToConsents: () -> Unit,
    onNavigateToTransport: () -> Unit,
    onNavigateToAiAnalytics: () -> Unit,
    onSelectStudent: (Student) -> Unit,
    onUpdateAttendanceStatus: (String, String) -> Unit,
    onNavigateToAnnouncements: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (type) {
        DashboardWidgetType.ATTENDANCE -> TodayAttendanceInteractiveWidget(
            state = state,
            onOpenQuickAttendance = onOpenQuickAttendance,
            onNavigateToAttendance = onNavigateToAttendance,
            onUpdateAttendanceStatus = onUpdateAttendanceStatus,
            modifier = modifier
        )
        DashboardWidgetType.FINANCE -> UncollectedFeesInteractiveWidget(
            state = state,
            onOpenQuickPayment = onOpenQuickPayment,
            onSelectInvoiceForPayment = onSelectInvoiceForPayment,
            onSendPaymentReminder = onSendPaymentReminder,
            onNavigateToFinance = onNavigateToFinance,
            modifier = modifier
        )
        DashboardWidgetType.ANNOUNCEMENTS -> AnnouncementsSummaryWidget(
            state = state,
            onNavigateToAnnouncements = onNavigateToAnnouncements,
            modifier = modifier
        )
        DashboardWidgetType.CASH_APPROVALS -> DailyCashAndApprovalsInteractiveWidget(
            state = state,
            onApproveLeaveNotice = onApproveLeaveNotice,
            onMarkMedicineGiven = onMarkMedicineGiven,
            onApproveConsent = onApproveConsent,
            onNavigateToConsents = onNavigateToConsents,
            onNavigateToFinance = onNavigateToFinance,
            modifier = modifier
        )
        DashboardWidgetType.CAMPUS_RISK -> CriticalRiskAndCampusPulseWidget(
            state = state,
            onNavigateToTransport = onNavigateToTransport,
            onNavigateToAiAnalytics = onNavigateToAiAnalytics,
            onSelectStudent = onSelectStudent,
            modifier = modifier
        )
    }
}

/**
 * WIDGET 1: Today's Attendance Rate & Status Distribution ('Bugün Beklenen Yoklama')
 * Enriched with an interactive pie/donut chart visualizing:
 * - Katıldı (Present) - Emerald
 * - Gelmedi (Absent) - Rose
 * - Geç Kaldı (Late) - Amber
 * - İzinli (Excused) - Blue
 */
@Composable
fun TodayAttendanceInteractiveWidget(
    state: Okul360UiState,
    onOpenQuickAttendance: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onUpdateAttendanceStatus: (String, String) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        TodayAttendanceWidgetSkeleton(modifier = modifier)
        return
    }
    var expanded by remember { mutableStateOf(true) }
    var selectedSliceId by remember { mutableStateOf<String?>(null) } // "PRESENT", "ABSENT", "LATE", "EXCUSED"
    val brand = LocalDashboardBrandPalette.current

    val records = state.attendanceRecords
    val totalStudents = state.students.size.coerceAtLeast(1)
    val presentCount = if (records.isNotEmpty()) {
        records.count { it.status.equals("PRESENT", ignoreCase = true) }
    } else {
        state.dashboardSummary?.present_today_count ?: (totalStudents - 3).coerceAtLeast(1)
    }
    val absentCount = if (records.isNotEmpty()) {
        records.count { it.status.equals("ABSENT", ignoreCase = true) }
    } else {
        state.dashboardSummary?.absent_today_count ?: 2
    }
    val lateCount = if (records.isNotEmpty()) {
        records.count { it.status.equals("LATE", ignoreCase = true) }
    } else {
        state.dashboardSummary?.late_today_count ?: 1
    }
    val excusedCount = if (records.isNotEmpty()) {
        records.count { it.status.equals("EXCUSED", ignoreCase = true) }
    } else {
        0
    }

    val totalRecorded = (presentCount + absentCount + lateCount + excusedCount).coerceAtLeast(1)
    val attendanceRateFloat = ((presentCount + lateCount * 0.5f) / totalRecorded.toFloat()).coerceIn(0f, 1f)
    val attendancePercent = (attendanceRateFloat * 100).toInt()

    val absentRecords = remember(records) {
        records.filter { it.status.equals("ABSENT", ignoreCase = true) }
    }
    val lateRecords = remember(records) {
        records.filter { it.status.equals("LATE", ignoreCase = true) }
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .testTag("widget_today_attendance"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (expanded) brand.primary.copy(alpha = 0.45f) else Color(0xFFE2E8F0)),
        shadowElevation = if (expanded) 3.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Icon, Title & Expand/Collapse Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brand.containerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Bugün Beklenen Yoklama",
                            tint = brand.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Bugün Beklenen Yoklama",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = brand.containerLight
                            ) {
                                Text(
                                    text = "Pasta Grafik",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brand.primary,
                                    softWrap = false,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Text(
                            text = "$totalRecorded Kayıt • %$attendancePercent Katılım • Dilimlere dokunarak filtreleyin",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Daralt" else "Genişlet",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // INTERACTIVE ATTENDANCE STATUS PIE / DONUT CHART
            // =========================================================================
            TodayAttendancePieChart(
                presentCount = presentCount,
                absentCount = absentCount,
                lateCount = lateCount,
                excusedCount = excusedCount,
                selectedSliceId = selectedSliceId,
                onSliceSelected = { slice ->
                    selectedSliceId = slice?.id
                }
            )

            // Expandable Detail Section: Filtered lists & class breakdowns
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Contextual Feedback Card based on selected slice
                    when (selectedSliceId) {
                        "ABSENT" -> {
                            // Gelmedi (Absent) Focus View
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Gelmeyen Öğrenciler ($absentCount)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF991B1B)
                                            )
                                        }
                                        Text(
                                            text = "Filtreyi Kaldır ✕",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626),
                                            modifier = Modifier.clickable { selectedSliceId = null }
                                        )
                                    }

                                    if (absentRecords.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        absentRecords.take(3).forEach { rec ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = rec.student_name,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = "Veli SMS Gönderildi • Mazeretsiz",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFB91C1C)
                                                    )
                                                }
                                                FilledTonalButton(
                                                    onClick = { onUpdateAttendanceStatus(rec.student_id, "PRESENT") },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = Color(0xFFDCFCE7),
                                                        contentColor = Color(0xFF15803D)
                                                    ),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Geldi Yap", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "LATE" -> {
                            // Geç Kaldı (Late) Focus View
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Geç Kalan Öğrenciler ($lateCount)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF92400E)
                                            )
                                        }
                                        Text(
                                            text = "Filtreyi Kaldır ✕",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706),
                                            modifier = Modifier.clickable { selectedSliceId = null }
                                        )
                                    }

                                    if (lateRecords.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        lateRecords.take(3).forEach { rec ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = rec.student_name,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        text = "Giriş Saati: ${rec.check_in_time ?: "09:15"}",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFB45309)
                                                    )
                                                }
                                                FilledTonalButton(
                                                    onClick = { onUpdateAttendanceStatus(rec.student_id, "PRESENT") },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = Color(0xFFDCFCE7),
                                                        contentColor = Color(0xFF15803D)
                                                    ),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Onayla", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // Default / Katıldı (Present) View: Class-level progress bars
                            Text(
                                text = "Sınıf Bazlı Katılım Oranları",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            state.classes.take(3).forEach { cls ->
                                val clsRecords = records.filter { it.class_id == cls.id }
                                val clsTotal = clsRecords.size.coerceAtLeast(cls.student_count.coerceAtLeast(1))
                                val clsPresent = if (clsRecords.isNotEmpty()) {
                                    clsRecords.count { it.status == "PRESENT" || it.status == "LATE" }
                                } else {
                                    (clsTotal * 0.94).toInt().coerceAtLeast(1)
                                }
                                val clsRate = (clsPresent.toFloat() / clsTotal.toFloat()).coerceIn(0f, 1f)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cls.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.width(68.dp)
                                    )
                                    LinearProgressIndicator(
                                        progress = { clsRate },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (clsRate >= 0.9f) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        trackColor = Color(0xFFE2E8F0)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "%${(clsRate * 100).toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.width(34.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons Row: Hızlı Yoklama Al & Yoklama Sayfası
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenQuickAttendance,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brand.primary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("widget_btn_quick_attendance")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hızlı Yoklama Al", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onNavigateToAttendance,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text(
                                text = "Yoklama Sayfası",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceMiniStatChip(
    label: String,
    count: Int,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = textColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

/**
 * WIDGET 2: Uncollected Fees & Overdue Collections ('Tahsil Edilmeyen Aidatlar')
 */
@Composable
fun UncollectedFeesInteractiveWidget(
    state: Okul360UiState,
    onOpenQuickPayment: () -> Unit,
    onSelectInvoiceForPayment: (Invoice) -> Unit,
    onSendPaymentReminder: (String, String) -> Unit,
    onNavigateToFinance: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        UncollectedFeesWidgetSkeleton(modifier = modifier)
        return
    }
    var expanded by remember { mutableStateOf(true) }
    var financeTab by remember { mutableIntStateOf(0) } // 0: Yıllık Trend (Recharts), 1: Bekleyen Faturalar
    val brand = LocalDashboardBrandPalette.current

    val unpaidInvoices = remember(state.invoices) {
        state.invoices.filter {
            !it.status.equals("PAID", ignoreCase = true) &&
                !it.status.equals("ODENDI", ignoreCase = true) &&
                (it.amount - it.paid_amount > 0)
        }
    }

    val overdueInvoices = remember(unpaidInvoices) {
        unpaidInvoices.filter { it.status.equals("OVERDUE", ignoreCase = true) }
    }

    val uncollectedTotal = remember(unpaidInvoices, state.financeSummary) {
        if (unpaidInvoices.isNotEmpty()) {
            unpaidInvoices.sumOf { (it.amount - it.paid_amount).coerceAtLeast(0.0) }
        } else {
            (state.financeSummary?.pending_balance ?: 25000.0) + (state.financeSummary?.overdue_balance ?: 15000.0)
        }
    }

    val collectedTotal = state.financeSummary?.total_collected_monthly
        ?: state.reportsData?.total_collected
        ?: 185000.0

    val totalTarget = (collectedTotal + uncollectedTotal).coerceAtLeast(1.0)
    val collectionRateFloat = (collectedTotal / totalTarget).toFloat().coerceIn(0f, 1f)
    val collectionPercent = (collectionRateFloat * 100).toInt()

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .testTag("widget_uncollected_fees"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (expanded) Color(0xFFD97706).copy(alpha = 0.45f) else Color(0xFFE2E8F0)),
        shadowElevation = if (expanded) 3.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            // Widget Header: Title, Overdue Badge & Total Outstanding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Finansal Özet ve Gelir Trendi",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Finansal Özet & Gelir Trendi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (overdueInvoices.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = "${overdueInvoices.size} Gecikmiş",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB91C1C),
                                        softWrap = false,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${unpaidInvoices.size} Bekleyen Fatura • %$collectionPercent Yıllık Tahsilat",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₺${String.format("%,.0f", uncollectedTotal)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (overdueInvoices.isNotEmpty()) Color(0xFFDC2626) else Color(0xFFB45309),
                            softWrap = false,
                            maxLines = 1
                        )
                        Text(
                            text = "Açık Bakiye",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B),
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Daralt" else "Genişlet",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Collection Progress Bar & Summary Strip
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tahsil Edilen: ₺${String.format("%,.0f", collectedTotal)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = "Tahsilat Oranı: %$collectionPercent",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { collectionRateFloat },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryBlue,
                    trackColor = Color(0xFFFDE68A)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Segmented Sub-Tab Switcher: [📈 Yıllık Trend (Recharts)] vs [📋 Bekleyen Faturalar]
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (financeTab == 0) Color.White else Color.Transparent,
                        shadowElevation = if (financeTab == 0) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                financeTab = 0
                                expanded = true
                            }
                            .testTag("financial_trend_tab_recharts")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (financeTab == 0) PrimaryBlue else Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Yıllık Trend (Recharts)",
                                fontSize = 11.sp,
                                fontWeight = if (financeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (financeTab == 0) PrimaryBlue else Color(0xFF64748B)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (financeTab == 1) Color.White else Color.Transparent,
                        shadowElevation = if (financeTab == 1) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                financeTab = 1
                                expanded = true
                            }
                            .testTag("financial_trend_tab_invoices")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = if (financeTab == 1) PrimaryBlue else Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Bekleyen Faturalar (${unpaidInvoices.size})",
                                fontSize = 11.sp,
                                fontWeight = if (financeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (financeTab == 1) PrimaryBlue else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // Expandable Content: Either Recharts Chart or Priority Invoices
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (financeTab == 0) {
                        // 1. RECHARTS-INSPIRED YEARLY INCOME-EXPENSE DUAL LINE & AREA CHART
                        YearlyFinancialTrendRechartsView(
                            financeSummary = state.financeSummary,
                            onNavigateToFinance = onNavigateToFinance
                        )
                    } else {
                        // 2. PRIORITY INVOICES LIST & ACTIONS
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Öncelikli Tahsilat Bekleyen Kayıtlar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val priorityInvoices = (overdueInvoices + unpaidInvoices.filter { !it.status.equals("OVERDUE", true) }).take(3)
                        if (priorityInvoices.isEmpty()) {
                            Text(
                                text = "Tüm faturalar zamanında tahsil edilmiş.",
                                fontSize = 12.sp,
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            priorityInvoices.forEach { invoice ->
                                val isOverdue = invoice.status.equals("OVERDUE", ignoreCase = true)
                                val remaining = (invoice.amount - invoice.paid_amount).coerceAtLeast(0.0)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isOverdue) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isOverdue) Color(0xFFFECACA) else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = invoice.student_name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${invoice.class_name})",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            Text(
                                                text = "₺${String.format("%,.0f", remaining)} • Son Gün: ${invoice.due_date}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isOverdue) Color(0xFFB91C1C) else Color(0xFF475569)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { onSendPaymentReminder(invoice.id, invoice.student_name) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = "Hatırlatma Gönder",
                                                    tint = Color(0xFFD97706),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            FilledTonalButton(
                                                onClick = { onSelectInvoiceForPayment(invoice) },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = PrimaryBlue,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Tahsil Et", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenQuickPayment,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brand.primary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("widget_btn_quick_payment")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hızlı Ödeme Al", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onNavigateToFinance,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text(
                                text = "Finans & Kasa",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * WIDGET 3: Daily Cash Summary & Pending Approvals ('Günlük Kasa Özeti ve Bekleyen Onaylar')
 */
@Composable
fun DailyCashAndApprovalsInteractiveWidget(
    state: Okul360UiState,
    onApproveLeaveNotice: (String) -> Unit,
    onMarkMedicineGiven: (String) -> Unit,
    onApproveConsent: (String, String) -> Unit,
    onNavigateToConsents: () -> Unit,
    onNavigateToFinance: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        DailyCashAndApprovalsWidgetSkeleton(modifier = modifier)
        return
    }
    var expanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bekleyen Onaylar, 1: Günlük Kasa
    val brand = LocalDashboardBrandPalette.current

    val pendingLeaves = remember(state.leaveNotices) {
        state.leaveNotices.filter { it.status.equals("PENDING", ignoreCase = true) }
    }
    val pendingMedicines = remember(state.medicineNotices) {
        state.medicineNotices.filter { it.status.equals("PENDING", ignoreCase = true) }
    }
    val pendingConsents = remember(state.consents) {
        state.consents.filter { !it.is_approved }
    }
    val pendingAppointments = remember(state.appointments) {
        state.appointments.filter { it.status.equals("PENDING", ignoreCase = true) }
    }

    val totalPendingApprovals = pendingLeaves.size + pendingMedicines.size + pendingConsents.size + pendingAppointments.size

    val monthlyCollected = state.financeSummary?.total_collected_monthly ?: 185000.0
    val monthlyExpenses = state.financeSummary?.total_expenses_monthly
        ?: state.expenses.sumOf { it.amount }.takeIf { it > 0 }
        ?: 62000.0
    val netBalance = monthlyCollected - monthlyExpenses

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
            .testTag("widget_cash_and_approvals"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (expanded) brand.primary.copy(alpha = 0.45f) else Color(0xFFE2E8F0)),
        shadowElevation = if (expanded) 3.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brand.containerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Kasa ve Bekleyen Onaylar",
                            tint = brand.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kasa & Bekleyen Onaylar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (totalPendingApprovals > 0) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF3E8FF)
                                ) {
                                    Text(
                                        text = "$totalPendingApprovals Bekleyen",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6D28D9),
                                        softWrap = false,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${pendingLeaves.size} İzin • ${pendingMedicines.size} İlaç • ${pendingConsents.size} Belge Onayı",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+₺${String.format("%,.0f", netBalance / 1000)}B",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF059669),
                            softWrap = false,
                            maxLines = 1
                        )
                        Text(
                            text = "Net Kasa",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B),
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Daralt" else "Genişlet",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Mini Tab Selector inside card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedTab == 0) Color.White else Color.Transparent,
                    shadowElevation = if (selectedTab == 0) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedTab = 0
                            expanded = true
                        }
                ) {
                    Text(
                        text = "Bekleyen Onaylar ($totalPendingApprovals)",
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) Color(0xFF6D28D9) else Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedTab == 1) Color.White else Color.Transparent,
                    shadowElevation = if (selectedTab == 1) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedTab = 1
                            expanded = true
                        }
                ) {
                    Text(
                        text = "Kasa & Nakit Akışı",
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) PrimaryBlue else Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedTab == 0) {
                        if (totalPendingApprovals == 0) {
                            Text(
                                text = "Bekleyen izin, ilaç veya belge onayı bulunmuyor.",
                                fontSize = 12.sp,
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            // Pending Medicine Notices
                            pendingMedicines.take(2).forEach { med ->
                                ApprovalQuickRow(
                                    badge = "İlaç Bildirimi",
                                    badgeBg = Color(0xFFFEF3C7),
                                    badgeColor = Color(0xFFB45309),
                                    title = "${med.student_name} • ${med.medicine_name}",
                                    subtitle = "Saat: ${med.time_to_give} • Doz: ${med.dose}",
                                    actionLabel = "Verildi İşaretle",
                                    onAction = { onMarkMedicineGiven(med.id) }
                                )
                            }

                            // Pending Leave Notices
                            pendingLeaves.take(2).forEach { leave ->
                                ApprovalQuickRow(
                                    badge = "İzin Talebi",
                                    badgeBg = Color(0xFFE0E7FF),
                                    badgeColor = Color(0xFF3730A3),
                                    title = "${leave.student_name} (${leave.start_date})",
                                    subtitle = leave.parent_note.ifBlank { "Veli mazeret izni bildirimi" },
                                    actionLabel = "Onayla",
                                    onAction = { onApproveLeaveNotice(leave.id) }
                                )
                            }

                            // Pending Consents
                            pendingConsents.take(2).forEach { consent ->
                                ApprovalQuickRow(
                                    badge = "Muvafakatname",
                                    badgeBg = Color(0xFFF3E8FF),
                                    badgeColor = Color(0xFF6D28D9),
                                    title = "${consent.student_name} • ${consent.title}",
                                    subtitle = "Son Tarih: ${consent.expiry_date}",
                                    actionLabel = "Onayla",
                                    onAction = { onApproveConsent(consent.id, "Yönetici Onayı") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onNavigateToConsents,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            Text("Tüm İzin & Muvafakatnameleri Yönet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        // Daily / Monthly Cash Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFECFDF5),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Toplam Tahsilat", fontSize = 10.5.sp, color = Color(0xFF047857))
                                    Text(
                                        text = "₺${String.format("%,.0f", monthlyCollected)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Giderler", fontSize = 10.5.sp, color = Color(0xFFB91C1C))
                                    Text(
                                        text = "₺${String.format("%,.0f", monthlyExpenses)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Kantin POS", fontSize = 10.5.sp, color = PrimaryBlue)
                                    Text(
                                        text = "₺${String.format("%,.0f", state.canteenWallet.today_spent.coerceAtLeast(1450.0))}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onNavigateToFinance,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            Text("Detaylı Kasa Raporuna Git", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApprovalQuickRow(
    badge: String,
    badgeBg: Color,
    badgeColor: Color,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(4.dp), color = badgeBg) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFDCFCE7),
                    contentColor = Color(0xFF15803D)
                ),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(actionLabel, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * WIDGET 4: Critical Risk Students & Live Campus/Bus Status ('Kritik Riskli Öğrenciler ve Servis / Kampüs Canlı Durum')
 */
@Composable
fun CriticalRiskAndCampusPulseWidget(
    state: Okul360UiState,
    onNavigateToTransport: () -> Unit,
    onNavigateToAiAnalytics: () -> Unit,
    onSelectStudent: (Student) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        CriticalRiskAndCampusPulseWidgetSkeleton(modifier = modifier)
        return
    }
    var expanded by remember { mutableStateOf(false) }

    val activeBuses = remember(state.busRoutes) {
        state.busRoutes.filter {
            it.current_status == "MORNING_ROUTE" || it.current_status == "EVENING_ROUTE"
        }
    }
    val riskItems = state.churnRiskList
    val onlineDevicesCount = state.hardwareDevices.count { it.status.equals("ONLINE", ignoreCase = true) }

    val brand = LocalDashboardBrandPalette.current

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
            .testTag("widget_campus_pulse_and_risk"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (expanded) brand.primary.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
        shadowElevation = if (expanded) 3.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = "Kampüs ve Risk Durumu",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Riskli Öğrenciler & Servis",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE0F2FE)
                            ) {
                                Text(
                                    text = "${activeBuses.size.coerceAtLeast(state.busRoutes.size)} Servis",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1),
                                    softWrap = false,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Text(
                            text = "${riskItems.size.coerceAtLeast(2)} Kritik Takip • ${onlineDevicesCount.coerceAtLeast(3)} RFID Turnike Aktif",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Daralt" else "Genişlet",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Summary Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF1F2),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${riskItems.size.coerceAtLeast(2)} Öğrenci",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFBE123C)
                            )
                            Text(
                                text = "Yakın Takip / Risk",
                                fontSize = 10.sp,
                                color = Color(0xFF9F1239)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            val firstBus = state.busRoutes.firstOrNull()
                            Text(
                                text = firstBus?.plate_number ?: "34 OKL 360",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "Varış: ~${firstBus?.eta_minutes_to_next_stop ?: 4} dk",
                                fontSize = 10.sp,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Kritik Devamsızlık / Ödeme Riski Taşıyan Öğrenciler",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (riskItems.isNotEmpty()) {
                        riskItems.take(2).forEach { risk ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFF1F2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        val std = state.students.find { it.id == risk.student_id || it.full_name == risk.student_name }
                                        if (std != null) onSelectStudent(std) else onNavigateToAiAnalytics()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${risk.student_name} (${risk.class_name})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF881337)
                                        )
                                        Text(
                                            text = risk.primary_reason,
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF9F1239),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE11D48)
                                    ) {
                                        Text(
                                            text = "Risk: %${risk.risk_score}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        state.students.take(2).forEach { std ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { onSelectStudent(std) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${std.full_name} (${std.class_name})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Veli: ${std.parent_name} • ${std.parent_phone}",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToTransport,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(Icons.Default.DirectionsBus, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Canlı Servis GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onNavigateToAiAnalytics,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text(
                                text = "AI Risk Analizi",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * WIDGET 3: Upcoming & Recent Announcements ('Yaklaşan Duyurular & İletişim')
 * Enhanced with smooth enter/exit animations for newly added announcements and announcements marked as read.
 */
@Composable
fun AnnouncementsSummaryWidget(
    state: Okul360UiState,
    onNavigateToAnnouncements: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalDashboardBrandPalette.current
    // Persistent tracking of read announcement IDs
    var readAnnouncementIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Local list for dynamically simulated incoming announcements
    var simulatedAnnouncements by rememberSaveable { mutableStateOf(listOf<Announcement>()) }
    // Filter mode: "UNREAD" (default) or "ALL"
    var activeFilter by rememberSaveable { mutableStateOf("UNREAD") }
    // Last read announcement for undo banner
    var lastReadAnnouncement by remember { mutableStateOf<Announcement?>(null) }

    val baseAnnouncements = remember(state.announcements, state.dashboardSummary) {
        val list = state.announcements.ifEmpty {
            state.dashboardSummary?.recent_announcements ?: emptyList()
        }
        list.sortedWith(compareByDescending<Announcement> { it.is_pinned }.thenByDescending { it.created_at })
    }

    // Combine simulated new announcements at the top with base announcements
    val allAnnouncements = remember(simulatedAnnouncements, baseAnnouncements) {
        (simulatedAnnouncements + baseAnnouncements).distinctBy { it.id }
    }

    val unreadCount = remember(allAnnouncements, readAnnouncementIds) {
        allAnnouncements.count { it.id !in readAnnouncementIds }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("announcements_summary_widget"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, NeutralLightBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(380, easing = FastOutSlowInEasing))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Widget Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brand.gradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Duyurular",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Yaklaşan Duyurular",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NeutralTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Animated unread count badge
                            AnimatedContent(
                                targetState = unreadCount,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInVertically(animationSpec = tween(350)) { -it } + fadeIn(animationSpec = tween(350)))
                                            .togetherWith(slideOutVertically(animationSpec = tween(350)) { it } + fadeOut(animationSpec = tween(350)))
                                    } else {
                                        (slideInVertically(animationSpec = tween(350)) { it } + fadeIn(animationSpec = tween(350)))
                                            .togetherWith(slideOutVertically(animationSpec = tween(350)) { -it } + fadeOut(animationSpec = tween(350)))
                                    }
                                },
                                label = "unread_announcements_counter"
                            ) { count ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (count > 0) brand.containerLight else SurfaceContainerHigh
                                ) {
                                    Text(
                                        text = "$count okunmamış",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (count > 0) brand.primary else NeutralTextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Son yayınlanan okul ve sınıf bilgilendirmeleri",
                            fontSize = 11.sp,
                            color = NeutralTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Action: Simulate new incoming announcement to see smooth entrance animation
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = brand.containerLight,
                        border = BorderStroke(1.dp, brand.containerBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val samples = listOf(
                                    Pair(
                                        "Meteoroloji Uyarısı & Servis Saatleri",
                                        "Yarın beklenen yoğun yağış sebebiyle okul servisleri 15 dakika erken hareket edecektir."
                                    ),
                                    Pair(
                                        "TÜBİTAK Bilim Şenliği Proje Kayıtları",
                                        "2026 Bahar dönemi robotik ve fen projeleri için kayıtlar başlamıştır."
                                    ),
                                    Pair(
                                        "Güz Dönemi Veli-Öğretmen Toplantısı",
                                        "Cumartesi günü saat 10:30'da okul konferans salonunda veli buluşması düzenlenecektir."
                                    ),
                                    Pair(
                                        "Okul Kütüphanesi Yeni Yayınlar",
                                        "Kütüphanemize 120 adet yeni referans ve kaynak kitap eklenmiştir."
                                    )
                                )
                                val sample = samples[simulatedAnnouncements.size % samples.size]
                                val newAnn = Announcement(
                                    id = "sim-ann-${System.currentTimeMillis()}",
                                    title = sample.first,
                                    content = sample.second,
                                    category = if (simulatedAnnouncements.size % 2 == 0) "EVENT" else "EMERGENCY",
                                    target_audience = "ALL",
                                    author_name = "Okul Yönetimi",
                                    author_role = "Yönetici",
                                    created_at = "Az önce",
                                    is_pinned = true
                                )
                                simulatedAnnouncements = listOf(newAnn) + simulatedAnnouncements
                            }
                            .testTag("simulate_new_announcement_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAlert,
                                contentDescription = "Yeni Duyuru Simüle Et",
                                tint = brand.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "+ Yeni",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = brand.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    TextButton(
                        onClick = onNavigateToAnnouncements,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("announcements_widget_view_all_btn")
                    ) {
                        Text(
                            text = "Pano",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = brand.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Tüm Duyurular",
                            tint = brand.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = NeutralLightBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

            // Filter Selector: Okunmamış vs Tümü
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeFilter == "UNREAD") brand.primary else SurfaceContainerHigh,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { activeFilter = "UNREAD" }
                        .testTag("announcements_filter_unread")
                ) {
                    Text(
                        text = "Okunmamışlar ($unreadCount)",
                        fontSize = 11.sp,
                        fontWeight = if (activeFilter == "UNREAD") FontWeight.Bold else FontWeight.Medium,
                        color = if (activeFilter == "UNREAD") Color.White else NeutralTextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeFilter == "ALL") brand.primary else SurfaceContainerHigh,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { activeFilter = "ALL" }
                        .testTag("announcements_filter_all")
                ) {
                    Text(
                        text = "Tüm Duyurular (${allAnnouncements.size})",
                        fontSize = 11.sp,
                        fontWeight = if (activeFilter == "ALL") FontWeight.Bold else FontWeight.Medium,
                        color = if (activeFilter == "ALL") Color.White else NeutralTextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (allAnnouncements.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceContainer,
                    border = BorderStroke(1.dp, NeutralLightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Campaign,
                            contentDescription = null,
                            tint = NeutralTextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Henüz yayınlanmış bir duyuru bulunmuyor",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeutralTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = onNavigateToAnnouncements,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Duyuru Panosuna Git", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Announcements List with Smooth Entrance and Exit Animations
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(380, easing = FastOutSlowInEasing)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All caught up empty state for UNREAD view
                    AnimatedVisibility(
                        visible = activeFilter == "UNREAD" && unreadCount == 0,
                        enter = fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                                expandVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(250)) +
                                shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tüm duyurular okundu",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                    Text(
                                        text = "Bekleyen yeni bir duyuru bulunmuyor. Geçmiş duyuruları görüntüleyebilirsiniz.",
                                        fontSize = 10.sp,
                                        color = NeutralTextSecondary
                                    )
                                }
                                TextButton(
                                    onClick = { activeFilter = "ALL" },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Tümünü Gör", fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Render announcement cards with smooth enter/exit transitions
                    allAnnouncements.take(5).forEach { announcement ->
                        val isRead = announcement.id in readAnnouncementIds
                        val isVisible = if (activeFilter == "UNREAD") !isRead else true

                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(animationSpec = tween(380, easing = FastOutSlowInEasing)) +
                                    expandVertically(animationSpec = tween(420, easing = FastOutSlowInEasing)) +
                                    slideInVertically(animationSpec = tween(380, easing = FastOutSlowInEasing)) { -it / 2 },
                            exit = fadeOut(animationSpec = tween(300)) +
                                    shrinkVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                                    slideOutVertically(animationSpec = tween(350, easing = FastOutSlowInEasing)) { -it / 2 }
                        ) {
                            AnnouncementItemCard(
                                announcement = announcement,
                                isRead = isRead,
                                isNewlySimulated = announcement.id.startsWith("sim-ann-"),
                                onToggleRead = {
                                    if (isRead) {
                                        readAnnouncementIds = readAnnouncementIds - announcement.id
                                    } else {
                                        readAnnouncementIds = readAnnouncementIds + announcement.id
                                        lastReadAnnouncement = announcement
                                    }
                                },
                                onClick = onNavigateToAnnouncements
                            )
                        }
                    }

                    // Undo snackbar / feedback banner when an announcement is marked as read
                    AnimatedVisibility(
                        visible = lastReadAnnouncement != null,
                        enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(350)),
                        exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(280))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Duyuru okundu olarak işaretlendi",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        lastReadAnnouncement?.let { ann ->
                                            readAnnouncementIds = readAnnouncementIds - ann.id
                                        }
                                        lastReadAnnouncement = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Geri Al",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick stats / footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pinnedCount = allAnnouncements.count { it.is_pinned }
                    Text(
                        text = if (pinnedCount > 0) "$pinnedCount sabitlenmiş • $unreadCount okunmamış" else "$unreadCount okunmamış duyuru",
                        fontSize = 11.sp,
                        color = NeutralTextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onNavigateToAnnouncements() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = NeutralTextSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Duyuru Panosunu Aç",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeutralTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementItemCard(
    announcement: Announcement,
    isRead: Boolean,
    isNewlySimulated: Boolean = false,
    onToggleRead: () -> Unit,
    onClick: () -> Unit
) {
    val categoryColor = when (announcement.category.uppercase()) {
        "EMERGENCY", "ACİL" -> Color(0xFFEF4444)
        "ACADEMIC", "AKADEMİK" -> Color(0xFF3B82F6)
        "EVENT", "ETKİNLİK" -> Color(0xFF8B5CF6)
        "FINANCIAL", "FİNANSAL" -> Color(0xFF10B981)
        else -> Color(0xFF64748B)
    }

    val categoryLabel = when (announcement.category.uppercase()) {
        "EMERGENCY", "ACİL" -> "Acil"
        "ACADEMIC", "AKADEMİK" -> "Akademik"
        "EVENT", "ETKİNLİK" -> "Etkinlik"
        "FINANCIAL", "FİNANSAL" -> "Finans"
        else -> "Genel"
    }

    val audienceLabel = when (announcement.target_audience.uppercase()) {
        "PARENTS", "VELİ" -> "Veliler"
        "TEACHERS", "ÖĞRETMEN" -> "Öğretmenler"
        "STUDENTS", "ÖĞRENCİ" -> "Öğrenciler"
        "BRANCH", "ŞUBE" -> "Şube"
        else -> "Tüm Okul"
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isNewlySimulated -> Color(0xFF8B5CF6).copy(alpha = 0.65f)
            announcement.is_pinned -> Color(0xFFF59E0B).copy(alpha = 0.45f)
            isRead -> NeutralLightBorder.copy(alpha = 0.5f)
            else -> NeutralLightBorder
        },
        animationSpec = tween(300),
        label = "card_border_anim"
    )

    val animatedCardBackground by animateColorAsState(
        targetValue = when {
            isNewlySimulated -> Color(0xFF8B5CF6).copy(alpha = 0.05f)
            isRead -> SurfaceContainerLow.copy(alpha = 0.6f)
            else -> SurfaceContainerLow
        },
        animationSpec = tween(300),
        label = "card_bg_anim"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = animatedCardBackground,
        border = BorderStroke(1.dp, animatedBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("announcement_item_${announcement.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header Row: Badges, Category, Pin, and Mark as Read action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isNewlySimulated) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF8B5CF6)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "YENİ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (announcement.is_pinned) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Sabitlendi",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Sabit",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = categoryLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NeutralLightBorder
                    ) {
                        Text(
                            text = audienceLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeutralTextSecondary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Interactive "Okundu Olarak İşaretle" / "Geri Al" Micro-Action
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isRead) SurfaceContainerHigh else Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isRead) NeutralLightBorder else Color(0xFF10B981).copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleRead() }
                        .testTag("announcement_mark_read_btn_${announcement.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isRead) Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = if (isRead) "Okundu" else "Okundu Olarak İşaretle",
                            tint = if (isRead) Color(0xFF059669) else Color(0xFF059669),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isRead) "Okundu" else "Okundu",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRead) NeutralTextSecondary else Color(0xFF059669)
                        )
                    }
                }
            }

            // Title & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = announcement.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRead) NeutralTextSecondary else NeutralTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = announcement.created_at,
                    fontSize = 10.sp,
                    color = NeutralTextSecondary
                )
            }

            if (announcement.content.isNotBlank()) {
                Text(
                    text = announcement.content,
                    fontSize = 11.sp,
                    color = if (isRead) NeutralTextSecondary.copy(alpha = 0.7f) else NeutralTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }
        }
    }
}



/**
 * Quick Attendance Modal Bottom Sheet ('Hızlı Yoklama Al')
 * Allows teachers and admins to take class attendance from anywhere in 2 taps without leaving the current screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAttendanceBottomSheet(
    state: Okul360UiState,
    onDismiss: () -> Unit,
    onClassSelected: (String) -> Unit,
    onStatusChanged: (String, String) -> Unit,
    onMarkAllPresent: () -> Unit,
    onSaveAttendance: () -> Unit,
    onNavigateToFullAttendance: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filteredRecords = remember(state.attendanceRecords, state.attendanceClassId) {
        if (state.attendanceClassId == "all" || state.attendanceClassId.isBlank()) {
            state.attendanceRecords
        } else {
            state.attendanceRecords.filter { it.class_id == state.attendanceClassId }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .navigationBarsPadding()
                .testTag("quick_attendance_bottom_sheet")
        ) {
            // Sheet Header
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
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hızlı Sınıf Yoklaması Al",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Tarih: ${state.attendanceDate} • Anında durum güncelle ve kaydet",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                TextButton(onClick = {
                    onMarkAllPresent()
                }) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tümü Geldi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF059669)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Class Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.attendanceClassId == "all",
                    onClick = { onClassSelected("all") },
                    label = { Text("Tüm Sınıflar", fontSize = 12.sp) }
                )
                state.classes.forEach { cls ->
                    FilterChip(
                        selected = state.attendanceClassId == cls.id,
                        onClick = { onClassSelected(cls.id) },
                        label = { Text(cls.name, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Student Attendance List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredRecords, key = { it.student_id.ifBlank { it.id } }) { record ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = record.student_name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                val clsName = state.classes.find { it.id == record.class_id }?.name ?: "Şube"
                                Text(
                                    text = clsName,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AttendanceStatus.entries.forEach { statusOption ->
                                    val isSelected = record.status.equals(statusOption.name, ignoreCase = true)
                                    val chipColor = Color(statusOption.colorHex)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) chipColor else chipColor.copy(alpha = 0.1f),
                                        modifier = Modifier
                                            .size(width = 36.dp, height = 32.dp)
                                            .clickable {
                                                onStatusChanged(record.student_id, statusOption.name)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = statusOption.shortCode,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = if (isSelected) Color.White else chipColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save & Full Page Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onNavigateToFullAttendance()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text("Detaylı Ekran", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onSaveAttendance()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(46.dp)
                        .testTag("quick_attendance_save_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yoklamayı Kaydet", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
