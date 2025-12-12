package com.smartcbwtf.mobile.model

/**
 * Enum representing the operation mode for the Scan & Weigh screen.
 */
enum class OperationMode {
    /**
     * Collection mode - used when collecting bags at HCF locations
     */
    COLLECTION,
    
    /**
     * Verification mode - used when verifying bags at CBWTF facility
     */
    VERIFY
}
