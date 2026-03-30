package com.example.unifiltered.ui.feed

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unifiltered.databinding.ActivityCreatePostBinding
import com.example.unifiltered.model.Society
import com.example.unifiltered.viewmodel.CreatePostState
import com.example.unifiltered.viewmodel.CreatePostViewModel
import kotlinx.coroutines.launch

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private val viewModel: CreatePostViewModel by viewModels()

    // NEW: Variables to track the dropdown state
    private var myOwnedSocieties: List<Society> = emptyList()
    private var selectedSociety: Society? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Close button logic
        binding.btnClose.setOnClickListener {
            finish() // Closes this screen and goes back to the feed
        }

        // NEW: Tell ViewModel to fetch the student's societies
        viewModel.loadMySocieties()

        // NEW: Observe societies and populate the dropdown menu
        lifecycleScope.launch {
            viewModel.mySocieties.collect { societies ->
                myOwnedSocieties = societies

                val options = mutableListOf("Post as Myself")
                options.addAll(societies.map { "Post as ${it.name}" })

                val adapter = ArrayAdapter(this@CreatePostActivity, android.R.layout.simple_dropdown_item_1line, options)
                binding.spinnerIdentity.setAdapter(adapter)
            }
        }

        // NEW: Listen for dropdown selection changes
        binding.spinnerIdentity.setOnItemClickListener { _, _, position, _ ->
            selectedSociety = if (position == 0) {
                null // Index 0 is "Post as Myself"
            } else {
                myOwnedSocieties[position - 1] // Subtract 1 because index 0 is "Myself"
            }
        }

        // Post button logic
        binding.btnPost.setOnClickListener {
            val content = binding.etPostContent.text.toString().trim()
            if (content.isNotEmpty()) {
                // UPDATED: Pass the selected society to the ViewModel
                viewModel.createPost(content, selectedSociety)
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