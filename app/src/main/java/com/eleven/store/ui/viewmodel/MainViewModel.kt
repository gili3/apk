package com.eleven.store.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleven.store.data.model.*
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

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try { repo.loginWithEmail(email, password); repo.syncFcmToken(); onResult(true, null) }
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

    fun register(name: String, email: String, phone: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try { repo.registerWithEmail(name, email, phone, password); repo.syncFcmToken(); onResult(true, null) }
            catch (e: Exception) { onResult(false, mapAuthError(e, "فشل إنشاء الحساب، يرجى المحاولة مرة أخرى")) }
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

    fun logout() = repo.logout()

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

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct

    // يميّز بين "لا يزال يحمّل" و"لم يتم العثور على المنتج" — مطابق لتمييز
    // isLoading / error || !product في ProductDetail.tsx بالموقع
    private val _isProductLoading = MutableStateFlow(false)
    val isProductLoading: StateFlow<Boolean> = _isProductLoading

    // ─── Cart ───────────────────────────────────────────────────
    val cartItems: StateFlow<List<CartItem>> = repo.observeCart()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
    val favoriteIds: StateFlow<Set<String>> = repo.observeFavorites()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

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
                _error.value = e.message
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
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
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
            } finally {
                _isLoading.value = false
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

    fun loadOrders() {
        viewModelScope.launch { _orders.value = repo.getOrders() }
    }

    fun loadOrder(orderId: String) {
        viewModelScope.launch { _selectedOrder.value = repo.getOrder(orderId) }
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

    fun loadNotifications() {
        viewModelScope.launch { _notifications.value = repo.getNotifications() }
    }

    fun markNotificationRead(notifId: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == notifId) it.copy(isRead = true) else it
        }
        viewModelScope.launch {
            try { repo.markNotificationRead(notifId) } catch (_: Exception) { }
        }
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        viewModelScope.launch {
            try { repo.markAllNotificationsRead() } catch (_: Exception) { }
        }
    }

    fun deleteNotification(notifId: String) {
        _notifications.value = _notifications.value.filterNot { it.id == notifId }
        viewModelScope.launch {
            try { repo.deleteNotification(notifId) } catch (_: Exception) { }
        }
    }

    fun clearError() { _error.value = null }
}