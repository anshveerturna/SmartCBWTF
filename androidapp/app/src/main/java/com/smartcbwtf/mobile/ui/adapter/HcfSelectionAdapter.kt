package com.smartcbwtf.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartcbwtf.mobile.databinding.ItemHcfSelectionBinding
import com.smartcbwtf.mobile.viewmodel.NearbyHcf
import kotlin.math.roundToInt

class HcfSelectionAdapter(
    private val onHcfSelected: (NearbyHcf) -> Unit
) : ListAdapter<NearbyHcf, HcfSelectionAdapter.HcfViewHolder>(HcfDiffCallback()) {

    private var selectedHcfId: String? = null

    fun setSelectedHcf(hcfId: String?) {
        val oldSelected = selectedHcfId
        selectedHcfId = hcfId
        
        // Find and notify old and new positions
        currentList.forEachIndexed { index, nearbyHcf ->
            if (nearbyHcf.hcf.id == oldSelected || nearbyHcf.hcf.id == hcfId) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HcfViewHolder {
        val binding = ItemHcfSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HcfViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HcfViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HcfViewHolder(
        private val binding: ItemHcfSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(nearbyHcf: NearbyHcf) {
            val hcf = nearbyHcf.hcf
            
            binding.tvHcfItemName.text = hcf.name
            binding.tvHcfItemAddress.text = hcf.address ?: "No address available"
            binding.tvHcfItemDistance.text = "${nearbyHcf.distanceMeters.roundToInt()}m away"
            
            val isSelected = selectedHcfId == hcf.id
            binding.radioHcfSelect.isChecked = isSelected
            
            // Update card appearance based on selection
            if (isSelected) {
                binding.cardHcfItem.strokeWidth = 2
                binding.cardHcfItem.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(com.smartcbwtf.mobile.R.color.primary_color)
                    )
                )
                binding.cardHcfItem.setCardBackgroundColor(
                    binding.root.context.getColor(com.smartcbwtf.mobile.R.color.mint_glow)
                )
            } else {
                binding.cardHcfItem.strokeWidth = 1
                binding.cardHcfItem.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(com.smartcbwtf.mobile.R.color.divider_color)
                    )
                )
                binding.cardHcfItem.setCardBackgroundColor(
                    binding.root.context.getColor(android.R.color.white)
                )
            }
            
            binding.root.setOnClickListener {
                onHcfSelected(nearbyHcf)
            }
        }
    }

    class HcfDiffCallback : DiffUtil.ItemCallback<NearbyHcf>() {
        override fun areItemsTheSame(oldItem: NearbyHcf, newItem: NearbyHcf): Boolean {
            return oldItem.hcf.id == newItem.hcf.id
        }

        override fun areContentsTheSame(oldItem: NearbyHcf, newItem: NearbyHcf): Boolean {
            return oldItem == newItem
        }
    }
}
