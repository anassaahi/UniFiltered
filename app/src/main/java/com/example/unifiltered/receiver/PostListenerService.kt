package com.example.unifiltered.receiver

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.unifiltered.model.Post
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PostListenerService : Service() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var sender: PostBroadcastSender
    private var isFirstLoad = true

    override fun onCreate() {
        super.onCreate()
        sender = PostBroadcastSender(this)
        startListening()
    }

    private fun startListening() {
        // Listen for new documents in the "posts" collection
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                // We only want to notify for NEW posts added after the app starts listening
                if (isFirstLoad) {
                    isFirstLoad = false
                    return@addSnapshotListener
                }

                for (dc in snapshots.documentChanges) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val post = dc.document.toObject(Post::class.java)
                        sender.sendPostNotification(post.authorName ?: "A user")
                    }
                }
            }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
