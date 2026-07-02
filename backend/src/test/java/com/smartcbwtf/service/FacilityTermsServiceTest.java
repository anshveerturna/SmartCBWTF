package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilityTerms;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.FacilityTermsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityTermsServiceTest {

    @Mock
    private FacilityTermsRepository termsRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AppUserRepository userRepository;

    private FacilityTermsService service;

    @BeforeEach
    void setUp() {
        service = new FacilityTermsService(termsRepository, facilityRepository, userRepository);
    }

    @Test
    void getLatestTermsWithoutFacilityReturnsEmptyWithoutCrossFacilityLookup() {
        assertTrue(service.getLatestTerms(null).isEmpty());

        verifyNoInteractions(termsRepository, facilityRepository, userRepository);
    }

    @Test
    void getLatestTermsReturnsOnlyRequestedFacilityTerms() {
        UUID facilityId = UUID.randomUUID();
        FacilityTerms terms = activeTerms(facilityId);
        when(termsRepository.findByFacilityIdAndActiveTrue(facilityId)).thenReturn(Optional.of(terms));

        var response = service.getLatestTerms(facilityId);

        assertTrue(response.isPresent());
        assertEquals(facilityId, response.get().getFacilityId());
        assertEquals("facility-v1", response.get().getVersion());
        verify(termsRepository).findByFacilityIdAndActiveTrue(facilityId);
        verifyNoInteractions(facilityRepository, userRepository);
    }

    @Test
    void getActiveTermsEntityDoesNotBorrowTermsFromOtherFacilities() {
        UUID facilityId = UUID.randomUUID();
        when(termsRepository.findByFacilityIdAndActiveTrue(facilityId)).thenReturn(Optional.empty());

        assertTrue(service.getActiveTermsEntity(facilityId).isEmpty());

        verify(termsRepository).findByFacilityIdAndActiveTrue(facilityId);
        verifyNoInteractions(facilityRepository, userRepository);
    }

    private FacilityTerms activeTerms(UUID facilityId) {
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setName("Facility A");

        FacilityTerms terms = new FacilityTerms();
        terms.setId(UUID.randomUUID());
        terms.setFacility(facility);
        terms.setVersion("facility-v1");
        terms.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        terms.setTextHtml("<p>Facility terms</p>");
        terms.setActive(true);
        return terms;
    }
}
