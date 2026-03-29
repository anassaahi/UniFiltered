package com.example.unifiltered.ui.feed

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unifiltered.databinding.ActivityPostDetailBinding
import com.example.unifiltered.viewmodel.PostDetailViewModel
import kotlinx.coroutines.launch

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private val viewModel: PostDetailViewModel by viewModels()
    private lateinit var commentAdapter: CommentAdapter
    private var currentPostId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Get the data passed from the Feed
        currentPostId = intent.getStringExtra("POST_ID") ?: ""
        val authorName = intent.getStringExtra("AUTHOR_NAME") ?: "Unknown"
        val content = intent.getStringExtra("CONTENT") ?: ""
        val likesCount = intent.getIntExtra("LIKES_COUNT", 0)

        // 2. Set the Original Post UI
        binding.tvDetailAuthorName.text = authorName
        binding.tvDetailContent.text = content
        binding.tvDetailLikes.text = "$likesCount Likes"

        binding.btnBack.setOnClickListener { finish() }

        // 3. Set up the Comments section
        setupRecyclerView()
        observeComments()

        // Tell the ViewModel to start listening to Firestore for this specific post
        if (currentPostId.isNotEmpty()) {
            viewModel.loadComments(currentPostId)
        }

        // 4. Handle Sending a new Comment
        binding.btnSendComment.setOnClickListener {
            val commentText = binding.etCommentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                viewModel.addComment(currentPostId, commentText)
                binding.etCommentInput.text.clear() // Clear the typing box instantly
            } else {
                Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter()
        binding.recyclerViewComments.apply {
            adapter = commentAdapter
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
        }
    }

    private fun observeComments() {
        lifecycleScope.launch {
            viewModel.comments.collect { commentList ->
                commentAdapter.submitList(commentList)

                // Automatically scroll to the bottom when a new comment is added
                if (commentList.isNotEmpty()) {
                    binding.recyclerViewComments.smoothScrollToPosition(commentList.size - 1)
                }
            }
        }
    }
}