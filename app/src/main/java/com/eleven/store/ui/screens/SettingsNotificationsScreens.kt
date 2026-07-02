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
import com.eleven.store.ui.components.ElevenButton
import com.eleven.store.ui.components.ElevenTopBar
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                                        snackbarHostState.showSnackbar(
                                                            "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
                                                        )
                                                    }
                                                    return@Button
                                                }
                                                if (newPassword != confirmPassword) {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            "كلمتا المرور غير متطابقتين"
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
                                                            snackbarHostState.showSnackbar(
                                                                "تم تغيير كلمة المرور بنجاح"
                                                            )
                                                        }
                                                    } else {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                msg ?: "فشل تغيير كلمة المرور"
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
                                                        snackbarHostState.showSnackbar(
                                                            "تم حذف الحساب بنجاح"
                                                        )
                                                    } else {
                                                        snackbarHostState.showSnackbar(
                                                            msg ?: "فشل حذف الحساب"
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
//  NOTIFICATIONS SCREEN — نسخة طبق الأصل من Notifications.tsx
// ═══════════════════════════════════════════════════════════════

private data class NotifTypeConfig(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val bg: Color,
    val color: Color,
)

private val TYPE_CONFIG = mapOf(
    "order" to NotifTypeConfig(Icons.Filled.CheckCircle, Color(0xFFEFF6FF), Color(0xFF2563EB)),
    "promo" to NotifTypeConfig(Icons.Filled.LocalOffer, Color(0xFFFFFBEB), Color(0xFFD97706)),
    "shipping" to NotifTypeConfig(Icons.Filled.LocalShipping, Color(0xFFF0FDF4), Color(0xFF16A34A)),
    "welcome" to NotifTypeConfig(Icons.Filled.CardGiftcard, Color(0xFFF3E8FF), Color(0xFF9333EA)),
    "general" to NotifTypeConfig(Icons.Filled.Notifications, Color(0xFFF9FAFB), Color(0xFF4B5563)),
)

@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (user != null) viewModel.loadNotifications()
    }

    val unreadCount = notifications.count { !it.isRead }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ElevenTopBar(title = "الإشعارات", onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = 24.dp + padding.calculateTopPadding(),
                bottom = 32.dp + padding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Header ─────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
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
                    if (unreadCount > 0 && user != null) {
                        TextButton(
                            onClick = {
                                viewModel.markAllNotificationsRead()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "تم تحديد جميع الإشعارات كمقروءة"
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
                                onClick = { /* TODO: navigate to login */ },
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

            // ── حالة فارغة ─────────────────────────────
            if (notifications.isEmpty()) {
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

            // ── قائمة الإشعارات ────────────────────────
            items(notifications, key = { it.id }) { notif ->
                val cfg = TYPE_CONFIG[notif.type] ?: TYPE_CONFIG["general"]!!
                val isUnread = !notif.isRead

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            if (isUnread && user != null) {
                                viewModel.markNotificationRead(notif.id)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnread)
                            Accent.copy(alpha = 0.05f)
                        else MaterialTheme.colorScheme.surface,
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isUnread) Accent.copy(alpha = 0.3f) else Border,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // أيقونة النوع
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
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

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // نقطة غير مقروء
                                    if (isUnread) {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .background(Accent, CircleShape),
                                        )
                                    }
                                    // زر حذف
                                    if (user != null) {
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteNotification(notif.id)
                                            },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Text(
                                                "✕",
                                                color = MutedForeground,
                                                fontSize = 14.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}