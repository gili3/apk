package com.eleven.store.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleven.store.data.model.*
import com.eleven.store.ui.components.ElevenButton
import com.eleven.store.ui.components.ElevenTopBar
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════
//  دالة مساعدة لتنسيق الأسعار
// ═══════════════════════════════════════════════════════════════
private fun formatPriceCheckout(value: Any?): String {
    val d = when (value) {
        is Double -> value
        is Long -> value.toDouble()
        is Int -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    val s = if (d == d.toLong().toDouble()) d.toLong().toString()
    else "%.2f".format(d)
    return "$s ج.س"
}

// ═══════════════════════════════════════════════════════════════
//  CHECKOUT SCREEN — نسخة طبق الأصل من Checkout.tsx
// ═══════════════════════════════════════════════════════════════

/**
 * شريط الخطوات — مطابق لـ StepBar في الموقع
 * w-9 h-9 rounded-full + label + خط بينهم
 */
@Composable
private fun CheckoutStepBar(current: Int) {
    val steps = listOf(
        Triple(1, "العنوان", Icons.Filled.LocationOn),
        Triple(2, "الدفع", Icons.Filled.CreditCard),
        Triple(3, "التأكيد", Icons.Filled.CheckCircle),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { index, (n, label, _) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // الدائرة
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (current >= n) Accent
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (current > n) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Text(
                            n.toString(),
                            color = if (current >= n) Color.White else MutedForeground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (current >= n) Accent else MutedForeground,
                )
            }
            // الخط الفاصل بين الخطوات
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .padding(top = 17.dp, start = 8.dp, end = 8.dp)
                        .width(64.dp)
                        .height(2.dp)
                        .background(
                            if (current > n) Accent else Border,
                            RoundedCornerShape(50),
                        ),
                )
            }
        }
    }
}

@Composable
fun CheckoutScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit,
    buyNowItems: List<CartItem> = emptyList(),
) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    // ✅ مصدر العناصر: buyNowItems أو السلة — مطابق للموقع
    val orderItems = if (buyNowItems.isNotEmpty()) buyNowItems else cartItems
    val orderTotal = if (buyNowItems.isNotEmpty())
        buyNowItems.sumOf { it.price * it.quantity }
    else cartTotal

    var step by remember { mutableStateOf(1) }
    var selectedAddress by remember { mutableStateOf<Address?>(null) }
    var receiptUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var agree by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val checkoutScope = rememberCoroutineScope()

    // ✅ الشحن مجاني فقط إذا كان الحد مفعّلاً (> 0) والمجموع يبلغه
    val shippingCost = if (storeSettings.freeShippingMinOrder > 0 &&
        orderTotal >= storeSettings.freeShippingMinOrder
    ) 0.0
    else (storeSettings.shippingCost.takeIf { it > 0 } ?: 30.0)
    // ✅ الكوبون يُطبَّق من شاشة السلة فقط، وليس عند "شراء الآن" (لا واجهة كوبون هناك)
    val isCartCheckout = buyNowItems.isEmpty()
    val discountAmount = if (isCartCheckout) viewModel.appliedCouponDiscount else 0.0
    val finalTotal = orderTotal - discountAmount + shippingCost

    val receiptPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) receiptUri = uri }

    LaunchedEffect(Unit) { viewModel.loadAddresses() }
    LaunchedEffect(addresses) {
        if (addresses.isNotEmpty() && selectedAddress == null) {
            selectedAddress = addresses.firstOrNull { it.isDefault } ?: addresses.first()
        }
    }

    Scaffold(
        topBar = { ElevenTopBar(title = "إتمام الطلب", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // العنوان الرئيسي — مطابق للموقع
            Text(
                "إتمام الطلب",
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Accent, RoundedCornerShape(50)),
            )
            Spacer(Modifier.height(24.dp))

            // البطاقة الرئيسية — مطابق لـ Card في الموقع
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            ) {
                Column {
                    // h-1 bg-accent
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Accent),
                    )
                    // StepBar
                    CheckoutStepBar(current = step)

                    // محتوى الخطوة — p-6 pt-2
                    Column(
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 8.dp,
                            bottom = 24.dp,
                        ),
                    ) {
                        when (step) {
                            1 -> AddressStepContent(
                                addresses = addresses,
                                selected = selectedAddress,
                                onSelect = { selectedAddress = it },
                                onNext = {
                                    if (selectedAddress != null) step = 2
                                    else checkoutScope.launch {
                                        snackbarHostState.showSnackbar("يرجى اختيار عنوان الشحن")
                                    }
                                },
                            )
                            2 -> PaymentStepContent(
                                storeSettings = storeSettings,
                                total = finalTotal,
                                receiptUri = receiptUri,
                                onPickReceipt = { receiptPicker.launch("image/*") },
                                onRemoveReceipt = { receiptUri = null },
                                onNext = {
                                    if (receiptUri != null) step = 3
                                    else checkoutScope.launch {
                                        snackbarHostState.showSnackbar("يرجى رفع إيصال الدفع")
                                    }
                                },
                                onBack = { step = 1 },
                            )
                            3 -> ConfirmationStepContent(
                                address = selectedAddress,
                                total = finalTotal,
                                items = orderItems,
                                agree = agree,
                                onAgreeChange = { agree = it },
                                isLoading = isLoading,
                                onBack = { step = 2 },
                                onPlace = {
                                    isLoading = true
                                    viewModel.placeOrder(
                                        order = Order(
                                            userId = user?.uid ?: "",
                                            items = orderItems,
                                            total = finalTotal,
                                            subtotal = orderTotal,
                                            shippingCost = shippingCost,
                                            status = OrderStatus.PENDING,
                                            shippingAddress = selectedAddress,
                                            paymentMethod = "bank_transfer",
                                            paymentReceipt = receiptUri?.lastPathSegment ?: "",
                                        ),
                                        clearCart = buyNowItems.isEmpty(),
                                        useCoupon = isCartCheckout,
                                    ) { ok, id ->
                                        isLoading = false
                                        if (ok) onOrderPlaced(id)
                                        else checkoutScope.launch {
                                            snackbarHostState.showSnackbar(id)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  STEP 1: ADDRESS — مطابق لـ AddressStep في الموقع
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AddressStepContent(
    addresses: List<Address>,
    selected: Address?,
    onSelect: (Address) -> Unit,
    onNext: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // العنوان + زر إضافة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "عنوان الشحن",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onBackground,
            )
            TextButton(onClick = { /* TODO: navigate to add address */ }) {
                Icon(
                    Icons.Filled.Add,
                    null,
                    tint = Accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "إضافة عنوان",
                    color = Accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }

        if (addresses.isEmpty()) {
            // بطاقة تحذيرية
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Warning.copy(alpha = 0.1f),
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Warning.copy(alpha = 0.4f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        null,
                        tint = Warning,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "لا توجد عناوين مسجّلة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Warning,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "يرجى إضافة عنوان للمتابعة",
                        fontSize = 14.sp,
                        color = Warning.copy(alpha = 0.8f),
                    )
                }
            }
        } else {
            // قائمة العناوين
            addresses.forEach { addr ->
                val isSelected = selected?.id == addr.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(addr) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Accent.copy(alpha = 0.05f)
                        else MaterialTheme.colorScheme.surface,
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Accent else Border,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Radio circle
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(
                                    2.dp,
                                    if (isSelected) Accent else Border,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .background(Accent, CircleShape),
                                )
                            }
                        }
                        Column {
                            Text(
                                addr.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                "${addr.city} — ${addr.address}",
                                fontSize = 14.sp,
                                color = MutedForeground,
                            )
                            Text(
                                addr.phone,
                                fontSize = 14.sp,
                                color = MutedForeground,
                            )
                        }
                    }
                }
            }
        }

        // زر المتابعة
        ElevenButton(
            text = "المتابعة للدفع",
            onClick = onNext,
            enabled = selected != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  STEP 2: PAYMENT — مطابق لـ PaymentStep في الموقع
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PaymentStepContent(
    storeSettings: StoreSettings,
    total: Double,
    receiptUri: android.net.Uri?,
    onPickReceipt: () -> Unit,
    onRemoveReceipt: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            "الدفع عبر التحويل البنكي",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // بيانات الحساب البنكي
        Card(
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "بيانات الحساب البنكي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Accent,
                )
                Spacer(Modifier.height(12.dp))

                listOf(
                    "البنك" to storeSettings.bankName.ifBlank { "البنك الأهلي" },
                    "رقم الحساب" to storeSettings.bankAccountNumber.ifBlank { "SA1234567890" },
                    "اسم المستفيد" to storeSettings.bankAccountName.ifBlank { "Eleven Store" },
                ).forEach { (label, value) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically,
                    ) {
                        Text(label, fontSize = 14.sp, color = MutedForeground)
                        Text(
                            value,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    HorizontalDivider(color = Border)
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically,
                ) {
                    Text(
                        "المبلغ المطلوب",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        formatPriceCheckout(total),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Accent,
                    )
                }
            }
        }

        // رفع الإيصال
        Column {
            Text(
                "ارفق إيصال التحويل",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 2.dp,
                        color = if (receiptUri != null) Accent else Border,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .background(
                        if (receiptUri != null) Accent.copy(alpha = 0.05f)
                        else Color.Transparent,
                    )
                    .clickable { onPickReceipt() },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        if (receiptUri != null) Icons.Filled.CheckCircle
                        else Icons.Filled.Upload,
                        null,
                        tint = if (receiptUri != null) Accent else MutedForeground,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = receiptUri?.lastPathSegment ?: "اختيار صورة الإيصال",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (receiptUri != null) Accent else MutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (receiptUri != null) {
                TextButton(onClick = onRemoveReceipt) {
                    Icon(
                        Icons.Filled.Close,
                        null,
                        tint = Destructive,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "إزالة الملف",
                        fontSize = 12.sp,
                        color = Destructive,
                    )
                }
            }
        }

        // أزرار
        ElevenButton(
            text = "مراجعة الطلب",
            onClick = onNext,
            enabled = receiptUri != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "العودة لاختيار العنوان",
                color = MutedForeground,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  STEP 3: CONFIRMATION — مطابق لـ ConfirmationStep في الموقع
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ConfirmationStepContent(
    address: Address?,
    total: Double,
    items: List<CartItem>,
    agree: Boolean,
    onAgreeChange: (Boolean) -> Unit,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPlace: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            "تأكيد الطلب",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // عنوان الشحن + الإجمالي
        Card(
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "عنوان الشحن",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Accent,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    address?.displayName ?: "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "${address?.city ?: ""} — ${address?.address ?: ""}",
                    fontSize = 14.sp,
                    color = MutedForeground,
                )
                Text(
                    address?.phone ?: "",
                    fontSize = 14.sp,
                    color = MutedForeground,
                )

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically,
                ) {
                    Text(
                        "الإجمالي النهائي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        formatPriceCheckout(total),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Accent,
                    )
                }
            }
        }

        // المنتجات
        Card(
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "المنتجات (${items.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Accent,
                )
                Spacer(Modifier.height(12.dp))
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            model = item.image,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Border, RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "× ${item.quantity}",
                                fontSize = 12.sp,
                                color = MutedForeground,
                            )
                        }
                        Text(
                            "${formatPriceCheckout(item.price * item.quantity)} ج.س",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent,
                        )
                    }
                    if (index < items.lastIndex) {
                        HorizontalDivider(color = Border)
                    }
                }
            }
        }

        // الموافقة على الشروط
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    RoundedCornerShape(12.dp),
                )
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .clickable { onAgreeChange(!agree) }
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(
                checked = agree,
                onCheckedChange = { onAgreeChange(it) },
                colors = CheckboxDefaults.colors(checkedColor = Accent),
            )
            Text(
                "أوافق على الشروط والأحكام وسياسة الخصوصية",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        // أزرار
        ElevenButton(
            text = if (isLoading) "جاري إتمام الطلب..." else "إتمام الطلب الآن",
            onClick = onPlace,
            enabled = agree && !isLoading,
            isLoading = isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        )
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "العودة لتعديل الدفع",
                color = MutedForeground,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}