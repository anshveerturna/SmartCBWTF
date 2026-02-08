package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.FacilityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Mobile facility endpoints used by Android verification flow.
 */
@RestController
@RequestMapping("/api/facilities")
@PreAuthorize("hasAnyRole('DRIVER', 'PLANT_OPERATOR', 'CBWTF_ADMIN')")
public class MobileFacilityController {

    private final FacilityRepository facilityRepository;

    public MobileFacilityController(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @GetMapping("/active")
    public ResponseEntity<FacilityInfoDTO> getActiveFacility() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.notFound().build();
        }
        return facilityRepository.findById(tenantId)
                .map(f -> ResponseEntity.ok(toDto(f)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacilityInfoDTO> getFacility(@PathVariable("id") UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null || !tenantId.equals(id)) {
            return ResponseEntity.notFound().build();
        }
        return facilityRepository.findById(id)
                .map(f -> ResponseEntity.ok(toDto(f)))
                .orElse(ResponseEntity.notFound().build());
    }

    private FacilityInfoDTO toDto(Facility facility) {
        return new FacilityInfoDTO(
                facility.getId().toString(),
                facility.getCode(),
                facility.getName(),
                facility.getGpsLat(),
                facility.getGpsLon(),
                facility.getGeofenceRadiusM());
    }

    public record FacilityInfoDTO(
            String id,
            String code,
            String name,
            Double gpsLat,
            Double gpsLon,
            Integer geofenceRadiusM) {
    }
}
