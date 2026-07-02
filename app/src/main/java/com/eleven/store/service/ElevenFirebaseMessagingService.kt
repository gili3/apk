package com.eleven.store.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.eleven.store.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class ElevenFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Eleven Store"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""

        showNotification(title, body)

        // Save to Firestore notifications collection
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("notifications")
            .add(
                mapOf(
                    "title" to title,
                    "body" to body,
                    "type" to (remoteMessage.data["type"] ?: "general"),
                    "isRead" to false,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
            )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // ✅ نفس الإصلاح: "fcmTokens" (مصفوفة)، وليس "fcmToken" — يطابق ما يقرأه السيرفر بالموقع
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token))
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "eleven_store_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Eleven Store",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "إشعارات متجر Eleven"
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
