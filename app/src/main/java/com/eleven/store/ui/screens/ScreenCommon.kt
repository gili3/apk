package com.eleven.store.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleven.store.data.model.*
import com.eleven.store.ui.components.*
import com.eleven.store.ui.theme.Accent
import com.eleven.store.ui.theme.Border
import com.eleven.store.ui.theme.Muted
import com.eleven.store.ui.theme.MutedForeground
import com.eleven.store.ui.theme.Primary
import com.eleven.store.ui.theme.Destructive
import com.eleven.store.ui.theme.Warning
import com.eleven.store.ui.theme.Success
import com.eleven.store.ui.theme.SuccessBg
import com.eleven.store.ui.theme.Info
import com.eleven.store.ui.theme.White
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// دوال ومساعدات مشتركة بين شاشات الشاشات الأخرى (تم فصلها من OtherScreens.kt)

// ═══════════════════════════════════════════════════════════════
// دالة مساعدة موحّدة لتنسيق الأرقام والأسعار — تُستخدم من كل الشاشات
// (كانت هذه الدالة مكررة بـ 6 نسخ مختلفة موزعة على الشاشات، كل نسخة
// بسلوك مختلف: بعضها بدون فواصل ألفية إطلاقاً، وبعضها يضيف الفاصلة
// فقط عند وجود كسر عشري، وبعضها يعتمد على لغة الجهاز عبر "%,.2f"
// مما قد يُخرج فواصل/أرقاماً مختلفة حسب لغة النظام. الآن دالة واحدة
// فقط، بتنسيق ثابت (أرقام لاتينية + فاصلة إنجليزية) بغض النظر عن
// لغة الجهاز، ليطابق تماماً تنسيق لوحة التحكم على الويب)
// ═══════════════════════════════════════════════════════════════

private val PRICE_SYMBOLS = java.text.DecimalFormatSymbols(java.util.Locale.US)
private val INTEGER_FORMAT = java.text.DecimalFormat("#,##0", PRICE_SYMBOLS)
private val DECIMAL_FORMAT = java.text.DecimalFormat("#,##0.00", PRICE_SYMBOLS)

private fun toSafeDouble(value: Any?): Double = when (value) {
    is Double -> value
    is Long -> value.toDouble()
    is Int -> value.toDouble()
    is String -> value.toDoubleOrNull() ?: 0.0
    is Number -> value.toDouble()
    else -> 0.0
}

/** تنسيق رقم بفواصل الآلاف بدون عملة (للكميات، المخزون، إلخ) */
internal fun formatNumber(value: Any?): String {
    val d = toSafeDouble(value)
    return if (d == d.toLong().toDouble()) INTEGER_FORMAT.format(d) else DECIMAL_FORMAT.format(d)
}

/** تنسيق سعر بفواصل الآلاف مع إضافة عملة "ج.س" */
internal fun formatPrice(value: Any?): String = "${formatNumber(value)} ج.س"

internal fun openUri(context: android.content.Context, uri: String) {
    try {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri)))
    } catch (_: Exception) { }
}
