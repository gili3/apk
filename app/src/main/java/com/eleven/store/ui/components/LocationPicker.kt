package com.eleven.store.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.eleven.store.ui.theme.Accent
import com.eleven.store.ui.theme.Border
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

// افتراضي: مركز الخرطوم — يُستخدم فقط لو ما فيه إحداثيات محفوظة ولا صلاحية موقع
private val DEFAULT_LOCATION = LatLng(15.5007, 32.5599)

// ✅ Geocoding عكسي عبر android.location.Geocoder المدمج بالنظام (مجاني، بلا
// أي تكلفة أو مفتاح API إضافي) — يدعم الواجهة الحديثة (API 33+) القائمة على
// Listener، مع رجوع للنسخة القديمة المتزامنة بالإصدارات الأقدم (minSdk 24).
private fun reverseGeocode(
    context: android.content.Context,
    lat: Double,
    lng: Double,
    onResult: (city: String, addressLine: String) -> Unit,
) {
    if (!android.location.Geocoder.isPresent()) return
    val geocoder = android.location.Geocoder(context, java.util.Locale("ar"))
    fun handle(list: List<android.location.Address>?) {
        val a = list?.firstOrNull() ?: return
        val city = a.locality ?: a.subAdminArea ?: a.adminArea ?: ""
        val line = listOfNotNull(a.subLocality, a.thoroughfare).filter { it.isNotBlank() }
            .joinToString("، ").ifBlank { a.getAddressLine(0) ?: "" }
        onResult(city, line)
    }
    try {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocation(lat, lng, 1) { handle(it) }
        } else {
            @Suppress("DEPRECATION")
            handle(geocoder.getFromLocation(lat, lng, 1))
        }
    } catch (_: Exception) {
        // لا اتصال بخدمة Geocoding بالجهاز — نتجاهل بصمت، الإحداثيات نفسها كافية
    }
}

/**
 * ✅ جديد: اختيار موقع عنوان التوصيل من خريطة جوجل.
 * - دبوس ثابت بمنتصف الخريطة (نمط Uber/كريم) يتحرك المستخدم بسحب الخريطة تحته.
 * - زر "موقعي الحالي" يطلب صلاحية الموقع ثم يجلب الإحداثيات عبر FusedLocationProvider.
 * - onLocationSelected يُستدعى بكل تحريك للخريطة بإحداثيات مركزها الحالي.
 * - onAddressResolved يُستدعى بعنوان تقريبي (Geocoding عكسي مجاني) لتعبئة الحقول النصية.
 */
@Composable
fun LocationPicker(
    initialLatitude: Double,
    initialLongitude: Double,
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
    // ✅ جديد: يُستدعى بعنوان نصي تقريبي (حي/مدينة) بعد Geocoding عكسي —
    // اختياري، يُستخدم لتعبئة حقلي "المدينة"/"العنوان" تلقائياً لو كانا
    // فارغين فقط (بدون الكتابة فوق ما كتبه المستخدم بنفسه)
    onAddressResolved: (city: String, addressLine: String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val startLatLng = remember {
        if (initialLatitude != 0.0 || initialLongitude != 0.0)
            LatLng(initialLatitude, initialLongitude)
        else DEFAULT_LOCATION
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLatLng, 15f)
    }

    var isLocating by remember { mutableStateOf(false) }
    // ✅ إصلاح الثغرة: LaunchedEffect(cameraPositionState.isMoving) كان يُطلَق
    // فوراً عند أول رسم للخريطة (isMoving تبدأ false)، فيُبلَّغ الدبوس الافتراضي
    // (مركز الخرطوم عند عدم وجود صلاحية GPS ولا إحداثيات محفوظة) وكأنه موقع
    // اختاره المستخدم فعلياً — بلا أي سحب حقيقي للخريطة. هذا العلم يمنع ذلك:
    // لا يُعتبر "تم اختيار موقع" إلا بعد أن تتحرك الخريطة فعلياً مرة واحدة على
    // الأقل (سحب المستخدم، أو تحريك برمجي عبر moveToCurrentLocation/animate)
    // ثم تتوقف — أول رسم للخريطة لا يُحسب حركة إطلاقاً.
    var hasMapMoved by remember { mutableStateOf(false) }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    val activity = context as? android.app.Activity

    // يُنشئ عميل الموقع مرة واحدة
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun moveToCurrentLocation() {
        isLocating = true
        try {
            val cancellationSource = com.google.android.gms.tasks.CancellationTokenSource()
            val loc = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationSource.token,
            ).await()
            if (loc != null) {
                val target = LatLng(loc.latitude, loc.longitude)
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 16f))
                onLocationSelected(target.latitude, target.longitude)
            } else {
                permissionDeniedMessage = "تعذّر تحديد موقعك الحالي، تأكد من تفعيل GPS"
            }
        } catch (e: Exception) {
            permissionDeniedMessage = "تعذّر تحديد موقعك الحالي، تأكد من تفعيل GPS"
        } finally {
            isLocating = false
        }
    }

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            permanentlyDenied = false
            permissionDeniedMessage = null
            scope.launch { moveToCurrentLocation() }
        } else {
            // ✅ لو النظام ما عاد يعرض تبرير الطلب (Rationale) بعد الرفض، فهذا
            // يعني غالباً "لا تسأل مرة ثانية" — نوجّه المستخدم لإعدادات التطبيق
            // مباشرة بدل تكرار طلب صلاحية لن يظهر أصلاً.
            val showRationale = activity?.let {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    it, Manifest.permission.ACCESS_FINE_LOCATION,
                )
            } ?: true
            permanentlyDenied = !showRationale
            permissionDeniedMessage = if (permanentlyDenied)
                "رفضت صلاحية الموقع بشكل دائم — فعّلها من إعدادات التطبيق لاستخدام تحديد الموقع"
            else
                "يحتاج التطبيق صلاحية الموقع لتحديد عنوانك تلقائياً"
        }
    }

    // أول ما تُفتح الخريطة ولا يوجد إحداثيات محفوظة مسبقاً: نحاول جلب الموقع
    // الحالي تلقائياً لو الصلاحية ممنوحة أصلاً (بدون إزعاج المستخدم بطلبها).
    LaunchedEffect(Unit) {
        if ((initialLatitude == 0.0 && initialLongitude == 0.0) && hasLocationPermission()) {
            moveToCurrentLocation()
        } else if (initialLatitude != 0.0 || initialLongitude != 0.0) {
            onLocationSelected(initialLatitude, initialLongitude)
        }
    }

    Column(modifier = modifier) {
        Text(
            "حدّد موقعك على الخريطة",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                ),
                onMapClick = { latLng -> onLocationSelected(latLng.latitude, latLng.longitude) },
            )

            // دبوس ثابت بمنتصف الخريطة — يمثّل نقطة التوصيل الحالية
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = "موقع التوصيل",
                tint = Accent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .offset(y = (-20).dp),
            )

            // زر "موقعي الحالي"
            FilledIconButton(
                onClick = {
                    if (hasLocationPermission()) {
                        scope.launch { moveToCurrentLocation() }
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White),
            ) {
                if (isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Accent)
                } else {
                    Icon(Icons.Filled.MyLocation, contentDescription = "موقعي الحالي", tint = Accent)
                }
            }
        }

        // نُحدِّث الإحداثيات عند توقف الخريطة عن الحركة (سحب المستخدم للدبوس الثابت)
        // — فقط إن كانت هناك حركة فعلية سابقة (راجع تعليق hasMapMoved أعلاه)،
        // وإلا فأول تركيب (composition) للخريطة كان يُبلَّغ خطأً كاختيار حقيقي.
        LaunchedEffect(cameraPositionState.isMoving) {
            if (cameraPositionState.isMoving) {
                hasMapMoved = true
            } else if (hasMapMoved) {
                val target = cameraPositionState.position.target
                onLocationSelected(target.latitude, target.longitude)
                reverseGeocode(context, target.latitude, target.longitude, onAddressResolved)
            }
        }

        permissionDeniedMessage?.let { msg ->
            Spacer(Modifier.height(4.dp))
            Text(msg, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            if (permanentlyDenied) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = {
                        val uri = android.net.Uri.fromParts("package", context.packageName, null)
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri,
                        )
                        runCatching { context.startActivity(intent) }
                    },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("فتح إعدادات التطبيق", fontSize = 12.sp, color = Accent)
                }
            }
        }
    }
}
