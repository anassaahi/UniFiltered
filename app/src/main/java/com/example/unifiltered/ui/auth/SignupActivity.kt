package com.example.unifiltered.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unifiltered.databinding.ActivitySignupBinding
import com.example.unifiltered.ui.MainActivity
import com.example.unifiltered.viewmodel.AuthState
import com.example.unifiltered.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.google.firebase.auth.FirebaseAuth.getInstance().firebaseAuthSettings.forceRecaptchaFlowForTesting(true)
        // Navigate to Login
        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            val role = if (binding.radioAdmin.isChecked) "admin" else "student"

            if (name.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                authViewModel.signUp(name, email, pass, role)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> binding.progressBar.visibility = View.VISIBLE
                    is AuthState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                        finish()
                    }
                    is AuthState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@SignupActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
}