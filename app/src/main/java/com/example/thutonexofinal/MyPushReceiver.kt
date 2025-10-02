package com.example.thutonexofinal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class MyPushReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras ?: return
        val message = extras.getString("message") ?: ""
        val chatId = extras.getString("chatId") ?: return
        val senderId = extras.getString("senderId") ?: return
        val senderName = extras.getString("senderName") ?: "New Message"

        // Suppress notification if user is in this chat
        val prefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
        val currentChatId = prefs.getString("current_chat_id", null)
        if (currentChatId == chatId) return

        val chatIntent = Intent(context, ChatActivity::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("receiverId", senderId)
            putExtra("receiverName", senderName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            chatIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "pushy_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pushy Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logothutonexo)
            .setContentTitle(senderName) // now shows sender
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(chatId.hashCode(), notification)
    }
}


//package com.example.thutonexofinal
//
//import android.Manifest
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.content.Context
//import android.content.Intent
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import me.pushy.sdk.Pushy
//import android.content.BroadcastReceiver
//import android.os.Build
//import androidx.annotation.RequiresPermission
//import java.util.Random
//
//
//class MyPushReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        val data = intent.getBundleExtra("notification") ?: return
//
//        val message = data.getString("message") ?: "New message"
//        val sender = data.getString("sender") ?: "Unknown"
//        val chatId = data.getString("chatId")
//
//        // Check if user is already inside this chat
//        val activeChatId = context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
//            .getString("activeChatId", null)
//
//        if (activeChatId != null && activeChatId == chatId) {
//            // Already viewing chat → don’t show notification
//            return
//        }
//
//        val channelId = "chat_messages"
//        val notificationManager =
//            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Chat Messages",
//                NotificationManager.IMPORTANCE_HIGH
//            )
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        val notification = NotificationCompat.Builder(context, channelId)
//            .setContentTitle("Message from $sender")
//            .setContentText(message)
//            .setSmallIcon(R.drawable.ic_notification) // make sure this icon exists
//            .setAutoCancel(true)
//            .build()
//
//        notificationManager.notify(Random().nextInt(), notification)
//    }
//}
//
////class MyPushReceiver : BroadcastReceiver() {
////    override fun onReceive(context: Context, intent: Intent) {
////        val data = intent.getBundleExtra("notification") ?: return
////
////        // Example payload fields
////        val message = data.getString("message") ?: "New message"
////        val sender = data.getString("sender") ?: "Unknown"
////
////        // Show notification
////        val channelId = "chat_messages"
////        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
////
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
////            val channel = NotificationChannel(channelId, "Chat Messages", NotificationManager.IMPORTANCE_HIGH)
////            notificationManager.createNotificationChannel(channel)
////        }
////
////        val notification = NotificationCompat.Builder(context, channelId)
////            .setContentTitle("Message from $sender")
////            .setContentText(message)
////            .setSmallIcon(R.drawable.ic_notification) // make sure this exists
////            .setAutoCancel(true)
////            .build()
////
////        notificationManager.notify(Random().nextInt(), notification)
////    }
////}
//
//
////class MyPushReceiver : BroadcastReceiver() {
////
////    override fun onReceive(context: Context, intent: Intent) {
////        val title = intent.getStringExtra("title") ?: "New Message"
////        val body = intent.getStringExtra("body") ?: ""
////
////        // Android 13+ check for notification permission
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
////            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
////            != android.content.pm.PackageManager.PERMISSION_GRANTED
////        ) {
////            // Permission not granted, skip notification
////            return
////        }
////
////        val notification = NotificationCompat.Builder(context, "chat_channel")
////            .setSmallIcon(R.drawable.logothutonexo)
////            .setContentTitle(title)
////            .setContentText(body)
////            .setPriority(NotificationCompat.PRIORITY_HIGH)
////            .setAutoCancel(true)
////            .build()
////
////        val notificationId = (System.currentTimeMillis() % 10000).toInt()
////        NotificationManagerCompat.from(context).notify(notificationId, notification)
////    }
////}
//
////class MyPushReceiver : BroadcastReceiver() { // correct class
////    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
////    override fun onReceive(context: Context, intent: Intent) {
////        val title = intent.getStringExtra("title") ?: "New Message"
////        val body = intent.getStringExtra("body") ?: ""
////
////        // Build the notification
////        val notification = NotificationCompat.Builder(context, "chat_channel")
////            .setSmallIcon(R.drawable.logothutonexo) // replace with your icon
////            .setContentTitle(title)
////            .setContentText(body)
////            .setPriority(NotificationCompat.PRIORITY_HIGH)
////            .setAutoCancel(true)
////            .build()
////
////        NotificationManagerCompat.from(context).notify(
////            (System.currentTimeMillis() % 10000).toInt(),
////            notification
////        )
////    }
////}
//
////
////import android.content.BroadcastReceiver
////import android.content.Context
////import android.content.Intent
////import android.widget.Toast
////import me.pushy.sdk.Pushy
////import android.app.NotificationManager
////import androidx.core.app.NotificationCompat
////import android.content.Context.NOTIFICATION_SERVICE
////import androidx.core.app.NotificationManagerCompat
////
////class MyPushReceiver : BroadcastReceiver() {
////    override fun onReceive(context: Context, intent: Intent) {
////        // Get notification payload
////        val payload = intent.getStringExtra("message")
////
////        // Show a toast
////        Toast.makeText(context, "New message: $payload", Toast.LENGTH_LONG).show()
////
//////        // Build a system notification
//////        val notification = NotificationCompat.Builder(context, "chat_channel")
//////            .setSmallIcon(R.drawable.ic_message)
//////            .setContentTitle(title) // sender’s name
//////            .setContentText(body)   // actual message
//////            .setPriority(NotificationCompat.PRIORITY_HIGH)
//////            .build()
//////
//////        NotificationManagerCompat.from(context).notify(1, notification)
////
////        // TODO: You can instead build a Notification here to show in status bar
////        // Example:
////        /*
////        val notification = NotificationCompat.Builder(context, "chat_channel")
////            .setSmallIcon(R.drawable.ic_message)
////            .setContentTitle("New Message")
////            .setContentText(payload)
////            .setPriority(NotificationCompat.PRIORITY_HIGH)
////            .build()
////
////        NotificationManagerCompat.from(context).notify(1, notification)
////        */
////    }
////}