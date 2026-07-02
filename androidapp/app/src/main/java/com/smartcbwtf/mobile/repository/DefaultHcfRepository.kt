package com.smartcbwtf.mobile.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.smartcbwtf.mobile.database.dao.HcfDao
import com.smartcbwtf.mobile.database.entity.HcfEntity
import com.smartcbwtf.mobile.network.api.HcfApi
import com.smartcbwtf.mobile.network.model.HcfRegistrationRequest
import com.smartcbwtf.mobile.network.model.HcfRegistrationResponse
import com.smartcbwtf.mobile.network.model.TermsResponse
import com.smartcbwtf.mobile.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

@Singleton
class DefaultHcfRepository @Inject constructor(
    private val dao: HcfDao,
    private val api: HcfApi,
    private val networkMonitor: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : HcfRepository {

    override fun getAll(): Flow<List<HcfEntity>> = dao.getAll()

    override suspend fun refresh() = withContext(ioDispatcher) {
        if (!networkMonitor.isOnline()) {
            throw Exception("No internet connection")
        }
        val remote = api.getAll()
        val entities = remote.map {
            HcfEntity(
                id = it.id,
                name = it.name,
                address = it.address,
                city = it.city,
                state = it.state,
                postalCode = it.postalCode,
                phone = it.phone,
                latitude = it.latitude,
                longitude = it.longitude,
                approved = it.approved,
            )
        }
        dao.upsertAll(entities)
    }

    override suspend fun register(request: HcfRegistrationRequest): HcfRegistrationResponse = 
        withContext(ioDispatcher) {
            if (!networkMonitor.isOnline()) {
                throw Exception("No internet connection")
            }
            try {
                api.register(request)
            } catch (e: retrofit2.HttpException) {
                // Parse the actual error message from the server response
                val serverMessage = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody != null) {
                        val json = org.json.JSONObject(errorBody)
                        json.optString("message").takeIf { it.isNotBlank() }
                    } else null
                } catch (_: Exception) { null }

                when (e.code()) {
                    409 -> throw Exception(serverMessage ?: "Another HCF is already registered at this location. Please move to a different location and try again.")
                    400 -> throw Exception(serverMessage ?: "Invalid registration data. Please check your inputs.")
                    else -> throw Exception(serverMessage ?: "Registration failed: ${e.message()}")
                }
            }
        }
    
    override suspend fun getLatestTerms(facilityId: String?): TermsResponse = 
        withContext(ioDispatcher) {
            if (!networkMonitor.isOnline()) {
                throw Exception("No internet connection")
            }
            try {
                api.getLatestTerms(facilityId)
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    // Return default terms if none exist in backend
                    TermsResponse(
                        id = null,
                        facilityId = null,
                        facilityName = null,
                        version = "2025-01-01-default",
                        effectiveFrom = "2025-01-01",
                        textHtml = DEFAULT_TERMS_HTML,
                        active = true
                    )
                } else {
                    throw e
                }
            }
        }
    
    companion object {
        private const val DEFAULT_TERMS_HTML = """
<h2>BIO MEDICAL WASTE COLLECTION & DISPOSAL SERVICES</h2>
<h3>TERMS &amp; CONDITIONS</h3>

<h4>1. TERMS OF PAYMENT</h4>
<ol>
<li>All payment will be made advance of the month. In case payments are not received within month, service will be suspended.</li>
<li>Payment should be transfer by NEFT/RTGS/IMPS/Cheque &amp; online. (No Cash)</li>
<li>GST on BMW Services is 5% will be charged extra as per Govt. rule.</li>
</ol>

<h4>2. RESPONSIBILITIES OF THE SERVICE PROVIDER</h4>
<ol>
<li>The Service Provider shall comply with provisions stipulated in Schedule-I of the Bio-medical Waste Management Rules, 2016, as amended from time to time.</li>
<li>The Service Provider shall collect the segregated bio-medical waste from the designated collection point.</li>
<li>The Service Provider shall transport the segregated waste in closed container vehicle to its treatment facility.</li>
</ol>

<h4>3. RESPONSIBILITIES OF THE WASTE GENERATOR</h4>
<ol>
<li>The Waste Generator shall segregate bio-medical waste at the point of generation in accordance with the Bio-medical Waste Management Rules, 2016, as amended from time to time.</li>
<li>The Waste Generator shall collect, pack, label and handover the segregated BMW in non-chlorinated bags.</li>
<li>The Waste Generator shall take all necessary steps to ensure that the waste is handled without causing any adverse effect to human health and environment.</li>
</ol>

<h4>DECLARATION</h4>
<p>I/We have read and understood the entire contents of this agreement and give my/our free consent to the terms and conditions set out herein above.</p>
"""
    }
    
    override suspend fun uploadRentAgreement(context: Context, uri: Uri): String = 
        withContext(ioDispatcher) {
            if (!networkMonitor.isOnline()) {
                throw Exception("No internet connection")
            }
            
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(context, uri)
            
            // Create streaming request body from ContentResolver
            val requestBody = object : RequestBody() {
                override fun contentType(): MediaType? = mimeType.toMediaTypeOrNull()
                
                override fun contentLength(): Long {
                    return contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1
                }
                
                override fun writeTo(sink: BufferedSink) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        sink.writeAll(inputStream.source())
                    } ?: throw Exception("Cannot read file")
                }
            }
            
            val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
            val response = api.uploadRentAgreement(part)
            response["url"] ?: throw Exception("Upload failed: No URL returned")
        }
    
    private fun getFileName(context: Context, uri: Uri): String {
        var name = "document"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
