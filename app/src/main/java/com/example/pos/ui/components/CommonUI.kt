package com.example.pos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.ui.theme.*
import java.util.Calendar

@Composable
fun posTextFieldColors(
    focusedTextColor: Color = TextPrimary,
    unfocusedTextColor: Color = TextPrimary,
    focusedContainerColor: Color = SurfaceDarkVariant,
    unfocusedContainerColor: Color = SurfaceDark,
    disabledContainerColor: Color = SurfaceDarkVariant,
    errorContainerColor: Color = SurfaceDark,
    focusedBorderColor: Color = EmeraldBright,
    unfocusedBorderColor: Color = SurfaceBorder,
    disabledBorderColor: Color = SurfaceBorder.copy(alpha = 0.5f),
    errorBorderColor: Color = ErrorRed,
    cursorColor: Color = EmeraldBright,
    errorCursorColor: Color = ErrorRed,
    focusedLabelColor: Color = EmeraldBright,
    unfocusedLabelColor: Color = TextSecondary,
    disabledLabelColor: Color = TextMuted,
    errorLabelColor: Color = ErrorRed,
    focusedPlaceholderColor: Color = TextMuted,
    unfocusedPlaceholderColor: Color = TextMuted,
    disabledPlaceholderColor: Color = TextMuted,
    errorPlaceholderColor: Color = TextMuted,
    focusedLeadingIconColor: Color = EmeraldBright,
    unfocusedLeadingIconColor: Color = TextSecondary,
    disabledLeadingIconColor: Color = TextMuted,
    errorLeadingIconColor: Color = ErrorRed,
    focusedTrailingIconColor: Color = TextSecondary,
    unfocusedTrailingIconColor: Color = TextSecondary,
    disabledTrailingIconColor: Color = TextMuted,
    errorTrailingIconColor: Color = ErrorRed,
    focusedPrefixColor: Color = TextSecondary,
    unfocusedPrefixColor: Color = TextSecondary,
    disabledPrefixColor: Color = TextMuted,
    errorPrefixColor: Color = TextSecondary,
    selectionColors: TextSelectionColors = TextSelectionColors(
        handleColor = EmeraldBright,
        backgroundColor = EmeraldBright.copy(alpha = 0.35f)
    )
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = focusedTextColor,
    unfocusedTextColor = unfocusedTextColor,
    disabledTextColor = TextMuted,
    errorTextColor = TextPrimary,
    focusedContainerColor = focusedContainerColor,
    unfocusedContainerColor = unfocusedContainerColor,
    disabledContainerColor = disabledContainerColor,
    errorContainerColor = errorContainerColor,
    cursorColor = cursorColor,
    errorCursorColor = errorCursorColor,
    selectionColors = selectionColors,
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    disabledBorderColor = disabledBorderColor,
    errorBorderColor = errorBorderColor,
    focusedLabelColor = focusedLabelColor,
    unfocusedLabelColor = unfocusedLabelColor,
    disabledLabelColor = disabledLabelColor,
    errorLabelColor = errorLabelColor,
    focusedPlaceholderColor = focusedPlaceholderColor,
    unfocusedPlaceholderColor = unfocusedPlaceholderColor,
    disabledPlaceholderColor = disabledPlaceholderColor,
    errorPlaceholderColor = errorPlaceholderColor,
    focusedLeadingIconColor = focusedLeadingIconColor,
    unfocusedLeadingIconColor = unfocusedLeadingIconColor,
    disabledLeadingIconColor = disabledLeadingIconColor,
    errorLeadingIconColor = errorLeadingIconColor,
    focusedTrailingIconColor = focusedTrailingIconColor,
    unfocusedTrailingIconColor = unfocusedTrailingIconColor,
    disabledTrailingIconColor = disabledTrailingIconColor,
    errorTrailingIconColor = errorTrailingIconColor,
    focusedPrefixColor = focusedPrefixColor,
    unfocusedPrefixColor = unfocusedPrefixColor,
    disabledPrefixColor = disabledPrefixColor,
    errorPrefixColor = errorPrefixColor
)

@Composable
fun PrimaryGreenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = EmeraldPrimary.copy(alpha = 0.5f)
            ),
        border = if (!enabled) androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldBright,
            contentColor = TextOnPrimary,
            disabledContainerColor = EmeraldBright.copy(alpha = 0.15f),
            disabledContentColor = TextMuted
        ),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (enabled) TextOnPrimary else TextMuted
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (enabled) TextOnPrimary else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun SecondaryDarkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = SurfaceDarkVariant,
    contentColor: Color = TextPrimary,
    borderColor: Color = SurfaceBorder,
    iconTint: Color = EmeraldPrimary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(26.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = iconTint
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceDark,
    borderColor: Color = SurfaceBorder,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBgColor: Color = EmeraldContainer,
    iconTint: Color = EmeraldBright,
    modifier: Modifier = Modifier,
    subValue: String? = null,
    isPositive: Boolean = true
) {
    SurfaceCard(
        modifier = modifier,
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )
        )

        if (subValue != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isPositive) EmeraldPrimary else ErrorRed,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    backgroundColor: Color = SurfaceDarkVariant,
    borderColor: Color = SurfaceBorder,
    selectedBackgroundColor: Color = EmeraldBright,
    selectedTextColor: Color = BgDark,
    unselectedTextColor: Color = TextSecondary
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) selectedBackgroundColor else backgroundColor
            )
            .border(
                width = 1.dp,
                color = if (isSelected) selectedBackgroundColor else borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (isSelected) selectedTextColor else unselectedTextColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
fun StockBadge(isLowStock: Boolean, stockQty: Int) {
    val bgColor = if (isLowStock) WarningContainer else EmeraldContainer
    val textColor = if (isLowStock) WarningYellow else EmeraldBright
    val textLabel = if (isLowStock) "Low stock" else "In stock"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(0.5.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$textLabel: $stockQty",
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun RetailSyncHeader(
    shopName: String,
    ownerName: String,
    profilePhotoUri: String? = null,
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    hasUnreadNotifications: Boolean = false,
    unreadCount: Int = if (hasUnreadNotifications) 1 else 0
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }
    val effectiveUnreadCount = if (unreadCount > 0) unreadCount else if (hasUnreadNotifications) 1 else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onProfileClick() }
        ) {
            ProfileAvatar(
                photoUri = profilePhotoUri,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = EmeraldBright,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ownerName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Shop Switcher Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDarkVariant)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                    .clickable { }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shopName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Shop",
                        tint = EmeraldBright,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Notification Bell with Floating Count Badge
            Box(
                modifier = Modifier.padding(end = 6.dp, top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Bell Icon Circular Container with dedicated ripple
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceDarkVariant)
                        .border(1.dp, SurfaceBorder.copy(alpha = 0.8f), CircleShape)
                        .clickable(onClick = onNotificationClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (effectiveUnreadCount > 0) Icons.Default.Notifications else Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = if (effectiveUnreadCount > 0) EmeraldBright else TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (effectiveUnreadCount > 0) {
                    val countDisplay = if (effectiveUnreadCount > 99) "99+" else effectiveUnreadCount.toString()
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 5.dp, y = (-4).dp)
                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                            .background(EmeraldBright, shape = RoundedCornerShape(9.dp))
                            .border(1.5.dp, BgDark, shape = RoundedCornerShape(9.dp))
                            .clickable(onClick = onNotificationClick)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countDisplay,
                            color = TextOnPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        NavigationItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavigationItem("Sales", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
        NavigationItem("Stock", Icons.Filled.Inventory, Icons.Outlined.Inventory2),
        NavigationItem("Reports", Icons.Filled.BarChart, Icons.Outlined.BarChart),
        NavigationItem("More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
    )

    Surface(
        color = SurfaceDark,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = SurfaceBorder, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedTab == index

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) EmeraldContainer else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = if (isSelected) EmeraldBright else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) EmeraldBright else TextMuted,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

private data class NavigationItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
