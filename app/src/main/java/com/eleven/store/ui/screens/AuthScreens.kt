package com.eleven.store.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleven.store.ui.components.ElevenSnackbarHost
import com.eleven.store.ui.components.SnackbarType
import com.eleven.store.ui.components.showMessage
import com.eleven.store.ui.components.ElevenTopBar
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════
//  شعار Google — نفس مسارات SVG المستخدمة بالضبط في الموقع
//  (Login.tsx / Register.tsx) مبني هنا مباشرة بالكود بدون أي ملف
//  drawable منفصل، حتى يبقى كل شيء داخل هذا الملف فقط.
// ═══════════════════════════════════════════════════════════════

private val GoogleLogoIcon: androidx.compose.ui.graphics.vector.ImageVector by lazy {
    androidx.compose.ui.graphics.vector.ImageVector.Builder(
        name = "GoogleLogo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser()
                .parsePathString("M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z")
                .toNodes(),
            fill = androidx.compose.ui.graphics.SolidColor(Color(0xFF4285F4)),
        )
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser()
                .parsePathString("M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z")
                .toNodes(),
            fill = androidx.compose.ui.graphics.SolidColor(Color(0xFF34A853)),
        )
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser()
                .parsePathString("M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z")
                .toNodes(),
            fill = androidx.compose.ui.graphics.SolidColor(Color(0xFFFBBC05)),
        )
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser()
                .parsePathString("M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z")
                .toNodes(),
            fill = androidx.compose.ui.graphics.SolidColor(Color(0xFFEA4335)),
        )
    }.build()
}

// ═══════════════════════════════════════════════════════════════
//  ثوابت و دوال مساعدة مشتركة
// ═══════════════════════════════════════════════════════════════

private const val GOOGLE_WEB_CLIENT_ID =
    "418964206430-qge3vqln3bdv4rofe8q485fceg0emj55.apps.googleusercontent.com"

/** يبني GoogleSignInClient بنفس إعدادات الويب */
internal fun buildGoogleSignInClient(context: android.content.Context) =
    com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
        context,
        com.google.android.gms.auth.api.signin.GoogleSignInOptions
            .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
    )

// ═══════════════════════════════════════════════════════════════
//  AUTH VALIDATION — تحقق حقيقي من صيغة المدخلات (بدل isBlank فقط)
// ═══════════════════════════════════════════════════════════════

private object AuthValidation {

    // نفس نمط RFC-5322 المبسّط المستخدم عادة في نماذج الويب — يرفض
    // "asd" أو "asd@asd" لكنه يقبل أي بريد صحيح فعلياً
    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    // يقبل أرقام سعودية/دولية بصيغ شائعة: +9665xxxxxxxx أو 05xxxxxxxx أو دولي عام
    private val PHONE_REGEX = Regex("^\\+?[0-9]{8,15}$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun isValidPhone(phone: String): Boolean =
        phone.isBlank() || PHONE_REGEX.matches(phone.trim().replace(" ", ""))

    fun emailError(email: String): String? = when {
        email.isBlank() -> "يرجى إدخال البريد الإلكتروني"
        !isValidEmail(email) -> "صيغة البريد الإلكتروني غير صحيحة"
        else -> null
    }

    fun loginPasswordError(password: String): String? =
        if (password.isBlank()) "يرجى إدخال كلمة المرور" else null

    fun nameError(name: String): String? = when {
        name.trim().isBlank() -> "يرجى إدخال الاسم"
        name.trim().length < 3 -> "الاسم يجب أن يكون 3 أحرف على الأقل"
        else -> null
    }

    fun phoneError(phone: String): String? =
        if (!isValidPhone(phone)) "رقم الهاتف غير صحيح" else null

    fun passwordError(password: String): String? = when {
        password.isBlank() -> "يرجى إدخال كلمة المرور"
        password.length < 8 -> "كلمة المرور يجب أن تكون 8 أحرف على الأقل"
        else -> null
    }

    fun confirmPasswordError(password: String, confirm: String): String? = when {
        confirm.isBlank() -> "يرجى تأكيد كلمة المرور"
        password != confirm -> "كلمتا المرور غير متطابقتين"
        else -> null
    }

    /** قوة كلمة المرور: 0 = فارغة/ضعيفة جداً، 1 = ضعيفة، 2 = متوسطة، 3 = قوية */
    fun passwordStrength(password: String): Int {
        if (password.isEmpty()) return 0
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 10) score++
        if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return score.coerceIn(0, 3)
    }

    fun passwordStrengthLabel(strength: Int): String = when (strength) {
        0 -> "ضعيفة جداً"
        1 -> "ضعيفة"
        2 -> "متوسطة"
        else -> "قوية"
    }

    fun passwordStrengthColor(strength: Int): Color = when (strength) {
        0, 1 -> Color(0xFFEF4444)
        2 -> Color(0xFFF97316)
        else -> Color(0xFF22C55E)
    }
}

// ═══════════════════════════════════════════════════════════════
//  LOGO — مطابق للموقع: 11 / ELEVEN
//  <span className="text-4xl font-bold">11</span>
//  <span className="text-xs tracking-widest font-bold">ELEVEN</span>
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AuthLogo() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "11",
            color = Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            fontFamily = FontFamily.Serif,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "ELEVEN",
            color = Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 3.sp,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  ERROR BOX — مطابق للموقع:
//  <div className="p-4 bg-destructive/10 border border-destructive/30 text-destructive rounded-lg text-sm font-medium">
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AuthErrorBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Destructive.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(1.dp, Destructive.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Text(
            message,
            color = Destructive,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  OR DIVIDER — مطابق للموقع:
//  <div className="relative my-2">...<span>أو</span>...</div>
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OrDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalDivider(color = Border)
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                "أو",
                color = MutedForeground,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  GOOGLE BUTTON — مطابق للموقع:
//  <Button variant="outline" className="...gap-3 rounded-lg">
//    <svg>...Google Icon...</svg>
//    تسجيل الدخول عبر Google
//  </Button>
// ═══════════════════════════════════════════════════════════════

@Composable
private fun GoogleSignInButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
        ),
    ) {
        // شعار Google الحقيقي — مبني بالكود مباشرة بهذا الملف (بدون ملف drawable خارجي)
        Icon(
            imageVector = GoogleLogoIcon,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  AUTH CARD WRAPPER — مطابق للموقع:
//  <Card className="border-border bg-card overflow-hidden shadow-lg">
//    <div className="h-1 bg-accent"></div>
//    <CardContent className="p-8">
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AuthCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // ✅ يجبر المحتوى يمتد على كامل عرض البطاقة (بدون هذا قد ينكمش لعرض
        // أضيق عنصر ويظهر منزاح لجهة واحدة بدل أن يكون ممتد ومتمركز مثل الموقع)
        Column(modifier = Modifier.fillMaxWidth()) {
            // h-1 bg-accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Accent),
            )
            // p-8
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                content = content,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  INPUT FIELD WITH ICON — مطابق للموقع:
//  <div className="relative">
//    <Icon className="absolute right-3..." />
//    <Input className="pr-10 h-11 border-border bg-secondary/30 rounded-lg" />
//  </div>
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    // ✅ جديد: رسالة خطأ خاصة بهذا الحقل تظهر تحته مباشرة (بدل الاعتماد
    // فقط على صندوق خطأ عام أعلى النموذج لا يوضّح أي حقل فيه المشكلة)
    errorMessage: String? = null,
    imeAction: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
) {
    // ✅ جديد: يمرّر الحقل تلقائياً لأعلى منطقة مرئية فوق الكيبورد بمجرد
    // حصوله على التركيز (سواء بالضغط عليه مباشرة أو بالانتقال إليه عبر
    // "التالي" بالكيبورد من الحقل السابق) — بدل بقائه مخفياً جزئياً أو
    // كلياً خلف الكيبورد بانتظار تمرير يدوي من المستخدم. يعمل مع أي حقل
    // يستخدم AuthTextField تلقائياً بصرف النظر عن الشاشة (دخول/تسجيل/
    // استعادة كلمة مرور/رمز تأكيد)، بلا حاجة لتعديل كل شاشة على حدة.
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MutedForeground) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            // ✅ إصلاح: ارتفاع ثابت 44dp كان أقل من الحد الأدنى الذي يحتاجه
            // OutlinedTextField بحشوته الداخلية الافتراضية بـMaterial3، فكان
            // نص المستخدم (والـ placeholder) يُقصّ رأسياً ويظهر نصفه فقط.
            // heightIn(min) يسمح للحقل بأخذ الارتفاع الطبيعي الكافي دون قصّ،
            // بدل فرض قيمة أصغر من اللازم.
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        scope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                },
            singleLine = singleLine,
            enabled = enabled,
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Border,
                errorBorderColor = Destructive,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            ),
        )
        if (errorMessage != null) {
            Text(
                errorMessage,
                color = Destructive,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  LABEL — مطابق للموقع:
//  <label className="text-sm font-semibold text-foreground">
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FieldLabel(text: String, required: Boolean = false) {
    Row {
        Text(
            text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (required) {
            Text(
                " *",
                color = Destructive,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  LOGIN SCREEN — نسخة طبق الأصل من Login.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showForgotPassword by remember { mutableStateOf(false) }
    // ✅ جديد: تُملأ فقط عند محاولة دخول بحساب بريد غير مؤكَّد — تفتح شاشة
    // إدخال رمز التأكيد مباشرة بدل ترك المستخدم برسالة نصية بلا إجراء.
    var pendingVerificationEmail by remember { mutableStateOf<String?>(null) }
    // ✅ جديد: أخطاء لحظية لكل حقل — تُحسب فقط بعد أول محاولة إرسال حتى لا
    // تظهر رسائل حمراء للمستخدم قبل ما يكتب أي شيء أصلاً
    var attemptedSubmit by remember { mutableStateOf(false) }
    val emailError = if (attemptedSubmit) AuthValidation.emailError(email) else null
    val passwordError = if (attemptedSubmit) AuthValidation.loginPasswordError(password) else null
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val googleClient = remember { buildGoogleSignInClient(context) }
    val googleLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            account?.idToken?.let { idToken ->
                isLoading = true
                viewModel.signInWithGoogle(idToken) { ok, msg ->
                    isLoading = false
                    if (ok) onLoginSuccess()
                    else error = msg ?: "فشل تسجيل الدخول عبر Google"
                }
            } ?: run { error = "فشل تسجيل الدخول عبر Google" }
        } catch (_: Exception) {
            error = "فشل تسجيل الدخول عبر Google"
        }
    }

    // ── حالة نسيت كلمة المرور ──
    if (showForgotPassword) {
        ForgotPasswordContent(
            viewModel = viewModel,
            onBackToLogin = { showForgotPassword = false },
        )
        return
    }

    // ── حالة: بريد غير مؤكَّد، بانتظار إدخال رمز التأكيد ──
    pendingVerificationEmail?.let { emailNeedingVerification ->
        VerifyEmailOtpContent(
            email = emailNeedingVerification,
            viewModel = viewModel,
            onVerified = { pendingVerificationEmail = null },
            onBack = { pendingVerificationEmail = null },
        )
        return
    }

    // ════════ شاشة تسجيل الدخول ════════
    Scaffold(
        topBar = { ElevenTopBar(title = "تسجيل الدخول") }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                AuthCard {
                    // ── Logo + Title ──────────────────────────
                    AuthLogo()
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "تسجيل الدخول",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "أهلاً بك في متجرنا",
                        color = MutedForeground,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(32.dp))

                    // ── Error ─────────────────────────────────
                    if (error.isNotEmpty()) {
                        AuthErrorBox(error)
                        Spacer(Modifier.height(20.dp))
                    }

                    // ── Email ─────────────────────────────────
                    FieldLabel("البريد الإلكتروني", required = true)
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            error = ""
                        },
                        placeholder = "your@email.com",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Email,
                                null,
                                tint = MutedForeground,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        keyboardType = KeyboardType.Email,
                        enabled = !isLoading,
                        errorMessage = emailError,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                        ),
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Password ──────────────────────────────
                    // ✅ نُقل رابط "نسيت كلمة المرور؟" من هنا إلى أسفل زر تسجيل
                    // الدخول بناءً على طلب مباشر — كان بجانب تسمية الحقل سابقاً.
                    FieldLabel("كلمة المرور", required = true)
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            error = ""
                        },
                        placeholder = "••••••••",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Lock,
                                null,
                                tint = MutedForeground,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    null,
                                    tint = MutedForeground,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        enabled = !isLoading,
                        errorMessage = passwordError,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Login Button ──────────────────────────
                    Button(
                        onClick = {
                            attemptedSubmit = true
                            // ✅ تحقق محلي فوري قبل مناداة Firebase — يمنع طلب شبكة
                            // غير ضروري لبريد بصيغة خاطئة واضحة، ويوجّه المستخدم
                            // فوراً لمكان الخطأ بدل رسالة عامة بعد انتظار الشبكة
                            val localError = AuthValidation.emailError(email)
                                ?: AuthValidation.loginPasswordError(password)
                            if (localError != null) return@Button

                            isLoading = true
                            error = ""
                            viewModel.login(email.trim(), password) { ok, msg, unverifiedEmail ->
                                isLoading = false
                                if (ok) onLoginSuccess()
                                else if (unverifiedEmail != null) pendingVerificationEmail = unverifiedEmail
                                else error = msg ?: "فشل تسجيل الدخول"
                            }
                        },
                        // ✅ يطابق سلوك الموقع بالضبط: disabled={loading} فقط دون تعطيل حسب الحقول
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Color.White,
                        ),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("جاري التحميل...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Text("تسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    // ── نسيت كلمة المرور؟ ──────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextButton(onClick = { showForgotPassword = true }) {
                            Text(
                                "نسيت كلمة المرور؟",
                                color = Accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // ── Or Divider ────────────────────────────
                    OrDivider()

                    // ── Google Button ─────────────────────────
                    GoogleSignInButton(
                        text = "تسجيل الدخول عبر Google",
                        enabled = !isLoading,
                        onClick = {
                            // ✅ إصلاح: GoogleSignInClient يحتفظ بآخر حساب مُستخدَم ويعيد
                            // استخدامه بصمت بدون عرض قائمة اختيار الحساب — حتى لو المستخدم
                            // سجّل خروجه من التطبيق (signOut هنا هو Firebase Auth فقط، لا
                            // يمسح ذاكرة GoogleSignInClient نفسه). نستدعي signOut() على
                            // عميل جوجل تحديداً قبل كل محاولة دخول لإجباره على نسيان الحساب
                            // المخزَّن وعرض كل الحسابات المتاحة على الجهاز من جديد.
                            googleClient.signOut().addOnCompleteListener {
                                googleLauncher.launch(googleClient.signInIntent)
                            }
                        },
                    )

                    // ── Register Link ─────────────────────────
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "ليس لديك حساب؟ ",
                            color = MutedForeground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "إنشاء حساب جديد",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable(onClick = onNavigateToRegister),
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  FORGOT PASSWORD — خطوتان الآن بدل رابط: (1) طلب رمز عبر البريد،
//  (2) إدخال الرمز + كلمة مرور جديدة معاً لإتمام إعادة التعيين.
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ForgotPasswordContent(
    viewModel: MainViewModel,
    onBackToLogin: () -> Unit,
) {
    var forgotEmail by remember { mutableStateOf("") }
    var forgotLoading by remember { mutableStateOf(false) }
    // ✅ null = لسه بخطوة إدخال البريد، غير null = الرمز أُرسل، ننتقل لخطوة
    // إدخال الرمز + كلمة المرور الجديدة لنفس هذا البريد تحديداً
    var otpSentTo by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val sentTo = otpSentTo
    if (sentTo != null) {
        ResetPasswordOtpContent(
            email = sentTo,
            viewModel = viewModel,
            onResetSuccess = onBackToLogin,
            onBack = { otpSentTo = null },
        )
        return
    }

    Scaffold(
        snackbarHost = { ElevenSnackbarHost(snackbarHostState) },
        topBar = { ElevenTopBar(title = "استعادة كلمة المرور", onBack = onBackToLogin) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AuthCard {
                    AuthLogo()
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "استعادة كلمة المرور",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "سنرسل رمز تأكيد إلى بريدك الإلكتروني",
                        color = MutedForeground,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(28.dp))

                    FieldLabel("البريد الإلكتروني", required = true)
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        placeholder = "your@email.com",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Email,
                                null,
                                tint = MutedForeground,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        keyboardType = KeyboardType.Email,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (forgotEmail.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showMessage("يرجى إدخال بريدك الإلكتروني", SnackbarType.ERROR)
                                }
                                return@Button
                            }
                            forgotLoading = true
                            val emailToSend = forgotEmail.trim()
                            // ✅ لا نكشف للمستخدم إن كان البريد مسجَّلاً بحساب من عدمه —
                            // نفس الرسالة دائماً (sendPasswordResetOtp بالسيرفر يتصرف
                            // بنفس المنطق من جهته)، وننتقل لخطوة إدخال الرمز في الحالتين.
                            viewModel.requestPasswordResetOtp(emailToSend) { _ ->
                                forgotLoading = false
                                otpSentTo = emailToSend
                            }
                        },
                        enabled = forgotEmail.isNotBlank() && !forgotLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Color.White,
                        ),
                    ) {
                        if (forgotLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("جاري الإرسال...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Text("إرسال رمز التأكيد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    TextButton(
                        onClick = onBackToLogin,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "العودة لتسجيل الدخول",
                            color = MutedForeground,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  RESET PASSWORD OTP — الخطوة الثانية: رمز التأكيد + كلمة المرور
//  الجديدة معاً، تحقق وتغيير بطلب واحد (confirmPasswordResetOtp)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ResetPasswordOtpContent(
    email: String,
    viewModel: MainViewModel,
    onResetSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var resending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Scaffold(
        snackbarHost = { ElevenSnackbarHost(snackbarHostState) },
        topBar = { ElevenTopBar(title = "رمز التأكيد", onBack = onBack) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AuthCard {
                    AuthLogo()
                    Spacer(Modifier.height(24.dp))
                    Icon(
                        Icons.Filled.MarkEmailRead,
                        null,
                        tint = Accent,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "أدخل رمز التأكيد",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "أرسلنا رمزاً من 6 أرقام إلى $email",
                        color = MutedForeground,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(28.dp))

                    if (error.isNotEmpty()) {
                        AuthErrorBox(error)
                        Spacer(Modifier.height(20.dp))
                    }

                    FieldLabel("رمز التأكيد", required = true)
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { otp = it; error = "" } },
                        placeholder = "000000",
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, null, tint = MutedForeground, modifier = Modifier.size(22.dp))
                        },
                        keyboardType = KeyboardType.Number,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                        ),
                    )

                    Spacer(Modifier.height(20.dp))

                    FieldLabel("كلمة المرور الجديدة", required = true)
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; error = "" },
                        placeholder = "••••••••",
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, null, tint = MutedForeground, modifier = Modifier.size(22.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    null, tint = MutedForeground, modifier = Modifier.size(22.dp),
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                        ),
                    )

                    Spacer(Modifier.height(20.dp))

                    FieldLabel("تأكيد كلمة المرور", required = true)
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; error = "" },
                        placeholder = "••••••••",
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, null, tint = MutedForeground, modifier = Modifier.size(22.dp))
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            when {
                                otp.length != 6 -> error = "أدخل رمز التأكيد المكوّن من 6 أرقام"
                                AuthValidation.passwordError(newPassword) != null ->
                                    error = AuthValidation.passwordError(newPassword)!!
                                newPassword != confirmPassword -> error = "كلمتا المرور غير متطابقتين"
                                else -> {
                                    isLoading = true
                                    error = ""
                                    viewModel.confirmPasswordResetOtp(email, otp, newPassword) { ok, msg ->
                                        isLoading = false
                                        if (ok) {
                                            scope.launch {
                                                snackbarHostState.showMessage(
                                                    "تم تغيير كلمة المرور بنجاح، سجّل الدخول بكلمة المرور الجديدة",
                                                    SnackbarType.SUCCESS,
                                                )
                                            }
                                            onResetSuccess()
                                        } else {
                                            error = msg ?: "تعذّر إعادة تعيين كلمة المرور"
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("جاري التأكيد...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Text("تأكيد وتغيير كلمة المرور", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            resending = true
                            viewModel.requestPasswordResetOtp(email) {
                                resending = false
                                scope.launch {
                                    snackbarHostState.showMessage("تم إرسال رمز جديد إلى بريدك", SnackbarType.SUCCESS)
                                }
                            }
                        },
                        enabled = !resending,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (resending) "جاري الإرسال..." else "لم يصلك الرمز؟ إعادة الإرسال",
                            color = Accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  VERIFY EMAIL OTP — تظهر بعد إنشاء الحساب مباشرة (التحقق إجباري
//  الآن، فالحساب الجديد لا يُبقي المستخدم مسجّل دخول)، وأيضاً عند محاولة
//  دخول لاحقة بحساب لم يُؤكَّد بعد (راجع LoginScreen). إدخال رمز مكوَّن
//  من 6 أرقام بدل فتح رابط خارجي — نفس فلسفة ResetPasswordOtpContent.
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VerifyEmailOtpContent(
    email: String,
    sendFailed: Boolean = false,
    viewModel: MainViewModel,
    onVerified: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var resending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { ElevenSnackbarHost(snackbarHostState) },
        topBar = { ElevenTopBar(title = "تأكيد البريد الإلكتروني", onBack = onBack) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AuthCard {
                    AuthLogo()
                    Spacer(Modifier.height(24.dp))
                    Icon(
                        Icons.Filled.MarkEmailRead,
                        null,
                        tint = Accent,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "أدخل رمز التأكيد",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (sendFailed)
                            "تم إنشاء حسابك بنجاح، لكن تعذّر إرسال رمز التأكيد الآن. اضغط \"إعادة الإرسال\" أدناه."
                        else
                            "أرسلنا رمزاً من 6 أرقام إلى${if (email.isNotBlank()) " $email" else " بريدك الإلكتروني"}",
                        color = MutedForeground,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(28.dp))

                    if (error.isNotEmpty()) {
                        AuthErrorBox(error)
                        Spacer(Modifier.height(20.dp))
                    }

                    FieldLabel("رمز التأكيد", required = true)
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { otp = it; error = "" } },
                        placeholder = "000000",
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, null, tint = MutedForeground, modifier = Modifier.size(22.dp))
                        },
                        keyboardType = KeyboardType.Number,
                        enabled = !isLoading,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (otp.length != 6) {
                                error = "أدخل رمز التأكيد المكوّن من 6 أرقام"
                                return@Button
                            }
                            isLoading = true
                            error = ""
                            viewModel.confirmEmailVerificationOtp(email, otp) { ok, msg ->
                                isLoading = false
                                if (ok) {
                                    scope.launch {
                                        snackbarHostState.showMessage("تم تأكيد بريدك بنجاح، يمكنك تسجيل الدخول الآن", SnackbarType.SUCCESS)
                                    }
                                    onVerified()
                                } else {
                                    error = msg ?: "تعذّر تأكيد البريد"
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("جاري التأكيد...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Text("تأكيد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            resending = true
                            viewModel.resendEmailVerificationOtpByEmail(email) {
                                resending = false
                                scope.launch {
                                    snackbarHostState.showMessage("تم إرسال رمز جديد إلى بريدك", SnackbarType.SUCCESS)
                                }
                            }
                        },
                        enabled = !resending,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (resending) "جاري الإرسال..." else "لم يصلك الرمز؟ إعادة الإرسال",
                            color = Accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  REGISTER SCREEN — نسخة طبق الأصل من Register.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
fun RegisterScreen(
    viewModel: MainViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var agreeTerms by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    // ✅ جديد: التسجيل الآن لا يُدخل المستخدم للتطبيق مباشرة (التحقق إجباري) —
    // بعد نجاح إنشاء الحساب نعرض هذه الشاشة بدل تنفيذ onRegisterSuccess فوراً.
    var showVerifyEmailSent by remember { mutableStateOf(false) }
    var registeredEmail by remember { mutableStateOf("") }
    // ✅ جديد: هل فشل إرسال رابط التأكيد فعلياً؟ لعرض تنبيه صادق بدل افتراض
    // نجاح الإرسال دائماً (كان بيتبلع بصمت سابقاً).
    var verificationEmailFailed by remember { mutableStateOf(false) }
    // ✅ تشخيص: وصف الاستثناء الفعلي (نوعه/كوده/رسالته) لما إرسال رابط
    // التأكيد يفشل، عشان يظهر بالشاشة نفسها بدل ما يضيع بـLog.w محلي غير
    // متاح بدون Logcat — هذا اللي يحسم هل الفشل محلي (مثلاً شبكة/توكن) أو
    // وصل فعلاً للسيرفر وفشل هناك.
    var verificationErrorDetail by remember { mutableStateOf<String?>(null) }
    // ✅ جديد: تظهر أخطاء الحقول فقط بعد أول محاولة إرسال
    var attemptedSubmit by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val nameError = if (attemptedSubmit) AuthValidation.nameError(name) else null
    val emailError = if (attemptedSubmit) AuthValidation.emailError(email) else null
    val phoneError = if (attemptedSubmit) AuthValidation.phoneError(phone) else null
    val passwordError = if (attemptedSubmit) AuthValidation.passwordError(password) else null
    // ✅ تطابق التأكيد يظهر بمجرد ما المستخدم يبدأ يكتب فيه (مو لازم ينتظر submit)
    // — تجربة أفضل، لأن هذا الحقل بطبيعته تفاعلي أثناء الكتابة
    val confirmPasswordError = if (confirmPassword.isNotEmpty() || attemptedSubmit)
        AuthValidation.confirmPasswordError(password, confirmPassword) else null
    val passwordStrength = AuthValidation.passwordStrength(password)

    val googleClient = remember { buildGoogleSignInClient(context) }
    val googleLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            account?.idToken?.let { idToken ->
                isLoading = true
                viewModel.signInWithGoogle(idToken) { ok, msg ->
                    isLoading = false
                    if (ok) onRegisterSuccess()
                    else error = msg ?: "فشل التسجيل عبر Google"
                }
            } ?: run { error = "فشل التسجيل عبر Google" }
        } catch (_: Exception) {
            error = "فشل التسجيل عبر Google"
        }
    }

    // ── حالة: تم إنشاء الحساب وأُرسل رمز التأكيد ──
    if (showVerifyEmailSent) {
        VerifyEmailOtpContent(
            email = registeredEmail,
            sendFailed = verificationEmailFailed,
            viewModel = viewModel,
            onVerified = onNavigateToLogin,
            onBack = onNavigateToLogin,
        )
        return
    }

    // ✅ تحقق أدق من قواعد صيغة البريد/الهاتف، بدل isBlank فقط سابقاً
    fun validate(): String? {
        AuthValidation.nameError(name)?.let { return it }
        AuthValidation.emailError(email)?.let { return it }
        AuthValidation.phoneError(phone)?.let { return it }
        AuthValidation.passwordError(password)?.let { return it }
        AuthValidation.confirmPasswordError(password, confirmPassword)?.let { return it }
        if (!agreeTerms) return "يجب الموافقة على الشروط والأحكام"
        return null
    }

    Scaffold(
        topBar = { ElevenTopBar(title = "إنشاء حساب") }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AuthCard {
                    // ── Logo + Title ──────────────────────────
                    AuthLogo()
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "إنشاء حساب جديد",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "انضم إلينا واستمتع بالتسوق المميز",
                        color = MutedForeground,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(28.dp))

                    // ── Error ─────────────────────────────────
                    if (error.isNotEmpty()) {
                        AuthErrorBox(error)
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── Name ──────────────────────────────────
                    FieldLabel("الاسم الكامل", required = true)
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = name,
                        onValueChange = { name = it; error = "" },
                        placeholder = "اسمك",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Person,
                                null,
                                tint = MutedForeground,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        enabled = !isLoading,
                        errorMessage = nameError,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                        ),
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Email ─────────────────────────────────
                    FieldLabel("البريد الإلكتروني", required = true)
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it; error = "" },
                        placeholder = "your@email.com",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Email,
                                null,
                                tint = MutedForeground,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        keyboardType = KeyboardType.Email,
                        enabled = !isLoading,
                        errorMessage = emailError,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                        ),
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Phone (optional) ──────────────────────
                    Row {
                        Text(
                            "رقم الهاتف ",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "(اختياري)",
                            fontSize = 12.sp,
                            color = MutedForeground,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = "0912345678",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Phone,
                                null,
                                tint = MutedForeground,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        keyboardType = KeyboardType.Phone,
                        enabled = !isLoading,
                        errorMessage = phoneError,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                        ),
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Password ──────────────────────────────
                    FieldLabel("كلمة المرور", required = true)
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = password,
                        onValueChange = { password = it; error = "" },
                        placeholder = "••••••••",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Lock,
                                null,
                                tint = MutedForeground,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    null,
                                    tint = MutedForeground,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        enabled = !isLoading,
                        errorMessage = passwordError,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                        ),
                    )

                    // ── Password Strength Meter ───────────────
                    // ✅ جديد: يعطي المستخدم إشارة فورية عن قوة كلمة المرور
                    // وهو يكتب، بدل ما يكتشف إنها "ضعيفة" فقط بعد محاولة الإرسال
                    if (password.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(modifier = Modifier.weight(1f)) {
                                repeat(3) { index ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .padding(end = if (index < 2) 4.dp else 0.dp)
                                            .background(
                                                if (index < passwordStrength)
                                                    AuthValidation.passwordStrengthColor(passwordStrength)
                                                else Border,
                                                RoundedCornerShape(2.dp),
                                            )
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                AuthValidation.passwordStrengthLabel(passwordStrength),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AuthValidation.passwordStrengthColor(passwordStrength),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Confirm Password ──────────────────────
                    FieldLabel("تأكيد كلمة المرور", required = true)
                    Spacer(Modifier.height(8.dp))
                    AuthTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; error = "" },
                        placeholder = "••••••••",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Lock,
                                null,
                                tint = MutedForeground,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        trailingIcon = {
                            // ✅ عند التطابق يظهر تلميح أخضر بدل مجرد أيقونة إظهار/إخفاء،
                            // تأكيد بصري فوري للمستخدم إنه كتب كلمة المرور صح
                            if (confirmPassword.isNotEmpty() && password == confirmPassword) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(22.dp),
                                )
                            } else {
                                IconButton(onClick = { showConfirm = !showConfirm }) {
                                    Icon(
                                        if (showConfirm) Icons.Filled.VisibilityOff
                                        else Icons.Filled.Visibility,
                                        null,
                                        tint = MutedForeground,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        },
                        visualTransformation = if (showConfirm) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        enabled = !isLoading,
                        errorMessage = confirmPasswordError,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Agree Terms ───────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp),
                            )
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.clickable { agreeTerms = !agreeTerms },
                            verticalAlignment = Alignment.Top,
                        ) {
                            Checkbox(
                                checked = agreeTerms,
                                onCheckedChange = { agreeTerms = it },
                                colors = CheckboxDefaults.colors(checkedColor = Accent),
                            )
                            Spacer(Modifier.width(8.dp))
                            val termsText = androidx.compose.ui.text.buildAnnotatedString {
                                append("أوافق على ")
                                withStyle(
                                    androidx.compose.ui.text.SpanStyle(
                                        color = Accent,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                ) { append("شروط الاستخدام") }
                                append(" و")
                                withStyle(
                                    androidx.compose.ui.text.SpanStyle(
                                        color = Accent,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                ) { append("سياسة الخصوصية") }
                            }
                            Text(
                                termsText,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Register Button ───────────────────────
                    Button(
                        onClick = {
                            attemptedSubmit = true
                            val validationError = validate()
                            if (validationError != null) {
                                error = validationError
                                return@Button
                            }
                            isLoading = true
                            error = ""
                            viewModel.register(
                                name.trim(),
                                email.trim(),
                                phone.trim(),
                                password
                            ) { ok, msg, verificationEmailSent, verificationEmailErrorDetail ->
                                isLoading = false
                                if (ok) {
                                    registeredEmail = email.trim()
                                    verificationEmailFailed = !verificationEmailSent
                                    verificationEmailErrorDetail?.let { verificationErrorDetail = it }
                                    showVerifyEmailSent = true
                                } else error = msg ?: "فشل إنشاء الحساب، يرجى المحاولة مرة أخرى"
                            }
                        },
                        // ✅ إصلاح: كان الزر مفعّلاً دائماً (enabled = !isLoading فقط)،
                        // والموافقة على الشروط تُفحص فقط عند الضغط (validate())، فيبدو
                        // الزر جاهزاً للاستخدام رغم أن الضغط عليه بلا موافقة سابقة كان
                        // سيُرجع خطأً فوراً. تعطيله فعلياً حتى الموافقة يعكس الحالة
                        // الحقيقية للنموذج بدل الاعتماد على رسالة خطأ بعد الضغط.
                        enabled = !isLoading && agreeTerms,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Color.White,
                        ),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("جاري الإنشاء...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Text("إنشاء الحساب", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    // ── Or Divider ────────────────────────────
                    OrDivider()

                    // ── Google Button ─────────────────────────
                    GoogleSignInButton(
                        text = "التسجيل عبر Google",
                        enabled = !isLoading,
                        onClick = {
                            // ✅ إصلاح: نفس مشكلة شاشة تسجيل الدخول — إجبار عميل جوجل على
                            // نسيان الحساب المخزَّن قبل فتح قائمة اختيار الحساب.
                            googleClient.signOut().addOnCompleteListener {
                                googleLauncher.launch(googleClient.signInIntent)
                            }
                        },
                    )

                    // ── Login Link ────────────────────────────
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "هل لديك حساب بالفعل؟ ",
                            color = MutedForeground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "تسجيل الدخول",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable(onClick = onNavigateToLogin),
                        )
                    }
                }
            }
        }
    }
}