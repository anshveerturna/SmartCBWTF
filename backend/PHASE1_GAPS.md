# Phase 1 – Gaps & Follow-ups

Remaining limitations after Phase 1 backend completion.

## Functional gaps
- Email delivery: EmailService is a logging stub; no SMTP/provider integration or retry policy.
- Label PDFs: Batch PDF lists QR strings only (no QR images/barcodes); suitable for Model A text, not scannable artwork.
- Audit depth: AuditLog stores minimal JSON/null; no hashing/signing, request metadata, IP/user-agent capture, or tamper checks.
- User provisioning: No admin APIs to create/reset users or rotate passwords; relies on out-of-band seeding. Password policies not enforced.
- BLE validation: Backend trusts provided `weightKg`; cannot verify BLE source or tamper evidence.
- Geofence flexibility: Uses global radii and facility override only; no per-HCF override or adaptive radius.
- Missing exports: `export_job` table unused; no export/report endpoints implemented.
- Tests: No automated unit/integration tests for services/controllers/repositories; no PDF/alerts/analytics regression coverage.

## Technical gaps
- PDFs: Simple text layout; no branding, totals table formatting, or digital signatures. Label PDF pagination not handled for large batches.
- Observability: No metrics/tracing/log correlation propagation; no health/liveness/readiness probes in Docker/runtime.
- Data validation: No server-side max-range checks on GPS/weights; no rate limiting on bag event ingestion.
- Docker/runtime: Container build uses Maven inside image; no multi-stage cache or healthcheck; relies on external Postgres at runtime.
