package com.eleven.store.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.eleven.store.MainActivity
import com.eleven.store.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth
import android.util.Log

/**
 * ELEVEN STORE — استقبال إشعارات الـPush (إعادة بناء كاملة)
 * ─────────────────────────────────────────────────────────
 * السيرفر يرسل رسائل "data-only" حصراً (بدون حقل notification أعلى
 * المستوى) — هذا شرط تقني إلزامي: أي حقل notification أعلى المستوى يجعل
 * النظام نفسه (وليس هذا الملف) يقرر عرض الإشعار عبر مسار عرض تلقائي، وقد لا
 * يستدعي onMessageReceived إطلاقاً على بعض الأجهزة/الأوضاع (Doze، توفير
 * الطاقة). لذلك: onMessageReceived هنا هو دائماً المسؤول الوحيد عن بناء
 * وعرض الإشعار، بنفس الشكل تماماً الذي يبنيه service worker على الموقع.
 */
class ElevenFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val notificationId = data["notificationId"]
        val title = data["title"] ?: "Eleven Store"
        val body = data["body"] ?: ""
        val type = data["type"] ?: "general"
        val actionRoute = data["actionRoute"]?.takeIf { it.isNotBlank() }

        showNotification(notificationId, title, body, type, actionRoute)

        // لا كتابة على "users/{uid}/notifications" من هنا عمداً — سجل
        // الإشعار مكتوب بالفعل قبل وصول هذا الـPush أصلاً (نواة notify() في
        // functions/src/lib/notifications.ts تكتب السجل أولاً ثم ترسل الـPush
        // بعد نجاح الكتابة)، فأي كتابة إضافية هنا تُنتج نسخة مكرَّرة صريحة.
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // set(merge) بدل update — update() يفشل بصمت إن لم تكن وثيقة
        // users/{uid} موجودة بعد وقت وصول التوكن (مثال: توكن يصل قبل اكتمال
        // إنشاء وثيقة الحساب)، فيضيع تسجيل هذا الجهاز نهائياً.
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .set(mapOf("fcmTokens" to com.google.firebase.firestore.FieldValue.arrayUnion(token)), SetOptions.merge())
            .addOnFailureListener { e -> Log.e("FCM", "فشل حفظ توكن الإشعارات: ${e.message}", e) }
    }

    private fun showNotification(notificationId: String?, title: String, body: String, type: String, actionRoute: String?) {
        // نفس معرّف القناة المُنشأة مسبقاً بـ ElevenStoreApp عند إقلاع
        // التطبيق (وليس معرّفاً حرفياً مكرراً هنا) — قناة واحدة موحّدة
        // للتطبيق كله، تُنشأ مرة واحدة بدل كل مرة يصل فيها إشعار.
        val channelId = getString(R.string.default_notification_channel_id)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // "order/abc123" (Route.ORDER_DETAIL بالضبط) — نفس تنسيق actionRoute
        // المُرسل من السيرفر بعد إزالة الشرطة المائلة الأولى، فيُفتح مباشرة
        // نفس المسار الذي يفتحه الرابط المكافئ على الموقع (/order/abc123).
        val navRoute = actionRoute?.removePrefix("/")

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!navRoute.isNullOrBlank()) {
                putExtra(MainActivity.NOTIFICATION_ROUTE_EXTRA, navRoute)
            }
        }
        // ✅ إصلاح تكرار العرض: المعرّف السابق كان مبنياً من
        // (العنوان+النص+الوقت الحالي).hashCode() — أي فريد دوماً حتى لإعادة
        // تسليم نفس الحدث بالضبط من FCM (نادر لكن وارد على مستوى الشبكة)،
        // فيظهران كإشعارين منفصلين بقائمة النظام رغم كونهما نفس الحدث. الآن:
        // notificationId قادم من السيرفر (معرّف حتمي = sha1 لمفتاح الحدث نفسه،
        // راجع functions/src/lib/notifications.ts) فتُستبدَل أي إعادة تسليم
        // بنفس الإشعار المعروض بدل تكديس نسخة ثانية. عند غيابه (احتياطي فقط
        // لتوافق نسخ قديمة من السيرفر) نعود لمعرّف عشوائي كسابقاً.
        val stableId = notificationId?.hashCode() ?: (title + body + System.currentTimeMillis()).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            this, stableId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup("eleven_store_$type")
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(stableId, notification)
    }
}
