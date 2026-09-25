package com.eleven.store.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
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
//  SETTINGS SCREEN
// ═══════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit = onBack,
    onNavigateToContact: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    // ✅ جديد: تفضيل إشعارات الجوال (Push) — يُحفظ محلياً ويقرأه
    // ElevenFirebaseMessagingService قبل عرض أي إشعار.
    var pushEnabled by remember {
        mutableStateOf(com.eleven.store.util.AppPreferences.isPushEnabled(context))
    }

    // ✅ إصلاح: أُزيل التوجيه الافتراضي لـ eleven-sd.com — لو مفيش رابط
    // موقع حقيقي مضبوط من الأدمن، baseUrl تبقى فاضية والأزرار تحت بتتصرف
    // بمنعها من فتح رابط خاطئ (راجع onClick بتاع كل زر).
    val baseUrl = remember(storeSettings.websiteUrl) {
        storeSettings.websiteUrl.trim().trimEnd('/')
    }

    // ✅ إصلاح: تسجيل الخروج كان يحدث فوراً بلا أي تأكيد — ضغطة واحدة بالخطأ
    // (خصوصاً أن الزر أحمر ومجاور لعناصر أخرى بنفس الصفحة) كانت تُخرج
    // المستخدم من حسابه مباشرة بلا أي فرصة للتراجع.
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPw by remember { mutableStateOf(false) }
    var showNewPw by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ✅ إصلاح: حذف الحساب لم يكن يطلب تأكيداً ولا كلمة مرور — كان يستدعي
    // deleteAccount("") مباشرة، فيفشل دائماً لحسابات البريد (إعادة المصادقة
    // تحتاج كلمة مرور صحيحة) ولا يعالج حسابات Google إطلاقاً. الآن نعرض
    // نافذة تأكيد، ونطلب كلمة المرور لحسابات البريد أو إعادة مصادقة Google
    // فعلية لحسابات Google، قبل تنفيذ الحذف.
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var showDeletePw by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    // ✅ جديد: خطوة رمز التأكيد (OTP) — بعد نجاح إعادة المصادقة (كلمة مرور
    // أو Google)، لا يُحذف الحساب فوراً. تتحول النافذة لخطوة إدخال الرمز
    // المُرسَل للبريد، ولا يتم الحذف الفعلي إلا بعد تأكيده.
    var deleteOtpStep by remember { mutableStateOf(false) }
    var deleteOtpCode by remember { mutableStateOf("") }
    var isResendingOtp by remember { mutableStateOf(false) }
    val isGoogleAccount = remember(user?.uid) { viewModel.isCurrentUserGoogleAccount() }
    val deleteGoogleLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val idToken = account?.idToken
            if (idToken == null) {
                isDeleting = false
                scope.launch { snackbarHostState.showMessage("تعذّرت إعادة المصادقة عبر Google", SnackbarType.ERROR) }
                return@rememberLauncherForActivityResult
            }
            viewModel.deleteAccountWithGoogle(idToken) { ok, msg ->
                isDeleting = false
                scope.launch {
                    if (ok) {
                        // ✅ لم يُحذف الحساب بعد — فقط طُلب رمز التأكيد. ننتقل
                        // لخطوة إدخال الرمز بدل إغلاق النافذة بنجاح مباشرة.
                        deleteOtpStep = true
                        snackbarHostState.showMessage("أرسلنا رمز تأكيد إلى بريدك الإلكتروني", SnackbarType.SUCCESS)
                    } else {
                        snackbarHostState.showMessage(msg ?: "فشل إرسال رمز التأكيد", SnackbarType.ERROR)
                    }
                }
            }
        } catch (_: Exception) {
            isDeleting = false
            scope.launch { snackbarHostState.showMessage("تعذّرت إعادة المصادقة عبر Google", SnackbarType.ERROR) }
        }
    }

    // ── معالجات الأحداث (منطق فقط، بلا واجهة) ────────────────────────
    // مُعرَّفة هنا لأنها تحتاج viewModel/scope/snackbarHostState وكل
    // متغيرات الحالة أعلاه، وتُمرَّر كدوال جاهزة (lambdas) للأقسام
    // البصرية أسفل — كل قسم بيعرف "يسوي وش" بلا ما يعرف "تفاصيل الحالة".

    val onSavePassword: () -> Unit = savePassword@{
        if (newPassword.length < 8) {
            scope.launch {
                snackbarHostState.showMessage(
                    "كلمة المرور يجب أن تكون 8 أحرف على الأقل",
                    SnackbarType.ERROR
                )
            }
            return@savePassword
        }
        if (newPassword != confirmPassword) {
            scope.launch {
                snackbarHostState.showMessage(
                    "كلمتا المرور غير متطابقتين",
                    SnackbarType.ERROR
                )
            }
            return@savePassword
        }
        isLoading = true
        viewModel.changePassword(currentPassword, newPassword) { ok, msg ->
            isLoading = false
            if (ok) {
                showChangePassword = false
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""
                scope.launch {
                    snackbarHostState.showMessage("تم تغيير كلمة المرور بنجاح", SnackbarType.SUCCESS)
                }
            } else {
                scope.launch {
                    snackbarHostState.showMessage(msg ?: "فشل تغيير كلمة المرور", SnackbarType.ERROR)
                }
            }
        }
    }

    // ✅ الصفوف تظهر دائماً، وتفتح رابط الموقع فقط (بلا صفحات داخلية).
    val onOpenPrivacyPolicy: () -> Unit = {
        if (baseUrl.isBlank()) {
            scope.launch { snackbarHostState.showMessage("الصفحة غير متاحة حالياً", SnackbarType.ERROR) }
        } else {
            openUri(context, "$baseUrl/privacy-policy")
        }
    }

    val onOpenTermsOfService: () -> Unit = {
        if (baseUrl.isBlank()) {
            scope.launch { snackbarHostState.showMessage("الصفحة غير متاحة حالياً", SnackbarType.ERROR) }
        } else {
            openUri(context, "$baseUrl/terms")
        }
    }

    val onResendDeleteOtp: () -> Unit = {
        isResendingOtp = true
        viewModel.deleteAccount(deletePassword) { ok, msg ->
            isResendingOtp = false
            scope.launch {
                snackbarHostState.showMessage(
                    if (ok) "تم إرسال رمز جديد" else (msg ?: "تعذّر إرسال رمز جديد"),
                    if (ok) SnackbarType.SUCCESS else SnackbarType.ERROR,
                )
            }
        }
    }

    val onConfirmDeleteStep: () -> Unit = {
        if (deleteOtpStep) {
            isDeleting = true
            viewModel.confirmAccountDeletion(deleteOtpCode) { ok, msg ->
                isDeleting = false
                if (ok) {
                    showDeleteAccountDialog = false
                    deleteOtpStep = false
                    deleteOtpCode = ""
                    deletePassword = ""
                    scope.launch {
                        snackbarHostState.showMessage("تم حذف الحساب بنجاح", SnackbarType.SUCCESS)
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showMessage(msg ?: "فشل تأكيد الحذف", SnackbarType.ERROR)
                    }
                }
            }
        } else if (isGoogleAccount) {
            isDeleting = true
            val googleClient = buildGoogleSignInClient(context)
            // ✅ إصلاح (تناسق مع تسجيل الدخول/التسجيل أعلى الملف): بلا
            // signOut() هنا، GoogleSignInClient يعيد الحساب المخزَّن مسبقاً
            // بصمت تام أحياناً (بلا أي شاشة تأكيد) — ما يُفقِد إعادة
            // المصادقة معناها الأمني الحقيقي (التأكد أن المستخدم لا يزال
            // يملك وصولاً فعلياً لحساب جوجل قبل حذف حسابه نهائياً).
            googleClient.signOut().addOnCompleteListener {
                deleteGoogleLauncher.launch(googleClient.signInIntent)
            }
        } else {
            isDeleting = true
            viewModel.deleteAccount(deletePassword) { ok, msg ->
                isDeleting = false
                if (ok) {
                    // ✅ لم يُحذف الحساب بعد — فقط طُلب رمز التأكيد.
                    deleteOtpStep = true
                    scope.launch {
                        snackbarHostState.showMessage("أرسلنا رمز تأكيد إلى بريدك الإلكتروني", SnackbarType.SUCCESS)
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showMessage(msg ?: "فشل حذف الحساب", SnackbarType.ERROR)
                    }
                }
            }
        }
    }

    val onDismissDeleteDialog: () -> Unit = {
        if (!isDeleting) {
            showDeleteAccountDialog = false
            deleteOtpStep = false
            deleteOtpCode = ""
            deletePassword = ""
        }
    }

    // كلمة المرور تخص حسابات البريد فقط — حساب Google وحده لا يملك كلمة مرور
    // محلية، فزر "تغيير كلمة المرور" له كان يفشل دائماً (إعادة المصادقة بالبريد
    // تحتاج كلمة مرور غير موجودة أصلاً).
    val hasPasswordProvider = user?.providerData?.any { it.providerId == "password" } == true

    Scaffold(
        snackbarHost = { ElevenSnackbarHost(snackbarHostState) },
        topBar = {
            ElevenTopBar(title = "الإعدادات", onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = 16.dp + padding.calculateTopPadding(),
                bottom = 32.dp + padding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── الحساب ──────────────────────────────────
            item {
                SettingsSection(title = "الحساب") {
                    AccountSection(
                        user = user,
                        isGoogleAccount = isGoogleAccount,
                        canChangePassword = hasPasswordProvider,
                        showChangePassword = showChangePassword,
                        onToggleChangePassword = { showChangePassword = !showChangePassword },
                        currentPassword = currentPassword,
                        onCurrentPasswordChange = { currentPassword = it },
                        newPassword = newPassword,
                        onNewPasswordChange = { newPassword = it },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        showCurrentPw = showCurrentPw,
                        onToggleShowCurrentPw = { showCurrentPw = !showCurrentPw },
                        showNewPw = showNewPw,
                        onToggleShowNewPw = { showNewPw = !showNewPw },
                        isLoading = isLoading,
                        onSavePassword = onSavePassword,
                        onCancelChangePassword = { showChangePassword = false },
                        onNavigateToLogin = onNavigateToLogin,
                    )
                }
            }

            // ── التفضيلات ───────────────────────────────
            item {
                SettingsSection(title = "التفضيلات") {
                    PreferencesSection(
                        pushEnabled = pushEnabled,
                        onPushChange = { enabled ->
                            pushEnabled = enabled
                            com.eleven.store.util.AppPreferences.setPushEnabled(context, enabled)
                        },
                    )
                }
            }

            // ── المساعدة والقانوني ───────────────────────
            item {
                SettingsSection(title = "المساعدة والقانوني") {
                    HelpSection(
                        onContact = onNavigateToContact,
                        onAbout = onNavigateToAbout,
                        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                        onOpenTermsOfService = onOpenTermsOfService,
                    )
                }
            }

            // ── تسجيل الخروج / حذف الحساب — لا معنى لهما بلا حساب مسجَّل دخول ──
            if (user != null) {
                item { LogoutButton(onClick = { showLogoutConfirm = true }) }
                item {
                    DeleteAccountButton(
                        onClick = {
                            deletePassword = ""
                            showDeleteAccountDialog = true
                        },
                    )
                }
            }

            // ── تذييل: رقم النسخة الفعلي من البناء (كان مكتوباً يدوياً 1.0.0) ──
            item {
                Text(
                    "Eleven Store — النسخة ${com.eleven.store.BuildConfig.VERSION_NAME}",
                    color = MutedForeground,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // ✅ إصلاح: تأكيد قبل تسجيل الخروج فعلياً — يمنع خروجاً غير مقصود بضغطة واحدة.
    LogoutConfirmDialog(
        show = showLogoutConfirm,
        onConfirm = {
            showLogoutConfirm = false
            viewModel.logout()
        },
        onDismiss = { showLogoutConfirm = false },
    )

    // ✅ نافذة تأكيد حذف الحساب — خطوتان الآن:
    // 1) إثبات الهوية: كلمة المرور الحالية (بريد/كلمة مرور) أو إعادة مصادقة
    //    Google، وينتج عنها طلب رمز تأكيد (OTP) يُرسَل للبريد.
    // 2) إدخال رمز الـ6 أرقام — الحذف الفعلي لا يحدث إلا بعد تأكيده بنجاح.
    DeleteAccountDialog(
        show = showDeleteAccountDialog,
        deleteOtpStep = deleteOtpStep,
        deleteOtpCode = deleteOtpCode,
        onOtpCodeChange = { if (it.length <= 6 && it.all(Char::isDigit)) deleteOtpCode = it },
        isDeleting = isDeleting,
        isGoogleAccount = isGoogleAccount,
        isResendingOtp = isResendingOtp,
        onResendOtp = onResendDeleteOtp,
        deletePassword = deletePassword,
        onDeletePasswordChange = { deletePassword = it },
        showDeletePw = showDeletePw,
        onToggleShowDeletePw = { showDeletePw = !showDeletePw },
        onConfirm = onConfirmDeleteStep,
        onDismiss = onDismissDeleteDialog,
    )
}

// ═══════════════════════════════════════════════════════════════
//  أقسام شاشة الإعدادات — كل قسم مستقل، بلا معرفة بتفاصيل الحالة
//  (فقط قيم للعرض ودوال جاهزة (lambdas) للأحداث)، عشان تسهل قراءة
//  وصيانة كل قسم لوحده بدل دالة واحدة ضخمة.
// ═══════════════════════════════════════════════════════════════

/** عنوان قسم صغير رمادي فوق البطاقة. */
@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 12.sp,
            color = MutedForeground,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        content()
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Column(content = content)
    }
}

/** صف إعداد قابل للضغط: أيقونة + نص (+ سطر ثانوي اختياري) + سهم. */
@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MutedForeground)
            }
        }
        Icon(Icons.Filled.ChevronLeft, null, tint = MutedForeground, modifier = Modifier.size(18.dp))
    }
}

/** صف بمفتاح تبديل (Switch): الصف كله قابل للضغط. */
@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MutedForeground)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Neutral300,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun AccountSection(
    user: com.google.firebase.auth.FirebaseUser?,
    isGoogleAccount: Boolean,
    canChangePassword: Boolean,
    showChangePassword: Boolean,
    onToggleChangePassword: () -> Unit,
    currentPassword: String,
    onCurrentPasswordChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    showCurrentPw: Boolean,
    onToggleShowCurrentPw: () -> Unit,
    showNewPw: Boolean,
    onToggleShowNewPw: () -> Unit,
    isLoading: Boolean,
    onSavePassword: () -> Unit,
    onCancelChangePassword: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    SettingsCard {
        if (user == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "سجّل الدخول لإدارة إعدادات حسابك",
                    color = MutedForeground,
                    fontSize = 14.sp,
                )
                ElevenButton(text = "تسجيل الدخول", onClick = onNavigateToLogin)
            }
            return@SettingsCard
        }

        // البريد + طريقة الدخول
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Email, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.email.orEmpty(),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (isGoogleAccount) "مسجَّل عبر Google" else "مسجَّل بالبريد وكلمة المرور",
                    color = MutedForeground,
                    fontSize = 12.sp,
                )
            }
        }

        if (canChangePassword) {
            HorizontalDivider(color = Border)
            SettingsRow(
                icon = Icons.Filled.Lock,
                label = "تغيير كلمة المرور",
                onClick = onToggleChangePassword,
            )
            if (showChangePassword) {
                ChangePasswordForm(
                    currentPassword = currentPassword,
                    onCurrentPasswordChange = onCurrentPasswordChange,
                    newPassword = newPassword,
                    onNewPasswordChange = onNewPasswordChange,
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = onConfirmPasswordChange,
                    showCurrentPw = showCurrentPw,
                    onToggleShowCurrentPw = onToggleShowCurrentPw,
                    showNewPw = showNewPw,
                    onToggleShowNewPw = onToggleShowNewPw,
                    isLoading = isLoading,
                    onSave = onSavePassword,
                    onCancel = onCancelChangePassword,
                )
            }
        }
    }
}

/** حقل كلمة مرور موحّد (كان منسوخاً حرفياً 3 مرات بالنموذج). */
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    imeAction: androidx.compose.ui.text.input.ImeAction,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp) },
        leadingIcon = {
            Icon(Icons.Filled.Lock, null, tint = MutedForeground, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    null,
                    tint = MutedForeground,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = Border,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
private fun ChangePasswordForm(
    currentPassword: String,
    onCurrentPasswordChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    showCurrentPw: Boolean,
    onToggleShowCurrentPw: () -> Unit,
    showNewPw: Boolean,
    onToggleShowNewPw: () -> Unit,
    isLoading: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PasswordField(
            value = currentPassword,
            onValueChange = onCurrentPasswordChange,
            placeholder = "كلمة المرور الحالية",
            visible = showCurrentPw,
            onToggleVisible = onToggleShowCurrentPw,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
        )
        PasswordField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            placeholder = "كلمة المرور الجديدة (8 أحرف على الأقل)",
            visible = showNewPw,
            onToggleVisible = onToggleShowNewPw,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
        )
        // زر الإظهار واحد للحقلين الجديدين — التأكيد يتبع نفس حالة الإظهار.
        PasswordField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = "تأكيد كلمة المرور الجديدة",
            visible = showNewPw,
            onToggleVisible = onToggleShowNewPw,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onSave,
                enabled = !isLoading &&
                    currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text(
                    if (isLoading) "جاري الحفظ..." else "حفظ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                )
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            ) {
                Text("إلغاء", fontSize = 14.sp, color = MutedForeground)
            }
        }
    }
}

@Composable
private fun PreferencesSection(
    pushEnabled: Boolean,
    onPushChange: (Boolean) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsCard {
        SettingsSwitchRow(
            icon = Icons.Filled.Notifications,
            label = "إشعارات الجوال",
            subtitle = "تنبيهات الطلبات والعروض على هاتفك",
            checked = pushEnabled,
            onCheckedChange = onPushChange,
        )
        HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 16.dp))
        // ✅ جديد: خيار يدوي لوضع العرض (نظام/فاتح/داكن) — قبل هذا كان
        // التطبيق يتبع وضع النظام فقط بلا أي تحكم من المستخدم.
        SettingsThemeRow(
            mode = com.eleven.store.util.ThemePrefs.current,
            onModeChange = { com.eleven.store.util.ThemePrefs.setMode(context, it) },
        )
    }
}

/** صف اختيار وضع العرض: نظام / فاتح / داكن — عبر ثلاث شرائح (Chips). */
@Composable
private fun SettingsThemeRow(
    mode: com.eleven.store.util.ThemeMode,
    onModeChange: (com.eleven.store.util.ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.DarkMode, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
            Text(
                "مظهر التطبيق",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val options = listOf(
                com.eleven.store.util.ThemeMode.SYSTEM to "تلقائي",
                com.eleven.store.util.ThemeMode.LIGHT to "فاتح",
                com.eleven.store.util.ThemeMode.DARK to "داكن",
            )
            options.forEach { (optionMode, optionLabel) ->
                val selected = mode == optionMode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Accent else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onModeChange(optionMode) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        optionLabel,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) AccentForeground else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpSection(
    onContact: () -> Unit,
    onAbout: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTermsOfService: () -> Unit,
) {
    SettingsCard {
        SettingsRow(
            icon = Icons.Filled.Phone,
            label = "اتصل بنا",
            onClick = onContact,
        )
        HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 16.dp))
        SettingsRow(
            icon = Icons.Filled.Info,
            label = "حول Eleven",
            onClick = onAbout,
        )
        HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 16.dp))
        SettingsRow(
            icon = Icons.Filled.PrivacyTip,
            label = "سياسة الخصوصية",
            onClick = onOpenPrivacyPolicy,
        )
        HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 16.dp))
        SettingsRow(
            icon = Icons.Filled.Description,
            label = "الشروط والأحكام",
            onClick = onOpenTermsOfService,
        )
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ExitToApp,
            null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "تسجيل الخروج",
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}

/** حذف الحساب: إجراء نادر ومدمِّر — زر نصي هادئ منفصل بدل صف بين إعدادات عادية. */
@Composable
private fun DeleteAccountButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(contentColor = Destructive),
    ) {
        Icon(Icons.Filled.DeleteForever, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("حذف الحساب", fontSize = 14.sp)
    }
}

@Composable
private fun LogoutConfirmDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل الخروج", fontWeight = FontWeight.Bold) },
        text = { Text("هل أنت متأكد أنك تريد تسجيل الخروج من حسابك؟", fontSize = 14.sp, color = MutedForeground) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("تسجيل الخروج", color = Destructive, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

@Composable
private fun DeleteAccountDialog(
    show: Boolean,
    deleteOtpStep: Boolean,
    deleteOtpCode: String,
    onOtpCodeChange: (String) -> Unit,
    isDeleting: Boolean,
    isGoogleAccount: Boolean,
    isResendingOtp: Boolean,
    onResendOtp: () -> Unit,
    deletePassword: String,
    onDeletePasswordChange: (String) -> Unit,
    showDeletePw: Boolean,
    onToggleShowDeletePw: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (deleteOtpStep) "أدخل رمز التأكيد" else "حذف الحساب نهائياً",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (deleteOtpStep) {
                    Text(
                        "أرسلنا رمزاً من 6 أرقام إلى بريدك الإلكتروني. أدخله أدناه لتأكيد حذف حسابك نهائياً — لا يمكن التراجع عن هذا الإجراء بعد التأكيد.",
                        fontSize = 14.sp,
                        color = MutedForeground,
                    )
                    OutlinedTextField(
                        value = deleteOtpCode,
                        onValueChange = onOtpCodeChange,
                        placeholder = { Text("رمز التأكيد", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, null, tint = MutedForeground, modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isDeleting,
                        shape = RoundedCornerShape(8.dp),
                    )
                    // ✅ إعادة الإرسال متاحة فقط لحسابات البريد/كلمة المرور —
                    // كلمة المرور محفوظة مؤقتاً بالحالة فيمكن إعادة طلب رمز
                    // جديد مباشرة. حسابات Google تحتاج إعادة مصادقة كاملة من
                    // جديد لإعادة الإرسال، فنطلب من المستخدم البدء من جديد.
                    if (!isGoogleAccount) {
                        TextButton(
                            enabled = !isDeleting && !isResendingOtp,
                            onClick = onResendOtp,
                        ) { Text(if (isResendingOtp) "جاري الإرسال..." else "لم يصلك الرمز؟ إعادة الإرسال") }
                    }
                } else {
                    Text(
                        "سيتم حذف حسابك وكل بياناته نهائياً، ولا يمكن التراجع عن هذا الإجراء.",
                        fontSize = 14.sp,
                        color = MutedForeground,
                    )
                    if (isGoogleAccount) {
                        Text(
                            "للمتابعة، يجب تأكيد هويتك عبر Google مرة أخرى.",
                            fontSize = 13.sp,
                            color = MutedForeground,
                        )
                    } else {
                        OutlinedTextField(
                            value = deletePassword,
                            onValueChange = onDeletePasswordChange,
                            placeholder = { Text("كلمة المرور الحالية", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, null, tint = MutedForeground, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = onToggleShowDeletePw) {
                                    Icon(
                                        if (showDeletePw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        null,
                                        tint = MutedForeground,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
                            visualTransformation = if (showDeletePw) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isDeleting,
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isDeleting && (
                    if (deleteOtpStep) deleteOtpCode.length == 6
                    else (isGoogleAccount || deletePassword.isNotBlank())
                ),
                colors = ButtonDefaults.buttonColors(containerColor = Destructive),
                onClick = onConfirm,
            ) {
                Text(
                    if (isDeleting) "جاري التنفيذ..." else if (deleteOtpStep) "تأكيد الحذف نهائياً" else "متابعة",
                    color = Color.White,
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                enabled = !isDeleting,
                onClick = onDismiss,
            ) { Text("إلغاء") }
        },
    )
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

    // ✅ توحيد سلوك الإشعارات: يعرض رسالة "الطلب غير موجود" مرة واحدة عند
    // التحويل من OrderDetailScreen (راجع MainViewModel.orderNotFoundMessage)،
    // ثم يستهلكها فوراً حتى لا تتكرر بأي إعادة تركيب لاحقة للشاشة.
    val orderNotFoundMessage by viewModel.orderNotFoundMessage.collectAsStateWithLifecycle()
    LaunchedEffect(orderNotFoundMessage) {
        orderNotFoundMessage?.let { message ->
            viewModel.consumeOrderNotFoundMessage()
            snackbarHostState.showSnackbar(message)
        }
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
                                // ✅ إصلاح: actionRoute مخزَّن بالسيرفر بشرطة بداية ("/order/xyz")
                                // بينما مسارات NavGraph بدون شرطة بداية ("order/{orderId}") —
                                // بدون إزالتها هنا كان التنقل يفشل بصمت (لا يحدث شيء عند الضغط)،
                                // رغم أن نفس المسار يعمل من تنبيه النظام لأن
                                // ElevenFirebaseMessagingService يزيل الشرطة هناك فقط.
                                notif.actionRoute?.removePrefix("/")?.takeIf { it.isNotBlank() }?.let(onOpenRoute)
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
                    // ✅ جديد (عرض صورة الإشعار): صورة مصغّرة عند توفر imageUrl —
                    // Coil (AsyncImage) يحمّلها بشكل Lazy تلقائياً فقط عند ظهور
                    // العنصر ضمن LazyColumn، مع placeholder/error موحّدين بنفس
                    // نمط بطاقات المنتجات أعلاه؛ لا يُكسر شيء إن فشل التحميل —
                    // فقط تبقى الأيقونة الرمزية ظاهرة كما كانت دائماً.
                    if (!notif.imageUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Neutral100),
                        ) {
                            AsyncImage(
                                model = notif.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                placeholder = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Image),
                                error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Image),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } else {
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
