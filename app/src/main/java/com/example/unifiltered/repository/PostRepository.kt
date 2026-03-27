package com.example.unifiltered.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.unifiltered.model.Post
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
class PostRepository {
    private val db = FirebaseFirestore.getInstance()

    // callbackFlow converts Firebase's real-time listener into a Kotlin Flow
    fun getAllPosts(): Flow<List<Post>> = callbackFlow {
        // Look in the "posts" collection and order by newest first
        val subscription = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Convert the raw Firestore data into our Post Kotlin objects
                    val posts = snapshot.toObjects(Post::class.java)
                    trySend(posts).isSuccess
                }
            }

        // Clean up the listener if the user leaves the screen
        awaitClose { subscription.remove() }
    }
    suspend fun createPost(content: String): Result<Boolean> {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) return Result.failure(Exception("User not logged in"))

            // Fetch the user's name from the 'users' collection so we can attach it to the post
            val userDoc = db.collection("users").document(currentUser.uid).get().await()
            val authorName = userDoc.getString("name") ?: "Unknown Student"

            // Generate a unique ID for the new post
            val postId = db.collection("posts").document().id

            val newPost = Post(
                postId = postId,
                authorId = currentUser.uid,
                authorName = authorName,
                content = content,
                timestamp = System.currentTimeMillis(),
                likesCount = 0
            )

            // Save it to Firestore
            db.collection("posts").document(postId).set(newPost).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}