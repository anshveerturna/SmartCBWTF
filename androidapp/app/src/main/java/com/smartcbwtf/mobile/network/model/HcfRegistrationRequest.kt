package com.smartcbwtf.mobile.network.model

data class HcfRegistrationRequest(
    val name: String,
    val address: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val phone: String?,
    val email: String?,
    val beds: Int?,
    val latitude: Double?,
    val longitude: Double?,
)
