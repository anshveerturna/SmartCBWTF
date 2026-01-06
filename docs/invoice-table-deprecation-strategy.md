# Invoice Table Deprecation Strategy

> **Status**: PRESERVED (Do Not Delete)\
> **Decision Date**: 2026-01-05\
> **Review Date**: 2026-07-01

## Current State

The `invoice` table exists in the database but:

- **No new inserts** are being made as of billing engine v2.0
- **All APIs deprecated** with 410 Gone responses
- **UI removed** - Invoice pages deleted from frontend

## Preservation Rationale

> [!CAUTION]
> Do NOT drop this table without CFO approval. Auditors require access to
> historical invoice records.

1. **Audit Requirements**: Historical invoices may be needed for:
   - Tax audits (7-year retention under Indian tax law)
   - Financial reconciliation
   - Dispute resolution

2. **Data Integrity**: Legacy invoices are linked to:
   - Bills (via `bill_id` foreign key)
   - Payments (via `invoice_id` references)

## Migration Plan

### Phase 1: Current (Soft Deprecation) ✅

- APIs return 410 Gone
- UI removed
- Access logged for monitoring
- Table remains in production schema

### Phase 2: Archive (After 6 months)

- Create `archive.invoice` table
- Migrate all records
- Update any remaining FK references
- Mark original table for deletion

### Phase 3: Removal (After 12 months)

- Verify no queries against `public.invoice`
- Get explicit CFO sign-off
- Drop table with backup

## Code Deprecation

The following code is marked `@Deprecated(forRemoval = true)`:

| File                         | Element                    |
| ---------------------------- | -------------------------- |
| `InvoiceController.java`     | Entire class               |
| `InvoiceService.java`        | `generate()` method        |
| `BillGenerationService.java` | `generateInvoice()` method |

## Monitoring

Check for deprecated API access:

```sql
SELECT * FROM audit_log 
WHERE entity_type = 'INVOICE' 
AND created_at > NOW() - INTERVAL '30 days';
```

Check deprecated controller access via logs:

```bash
grep "DEPRECATED API accessed" /var/log/smartcbwtf/app.log
```

## Sign-off Required

Before Phase 2 or Phase 3:

- [ ] CFO approval
- [ ] Audit team confirmation
- [ ] 30-day advance notice to stakeholders
