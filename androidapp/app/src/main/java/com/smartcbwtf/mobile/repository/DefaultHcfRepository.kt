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
                when (e.code()) {
                    409 -> throw Exception("Another HCF is already registered at this location. Please move to a different location and try again.")
                    400 -> throw Exception("Invalid registration data. Please check your inputs.")
                    else -> throw Exception("Registration failed: ${e.message()}")
                }
            }
        }
    
    override suspend fun getLatestTerms(facilityId: String?): TermsResponse = 
        withContext(ioDispatcher) {
            if (!networkMonitor.isOnline()) {
                throw Exception("No internet connection")
            }
            api.getLatestTerms(facilityId)
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

