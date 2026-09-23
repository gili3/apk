package com.eleven.store.util

import android.content.Context

/**
 * ELEVEN STORE — تفضيلات التطبيق المحلية (SharedPreferences)
 * ─────────────────────────────────────────────────────────
 * تُستخدم حالياً لتفضيل واحد: عرض إشعارات الجوال (Push).
 * الافتراضي "مفعّل" — نفس السلوك السابق تماماً لمن لم يغيّر الإعداد.
 * سجل الإشعارات داخل التطبيق (Firestore) لا يتأثر بهذا التفضيل.
 */
object AppPreferences {
    private const val FILE_NAME = "eleven_prefs"
    private const val KEY_PUSH_ENABLED = "push_enabled"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun isPushEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PUSH_ENABLED, true)

    fun setPushEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PUSH_ENABLED, enabled).apply()
    }
}
