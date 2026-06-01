package com.example.unifiltered.repository

import com.example.unifiltered.model.Society
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SocietyRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun createSociety(name: String, description: String): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not logged in")

            // Generate a unique ID for the new society
            val societyRef = db.collection("societies").document()

            val newSociety = Society(
                societyId = societyRef.id,
                name = name,
                description = description,
                creatorId = currentUser.uid,
                isOfficial = false, // GATEKEEPING: Always false by default!
                followerCount = 0,
                createdAt = System.currentTimeMillis()
            )

            // Save to Firestore
            societyRef.set(newSociety).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Checks if the current user has this society ID in their 'followedSocieties' list
    suspend fun isUserFollowing(societyId: String): Boolean {
        return try {
            val uid = auth.currentUser?.uid ?: return false
            val userDoc = db.collection("users").document(uid).get().await()
            val followedList = userDoc.get("followedSocieties") as? List<String> ?: emptyList()
            followedList.contains(societyId)
        } catch (e: Exception) {
            false
        }
    }

    // Uses a Batch to safely update both the User and the Society at the same time
    suspend fun toggleFollow(societyId: String, isCurrentlyFollowing: Boolean): Result<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val userRef = db.collection("users").document(uid)
            val societyRef = db.collection("societies").document(societyId)

            db.runBatch { batch ->
                if (isCurrentlyFollowing) {
                    // Unfollow
                    batch.update(userRef, "followedSocieties", FieldValue.arrayRemove(societyId))
                    batch.update(societyRef, "followerCount", FieldValue.increment(-1))
                } else {
                    // Follow
                    batch.update(userRef, "followedSocieties", FieldValue.arrayUnion(societyId))
                    batch.update(societyRef, "followerCount", FieldValue.increment(1))
                }
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // --- ADMIN & VERIFICATION FUNCTIONS ---

    // 1. Student applies for the badge
    suspend fun submitVerificationRequest(societyId: String, societyName: String): Result<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
            val requestRef = db.collection("verification_requests").document()

            val request = com.example.unifiltered.model.VerificationRequest(
                requestId = requestRef.id,
                societyId = societyId,
                societyName = societyName,
                requesterId = uid,
                status = "pending",
                timestamp = System.currentTimeMillis()
            )

            requestRef.set(request).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. You (Admin) fetch all pending tickets
    suspend fun getPendingRequests(): Result<List<com.example.unifiltered.model.VerificationRequest>> {
        return try {
            val snapshot = db.collection("verification_requests")
                .whereEqualTo("status", "pending")
                .get().await()
            val requests = snapshot.toObjects(com.example.unifiltered.model.VerificationRequest::class.java)
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. You (Admin) approve a ticket!
    suspend fun approveSociety(requestId: String, societyId: String): Result<Boolean> {
        return try {
            val requestRef = db.collection("verification_requests").document(requestId)
            val societyRef = db.collection("societies").document(societyId)

            // Run a Batch to update both databases instantly
            db.runBatch { batch ->
                batch.update(requestRef, "status", "approved")
                batch.update(societyRef, "isOfficial", true)
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getMyOwnedSocieties(): Result<List<Society>> {

        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
            // Fetch only societies where creatorId matches the logged-in student
            val snapshot = db.collection("societies").whereEqualTo("creatorId", uid).get().await()
            val mySocieties = snapshot.toObjects(Society::class.java)
            Result.success(mySocieties)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    fun getAllSocieties(): Flow<List<Society>> = callbackFlow {
        val subscription = db.collection("societies")
            .orderBy("followerCount", com.google.firebase.firestore.Query.Direction.DESCENDING) // Most popular first!
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val societies = snapshot.toObjects(Society::class.java)
                    trySend(societies).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }
    fun getSocietyLive(societyId: String): Flow<com.example.unifiltered.model.Society?> = callbackFlow {
        val subscription = db.collection("societies").document(societyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val society = snapshot.toObject(com.example.unifiltered.model.Society::class.java)
                    trySend(society).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }
}