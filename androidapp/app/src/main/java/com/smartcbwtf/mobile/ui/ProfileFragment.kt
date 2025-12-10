package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        binding.textProfileName.text = "Name: Operator"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
