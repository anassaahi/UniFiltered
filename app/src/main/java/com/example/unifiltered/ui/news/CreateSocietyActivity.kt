package com.example.unifiltered.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unifiltered.databinding.ActivityCreateSocietyBinding
import com.example.unifiltered.repository.SocietyRepository
import kotlinx.coroutines.launch

class CreateSocietyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateSocietyBinding
    private val repository = SocietyRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateSocietyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSubmitSociety.setOnClickListener {
            val name = binding.etSocietyName.text.toString().trim()
            val bio = binding.etSocietyBio.text.toString().trim()

            if (name.isNotEmpty() && bio.isNotEmpty()) {
                // Disable button to prevent double-clicking
                binding.btnSubmitSociety.isEnabled = false
                binding.btnSubmitSociety.text = "Creating..."

                lifecycleScope.launch {
                    val result = repository.createSociety(name, bio)
                    if (result.isSuccess) {
                        Toast.makeText(this@CreateSocietyActivity, "Society Created!", Toast.LENGTH_SHORT).show()
                        finish() // Close the screen and go back
                    } else {
                        Toast.makeText(this@CreateSocietyActivity, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        binding.btnSubmitSociety.isEnabled = true
                        binding.btnSubmitSociety.text = "Create Society"
                    }
                }
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}