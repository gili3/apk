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
        val title = data["title"] ?: "Eleven Store"
        val body = data["body"] ?: ""
        val type = data["type"] ?: "general"
        val actionRoute = data["actionRoute"]?.takeIf { it.isNotBlank() }

        showNotification(title, body, type, actionRoute)

        // لا كتابة على "users/{uid}/notifications" من هنا عمداً — السيرفر هو
        // من يكتب هذا السجل عند إرسال الـPush (notification-service.ts::
        // notifyUser)، فكتابة إضافية هنا تُنتج نسخة مكرَّرة من كل إشعار.
        // مصدر الحقيقة الوحيد لسجلّات الإشعارات هو السيرفر (أو placeOrder
        // المحلي عند الشراء المباشر من التطبيق نفسه — راجع FirestoreRepository).
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

    private fun showNotification(title: String, body: String, type: String, actionRoute: String?) {
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
        val requestCode = (title + body + System.currentTimeMillis()).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            this, requestCode, intent,
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

        // معرّف فريد لكل إشعار (بدل معرّف ثابت واحد) — إشعارات متعددة تتراكم
        // بقائمة النظام بدل أن يستبدل كل إشعار جديد سابقه فوراً.
        notificationManager.notify(requestCode, notification)
    }
}
