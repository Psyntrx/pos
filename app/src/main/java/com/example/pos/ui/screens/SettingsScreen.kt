package com.example.pos.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
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
    onThemeModeChange: (ThemeMode) -> Unit,
    profilePhotoUri: String?,
    onProfilePhotoChange: (String?) -> Unit,
    onOpenEmployees: () -> Unit,
    onOpenCreditReminders: () -> Unit,
    onOpenExpenses: () -> Unit
) {
    val shop by viewModel.shop.collectAsState()
    var showShopEditor by remember { mutableStateOf(false) }
    var showPhotoPreview by remember { mutableStateOf(false) }
    var photoBeingEdited by remember { mutableStateOf<Bitmap?>(null) }
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
            photoUri = profilePhotoUri,
            onChangePhoto = { photoPicker.launch(arrayOf("image/*")) },
            onPreviewPhoto = { showPhotoPreview = true },
            onRemovePhoto = { onProfilePhotoChange(null) }
        )

        Spacer(modifier = Modifier.height(22.dp))
        SettingsSectionLabel("PREFERENCES")
        Spacer(modifier = Modifier.height(9.dp))

        AppearanceSettingsCard(
            selectedMode = themeMode,
            onModeSelected = onThemeModeChange
        )

        Spacer(modifier = Modifier.height(22.dp))
        SettingsSectionLabel("SHOP & TEAM")
        Spacer(modifier = Modifier.height(9.dp))

        // Settings Menu Items
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

        SettingsMenuItem(
            label = "Notifications",
            icon = Icons.Default.Notifications,
            onClick = { }
        )

        SettingsMenuItem(
            label = "Credit Reminders",
            icon = Icons.Default.Receipt,
            onClick = onOpenCreditReminders
        )

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
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
private fun ProfileHeaderCard(
    shop: Shop,
    photoUri: String?,
    onChangePhoto: () -> Unit,
    onPreviewPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
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
                        shop.ownerName.firstOrNull()?.uppercase() ?: "?",
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
                Text(shop.ownerName, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("Store owner", color = EmeraldBright, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(shop.email, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
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
                            address = address.trim()
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
    onModeSelected: (ThemeMode) -> Unit
) {
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
        }
    }
}

@Composable
fun SettingsMenuItem(
    label: String,
    icon: ImageVector,
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

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
