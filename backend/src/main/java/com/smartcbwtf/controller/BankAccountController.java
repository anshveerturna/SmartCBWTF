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

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bank Account management controller for CBWTF Admin.
 * Allows CBWTF admins to manage their facility's bank accounts.
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
    public ResponseEntity<List<BankAccountDTO>> listAccounts() {
        UUID facilityId = getCurrentFacilityId();
        if (facilityId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<BankAccount> accounts = bankAccountRepository
                .findByFacilityIdOrderByIsPrimaryDescCreatedAtDesc(facilityId);
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

        // If this is the first account, make it primary
        boolean isFirst = bankAccountRepository.countByFacilityId(facilityId) == 0;

        BankAccount account = new BankAccount();
        account.setFacility(facility);
        account.setAccountName(request.accountName());
        account.setAccountNumber(request.accountNumber());
        account.setIfscCode(request.ifscCode().toUpperCase());
        account.setBankName(request.bankName());
        account.setBranchName(request.branchName());
        account.setIsPrimary(isFirst);

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
                .map(account -> {
                    account.setAccountName(request.accountName());
                    account.setAccountNumber(request.accountNumber());
                    account.setIfscCode(request.ifscCode().toUpperCase());
                    account.setBankName(request.bankName());
                    account.setBranchName(request.branchName());
                    return ResponseEntity.ok(BankAccountDTO.from(bankAccountRepository.save(account)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== DELETE ACCOUNT ==========

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteAccount(@PathVariable("id") UUID id) {
        UUID facilityId = getCurrentFacilityId();
        return bankAccountRepository.findById(id)
                .filter(a -> a.getFacility().getId().equals(facilityId))
                .map(account -> {
                    boolean wasPrimary = Boolean.TRUE.equals(account.getIsPrimary());
                    bankAccountRepository.delete(account);

                    // If we deleted the primary, set another one as primary
                    if (wasPrimary) {
                        bankAccountRepository.findByFacilityIdOrderByIsPrimaryDescCreatedAtDesc(facilityId)
                                .stream().findFirst().ifPresent(a -> {
                                    a.setIsPrimary(true);
                                    bankAccountRepository.save(a);
                                });
                    }

                    log.info("Deleted bank account {} for facility {}", id, facilityId);
                    return ResponseEntity.noContent().<Void>build();
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
            String branchName,
            boolean isPrimary,
            String createdAt) {
        public static BankAccountDTO from(BankAccount account) {
            return new BankAccountDTO(
                    account.getId(),
                    account.getAccountName(),
                    account.getAccountNumber(),
                    account.getIfscCode(),
                    account.getBankName(),
                    account.getBranchName(),
                    Boolean.TRUE.equals(account.getIsPrimary()),
                    account.getCreatedAt().toString());
        }
    }

    public record CreateBankAccountRequest(
            @NotBlank String accountName,
            @NotBlank String accountNumber,
            @NotBlank String ifscCode,
            @NotBlank String bankName,
            String branchName) {
    }

    public record UpdateBankAccountRequest(
            @NotBlank String accountName,
            @NotBlank String accountNumber,
            @NotBlank String ifscCode,
            @NotBlank String bankName,
            String branchName) {
    }
}
