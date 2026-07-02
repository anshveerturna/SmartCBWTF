package com.smartcbwtf.mobile.repository

import com.smartcbwtf.mobile.database.entity.BagEventEntity
import com.smartcbwtf.mobile.network.model.SyncResponse
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class DefaultBagEventRepositoryMappingTest {

    @Test
    fun `resolveSuccessfulEventIds marks only matching successful ack positions`() {
        val firstId = UUID.randomUUID()
        val secondId = UUID.randomUUID()
        val pending = listOf(
            BagEventEntity(
                id = firstId,
                qrCode = "QR-1",
                eventType = "HCF_COLLECTION",
                eventTs = 1000L,
                gpsLat = 1.0,
                gpsLon = 1.0,
                weightKg = 1.0,
                hcfId = "h1",
                facilityId = "f1"
            ),
            BagEventEntity(
                id = secondId,
                qrCode = "QR-2",
                eventType = "HCF_COLLECTION",
                eventTs = 2000L,
                gpsLat = 2.0,
                gpsLon = 2.0,
                weightKg = 2.0,
                hcfId = "h2",
                facilityId = "f1"
            )
        )

        val acks = listOf(
            SyncResponse.Ack(qrCode = "QR-1", status = "SUCCESS", message = null),
            SyncResponse.Ack(qrCode = "QR-2", status = "FAILED", message = "bad data")
        )

        val result = resolveSuccessfulEventIds(pending, acks)
        assertEquals(listOf(firstId), result)
    }

    @Test
    fun `resolveSuccessfulEventIds rejects mismatched qr at same position`() {
        val firstId = UUID.randomUUID()
        val pending = listOf(
            BagEventEntity(
                id = firstId,
                qrCode = "QR-1",
                eventType = "HCF_COLLECTION",
                eventTs = 1000L,
                gpsLat = 1.0,
                gpsLon = 1.0,
                weightKg = 1.0,
                hcfId = "h1",
                facilityId = "f1"
            )
        )
        val acks = listOf(
            SyncResponse.Ack(qrCode = "QR-X", status = "SUCCESS", message = null)
        )

        val result = resolveSuccessfulEventIds(pending, acks)
        assertEquals(emptyList<UUID>(), result)
    }

    @Test
    fun `toPayload preserves gps accuracy event timestamp and facility`() {
        val entity = BagEventEntity(
            id = UUID.randomUUID(),
            qrCode = "QR-1",
            eventType = "HCF_COLLECTION",
            eventTs = 123456789L,
            gpsLat = 1.0,
            gpsLon = 2.0,
            gpsAccuracyM = 7.5,
            weightKg = 3.0,
            hcfId = "h1",
            facilityId = "f1",
            driverId = "driver-1"
        )

        val payload = entity.toPayload()

        assertEquals(7.5, payload.gpsAccuracyM!!, 0.0)
        assertEquals(Instant.ofEpochMilli(123456789L).toString(), payload.eventTs)
        assertEquals("f1", payload.facilityId)
    }
}
