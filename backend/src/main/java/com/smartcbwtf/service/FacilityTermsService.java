package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilityTerms;
import com.smartcbwtf.dto.TermsCreateRequest;
import com.smartcbwtf.dto.TermsListItem;
import com.smartcbwtf.dto.TermsResponse;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.FacilityTermsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing facility-specific Terms & Conditions.
 */
@Service
public class FacilityTermsService {

    private final FacilityTermsRepository termsRepository;
    private final FacilityRepository facilityRepository;
    private final AppUserRepository userRepository;

    public FacilityTermsService(
            FacilityTermsRepository termsRepository,
            FacilityRepository facilityRepository,
            AppUserRepository userRepository) {
        this.termsRepository = termsRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get the latest active terms for a facility.
     * Falls back to any active terms if facility-specific not found.
     */
    public Optional<TermsResponse> getLatestTerms(UUID facilityId) {
        // First try facility-specific if provided
        Optional<FacilityTerms> terms = Optional.empty();
        if (facilityId != null) {
            terms = termsRepository.findByFacilityIdAndActiveTrue(facilityId);
        }
        
        // Fallback to any active terms (global default)
        if (terms.isEmpty()) {
            List<FacilityTerms> allActive = termsRepository.findAllActive();
            if (!allActive.isEmpty()) {
                terms = Optional.of(allActive.get(0));
            }
        }

        return terms.map(this::toResponse);
    }

    /**
     * Convert FacilityTerms entity to TermsResponse DTO
     */
    private TermsResponse toResponse(FacilityTerms t) {
        String facilityName = t.getFacility() != null ? t.getFacility().getName() : null;
        UUID facId = t.getFacility() != null ? t.getFacility().getId() : null;
        return new TermsResponse(
                t.getId(),
                facId,
                facilityName,
                t.getVersion(),
                t.getEffectiveFrom(),
                t.getTextHtml(),
                Boolean.TRUE.equals(t.getActive())
        );
    }

    /**
     * Get the active FacilityTerms entity for a facility.
     */
    public Optional<FacilityTerms> getActiveTermsEntity(UUID facilityId) {
        Optional<FacilityTerms> terms = Optional.empty();
        if (facilityId != null) {
            terms = termsRepository.findByFacilityIdAndActiveTrue(facilityId);
        }
        if (terms.isEmpty()) {
            List<FacilityTerms> allActive = termsRepository.findAllActive();
            if (!allActive.isEmpty()) {
                return Optional.of(allActive.get(0));
            }
        }
        return terms;
    }

    /**
     * List all terms versions for a facility.
     */
    public List<TermsListItem> listTerms(UUID facilityId) {
        return termsRepository.findByFacilityIdOrderByEffectiveFromDesc(facilityId)
                .stream()
                .map(t -> new TermsListItem(
                        t.getId(),
                        t.getVersion(),
                        t.getEffectiveFrom(),
                        t.getActive(),
                        t.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Create new terms version for a facility.
     */
    @Transactional
    public FacilityTerms createTerms(UUID facilityId, TermsCreateRequest request) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));

        // Check for duplicate version
        if (termsRepository.findByFacilityIdAndVersion(facilityId, request.getVersion()).isPresent()) {
            throw new IllegalArgumentException("Terms version already exists: " + request.getVersion());
        }

        FacilityTerms terms = new FacilityTerms();
        terms.setFacility(facility);
        terms.setVersion(request.getVersion());
        terms.setTextHtml(request.getTextHtml());
        terms.setEffectiveFrom(request.getEffectiveFrom());
        terms.setCreatedAt(Instant.now());
        terms.setUpdatedAt(Instant.now());

        // Set creator if provided
        if (request.getCreatedByUserId() != null) {
            userRepository.findById(request.getCreatedByUserId())
                    .ifPresent(terms::setCreatedBy);
        }

        // Handle activation
        if (Boolean.TRUE.equals(request.getSetActive())) {
            termsRepository.deactivateAllForFacility(facilityId);
            terms.setActive(true);
        } else {
            terms.setActive(false);
        }

        return termsRepository.save(terms);
    }

    /**
     * Activate a specific terms version.
     */
    @Transactional
    public FacilityTerms activateTerms(UUID facilityId, UUID termsId) {
        FacilityTerms terms = termsRepository.findById(termsId)
                .orElseThrow(() -> new IllegalArgumentException("Terms not found: " + termsId));

        if (!terms.getFacility().getId().equals(facilityId)) {
            throw new IllegalArgumentException("Terms does not belong to facility");
        }

        // Deactivate all other terms for this facility
        termsRepository.deactivateAllForFacility(facilityId);

        // Activate this one
        terms.setActive(true);
        terms.setUpdatedAt(Instant.now());
        return termsRepository.save(terms);
    }
}
