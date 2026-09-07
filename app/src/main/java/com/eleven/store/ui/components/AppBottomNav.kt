package com.eleven.store.ui.components

// ✅ فُصل من MainActivity.kt — راجع AppHeader.kt لنفس السبب.

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.eleven.store.navigation.BottomNavItem
import com.eleven.store.navigation.Route
import com.eleven.store.ui.icons.LucideIcons
import com.eleven.store.ui.theme.*

// ═══════════════════════════════════════════════════════════════
//  BOTTOM NAV — مطابق تماماً لـ BottomNav.tsx
//  الأيقونات المستخدمة:
//  - الرئيسية: LucideIcons.Home (مطابق حرفياً لـ Home من lucide-react)
//  - المنتجات: LucideIcons.ShoppingBag (مطابق حرفياً لـ ShoppingBag من lucide-react)
//  - المفضلة: LucideIcons.Heart (مطابق حرفياً لـ Heart من lucide-react)
//  - السلة: LucideIcons.ShoppingCart (مطابق حرفياً لـ ShoppingCart من lucide-react)
//  - الملف: LucideIcons.User (مطابق حرفياً لـ User من lucide-react)
// ═══════════════════════════════════════════════════════════════

@Composable
fun ElevenBottomNav(
    navController: NavController,
    cartCount: Int,
) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // إخفاء في شاشات معينة
    val hideRoutes = listOf(Route.LOGIN, Route.REGISTER, Route.CHECKOUT)
    val hidePatterns = listOf("product/", "order/")
    if (hideRoutes.any { currentRoute == it }) return
    if (hidePatterns.any { currentRoute?.startsWith(it) == true }) return

    val unselectedColor = MutedForeground

    Surface(
        shadowElevation = 8.dp,
        color = Color.White,
    ) {
        Column {
            HorizontalDivider(color = Border, thickness = 1.dp)

            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(76.dp) // زيادة طفيفة عن 72dp لاستيعاب تكبير الأيقونات والنص
                    .navigationBarsPadding(),
            ) {
                BottomNavItem.entries.forEach { item ->
                    val selected = when (item) {
                        BottomNavItem.HOME      -> currentRoute == Route.HOME
                        BottomNavItem.PRODUCTS  -> currentRoute?.startsWith(Route.PRODUCTS) == true
                        BottomNavItem.FAVORITES -> currentRoute == Route.FAVORITES
                        BottomNavItem.CART      -> currentRoute == Route.CART
                        BottomNavItem.PROFILE   -> currentRoute == Route.PROFILE
                    }

                    // ✅ نفس أيقونات lucide المستخدمة في الموقع حرفياً (وليس Material
                    // Icons التي تبدو شبيهة لكنها مختلفة بالخط والتناسب)
                    val icon: ImageVector = when (item) {
                        BottomNavItem.HOME      -> LucideIcons.Home
                        BottomNavItem.PRODUCTS  -> LucideIcons.ShoppingBag
                        BottomNavItem.FAVORITES -> LucideIcons.Heart
                        BottomNavItem.CART      -> LucideIcons.ShoppingCart
                        BottomNavItem.PROFILE   -> LucideIcons.User
                    }

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(item.route) {
                                    popUpTo(Route.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    // ✅ عداد السلة أحمر — مطابق لـ bg-destructive
                                    if (item == BottomNavItem.CART && cartCount > 0) {
                                        Badge(containerColor = Destructive) {
                                            Text(
                                                if (cartCount > 99) "99+"
                                                else cartCount.toString(),
                                            )
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = item.labelAr,
                                    // ✅ الموقع يستخدم text-primary للعنصر النشط (سلة رمادية مزرقّة)
                                    // وليس accent (نحاسي) — كانت هذه نقطة الاختلاف في اللون
                                    tint = if (selected) Primary else unselectedColor,
                                    modifier = Modifier.size(26.dp), // تكبير طفيف عن 24dp (w-6 h-6) ليطابق الحجم الفعلي بالموقع
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.labelAr,
                                fontSize = 13.sp, // تكبير طفيف عن 12.sp ليطابق الموقع
                                fontWeight = if (selected) FontWeight.SemiBold
                                else FontWeight.Normal,
                                color = if (selected) Primary else unselectedColor,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = unselectedColor,
                            unselectedTextColor = unselectedColor,
                        ),
                    )
                }
            }
        }
    }
}
