package com.eleven.store.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════
//  ELEVEN STORE — DESIGN TOKENS (SINGLE SOURCE OF TRUTH)
//  هذا الملف هو المصدر الوحيد لكل ألوان تطبيق الأندرويد.
//  يقابله في الموقع: client/src/lib/colors.ts + client/src/index.css
//  أي لون جديد يُضاف هنا فقط، ولا يُكتب Hex مباشرة داخل الشاشات.
// ═══════════════════════════════════════════════════════════════

// ── الأساس (المرتكزات الثلاثة المطلوبة) ──────────────────────────
val Ink        = Color(0xFF1A1A1A) // أسود دافئ - Foreground
val PureWhite  = Color(0xFFFFFFFF) // أبيض - Background
val SlateBrand = Color(0xFF607D8B) // Blue-Grey - اللون الأساسي الجديد للعلامة

// ── تدرّج محايد (Neutral scale) — مبني من Ink إلى White بميل بارد خفيف ─
val Neutral50  = Color(0xFFF5F7F8)
val Neutral100 = Color(0xFFECEFF1)
val Neutral200 = Color(0xFFDDE3E6)
val Neutral300 = Color(0xFFC2CBCF)
val Neutral400 = Color(0xFF9FACB2)
val Neutral500 = Color(0xFF78909C)
val Neutral600 = Color(0xFF5F7580)
val Neutral700 = Color(0xFF46565F)
val Neutral800 = Color(0xFF2E383D)
val Neutral900 = Ink

// ── تدرّج العلامة (Primary scale) — مبني من 607D8B ───────────────
val Primary50  = Color(0xFFEDF1F3)
val Primary100 = Color(0xFFDCE4E8)
val Primary200 = Color(0xFFB7C7CE)
val Primary300 = Color(0xFF94AAB4)
val Primary400 = Color(0xFF7B96A1)
val Primary500 = SlateBrand
val Primary600 = Color(0xFF4E6873)
val Primary700 = Color(0xFF3D525B)
val Primary800 = Color(0xFF2C3C43)
val Primary900 = Color(0xFF1C262A)

// ── لون التمييز الدافئ (Accent) — يوازن برودة الأزرق الرمادي ──────
val Accent100 = Color(0xFFF3E7DC)
val Accent500 = Color(0xFFB4805A)
val Accent600 = Color(0xFF96683F)

// ── الألوان الدلالية (Semantic) ──────────────────────────────────
val Success   = Color(0xFF3F8F5F)
val SuccessBg = Color(0x1A3F8F5F)
val SuccessBorder = Color(0x333F8F5F)
val Warning   = Color(0xFFC98A2E)
val WarningBg = Color(0x1AC98A2E)
val Destructive   = Color(0xFFC6483B)
val DestructiveBg = Color(0x1AC6483B)
val Info      = Primary500
val InfoBg    = Color(0x1A607D8B)

// ── أدوار واجهة (Semantic UI roles - LIGHT) ──────────────────────
val Primary            = Primary500
val PrimaryForeground  = PureWhite
val PrimaryBg          = Color(0x1A607D8B)

val Background = PureWhite
val Foreground = Ink

val CardBg           = PureWhite
val CardForeground   = Ink

val Secondary           = Neutral50
val SecondaryForeground = Ink

val Muted           = Neutral100
val MutedForeground = Neutral500

val Accent           = Accent500
val AccentForeground = PureWhite

val DestructiveForeground = PureWhite
val SuccessForeground     = PureWhite
val WarningForeground     = PureWhite
val InfoForeground        = PureWhite

val Border  = Neutral200
val InputBg = Neutral50
val Ring    = Primary500
val White   = PureWhite

// ── أدوار واجهة (DARK) ────────────────────────────────────────────
val DarkBackground      = Ink
val DarkForeground      = Color(0xFFF2F3F4)
val DarkCard            = Color(0xFF22282B)
val DarkBorder          = Color(0xFF354044)
val DarkMuted           = Color(0xFF2A3236)
val DarkMutedForeground = Neutral400
val DarkInput           = Color(0xFF2A3236)
val DarkPrimary         = Primary300
val DarkAccent          = Color(0xFFD1A47C)
val DarkDestructive     = Color(0xFFE2695C)
val DarkSuccess         = Color(0xFF5FAE7C)
val DarkWarning         = Color(0xFFDCA24E)

// ═══════════════════════════════════════════════════════════════
//  COLOR SCHEMES
// ═══════════════════════════════════════════════════════════════

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = PrimaryForeground,
    primaryContainer = Primary100,
    onPrimaryContainer = Primary900,
    secondary = Secondary,
    onSecondary = SecondaryForeground,
    secondaryContainer = Neutral100,
    onSecondaryContainer = Foreground,
    tertiary = Accent,
    onTertiary = AccentForeground,
    tertiaryContainer = Accent100,
    onTertiaryContainer = Accent600,
    background = Background,
    onBackground = Foreground,
    surface = CardBg,
    onSurface = CardForeground,
    surfaceVariant = Muted,
    onSurfaceVariant = MutedForeground,
    error = Destructive,
    onError = DestructiveForeground,
    outline = Border,
    outlineVariant = Neutral200,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Ink,
    primaryContainer = Primary700,
    onPrimaryContainer = Primary100,
    secondary = DarkMuted,
    onSecondary = DarkForeground,
    tertiary = DarkAccent,
    onTertiary = Ink,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkCard,
    onSurface = DarkForeground,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkMutedForeground,
    error = DarkDestructive,
    onError = Ink,
    outline = DarkBorder,
)

// ═══════════════════════════════════════════════════════════════
//  SPACING
// ═══════════════════════════════════════════════════════════════

data class ElevenSpacing(
    val s1: Dp = 4.dp,
    val s2: Dp = 8.dp,
    val s3: Dp = 12.dp,
    val s4: Dp = 16.dp,
    val s5: Dp = 20.dp,
    val s6: Dp = 24.dp,
    val s8: Dp = 32.dp,
    val s10: Dp = 40.dp,
    val s12: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { ElevenSpacing() }

// ═══════════════════════════════════════════════════════════════
//  TYPOGRAPHY
// ═══════════════════════════════════════════════════════════════

// ملاحظة: fontFamily = InterFontFamily في كل الأنماط — يطابق
// font-family: 'Inter' المطبّق على body في index.css بالموقع.
val ElevenTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
)

// ═══════════════════════════════════════════════════════════════
//  THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════

@Composable
fun ElevenStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalSpacing provides ElevenSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ElevenTypography,
            content = content
        )
    }
}
