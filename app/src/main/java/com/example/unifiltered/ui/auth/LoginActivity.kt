package com.example.unifiltered.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unifiltered.databinding.ActivityLoginBinding
import com.example.unifiltered.ui.MainActivity
import com.example.unifiltered.viewmodel.AuthState
import com.example.unifiltered.viewmodel.AuthViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.google.firebase.auth.FirebaseAuth.getInstance().firebaseAuthSettings.forceRecaptchaFlowForTesting(true)
        // Navigate to Signup
        binding.tvSignupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString().trim()
            val pass = binding.etLoginPassword.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                authViewModel.login(email, pass)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
//        binding.btnLogin.setOnClickListener {
//            // Bypass Firebase Auth temporarily!
//            android.widget.Toast.makeText(this, "Skipping Login for Development!", android.widget.Toast.LENGTH_SHORT).show()
//            val intent = android.content.Intent(this@LoginActivity, com.example.unifiltered.ui.MainActivity::class.java)
//            startActivity(intent)
//            finish()
//        }

        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> binding.loginProgressBar.visibility = View.VISIBLE
                    is AuthState.Success -> {
                        binding.loginProgressBar.visibility = View.GONE
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is AuthState.Error -> {
                        binding.loginProgressBar.visibility = View.GONE
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> binding.loginProgressBar.visibility = View.GONE
                }
            }
        }
    }
}