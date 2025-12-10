package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentLoginBinding
import com.smartcbwtf.mobile.viewmodel.AuthEvent
import com.smartcbwtf.mobile.viewmodel.AuthState
import com.smartcbwtf.mobile.viewmodel.AuthViewModel
import com.smartcbwtf.mobile.viewmodel.LoginError
import com.smartcbwtf.mobile.viewmodel.LoginState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import android.util.Log

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: AuthViewModel by activityViewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        // Hide ActionBar
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        // Entry Animation
        binding.cardLogin.alpha = 0f
        binding.cardLogin.translationY = 100f
        binding.cardLogin.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            viewModel.login(username, password)
        }

        binding.btnForgotPassword.setOnClickListener {
            Toast.makeText(requireContext(), "Please contact admin to reset password", Toast.LENGTH_SHORT).show()
        }

        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilUsername.error = null
                binding.tilPassword.error = null
                binding.layoutErrorBanner.isVisible = false
                
                if (binding.etUsername.hasFocus()) viewModel.clearError(LoginError.EmptyUsername)
                if (binding.etPassword.hasFocus()) viewModel.clearError(LoginError.EmptyPassword)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        binding.etUsername.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.loginState.collect { state ->
                        handleLoginState(state)
                    }
                }
                launch {
                    viewModel.authEvents.collect { event ->
                        Log.d("LoginFragment", "Received event: $event")
                        when (event) {
                            is AuthEvent.NavigateToHome -> {
                                try {
                                    val navController = findNavController()
                                    Log.d("LoginFragment", "Current destination: ${navController.currentDestination?.label} (ID: ${navController.currentDestination?.id})")
                                    if (navController.currentDestination?.id == R.id.loginFragment) {
                                        Log.d("LoginFragment", "Navigating to Home with action ID: ${R.id.action_loginFragment_to_homeFragment}")
                                        navController.navigate(R.id.action_loginFragment_to_homeFragment)
                                        Log.d("LoginFragment", "Navigate called, new destination: ${navController.currentDestination?.label} (ID: ${navController.currentDestination?.id})")
                                    } else {
                                        Log.e("LoginFragment", "Cannot navigate: Current destination is not LoginFragment")
                                    }
                                } catch (e: Exception) {
                                    Log.e("LoginFragment", "Navigation failed", e)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleLoginState(state: LoginState) {
        val isLoading = state is LoginState.Loading
        binding.progressBar.isVisible = isLoading
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "" else "Login"
        binding.etUsername.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        
        if (isLoading || state is LoginState.Success) {
            binding.tilUsername.error = null
            binding.tilPassword.error = null
            binding.layoutErrorBanner.isVisible = false
        }

        when (state) {
            is LoginState.ValidationFailed -> {
                if (state.errors.contains(LoginError.EmptyUsername)) {
                    binding.tilUsername.error = "Please enter a username"
                }
                if (state.errors.contains(LoginError.EmptyPassword)) {
                    binding.tilPassword.error = "Please enter a password"
                }
            }
            is LoginState.Error -> {
                when (state.error) {
                    LoginError.InvalidCredentials -> {
                        binding.layoutErrorBanner.isVisible = true
                        android.animation.ObjectAnimator.ofFloat(binding.cardLogin, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
                            .setDuration(500)
                            .start()
                    }
                    LoginError.NetworkError -> {
                        com.google.android.material.snackbar.Snackbar.make(
                            binding.root,
                            "No internet connection. Please check your network and try again.",
                            com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
                        ).setAction("Retry") {
                            val username = binding.etUsername.text.toString()
                            val password = binding.etPassword.text.toString()
                            viewModel.login(username, password)
                        }.show()
                    }
                    LoginError.UnknownError -> {
                        com.google.android.material.snackbar.Snackbar.make(
                            binding.root,
                            "Something went wrong. Please try again.",
                            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                        ).show()
                    }
                    else -> {}
                }
            }
            else -> {}
        }
    }

    override fun onStop() {
        super.onStop()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
