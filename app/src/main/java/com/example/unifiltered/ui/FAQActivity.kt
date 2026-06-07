package com.example.unifiltered.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.unifiltered.databinding.ActivityFaqBinding

class FAQActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaqBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}