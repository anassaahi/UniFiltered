package com.example.unifiltered.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.unifiltered.model.Post
import com.example.unifiltered.model.Comment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

import com.google.firebase.firestore.FieldValue // NEW IMPORT
class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // Centralized Auth instance
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
    suspend fun toggleLike(postId: String, isCurrentlyLiked: Boolean): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not logged in")
            val postRef = db.collection("posts").document(postId)

            if (isCurrentlyLiked) {
                // If they already liked it, remove their ID (Unlike)
                postRef.update("likedBy", FieldValue.arrayRemove(currentUser.uid)).await()
            } else {
                // If they haven't liked it, add their ID (Like)
                postRef.update("likedBy", FieldValue.arrayUnion(currentUser.uid)).await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
                timestamp = System.currentTimeMillis()
                // We removed likesCount here.
                // The Post model will automatically set 'likedBy' to an empty list!
            )

            // Save it to Firestore
            db.collection("posts").document(postId).set(newPost).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // --- COMMENT FUNCTIONS ---

    // 1. Listen for comments on a specific post (Oldest first, like a normal chat)
    fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val subscription = db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val comments = snapshot.toObjects(com.example.unifiltered.model.Comment::class.java)
                    trySend(comments).isSuccess
                }
            }

        awaitClose { subscription.remove() }
    }

    // 2. Add a new comment to a specific post's sub-collection
    suspend fun addComment(postId: String, text: String): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not logged in")

            // Get the user's name again so it shows up on the comment
            val userDoc = db.collection("users").document(currentUser.uid).get().await()
            val authorName = userDoc.getString("name") ?: "Unknown Student"

            // Point to the sub-collection and generate a new unique ID
            val commentRef = db.collection("posts").document(postId).collection("comments").document()

            val newComment = com.example.unifiltered.model.Comment(
                commentId = commentRef.id,
                authorId = currentUser.uid,
                authorName = authorName,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            // Save the comment in the sub-collection
            commentRef.set(newComment).await()

            // NEW: Instantly add +1 to the parent Post's comment counter!
            db.collection("posts").document(postId).update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1)).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}