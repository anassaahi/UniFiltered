package com.example.unifiltered.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ProgressBar
import com.example.unifiltered.R
import com.example.unifiltered.repository.PostRepository
import com.example.unifiltered.ui.feed.PostAdapter
import com.example.unifiltered.ui.feed.PostDetailActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MyPostsFragment : Fragment() {

    private lateinit var postAdapter: PostAdapter
    private val auth = FirebaseAuth.getInstance()
    private val postRepo = PostRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_my_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val currentUserId = auth.currentUser?.uid ?: return

        postAdapter = PostAdapter(
            currentUserId = currentUserId,
            onPostClick = { clickedPost ->
                val intent = Intent(requireContext(), PostDetailActivity::class.java).apply {
                    putExtra("POST_ID", clickedPost.postId)
                    putExtra("AUTHOR_NAME", clickedPost.authorName)
                    putExtra("CONTENT", clickedPost.content)
                    putExtra("LIKES_COUNT", clickedPost.likedBy.size)
                    putExtra("IMAGE_URL", clickedPost.imageUrl)
                }
                startActivity(intent)
            },
            onLikeClick = { clickedPost, isCurrentlyLiked ->
                lifecycleScope.launch { postRepo.toggleLike(clickedPost.postId, isCurrentlyLiked) }
            },
            onDeleteClick = { postToDelete ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Post")
                    .setMessage("Are you sure you want to delete this post?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            val result = postRepo.deletePost(postToDelete.postId, postToDelete.imageUrl)
                            if (result.isSuccess) Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        recyclerView.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        lifecycleScope.launch {
            postRepo.getMyPosts(currentUserId).collect { posts ->
                progressBar.visibility = View.GONE
                postAdapter.submitList(posts)
            }
        }
    }
}