package com.eleven.store.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eleven.store.data.model.Product
import com.eleven.store.ui.theme.Accent
import com.eleven.store.ui.theme.Border
import com.eleven.store.ui.theme.Destructive
import com.eleven.store.ui.theme.DestructiveBg
import com.eleven.store.ui.theme.Info
import com.eleven.store.ui.theme.InfoBg
import com.eleven.store.ui.theme.MutedForeground
import com.eleven.store.ui.theme.Neutral100
import com.eleven.store.ui.theme.Neutral200
import com.eleven.store.ui.theme.Primary
import com.eleven.store.ui.theme.PrimaryBg
import com.eleven.store.ui.theme.Success
import com.eleven.store.ui.theme.SuccessBg
import com.eleven.store.ui.theme.Warning
import com.eleven.store.ui.theme.WarningBg

// ═══════════════════════════════════════════════════════════════
//  PRODUCT CARD  — matches ProductCard.tsx
// ═══════════════════════════════════════════════════════════════

// ── مطابق تماماً لبطاقة صفحة Products.tsx بالموقع ──
// ملاحظة: لا توجد شارة "جديد" بالموقع هنا (فقط خصم أو "مميز")،
// ولا يوجد زر مفضلة فوق الصورة — المفضلة زر منفصل أسفل بجانب "إضافة للسلة".
// كان زر "إضافة للسلة" مفقوداً بالكامل من هذه البطاقة بالتطبيق — تمت إضافته.
@Composable
fun ProductCard(
    product: Product,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onAddToCart: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    formatPrice: (Any?) -> String = { value ->
        val number = when (value) {
            is String -> value.toDoubleOrNull() ?: 0.0
            is Number -> value.toDouble()
            else -> 0.0
        }
        "%.2f ج.س".format(number)
    }
) {
    val discount = product.discountPercent
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp), // rounded-xl
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // bg-card
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border), // border border-border
    ) {
        Column {
            // ── صورة 1:1 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant) // bg-secondary/20
            ) {
                AsyncImage(
                    model = product.mainImage,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // شارة واحدة فقط: خصم، أو "مميز" إذا لا يوجد خصم — top-2 right-2
                if (discount != null && discount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Destructive, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "-$discount%",
                            color = Color.White,
                            fontSize = 12.sp, // text-xs
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (product.isFeatured) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Accent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("مميز", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // أيقونة المفضلة — شفافة الخلفية، أعلى يسار الصورة (top-2 left-2 في الموقع)
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(32.dp),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Destructive else Color.White,
                        modifier = Modifier.size(22.dp), // مطابق لـ w-5 h-5 مع تكبير طفيف
                    )
                }
            }
            // ── معلومات المنتج — p-3 ──
            Column(modifier = Modifier.padding(12.dp)) {
                // اسم المنتج — font-bold text-sm line-clamp-1 mb-1
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                // السعر — items-baseline gap-1.5 mb-3
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = formatPrice(product.price),
                        fontSize = 16.sp, // text-base
                        fontWeight = FontWeight.ExtraBold,
                        color = Accent,
                    )
                    product.originalPrice?.let { orig ->
                        if (orig > product.price) {
                            Text(
                                text = formatPrice(orig),
                                fontSize = 12.sp, // text-xs
                                color = MutedForeground,
                                textDecoration = TextDecoration.LineThrough,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp)) // mb-3 قبل زر الإضافة للسلة
                // ── زر إضافة للسلة — بعرض كامل، مطابق للموقع ──
                Button(
                    onClick = onAddToCart,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة للسلة", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  SECTION HEADER — underline accent decoration
// ═══════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(
    title: String,
    onViewAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .background(Accent, RoundedCornerShape(50))
            )
        }
        if (onViewAll != null) {
            TextButton(onClick = onViewAll) {
                Text("عرض المزيد", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  ACCENT BUTTON
// ═══════════════════════════════════════════════════════════════

@Composable
fun ElevenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  ORDER STATUS BADGE — matches getStatusBadge() in Orders.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
fun OrderStatusBadge(status: com.eleven.store.data.model.OrderStatus) {
    val (bg, fg) = when (status) {
        com.eleven.store.data.model.OrderStatus.DELIVERED ->
            SuccessBg to Success
        com.eleven.store.data.model.OrderStatus.PENDING ->
            WarningBg to Warning
        com.eleven.store.data.model.OrderStatus.SHIPPED ->
            InfoBg to Info
        com.eleven.store.data.model.OrderStatus.CANCELLED ->
            DestructiveBg to Destructive
        com.eleven.store.data.model.OrderStatus.PAID ->
            PrimaryBg to Primary
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(status.label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ═══════════════════════════════════════════════════════════════
//  PRODUCT CARD SKELETON
// ═══════════════════════════════════════════════════════════════

@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(
                        Brush.linearGradient(listOf(Neutral200, Neutral100, Neutral200))
                    )
            )
            Column(Modifier.padding(12.dp)) {
                Box(Modifier.fillMaxWidth(0.8f).height(12.dp).background(Neutral200, RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.4f).height(14.dp).background(Neutral200, RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f).height(32.dp).background(Neutral200, RoundedCornerShape(8.dp)))
                    Box(Modifier.width(32.dp).height(32.dp).background(Neutral200, RoundedCornerShape(8.dp)))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  ELEVEN TOP APP BAR
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElevenTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        )
    )
}