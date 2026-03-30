package com.example.unifiltered.ui.news

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unifiltered.databinding.ActivitySocietyDetailBinding
import com.example.unifiltered.ui.feed.PostAdapter
import com.example.unifiltered.ui.feed.PostDetailActivity
import com.example.unifiltered.viewmodel.SocietyDetailViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.unifiltered.repository.SocietyRepository

class SocietyDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySocietyDetailBinding
    private val viewModel: SocietyDetailViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter
    private var societyId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocietyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Get the basic data (Keep this)
        societyId = intent.getStringExtra("SOCIETY_ID") ?: ""
        val name = intent.getStringExtra("SOCIETY_NAME") ?: "Society"
        val bio = intent.getStringExtra("SOCIETY_BIO") ?: ""
        val creatorId = intent.getStringExtra("SOCIETY_CREATOR_ID") ?: ""
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        binding.tvDetailSocietyName.text = name
        binding.tvDetailSocietyBio.text = bio
        binding.btnBack.setOnClickListener { finish() }

        // 2. Start listening to the live database
        viewModel.listenToSociety(societyId)

        // 3. React to changes instantly!
        lifecycleScope.launch {
            viewModel.liveSociety.collect { society ->
                if (society != null) {
                    // Show or hide the Blue Tick based on live data
                    binding.ivDetailOfficialBadge.visibility = if (society.isOfficial) View.VISIBLE else View.GONE

                    // Handle the Apply Button
                    if (society.isOfficial) {
                        binding.btnApplyOfficial.visibility = View.GONE // If verified, banish the button!
                    } else if (currentUserId == society.creatorId) {
                        binding.btnApplyOfficial.visibility = View.VISIBLE // Show if they own it and it's not verified
                    } else {
                        binding.btnApplyOfficial.visibility = View.GONE
                    }
                }
            }
        }

        // Apply Button Click Listener
        binding.btnApplyOfficial.setOnClickListener {
            binding.btnApplyOfficial.isEnabled = false
            lifecycleScope.launch {
                val result = SocietyRepository().submitVerificationRequest(societyId, name)
                if (result.isSuccess) {
                    android.widget.Toast.makeText(this@SocietyDetailActivity, "Request sent to Admin!", android.widget.Toast.LENGTH_SHORT).show()
                    binding.btnApplyOfficial.text = "Request Pending"
                }
            }
        }

        // 4. Setup RecyclerView (Reusing our awesome PostAdapter!)
        postAdapter = PostAdapter(
            currentUserId = currentUserId, // Now it uses the variable we defined at the top
            onPostClick = { clickedPost ->
                val intent = Intent(this, PostDetailActivity::class.java).apply {
                    putExtra("POST_ID", clickedPost.postId)
                    putExtra("AUTHOR_NAME", clickedPost.authorName)
                    putExtra("CONTENT", clickedPost.content)
                    putExtra("LIKES_COUNT", clickedPost.likedBy.size)
                }
                startActivity(intent)
            },
            onLikeClick = { clickedPost, isCurrentlyLiked ->
                viewModel.toggleLike(clickedPost.postId, isCurrentlyLiked)
            }
        )

        binding.recyclerViewSocietyPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(this@SocietyDetailActivity)
        }

        // 5. Observe Data
        viewModel.checkFollowStatus(societyId)
        viewModel.loadSocietyPosts(societyId)

        lifecycleScope.launch {
            viewModel.isFollowing.collect { isFollowing ->
                if (isFollowing) {
                    binding.btnFollow.text = "Following"
                    binding.btnFollow.setBackgroundColor(android.graphics.Color.GRAY)
                } else {
                    binding.btnFollow.text = "Follow"
                    binding.btnFollow.setBackgroundColor(getColor(android.R.color.holo_blue_dark)) // Or your app's primary color
                }
            }
        }

        lifecycleScope.launch {
            viewModel.societyPosts.collect { posts ->
                postAdapter.submitList(posts)
            }
        }

        // 6. Follow Button Action
        binding.btnFollow.setOnClickListener {
            viewModel.toggleFollow(societyId)
        }
    }
}