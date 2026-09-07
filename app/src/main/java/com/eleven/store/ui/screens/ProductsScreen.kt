package com.eleven.store.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleven.store.data.model.Product
import com.eleven.store.ui.theme.Accent
import com.eleven.store.ui.theme.Border
import com.eleven.store.ui.theme.Destructive
import com.eleven.store.ui.theme.MutedForeground
import com.eleven.store.ui.theme.Neutral100
import com.eleven.store.ui.viewmodel.MainViewModel

// ═══════════════════════════════════════════════════════════════
//  PRODUCTS SCREEN — نسخة طبق الأصل من صفحة Products.tsx بالموقع
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductsScreen(
    viewModel: MainViewModel,
    initialCategory: String = "",
    initialFilter: String = "",
    initialSearch: String = "",
    onProductClick: (String) -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit = onBack,
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // ── حالة الفلاتر — مطابق للموقع ─────────────────────────────
    var selectedCategory by remember {
        mutableStateOf(if (initialCategory.isNotBlank()) initialCategory else "all")
    }
    var filterType by remember {
        mutableStateOf(if (initialFilter.isNotBlank()) initialFilter else "all")
    }
    var selectedBrand by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf(initialSearch) }

    // تحميل المنتجات عند تغيّر الفلاتر
    LaunchedEffect(selectedCategory, filterType, selectedBrand, searchQuery) {
        viewModel.loadProducts(
            categoryId = selectedCategory.takeIf { it != "all" },
            isFeatured = if (filterType == "featured") true else null,
            isNew = if (filterType == "new") true else null,
            isBestSeller = if (filterType == "bestSeller") true else null,
            onSale = if (filterType == "onSale") true else null,
            brandId = if (filterType == "brands" && selectedBrand != "all") selectedBrand else null,
            searchQuery = searchQuery.trim().takeIf { it.isNotEmpty() },
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ── الفلاتر — مطابقة للموقع: sticky top-0 z-10 bg-background/95 ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                    shadowElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // صفين منسدلين جنباً إلى جنب
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            // ── منسدل الفئات — "كل الفئات" ──
                            Box(modifier = Modifier.weight(1f)) {
                                var catExpanded by remember { mutableStateOf(false) }
                                val catLabel = if (selectedCategory == "all") "كل الفئات"
                                else categories.find { it.id == selectedCategory }?.name ?: "كل الفئات"

                                OutlinedButton(
                                    onClick = { catExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Border
                                    ),
                                ) {
                                    Text(
                                        catLabel,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text("▾", fontSize = 11.sp, color = MutedForeground)
                                }
                                DropdownMenu(
                                    expanded = catExpanded,
                                    onDismissRequest = { catExpanded = false },
                                    modifier = Modifier.background(
                                        MaterialTheme.colorScheme.surface
                                    ),
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "كل الفئات",
                                                fontWeight = if (selectedCategory == "all")
                                                    FontWeight.SemiBold
                                                else
                                                    FontWeight.Normal,
                                            )
                                        },
                                        onClick = {
                                            selectedCategory = "all"
                                            catExpanded = false
                                        },
                                        modifier = if (selectedCategory == "all")
                                            Modifier.background(Accent.copy(alpha = 0.1f))
                                        else
                                            Modifier,
                                        trailingIcon = if (selectedCategory == "all") {
                                            {
                                                Text(
                                                    "✓",
                                                    color = Accent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else null,
                                    )
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    cat.name,
                                                    fontWeight = if (selectedCategory == cat.id)
                                                        FontWeight.SemiBold
                                                    else
                                                        FontWeight.Normal,
                                                )
                                            },
                                            onClick = {
                                                selectedCategory = cat.id
                                                catExpanded = false
                                            },
                                            modifier = if (selectedCategory == cat.id)
                                                Modifier.background(Accent.copy(alpha = 0.1f))
                                            else
                                                Modifier,
                                            trailingIcon = if (selectedCategory == cat.id) {
                                                {
                                                    Text(
                                                        "✓",
                                                        color = Accent,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else null,
                                        )
                                    }
                                }
                            }

                            // ── منسدل الفرز — "جميع المنتجات" ──
                            Box(modifier = Modifier.weight(1f)) {
                                var filterExpanded by remember { mutableStateOf(false) }
                                val filterLabel = when (filterType) {
                                    "new" -> "المنتجات الجديدة"
                                    "bestSeller" -> "الأكثر مبيعاً"
                                    "featured" -> "المنتجات المميزة"
                                    "onSale" -> "العروض والخصومات"
                                    "brands" -> "العلامات التجارية"
                                    else -> "جميع المنتجات"
                                }
                                OutlinedButton(
                                    onClick = { filterExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Border
                                    ),
                                ) {
                                    Text(
                                        filterLabel,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text("▾", fontSize = 11.sp, color = MutedForeground)
                                }
                                DropdownMenu(
                                    expanded = filterExpanded,
                                    onDismissRequest = { filterExpanded = false },
                                    modifier = Modifier.background(
                                        MaterialTheme.colorScheme.surface
                                    ),
                                ) {
                                    listOf(
                                        "all" to "جميع المنتجات",
                                        "new" to "المنتجات الجديدة",
                                        "bestSeller" to "الأكثر مبيعاً",
                                        "featured" to "المنتجات المميزة",
                                        "onSale" to "العروض والخصومات",
                                        "brands" to "العلامات التجارية",
                                    ).forEach { (value, label) ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    label,
                                                    fontWeight = if (filterType == value)
                                                        FontWeight.SemiBold
                                                    else
                                                        FontWeight.Normal,
                                                )
                                            },
                                            onClick = {
                                                filterType = value
                                                if (value != "brands") selectedBrand = "all"
                                                filterExpanded = false
                                            },
                                            modifier = if (filterType == value)
                                                Modifier.background(Accent.copy(alpha = 0.1f))
                                            else
                                                Modifier,
                                            trailingIcon = if (filterType == value) {
                                                {
                                                    Text(
                                                        "✓",
                                                        color = Accent,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else null,
                                        )
                                    }
                                }
                            }
                        }

                        // ── منسدل العلامات التجارية — يظهر شرطياً ──
                        if (filterType == "brands") {
                            var brandExpanded by remember { mutableStateOf(false) }
                            val brandLabel = if (selectedBrand == "all") "اختر علامة تجارية"
                            else brands.find { it.id == selectedBrand }?.name ?: "اختر علامة تجارية"

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { brandExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Border
                                    ),
                                ) {
                                    Text(
                                        brandLabel,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text("▾", fontSize = 11.sp, color = MutedForeground)
                                }
                                DropdownMenu(
                                    expanded = brandExpanded,
                                    onDismissRequest = { brandExpanded = false },
                                    modifier = Modifier.background(
                                        MaterialTheme.colorScheme.surface
                                    ),
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "جميع الماركات",
                                                fontWeight = if (selectedBrand == "all")
                                                    FontWeight.SemiBold
                                                else
                                                    FontWeight.Normal,
                                            )
                                        },
                                        onClick = {
                                            selectedBrand = "all"
                                            brandExpanded = false
                                        },
                                        modifier = if (selectedBrand == "all")
                                            Modifier.background(Accent.copy(alpha = 0.1f))
                                        else
                                            Modifier,
                                        trailingIcon = if (selectedBrand == "all") {
                                            {
                                                Text(
                                                    "✓",
                                                    color = Accent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else null,
                                    )
                                    brands.forEach { brand ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    brand.name,
                                                    fontWeight = if (selectedBrand == brand.id)
                                                        FontWeight.SemiBold
                                                    else
                                                        FontWeight.Normal,
                                                )
                                            },
                                            onClick = {
                                                selectedBrand = brand.id
                                                brandExpanded = false
                                            },
                                            modifier = if (selectedBrand == brand.id)
                                                Modifier.background(Accent.copy(alpha = 0.1f))
                                            else
                                                Modifier,
                                            trailingIcon = if (selectedBrand == brand.id) {
                                                {
                                                    Text(
                                                        "✓",
                                                        color = Accent,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else null,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Border, thickness = 1.dp)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                // ── حالة التحميل ──────────────────────────────────
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 128.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = Accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }

                // ── لا توجد منتجات ────────────────────────────────
                products.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("🔍", fontSize = 60.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "لم نجد أي منتجات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "حاول تغيير الفلاتر أو العودة للصفحة الرئيسية",
                            color = MutedForeground,
                            fontSize = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onGoHome,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        ) {
                            Text(
                                "العودة للرئيسية",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // ── شبكة المنتجات — grid-cols-2 ──────────────────
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 24.dp,
                            bottom = 80.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductsScreenProductCard(
                                product = product,
                                isFavorite = product.id in favoriteIds,
                                onFavoriteToggle = { viewModel.toggleFavorite(product.id) },
                                onAddToCart = { viewModel.addToCart(product) },
                                onClick = { onProductClick(product.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  PRODUCT CARD — نسخة طبق الأصل من Product Card في Products.tsx
//  - rounded-xl border border-border
//  - صورة 1:1 aspect-square مع hover scale
//  - شارة خصم (أعلى يمين) + شارة مميز (أعلى يمين)
//  - اسم المنتج: font-bold text-sm line-clamp-1
//  - السعر: text-base font-extrabold text-accent + السعر الأصلي
//  - زر إضافة للسلة + زر مفضلة (ظاهرين دائماً)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProductsScreenProductCard(
    product: Product,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onAddToCart: () -> Unit,
    onClick: () -> Unit,
) {
    val discount = product.discountPercent
    val imageSrc = product.mainImage

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Column {
            // ── صورة 1:1 ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Neutral100),
            ) {
                AsyncImage(
                    model = imageSrc,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // شارة الخصم — أعلى يمين
                if (discount != null && discount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Destructive, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "-$discount%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // شارة "مميز" — أعلى يمين (إذا كان مميز ولا يوجد خصم)
                if (product.isFeatured && (discount == null || discount <= 0)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Accent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "مميز",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // ── معلومات المنتج — p-3 ──────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // اسم المنتج — font-bold text-sm line-clamp-1
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // السعر
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${formatNum(product.price)} ج.س",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Accent,
                    )
                    if (discount != null && discount > 0 && product.originalPrice != null) {
                        Text(
                            text = "${formatNum(product.originalPrice)}",
                            fontSize = 12.sp,
                            color = MutedForeground,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // أزرار: إضافة للسلة + مفضلة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // زر إضافة للسلة
                    Button(
                        onClick = onAddToCart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "إضافة للسلة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // زر المفضلة
                    OutlinedButton(
                        onClick = onFavoriteToggle,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.size(32.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                        ),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Destructive
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

// ملاحظة: formatNum مُعرّفة بشكل مشترك في HomeScreen.kt