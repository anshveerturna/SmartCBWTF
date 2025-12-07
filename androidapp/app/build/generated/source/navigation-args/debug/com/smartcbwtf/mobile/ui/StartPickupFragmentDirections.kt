package com.smartcbwtf.mobile.ui

import android.os.Bundle
import androidx.navigation.NavDirections
import com.smartcbwtf.mobile.R
import kotlin.Int
import kotlin.String

public class StartPickupFragmentDirections private constructor() {
  private data class ActionStartPickupFragmentToScanWeighFragment(
    public val hcfId: String,
    public val eventType: String = "COLLECTION",
  ) : NavDirections {
    public override val actionId: Int = R.id.action_startPickupFragment_to_scanWeighFragment

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("hcfId", this.hcfId)
        result.putString("eventType", this.eventType)
        return result
      }
  }

  public companion object {
    public fun actionStartPickupFragmentToScanWeighFragment(hcfId: String, eventType: String =
        "COLLECTION"): NavDirections = ActionStartPickupFragmentToScanWeighFragment(hcfId,
        eventType)
  }
}
