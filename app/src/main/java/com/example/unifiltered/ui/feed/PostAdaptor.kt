package com.example.unifiltered.ui.feed

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.unifiltered.databinding.ItemPostBinding
import com.example.unifiltered.model.Post

// We now pass in the current user's ID, and a separate listener for the Like button
class PostAdapter(
    private val currentUserId: String,
    private val onPostClick: (Post) -> Unit,
    private val onLikeClick: (Post, Boolean) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    inner class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.tvAuthorName.text = post.authorName
            binding.tvPostContent.text = post.content

            // Calculate total likes by looking at the size of the list
            binding.tvLikeCount.text = "${post.likedBy.size} Likes"

            // Check if the current user has liked this post
            val isLikedByMe = post.likedBy.contains(currentUserId)

            // Make the like button red if liked, grey if not
            if (isLikedByMe) {
                binding.tvLikeCount.setTextColor(Color.RED)
            } else {
                binding.tvLikeCount.setTextColor(Color.DKGRAY)
            }

            // Listen for clicks on the Like button specifically
            binding.tvLikeCount.setOnClickListener {
                onLikeClick(post, isLikedByMe)
            }

            // Listen for clicks on the rest of the card to go to details
            binding.root.setOnClickListener {
                onPostClick(post)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }
        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}