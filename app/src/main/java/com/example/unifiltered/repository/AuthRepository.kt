package com.example.unifiltered.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.unifiltered.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Suspend function allows us to run this on a background thread using Coroutines
    suspend fun signUpUser(name: String, email: String, pass: String, role: String): Result<Boolean> {
        return try {
            // 1. Create the user in Firebase Authentication
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val userId = authResult.user?.uid

            if (userId != null) {
                // 2. Create the user profile object
                val newUser = User(uid = userId, name = name, email = email, role = role)

                // 3. Save the profile to Firestore in a "users" collection
                db.collection("users").document(userId).set(newUser).await()
                Result.success(true)
            } else {
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun loginUser(email: String, pass: String): Result<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // You will add loginUser() here later!
}