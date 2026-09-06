package com.eleven.store.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.eleven.store.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    // ✅ v2: لم يعد التطبيق يحتاج معرّف حساب المدير إطلاقاً — إشعار "طلب
    // جديد" لصاحب المتجر تُنشئه الآن Cloud Function واحدة (onOrderCreated)
    // تقرأ OWNER_OPEN_ID من إعدادات بيئة Cloud Functions فقط. سابقاً كان
    // هذا المعرّف مكرَّراً بثلاثة أماكن مستقلة (هنا، env.ts بالسيرفر،
    // firestore.rules) وكانت نسخة هذا الملف تحديداً متروكة بقيمة Placeholder
    // حرفية لم تُستبدل أبداً، فلم يكن يصل أي إشعار "طلب جديد" لصاحب المتجر
    // عن طلبات وصلت من التطبيق. التوحيد بمصدر واحد يمنع هذا الصنف من
    // الأخطاء بنيوياً بدل الاعتماد على تذكّر تحديث ثلاث نسخ متطابقة يدوياً.

    private val db      = FirebaseFirestore.getInstance()
    private val auth    = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val uid get() = auth.currentUser?.uid

    var lastProductsError: String? = null

    // ─── Auth ───────────────────────────────────────────────────
    val currentUser: FirebaseUser? get() = auth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun loginWithEmail(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password).await()

    // ✅ التسجيل يطابق سلوك الموقع: ينشئ الحساب، يضبط الاسم في Firebase Auth،
    // ثم يكتب وثيقة المستخدم في Firestore (نفس الحقول المستخدمة في الموقع)
    suspend fun registerWithEmail(name: String, email: String, phone: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("تعذر إنشاء الحساب")

        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user.updateProfile(profileUpdates).await()

        db.collection("users").document(user.uid).set(
            mapOf(
                "id" to user.uid,
                "name" to name,
                "email" to email,
                "phone" to phone,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
        ).await()

        return user
    }

    // ✅ تسجيل الدخول/التسجيل عبر Google، مطابق لسلوك الموقع (signInWithPopup + setDoc merge)
    suspend fun signInWithGoogleIdToken(idToken: String): FirebaseUser {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: throw IllegalStateException("فشل تسجيل الدخول عبر Google")

        db.collection("users").document(user.uid).set(
            mapOf(
                "id" to user.uid,
                "name" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "phone" to "",
                "avatar" to (user.photoUrl?.toString() ?: ""),
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()

        return user
    }

    suspend fun sendPasswordReset(email: String) =
        auth.sendPasswordResetEmail(email).await()

    // ✅ يراقب وثيقة users/{uid} مباشرةً — هذا هو مصدر رقم الهاتف الحقيقي
    // (وليس user.phoneNumber من Firebase Auth، فهو مخصص لتسجيل الدخول عبر
    // رقم الهاتف ويبقى فارغاً دائماً لحسابات البريد/كلمة المرور)
    fun observeUserProfile(): Flow<UserProfile?> = callbackFlow {
        val currentUid = uid
        if (currentUid == null) {
            trySend(null)
            awaitClose { }
        } else {
            val registration = db.collection("users").document(currentUid)
                .addSnapshotListener { snap, _ ->
                    trySend(snap?.toObject(UserProfile::class.java))
                }
            awaitClose { registration.remove() }
        }
    }

    // ✅ يحدّث الاسم في Firebase Auth (displayName) ورقم الهاتف + الاسم في
    // وثيقة Firestore (users/{uid})، بنفس منطق التسجيل ومطابق لسلوك الموقع
    suspend fun updateUserProfile(name: String, phone: String) {
        val user = auth.currentUser ?: throw IllegalStateException("لا يوجد مستخدم مسجل الدخول")

        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user.updateProfile(profileUpdates).await()

        db.collection("users").document(user.uid).set(
            mapOf(
                "id" to user.uid,
                "name" to name,
                "email" to (user.email ?: ""),
                "phone" to phone,
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: throw IllegalStateException("لا يوجد مستخدم مسجل")
        val email = user.email ?: throw IllegalStateException("لا يوجد بريد إلكتروني مرتبط بالحساب")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }

    suspend fun deleteAccount(currentPassword: String) {
        val user = auth.currentUser ?: throw IllegalStateException("لا يوجد مستخدم مسجل")
        val email = user.email ?: throw IllegalStateException("لا يوجد بريد إلكتروني مرتبط بالحساب")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.delete().await()
    }

    // ✅ إصلاح خصوصية: نحذف توكن FCM الخاص *بهذا الجهاز تحديداً* من fcmTokens
    // قبل تسجيل الخروج فعلياً. بدون هذا، لو استُخدم نفس الجهاز لاحقاً من
    // مستخدم آخر (جهاز مشترك)، يبقى توكن الحساب الأول مسجَّلاً وموجّهاً
    // فعلياً لنفس الجهاز — فإشعارات حسّاسة (تفاصيل طلب) قد تصل للمستخدم
    // الثاني الذي يستخدم الجهاز فعلياً الآن. نجلب التوكن ونحذفه بينما
    // المستخدم لا يزال مسجّل دخول (auth.currentUser ما زال متوفراً)، ثم
    // نسجّل الخروج. أي فشل بجلب/حذف التوكن (لا يوجد اتصال مثلاً) لا يجب أن
    // يمنع تسجيل الخروج نفسه أبداً.
    suspend fun logout() {
        try {
            val u = uid
            if (u != null) {
                val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                if (!token.isNullOrBlank()) {
                    db.collection("users").document(u)
                        .set(
                            mapOf("fcmTokens" to com.google.firebase.firestore.FieldValue.arrayRemove(token)),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        .await()
                }
            }
        } catch (e: Exception) {
            Log.w("FirestoreRepository", "تعذّر حذف توكن FCM عند تسجيل الخروج (لن يمنع تسجيل الخروج)", e)
        }
        auth.signOut()
    }

    // ─── Helper: أي نوع رقمي → Double بأمان ────────────────────
    // Firestore يخزّن الأرقام أحياناً كـ Long وأحياناً كـ Double حسب مصدر البيانات.
    // هذه الدالة تتعامل مع كلا النوعين بأمان تام.
    private fun DocumentSnapshot.getDouble2(field: String): Double =
        when (val v = get(field)) {
            is Double -> v
            is Long   -> v.toDouble()
            is Int    -> v.toDouble()
            is Float  -> v.toDouble()
            is Number -> v.toDouble()
            else      -> 0.0
        }

    private fun DocumentSnapshot.getLong2(field: String): Long =
        when (val v = get(field)) {
            is Long   -> v
            is Double -> v.toLong()
            is Int    -> v.toLong()
            is Number -> v.toLong()
            else      -> 0L
        }

    // ─── Helper: تحويل document → Order بدون toObject() ────────
    // القراءة اليدوية تتجنب أي تعارض بين Long/Double/Int في Firestore
    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toOrder(): Order? {
        return try {
            val itemsList = (get("items") as? List<Map<String, Any>>) ?: emptyList()
            val items = itemsList.mapNotNull { map ->
                try {
                    CartItem(
                        id        = map["id"]?.toString() ?: map["productId"]?.toString() ?: "",
                        productId = map["productId"]?.toString() ?: "",
                        name      = map["name"]?.toString() ?: "",
                        price     = when (val p = map["price"]) {
                            is Double -> p
                            is Long   -> p.toDouble()
                            is Number -> p.toDouble()
                            else      -> 0.0
                        },
                        quantity  = when (val q = map["quantity"]) {
                            is Long   -> q.toInt()
                            is Int    -> q
                            is Number -> q.toInt()
                            else      -> 1
                        },
                        image     = map["image"]?.toString() ?: "",
                    )
                } catch (e: Exception) { null }
            }

            val addrMap = get("shippingAddress") as? Map<String, Any>
            val address = addrMap?.let { m ->
                val createdAtMap = m["createdAt"] as? Map<String, Any>
                Address(
                    id        = m["id"]?.toString() ?: "",
                    fullName  = m["fullName"]?.toString() ?: "",
                    name      = m["name"]?.toString() ?: "",
                    phone     = m["phone"]?.toString() ?: "",
                    city      = m["city"]?.toString() ?: "",
                    address   = m["address"]?.toString() ?: "",
                    isDefault = m["isDefault"] as? Boolean ?: false,
                )
            }

            Order(
                id                = id,
                orderNumber       = getString("orderNumber") ?: "",
                userId            = getString("userId") ?: "",
                items             = items,
                total             = getDouble2("total"),
                subtotal          = getDouble2("subtotal"),
                shippingCost      = getDouble2("shippingCost"),
                discount          = getDouble2("discount"),
                couponCode        = getString("couponCode"),
                status            = OrderStatus.from(getString("status") ?: "pending"),
                paymentStatus     = getString("paymentStatus") ?: "unpaid",
                shippingAddress   = address,
                paymentMethod     = getString("paymentMethod") ?: "",
                paymentReceipt    = getString("paymentReceipt") ?: "",
                verificationToken = getString("verificationToken") ?: "",
                notes             = getString("notes") ?: "",
                createdAt         = getTimestamp("createdAt"),
            )
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "toOrder() failed for doc $id: ${e.message}", e)
            null
        }
    }

    // ─── Store Settings ─────────────────────────────────────────
    suspend fun getStoreSettings(): StoreSettings {
        return try {
            val doc = db.collection("settings").document("store").get().await()
            doc.toObject(StoreSettings::class.java) ?: StoreSettings()
        } catch (e: Exception) { StoreSettings() }
    }

    // ─── Banners ────────────────────────────────────────────────
    suspend fun getBanners(): List<Banner> {
        return try {
            db.collection("banners")
                .whereEqualTo("isActive", true)
                .orderBy("order", Query.Direction.ASCENDING)
                .get().await()
                .documents.mapNotNull { doc ->
                    doc.toObject(Banner::class.java)?.copy(id = doc.id)
                }
        } catch (e: Exception) {
            try {
                db.collection("banners")
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get().await()
                    .documents.mapNotNull { doc ->
                        doc.toObject(Banner::class.java)?.copy(id = doc.id)
                    }.filter { it.isActive }
            } catch (e2: Exception) { emptyList() }
        }
    }

    // ─── Categories ─────────────────────────────────────────────
    // مطابق تماماً لاستعلام getCategories في firestore-router.ts بالموقع:
    // فلترة isActive فقط بدون أي ترتيب إضافي (لا يوجد orderBy بالموقع)
    suspend fun getCategories(): List<Category> {
        return try {
            db.collection("categories")
                .whereEqualTo("isActive", true)
                .get().await()
                .documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.copy(id = doc.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    // ─── Brands ─────────────────────────────────────────────────
    suspend fun getBrands(): List<Brand> {
        return try {
            db.collection("brands")
                .get().await()
                .documents.mapNotNull { doc ->
                    doc.toObject(Brand::class.java)?.copy(id = doc.id)
                }.filter { it.isActive }
        } catch (e: Exception) { emptyList() }
    }

    // ─── Products ───────────────────────────────────────────────
    suspend fun getProducts(
        categoryId:   String?  = null,
        isFeatured:   Boolean? = null,
        isNew:        Boolean? = null,
        isBestSeller: Boolean? = null,
        onSale:       Boolean? = null,
        brandId:      String?  = null,
        searchQuery:  String?  = null,
        // ✅ إصلاح: كان 50 (أقل من حد الموقع 100)، ما يجعل نتائج البحث/العرض
        // على الأندرويد أضيق من الموقع لنفس الاستعلام تماماً. جرى توحيده مع
        // حد الموقع (server/firestore-router.ts getProducts limit(100)).
        limit:        Long     = 100,
    ): List<Product> {
        lastProductsError = null

        // ✅ Algolia: عند وجود نص بحث فعلي ومفاتيح Algolia مضبوطة، نستخدم
        // البحث الحقيقي بدل جلب حتى 100 منتج من Firestore وفلترتهم محلياً
        // بـcontains() (نفس القيد القديم المشترك مع الموقع). عند فشل الطلب
        // أو فراغ النتيجة أو عدم ضبط المفاتيح، نرجع تلقائياً لمسار Firestore
        // القديم أدناه — البحث لا يتعطل كلياً بغياب Algolia.
        if (!searchQuery.isNullOrBlank() && AlgoliaSearchService.isConfigured) {
            val hits = AlgoliaSearchService.searchProducts(
                query = searchQuery.trim(),
                categoryId = categoryId,
                brandId = brandId,
                onSale = onSale,
                isFeatured = isFeatured,
                // ✅ إصلاح: كانا يُسقَطان بصمت قبل هذا التعديل — البحث بالنص
                // مع فلتر "جديد"/"الأكثر مبيعاً" كان يرجع نتائج من كل المنتجات
                // بدل الاقتصار على الفلتر المطلوب (خلافاً لمسار Firestore
                // الاحتياطي أدناه الذي كان يطبّقهما بشكل صحيح، فيتضارب سلوك
                // نفس الشاشة حسب توفر Algolia من عدمه).
                isBestSeller = isBestSeller,
                isNew = isNew,
            )
            if (hits.isNotEmpty()) return hits
            // نتيجة فارغة من Algolia (لا فرق بين "لا نتائج فعلاً" و"فشل شبكة"
            // من منظور المستخدم) — نكمل للمسار القديم أدناه كشبكة أمان أخيرة
            // بدل عرض "لا نتائج" قد تكون غير صحيحة بسبب عطل مؤقت بـAlgolia فقط.
        }

        return try {
            var query: Query = db.collection("products")
            query = query.whereEqualTo("isActive", true)
            categoryId?.let   { query = query.whereEqualTo("categoryId",   it) }
            isFeatured?.let   { query = query.whereEqualTo("isFeatured",   it) }
            // ✅ إصلاح جذري: isNew لم يكن يُكتب كحقل Boolean في أي مستند إطلاقاً
            // (نفس الحقل المفقود على الموقع) — هذا الفلتر كان يعيد قائمة فارغة
            // دائماً. الموقع أصلح هذا باستخدام نافذة تاريخية (آخر 30 يوماً) بدل
            // حقل Boolean وهمي؛ نطابق هنا نفس المنطق تماماً بدل حقل isNew.
            var newSince: java.util.Date? = null
            if (isNew == true) {
                newSince = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -30) }.time
                query = query.whereGreaterThanOrEqualTo("createdAt", newSince)
            }
            isBestSeller?.let { query = query.whereEqualTo("isBestSeller", it) }
            onSale?.let       { query = query.whereEqualTo("isOnSale",     it) }
            brandId?.let      { query = query.whereEqualTo("brandId",      it) }
            // ✅ إصلاح: لم يكن هناك أي ترتيب — النتائج كانت تعود بترتيب مستند
            // عشوائي فعلياً بدل الأحدث أولاً (نفس عائلة مشكلة الفهارس المذكورة
            // بتعليقات السيرفر). يطابق الآن ترتيب الموقع (createdAt DESC).
            query = query.orderBy("createdAt", Query.Direction.DESCENDING)
            query = query.limit(limit)

            val results = query.get().await()
                .documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }

            if (!searchQuery.isNullOrBlank()) {
                val q = searchQuery.lowercase()
                results.filter {
                    it.name.lowercase().contains(q) ||
                    it.description.lowercase().contains(q)
                }
            } else results
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "getProducts failed: ${e.message}", e)
            lastProductsError = "${e.javaClass.simpleName}: ${e.message}"
            emptyList()
        }
    }

    suspend fun getProduct(id: String): Product? {
        return try {
            val doc = db.collection("products").document(id).get().await()
            doc.toObject(Product::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "getProduct($id) failed: ${e.message}", e)
            null
        }
    }

    // ─── Cart ───────────────────────────────────────────────────
    // ✅ مطابق تماماً لـ getCart بالموقع: يجلب أحدث مخزون لكل منتج في السلة، ويحذف تلقائياً
    // أي عنصر أصبح منتجه غير نشط أو نفدت كميته بالكامل
    fun observeCart(): Flow<List<CartItem>> = callbackFlow {
        val u = uid ?: run { trySend(emptyList()); awaitClose {}; return@callbackFlow }
        val cartCollection = db.collection("users").document(u).collection("cart")
        val listener = cartCollection.addSnapshotListener { snap, _ ->
            val rawItems = snap?.documents?.mapNotNull { doc ->
                doc.toObject(CartItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            if (rawItems.isEmpty()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            launch {
                val enriched = mutableListOf<CartItem>()
                val batch = db.batch()
                var needsCommit = false
                for (item in rawItems) {
                    try {
                        val productSnap = db.collection("products").document(item.productId).get().await()
                        val isActive = productSnap.getBoolean("isActive") != false
                        val stock = productSnap.getLong2("stock")
                        if (!productSnap.exists() || !isActive || stock <= 0) {
                            // المنتج لم يعد موجوداً/نشطاً أو نفدت كميته بالكامل: نحذفه تلقائياً من السلة
                            batch.delete(cartCollection.document(item.id))
                            needsCommit = true
                            continue
                        }
                        val cappedQty = minOf(item.quantity.toLong(), stock).toInt()
                        if (cappedQty != item.quantity) {
                            batch.update(cartCollection.document(item.id), "quantity", cappedQty)
                            needsCommit = true
                        }
                        enriched.add(item.copy(quantity = cappedQty, stock = stock))
                    } catch (e: Exception) {
                        enriched.add(item)
                    }
                }
                if (needsCommit) {
                    try { batch.commit().await() } catch (e: Exception) { /* ignore */ }
                }
                trySend(enriched)
            }
        }
        awaitClose { listener.remove() }
    }

    // ✅ يتحقق من المخزون ولا يسمح بتجاوزه عند الإضافة للسلة (مطابق لمنطق السيرفر في الموقع)
    suspend fun addToCart(item: CartItem) {
        val u = uid ?: return
        val cartRef = db.collection("users").document(u).collection("cart").document(item.productId)
        val productRef = db.collection("products").document(item.productId)

        db.runTransaction { tx ->
            val productSnap = tx.get(productRef)
            if (!productSnap.exists() || productSnap.getBoolean("isActive") == false) {
                throw IllegalStateException("هذا المنتج لم يعد متوفراً")
            }
            val stock = productSnap.getLong("stock") ?: 0L
            if (stock <= 0) {
                throw IllegalStateException("الكمية المطلوبة غير متوفرة في المخزون")
            }

            val cartSnap = tx.get(cartRef)
            val currentQty = if (cartSnap.exists()) (cartSnap.getLong("quantity") ?: 0L) else 0L
            val requestedQty = currentQty + item.quantity

            if (requestedQty > stock) {
                throw IllegalStateException("الكمية المطلوبة غير متوفرة في المخزون")
            }

            if (cartSnap.exists()) {
                tx.update(cartRef, "quantity", requestedQty)
            } else {
                tx.set(cartRef, item.copy(id = item.productId, quantity = requestedQty.toInt()))
            }
            null
        }.await()
    }

    suspend fun removeFromCart(productId: String) {
        val u = uid ?: return
        db.collection("users").document(u).collection("cart").document(productId).delete().await()
    }

    // ✅ تعديل الكمية من السلة فقط، مقيّداً بالكمية المتوفرة في المخزون
    // يعيد true إذا تم تقييد الكمية بحد المخزون (capped) — مطابق لاستجابة updateCartQuantity في الموقع
    suspend fun updateQuantity(productId: String, newQty: Int): Boolean {
        val u = uid ?: return false
        val productSnap = db.collection("products").document(productId).get().await()
        val stock = productSnap.getLong("stock") ?: 0L
        if (stock <= 0) {
            db.collection("users").document(u).collection("cart").document(productId).delete().await()
            throw IllegalStateException("الكمية المطلوبة غير متوفرة في المخزون")
        }
        val capped = newQty.toLong() > stock
        val finalQty = minOf(newQty.toLong(), stock)
        db.collection("users").document(u).collection("cart")
            .document(productId)
            .update("quantity", finalQty).await()
        return capped
    }

    suspend fun clearCart() {
        val u = uid ?: return
        val batch = db.batch()
        val items = db.collection("users").document(u).collection("cart").get().await()
        items.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    // ─── Favorites ──────────────────────────────────────────────
    fun observeFavorites(): Flow<List<String>> = callbackFlow {
        val u = uid ?: run { trySend(emptyList()); awaitClose {}; return@callbackFlow }
        val listener = db.collection("users").document(u).collection("favorites")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.map { it.id } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ✅ إصلاح: كانت FavoritesScreen تشتق قائمة منتجات المفضلة عبر تصفية
    // viewModel.allProducts (حالة مشتركة تُملؤها شاشة المنتجات بآخر فلتر/بحث
    // استخدمه المستخدم هناك، ومحدودة بـ100 نتيجة، وتستبعد المنتجات نافدة
    // المخزون). النتيجة: شاشة المفضلة تظهر فارغة أو ناقصة إن لم تتم زيارة
    // شاشة المنتجات أصلاً، أو إن كان آخر فلتر مستخدم هناك لا يشمل كل
    // المنتجات المفضَّلة (فئة معيّنة، بحث نصي، فلتر "عروض"...)، أو إن كان
    // أحد عناصر المفضلة نافد المخزون. الموقع (getFavorites بـfirestore-
    // router.ts) يجلب بيانات كل منتج مفضَّل مباشرة من مستنده الخاص بدل
    // الاعتماد على أي قائمة/فلتر آخر — هذه الدالة تطابق نفس المنطق تماماً
    // (تُبقي عناصر المخزون=0 ظاهرة كالموقع، وتحذف تلقائياً أي مفضّلة لمنتج
    // لم يعد موجوداً أو أصبح غير نشط).
    suspend fun getFavoriteProducts(): List<Product> {
        val u = uid ?: return emptyList()
        val favRef = db.collection("users").document(u).collection("favorites")
        return try {
            val favDocs = favRef.get().await().documents
            if (favDocs.isEmpty()) return emptyList()

            val productIds = favDocs.map { it.getString("productId") ?: it.id }
            val productSnaps = productIds.map { id ->
                db.collection("products").document(id).get().await()
            }

            val batch = db.batch()
            var needsCommit = false
            val result = mutableListOf<Product>()

            favDocs.forEachIndexed { idx, favDoc ->
                val snap = productSnaps[idx]
                val product = if (snap.exists()) snap.toObject(Product::class.java)?.copy(id = snap.id) else null
                if (product == null || snap.getBoolean("isActive") == false) {
                    // المنتج لم يعد موجوداً أو أصبح غير نشط: نحذفه تلقائياً من المفضلة
                    // (مطابق تماماً لنفس التنظيف التلقائي في getFavorites بالموقع)
                    batch.delete(favRef.document(favDoc.id))
                    needsCommit = true
                } else {
                    result.add(product)
                }
            }
            if (needsCommit) {
                try { batch.commit().await() } catch (_: Exception) { /* تنظيف ثانوي، تجاهل الفشل */ }
            }
            result
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "getFavoriteProducts failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun toggleFavorite(productId: String): Boolean {
        val u = uid ?: return false
        val ref = db.collection("users").document(u).collection("favorites").document(productId)
        val doc = ref.get().await()
        return if (doc.exists()) {
            ref.delete().await(); false
        } else {
            ref.set(mapOf(
                "productId" to productId,
                "addedAt" to com.google.firebase.Timestamp.now()
            )).await(); true
        }
    }

    // ─── Addresses ──────────────────────────────────────────────
    suspend fun getAddresses(): List<Address> {
        val u = uid ?: return emptyList()
        return try {
            db.collection("users").document(u).collection("addresses")
                .get().await()
                .documents.mapNotNull { it.toObject(Address::class.java)?.copy(id = it.id) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addAddress(address: Address): String {
        val u = uid ?: return ""
        val ref = db.collection("users").document(u).collection("addresses").document()
        ref.set(address).await()
        return ref.id
    }

    suspend fun updateAddress(addressId: String, address: Address) {
        val u = uid ?: return
        db.collection("users").document(u).collection("addresses").document(addressId).set(address).await()
    }

    suspend fun deleteAddress(addressId: String) {
        val u = uid ?: return
        db.collection("users").document(u).collection("addresses").document(addressId).delete().await()
    }

    // ─── Orders ─────────────────────────────────────────────────
    // ✅ إصلاح حرج: كان يُخزَّن في paymentReceipt فقط `receiptUri.lastPathSegment`
    // (وهو أحياناً معرّف رقمي داخلي من موفّر المحتوى على الجهاز، لا علاقة له بأي
    // صورة فعلية) بدل رفع الصورة ثم تخزين رابط تنزيلها الحقيقي. هذا ما جعل زر
    // "التحقق من الإيصال" في لوحة تحكم الموقع يفتح روابط خاطئة مثل
    // https://eleven-sd.com/admin/1000314108 بدل صورة الإيصال الفعلية من
    // Firebase Storage. الآن نرفع الصورة فعلياً ونعيد رابط تنزيل حقيقي وصالح.
    suspend fun uploadPaymentReceipt(context: Context, uri: Uri): String {
        val u = uid ?: throw IllegalStateException("يجب تسجيل الدخول لرفع إيصال الدفع")
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("تعذر قراءة صورة الإيصال المختارة")
        val ref = storage.reference.child("receipts/$u/${System.currentTimeMillis()}.jpg")
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun getOrders(): List<Order> {
        val u = uid ?: return emptyList()
        return try {
            db.collection("orders")
                .whereEqualTo("userId", u)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
                .documents.mapNotNull { it.toOrder() }    // ← قراءة يدوية آمنة
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "getOrders failed: ${e.message}", e)
            lastProductsError = "getOrders: ${e.message}"
            // Fallback بدون orderBy
            try {
                db.collection("orders")
                    .whereEqualTo("userId", u)
                    .get().await()
                    .documents.mapNotNull { it.toOrder() }
                    .sortedByDescending { it.createdAt?.seconds }
            } catch (e2: Exception) {
                Log.e("FirestoreRepo", "getOrders fallback failed: ${e2.message}", e2)
                emptyList()
            }
        }
    }

    // ✅ إصلاح ثغرة حرجة: كانت هذه الدالة تُرجع أي طلب بمجرد معرفة رقم مستنده (orderId)
    // دون أي تحقق من أن المستخدم الحالي هو صاحب الطلب فعلاً — أي رابط مباشر لفاتورة
    // كان يفتح فاتورة أي مستخدم آخر بكل بياناته. الآن نتحقق محلياً من ملكية الطلب
    // (userId == uid الحالي) قبل إرجاعه، ونُرجع null (= "غير مصرح/غير موجود" من منظور
    // العميل) إن لم يكن المستخدم هو المالك. الحماية الملزمة الفعلية هي قواعد Firestore
    // (راجع firestore.rules) لأن أي تحقق داخل التطبيق يمكن تجاوزه من عميل معدَّل.
    suspend fun getOrder(orderId: String): Order? {
        val currentUid = uid ?: return null
        return try {
            val doc = db.collection("orders").document(orderId).get().await()
            val order = doc.toOrder() ?: return null
            if (order.userId != currentUid) {
                Log.w("FirestoreRepo", "getOrder($orderId) unauthorized access attempt by $currentUid (owner=${order.userId})")
                return null
            }
            order
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "getOrder($orderId) failed: ${e.message}", e)
            null
        }
    }

    // ─── Coupons ──────────────────────────────────────────────────
    // نفس قواعد التحقق المستخدمة في placeOrder (وفي السيرفر بالموقع) حتى لا يختلف
    // السلوك بين المعاينة قبل الدفع والتنفيذ الفعلي عند إتمام الطلب.
    private fun evaluateCoupon(coupon: Coupon?, subtotal: Double): CouponResult {
        if (coupon == null) return CouponResult.Invalid("كود الخصم غير صالح")
        if (!coupon.isActive) return CouponResult.Invalid("كود الخصم غير مُفعّل حالياً")
        coupon.expiresAt?.let {
            if (it.toDate().time < System.currentTimeMillis()) {
                return CouponResult.Invalid("انتهت صلاحية كود الخصم")
            }
        }
        if (subtotal < coupon.minOrderAmount) {
            return CouponResult.Invalid("الحد الأدنى للطلب لاستخدام هذا الكود ${coupon.minOrderAmount} ج.س")
        }
        if (coupon.usageLimit > 0 && coupon.usageCount >= coupon.usageLimit) {
            return CouponResult.Invalid("تم استنفاد عدد مرات استخدام هذا الكود")
        }
        val discount = if (coupon.discountType == "percentage")
            subtotal * (coupon.discountValue / 100.0)
        else
            minOf(coupon.discountValue, subtotal)
        return CouponResult.Valid(discountAmount = Math.round(discount * 100) / 100.0, coupon = coupon)
    }

    /** معاينة فورية قبل الدفع — قراءة فقط، لا تلمس عدّاد الاستخدام. */
    suspend fun validateCoupon(code: String, subtotal: Double): CouponResult {
        return try {
            val doc = db.collection("coupons").document(code.trim().uppercase()).get().await()
            val coupon = doc.toObject(Coupon::class.java)?.copy(code = doc.id)
            evaluateCoupon(if (doc.exists()) coupon else null, subtotal)
        } catch (e: Exception) {
            CouponResult.Invalid("تعذر التحقق من كود الخصم")
        }
    }

    suspend fun placeOrder(order: Order, couponCode: String? = null): String {
        val u = uid ?: throw IllegalStateException("يجب تسجيل الدخول لإتمام الطلب")
        val ref = db.collection("orders").document()
        val verificationToken = java.util.UUID.randomUUID().toString().replace("-", "").take(26)

        val counterRef = db.collection("counters").document("orders")
        val code = couponCode?.trim()?.uppercase()
        val couponRef = code?.let { db.collection("coupons").document(it) }
        // ✅ إصلاح (Audit M-4): سجل استخدام الكوبون لكل مستخدم — منع إعادة استخدام
        // نفس الكوبون أكثر من مرة لكل مستخدم (مطابق لقاعدة coupons/{code}/usedBy/{uid}).
        val couponUsedByRef = couponRef?.collection("usedBy")?.document(u)

        // ✅ إصلاح (Audit H-1/H-2/M-4): تم دمج كل شيء ضمن معاملة (transaction) واحدة
        // ذرّية بالكامل — قراءة المنتجات/الكوبون/العدّاد/سجل الاستخدام، ثم كتابة خصم
        // المخزون + زيادة استخدام الكوبون + سجل usedBy + عدّاد رقم الطلب + مستند
        // الطلب نفسه، كلها معاً أو لا شيء منها إطلاقاً. سابقاً كانت هذه ثلاث عمليات
        // منفصلة (معاملتان + كتابة حرة)، فكان ممكناً أن يُخصَم المخزون/الكوبون بلا
        // إنشاء مستند الطلب فعلياً إذا انقطع الاتصال بين الخطوات.
        // ⚠️ ملاحظة: هذا يمنع تلاعب واجهة التطبيق بالسعر، وقواعد Firestore (firestore.rules)
        // تفرض الآن نفس التحقق من السعر/الخصم مباشرة على مستوى القاعدة أيضاً (دفاع مزدوج)
        // لأي كتابة تصل خارج التطبيق تماماً.
        // ✅ إصلاح (ثغرة مالية): shippingCost كان يُؤخذ سابقاً مباشرة من order
        // (كائن مبني على العميل) بلا أي إعادة اشتقاق من settings/store داخل
        // الـtransaction — بعكس السعر والكوبون اللذين يُعاد التحقق منهما فعلياً.
        // عميل مُعاد بناؤه (APK مفكوك) كان يستطيع إرسال shippingCost: 0 مع كل
        // طلب. الآن يُشتق من settings/store هنا (نفس منطق shippingBase/
        // freeShippingThreshold المستخدم بالسيرفر)، ويجب أن يطابقه أيضاً
        // firestore.rules (shippingCostValid) كخط دفاع مستقل ثانٍ.
        val settingsRef = db.collection("settings").document("store")

        val orderId = db.runTransaction { tx ->
            val productRefs = order.items.map { db.collection("products").document(it.productId) }
            val productSnaps = productRefs.map { tx.get(it) }
            val couponSnap = couponRef?.let { tx.get(it) }
            val couponUsedBySnap = couponUsedByRef?.let { tx.get(it) }
            val counterSnap = tx.get(counterRef)
            val settingsSnap = tx.get(settingsRef)

            val authoritativeItems = productSnaps.mapIndexed { idx, snap ->
                val item = order.items[idx]
                if (!snap.exists() || snap.getBoolean("isActive") == false) {
                    throw IllegalStateException("${item.name}: هذا المنتج لم يعد متوفراً")
                }
                val stock = snap.getLong("stock") ?: 0L
                if (item.quantity > stock) {
                    throw IllegalStateException("${item.name}: الكمية المطلوبة غير متوفرة في المخزون")
                }
                val price = when (val p = snap.get("price")) {
                    is Double -> p
                    is Long -> p.toDouble()
                    is Number -> p.toDouble()
                    else -> 0.0
                }
                item.copy(price = price, name = snap.getString("name") ?: item.name)
            }
            val subtotal = authoritativeItems.sumOf { it.price * it.quantity }

            var discountAmount = 0.0
            var appliedCoupon: String? = null
            if (code != null) {
                if (couponUsedBySnap?.exists() == true) {
                    throw IllegalStateException("لقد استخدمت هذا الكود من قبل")
                }
                val coupon = couponSnap?.takeIf { it.exists() }
                    ?.toObject(Coupon::class.java)?.copy(code = code)
                when (val check = evaluateCoupon(coupon, subtotal)) {
                    is CouponResult.Valid -> { discountAmount = check.discountAmount; appliedCoupon = code }
                    is CouponResult.Invalid -> throw IllegalStateException(check.message)
                }
            }

            // ✅ سعر الشحن مشتق هنا من settings/store الحقيقي، وليس من order.shippingCost
            // القادم من العميل — نفس منطق shippingBase/freeShippingThreshold بالسيرفر.
            val shippingBase = when (val s = settingsSnap.get("shippingCost")) {
                is Number -> s.toDouble()
                else -> 30.0
            }
            val freeShippingThreshold = when (val t = settingsSnap.get("freeShippingThreshold")) {
                is Number -> t.toDouble()
                else -> 0.0
            }
            val shippingCost = if (freeShippingThreshold > 0 && subtotal >= freeShippingThreshold) 0.0 else shippingBase

            val total = Math.round((subtotal - discountAmount + shippingCost) * 100) / 100.0
            val current = (counterSnap.getLong("current") ?: 11001000L)
            val next = current + 1

            // ── الكتابات (كل القراءات أعلاه تسبق أي كتابة، كما تفرضه واجهة Transaction) ──
            productSnaps.forEachIndexed { idx, snap ->
                val item = order.items[idx]
                val stock = snap.getLong("stock") ?: 0L
                tx.update(productRefs[idx], "stock", stock - item.quantity)
            }
            if (appliedCoupon != null && couponRef != null) {
                val usageCount = couponSnap?.getLong("usageCount") ?: 0L
                tx.update(couponRef, "usageCount", usageCount + 1)
                couponUsedByRef?.let { tx.set(it, mapOf("usedAt" to com.google.firebase.Timestamp.now())) }
            }
            tx.set(counterRef, mapOf("current" to next), com.google.firebase.firestore.SetOptions.merge())

            val orderWithId = order.copy(
                id = ref.id,
                userId = u,
                items = authoritativeItems,
                subtotal = subtotal,
                discount = discountAmount,
                couponCode = appliedCoupon,
                shippingCost = shippingCost,
                total = total,
                orderNumber = next.toString(),
                verificationToken = verificationToken,
                createdAt = com.google.firebase.Timestamp.now(),
            )
            tx.set(ref, orderWithId)

            ref.id
        }.await()

        // ✅ نظام الإشعارات v2: لم يعد هذا الكود يكتب أي إشعار بنفسه.
        // Cloud Function واحدة (functions/src/triggers/orderTriggers.ts::
        // onOrderCreated) تلاحظ إنشاء مستند orders/{orderId} نفسه أعلاه
        // وتُنشئ إشعارَي العميل والأدمن + ترسل Push فعلياً عبر Admin SDK.
        // سابقاً كانت الكتابة هنا تتم مباشرة من العميل (Client SDK) فتُنشئ
        // سجل الإشعار فقط بدون أي قدرة على إرسال Push حقيقي — أي أن صاحب
        // المتجر لم يكن يصله أي تنبيه إطلاقاً عن طلبات وصلت من هذا التطبيق
        // تحديداً، فقط سجل صامت يظهر إن فتح قائمة إشعاراته بنفسه صدفة.
        return orderId
    }

    // ─── Notifications (v2) ───────────────────────────────────────
    // ⚠️ لا توجد أي دالة كتابة مباشرة على Firestore هنا بعد الآن — كل إنشاء
    // إشعار يتم حصراً عبر Cloud Functions (Admin SDK)، انظر placeOrder أعلاه
    // وorderTriggers.ts. القراءة فقط real-time تبقى مباشرة (مسموحة بقواعد
    // الأمان)، والتعديل (قراءة/حذف) يمر عبر Callable Functions أدناه.

    // نسخة لحظية (real-time) عبر addSnapshotListener — لا يوجد استعلام "لمرّة
    // واحدة" منفصل هنا عمداً: أي إشعار جديد (Push من السيرفر يكتب هنا، أو
    // onMessageReceived بتطبيق الأندرويد نفسه) يظهر فوراً بقائمة الإشعارات
    // وبعداد "غير المقروء" بالهيدر دون أي تأخير أو حاجة لإعادة الجلب يدوياً.
    // onError يُبلَّغ للمستدعي (بدل Log فقط) — بدونه، أي خطأ (مثال: فهرس
    // Firestore مركّب غير مُفعّل بعد) يجعل القائمة تبقى فارغة للأبد بصمت تام.
    fun observeNotifications(onError: (Throwable) -> Unit = {}): Flow<List<NotificationItem>> = callbackFlow {
        val u = uid ?: run { trySend(emptyList()); awaitClose {}; return@callbackFlow }
        val listener = db.collection("users").document(u).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "observeNotifications failed: ${error.message}", error)
                    onError(error)
                    return@addSnapshotListener
                }
                val items = snap?.documents?.mapNotNull {
                    it.toObject(NotificationItem::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    // ✅ v2: تعليم "مقروء"/الحذف كتابة مباشرة على Firestore (Client SDK) —
    // مسموحة الآن بقواعد الأمان (محصورة بحقلي isRead/readAt فقط للتعديل)،
    // وتُصان تلقائياً بطابور offline محلي من Firestore SDK نفسه عند انقطاع
    // الاتصال، تُزامَن فور عودته دون أي كود إضافي. عدّاد notifUnreadCount لا
    // يتأثر بهذه الكتابة مباشرة — يُسوّى ذرّياً بواسطة Cloud Function مستقلة
    // (notificationCounterTrigger.ts) تلاحظ هذه الكتابة نفسها بصرف النظر عن
    // مصدرها، فيبقى صحيحاً ومطابقاً لما يعرضه الموقع دون أي استدعاء إضافي هنا.
    suspend fun markNotificationRead(notifId: String) {
        val u = uid ?: return
        db.collection("users").document(u).collection("notifications")
            .document(notifId)
            .update(mapOf("isRead" to true, "readAt" to Timestamp.now()))
            .await()
    }

    suspend fun markAllNotificationsRead() {
        val u = uid ?: return
        val unread = db.collection("users").document(u).collection("notifications")
            .whereEqualTo("isRead", false)
            .get().await()
        if (unread.isEmpty) return
        val now = Timestamp.now()
        val batch = db.batch()
        unread.documents.forEach { batch.update(it.reference, mapOf("isRead" to true, "readAt" to now)) }
        batch.commit().await()
    }

    suspend fun deleteNotification(notifId: String) {
        val u = uid ?: return
        db.collection("users").document(u).collection("notifications")
            .document(notifId).delete().await()
    }

    // حذف كل إشعارات المستخدم دفعة واحدة (زر "حذف الكل") — نفس السلوك
    // المتاح في نسخة الموقع (deleteAllNotifications بالسيرفر).
    suspend fun deleteAllNotifications() {
        val u = uid ?: return
        val all = db.collection("users").document(u).collection("notifications")
            .get().await()
        if (all.isEmpty) return
        val batch = db.batch()
        all.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    // ✅ جديد v2: عداد "غير المقروء" الحقيقي — حقل مشتق يُحدَّث ذرّياً على
    // السيرفر (Cloud Function) مع كل إنشاء/تعليم كمقروء/حذف، بدل عدّ عناصر
    // observeNotifications أعلاه (محدودة بـlimit(50) — تُعطي رقماً خاطئاً
    // بمجرد تجاوز غير المقروء هذا الحد). هذا هو نفس الحقل الذي يقرأه جرس
    // الإشعارات بالموقع مباشرة، فيتطابق الرقم المعروض على المنصتين دوماً.
    fun observeUnreadCount(): Flow<Int> = callbackFlow {
        val u = uid ?: run { trySend(0); awaitClose {}; return@callbackFlow }
        val listener = db.collection("users").document(u)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "observeUnreadCount failed: ${error.message}", error)
                    return@addSnapshotListener
                }
                trySend((snap?.getLong("notifUnreadCount") ?: 0L).toInt().coerceAtLeast(0))
            }
        awaitClose { listener.remove() }
    }

    // ─── FCM Token ──────────────────────────────────────────────
    // ✅ إصلاح: كان يُكتب سابقاً في حقل "fcmToken" (مفرد)، بينما يقرأ السيرفر/الموقع من
    // "fcmTokens" (مصفوفة) — ما كان يعني عملياً أن إشعارات الـPush لا تصل لأي مستخدم أندرويد.
    // الآن نستخدم نفس الحقل والبنية (arrayUnion) المستخدمة في الموقع، وتدعم تعدد الأجهزة لنفس المستخدم.
    suspend fun updateFcmToken(token: String) {
        val u = uid ?: return
        db.collection("users").document(u)
            .set(
                mapOf("fcmTokens" to com.google.firebase.firestore.FieldValue.arrayUnion(token)),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()
    }

    /**
     * يُستدعى بعد كل تسجيل دخول/تسجيل ناجح لضمان تسجيل توكن FCM الحالي فوراً —
     * يعالج مشكلة توكن يُنشأ قبل تسجيل الدخول (onNewToken وحده لا يعيد المحاولة لاحقاً).
     */
    suspend fun syncFcmToken() {
        try {
            val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            updateFcmToken(token)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "syncFcmToken failed: ${e.message}", e)
        }
    }
}
