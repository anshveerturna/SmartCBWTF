package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Bill;
import com.smartcbwtf.domain.BillingSnapshot;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.BillRepository;
import com.smartcbwtf.repository.FacilityRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TallyExportServiceTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private FacilityRepository facilityRepository;

    @Test
    void exportBillsForMonthNeutralizesFormulaLikeTextCells() throws Exception {
        UUID facilityId = UUID.randomUUID();
        YearMonth month = YearMonth.of(2026, 6);
        Facility facility = facility(facilityId);
        Bill bill = bill(facility, month);

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(billRepository.findByFacilityIdAndBillingMonth(facilityId, month.atDay(1)))
                .thenReturn(List.of(bill));

        byte[] bytes = new TallyExportService(billRepository, facilityRepository)
                .exportBillsForMonth(facilityId, month);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var row = workbook.getSheetAt(0).getRow(1);

            assertEquals("'=HCF-CODE", row.getCell(0).getStringCellValue());
            assertEquals("'+Hospital quoted", row.getCell(1).getStringCellValue());
            assertEquals("'@MODEL", row.getCell(4).getStringCellValue());
            assertEquals("'-STATUS", row.getCell(19).getStringCellValue());
            assertEquals("BMW Charges for +Hospital quoted - 2026-06 (Adj: \tdiscount)",
                    row.getCell(20).getStringCellValue());
        }
    }

    private Facility facility(UUID facilityId) {
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setCode("CBWTF");
        facility.setName("Facility");
        facility.setAddress("Address");
        return facility;
    }

    private Bill bill(Facility facility, YearMonth month) {
        Hcf hcf = new Hcf();
        hcf.setId(UUID.randomUUID());
        hcf.setCode("=HCF-CODE");
        hcf.setName("+Hospital\nquoted");
        hcf.setAddress("Address");

        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID());
        agreement.setAgreementNumber("AGR-1");
        agreement.setFacility(facility);
        agreement.setHcf(hcf);
        agreement.setStatusEnum(Agreement.Status.ACTIVE);
        agreement.setStartDate(month.atDay(1));
        agreement.setPerBedPerDayRate(new BigDecimal("10.00"));

        BillingSnapshot snapshot = new BillingSnapshot();
        snapshot.setAgreement(agreement);
        snapshot.setFacility(facility);
        snapshot.setBillingMonth(month.atDay(1));

        Bill bill = new Bill();
        bill.setFacility(facility);
        bill.setAgreement(agreement);
        bill.setSnapshot(snapshot);
        bill.setBillingMonth(month.atDay(1));
        bill.setBillingModel("@MODEL");
        bill.setSnapshotBeds(10);
        bill.setSnapshotRatePerBed(new BigDecimal("10.00"));
        bill.setSnapshotMonthlyCharge(new BigDecimal("3000.00"));
        bill.setPickupWeightKg(new BigDecimal("45.500"));
        bill.setPickupEventCount(2);
        bill.setPickupEventHash("hash");
        bill.setBaseAllowanceKg(new BigDecimal("30.000"));
        bill.setExcessWeightKg(new BigDecimal("15.500"));
        bill.setBaseAmount(new BigDecimal("3000.00"));
        bill.setExcessAmount(new BigDecimal("775.00"));
        bill.setSubtotal(new BigDecimal("3775.00"));
        bill.setAdjustmentAmount(new BigDecimal("-100.00"));
        bill.setAdjustmentReason("\tdiscount");
        bill.setCgst(new BigDecimal("91.88"));
        bill.setSgst(new BigDecimal("91.88"));
        bill.setTotalAmount(new BigDecimal("3858.76"));
        bill.setFinalPayableAmount(new BigDecimal("3758.76"));
        bill.setStatus("-STATUS");
        return bill;
    }
}
