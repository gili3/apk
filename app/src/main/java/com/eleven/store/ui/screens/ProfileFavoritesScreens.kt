package com.eleven.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleven.store.data.model.Address
import com.eleven.store.data.model.Product
import com.eleven.store.ui.components.ElevenButton
import com.eleven.store.ui.components.ElevenTopBar
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════
//  دالة مساعدة لتنسيق الأسعار
// ═══════════════════════════════════════════════════════════════
private fun formatPriceProfile(value: Any?): String {
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

// ═══════════════════════════════════════════════════════════════
//  PROFILE SCREEN — نسخة طبق الأصل من Profile.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateToOrders: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf<ProfileTab>(ProfileTab.INFO) }
    var showForm by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<Address?>(null) }

    LaunchedEffect(user) { if (user != null) viewModel.loadAddresses() }

    // ── حالة عدم تسجيل الدخول ──
    if (user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Filled.Person,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MutedForeground,
                )
                Text(
                    "يرجى تسجيل الدخول للوصول إلى ملفك الشخصي",
                    color = MutedForeground,
                    fontSize = 16.sp,
                )
                ElevenButton(
                    text = "تسجيل الدخول",
                    onClick = onNavigateToLogin,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            ElevenTopBar(
                title = "الملف الشخصي",
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "الإعدادات")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = 24.dp + padding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ════════════════════════════════════════════════
            //  HEADER GRADIENT — مطابق للموقع:
            //  linear-gradient(180deg, ink → neutral[800])
            //  rounded-b-3xl
            // ════════════════════════════════════════════════
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A1A2E), // ink
                                    Color(0xFF2D2D44), // neutral[800]
                                ),
                            ),
                            RoundedCornerShape(
                                bottomStart = 24.dp,
                                bottomEnd = 24.dp,
                            ),
                        )
                        .padding(top = 32.dp, bottom = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Avatar circle
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(
                                    Color.White.copy(alpha = 0.1f),
                                    CircleShape,
                                )
                                .border(2.dp, Accent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = user!!.displayName?.firstOrNull()
                                    ?.uppercaseChar()
                                    ?.toString()
                                    ?: user!!.email?.firstOrNull()
                                        ?.uppercaseChar()
                                        ?.toString()
                                    ?: "11",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = Accent,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            user!!.displayName ?: "مستخدم",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            user!!.email ?: "",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // ════════════════════════════════════════════════
            //  TAB BAR — مطابق للموقع:
            //  flex gap-1 bg-white rounded-xl p-1
            // ════════════════════════════════════════════════
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp),
                        )
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                ) {
                    ProfileTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) Accent else Color.Transparent,
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { selectedTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                tab.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isSelected) Color.White else MutedForeground,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ════════════════════════════════════════════════
            //  TAB: المعلومات الشخصية
            // ════════════════════════════════════════════════
            if (selectedTab == ProfileTab.INFO) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Border,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            ProfileInfoRow(
                                icon = Icons.Filled.Person,
                                label = "الاسم الكامل",
                                value = user!!.displayName ?: "—",
                            )
                            HorizontalDivider(color = Border)
                            ProfileInfoRow(
                                icon = Icons.Filled.Email,
                                label = "البريد الإلكتروني",
                                value = user!!.email ?: "—",
                            )
                            HorizontalDivider(color = Border)
                            ProfileInfoRow(
                                icon = Icons.Filled.Phone,
                                label = "رقم الهاتف",
                                value = user!!.phoneNumber ?: "—",
                            )
                        }
                    }
                }
            }

            // ════════════════════════════════════════════════
            //  TAB: العناوين
            // ════════════════════════════════════════════════
            if (selectedTab == ProfileTab.ADDRESSES) {
                // قائمة العناوين
                if (addresses.isNotEmpty()) {
                    items(addresses, key = { it.id }) { addr ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Border,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // أيقونة الموقع
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant
                                                .copy(alpha = 0.3f),
                                            RoundedCornerShape(12.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        null,
                                        tint = Accent,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                // معلومات العنوان
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            addr.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                        if (addr.isDefault) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        Accent.copy(alpha = 0.1f),
                                                        RoundedCornerShape(4.dp),
                                                    )
                                                    .padding(
                                                        horizontal = 6.dp,
                                                        vertical = 1.dp,
                                                    ),
                                            ) {
                                                Text(
                                                    "افتراضي",
                                                    color = Accent,
                                                    fontSize = 10.sp,
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        "${addr.city}، ${addr.address}",
                                        color = MutedForeground,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        addr.phone,
                                        color = MutedForeground,
                                        fontSize = 12.sp,
                                    )
                                }
                                // زر تعديل
                                IconButton(
                                    onClick = {
                                        editingAddress = addr
                                        showForm = true
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        null,
                                        tint = MutedForeground,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                // زر حذف
                                IconButton(
                                    onClick = {
                                        viewModel.deleteAddress(addr.id)
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        null,
                                        tint = Destructive.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                } else if (!showForm) {
                    item {
                        Text(
                            "لا توجد عناوين مسجّلة",
                            color = MutedForeground,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        )
                    }
                }

                // نموذج إضافة / تعديل عنوان
                if (showForm) {
                    item {
                        AddressFormCard(
                            initial = editingAddress,
                            onSave = { fullName, phone, city, address, isDefault ->
                                val newAddress = Address(
                                    fullName = fullName,
                                    phone = phone,
                                    city = city,
                                    address = address,
                                    isDefault = isDefault,
                                )
                                if (editingAddress != null) {
                                    viewModel.updateAddress(
                                        editingAddress!!.id,
                                        newAddress,
                                    ) {
                                        showForm = false
                                        editingAddress = null
                                    }
                                } else {
                                    viewModel.addAddress(newAddress) {
                                        showForm = false
                                        editingAddress = null
                                    }
                                }
                            },
                            onCancel = {
                                showForm = false
                                editingAddress = null
                            },
                        )
                    }
                } else {
                    // زر إضافة عنوان جديد
                    item {
                        Button(
                            onClick = {
                                editingAddress = null
                                showForm = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onBackground,
                                contentColor = MaterialTheme.colorScheme.background,
                            ),
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "إضافة عنوان جديد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  PROFILE TAB ENUM
// ═══════════════════════════════════════════════════════════════

private enum class ProfileTab(val label: String) {
    INFO("البيانات الشخصية"),
    ADDRESSES("العناوين"),
}

// ═══════════════════════════════════════════════════════════════
//  PROFILE INFO ROW — صف معلومات المستخدم
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Accent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(label, color = MutedForeground, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  ADDRESS FORM CARD — نموذج إضافة / تعديل عنوان
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AddressFormCard(
    initial: Address?,
    onSave: (fullName: String, phone: String, city: String, address: String, isDefault: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var fullName by remember { mutableStateOf(initial?.fullName ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var city by remember { mutableStateOf(initial?.city ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var isDefault by remember { mutableStateOf(initial?.isDefault ?: false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(
                Triple("fullName", "الاسم الكامل", "أحمد محمد"),
                Triple("phone", "رقم الهاتف", "+966501234567"),
                Triple("city", "المدينة", "الرياض"),
                Triple("address", "العنوان التفصيلي", "الشارع والحي"),
            ).forEach { (key, label, placeholder) ->
                Column {
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = when (key) {
                            "fullName" -> fullName
                            "phone" -> phone
                            "city" -> city
                            "address" -> address
                            else -> ""
                        },
                        onValueChange = { v ->
                            when (key) {
                                "fullName" -> fullName = v
                                "phone" -> phone = v
                                "city" -> city = v
                                "address" -> address = v
                            }
                        },
                        placeholder = { Text(placeholder, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Border,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                .copy(alpha = 0.2f),
                        ),
                    )
                }
            }

            // Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = isDefault,
                    onCheckedChange = { isDefault = it },
                    colors = CheckboxDefaults.colors(checkedColor = Accent),
                )
                Text(
                    "تعيين كعنوان افتراضي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // أزرار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        if (fullName.isNotBlank() && phone.isNotBlank() &&
                            city.isNotBlank() && address.isNotBlank()
                        ) {
                            onSave(fullName, phone, city, address, isDefault)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) {
                    Text(
                        if (initial != null) "حفظ التعديل" else "حفظ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                    )
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                ) {
                    Text("إلغاء", fontSize = 14.sp, color = MutedForeground)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  FAVORITES SCREEN — نسخة طبق الأصل من Favorites.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FavoriteProductItem(
    product: Product,
    onAddToCart: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Column {
            // صورة المنتج مع زر المفضلة
            Box {
                AsyncImage(
                    model = product.mainImage,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                // زر قلب أعلى اليمين
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        null,
                        tint = Destructive,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            // معلومات المنتج
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatPriceProfile(product.price),
                    color = Accent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(8.dp))
                // زر إضافة للسلة
                Button(
                    onClick = onAddToCart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        Icons.Filled.ShoppingCart,
                        null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "أضف للسلة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    onProductClick: (String) -> Unit,
    onNavigateToProducts: () -> Unit,
) {
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadProducts() }

    val favProducts = allProducts.filter { it.id in favoriteIds }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ElevenTopBar(title = "المفضلة") },
    ) { padding ->
        if (favProducts.isEmpty()) {
            // ── حالة فارغة ──
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
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .background(
                                Destructive.copy(alpha = 0.1f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.FavoriteBorder,
                            null,
                            modifier = Modifier.size(56.dp),
                            tint = Destructive.copy(alpha = 0.5f),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "لا توجد منتجات مفضلة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "أضف المنتجات التي تعجبك إلى المفضلة لتجدها هنا بسهولة",
                        color = MutedForeground,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = onNavigateToProducts,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(horizontal = 32.dp),
                    ) {
                        Text(
                            "تسوق الآن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        } else {
            // ── شبكة المنتجات المفضلة ──
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 16.dp + padding.calculateTopPadding(),
                    bottom = 16.dp + padding.calculateBottomPadding(),
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(favProducts, key = { it.id }) { product ->
                    FavoriteProductItem(
                        product = product,
                        onAddToCart = {
                            viewModel.addToCart(product, 1)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("تمت الإضافة إلى السلة 🛒")
                            }
                        },
                        onToggleFavorite = {
                            viewModel.toggleFavorite(product.id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("تمت الإزالة من المفضلة")
                            }
                        },
                        onClick = { onProductClick(product.id) },
                    )
                }
            }
        }
    }
}