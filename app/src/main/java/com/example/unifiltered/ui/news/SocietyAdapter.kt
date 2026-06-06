package com.example.unifiltered.ui.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.unifiltered.databinding.ItemSocietyBinding
import com.example.unifiltered.model.Society

class SocietyAdapter(private val onSocietyClick: (Society) -> Unit) :
    ListAdapter<Society, SocietyAdapter.SocietyViewHolder>(SocietyDiffCallback()) {

    inner class SocietyViewHolder(private val binding: ItemSocietyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(society: Society) {
            binding.tvSocietyName.text = society.name
            binding.tvSocietyBio.text = society.description
            binding.tvFollowerCount.text = "${society.followerCount} Followers"

            // Show or hide the blue tick!
            binding.ivOfficialBadge.visibility = if (society.isOfficial) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onSocietyClick(society) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SocietyViewHolder {
        val binding = ItemSocietyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SocietyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SocietyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SocietyDiffCallback : DiffUtil.ItemCallback<Society>() {
        override fun areItemsTheSame(oldItem: Society, newItem: Society) = oldItem.societyId == newItem.societyId
        override fun areContentsTheSame(oldItem: Society, newItem: Society) = oldItem == newItem
    }
}