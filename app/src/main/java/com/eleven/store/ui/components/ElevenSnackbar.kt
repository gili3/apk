package com.eleven.store.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ✅ إعادة تصميم موحّدة لرسائل التنبيه القصيرة (Snackbar) في التطبيق —
 * كانت كل الشاشات (السلة، الدفع، الملف الشخصي، تسجيل الدخول...) تستخدم
 * SnackbarHost الافتراضي بلون رمادي واحد لكل الرسائل، بلا أي تمييز بين
 * رسالة نجاح ("تم تطبيق كود الخصم") ورسالة خطأ ("يرجى إدخال بريدك
 * الإلكتروني") — بالضبط نفس المشكلة التي كانت في Toast بالموقع.
 *
 * الآن أربعة أنواع بنفس الألوان الست عشرية المستخدمة في Toaster
 * بالموقع (success/error/warning/info)، مع أيقونة مناسبة لكل نوع،
 * حواف مستديرة وظل ناعم — استبدل `SnackbarHost(state)` بـ
 * `ElevenSnackbarHost(state)` في أي Scaffold، واستخدم
 * `state.showMessage(text, type)` بدل `state.showSnackbar(text)`.
 */
enum class SnackbarType { SUCCESS, ERROR, WARNING, INFO }

private class TypedSnackbarVisuals(
    override val message: String,
    val type: SnackbarType,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

/** يعرض رسالة Snackbar ملوّنة حسب نوعها. استخدم هذه بدل showSnackbar() مباشرة. */
suspend fun SnackbarHostState.showMessage(
    message: String,
    type: SnackbarType = SnackbarType.INFO,
): SnackbarResult = showSnackbar(TypedSnackbarVisuals(message, type))

private data class SnackbarPalette(val bg: Color, val fg: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private fun paletteFor(type: SnackbarType): SnackbarPalette = when (type) {
    SnackbarType.SUCCESS -> SnackbarPalette(Color(0xFFF0FDF4), Color(0xFF166534), Icons.Filled.CheckCircle)
    SnackbarType.ERROR   -> SnackbarPalette(Color(0xFFFEF2F2), Color(0xFF991B1B), Icons.Filled.Error)
    SnackbarType.WARNING -> SnackbarPalette(Color(0xFFFFFBEB), Color(0xFF92400E), Icons.Filled.Warning)
    SnackbarType.INFO    -> SnackbarPalette(Color(0xFFEFF6FF), Color(0xFF1E3A8A), Icons.Filled.Info)
}

@Composable
fun ElevenSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState) { data: SnackbarData ->
        val visuals = data.visuals
        val type = (visuals as? TypedSnackbarVisuals)?.type ?: SnackbarType.INFO
        val palette = paletteFor(type)
        Snackbar(
            modifier = Modifier.padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = palette.bg,
            contentColor = palette.fg,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(palette.icon, null, tint = palette.fg, modifier = Modifier)
                Spacer(Modifier.width(10.dp))
                Text(visuals.message, color = palette.fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
