package com.eleven.store.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

// ═══════════════════════════════════════════════════════════════
//  CONNECTIVITY OBSERVER
//  ✅ جديد: يراقب حالة الاتصال بالإنترنت لحظياً (لا مجرد فحص لمرة واحدة)
//  عبر NetworkCallback الرسمي من ConnectivityManager. مصدر واحد يُستخدم
//  في كل التطبيق (بانر عام + أي شاشة تحتاج تمييز "بلا إنترنت" عن أي
//  فشل آخر).
// ═══════════════════════════════════════════════════════════════

/**
 * ✅ إصلاح (تفاعل وهمي بلا إنترنت): فحص فوري (لمرة واحدة، بلا Flow) لحالة
 * الاتصال الفعلية — يُستخدم قبل أي عملية كتابة حسّاسة (سلة/مفضلة/طلب/رفع
 * إيصال) لرفض المحاولة فوراً برسالة واضحة، بدل تركها لـ Firestore الذي
 * يكتب أي set()/update()/delete() إلى الكاش المحلي فوراً ويُبقيها "معلّقة"
 * صامتة (لا نجاح حقيقي ولا فشل ظاهر) حتى تعود الشبكة — وهو تحديداً سبب
 * شعور المستخدم أن الإضافة "نجحت" فعلاً بلا إنترنت. لا يغني هذا عن
 * NoInternetBanner (تنبيه دائم) بل يمنع تحديداً كل عملية كتابة من
 * الاستمرار بصمت أثناء الانقطاع.
 */
fun isDeviceOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

/** يرجع Flow<Boolean> يصدر true/false كلما تغيّرت حالة الاتصال الفعلية بالشبكة (لا مجرد "مسجَّل بشبكة" بل بها إنترنت فعلي قابل للتحقق) */
fun Context.observeIsOnline(): Flow<Boolean> = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun currentlyOnline(): Boolean = isDeviceOnline(this@observeIsOnline)

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(currentlyOnline()) }
        override fun onLost(network: Network) { trySend(currentlyOnline()) }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            trySend(currentlyOnline())
        }
    }

    trySend(currentlyOnline())

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    connectivityManager.registerNetworkCallback(request, callback)

    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()

/** Composable state: هل الجهاز متصل بالإنترنت الآن؟ يتحدّث تلقائياً لحظياً. */
@Composable
fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val state by produceState(initialValue = true, context) {
        context.observeIsOnline().collect { value = it }
    }
    return state
}

/**
 * بانر رفيع يظهر أعلى الشاشة تلقائياً عند فقدان الاتصال، ويختفي فوراً
 * عند عودته — بدون أي تدخّل من كل شاشة على حدة. يُدرَج مرة واحدة فقط
 * فوق NavHost في MainActivity.
 */
@Composable
fun NoInternetBanner() {
    val isOnline = rememberIsOnline()
    if (!isOnline) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDC2626))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier,
            )
            Text(
                "لا يوجد اتصال بالإنترنت",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
