package com.eleven.store

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.eleven.store.navigation.ElevenNavGraph
import com.eleven.store.ui.components.ElevenHeader
import com.eleven.store.ui.components.ElevenBottomNav
import com.eleven.store.ui.components.ElevenDrawer
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        pendingNotificationRoute.value = intent?.getStringExtra(NOTIFICATION_ROUTE_EXTRA)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ElevenStoreTheme {
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
                    onCartClick = { navController.navigate(Route.CART) },
                    onNotificationsClick = { navController.navigate(Route.NOTIFICATIONS) },
                    onSearchSubmit = { query ->
                        navController.navigate("${Route.PRODUCTS}?search=$query")
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
                // ✅ جديد: بانر عام يظهر تلقائياً عند فقدان الاتصال بالإنترنت،
                // فوق أي شاشة داخل التطبيق، بدون أي تعديل بكل شاشة على حدة.
                com.eleven.store.util.NoInternetBanner()
                Box(modifier = Modifier.weight(1f)) {
                    ElevenNavGraph(
                        navController = navController,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}
