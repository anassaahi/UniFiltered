package com.example.unifiltered.ui.feed

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.unifiltered.R
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

            // NEW: Show the blue tick on the post if it was made by an official society!
            binding.ivPostOfficialBadge.visibility = if (post.isOfficialSocietyPost) View.VISIBLE else View.GONE

            binding.tvPostContent.text = post.content
            binding.tvLikeCount.text = "${post.likedBy.size} Likes"

            val isLikedByMe = post.likedBy.contains(currentUserId)

            // Switch the icon and color based on the like status
            if (isLikedByMe) {
                binding.tvLikeCount.setTextColor(Color.parseColor("#D97706"))
                binding.ivLikeIcon.setImageResource(R.drawable.ic_heart_filled)
                binding.ivLikeIcon.setColorFilter(Color.parseColor("#D97706")) // Tints the heart red
            } else {
                binding.tvLikeCount.setTextColor(Color.DKGRAY)
                binding.ivLikeIcon.setImageResource(R.drawable.ic_heart_outline)
                binding.ivLikeIcon.setColorFilter(Color.DKGRAY) // Tints the outline grey
            }

            // Set the comment count text
            binding.tvCommentCount.text = "${post.commentsCount} Comments"

            // NEW: Load and display post image if exists
            if (post.imageUrl.isNotEmpty()) {
                binding.ivPostImage.load(post.imageUrl) {
                    crossfade(true)
                }
                binding.ivPostImage.visibility = View.VISIBLE
            } else {
                binding.ivPostImage.visibility = View.GONE
            }

            // Listen for clicks on the entire layout (Icon + Text combined)
            binding.layoutLike.setOnClickListener {
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
