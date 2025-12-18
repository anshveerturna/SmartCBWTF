# Current Status – SmartCBWTF (Dec 17, 2025)

Comprehensive snapshot so a new contributor can understand intent, architecture, current implementation, and gaps across Android, Backend, and planned Admin UI.

## 1) Vision & Business Context
- Manage biomedical waste for a Common Biomedical Waste Treatment Facility (CBWTF).
- Onboard HCFs with agreements; centrally issue QR labels; track bags end-to-end with GPS + Bluetooth weight; prevent unauthorized pickups; generate alerts, analytics, and invoices.
- Hard rules: GPS auto-captured (no manual coords), weight only from BLE scale (no manual entry), QR labels pre-printed (Model A), auditable and role-secured.

## 2) Architecture (High Level)
- Android app (drivers/operators) → REST (JWT) → Spring Boot backend → PostgreSQL (+ optional PostGIS). WorkManager handles offline sync.
- Admin UI (React SPA planned) → REST (JWT) → backend for approvals, labels, analytics, billing.
- Security: JWT, role-based (CBWTF_ADMIN, HCF_ADMIN, DRIVER, PLANT_OPERATOR). Audit logging for critical events.

## 3) Domain Model (key entities)
- Facility, Hcf, Agreement, BagLabel, BagEvent (HCF_COLLECTION, CBWTF_VERIFICATION), Attendance, Invoice, AppUser, AuditLog, ExportJob.
- IDs are UUID; timestamps are zone-aware; geofence radii per facility (or default); weight mismatch thresholds configurable.

## 4) Backend – What's Implemented
- JWT auth (bcrypt), role guards; public only for auth/health/HCF registration.
- HCF registration + approval → Agreement PDF + email stub + audit.
- QR label issuance (Model A) → PDF batch and codes.
- Bag events ingest `/api/bags/events/sync` with geofence + mismatch checks; marks labels used on verification; audit logging.
- Alerts: missing bags, mismatched bags.
- Analytics: per-HCF and facility totals, category splits, trends, missing/mismatch counts.
- Invoices: per-bed-per-day, PDF generation, status lifecycle.
- Flyway baseline schema (V1-V5); Dockerfile; health endpoints.
- **Attendance API** (NEW): `POST /api/attendance/sync` with batch sync, geofence validation (50m default), server-side cooldown enforcement (5min default), audit logging.

### Backend Gaps / Phase 2
- Email is stubbed (logs only); QR artwork in PDFs is text-only.
- No user provisioning API (seed users manually); export jobs reserved but not built.
- Geofence/weight thresholds static; no per-HCF overrides yet.

## 5) Android App – Architecture & Modules
- Stack: Kotlin, MVVM, Hilt DI, Coroutines/Flow, Room (offline cache/sync queue), Retrofit/OkHttp, WorkManager, ZXing/ML Kit for QR, Fused Location Provider, BLE scale abstraction (RealBluetoothScaleService + MockScaleService).
- Navigation via Jetpack Navigation; theming with Material components; data binding/view binding.

### Key Mobile Flows
- Login (JWT) → stores token; guards flows.
- HCF Registration: captures GPS automatically; validates phone/PAN/GST/Aadhaar/Email; dismisses keyboard on outside tap.
- Pickup (HCF_COLLECTION): GPS + BLE weight + QR scan; nearest HCF selection; stores unsynced events; WorkManager sync.
- Verify at CBWTF: second weight + GPS; geofence against facility.
- **Mark Attendance** (IMPLEMENTED): dedicated screen with geofence check, offline queue, WorkManager sync.

### Mark Attendance Flow (fully implemented)
1) Dashboard card tap → permission check → one-shot GPS fetch in HomeFragment.
2) Navigate to AttendanceFragment with lat/lon (0/0 if unavailable).
3) AttendanceFragment:
    - Location status (loading/success/error) and "Retry Location" to re-fetch GPS via LocationHelper.getCurrentLocation().
    - Reads local HCFs (Room via HcfRepository) and filters within 50m (GeoUtils.haversineMeters).
    - States: no HCF (button disabled), single HCF (auto-selected), multiple (selection list).
    - Mark button saves AttendanceEventEntity to Room (offline-first), schedules SyncAttendanceWorker.
    - Cooldown persisted via Room; restored on ViewModel init. Server enforces cooldown independently.

### Android Recent UX/Logic Changes
- Attendance screen replaces modal popups; duplicate back button removed (uses toolbar only).
- HCF selection list, "Not at registered location" handling, cooldown card, loading overlay.
- Dashboard card added; animations include attendance card.
- Form validation fixes for Register HCF; keyboard dismiss on tap-outside; Verify toolbar title corrected.
- **Attendance offline queue**: AttendanceEventEntity + AttendanceDao + AttendanceRepository + SyncAttendanceWorker.

### Android Gaps / Known Issues
- Location fetch can be slow on cold start; no timeout/backoff beyond manual Retry.
- Initial nav may pass 0/0 if HomeFragment fetch fails; user must retry in Attendance screen.

## 6) Admin UI (planned, not in repo code)
- React + TS + Material UI/Chakra, axios, React Query; pages for approvals, label issuance, alerts, analytics, invoices; JWT auth with role guards.

## 7) Build/Run
- Android: `cd androidapp && ./gradlew assembleDebug` (needs Play Services + location runtime perms). API base URL via `BuildConfig.BASE_URL` (Gradle property `-PAPI_BASE_URL=...`).
- Backend: `cd backend && mvn -DskipTests clean package` then `java -jar target/backend-0.0.1-SNAPSHOT.jar` (PostgreSQL per `application.yml`), or `docker build -t smartcbwtf-backend .`.
- Admin UI: not present in repo; planned via Vite.

## 8) Directory Landmarks
- `androidapp/` – mobile app source (MVVM, Room, BLE, location, navigation, layouts, drawables).
- `backend/` – Spring Boot project (controllers, services, repos, security, Flyway, Dockerfile).
- `docs/HCF_REGISTRATION_CONFIGURATION.md` – agreement numbering, terms mgmt, templates, email/PDF settings.
- `currentstatusreadme.md` – this file.

## 9) Outstanding Work / Next Steps
- Add location fetch timeout/backoff and better UX for cold GPS start.
- Add telemetry/analytics for attendance attempts/failures.
- Backend: real email provider, QR artwork in PDFs, user provisioning endpoints, export jobs, per-HCF geofence/threshold overrides.
- Admin UI: scaffold pages and hook to existing backend endpoints.

## 10) Attendance Implementation Details (Dec 17, 2025)

### Backend Components
| File | Purpose |
|------|---------|
| `Attendance.java` | JPA entity with driver, HCF, eventTs, GPS coords, distance, clientEventId |
| `V5__attendance.sql` | Flyway migration with indexes for driver-hcf-ts lookups |
| `AttendanceRepository.java` | JPA repository with cooldown-check queries |
| `AttendanceSyncItem.java` | Single event DTO within batch request |
| `AttendanceSyncRequest.java` | Batch request wrapper |
| `AttendanceSyncResponse.java` | Response with successIds, per-item results |
| `AttendanceSyncItemResult.java` | Per-item result with errorCode, cooldownRemainingMs |
| `AttendanceService.java` | Geofence validation, cooldown enforcement, audit logging |
| `AttendanceController.java` | `POST /api/attendance/sync` endpoint (DRIVER role) |

### Android Components
| File | Purpose |
|------|---------|
| `AttendanceEventEntity.kt` | Room entity with synced flag, syncError |
| `AttendanceDao.kt` | DAO with pending/synced queries, cooldown check |
| `AttendanceApi.kt` | Retrofit interface for sync endpoint |
| `AttendanceRepository.kt` | Interface for attendance operations |
| `DefaultAttendanceRepository.kt` | Implementation with offline queue, batch sync |
| `SyncAttendanceWorker.kt` | WorkManager worker for background sync |
| `AttendanceViewModel.kt` | Updated with repository integration, persistent cooldown |

### Configuration
- Backend `application.yml`:
  - `app.attendance.geofence-radius-m: 50`
  - `app.attendance.cooldown-minutes: 5`
- Android Room database version bumped to 3.
