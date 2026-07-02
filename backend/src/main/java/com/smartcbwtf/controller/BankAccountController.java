package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.BankAccount;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.BankAccountRepository;
import com.smartcbwtf.repository.FacilityRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
    private static final String ACCOUNT_NUMBER_PATTERN = "[0-9]{9,30}";
    private static final String IFSC_PATTERN = "(?i)[A-Z]{4}0[A-Z0-9]{6}";
    private static final String OPTIONAL_UPI_PATTERN = "^$|[A-Za-z0-9._-]{2,64}@[A-Za-z][A-Za-z0-9.-]{2,64}$";
    private static final int MAX_ACCOUNT_NAME_LENGTH = 255;
    private static final int MAX_BANK_NAME_LENGTH = 100;
    private static final int MAX_UPI_ID_LENGTH = 100;

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
        return findTenantAccount(id, facilityId)
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
        account.setAccountName(trimRequired(request.accountName()));
        account.setAccountNumber(trimRequired(request.accountNumber()));
        account.setIfscCode(normalizeIfsc(request.ifscCode()));
        account.setBankName(trimRequired(request.bankName()));
        account.setUpiId(trimToNull(request.upiId()));
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
        return findTenantAccount(id, facilityId)
                .filter(a -> a.getStatus() == BankAccount.Status.ACTIVE) // Can only update active accounts
                .map(account -> {
                    account.setAccountName(trimRequired(request.accountName()));
                    account.setAccountNumber(trimRequired(request.accountNumber()));
                    account.setIfscCode(normalizeIfsc(request.ifscCode()));
                    account.setBankName(trimRequired(request.bankName()));
                    account.setUpiId(trimToNull(request.upiId()));
                    return ResponseEntity.ok(BankAccountDTO.from(bankAccountRepository.save(account)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== DISABLE ACCOUNT (No delete allowed) ==========

    @PutMapping("/{id}/disable")
    @Transactional
    public ResponseEntity<?> disableAccount(@PathVariable("id") UUID id) {
        UUID facilityId = getCurrentFacilityId();
        return findTenantAccount(id, facilityId)
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
        return findTenantAccount(id, facilityId)
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

    private Optional<BankAccount> findTenantAccount(UUID accountId, UUID facilityId) {
        return facilityId == null
                ? Optional.empty()
                : bankAccountRepository.findByIdAndFacilityId(accountId, facilityId);
    }

    private static String normalizeIfsc(String value) {
        return trimRequired(value).toUpperCase(Locale.ROOT);
    }

    private static String trimRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required value is blank");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
            @NotBlank @Size(max = MAX_ACCOUNT_NAME_LENGTH) String accountName,
            @NotBlank @Pattern(regexp = ACCOUNT_NUMBER_PATTERN, message = "must be 9 to 30 digits") String accountNumber,
            @NotBlank @Pattern(regexp = IFSC_PATTERN, message = "must be a valid IFSC code") String ifscCode,
            @NotBlank @Size(max = MAX_BANK_NAME_LENGTH) String bankName,
            @Size(max = MAX_UPI_ID_LENGTH) @Pattern(regexp = OPTIONAL_UPI_PATTERN, message = "must be a valid UPI ID") String upiId) {
    }

    public record UpdateBankAccountRequest(
            @NotBlank @Size(max = MAX_ACCOUNT_NAME_LENGTH) String accountName,
            @NotBlank @Pattern(regexp = ACCOUNT_NUMBER_PATTERN, message = "must be 9 to 30 digits") String accountNumber,
            @NotBlank @Pattern(regexp = IFSC_PATTERN, message = "must be a valid IFSC code") String ifscCode,
            @NotBlank @Size(max = MAX_BANK_NAME_LENGTH) String bankName,
            @Size(max = MAX_UPI_ID_LENGTH) @Pattern(regexp = OPTIONAL_UPI_PATTERN, message = "must be a valid UPI ID") String upiId) {
    }
}
