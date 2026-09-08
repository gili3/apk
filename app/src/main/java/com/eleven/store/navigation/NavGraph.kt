package com.eleven.store.navigation

import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleven.store.ui.components.ElevenButton
import com.eleven.store.ui.components.ElevenTopBar
import com.eleven.store.ui.screens.*
import com.eleven.store.ui.theme.Accent
import com.eleven.store.ui.theme.MutedForeground
import com.eleven.store.ui.viewmodel.MainViewModel

// ─── Route constants ────────────────────────────────────────────
object Route {
    const val HOME           = "home"
    const val PRODUCTS       = "products"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val CART           = "cart"
    const val FAVORITES      = "favorites"
    const val PROFILE        = "profile"
    const val ORDERS         = "orders"
    const val ORDER_DETAIL   = "order/{orderId}"
    const val CHECKOUT       = "checkout"
    const val BUY_NOW        = "buynow/{productId}/{quantity}"
    const val LOGIN          = "login"
    const val REGISTER       = "register"
    const val SETTINGS       = "settings"
    const val NOTIFICATIONS  = "notifications"
    const val ABOUT          = "about"
    const val CONTACT        = "contact"

    fun productDetail(id: String) = "product/$id"
    fun orderDetail(id: String) = "order/$id"
    fun buyNow(productId: String, quantity: Int) = "buynow/$productId/$quantity"
}

// Bottom nav items — matches BottomNav.tsx exactly
enum class BottomNavItem(
    val route: String,
    val labelAr: String,
    val iconResName: String,
) {
    HOME(Route.HOME, "الرئيسية", "home"),
    PRODUCTS(Route.PRODUCTS, "المنتجات", "shopping_bag"),
    FAVORITES(Route.FAVORITES, "المفضلة", "favorite"),
    CART(Route.CART, "السلة", "shopping_cart"),
    PROFILE(Route.PROFILE, "الملف", "person"),
}

@Composable
fun ElevenNavGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
) {
    // ✅ تحسين استجابة التنقل: انتقال سريع (180ms) بدل القفزة الفجائية
    // بدون أنيميشن — يخلي التنقل يحس أسرع وأنعم بدل ما يبين متقطّع.
    val fastFade = tween<Float>(durationMillis = 180)
    val fastSlide = tween<IntOffset>(durationMillis = 180)

    NavHost(
        navController = navController,
        startDestination = Route.HOME,
        enterTransition = {
            slideInHorizontally(fastSlide, initialOffsetX = { it / 6 }) + fadeIn(fastFade)
        },
        exitTransition = {
            fadeOut(fastFade)
        },
        popEnterTransition = {
            fadeIn(fastFade)
        },
        popExitTransition = {
            slideOutHorizontally(fastSlide, targetOffsetX = { it / 6 }) + fadeOut(fastFade)
        },
    ) {
        composable(Route.HOME) {
            val context = androidx.compose.ui.platform.LocalContext.current
            HomeScreen(
                viewModel = viewModel,
                onProductClick = { navController.navigate(Route.productDetail(it)) },
                // ✅ إصلاح: ترميز القيمة قبل دمجها بمسار التنقل — بدون هذا، أي
                // قيمة تحتوي مسافة أو رمز &/#/% تكسر تحليل معاملات الاستعلام.
                onCategoryClick = {
                    navController.navigate("${Route.PRODUCTS}?category=${android.net.Uri.encode(it)}")
                },
                onViewAllClick = { filter ->
                    navController.navigate("${Route.PRODUCTS}?filter=${android.net.Uri.encode(filter)}")
                },
                // ✅ جديد: فكّ رابط البانر (banner.link من لوحة التحكم) — رابط
                // خارجي كامل (http/https) يُفتح بمتصفح خارجي، وأي شيء آخر
                // يُعامَل كمسار داخلي للتطبيق (بعد إزالة الشرطة الأولى إن
                // وُجدت، بنفس تحويل actionRoute بالإشعارات) فيُفتح مباشرة
                // بنفس شاشات المنتج/التصنيف/إلخ دون أي خطوة إضافية.
                onOpenLink = { link ->
                    if (link.startsWith("http://") || link.startsWith("https://")) {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link))
                            )
                        } catch (_: Exception) {
                            // رابط خارجي غير صالح/بلا تطبيق يفتحه — نتجاهل بأمان
                            // بدل تعطّل التطبيق.
                        }
                    } else {
                        val route = link.removePrefix("/")
                        if (route.isNotBlank()) {
                            try {
                                navController.navigate(route)
                            } catch (_: Exception) {
                                // مسار غير معروف بالتطبيق (مثال: رابط يخص الموقع فقط
                                // وليس له مقابل بالتطبيق) — نتجاهل بأمان بدل تعطّل
                                // التطبيق بالكامل بسبب رابط أدخله الأدمن بالخطأ.
                            }
                        }
                    }
                },
            )
        }
        composable(
            route = "${Route.PRODUCTS}?category={category}&filter={filter}&search={search}",
            arguments = listOf(
                navArgument("category") { defaultValue = ""; nullable = true },
                navArgument("filter") { defaultValue = ""; nullable = true },
                navArgument("search") { defaultValue = ""; nullable = true },
            )
        ) { back ->
            ProductsScreen(
                viewModel = viewModel,
                initialCategory = back.arguments?.getString("category") ?: "",
                initialFilter = back.arguments?.getString("filter") ?: "",
                initialSearch = back.arguments?.getString("search") ?: "",
                onProductClick = { navController.navigate(Route.productDetail(it)) },
                onBack = { navController.popBackStack() },
                onGoHome = { navController.navigate(Route.HOME) { popUpTo(Route.HOME) { inclusive = true } } },
            )
        }
        composable(
            route = Route.PRODUCT_DETAIL,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { back ->
            val productId = back.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                viewModel = viewModel,
                productId = productId,
                onBack = { navController.popBackStack() },
                onCartClick = { navController.navigate(Route.CART) },
                // ✅ شراء الآن: ينتقل لـ CheckoutScreen مع المنتج مباشرة
                onBuyNow = { pid, qty -> navController.navigate(Route.buyNow(pid, qty)) },
                onGoToProducts = {
                    navController.navigate(Route.PRODUCTS) { popUpTo(Route.PRODUCT_DETAIL) { inclusive = true } }
                },
            )
        }
        composable(Route.CART) {
            CartScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCheckout = { navController.navigate(Route.CHECKOUT) },
                onProductClick = { navController.navigate(Route.productDetail(it)) },
            )
        }

        // ✅ شراء الآن — CheckoutScreen مستقل عن السلة
        composable(
            route = Route.BUY_NOW,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("quantity")  { type = NavType.IntType },
            )
        ) { back ->
            val productId = back.arguments?.getString("productId") ?: ""
            val quantity  = back.arguments?.getInt("quantity") ?: 1
            // نجلب المنتج ونبني CartItem هنا ونمرره لـ CheckoutScreen
            val product by viewModel.selectedProduct.collectAsStateWithLifecycle()
            val isLoadingProduct by viewModel.isProductLoading.collectAsStateWithLifecycle()
            // ✅ إصلاح: لا نعيد تحميل المنتج من الشبكة لو كان محمَّلاً أصلاً بنفس
            // المعرّف (المستخدم قادم للتو من صفحة تفاصيل نفس المنتج مثلاً) —
            // كان يُعاد التحميل دائماً حتى لو كانت البيانات جاهزة في selectedProduct.
            androidx.compose.runtime.LaunchedEffect(productId) {
                if (viewModel.selectedProduct.value?.id != productId) {
                    viewModel.loadProduct(productId)
                }
            }
            val p = product
            if (isLoadingProduct) {
                Scaffold(
                    topBar = { ElevenTopBar(title = "إتمام الطلب", onBack = { navController.popBackStack() }) },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent, strokeWidth = 3.dp)
                    }
                }
            } else if (p == null) {
                // ✅ المنتج غير موجود/محذوف — رسالة واضحة بدل شاشة فارغة تماماً
                Scaffold(
                    topBar = { ElevenTopBar(title = "إتمام الطلب", onBack = { navController.popBackStack() }) },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Icon(Icons.Filled.ErrorOutline, null, modifier = Modifier.size(56.dp), tint = MutedForeground)
                            Text(
                                "هذا المنتج غير متاح، قد يكون قد حُذف أو نفدت كميته",
                                textAlign = TextAlign.Center,
                                color = MutedForeground,
                            )
                            ElevenButton(text = "العودة", onClick = { navController.popBackStack() })
                        }
                    }
                }
            } else {
                val buyNowItem = com.eleven.store.data.model.CartItem(
                    id = p.id, productId = p.id, name = p.name,
                    price = p.price, quantity = quantity, image = p.mainImage,
                )
                CheckoutScreen(
                    viewModel = viewModel,
                    buyNowItems = listOf(buyNowItem),
                    onBack = { navController.popBackStack() },
                    onOrderPlaced = { orderId ->
                        navController.navigate(Route.orderDetail(orderId)) {
                            popUpTo(Route.BUY_NOW) { inclusive = true }
                        }
                    },
                )
            }
        }
        composable(Route.FAVORITES) {
            FavoritesScreen(
                viewModel = viewModel,
                onProductClick = { navController.navigate(Route.productDetail(it)) },
                onNavigateToProducts = {
                    navController.navigate(Route.PRODUCTS) { popUpTo(Route.FAVORITES) { inclusive = true } }
                },
                onNavigateToLogin = { navController.navigate(Route.LOGIN) },
            )
        }
        composable(Route.PROFILE) {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateToOrders = { navController.navigate(Route.ORDERS) },
                onNavigateToSettings = { navController.navigate(Route.SETTINGS) },
                onNavigateToLogin = { navController.navigate(Route.LOGIN) },
            )
        }
        composable(Route.ORDERS) {
            OrdersScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOrderClick = { navController.navigate(Route.orderDetail(it)) },
                onStartShopping = { navController.navigate(Route.PRODUCTS) },
                onNavigateToLogin = { navController.navigate(Route.LOGIN) },
            )
        }
        composable(
            route = Route.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { back ->
            val orderId = back.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(
                viewModel = viewModel,
                orderId = orderId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.CHECKOUT) {
            CheckoutScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOrderPlaced = { orderId ->
                    navController.navigate(Route.orderDetail(orderId)) {
                        popUpTo(Route.CART) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate(Route.REGISTER) },
            )
        }
        composable(Route.REGISTER) {
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(Route.LOGIN) { popUpTo(Route.REGISTER) { inclusive = true } } },
            )
        }
        composable(Route.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(Route.LOGIN) },
            )
        }
        composable(Route.NOTIFICATIONS) {
            NotificationsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenRoute = { route -> navController.navigate(route) },
            )
        }
        composable(Route.ABOUT) {
            AboutScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Route.CONTACT) {
            ContactScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
