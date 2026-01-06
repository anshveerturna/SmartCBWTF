# Tally Export Format Specification

> **Version**: 1.0\
> **Last Updated**: 2026-01-05\
> **Status**: LOCKED - Contact engineering before any changes

## Overview

This document defines the schema contract for the Excel export used by Tally
accounting software. The accounting team treats this format as a contract.

## File Format

- **Type**: Microsoft Excel OpenXML (.xlsx)
- **Encoding**: UTF-8
- **Endpoint**:
  `GET /api/cbwtf/billing/bills/export/tally?year={YYYY}&month={MM}`

## Column Schema (FIXED ORDER)

| Column | Name               | Type    | Format                                     | Description                         |
| ------ | ------------------ | ------- | ------------------------------------------ | ----------------------------------- |
| A      | HCF Name           | Text    | -                                          | Healthcare facility name            |
| B      | HCF Code           | Text    | -                                          | Unique facility identifier          |
| C      | Billing Month      | Date    | YYYY-MM                                    | First day of billing month          |
| D      | Billing Model      | Text    | `BEDDED` or `FIXED_MONTHLY`                | Agreement billing model             |
| E      | Bed Count          | Integer | 0                                          | Number of beds (0 if FIXED_MONTHLY) |
| F      | Pickup Weight (kg) | Decimal | 3 decimal places                           | Total waste collected               |
| G      | Base Amount        | Decimal | 2 decimal places                           | Monthly base charge                 |
| H      | Excess Weight (kg) | Decimal | 3 decimal places                           | Weight above allowance              |
| I      | Excess Amount      | Decimal | 2 decimal places                           | Charge for excess weight            |
| J      | Subtotal           | Decimal | 2 decimal places                           | Base + Excess                       |
| K      | Adjustment         | Decimal | 2 decimal places                           | Concession (negative) or 0          |
| L      | Final Bill Amount  | Decimal | 2 decimal places                           | Subtotal + Adjustment               |
| M      | CGST (9%)          | Decimal | 2 decimal places                           | Informational only                  |
| N      | SGST (9%)          | Decimal | 2 decimal places                           | Informational only                  |
| O      | Total with GST     | Decimal | 2 decimal places                           | Final + CGST + SGST                 |
| P      | Status             | Text    | `FINALIZED` or `FINALIZED_WITH_ADJUSTMENT` | Bill status                         |
| Q      | Narration          | Text    | -                                          | Tally narration field               |

## Decimal Formatting Rules

- **All amounts**: 2 decimal places, no thousands separator
- **Weights**: 3 decimal places
- **Locale**: Fixed to `en-US` (no locale-dependent formatting)
- **Negative values**: Displayed with minus sign (e.g., `-500.00`)

## Sample Row

```
HCF Name: "Apollo Hospital"
HCF Code: "APL001"
Billing Month: 2026-01
Billing Model: BEDDED
Bed Count: 150
Pickup Weight: 125.500
Base Amount: 7500.00
Excess Weight: 10.500
Excess Amount: 525.00
Subtotal: 8025.00
Adjustment: -500.00
Final Bill Amount: 7525.00
CGST: 677.25
SGST: 677.25
Total with GST: 8879.50
Status: FINALIZED_WITH_ADJUSTMENT
Narration: "Jan 2026 - Bio-medical waste management"
```

## Summary Row

The last row contains totals:

- Column A: "TOTAL"
- Column L: Sum of all Final Bill Amounts
- Column O: Sum of all Total with GST

## Important Notes

> [!CAUTION]
> Do NOT change column order or names without coordinating with:
>
> 1. Accounting department
> 2. Tally import configuration owner

> [!WARNING]
> GST columns (M, N, O) are for reference only. Actual GST invoices are
> generated in Tally.

## Change Log

| Date       | Version | Changes               |
| ---------- | ------- | --------------------- |
| 2026-01-05 | 1.0     | Initial specification |
