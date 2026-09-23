package com.eleven.store.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleven.store.data.model.*
import com.eleven.store.ui.components.ElevenButton
import com.eleven.store.ui.components.ElevenTopBar
import com.eleven.store.ui.components.OrderStatusBadge
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel

// ═══════════════════════════════════════════════════════════════
//  دوال مساعدة مشتركة
// ═══════════════════════════════════════════════════════════════
// ملاحظة: تنسيق السعر موحّد عبر formatPrice() في ScreenCommon.kt

/** تسمية طريقة الدفع */
private fun paymentMethodLabel(method: String): String = when (method) {
    "bank_transfer" -> "تحويل بنكي"
    "cash"          -> "دفع عند الاستلام"
    "card"          -> "بطاقة ائتمانية"
    else            -> method.ifBlank { "-" }
}

/** تنسيق التاريخ العربي */
private fun formatArabicDate(date: java.util.Date): String {
    val months = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    val cal = java.util.Calendar.getInstance().apply { time = date }
    return "${cal.get(java.util.Calendar.DAY_OF_MONTH)} ${months[cal.get(java.util.Calendar.MONTH)]} ${cal.get(java.util.Calendar.YEAR)}"
}

/** ✅ إجمالي واحد موحّد للقائمة والتفاصيل (كانا يحسبانه بطريقتين مختلفتين). */
private fun orderTotal(order: Order): Double {
    if (order.total > 0) return order.total
    val subtotal = order.items.sumOf { it.price * it.quantity }
    return (subtotal - order.discount + order.shippingCost).coerceAtLeast(0.0)
}

// ═══════════════════════════════════════════════════════════════
//  ORDERS SCREEN — تصميم «البسيط»: صفوف بخطوط فاصلة بلا بطاقات
// ═══════════════════════════════════════════════════════════════

@Composable
fun OrdersScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    onStartShopping: () -> Unit = onBack,
    onNavigateToLogin: () -> Unit = onBack,
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    // ✅ إصلاح: كانت الشاشة تستخدم isLoading الخاص بالمنتجات، فتظهر "لا توجد
    // طلبات" قبل انتهاء تحميل الطلبات، وسبينر كلما تحمّلت المنتجات.
    val ordersLoaded by viewModel.ordersLoaded.collectAsStateWithLifecycle()

    LaunchedEffect(user) { if (user != null) viewModel.loadOrders() }

    val onSurface = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val line = MaterialTheme.colorScheme.outline

    // ✅ إصلاح: getOrders() تُرجع قائمة فارغة بصمت لزائر غير مسجّل دخول،
    // فكانت الشاشة تعرض "لا توجد طلبات بعد" المُضلِّلة.
    if (user == null) {
        Scaffold(topBar = { ElevenTopBar(title = "طلباتي", onBack = onBack) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        Icons.Filled.ShoppingBag,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = muted,
                    )
                    Text(
                        "يرجى تسجيل الدخول لعرض طلباتك",
                        color = muted,
                        fontSize = 16.sp,
                    )
                    ElevenButton(
                        text = "تسجيل الدخول",
                        onClick = onNavigateToLogin,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }
        }
        return
    }

    Scaffold(topBar = { ElevenTopBar(title = "طلباتي", onBack = onBack) }) { padding ->
        when {
            // ── تحميل ──
            !ordersLoaded && orders.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = onSurface, strokeWidth = 3.dp)
                }
            }

            // ── فارغة ──
            orders.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.ShoppingBag,
                            null,
                            modifier = Modifier.size(56.dp),
                            tint = muted,
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "لا توجد طلبات بعد",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "ابدأ التسوق الآن وستظهر طلباتك هنا",
                            color = muted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))
                        ElevenButton(
                            text = "ابدأ التسوق",
                            onClick = onStartShopping,
                            icon = Icons.Filled.ShoppingBag,
                            modifier = Modifier.fillMaxWidth(0.6f),
                        )
                    }
                }
            }

            // ── قائمة الطلبات ──
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp,
                        top = 4.dp + padding.calculateTopPadding(),
                        bottom = 24.dp + padding.calculateBottomPadding(),
                    ),
                ) {
                    items(orders, key = { it.id }) { order ->
                        val date = order.createdAt?.toDate()?.let { formatArabicDate(it) }
                        val itemCount = order.items.size
                        val total = orderTotal(order)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOrderClick(order.id) }
                                    .heightIn(min = 72.dp)
                                    .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            "طلب #${order.orderNumber}",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = onSurface,
                                        )
                                        OrderStatusBadge(order.status)
                                    }
                                    val meta = listOfNotNull(
                                        date,
                                        if (itemCount > 0) "عدد المنتجات: $itemCount" else null,
                                    ).joinToString(" · ")
                                    if (meta.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(meta, color = muted, fontSize = 12.sp)
                                    }
                                }
                                if (total > 0) {
                                    Text(
                                        formatPrice(total),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = onSurface,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Icon(
                                    Icons.Filled.ChevronLeft,
                                    contentDescription = null,
                                    tint = muted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            HorizontalDivider(color = line)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  ORDER DETAIL SCREEN — بلا فاتورة: الحالة، المنتجات، الملخص، التوصيل، الدفع
// ═══════════════════════════════════════════════════════════════

@Composable
fun OrderDetailScreen(
    viewModel: MainViewModel,
    orderId: String,
    onBack: () -> Unit,
    // ✅ توحيد سلوك الإشعارات: يُستدعى عند تأكّد أن الطلب غير موجود (بعد
    // انتهاء التحميل فعلياً لا أثناءه) — المستدعي (NavGraph) يحوّل لصفحة
    // الإشعارات مع رسالة مناسبة بدل ترك المستخدم على سبينر لا نهائي.
    onOrderNotFound: () -> Unit = {},
) {
    val order by viewModel.selectedOrder.collectAsStateWithLifecycle()
    val isOrderLoading by viewModel.isOrderLoading.collectAsStateWithLifecycle()

    // ✅ نفرّق بين "لم يبدأ التحميل بعد" و"انتهى التحميل بلا نتيجة" — بدون
    // هذا التمييز، isOrderLoading==false في أول تركيب (قبل انطلاق
    // LaunchedEffect(orderId) أدناه) كانت ستُفسَّر خطأً كـ"غير موجود" فوراً.
    var loadAttempted by remember(orderId) { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        loadAttempted = false
        viewModel.loadOrder(orderId)
    }
    LaunchedEffect(isOrderLoading) {
        if (isOrderLoading) loadAttempted = true
    }

    val confirmedNotFound = loadAttempted && !isOrderLoading && order == null
    LaunchedEffect(confirmedNotFound) {
        if (confirmedNotFound) {
            viewModel.setOrderNotFoundMessage("تعذّر العثور على هذا الطلب — قد يكون محذوفاً أو غير متاح")
            onOrderNotFound()
        }
    }

    val onSurface = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val line = MaterialTheme.colorScheme.outline

    Scaffold(topBar = { ElevenTopBar(title = "تفاصيل الطلب", onBack = onBack) }) { padding ->
        val o = order
        if (o == null) {
            // ── تحميل (أو تأكّد عدم الوجود وجارٍ التحويل لصفحة الإشعارات) ──
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = onSurface)
            }
        } else {
            val subtotal = if (o.subtotal > 0) o.subtotal else o.items.sumOf { it.price * it.quantity }
            val total = orderTotal(o)
            val date = o.createdAt?.toDate()?.let { formatArabicDate(it) }
            val address = o.shippingAddress

            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp,
                    top = 8.dp + padding.calculateTopPadding(),
                    bottom = 32.dp + padding.calculateBottomPadding(),
                ),
            ) {
                // ── الرأس ──
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "طلب #${o.orderNumber}",
                                fontFamily = ElevenSerifFontFamily,
                                fontSize = 26.sp,
                                color = onSurface,
                            )
                            OrderStatusBadge(o.status)
                        }
                        if (date != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(date, color = muted, fontSize = 13.sp)
                        }
                    }
                }

                // ── المنتجات ──
                item { OrderSectionHeader("المنتجات") }
                items(o.items) { line2 ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                if (line2.image.isNotBlank()) {
                                    AsyncImage(
                                        model = line2.image,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    line2.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${line2.quantity} × ${formatPrice(line2.price)}",
                                    fontSize = 12.sp,
                                    color = muted,
                                )
                            }
                            Text(
                                formatPrice(line2.price * line2.quantity),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = onSurface,
                            )
                        }
                        HorizontalDivider(color = line)
                    }
                }

                // ── الملخص ──
                item { OrderSectionHeader("الملخص") }
                item { OrderValueRow("المجموع الفرعي", formatPrice(subtotal)) }
                if (o.discount > 0) {
                    item {
                        OrderValueRow(
                            label = if (o.couponCode.isNullOrBlank()) "الخصم" else "الخصم (${o.couponCode})",
                            value = "-${formatPrice(o.discount)}",
                            valueColor = Success,
                        )
                    }
                }
                item {
                    OrderValueRow(
                        "الشحن",
                        if (o.shippingCost > 0) formatPrice(o.shippingCost) else "مجاني",
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("الإجمالي", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = onSurface)
                        Text(formatPrice(total), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onSurface)
                    }
                }

                // ── التوصيل ──
                if (address != null) {
                    item { OrderSectionHeader("التوصيل") }
                    item { OrderValueRow("المستلم", address.displayName.ifBlank { "-" }) }
                    item { OrderValueRow("الهاتف", address.phone.ifBlank { "-" }) }
                    item {
                        OrderValueRow(
                            "العنوان",
                            listOf(address.city, address.address).filter { it.isNotBlank() }
                                .joinToString("، ").ifBlank { "-" },
                        )
                    }
                }

                // ── الدفع ──
                item { OrderSectionHeader("الدفع") }
                item { OrderValueRow("طريقة الدفع", paymentMethodLabel(o.paymentMethod)) }

                // ── ملاحظات ──
                if (o.notes.isNotBlank()) {
                    item { OrderSectionHeader("ملاحظات") }
                    item {
                        Text(
                            o.notes,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = muted,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  عناصر شاشة تفاصيل الطلب
// ═══════════════════════════════════════════════════════════════

/** عنوان قسم بخط Serif مع خط فاصل رفيع. */
@Composable
private fun OrderSectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            fontFamily = ElevenSerifFontFamily,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 28.dp, bottom = 8.dp),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

/** سطر «عنوان ← قيمة» مع خط فاصل رفيع. */
@Composable
private fun OrderValueRow(label: String, value: String, valueColor: Color? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = valueColor ?: MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}
