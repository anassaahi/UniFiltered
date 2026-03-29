package com.example.unifiltered.ui.feed

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.unifiltered.databinding.ActivityPostDetailBinding

class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Retrieve the data passed from the FeedFragment
        val postId = intent.getStringExtra("POST_ID") ?: ""
        val authorName = intent.getStringExtra("AUTHOR_NAME") ?: "Unknown"
        val content = intent.getStringExtra("CONTENT") ?: ""
        val likesCount = intent.getIntExtra("LIKES_COUNT", 0)

        // 2. Bind the data to the UI elements
        binding.tvDetailAuthorName.text = authorName
        binding.tvDetailContent.text = content
        binding.tvDetailLikes.text = "$likesCount Likes"

        // 3. Handle the Back Button
        binding.btnBack.setOnClickListener {
            finish() // Closes this screen and returns to the feed
        }
    }
}