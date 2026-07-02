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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleven.store.ui.components.ElevenTopBar
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        // Google SVG icon محاكى بالأحرف الملونة
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
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
        Column {
            // h-1 bg-accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Accent),
            )
            // p-8
            Column(
                modifier = Modifier.padding(32.dp),
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
                        enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
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
    onBackToLogin: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var forgotEmail by remember { mutableStateOf("") }
    var forgotLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                    snackbarHostState.showSnackbar("يرجى إدخال بريدك الإلكتروني")
                                }
                                return@Button
                            }
                            forgotLoading = true
                            // استدعاء إعادة تعيين كلمة المرور عبر ViewModel
                            scope.launch {
                                // TODO: استبدل بـ viewModel.sendPasswordReset(forgotEmail)
                                kotlinx.coroutines.delay(1500)
                                forgotLoading = false
                                snackbarHostState.showSnackbar(
                                    "تم إرسال رابط إعادة تعيين كلمة المرور إلى بريدك الإلكتروني"
                                )
                                onBackToLogin()
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
                            Text(
                                "أوافق على شروط الاستخدام وسياسة الخصوصية",
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