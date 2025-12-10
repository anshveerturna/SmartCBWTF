package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentSettingsBinding
import com.smartcbwtf.mobile.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val viewModel: SettingsViewModel by viewModels()
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.tvAppVersion.text = "Version: ${viewModel.appVersion}"
        binding.tvBaseUrl.text = "Base URL: ${viewModel.baseUrl}"
        binding.tvScaleService.text = "Scale Service: ${viewModel.scaleServiceType}"

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.username.collect { user ->
                binding.tvUser.text = "User: $user"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pendingCount.collect { count ->
                binding.tvPendingEvents.text = "Pending Events: $count"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
