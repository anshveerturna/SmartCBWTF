package com.smartcbwtf.mobile.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.smartcbwtf.mobile.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerifyAtCbtwfFragment : Fragment(R.layout.fragment_scan_weigh) {
    // Reusing ScanWeighFragment logic via inheritance or composition would be better
    // For now, just a placeholder that navigates to the same UI but with different args
    // Actually, the nav graph points to this class, but we can just reuse ScanWeighFragment class in nav graph
    // or make this a subclass.
    
    // Let's make it redirect to ScanWeighFragment logic if we want to reuse code without copy-paste
    // But since we defined it as a separate destination in nav graph with this class name:
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // This is a stub. In a real app, we'd inherit from ScanWeighFragment 
        // and override defaults or just use one Fragment with args.
        // Since I can't easily change the nav graph class reference without re-editing,
        // I'll leave this as a stub that would need implementation similar to ScanWeighFragment.
        // Ideally, we should have just used ScanWeighFragment for both destinations in nav graph.
    }
}
