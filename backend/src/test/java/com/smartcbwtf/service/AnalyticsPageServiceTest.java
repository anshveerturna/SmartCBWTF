package com.smartcbwtf.service;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.AnalyticsPageDTO;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.BagEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsPageServiceTest {

    @Mock
    private BagEventRepository bagEventRepository;

    @Mock
    private AgreementRepository agreementRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AnalyticsPageService service;

    @Test
    void getProcessedBagsBatchesStaffLookupsForPage() {
        UUID facilityId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 7, 1);
        LocalDate toDate = LocalDate.of(2026, 7, 2);

        BagEvent firstEvent = bagEvent(firstUserId, "QR-1", "Yellow", "City Clinic");
        BagEvent secondEvent = bagEvent(secondUserId, "QR-2", "Red", "Dental Care");

        when(bagEventRepository.findProcessedBagsForFacility(
                eq(facilityId), any(Instant.class), any(Instant.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(firstEvent, secondEvent), PageRequest.of(0, 20), 2));
        when(appUserRepository.findAllById(any()))
                .thenReturn(List.of(user(firstUserId, "Nisha Rao", "nisha"),
                        user(secondUserId, "", "operator.two")));

        AnalyticsPageDTO.ProcessedBagsResponse response = service.getProcessedBags(
                facilityId, fromDate, toDate, null, 0, 20);

        assertThat(response.bags())
                .extracting(AnalyticsPageDTO.ProcessedBagEntry::staffName)
                .containsExactly("Nisha Rao", "operator.two");

        ArgumentCaptor<Iterable<UUID>> idsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(appUserRepository).findAllById(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(firstUserId, secondUserId);
        verify(appUserRepository, never()).findById(any());
    }

    private static BagEvent bagEvent(UUID collectedByUserId, String qrCode, String category, String hcfName) {
        BagLabel bagLabel = new BagLabel();
        bagLabel.setQrCode(qrCode);
        bagLabel.setCategory(category);

        Hcf hcf = new Hcf();
        hcf.setName(hcfName);

        BagEvent event = new BagEvent();
        event.setId(UUID.randomUUID());
        event.setCollectedByUserId(collectedByUserId);
        event.setBagLabel(bagLabel);
        event.setHcf(hcf);
        event.setEventTs(Instant.parse("2026-07-01T10:15:30Z"));
        event.setEventType("CBWTF_VERIFICATION");
        event.setWeightKg(new BigDecimal("2.3454"));
        event.setAnomalyState("OK");
        return event;
    }

    private static AppUser user(UUID id, String fullName, String username) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setFullName(fullName);
        user.setUsername(username);
        return user;
    }
}
