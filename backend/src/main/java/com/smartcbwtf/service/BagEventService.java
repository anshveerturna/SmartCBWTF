package com.smartcbwtf.service;

import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.BagEventSyncItem;
import com.smartcbwtf.dto.BagEventSyncRequest;
import com.smartcbwtf.dto.BagEventSyncResponse;
import com.smartcbwtf.dto.BagVerifyRequest;
import com.smartcbwtf.dto.BagVerifyResponse;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BagEventService {

    private final BagLabelRepository bagLabelRepository;
    private final BagEventRepository bagEventRepository;
    private final AuditLogService auditLogService;
    private final double hcfGeofenceRadiusM;
    private final double facilityDefaultRadiusM;
    private final double mismatchThresholdKg;
    private final double maxGpsAccuracyM;

    public BagEventService(BagLabelRepository bagLabelRepository,
                           BagEventRepository bagEventRepository,
                           AuditLogService auditLogService,
                           @Value("${app.geofence.hcf-radius-m:200}") double hcfGeofenceRadiusM,
                           @Value("${app.geofence.facility-default-radius-m:200}") double facilityDefaultRadiusM,
                           @Value("${app.weight.mismatch-threshold-kg:0.5}") double mismatchThresholdKg,
                           @Value("${app.gps.max-accuracy-m:100}") double maxGpsAccuracyM) {
        this.bagLabelRepository = bagLabelRepository;
        this.bagEventRepository = bagEventRepository;
        this.auditLogService = auditLogService;
        this.hcfGeofenceRadiusM = hcfGeofenceRadiusM;
        this.facilityDefaultRadiusM = facilityDefaultRadiusM;
        this.mismatchThresholdKg = mismatchThresholdKg;
        this.maxGpsAccuracyM = maxGpsAccuracyM;
    }

    @Transactional
    public BagEventSyncResponse sync(BagEventSyncRequest request) {
        List<BagEventSyncResponse.Ack> acks = new ArrayList<>();
        for (BagEventSyncItem item : request.getEvents()) {
            try {
                BagLabel label = bagLabelRepository.findByQrCode(item.getQrCode())
                        .orElseThrow(() -> new IllegalArgumentException("Label not found"));
                validateEventType(item.getEventType());
                String normalizedEventType = item.getEventType().toUpperCase();
                Instant eventTs = item.getEventTs() != null ? item.getEventTs() : Instant.now();

                if (bagEventRepository.existsByBagLabelIdAndEventTypeAndEventTs(label.getId(), normalizedEventType, eventTs)) {
                    throw new IllegalArgumentException("Duplicate event for label and timestamp");
                }

                if ("HCF_COLLECTION".equalsIgnoreCase(item.getEventType())
                        && !"ISSUED".equalsIgnoreCase(label.getStatus())) {
                    throw new IllegalStateException("Label not in ISSUED status");
                }

                validateFacility(item, label);
                validateGpsAccuracy(item);

                BagEvent event = new BagEvent();
                event.setBagLabel(label);
                event.setFacility(label.getFacility());
                event.setHcf(label.getHcf());
                event.setEventType(normalizedEventType);
                event.setEventTs(eventTs);
                event.setGpsLat(item.getGpsLat());
                event.setGpsLon(item.getGpsLon());
                event.setWeightKg(item.getWeightKg());
                event.setCollectedByUserId(item.getCollectedByUserId());
                event.setAppDeviceId(item.getAppDeviceId());
                event.setNotes(item.getNotes());

                String anomaly = geofenceCheck(item, label.getHcf(), label.getFacility());
                MismatchResult mismatchResult = new MismatchResult(anomaly, null);
                if ("CBWTF_VERIFICATION".equalsIgnoreCase(item.getEventType())) {
                    mismatchResult = evaluateMismatch(label.getId(), item.getWeightKg(), anomaly);
                    anomaly = mismatchResult.getAnomaly();
                    label.setStatus("USED");
                    label.setUsedAt(event.getEventTs());
                    bagLabelRepository.save(label);
                }
                event.setAnomalyState(anomaly);
                bagEventRepository.save(event);
                String dataJson = buildAuditDataJson(mismatchResult);
                auditLogService.log("BAG_EVENT", event.getId(), "CREATE", item.getCollectedByUserId(), dataJson);
                acks.add(new BagEventSyncResponse.Ack(item.getQrCode(), "SUCCESS", anomaly));
            } catch (Exception ex) {
                acks.add(new BagEventSyncResponse.Ack(item.getQrCode(), "FAILED", ex.getMessage()));
            }
        }
        return new BagEventSyncResponse(acks);
    }

    private void validateEventType(String eventType) {
        if (!"HCF_COLLECTION".equalsIgnoreCase(eventType) && !"CBWTF_VERIFICATION".equalsIgnoreCase(eventType)) {
            throw new IllegalArgumentException("Unsupported event type");
        }
    }

    private void validateFacility(BagEventSyncItem item, BagLabel label) {
        if (label.getFacility() == null) {
            throw new IllegalStateException("Label missing facility assignment");
        }
        if (item.getFacilityId() != null && !item.getFacilityId().equals(label.getFacility().getId())) {
            throw new IllegalArgumentException("Facility does not match label facility");
        }
    }

    private void validateGpsAccuracy(BagEventSyncItem item) {
        if (item.getGpsAccuracyM() != null && item.getGpsAccuracyM() > maxGpsAccuracyM) {
            throw new IllegalArgumentException("GPS accuracy too low");
        }
    }

    private String geofenceCheck(BagEventSyncItem item, Hcf hcf, Facility facility) {
        if (item.getGpsLat() == null || item.getGpsLon() == null) {
            return "OK";
        }
        double lat = item.getGpsLat();
        double lon = item.getGpsLon();
        if ("HCF_COLLECTION".equalsIgnoreCase(item.getEventType())) {
            double distance = haversineMeters(lat, lon, hcf.getGpsLat(), hcf.getGpsLon());
            return distance > hcfGeofenceRadiusM ? "OUT_OF_GEOFENCE" : "OK";
        }
        if ("CBWTF_VERIFICATION".equalsIgnoreCase(item.getEventType())) {
            Double facLat = facility.getGpsLat();
            Double facLon = facility.getGpsLon();
            if (facLat == null || facLon == null) {
                return "OK";
            }
            double radius = facility.getGeofenceRadiusM() != null ? facility.getGeofenceRadiusM() : facilityDefaultRadiusM;
            double distance = haversineMeters(lat, lon, facLat, facLon);
            return distance > radius ? "OUT_OF_GEOFENCE" : "OK";
        }
        return "OK";
    }

    private MismatchResult evaluateMismatch(UUID bagLabelId, BigDecimal cbtwfWeight, String existingAnomaly) {
        Optional<BagEvent> latestCollection = bagEventRepository.findFirstByBagLabelIdAndEventTypeOrderByEventTsDesc(bagLabelId, "HCF_COLLECTION");
        if (latestCollection.isEmpty()) {
            return new MismatchResult(existingAnomaly, null);
        }
        BigDecimal hcfWeight = latestCollection.get().getWeightKg();
        BigDecimal delta = cbtwfWeight.subtract(hcfWeight).abs();
        if (delta.compareTo(BigDecimal.valueOf(mismatchThresholdKg)) > 0) {
            return new MismatchResult("MISMATCH", delta);
        }
        return new MismatchResult(existingAnomaly, null);
    }

    private String buildAuditDataJson(MismatchResult mismatchResult) {
        if ("MISMATCH".equals(mismatchResult.getAnomaly()) && mismatchResult.getDelta() != null) {
            return "{\"mismatchKg\":" + mismatchResult.getDelta() + "}";
        }
        return null;
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000.0; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    private static class MismatchResult {
        private final String anomaly;
        private final BigDecimal delta;

        MismatchResult(String anomaly, BigDecimal delta) {
            this.anomaly = anomaly;
            this.delta = delta;
        }

        String getAnomaly() { return anomaly; }
        BigDecimal getDelta() { return delta; }
    }

    /**
     * Dedicated verification endpoint for CBWTF facility.
     * Returns geofence violation with 403, idempotency conflict with 409.
     */
    @Transactional
    public VerifyResult verifyBag(BagVerifyRequest request) {
        // Find bag label
        BagLabel label = bagLabelRepository.findByQrCode(request.getQrCode())
                .orElse(null);
        if (label == null) {
            return new VerifyResult(404, BagVerifyResponse.error("NOT_FOUND", "Bag label not found"));
        }

        // Check idempotency - already verified
        if (bagEventRepository.existsByBagLabelIdAndEventType(label.getId(), "CBWTF_VERIFICATION")) {
            return new VerifyResult(409, BagVerifyResponse.error("ALREADY_VERIFIED", "Bag has already been verified"));
        }

        Facility facility = label.getFacility();
        if (facility == null) {
            return new VerifyResult(400, BagVerifyResponse.error("NO_FACILITY", "Bag label not assigned to a facility"));
        }

        // Server-side geofence check (authoritative)
        Double facLat = facility.getGpsLat();
        Double facLon = facility.getGpsLon();
        if (facLat != null && facLon != null && request.getGpsLat() != null && request.getGpsLon() != null) {
            double radius = facility.getGeofenceRadiusM() != null ? facility.getGeofenceRadiusM() : facilityDefaultRadiusM;
            double distance = haversineMeters(request.getGpsLat(), request.getGpsLon(), facLat, facLon);
            if (distance > radius) {
                return new VerifyResult(403, BagVerifyResponse.error("OUT_OF_GEOFENCE",
                        String.format("Device is %.0fm from facility (max %.0fm)", distance, radius)));
            }
        }

        // Create verification event
        Instant verifiedAt = request.getEventTs() != null ? request.getEventTs() : Instant.now();

        BagEvent event = new BagEvent();
        event.setBagLabel(label);
        event.setFacility(facility);
        event.setHcf(label.getHcf());
        event.setEventType("CBWTF_VERIFICATION");
        event.setEventTs(verifiedAt);
        event.setGpsLat(request.getGpsLat());
        event.setGpsLon(request.getGpsLon());
        event.setWeightKg(request.getWeightKg());
        event.setCollectedByUserId(request.getVerifiedByUserId());
        event.setAppDeviceId(request.getDeviceId());
        event.setNotes(request.getNotes());

        // Check for weight mismatch
        MismatchResult mismatchResult = evaluateMismatch(label.getId(), request.getWeightKg(), "OK");

        event.setAnomalyState(mismatchResult.getAnomaly());
        bagEventRepository.save(event);

        // Update label status
        label.setStatus("USED");
        label.setUsedAt(verifiedAt);
        bagLabelRepository.save(label);

        // Audit log
        String dataJson = buildAuditDataJson(mismatchResult);
        auditLogService.log("BAG_EVENT", event.getId(), "VERIFY", request.getVerifiedByUserId(), dataJson);

        BagVerifyResponse response = BagVerifyResponse.success(
                label.getId(),
                verifiedAt,
                mismatchResult.getAnomaly(),
                mismatchResult.getDelta()
        );

        return new VerifyResult(200, response);
    }

    /**
     * Result wrapper with HTTP status code
     */
    public static class VerifyResult {
        private final int httpStatus;
        private final BagVerifyResponse response;

        public VerifyResult(int httpStatus, BagVerifyResponse response) {
            this.httpStatus = httpStatus;
            this.response = response;
        }

        public int getHttpStatus() { return httpStatus; }
        public BagVerifyResponse getResponse() { return response; }
    }
}
