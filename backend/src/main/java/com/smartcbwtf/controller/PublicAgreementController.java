package com.smartcbwtf.controller;

import com.smartcbwtf.dto.AgreementVerificationDTO;
import com.smartcbwtf.repository.AgreementRepository;
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
                .map(agreement -> {
                    boolean isExpired = agreement.getEndDate() != null
                            && agreement.getEndDate().isBefore(LocalDate.now());
                    boolean isActive = "ACTIVE".equals(agreement.getStatus());

                    String status;
                    boolean valid;

                    if (!isActive) {
                        status = "INACTIVE";
                        valid = false;
                    } else if (isExpired) {
                        status = "EXPIRED";
                        valid = false;
                    } else {
                        status = "ACTIVE";
                        valid = true;
                    }

                    String billingInfo = agreement.getHcf().getBillingModel() != null
                            ? agreement.getHcf().getBillingModel().name()
                            : "BEDDED";
                    if (Boolean.FALSE.equals(agreement.getHcf().getBedded())) {
                        billingInfo = "FIXED (Non-Bedded)";
                    }

                    return ResponseEntity.ok(new AgreementVerificationDTO(
                            status,
                            valid,
                            agreement.getHcf().getName(),
                            agreement.getHcf().getCode(),
                            agreement.getHcf().getAddress(),
                            agreement.getHcf().getState(),
                            agreement.getHcf().getPincode(),
                            agreement.getHcf().getHcfType() != null ? agreement.getHcf().getHcfType().name() : null,
                            agreement.getHcf().getContactEmail(),
                            agreement.getHcf().getDoctorName(),
                            agreement.getHcf().getContactPhone(),
                            agreement.getHcf().getNumberOfBeds(),
                            agreement.getAgreementNumber(),
                            agreement.getStartDate(),
                            agreement.getEndDate(),
                            agreement.getFacility().getName(),
                            agreement.getFacility().getAddress(),
                            agreement.getFacility().getContactPhone() + " / "
                                    + agreement.getFacility().getContactEmail(),
                            billingInfo,
                            agreement.getCreatedAt()));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
