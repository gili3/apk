package com.eleven.store.ui.screens

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleven.store.ui.theme.Ink

// ═══════════════════════════════════════════════════════════════
//  WELCOME OVERLAY — شاشة ترحيب داخل Compose
//
//  ✅ جديد: هذه ليست "شاشة إقلاع" (تلك مهمّة نظام أندرويد 12+ في
//  Theme.ElevenStore.Starting + MainActivity.installSplashScreen، ومصمَّمة
//  لتكون قصيرة جداً بحكم النظام). هذه الشاشة تظهر بعدها مباشرة وتبقى ظاهرة
//  فوق كامل التطبيق (الهيدر/الشريط السفلي أيضاً) طوال أول تحميل فعلي
//  لبيانات الرئيسية (بانرات/تصنيفات/منتجات)، بدل ترك المستخدم يرى هيكل
//  الصفحة فارغاً/بشكل مبعثر يتجمّع قطعة قطعة. بما أن مظهرها (علامة تجارية +
//  حركة بسيطة) لا علاقة له بالتحميل الفعلي، فالانتظار "يُشعَر" أقصر بكثير
//  من نفس المدة أمام سكلتون فارغ أو دوّارة تحميل مجرّدة — وهذا بالضبط
//  الهدف المطلوب ("تفقد/تقلل إحساس الانتظار").
//
//  التحكّم بإظهارها/إخفائها (حد أدنى + حد أقصى بالوقت مرتبط بحالة تحميل
//  الصفحة الرئيسية الفعلية) موجود بـElevenApp في MainActivity.kt، وتُغلَّف
//  هناك بـAnimatedVisibility (fade) بدل إخفاء فوري مفاجئ.
// ═══════════════════════════════════════════════════════════════

@Composable
fun AppWelcomeOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Ink, Color(0xFF1E293B)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BreathingWordmark()
            Spacer(Modifier.height(10.dp))
            Text(
                "اكتشف مجموعتنا الحصرية",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(36.dp))
            LoadingDots()
        }
    }
}

// شعار "Eleven" بحركة تنفّس خفيفة (تكبير/تصغير + نبضة شفافية) — بنفس خط
// وأسلوب النص الترويجي بالبانر الرئيسي (FontFamily.Serif) حتى يبقى الانطباع
// الأول متّسقاً مع باقي هوية التطبيق البصرية، لا عنصراً غريباً عنها.
@Composable
private fun BreathingWordmark() {
    val transition = rememberInfiniteTransition(label = "welcome_breathing")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "welcome_scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "welcome_alpha",
    )

    Text(
        "Eleven",
        color = Color.White,
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Serif,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
    )
}

// ثلاث نقاط تنبض بالتتابع (تأخير مختلف لكل نقطة) بدل دوّارة تحميل تقليدية —
// إيحاء بحركة/تقدّم دون رقم أو نسبة مضلِّلة (لا نعرف مدة التحميل الفعلية).
@Composable
private fun LoadingDots() {
    val transition = rememberInfiniteTransition(label = "welcome_dots")
    Row {
        val delays = listOf(0, 150, 300)
        delays.forEachIndexed { index, delayMs ->
            val dotAlpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, delayMillis = delayMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot_$index",
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(0.7f + dotAlpha * 0.3f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = dotAlpha)),
            )
            if (index != delays.lastIndex) Spacer(Modifier.width(8.dp))
        }
    }
}
