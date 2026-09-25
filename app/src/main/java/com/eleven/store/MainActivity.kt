package com.eleven.store

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eleven.store.navigation.BottomNavItem
import com.eleven.store.navigation.ElevenNavGraph
import com.eleven.store.navigation.Route
import com.eleven.store.ui.icons.LucideIcons
import com.eleven.store.ui.screens.AppWelcomeOverlay
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel
import com.eleven.store.util.NoInternetBanner
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {

    // مسار الفتح المُطلوب من إشعار تم الضغط عليه (مثال: "order/abc123") —
    // يُقرأ أولاً من intent إطلاق النشاط، ويُحدَّث لاحقاً في onNewIntent إن
    // كان النشاط مفتوحاً أصلاً عند الضغط على إشعار جديد (بدل إعادة الإطلاق).
    private val pendingNotificationRoute = mutableStateOf<String?>(null)

    // ✅ إصلاح: على أندرويد 13 (API 33) فأعلى، صلاحية POST_NOTIFICATIONS يجب طلبها
    // صراحةً وقت التشغيل — كانت معلنة فقط في AndroidManifest.xml دون أي طلب فعلي،
    // ما يعني أن النظام يمنع ظهور أي إشعار بصمت حتى لو وصل الـPush بنجاح من FCM.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or denied — لا شيء إضافي مطلوب، الإشعارات ستعمل إن سُمح بها */ }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // ✅ جديد: يُبقي شاشة إقلاع النظام (Theme.ElevenStore.Starting) ظاهرة حتى
    // أول رسمة فعلية لواجهة Compose — بدل اختفائها فوراً ثم وماضة بيضاء
    // فارغة لحظية قبل أن يُقلع Compose. يُقلَب مرة واحدة فقط (SideEffect
    // بأول composition لـElevenApp أدناه)، وليس مرتبطاً بتحميل البيانات
    // (ذلك تحديداً دور شاشة الترحيب AppWelcomeOverlay داخل ElevenApp —
    // فصل الاثنين مقصود: شاشة النظام يجب أن تكون قصيرة جداً بينما شاشة
    // الترحيب هي من تمتص وقت أول تحميل فعلي للبيانات).
    private var contentDrawn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !contentDrawn }
        super.onCreate(savedInstanceState)
        com.eleven.store.util.ThemePrefs.init(this)
        requestNotificationPermissionIfNeeded()
        pendingNotificationRoute.value = intent?.getStringExtra(NOTIFICATION_ROUTE_EXTRA)
        enableEdgeToEdge()
        setContent {
            // ✅ رُفع إنشاء الـViewModel إلى هنا (فوق ElevenStoreTheme) حتى تصل
            // إعدادات المتجر (ومنها ألوان لوحة التحكم) إلى الثيم نفسه؛ viewModel()
            // بنفس الـActivity يُرجع نفس النسخة عند استدعائه لاحقاً داخل ElevenApp.
            val viewModel: MainViewModel = viewModel()
            val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()

            val themeMode = com.eleven.store.util.ThemePrefs.current
            val darkTheme = when (themeMode) {
                com.eleven.store.util.ThemeMode.SYSTEM -> isSystemInDarkTheme()
                com.eleven.store.util.ThemeMode.LIGHT -> false
                com.eleven.store.util.ThemeMode.DARK -> true
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ElevenStoreTheme(
                    darkTheme = darkTheme,
                    primaryColor = storeSettings.primaryColor,
                    secondaryColor = storeSettings.secondaryColor,
                    backgroundColor = storeSettings.backgroundColor,
                ) {
                    // أول رسمة فعلية للواجهة ← نُخفي شاشة إقلاع النظام الآن
                    // (شاشة الترحيب AppWelcomeOverlay بداخل ElevenApp هي من
                    // تتكفّل بامتصاص وقت تحميل البيانات الفعلي من هنا فصاعداً).
                    SideEffect { contentDrawn = true }
                    ElevenApp(
                        pendingRoute = pendingNotificationRoute.value,
                        onPendingRouteConsumed = { pendingNotificationRoute.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(NOTIFICATION_ROUTE_EXTRA)?.let { pendingNotificationRoute.value = it }
    }

    companion object {
        /** اسم الـextra الذي يضعه ElevenFirebaseMessagingService بالـPendingIntent عند الضغط على إشعار. */
        const val NOTIFICATION_ROUTE_EXTRA = "notification_route"
    }
}

// ═══════════════════════════════════════════════════════════════
//  ELEVEN APP — التطبيق الرئيسي
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElevenApp(
    pendingRoute: String? = null,
    onPendingRouteConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val cartCount by viewModel.cartCount.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    // ✅ جديد: شاشة الترحيب (AppWelcomeOverlay) — تبقى فوق كامل التطبيق طوال
    // أول تحميل فعلي لبيانات الرئيسية فقط، ثم تختفي نهائياً (remember بلا
    // مفتاح = مرتبطة بعمر ElevenApp/الـActivity، لا تُعاد عند كل عودة لتبويب
    // الرئيسية). حد أدنى 900ms (حتى لو كانت البيانات بكاش محلي وجاهزة فوراً
    // — عرض ومضة بلا معنى أسوأ من عدم إظهارها إطلاقاً)، وحد أقصى أمان 3.5s
    // (حتى لا يظل المستخدم حبيس شاشة الترحيب لو تأخر الاتصال فعلياً؛ تكمل
    // شاشة الرئيسية عندها التحميل بنفسها بحالاتها التدريجية المعتادة).
    val bannersLoading by viewModel.bannersLoading.collectAsStateWithLifecycle()
    val categoriesLoading by viewModel.categoriesLoading.collectAsStateWithLifecycle()
    val homeProductsLoading by viewModel.homeProductsLoading.collectAsStateWithLifecycle()
    val isHomeDataLoading = bannersLoading || categoriesLoading || homeProductsLoading
    var showWelcome by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(900)
        withTimeoutOrNull(3500) {
            snapshotFlow { isHomeDataLoading }.first { loading -> !loading }
        }
        showWelcome = false
    }

    // فتح المسار المطلوب من إشعار تم الضغط عليه (order/{id} أو notifications
    // مثلاً) — يعمل سواء كان التطبيق مغلقاً (أول إطلاق) أو مفتوحاً أصلاً
    // بالخلفية (onNewIntent). يُستهلك مرة واحدة فقط حتى لا يُعاد التنقل
    // لنفس الوجهة عند أي إعادة تركيب لاحقة للواجهة.
    LaunchedEffect(pendingRoute) {
        if (!pendingRoute.isNullOrBlank()) {
            navController.navigate(pendingRoute) { launchSingleTop = true }
            onPendingRouteConsumed()
        }
    }

    LaunchedEffect(user) {
        if (user != null) {
            viewModel.loadNotifications()
            // ✅ يغطي المستخدمين المسجّلين دخولهم مسبقاً (قبل هذا الإصلاح) — وليس فقط تسجيل الدخول الجديد
            viewModel.syncFcmToken()
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ✅ إصلاح (تنقل الهيدر): نفس حارس الـdebounce المستخدم بالشريط السفلي
    // والقائمة الجانبية — يمنع تراكم وجهات مكرّرة بالـBack Stack عند نقر
    // متكرر سريع على أيقونتَي السلة/الإشعارات بالهيدر، ويعيد استخدام الوجهة
    // إن كانت محفوظة أصلاً (saveState/restoreState) بدل إنشاء نسخة جديدة.
    var lastHeaderNavTimestamp by remember { mutableStateOf(0L) }
    fun navigateHeaderTarget(route: String) {
        val now = System.currentTimeMillis()
        if (now - lastHeaderNavTimestamp < 300L) return
        lastHeaderNavTimestamp = now
        navController.navigate(route) {
            popUpTo(Route.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ElevenDrawer(
                user = user,
                navController = navController,
                viewModel = viewModel,
                onClose = { scope.launch { drawerState.close() } },
            )
        },
        gesturesEnabled = true,
    ) {
        Scaffold(
            topBar = {
                ElevenHeader(
                    navController = navController,
                    cartCount = cartCount,
                    hasUser = user != null,
                    unreadCount = unreadCount,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    // ✅ إصلاح (تنقل الهيدر): كانت هذه الأزرار تستخدم navigate() مباشرة
                    // بلا launchSingleTop/popUpTo — بخلاف الشريط السفلي والقائمة الجانبية.
                    // فكل نقرة على السلة أو الإشعارات من الهيدر (حتى لو كنّا فيها أصلاً،
                    // أو نتنقّل بينها وبين الرئيسية بشكل متكرر) كانت تدفع وجهة جديدة فوق
                    // الـBack Stack بدل إعادة استخدام الموجودة، فتتراكم نسخ مكرّرة يجب
                    // الرجوع خلالها بزر الرجوع للخروج فعلياً — يبدو للمستخدم أن الزر
                    // "لا يستجيب مباشرة" رغم أنه فعلياً ينقل، لكن لصفحة مكرّرة فوق نفس
                    // الصفحة. الآن نستخدم نفس نمط navigateDebounced في الشريط السفلي.
                    onCartClick = { navigateHeaderTarget(Route.CART) },
                    onNotificationsClick = { navigateHeaderTarget(Route.NOTIFICATIONS) },
                    onLogoClick = { navigateHeaderTarget(Route.HOME) },
                    onSearchSubmit = { query ->
                        // ✅ إصلاح: ترميز نص البحث قبل دمجه بمسار التنقل يدوياً —
                        // بحث يحتوي مسافة أو رمز &/#/% كان يكسر تحليل الاستعلام.
                        navController.navigate("${Route.PRODUCTS}?search=${android.net.Uri.encode(query)}")
                    },
                )
            },
            bottomBar = {
                ElevenBottomNav(
                    navController = navController,
                    cartCount = cartCount,
                )
            },
            containerColor = Background,
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // ✅ إصلاح: بانر عام يظهر تلقائياً عند فقدان الاتصال ويختفي فور
                // عودته — مصدر واحد فوق كل الشجرة بدل أن تكتشف كل شاشة الحالة بنفسها.
                NoInternetBanner()
                Box(modifier = Modifier.weight(1f)) {
                    ElevenNavGraph(
                        navController = navController,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showWelcome,
        exit = fadeOut(tween(450)),
    ) {
        AppWelcomeOverlay()
    }
    }
}

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
    onLogoClick: () -> Unit,
    onSearchSubmit: (String) -> Unit,
) {
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // إخفاء الهيدر في شاشات معينة
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    // ✅ إصلاح: كل شاشة تملك ElevenTopBar خاصاً بها (سهم رجوع + عنوان) كانت
    // تُعرض تحت الهيدر العام مباشرة — شريطان علويان متتاليان يهدران مساحة
    // ويُلبسان زر الرجوع الحقيقي وسط أيقونات مشابهة (القائمة). نوسّع القائمة
    // لتغطي كل الشاشات التي تستخدم ElevenTopBar بدل الاقتصار على product/ وorder/.
    val hideRoutes = listOf(
        Route.LOGIN, Route.REGISTER, Route.SETTINGS, Route.NOTIFICATIONS,
        Route.ORDERS, Route.CHECKOUT, Route.ABOUT, Route.CONTACT, Route.CART,
    )
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
                    // ✅ إصلاح: كان النقر هنا يستدعي navigate() مباشرة (منطق منفصل
                    // ومختلف عن باقي أزرار الهيدر)، والآن يُفوَّض لنفس دالة
                    // navigateHeaderTarget المعرَّفة في ElevenApp (debounce +
                    // launchSingleTop + saveState/restoreState)، عبر onLogoClick.
                    modifier = Modifier.clickable { onLogoClick() },
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
                            .focusRequester(focusRequester),
                        // ✅ إصلاح: height(46.dp) كان أصغر من الحد الأدنى الطبيعي
                        // لصندوق OutlinedTextField (~56dp)، فيقص الحقل ويُظهر نصفه
                        // فقط بدل عرضه كاملاً. الآن نتحكم بالارتفاع عبر
                        // minHeight/padding داخلي بدل فرض ارتفاع أصغر من اللازم.
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

    // ✅ إصلاح: حارس debounce بسيط — يتجاهل أي نقر خلال ~300ms من آخر تنقل،
    // لمنع دفع أكثر من وجهة متتالية للـBack Stack عند نقر سريع متكرر.
    var lastNavTimestamp by remember { mutableStateOf(0L) }
    fun navigateDebounced(route: String) {
        val now = System.currentTimeMillis()
        if (now - lastNavTimestamp < 300L) return
        lastNavTimestamp = now
        navController.navigate(route) {
            popUpTo(Route.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

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
                                navigateDebounced(item.route)
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

// ═══════════════════════════════════════════════════════════════
//  DRAWER — القائمة الجانبية
// ═══════════════════════════════════════════════════════════════

@Composable
fun ElevenDrawer(
    user: com.google.firebase.auth.FirebaseUser?,
    navController: NavController,
    viewModel: MainViewModel,
    onClose: () -> Unit,
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val drawerWidth = if (screenWidthDp < 640) screenWidthDp.dp else 320.dp

    // ✅ نفس حارس الـdebounce المستخدم بالشريط السفلي — يمنع نقر متكرر سريع
    // على روابط القائمة الجانبية من دفع أكثر من وجهة متتالية للـBack Stack.
    var lastDrawerNavTimestamp by remember { mutableStateOf(0L) }
    fun navigateDebounced(route: String) {
        val now = System.currentTimeMillis()
        if (now - lastDrawerNavTimestamp < 300L) return
        lastDrawerNavTimestamp = now
        navController.navigate(route) {
            popUpTo(Route.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        onClose()
    }

    ModalDrawerSheet(
        modifier = Modifier.width(drawerWidth),
        drawerContainerColor = Background,
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Accent.copy(alpha = 0.1f), Accent.copy(alpha = 0.05f)),
                        ),
                    )
                    .padding(24.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "11",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 24.sp,
                        )
                    }
                    Column {
                        Text(
                            text = "ELEVEN",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = user?.email ?: "أهلاً بك في Eleven",
                            color = MutedForeground,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            HorizontalDivider(color = Border, thickness = 1.dp)

            // Navigation Links
            val navLinks = listOf(
                Triple(LucideIcons.Home, "الرئيسية", Route.HOME),
                Triple(LucideIcons.Heart, "المفضلة", Route.FAVORITES),
                Triple(LucideIcons.ShoppingBag, "طلباتي", Route.ORDERS),
                Triple(LucideIcons.User, "الملف الشخصي", Route.PROFILE),
                Triple(LucideIcons.Bell, "الإشعارات", Route.NOTIFICATIONS),
                Triple(LucideIcons.Info, "الإعدادات", Route.SETTINGS), // مطابق للموقع: Header.tsx يستخدم أيقونة Info لـ"الإعدادات" أيضاً (نفس أيقونة "حول")
                Triple(LucideIcons.Phone, "اتصل بنا", Route.CONTACT),
                Triple(LucideIcons.Info, "حول", Route.ABOUT),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                navLinks.forEach { (icon, label, route) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                navigateDebounced(route)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Foreground,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Foreground,
                        )
                    }
                }
            }

            // Footer
            HorizontalDivider(color = Border, thickness = 1.dp)

            Column(modifier = Modifier.padding(16.dp)) {
                if (user != null) {
                    OutlinedButton(
                        onClick = {
                            viewModel.logout()
                            navController.navigate(Route.HOME) {
                                popUpTo(0) { inclusive = true }
                            }
                            onClose()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Destructive.copy(alpha = 0.3f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Destructive,
                        ),
                    ) {
                        Icon(
                            LucideIcons.LogOut,
                            null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("تسجيل الخروج", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            navController.navigate(Route.LOGIN)
                            onClose()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    ) {
                        Text(
                            "تسجيل الدخول",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "ELEVEN STORE",
                    color = MutedForeground.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}