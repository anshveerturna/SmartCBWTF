package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentHomeBinding
import com.smartcbwtf.mobile.viewmodel.AuthState
import com.smartcbwtf.mobile.viewmodel.AuthViewModel
import com.smartcbwtf.mobile.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: AuthViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        android.util.Log.d("HomeFragment", "onViewCreated")

        binding.btnStartPickup.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_startPickupFragment)
        }

        binding.btnVerifyAtCbtwf.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_verifyAtCbtwfFragment)
        }

        binding.btnRegisterHcf.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_hcfRegistrationFragment)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                if (state is AuthState.Unauthenticated) {
                    findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.pendingCount.collect { count ->
                binding.tvPending.text = "Pending events: $count"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.lastSyncFailed.collect { failed ->
                binding.tvSyncError.visibility = if (failed) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        android.util.Log.d("HomeFragment", "onDestroyView")
        _binding = null
    }
}
