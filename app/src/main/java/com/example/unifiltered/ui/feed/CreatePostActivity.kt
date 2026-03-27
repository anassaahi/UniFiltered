package com.example.unifiltered.ui.feed

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unifiltered.databinding.ActivityCreatePostBinding
import com.example.unifiltered.viewmodel.CreatePostState
import com.example.unifiltered.viewmodel.CreatePostViewModel
import kotlinx.coroutines.launch

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private val viewModel: CreatePostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Close button logic
        binding.btnClose.setOnClickListener {
            finish() // Closes this screen and goes back to the feed
        }

        // Post button logic
        binding.btnPost.setOnClickListener {
            val content = binding.etPostContent.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.createPost(content)
            } else {
                Toast.makeText(this, "Post cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe the state to show loading spinner or close on success
        lifecycleScope.launch {
            viewModel.postState.collect { state ->
                when (state) {
                    is CreatePostState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnPost.isEnabled = false
                    }
                    is CreatePostState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@CreatePostActivity, "Posted!", Toast.LENGTH_SHORT).show()
                        finish() // Head back to the feed automatically!
                    }
                    is CreatePostState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnPost.isEnabled = true
                        Toast.makeText(this@CreatePostActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> Unit
                }
            }
        }
    }
}