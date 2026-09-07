package com.eleven.store.ui.components

// ✅ فُصل من MainActivity.kt — راجع AppHeader.kt لنفس السبب.

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.eleven.store.navigation.Route
import com.eleven.store.ui.icons.LucideIcons
import com.eleven.store.ui.theme.*
import com.eleven.store.ui.viewmodel.MainViewModel

// ═══════════════════════════════════════════════════════════════
//  DRAWER — القائمة الجانبية
// ═══════════════════════════════════════════════════════════════

@Composable
fun ElevenDrawer(
    user: com.google.firebase.auth.FirebaseUser?,
    navController: NavController,
    viewModel: MainViewModel,
    onClose: () -> Unit,
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val drawerWidth = if (screenWidthDp < 640) screenWidthDp.dp else 320.dp

    ModalDrawerSheet(
        modifier = Modifier.width(drawerWidth),
        drawerContainerColor = Background,
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Accent.copy(alpha = 0.1f), Accent.copy(alpha = 0.05f)),
                        ),
                    )
                    .padding(24.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "11",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 24.sp,
                        )
                    }
                    Column {
                        Text(
                            text = "ELEVEN",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = user?.email ?: "أهلاً بك في Eleven",
                            color = MutedForeground,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            HorizontalDivider(color = Border, thickness = 1.dp)

            // Navigation Links
            val navLinks = listOf(
                Triple(LucideIcons.Home, "الرئيسية", Route.HOME),
                Triple(LucideIcons.Heart, "المفضلة", Route.FAVORITES),
                Triple(LucideIcons.ShoppingBag, "طلباتي", Route.ORDERS),
                Triple(LucideIcons.User, "الملف الشخصي", Route.PROFILE),
                Triple(LucideIcons.Bell, "الإشعارات", Route.NOTIFICATIONS),
                Triple(LucideIcons.Info, "الإعدادات", Route.SETTINGS), // مطابق للموقع: Header.tsx يستخدم أيقونة Info لـ"الإعدادات" أيضاً (نفس أيقونة "حول")
                Triple(LucideIcons.Phone, "اتصل بنا", Route.CONTACT),
                Triple(LucideIcons.Info, "حول", Route.ABOUT),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                navLinks.forEach { (icon, label, route) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                navController.navigate(route) {
                                    popUpTo(Route.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                onClose()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Foreground,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Foreground,
                        )
                    }
                }
            }

            // Footer
            HorizontalDivider(color = Border, thickness = 1.dp)

            Column(modifier = Modifier.padding(16.dp)) {
                if (user != null) {
                    OutlinedButton(
                        onClick = {
                            viewModel.logout()
                            navController.navigate(Route.HOME) {
                                popUpTo(0) { inclusive = true }
                            }
                            onClose()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Destructive.copy(alpha = 0.3f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Destructive,
                        ),
                    ) {
                        Icon(
                            LucideIcons.LogOut,
                            null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("تسجيل الخروج", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            navController.navigate(Route.LOGIN)
                            onClose()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    ) {
                        Text(
                            "تسجيل الدخول",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "ELEVEN STORE",
                    color = MutedForeground.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}