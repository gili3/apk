package com.eleven.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleven.store.ui.components.ElevenSnackbarHost
import com.eleven.store.ui.components.SnackbarType
import com.eleven.store.ui.components.showMessage
import com.eleven.store.ui.components.ElevenButton
import com.eleven.store.ui.components.ElevenTopBar
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel
import com.eleven.store.data.model.NotificationItem
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════
//  SETTINGS SCREEN — نسخة طبق الأصل من Settings.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    val baseUrl = remember(storeSettings.websiteUrl) {
        storeSettings.websiteUrl.ifBlank { "https://eleven-sd.com" }.trimEnd('/')
    }

    var showChangePassword by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPw by remember { mutableStateOf(false) }
    var showNewPw by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { ElevenSnackbarHost(snackbarHostState) },
        topBar = {
            ElevenTopBar(title = "الإعدادات", onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = 24.dp + padding.calculateTopPadding(),
                bottom = 32.dp + padding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── العنوان الرئيسي ─────────────────────────
            item {
                Text(
                    "الإعدادات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // ════════════════════════════════════════════
            //  بطاقة الحساب
            // ════════════════════════════════════════════
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier.padding(
                                start = 20.dp, end = 20.dp,
                                top = 20.dp, bottom = 12.dp,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                null,
                                tint = Accent,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                "الحساب",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        HorizontalDivider(color = Border)

                        Column(modifier = Modifier.padding(12.dp)) {
                            // البريد الإلكتروني الحالي
                            if (user != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant
                                                .copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .padding(12.dp),
                                ) {
                                    Column {
                                        Text(
                                            "الحساب الحالي",
                                            color = MutedForeground,
                                            fontSize = 12.sp,
                                        )
                                        Text(
                                            user!!.email ?: "",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            // زر تغيير كلمة المرور
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showChangePassword = !showChangePassword
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Lock,
                                    null,
                                    tint = MutedForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    "تغيير كلمة المرور",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.Filled.ChevronLeft,
                                    null,
                                    tint = MutedForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            // نموذج تغيير كلمة المرور
                            if (showChangePassword) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant
                                                .copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    // كلمة المرور الحالية
                                    OutlinedTextField(
                                        value = currentPassword,
                                        onValueChange = { currentPassword = it },
                                        placeholder = {
                                            Text(
                                                "كلمة المرور الحالية",
                                                fontSize = 14.sp,
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Lock,
                                                null,
                                                tint = MutedForeground,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { showCurrentPw = !showCurrentPw },
                                            ) {
                                                Icon(
                                                    if (showCurrentPw) Icons.Filled.VisibilityOff
                                                    else Icons.Filled.Visibility,
                                                    null,
                                                    tint = MutedForeground,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        },
                                        visualTransformation = if (showCurrentPw)
                                            VisualTransformation.None
                                        else PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Accent,
                                            unfocusedBorderColor = Border,
                                            unfocusedContainerColor = MaterialTheme
                                                .colorScheme
                                                .background,
                                        ),
                                    )

                                    // كلمة المرور الجديدة
                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { newPassword = it },
                                        placeholder = {
                                            Text(
                                                "كلمة المرور الجديدة",
                                                fontSize = 14.sp,
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Lock,
                                                null,
                                                tint = MutedForeground,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { showNewPw = !showNewPw },
                                            ) {
                                                Icon(
                                                    if (showNewPw) Icons.Filled.VisibilityOff
                                                    else Icons.Filled.Visibility,
                                                    null,
                                                    tint = MutedForeground,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        },
                                        visualTransformation = if (showNewPw)
                                            VisualTransformation.None
                                        else PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Accent,
                                            unfocusedBorderColor = Border,
                                            unfocusedContainerColor = MaterialTheme
                                                .colorScheme
                                                .background,
                                        ),
                                    )

                                    // تأكيد كلمة المرور الجديدة
                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it },
                                        placeholder = {
                                            Text(
                                                "تأكيد كلمة المرور الجديدة",
                                                fontSize = 14.sp,
                                            )
                                        },
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Accent,
                                            unfocusedBorderColor = Border,
                                            unfocusedContainerColor = MaterialTheme
                                                .colorScheme
                                                .background,
                                        ),
                                    )

                                    // أزرار
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Button(
                                            onClick = {
                                                if (newPassword.length < 6) {
                                                    scope.launch {
                                                        snackbarHostState.showMessage(
                                                            "كلمة المرور يجب أن تكون 6 أحرف على الأقل",
                                                            SnackbarType.ERROR
                                                        )
                                                    }
                                                    return@Button
                                                }
                                                if (newPassword != confirmPassword) {
                                                    scope.launch {
                                                        snackbarHostState.showMessage(
                                                            "كلمتا المرور غير متطابقتين",
                                                            SnackbarType.ERROR
                                                        )
                                                    }
                                                    return@Button
                                                }
                                                isLoading = true
                                                viewModel.changePassword(
                                                    currentPassword,
                                                    newPassword,
                                                ) { ok, msg ->
                                                    isLoading = false
                                                    if (ok) {
                                                        showChangePassword = false
                                                        currentPassword = ""
                                                        newPassword = ""
                                                        confirmPassword = ""
                                                        scope.launch {
                                                            snackbarHostState.showMessage(
                                                                "تم تغيير كلمة المرور بنجاح",
                                                                SnackbarType.SUCCESS
                                                            )
                                                        }
                                                    } else {
                                                        scope.launch {
                                                            snackbarHostState.showMessage(
                                                                msg ?: "فشل تغيير كلمة المرور",
                                                                SnackbarType.ERROR
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = !isLoading,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Accent,
                                            ),
                                        ) {
                                            Text(
                                                if (isLoading) "جاري الحفظ..." else "حفظ",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White,
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                showChangePassword = false
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                Border,
                                            ),
                                        ) {
                                            Text(
                                                "إلغاء",
                                                fontSize = 14.sp,
                                                color = MutedForeground,
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Border)

                            // زر حذف الحساب
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            viewModel.deleteAccount("") { ok, msg ->
                                                scope.launch {
                                                    if (ok) {
                                                        snackbarHostState.showMessage(
                                                            "تم حذف الحساب بنجاح",
                                                            SnackbarType.SUCCESS
                                                        )
                                                    } else {
                                                        snackbarHostState.showMessage(
                                                            msg ?: "فشل حذف الحساب",
                                                            SnackbarType.ERROR
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.DeleteForever,
                                    null,
                                    tint = Destructive,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    "حذف الحساب",
                                    fontSize = 14.sp,
                                    color = Destructive,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.Filled.ChevronLeft,
                                    null,
                                    tint = MutedForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ════════════════════════════════════════════
            //  بطاقة القانونية
            // ════════════════════════════════════════════
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier.padding(
                                start = 20.dp, end = 20.dp,
                                top = 20.dp, bottom = 12.dp,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.PrivacyTip,
                                null,
                                tint = Accent,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                "القانونية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        HorizontalDivider(color = Border)

                        Column(modifier = Modifier.padding(4.dp)) {
                            // سياسة الخصوصية
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        openUri(context, "$baseUrl/privacy-policy")
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.PrivacyTip,
                                    null,
                                    tint = MutedForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    "سياسة الخصوصية",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.Filled.ChevronLeft,
                                    null,
                                    tint = MutedForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            HorizontalDivider(
                                color = Border,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )

                            // الشروط والأحكام
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        openUri(context, "$baseUrl/terms")
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Description,
                                    null,
                                    tint = MutedForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    "الشروط والأحكام",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.Filled.ChevronLeft,
                                    null,
                                    tint = MutedForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ════════════════════════════════════════════
            //  زر تسجيل الخروج
            // ════════════════════════════════════════════
            item {
                OutlinedButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Destructive,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Destructive,
                    ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "تسجيل الخروج",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }

            // ════════════════════════════════════════════
            //  تذييل
            // ════════════════════════════════════════════
            item {
                Text(
                    "Eleven Store — النسخة 1.0.0",
                    color = MutedForeground,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  NOTIFICATIONS SCREEN — إعادة تصميم شاملة: تجميع حسب التاريخ،
//  سحب للحذف، سحب للتحديث، وحالة خطأ/تحميل واضحة بدل قائمة فارغة
//  صامتة عند فشل الجلب (مثال: فهرس Firestore غير مُفعّل).
// ═══════════════════════════════════════════════════════════════

private data class NotifTypeConfig(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val bg: Color,
    val color: Color,
)

// ✅ توحيد: نفس الخمسة ألوان المستخدمة بحالات الطلب بالضبط (Theme.kt)،
// بدون أي درجة منفصلة (كانت EFF6FF/2563EB مثلاً مختلفة عن StateBlueBg/Fg
// الرسمي)، وبدون بنفسجي لـ welcome لأنه ليس من ضمن الخمسة ألوان المعتمدة —
// استُبدل برمادي محايد ليطابق التوثيق الرسمي حرفياً، وبنفس ما طُبّق بالموقع.
private val TYPE_CONFIG = mapOf(
    "order" to NotifTypeConfig(Icons.Filled.CheckCircle, StateBlueBg, StateBlueFg),
    "promo" to NotifTypeConfig(Icons.Filled.LocalOffer, StateOrangeBg, StateOrangeFg),
    "shipping" to NotifTypeConfig(Icons.Filled.LocalShipping, StateGreenBg, StateGreenFg),
    "welcome" to NotifTypeConfig(Icons.Filled.CardGiftcard, Neutral100, TextSecondary),
    "general" to NotifTypeConfig(Icons.Filled.Notifications, Neutral100, TextSecondary),
)

/** يحدّد التسمية اليومية (اليوم / أمس / هذا الأسبوع / أقدم) لتجميع الإشعارات تحتها. */
private fun dateGroupLabel(date: java.util.Date): String {
    val cal = java.util.Calendar.getInstance()
    val today = cal.clone() as java.util.Calendar

    val target = java.util.Calendar.getInstance().apply { time = date }

    fun sameDay(a: java.util.Calendar, b: java.util.Calendar) =
        a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
            a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)

    if (sameDay(today, target)) return "اليوم"

    val yesterday = (today.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    if (sameDay(yesterday, target)) return "أمس"

    val weekAgo = (today.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -7) }
    if (target.after(weekAgo)) return "هذا الأسبوع"

    return "أقدم"
}

private val GROUP_ORDER = listOf("اليوم", "أمس", "هذا الأسبوع", "أقدم")

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenRoute: (String) -> Unit = {},
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val isLoading by viewModel.notificationsLoading.collectAsStateWithLifecycle()
    val loadError by viewModel.notificationsError.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        if (user != null) viewModel.loadNotifications()
    }

    // ✅ جديد: بمجرد دخول شاشة الإشعارات، تُعتبر كل الإشعارات الظاهرة حالياً
    // "مقروءة" تلقائياً — مطابق لنفس السلوك المضاف بصفحة الإشعارات بالموقع.
    // مرة واحدة فقط لكل زيارة للشاشة (وليس عند كل تحديث لاحق للقائمة).
    var didAutoMarkRead by remember { mutableStateOf(false) }
    LaunchedEffect(user, isLoading) {
        if (user == null || isLoading || didAutoMarkRead) return@LaunchedEffect
        didAutoMarkRead = true
        if (notifications.any { !it.isRead }) {
            viewModel.markAllNotificationsRead()
        }
    }

    // ✅ v2: عداد "غير المقروء" الحقيقي من viewModel.unreadCount (حقل
    // notifUnreadCount المُسوَّى ذرّياً بسيرفر Cloud Functions) بدل عدّ
    // العناصر المحمَّلة هنا فقط (كانت محدودة بـlimit(50) بـobserveNotifications
    // — رقم خاطئ لأي مستخدم يتجاوز غير مقروئه هذا الحد).
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    // ── تجميع الإشعارات حسب التاريخ، بترتيب ثابت (اليوم أولاً) ──
    val grouped = remember(notifications) {
        notifications
            .groupBy { dateGroupLabel(it.createdAt?.toDate() ?: java.util.Date()) }
            .toSortedMap(compareBy { GROUP_ORDER.indexOf(it).let { i -> if (i == -1) GROUP_ORDER.size else i } })
    }

    Scaffold(
        snackbarHost = { ElevenSnackbarHost(snackbarHostState) },
        topBar = {
            ElevenTopBar(title = "الإشعارات", onBack = onBack)
        },
    ) { padding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { if (user != null) viewModel.loadNotifications() },
            state = pullState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 24.dp,
                    bottom = 32.dp + padding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // ── Header ─────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 28.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "الإشعارات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(Accent, RoundedCornerShape(50)),
                            )
                            if (unreadCount > 0) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "$unreadCount غير مقروء",
                                    color = MutedForeground,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                        // ✅ جديد: زر "حذف الكل" — لمطابقة صفحة الإشعارات في الموقع 100%.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (unreadCount > 0 && user != null) {
                                TextButton(
                                    onClick = {
                                        viewModel.markAllNotificationsRead()
                                        scope.launch {
                                            snackbarHostState.showMessage(
                                                "تم تحديد جميع الإشعارات كمقروءة",
                                                SnackbarType.SUCCESS
                                            )
                                        }
                                    },
                                ) {
                                    Text(
                                        "تحديد الكل كمقروء",
                                        color = Accent,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                            if (notifications.isNotEmpty() && user != null) {
                                var showConfirmDeleteAll by remember { mutableStateOf(false) }
                                TextButton(onClick = { showConfirmDeleteAll = true }) {
                                    Icon(
                                        Icons.Filled.DeleteForever,
                                        null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "حذف الكل",
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                    )
                                }
                                if (showConfirmDeleteAll) {
                                    AlertDialog(
                                        onDismissRequest = { showConfirmDeleteAll = false },
                                        title = { Text("حذف جميع الإشعارات") },
                                        text = { Text("هل تريد حذف جميع الإشعارات؟ لا يمكن التراجع عن هذا الإجراء.") },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                showConfirmDeleteAll = false
                                                viewModel.deleteAllNotifications()
                                                scope.launch {
                                                    snackbarHostState.showMessage("تم حذف جميع الإشعارات", SnackbarType.SUCCESS)
                                                }
                                            }) { Text("حذف", color = Color(0xFFDC2626)) }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showConfirmDeleteAll = false }) { Text("إلغاء") }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // ── رسالة غير مسجل دخول ───────────────────
                if (user == null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "سجّل الدخول لمشاهدة إشعاراتك الحقيقية",
                                    color = MutedForeground,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Button(
                                    // ✅ إصلاح: كان بلا أي تنفيذ (TODO فارغ) — يستخدم الآن
                                    // نفس آلية onOpenRoute المستخدمة أصلاً بهذه الشاشة للتنقل.
                                    onClick = { onOpenRoute(com.eleven.store.navigation.Route.LOGIN) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Accent,
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 8.dp,
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        "تسجيل الدخول",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── حالة خطأ بالجلب (بدل قائمة فارغة صامتة) ─
                if (loadError != null && user != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFEF2F2),
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.ErrorOutline,
                                        null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        "تعذّر تحميل الإشعارات",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF991B1B),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "تحقق من اتصال الإنترنت وحاول مجدداً. إن استمرت المشكلة، قد تحتاج قاعدة البيانات فهرساً لم يُفعَّل بعد.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF991B1B),
                                    lineHeight = 18.sp,
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { viewModel.loadNotifications() },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                ) {
                                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("إعادة المحاولة", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // ── هيكل تحميل أوّلي (skeleton) ────────────
                if (isLoading && notifications.isEmpty() && loadError == null) {
                    items(3) {
                        NotificationSkeletonCard()
                    }
                }

                // ── حالة فارغة حقيقية (بعد انتهاء التحميل، بدون خطأ) ─
                if (!isLoading && loadError == null && notifications.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 96.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant
                                            .copy(alpha = 0.3f),
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    null,
                                    tint = MutedForeground,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "لا توجد إشعارات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "سنقوم بتنبيهك عند وجود تحديثات جديدة",
                                color = MutedForeground,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                // ── قائمة الإشعارات مجمّعة حسب التاريخ ─────
                grouped.forEach { (label, items) ->
                    item(key = "header_$label") {
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MutedForeground,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp),
                        )
                    }
                    items(items, key = { it.id }) { notif ->
                        SwipeableNotificationCard(
                            notif = notif,
                            canInteract = user != null,
                            onClick = {
                                if (!notif.isRead && user != null) {
                                    viewModel.markNotificationRead(notif.id)
                                }
                                // فتح المسار المرتبط بالإشعار (مثال: تفاصيل الطلب) إن وُجد —
                                // نفس السلوك الذي يفتحه الضغط على إشعار النظام من قائمة التنبيهات.
                                notif.actionRoute.takeIf { it.isNotBlank() }?.let(onOpenRoute)
                            },
                            onDelete = {
                                viewModel.deleteNotification(notif.id)
                                scope.launch { snackbarHostState.showMessage("تم حذف الإشعار", SnackbarType.SUCCESS) }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** بطاقة إشعار واحدة — قابلة للسحب للحذف، بشكل موحّد واحد سواء كان الإشعار مقروءاً أو لا. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationCard(
    notif: NotificationItem,
    canInteract: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val cfg = TYPE_CONFIG[notif.type] ?: TYPE_CONFIG["general"]!!

    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart && canInteract) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    androidx.compose.material3.SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.padding(vertical = 6.dp),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = canInteract,
        backgroundContent = {
            // ✅ إصلاح: كانت خلفية الحذف عند السحب لوناً باهتاً جداً (0xFFFEE2E2)
            // بالكاد يُلاحظ. الآن لون أحمر قوي وأنيق (تدرّج الخطر الأساسي) مع
            // أيقونة بيضاء واضحة، يعكس بجلاء أن الإجراء هو حذف نهائي — ومطابق
            // للون الحذف المستخدم في نسخة الموقع.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE11D48), RoundedCornerShape(14.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.DeleteForever,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // أيقونة النوع بخلفية دائرية ناعمة
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(cfg.bg, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            cfg.icon,
                            null,
                            tint = cfg.color,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    // المحتوى
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            notif.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            notif.message,
                            color = MutedForeground,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            notif.timeAgo,
                            color = MutedForeground,
                            fontSize = 12.sp,
                        )
                    }

                    // ✅ جديد: زر حذف ظاهر دائماً (بدل الاعتماد على السحب فقط) —
                    // لمطابقة صفحة الإشعارات بالموقع 100%، حيث الحذف متاح بزر
                    // صريح دائماً بالإضافة للسحب.
                    if (canInteract) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "حذف الإشعار",
                                tint = MutedForeground,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** بطاقة هيكلية (skeleton) بسيطة تُعرض أثناء أول تحميل — بديل أنيق لدوّارة تحميل مجرّدة. */
@Composable
private fun NotificationSkeletonCard() {
    val alpha by rememberInfiniteTransitionAlpha()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MutedForeground.copy(alpha = alpha * 0.15f), CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .background(MutedForeground.copy(alpha = alpha * 0.15f), RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(MutedForeground.copy(alpha = alpha * 0.12f), RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(10.dp)
                        .background(MutedForeground.copy(alpha = alpha * 0.1f), RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

@Composable
private fun rememberInfiniteTransitionAlpha(): State<Float> {
    val transition = rememberInfiniteTransition(label = "skeleton")
    return transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
}
