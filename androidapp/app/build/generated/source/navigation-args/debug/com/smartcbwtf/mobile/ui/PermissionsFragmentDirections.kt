package com.smartcbwtf.mobile.ui

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.smartcbwtf.mobile.R

public class PermissionsFragmentDirections private constructor() {
  public companion object {
    public fun actionPermissionsFragmentToHomeFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_permissionsFragment_to_homeFragment)
  }
}
