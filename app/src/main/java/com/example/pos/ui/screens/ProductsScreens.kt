package com.example.pos.ui.screens

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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.Product
import com.example.pos.ui.components.FilterChipItem
import com.example.pos.ui.components.PrimaryGreenButton
import com.example.pos.ui.components.StockBadge
import com.example.pos.ui.components.SurfaceCard
import com.example.pos.ui.theme.*
import com.example.pos.viewmodel.PosViewModel

@Composable
fun LegacyProductsScreen(
    viewModel: PosViewModel,
    onOpenAddProduct: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val categories = listOf("All", "Groceries", "Beverages", "Snacks", "Household")

    val filteredProducts = products.filter { product ->
        val matchesCategory = (selectedCategory == "All" || product.category.equals(selectedCategory, ignoreCase = true))
        val matchesSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.sku.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Products",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                IconButton(
                    onClick = onOpenAddProduct,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Product",
                        tint = EmeraldBright
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search products...", color = TextSecondary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = EmeraldBright)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                colors = com.example.pos.ui.components.posTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Filter Pills Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    FilterChipItem(
                        label = cat,
                        isSelected = selectedCategory == cat,
                        onClick = { viewModel.selectCategory(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Product Cards List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No products found",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredProducts) { product ->
                        LegacyProductListItemCard(product = product)
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onOpenAddProduct,
            containerColor = EmeraldBright,
            contentColor = TextOnPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Product",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun LegacyProductListItemCard(product: Product) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = SurfaceDark,
        borderColor = SurfaceBorder,
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Product Icon / Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDarkVariant)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (product.category.lowercase()) {
                            "groceries" -> Icons.Default.LocalGroceryStore
                            "beverages" -> Icons.Default.Category
                            else -> Icons.Default.ShoppingBag
                        },
                        contentDescription = product.name,
                        tint = EmeraldBright,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    )

                    Text(
                        text = "SKU: ${product.sku}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "KSh ${String.format("%,.0f", product.sellingPrice)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldBright,
                                fontSize = 14.sp
                            )
                        )
                        if (product.hasWholesalePricing) {
                            Text(
                                text = "WS: KSh ${String.format("%,.0f", product.wholesalePrice)} (≥${product.wholesaleQuantity})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = EmeraldBright.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                StockBadge(
                    isLowStock = product.isLowStock,
                    stockQty = product.stockQuantity
                )
            }
        }
    }
}

@Composable
fun LegacyAddProductScreen(
    viewModel: PosViewModel,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Groceries") }
    var sku by remember { mutableStateOf("SK-${(100..999).random()}") }
    var unit by remember { mutableStateOf("pcs") }
    var buyingPrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var wholesalePrice by remember { mutableStateOf("") }
    var wholesaleQuantity by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var minStock by remember { mutableStateOf("10") }
    var supplier by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Product",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Add Product Image Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDarkVariant)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Upload",
                        tint = EmeraldBright,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add product image",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                colors = com.example.pos.ui.components.posTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = EmeraldBright) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                colors = com.example.pos.ui.components.posTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU / Code") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = buyingPrice,
                    onValueChange = { buyingPrice = it },
                    label = { Text("Buying Price") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it },
                    label = { Text("Retail Price *") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = wholesalePrice,
                    onValueChange = { wholesalePrice = it },
                    label = { Text("Wholesale Price") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = wholesaleQuantity,
                    onValueChange = { wholesaleQuantity = it.filter(Char::isDigit) },
                    label = { Text("Wholesale Min Qty") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = minStock,
                    onValueChange = { minStock = it },
                    label = { Text("Minimum Stock") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                    colors = com.example.pos.ui.components.posTextFieldColors(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = supplier,
                onValueChange = { supplier = it },
                label = { Text("Supplier (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                colors = com.example.pos.ui.components.posTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryGreenButton(
                text = "Save Product",
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addProduct(
                            name = name,
                            category = category,
                            sku = sku,
                            unit = unit,
                            buyingPrice = buyingPrice.toDoubleOrNull() ?: 100.0,
                            sellingPrice = sellingPrice.toDoubleOrNull() ?: 150.0,
                            quantity = quantity.toIntOrNull() ?: 10,
                            minStock = minStock.toIntOrNull() ?: 5,
                            supplier = supplier,
                            wholesalePrice = wholesalePrice.toDoubleOrNull()?.takeIf { it > 0 },
                            wholesaleQuantity = wholesaleQuantity.toIntOrNull()?.takeIf { it > 0 }
                        )
                        onClose()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockInScreen(
    viewModel: PosViewModel,
    initialProductId: String? = null,
    onClose: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val shop by viewModel.shop.collectAsState()

    val initialProduct = remember(products, initialProductId) {
        if (initialProductId != null) {
            products.find { it.id == initialProductId } ?: products.firstOrNull()
        } else {
            products.firstOrNull()
        }
    }

    var selectedProduct by remember { mutableStateOf<Product?>(initialProduct) }
    var productSearchQuery by remember { mutableStateOf(initialProduct?.name ?: "") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    var quantity by remember { mutableIntStateOf(10) }
    var quantityText by remember { mutableStateOf("10") }
    var buyingPrice by remember { mutableStateOf(initialProduct?.buyingPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var sellingPrice by remember { mutableStateOf(initialProduct?.sellingPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var supplier by remember { mutableStateOf(initialProduct?.supplier ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun onSelectProduct(prod: Product) {
        selectedProduct = prod
        productSearchQuery = prod.name
        isDropdownExpanded = false
        buyingPrice = if (prod.buyingPrice > 0) prod.buyingPrice.toString() else ""
        sellingPrice = if (prod.sellingPrice > 0) prod.sellingPrice.toString() else ""
        supplier = prod.supplier
        errorMessage = null
    }

    LaunchedEffect(products, initialProductId) {
        if (initialProductId != null) {
            products.find { it.id == initialProductId }?.let { prod ->
                onSelectProduct(prod)
            }
        }
    }

    // Filter products based on search query
    val filteredProducts = remember(products, productSearchQuery) {
        if (productSearchQuery.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(productSearchQuery.trim(), ignoreCase = true) ||
                it.sku.contains(productSearchQuery.trim(), ignoreCase = true) ||
                it.category.contains(productSearchQuery.trim(), ignoreCase = true)
            }
        }
    }

    // Live margin calculation
    val parsedBuying = buyingPrice.toDoubleOrNull()
    val parsedSelling = sellingPrice.toDoubleOrNull()
    val marginPercentage = if (parsedBuying != null && parsedSelling != null && parsedSelling > 0) {
        ((parsedSelling - parsedBuying) / parsedSelling * 100)
    } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Stock In",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Restock inventory & update pricing",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Searchable Product Selector with Dropdown
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = productSearchQuery,
                    onValueChange = {
                        productSearchQuery = it
                        isDropdownExpanded = true
                        errorMessage = null
                        if (selectedProduct != null && !selectedProduct!!.name.equals(it, ignoreCase = true)) {
                            selectedProduct = null
                        }
                    },
                    label = { Text("Product *", color = TextSecondary) },
                    placeholder = { Text("Search name, SKU, or category...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldBright) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (productSearchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    productSearchQuery = ""
                                    selectedProduct = null
                                    isDropdownExpanded = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDarkVariant,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = EmeraldBright,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                ) {
                    if (filteredProducts.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "No matching products found",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = { },
                            enabled = false
                        )
                    } else {
                        filteredProducts.forEach { prod ->
                            val isCurrSelected = selectedProduct?.id == prod.id
                            DropdownMenuItem(
                                text = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = prod.name,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrSelected) EmeraldBright else TextPrimary,
                                                    fontSize = 15.sp
                                                )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (prod.isLowStock) WarningContainer else EmeraldContainer)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${prod.stockQuantity} ${prod.unit}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (prod.isLowStock) WarningYellow else EmeraldBright,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${prod.category} • SKU: ${prod.sku} • Buy: KSh ${prod.buyingPrice.toInt()} • Sell: KSh ${prod.sellingPrice.toInt()}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                },
                                onClick = { onSelectProduct(prod) },
                                modifier = Modifier.background(
                                    if (isCurrSelected) EmeraldContainer.copy(alpha = 0.5f) else Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            // Current Stock & Threshold Info Badge
            if (selectedProduct != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDarkVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Stock: ${selectedProduct!!.stockQuantity} ${selectedProduct!!.unit}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (selectedProduct!!.isLowStock) WarningYellow else EmeraldBright,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Low Stock Alert: ${selectedProduct!!.minStock} ${selectedProduct!!.unit}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Quantity Input (Direct Typing + Stepper)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quantity to Add *",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val curr = quantityText.toIntOrNull() ?: 1
                            if (curr > 1) {
                                quantity = curr - 1
                                quantityText = (curr - 1).toString()
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDarkVariant)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                    ) {
                        Text("-", style = MaterialTheme.typography.titleLarge.copy(color = EmeraldBright))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() }
                            quantityText = filtered
                            quantity = filtered.toIntOrNull() ?: 0
                            errorMessage = null
                        },
                        modifier = Modifier.width(75.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDarkVariant,
                            unfocusedContainerColor = SurfaceDarkVariant,
                            focusedBorderColor = EmeraldBright,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val curr = quantityText.toIntOrNull() ?: 0
                            quantity = curr + 1
                            quantityText = (curr + 1).toString()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDarkVariant)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                    ) {
                        Text("+", style = MaterialTheme.typography.titleLarge.copy(color = EmeraldBright))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Buying Price (Cost per Unit) - Prefilled & Editable
            OutlinedTextField(
                value = buyingPrice,
                onValueChange = {
                    buyingPrice = it
                    errorMessage = null
                },
                label = { Text("Buying Price / Cost (KSh)", color = TextSecondary) },
                placeholder = { Text(selectedProduct?.buyingPrice?.toString() ?: "0.0", color = TextMuted) },
                supportingText = {
                    selectedProduct?.let {
                        Text(
                            text = "Current: KSh ${it.buyingPrice}. Modifying updates the product cost.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                colors = com.example.pos.ui.components.posTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Selling Price (Retail Price) - Prefilled & Editable
            OutlinedTextField(
                value = sellingPrice,
                onValueChange = {
                    sellingPrice = it
                    errorMessage = null
                },
                label = { Text("Selling Price / Retail (KSh)", color = TextSecondary) },
                placeholder = { Text(selectedProduct?.sellingPrice?.toString() ?: "0.0", color = TextMuted) },
                supportingText = {
                    selectedProduct?.let {
                        Text(
                            text = "Current: KSh ${it.sellingPrice}. Modifying updates the retail price.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDarkVariant,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = EmeraldBright,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            // Dynamic Margin Preview
            if (marginPercentage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (marginPercentage >= 0) EmeraldContainer else ErrorContainer)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (marginPercentage >= 0) Icons.Default.CheckCircle else Icons.Default.Clear,
                        contentDescription = null,
                        tint = if (marginPercentage >= 0) EmeraldBright else ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (marginPercentage >= 0) "Expected Gross Margin: ${String.format("%.1f", marginPercentage)}%" else "Selling price is below cost",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (marginPercentage >= 0) EmeraldBright else ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Supplier - Prefilled & Editable
            OutlinedTextField(
                value = supplier,
                onValueChange = {
                    supplier = it
                    errorMessage = null
                },
                label = { Text("Supplier", color = TextSecondary) },
                placeholder = { Text("Supplier name", color = TextMuted) },
                supportingText = {
                    selectedProduct?.let {
                        if (it.supplier.isNotBlank()) {
                            Text("Current: ${it.supplier}", color = TextMuted, fontSize = 11.sp)
                        } else null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                colors = com.example.pos.ui.components.posTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Date & Time
            OutlinedTextField(
                value = "Today, 10:00 AM",
                onValueChange = {},
                readOnly = true,
                label = { Text("Date & Time") },
                trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldBright) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                colors = com.example.pos.ui.components.posTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Recorded By Employee
            OutlinedTextField(
                value = "${shop.ownerName.ifBlank { "Owner" }} (Owner)",
                onValueChange = {},
                readOnly = true,
                label = { Text("Employee") },
                trailingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldBright) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                colors = com.example.pos.ui.components.posTextFieldColors()
            )

            // Error Display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 8. Submit Button
            PrimaryGreenButton(
                text = "Record Stock In",
                onClick = {
                    val prod = selectedProduct ?: filteredProducts.firstOrNull()
                    if (prod == null) {
                        errorMessage = "Please select a valid product to restock."
                        return@PrimaryGreenButton
                    }

                    val parsedQty = quantityText.toIntOrNull() ?: quantity
                    if (parsedQty <= 0) {
                        errorMessage = "Please enter a valid quantity greater than 0."
                        return@PrimaryGreenButton
                    }

                    val finalBuying = buyingPrice.toDoubleOrNull() ?: prod.buyingPrice
                    val finalSelling = sellingPrice.toDoubleOrNull() ?: prod.sellingPrice
                    val finalSupplier = supplier.trim().ifBlank { prod.supplier }

                    viewModel.recordStockIn(
                        productId = prod.id,
                        quantityAdded = parsedQty,
                        buyingPrice = finalBuying,
                        sellingPrice = finalSelling,
                        supplier = finalSupplier,
                        employeeName = shop.ownerName.ifBlank { "Store Owner" }
                    )

                    onClose()
                }
            )
        }
    }
}
