package com.smartcbwtf.mobile.ui

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.smartcbwtf.mobile.R

public class SplashFragmentDirections private constructor() {
  public companion object {
    public fun actionSplashFragmentToLoginFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_splashFragment_to_loginFragment)

    public fun actionSplashFragmentToHomeFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_splashFragment_to_homeFragment)
  }
}
