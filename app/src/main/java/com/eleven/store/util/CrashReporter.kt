package com.eleven.store.util

import android.os.Build
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import java.util.concurrent.TimeUnit

/**
 * ELEVEN STORE — إبلاغ أخطاء التطبيق للسجل المركزي (بطلب الأدمن)
 * ─────────────────────────────────────────────────────────
 * نظير lib/errorReporter.ts بلوحة التحكم، لكن للأندرويد: يبلّغ عن
 * الكراشات القاتلة (uncaught exceptions) وأي خطأ غير قاتل (non-fatal) من
 * نقاط catch بالتطبيق، عبر Cloud Function واحدة (reportClientError —
 * راجع functions/src/callables/errorReporting.ts) تكتب لنفس مجموعة
 * Firestore "systemErrorLogs" التي تقرأها صفحة "سجل الأخطاء" بلوحة التحكم.
 *
 * ⚠️ محاولة "أفضل جهد" فقط للكراشات القاتلة: العملية بصدد الانتهاء، فلا
 * ضمان اكتمال طلب الشبكة. نحجب Thread المُتعطِّل (Tasks.await) لمدة تصل
 * لـ3 ثوانٍ كحد أقصى لإعطاء الطلب فرصة، ثم نُسلِّم الاستثناء لأي معالج
 * أصلي كان موجوداً (النظام نفسه غالباً) — لا نُخفي الكراش عن المستخدم أو
 * عن أي أداة تشخيص أخرى مثبَّتة، فقط نضيف تقريراً إضافياً قبل ذلك.
 */
object CrashReporter {
    private const val TAG = "CrashReporter"
    private var appVersion: String = "unknown"

    /**
     * يُستدعى مرة واحدة من ElevenStoreApp.onCreate() قبل أي كود آخر — حتى
     * يُلتقَط أي كراش يحدث بأقرب وقت ممكن من الإقلاع.
     */
    fun install(versionName: String) {
        appVersion = versionName
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val functions = FirebaseFunctions.getInstance()
                val data = hashMapOf(
                    "message" to (throwable.message ?: throwable.javaClass.simpleName),
                    "stack" to Log.getStackTraceString(throwable),
                    "appVersion" to appVersion,
                    "deviceInfo" to deviceInfoString(),
                    "severity" to "fatal",
                )
                // ✅ حجب متزامن (وليس coroutine/callback) عمداً: هذا المعالج يُستدعى
                // على Thread على وشك إنهاء العملية بالكامل — أي كود غير متزامن هنا
                // لن يكتمل إطلاقاً لأن العملية ستموت قبل تشغيل أي callback لاحق.
                //
                // ✅ إصلاح مهم: Tasks.await() يرمي IllegalStateException فوراً لو استُدعي
                // على الـmain thread ("Must not be called on the main thread") — وأغلب
                // كراشات واجهة التطبيق تحدث على الـmain thread تحديداً، فكانت تقارير
                // الكراش لا تصل للوحة إطلاقاً. الحل: الإرسال والانتظار على Thread
                // مستقل، والـthread الحالي (حتى لو main) ينتظره بـjoin بحد أقصى 3.5 ثانية.
                val sender = Thread {
                    try {
                        Tasks.await(
                            functions.getHttpsCallable("reportClientError").call(data),
                            3, TimeUnit.SECONDS
                        )
                    } catch (e: Throwable) {
                        Log.e(TAG, "فشل إرسال تقرير الكراش", e)
                    }
                }
                sender.start()
                sender.join(3500)
            } catch (reportingError: Throwable) {
                // فشل الإبلاغ نفسه لا يجب أن يمنع تسليم الكراش الأصلي للمعالج التالي.
                Log.e(TAG, "فشل إرسال تقرير الكراش", reportingError)
            } finally {
                // نُسلِّم دائماً للمعالج الأصلي (نظام أندرويد عادةً) — لا نُخفي الكراش
                // ولا نمنع تصرّف النظام الطبيعي (شاشة "توقف التطبيق"، إعادة التشغيل...).
                if (previousHandler != null) {
                    previousHandler.uncaughtException(thread, throwable)
                } else {
                    Runtime.getRuntime().exit(2)
                }
            }
        }
    }

    /**
     * إبلاغ عن خطأ غير قاتل — يُستخدم داخل catch blocks بالتطبيق (مثال:
     * فشل إنشاء طلب، فشل مزامنة مخزون) حيث لا نريد إيقاف التطبيق، فقط
     * تسجيل أن هذا حدث فعلياً حتى يراه الأدمن. غير متزامن بالكامل (fire and
     * forget) — لا ينتظر النتيجة ولا يرمي أبداً، فلا يؤثر على مسار المستخدم.
     */
    fun reportNonFatal(throwable: Throwable, route: String? = null) {
        try {
            val functions = FirebaseFunctions.getInstance()
            val data = hashMapOf(
                "message" to (throwable.message ?: throwable.javaClass.simpleName),
                "stack" to Log.getStackTraceString(throwable),
                "route" to route,
                "appVersion" to appVersion,
                "deviceInfo" to deviceInfoString(),
                "severity" to "error",
            )
            functions.getHttpsCallable("reportClientError").call(data)
                .addOnFailureListener { e -> Log.e(TAG, "فشل إرسال تقرير خطأ غير قاتل", e) }
        } catch (e: Throwable) {
            Log.e(TAG, "فشل إرسال تقرير خطأ غير قاتل", e)
        }
    }

    private fun deviceInfoString(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    }
}
