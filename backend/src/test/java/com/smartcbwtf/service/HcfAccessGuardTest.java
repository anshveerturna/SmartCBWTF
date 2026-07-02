package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.ApprovalStatus;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.HcfBedAccessCategory;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HcfAccessGuardTest {

    private final HcfRepository hcfRepository = mock(HcfRepository.class);
    private final AgreementRepository agreementRepository = mock(AgreementRepository.class);
    private final HcfAccessGuard guard = new HcfAccessGuard(hcfRepository, agreementRepository);

    @Test
    void tenantAwareCheckDeniesHcfOutsideFacility() {
        UUID hcfId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(hcfRepository.findByIdAndFacilityId(hcfId, facilityId)).thenReturn(Optional.empty());

        HcfAccessGuard.AccessCheckResult result = guard.checkPortalAccess(hcfId, facilityId);

        assertFalse(result.isAllowed());
        assertTrue("HCF_NOT_FOUND".equals(result.getErrorCode()));
        verify(hcfRepository).findByIdAndFacilityId(hcfId, facilityId);
        verify(agreementRepository, never()).findActiveByHcfAndFacility(hcfId, facilityId);
        verify(agreementRepository, never()).findFirstByHcfIdAndStatusOrderByStartDateDesc(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tenantAwareCheckRequiresActiveAgreementInSameFacility() {
        UUID hcfId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        Hcf hcf = eligibleApprovedHcf(hcfId);
        Agreement agreement = new Agreement();
        agreement.setEndDate(LocalDate.now().plusDays(30));
        when(hcfRepository.findByIdAndFacilityId(hcfId, facilityId)).thenReturn(Optional.of(hcf));
        when(agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)).thenReturn(Optional.of(agreement));

        HcfAccessGuard.AccessCheckResult result = guard.checkPortalAccess(hcfId, facilityId);

        assertTrue(result.isAllowed());
        assertNull(result.getErrorCode());
        verify(hcfRepository).findByIdAndFacilityId(hcfId, facilityId);
        verify(agreementRepository).findActiveByHcfAndFacility(hcfId, facilityId);
        verify(agreementRepository, never()).findFirstByHcfIdAndStatusOrderByStartDateDesc(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static Hcf eligibleApprovedHcf(UUID hcfId) {
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setName("General Hospital");
        hcf.setCode("HCF-001");
        hcf.setAddress("Main Road");
        hcf.setGpsLat(28.61);
        hcf.setGpsLon(77.20);
        hcf.setStatus("ACTIVE");
        hcf.setApprovalStatus(ApprovalStatus.APPROVED);
        hcf.setBedAccessCategory(HcfBedAccessCategory.ABOVE_30_BEDS);
        return hcf;
    }
}
