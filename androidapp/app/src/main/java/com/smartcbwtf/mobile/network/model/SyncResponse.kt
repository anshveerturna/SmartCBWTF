package com.smartcbwtf.mobile.network.model

/**
 * Response from bag events sync endpoint.
 * Backend returns acks per QR code with status SUCCESS/FAILED.
 */
data class SyncResponse(
    val acks: List<Ack> = emptyList(),
) {
    data class Ack(
        val qrCode: String,
        val status: String,
        val message: String?,
    )
    
    /**
     * Extract QR codes that were successfully synced.
     */
    val successQrCodes: List<String>
        get() = acks.filter { it.status == "SUCCESS" }.map { it.qrCode }
}
