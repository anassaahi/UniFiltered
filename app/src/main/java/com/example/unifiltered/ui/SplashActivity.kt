package com.example.unifiltered.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.unifiltered.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    // A flag to tell the system when it's safe to dismiss its splash screen
    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install the splash screen BEFORE super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 2. Do NOT call setContentView()!
        // We do not want to load activity_splash.xml at all.

        // 3. Tell the system to keep showing its splash screen until isReady becomes true
        splashScreen.setKeepOnScreenCondition { !isReady }

        // 4. Run your timer and logic
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }

            // Tell the system we are done, and close this activity
            isReady = true
            finish()
        }, 1500)
    }
}