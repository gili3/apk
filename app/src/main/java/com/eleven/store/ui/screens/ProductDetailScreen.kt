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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleven.store.ui.theme.Accent
import com.eleven.store.ui.theme.Border
import com.eleven.store.ui.theme.Destructive
import com.eleven.store.ui.theme.MutedForeground
import com.eleven.store.ui.theme.Warning
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════
//  دالة مساعدة لتنسيق الأسعار — مطابقة لـ formatNumber في الموقع
// ═══════════════════════════════════════════════════════════════
private fun formatPriceDetail(value: Any?): String {
    val d = when (value) {
        is Double -> value
        is Long -> value.toDouble()
        is Int -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    return if (d == d.toLong().toDouble()) d.toLong().toString()
    else "%.2f".format(d)
}

// ═══════════════════════════════════════════════════════════════
//  PRODUCT DETAIL SCREEN — نسخة طبق الأصل من ProductDetail.tsx
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: MainViewModel,
    productId: String,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    onBuyNow: ((productId: String, quantity: Int) -> Unit)? = null,
    onGoToProducts: () -> Unit = onBack,
) {
    val product by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val isProductLoading by viewModel.isProductLoading.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // ── مشاركة رابط المنتج — مطابقة لـ handleShare في الموقع ──
    fun shareProduct() {
        val baseUrl = storeSettings.websiteUrl.ifBlank { "https://eleven-sd.com" }.trimEnd('/')
        val link = "$baseUrl/product/$productId"

        // نسخ الرابط للحافظة — نفس سلوك navigator.clipboard.writeText بالموقع
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("رابط المنتج", link))

        // مشاركة النظام كخيار إضافي (تحسين للجوال)
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, link)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة المنتج"))

        coroutineScope.launch { snackbarHostState.showSnackbar("تم نسخ رابط المنتج") }
    }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // زر الرجوع
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ChevronLeft,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    // اسم المنتج في المنتصف
                    Text(
                        text = product?.name ?: "المنتج",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // زر المشاركة
                    IconButton(onClick = { shareProduct() }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "مشاركة",
                            tint = Accent,
                        )
                    }

                    // زر المفضلة
                    IconButton(onClick = { viewModel.toggleFavorite(productId) }) {
                        val isFav = productId in favoriteIds
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFav) Destructive
                            else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                HorizontalDivider(
                    color = Border,
                    thickness = 1.dp,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        },
        bottomBar = {
            if (product != null) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                ) {
                    Column {
                        HorizontalDivider(color = Border, thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            val outOfStock = (product?.stock ?: 0) <= 0

                            // زر "أضف للسلة" — يظهر أولاً في RTL (يمين)
                            Button(
                                onClick = {
                                    product?.let { p ->
                                        viewModel.addToCart(p, 1)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("تمت الإضافة إلى السلة 🛒")
                                        }
                                    }
                                },
                                enabled = !outOfStock,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    contentColor = MaterialTheme.colorScheme.background,
                                ),
                            ) {
                                Icon(
                                    Icons.Filled.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    if (outOfStock) "نفذت الكمية" else "أضف للسلة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                )
                            }

                            // زر "شراء الآن"
                            Button(
                                onClick = {
                                    product?.let { p ->
                                        onBuyNow?.invoke(p.id, 1)
                                    }
                                },
                                enabled = !outOfStock,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accent,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text(
                                    "شراء الآن",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            // ── حالة التحميل — مطابقة للموقع ──
            product == null && isProductLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(
                            color = Accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            "جاري تحميل المنتج...",
                            color = MutedForeground,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // ── المنتج غير موجود — مطابقة للموقع ──
            product == null && !isProductLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            "المنتج غير موجود",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "عذراً، لم نتمكن من العثور على هذا المنتج",
                            color = MutedForeground,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onGoToProducts,
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                "العودة للمتجر",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // ── تفاصيل المنتج ──
            else -> {
                val p = product!!
                val images = p.images.filter { it.isNotBlank() }
                    .ifEmpty { listOfNotNull(p.image.takeIf { it.isNotBlank() }) }
                val pagerState = rememberPagerState(pageCount = { images.size.coerceAtLeast(1) })

                val discount = if (p.originalPrice != null && p.originalPrice!! > p.price) {
                    (((p.originalPrice!! - p.price) / p.originalPrice!!) * 100).toInt()
                } else {
                    p.discountPercent?.toInt() ?: 0
                }

                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    // ── معرض الصور — مطابق للموقع: h-[380px] ──
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            if (images.isNotEmpty()) {
                                HorizontalPager(state = pagerState) { page ->
                                    AsyncImage(
                                        model = images[page],
                                        contentDescription = p.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }

                            // مصغرات الصور — أسفل المنتصف
                            if (images.size > 1) {
                                LazyRow(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                ) {
                                    items(images.indices.toList()) { i ->
                                        val isSelected = pagerState.currentPage == i
                                        val scope = rememberCoroutineScope()
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.dp,
                                                    color = if (isSelected)
                                                        MaterialTheme.colorScheme.onBackground
                                                    else
                                                        Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp),
                                                )
                                                .clickable {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(i)
                                                    }
                                                },
                                        ) {
                                            AsyncImage(
                                                model = images[i],
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(8.dp)),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── معلومات المنتج — px-6 py-6 ──
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                        ) {
                            // ── شارة الحالة — مطابقة للموقع ──
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                val badgeLabel = when {
                                    p.isNew -> "جديد"
                                    p.isOnSale -> "عرض خاص"
                                    p.isBestSeller -> "الأكثر مبيعاً"
                                    else -> "مميز"
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Destructive, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        badgeLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // ── اسم المنتج — مطابق للموقع: text-2xl font-bold Georgia ──
                            Text(
                                text = p.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                lineHeight = 32.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )

                            Spacer(Modifier.height(12.dp))

                            // ── السعر — مطابق للموقع ──
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = "${formatPriceDetail(p.price)} ج.س",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Accent,
                                )
                                if (p.originalPrice != null && p.originalPrice!! > p.price) {
                                    Text(
                                        text = "${formatPriceDetail(p.originalPrice)} ج.س",
                                        fontSize = 18.sp,
                                        color = MutedForeground,
                                        textDecoration = TextDecoration.LineThrough,
                                    )
                                    if (discount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .background(Accent, RoundedCornerShape(4.dp))
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
                                }
                            }

                            Spacer(Modifier.height(20.dp))
                            HorizontalDivider(color = Border, thickness = 1.dp)
                            Spacer(Modifier.height(20.dp))

                            // ── الوصف — مطابق للموقع ──
                            Text(
                                "الوصف",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = p.description.ifBlank {
                                    "تتميز هذه القطعة بتصميم أنيق وعصري يتناسب مع جميع الأذواق."
                                },
                                color = MutedForeground,
                                lineHeight = 26.sp,
                                fontSize = 14.sp,
                            )

                            Spacer(Modifier.height(20.dp))
                            HorizontalDivider(color = Border, thickness = 1.dp)
                            Spacer(Modifier.height(20.dp))

                            // ── التوفر — مطابق للموقع ──
                            Text(
                                "التوفر",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(8.dp))
                            if ((p.stock ?: 0) > 0) {
                                val stockLow = (p.stock ?: 0) <= 5
                                Text(
                                    text = "متوفر في المخزون" +
                                            if (stockLow) " (بقي ${p.stock} فقط)" else "",
                                    fontSize = 14.sp,
                                    color = if (stockLow) Warning else MutedForeground,
                                    fontWeight = if (stockLow) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            } else {
                                Text(
                                    text = "نفذت الكمية من المخزون",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Destructive,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}