package com.eleven.store.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Image
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
import com.eleven.store.ui.components.ProductCard
import com.eleven.store.ui.components.ProductCardSkeleton
import com.eleven.store.ui.components.rememberInfiniteTransitionAlpha
import com.eleven.store.ui.theme.Accent
import com.eleven.store.ui.theme.Border
import com.eleven.store.ui.theme.Destructive
import com.eleven.store.ui.theme.Ink
import com.eleven.store.ui.theme.MutedForeground
import com.eleven.store.ui.theme.Neutral100
import com.eleven.store.ui.theme.Neutral200
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════
//  HOME SCREEN — نسخة طبق الأصل من Home.tsx في الموقع
// ═══════════════════════════════════════════════════════════════

// ══════════════════════════════════════════════════════════════
//  SECTION REVEAL
//  ✅ جديد: ظهور تدريجي (fade + slide من الأسفل قليلاً) لكل قسم بالصفحة
//  الرئيسية بدل ظهورها فجأة كلها دفعة واحدة — خصوصاً مباشرة بعد اختفاء
//  شاشة الترحيب (AppWelcomeOverlay بـMainActivity.kt)، فالانتقال يبان
//  متتابعاً ومقصوداً بدل "قفزة" واحدة من شاشة الترحيب لمحتوى كامل جامد.
//  تأخير بسيط متدرّج حسب ترتيب القسم (index) — أول قسم (البانر) يظهر شبه
//  فوري، وكل قسم بعده بفارق 60ms تقريباً، بحد أقصى معقول حتى لا تتأخر
//  الأقسام السفلية كثيراً لو المستخدم مرّر لأسفل بسرعة.
// ══════════════════════════════════════════════════════════════
@Composable
private fun SectionReveal(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index * 60L).coerceAtMost(240L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(
            animationSpec = tween(350),
            initialOffsetY = { it / 8 },
        ),
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onProductClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onViewAllClick: (String) -> Unit,
    // ✅ جديد: "عرض المزيد" بقسم التصنيفات صار يفتح شاشة تصنيفات/علامات
    // تجارية مخصّصة (شبكة بطاقات بصورة) بدل صفحة المنتجات العامة بلا فلتر
    // — تصفح بصري مباشر بدل الاعتماد على منسدل الفلاتر داخل شاشة المنتجات.
    onViewCategories: () -> Unit,
    // ✅ جديد: يُستدعى عند الضغط على بانر/زر بانر يحمل رابطاً (banner.link)
    // — مسار داخلي مثل "/product/xxx" أو "/category/xxx"، أو رابط خارجي كامل
    // (https://...). المنطق الفعلي لفكّ هذا الرابط (تنقّل داخلي أو فتح متصفح)
    // موجود بـNavGraph.kt حيث تتوفر navController وcontext معاً.
    onOpenLink: (String) -> Unit = {},
) {
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val bannersLoading by viewModel.bannersLoading.collectAsStateWithLifecycle()
    val bannersError by viewModel.bannersError.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoriesLoading by viewModel.categoriesLoading.collectAsStateWithLifecycle()
    val categoriesError by viewModel.categoriesError.collectAsStateWithLifecycle()
    val featuredProducts by viewModel.featuredProducts.collectAsStateWithLifecycle()
    val newArrivals by viewModel.newArrivals.collectAsStateWithLifecycle()
    val bestSellers by viewModel.bestSellers.collectAsStateWithLifecycle()
    val onSaleProducts by viewModel.onSaleProducts.collectAsStateWithLifecycle()
    val homeProductsLoading by viewModel.homeProductsLoading.collectAsStateWithLifecycle()
    val homeProductsError by viewModel.homeProductsError.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    // ✅ إعادة تصميم: لم تعد هناك حالة تحميل/خطأ واحدة تغطي الصفحة كلها —
    // كل قسم (بانر/تصنيفات/منتجات) مستقل تماماً (راجع loadHomeData بـ
    // MainViewModel). "فشل تام" (تُعرَض معه شاشة الخطأ الكاملة) يعني فعلياً
    // أن كل الأقسام الثلاثة فشلت معاً ولا يوجد أي محتوى ظاهر من قبل — أي فشل
    // جزئي (قسم واحد فقط) يُعرَض كرسالة صغيرة داخل قسمه فقط، بينما تبقى بقية
    // الصفحة تعمل بشكل طبيعي.
    val isLoading = bannersLoading || categoriesLoading || homeProductsLoading
    val hasAnyContent = banners.isNotEmpty() || categories.isNotEmpty() ||
        featuredProducts.isNotEmpty() || newArrivals.isNotEmpty() ||
        bestSellers.isNotEmpty() || onSaleProducts.isNotEmpty()
    val allSectionsFailed = bannersError != null && categoriesError != null && homeProductsError != null

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        // ── فشل تحميل فعلي (شبكة/سيرفر) ولا يوجد أي محتوى معروض أصلاً ──
        // ✅ لو فيه محتوى مسبق (من تحميل سابق ناجح) لا نُخفيه بسبب فشل
        // مؤقت لاحق — نعرض التنبيه فقط لو الصفحة فارغة تماماً بسببه.
        if (allSectionsFailed && !hasAnyContent && !isLoading) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MutedForeground,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "تعذّر تحميل الصفحة الرئيسية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "تحقق من اتصالك بالإنترنت وحاول مرة أخرى",
                        color = MutedForeground,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.loadHomeData() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                    ) {
                        Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── 1. Banner Slider — mx-4 mt-3 rounded-2xl h-200 ──────
        item {
            SectionReveal(index = 0) {
            BannerSlider(
                banners = banners,
                isLoading = bannersLoading,
                onShopClick = { onViewAllClick("") },
                // ✅ إصلاح: كان زر/بانر البانر يتجاهل banner.link تماماً ويذهب
                // دائماً لصفحة كل المنتجات، حتى لو كان الأدمن قد حدّد رابط منتج
                // أو صفحة معيّنة من لوحة التحكم — فيبدو للمستخدم وكأن "الرابط
                // لا يفتح". الآن: رابط محدَّد → onOpenLink، وإلا → onShopClick.
                onBannerLinkClick = { link -> onOpenLink(link) },
            )
            }
        }

        // ── فشل جزئي: تصنيفات/علامات فقط (باقي الصفحة تعمل بشكل طبيعي) ──
        if (categoriesError != null && categories.isEmpty() && !categoriesLoading) {
            item { SectionErrorRetry(categoriesError!!) { viewModel.loadCategoriesAndBrands() } }
        }

        // ── هيكل تحميل مؤقت للتصنيفات (بدل عدم ظهور القسم إطلاقاً لحد
        // وصول البيانات) — دوائر نابضة بنفس حجم/تباعد CategoriesRow الحقيقية
        // حتى لا "تقفز" الصفحة لأعلى/أسفل لحظة استبدال الهيكل بالمحتوى.
        if (categoriesLoading && categories.isEmpty()) {
            item {
                SectionReveal(index = 1) {
                    HomeSectionHeaderSkeleton()
                    CategoriesRowSkeleton()
                }
            }
        }

        // ── 2. التصنيفات — أول 5 فقط، بصف واحد ──────────────────
        if (categories.isNotEmpty()) {
            item {
                SectionReveal(index = 1) {
                HomeSectionHeader(title = "التصنيفات", onViewAll = onViewCategories)
                CategoriesRow(
                    categories = categories.take(5),
                    onCategoryClick = onCategoryClick,
                )
                }
            }
        }

        // ── فشل جزئي: أقسام المنتجات فقط (البانر/التصنيفات تعملان بشكل طبيعي) ──
        if (homeProductsError != null && !homeProductsLoading &&
            onSaleProducts.isEmpty() && featuredProducts.isEmpty() &&
            bestSellers.isEmpty() && newArrivals.isEmpty()
        ) {
            item { SectionErrorRetry(homeProductsError!!) { viewModel.loadHomeProducts() } }
        }

        // ── هيكل تحميل مؤقت لأقسام المنتجات — قسمين وهميين (بدل فراغ
        // كامل لحد وصول أول منتج) حتى يشعر المستخدم أن المحتوى "قادم"
        // فعلاً وليس أن الصفحة توقفت أو فشلت بصمت.
        if (homeProductsLoading &&
            onSaleProducts.isEmpty() && featuredProducts.isEmpty() &&
            bestSellers.isEmpty() && newArrivals.isEmpty()
        ) {
            item {
                SectionReveal(index = 2) {
                    HomeSectionHeaderSkeleton()
                    ProductRowSkeleton()
                }
            }
            item {
                SectionReveal(index = 3) {
                    HomeSectionHeaderSkeleton()
                    ProductRowSkeleton()
                }
            }
        }

        // ── 3. العروض والخصومات ─────────────────────────────────
        if (onSaleProducts.isNotEmpty()) {
            item {
                SectionReveal(index = 2) {
                HomeSectionHeader(title = "العروض والخصومات", onViewAll = { onViewAllClick("onSale") })
                ProductRow(
                    products = onSaleProducts,
                    favoriteIds = favoriteIds,
                    onProductClick = onProductClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                )
                }
            }
        }

        // ── 4. المنتجات المميزة ──────────────────────────────────
        if (featuredProducts.isNotEmpty()) {
            item {
                SectionReveal(index = 3) {
                HomeSectionHeader(title = "المنتجات المميزة", onViewAll = { onViewAllClick("featured") })
                ProductRow(
                    products = featuredProducts,
                    favoriteIds = favoriteIds,
                    onProductClick = onProductClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                )
                }
            }
        }

        // ── 5. الأكثر مبيعاً ────────────────────────────────────
        if (bestSellers.isNotEmpty()) {
            item {
                SectionReveal(index = 4) {
                HomeSectionHeader(title = "الأكثر مبيعاً", onViewAll = { onViewAllClick("bestSeller") })
                ProductRow(
                    products = bestSellers,
                    favoriteIds = favoriteIds,
                    onProductClick = onProductClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                )
                }
            }
        }

        // ── 6. المنتجات الجديدة ──────────────────────────────────
        if (newArrivals.isNotEmpty()) {
            item {
                SectionReveal(index = 5) {
                HomeSectionHeader(title = "المنتجات الجديدة", onViewAll = { onViewAllClick("new") })
                ProductRow(
                    products = newArrivals,
                    favoriteIds = favoriteIds,
                    onProductClick = onProductClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                )
                }
            }
        }

        // ── 7. العلامات التجارية ─────────────────────────────────
        if (brands.isNotEmpty()) {
            item {
                SectionReveal(index = 6) {
                // ✅ "عرض المزيد" هنا يفتح نفس شاشة التصنيفات/العلامات
                // التجارية الجديدة (مباشرة على قسم العلامات) بدل صفحة
                // المنتجات بفلتر "brands" المجرَّد — نفس شاشة تصفّح واحدة
                // لكل من التصنيفات والعلامات، بدل وجهتين مختلفتين لهما.
                HomeSectionHeader(title = "العلامات التجارية", onViewAll = onViewCategories)
                BrandsRow(brands = brands)
                Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
    }
}

// ══════════════════════════════════════════════════════════════
//  SECTION ERROR RETRY
//  ✅ جديد: رسالة صغيرة داخل قسم واحد فقط (تصنيفات أو منتجات) عند فشل
//  تحميله تحديداً، بينما بقية أقسام الصفحة (بانر مثلاً) تظهر بشكل طبيعي —
//  تفادياً لإخفاء الصفحة كلها بسبب فشل قسم واحد فقط.
// ══════════════════════════════════════════════════════════════
@Composable
private fun SectionErrorRetry(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Neutral100)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MutedForeground, modifier = Modifier.size(20.dp))
        Text(
            message,
            color = MutedForeground,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 10.dp).weight(1f),
        )
        Text(
            "إعادة المحاولة",
            color = Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onRetry),
        )
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
    onBannerLinkClick: (String) -> Unit = {},
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
            // حالة تحميل — نفس نبضة الـskeleton المستخدمة بباقي التطبيق
            // (بديل أنيق لدوّارة تحميل مجرّدة فوق خلفية ثابتة).
            isLoading && banners.isEmpty() -> {
                val alpha by com.eleven.store.ui.components.rememberInfiniteTransitionAlpha()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Neutral200.copy(alpha = alpha))
                )
            }

            // لا يوجد بانرات → واجهة ترحيبية أغنى بصرياً من الموقع (تدرّج
            // قطري + دوائر زخرفية خفيفة) بدل تدرّج أفقي مسطّح — أول شيء
            // يراه مستخدم متجر جديد بلا بانرات مُعدّة من لوحة التحكم بعد،
            // فمن المهم ألا يبدو كخلفية فارغة/بديلة واضحة.
            banners.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Ink, Color(0xFF1E293B), Accent),
                            )
                        )
                ) {
                    // دوائر زخرفية شفافة — إحساس "علامة تجارية" بدل مسطح تماماً
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 40.dp, y = (-40).dp)
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 20.dp, y = 30.dp)
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
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
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "اكتشف مجموعتنا الحصرية",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onShopClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Ink,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp),
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
                    // ✅ إصلاح: رابط البانر (إن وُجد) هو الوجهة الفعلية عند
                    // الضغط — سواء بالضغط على الصورة نفسها أو زر الـCTA؛ يذهب
                    // لصفحة كل المنتجات فقط إن كان البانر بلا رابط محدَّد أصلاً.
                    val onTap: () -> Unit = {
                        if (b.link.isNotBlank()) onBannerLinkClick(b.link) else onShopClick()
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable(onClick = onTap)
                    ) {
                        // صورة البانر
                        if (b.image.isNotBlank()) {
                            AsyncImage(
                                model = b.image,
                                contentDescription = b.title,
                                contentScale = ContentScale.Crop,
                                error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Image),
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
                                    onClick = onTap,
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
//  SKELETONS — التصنيفات/المنتجات (هيكل تحميل مؤقت لحد وصول البيانات)
//  ✅ جديد: نفس أبعاد/تباعد الصفوف الحقيقية (CategoriesRow/ProductRow)
//  تماماً حتى لا تتحرك الصفحة رأسياً لحظة استبدال الهيكل بالمحتوى الفعلي.
// ══════════════════════════════════════════════════════════════

@Composable
private fun HomeSectionHeaderSkeleton() {
    val alpha by rememberInfiniteTransitionAlpha()
    val tone = Neutral200.copy(alpha = alpha)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(120.dp).height(16.dp).background(tone, RoundedCornerShape(4.dp)))
        Box(Modifier.width(60.dp).height(14.dp).background(tone, RoundedCornerShape(4.dp)))
    }
}

@Composable
private fun CategoriesRowSkeleton() {
    val alpha by rememberInfiniteTransitionAlpha()
    val tone = Neutral200.copy(alpha = alpha)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(5) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(tone))
                Box(Modifier.width(44.dp).height(10.dp).background(tone, RoundedCornerShape(4.dp)))
            }
        }
    }
}

@Composable
private fun ProductRowSkeleton() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(3) {
            ProductCardSkeleton(modifier = Modifier.width(160.dp))
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
    // ✅ حجم/تباعد أضيق قليلاً (52dp بدل 56dp، تباعد 12dp بدل 16dp) ليتّسع
    // صف واحد لـ5 تصنيفات على معظم الشاشات بأقل سحب أفقي ممكن
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(categories, key = { it.id }) { cat ->
            Column(
                modifier = Modifier
                    .clickable { onCategoryClick(cat.id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // w-14 h-14 rounded-full bg-secondary
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center,
                ) {
                    if (cat.image.isNotBlank()) {
                        AsyncImage(
                            model = cat.image,
                            contentDescription = cat.name,
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Image),
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
                    modifier = Modifier.width(60.dp),
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  PRODUCT ROW
//  - overflow-x-auto pb-2 px-4, flex gap-3
//  - ✅ حتى 8 منتجات (بدل 3 سابقاً) مع سحب أفقي لعرض المزيد — الاكتفاء
//    بـ3 كان يُخفي أغلب الـ10 منتجات المجلوبة أصلاً لكل قسم بلا داعٍ طالما
//    أن السحب الأفقي متاح بالفعل بهذا الصف
//  - عرض البطاقة 160dp — نفس بطاقة صفحة المنتجات (ProductCard الموحّدة)
// ══════════════════════════════════════════════════════════════

@Composable
private fun ProductRow(
    products: List<Product>,
    favoriteIds: Set<String>,
    onProductClick: (String) -> Unit,
    onFavoriteToggle: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(products.take(8), key = { it.id }) { product ->
            ProductCard(
                product = product,
                isFavorite = product.id in favoriteIds,
                onFavoriteToggle = { onFavoriteToggle(product.id) },
                onClick = { onProductClick(product.id) },
                modifier = Modifier.width(160.dp),
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  BRANDS ROW
//  ✅ إعادة تصميم: 4 بطاقات فقط تملأ عرض الشاشة بالتساوي (weight بدل حجم
//  ثابت + LazyRow قابل للسحب) — متجاورة ومقفلة بفراغ صغير موحّد (8dp) بدل
//  الفراغات الكبيرة السابقة (12dp بين بطاقات 96dp ثابتة العرض)
// ══════════════════════════════════════════════════════════════

@Composable
private fun BrandsRow(brands: List<Brand>) {
    // ✅ إصلاح: Row العادي بالتصميم السابق كان يقصّ العلامات على أول 4 نهائياً
    // (weight بيملأ العرض بالضبط بلا سحب) — أي علامة إضافية لا تظهر إطلاقاً
    // ولا طريقة للوصول لها. رجعناها LazyRow قابلة للسحب، لكن بعرض بطاقة ثابت
    // (80dp) يُحاكي "4 بطاقات تقريباً بالشاشة متجاورة بفراغ صغير" على معظم
    // الهواتف، مع بقاء بقية العلامات (لو أكثر من 4) قابلة للوصول بالسحب.
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(brands, key = { it.id }) { brand ->
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, Border, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (brand.logo.isNotBlank()) {
                    AsyncImage(
                        model = brand.logo,
                        contentDescription = brand.name,
                        contentScale = ContentScale.Fit,
                        error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Image),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                    )
                } else {
                    Text(
                        brand.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

// ── مساعد تنسيق الأرقام ───────────────────────────────────────
// ملاحظة: تنسيق الأرقام أصبح موحداً عبر formatNumber()/formatPrice() في ScreenCommon.kt