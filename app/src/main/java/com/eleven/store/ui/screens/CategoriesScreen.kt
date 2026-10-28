package com.eleven.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleven.store.data.model.Brand
import com.eleven.store.data.model.Category
import com.eleven.store.ui.theme.Border
import com.eleven.store.ui.theme.MutedForeground
import com.eleven.store.ui.viewmodel.MainViewModel

// ═══════════════════════════════════════════════════════════════
//  CATEGORIES SCREEN — تصفح بصري للتصنيفات + العلامات التجارية
//  ✅ جديد: كان "عرض المزيد" بقسم التصنيفات بالرئيسية يفتح شاشة
//  المنتجات العامة بلا فلتر (يحتاج المستخدم يفتح منسدل "كل الفئات"
//  بنفسه بعدها) — الآن شاشة تصفح مخصّصة بشبكة بطاقات صورة لكل
//  تصنيف/علامة، والضغط على أي بطاقة يودّي مباشرة لمنتجاتها المفلترة.
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    onCategoryClick: (String) -> Unit,
    onBrandClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        "التصنيفات والعلامات التجارية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }
        },
    ) { padding ->
        if (categories.isEmpty() && brands.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("لا توجد تصنيفات أو علامات تجارية حالياً", color = MutedForeground)
            }
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (categories.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "التصنيفات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(categories, key = { "cat_${it.id}" }) { cat ->
                    CategoryGridCard(cat, onClick = { onCategoryClick(cat.id) })
                }
            }

            if (brands.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "العلامات التجارية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(brands, key = { "brand_${it.id}" }) { brand ->
                    BrandGridCard(brand, onClick = { onBrandClick(brand.id) })
                }
            }
        }
    }
}

@Composable
private fun CategoryGridCard(cat: Category, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center,
        ) {
            if (cat.image.isNotBlank()) {
                AsyncImage(
                    model = cat.image,
                    contentDescription = cat.name,
                    contentScale = ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Filled.Image),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Filled.ShoppingBag,
                    contentDescription = null,
                    tint = MutedForeground,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(
            text = cat.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BrandGridCard(brand: Brand, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, Border, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (brand.logo.isNotBlank()) {
                AsyncImage(
                    model = brand.logo,
                    contentDescription = brand.name,
                    contentScale = ContentScale.Fit,
                    error = rememberVectorPainter(Icons.Filled.Image),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                )
            } else {
                Text(
                    brand.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MutedForeground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
        Text(
            text = brand.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
