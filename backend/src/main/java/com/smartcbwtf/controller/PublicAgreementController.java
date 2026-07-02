package com.smartcbwtf.controller;

import com.smartcbwtf.dto.AgreementVerificationDTO;
import com.smartcbwtf.repository.AgreementRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
public class PublicAgreementController {

    private final AgreementRepository agreementRepository;

    public PublicAgreementController(AgreementRepository agreementRepository) {
        this.agreementRepository = agreementRepository;
    }

    @GetMapping("/agreements/{id}/verify")
    public ResponseEntity<AgreementVerificationDTO> verifyAgreement(@PathVariable UUID id) {
        return agreementRepository.findById(id)
                .map(agreement -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(toPublicVerification(agreement)))
                .orElse(ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build());
    }

    private AgreementVerificationDTO toPublicVerification(com.smartcbwtf.domain.Agreement agreement) {
        String status = effectiveStatus(agreement);
        boolean valid = "ACTIVE".equals(status);
        var hcf = agreement.getHcf();
        var facility = agreement.getFacility();

        return new AgreementVerificationDTO(
                status,
                valid,
                hcf != null ? hcf.getName() : null,
                hcf != null ? hcf.getCode() : null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                agreement.getAgreementNumber(),
                agreement.getStartDate(),
                agreement.getEndDate(),
                facility != null ? facility.getName() : null,
                null,
                null,
                null,
                agreement.getCreatedAt());
    }

    private String effectiveStatus(com.smartcbwtf.domain.Agreement agreement) {
        if (agreement.getHcf() == null || agreement.getFacility() == null) {
            return "INVALID";
        }
        if (!"ACTIVE".equals(agreement.getStatus())) {
            return "INACTIVE";
        }
        if (agreement.getEndDate() != null && agreement.getEndDate().isBefore(LocalDate.now())) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }
}
