package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Payment Receipt Sequence - FY-scoped like invoices.
 * Composite PK: (facility_id, financial_year)
 */
@Entity
@Table(name = "payment_receipt_sequence")
@IdClass(PaymentReceiptSequence.SequenceId.class)
public class PaymentReceiptSequence {

    @Id
    @Column(name = "facility_id")
    private UUID facilityId;

    @Id
    @Column(name = "financial_year", length = 9)
    private String financialYear; // e.g., "2024-2025"

    @Column(name = "last_number", nullable = false)
    private Integer lastNumber = 0;

    // Composite key class
    public static class SequenceId implements Serializable {
        private UUID facilityId;
        private String financialYear;

        public SequenceId() {
        }

        public SequenceId(UUID facilityId, String financialYear) {
            this.facilityId = facilityId;
            this.financialYear = financialYear;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SequenceId that = (SequenceId) o;
            return Objects.equals(facilityId, that.facilityId) &&
                    Objects.equals(financialYear, that.financialYear);
        }

        @Override
        public int hashCode() {
            return Objects.hash(facilityId, financialYear);
        }
    }

    // Getters and setters
    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    public String getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(String financialYear) {
        this.financialYear = financialYear;
    }

    public Integer getLastNumber() {
        return lastNumber;
    }

    public void setLastNumber(Integer lastNumber) {
        this.lastNumber = lastNumber;
    }
}
