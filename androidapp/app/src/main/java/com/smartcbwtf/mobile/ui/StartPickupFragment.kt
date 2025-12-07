package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.database.entity.HcfEntity
import com.smartcbwtf.mobile.databinding.FragmentStartPickupBinding
import com.smartcbwtf.mobile.viewmodel.StartPickupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.widget.Toast

@AndroidEntryPoint
class StartPickupFragment : Fragment(R.layout.fragment_start_pickup) {

    private val viewModel: StartPickupViewModel by viewModels()
    private var _binding: FragmentStartPickupBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStartPickupBinding.bind(view)

        val adapter = HcfAdapter { hcf ->
            val action = StartPickupFragmentDirections.actionStartPickupFragmentToScanWeighFragment(
                hcfId = hcf.id,
                eventType = "COLLECTION"
            )
            findNavController().navigate(action)
        }

        binding.rvHcfs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHcfs.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.hcfs.collect { list ->
                adapter.submitList(list)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.progressBar.isVisible = loading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { err ->
                if (!err.isNullOrBlank()) {
                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class HcfAdapter(private val onClick: (HcfEntity) -> Unit) : RecyclerView.Adapter<HcfAdapter.ViewHolder>() {
    private var items: List<HcfEntity> = emptyList()

    fun submitList(newItems: List<HcfEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(32, 32, 32, 32)
            textSize = 18f
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        (holder.itemView as TextView).text = item.name
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
