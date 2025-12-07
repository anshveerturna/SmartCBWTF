package com.smartcbwtf.mobile.ui

import android.os.Bundle
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.smartcbwtf.mobile.R
import kotlin.Int
import kotlin.String

public class HomeFragmentDirections private constructor() {
  private data class ActionHomeFragmentToVerifyAtCbtwfFragment(
    public val hcfId: String = "",
    public val eventType: String = "CBWTF_VERIFICATION",
  ) : NavDirections {
    public override val actionId: Int = R.id.action_homeFragment_to_verifyAtCbtwfFragment

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("hcfId", this.hcfId)
        result.putString("eventType", this.eventType)
        return result
      }
  }

  public companion object {
    public fun actionHomeFragmentToStartPickupFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_homeFragment_to_startPickupFragment)

    public fun actionHomeFragmentToVerifyAtCbtwfFragment(hcfId: String = "", eventType: String =
        "CBWTF_VERIFICATION"): NavDirections = ActionHomeFragmentToVerifyAtCbtwfFragment(hcfId,
        eventType)

    public fun actionHomeFragmentToHcfRegistrationFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_homeFragment_to_hcfRegistrationFragment)

    public fun actionHomeFragmentToLoginFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_homeFragment_to_loginFragment)
  }
}
