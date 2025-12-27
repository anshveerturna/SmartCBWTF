package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.smartcbwtf.mobile.BuildConfig
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * About screen showing basic app information.
 * Displays version, build type, and copyright information.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        // Display version info
        binding.tvVersion.text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        binding.tvBuild.text = if (BuildConfig.DEBUG) "Debug" else "Release"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
