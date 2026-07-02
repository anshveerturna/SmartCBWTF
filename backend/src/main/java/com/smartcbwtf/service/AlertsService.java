package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.dto.AlertMissingBagDto;
import com.smartcbwtf.dto.AlertMismatchedBagDto;
import com.smartcbwtf.repository.BagEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertsService {

    private static final String UNKNOWN_CATEGORY = "UNKNOWN";
    private static final String UNKNOWN_HCF = "Unknown HCF";
    private static final String UNKNOWN_QR = "UNKNOWN_QR";
    private static final int MAX_ALERT_ROWS = 100;

    private final BagEventRepository bagEventRepository;
    private final long missingBagHours;

    public AlertsService(BagEventRepository bagEventRepository,
                         @Value("${app.alerts.missing-bag-hours:24}") long missingBagHours) {
        this.bagEventRepository = bagEventRepository;
        this.missingBagHours = missingBagHours;
    }

    public List<AlertMissingBagDto> listMissingBags() {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return List.of();
        }
        Instant cutoff = Instant.now().minus(missingBagHours, ChronoUnit.HOURS);
        List<BagEvent> missing = bagEventRepository.findMissingBags(facilityId, cutoff,
                PageRequest.of(0, MAX_ALERT_ROWS));
        List<AlertMissingBagDto> result = new ArrayList<>();
        for (BagEvent e : missing) {
            BagLabel label = e.getBagLabel();
            result.add(new AlertMissingBagDto(
                    labelIdOf(label),
                    qrCodeOf(label),
                    categoryOf(label),
                    hcfNameOf(e),
                    weightOf(e),
                    e.getEventTs(),
                    e.getGpsLat(),
                    e.getGpsLon(),
                    e.getCollectedByUserId()
            ));
        }
        return result;
    }

    public List<AlertMismatchedBagDto> listMismatchedBags() {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return List.of();
        }
        List<BagEvent> mismatches = bagEventRepository.findByFacilityIdAndEventTypeAndAnomalyStateOrderByEventTsDesc(
                facilityId,
                "CBWTF_VERIFICATION",
                "MISMATCH",
                PageRequest.of(0, MAX_ALERT_ROWS));
        List<AlertMismatchedBagDto> result = new ArrayList<>();
        for (BagEvent v : mismatches) {
            // find latest HCF_COLLECTION for same label
            BagLabel label = v.getBagLabel();
            UUID labelId = labelIdOf(label);
            Optional<BagEvent> hcfEventOpt = labelId != null
                    ? bagEventRepository.findFirstByBagLabelIdAndEventTypeOrderByEventTsDesc(labelId, "HCF_COLLECTION")
                    : Optional.empty();
            BigDecimal hcfWeight = hcfEventOpt.map(AlertsService::weightOf).orElse(BigDecimal.ZERO);
            BigDecimal verificationWeight = weightOf(v);
            BigDecimal delta = verificationWeight.subtract(hcfWeight).abs();
            result.add(new AlertMismatchedBagDto(
                    labelId,
                    qrCodeOf(label),
                    categoryOf(label),
                    hcfNameOf(v),
                    hcfWeight,
                    verificationWeight,
                    delta,
                    v.getEventTs()
            ));
        }
        return result;
    }

    private static UUID labelIdOf(BagLabel label) {
        return label != null ? label.getId() : null;
    }

    private static String qrCodeOf(BagLabel label) {
        if (label == null || label.getQrCode() == null || label.getQrCode().isBlank()) {
            return UNKNOWN_QR;
        }
        return label.getQrCode();
    }

    private static String categoryOf(BagLabel label) {
        if (label == null || label.getCategory() == null || label.getCategory().isBlank()) {
            return UNKNOWN_CATEGORY;
        }
        return label.getCategory();
    }

    private static String hcfNameOf(BagEvent event) {
        if (event.getHcf() == null || event.getHcf().getName() == null || event.getHcf().getName().isBlank()) {
            return UNKNOWN_HCF;
        }
        return event.getHcf().getName();
    }

    private static BigDecimal weightOf(BagEvent event) {
        return event.getWeightKg() != null ? event.getWeightKg() : BigDecimal.ZERO;
    }
}
