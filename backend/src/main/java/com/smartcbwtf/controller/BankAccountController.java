package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.BankAccount;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.BankAccountRepository;
import com.smartcbwtf.repository.FacilityRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bank Account management controller for CBWTF Admin.
 * Supports Phase 10 payments integration.
 * Cannot delete accounts - only disable.
 */
@RestController
@RequestMapping("/api/cbwtf/bank-accounts")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class BankAccountController {

    private static final Logger log = LoggerFactory.getLogger(BankAccountController.class);

    private final BankAccountRepository bankAccountRepository;
    private final FacilityRepository facilityRepository;

    public BankAccountController(BankAccountRepository bankAccountRepository, FacilityRepository facilityRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.facilityRepository = facilityRepository;
    }

    // ========== LIST ACCOUNTS ==========

    @GetMapping
    public ResponseEntity<List<BankAccountDTO>> listAccounts(
            @RequestParam(name = "status", required = false) String status) {
        UUID facilityId = getCurrentFacilityId();
        if (facilityId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<BankAccount> accounts;
        if ("ACTIVE".equalsIgnoreCase(status)) {
            accounts = bankAccountRepository.findByFacilityIdAndStatus(facilityId, BankAccount.Status.ACTIVE);
        } else {
            accounts = bankAccountRepository.findByFacilityIdOrderByIsPrimaryDescCreatedAtDesc(facilityId);
        }

        List<BankAccountDTO> result = accounts.stream().map(BankAccountDTO::from).toList();
        return ResponseEntity.ok(result);
    }

    // ========== GET ACCOUNT ==========

    @GetMapping("/{id}")
    public ResponseEntity<BankAccountDTO> getAccount(@PathVariable("id") UUID id) {
        UUID facilityId = getCurrentFacilityId();
        return bankAccountRepository.findById(id)
                .filter(a -> a.getFacility().getId().equals(facilityId))
                .map(BankAccountDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== CREATE ACCOUNT ==========

    @PostMapping
    @Transactional
    public ResponseEntity<BankAccountDTO> createAccount(@Valid @RequestBody CreateBankAccountRequest request) {
        UUID facilityId = getCurrentFacilityId();
        if (facilityId == null) {
            return ResponseEntity.badRequest().build();
        }

        Facility facility = facilityRepository.findById(facilityId).orElse(null);
        if (facility == null) {
            return ResponseEntity.badRequest().build();
        }

        // If this is the first active account, make it primary
        boolean isFirst = bankAccountRepository.countByFacilityIdAndStatus(facilityId, BankAccount.Status.ACTIVE) == 0;

        BankAccount account = new BankAccount();
        account.setFacility(facility);
        account.setAccountName(request.accountName());
        account.setAccountNumber(request.accountNumber());
        account.setIfscCode(request.ifscCode().toUpperCase());
        account.setBankName(request.bankName());
        account.setUpiId(request.upiId());
        account.setIsPrimary(isFirst);
        account.setStatus(BankAccount.Status.ACTIVE);

        account = bankAccountRepository.save(account);
        log.info("Created bank account {} for facility {}", account.getId(), facilityId);

        return ResponseEntity.ok(BankAccountDTO.from(account));
    }

    // ========== UPDATE ACCOUNT ==========

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<BankAccountDTO> updateAccount(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateBankAccountRequest request) {

        UUID facilityId = getCurrentFacilityId();
        return bankAccountRepository.findById(id)
                .filter(a -> a.getFacility().getId().equals(facilityId))
                .filter(a -> a.getStatus() == BankAccount.Status.ACTIVE) // Can only update active accounts
                .map(account -> {
                    account.setAccountName(request.accountName());
                    account.setAccountNumber(request.accountNumber());
                    account.setIfscCode(request.ifscCode().toUpperCase());
                    account.setBankName(request.bankName());
                    account.setUpiId(request.upiId());
                    return ResponseEntity.ok(BankAccountDTO.from(bankAccountRepository.save(account)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== DISABLE ACCOUNT (No delete allowed) ==========

    @PutMapping("/{id}/disable")
    @Transactional
    public ResponseEntity<?> disableAccount(@PathVariable("id") UUID id) {
        UUID facilityId = getCurrentFacilityId();
        return bankAccountRepository.findById(id)
                .filter(a -> a.getFacility().getId().equals(facilityId))
                .filter(a -> a.getStatus() == BankAccount.Status.ACTIVE)
                .map(account -> {
                    boolean wasPrimary = Boolean.TRUE.equals(account.getIsPrimary());

                    account.setStatus(BankAccount.Status.DISABLED);
                    account.setDisabledAt(Instant.now());
                    account.setIsPrimary(false);
                    bankAccountRepository.save(account);

                    // If we disabled the primary, set another active one as primary
                    if (wasPrimary) {
                        bankAccountRepository.findByFacilityIdAndStatus(facilityId, BankAccount.Status.ACTIVE)
                                .stream().findFirst().ifPresent(a -> {
                                    a.setIsPrimary(true);
                                    bankAccountRepository.save(a);
                                });
                    }

                    log.info("Disabled bank account {} for facility {}", id, facilityId);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Account disabled"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== SET PRIMARY ==========

    @PostMapping("/{id}/set-primary")
    @Transactional
    public ResponseEntity<BankAccountDTO> setPrimary(@PathVariable("id") UUID id) {
        UUID facilityId = getCurrentFacilityId();
        return bankAccountRepository.findById(id)
                .filter(a -> a.getFacility().getId().equals(facilityId))
                .filter(a -> a.getStatus() == BankAccount.Status.ACTIVE) // Only active accounts can be primary
                .map(account -> {
                    // Clear all primary flags for this facility
                    bankAccountRepository.clearPrimaryForFacility(facilityId);

                    // Set this one as primary
                    account.setIsPrimary(true);
                    account = bankAccountRepository.save(account);

                    log.info("Set bank account {} as primary for facility {}", id, facilityId);
                    return ResponseEntity.ok(BankAccountDTO.from(account));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== HELPERS ==========

    private UUID getCurrentFacilityId() {
        TenantContext.TenantInfo info = TenantContext.get();
        return info != null ? info.tenantId() : null;
    }

    // ========== DTOs ==========

    public record BankAccountDTO(
            UUID id,
            String accountName,
            String accountNumber,
            String ifscCode,
            String bankName,
            String upiId,
            boolean isPrimary,
            String status,
            String createdAt,
            String disabledAt) {
        public static BankAccountDTO from(BankAccount account) {
            return new BankAccountDTO(
                    account.getId(),
                    account.getAccountName(),
                    account.getAccountNumber(),
                    account.getIfscCode(),
                    account.getBankName(),
                    account.getUpiId(),
                    Boolean.TRUE.equals(account.getIsPrimary()),
                    account.getStatus().name(),
                    account.getCreatedAt().toString(),
                    account.getDisabledAt() != null ? account.getDisabledAt().toString() : null);
        }
    }

    public record CreateBankAccountRequest(
            @NotBlank String accountName,
            @NotBlank String accountNumber,
            @NotBlank String ifscCode,
            @NotBlank String bankName,
            String upiId) {
    }

    public record UpdateBankAccountRequest(
            @NotBlank String accountName,
            @NotBlank String accountNumber,
            @NotBlank String ifscCode,
            @NotBlank String bankName,
            String upiId) {
    }
}
