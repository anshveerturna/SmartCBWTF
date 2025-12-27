package com.smartcbwtf.mobile.ui

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.smartcbwtf.mobile.R

public class LocationDisclosureFragmentDirections private constructor() {
  public companion object {
    public fun actionLocationDisclosureFragmentToHomeFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_locationDisclosureFragment_to_homeFragment)
  }
}
