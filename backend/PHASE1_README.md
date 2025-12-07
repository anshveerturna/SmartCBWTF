# Phase 1 – Backend Completion Summary

## Overview
Smart CBWTF backend implements JWT-secured tracking of biomedical waste bags from healthcare facilities (HCFs) to CBWTF, covering registration/approval, QR label issuance, bag event capture with geofence and weight checks, alerts, analytics, invoicing, PDFs, and audit logging.

## Architecture (ASCII)
```
[Mobile App] --JWT--> [API Gateway / Spring Boot]
                       |-- Auth (JWT)
                       |-- HCF register/approve -> Agreement + PDF + Email
                       |-- Labels -> PDF batch (Model A)
                       |-- Bag Events -> Geofence/Mismatch/Missing -> Audit
                       |-- Alerts (missing/mismatch)
                       |-- Analytics (HCF/Facility)
                       |-- Invoices -> PDF -> Audit
                       |-- Audit Log Repo
                       |-- Email Stub
                       |-- PDFBox Renderer
[PostgreSQL + Flyway] <------------------------------
```

## Backend Modules Implemented
- Authentication (JWT, bcrypt, role-based filter chain)
- HCF registration and approval (agreement PDF + email stub)
- QR label issuance (Model A batch PDF)
- Bag event ingestion (HCF_COLLECTION, CBWTF_VERIFICATION) with geofence + weight mismatch + audit
- Alerts (missing bags, mismatched bags)
- Analytics (HCF/facility aggregates, trends, mismatch/missing counts)
- Invoices (generate/list/pdf) with per-bed-per-day formula and PDF
- Audit logging service + repository
- Flyway baseline schema
- PDF generation via PDFBox
- Email stub (logs)

## Major Flows
- **HCF registration + agreements**: `/api/hcfs/register` captures HCF with mandatory GPS; approval `/api/hcfs/{hcfId}/approve` (admin) activates HCF, creates Agreement, renders PDF, emails stakeholders, logs audit.
- **QR label issuance (Model A)**: `/api/labels/issue` (admin) issues centrally printed QR codes, returns codes + PDF batch for printing, logs audit.
- **HCF_COLLECTION**: `/api/bags/events/sync` (driver) ingests events; validates ISSUED status, checks HCF geofence, stores event, audit logs.
- **CBWTF_VERIFICATION**: same sync endpoint; checks facility geofence, computes weight mismatch vs last collection, marks label USED, audit logs.
- **Geofence + mismatch logic**: haversine meters with configurable radii; mismatch if delta > `app.weight.mismatch-threshold-kg`.
- **Missing bag logic**: bags collected without verification older than `app.alerts.missing-bag-hours` flagged via repository query and analytics missing count.
- **Alerts**: `/api/alerts/missing_bags` (admin), `/api/alerts/mismatched_bags` (admin) surface missing/mismatch sets with metadata.
- **Analytics**: `/api/analytics/hcf/{hcfId}` (admin) and `/api/analytics/facility/{facilityId}` (admin) return totals, per-category weights/counts, mismatch/missing counts, and per-day trends.
- **Invoices**: `/api/invoices/generate` (admin) uses `per_bed_per_day_rate × beds × days`, taxes optional; PDFs rendered; `/list` and `/{id}/pdf` (admin) serve listings and documents.

## Entities (brief schema)
- `Facility`: code, name, address, contact, GPS, optional geofence radius.
- `Hcf`: code, name, contact, beds, GPS (required), status.
- `Agreement`: number, HCF, facility, start/end, per-bed rate, pdf_url, status.
- `AppUser`: username, password_hash (bcrypt), role, optional facility/hcf links.
- `BagLabel`: hcf, facility, category, serial, qr_code, status, issued/used timestamps.
- `BagEvent`: bag_label, facility, hcf, event_type, event_ts, gps_lat/lon, weight_kg, collected_by_user_id, device, anomaly_state, notes.
- `Invoice`: invoice_number, hcf, facility, agreement, period, beds, rate, base/tax/total, pdf_url, status.
- `AuditLog`: entity_type/id, action, actor_user_id, ts, data_json.
- `ExportJob`: reserved for future exports.

## API Endpoints
- **Auth**: `POST /api/auth/login`
- **Health**: `GET /api/health`, `GET /actuator/health`
- **HCF**: `POST /api/hcfs/register` (public), `GET /api/hcfs/pending` (admin), `POST /api/hcfs/{hcfId}/approve` (admin)
- **Labels**: `POST /api/labels/issue` (admin)
- **Bag Events**: `POST /api/bags/events/sync` (driver)
- **Alerts**: `GET /api/alerts/missing_bags` (admin), `GET /api/alerts/mismatched_bags` (admin)
- **Analytics**: `GET /api/analytics/hcf/{hcfId}?start=YYYY-MM-DD&end=YYYY-MM-DD` (admin), `GET /api/analytics/facility/{facilityId}?start=...&end=...` (admin)
- **Invoices**: `POST /api/invoices/generate` (admin), `GET /api/invoices/list` (admin), `GET /api/invoices/{id}/pdf` (admin)
- **Auth-free**: `/api/hcfs/register`, `/api/auth/**`, `/api/health`, `/actuator/health`, docs endpoints.

## Access Control
- Roles carried in JWT claim `role`; Spring Security attaches `ROLE_<role>` authority.
- Method-level `@PreAuthorize` enforces: admin (`CBWTF_ADMIN`) on approvals, label issuance, invoices, analytics, alerts; driver on bag event sync.
- All other authenticated routes require a valid JWT; public endpoints limited to auth/health/HCF registration.

## Directory Structure (backend)
```
backend/
  Dockerfile
  pom.xml
  src/main/java/com/smartcbwtf/
    config/ (security, JWT filter/service)
    controller/ (auth, hcf, labels, bags, alerts, analytics, invoices, health)
    domain/ (entities)
    dto/ (request/response models)
    repository/ (JPA repositories)
    service/ (business logic, PDF/email/audit)
  src/main/resources/
    application.yml
    db/migration/V1__init.sql
  target/ (build output)
```

## Build & Run
- **Maven**: `mvn -DskipTests clean package` (Java 17). JAR: `target/backend-0.0.1-SNAPSHOT.jar`.
- **Run locally**: `java -jar target/backend-0.0.1-SNAPSHOT.jar` (requires PostgreSQL per `application.yml`).
- **Docker**: `docker build -t smartcbwtf-backend .` then `docker run -p 8080:8080 --env-file .env smartcbwtf-backend` (provide DB envs matching `spring.datasource.*`).

## Configuration (application.yml)
- JWT: `security.jwt.secret`, `issuer`, `access-token-ttl-minutes`.
- Geofence: `app.geofence.hcf-radius-m`, `facility-default-radius-m`.
- Weight mismatch: `app.weight.mismatch-threshold-kg`.
- Missing bag window: `app.alerts.missing-bag-hours`.
- Datasource: PostgreSQL URL/credentials; Flyway enabled, `ddl-auto: validate`.

## Observability
- Request logging filter emits `traceId`, method, path, status, user (if authenticated), and duration; `X-Trace-Id` header returned on every response for correlation.

## BLE Weight Enforcement
- Bag events require `weightKg`, `gpsLat`, `gpsLon` (@NotNull). Backend assumes weights originate from BLE scale; clients must supply the numeric reading. Events missing GPS/weight will be rejected via validation or DB constraints.

## Limitations / Phase 2 Next Steps
- Email service is a stub (logs only); replace with SMTP provider.
- Audit logs currently store minimal JSON; extend with richer payloads and hashing if needed.
- No QR image rendering; label PDF lists codes as text (sufficient for Model A printing; QR artwork can be added later).
- No user provisioning endpoints; seed users via DB or future admin APIs.
- Geofence radii are static; future: per-HCF overrides and mobile-side coarse validation.
- Export job pipeline reserved but not implemented.

## Android Integration Notes
- Auth: `POST /api/auth/login` returns JWT; include `Authorization: Bearer <token>` on requests.
- Bag events payload: `qrCode`, `eventType` (`HCF_COLLECTION` or `CBWTF_VERIFICATION`), `eventTs` (ISO), `gpsLat`, `gpsLon`, `weightKg` (BLE), `collectedByUserId`, optional `appDeviceId`, `notes`.
- Labels: receive `qrCodes` array and `pdfUrl` for printing; use qrCodes to encode in on-device UI if needed.
- Use analytics/alerts/invoice endpoints for dashboards and document retrieval; PDFs served via `invoice.pdfUrl` and label batch `pdfUrl`.

## Module Verification Checklist
- JWT + bcrypt + roles enforced; public endpoints limited; admin/driver method guards applied where required.
- HCF registration requires GPS; approval activates and creates agreement PDF + email + audit.
- Labels issuance returns QR list + PDF; audit logged.
- Bag events validate status, geofence, mismatch; mark labels used on verification; audit logged.
- Alerts endpoints surface missing/mismatched bags.
- Analytics endpoints return totals, category splits, trends, mismatch/missing counts.
- Invoices follow per-bed-per-day formula, PDF generated, audit logged.
- Flyway baseline present; project builds on Java 17; Dockerfile provided.
