package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.viewmodel.AuthState
import com.smartcbwtf.mobile.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    AuthState.Authenticated -> {
                        findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
                    }
                    AuthState.Unauthenticated -> {
                        findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                    }
                    AuthState.Loading -> {
                        // Stay on splash
                    }
                }
            }
        }
    }
}
