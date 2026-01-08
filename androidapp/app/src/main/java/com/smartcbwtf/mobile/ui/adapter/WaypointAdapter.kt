package com.smartcbwtf.mobile.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.network.model.MobileWaypointDTO

/**
 * Adapter for displaying route waypoints (HCFs) in order.
 */
class WaypointAdapter(
    private val routeColor: String?,
    private val onWaypointClick: ((MobileWaypointDTO) -> Unit)? = null
) : ListAdapter<MobileWaypointDTO, WaypointAdapter.WaypointViewHolder>(WaypointDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaypointViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_waypoint, parent, false)
        return WaypointViewHolder(view)
    }

    override fun onBindViewHolder(holder: WaypointViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WaypointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textSequenceNumber: TextView = itemView.findViewById(R.id.textSequenceNumber)
        private val textHcfName: TextView = itemView.findViewById(R.id.textHcfName)
        private val textHcfCode: TextView = itemView.findViewById(R.id.textHcfCode)
        private val textHcfAddress: TextView = itemView.findViewById(R.id.textHcfAddress)

        fun bind(waypoint: MobileWaypointDTO) {
            textSequenceNumber.text = waypoint.sequenceOrder.toString()
            textHcfName.text = waypoint.hcfName
            textHcfCode.text = waypoint.hcfCode
            textHcfAddress.text = waypoint.hcfAddress ?: ""
            textHcfAddress.visibility = if (waypoint.hcfAddress.isNullOrBlank()) View.GONE else View.VISIBLE

            // Apply route color to sequence badge
            routeColor?.let { color ->
                try {
                    val drawable = textSequenceNumber.background as? GradientDrawable
                    drawable?.setColor(Color.parseColor(color))
                } catch (e: Exception) {
                    // Use default color if parsing fails
                }
            }

            itemView.setOnClickListener {
                onWaypointClick?.invoke(waypoint)
            }
        }
    }

    class WaypointDiffCallback : DiffUtil.ItemCallback<MobileWaypointDTO>() {
        override fun areItemsTheSame(oldItem: MobileWaypointDTO, newItem: MobileWaypointDTO): Boolean {
            return oldItem.waypointId == newItem.waypointId
        }

        override fun areContentsTheSame(oldItem: MobileWaypointDTO, newItem: MobileWaypointDTO): Boolean {
            return oldItem == newItem
        }
    }
}
