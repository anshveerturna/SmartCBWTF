# Smart CBWTF – End-to-End Design & Implementation Plan

This document is the living blueprint for a production-grade system to manage biomedical waste collection, tracking, analytics, and billing for a Common Bio Medical Waste Treatment Facility (CBWTF). The system includes:

- **Backend**: Java 17, Spring Boot, PostgreSQL (optionally PostGIS), JWT auth, Flyway migrations, PDF generation, email stub, Docker-ready.
- **Android App**: Kotlin, MVVM, Room offline cache, WorkManager sync, QR scan, Bluetooth scale abstraction (real + mock), GPS-only location capture, Retrofit networking.
- **Admin UI**: React SPA with Material UI/Chakra UI, axios, Recharts/Chart.js for analytics, role-based features for CBWTF admins.

If any detail is ambiguous, we choose secure, auditable, maintainable defaults (no manual GPS/weight entry, strict auth, auditable events).

---
## 1. Business Goals & Constraints
- Onboard HCFs with agreements; agreements drive billing.
- Pre-issue QR labels (Model A) centrally printed and handed to HCFs.
- Track each bag from HCF to CBWTF using **QR + GPS + Bluetooth weight**; no manual GPS/weight fields exist.
- Prevent unauthorized pickup/theft via geofencing, anomaly flags, and audit logs.
- Provide per-HCF and CBWTF-wide analytics and alerts (missing/mismatch).
- Generate invoices (per-bed-per-day) with PDFs and status lifecycle.

Hard constraints:
- GPS auto-captured (registration, HCF collection, CBWTF verification); no manual coordinate entry.
- Weight only from Bluetooth scale; UI omits manual weight inputs.
- QR labels are pre-generated and centrally printed (Model A).

---
## 2. High-Level Architecture
**Overview**
- **Mobile (Android)** → REST APIs (JWT) → **Spring Boot backend** → **PostgreSQL**.
- **Admin UI (React SPA)** → REST APIs (JWT) → backend.
- **Storage**: PostgreSQL with optional PostGIS; blob storage (local/posix placeholder) for PDFs/label sheets.
- **Security**: JWT auth, role-based access (CBWTF_ADMIN, HCF_ADMIN, DRIVER, PLANT_OPERATOR). Passwords hashed (bcrypt). Audit log for critical actions/events.

**Deployability**
- Backend packaged as Docker image; configurable via `application.yml` and env vars.
- Admin UI as static SPA (can be containerized and served behind reverse proxy).
- Android app ships with build flavors (dev uses MockScaleService by default; prod uses RealBluetoothScaleService).

---
## 3. Domain Model (Entities & Tables)
- `facility` (CBWTF site metadata).
- `hcf` (hospital/clinic) with GPS at registration, status lifecycle.
- `agreement` (links HCF to facility; terms, rate, PDF).
- `bag_label` (pre-issued QR labels; Model A issuance, status ISSUED/USED/VOID).
- `bag_event` (HCF_COLLECTION, CBWTF_VERIFICATION) with GPS, weight, anomalies.
- `app_user` (auth users with roles; optional facility/hcf scope).
- `invoice` (per-bed-per-day billing; PDF, status DRAFT/SENT/PAID/CANCELLED).
- `audit_log` (CREATE/UPDATE/DELETE/LOGIN/EVENT_PROCESS with data hash).
- `export_job` (optional) for CSV/PDF batch exports.

All IDs are UUID. Timestamps are zone-aware. Flyway migrations define schema.

---
## 4. Key Flows (E2E)
**Flow 1: HCF Registration & Agreement**
- Mobile/web captures GPS automatically; POST `/api/hcfs/register` (status=PENDING_APPROVAL).
- Admin reviews PENDING; sets rate/dates; approve → generate agreement number (facility code + year + seq), create PDF, email stub.
- Agreement number reused in invoices and login IDs for HCF admins if needed.

**Flow 2: QR Label Batch Issuance (Model A)**
- Admin issues labels: HCF + category + quantity → create `bag_label` records with encoded QR like `CBWTF|HCF123|YELLOW|00001234`.
- Provide `/api/labels/export?batchId=...` to download PDF sheets for printing.

**Flow 3: Collection at HCF (First weight + GPS)**
- Driver login (JWT). App fetches nearest HCFs by GPS (`/api/hcfs/nearest`).
- For each bag: scan QR, read GPS, read weight via Bluetooth scale (stabilized). Store locally in Room as `HCF_COLLECTION` pending sync.
- WorkManager syncs via `/api/bags/events/sync` (idempotent). Backend validates label status/HCF match, sets label USED/COLLECTED, geofence check, anomaly flag.

**Flow 4: Verification at CBWTF (Second weight + GPS)**
- At plant, GPS validated against facility geofence. App lists bags needing verification.
- For each bag: scan QR, read stabilized Bluetooth weight, capture GPS, POST event `CBWTF_VERIFICATION`.
- Backend compares HCF vs CBWTF weight; flag mismatches above threshold.

**Flow 5: Alerts & Analytics**
- Missing bags: HCF_COLLECTION without CBWTF_VERIFICATION after threshold → `/api/alerts/missing_bags`.
- Mismatch bags: large weight delta → `/api/alerts/mismatched_bags`.
- Per-HCF analytics `/api/analytics/hcf/{id}`; facility-wide `/api/analytics/facility/{id}` (weights, counts, categories, top generators, anomalies).

**Flow 6: Billing & Invoices**
- Generate invoice per HCF & period using per-bed-per-day rate; compute base/tax/total; PDF with waste summary by category.
- Endpoints to list, download PDF, mark SENT/PAID, optional email dispatch.

---
## 5. Backend Design (Spring Boot)
- **Modules/Layers**: controller → service → repository; DTOs for requests/responses; mappers; validation with Bean Validation.
- **Security**: JWT filter, password hashing (bcrypt), method-level `@PreAuthorize` by role.
- **Persistence**: Spring Data JPA; Flyway migrations; optional PostGIS column for geofence/nearest queries (fallback to haversine SQL).
- **PDF**: OpenPDF/iText for agreements, invoices, label sheets.
- **Email**: abstraction with SMTP impl + dev stub/no-op.
- **Geo**: facility geofence radius stored per facility; nearest HCF query using SQL.
- **Anomalies**: enum flags OUT_OF_GEOFENCE, MISMATCH.
- **Idempotency**: sync endpoint deduplicates via client-generated UUID per event.

Key endpoints (non-exhaustive):
- Auth: `/api/auth/login`, `/api/auth/refresh`.
- HCF: `/api/hcfs/register`, `/api/hcfs/pending`, `/api/hcfs/{id}/approve`, `/api/hcfs/nearest`.
- Agreements: `/api/agreements/{id}/pdf`.
- Labels: `/api/labels/issue`, `/api/labels/export`.
- Events: `/api/bags/events/sync`, `/api/bags/pending-verification`, `/api/bags/{id}/verify`.
- Alerts: `/api/alerts/missing_bags`, `/api/alerts/mismatched_bags`.
- Analytics: `/api/analytics/hcf/{id}`, `/api/analytics/facility/{id}`.
- Billing: `/api/invoices/generate`, `/api/invoices`, `/api/invoices/{id}/pdf`, `/api/invoices/{id}/status`.

---
## 6. Android App Design (Kotlin)
- **Architecture**: MVVM + Repository; Coroutines + Flow; Hilt for DI.
- **Storage**: Room DB for cached HCFs, bag labels (optional), unsynced events (with idempotency keys).
- **Networking**: Retrofit + OkHttp interceptors (auth header, logging in debug).
- **Config**: Base API URL is provided via `BuildConfig.BASE_URL`; override at build time with Gradle property `-PAPI_BASE_URL=https://your.endpoint/`.
- **Auth**: login screen, stores JWT securely (EncryptedSharedPreferences/Datastore); token refresh support.
- **Location**: Fused Location Provider; no UI for manual entry.
- **QR Scan**: ZXing/ML Kit.
- **Bluetooth Scale**: `ScaleService` interface with `RealBluetoothScaleService` (BLE, parses strings like `"ST,GS,+  5.0kg"`, stabilization) and `MockScaleService` (dev default; simulate weight after ~2s, random 1–10 kg, debug UI control). UI observes stable weight stream.
- **Flows**:
  - HCF registration screen (captures GPS automatically).
  - Start Pickup: get GPS, call nearest HCF, select.
  - Scan & Weigh: connect scale, scan QR, auto GPS, require stable weight before enabling submit.
  - Verification at CBWTF: similar but with facility geofence validation.
- **Offline/Sync**: WorkManager periodic/one-off jobs to sync unsent events; exponential backoff; marks synced rows by server ACK; retries idempotently.

---
## 7. Admin UI (React SPA)
- **Stack**: Vite + React + TypeScript, Material UI or Chakra UI, axios, React Query/SWR, Recharts/Chart.js.
- **Auth**: JWT stored in memory + refresh; route guards per role.
- **Pages**:
  - Login.
  - HCF approvals (list PENDING → approve/reject; set rate/dates; generate agreement PDF link).
  - Label issuance (form) + download label sheet PDF.
  - Alerts (missing/mismatched) tables.
  - Analytics dashboards (per-HCF and facility-wide charts).
  - Invoices list/detail with PDF download and status update.

---
## 8. Data Model (Initial DDL Outline)
We will codify in Flyway migrations (excerpt):
- `app_user(id UUID PK, username UNIQUE, password_hash, full_name, email, role, facility_id FK, hcf_id FK, created_at)`
- `facility(id UUID PK, code UNIQUE, name, address, contact_email, contact_phone, gps_lat, gps_lon, geofence_radius_m, created_at, updated_at)`
- `hcf(id UUID PK, code UNIQUE, name, address, contact_email, contact_phone, number_of_beds, gps_lat, gps_lon, status, created_at, updated_at)`
- `agreement(id UUID PK, agreement_number UNIQUE, hcf_id FK, facility_id FK, start_date, end_date, per_bed_per_day_rate, terms_text, pdf_url, status, created_at, updated_at)`
- `bag_label(id UUID PK, hcf_id FK, facility_id FK, category, serial_no, qr_code UNIQUE, status, issued_at, used_at, void_reason)`
- `bag_event(id UUID PK, bag_label_id FK, facility_id FK, hcf_id FK, event_type, event_ts, gps_lat, gps_lon, weight_kg, collected_by_user_id FK, app_device_id, anomaly_state, notes, created_at)`
- `invoice(id UUID PK, invoice_number UNIQUE, hcf_id FK, facility_id FK, agreement_id FK, period_start, period_end, beds, per_bed_per_day_rate, base_amount, tax_amount, total_amount, pdf_url, status, created_at, updated_at)`
- `audit_log(id UUID PK, entity_type, entity_id, action, actor_user_id, ts, data_json JSONB, data_hash)`
- `export_job(id UUID PK, job_type, requested_by_user_id, status, started_at, completed_at, artifact_url, params_json)`

---
## 9. Security & Compliance
- JWT for API auth; short-lived access tokens, refresh endpoint.
- Bcrypt for password hashing; minimum complexity enforced.
- Role-based method security; deny-by-default for admin endpoints.
- Audit logging for key actions and logins; data hash for tamper detection.
- Input validation and size limits; rate limiting (future-ready via proxy/gateway).
- No manual GPS/weight fields anywhere in UI; all captured via sensors/services.

---
## 10. Observability & Reliability
- Structured logging (request IDs, user IDs on context) for major flows.
- Idempotent sync via client event UUIDs; backend de-duplicates.
- Background sync retries with backoff on Android.
- Health checks: `/actuator/health`, DB migration on startup.

---
## 11. Project Structure (planned)
```
backend/
  pom.xml (or build.gradle)
  src/main/java/... (controllers, services, repositories, security, dto, pdf, email, config)
  src/main/resources/
    application.yml
    db/migration/V1__init.sql
  Dockerfile
frontend/
  package.json
  src/ (routes, pages, components, api client, auth guards)
android-app/
  build.gradle settings
  app/src/main/java/... (ui, viewmodels, repositories, data, bluetooth, qr, di)
  app/src/main/res/
  README-android.md
README.md (this file)
```

---
## 12. Implementation Plan & Next Steps
1. **Scaffold backend**: Spring Boot project, Flyway baseline migration, core entities/DTOs, JWT security, controllers stubs, Dockerfile, sample config.
2. **Scaffold Android app**: MVVM skeleton, auth flow, Room schema for events, Retrofit client, ScaleService interface + mock & real stubs, WorkManager sync setup.
3. **Scaffold admin UI**: Vite + React + TS, auth guard, pages skeleton (HCF approvals, label issuance, alerts, analytics, invoices), axios client.
4. **Add PDFs**: agreement, invoice, label sheet generators.
5. **Add analytics & alerts queries**: SQL views/services; expose REST endpoints.
6. **Testing**: unit tests for services/controllers; Android unit/UI tests; frontend component smoke tests.
7. **Docs**: update runbooks, API docs (Springdoc OpenAPI), environment setup.

---
## 13. Running the Projects (to be completed as code is added)
- Backend: `./mvnw spring-boot:run` or `./gradlew bootRun`; env vars for DB and JWT secret; `docker build` for container.
- Frontend: `npm install && npm run dev`.
- Android: open in Android Studio; select dev flavor (MockScaleService default); run on emulator/physical device.

---
## 14. Additional Documentation

- **[HCF Registration Configuration Guide](docs/HCF_REGISTRATION_CONFIGURATION.md)** - Detailed instructions for configuring:
  - Agreement Number Generator (format, prefix, separator, sequence digits)
  - Terms & Conditions management (creating, updating, activating versions)
  - Facility templates for agreement PDFs
  - Email and PDF storage settings

---
## 15. Assumptions
- Single CBWTF facility initially; multi-facility supported via `facility` entity.
- BLE scale sends ASCII weight strings like `ST,GS,+  5.0kg`; stabilization handled client-side.
- Email sending can be stubbed in dev; prod uses SMTP credentials.
- PDFs stored locally or to an object store; path kept in DB.

---
## 16. Change Log
- v0.2 (2025-12-12): Added HCF Registration flow with configurable agreement numbers, terms management, PDF generation
- v0.1 (initial): Initial architecture README and plan.

