package com.smartcbwtf.mobile.network.model

data class HcfDto(
    val id: String,
    val name: String,
    val address: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val phone: String?,
    val latitude: Double?,
    val longitude: Double?,
    val approved: Boolean,
)
