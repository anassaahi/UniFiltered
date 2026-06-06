package com.example.unifiltered.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.unifiltered.databinding.ItemCommentBinding
import com.example.unifiltered.model.ThreadedComment

class CommentAdapter(
    private val onReplyClicked: (ThreadedComment) -> Unit
) : ListAdapter<ThreadedComment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    inner class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(threaded: ThreadedComment) {
            val comment = threaded.comment

            binding.tvCommentAuthor.text = comment.authorName
            binding.tvCommentText.text = comment.text

            // --- THE REDDIT LINES MAGIC ---
            // 1. Clear any old lines from recycling
            binding.llDepthLines.removeAllViews()

            val context = binding.root.context
            val density = context.resources.displayMetrics.density

            // 2. Draw a vertical line for every level of depth
            for (i in 0 until threaded.depth) {

                // Create a transparent spacer block (24dp wide) to separate the lines
                val spacer = android.widget.FrameLayout(context).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        (24 * density).toInt(),
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                // Create the actual 2dp thick vertical orange line
                val line = android.view.View(context).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        (2 * density).toInt(),
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    ).apply {
                        // Put the line directly in the center of the 24dp spacer block
                        gravity = android.view.Gravity.CENTER_HORIZONTAL
                    }
                    // Using your divider color #27272A (Dark Graphite).
                    // (Change this to "#D9F99D" if you want the lines to be lime!)
                    setBackgroundColor(android.graphics.Color.parseColor("#27272A"))
                }

                // Add the line to the spacer, and the spacer to the layout
                spacer.addView(line)
                binding.llDepthLines.addView(spacer)
            }
            // ------------------------------

            binding.tvReplyButton.setOnClickListener {
                onReplyClicked(threaded)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<ThreadedComment>() {
        override fun areItemsTheSame(oldItem: ThreadedComment, newItem: ThreadedComment): Boolean {
            return oldItem.comment.commentId == newItem.comment.commentId
        }
        override fun areContentsTheSame(oldItem: ThreadedComment, newItem: ThreadedComment): Boolean {
            return oldItem == newItem
        }
    }
}