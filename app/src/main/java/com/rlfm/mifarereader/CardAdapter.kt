package com.rlfm.mifarereader

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rlfm.mifarereader.databinding.ItemCardBinding

/**
 * Adapter for displaying card entries in a RecyclerView with smooth animations
 */
class CardAdapter : ListAdapter<CardEntry, CardAdapter.CardViewHolder>(CardDiffCallback()) {

    private var lastPosition = -1

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

        // Animate item when it appears
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_in_right)
            holder.itemView.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun onViewDetachedFromWindow(holder: CardViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    fun resetAnimation() {
        lastPosition = -1
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
