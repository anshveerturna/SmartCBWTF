package com.smartcbwtf.mobile.network.model

import com.google.gson.annotations.SerializedName

data class HcfRegistrationResponse(
    val status: String,
    @SerializedName("hcfId") val hcfId: String,
    @SerializedName("hcfCode") val hcfCode: String,
    @SerializedName("agreementId") val agreementId: String?,
    @SerializedName("agreementNumber") val agreementNumber: String?,
    @SerializedName("pdfUrl") val pdfUrl: String?,
    val message: String?,
    @SerializedName("templateUsed") val templateUsed: TemplateInfo? = null
)

data class TemplateInfo(
    @SerializedName("templateId") val templateId: String?,
    @SerializedName("templateVersion") val templateVersion: Int?,
    @SerializedName("facilityId") val facilityId: String?
)
