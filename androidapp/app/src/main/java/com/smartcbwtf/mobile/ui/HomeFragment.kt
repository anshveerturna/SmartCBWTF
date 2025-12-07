package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
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

    private val viewModel: AuthViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.btnStartPickup.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_startPickupFragment)
        }

        binding.btnVerifyAtCbtwf.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_verifyAtCbtwfFragment)
        }

        binding.btnRegisterHcf.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_hcfRegistrationFragment)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
