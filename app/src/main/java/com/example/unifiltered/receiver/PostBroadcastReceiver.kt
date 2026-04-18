package com.example.unifiltered.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.unifiltered.R

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

        // Build the notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using a system icon for reliability
            .setContentTitle("UniFiltered: New Post")
            .setContentText("Hey! $authorName just shared a new post!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Show the notification
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    companion object {
        const val ACTION_POST_CREATED = "com.example.unifiltered.ACTION_POST_CREATED"
    }
}
