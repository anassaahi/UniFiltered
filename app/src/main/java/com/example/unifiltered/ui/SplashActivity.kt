package com.example.unifiltered.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.unifiltered.R
import com.example.unifiltered.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Give the splash screen 1.5 seconds to shine, then check login status
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Already logged in! Go straight to the feed.
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // Not logged in. Go to the login screen.
                startActivity(Intent(this, LoginActivity::class.java)) // Change to your actual Login Activity name if different
            }
            finish() // Destroy the splash screen so the back button doesn't return to it
        }, 1500)
    }
}