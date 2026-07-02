package com.eleven.store.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
import com.eleven.store.ui.components.InvoiceQrCode
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel

// ═══════════════════════════════════════════════════════════════
//  دوال مساعدة مشتركة
// ═══════════════════════════════════════════════════════════════

/** تنسيق السعر بالعملة — "1,234.56 ج.س" */
private fun formatPriceOrders(value: Any?): String {
    val d = when (value) {
        is Double -> value
        is Long   -> value.toDouble()
        is Int    -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else      -> 0.0
    }
    val s = if (d == d.toLong().toDouble()) d.toLong().toString()
            else "%,.2f".format(d)
    return "$s ج.س"
}

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

// ═══════════════════════════════════════════════════════════════
//  ORDER STATUS BADGE — مطابق لـ Badge في Orders.tsx / OrderDetail.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OrderStatusBadge(status: OrderStatus) {
    val (label, bg, fg, border) = when (status) {
        OrderStatus.PENDING   -> QuadStatus("قيد الانتظار",   Warning.copy(alpha = 0.1f),       Warning,       Warning.copy(alpha = 0.2f))
        OrderStatus.PAID      -> QuadStatus("تم الدفع",       Accent.copy(alpha = 0.1f),        Accent,        Accent.copy(alpha = 0.2f))
        OrderStatus.SHIPPED   -> QuadStatus("خرج للتوصيل",    Primary.copy(alpha = 0.1f),       Primary,       Primary.copy(alpha = 0.2f))
        OrderStatus.DELIVERED -> QuadStatus("تم التسليم",     Success.copy(alpha = 0.1f),       Success,       Success.copy(alpha = 0.2f))
        OrderStatus.CANCELLED -> QuadStatus("ملغى",           Destructive.copy(alpha = 0.1f),   Destructive,   Destructive.copy(alpha = 0.2f))
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private data class QuadStatus(
    val label: String,
    val bg: Color,
    val fg: Color,
    val border: Color,
)

// ═══════════════════════════════════════════════════════════════
//  ORDERS SCREEN — نسخة طبق الأصل من Orders.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
fun OrdersScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    onStartShopping: () -> Unit = onBack,
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadOrders() }

    Scaffold(topBar = { ElevenTopBar(title = "طلباتي", onBack = onBack) }) { padding ->
        when {
            // ── تحميل ──
            isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Accent, strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("جاري التحميل...", color = MutedForeground, fontSize = 14.sp)
                    }
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
                        Box(
                            Modifier
                                .size(96.dp)
                                .background(Accent.copy(alpha = 0.1f), CircleShape)
                                .border(2.dp, Accent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.ShoppingBag,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = Accent,
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "لا توجد طلبات بعد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "ابدأ التسوق الآن واستمتع بمنتجاتنا الرائعة والمختارة بعناية",
                            color = MutedForeground,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(32.dp))
                        ElevenButton(
                            text = "ابدأ التسوق",
                            onClick = onStartShopping,
                            icon = Icons.Filled.ShoppingBag,
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(48.dp),
                        )
                    }
                }
            }

            // ── قائمة الطلبات ──
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 8.dp + padding.calculateTopPadding(),
                        bottom = 24.dp + padding.calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Header
                    item {
                        Column(Modifier.padding(bottom = 8.dp)) {
                            Text(
                                "طلباتي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.width(48.dp).height(4.dp).background(Accent, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "تتبع جميع طلباتك وحالتها",
                                color = MutedForeground,
                                fontSize = 14.sp,
                            )
                        }
                    }

                    items(orders, key = { it.id }) { order ->
                        val dateObj = order.createdAt?.toDate() ?: java.util.Date()
                        val itemCount = order.items.size
                        val total = if (order.total > 0) order.total
                                    else order.items.sumOf { it.price * it.quantity }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOrderClick(order.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            "طلب #${order.orderNumber}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                        OrderStatusBadge(order.status)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        formatArabicDate(dateObj),
                                        color = MutedForeground,
                                        fontSize = 12.sp,
                                    )
                                    if (itemCount > 0) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "عدد المنتجات: $itemCount",
                                            color = MutedForeground,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    if (total > 0) {
                                        Text(
                                            formatPriceOrders(total),
                                            fontWeight = FontWeight.Bold,
                                            color = Accent,
                                            fontSize = 14.sp,
                                        )
                                    }
                                    Icon(
                                        Icons.Filled.Visibility,
                                        null,
                                        tint = Accent,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  ORDER DETAIL SCREEN — نسخة طبق الأصل من OrderDetail.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
fun OrderDetailScreen(
    viewModel: MainViewModel,
    orderId: String,
    onBack: () -> Unit,
) {
    val order by viewModel.selectedOrder.collectAsStateWithLifecycle()
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }

    Scaffold(topBar = { ElevenTopBar(title = "تفاصيل الطلب", onBack = onBack) }) { padding ->
        when {
            // ── تحميل ──
            order == null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Accent)
                }
            }

            // ── تفاصيل الطلب ──
            else -> {
                val o = order!!
                val statusLabel = o.status.label
                val paymentLabel = paymentMethodLabel(o.paymentMethod)
                val dateObj = o.createdAt?.toDate() ?: java.util.Date()
                val formattedDate = formatArabicDate(dateObj)
                val subtotal = o.items.sumOf { it.price * it.quantity }
                val total = if (o.total > 0) o.total else subtotal + o.shippingCost

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 16.dp + padding.calculateTopPadding(),
                        bottom = 24.dp + padding.calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ── Header ───────────────────────────
                    item {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "طلب #${o.orderNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 30.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                OrderStatusBadge(o.status)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(formattedDate, color = MutedForeground, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.width(40.dp).height(4.dp).background(Accent, RoundedCornerShape(2.dp)))
                        }
                    }

                    // ── معلومات الطلب ────────────────────
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Inventory2,
                                        null,
                                        tint = Accent,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Text(
                                        "معلومات الطلب",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                InfoRow("الحالة") { OrderStatusBadge(o.status) }
                                HorizontalDivider(color = Border)
                                InfoRow("طريقة الدفع", paymentLabel)
                                HorizontalDivider(color = Border)
                                InfoRow("تاريخ الطلب", formattedDate)
                            }
                        }
                    }

                    // ── عنوان الشحن ──────────────────────
                    o.shippingAddress?.let { addr ->
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                            ) {
                                Column(Modifier.padding(20.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.LocationOn,
                                            null,
                                            tint = Accent,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Text(
                                            "عنوان الشحن",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        addr.displayName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        "${addr.city} — ${addr.address}",
                                        color = MutedForeground,
                                        fontSize = 14.sp,
                                    )
                                    Text(
                                        addr.phone,
                                        color = MutedForeground,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }

                    // ── المنتجات ─────────────────────────
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.CreditCard,
                                        null,
                                        tint = Accent,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Text(
                                        "المنتجات (${o.items.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                o.items.forEachIndexed { idx, item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        AsyncImage(
                                            model = item.image,
                                            contentDescription = item.name,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(1.dp, Border, RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop,
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                item.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                "× ${item.quantity}",
                                                color = MutedForeground,
                                                fontSize = 12.sp,
                                            )
                                        }
                                        Text(
                                            formatPriceOrders(item.price * item.quantity),
                                            fontWeight = FontWeight.Bold,
                                            color = Accent,
                                            fontSize = 14.sp,
                                        )
                                    }
                                    if (idx < o.items.lastIndex) {
                                        Spacer(Modifier.height(12.dp))
                                        HorizontalDivider(color = Border)
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = Border)
                                Spacer(Modifier.height(12.dp))

                                InfoRow("المجموع الفرعي", formatPriceOrders(subtotal), bold = false)
                                Spacer(Modifier.height(4.dp))
                                InfoRow(
                                    "الشحن",
                                    if (o.shippingCost > 0) formatPriceOrders(o.shippingCost) else "مجاني",
                                    bold = false,
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
                                        "الإجمالي",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        formatPriceOrders(total),
                                        fontWeight = FontWeight.Bold,
                                        color = Accent,
                                        fontSize = 20.sp,
                                    )
                                }
                            }
                        }
                    }

                    // ── الفاتورة ─────────────────────────
                    item {
                        Column(Modifier.padding(top = 8.dp)) {
                            Text(
                                "الفاتورة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.width(32.dp).height(4.dp).background(Accent, RoundedCornerShape(2.dp)))
                        }
                    }

                    item {
                        InvoiceCard(
                            order = o,
                            storeSettings = storeSettings,
                            subtotal = subtotal,
                            total = total,
                            statusLabel = statusLabel,
                            paymentLabel = paymentLabel,
                            formattedDate = formattedDate,
                        )
                    }

                    // ── زر طباعة الفاتورة ────────────────
                    item {
                        Box(
                            Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            ElevenButton(
                                text = "طباعة الفاتورة",
                                onClick = { shareOrPrintInvoice(context, o, storeSettings) },
                                icon = Icons.Filled.Print,
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(48.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  INFO ROW — صف معلومات داخل البطاقات
// ═══════════════════════════════════════════════════════════════

@Composable
private fun InfoRow(
    label: String,
    value: String? = null,
    bold: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically,
    ) {
        Text(label, color = MutedForeground, fontSize = 14.sp)
        if (content != null) content()
        else Text(
            value ?: "",
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  INVOICE CARD — نسخة طبق الأصل من InvoicePrint في OrderDetail.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
private fun InvoiceCard(
    order: Order,
    storeSettings: StoreSettings,
    subtotal: Double,
    total: Double,
    statusLabel: String,
    paymentLabel: String,
    formattedDate: String,
) {
    val verifyUrl = remember(order.verificationToken, storeSettings.websiteUrl) {
        val base = storeSettings.websiteUrl.ifBlank { "https://eleven-sd.com" }.trimEnd('/')
        "$base/verify-order/${order.verificationToken}"
    }

    val brandColor = Accent
    val brandTint = Accent.copy(alpha = 0.1f)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // ── Header ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        "11",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = brandColor,
                        fontFamily = FontFamily.Serif,
                    )
                    Text(
                        "ELEVEN",
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold,
                        color = brandColor,
                    )
                    if (storeSettings.address.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            storeSettings.address,
                            fontSize = 12.sp,
                            color = MutedForeground,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "INVOICE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = brandColor,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "#${order.orderNumber}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        formattedDate,
                        fontSize = 12.sp,
                        color = MutedForeground,
                    )
                }
            }

            // ── Customer + Status ───────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "العميل",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = brandColor,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        order.shippingAddress?.displayName ?: "-",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        order.shippingAddress?.phone ?: "-",
                        color = MutedForeground,
                        fontSize = 13.sp,
                    )
                    Text(
                        "${order.shippingAddress?.city ?: ""} — ${order.shippingAddress?.address ?: ""}",
                        color = MutedForeground,
                        fontSize = 13.sp,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "حالة الطلب",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = brandColor,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .background(brandTint, RoundedCornerShape(6.dp))
                            .border(1.dp, brandColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            statusLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = brandColor,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        paymentLabel,
                        fontSize = 13.sp,
                        color = MutedForeground,
                    )
                }
            }

            // ── Products Table ──────────────────────────
            HorizontalDivider(color = Border)
            Spacer(Modifier.height(16.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(brandTint)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
            ) {
                Text("المنتج", Modifier.weight(2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("الكمية", Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                Text("السعر", Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                Text("المجموع", Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.End)
            }

            order.items.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                ) {
                    Text(item.name, Modifier.weight(2f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                    Text("${item.quantity}", Modifier.weight(1f), fontSize = 14.sp, color = MutedForeground, textAlign = TextAlign.Center)
                    Text(formatPriceOrders(item.price), Modifier.weight(1.2f), fontSize = 14.sp, color = MutedForeground, textAlign = TextAlign.Center)
                    Text(formatPriceOrders(item.price * item.quantity), Modifier.weight(1.2f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = Border)
            }

            Spacer(Modifier.height(20.dp))

            // ── Totals ──────────────────────────────────
            Column(Modifier.fillMaxWidth(0.62f).align(Alignment.End)) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    Arrangement.SpaceBetween,
                ) {
                    Text("المجموع الفرعي", fontSize = 14.sp, color = MutedForeground)
                    Text(formatPriceOrders(subtotal), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                }
                HorizontalDivider(color = Border)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    Arrangement.SpaceBetween,
                ) {
                    Text("الشحن", fontSize = 14.sp, color = MutedForeground)
                    Text(
                        if (order.shippingCost > 0) formatPriceOrders(order.shippingCost) else "مجاني",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(brandTint, RoundedCornerShape(8.dp))
                        .border(2.dp, brandColor, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically,
                ) {
                    Text("الإجمالي", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text(formatPriceOrders(total), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = brandColor)
                }
            }

            // ── QR ──────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Border)

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                InvoiceQrCode(value = verifyUrl, sizeDp = 110.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "تحقق من صحة الطلب عبر مسح الرمز",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "شكراً لتسوقكم من ${storeSettings.storeName.ifBlank { "Eleven" }}",
                    fontSize = 12.sp,
                    color = MutedForeground,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  مشاركة / طباعة الفاتورة
// ═══════════════════════════════════════════════════════════════

private fun shareOrPrintInvoice(
    context: android.content.Context,
    order: Order,
    storeSettings: StoreSettings,
) {
    val base = storeSettings.websiteUrl.ifBlank { "https://eleven-sd.com" }.trimEnd('/')
    val verifyUrl = "$base/verify-order/${order.verificationToken}"
    val shareText = "فاتورة الطلب #${order.orderNumber}\n$verifyUrl"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
}