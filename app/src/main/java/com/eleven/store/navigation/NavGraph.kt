package com.eleven.store.navigation

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
import com.eleven.store.ui.screens.*
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
    NavHost(
        navController = navController,
        startDestination = Route.HOME,
    ) {
        composable(Route.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onProductClick = { navController.navigate(Route.productDetail(it)) },
                onCategoryClick = { navController.navigate("${Route.PRODUCTS}?category=$it") },
                onViewAllClick = { filter ->
                    navController.navigate("${Route.PRODUCTS}?filter=$filter")
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
            androidx.compose.runtime.LaunchedEffect(productId) { viewModel.loadProduct(productId) }
            val p = product
            if (p != null) {
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
            )
        }
        composable(Route.NOTIFICATIONS) {
            NotificationsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
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
