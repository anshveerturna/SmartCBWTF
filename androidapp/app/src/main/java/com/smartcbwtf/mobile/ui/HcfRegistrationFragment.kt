package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentHcfRegistrationBinding
import com.smartcbwtf.mobile.viewmodel.HcfRegistrationViewModel
import com.smartcbwtf.mobile.viewmodel.RegistrationState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HcfRegistrationFragment : Fragment(R.layout.fragment_hcf_registration) {

    private val viewModel: HcfRegistrationViewModel by viewModels()
    private var _binding: FragmentHcfRegistrationBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHcfRegistrationBinding.bind(view)

        binding.btnRegister.setOnClickListener {
            val beds = binding.etBeds.text?.toString()?.toIntOrNull()
            viewModel.submit(
                name = binding.etName.text.toString(),
                address = binding.etAddress.text?.toString(),
                city = binding.etCity.text?.toString(),
                state = binding.etState.text?.toString(),
                postalCode = binding.etPostal.text?.toString(),
                phone = binding.etPhone.text?.toString(),
                email = binding.etEmail.text?.toString(),
                beds = beds
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                binding.btnRegister.isEnabled = state !is RegistrationState.Loading
                when (state) {
                    is RegistrationState.Success -> {
                        Toast.makeText(requireContext(), "Registered", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                        viewModel.reset()
                    }
                    is RegistrationState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
