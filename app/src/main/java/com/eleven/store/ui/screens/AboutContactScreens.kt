package com.eleven.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eleven.store.ui.components.ElevenTopBar
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════
//  مكوّنات مساعدة مشتركة بين About و Contact
// ═══════════════════════════════════════════════════════════════

/**
 * خط تحت العنوان — مطابق لـ:
 * <div className="w-16 h-1 bg-accent mx-auto rounded-full mb-6"></div>
 */
@Composable
private fun SectionUnderline(
    modifier: Modifier = Modifier,
    color: Color = Accent,
    width: Int = 64,
) {
    Box(
        modifier
            .width(width.dp)
            .height(4.dp)
            .background(color, RoundedCornerShape(50))
    )
}

/**
 * أيقونة داخل box — مطابق لـ:
 * <div className="w-14 h-14 rounded-lg bg-accent/10 border border-accent flex items-center justify-center mx-auto mb-4">
 *   <feature.icon className="w-7 h-7 text-accent" />
 * </div>
 */
@Composable
private fun IconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    iconSize: androidx.compose.ui.unit.Dp = 28.dp,
    bgColor: Color = Accent.copy(alpha = 0.1f),
    borderColor: Color = Accent,
    tint: Color = Accent,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(bgColor, shape)
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

// ملاحظة: openUri مُعرّفة بشكل مشترك في ScreenCommon.kt

// ═══════════════════════════════════════════════════════════════
//  ABOUT SCREEN — نسخة طبق الأصل من About.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
fun AboutScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val storeName = storeSettings.storeName.ifBlank { "Eleven" }
    val storeDescription = storeSettings.storeDescription.ifBlank {
        "نحن متجر متخصص في الملابس الراقية والمنتجات المختارة بعناية لتلبية ذوقك الفريد."
    }
    val storeVision = storeSettings.storeVision.ifBlank {
        "نهدف إلى أن نكون الوجهة الأولى للتسوق عبر الإنترنت، حيث يجد العميل كل ما يحتاجه بأسعار تنافسية وجودة لا تضاهى."
    }
    val storeMission = storeSettings.storeMission.ifBlank {
        "توفير منصة آمنة وموثوقة تربط بين أفضل المنتجات والمستهلكين، مع التركيز على سرعة التوصيل وضمان رضا العملاء التام."
    }
    val storeAboutImage = storeSettings.storeAboutImage.ifBlank {
        "https://images.unsplash.com/photo-1534452203293-494d7ddbf7e0?w=800&auto=format&fit=crop&q=60"
    }

    Scaffold(
        topBar = {
            ElevenTopBar(title = "حول $storeName", onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp + padding.calculateTopPadding(),
                bottom = 32.dp + padding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // ════════════════════════════════════════════════════════
            //  HEADER — max-w-3xl mx-auto text-center mb-16
            // ════════════════════════════════════════════════════════
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 64.dp),
                ) {
                    Text(
                        "حول $storeName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        lineHeight = 52.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    SectionUnderline(width = 64)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        storeDescription,
                        color = MutedForeground,
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ════════════════════════════════════════════════════════
            //  VISION & MISSION + IMAGE
            //  الموقع: grid-cols-1 md:grid-cols-2 — على عرض الموبايل تكون
            //  عمودًا واحدًا متتاليًا (رؤية، مهمة، ثم صورة)، وليست جنبًا إلى جنب
            // ════════════════════════════════════════════════════════
            item {
                Column(
                    modifier = Modifier.padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    // Vision
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconBadge(
                                icon = Icons.Filled.EmojiEvents,
                                size = 48.dp,
                                iconSize = 24.dp,
                            )
                            Text(
                                "رؤيتنا",
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            storeVision,
                            color = MutedForeground,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                        )
                    }

                    // Mission
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconBadge(
                                icon = Icons.Filled.Bolt,
                                size = 48.dp,
                                iconSize = 24.dp,
                            )
                            Text(
                                "مهمتنا",
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            storeMission,
                            color = MutedForeground,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                        )
                    }

                    // Image — aspect-square rounded-lg overflow-hidden
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .border(1.dp, Border, RoundedCornerShape(12.dp)),
                    ) {
                        AsyncImage(
                            model = storeAboutImage,
                            contentDescription = storeName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // ════════════════════════════════════════════════════════
            //  FEATURES — "لماذا تختار Eleven؟"
            //  text-4xl text-center mb-4
            // ════════════════════════════════════════════════════════
            item {
                Column(
                    modifier = Modifier.padding(bottom = 80.dp),
                ) {
                    Text(
                        "لماذا تختار Eleven؟",
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    SectionUnderline(
                        width = 64,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(48.dp))

                    // grid-cols-4 gap-6
                    val features = listOf(
                        Quad(Icons.Filled.ShoppingBag, "منتجات مختارة", "نختار منتجاتنا بعناية لضمان أعلى معايير الجودة والأناقة"),
                        Quad(Icons.Filled.VerifiedUser, "تسوق آمن", "بياناتك ومدفوعاتك محمية بتشفير عالي المستوى"),
                        Quad(Icons.Filled.LocalShipping, "توصيل سريع", "نصل إليك أينما كنت في أسرع وقت ممكن"),
                        Quad(Icons.Filled.Headset, "دعم فني", "فريقنا متواجد دائماً للرد على استفساراتك"),
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        features.forEach { (icon, title, desc) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Border
                                ),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = 32.dp,
                                            bottom = 24.dp,
                                            start = 16.dp,
                                            end = 16.dp,
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    IconBadge(
                                        icon = icon,
                                        size = 56.dp,
                                        iconSize = 28.dp,
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        desc,
                                        color = MutedForeground,
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ════════════════════════════════════════════════════════
            //  STATS — bg-gradient-to-r from-accent/10 to-accent/5
            // ════════════════════════════════════════════════════════
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 80.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Accent.copy(alpha = 0.10f),
                                    Accent.copy(alpha = 0.05f),
                                )
                            ),
                            RoundedCornerShape(12.dp),
                        )
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(vertical = 48.dp, horizontal = 16.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        listOf(
                            "10K+" to "عميل راضي",
                            "5K+" to "منتج متنوع",
                            "24/7" to "دعم عملاء",
                        ).forEach { (num, label) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    num,
                                    color = Accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    label,
                                    color = MutedForeground,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            // ════════════════════════════════════════════════════════
            //  TEAM — "فريقنا المتميز" — grid-cols-2 gap-8
            // ════════════════════════════════════════════════════════
            item {
                Column(
                    modifier = Modifier.padding(bottom = 80.dp),
                ) {
                    Text(
                        "فريقنا المتميز",
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    SectionUnderline(
                        width = 64,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(48.dp))

                    val team = listOf(
                        TeamMember(
                            icon = Icons.Filled.Groups,
                            title = "فريق محترف",
                            subtitle = "متخصصون في تجارة التجزئة",
                            description = "فريقنا يتكون من محترفين ذوي خبرة عالية في مجال التجارة الإلكترونية والخدمات اللوجستية.",
                        ),
                        TeamMember(
                            icon = Icons.Filled.EmojiEvents,
                            title = "جودة معترف بها",
                            subtitle = "شهادات دولية",
                            description = "نحن حاصلون على عدة شهادات دولية تؤكد التزامنا بأعلى معايير الجودة والخدمة.",
                        ),
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                    ) {
                        team.forEach { member ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Border
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        IconBadge(
                                            icon = member.icon,
                                            size = 64.dp,
                                            iconSize = 32.dp,
                                            shape = CircleShape,
                                        )
                                        Column {
                                            Text(
                                                member.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                            Text(
                                                member.subtitle,
                                                color = MutedForeground,
                                                fontSize = 14.sp,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        member.description,
                                        color = MutedForeground,
                                        fontSize = 16.sp,
                                        lineHeight = 26.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ════════════════════════════════════════════════════════
            //  CONTACT SECTION — مثل APK AboutContactScreen
            //  bg-card border rounded-2xl p-8 md:p-12
            // ════════════════════════════════════════════════════════
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Text(
                            "تواصل معنا",
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(8.dp))
                        SectionUnderline(width = 48)
                        Spacer(Modifier.height(32.dp))

                        // grid-cols-2 gap-4
                        val contactItems = listOf(
                            ContactItem(
                                emoji = "📞",
                                label = "الهاتف",
                                value = storeSettings.phone.ifBlank { "—" },
                                onClick = if (storeSettings.phone.isNotBlank()) {
                                    { openUri(context, "tel:${storeSettings.phone}") }
                                } else null,
                            ),
                            ContactItem(
                                emoji = "✉️",
                                label = "البريد الإلكتروني",
                                value = storeSettings.email.ifBlank { "—" },
                                onClick = if (storeSettings.email.isNotBlank()) {
                                    { openUri(context, "mailto:${storeSettings.email}") }
                                } else null,
                            ),
                            ContactItem(
                                emoji = "💬",
                                label = "واتساب",
                                value = storeSettings.whatsapp.ifBlank { "—" },
                                onClick = if (storeSettings.whatsapp.isNotBlank()) {
                                    {
                                        openUri(
                                            context,
                                            "https://wa.me/${storeSettings.whatsapp.replace(Regex("\\D"), "")}"
                                        )
                                    }
                                } else null,
                            ),
                            ContactItem(
                                emoji = "📍",
                                label = "العنوان",
                                value = storeSettings.address.ifBlank { "—" },
                                onClick = null,
                            ),
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            contactItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.dp,
                                            Border,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .then(
                                            if (item.onClick != null)
                                                Modifier.clickable(
                                                    onClick = item.onClick
                                                )
                                            else Modifier
                                        )
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    Text(item.emoji, fontSize = 24.sp)
                                    Column {
                                        Text(
                                            item.label,
                                            color = MutedForeground,
                                            fontSize = 12.sp,
                                        )
                                        Text(
                                            item.value,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (item.onClick != null)
                                                Accent
                                            else
                                                MaterialTheme.colorScheme.onBackground,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
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
}

// ═══════════════════════════════════════════════════════════════
//  CONTACT SCREEN — نسخة طبق الأصل من Contact.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
fun ContactScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ElevenTopBar(title = "اتصل بنا", onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp + padding.calculateTopPadding(),
                bottom = 32.dp + padding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // ════════════════════════════════════════════════════════
            //  HEADER — max-w-3xl mx-auto text-center mb-16
            // ════════════════════════════════════════════════════════
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 64.dp),
                ) {
                    Text(
                        "اتصل بنا",
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        lineHeight = 52.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    SectionUnderline(width = 64)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "هل لديك استفسار أو اقتراح؟ نحن هنا لمساعدتك دائماً. تواصل معنا بأي طريقة مناسبة لك.",
                        color = MutedForeground,
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ════════════════════════════════════════════════════════
            //  CONTENT — grid-cols-3 (1 sidebar + 2 form)
            // ════════════════════════════════════════════════════════
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    // ════════ SIDEBAR — lg:col-span-1 space-y-4 ════════
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Phone
                        if (storeSettings.phone.isNotBlank()) {
                            ContactInfoCard(
                                icon = Icons.Filled.Phone,
                                title = "اتصل بنا",
                                value = storeSettings.phone,
                                onClick = { openUri(context, "tel:${storeSettings.phone}") },
                            )
                        }

                        // Email
                        if (storeSettings.email.isNotBlank()) {
                            ContactInfoCard(
                                icon = Icons.Filled.Email,
                                title = "البريد الإلكتروني",
                                value = storeSettings.email,
                                onClick = { openUri(context, "mailto:${storeSettings.email}") },
                            )
                        }

                        // Address
                        if (storeSettings.address.isNotBlank()) {
                            ContactInfoCard(
                                icon = Icons.Filled.LocationOn,
                                title = "الموقع",
                                value = storeSettings.address,
                                onClick = null,
                            )
                        }

                        // WhatsApp
                        if (storeSettings.whatsapp.isNotBlank()) {
                            ContactInfoCard(
                                icon = Icons.Filled.Chat,
                                title = "واتساب",
                                value = storeSettings.whatsapp,
                                iconBg = Success.copy(alpha = 0.1f),
                                iconBorder = Success,
                                iconTint = Success,
                                valueColor = Success,
                                onClick = {
                                    openUri(
                                        context,
                                        "https://wa.me/${storeSettings.whatsapp.replace(Regex("\\D"), "")}"
                                    )
                                },
                            )
                        }

                        // Working Hours
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Border
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                IconBadge(
                                    icon = Icons.Filled.Schedule,
                                    size = 48.dp,
                                    iconSize = 24.dp,
                                )
                                Column {
                                    Text(
                                        "ساعات العمل",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "السبت - الخميس",
                                        color = MutedForeground,
                                        fontSize = 14.sp,
                                    )
                                    Text(
                                        "9:00 - 22:00",
                                        color = MutedForeground,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }

                    // ════════ FORM — lg:col-span-2 ════════
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Border
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Text(
                                "أرسل لنا رسالة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(24.dp))

                            // grid-cols-1 sm:grid-cols-2 gap-6 — عمودي على الموبايل
                            Column(
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "الاسم الكامل",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        placeholder = { Text("اسمك الكامل") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Accent,
                                            unfocusedBorderColor = Border,
                                            unfocusedContainerColor = MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                                .copy(alpha = 0.3f),
                                        ),
                                    )
                                }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "البريد الإلكتروني",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        placeholder = { Text("email@example.com") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Email
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Accent,
                                            unfocusedBorderColor = Border,
                                            unfocusedContainerColor = MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                                .copy(alpha = 0.3f),
                                        ),
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // Subject
                            Text(
                                "الموضوع",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = subject,
                                onValueChange = { subject = it },
                                placeholder = { Text("كيف يمكننا مساعدتك؟") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Accent,
                                    unfocusedBorderColor = Border,
                                    unfocusedContainerColor = MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                        .copy(alpha = 0.3f),
                                ),
                            )

                            Spacer(Modifier.height(24.dp))

                            // Message
                            Text(
                                "الرسالة",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                placeholder = { Text("اكتب رسالتك هنا...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Accent,
                                    unfocusedBorderColor = Border,
                                    unfocusedContainerColor = MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                        .copy(alpha = 0.3f),
                                ),
                            )

                            Spacer(Modifier.height(24.dp))

                            // Submit Button — py-6 = 48dp
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "تم إرسال رسالتك بنجاح، سنقوم بالرد عليك قريباً"
                                        )
                                    }
                                    name = ""
                                    email = ""
                                    subject = ""
                                    message = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accent,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    null,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "إرسال الرسالة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                )
                            }

                            Spacer(Modifier.height(24.dp))
                            Text(
                                "سنقوم بالرد على رسالتك في أسرع وقت ممكن",
                                color = MutedForeground,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  مكوّن ContactInfoCard — مطابق لـ Card في Contact.tsx
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ContactInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    iconBg: Color = Accent.copy(alpha = 0.1f),
    iconBorder: Color = Accent,
    iconTint: Color = Accent,
    valueColor: Color = Accent,
    onClick: (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(
                icon = icon,
                size = 48.dp,
                iconSize = 24.dp,
                bgColor = iconBg,
                borderColor = iconBorder,
                tint = iconTint,
            )
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    value,
                    color = if (onClick != null) valueColor else MutedForeground,
                    fontSize = 14.sp,
                    fontWeight = if (onClick != null) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Data classes مساعدة
// ═══════════════════════════════════════════════════════════════

private data class Quad(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
)

private data class TeamMember(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val description: String,
)

private data class ContactItem(
    val emoji: String,
    val label: String,
    val value: String,
    val onClick: (() -> Unit)?,
)