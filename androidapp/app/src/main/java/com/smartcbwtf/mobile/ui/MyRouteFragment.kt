package com.smartcbwtf.mobile.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentMyRouteBinding
import com.smartcbwtf.mobile.network.model.MobileRouteResponse
import com.smartcbwtf.mobile.ui.adapter.WaypointAdapter
import com.smartcbwtf.mobile.viewmodel.MyRouteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment displaying the staff member's assigned route and waypoints.
 */
@AndroidEntryPoint
class MyRouteFragment : Fragment(R.layout.fragment_my_route) {

    private val viewModel: MyRouteViewModel by viewModels()
    private var _binding: FragmentMyRouteBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyRouteBinding.bind(view)

        binding.btnRetry.setOnClickListener {
            viewModel.loadRoute()
        }

        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MyRouteViewModel.UiState.Loading -> showLoading()
                        is MyRouteViewModel.UiState.Success -> showRoute(state.route)
                        is MyRouteViewModel.UiState.NoRouteAssigned -> showNoRoute()
                        is MyRouteViewModel.UiState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.loadingContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.GONE
        binding.noRouteContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
    }

    private fun showRoute(route: MobileRouteResponse) {
        binding.loadingContainer.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
        binding.noRouteContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE

        // Set header info
        binding.textRouteName.text = route.routeName
        binding.textFacilityName.text = route.facilityName
        binding.textStopsCount.text = route.waypoints.size.toString()
        binding.textTimeframe.text = "${route.completionDays ?: 1} day(s)"

        // Apply route color
        route.routeColor?.let { color ->
            try {
                val drawable = binding.viewRouteColor.background as? GradientDrawable
                drawable?.setColor(Color.parseColor(color))
            } catch (e: Exception) {
                // Use default color
            }
        }

        // Setup waypoints RecyclerView
        val adapter = WaypointAdapter(route.routeColor) { waypoint ->
            // Optional: Navigate to HCF details or start pickup
        }
        binding.recyclerWaypoints.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerWaypoints.adapter = adapter
        adapter.submitList(route.waypoints)
    }

    private fun showNoRoute() {
        binding.loadingContainer.visibility = View.GONE
        binding.contentContainer.visibility = View.GONE
        binding.noRouteContainer.visibility = View.VISIBLE
        binding.errorContainer.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.loadingContainer.visibility = View.GONE
        binding.contentContainer.visibility = View.GONE
        binding.noRouteContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.VISIBLE
        binding.textErrorMessage.text = message
    }
}
