package com.smartcbwtf.mobile.network.model

import com.google.gson.annotations.SerializedName

data class HcfRegistrationRequest(
    val name: String,
    val address: String?,
    @SerializedName("contactPhone") val phone: String?,
    @SerializedName("contactEmail") val email: String?,
    @SerializedName("doctorName") val doctorName: String?,
    @SerializedName("panNo") val panNo: String?,
    @SerializedName("gstNo") val gstNo: String?,
    @SerializedName("aadharNo") val aadharNo: String?,
    val bedded: Boolean = false,
    @SerializedName("numberOfBeds") val numberOfBeds: Int?,
    @SerializedName("pcbAuthorizationNo") val pcbAuthorizationNo: String?,
    @SerializedName("otherNotes") val otherNotes: String?,
    @SerializedName("monthlyCharges") val monthlyCharges: Double?,
    
    // Terms acceptance (required)
    @SerializedName("termsAccepted") val termsAccepted: Boolean = true,
    @SerializedName("termsVersion") val termsVersion: String?,
    
    // GPS coordinates (required)
    @SerializedName("registrationGpsLat") val gpsLatitude: Double,
    @SerializedName("registrationGpsLon") val gpsLongitude: Double,
    @SerializedName("registrationGpsAccuracy") val gpsAccuracy: Float,
    
    // Agreement dates (optional - defaults to 1 year)
    @SerializedName("agreementStartDate") val agreementStartDate: String? = null,
    @SerializedName("agreementEndDate") val agreementEndDate: String? = null,
    
    // Facility ID (optional - defaults to system facility)
    @SerializedName("facilityId") val facilityId: String? = null,
    
    // Registered by user ID (from current session)
    @SerializedName("registeredByUserId") val registeredByUserId: String? = null,
)
