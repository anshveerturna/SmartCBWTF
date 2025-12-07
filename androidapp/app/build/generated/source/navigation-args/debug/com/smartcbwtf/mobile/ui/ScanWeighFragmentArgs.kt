package com.smartcbwtf.mobile.ui

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException
import kotlin.String
import kotlin.jvm.JvmStatic

public data class ScanWeighFragmentArgs(
  public val hcfId: String,
  public val eventType: String = "COLLECTION",
) : NavArgs {
  public fun toBundle(): Bundle {
    val result = Bundle()
    result.putString("hcfId", this.hcfId)
    result.putString("eventType", this.eventType)
    return result
  }

  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    result.set("hcfId", this.hcfId)
    result.set("eventType", this.eventType)
    return result
  }

  public companion object {
    @JvmStatic
    public fun fromBundle(bundle: Bundle): ScanWeighFragmentArgs {
      bundle.setClassLoader(ScanWeighFragmentArgs::class.java.classLoader)
      val __hcfId : String?
      if (bundle.containsKey("hcfId")) {
        __hcfId = bundle.getString("hcfId")
        if (__hcfId == null) {
          throw IllegalArgumentException("Argument \"hcfId\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"hcfId\" is missing and does not have an android:defaultValue")
      }
      val __eventType : String?
      if (bundle.containsKey("eventType")) {
        __eventType = bundle.getString("eventType")
        if (__eventType == null) {
          throw IllegalArgumentException("Argument \"eventType\" is marked as non-null but was passed a null value.")
        }
      } else {
        __eventType = "COLLECTION"
      }
      return ScanWeighFragmentArgs(__hcfId, __eventType)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): ScanWeighFragmentArgs {
      val __hcfId : String?
      if (savedStateHandle.contains("hcfId")) {
        __hcfId = savedStateHandle["hcfId"]
        if (__hcfId == null) {
          throw IllegalArgumentException("Argument \"hcfId\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"hcfId\" is missing and does not have an android:defaultValue")
      }
      val __eventType : String?
      if (savedStateHandle.contains("eventType")) {
        __eventType = savedStateHandle["eventType"]
        if (__eventType == null) {
          throw IllegalArgumentException("Argument \"eventType\" is marked as non-null but was passed a null value")
        }
      } else {
        __eventType = "COLLECTION"
      }
      return ScanWeighFragmentArgs(__hcfId, __eventType)
    }
  }
}
