package com.smartcbwtf.mobile.ui

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.smartcbwtf.mobile.R

public class LoginFragmentDirections private constructor() {
  public companion object {
    public fun actionLoginFragmentToHomeFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_loginFragment_to_homeFragment)

    public fun actionLoginFragmentToPermissionsFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_loginFragment_to_permissionsFragment)
  }
}
