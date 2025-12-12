package com.smartcbwtf.mobile.network.model

import com.google.gson.annotations.SerializedName

data class TermsResponse(
    val id: String?,
    @SerializedName("facilityId") val facilityId: String?,
    @SerializedName("facilityName") val facilityName: String?,
    val version: String,
    @SerializedName("textHtml") val textHtml: String,
    @SerializedName("effectiveFrom") val effectiveFrom: String?,
    val active: Boolean = true
)
