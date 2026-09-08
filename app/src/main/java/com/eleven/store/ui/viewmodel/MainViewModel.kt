package com.eleven.store.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleven.store.data.model.*
import com.eleven.store.data.repository.EmailNotVerifiedException
import com.eleven.store.data.repository.FirestoreRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    val repo = FirestoreRepository()

    // ─── Auth ───────────────────────────────────────────────────
    val currentUser = repo.observeAuthState().stateIn(
        viewModelScope, SharingStarted.Eagerly, repo.currentUser
    )

    // ✅ بيانات الملف الشخصي من Firestore (فيها رقم الهاتف الحقيقي)
    val userProfile = repo.observeUserProfile().stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    fun updateProfile(name: String, phone: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try { repo.updateUserProfile(name, phone); onResult(true, null) }
            catch (e: Exception) { onResult(false, e.message ?: "تعذر حفظ التعديلات") }
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try { repo.loginWithEmail(email, password); repo.syncFcmToken(); onResult(true, null) }
            catch (e: EmailNotVerifiedException) {
                onResult(false, "لم يتم تأكيد بريدك الإلكتروني بعد. أرسلنا رابط تأكيد جديد إلى بريدك، افتحه ثم سجّل الدخول مرة أخرى.")
            }
            catch (e: Exception) { onResult(false, mapAuthError(e, "فشل تسجيل الدخول")) }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try { repo.changePassword(currentPassword, newPassword); onResult(true, null) }
            catch (e: Exception) { onResult(false, e.message) }
        }
    }

    fun deleteAccount(currentPassword: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try { repo.deleteAccount(currentPassword); onResult(true, null) }
            catch (e: Exception) { onResult(false, e.message) }
        }
    }

    fun deleteAccountWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try { repo.deleteAccountWithGoogle(idToken); onResult(true, null) }
            catch (e: Exception) { onResult(false, e.message) }
        }
    }

    fun isCurrentUserGoogleAccount(): Boolean = repo.isCurrentUserGoogleAccount()

    // ✅ إصلاح: registerWithEmail تُسجّل الخروج تلقائياً بعد إنشاء الحساب
    // (التحقق أصبح إجبارياً)، فاستدعاء syncFcmToken بعدها كان سيفشل بصمت
    // لعدم وجود مستخدم مسجَّل دخول فعلياً.
    fun register(name: String, email: String, phone: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try { repo.registerWithEmail(name, email, phone, password); onResult(true, null) }
            catch (e: Exception) { onResult(false, mapAuthError(e, "فشل إنشاء الحساب، يرجى المحاولة مرة أخرى")) }
        }
    }

    // ✅ جديد: إعادة إرسال رابط تأكيد البريد الإلكتروني من شاشة الإعدادات
    fun resendEmailVerification(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try { repo.resendEmailVerification(); onResult(true, null) }
            catch (e: Exception) { onResult(false, "تعذّر إرسال رابط التأكيد، حاول لاحقاً") }
        }
    }

    fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try { repo.signInWithGoogleIdToken(idToken); repo.syncFcmToken(); onResult(true, null) }
            catch (e: Exception) { onResult(false, "فشل تسجيل الدخول عبر Google") }
        }
    }

    // ✅ يطابق رسائل الأخطاء المستخدمة في الموقع (Login.tsx / Register.tsx)
    private fun mapAuthError(e: Exception, fallback: String): String {
        return when (e) {
            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "البريد الإلكتروني غير مسجل"
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "البريد الإلكتروني مستخدم بالفعل"
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "كلمة المرور ضعيفة جداً"
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> when (e.errorCode) {
                "ERROR_WRONG_PASSWORD" -> "كلمة المرور غير صحيحة"
                "ERROR_INVALID_EMAIL" -> "البريد الإلكتروني غير صحيح"
                else -> fallback
            }
            is com.google.firebase.FirebaseTooManyRequestsException -> "تم تجاوز عدد المحاولات، يرجى المحاولة لاحقاً"
            else -> fallback
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try { repo.sendPasswordReset(email); onResult(true) }
            catch (e: Exception) { onResult(false) }
        }
    }

    // ✅ تعديل: logout() بالريبوزيتوري أصبحت suspend (تحذف توكن FCM الخاص
    // بالجهاز من Firestore قبل تسجيل الخروج)، لذا يجب استدعاؤها من كوروتين.
    fun logout() {
        viewModelScope.launch {
            repo.logout()
        }
    }

    // ─── Store Data ─────────────────────────────────────────────
    private val _storeSettings = MutableStateFlow(StoreSettings())
    val storeSettings: StateFlow<StoreSettings> = _storeSettings

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _brands = MutableStateFlow<List<Brand>>(emptyList())
    val brands: StateFlow<List<Brand>> = _brands

    // ─── Products ───────────────────────────────────────────────
    private val _featuredProducts = MutableStateFlow<List<Product>>(emptyList())
    val featuredProducts: StateFlow<List<Product>> = _featuredProducts

    private val _newArrivals = MutableStateFlow<List<Product>>(emptyList())
    val newArrivals: StateFlow<List<Product>> = _newArrivals

    private val _bestSellers = MutableStateFlow<List<Product>>(emptyList())
    val bestSellers: StateFlow<List<Product>> = _bestSellers

    private val _onSaleProducts = MutableStateFlow<List<Product>>(emptyList())
    val onSaleProducts: StateFlow<List<Product>> = _onSaleProducts

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts

    // ✅ جديد (Pagination/Infinite Scroll): هل توجد صفحة منتجات تالية،
    // وحالة تحميلها — منفصلة عن isLoading/الصفحة الأولى حتى لا يظهر سبينر
    // ملء الشاشة عند التمرير لأسفل لتحميل المزيد فقط.
    private val _hasMoreProducts = MutableStateFlow(false)
    val hasMoreProducts: StateFlow<Boolean> = _hasMoreProducts

    private val _isLoadingMoreProducts = MutableStateFlow(false)
    val isLoadingMoreProducts: StateFlow<Boolean> = _isLoadingMoreProducts

    // آخر معاملات فلترة/بحث مُستخدَمة بـloadProducts — تُستخدَم في
    // loadMoreProducts() لطلب الصفحة التالية بنفس الفلاتر بالضبط.
    private var lastProductsQuery: ProductsQueryParams? = null

    data class ProductsQueryParams(
        val categoryId: String?,
        val isFeatured: Boolean?,
        val isNew: Boolean?,
        val isBestSeller: Boolean?,
        val onSale: Boolean?,
        val brandId: String?,
        val searchQuery: String?,
    )

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct

    // يميّز بين "لا يزال يحمّل" و"لم يتم العثور على المنتج" — مطابق لتمييز
    // isLoading / error || !product في ProductDetail.tsx بالموقع
    private val _isProductLoading = MutableStateFlow(false)
    val isProductLoading: StateFlow<Boolean> = _isProductLoading

    // ─── Cart ───────────────────────────────────────────────────
    // ✅ إصلاح: onError يُبلَّغ هنا (نفس نمط الإشعارات) بدل أن يُعامَل أي خطأ
    // لحظي بمستمع Firestore كـ"سلة فارغة" صامتة — بدونه يختفي زر "إتمام
    // الشراء" فجأة عند أي انقطاع اتصال قصير، رغم أن السلة تحتوي عناصر فعلياً.
    // _cartRetryTrigger يسمح بإعادة فتح مستمع جديد فعلياً عند "إعادة المحاولة"
    // (وليس فقط مسح رسالة الخطأ)، لأن بعض الأخطاء (صلاحيات مثلاً) توقف
    // المستمع القديم نهائياً ولا يكفي انتظار عودة الاتصال وحده.
    private val _cartError = MutableStateFlow<String?>(null)
    val cartError: StateFlow<String?> = _cartError
    private val _cartRetryTrigger = MutableStateFlow(0)

    val cartItems: StateFlow<List<CartItem>> = _cartRetryTrigger.flatMapLatest {
        repo.observeCart(onError = { e -> _cartError.value = e.message ?: "تعذّر تحميل السلة" })
            .onEach { _cartError.value = null }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun retryCart() {
        _cartError.value = null
        _cartRetryTrigger.value++
    }

    val cartCount: StateFlow<Int> = cartItems
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // تم إصلاح: استخدام Double مباشرة
    val cartTotal: StateFlow<Double> = cartItems
        .map { items ->
            items.sumOf { item ->
                item.price * item.quantity  // price هو Double في CartItem
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // ─── Favorites ──────────────────────────────────────────────
    // ✅ إصلاح: نفس نمط السلة أعلاه — خطأ لحظي لم يعد يُترجَم زوراً إلى
    // "لا توجد مفضّلة"، ومع trigger لإعادة فتح مستمع جديد فعلياً عند إعادة المحاولة.
    private val _favoritesError = MutableStateFlow<String?>(null)
    val favoritesError: StateFlow<String?> = _favoritesError
    private val _favoritesRetryTrigger = MutableStateFlow(0)

    val favoriteIds: StateFlow<Set<String>> = _favoritesRetryTrigger.flatMapLatest {
        repo.observeFavorites(onError = { e -> _favoritesError.value = e.message ?: "تعذّر تحميل المفضلة" })
            .onEach { _favoritesError.value = null }
    }.map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun retryFavorites() {
        _favoritesError.value = null
        _favoritesRetryTrigger.value++
    }

    // ✅ جديد: بيانات منتجات المفضلة الكاملة (اسم/سعر/صورة/مخزون) مجلوبة
    // مباشرة من مستند كل منتج — تماماً مثل getFavorites بالموقع، بدل
    // الاعتماد على allProducts (انظر شرح الإصلاح في FirestoreRepository
    // .getFavoriteProducts). تُحدَّث كلما تغيّرت مجموعة المفضّلة.
    private val _favoriteProducts = MutableStateFlow<List<Product>>(emptyList())
    val favoriteProducts: StateFlow<List<Product>> = _favoriteProducts

    private val _favoritesLoading = MutableStateFlow(false)
    val favoritesLoading: StateFlow<Boolean> = _favoritesLoading

    // ✅ إصلاح: getFavoriteProducts() صارت ترفع الاستثناء بدل ابتلاعه (انظر
    // FirestoreRepository) — لازم نمسكه هنا ونميّزه، بدل انهيار كامل للتطبيق
    // أو (كما كان سابقاً) إظهار "لا توجد مفضلة" وهمية عند أي خطأ شبكة.
    private val _favoriteProductsError = MutableStateFlow<String?>(null)
    val favoriteProductsError: StateFlow<String?> = _favoriteProductsError

    init {
        viewModelScope.launch {
            favoriteIds.collect { loadFavoriteProducts() }
        }
    }

    fun loadFavoriteProducts() {
        viewModelScope.launch {
            _favoritesLoading.value = true
            _favoriteProductsError.value = null
            try {
                _favoriteProducts.value = repo.getFavoriteProducts()
            } catch (e: Exception) {
                _favoriteProductsError.value = e.message ?: "تعذّر تحميل المفضلة"
            } finally {
                _favoritesLoading.value = false
            }
        }
    }

    // ─── Orders ─────────────────────────────────────────────────
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder

    // ─── Addresses ──────────────────────────────────────────────
    private val _addresses = MutableStateFlow<List<Address>>(emptyList())
    val addresses: StateFlow<List<Address>> = _addresses

    // ─── Notifications ──────────────────────────────────────────
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications

    // ✅ v2: عداد "غير المقروء" الحقيقي — منفصل تماماً عن _notifications
    // (المحدودة بـlimit(50) بـobserveNotifications)، ومصدره نفس الحقل الذي
    // يقرأه جرس الإشعارات بالموقع (users/{uid}.notifUnreadCount)، فيتطابق
    // الرقم المعروض بين المنصتين دائماً حتى لو تجاوز غير المقروء 50 عنصراً.
    // Eagerly (كـcartCount/favoriteIds أعلاه): يبدأ فور إنشاء الـViewModel
    // (أي فور تسجيل الدخول عملياً)، بغض النظر عن زيارة شاشة الإشعارات أم لا
    // — ليظهر رقم صحيح فوراً على أي badge مستقبلي (هيدر/تبويب سفلي).
    val unreadCount: StateFlow<Int> = repo.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ✅ إصلاح: كان أي خطأ بمستمع الإشعارات (مثال: فهرس Firestore مركّب غير
    // مُفعّل بعد لمجموعة notifications) يُكتب فقط بالـLog ولا يظهر للمستخدم
    // إطلاقاً — القائمة تبقى فارغة للأبد بدون أي تفسير أو طريقة لإعادة المحاولة.
    private val _notificationsLoading = MutableStateFlow(false)
    val notificationsLoading: StateFlow<Boolean> = _notificationsLoading

    private val _notificationsError = MutableStateFlow<String?>(null)
    val notificationsError: StateFlow<String?> = _notificationsError

    // ─── Loading / Error ────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ─── Init ───────────────────────────────────────────────────
    init { loadHomeData() }

    fun loadHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val settingsDeferred    = async { repo.getStoreSettings() }
                val bannersDeferred     = async { repo.getBanners() }
                val categoriesDeferred  = async { repo.getCategories() }
                val brandsDeferred      = async { repo.getBrands() }
                val featuredDeferred    = async { repo.getProducts(isFeatured   = true, limit = 10) }
                val newDeferred         = async { repo.getProducts(isNew        = true, limit = 10) }
                val bestDeferred        = async { repo.getProducts(isBestSeller = true, limit = 10) }
                val onSaleDeferred      = async { repo.getProducts(onSale       = true, limit = 10) }

                _storeSettings.value    = settingsDeferred.await()
                _banners.value          = bannersDeferred.await()
                _categories.value       = categoriesDeferred.await()
                _brands.value           = brandsDeferred.await()
                // إخفاء المنتجات منتهية الكمية من جميع أقسام الصفحة الرئيسية
                _featuredProducts.value = featuredDeferred.await().filter { it.stock > 0 }
                _newArrivals.value      = newDeferred.await().filter { it.stock > 0 }
                _bestSellers.value      = bestDeferred.await().filter { it.stock > 0 }
                _onSaleProducts.value   = onSaleDeferred.await().filter { it.stock > 0 }
            } catch (e: Exception) {
                // ✅ إصلاح: كانت e.message تُعرَض للمستخدم كما هي (نص استثناء
                // تقني خام، وأحياناً فارغة تماماً)، بلا أي تفريق بين انقطاع
                // الاتصال وأي فشل آخر — نفس الإصلاح المطبَّق بـ FirestoreRepository
                // .getProducts، هنا أيضاً حتى لا تظهر شاشة رئيسية فارغة برسالة
                // غامضة عند انقطاع الاتصال تحديداً.
                _error.value = repo.friendlyLoadError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadProducts(
        categoryId:   String?  = null,
        isFeatured:   Boolean? = null,
        isNew:        Boolean? = null,
        isBestSeller: Boolean? = null,
        onSale:       Boolean? = null,
        brandId:      String?  = null,
        searchQuery:  String?  = null,
    ) {
        // ✅ صفحة أولى جديدة (فلتر/بحث تغيّر) — نصفّر أي مؤشر ترقيم قديم
        // (Firestore cursor أو رقم صفحة Algolia) حتى لا تُخلط صفحات فلتر
        // سابق بفلتر جديد.
        val params = ProductsQueryParams(categoryId, isFeatured, isNew, isBestSeller, onSale, brandId, searchQuery)
        lastProductsQuery = params
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _hasMoreProducts.value = false
            try {
                // إخفاء المنتجات منتهية الكمية من قائمة المنتجات
                _allProducts.value = repo.getProducts(
                    categoryId   = categoryId,
                    isFeatured   = isFeatured,
                    isNew        = isNew,
                    isBestSeller = isBestSeller,
                    onSale       = onSale,
                    brandId      = brandId,
                    searchQuery  = searchQuery,
                ).filter { it.stock > 0 }
                _error.value = repo.lastProductsError
                _hasMoreProducts.value = repo.lastProductsHasMore
            } catch (e: Exception) {
                // ✅ إجراء دفاعي: repo.getProducts() محميّة داخلياً حالياً بالكامل
                // ولا ترفع استثناءً فعلياً، لكن أي تعديل مستقبلي لا يلتزم بنفس
                // الانضباط سيسبب انهياراً كاملاً غير متوقَّع بدل رسالة خطأ لطيفة.
                _error.value = e.message ?: "تعذّر تحميل المنتجات"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ جديد (Pagination/Infinite Scroll): يجلب الصفحة التالية بنفس فلاتر/
    // بحث آخر استدعاء لـloadProducts بالضبط، ويُلحقها بالقائمة الحالية —
    // بلا سبينر ملء الشاشة (isLoadingMoreProducts منفصلة عن isLoading).
    fun loadMoreProducts() {
        val params = lastProductsQuery ?: return
        if (!_hasMoreProducts.value || _isLoadingMoreProducts.value) return
        viewModelScope.launch {
            _isLoadingMoreProducts.value = true
            try {
                val nextPage = repo.getProducts(
                    categoryId   = params.categoryId,
                    isFeatured   = params.isFeatured,
                    isNew        = params.isNew,
                    isBestSeller = params.isBestSeller,
                    onSale       = params.onSale,
                    brandId      = params.brandId,
                    searchQuery  = params.searchQuery,
                    startAfter   = repo.lastProductsCursor,
                    algoliaPage  = repo.lastAlgoliaPage,
                ).filter { it.stock > 0 }
                _allProducts.value = _allProducts.value + nextPage
                _hasMoreProducts.value = repo.lastProductsHasMore
            } catch (e: Exception) {
                // فشل تحميل صفحة إضافية لا يجب أن يمسح القائمة المعروضة أصلاً —
                // فقط نوقف مؤشر "جاري التحميل"، والمستخدم يقدر يعيد المحاولة
                // بالتمرير مجدداً (hasMoreProducts تبقى كما كانت).
            } finally {
                _isLoadingMoreProducts.value = false
            }
        }
    }

    fun loadProduct(id: String) {
        viewModelScope.launch {
            _isProductLoading.value = true
            _selectedProduct.value = null
            try {
                _selectedProduct.value = repo.getProduct(id)
            } finally {
                _isProductLoading.value = false
            }
        }
    }

    fun addToCart(product: Product, quantity: Int = 1, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repo.addToCart(
                    CartItem(
                        productId = product.id,
                        name      = product.name,
                        price     = product.price.toDouble(),  // تحويل Long إلى Double
                        quantity  = quantity,
                        image     = product.mainImage,
                    )
                )
                onResult?.invoke(true, null)
            } catch (e: Exception) {
                onResult?.invoke(false, e.message ?: "تعذر الإضافة إلى السلة")
            }
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch { repo.removeFromCart(productId) }
    }

    fun updateCartQuantity(item: CartItem, delta: Int, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val newQty = item.quantity + delta
            try {
                if (newQty <= 0) {
                    repo.removeFromCart(item.productId)
                    onResult?.invoke(true, null)
                } else {
                    // ✅ نفس منطق الموقع: إذا تم تقييد الكمية بحد المخزون (capped) نعرض نفس رسالة الموقع
                    val capped = repo.updateQuantity(item.productId, newQty)
                    if (capped) onResult?.invoke(false, "لا يمكن تجاوز الكمية المتوفرة في المخزون")
                    else onResult?.invoke(true, null)
                }
            } catch (e: Exception) {
                onResult?.invoke(false, e.message ?: "تعذر تحديث الكمية")
            }
        }
    }

    fun clearCart() { viewModelScope.launch { repo.clearCart() } }

    fun toggleFavorite(productId: String) {
        viewModelScope.launch { repo.toggleFavorite(productId) }
    }

    // ✅ جديد (توحيد سلوك الإشعارات): يميّز "لا يزال يحمّل" عن "الطلب فعلاً
    // غير موجود/غير مصرَّح بالوصول له" — نفس تمييز isProductLoading أعلاه.
    // بدونها، OrderDetailScreen كانت تعامل selectedOrder==null دائماً على
    // أنها "جاري التحميل" (سبينر لا نهائي) حتى لو تأكد الفشل فعلياً.
    private val _isOrderLoading = MutableStateFlow(false)
    val isOrderLoading: StateFlow<Boolean> = _isOrderLoading

    // ✅ جديد (توحيد سلوك الإشعارات): رسالة تُعرض مرة واحدة في صفحة
    // الإشعارات عند التحويل إليها بسبب طلب غير موجود (بدل تركه صامتاً).
    // "تُستهلك" مرة واحدة فقط (نفس أسلوب pendingNotificationRoute بـ
    // MainActivity) حتى لا تتكرر عند أي إعادة تركيب لاحقة للشاشة.
    private val _orderNotFoundMessage = MutableStateFlow<String?>(null)
    val orderNotFoundMessage: StateFlow<String?> = _orderNotFoundMessage
    fun setOrderNotFoundMessage(message: String) { _orderNotFoundMessage.value = message }
    fun consumeOrderNotFoundMessage() { _orderNotFoundMessage.value = null }

    fun loadOrders() {
        viewModelScope.launch { _orders.value = repo.getOrders() }
    }

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _isOrderLoading.value = true
            _selectedOrder.value = null
            try {
                _selectedOrder.value = repo.getOrder(orderId)
            } finally {
                _isOrderLoading.value = false
            }
        }
    }

    // ─── Coupon ─────────────────────────────────────────────────
    // ✅ حالة مشتركة بين CartScreen و CheckoutScreen (نفس الـ ViewModel عبر شاشات التنقل)
    // المعاينة هنا فقط للعرض؛ التحقق النهائي والخصم الفعلي يتمّان داخل repo.placeOrder()
    // عبر transaction ذرّية على Firestore مباشرة، تماماً كما في السيرفر بالموقع.
    var appliedCouponCode by mutableStateOf<String?>(null)
        private set
    var appliedCouponDiscount by mutableDoubleStateOf(0.0)
        private set

    fun validateCoupon(code: String, subtotal: Double, onResult: (CouponResult) -> Unit) {
        viewModelScope.launch {
            val result = repo.validateCoupon(code, subtotal)
            if (result is CouponResult.Valid) {
                appliedCouponCode = result.coupon.code
                appliedCouponDiscount = result.discountAmount
            }
            onResult(result)
        }
    }

    fun clearCoupon() {
        appliedCouponCode = null
        appliedCouponDiscount = 0.0
    }

    fun syncFcmToken() {
        viewModelScope.launch { repo.syncFcmToken() }
    }

    // ✅ إصلاح ثغرة رابط الإيصال: يرفع صورة الإيصال فعلياً إلى Firebase Storage
    // ويُرجع رابط تنزيل حقيقي (بدل تخزين اسم/معرّف الملف المحلي فقط).
    suspend fun uploadPaymentReceipt(context: android.content.Context, uri: android.net.Uri): String =
        repo.uploadPaymentReceipt(context, uri)

    fun placeOrder(
        order: Order,
        clearCart: Boolean = true,  // ✅ false عند شراء الآن لحماية السلة
        useCoupon: Boolean = true,  // ✅ false عند شراء الآن — الكوبون يُطبَّق من شاشة السلة فقط
        onResult: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val id = repo.placeOrder(order, if (useCoupon) appliedCouponCode else null)
                if (clearCart) repo.clearCart()  // ✅ لا تمسح السلة عند شراء الآن
                if (useCoupon) clearCoupon()
                onResult(true, id)
            } catch (e: Exception) {
                onResult(false, e.message ?: "حدث خطأ")
            }
        }
    }

    fun loadAddresses() {
        viewModelScope.launch { _addresses.value = repo.getAddresses() }
    }

    fun addAddress(address: Address, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.addAddress(address)
            loadAddresses()
            onDone()
        }
    }

    fun updateAddress(id: String, address: Address, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.updateAddress(id, address)
            loadAddresses()
            onDone()
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch {
            repo.deleteAddress(id)
            loadAddresses()
        }
    }

    // ✅ إصلاح: كانت loadNotifications() تجلب القائمة مرّة واحدة فقط (get()) —
    // أي إشعار جديد يصل والمستخدم فاتح التطبيق لا يظهر لا بالقائمة ولا بعدّاد
    // "غير مقروء" بالهيدر إلا بعد إعادة تسجيل الدخول أو إعادة فتح شاشة الإشعارات.
    // الآن تفتح مستمعاً لحظياً (addSnapshotListener عبر observeNotifications) يبقى
    // شغّالاً طوال حياة الـViewModel ويحدّث القائمة والعدّاد فوراً عند وصول أي إشعار جديد.
    private var notificationsJob: kotlinx.coroutines.Job? = null

    fun loadNotifications() {
        notificationsJob?.cancel()
        _notificationsError.value = null
        _notificationsLoading.value = true
        notificationsJob = viewModelScope.launch {
            repo.observeNotifications(
                onError = { e ->
                    _notificationsLoading.value = false
                    _notificationsError.value = e.message ?: "تعذّر تحميل الإشعارات"
                }
            ).collect {
                _notificationsLoading.value = false
                _notificationsError.value = null
                _notifications.value = it
            }
        }
    }

    fun markNotificationRead(notifId: String) {
        val previous = _notifications.value
        _notifications.value = previous.map {
            if (it.id == notifId) it.copy(isRead = true) else it
        }
        viewModelScope.launch {
            try {
                repo.markNotificationRead(notifId)
            } catch (e: Exception) {
                // ✅ إصلاح: التحديث كان "متفائلاً" (optimistic) بلا أي تراجع عند
                // فشل الكتابة الفعلية على Firestore — تظل القائمة المحلية تُظهر
                // العنصر كمقروء للأبد رغم أن الخادم لا يزال يعتبره غير مقروء،
                // فيتعارض هذا مع عداد unreadCount الحقيقي (المُصلَح أعلاه).
                Log.w("MainViewModel", "فشل تحديث حالة القراءة، سيتم التراجع", e)
                _notifications.value = previous
            }
        }
    }

    fun markAllNotificationsRead() {
        val previous = _notifications.value
        _notifications.value = previous.map { it.copy(isRead = true) }
        viewModelScope.launch {
            try {
                repo.markAllNotificationsRead()
            } catch (e: Exception) {
                Log.w("MainViewModel", "فشل تحديد الكل كمقروء، سيتم التراجع", e)
                _notifications.value = previous
            }
        }
    }

    fun deleteNotification(notifId: String) {
        val previous = _notifications.value
        _notifications.value = previous.filterNot { it.id == notifId }
        viewModelScope.launch {
            try {
                repo.deleteNotification(notifId)
            } catch (e: Exception) {
                Log.w("MainViewModel", "فشل حذف الإشعار، سيتم التراجع", e)
                _notifications.value = previous
            }
        }
    }

    // ✅ جديد: حذف كل الإشعارات — مطابق لزر "حذف الكل" في نسخة الموقع.
    // ✅ إصلاح: لم يكن هناك أي تراجع عند فشل الحذف الفعلي على Firestore
    // (خلافاً لبقية دوال الإشعارات أعلاه التي تتبع كلها نفس نمط
    // optimistic update + rollback) — كانت القائمة تظهر فارغة للمستخدم
    // حتى لو فشلت العملية فعلياً (بلا اتصال مثلاً)، بينما الإشعارات لا تزال
    // موجودة على الخادم.
    fun deleteAllNotifications() {
        val previous = _notifications.value
        _notifications.value = emptyList()
        viewModelScope.launch {
            try {
                repo.deleteAllNotifications()
            } catch (e: Exception) {
                Log.w("MainViewModel", "فشل حذف كل الإشعارات، سيتم التراجع", e)
                _notifications.value = previous
            }
        }
    }

    fun clearError() { _error.value = null }
}