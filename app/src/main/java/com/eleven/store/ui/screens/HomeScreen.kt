package com.eleven.store.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleven.store.data.model.Banner
import com.eleven.store.data.model.Brand
import com.eleven.store.data.model.Category
import com.eleven.store.data.model.Product
import com.eleven.store.ui.theme.Accent
import com.eleven.store.ui.theme.Border
import com.eleven.store.ui.theme.Destructive
import com.eleven.store.ui.theme.MutedForeground
import com.eleven.store.ui.theme.Neutral100
import com.eleven.store.ui.theme.Neutral300
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════
//  HOME SCREEN — نسخة طبق الأصل من Home.tsx في الموقع
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onProductClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onViewAllClick: (String) -> Unit,
) {
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val featuredProducts by viewModel.featuredProducts.collectAsStateWithLifecycle()
    val newArrivals by viewModel.newArrivals.collectAsStateWithLifecycle()
    val bestSellers by viewModel.bestSellers.collectAsStateWithLifecycle()
    val onSaleProducts by viewModel.onSaleProducts.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        // ── 1. Banner Slider — mx-4 mt-3 rounded-2xl h-200 ──────
        item {
            BannerSlider(
                banners = banners,
                isLoading = isLoading,
                onShopClick = { onViewAllClick("") },
            )
        }

        // ── 2. التصنيفات — أول 4 فقط ────────────────────────────
        if (categories.isNotEmpty()) {
            item {
                HomeSectionHeader(title = "التصنيفات", onViewAll = { onViewAllClick("") })
                CategoriesRow(
                    categories = categories.take(4),
                    onCategoryClick = onCategoryClick,
                )
            }
        }

        // ── 3. العروض والخصومات ─────────────────────────────────
        if (onSaleProducts.isNotEmpty()) {
            item {
                HomeSectionHeader(title = "العروض والخصومات", onViewAll = { onViewAllClick("onSale") })
                ProductRow(
                    products = onSaleProducts,
                    favoriteIds = favoriteIds,
                    onProductClick = onProductClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onAddToCart = { viewModel.addToCart(it) },
                )
            }
        }

        // ── 4. المنتجات المميزة ──────────────────────────────────
        if (featuredProducts.isNotEmpty()) {
            item {
                HomeSectionHeader(title = "المنتجات المميزة", onViewAll = { onViewAllClick("featured") })
                ProductRow(
                    products = featuredProducts,
                    favoriteIds = favoriteIds,
                    onProductClick = onProductClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onAddToCart = { viewModel.addToCart(it) },
                )
            }
        }

        // ── 5. الأكثر مبيعاً ────────────────────────────────────
        if (bestSellers.isNotEmpty()) {
            item {
                HomeSectionHeader(title = "الأكثر مبيعاً", onViewAll = { onViewAllClick("bestSeller") })
                ProductRow(
                    products = bestSellers,
                    favoriteIds = favoriteIds,
                    onProductClick = onProductClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onAddToCart = { viewModel.addToCart(it) },
                )
            }
        }

        // ── 6. المنتجات الجديدة ──────────────────────────────────
        if (newArrivals.isNotEmpty()) {
            item {
                HomeSectionHeader(title = "المنتجات الجديدة", onViewAll = { onViewAllClick("new") })
                ProductRow(
                    products = newArrivals,
                    favoriteIds = favoriteIds,
                    onProductClick = onProductClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onAddToCart = { viewModel.addToCart(it) },
                )
            }
        }

        // ── 7. العلامات التجارية ─────────────────────────────────
        if (brands.isNotEmpty()) {
            item {
                HomeSectionHeader(title = "العلامات التجارية", onViewAll = { onViewAllClick("brands") })
                BrandsRow(brands = brands.take(3))
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  BANNER SLIDER
//  - relative mx-4 mt-3 rounded-2xl overflow-hidden h-200dp
//  - gradient from-black/50 to-transparent
//  - النص في المنتصف مع max-width (مطابق للموقع: flex items-center px-5 max-w-xs)
//  - dots في الأسفل
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BannerSlider(
    banners: List<Banner>,
    isLoading: Boolean,
    onShopClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .height(200.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        when {
            // حالة تحميل
            isLoading && banners.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Neutral300, Neutral100)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Accent)
                }
            }

            // لا يوجد بانرات → fallback ذهبي مثل الموقع
            banners.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Accent.copy(alpha = 0.80f), Accent)
                            )
                        )
                ) {
                    // توسيط رأسي فقط + محاذاة للبداية أفقياً — مطابق للموقع:
                    // flex items-center justify-start px-6 (وليس توسيط كامل)
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 24.dp)
                            .widthIn(max = 320.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            "مرحباً في Eleven",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "اكتشف مجموعتنا الحصرية",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onShopClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Accent,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text("تسوق الآن", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // بانرات Firestore — pager مع auto-play
            else -> {
                val pagerState = rememberPagerState(pageCount = { banners.size })

                LaunchedEffect(banners.size) {
                    if (banners.size <= 1) return@LaunchedEffect
                    while (true) {
                        delay(5000)
                        val next = (pagerState.currentPage + 1) % banners.size
                        pagerState.animateScrollToPage(next)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val b = banners[page]
                    Box(Modifier.fillMaxSize()) {
                        // صورة البانر
                        if (b.image.isNotBlank()) {
                            AsyncImage(
                                model = b.image,
                                contentDescription = b.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        
                        // gradient overlay: bg-gradient-to-l from-black/50 to-transparent
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.50f),
                                        )
                                    )
                                )
                        )
                        
                        // النص — توسيط رأسي فقط ومحاذاة للبداية أفقياً (يمين في RTL)
                        // مطابق للموقع: flex items-center px-5 (بدون justify-center) + max-w-xs
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column(
                                modifier = Modifier.widthIn(max = 320.dp),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                Text(
                                    text = b.title,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    lineHeight = 26.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (b.description.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = b.description,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = onShopClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Accent,
                                        contentColor = Color.White,
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp),
                                ) {
                                    Text(
                                        b.cta.ifBlank { "تسوق الآن" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                // Dots — أسفل المنتصف
                if (banners.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(banners.size) { i ->
                            val active = pagerState.currentPage == i
                            Box(
                                Modifier
                                    .height(6.dp)
                                    .width(if (active) 20.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) Accent else Color.White.copy(0.60f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  SECTION HEADER
//  - flex justify-between items-center px-4 pt-6 pb-3
//  - العنوان يسار (font Georgia مقارب bold)
//  - "عرض المزيد ←" يمين بلون Accent
// ══════════════════════════════════════════════════════════════

@Composable
private fun HomeSectionHeader(title: String, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // العنوان — يسار
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.onBackground,
        )
        // عرض المزيد ← — يمين
        Row(
            modifier = Modifier.clickable(onClick = onViewAll),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "عرض المزيد",
                color = Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  CATEGORIES ROW
//  - overflow-x-auto px-4, gap-4
//  - w-14 h-14 (56dp) rounded-full
//  - اسم التصنيف w-16 truncate
// ══════════════════════════════════════════════════════════════

@Composable
private fun CategoriesRow(
    categories: List<Category>,
    onCategoryClick: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(categories) { cat ->
            Column(
                modifier = Modifier
                    .clickable { onCategoryClick(cat.id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // w-14 h-14 rounded-full bg-secondary
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center,
                ) {
                    if (cat.image.isNotBlank()) {
                        AsyncImage(
                            model = cat.image,
                            contentDescription = cat.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            tint = MutedForeground,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                // text-xs font-medium text-center w-16 truncate
                Text(
                    text = cat.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.width(64.dp),
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  PRODUCT ROW
//  - overflow-x-auto pb-2 px-4, flex gap-3
//  - أول 3 منتجات فقط (slice 0,3)
//  - عرض البطاقة 160dp
// ══════════════════════════════════════════════════════════════

@Composable
private fun ProductRow(
    products: List<Product>,
    favoriteIds: Set<String>,
    onProductClick: (String) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onAddToCart: (Product) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(products.take(3)) { product ->
            HomeProductCard(
                product = product,
                isFavorite = product.id in favoriteIds,
                onFavoriteToggle = { onFavoriteToggle(product.id) },
                onAddToCart = { onAddToCart(product) },
                onClick = { onProductClick(product.id) },
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  HOME PRODUCT CARD — مطابق ProductCard.tsx
//  - rounded-xl border border-gray-100, shadow-sm
//  - صورة 1:1 aspect-square
//  - badge خصم أعلى يمين (top-2 right-2)
//  - زر مفضلة أعلى يسار (top-2 left-2) — rounded-lg bg-white/80
//  - معلومات المنتج: p-2.5 gap-1
//  - السعر يسار + زر سلة يمين (w-9 h-9 rounded-lg bg-accent)
// ══════════════════════════════════════════════════════════════

@Composable
private fun HomeProductCard(
    product: Product,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onAddToCart: () -> Unit,
    onClick: () -> Unit,
) {
    val discount = product.discountPercent
    val imageSrc = product.mainImage
    val outOfStock = product.stock <= 0

    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Border),
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

                // badge خصم — أعلى يمين (top-2 right-2)
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

                // زر مفضلة — أعلى يسار (top-2 left-2)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(
                            Color.White.copy(alpha = 0.80f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(onClick = onFavoriteToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Destructive else MutedForeground,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // ── معلومات المنتج — p-2.5 ──────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // اسم المنتج — font-bold text-xs line-clamp-1
                Text(
                    text = product.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // السعر + زر السلة — justify-between
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    // السعر — يسار
                    Column {
                        Text(
                            text = "${formatNum(product.price)} ج.س",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Accent,
                        )
                        if (discount != null && discount > 0 && product.originalPrice != null) {
                            Text(
                                text = "${formatNum(product.originalPrice)} ج.س",
                                fontSize = 12.sp,
                                color = MutedForeground,
                                textDecoration = TextDecoration.LineThrough,
                            )
                        }
                    }

                    // زر سلة — يمين: w-9 h-9 rounded-lg
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (outOfStock) MutedForeground.copy(alpha = 0.4f) else Accent,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable(enabled = !outOfStock, onClick = onAddToCart),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = "أضف للسلة",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  BRANDS ROW
//  - flex gap-3, w-24 h-24 rounded-xl border
// ══════════════════════════════════════════════════════════════

@Composable
private fun BrandsRow(brands: List<Brand>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(brands) { brand ->
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, Border, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (brand.logo.isNotBlank()) {
                    AsyncImage(
                        model = brand.logo,
                        contentDescription = brand.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                    )
                } else {
                    Text(
                        brand.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedForeground,
                    )
                }
            }
        }
    }
}

// ── مساعد تنسيق الأرقام ───────────────────────────────────────
internal fun formatNum(value: Any?): String {
    val d = when (value) {
        null -> return "0"
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    return if (d == d.toLong().toDouble()) d.toLong().toString()
    else "%.2f".format(d)
}