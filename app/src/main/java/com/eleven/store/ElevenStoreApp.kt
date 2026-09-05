package com.eleven.store

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

// ═══════════════════════════════════════════════════════════════
//  ELEVEN STORE — Application class
//
//  Coil Cache: يوفّر ImageLoader واحد للتطبيق كاملاً مع:
//   - Memory Cache: 25% من ذاكرة التطبيق المتاحة (صور تظهر فوراً
//     عند التنقل بين الشاشات دون إعادة تحميل).
//   - Disk Cache: 100MB على تخزين التطبيق الداخلي (صور المنتجات
//     تبقى معروضة حتى بدون إنترنت بعد أول تحميل لها).
//  هذا لا يضيف حجماً يُذكر للـAPK لأن coil-compose أصلاً معتمدة
//  في المشروع؛ الإضافة هنا فقط تُفعّل وتُهيّئ الكاش صراحة.
// ═══════════════════════════════════════════════════════════════
class ElevenStoreApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initFirebaseAppCheck()
    }

    // ✅ إضافة (Audit المرحلة 3، بند 3.7 — الحل الجذري لمشكلة نقص المخزون
    // المباشر): بمجرد تفعيل "Enforce" على Firestore بـFirebase Console (خطوة
    // يدوية إلزامية، غير ممكنة من هذا الكود)، أي طلب Firestore لا يحمل توكن
    // App Check صالحاً (كطلب من Postman بتوكن Firebase Auth عادي دون المرور
    // فعلياً بهذا التطبيق) يُرفَض تلقائياً على مستوى Firebase نفسه، قبل حتى
    // الوصول لقواعد Firestore Rules. Play Integrity هو المزوّد الموصى به من
    // جوجل لتطبيقات أندرويد حقيقية (بديل SafetyNet المهجور).
    //
    // ⚠️ تحذير مهم قبل تفعيل "Enforce" فعلياً: يجب مراقبة مقاييس App Check
    // بلوحة Firebase Console لمدة 24–48 ساعة على الأقل بعد نشر هذا الإصدار،
    // والتأكد أن نسبة الطلبات "Verified" مرتفعة فعلياً، **قبل** تفعيل الإنفاذ —
    // وإلا قد يُحظَر مستخدمون حقيقيون (نسخ قديمة من التطبيق لم تُحدَّث بعد لا
    // تملك هذا الكود إطلاقاً). هذا ينطبق أيضاً على موقع الويب (راجع
    // client/src/lib/firebase.ts) الذي يحتاج مزوّد reCAPTCHA منفصلاً — تفعيل
    // الإنفاذ على Firestore يشمل الموقع والأندرويد معاً، وليس الأندرويد فقط.
    private fun initFirebaseAppCheck() {
        Firebase.initialize(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }

    // ✅ إصلاح جذري (سبب رئيسي لعدم وصول الإشعارات): كانت قناة الإشعارات
    // (NotificationChannel) تُنشأ فقط داخل onMessageReceived بخدمة FCM —
    // أي فقط عندما يكون التطبيق بالمقدمة وقت وصول الإشعار. لكن الحالة
    // الأكثر شيوعاً هي وصول Push والتطبيق بالخلفية أو مغلق تماماً، وفي هذه
    // الحالة النظام هو من يعرض الإشعار مباشرة (onMessageReceived لا يُستدعى
    // إطلاقاً)، معتمداً على قناة موجودة مسبقاً (عبر default_notification_channel_id
    // بـ AndroidManifest). إن لم تكن القناة قد أُنشئت بعد — لا يوجد مكان
    // صالح لعرض الإشعار، وبعض الأجهزة (شاومي/سامسونج خصوصاً) تُسقطه بصمت.
    // الحل: إنشاء القناة مرة واحدة عند أول إقلاع للتطبيق، قبل وصول أي Push.
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getString(R.string.default_notification_channel_id)
            val channel = NotificationChannel(
                channelId,
                "إشعارات Eleven Store",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "تحديثات الطلبات والعروض والإشعارات العامة"
                enableVibration(true)
                enableLights(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB
                    .build()
            }
            .respectCacheHeaders(false) // صور المنتجات لا تتغيّر كثيراً، لذا نعتمد كاش محلي ثابت بدل رؤوس HTTP
            .crossfade(true)
            .build()
    }
}
