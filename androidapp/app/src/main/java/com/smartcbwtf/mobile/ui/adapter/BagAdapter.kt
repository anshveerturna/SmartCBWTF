package com.smartcbwtf.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartcbwtf.mobile.databinding.ItemBagEntryBinding
import com.smartcbwtf.mobile.viewmodel.BagEntry
import java.util.Locale

/**
 * Adapter for displaying scanned bags with delete functionality.
 */
class BagAdapter(
    private val onDeleteClick: (Int) -> Unit
) : ListAdapter<BagEntry, BagAdapter.BagViewHolder>(BagDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BagViewHolder {
        val binding = ItemBagEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BagViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class BagViewHolder(
        private val binding: ItemBagEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bag: BagEntry, position: Int) {
            binding.tvBagIndex.text = (position + 1).toString()
            binding.tvBagWeight.text = String.format(Locale.US, "%.2f kg", bag.weightKg)
            
            // Show truncated QR code (first 30 chars)
            val qrPreview = if (bag.qrCode.length > 30) {
                bag.qrCode.take(30) + "..."
            } else {
                bag.qrCode
            }
            binding.tvBagQr.text = "QR: $qrPreview"

            binding.btnDeleteBag.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }

    private class BagDiffCallback : DiffUtil.ItemCallback<BagEntry>() {
        override fun areItemsTheSame(oldItem: BagEntry, newItem: BagEntry): Boolean {
            return oldItem.qrCode == newItem.qrCode
        }

        override fun areContentsTheSame(oldItem: BagEntry, newItem: BagEntry): Boolean {
            return oldItem == newItem
        }
    }
}
