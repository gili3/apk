package com.eleven.store.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

// ─── Product ───────────────────────────────────────────────────
// ⚠️ @IgnoreExtraProperties ضروري: مستند المنتج فيه حقول زائدة غير
// موجودة بهذا الكلاس (basePrice, discountType, discountValue, createdAt,
// updatedAt). بدون هذا الأنوتيشن، Firestore يرمي Exception عند toObject()
// وهذا كان سبب فشل جلب المنتجات بالكامل (مع أو بدون فلتر).
@IgnoreExtraProperties
data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val originalPrice: Double? = null,
    val images: List<String> = emptyList(),
    val image: String = "",
    val categoryId: String = "",
    val brandId: String = "",
    val stock: Int = 0,
    val isFeatured: Boolean = false,
    val isNew: Boolean = false,
    val isBestSeller: Boolean = false,
    val isOnSale: Boolean = false,     // الحقل الحقيقي في Firestore
    // ⚠️ تم حذف حقل onSale المكرر — كان يتعارض مع isOnSale على مستوى
    // getters في جافا (isOnSale() مقابل getOnSale()) ويسبب
    // RuntimeException: Found conflicting getters for name isOnSale
    // عند كل عملية toObject(Product::class.java)، وهذا كان السبب
    // الحقيقي لفشل جلب كل المنتجات بدون استثناء.
    val tags: List<String> = emptyList(),
) {
    val mainImage: String get() = images.firstOrNull()?.takeIf { it.isNotBlank() } ?: image
    val discountPercent: Int?
        get() = if (originalPrice != null && originalPrice > price)
            ((1 - price / originalPrice) * 100).toInt() else null
}

// ─── Category ──────────────────────────────────────────────────
@IgnoreExtraProperties
data class Category(
    val id: String = "",
    val name: String = "",
    val image: String = "",
    val order: Int = 0,
    val isActive: Boolean = true,
)

// ─── Brand ─────────────────────────────────────────────────────
@IgnoreExtraProperties
data class Brand(
    val id: String = "",
    val name: String = "",
    val logo: String = "",
    val link: String = "",
    val isActive: Boolean = true,
)

// ─── Banner ────────────────────────────────────────────────────
// حقول مطابقة 100% لمجموعة "banners" في Firestore
@IgnoreExtraProperties
data class Banner(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val image: String = "",
    val cta: String = "كشف الجديد",
    val link: String = "",
    val order: Int = 0,
    val isActive: Boolean = true,   // ← مطلوب لفلتر Firestore
)

// ─── Cart ──────────────────────────────────────────────────────
@IgnoreExtraProperties
data class CartItem(
    val id: String = "",
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val image: String = "",
    // ✅ يُملأ من بيانات المنتج الحية عند القراءة (نفس منطق getCart بالموقع)، وليس مخزّناً في مستند السلة نفسه
    val stock: Long = 0L,
) {
    val total: Double get() = price * quantity
}

// ─── Order ─────────────────────────────────────────────────────
// حقول مطابقة 100% لمجموعة "orders" في Firestore (نفس الحقول التي يستخدمها الموقع)
@IgnoreExtraProperties
data class Order(
    val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0,
    val subtotal: Double = 0.0,
    val shippingCost: Double = 0.0,
    val discount: Double = 0.0,
    val couponCode: String? = null,
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentStatus: String = "unpaid",
    val shippingAddress: Address? = null,   // ← نفس اسم الحقل في الموقع (shippingAddress)، بدل address
    val paymentMethod: String = "",
    val paymentReceipt: String = "",
    val verificationToken: String = "",     // ← يُستخدم لبناء رابط التحقق وQR في الفاتورة
    val notes: String = "",
    val createdAt: Timestamp? = null,
)

enum class OrderStatus(val label: String) {
    PENDING("قيد الانتظار"),
    PAID("تم الدفع"),
    SHIPPED("خرج للتوصيل"),
    DELIVERED("تم التسليم"),
    CANCELLED("ملغى");

    companion object {
        fun from(value: String): OrderStatus =
            entries.find { it.name.lowercase() == value.lowercase() } ?: PENDING
    }
}

// ─── Address ───────────────────────────────────────────────────
@IgnoreExtraProperties
data class Address(
    val id: String = "",
    val fullName: String = "",
    val name: String = "",      // بعض السجلات القديمة تستخدم name بدل fullName (نفس منطق الموقع: fullName || name)
    val phone: String = "",
    val city: String = "",
    val address: String = "",
    val isDefault: Boolean = false,
) {
    val displayName: String get() = fullName.ifBlank { name }
}

// ─── Store Settings ────────────────────────────────────────────
// حقول مطابقة لمجموعة settings/store في Firestore (نفس ما يقرأه الموقع)
@IgnoreExtraProperties
data class StoreSettings(
    val storeName: String = "Eleven Store",
    val logo: String = "",
    val address: String = "",                 // عنوان المتجر (يظهر في رأس الفاتورة)
    val phone: String = "",
    val email: String = "",
    val websiteUrl: String = "",              // رابط الموقع (لبناء رابط verify-order/{token} في رمز QR)
    val currency: String = "ج.س",
    val currencySymbol: String = "ج.س",
    val shippingCost: Double = 0.0,
    val freeShippingThreshold: Double = 0.0,  // ← نفس اسم الحقل في السيرفر
    val bankName: String = "",
    val bankAccountName: String = "",
    val bankAccountNumber: String = "",
    val whatsapp: String = "",
    val instagram: String = "",
    val facebook: String = "",
    val twitter: String = "",
    val aboutText: String = "",
    val storeDescription: String = "",
    val storeVision: String = "",
    val storeMission: String = "",
    val storeAboutImage: String = "",
) {
    // اسم متوافق مع الكود القديم في الشاشات (checkout كان يستخدم freeShippingMinOrder)
    val freeShippingMinOrder: Double get() = freeShippingThreshold
}

// ─── Notification ──────────────────────────────────────────────
@IgnoreExtraProperties
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null,
) {
    /** اسم بديل لمحتوى الإشعار (نفس body) */
    val message: String get() = body

    /** نص نسبي لوقت الإشعار، مثل "منذ 5 دقائق" */
    val timeAgo: String get() {
        val created = createdAt?.toDate()?.time ?: return ""
        val diffMs = System.currentTimeMillis() - created
        val minutes = diffMs / 60_000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "الآن"
            minutes < 60 -> "منذ $minutes دقيقة"
            hours < 24 -> "منذ $hours ساعة"
            days < 30 -> "منذ $days يوم"
            else -> "منذ ${days / 30} شهر"
        }
    }
}

// ─── Coupon ────────────────────────────────────────────────────
// مطابق لمجموعة "coupons" بـ Firestore، وتُدار من لوحة تحكم الموقع فقط (لا تُكتب من التطبيق)
@IgnoreExtraProperties
data class Coupon(
    val code: String = "",
    val discountType: String = "percentage", // "percentage" | "fixed"
    val discountValue: Double = 0.0,
    val isActive: Boolean = true,
    val minOrderAmount: Double = 0.0,
    val usageLimit: Long = 0L, // 0 = غير محدود
    val usageCount: Long = 0L,
    val expiresAt: Timestamp? = null,
)

/** نتيجة التحقق من كوبون قبل إتمام الطلب — تُستخدم للعرض فقط، التحقق النهائي يتم عند إنشاء الطلب. */
sealed class CouponResult {
    data class Valid(val discountAmount: Double, val coupon: Coupon) : CouponResult()
    data class Invalid(val message: String) : CouponResult()
}

