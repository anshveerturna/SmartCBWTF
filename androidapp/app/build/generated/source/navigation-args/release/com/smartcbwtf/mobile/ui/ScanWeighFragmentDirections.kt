package com.smartcbwtf.mobile.ui

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.smartcbwtf.mobile.R

public class ScanWeighFragmentDirections private constructor() {
  public companion object {
    public fun actionScanWeighFragmentToQrScannerFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_scanWeighFragment_to_qrScannerFragment)
  }
}
