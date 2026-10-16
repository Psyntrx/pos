package com.example.pos.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.Product
import com.example.pos.data.ProductImageStorage
import com.example.pos.ui.components.FilterChipItem
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.StockBadge
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val standardProductCategories = listOf("Groceries", "Beverages", "Snacks", "Household")

private fun suggestedProductSku(products: List<Product>): String {
    val existingSkus = products.map { it.sku.trim().lowercase() }.toSet()
    var number = products.size + 1
    var candidate = "SKU-${number.toString().padStart(4, '0')}"
    while (candidate.lowercase() in existingSkus) {
        number++
        candidate = "SKU-${number.toString().padStart(4, '0')}"
    }
    return candidate
}

@Composable
fun ProductsScreen(
    viewModel: PosViewModel,
    onOpenAddProduct: () -> Unit
) {
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    val products by viewModel.products.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories = remember(products) {
        listOf("All") + (standardProductCategories + products.map { it.category.trim() })
            .filter(String::isNotBlank)
            .distinctBy(String::lowercase)
    }
    val filteredProducts = products.filter { product ->
        val matchesCategory = selectedCategory == "All" ||
            product.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
            product.name.contains(searchQuery.trim(), ignoreCase = true) ||
            product.sku.contains(searchQuery.trim(), ignoreCase = true)
        matchesCategory && matchesSearch
    }
    val lowStockCount = products.count(Product::isLowStock)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Inventory",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Your products, under control",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }
            FilledTonalButton(
                onClick = onOpenAddProduct,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = EmeraldBright,
                    contentColor = TextOnPrimary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add product", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        InventoryOverviewCard(totalProducts = products.size, lowStockCount = lowStockCount)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            placeholder = { Text("Search name or SKU", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldBright) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(17.dp),
            textStyle = LocalTextStyle.current.copy(color = TextPrimary),
            colors = productTextFieldColors(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(categories) { category ->
                FilterChipItem(
                    label = category,
                    isSelected = selectedCategory.equals(category, ignoreCase = true),
                    onClick = { viewModel.selectCategory(category) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PRODUCT CATALOG", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Text("${filteredProducts.size} items", color = TextSecondary, fontSize = 12.sp)
        }

        if (filteredProducts.isEmpty()) {
            EmptyProductsState(
                hasFilters = searchQuery.isNotBlank() || selectedCategory != "All",
                onClearFilters = {
                    viewModel.updateSearchQuery("")
                    viewModel.selectCategory("All")
                },
                onAddProduct = onOpenAddProduct,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                items(filteredProducts, key = Product::id) { product ->
                    ProductListItemCard(product = product, onEdit = { editingProduct = product })
                }
            }
        }
    }

    editingProduct?.let { product ->
        EditProductDialog(
            product = product,
            viewModel = viewModel,
            onDismiss = { editingProduct = null }
        )
    }
}

@Composable
private fun InventoryOverviewCard(totalProducts: Int, lowStockCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(EmeraldContainer, CardGradientStart, CardGradientEnd)))
            .border(1.dp, EmeraldBright.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 17.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 29.dp, y = (-39).dp)
                .size(118.dp)
                .background(Brush.radialGradient(listOf(EmeraldBright.copy(alpha = 0.18f), Color.Transparent)), CircleShape)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("INVENTORY OVERVIEW", color = EmeraldBright.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                Spacer(Modifier.height(5.dp))
                Text("$totalProducts", color = TextPrimary, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text(if (totalProducts == 1) "product in your catalog" else "products in your catalog", color = TextSecondary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(25.dp))
                Spacer(Modifier.height(9.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (lowStockCount > 0) WarningContainer else EmeraldContainer)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (lowStockCount > 0) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(15.dp))
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (lowStockCount > 0) "$lowStockCount low stock" else "Stock healthy",
                        color = if (lowStockCount > 0) WarningYellow else EmeraldBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ProductListItemCard(product: Product, onEdit: () -> Unit) {
    val accent = when (product.category.lowercase()) {
        "groceries" -> Color(0xFF16834A)
        "beverages" -> Color(0xFF2879B9)
        "snacks" -> Color(0xFFAA650B)
        "household" -> Color(0xFF7853A9)
        else -> EmeraldBright
    }
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder,
        cornerRadius = 19.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.13f))
                    .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                ProductPhotoImage(product.imageUri) {
                    Icon(
                        imageVector = when (product.category.lowercase()) {
                            "groceries" -> Icons.Default.LocalGroceryStore
                            "beverages" -> Icons.Default.Category
                            else -> Icons.Default.ShoppingBag
                        },
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${product.category}  ·  ${product.sku}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                StockBadge(isLowStock = product.isLowStock, stockQty = product.stockQuantity)
            }
            Spacer(Modifier.width(7.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "KSh ${String.format("%,.0f", product.sellingPrice)}",
                    color = EmeraldBright,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text("per ${product.unit}", color = TextMuted, fontSize = 10.sp)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${product.name}", tint = EmeraldBright)
            }
        }
    }
}

@Composable
private fun EditProductDialog(
    product: Product,
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val initialSku = product.sku.ifBlank { suggestedProductSku(products) }
    var name by remember(product.id) { mutableStateOf(product.name) }
    var category by remember(product.id) { mutableStateOf(product.category) }
    var sku by remember(product.id) { mutableStateOf(initialSku) }
    var unit by remember(product.id) { mutableStateOf(product.unit.ifBlank { "pcs" }) }
    var buyingPrice by remember(product.id) { mutableStateOf(product.buyingPrice.toString()) }
    var sellingPrice by remember(product.id) { mutableStateOf(product.sellingPrice.toString()) }
    var quantity by remember(product.id) { mutableStateOf(product.stockQuantity.toString()) }
    var minStock by remember(product.id) { mutableStateOf(product.minStock.toString()) }
    var supplier by remember(product.id) { mutableStateOf(product.supplier) }
    var imageUri by remember(product.id) { mutableStateOf(product.imageUri) }
    var attemptedSave by remember(product.id) { mutableStateOf(false) }
    var saveError by remember(product.id) { mutableStateOf<String?>(null) }

    val parsedBuyingPrice = buyingPrice.toDoubleOrNull()
    val parsedSellingPrice = sellingPrice.toDoubleOrNull()
    val parsedQuantity = quantity.toIntOrNull()
    val parsedMinStock = minStock.toIntOrNull()
    val effectiveSku = sku.trim().ifBlank { initialSku }
    val duplicateSku = effectiveSku.isNotBlank() && products.any {
        it.id != product.id && it.sku.isNotBlank() && it.sku.equals(effectiveSku, ignoreCase = true)
    }
    val valid = name.isNotBlank() && category.isNotBlank() && !duplicateSku &&
        unit.isNotBlank() && parsedBuyingPrice != null && parsedBuyingPrice.isFinite() && parsedBuyingPrice >= 0 &&
        parsedSellingPrice != null && parsedSellingPrice.isFinite() && parsedSellingPrice > 0 &&
        parsedQuantity != null && parsedQuantity >= 0 && parsedMinStock != null && parsedMinStock >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit product", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProductPhotoField(imageUri = imageUri, onImageUriChange = { imageUri = it })
                ProductFormField(name, { name = it; saveError = null }, "Product name *", placeholder = "Product name")
                ProductFormField(category, { category = it; saveError = null }, "Category *", placeholder = "Category")
                ProductFormField(
                    sku,
                    { sku = it; saveError = null },
                    "SKU / barcode *",
                    placeholder = "Unique code",
                    isError = attemptedSave && (sku.isBlank() || duplicateSku),
                    supportingText = if (attemptedSave && duplicateSku) "This SKU is already in use." else null
                )
                ProductFormField(unit, { unit = it; saveError = null }, "Unit *", placeholder = "e.g. kg, pcs, bottle")
                ProductFormField(buyingPrice, { buyingPrice = it; saveError = null }, "Cost price *", prefix = "KSh", keyboardType = KeyboardType.Decimal)
                ProductFormField(sellingPrice, { sellingPrice = it; saveError = null }, "Selling price *", prefix = "KSh", keyboardType = KeyboardType.Decimal)
                ProductFormField(quantity, { quantity = it.filter(Char::isDigit); saveError = null }, "Quantity *", keyboardType = KeyboardType.Number)
                ProductFormField(minStock, { minStock = it.filter(Char::isDigit); saveError = null }, "Low-stock alert *", keyboardType = KeyboardType.Number)
                ProductFormField(supplier, { supplier = it }, "Supplier", placeholder = "Optional")
                if (attemptedSave && !valid && saveError == null) {
                    Text("Check all required fields and numeric values.", color = ErrorRed, fontSize = 12.sp)
                }
                saveError?.let { Text(it, color = ErrorRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                attemptedSave = true
                saveError = null
                if (valid) {
                    val saved = viewModel.updateProduct(
                        productId = product.id,
                        name = name,
                        category = category,
                        sku = effectiveSku,
                        unit = unit,
                        buyingPrice = parsedBuyingPrice!!,
                        sellingPrice = parsedSellingPrice!!,
                        quantity = parsedQuantity!!,
                        minStock = parsedMinStock!!,
                        supplier = supplier,
                        imageUri = imageUri
                    )
                    if (saved) onDismiss() else saveError = "Could not update the product. Check its SKU and try again."
                }
            }) { Text("Save changes", color = EmeraldBright, fontWeight = FontWeight.Bold) }
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
private fun EmptyProductsState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    onAddProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    SurfaceCard(
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder,
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(68.dp).clip(CircleShape).background(EmeraldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(if (hasFilters) "No matching products" else "Your catalog is ready", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (hasFilters) "Try another search or clear your filters." else "Add your first product to start managing stock.",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = if (hasFilters) onClearFilters else onAddProduct) {
                Text(if (hasFilters) "Clear filters" else "Add a product", color = EmeraldBright, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddProductScreen(
    viewModel: PosViewModel,
    onClose: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf(suggestedProductSku(products)) }
    var unit by remember { mutableStateOf("") }
    var buyingPrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var minStock by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var attemptedSave by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val categories = remember(products) {
        (standardProductCategories + products.map { it.category.trim() })
            .filter(String::isNotBlank)
            .distinctBy(String::lowercase)
    }
    val parsedBuyingPrice = buyingPrice.toDoubleOrNull()
    val parsedSellingPrice = sellingPrice.toDoubleOrNull()
    val parsedQuantity = quantity.toIntOrNull()
    val parsedMinStock = minStock.toIntOrNull()
    val duplicateSku = sku.isNotBlank() && products.any { it.sku.equals(sku.trim(), ignoreCase = true) }
    val nameError = attemptedSave && name.isBlank()
    val categoryError = attemptedSave && category.isBlank()
    val skuError = attemptedSave && (sku.isBlank() || duplicateSku)
    val unitError = attemptedSave && unit.isBlank()
    val buyingPriceError = attemptedSave && (parsedBuyingPrice == null || !parsedBuyingPrice.isFinite() || parsedBuyingPrice < 0)
    val sellingPriceError = attemptedSave && (parsedSellingPrice == null || !parsedSellingPrice.isFinite() || parsedSellingPrice <= 0)
    val quantityError = attemptedSave && (parsedQuantity == null || parsedQuantity < 0)
    val minStockError = attemptedSave && (parsedMinStock == null || parsedMinStock < 0)
    val formIsValid = name.isNotBlank() && category.isNotBlank() && sku.isNotBlank() && !duplicateSku &&
        unit.isNotBlank() && parsedBuyingPrice != null && parsedBuyingPrice.isFinite() && parsedBuyingPrice >= 0 &&
        parsedSellingPrice != null && parsedSellingPrice.isFinite() && parsedSellingPrice > 0 &&
        parsedQuantity != null && parsedQuantity >= 0 && parsedMinStock != null && parsedMinStock >= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(SurfaceDarkVariant)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Add product", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Build your catalog entry", color = TextSecondary, fontSize = 12.sp)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            AddProductIntroCard()
            Spacer(Modifier.height(16.dp))

            FormSectionCard(title = "Product details", subtitle = "Identify and organize this item", icon = Icons.Default.ShoppingBag) {
                ProductPhotoField(imageUri = imageUri, onImageUriChange = { imageUri = it })
                Spacer(Modifier.height(12.dp))
                ProductFormField(
                    value = name,
                    onValueChange = { name = it; saveError = null },
                    label = "Product name *",
                    placeholder = "e.g. Fresh milk 500ml",
                    isError = nameError,
                    supportingText = if (nameError) "Enter a product name." else null
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = it }
                ) {
                    ProductFormField(
                        value = category,
                        onValueChange = {},
                        label = "Category *",
                        placeholder = "Choose a category",
                        readOnly = true,
                        isError = categoryError,
                        supportingText = if (categoryError) "Choose a category." else null,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = TextPrimary) },
                                onClick = {
                                    category = option
                                    categoryMenuExpanded = false
                                    saveError = null
                                },
                                leadingIcon = if (category.equals(option, true)) {
                                    { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldBright) }
                                } else null
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    ProductFormField(
                        value = sku,
                        onValueChange = { sku = it; saveError = null },
                        label = "SKU / barcode *",
                        placeholder = "Enter unique code",
                        isError = skuError,
                        supportingText = when {
                            skuError && duplicateSku -> "This SKU is already in use."
                            skuError -> "Enter a SKU."
                            else -> null
                        },
                        trailingIcon = {
                            IconButton(onClick = { sku = suggestedProductSku(products); saveError = null }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Generate a new SKU", tint = EmeraldBright)
                            }
                        },
                        modifier = Modifier.weight(1.4f)
                    )
                    ProductFormField(
                        value = unit,
                        onValueChange = { unit = it; saveError = null },
                        label = "Unit *",
                        placeholder = "Enter unit",
                        isError = unitError,
                        supportingText = if (unitError) "Enter a unit." else null,
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            FormSectionCard(title = "Pricing", subtitle = "Set your cost and selling price", icon = Icons.Default.AttachMoney) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    ProductFormField(
                        value = buyingPrice,
                        onValueChange = { buyingPrice = it; saveError = null },
                        label = "Cost price *",
                        placeholder = "0.00",
                        prefix = "KSh",
                        keyboardType = KeyboardType.Decimal,
                        isError = buyingPriceError,
                        supportingText = if (buyingPriceError) "Enter a valid amount ≥ 0." else null,
                        modifier = Modifier.weight(1f)
                    )
                    ProductFormField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it; saveError = null },
                        label = "Selling price *",
                        placeholder = "0.00",
                        prefix = "KSh",
                        keyboardType = KeyboardType.Decimal,
                        isError = sellingPriceError,
                        supportingText = if (sellingPriceError) "Enter a price above 0." else null,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (parsedBuyingPrice != null && parsedSellingPrice != null && parsedSellingPrice > 0) {
                    val margin = (parsedSellingPrice - parsedBuyingPrice) / parsedSellingPrice * 100
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(EmeraldContainer).padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = if (margin >= 0) "Gross margin  ${"%.1f".format(margin)}%" else "Selling price is below cost",
                            color = if (margin >= 0) EmeraldBright else WarningYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            FormSectionCard(title = "Opening stock", subtitle = "Record the starting quantity", icon = Icons.Default.Inventory2) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    ProductFormField(
                        value = quantity,
                        onValueChange = { quantity = it.filter(Char::isDigit); saveError = null },
                        label = "Quantity *",
                        placeholder = "0",
                        keyboardType = KeyboardType.Number,
                        isError = quantityError,
                        supportingText = if (quantityError) "Enter a quantity ≥ 0." else null,
                        modifier = Modifier.weight(1f)
                    )
                    ProductFormField(
                        value = minStock,
                        onValueChange = { minStock = it.filter(Char::isDigit); saveError = null },
                        label = "Low-stock alert *",
                        placeholder = "Enter threshold",
                        keyboardType = KeyboardType.Number,
                        isError = minStockError,
                        supportingText = if (minStockError) "Enter a threshold ≥ 0." else null,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                ProductFormField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = "Supplier (optional)",
                    placeholder = "Supplier name"
                )
            }
            if (saveError != null) {
                Spacer(Modifier.height(12.dp))
                Text(saveError.orEmpty(), color = ErrorRed, fontSize = 12.sp)
            }
            Spacer(Modifier.height(20.dp))
        }

        Surface(color = BgDark, shadowElevation = 10.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
                PrimaryGreenButton(
                    text = "Save product",
                    onClick = {
                        attemptedSave = true
                        saveError = null
                        if (formIsValid) {
                            val saved = viewModel.addProduct(
                                name = name.trim(),
                                category = category.trim(),
                                sku = sku.trim(),
                                unit = unit.trim(),
                                buyingPrice = parsedBuyingPrice!!,
                                sellingPrice = parsedSellingPrice!!,
                                quantity = parsedQuantity!!,
                                minStock = parsedMinStock!!,
                                supplier = supplier.trim(),
                                imageUri = imageUri
                            )
                            if (saved) onClose() else saveError = "Could not save this product. Check the SKU and try again."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.CheckCircle
                )
                Text(
                    "Required fields are marked with *",
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ProductPhotoField(
    imageUri: String?,
    onImageUriChange: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedUri = withContext(Dispatchers.IO) { ProductImageStorage.save(context, uri) }
                if (savedUri != null) {
                    onImageUriChange(savedUri)
                    imageError = null
                } else {
                    imageError = "Could not load that photo. Try another image."
                }
            }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                val savedUri = withContext(Dispatchers.IO) { ProductImageStorage.save(context, bitmap) }
                if (savedUri != null) {
                    onImageUriChange(savedUri)
                    imageError = null
                } else {
                    imageError = "Could not save the photo. Please try again."
                }
            }
        }
    }

    Column {
        SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceDarkVariant,
            borderColor = SurfaceBorder,
            cornerRadius = 16.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldContainer)
                        .clickable { picker.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    ProductPhotoImage(imageUri) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(25.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (imageUri == null) "Product photo" else "Photo selected",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Optional · square thumbnail", color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Row {
                        TextButton(
                            onClick = { picker.launch(arrayOf("image/*")) },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Text(if (imageUri == null) "Gallery" else "Change", color = EmeraldBright, fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { camera.launch(null) },
                            contentPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 0.dp, bottom = 0.dp)
                        ) {
                            Text("Camera", color = EmeraldBright, fontSize = 12.sp)
                        }
                    }
                }
                if (imageUri != null) {
                    TextButton(onClick = { onImageUriChange(null) }) {
                        Text("Remove", color = ErrorRed, fontSize = 11.sp)
                    }
                }
            }
        }
        imageError?.let { Text(it, color = ErrorRed, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp)) }
    }
}

@Composable
private fun ProductPhotoImage(
    imageUri: String?,
    fallback: @Composable () -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, imageUri) {
        value = if (imageUri.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = "Product photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        fallback()
    }
}

@Composable
private fun AddProductIntroCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(21.dp))
            .background(Brush.linearGradient(listOf(CardGradientEnd, SurfaceDarkVariant)))
            .border(1.dp, EmeraldBright.copy(alpha = 0.16f), RoundedCornerShape(21.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(EmeraldBright.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Make it easy to find", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text("Accurate details keep checkout and stock reports right.", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
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
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = TextMuted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ProductFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    prefix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 12.sp) },
        placeholder = placeholder?.let { hint -> { Text(hint, fontSize = 12.sp, color = TextMuted) } },
        prefix = prefix?.let { currency -> { Text("$currency ", color = TextSecondary, fontSize = 12.sp) } },
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { message -> { Text(message, fontSize = 10.sp) } },
        isError = isError,
        readOnly = readOnly,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        textStyle = LocalTextStyle.current.copy(color = TextPrimary),
        colors = productTextFieldColors()
    )
}

@Composable
private fun productTextFieldColors() = com.example.pos.ui.components.posTextFieldColors(
    unfocusedContainerColor = SurfaceDarkVariant
)
