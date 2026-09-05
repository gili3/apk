package com.eleven.store.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
private fun buildGoogleSignInClient(context: android.content.Context) =
    com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
        context,
        com.google.android.gms.auth.api.signin.GoogleSignInOptions
            .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
    )

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
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MutedForeground) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = Border,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    )
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

    // ════════ شاشة تسجيل الدخول ════════
    Scaffold(
        topBar = { ElevenTopBar(title = "تسجيل الدخول") }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
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
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Password ──────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FieldLabel("كلمة المرور", required = true)
                        TextButton(
                            onClick = { showForgotPassword = true },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(
                                "نسيت كلمة المرور؟",
                                color = Accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
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
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Login Button ──────────────────────────
                    Button(
                        onClick = {
                            isLoading = true
                            error = ""
                            viewModel.login(email, password) { ok, msg ->
                                isLoading = false
                                if (ok) onLoginSuccess()
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

                    // ── Or Divider ────────────────────────────
                    OrDivider()

                    // ── Google Button ─────────────────────────
                    GoogleSignInButton(
                        text = "تسجيل الدخول عبر Google",
                        enabled = !isLoading,
                        onClick = {
                            googleLauncher.launch(googleClient.signInIntent)
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
//  FORGOT PASSWORD — مطابقة لحالة showForgot في Login.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ForgotPasswordContent(
    viewModel: MainViewModel,
    onBackToLogin: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var forgotEmail by remember { mutableStateOf("") }
    var forgotLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { ElevenSnackbarHost(snackbarHostState) },
        topBar = { ElevenTopBar(title = "استعادة كلمة المرور", onBack = onBackToLogin) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
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
                        "سنرسل رابط الاسترداد إلى بريدك الإلكتروني",
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
                            // ✅ يطابق سلوك الموقع بالضبط: استدعاء Firebase الحقيقي
                            // sendPasswordResetEmail(auth, forgotEmail) بدل المحاكاة الوهمية
                            viewModel.sendPasswordReset(forgotEmail) { success ->
                                forgotLoading = false
                                scope.launch {
                                    if (success) {
                                        snackbarHostState.showMessage(
                                            "تم إرسال رابط إعادة تعيين كلمة المرور إلى بريدك الإلكتروني",
                                            SnackbarType.SUCCESS
                                        )
                                        forgotEmail = ""
                                        onBackToLogin()
                                    } else {
                                        snackbarHostState.showMessage(
                                            "لم يتم العثور على حساب بهذا البريد الإلكتروني",
                                            SnackbarType.ERROR
                                        )
                                    }
                                }
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
                            Text("إرسال رابط الاسترداد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

    // ✅ نفس قواعد التحقق من Register.tsx
    fun validate(): String? {
        if (name.trim().length < 3) return "الاسم يجب أن يكون 3 أحرف على الأقل"
        if (email.isBlank()) return "يرجى إدخال البريد الإلكتروني"
        if (password.length < 6) return "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
        if (password != confirmPassword) return "كلمتا المرور غير متطابقتين"
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
                        placeholder = "أحمد محمد",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Person,
                                null,
                                tint = MutedForeground,
                                modifier = Modifier.size(22.dp),
                            )
                        },
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
                        placeholder = "+966501234567",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Phone,
                                null,
                                tint = MutedForeground,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        keyboardType = KeyboardType.Phone,
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
                    )

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
                            IconButton(onClick = { showConfirm = !showConfirm }) {
                                Icon(
                                    if (showConfirm) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    null,
                                    tint = MutedForeground,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        },
                        visualTransformation = if (showConfirm) VisualTransformation.None
                        else PasswordVisualTransformation(),
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
                            val validationError = validate()
                            if (validationError != null) {
                                error = validationError
                                return@Button
                            }
                            isLoading = true
                            error = ""
                            viewModel.register(
                                name.trim(),
                                email,
                                phone,
                                password
                            ) { ok, msg ->
                                isLoading = false
                                if (ok) onRegisterSuccess()
                                else error = msg ?: "فشل إنشاء الحساب، يرجى المحاولة مرة أخرى"
                            }
                        },
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
                            googleLauncher.launch(googleClient.signInIntent)
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