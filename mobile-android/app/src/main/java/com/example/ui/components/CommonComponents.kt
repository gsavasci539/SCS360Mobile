package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Branch
import com.example.data.model.UserRole
import com.example.ui.theme.*

@Composable
fun TopBrandHeader(
    institutionName: String,
    branches: List<Branch>,
    selectedBranchId: String,
    selectedRole: UserRole,
    onBranchSelected: (String) -> Unit,
    onRoleSelected: (UserRole) -> Unit,
    onSettingsClicked: () -> Unit,
    onNotificationsClicked: (() -> Unit)? = null,
    onSearchClicked: (() -> Unit)? = null,
    onChatbotClicked: (() -> Unit)? = null,
    unreadNotificationsCount: Int = 3,
    modifier: Modifier = Modifier
) {
    var branchMenuExpanded by remember { mutableStateOf(false) }
    var roleMenuExpanded by remember { mutableStateOf(false) }

    val currentBranch = branches.find { it.id == selectedBranchId } ?: branches.firstOrNull()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_scs360_mark),
                                contentDescription = "SCS360",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { branchMenuExpanded = true }
                                    .padding(vertical = 2.dp, horizontal = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "SCS",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF163B3B)
                                        )
                                    )
                                    Text(
                                        text = "360",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = Color(0xFF176B5B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PrimaryBlueContainer)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "SaaS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OnPrimaryBlueContainer
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apartment,
                                        contentDescription = "Şube",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${currentBranch?.name ?: institutionName} (${currentBranch?.student_count ?: 0})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Şube Seç",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = branchMenuExpanded,
                                onDismissRequest = { branchMenuExpanded = false },
                                modifier = Modifier.width(300.dp)
                            ) {
                                Text(
                                    text = "Aktif Şube Seçimi ($institutionName)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                HorizontalDivider()
                                branches.forEach { branch ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = branch.name,
                                                    fontWeight = if (branch.id == selectedBranchId) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "${branch.code} • ${branch.student_count} Öğrenci • ${branch.class_count} Sınıf",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (branch.id == selectedBranchId) Icons.Default.CheckCircle else Icons.Outlined.Apartment,
                                                contentDescription = null,
                                                tint = if (branch.id == selectedBranchId) SecondaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = {
                                            onBranchSelected(branch.id)
                                            branchMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // AI Chatbot Button (Compact 32dp circle without M3 48dp background expansion)
                        if (onChatbotClicked != null) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF4338CA), Color(0xFF7C3AED))))
                                    .clickable { onChatbotClicked() }
                                    .testTag("top_header_chatbot_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Chatbot",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Global Search Button
                        if (onSearchClicked != null) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                    .clickable { onSearchClicked() }
                                    .testTag("top_header_search_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Genel Arama",
                                    tint = BrandNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Notification Bell Button
                        if (onNotificationsClicked != null) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                    .clickable { onNotificationsClicked() },
                                contentAlignment = Alignment.Center
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotificationsCount > 0) {
                                            Badge(
                                                containerColor = AccentRose,
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    text = if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString(),
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Bildirim Merkezi",
                                        tint = BrandNavy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Quick role badge button
                        RoleSwitchBadge(
                            selectedRole = selectedRole,
                            onRoleSelected = onRoleSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    if (isLoading) {
        MetricStatCardSkeleton(modifier = modifier)
        return
    }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(6.dp)),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SectionTitle(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RoleSwitchBadge(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .widthIn(max = 130.dp)
            .clip(RoundedCornerShape(20.dp))
            .testTag("role_switch_badge_button"),
        color = Color(selectedRole.badgeColorHex).copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedRole.badgeColorHex).copy(alpha = 0.35f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(selectedRole.badgeColorHex))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = selectedRole.displayName.split("/").first().trim(),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(selectedRole.badgeColorHex),
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



