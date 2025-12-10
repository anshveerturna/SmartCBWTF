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

  private data class ActionHomeFragmentToScanWeighFragment(
    public val hcfId: String = "",
    public val eventType: String = "HCF_COLLECTION",
  ) : NavDirections {
    public override val actionId: Int = R.id.action_homeFragment_to_scanWeighFragment

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

    public fun actionHomeFragmentToScanWeighFragment(hcfId: String = "", eventType: String =
        "HCF_COLLECTION"): NavDirections = ActionHomeFragmentToScanWeighFragment(hcfId, eventType)

    public fun actionHomeFragmentToHcfRegistrationFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_homeFragment_to_hcfRegistrationFragment)

    public fun actionHomeFragmentToLoginFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_homeFragment_to_loginFragment)

    public fun actionHomeFragmentToSettingsFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_homeFragment_to_settingsFragment)

    public fun actionHomeFragmentToProfileFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_homeFragment_to_profileFragment)
  }
}
