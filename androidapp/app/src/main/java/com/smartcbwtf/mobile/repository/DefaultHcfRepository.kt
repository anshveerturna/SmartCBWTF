package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.database.dao.HcfDao
import com.smartcbwtf.mobile.database.entity.HcfEntity
import com.smartcbwtf.mobile.network.api.HcfApi
import com.smartcbwtf.mobile.network.model.HcfRegistrationRequest
import com.smartcbwtf.mobile.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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

    override suspend fun register(
        name: String,
        address: String?,
        city: String?,
        state: String?,
        postalCode: String?,
        phone: String?,
        email: String?,
        beds: Int?,
        latitude: Double?,
        longitude: Double?
    ) = withContext(ioDispatcher) {
        if (!networkMonitor.isOnline()) {
            throw Exception("No internet connection")
        }
        api.register(
            HcfRegistrationRequest(
                name = name,
                address = address,
                city = city,
                state = state,
                postalCode = postalCode,
                phone = phone,
                email = email,
                beds = beds,
                latitude = latitude,
                longitude = longitude
            )
        )
    }
}
