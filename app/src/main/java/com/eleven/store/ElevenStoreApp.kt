package com.eleven.store

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

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
