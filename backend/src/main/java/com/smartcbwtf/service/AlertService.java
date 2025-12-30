package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.AlertRepository;
import com.smartcbwtf.repository.FacilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Alert Service - Creates and manages alerts.
 * 
 * INVARIANTS:
 * - Alerts are idempotent (UNIQUE event_id + type)
 * - Alerts are immutable (except is_read)
 * - All alerts are tenant-scoped
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final FacilityRepository facilityRepository;

    public AlertService(AlertRepository alertRepository, FacilityRepository facilityRepository) {
        this.alertRepository = alertRepository;
        this.facilityRepository = facilityRepository;
    }

    /**
     * Create an alert if not already exists for this event.
     * Returns true if alert was created, false if already existed.
     */
    @Transactional
    public boolean createAlert(UUID eventId, UUID facilityId, AlertType type, AlertSeverity severity,
            String title, String message, String relatedEntityType, UUID relatedEntityId) {
        // Idempotency check
        if (alertRepository.existsByEventIdAndType(eventId, type)) {
            log.debug("Alert already exists for event {} type {}", eventId, type);
            return false;
        }

        Facility facility = facilityRepository.findById(facilityId).orElse(null);
        if (facility == null) {
            log.error("Facility not found: {}", facilityId);
            return false;
        }

        Alert alert = new Alert();
        alert.setEventId(eventId);
        alert.setFacility(facility);
        alert.setType(type);
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setRelatedEntityType(relatedEntityType);
        alert.setRelatedEntityId(relatedEntityId);

        try {
            alertRepository.save(alert);
            log.info("Created alert: type={} facility={} title={}", type, facilityId, title);
            return true;
        } catch (Exception e) {
            // Handle duplicate key from race condition
            log.warn("Failed to create alert (likely duplicate): {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get alerts for facility with pagination.
     */
    public Page<Alert> getAlerts(UUID facilityId, Pageable pageable) {
        return alertRepository.findByFacilityId(facilityId, pageable);
    }

    /**
     * Get alerts by category.
     */
    public Page<Alert> getAlertsByCategory(UUID facilityId, String category, Pageable pageable) {
        List<AlertType> types = getTypesByCategory(category);
        if (types.isEmpty()) {
            return alertRepository.findByFacilityId(facilityId, pageable);
        }
        return alertRepository.findByFacilityIdAndTypeIn(facilityId, types, pageable);
    }

    private List<AlertType> getTypesByCategory(String category) {
        return switch (category.toUpperCase()) {
            case "FINANCIAL" -> Arrays.asList(
                    AlertType.BILL_GENERATED, AlertType.PAYMENT_DUE,
                    AlertType.PAYMENT_REMINDER_SENT, AlertType.PAYMENT_OVERDUE);
            case "OPERATIONAL" -> Arrays.asList(
                    AlertType.UNVERIFIED_BAG_DETECTED, AlertType.WEIGHT_MISMATCH_DETECTED,
                    AlertType.GPS_ANOMALY_DETECTED, AlertType.MISSED_PICKUP_DETECTED,
                    AlertType.BLUE_WASTE_BELOW_THRESHOLD);
            case "COMPLIANCE" -> Arrays.asList(
                    AlertType.AGREEMENT_EXPIRING, AlertType.AGREEMENT_EXPIRED,
                    AlertType.REPORT_GENERATED, AlertType.REPORT_FLAGGED);
            default -> List.of();
        };
    }

    /**
     * Get unread count.
     */
    public long getUnreadCount(UUID facilityId) {
        return alertRepository.countByFacilityIdAndIsReadFalse(facilityId);
    }

    /**
     * Mark alert as read.
     */
    @Transactional
    public boolean markAsRead(UUID alertId, UUID facilityId) {
        return alertRepository.markAsRead(alertId, facilityId) > 0;
    }

    /**
     * Mark all alerts as read.
     */
    @Transactional
    public int markAllAsRead(UUID facilityId) {
        return alertRepository.markAllAsRead(facilityId);
    }
}
