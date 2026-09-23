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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleven.store.data.model.Address
import com.eleven.store.data.model.Product
import com.eleven.store.ui.components.ElevenSnackbarHost
import com.eleven.store.ui.components.SnackbarType
import com.eleven.store.ui.components.showMessage
import com.eleven.store.ui.components.ElevenButton
import com.eleven.store.ui.components.ElevenTopBar
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════
//  دالة مساعدة لتنسيق الأسعار
// ═══════════════════════════════════════════════════════════════
// ملاحظة: تنسيق السعر أصبح موحداً عبر formatPrice() في ScreenCommon.kt

// ═══════════════════════════════════════════════════════════════
//  PROFILE SCREEN — تصميم «البسيط»: بلا بطاقات، خطوط فاصلة رفيعة
//  وعناوين Serif. الاسم بارز بالأعلى، ثم قسما البيانات والعناوين.
// ═══════════════════════════════════════════════════════════════

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val addressesError by viewModel.addressesError.collectAsStateWithLifecycle()

    var showForm by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<Address?>(null) }
    // ✅ جديد: حذف العنوان كان فورياً بلا تأكيد — الآن نافذة تأكيد قبل الحذف.
    var addressToDelete by remember { mutableStateOf<Address?>(null) }

    // ── تعديل الاسم ورقم الهاتف ──
    var isEditingInfo by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var savingInfo by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(user) { if (user != null) viewModel.loadAddresses() }
    // ✅ إصلاح: نفس إصلاح CheckoutScreen — يعرض رسالة واضحة بدل قائمة عناوين
    // فارغة صامتة عند فشل التحميل فعلياً (غير متاح بلا إنترنت مثلاً)
    LaunchedEffect(addressesError) {
        addressesError?.let {
            snackbarHostState.showMessage(it, SnackbarType.ERROR)
            viewModel.consumeAddressesError()
        }
    }

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

    val account = user!!
    val rawName = userProfile?.name?.takeIf { it.isNotBlank() }
        ?: account.displayName?.takeIf { it.isNotBlank() }
    val displayName = rawName ?: "مستخدم"
    val email = account.email.orEmpty()
    val phone = userProfile?.phone?.takeIf { it.isNotBlank() }

    val onSurface = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val line = MaterialTheme.colorScheme.outline

    Scaffold(
        snackbarHost = { ElevenSnackbarHost(snackbarHostState) },
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
                start = 20.dp, end = 20.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 24.dp + padding.calculateBottomPadding(),
            ),
        ) {
            // ── الاسم والبريد ──
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        displayName,
                        fontFamily = ElevenSerifFontFamily,
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                        color = onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (email.isNotBlank()) {
                        Text(email, color = muted, fontSize = 13.sp)
                    }
                }
            }

            // ── البيانات ──
            item {
                ProfileSectionHeader(
                    title = "البيانات",
                    actionLabel = if (!isEditingInfo) "تعديل" else null,
                ) {
                    editName = rawName ?: ""
                    editPhone = userProfile?.phone ?: ""
                    isEditingInfo = true
                }
            }
            if (!isEditingInfo) {
                item { ProfileValueRow("الهاتف", phone ?: "—") }
                item { ProfileValueRow("البريد الإلكتروني", email.ifBlank { "—" }) }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("الاسم الكامل") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = onSurface,
                                unfocusedBorderColor = line,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editPhone,
                            // ✅ يقبل الأرقام فقط (مع علامة + اختيارية بالبداية لمفتاح
                            // الدولة)، ويمنع أي حرف أو رمز آخر عبر الفلترة هنا (لوحة
                            // المفاتيح الرقمية وحدها لا تمنع اللصق بأحرف).
                            onValueChange = { v ->
                                editPhone = v.filterIndexed { i, c -> c.isDigit() || (c == '+' && i == 0) }
                            },
                            label = { Text("رقم الهاتف") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = onSurface,
                                unfocusedBorderColor = line,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "البريد الإلكتروني: ${email.ifBlank { "—" }}",
                            fontSize = 12.sp,
                            color = muted,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Button(
                                onClick = {
                                    if (editName.isBlank()) {
                                        coroutineScope.launch {
                                            snackbarHostState.showMessage("الاسم مطلوب", SnackbarType.ERROR)
                                        }
                                        return@Button
                                    }
                                    savingInfo = true
                                    viewModel.updateProfile(editName.trim(), editPhone.trim()) { success, error ->
                                        savingInfo = false
                                        coroutineScope.launch {
                                            snackbarHostState.showMessage(
                                                if (success) "تم حفظ التعديلات بنجاح" else (error ?: "تعذر حفظ التعديلات"),
                                                if (success) SnackbarType.SUCCESS else SnackbarType.ERROR
                                            )
                                        }
                                        if (success) isEditingInfo = false
                                    }
                                },
                                enabled = !savingInfo,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground,
                                    contentColor = MaterialTheme.colorScheme.background,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                if (savingInfo) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.background,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("حفظ", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            OutlinedButton(
                                onClick = { isEditingInfo = false },
                                enabled = !savingInfo,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Text("إلغاء")
                            }
                        }
                    }
                }
            }

            // ── العناوين ──
            item {
                ProfileSectionHeader(
                    title = "العناوين",
                    actionLabel = if (!showForm) "إضافة" else null,
                ) {
                    editingAddress = null
                    showForm = true
                }
            }
            if (addresses.isNotEmpty()) {
                items(addresses, key = { it.id }) { addr ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        addr.fullName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = onSurface,
                                    )
                                    if (addr.isDefault) {
                                        Box(
                                            modifier = Modifier
                                                .border(1.dp, line, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 1.dp),
                                        ) {
                                            Text("افتراضي", fontSize = 11.sp, color = muted)
                                        }
                                    }
                                }
                                Text(
                                    "${addr.city}، ${addr.address}",
                                    color = muted,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(addr.phone, color = muted, fontSize = 12.sp)
                            }
                            // ✅ أزرار بالحجم الافتراضي (48dp) بدل 32dp — أسهل للّمس
                            IconButton(
                                onClick = {
                                    editingAddress = addr
                                    showForm = true
                                },
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "تعديل العنوان",
                                    tint = muted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(onClick = { addressToDelete = addr }) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = "حذف العنوان",
                                    tint = Destructive.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        HorizontalDivider(color = line)
                    }
                }
            } else if (!showForm) {
                item {
                    Text(
                        "لا توجد عناوين مسجّلة",
                        color = muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            if (showForm) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    AddressFormCard(
                        initial = editingAddress,
                        // ✅ توحيد البيانات: عنوان جديد يبدأ ببيانات الملف الشخصي
                        defaultFullName = rawName ?: "",
                        defaultPhone = userProfile?.phone ?: "",
                        onSave = { fullName, phoneNumber, city, address, isDefault ->
                            val newAddress = Address(
                                fullName = fullName,
                                phone = phoneNumber,
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
                }
            }
        }
    }

    // ✅ نافذة تأكيد حذف العنوان
    addressToDelete?.let { addr ->
        AlertDialog(
            onDismissRequest = { addressToDelete = null },
            title = { Text("حذف العنوان", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل تريد حذف هذا العنوان؟",
                    fontSize = 14.sp,
                    color = MutedForeground,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editingAddress?.id == addr.id) {
                            showForm = false
                            editingAddress = null
                        }
                        viewModel.deleteAddress(addr.id)
                        addressToDelete = null
                    },
                ) {
                    Text("حذف", color = Destructive, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { addressToDelete = null }) { Text("إلغاء") }
            },
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  عناصر شاشة الملف الشخصي
// ═══════════════════════════════════════════════════════════════

/** عنوان قسم بخط Serif + رابط إجراء اختياري (نص بخط سفلي) بمنطقة لمس 48dp. */
@Composable
private fun ProfileSectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontFamily = ElevenSerifFontFamily,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (actionLabel != null) {
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    actionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            Spacer(Modifier.height(48.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

/** سطر «عنوان ← قيمة» بسيط مع خط فاصل رفيع. */
@Composable
private fun ProfileValueRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

// ═══════════════════════════════════════════════════════════════
//  ADDRESS FORM CARD — نموذج إضافة / تعديل عنوان
// ═══════════════════════════════════════════════════════════════

@Composable
// ✅ إصلاح: كانت private، ما منع إعادة استخدامها من شاشة الدفع (انظر CheckoutScreens.kt)
fun AddressFormCard(
    initial: Address?,
    onSave: (fullName: String, phone: String, city: String, address: String, isDefault: Boolean) -> Unit,
    onCancel: () -> Unit,
    // ✅ إصلاح: توحيد حقلي الاسم والهاتف بين الملف الشخصي والعنوان — عنوان
    // جديد يُعبَّأ مسبقاً ببيانات الملف الشخصي بدل تكرار كتابتها من الصفر
    // (وبدل بقائها فارغة فتختلف عرضاً عن بيانات الملف الشخصي). عنوان قائم
    // بالفعل (initial != null) يحتفظ ببياناته الخاصة كما هي، لأن عنوان
    // الشحن قد يكون لمستلم مختلف عمداً.
    defaultFullName: String = "",
    defaultPhone: String = "",
) {
    // ✅ إصلاح: قائمة العناوين تبقى ظاهرة مع أزرار "تعديل" الخاصة بها حتى أثناء
    // فتح هذا النموذج (انظر مكان الاستدعاء بـProfileScreen) — لو ضغط المستخدم
    // "تعديل" على عنوان آخر والنموذج مفتوح أصلاً، كان يتغيّر editingAddress إلى
    // العنوان الجديد بينما remember غير المرتبط بمفتاح يُبقي حقول النموذج على
    // قيم العنوان *القديم*. عند الحفظ، كانت هذه القيم القديمة تُكتب فعلياً تحت
    // معرّف العنوان *الجديد* (editingAddress!!.id) — استبدال صامت لبيانات عنوان
    // بأخرى. ربط remember بمعرّف العنوان (initial?.id) يعيد تهيئة الحقول بمجرد
    // تغيّر هدف التعديل.
    val formKey = initial?.id ?: "new"
    var fullName by remember(formKey) { mutableStateOf(initial?.fullName ?: defaultFullName) }
    var phone by remember(formKey) { mutableStateOf(initial?.phone ?: defaultPhone) }
    var city by remember(formKey) { mutableStateOf(initial?.city ?: "") }
    var address by remember(formKey) { mutableStateOf(initial?.address ?: "") }
    var isDefault by remember(formKey) { mutableStateOf(initial?.isDefault ?: false) }

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
                                // ✅ إصلاح: رقم الهاتف يقبل أرقاماً فقط (مع + اختيارية بالبداية
                                // لمفتاح الدولة) — يمنع إدخال أحرف/رموز غير مطلوبة.
                                "phone" -> phone = v.filterIndexed { i, c -> c.isDigit() || (c == '+' && i == 0) }
                                "city" -> city = v
                                "address" -> address = v
                            }
                        },
                        placeholder = { Text(placeholder, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = if (key == "phone")
                            KeyboardOptions(keyboardType = KeyboardType.Phone)
                        else KeyboardOptions.Default,
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
                    placeholder = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Image),
                    error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                // زر قلب — شفاف الخلفية، أعلى يسار — موحّد مع بقية البطاقات
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        null,
                        tint = Destructive,
                        modifier = Modifier.size(22.dp),
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
                    text = formatPrice(product.price),
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
    onNavigateToLogin: () -> Unit,
) {
    // ✅ إصلاح: كانت هذه الشاشة تشتق القائمة من allProducts (حالة شاشة
    // المنتجات المفلترة) بدل جلب بيانات المفضلة الفعلية مباشرة — انظر شرح
    // الإصلاح الكامل في FirestoreRepository.getFavoriteProducts وMainViewModel.
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val favProducts by viewModel.favoriteProducts.collectAsStateWithLifecycle()
    val isLoading by viewModel.favoritesLoading.collectAsStateWithLifecycle()
    val loadError by viewModel.favoriteProductsError.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user) { if (user != null) viewModel.loadFavoriteProducts() }

    // ✅ إصلاح: getFavoriteProducts كانت تُرجع قائمة فارغة بصمت لزائر غير
    // مسجّل دخول (بلا uid)، فتظهر شاشة "لا توجد مفضلة" المُضلِّلة بدل طلب
    // تسجيل الدخول فعلياً — نفس علّة OrdersScreen بالضبط.
    if (user == null) {
        Scaffold(topBar = { ElevenTopBar(title = "المفضلة") }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MutedForeground,
                    )
                    Text(
                        "يرجى تسجيل الدخول لعرض المفضلة",
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
        }
        return
    }

    Scaffold(
        snackbarHost = { ElevenSnackbarHost(snackbarHostState) },
        topBar = { ElevenTopBar(title = "المفضلة") },
    ) { padding ->
        if (isLoading && favProducts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Accent, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
            }
        } else if (loadError != null && favProducts.isEmpty()) {
            // ── فشل تحميل فعلي (شبكة/سيرفر) — مختلف عن "لا توجد مفضلة فعلاً" ──
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    Icon(
                        Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MutedForeground,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "تعذّر تحميل المفضلة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "تحقق من اتصالك بالإنترنت وحاول مرة أخرى",
                        color = MutedForeground,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.loadFavoriteProducts() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                    ) {
                        Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (favProducts.isEmpty()) {
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
                                snackbarHostState.showMessage("تمت الإضافة إلى السلة 🛒", SnackbarType.SUCCESS)
                            }
                        },
                        onToggleFavorite = {
                            viewModel.toggleFavorite(product.id)
                            coroutineScope.launch {
                                snackbarHostState.showMessage("تمت الإزالة من المفضلة", SnackbarType.SUCCESS)
                            }
                        },
                        onClick = { onProductClick(product.id) },
                    )
                }
            }
        }
    }
}