package com.example.pos.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import com.example.pos.data.Employee
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.components.ProfileAvatar
import com.example.pos.data.Shop
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    themeMode: ThemeMode,
    colorTheme: AppColorTheme,
    customAccentColor: Color,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorThemeChange: (AppColorTheme) -> Unit,
    onCustomAccentColorChange: (Color) -> Unit,
    profilePhotoUri: String?,
    onProfilePhotoChange: (String?) -> Unit,
    onOpenEmployees: () -> Unit,
    onOpenCreditReminders: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenApprovals: () -> Unit = {}
) {
    val isOwner by viewModel.isOwner.collectAsState()
    val currentEmployee by viewModel.currentEmployee.collectAsState()
    val totalPendingCount by viewModel.totalPendingCount.collectAsState()
    val shop by viewModel.shop.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotificationsCount = notifications.count { !it.isRead }
    var showShopEditor by remember { mutableStateOf(false) }
    var showPhotoPreview by remember { mutableStateOf(false) }
    var showReminderSettings by remember { mutableStateOf(false) }
    var photoBeingEdited by remember { mutableStateOf<Bitmap?>(null) }
    var copyFeedback by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                photoBeingEdited = withContext(Dispatchers.IO) {
                    runCatching {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, bounds)
                        }
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = calculatePhotoSampleSize(bounds.outWidth, bounds.outHeight)
                        }
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, options)
                        }
                    }.getOrNull()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Settings", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Make RetailSync work for you", color = TextSecondary, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(EmeraldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldBright)
            }
        }

        ProfileHeaderCard(
            shop = shop,
            isOwner = isOwner,
            currentEmployee = currentEmployee,
            photoUri = profilePhotoUri,
            onChangePhoto = { photoPicker.launch(arrayOf("image/*")) },
            onPreviewPhoto = { showPhotoPreview = true },
            onRemovePhoto = { onProfilePhotoChange(null) }
        )

        if (isOwner) {
            Spacer(modifier = Modifier.height(14.dp))

            // Shop Code Sharing Card for Store Owner
            SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDark,
                borderColor = EmeraldBright.copy(alpha = 0.35f),
                cornerRadius = 20.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shop Code for Staff",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Share with employees so they can register their app",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldContainer)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(shop.id))
                                    copyFeedback = true
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = EmeraldBright,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (copyFeedback) "Copied!" else "Copy",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = EmeraldBright,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDarkVariant)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = shop.id,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = EmeraldBright,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SettingsSectionLabel("PREFERENCES")
        Spacer(modifier = Modifier.height(9.dp))

        AppearanceSettingsCard(
            selectedMode = themeMode,
            onModeSelected = onThemeModeChange,
            selectedColorTheme = colorTheme,
            customAccentColor = customAccentColor,
            onColorThemeSelected = onColorThemeChange,
            onCustomAccentColorSelected = onCustomAccentColorChange
        )

        Spacer(modifier = Modifier.height(22.dp))
        SettingsSectionLabel("SHOP & TEAM")
        Spacer(modifier = Modifier.height(9.dp))

        // Settings Menu Items
        if (isOwner) {
            SettingsMenuItem(
                label = "Approvals Center",
                icon = Icons.Default.VerifiedUser,
                badgeCount = totalPendingCount,
                onClick = onOpenApprovals
            )

            SettingsMenuItem(
                label = "Shop Information",
                icon = Icons.Default.Store,
                onClick = { showShopEditor = true }
            )

            SettingsMenuItem(
                label = "Employees & Permissions",
                icon = Icons.Default.People,
                onClick = onOpenEmployees
            )

            SettingsMenuItem(
                label = "Expenses Tracker",
                icon = Icons.Default.Receipt,
                onClick = onOpenExpenses
            )
        } else if (viewModel.canRecordExpense()) {
            SettingsMenuItem(
                label = "Expenses Tracker",
                icon = Icons.Default.Receipt,
                onClick = onOpenExpenses
            )
        }

        SettingsMenuItem(
            label = "Notifications & alerts",
            icon = Icons.Default.Notifications,
            badgeCount = unreadNotificationsCount,
            onClick = onOpenNotifications
        )

        if (isOwner || viewModel.canIssueCredit()) {
            SettingsMenuItem(
                label = "Credit Sales",
                icon = Icons.Default.Receipt,
                onClick = onOpenCreditReminders
            )
            SettingsMenuItem(
                label = "Credit Reminder Settings",
                icon = Icons.Default.Settings,
                onClick = { showReminderSettings = true }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        SettingsSectionLabel("SUPPORT")
        Spacer(modifier = Modifier.height(9.dp))

        SettingsMenuItem(
            label = "Help & Support",
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            onClick = { }
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Log Out Button
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ErrorContainer,
                contentColor = ErrorRed
            ),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed
                )
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showShopEditor) {
        ShopInformationDialog(
            shop = shop,
            onDismiss = { showShopEditor = false },
            onSave = { updatedShop ->
                viewModel.updateShopInformation(updatedShop)
                showShopEditor = false
            }
        )
    }
    photoBeingEdited?.let { bitmap ->
        CropProfilePhotoDialog(
            bitmap = bitmap,
            onDismiss = { photoBeingEdited = null },
            onSave = { croppedBitmap ->
                coroutineScope.launch {
                    val savedUri = withContext(Dispatchers.IO) {
                        runCatching {
                            val file = java.io.File(context.filesDir, "profile-photo-${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { output ->
                                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
                            }
                            profilePhotoUri?.let { oldUriString ->
                                runCatching {
                                    val oldFile = java.io.File(Uri.parse(oldUriString).path.orEmpty()).canonicalFile
                                    if (oldFile.parentFile == context.filesDir.canonicalFile && oldFile.name.startsWith("profile-photo-")) {
                                        oldFile.delete()
                                    }
                                }
                            }
                            Uri.fromFile(file).toString()
                        }.getOrNull()
                    }
                    if (savedUri != null) onProfilePhotoChange(savedUri)
                    photoBeingEdited = null
                }
            }
        )
    }
    if (showPhotoPreview && profilePhotoUri != null) {
        val savedBitmap by produceState<Bitmap?>(initialValue = null, profilePhotoUri) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(profilePhotoUri))?.use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
        }
        savedBitmap?.let { bitmap ->
            ZoomProfilePhotoDialog(bitmap = bitmap, onDismiss = { showPhotoPreview = false })
        }
    }
    if (showReminderSettings) {
        CreditReminderSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showReminderSettings = false }
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
private fun ProfileHeaderCard(
    shop: Shop,
    isOwner: Boolean = true,
    currentEmployee: Employee? = null,
    photoUri: String?,
    onChangePhoto: () -> Unit,
    onPreviewPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    val displayName = if (isOwner) shop.ownerName else currentEmployee?.name ?: shop.ownerName
    val displayEmail = if (isOwner) shop.email else currentEmployee?.email ?: shop.email
    val displayRole = if (isOwner) "Store owner" else "Staff (${currentEmployee?.role ?: "Cashier"})"

    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder,
        cornerRadius = 23.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(68.dp)) {
                ProfileAvatar(
                    photoUri = photoUri,
                    modifier = Modifier.size(62.dp),
                    onClick = if (photoUri != null) onPreviewPhoto else onChangePhoto
                ) {
                    Text(
                        displayName.firstOrNull()?.uppercase() ?: "?",
                        color = EmeraldPrimary,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(25.dp)
                        .clip(CircleShape)
                        .background(EmeraldBright)
                        .border(2.dp, SurfaceDark, CircleShape)
                        .clickable(onClick = onChangePhoto),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change profile photo", tint = TextOnPrimary, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(displayRole, color = EmeraldBright, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(displayEmail, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onChangePhoto, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (photoUri == null) "Add profile photo" else "Change photo", color = EmeraldBright, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (photoUri != null) {
                TextButton(onClick = onRemovePhoto, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                    Text("Remove", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun calculatePhotoSampleSize(width: Int, height: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sample = 1
    while (width / sample > 512 || height / sample > 512) sample *= 2
    return sample
}

@Composable
private fun CropProfilePhotoDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    var zoom by remember(bitmap) { mutableFloatStateOf(1f) }
    var pan by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var viewport by remember(bitmap) { mutableStateOf(IntSize.Zero) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(26.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Adjust profile photo", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Pinch to zoom and drag to position", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                InteractivePhotoCanvas(
                    bitmap = bitmap,
                    zoom = zoom,
                    pan = pan,
                    onZoomPanChange = { newZoom, newPan -> zoom = newZoom; pan = newPan },
                    onViewportChanged = { viewport = it },
                    showCircularGuide = true,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp))
                )
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ZoomIn, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Drag the image to choose what appears in the circle", color = TextMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            cropSquare(bitmap, viewport, zoom, pan)?.let(onSave)
                        },
                        enabled = viewport.width > 0 && viewport.height > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldBright, contentColor = TextOnPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Use photo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomProfilePhotoDialog(bitmap: Bitmap, onDismiss: () -> Unit) {
    var zoom by remember(bitmap) { mutableFloatStateOf(1f) }
    var pan by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var viewport by remember(bitmap) { mutableStateOf(IntSize.Zero) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Profile photo", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close photo preview", tint = TextSecondary)
                    }
                }
                InteractivePhotoCanvas(
                    bitmap = bitmap,
                    zoom = zoom,
                    pan = pan,
                    onZoomPanChange = { newZoom, newPan -> zoom = newZoom; pan = newPan },
                    onViewportChanged = { viewport = it },
                    showCircularGuide = false,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(17.dp))
                )
                Spacer(Modifier.height(12.dp))
                Text("Pinch to zoom · drag to explore", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun InteractivePhotoCanvas(
    bitmap: Bitmap,
    zoom: Float,
    pan: Offset,
    onZoomPanChange: (Float, Offset) -> Unit,
    onViewportChanged: (IntSize) -> Unit,
    showCircularGuide: Boolean,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(pan)
    val onTransform by rememberUpdatedState(onZoomPanChange)
    Canvas(
        modifier = modifier
            .background(Color.Black)
            .onSizeChanged(onViewportChanged)
            .pointerInput(bitmap) {
                detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                    val nextZoom = (currentZoom * gestureZoom).coerceIn(1f, 5f)
                    val base = maxOf(size.width / bitmap.width.toFloat(), size.height / bitmap.height.toFloat())
                    val drawWidth = bitmap.width * base * nextZoom
                    val drawHeight = bitmap.height * base * nextZoom
                    val maxX = ((drawWidth - size.width) / 2f).coerceAtLeast(0f)
                    val maxY = ((drawHeight - size.height) / 2f).coerceAtLeast(0f)
                    val nextPan = Offset(
                        x = (currentPan.x + gesturePan.x).coerceIn(-maxX, maxX),
                        y = (currentPan.y + gesturePan.y).coerceIn(-maxY, maxY)
                    )
                    onTransform(nextZoom, nextPan)
                }
            }
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val coverScale = maxOf(size.width / bitmap.width.toFloat(), size.height / bitmap.height.toFloat())
        val drawWidth = bitmap.width * coverScale * zoom
        val drawHeight = bitmap.height * coverScale * zoom
        val left = (size.width - drawWidth) / 2f + pan.x
        val top = (size.height - drawHeight) / 2f + pan.y
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(drawWidth.roundToInt(), drawHeight.roundToInt())
        )
        if (showCircularGuide) {
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = minOf(size.width, size.height) * 0.48f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

private fun cropSquare(bitmap: Bitmap, viewport: IntSize, zoom: Float, pan: Offset): Bitmap? {
    if (viewport.width <= 0 || viewport.height <= 0) return null
    val coverScale = maxOf(viewport.width / bitmap.width.toFloat(), viewport.height / bitmap.height.toFloat()) * zoom
    val drawnWidth = bitmap.width * coverScale
    val drawnHeight = bitmap.height * coverScale
    val left = (viewport.width - drawnWidth) / 2f + pan.x
    val top = (viewport.height - drawnHeight) / 2f + pan.y
    val sourceLeft = ((-left) / coverScale).roundToInt().coerceIn(0, bitmap.width - 1)
    val sourceTop = ((-top) / coverScale).roundToInt().coerceIn(0, bitmap.height - 1)
    val requestedSide = minOf(viewport.width, viewport.height).div(coverScale).roundToInt().coerceAtLeast(1)
    val side = minOf(requestedSide, bitmap.width - sourceLeft, bitmap.height - sourceTop)
    if (side <= 0) return null
    return runCatching { Bitmap.createBitmap(bitmap, sourceLeft, sourceTop, side, side) }.getOrNull()
}

@Composable
private fun ShopInformationDialog(
    shop: Shop,
    onDismiss: () -> Unit,
    onSave: (Shop) -> Unit
) {
    var name by remember(shop) { mutableStateOf(shop.name) }
    var owner by remember(shop) { mutableStateOf(shop.ownerName) }
    var email by remember(shop) { mutableStateOf(shop.email) }
    var phone by remember(shop) { mutableStateOf(shop.phone) }
    var address by remember(shop) { mutableStateOf(shop.address) }
    var category by remember(shop) { mutableStateOf(shop.category) }
    var dailyTargetStr by remember(shop) { mutableStateOf(if (shop.dailyTarget > 0) shop.dailyTarget.toInt().toString() else "30000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shop information", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShopInfoField("Shop name", name) { name = it }
                ShopInfoField("Owner name", owner) { owner = it }
                ShopInfoField("Email", email) { email = it }
                ShopInfoField("Phone", phone) { phone = it }
                ShopInfoField("Business category", category) { category = it }
                ShopInfoField("Address", address) { address = it }
                ShopInfoField("Daily Sales Target (KSh)", dailyTargetStr) { dailyTargetStr = it.filter { ch -> ch.isDigit() } }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        shop.copy(
                            name = name.trim(),
                            ownerName = owner.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            category = category.trim(),
                            address = address.trim(),
                            dailyTarget = dailyTargetStr.toDoubleOrNull() ?: shop.dailyTarget
                        )
                    )
                },
                enabled = name.isNotBlank() && owner.isNotBlank()
            ) {
                Text("Save changes", color = EmeraldBright, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = SurfaceDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}

@Composable
private fun ShopInfoField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(13.dp),
        textStyle = LocalTextStyle.current.copy(color = TextPrimary),
        colors = com.example.pos.ui.components.posTextFieldColors(
            unfocusedContainerColor = SurfaceDarkVariant
        )
    )
}

@Composable
private fun AppearanceSettingsCard(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    selectedColorTheme: AppColorTheme,
    customAccentColor: Color,
    onColorThemeSelected: (AppColorTheme) -> Unit,
    onCustomAccentColorSelected: (Color) -> Unit
) {
    var showCustomColorPicker by remember { mutableStateOf(false) }
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder,
        cornerRadius = 20.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BrightnessAuto, contentDescription = null, tint = EmeraldBright)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Appearance", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Choose how RetailSync looks", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val isSelected = mode == selectedMode
                    val icon = when (mode) {
                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                        ThemeMode.DARK -> Icons.Default.DarkMode
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) EmeraldContainer else SurfaceDarkVariant)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) EmeraldBright.copy(alpha = 0.55f) else SurfaceBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) EmeraldBright else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = mode.label,
                            color = if (isSelected) EmeraldPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = when (selectedMode) {
                    ThemeMode.SYSTEM -> "Follows your phone's light or dark setting."
                    ThemeMode.LIGHT -> "Always use the bright, easy-to-read theme."
                    ThemeMode.DARK -> "Use a low-glare dark theme."
                },
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(18.dp))
            Text("Color theme", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Choose an accent color for the app.", color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppColorTheme.entries.filter { it != AppColorTheme.CUSTOM }.forEach { colorTheme ->
                    val isSelected = colorTheme == selectedColorTheme
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onColorThemeSelected(colorTheme) }
                            )
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(27.dp)
                                .clip(CircleShape)
                                .background(colorTheme.colors.swatch)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, EmeraldBright, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = colorTheme.label,
                            color = if (isSelected) EmeraldPrimary else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SurfaceDarkVariant,
                borderColor = if (selectedColorTheme == AppColorTheme.CUSTOM) EmeraldBright else SurfaceBorder,
                cornerRadius = 14.dp,
                onClick = { showCustomColorPicker = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(customAccentColor)
                            .then(
                                if (selectedColorTheme == AppColorTheme.CUSTOM) {
                                    Modifier.border(2.dp, EmeraldBright, CircleShape)
                                } else Modifier
                            )
                    )
                    Spacer(Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Choose any color", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (selectedColorTheme == AppColorTheme.CUSTOM) {
                                "Custom accent selected · ${customAccentColor.toHexString()}"
                            } else {
                                "Create a custom accent color"
                            },
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Text("›", color = EmeraldBright, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showCustomColorPicker) {
        CustomAccentColorDialog(
            initialColor = customAccentColor,
            onDismiss = { showCustomColorPicker = false },
            onApply = { selectedColor ->
                onCustomAccentColorSelected(selectedColor)
                showCustomColorPicker = false
            }
        )
    }
}

@Composable
private fun CustomAccentColorDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onApply: (Color) -> Unit
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember(initialColor) { mutableStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableStateOf(initialHsv[1]) }
    var brightness by remember(initialColor) { mutableStateOf(initialHsv[2]) }
    val selectedColor = Color.hsv(hue, saturation, brightness)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose any color", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(2.dp, SurfaceBorder, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("#${selectedColor.toHexString()}", color = TextSecondary, fontSize = 13.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text("Hue", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                HueSpectrumPicker(hue = hue, onHueChange = { hue = it })
                Spacer(Modifier.height(14.dp))
                Text(
                    "Shade and brightness",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                SaturationBrightnessPicker(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onSelectionChange = { newSaturation, newBrightness ->
                        saturation = newSaturation
                        brightness = newBrightness
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap or drag anywhere in the color field to choose a shade.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selectedColor) }) {
                Text("Apply", color = EmeraldBright, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = SurfaceDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}

@Composable
private fun HueSpectrumPicker(hue: Float, onHueChange: (Float) -> Unit) {
    val spectrum = listOf(
        Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
        Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000)
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    onHueChange((position.x / size.width * 360f).coerceIn(0f, 360f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onHueChange((change.position.x / size.width * 360f).coerceIn(0f, 360f))
                }
            }
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(spectrum),
            cornerRadius = CornerRadius(size.height / 2f)
        )
        val markerX = (hue / 360f).coerceIn(0f, 1f) * size.width
        val marker = Offset(markerX, size.height / 2f)
        drawCircle(Color.Black, radius = 10.dp.toPx(), center = marker)
        drawCircle(Color.White, radius = 7.dp.toPx(), center = marker)
    }
}

@Composable
private fun SaturationBrightnessPicker(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onSelectionChange: (Float, Float) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(hue) {
                detectTapGestures { position ->
                    onSelectionChange(
                        (position.x / size.width).coerceIn(0f, 1f),
                        (1f - position.y / size.height).coerceIn(0f, 1f)
                    )
                }
            }
            .pointerInput(hue) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onSelectionChange(
                        (change.position.x / size.width).coerceIn(0f, 1f),
                        (1f - change.position.y / size.height).coerceIn(0f, 1f)
                    )
                }
            }
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
    ) {
        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        val selector = Offset(
            saturation.coerceIn(0f, 1f) * size.width,
            (1f - brightness.coerceIn(0f, 1f)) * size.height
        )
        drawCircle(Color.Black, radius = 11.dp.toPx(), center = selector, style = Stroke(3.dp.toPx()))
        drawCircle(Color.White, radius = 8.dp.toPx(), center = selector, style = Stroke(2.dp.toPx()))
    }
}

private fun Color.toHexString(): String =
    Integer.toHexString(toArgb()).takeLast(6).uppercase()

@Composable
fun SettingsMenuItem(
    label: String,
    icon: ImageVector,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder,
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDarkVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = EmeraldBright,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                            .background(EmeraldBright, shape = RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            color = TextOnPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CreditReminderSettingsDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onSaved: (() -> Unit)? = null
) {
    val config by viewModel.reminderConfig.collectAsState()
    var autoSend by remember { mutableStateOf(config.autoSendEnabled) }
    var advanceDaysInput by remember { mutableStateOf(config.advanceDaysBeforeDue.toString()) }
    var weekAdvanceDaysInput by remember { mutableStateOf(config.additionalAdvanceDaysBeforeDue.toString()) }
    var firstOverdueDaysInput by remember { mutableStateOf(config.firstOverdueDaysAfterDue.toString()) }
    var repeatInterval by remember { mutableStateOf(config.repeatInterval) }
    var repeatIntervalDaysInput by remember { mutableStateOf(config.repeatIntervalDays.toString()) }
    var repeatCountInput by remember { mutableStateOf(config.repeatCount.toString()) }
    var stopCondition by remember { mutableStateOf(config.stopCondition) }
    var channel by remember { mutableStateOf(config.defaultChannel) }
    var allowOffHours by remember { mutableStateOf(config.allowOffHoursSending) }
    var workingHoursStart by remember { mutableIntStateOf(config.workingHoursStart) }
    var workingHoursEnd by remember { mutableIntStateOf(config.workingHoursEnd) }
    var defaultCreditDuration by remember { mutableStateOf(viewModel.getCreditDurationDays()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = SurfaceDark,
        tonalElevation = 6.dp,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = EmeraldBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Credit Reminders & Terms",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automated WhatsApp & SMS configuration",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Default Credit Duration
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Default credit period (due date)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Applied when creating new credit sales without altering existing records", color = TextMuted, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(7 to "7 Days", 14 to "14 Days", 30 to "30 Days", 60 to "60 Days").forEach { (days, label) ->
                            val selected = defaultCreditDuration == days
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) EmeraldBright else SurfaceDarkVariant)
                                    .clickable { defaultCreditDuration = days }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) TextOnPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Auto-send toggle
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceDarkVariant,
                    borderColor = SurfaceBorder,
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic reminders", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Send WhatsApp & SMS without manual prompt", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = autoSend,
                            onCheckedChange = { autoSend = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextOnPrimary,
                                checkedTrackColor = EmeraldBright
                            )
                        )
                    }
                }

                // Delivery Channel
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Delivery channel", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            com.example.pos.data.ReminderDeliveryChannel.BOTH to "Both",
                            com.example.pos.data.ReminderDeliveryChannel.WHATSAPP to "WhatsApp",
                            com.example.pos.data.ReminderDeliveryChannel.SMS to "SMS"
                        ).forEach { (ch, label) ->
                            val selected = channel == ch
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) EmeraldBright else SurfaceDarkVariant)
                                    .clickable { channel = ch }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) TextOnPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Advance reminder offsets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Advance reminders (days before due)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weekAdvanceDaysInput,
                            onValueChange = { weekAdvanceDaysInput = it.filter(Char::isDigit).take(3) },
                            modifier = Modifier.weight(1f),
                            label = { Text("First notice") },
                            supportingText = { Text("Default: 7 days") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = advanceDaysInput,
                            onValueChange = { advanceDaysInput = it.filter(Char::isDigit).take(3) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Second notice") },
                            supportingText = { Text("Default: 3 days") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                // Due Date Reminder (Mandatory Core Rule)
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceDarkVariant,
                    borderColor = SurfaceBorder,
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Due date reminder (Mandatory)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Automatically dispatched on the due date for all unsettled accounts", color = TextMuted, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("ACTIVE", color = EmeraldBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // First Overdue Interval
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("First overdue reminder (days after due)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = firstOverdueDaysInput,
                        onValueChange = { firstOverdueDaysInput = it.filter(Char::isDigit).take(3) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Days after due date (default: 3)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Recurring Overdue Interval
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Recurring overdue reminder", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            com.example.pos.data.OverdueRepeatInterval.NONE to "None",
                            com.example.pos.data.OverdueRepeatInterval.DAILY to "Daily",
                            com.example.pos.data.OverdueRepeatInterval.EVERY_3_DAYS to "3 Days",
                            com.example.pos.data.OverdueRepeatInterval.WEEKLY to "Weekly",
                            com.example.pos.data.OverdueRepeatInterval.CUSTOM to "Custom"
                        ).forEach { (interval, label) ->
                            val selected = repeatInterval == interval
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) EmeraldBright else SurfaceDarkVariant)
                                    .clickable { repeatInterval = interval }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) TextOnPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (repeatInterval == com.example.pos.data.OverdueRepeatInterval.CUSTOM) {
                        OutlinedTextField(
                            value = repeatIntervalDaysInput,
                            onValueChange = { repeatIntervalDaysInput = it.filter(Char::isDigit).take(3) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Repeat every N days (1–365)") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    if (repeatInterval != com.example.pos.data.OverdueRepeatInterval.NONE) {
                        OutlinedTextField(
                            value = repeatCountInput,
                            onValueChange = { repeatCountInput = it.filter(Char::isDigit).take(3) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Maximum repeat reminders (default: 3)") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                // Stop Condition
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Stop condition", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            com.example.pos.data.OverdueStopCondition.PAID_ONLY to "Paid Only",
                            com.example.pos.data.OverdueStopCondition.ONE_MONTH to "1 Month",
                            com.example.pos.data.OverdueStopCondition.THREE_MONTHS to "3 Months"
                        ).forEach { (stopCond, label) ->
                            val selected = stopCondition == stopCond
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) EmeraldBright else SurfaceDarkVariant)
                                    .clickable { stopCondition = stopCond }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) TextOnPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Working Hours presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Business hours preset", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Triple(8, 17, "08:00–17:00"),
                            Triple(9, 18, "09:00–18:00"),
                            Triple(9, 12, "09:00–12:00"),
                            Triple(0, 24, "24-hour operation")
                        ).chunked(2).forEach { rowChoices ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowChoices.forEach { (start, end, label) ->
                                    FilterChip(
                                        selected = workingHoursStart == start && workingHoursEnd == end,
                                        onClick = {
                                            workingHoursStart = start
                                            workingHoursEnd = end
                                            allowOffHours = start == 0 && end == 24
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Working Hours Window
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceDarkVariant,
                    borderColor = SurfaceBorder,
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enforce business hours", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (allowOffHours) "Send at any time (${workingHoursStart.toString().padStart(2, '0')}:00–${workingHoursEnd.toString().padStart(2, '0')}:00)"
                                else "Only send ${workingHoursStart.toString().padStart(2, '0')}:00–${workingHoursEnd.toString().padStart(2, '0')} Nairobi time",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = !allowOffHours,
                            onCheckedChange = { allowOffHours = !it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextOnPrimary,
                                checkedTrackColor = EmeraldBright
                            )
                        )
                    }
                }

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = config.copy(
                        autoSendEnabled = autoSend,
                        advanceDaysBeforeDue = advanceDaysInput.toIntOrNull()?.coerceIn(1, 365) ?: config.advanceDaysBeforeDue,
                        additionalAdvanceDaysBeforeDue = weekAdvanceDaysInput.toIntOrNull()?.coerceIn(1, 365) ?: config.additionalAdvanceDaysBeforeDue,
                        firstOverdueDaysAfterDue = firstOverdueDaysInput.toIntOrNull()?.coerceIn(1, 365) ?: config.firstOverdueDaysAfterDue,
                        repeatInterval = repeatInterval,
                        repeatIntervalDays = repeatIntervalDaysInput.toIntOrNull()?.coerceIn(1, 365) ?: config.repeatIntervalDays,
                        repeatCount = repeatCountInput.toIntOrNull()?.coerceIn(0, 365) ?: config.repeatCount,
                        stopCondition = stopCondition,
                        defaultChannel = channel,
                        allowOffHoursSending = allowOffHours,
                        workingHoursStart = workingHoursStart.coerceIn(0, 23),
                        workingHoursEnd = workingHoursEnd.coerceIn(workingHoursStart.coerceIn(0, 23) + 1, 24)
                    )
                    viewModel.updateReminderConfig(updated)
                    viewModel.setCreditDurationDays(defaultCreditDuration)
                    onSaved?.invoke()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldBright,
                    contentColor = TextOnPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontSize = 12.sp)
            }
        }
    )

}
