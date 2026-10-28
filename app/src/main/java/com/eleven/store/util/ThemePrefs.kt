package com.eleven.store.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * ✅ إصلاح الوضع الداكن: التطبيق كان يتبع وضع النظام فقط (isSystemInDarkTheme())
 * بدون أي خيار يدوي للمستخدم ولا حفظ لأي تفضيل — أُضيف هنا تفضيل ثلاثي
 * (نظام / فاتح / داكن) محفوظ محلياً في SharedPreferences ويُقرأ عند إقلاع
 * التطبيق قبل رسم أول شاشة (راجع MainActivity.kt).
 */
enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageKey(key: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

object ThemePrefs {
    private const val PREFS_NAME = "eleven_theme_prefs"
    private const val KEY_MODE = "theme_mode"

    /** حالة قابلة للمراقبة من Compose — تُحدَّث فوراً عند تغيير المستخدم للخيار
     *  من شاشة الإعدادات، فتُعاد رسمة الواجهة بالكامل بالثيم الجديد. */
    var current by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        current = ThemeMode.fromStorageKey(prefs.getString(KEY_MODE, null))
    }

    fun setMode(context: Context, mode: ThemeMode) {
        current = mode
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.storageKey)
            .apply()
    }
}
