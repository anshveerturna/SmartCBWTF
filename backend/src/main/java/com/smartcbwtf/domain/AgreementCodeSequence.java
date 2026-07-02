package com.smartcbwtf.domain;

import jakarta.persistence.*;

/**
 * Sequence table for generating human-readable agreement codes.
 * Format: AGR-YYYY-NNNNN
 */
@Entity
@Table(name = "agreement_code_sequence")
public class AgreementCodeSequence {

    @Id
    @Column(name = "sequence_year")
    private Integer year;

    @Column(name = "last_value", nullable = false)
    private Integer lastValue = 0;

    public AgreementCodeSequence() {
    }

    public AgreementCodeSequence(Integer year, Integer lastValue) {
        this.year = year;
        this.lastValue = lastValue;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getLastValue() {
        return lastValue;
    }

    public void setLastValue(Integer lastValue) {
        this.lastValue = lastValue;
    }

    public Integer incrementAndGet() {
        this.lastValue++;
        return this.lastValue;
    }
}
