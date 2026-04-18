package com.example.unifiltered.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.unifiltered.R
import com.example.unifiltered.ui.MainActivity // Make sure this import points to your main app screen

class PostBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Check if the action matches our custom post action
        if (intent.action == ACTION_POST_CREATED) {
            val authorName = intent.getStringExtra("authorName") ?: "A user"
            showNotification(context, authorName)
        }
    }

    private fun showNotification(context: Context, authorName: String) {
        val channelId = "post_notifications"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "New Post Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications sent when someone creates a post"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // ==========================================
        // NEW: Create the Intent to open the app
        // ==========================================
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            // These flags ensure that tapping the notification doesn't create duplicate instances of your app
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Wrap it in a PendingIntent.
        // FLAG_IMMUTABLE is strictly required on modern Android versions (Android 12+) for security.
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Consider changing this to your @drawable/ic_launcher_foreground eventually!
            .setContentTitle("UniFiltered: New Post")
            .setContentText("Hey! $authorName just shared a new post!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // <--- THIS ATTACHES THE ACTION
            .setAutoCancel(true) // This makes the notification dismiss itself after it is tapped

        // Show the notification
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    companion object {
        const val ACTION_POST_CREATED = "com.example.unifiltered.ACTION_POST_CREATED"
    }
}