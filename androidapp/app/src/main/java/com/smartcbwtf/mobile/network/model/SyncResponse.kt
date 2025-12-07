package com.smartcbwtf.mobile.network.model

data class SyncResponse(
    val successIds: List<String> = emptyList(),
    val failedIds: List<String> = emptyList(),
)
