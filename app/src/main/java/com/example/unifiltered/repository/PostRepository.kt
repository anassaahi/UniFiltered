package com.example.unifiltered.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.unifiltered.model.Post
import com.example.unifiltered.model.Comment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue

class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // Centralized Auth instance
    private val storage = FirebaseStorage.getInstance() // Firebase Storage instance

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

    suspend fun createPost(
        content: String,
        societyId: String? = null,
        societyName: String? = null,
        imageUri: Uri? = null
    ): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not logged in")

            val finalAuthorName = if (societyName != null) {
                societyName
            } else {
                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                userDoc.getString("name") ?: "Unknown Student"
            }

            // THE FIX: Check raw fields to bypass legacy data typos!
            var absoluteOfficialStatus = false
            if (societyId != null) {
                val socDoc = db.collection("societies").document(societyId).get().await()
                absoluteOfficialStatus = socDoc.getBoolean("isOfficial")
                    ?: socDoc.getBoolean("official")
                            ?: false
            }

            val postId = db.collection("posts").document().id

            // NEW: Upload image if provided
            var imageUrl = ""
            if (imageUri != null) {
                val fileName = "post_images/${currentUser.uid}_${postId}_${System.currentTimeMillis()}.jpg"
                val storageRef = storage.reference.child(fileName)
                
                storageRef.putFile(imageUri).await()
                imageUrl = storageRef.downloadUrl.await().toString()
            }

            val newPost = com.example.unifiltered.model.Post(
                postId = postId,
                authorId = currentUser.uid,
                authorName = finalAuthorName,
                content = content,
                timestamp = System.currentTimeMillis(),
                imageUrl = imageUrl,
                postedAsSocietyId = societyId,
                isOfficialSocietyPost = absoluteOfficialStatus
            )

            db.collection("posts").document(postId).set(newPost).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMyPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val subscription = db.collection("posts")
            .whereEqualTo("authorId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                    // Sort locally so the newest posts are at the top
                    trySend(posts.sortedByDescending { it.timestamp }).isSuccess
                }
            }
        // THE FIX: Removed the long prefix here!
        awaitClose { subscription.remove() }
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
            db.collection("posts").document(postId).update("commentsCount", FieldValue.increment(1)).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPostsForSociety(societyId: String): Flow<List<Post>> = callbackFlow {
        val subscription = db.collection("posts")
            .whereEqualTo("postedAsSocietyId", societyId)
            // Note: We sort them locally in the ViewModel to avoid forcing you to create a composite index in Firebase right now!
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                    trySend(posts).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun deletePost(postId: String, imageUrl: String?): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not logged in")
            
            // 1. Verify ownership (optional but recommended)
            val postDoc = db.collection("posts").document(postId).get().await()
            val authorId = postDoc.getString("authorId")
            
            if (authorId != currentUser.uid) {
                throw Exception("You can only delete your own posts")
            }

            // 2. Delete image from Storage if it exists
            if (!imageUrl.isNullOrEmpty()) {
                try {
                    storage.getReferenceFromUrl(imageUrl).delete().await()
                } catch (e: Exception) {
                    // Log error but continue deleting the post from Firestore
                    println("Error deleting image: ${e.message}")
                }
            }

            // 3. Delete the post from Firestore
            db.collection("posts").document(postId).delete().await()
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}