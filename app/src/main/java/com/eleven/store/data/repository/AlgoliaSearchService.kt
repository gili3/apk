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
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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

    // ✅ جديد (Pagination): نتيجة صفحة واحدة من البحث + هل توجد صفحة تالية
    // (nbPages من استجابة Algolia نفسها — لا حاجة لتخمين عبر مقارنة الحجم).
    // ✅ إصلاح: أضيف حقل error — سابقاً كان أي فشل (بلا اتصال، مهلة، خطأ
    // سيرفر) يُبتلَع بصمت ويُعاد products=emptyList() بالضبط كما لو أن
    // البحث نفسه لم يُطابق أي منتج، فتظهر واجهة البحث رسالة "لم نجد أي
    // منتجات" حتى عندما يكون السبب الحقيقي انقطاع الاتصال. الآن الفشل
    // الفعلي يحمل سبباً واضحاً بدل نتيجة فارغة سليمة الشكل.
    data class SearchPage(val products: List<Product>, val hasMore: Boolean, val error: String? = null)

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
        // ✅ إصلاح (Pagination): كان hitsPerPage=60 بلا أي معامل "page" —
        // نتيجة واحدة ثابتة فقط، بلا أي طريقة لطلب المزيد. الآن حجم صفحة
        // معقول (30، مطابق لحجم صفحة التصفح العادي) + page قابل للزيادة.
        hitsPerPage: Int = 30,
        page: Int = 0,
    ): SearchPage = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext SearchPage(emptyList(), false)

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
                put("page", page)
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
                    return@withContext SearchPage(emptyList(), false, error = "تعذّر إتمام البحث، حاول مرة أخرى")
                }
                val json = JSONObject(response.body?.string().orEmpty())
                val hits: JSONArray = json.optJSONArray("hits") ?: JSONArray()
                val products = (0 until hits.length()).map { i -> hitToProduct(hits.getJSONObject(i)) }
                // nbPages من استجابة Algolia نفسها (وليس تخمين من حجم النتيجة) —
                // يخبرنا بدقة إن كانت هناك صفحة تالية فعلياً (page مفهرسة من 0).
                val nbPages = json.optInt("nbPages", if (products.isEmpty()) 0 else 1)
                SearchPage(products, hasMore = (page + 1) < nbPages)
            }
        } catch (e: Exception) {
            // ⚠️ فشل البحث عبر Algolia (بلا شبكة، خطأ مفاتيح...) — لا يوجد أي
            // fallback لفلترة Firestore محلية بعد الآن (كان هنا سابقاً، أُزيل
            // عمداً — راجع FirestoreRepository.getProducts لتفاصيل السبب:
            // البحث والفلترة يجب أن يمرّا عبر Algolia فقط، لا محلياً على الجهاز).
            Log.e("AlgoliaSearch", "searchProducts(\"$query\") failed: ${e.message}", e)
            // ✅ إصلاح: نفرّق هنا تحديداً بين "لا يوجد اتصال بالإنترنت" (رسالة
            // واضحة قابلة للتصرف من المستخدم) وأي فشل آخر (سيرفر Algolia، مهلة
            // غير متعلقة بالاتصال...) — بدل معاملة الحالتين كنتيجة بحث فارغة
            // سليمة الشكل، بلا أي تفسير للمستخدم عن سبب عدم ظهور نتائج.
            val message = if (e is UnknownHostException || e is SocketTimeoutException || e is IOException)
                "لا يوجد اتصال بالإنترنت. تحقق من اتصالك وحاول مرة أخرى"
            else
                "تعذّر إتمام البحث، حاول مرة أخرى"
            SearchPage(emptyList(), false, error = message)
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
            // ✅ إصلاح: بدون هذا، Product.isNew المحسوبة من createdAt تبقى false
            // دائماً لأي منتج راجع من Algolia (شارة "جديد" لا تظهر أبداً لنتائج
            // البحث). createdAtTimestamp نفسه مُستخدَم أصلاً أعلاه في بناء فلتر
            // "جديد" — نقرأه هنا أيضاً لبناء Timestamp متوافق مع مسار Firestore.
            createdAt = hit.optLong("createdAtTimestamp", 0L).takeIf { it > 0 }
                ?.let { ms -> com.google.firebase.Timestamp(ms / 1000, ((ms % 1000) * 1_000_000).toInt()) },
        )
    }
}
