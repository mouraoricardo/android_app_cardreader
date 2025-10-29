package com.rlfm.mifarereader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rlfm.mifarereader.databinding.ItemCardBinding

/**
 * Adapter for displaying card entries in a RecyclerView
 */
class CardAdapter : ListAdapter<CardEntry, CardAdapter.CardViewHolder>(CardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CardViewHolder(
        private val binding: ItemCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cardEntry: CardEntry) {
            binding.apply {
                tvCardUid.text = cardEntry.uid
                tvCardType.text = cardEntry.type
                tvTimestamp.text = cardEntry.getFormattedTime()
            }
        }
    }

    class CardDiffCallback : DiffUtil.ItemCallback<CardEntry>() {
        override fun areItemsTheSame(oldItem: CardEntry, newItem: CardEntry): Boolean {
            return oldItem.uid == newItem.uid && oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: CardEntry, newItem: CardEntry): Boolean {
            return oldItem == newItem
        }
    }
}
