package com.eleven.store.data.repository

import android.util.Log
import com.eleven.store.BuildConfig
import com.eleven.store.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * ELEVEN STORE — بحث المنتجات عبر Algolia REST API مباشرة.
 *
 * يقرأ فقط من نفس فهرس "products" الذي يزامنه سيرفر الموقع (Node) عند كل
 * إنشاء/تعديل/حذف منتج (انظر store-main/server/algolia-service.ts) — لا يوجد
 * أي منطق فهرسة مكرر هنا، هذا الملف قراءة فقط (Search-Only API Key).
 *
 * ⚠️ أمان: BuildConfig.ALGOLIA_SEARCH_API_KEY يجب أن يكون دائماً مفتاح
 * Search-Only (قراءة بحث فقط، بلا صلاحية كتابة/حذف/إدارة فهارس) — آمن
 * بالتصميم للتضمين داخل APK حتى بعد فك تحزيمه (decompile)، تماماً كمفاتيح
 * Firebase العامة المستخدمة أصلاً بالتطبيق. **لا يوضع مفتاح الـAdmin هنا
 * أبداً.**
 *
 * ⚠️ ملاحظة تقنية: هذا الاستدعاء مكتوب حسب توثيق Algolia REST Search API
 * المعروف (POST /1/indexes/{indexName}/query) لكن لم يتسنَّ بناء/تشغيل
 * التطبيق فعلياً في بيئة التطوير الحالية (بلا اتصال شبكة لتحميل Gradle
 * dependencies) للتحقق من الاستجابة الفعلية — يُنصح بتجربة بحث واحد يدوياً
 * بعد الربط بمفاتيح حقيقية قبل الاعتماد عليه بالإنتاج.
 */
object AlgoliaSearchService {

    val isConfigured: Boolean
        get() = BuildConfig.ALGOLIA_APP_ID.isNotBlank() && BuildConfig.ALGOLIA_SEARCH_API_KEY.isNotBlank()

    // يطابق ALGOLIA_PRODUCTS_INDEX الافتراضي المستخدم بالسيرفر (server/_core/env.ts)
    private const val INDEX_NAME = "products"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // نفس نافذة "جديد" (30 يوماً) المستخدمة في server/firestore-router.ts
    // وFirestoreRepository.getProducts وclient/src/lib/algolia.ts — يجب أن
    // تبقى القيمة متطابقة في المصادر الأربعة.
    private const val NEW_PRODUCT_WINDOW_MS = 30L * 24 * 60 * 60 * 1000

    suspend fun searchProducts(
        query: String,
        categoryId: String? = null,
        brandId: String? = null,
        onSale: Boolean? = null,
        isFeatured: Boolean? = null,
        // ✅ إصلاح: كانا مفقودين هنا تماماً كما بالموقع (نفس الثغرة) — البحث
        // النصي مع فلتر "جديد" أو "الأكثر مبيعاً" كان يتجاهلهما بصمت.
        isBestSeller: Boolean? = null,
        isNew: Boolean? = null,
        hitsPerPage: Int = 60,
    ): List<Product> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext emptyList()

        try {
            // ✅ نفس منطق الاستبعاد المطبَّق بالضبط على الموقع (client/src/lib/algolia.ts)
            // — لا حاجة لفلترة stock/isActive يدوياً بعد النتيجة لأن الفهرسة
            // نفسها تستبعد المنتجات غير المتاحة (انظر syncProductToIndex بالسيرفر).
            val filters = mutableListOf("isActive:true")
            categoryId?.let { filters += "categoryId:$it" }
            brandId?.let { filters += "brandId:$it" }
            if (onSale == true) filters += "isOnSale:true"
            if (isFeatured == true) filters += "isFeatured:true"
            if (isBestSeller == true) filters += "isBestSeller:true"
            if (isNew == true) {
                val threshold = System.currentTimeMillis() - NEW_PRODUCT_WINDOW_MS
                filters += "createdAtTimestamp > $threshold"
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("filters", filters.joinToString(" AND "))
                put("hitsPerPage", hitsPerPage)
            }.toString()

            val url = "https://${BuildConfig.ALGOLIA_APP_ID}-dsn.algolia.net/1/indexes/$INDEX_NAME/query"
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Algolia-Application-Id", BuildConfig.ALGOLIA_APP_ID)
                .addHeader("X-Algolia-API-Key", BuildConfig.ALGOLIA_SEARCH_API_KEY)
                .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("AlgoliaSearch", "HTTP ${response.code}: ${response.message}")
                    return@withContext emptyList()
                }
                val json = JSONObject(response.body?.string().orEmpty())
                val hits: JSONArray = json.optJSONArray("hits") ?: JSONArray()
                (0 until hits.length()).map { i -> hitToProduct(hits.getJSONObject(i)) }
            }
        } catch (e: Exception) {
            // ✅ فشل البحث عبر Algolia (بلا شبكة، خطأ مفاتيح...) لا يجب أن يُسقِط
            // الشاشة — المستدعي (FirestoreRepository.getProducts) يرجع تلقائياً
            // لفلترة Firestore المحلية القديمة عند نتيجة فارغة (انظر هناك).
            Log.e("AlgoliaSearch", "searchProducts(\"$query\") failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun hitToProduct(hit: JSONObject): Product {
        val imageUrl = hit.optString("imageUrl", "")
        return Product(
            id = hit.optString("objectID"),
            name = hit.optString("name"),
            description = hit.optString("description"),
            price = hit.optDouble("price", 0.0),
            originalPrice = if (hit.has("originalPrice") && !hit.isNull("originalPrice"))
                hit.optDouble("originalPrice") else null,
            images = if (imageUrl.isNotBlank()) listOf(imageUrl) else emptyList(),
            categoryId = hit.optString("categoryId"),
            brandId = hit.optString("brandId"),
            stock = hit.optInt("stock", 0),
            isFeatured = hit.optBoolean("isFeatured", false),
            isBestSeller = hit.optBoolean("isBestSeller", false),
            isOnSale = hit.optBoolean("isOnSale", false),
        )
    }
}
