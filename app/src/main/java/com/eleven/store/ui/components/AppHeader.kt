package com.eleven.store.ui.components

// ✅ فُصل هذا الملف من MainActivity.kt (كان يحتوي 776 سطراً) — الهيدر
// وحده منطق UI مستقل تماماً لا علاقة له بدورة حياة Activity، فنقله هنا
// يسهّل الصيانة ويقلّل حجم MainActivity لسطور تركيب التطبيق فقط.

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.eleven.store.navigation.Route
import com.eleven.store.ui.icons.LucideIcons
import com.eleven.store.ui.theme.*

// ═══════════════════════════════════════════════════════════════
//  HEADER — مطابق تماماً لـ Header.tsx
//  الأيقونات المستخدمة:
//  - Menu: LucideIcons.Menu (مطابق حرفياً لـ Menu من lucide-react)
//  - Cart: LucideIcons.ShoppingCart (مطابق حرفياً لـ ShoppingCart من lucide-react)
//  - Notifications: LucideIcons.Bell (مطابق حرفياً لـ Bell من lucide-react)
//  - Favorites: LucideIcons.Heart (مطابق حرفياً لـ Heart من lucide-react)
//  - Search: LucideIcons.Search (مطابق حرفياً لـ Search من lucide-react)
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElevenHeader(
    navController: NavController,
    cartCount: Int,
    hasUser: Boolean,
    unreadCount: Int,
    onMenuClick: () -> Unit,
    onCartClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSearchSubmit: (String) -> Unit,
) {
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // إخفاء الهيدر في شاشات معينة
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val hideRoutes = listOf(Route.LOGIN, Route.REGISTER)
    val hidePatterns = listOf("product/", "order/")
    if (hideRoutes.any { currentRoute == it }) return
    if (hidePatterns.any { currentRoute?.startsWith(it) == true }) return

    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        }
    }

    Column(modifier = Modifier.background(Background)) {
        Surface(
            color = Background.copy(alpha = 0.95f),
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // ═══ RIGHT: Menu + Cart ═══
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // مطابق لـ gap-3 (12px) في الموقع على مقاس الموبايل
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // ✅ Menu — مطابق لـ <Menu className="w-5 h-5" />
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            LucideIcons.Menu,
                            contentDescription = "القائمة",
                            tint = Foreground,
                            modifier = Modifier.size(22.dp), // w-5 h-5 = 22dp (تكبير طفيف)
                        )
                    }

                    // ✅ Cart مع عداد — مطابق لـ <ShoppingCart className="w-5 h-5" />
                    Box {
                        IconButton(onClick = onCartClick) {
                            Icon(
                                LucideIcons.ShoppingCart,
                                contentDescription = "السلة",
                                tint = Foreground,
                                modifier = Modifier.size(22.dp), // w-5 h-5 = 22dp (تكبير طفيف)
                            )
                        }
                        if (cartCount > 0) {
                            // ملاحظة: الموقع يضع الشارة فعلياً في أعلى يمين الأيقونة
                            // (className "-top-2 -right-2" ثابتة فيزيائياً ولا تتأثر بـ RTL)
                            // لذا نستخدم TopStart لأن "Start" في RTL = يمين فعلي
                            Badge(
                                containerColor = Accent,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-4).dp, y = 0.dp),
                            ) {
                                Text(
                                    text = if (cartCount > 99) "99+" else cartCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // ═══ CENTER: Logo 11/ELEVEN ═══
                Box(
                    modifier = Modifier.clickable {
                        navController.navigate(Route.HOME) {
                            popUpTo(Route.HOME) { inclusive = true }
                        }
                    },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            // مطابق لـ text-2xl (24px) في الموقع على مقاس الموبايل
                            text = "11",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 24.sp,
                            lineHeight = 24.sp,
                        )
                        Text(
                            // مطابق لـ text-xs (12px) tracking-widest في الموقع
                            text = "ELEVEN",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            fontSize = 12.sp,
                        )
                    }
                }

                // ═══ LEFT: Notifications + Favorites + Search ═══
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // مطابق لـ gap-3 (12px) في الموقع على مقاس الموبايل
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // ✅ Notifications — مطابق لـ <Bell className="w-5 h-5" />
                    if (hasUser) {
                        Box {
                            IconButton(onClick = onNotificationsClick) {
                                Icon(
                                    LucideIcons.Bell,
                                    contentDescription = "الإشعارات",
                                    tint = Foreground,
                                    modifier = Modifier.size(22.dp), // w-5 h-5 = 22dp (تكبير طفيف)
                                )
                            }
                            if (unreadCount > 0) {
                                // نفس السبب: الموقع يستخدم "-top-0.5 -right-0.5" فيزيائياً
                                // ولون bg-accent (وليس أحمر) — مطابق لهيدر الموقع
                                Badge(
                                    containerColor = Accent,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(x = 2.dp, y = 0.dp),
                                ) {
                                    Text(
                                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    // ✅ Search — مطابق لـ <Search className="w-5 h-5" />
                    IconButton(
                        onClick = {
                            isSearchOpen = !isSearchOpen
                            if (!isSearchOpen) {
                                searchText = ""
                                focusManager.clearFocus()
                            }
                        },
                    ) {
                        Icon(
                            // مطابق لـ <X /> من lucide (خط رفيع outline)، وليس X مصمت
                            imageVector = if (isSearchOpen) LucideIcons.Close
                            else LucideIcons.Search,
                            contentDescription = "بحث",
                            tint = Foreground,
                            modifier = Modifier.size(22.dp), // w-5 h-5 = 22dp (تكبير طفيف)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Border, thickness = 1.dp)

        // ═══ Search Bar ═══
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Background)
                        .padding(12.dp),
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = {
                            Text(
                                "ابحث عن منتجات...",
                                color = MutedForeground,
                                fontSize = 14.sp,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Right,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Border,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (searchText.isNotBlank()) {
                                        onSearchSubmit(searchText.trim())
                                        searchText = ""
                                        isSearchOpen = false
                                    }
                                },
                            ) {
                                Icon(
                                    LucideIcons.Search,
                                    contentDescription = "بحث",
                                    tint = MutedForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchText.isNotBlank()) {
                                    onSearchSubmit(searchText.trim())
                                    searchText = ""
                                    isSearchOpen = false
                                }
                            },
                        ),
                    )
                }
                HorizontalDivider(color = Border, thickness = 1.dp)
            }
        }
    }
}
