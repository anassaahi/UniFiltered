package com.example.unifiltered.receiver

import android.content.Context
import android.content.Intent

class PostBroadcastSender(private val context: Context) {

    fun sendPostNotification(authorName: String) {
        val intent = Intent(PostBroadcastReceiver.ACTION_POST_CREATED).apply {
            // Ensure only our app receives this broadcast
            `package` = context.packageName
            putExtra("authorName", authorName)
        }
        context.sendBroadcast(intent)
    }
}
