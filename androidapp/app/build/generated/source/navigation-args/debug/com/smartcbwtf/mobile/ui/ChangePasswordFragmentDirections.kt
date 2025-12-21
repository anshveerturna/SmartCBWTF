package com.smartcbwtf.mobile.ui

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.smartcbwtf.mobile.R

public class ChangePasswordFragmentDirections private constructor() {
  public companion object {
    public fun actionChangePasswordFragmentToHomeFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_changePasswordFragment_to_homeFragment)

    public fun actionChangePasswordFragmentToLoginFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_changePasswordFragment_to_loginFragment)
  }
}
