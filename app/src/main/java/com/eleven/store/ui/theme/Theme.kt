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
//  ELEVEN STORE — DESIGN TOKENS (SINGLE SOURCE OF TRUTH) — Design Rules
//  هذا الملف هو المصدر الوحيد لكل ألوان تطبيق الأندرويد.
//  يقابله في الموقع: client/src/lib/colors.ts + client/src/index.css
//  أي لون جديد يُضاف هنا فقط، ولا يُكتب Hex مباشرة داخل الشاشات.
//
//  القاعدة: 5 ألوان أساسية فقط — أسود/أبيض/رمادي للواجهة، وأخضر/أحمر/
//  أزرق/برتقالي محجوزة حصراً لحالات الطلب والتنبيهات. لا يوجد لون
//  "Accent" تجاري منفصل بعد الآن — Accent هنا = Ink (نفس Primary)
//  لأن الشاشات تستخدمه فعلياً كلون علامة أساسي، وليس كتمييز محايد.
// ═══════════════════════════════════════════════════════════════

// ── الأساس (المرتكزات الثلاثة المطلوبة) ──────────────────────────
val Ink        = Color(0xFF0F172A) // نص أساسي وPrimary — Foreground
val PureWhite  = Color(0xFFFFFFFF) // أبيض - Background
val TextSecondary = Color(0xFF64748B) // النص الثانوي — الدرجة الوحيدة المسموحة

// ── تدرّج محايد (رمادي واحد فقط — Tailwind Slate) ─────────────────
val Neutral50  = Color(0xFFF8FAFC)
val Neutral100 = Color(0xFFF1F5F9)
val Neutral200 = Color(0xFFE2E8F0)
val Neutral300 = Color(0xFFCBD5E1)
val Neutral400 = Color(0xFF94A3B8)
val Neutral500 = Color(0xFF64748B)
val Neutral600 = Color(0xFF475569)
val Neutral700 = Color(0xFF334155)
val Neutral800 = Color(0xFF1E293B)
val Neutral900 = Ink

// ── تدرّج العلامة (Primary scale) — أسود/رمادي غامق فقط ───────────
val Primary50  = Color(0xFFF8FAFC)
val Primary100 = Color(0xFFF1F5F9)
val Primary200 = Color(0xFFE2E8F0)
val Primary300 = Color(0xFFCBD5E1)
val Primary400 = Color(0xFF94A3B8)
val Primary500 = Color(0xFF334155)
val Primary600 = Color(0xFF1E293B)
val Primary700 = Ink
val Primary800 = Ink
val Primary900 = Ink

// ── ألوان الحالة الأربعة — لحالات الطلب والتنبيهات فقط ─────────────
val StateGreen   = Color(0xFF22C55E)
val StateGreenBg = Color(0xFFDCFCE7)
val StateGreenFg = Color(0xFF166534)
val StateRed     = Color(0xFFEF4444)
val StateRedBg   = Color(0xFFFEE2E2)
val StateRedFg   = Color(0xFF991B1B)
val StateBlue    = Color(0xFF3B82F6)
val StateBlueBg  = Color(0xFFDBEAFE)
val StateBlueFg  = Color(0xFF1E3A8A)
val StateOrange   = Color(0xFFF97316)
val StateOrangeBg = Color(0xFFFFEDD5)
val StateOrangeFg = Color(0xFF9A3412)

// ── الألوان الدلالية (Semantic) ──────────────────────────────────
val Success   = StateGreen
val SuccessBg = StateGreenBg
val SuccessBorder = Color(0xFFBBF7D0)
val Warning   = StateOrange
val WarningBg = StateOrangeBg
val Destructive   = StateRed
val DestructiveBg = StateRedBg
val Info      = StateBlue
val InfoBg    = StateBlueBg

// ── ألوان حالات الطلب — قيم Hex ثابتة ومطلوبة حرفياً ──────────────
// النظام: انتظار=رمادي / دفع=أخضر / توصيل=برتقالي / تسليم=أزرق / إلغاء=أحمر
// ✅ ألوان مصمتة قوية (لا تظليل باهت) لتمييز حالة الطلب بوضوح فوري —
// نفس القيم الحرفية في الموقع (client/src/lib/colors.ts → ORDER_STATUS_COLORS)
object OrderStatusColors {
    val PendingBg   = Color(0xFF475569)
    val PaidBg      = Color(0xFF16A34A)
    val ShippedBg   = Color(0xFFEA580C)
    val DeliveredBg = Color(0xFF2563EB)
    val CancelledBg = Color(0xFFDC2626)
    val Foreground  = Color(0xFFFFFFFF)
}

// ── أدوار واجهة (Semantic UI roles - LIGHT) ──────────────────────
val Primary            = Ink
val PrimaryForeground  = PureWhite
val PrimaryBg          = Color(0x1A0F172A)

val Background = PureWhite
val Foreground = Ink

val CardBg           = PureWhite
val CardForeground   = Ink

val Secondary           = Neutral50
val SecondaryForeground = Ink

val Muted           = Neutral100
val MutedForeground = TextSecondary

// Accent = نفس Primary (لا يوجد لون تمييز تجاري منفصل بعد الآن)
//
// 🐛 هذا هو السبب الجذري الحقيقي وراء "ألوان لوحة التحكم ما بتتغيّر بالتطبيق":
// كانا يُعرَّفان سابقاً كـ val ثابت في وقت الترجمة (= Ink حرفياً)، وليسا
// كقيمة تُقرأ من الثيم الحالي (CompositionLocal). وبما أن أكثر من 70 موضع
// في كل شاشات التطبيق (الأزرار، الروابط، الأيقونات، الحقول المركّزة،
// صناديق الاختيار...) تستخدم Accent مباشرة بدل MaterialTheme.colorScheme.primary،
// فإن أي لون يختاره الأدمن من لوحة التحكم لم يكن يصل فعلياً لأي من هذه
// العناصر إطلاقاً — بغض النظر عن الوضع الفاتح/الداكن أو نجاح الحفظ. تحويلهما
// إلى خاصية Composable تقرأ من الثيم الحالي يجعل كل نقطة استخدام قائمة (لا
// حاجة لتعديل عشرات الملفات) تعكس فوراً لون primaryColor المخصَّص من الأدمن.
// ✅ توسعة الثيمات: Accent كان يقرأ من primary حرفياً (لا يوجد لون تمييز
// منفصل). الآن يقرأ من tertiary بدل ذلك — وهو الفتحة التي يملأها
// accentColor القادم من لوحة التحكم في ElevenStoreTheme أدناه — حتى يصبح
// قابلاً للتخصيص باستقلالية عن Primary دون الحاجة لتعديل أي من عشرات
// المواضع التي تستخدم Accent مباشرة في الشاشات.
val Accent: Color
    @Composable get() = MaterialTheme.colorScheme.tertiary
val AccentForeground: Color
    @Composable get() = MaterialTheme.colorScheme.onTertiary

val DestructiveForeground = PureWhite
val SuccessForeground     = PureWhite
val WarningForeground     = PureWhite
val InfoForeground        = PureWhite

val Border  = Neutral200
val InputBg = Neutral50
val Ring    = Neutral400
val White   = PureWhite

// ── أدوار واجهة (DARK) ────────────────────────────────────────────
val DarkBackground      = Ink
val DarkForeground      = Color(0xFFF1F5F9)
val DarkCard            = Color(0xFF1E293B)
val DarkBorder          = Color(0xFF334155)
val DarkMuted           = Color(0xFF1E293B)
val DarkMutedForeground = Neutral400
val DarkInput           = Color(0xFF1E293B)
val DarkPrimary         = Color(0xFFE2E8F0)
val DarkAccent          = Color(0xFFE2E8F0)
val DarkDestructive     = Color(0xFFF87171)
val DarkSuccess         = Color(0xFF4ADE80)
val DarkWarning         = Color(0xFFFB923C)

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
    // ملاحظة: القيمة الأساسية هنا Ink/PureWhite حرفياً (وليس Accent/AccentForeground)
    // لأن هذا تعريف الثيم الأساسي نفسه — Accent أصبحت خاصية Composable تُقرأ
    // *من* MaterialTheme.colorScheme.primary بعد التطبيق، فلا يصح استخدامها هنا
    // كمُدخل لبناء الثيم نفسه (سيسبب مرجعية دائرية + خطأ ترجمة خارج سياق Composable).
    tertiary = Ink,
    onTertiary = PureWhite,
    tertiaryContainer = Primary100,
    onTertiaryContainer = Primary700,
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

// ═══════════════════════════════════════════════════════════════
//  ألوان قابلة للتخصيص من لوحة التحكم (settings/store في Firestore)
//  راجع: data/model/Models.kt::StoreSettings.primaryColor/secondaryColor/backgroundColor
// ═══════════════════════════════════════════════════════════════

/** يحوّل نص Hex مثل "#0F172A" إلى Color، أو null إن كان فارغاً/غير صالح
 *  (بدل تعطّل التطبيق بقيمة أدخلها الأدمن بالخطأ في لوحة التحكم). */
fun String.toColorOrNull(): Color? {
    if (this.isBlank()) return null
    return try {
        val hex = if (this.startsWith("#")) this else "#$this"
        if (!Regex("^#[0-9A-Fa-f]{6}$").matches(hex)) return null
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        null
    }
}

/** أبيض أو الحبر الأساسي فوق أي لون خلفية — حسب السطوع النسبي (WCAG) —
 *  حتى يبقى النص مقروءاً مهما كان اللون الذي يختاره الأدمن. */
private fun contrastingOnColor(bg: Color): Color {
    val luminance = 0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue
    return if (luminance > 0.6) Ink else PureWhite
}

// ═══════════════════════════════════════════════════════════════
//  اشتقاق ألوان الوضع الداكن من ألوان الوضع الفاتح (نفس الخوارزمية
//  حرفياً في themePresets.ts::deriveDarkTriplet بلوحة التحكم، حتى تطابق
//  المعاينة هناك ما يظهر فعلياً هنا). الفكرة: نحافظ على تدرّج الثيم اللوني
//  (Hue) ونغيّر فقط السطوع (Lightness) — فتبقى هوية الثيم واضحة بالوضعين
//  دون أن يحتاج الأدمن لإدخال 12 لوناً يدوياً (6 لكل وضع).
// ═══════════════════════════════════════════════════════════════

private fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> (g - b) / d + (if (g < b) 6f else 0f)
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    } / 6f
    return Triple(h, s, l)
}

private fun hslToRgb(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
    if (s == 0f) return Triple(l, l, l)
    fun hue2rgb(p: Float, q: Float, tIn: Float): Float {
        var t = tIn
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    return Triple(hue2rgb(p, q, h + 1f / 3f), hue2rgb(p, q, h), hue2rgb(p, q, h - 1f / 3f))
}

/** يشتق درجة بسطوع مستهدف مختلف من لون معيّن، مع الحفاظ على تدرّجه اللوني. */
private fun deriveDarkVariant(light: Color, targetLightness: Float, satScale: Float = 1f): Color {
    val (h, s, _) = rgbToHsl(light.red, light.green, light.blue)
    val (r, g, b) = hslToRgb(h, (s * satScale).coerceIn(0f, 1f), targetLightness)
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
}

@Composable
fun ElevenStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColor: String = "",
    secondaryColor: String = "",
    backgroundColor: String = "",
    // ✅ توسعة نظام الثيمات: تكتمل الست ألوان المقترحة من لوحة التحكم
    // (Primary/Secondary/Background/Surface/Text/Accent). راجع Models.kt
    // ::StoreSettings وthemePresets.ts بلوحة التحكم لنفس البنية هناك.
    surfaceColor: String = "",
    textColor: String = "",
    accentColor: String = "",
    content: @Composable () -> Unit
) {
    val base = if (darkTheme) DarkColorScheme else LightColorScheme

    // ✅ سابقاً: كل تخصيصات لوحة التحكم كانت تُستثنى بالكامل في الوضع
    // الداكن (يبقى التصميم الافتراضي دائماً)، لأن تطبيق نفس قيم الوضع
    // الفاتح حرفياً فوق خلفية داكنة يكسر التباين. الحل الآن: Primary/
    // Secondary/Accent هي هوية الثيم فتُطبَّق في الوضعين معاً (يُعاد حساب
    // onColor المناسب أوتوماتيكياً)، بينما Background/Surface/Text
    // (المُدخلة كقيم الوضع الفاتح فقط) تُشتق منها نسخة الوضع الداكن
    // حسابياً عبر deriveDarkVariant بدل تجاهلها بالكامل أو طلب إدخالها
    // مرتين من الأدمن.
    val primaryOverride = primaryColor.toColorOrNull()
    val secondaryOverride = secondaryColor.toColorOrNull()
    val accentOverride = accentColor.toColorOrNull() ?: primaryOverride

    val lightBackground = backgroundColor.toColorOrNull()
    val lightSurface = surfaceColor.toColorOrNull() ?: lightBackground
    val lightText = textColor.toColorOrNull()

    val backgroundOverride = if (!darkTheme) {
        lightBackground
    } else {
        lightBackground?.let { deriveDarkVariant(it, targetLightness = 0.09f, satScale = 0.55f) }
    }
    val surfaceOverride = if (!darkTheme) {
        lightSurface
    } else {
        lightSurface?.let { deriveDarkVariant(it, targetLightness = 0.15f, satScale = 0.45f) }
    }
    val textOverride = if (!darkTheme) {
        lightText
    } else {
        lightText?.let { deriveDarkVariant(it, targetLightness = 0.92f, satScale = 0.25f) }
    }

    val colorScheme = base.copy(
        primary = primaryOverride ?: base.primary,
        onPrimary = primaryOverride?.let(::contrastingOnColor) ?: base.onPrimary,
        secondary = secondaryOverride ?: base.secondary,
        onSecondary = secondaryOverride?.let(::contrastingOnColor) ?: base.onSecondary,
        tertiary = accentOverride ?: base.tertiary,
        onTertiary = accentOverride?.let(::contrastingOnColor) ?: base.onTertiary,
        background = backgroundOverride ?: base.background,
        onBackground = textOverride ?: backgroundOverride?.let(::contrastingOnColor) ?: base.onBackground,
        surface = surfaceOverride ?: base.surface,
        onSurface = textOverride ?: surfaceOverride?.let(::contrastingOnColor) ?: base.onSurface,
    )

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
