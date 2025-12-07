package com.smartcbwtf.service;

import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.dto.AlertMissingBagDto;
import com.smartcbwtf.dto.AlertMismatchedBagDto;
import com.smartcbwtf.repository.BagEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AlertsService {

    private final BagEventRepository bagEventRepository;
    private final long missingBagHours;

    public AlertsService(BagEventRepository bagEventRepository,
                         @Value("${app.alerts.missing-bag-hours:24}") long missingBagHours) {
        this.bagEventRepository = bagEventRepository;
        this.missingBagHours = missingBagHours;
    }

    public List<AlertMissingBagDto> listMissingBags() {
        Instant cutoff = Instant.now().minus(missingBagHours, ChronoUnit.HOURS);
        List<BagEvent> missing = bagEventRepository.findMissingBags(cutoff);
        List<AlertMissingBagDto> result = new ArrayList<>();
        for (BagEvent e : missing) {
            BagLabel label = e.getBagLabel();
            result.add(new AlertMissingBagDto(
                    label.getId(),
                    label.getQrCode(),
                    label.getCategory(),
                    e.getHcf().getName(),
                    e.getWeightKg(),
                    e.getEventTs(),
                    e.getGpsLat(),
                    e.getGpsLon(),
                    e.getCollectedByUserId()
            ));
        }
        return result;
    }

    public List<AlertMismatchedBagDto> listMismatchedBags() {
        List<BagEvent> mismatches = bagEventRepository.findByEventTypeAndAnomalyState("CBWTF_VERIFICATION", "MISMATCH");
        List<AlertMismatchedBagDto> result = new ArrayList<>();
        for (BagEvent v : mismatches) {
            // find latest HCF_COLLECTION for same label
            Optional<BagEvent> hcfEventOpt = bagEventRepository.findFirstByBagLabelIdAndEventTypeOrderByEventTsDesc(v.getBagLabel().getId(), "HCF_COLLECTION");
            BigDecimal hcfWeight = hcfEventOpt.map(BagEvent::getWeightKg).orElse(BigDecimal.ZERO);
            BigDecimal delta = v.getWeightKg().subtract(hcfWeight).abs();
            result.add(new AlertMismatchedBagDto(
                    v.getBagLabel().getId(),
                    v.getBagLabel().getQrCode(),
                    v.getBagLabel().getCategory(),
                    v.getHcf().getName(),
                    hcfWeight,
                    v.getWeightKg(),
                    delta,
                    v.getEventTs()
            ));
        }
        return result;
    }
}
