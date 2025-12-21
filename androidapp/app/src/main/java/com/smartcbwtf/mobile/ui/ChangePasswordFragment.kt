package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.smartcbwtf.mobile.MainActivity
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.databinding.FragmentChangePasswordBinding
import com.smartcbwtf.mobile.network.api.ProfileApi
import com.smartcbwtf.mobile.repository.AuthRepository
import com.smartcbwtf.mobile.storage.AuthTokenStore
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SECURITY: Forced password change screen.
 * 
 * User is locked here when mustChangePassword == true.
 * Cannot bypass via: back button, deep links, navigation.
 * 
 * Only options:
 * 1. Change password successfully → unlocks navigation
 * 2. Logout → clears all state
 */
@AndroidEntryPoint
class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChangePasswordViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.changePasswordButton.setOnClickListener {
            val currentPassword = binding.currentPasswordInput.text?.toString() ?: ""
            val newPassword = binding.newPasswordInput.text?.toString() ?: ""
            val confirmPassword = binding.confirmPasswordInput.text?.toString() ?: ""

            // Client-side validation
            if (currentPassword.isEmpty()) {
                showError("Please enter your current password")
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                showError("Please enter a new password")
                return@setOnClickListener
            }

            if (newPassword.length < 8) {
                showError("Password must be at least 8 characters")
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                showError("Passwords do not match")
                return@setOnClickListener
            }

            // Call API
            viewModel.changePassword(currentPassword, newPassword)
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is ChangePasswordUiState.Idle -> {
                            setLoading(false)
                            hideError()
                        }
                        is ChangePasswordUiState.Loading -> {
                            setLoading(true)
                            hideError()
                        }
                        is ChangePasswordUiState.Success -> {
                            setLoading(false)
                            Toast.makeText(context, "Password changed successfully!", Toast.LENGTH_LONG).show()
                            
                            // Notify MainActivity to navigate home
                            (activity as? MainActivity)?.onPasswordChangeComplete()
                        }
                        is ChangePasswordUiState.Error -> {
                            setLoading(false)
                            showError(state.message)
                        }
                        is ChangePasswordUiState.LoggedOut -> {
                            // Navigate to login screen
                            findNavController().navigate(R.id.action_changePasswordFragment_to_loginFragment)
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        binding.changePasswordButton.isEnabled = !loading
        binding.logoutButton.isEnabled = !loading
        binding.currentPasswordInput.isEnabled = !loading
        binding.newPasswordInput.isEnabled = !loading
        binding.confirmPasswordInput.isEnabled = !loading
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.errorText.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * UI State for password change screen.
 */
sealed class ChangePasswordUiState {
    object Idle : ChangePasswordUiState()
    object Loading : ChangePasswordUiState()
    object Success : ChangePasswordUiState()
    object LoggedOut : ChangePasswordUiState()
    data class Error(val message: String) : ChangePasswordUiState()
}

/**
 * ViewModel for password change functionality.
 */
@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val profileApi: ProfileApi,
    private val authTokenStore: AuthTokenStore,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChangePasswordUiState>(ChangePasswordUiState.Idle)
    val uiState: StateFlow<ChangePasswordUiState> = _uiState

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = ChangePasswordUiState.Loading
            try {
                // Call backend API to change password
                val response = profileApi.changePassword(
                    ChangePasswordRequest(
                        currentPassword = currentPassword,
                        newPassword = newPassword
                    )
                )

                if (response.isSuccessful) {
                    // SECURITY: Clear mustChangePassword flag ONLY after successful API response
                    authTokenStore.setMustChangePassword(false)
                    _uiState.value = ChangePasswordUiState.Success
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Password change failed"
                    _uiState.value = ChangePasswordUiState.Error(errorBody)
                }
            } catch (e: Exception) {
                _uiState.value = ChangePasswordUiState.Error(
                    e.message ?: "Network error - please try again"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                _uiState.value = ChangePasswordUiState.LoggedOut
            } catch (e: Exception) {
                // Even on error, clear local state
                _uiState.value = ChangePasswordUiState.LoggedOut
            }
        }
    }
}

/**
 * Request body for password change API.
 */
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

