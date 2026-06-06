package com.example.unifiltered.ui.feed

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.unifiltered.databinding.ActivityPostDetailBinding
import com.example.unifiltered.viewmodel.PostDetailViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private val viewModel: PostDetailViewModel by viewModels()
    private lateinit var commentAdapter: CommentAdapter
    private var currentPostId: String = ""
    private val db = FirebaseFirestore.getInstance()

    // NEW: Keep track of who we are replying to. Null means replying to the main post.
    private var replyingToCommentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentPostId = intent.getStringExtra("POST_ID") ?: ""
        val authorName = intent.getStringExtra("AUTHOR_NAME") ?: "Unknown"
        val content = intent.getStringExtra("CONTENT") ?: ""
        val likesCount = intent.getIntExtra("LIKES_COUNT", 0)
        val imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""

        binding.tvDetailAuthorName.text = authorName
        binding.tvDetailContent.text = content
        binding.tvDetailLikes.text = "$likesCount Likes"

        if (imageUrl.isNotEmpty()) {
            binding.ivDetailPostImage.load(imageUrl) { crossfade(true) }
            binding.ivDetailPostImage.visibility = View.VISIBLE
        } else if (currentPostId.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val postDoc = db.collection("posts").document(currentPostId).get().await()
                    val fetchedImageUrl = postDoc.getString("imageUrl") ?: ""
                    if (fetchedImageUrl.isNotEmpty()) {
                        binding.ivDetailPostImage.load(fetchedImageUrl) { crossfade(true) }
                        binding.ivDetailPostImage.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        observeComments()

        if (currentPostId.isNotEmpty()) {
            viewModel.loadComments(currentPostId)
        }

        // Handle Sending a Comment OR a Reply
        binding.btnSendComment.setOnClickListener {
            val commentText = binding.etCommentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {

                // IMPORTANT: Pass the replyingToCommentId to your ViewModel!
                viewModel.addComment(currentPostId, commentText, replyingToCommentId)

                binding.etCommentInput.text.clear()
                resetReplyState() // Clear the state so the next comment is a normal one
            } else {
                Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        // Pass the click behavior into the adapter
        commentAdapter = CommentAdapter { clickedItem ->
            // Set the state
            replyingToCommentId = clickedItem.comment.commentId

            // Update the UI so the user knows they are replying!
            binding.etCommentInput.requestFocus()
            binding.etCommentInput.hint = "Replying to ${clickedItem.comment.authorName}..."

            // Optional: Show a toast just to confirm the action
            Toast.makeText(this, "Replying to ${clickedItem.comment.authorName}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewComments.apply {
            adapter = commentAdapter
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
        }
    }

    private fun observeComments() {
        lifecycleScope.launch {
            viewModel.comments.collect { threadedList ->
                commentAdapter.submitList(threadedList)

                if (threadedList.isNotEmpty() && replyingToCommentId == null) {
                    binding.recyclerViewComments.smoothScrollToPosition(threadedList.size - 1)
                }
            }
        }
    }

    // Resets the text box to a normal post comment
    private fun resetReplyState() {
        replyingToCommentId = null
        binding.etCommentInput.hint = "Add a comment..."
    }
}