package com.example.unifiltered.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unifiltered.databinding.FragmentProfileBinding
import com.example.unifiltered.repository.PostRepository
import com.example.unifiltered.ui.AdminDashboardActivity
import com.example.unifiltered.ui.auth.LoginActivity
import com.example.unifiltered.ui.feed.PostAdapter
import com.example.unifiltered.ui.feed.PostDetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var postAdapter: PostAdapter
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val postRepo = PostRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid ?: ""
        val userEmail = currentUser?.email ?: ""

        // 1. Basic UI Setup & God Lock
        binding.tvProfileEmail.text = userEmail
        if (userEmail == "anas@gmail.com" || userEmail == "anasfaizsahi6@gmail.com") {
            binding.btnAdminLair.visibility = View.VISIBLE
        }

        binding.btnAdminLair.setOnClickListener {
            startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
        }

        // 2. Fetch User's Name from Firestore
        lifecycleScope.launch {
            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val name = userDoc.getString("name") ?: "Unknown User"
                binding.tvProfileName.text = name
            } catch (e: Exception) {
                binding.tvProfileName.text = "Student"
            }
        }

        // 3. Setup RecyclerView for "My Posts"
        postAdapter = PostAdapter(
            currentUserId = currentUserId,
            onPostClick = { clickedPost ->
                val intent = Intent(requireContext(), PostDetailActivity::class.java).apply {
                    putExtra("POST_ID", clickedPost.postId)
                    putExtra("AUTHOR_NAME", clickedPost.authorName)
                    putExtra("CONTENT", clickedPost.content)
                    putExtra("LIKES_COUNT", clickedPost.likedBy.size)
                }
                startActivity(intent)
            },
            onLikeClick = { clickedPost, isCurrentlyLiked ->
                lifecycleScope.launch {
                    postRepo.toggleLike(clickedPost.postId, isCurrentlyLiked)
                }
            }
        )

        binding.recyclerViewMyPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // 4. Load the Posts & Hide Spinner
        lifecycleScope.launch {
            postRepo.getMyPosts(currentUserId).collect { posts ->
                binding.profileProgressBar.visibility = View.GONE
                postAdapter.submitList(posts)
            }
        }

        // 5. Log Out Logic
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            // Clear the activity stack and jump back to Login
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}