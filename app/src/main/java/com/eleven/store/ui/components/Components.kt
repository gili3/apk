package com.eleven.store.ui.components

import com.eleven.store.ui.screens.formatPrice as sharedFormatPrice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eleven.store.data.model.Product
import com.eleven.store.ui.theme.Accent
import com.eleven.store.ui.theme.Destructive
import com.eleven.store.ui.theme.DestructiveBg
import com.eleven.store.ui.theme.Info
import com.eleven.store.ui.theme.InfoBg
import com.eleven.store.ui.theme.Neutral100
import com.eleven.store.ui.theme.Neutral200
import com.eleven.store.ui.theme.OrderStatusColors
import com.eleven.store.ui.theme.Primary
import com.eleven.store.ui.theme.PrimaryBg
import com.eleven.store.ui.theme.Success
import com.eleven.store.ui.theme.SuccessBg
import com.eleven.store.ui.theme.Warning
import com.eleven.store.ui.theme.WarningBg

// ═══════════════════════════════════════════════════════════════
//  PRODUCT CARD — تصميم مبسّط (صورة + اسم + سعر + مفضلة فقط)
//  - بدون حدود/ظل حول البطاقة، زوايا دائرية واسعة، صورة بنسبة عمودية
//  - بدون زر "إضافة للسلة" وبدون شارات خصم/مميز فوق البطاقة — الإضافة
//    للسلة وتفاصيل العرض تظهر بصفحة المنتج بعد الضغط على البطاقة
//  - العرض يُحدَّد من الخارج عبر modifier (fillMaxWidth بالشبكة،
//    width(160.dp) بصف الرئيسية الأفقي) ليبقى الشكل والمحتوى متطابقين
//    تماماً بين الشاشتين
@Composable
fun ProductCard(
    product: Product,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    formatPrice: (Any?) -> String = ::sharedFormatPrice,
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        // ── صورة — نسبة عمودية (4:5) بزوايا دائرية واسعة ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(RoundedCornerShape(18.dp))
                .background(Neutral100),
        ) {
            AsyncImage(
                model = product.mainImage,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Filled.Image),
                error = rememberVectorPainter(Icons.Filled.Image),
                modifier = Modifier.fillMaxSize(),
            )

            // زر مفضلة — دائرة بيضاء صغيرة أعلى الصورة
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(30.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    .clickable(onClick = onFavoriteToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Destructive else Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // اسم المنتج — سطر واحد فقط
        Text(
            text = product.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(2.dp))

        // السعر
        Text(
            text = formatPrice(product.price),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Accent,
        )
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
//  ORDER STATUS BADGE — ألوان ثابتة موحّدة مع الموقع ولوحة التحكم
//  (راجع OrderStatusColors في Theme.kt و lib/orderStatus.ts بالموقع)
// ═══════════════════════════════════════════════════════════════

@Composable
fun OrderStatusBadge(status: com.eleven.store.data.model.OrderStatus) {
    val bg = when (status) {
        com.eleven.store.data.model.OrderStatus.DELIVERED -> OrderStatusColors.DeliveredBg
        com.eleven.store.data.model.OrderStatus.PENDING -> OrderStatusColors.PendingBg
        com.eleven.store.data.model.OrderStatus.SHIPPED -> OrderStatusColors.ShippedBg
        com.eleven.store.data.model.OrderStatus.CANCELLED -> OrderStatusColors.CancelledBg
        com.eleven.store.data.model.OrderStatus.PAID -> OrderStatusColors.PaidBg
    }
    val fg = OrderStatusColors.Foreground
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