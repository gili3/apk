package com.eleven.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleven.store.data.model.CartItem
import com.eleven.store.data.model.CouponResult
import com.eleven.store.ui.components.ElevenButton
import com.eleven.store.ui.components.ElevenTopBar
import com.eleven.store.ui.theme.Accent
import com.eleven.store.ui.theme.Border
import com.eleven.store.ui.theme.Destructive
import com.eleven.store.ui.theme.MutedForeground
import com.eleven.store.ui.theme.Primary
import com.eleven.store.ui.theme.Success
import com.eleven.store.ui.theme.Warning
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ملاحظة: formatPrice مُعرّفة بشكل مشترك في ScreenCommon.kt

// دالة مساعدة لتنسيق الأرقام بدون عملة
private fun formatNumber(value: Any?): String {
    val number = when (value) {
        is String -> value.toDoubleOrNull() ?: 0.0
        is Number -> value.toDouble()
        else -> 0.0
    }
    return if (number == number.toLong().toDouble()) {
        number.toLong().toString()
    } else {
        "%.2f".format(number)
    }
}

// ═══════════════════════════════════════════════════════════════
//  الكوبونات لم تعد أكواداً ثابتة بالكود — تُقرأ وتُتحقق من مجموعة
//  "coupons" بقاعدة البيانات (تُدار من لوحة تحكم الموقع)، عبر
//  viewModel.validateCoupon(). راجع FirestoreRepository.evaluateCoupon().
// ═══════════════════════════════════════════════════════════════

@Composable
fun CartScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onProductClick: (String) -> Unit,
) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ── حالة الكوبون — تُقرأ من الـViewModel المشترك (تصل معه إلى CheckoutScreen) ──
    var couponCode by remember { mutableStateOf("") }
    var couponApplying by remember { mutableStateOf(false) }
    val couponApplied = viewModel.appliedCouponCode != null

    // الشحن يُحسب دائماً من إعدادات Firestore فقط
    val shippingCost = if (cartTotal >= storeSettings.freeShippingMinOrder && storeSettings.freeShippingMinOrder > 0)
        0.0 else storeSettings.shippingCost

    val discountAmount = viewModel.appliedCouponDiscount
    val grandTotal = cartTotal - discountAmount + shippingCost

    fun applyCoupon() {
        val code = couponCode.trim().uppercase()
        if (code.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("يرجى إدخال كود الخصم") }
            return
        }
        couponApplying = true
        viewModel.validateCoupon(code, cartTotal) { result ->
            couponApplying = false
            when (result) {
                is CouponResult.Valid -> {
                    couponCode = code
                    scope.launch { snackbarHostState.showSnackbar("تم تطبيق كود الخصم") }
                }
                is CouponResult.Invalid -> {
                    scope.launch { snackbarHostState.showSnackbar(result.message) }
                }
            }
        }
    }

    fun removeCoupon() {
        viewModel.clearCoupon()
        couponCode = ""
    }

    Scaffold(
        topBar = {
            ElevenTopBar(
                title = "سلة التسوق (${cartItems.size})",
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                    ) {
                        // ── حقل كود الخصم — مطابق لـ OrderSummary في الموقع ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = couponCode,
                                onValueChange = {
                                    if (!couponApplied) couponCode = it.uppercase()
                                },
                                placeholder = {
                                    Text(
                                        "كود الخصم",
                                        color = MutedForeground,
                                        fontSize = 14.sp,
                                    )
                                },
                                singleLine = true,
                                enabled = !couponApplied,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Accent,
                                    unfocusedBorderColor = Border,
                                    disabledBorderColor = Border,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                ),
                            )
                            if (couponApplied) {
                                OutlinedButton(
                                    onClick = { removeCoupon() },
                                    shape = RoundedCornerShape(10.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            Success
                                        )
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Success,
                                    ),
                                    modifier = Modifier.height(50.dp),
                                ) {
                                    Text(
                                        "إزالة",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { applyCoupon() },
                                    enabled = !couponApplying,
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(50.dp),
                                ) {
                                    if (couponApplying) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White,
                                        )
                                    } else {
                                        Text(
                                            "تطبيق",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }

                        // ── رسالة نجاح تطبيق الكوبون — مطابقة للموقع ──
                        if (couponApplied) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Success.copy(alpha = 0.1f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "✓ تم تطبيق الخصم",
                                    color = Success,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                TextButton(
                                    onClick = { removeCoupon() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                ) {
                                    Text(
                                        "إزالة",
                                        color = Success,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── ملخص الطلب — مطابق لـ OrderSummary في الموقع ──
                        // المجموع الفرعي
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "المجموع الفرعي",
                                color = MutedForeground,
                                fontSize = 14.sp,
                            )
                            Text(
                                "${formatNumber(cartTotal)} ج.س",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        // الخصم (إذا وجد)
                        if (couponApplied && discountAmount > 0.0) {
                            HorizontalDivider(
                                color = Border,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "الخصم",
                                    color = MutedForeground,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    "-${formatNumber(discountAmount)} ج.س",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Success,
                                )
                            }
                        }

                        // الشحن
                        HorizontalDivider(
                            color = Border,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "الشحن",
                                color = MutedForeground,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = if (shippingCost == 0.0) "مجاني" else "${formatNumber(shippingCost)} ج.س",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (shippingCost == 0.0) Success else MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        // الإجمالي
                        HorizontalDivider(
                            color = Border,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "الإجمالي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                "${formatNumber(grandTotal)} ج.س",
                                fontWeight = FontWeight.ExtraBold,
                                color = Accent,
                                fontSize = 24.sp,
                            )
                        }

                        // رسالة الشحن المجاني
                        if (storeSettings.freeShippingMinOrder > 0) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "الشحن مجاني للطلبات فوق ${formatNumber(storeSettings.freeShippingMinOrder)} ج.س",
                                color = MutedForeground,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // زر متابعة الدفع
                        ElevenButton(
                            text = "المتابعة للدفع",
                            onClick = onCheckout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            // ── حالة السلة الفارغة — مطابقة للموقع ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    // أيقونة السلة الفارغة
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(48.dp),
                            )
                            .border(1.dp, Border, RoundedCornerShape(48.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MutedForeground,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "سلتك فارغة",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "لم تضف أي منتجات بعد. اكتشف مجموعتنا الآن!",
                        color = MutedForeground,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                    ElevenButton(
                        text = "تسوق الآن",
                        onClick = { onProductClick("") },
                        modifier = Modifier
                            .height(48.dp)
                            .padding(horizontal = 32.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp + padding.calculateTopPadding(),
                    bottom = 16.dp + padding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // عدد المنتجات
                item {
                    Text(
                        "${cartItems.size} منتج",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedForeground,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                items(cartItems, key = { it.productId }) { item ->
                    CartItemRow(
                        item = item,
                        atMaxStock = item.stock > 0 && item.quantity >= item.stock,
                        onIncrease = {
                            viewModel.updateCartQuantity(item, 1) { ok, msg ->
                                if (!ok) scope.launch {
                                    snackbarHostState.showSnackbar(
                                        msg ?: "لا يمكن تجاوز الكمية المتوفرة في المخزون"
                                    )
                                }
                            }
                        },
                        onDecrease = {
                            viewModel.updateCartQuantity(item, -1)
                        },
                        onRemove = { viewModel.removeFromCart(item.productId) },
                        onProductClick = { onProductClick(item.productId) },
                    )
                }

                // رابط متابعة التسوق
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clickable { onProductClick("") }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "متابعة التسوق",
                            color = Accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    atMaxStock: Boolean = false,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onProductClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // p-3 في الموقع
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── صورة المنتج — مطابقة للموقع: w-20 h-20 rounded-xl ──
            Box(
                modifier = Modifier
                    .size(80.dp) // w-20 h-20 = 80dp
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onProductClick),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = item.image,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // ── معلومات المنتج — flex-1 ──
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${formatNumber(item.price)} ج.س",
                    color = Accent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )

                Spacer(Modifier.height(6.dp))

                // ── أزرار التحكم بالكمية — مطابقة للموقع ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 2.dp),
                ) {
                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Filled.Remove,
                            contentDescription = "تقليل الكمية",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        text = item.quantity.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.widthIn(min = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    IconButton(
                        onClick = onIncrease,
                        enabled = !atMaxStock,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "زيادة الكمية",
                            modifier = Modifier.size(12.dp),
                            tint = if (atMaxStock)
                                MutedForeground.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                // رسالة الحد الأقصى
                if (atMaxStock) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "وصلت للحد الأقصى المتوفر بالمخزون",
                        color = Warning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // ── زر الحذف — مطابق للموقع: p-2 rounded-full hover:bg-red-50 ──
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Destructive.copy(alpha = 0.05f),
                        RoundedCornerShape(999.dp),
                    ),
            ) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "حذف",
                    tint = Destructive,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}