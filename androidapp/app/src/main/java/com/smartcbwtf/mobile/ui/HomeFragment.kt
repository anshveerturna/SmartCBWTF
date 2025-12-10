package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.appcompat.widget.PopupMenu
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

        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        setupActions()
        setupProfileMenu()
        bindStatus()
        animateEntry()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }

    private fun setupActions() {
        val cards = listOf(binding.cardPickup, binding.cardVerify, binding.cardRegister)
        cards.forEach { card ->
            card.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                false
            }
        }

        binding.cardPickup.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_startPickupFragment)
        }

        binding.cardVerify.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_verifyAtCbtwfFragment)
        }

        binding.cardRegister.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_hcfRegistrationFragment)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }
    }

    private fun setupProfileMenu() {
        binding.avatarView.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor, android.view.Gravity.END)
            popup.menuInflater.inflate(R.menu.menu_profile, popup.menu)
            
            // Force icons to show in popup menu
            try {
                val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldMPopup.isAccessible = true
                val mPopup = fieldMPopup.get(popup)
                mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(mPopup, true)
            } catch (e: Exception) {
                // Ignore if reflection fails
            }
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_profile -> {
                        findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
                        true
                    }
                    R.id.menu_logout -> {
                        viewModel.logout()
                        val options = navOptions {
                            popUpTo(R.id.homeFragment) { inclusive = true }
                        }
                        findNavController().navigate(R.id.action_homeFragment_to_loginFragment, null, options)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        binding.textGreetingTitle.text = "Hello, Operator"
    }

    private fun bindStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.authState.collect { state ->
                        if (state is AuthState.Unauthenticated && isAdded) {
                            val options = navOptions {
                                popUpTo(R.id.homeFragment) { inclusive = true }
                            }
                            findNavController().navigate(R.id.action_homeFragment_to_loginFragment, null, options)
                        }
                    }
                }

                launch {
                    homeViewModel.pendingCount.collect { count ->
                        binding.textPendingValue.text = count.toString()
                        if (count > 0) {
                            binding.textPendingValue.setTextColor(resources.getColor(R.color.warningAccent, null))
                        } else {
                            binding.textPendingValue.setTextColor(resources.getColor(R.color.text_title, null))
                        }
                    }
                }

                launch {
                    homeViewModel.lastSyncFailed.collect { failed ->
                        if (failed) {
                            binding.textSyncValue.text = "Sync Issue"
                            binding.textSyncValue.setTextColor(resources.getColor(R.color.errorAccent, null))
                        } else {
                            binding.textSyncValue.text = "Just now"
                            binding.textSyncValue.setTextColor(resources.getColor(R.color.text_title, null))
                        }
                    }
                }
            }
        }
    }

    private fun animateEntry() {
        val fadeInViews = listOf(binding.cardGreeting, binding.cardPickup, binding.cardVerify, binding.cardRegister, binding.cardStatus)
        fadeInViews.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 20f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 50).toLong())
                .setDuration(240)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
    }
}
