package com.example.unifiltered.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.unifiltered.databinding.ItemPostBinding
import com.example.unifiltered.model.Post

class PostAdapter : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    // The ViewHolder holds the layout for a single item
    inner class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            // Map the data from the Post object to the UI elements
            binding.tvAuthorName.text = post.authorName
            binding.tvPostContent.text = post.content

            // A simple way to format the likes string
            binding.tvLikeCount.text = "${post.likesCount} Likes"

            // Later on, we can add click listeners here for the like button!
        }
    }

    // This creates new visual cards when the RecyclerView needs them
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    // This binds the data to a specific card as it scrolls onto the screen
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

    // DiffUtil helps the RecyclerView know exactly which items changed,
    // rather than redrawing the whole list every time.
    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            // Are they the exact same post? (Checking the unique ID)
            return oldItem.postId == newItem.postId
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            // Did the content inside the post change? (e.g., likes increased)
            return oldItem == newItem
        }
    }
}