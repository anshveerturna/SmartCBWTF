# SmartCBWTF

**Enterprise Biomedical Waste Management System for Common Biomedical Waste Treatment Facilities**

> **Version**: MVP 1.0  
> **Status**: ✅ MVP Complete – Ready for Pilot Rollout  
> **Last Updated**: December 17, 2025

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Core Design Principles](#2-core-design-principles)
3. [High-Level Architecture](#3-high-level-architecture)
4. [Implemented MVP Features](#4-implemented-mvp-features)
5. [Backend Capabilities](#5-backend-capabilities)
6. [Data Model Summary](#6-data-model-summary)
7. [What Is NOT Implemented Yet](#7-what-is-not-implemented-yet)
8. [Known Limitations](#8-known-limitations)
9. [How to Run the Project](#9-how-to-run-the-project)
10. [Project Status](#10-project-status)
11. [Next Phases](#11-next-phases)

---

## 1. Project Overview

### What is SmartCBWTF?

SmartCBWTF is a comprehensive digital system for managing biomedical waste collection, tracking, and compliance at Common Biomedical Waste Treatment Facilities (CBWTFs). It provides end-to-end traceability from Healthcare Facilities (HCFs) to the treatment plant.

### The Problem It Solves

| Problem | Impact | SmartCBWTF Solution |
|---------|--------|---------------------|
| Paper-based tracking | Error-prone, easily manipulated, hard to audit | Digital records with tamper-proof audit logs |
| Theft & unauthorized pickup | Waste not reaching treatment facility | GPS geofencing + weight verification at both ends |
| Compliance gaps | Failed regulatory inspections | Complete audit trail with timestamps and hashes |
| Billing disputes | Inaccurate weight records | Bluetooth scale capture (no manual entry) |
| Delayed alerts | Missing bags discovered too late | Real-time missing/mismatch detection |

### Who Uses It

| Role | Responsibilities |
|------|------------------|
| **CBWTF Admin** | Facility management, HCF approvals, label issuance, invoicing, analytics |
| **Driver** | Bag collection at HCFs, verification at CBWTF, attendance marking |
| **Plant Operator** | Bag verification and weight re-measurement at CBWTF |
| **HCF Admin** | View collection history, agreements, invoices *(future phase)* |

---

## 2. Core Design Principles

### 🔒 Offline-First Architecture
- All field operations (collection, verification, attendance) work without network connectivity
- Room database queues events locally; WorkManager syncs when online
- Idempotent sync via client-generated UUIDs prevents duplicates

### 📍 GPS & Bluetooth Enforced (No Manual Entry)
- **GPS**: Auto-captured at HCF registration, bag collection, CBWTF verification, and attendance marking
- **Weight**: Bluetooth scale only—UI has no manual weight input fields
- **Rationale**: Eliminates data manipulation at the point of capture

### 📋 Auditability & Tamper Resistance
- Every critical operation logged to `audit_log` table with JSON payload and SHA-256 hash
- Immutable event records; no update/delete on `bag_event` or `attendance`
- Timestamps are zone-aware (UTC storage with local offset)

### 🏗️ Separation of Concerns
- **Android App**: Field operations (drivers, plant operators)
- **Spring Boot Backend**: Business logic, validation, PDF generation, audit
- **Admin Portal** *(planned)*: Approvals, analytics, billing management

---

## 3. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FIELD LAYER                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────┐    ┌──────────────────┐    ┌───────────────────┐     │
│   │  Android App    │    │  Bluetooth Scale │    │   QR Labels       │     │
│   │  (Kotlin/MVVM)  │◄───│  (BLE Protocol)  │    │   (Pre-printed)   │     │
│   │                 │    └──────────────────┘    └───────────────────┘     │
│   │  • Room DB      │                                                       │
│   │  • WorkManager  │                                                       │
│   │  • GPS/Location │                                                       │
│   └────────┬────────┘                                                       │
│            │                                                                │
│            │ REST/JWT (Sync when online)                                    │
│            ▼                                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                              API LAYER                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────┐       │
│   │                    Spring Boot Backend                          │       │
│   │  • JWT Authentication (bcrypt)                                  │       │
│   │  • Role-based Authorization (DRIVER, CBWTF_ADMIN, etc.)         │       │
│   │  • Geofence Validation                                          │       │
│   │  • PDF Generation (OpenPDF)                                     │       │
│   │  • Audit Logging (SHA-256 hash)                                 │       │
│   └─────────────────────────────────────────────────────────────────┘       │
│                                    │                                        │
│                                    ▼                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                              DATA LAYER                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────┐       │
│   │                     PostgreSQL                                  │       │
│   │  • Flyway Migrations (V1–V5)                                    │       │
│   │  • UUID Primary Keys                                            │       │
│   │  • TIMESTAMPTZ for all timestamps                               │       │
│   └─────────────────────────────────────────────────────────────────┘       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Component | Technology |
|-----------|------------|
| Mobile App | Kotlin, MVVM, Hilt DI, Room, Retrofit, WorkManager, ZXing/ML Kit |
| Backend | Java 21, Spring Boot 3.2, Spring Security, Spring Data JPA |
| Database | PostgreSQL 14+ with Flyway migrations |
| PDF Generation | OpenPDF (iText fork) |
| Authentication | JWT (HMAC-SHA256), bcrypt password hashing |

---

## 4. Implemented MVP Features

### 4.1 Authentication & Session Management ✅

| Feature | Status | Details |
|---------|--------|---------|
| JWT-based login | ✅ Implemented | Access tokens (30 min TTL), refresh support |
| Auto logout on expiry | ✅ Implemented | Token validation on each request |
| Role-based access | ✅ Implemented | CBWTF_ADMIN, HCF_ADMIN, DRIVER, PLANT_OPERATOR |
| Secure token storage | ✅ Implemented | EncryptedSharedPreferences on Android |

### 4.2 Home Dashboard ✅

The driver/operator home screen provides access to all field operations:

| Dashboard Card | Status | Description |
|----------------|--------|-------------|
| Pickup Waste | ✅ Implemented | Navigate to HCF collection flow |
| Verify at CBWTF | ✅ Implemented | Navigate to verification flow |
| Register HCF | ✅ Implemented | New HCF onboarding form |
| Mark Attendance | ✅ Implemented | GPS-verified HCF visit confirmation |
| Sync Status | ✅ Implemented | Shows pending/synced event counts |
| Profile Menu | ✅ Implemented | User info, logout, settings |

### 4.3 Pickup Waste Flow (HCF Collection) ✅

Complete offline-capable waste collection workflow:

| Step | Implementation |
|------|----------------|
| 1. GPS Location Capture | Auto-captured via Fused Location Provider (no manual entry) |
| 2. Nearest HCF Selection | Filtered by GPS proximity using Haversine formula |
| 3. QR Scan | Camera-based barcode scanning (ZXing/ML Kit) |
| 4. Bluetooth Weight Capture | BLE scale reading with stabilization logic |
| 5. Multi-bag Session | "Add Bag" for additional bags, "Submit All" for batch |
| 6. Offline Queue | Events stored in Room DB with client-generated UUIDs |
| 7. Background Sync | WorkManager dispatches when connectivity restored |

**Event Type**: `HCF_COLLECTION`

### 4.4 Verify at CBWTF Flow ✅

Separate verification mode for plant operators:

| Feature | Status | Details |
|---------|--------|---------|
| Location enforcement | ✅ Implemented | Must be within CBWTF geofence (200m default) |
| QR scan | ✅ Implemented | Links to original collection event |
| Weight re-verification | ✅ Implemented | Bluetooth scale required |
| Mismatch detection | ✅ Implemented | Flags discrepancies > 0.5 kg threshold |
| Timestamped verification | ✅ Implemented | Server-side timestamp for audit |

**Event Type**: `CBWTF_VERIFICATION`

### 4.5 QR Scanning ✅

| Feature | Status | Details |
|---------|--------|---------|
| Camera-based scanning | ✅ Implemented | ZXing/ML Kit integration |
| Dedicated scanner UI | ✅ Implemented | Full-screen viewfinder with overlay |
| QR format parsing | ✅ Implemented | Format: `CBWTF\|HCF123\|YELLOW\|00001234` |

### 4.6 Bluetooth Weighing Scale ✅

| Feature | Status | Details |
|---------|--------|---------|
| BLE protocol support | ✅ Implemented | Parses `ST,GS,+  5.0kg` format strings |
| Stabilization logic | ✅ Implemented | Waits for stable reading before enabling submit |
| Mock scale service | ✅ Implemented | Dev/emulator testing (2s delay, 1–10 kg random) |
| Real scale service | ✅ Implemented | Production BLE implementation |
| No manual entry | ✅ Enforced | UI has no weight input field |

**Architecture**: `ScaleService` interface with `MockScaleService` (dev) and `RealBluetoothScaleService` (prod) implementations.

### 4.7 HCF Registration & Agreement Generation ✅

Complete digital onboarding workflow:

| Feature | Status | Details |
|---------|--------|---------|
| Registration form | ✅ Implemented | Name, address, beds, contact, PAN, GST, Aadhaar |
| GPS auto-capture | ✅ Implemented | Registration location recorded |
| Phone/email validation | ✅ Implemented | Format validation with error messages |
| Terms & Conditions | ✅ Implemented | Versioned T&C, must accept before submission |
| Configurable templates | ✅ Implemented | Per-facility HTML/PDF templates |
| Agreement number generation | ✅ Implemented | Format: `DEL-HCF-2025-00001` (configurable) |
| PDF generation | ✅ Implemented | OpenPDF with facility branding |
| PDF viewing/sharing | ✅ Implemented | In-app viewer, share intent, print support |

**Configuration**: See [docs/HCF_REGISTRATION_CONFIGURATION.md](docs/HCF_REGISTRATION_CONFIGURATION.md) for agreement number format options.

### 4.8 Attendance Marking ✅

GPS-enforced driver attendance at HCF locations:

| Feature | Status | Details |
|---------|--------|---------|
| Dedicated attendance screen | ✅ Implemented | Full UI with location status, HCF list |
| GPS requirement | ✅ Implemented | Must be within 50m of registered HCF |
| Multi-HCF handling | ✅ Implemented | Selection list if multiple HCFs in range |
| Cooldown enforcement | ✅ Implemented | 5 min between marks (server-authoritative) |
| Offline queue | ✅ Implemented | Room entity + WorkManager sync |
| Retry location | ✅ Implemented | Manual GPS re-fetch button |

**API Endpoint**: `POST /api/attendance/sync` (batch sync, DRIVER role required)

---

## 5. Backend Capabilities

### REST API Endpoints

| Category | Endpoint | Method | Description |
|----------|----------|--------|-------------|
| **Auth** | `/api/auth/login` | POST | JWT login |
| **Auth** | `/api/auth/refresh` | POST | Token refresh |
| **HCF** | `/api/hcfs/register` | POST | New HCF registration |
| **HCF** | `/api/hcfs/nearest` | GET | HCFs within GPS radius |
| **HCF** | `/api/hcfs/pending` | GET | Pending approvals (admin) |
| **HCF** | `/api/hcfs/{id}/approve` | POST | Approve HCF (admin) |
| **Bags** | `/api/bags/events/sync` | POST | Batch event sync (idempotent) |
| **Bags** | `/api/bags/pending-verification` | GET | Bags awaiting CBWTF verification |
| **Attendance** | `/api/attendance/sync` | POST | Batch attendance sync |
| **Labels** | `/api/labels/issue` | POST | Issue QR label batch |
| **Labels** | `/api/labels/export` | GET | Download label sheet PDF |
| **Agreements** | `/api/agreements/{id}/pdf` | GET | Download agreement PDF |
| **Alerts** | `/api/alerts/missing_bags` | GET | Bags not verified after threshold |
| **Alerts** | `/api/alerts/mismatched_bags` | GET | Weight discrepancy alerts |
| **Analytics** | `/api/analytics/hcf/{id}` | GET | Per-HCF statistics |
| **Analytics** | `/api/analytics/facility/{id}` | GET | Facility-wide statistics |
| **Invoices** | `/api/invoices/generate` | POST | Generate invoice |
| **Invoices** | `/api/invoices/{id}/pdf` | GET | Download invoice PDF |
| **Terms** | `/api/terms/latest` | GET | Active T&C for facility |
| **Health** | `/actuator/health` | GET | Health check |

### Core Services

| Service | Responsibility |
|---------|----------------|
| `AgreementNumberGeneratorService` | Atomic, configurable agreement number generation |
| `AgreementService` | PDF generation, approval workflow |
| `BagEventService` | Event validation, geofence check, mismatch detection |
| `AttendanceService` | Geofence validation, cooldown enforcement |
| `AuditLogService` | Tamper-resistant logging with SHA-256 hash |
| `LabelService` | QR code generation, batch issuance |
| `InvoiceService` | Per-bed-per-day billing, PDF generation |
| `EmailService` | Notification dispatch (currently stubbed) |

### Validation & Business Rules

| Rule | Configuration | Default |
|------|---------------|---------|
| HCF Geofence | `app.geofence.hcf-radius-m` | 200m |
| CBWTF Geofence | `app.geofence.facility-default-radius-m` | 200m |
| Attendance Geofence | `app.attendance.geofence-radius-m` | 50m |
| Weight Mismatch | `app.weight.mismatch-threshold-kg` | 0.5 kg |
| Missing Bag Alert | `app.alerts.missing-bag-hours` | 24 hours |
| Attendance Cooldown | `app.attendance.cooldown-minutes` | 5 min |

---

## 6. Data Model Summary

### Core Entities

| Entity | Purpose | Key Fields |
|--------|---------|------------|
| `Facility` | CBWTF site | code, name, address, GPS, geofence radius |
| `Hcf` | Healthcare facility | code, name, beds, GPS, status, registration fields |
| `Agreement` | HCF contract | agreement_number, rate, terms_version, PDF URL |
| `BagLabel` | Pre-issued QR label | qr_code, category, status (ISSUED/USED/VOID) |
| `BagEvent` | Collection/verification | event_type, GPS, weight, anomaly_state |
| `Attendance` | Driver HCF visit | driver, HCF, GPS, distance, client_event_id |
| `AppUser` | System user | username, role, facility/HCF scope |
| `AuditLog` | Tamper-proof log | entity_type, action, data_json, data_hash |
| `FacilityTerms` | Versioned T&C | version, text_html, active flag |
| `FacilityTemplate` | Agreement PDF template | template_type, content_location, variables |
| `Invoice` | Billing record | period, beds, rate, amounts, PDF URL, status |

### Database Migrations

| Migration | Purpose |
|-----------|---------|
| V1__init.sql | Base schema (facility, hcf, agreement, bag_label, bag_event, invoice, audit_log) |
| V2__bag_event_dedupe.sql | Idempotency support for event sync |
| V3__bag_event_performance_indexes.sql | Query optimization indexes |
| V4__hcf_agreement_terms_templates.sql | Registration GPS, terms, templates |
| V5__attendance.sql | Attendance entity with driver-HCF-ts indexes |

---

## 7. What Is NOT Implemented Yet

### 🔜 Phase 2 – Admin Web Portal
- React SPA for CBWTF administrators
- HCF approval workflow UI
- Label issuance interface
- Analytics dashboards
- Invoice management

### 🔜 Phase 2 – Billing & Payment
- Payment gateway integration
- Online payment processing
- Payment reconciliation
- Invoice email dispatch (email currently stubbed)

### 🔜 Phase 3 – Compliance & Exports
- CPCB/PCB report generation
- Carbon credit calculation
- Regulatory compliance exports
- Scheduled report automation
- Export job processing (schema exists, not implemented)

### 🔜 Phase 3 – Advanced Analytics
- Real-time dashboards
- Trend analysis
- Predictive alerts
- Collection route optimization

### 🔜 Phase 4 – Integrations
- CCTV verification integration
- GPS fleet tracking
- ERP/accounting system sync
- Hardware certification flows

### 🔜 Infrastructure
- User provisioning API (currently seed-based)
- Password reset flow
- Multi-factor authentication
- Per-HCF geofence/threshold overrides

---

## 8. Known Limitations

### Android App

| Limitation | Impact | Workaround |
|------------|--------|------------|
| Emulator camera | QR scanning requires physical device | Use mock data or real device |
| BLE dependency | Weight capture requires paired scale | Mock scale service for dev |
| Cold GPS start | Initial location fetch can be slow | Retry button provided |
| Location permission | Core functionality blocked without | Clear permission rationale in UI |

### Backend

| Limitation | Impact | Workaround |
|------------|--------|------------|
| Email stubbed | No actual emails sent | Logs email content; real SMTP in Phase 2 |
| QR artwork text-only | Label PDFs lack graphical QR | Future: image-based QR rendering |
| Static thresholds | No per-HCF geofence/weight overrides | Future: configurable per entity |
| Single-tenant | Assumes single CBWTF initially | Multi-facility supported in schema |

### Operational

| Limitation | Mitigation |
|------------|------------|
| Online required for HCF registration | Ensure connectivity for onboarding |
| Manual user provisioning | Users created via SQL seed; admin UI in Phase 2 |

---

## 9. How to Run the Project

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 LTS |
| PostgreSQL | 14+ |
| Android Studio | Arctic Fox+ |
| Maven | 3.8+ |

### Backend (Spring Boot)

```bash
# 1. Create PostgreSQL database
psql -U postgres -c "CREATE DATABASE smart_cbwtf;"
psql -U postgres -c "CREATE USER smart_cbwtf WITH PASSWORD 'change-me';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE smart_cbwtf TO smart_cbwtf;"

# 2. Navigate to backend directory
cd backend

# 3. Configure application.yml (update DB credentials, JWT secret)
# Edit src/main/resources/application.yml

# 4. Run with Maven
mvn -DskipTests spring-boot:run

# Backend starts at http://localhost:8080
# Health check: http://localhost:8080/actuator/health
```

**Docker Alternative:**
```bash
cd backend
docker build -t smartcbwtf-backend .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/smart_cbwtf \
  -e DB_USERNAME=smart_cbwtf \
  -e DB_PASSWORD=change-me \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  -e APP_EMAIL_ENABLED=false \
  smartcbwtf-backend
```

For production email delivery, set `BREVO_API_KEY` and leave `APP_EMAIL_ENABLED` enabled.

### Staging / Production Backend Deploy

The backend deploy scripts fail closed unless required runtime secrets are present,
Java 21 LTS is selected, and a PostgreSQL backup is created before app startup
and Flyway migrations.

Required remote env vars:

```bash
export DB_URL="jdbc:postgresql://host:5432/smart_cbwtf"
export DB_USERNAME="smart_cbwtf"
export DB_PASSWORD="..."
export JWT_SECRET="$(openssl rand -base64 48)"
export BREVO_API_KEY="..." # or export APP_EMAIL_ENABLED=false
export JAVA_BIN="/path/to/java-21/bin/java"
```

Deploy staging first:

```bash
cd backend && mvn -DskipTests package
cd ..
DEPLOY_ENV=staging SMARTCBWTF_DEPLOY_HOST=ec2-user@staging-host ./deploy-backend.sh
```

Deploy production only after staging smoke passes:

```bash
DEPLOY_ENV=production SMARTCBWTF_DEPLOY_HOST=ec2-user@production-host ./deploy-backend.sh
```

Backups are written to `$APP_HOME/db-backups` by default. Override with
`DB_BACKUP_DIR=/secure/backup/path` when needed.

### Android App

```bash
# 1. Open in Android Studio
cd androidapp
# File → Open → select androidapp folder

# 2. Configure API base URL (for emulator → localhost)
# In app/build.gradle.kts or via Gradle property:
# -PAPI_BASE_URL="http://10.0.2.2:8080"
# Note: 10.0.2.2 is emulator's host loopback

# 3. Build debug APK
./gradlew assembleDebug

# 4. Install on device/emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Emulator vs Real Device

| Feature | Emulator | Real Device |
|---------|----------|-------------|
| QR Scanning | ❌ Limited (no camera) | ✅ Full support |
| Bluetooth Scale | ❌ Mock service only | ✅ Real BLE support |
| GPS Location | ⚠️ Simulated location | ✅ Actual GPS |
| Network Sync | ✅ Works | ✅ Works |
| Offline Queue | ✅ Works | ✅ Works |

**Recommendation**: Use real device for full feature testing.

---

## 10. Project Status

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│              ✅  MVP COMPLETE                               │
│                                                             │
│   Ready for pilot rollout and real-device testing           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Completed Milestones

| Milestone | Status |
|-----------|--------|
| Core collection workflow (HCF_COLLECTION) | ✅ Complete |
| CBWTF verification workflow (CBWTF_VERIFICATION) | ✅ Complete |
| HCF registration & agreement PDF generation | ✅ Complete |
| Attendance tracking with geofence | ✅ Complete |
| Offline-first architecture (Room + WorkManager) | ✅ Complete |
| JWT authentication with role-based access | ✅ Complete |
| Audit logging with tamper detection | ✅ Complete |
| PDF generation (agreements, invoices, labels) | ✅ Complete |
| Alerts (missing bags, weight mismatch) | ✅ Complete |
| Analytics endpoints (per-HCF, facility-wide) | ✅ Complete |

### Ready For

| Activity | Status |
|----------|--------|
| Pilot Rollout | ✅ Ready – Deploy with select HCFs |
| Real Device Testing | ✅ Ready – Full BLE + GPS + camera |
| Management Demo | ✅ Ready – End-to-end workflow |
| Security Review | ✅ Ready – JWT, roles, audit trails |
| Compliance Audit | ✅ Ready – Audit logs with hashes |

---

## 11. Next Phases

### Phase 2.4: Admin Portal
- React SPA scaffolding (Vite + TypeScript)
- HCF approval workflow UI
- Label issuance interface
- Basic analytics dashboards

### Phase 2.5: Billing & Invoicing
- Invoice generation UI
- PDF dispatch via real email provider
- Payment status tracking

### Phase 3: Compliance & Integrations
- CPCB/PCB report templates
- Export job automation
- Per-HCF threshold configuration
- User provisioning API

### Phase 4: Advanced Features
- Real-time fleet tracking
- Predictive analytics
- Mobile push notifications
- CCTV integration

---

## Documentation

| Document | Location | Purpose |
|----------|----------|---------|
| HCF Registration Config | [docs/HCF_REGISTRATION_CONFIGURATION.md](docs/HCF_REGISTRATION_CONFIGURATION.md) | Agreement numbering, templates, terms |
| Current Status | [currentstatusreadme.md](currentstatusreadme.md) | Detailed implementation status |
| Phase 1 Gaps | [backend/PHASE1_GAPS.md](backend/PHASE1_GAPS.md) | Known gaps and improvements |

---

## Directory Structure

```
SmartCBWTF/
├── androidapp/                    # Android mobile application
│   ├── app/
│   │   └── src/main/java/com/smartcbwtf/mobile/
│   │       ├── bluetooth/         # BLE scale services
│   │       ├── database/          # Room entities, DAOs
│   │       ├── di/                # Hilt dependency injection
│   │       ├── network/           # Retrofit API clients
│   │       ├── repository/        # Data repositories
│   │       ├── ui/                # Fragments, adapters
│   │       ├── viewmodel/         # MVVM ViewModels
│   │       └── work/              # WorkManager workers
│   └── build.gradle.kts
├── backend/                       # Spring Boot backend
│   ├── src/main/java/com/smartcbwtf/
│   │   ├── controller/            # REST endpoints
│   │   ├── service/               # Business logic
│   │   ├── repository/            # JPA repositories
│   │   ├── domain/                # Entity classes
│   │   ├── dto/                   # Request/response DTOs
│   │   └── config/                # Security, JWT config
│   ├── src/main/resources/
│   │   ├── db/migration/          # Flyway migrations (V1-V5)
│   │   └── application.yml        # Configuration
│   ├── Dockerfile
│   └── pom.xml
├── docs/                          # Documentation
│   └── HCF_REGISTRATION_CONFIGURATION.md
├── README.md                      # This file
└── currentstatusreadme.md         # Detailed status tracking
```

---
##Future changes i want

The One Thing I’d Flag (not a bug, a decision)
⚠️ Agreement status logic

Right now:

HCF = PENDING_APPROVAL

Agreement = ACTIVE

This is not wrong, but it is a policy decision.

Two acceptable models:

Model A (current, acceptable):

Agreement becomes ACTIVE on registration

CBWTF approval is operational, not legal

Model B (stricter):

Agreement = DRAFT

Moves to ACTIVE only after approval

👉 For pilot, Model A is fine
👉 Just document this assumption clearly
## License

Proprietary – Internal use only.

---

## Contact

For questions, access requests, or onboarding support, contact the SmartCBWTF development team.
