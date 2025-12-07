package com.smartcbwtf.mobile.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hcfs")
data class HcfEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val phone: String?,
    val approved: Boolean = false,
)
