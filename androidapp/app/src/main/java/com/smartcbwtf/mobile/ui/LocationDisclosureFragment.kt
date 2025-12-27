package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.smartcbwtf.mobile.R
import com.smartcbwtf.mobile.repository.LocationRepository
import com.smartcbwtf.mobile.service.ForegroundLocationService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Location disclosure screen for Play Store compliance.
 * 
 * Shown after login to get explicit consent for location tracking.
 * Required for background location access on Android 10+.
 */
@AndroidEntryPoint
class LocationDisclosureFragment : Fragment() {

    @Inject
    lateinit var locationRepository: LocationRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_disclosure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)
        val btnDecline = view.findViewById<Button>(R.id.btnDecline)

        tvTitle.text = "📍 Location Tracking Required"
        tvDescription.text = """
            This app collects location data to:
            
            • Track waste pickup operations
            • Verify attendance at healthcare facilities
            • Monitor staff movement during duty hours
            
            Location is collected even when the app is closed or not in use.
            
            Your location data is only used for operational purposes and is securely stored.
        """.trimIndent()

        btnAccept.setOnClickListener {
            // Store consent
            locationRepository.setLocationConsent(true)
            
            // Start location tracking
            ForegroundLocationService.startService(requireContext())
            
            // Navigate to home using action ID
            findNavController().navigate(R.id.action_locationDisclosureFragment_to_homeFragment)
        }

        btnDecline.setOnClickListener {
            // Don't store consent - user can continue with limited functionality
            locationRepository.setLocationConsent(false)
            
            // Navigate to home anyway
            findNavController().navigate(R.id.action_locationDisclosureFragment_to_homeFragment)
        }
    }
}
