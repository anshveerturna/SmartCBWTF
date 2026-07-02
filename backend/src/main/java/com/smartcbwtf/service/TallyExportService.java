package com.smartcbwtf.service;

import com.smartcbwtf.domain.Bill;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.BillRepository;
import com.smartcbwtf.repository.FacilityRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Tally Export Service.
 * Generates Excel files compatible with Tally accounting software import.
 * 
 * Export includes ONE row per HCF per billing period with all billing details
 * including adjustments and GST breakup for accounting reference.
 */
@Service
public class TallyExportService {

    private static final Logger log = LoggerFactory.getLogger(TallyExportService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BillRepository billRepository;
    private final FacilityRepository facilityRepository;

    public TallyExportService(BillRepository billRepository, FacilityRepository facilityRepository) {
        this.billRepository = billRepository;
        this.facilityRepository = facilityRepository;
    }

    /**
     * Export bills for a month to Tally-compatible Excel.
     * 
     * @param facilityId Facility UUID
     * @param yearMonth  Billing month
     * @return Excel file bytes
     */
    public byte[] exportBillsForMonth(UUID facilityId, YearMonth yearMonth) throws IOException {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));

        LocalDate billingMonth = yearMonth.atDay(1);
        List<Bill> bills = billRepository.findByFacilityIdAndBillingMonth(facilityId, billingMonth);

        log.info("Exporting {} bills for Tally - Facility: {}, Month: {}",
                bills.size(), facility.getName(), yearMonth);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Bills - " + yearMonth.toString());

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create currency style
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0.00"));

            // Header row
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {
                    "HCF Code",
                    "HCF Name",
                    "Billing Period Start",
                    "Billing Period End",
                    "Billing Model",
                    "Beds",
                    "Rate Per Bed",
                    "Monthly Charge",
                    "Pickup Weight (kg)",
                    "Base Amount",
                    "Excess Weight (kg)",
                    "Excess Charge",
                    "Subtotal",
                    "Adjustment Amount",
                    "Final Bill Amount",
                    "CGST Amount",
                    "SGST Amount",
                    "Total GST",
                    "Total Payable",
                    "Bill Status",
                    "Narration"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (Bill bill : bills) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;

                Hcf hcf = bill.getAgreement() != null ? bill.getAgreement().getHcf() : null;
                LocalDate periodStart = bill.getBillingMonth();
                LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

                // HCF Code
                setTextValue(row.createCell(colNum++), hcf != null ? hcf.getCode() : "");

                // HCF Name
                setTextValue(row.createCell(colNum++), hcf != null ? hcf.getName() : "");

                // Billing Period Start
                setTextValue(row.createCell(colNum++), periodStart.format(DATE_FMT));

                // Billing Period End
                setTextValue(row.createCell(colNum++), periodEnd.format(DATE_FMT));

                // Billing Model
                setTextValue(row.createCell(colNum++), bill.getBillingModel() != null ? bill.getBillingModel() : "");

                // Beds
                Cell bedsCell = row.createCell(colNum++);
                bedsCell.setCellValue(bill.getSnapshotBeds() != null ? bill.getSnapshotBeds() : 0);

                // Rate Per Bed
                Cell rateCell = row.createCell(colNum++);
                setDecimalValue(rateCell, bill.getSnapshotRatePerBed(), currencyStyle);

                // Monthly Charge
                Cell monthlyCell = row.createCell(colNum++);
                setDecimalValue(monthlyCell, bill.getSnapshotMonthlyCharge(), currencyStyle);

                // Pickup Weight
                Cell pickupCell = row.createCell(colNum++);
                setDecimalValue(pickupCell, bill.getPickupWeightKg(), currencyStyle);

                // Base Amount
                Cell baseCell = row.createCell(colNum++);
                setDecimalValue(baseCell, bill.getBaseAmount(), currencyStyle);

                // Excess Weight
                Cell excessWeightCell = row.createCell(colNum++);
                setDecimalValue(excessWeightCell, bill.getExcessWeightKg(), currencyStyle);

                // Excess Charge
                Cell excessCell = row.createCell(colNum++);
                setDecimalValue(excessCell, bill.getExcessAmount(), currencyStyle);

                // Subtotal
                Cell subtotalCell = row.createCell(colNum++);
                setDecimalValue(subtotalCell, bill.getSubtotal(), currencyStyle);

                // Adjustment Amount
                Cell adjustCell = row.createCell(colNum++);
                setDecimalValue(adjustCell, bill.getAdjustmentAmount(), currencyStyle);

                // Final Bill Amount (before GST, after adjustment)
                Cell finalBillCell = row.createCell(colNum++);
                BigDecimal finalBill = bill.getSubtotal();
                if (bill.getAdjustmentAmount() != null) {
                    finalBill = finalBill.add(bill.getAdjustmentAmount());
                }
                setDecimalValue(finalBillCell, finalBill, currencyStyle);

                // CGST Amount
                Cell cgstCell = row.createCell(colNum++);
                setDecimalValue(cgstCell, bill.getCgst(), currencyStyle);

                // SGST Amount
                Cell sgstCell = row.createCell(colNum++);
                setDecimalValue(sgstCell, bill.getSgst(), currencyStyle);

                // Total GST
                Cell totalGstCell = row.createCell(colNum++);
                BigDecimal totalGst = BigDecimal.ZERO;
                if (bill.getCgst() != null)
                    totalGst = totalGst.add(bill.getCgst());
                if (bill.getSgst() != null)
                    totalGst = totalGst.add(bill.getSgst());
                setDecimalValue(totalGstCell, totalGst, currencyStyle);

                // Total Payable
                Cell totalPayableCell = row.createCell(colNum++);
                BigDecimal totalPayable = bill.getFinalPayableAmount() != null ? bill.getFinalPayableAmount()
                        : bill.getTotalAmount();
                setDecimalValue(totalPayableCell, totalPayable, currencyStyle);

                // Bill Status
                setTextValue(row.createCell(colNum++), bill.getStatus());

                // Narration
                String narration = buildNarration(bill, hcf, yearMonth);
                setTextValue(row.createCell(colNum++), narration);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Add summary row
            rowNum += 2;
            Row summaryLabelRow = sheet.createRow(rowNum++);
            setTextValue(summaryLabelRow.createCell(0), "SUMMARY");

            Row summaryRow = sheet.createRow(rowNum);
            setTextValue(summaryRow.createCell(0), "Total Bills: " + bills.size());

            BigDecimal totalAmount = bills.stream()
                    .map(b -> b.getFinalPayableAmount() != null ? b.getFinalPayableAmount() : b.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Cell totalCell = summaryRow.createCell(1);
            setTextValue(totalCell, "Total Amount: " + totalAmount.toPlainString());

            // Write to bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            log.info("Generated Tally export: {} bills, total amount: {}", bills.size(), totalAmount);
            return out.toByteArray();
        }
    }

    private void setDecimalValue(Cell cell, BigDecimal value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(style);
        } else {
            cell.setCellValue(0.0);
            cell.setCellStyle(style);
        }
    }

    private void setTextValue(Cell cell, String value) {
        cell.setCellValue(safeSpreadsheetText(value));
    }

    private String safeSpreadsheetText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ');
        String trimmedLeading = normalized.stripLeading();
        if (!trimmedLeading.isEmpty() && isSpreadsheetFormulaPrefix(trimmedLeading.charAt(0))) {
            return "'" + normalized;
        }
        return normalized;
    }

    private boolean isSpreadsheetFormulaPrefix(char value) {
        return value == '=' || value == '+' || value == '-' || value == '@' || value == '\t';
    }

    private String buildNarration(Bill bill, Hcf hcf, YearMonth yearMonth) {
        StringBuilder sb = new StringBuilder();
        sb.append("BMW Charges for ");
        sb.append(hcf != null ? hcf.getName() : "HCF");
        sb.append(" - ");
        sb.append(yearMonth.toString());

        if (bill.hasAdjustment()) {
            sb.append(" (Adj: ").append(bill.getAdjustmentReason()).append(")");
        }

        return sb.toString();
    }
}
