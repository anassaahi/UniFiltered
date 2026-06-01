package com.example.unifiltered.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.unifiltered.model.User
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withTimeout

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Suspend function allows us to run this on a background thread using Coroutines
    suspend fun signUpUser(name: String, email: String, pass: String, role: String): Result<Boolean> {
        return try {
            // Stop waiting if it takes longer than 8 seconds
            withTimeout(20000) {
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val userId = authResult.user?.uid

                if (userId != null) {
                    val newUser = User(uid = userId, name = name, email = email, role = role)
                    db.collection("users").document(userId).set(newUser).await()
                    Result.success(true)
                } else {
                    Result.failure(Exception("User creation failed: UID is null"))
                }
            }
        } catch (e: Exception) {
            Log.e("FIREBASE_AUTH_ERROR", "Sign Up Failed or Timed Out!", e)
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, pass: String): Result<Boolean> {
        return try {
            // Stop waiting if it takes longer than 8 seconds
            withTimeout(20000) {
                auth.signInWithEmailAndPassword(email, pass).await()
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("FIREBASE_AUTH_ERROR", "Login Failed or Timed Out!", e)
            Result.failure(e)
        }
    }

    // You will add loginUser() here later!
}