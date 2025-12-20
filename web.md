SYSTEM PROMPT — SMARTCBWTF ADMIN & HCF PORTAL (ENTERPRISE SaaS)

CONTEXT
-------
We have successfully completed an almost production-ready version of an enterprise biomedical waste management system called **SmartCBWTF**.

The platform already includes:
- Android field application (Drivers, Plant Operators)
- Spring Boot backend with:
  - JWT authentication
  - Role-based access control
  - Audit logs with cryptographic hashing
  - Offline-first sync (Room + WorkManager)
- Fully implemented core workflows:
  - HCF registration & agreement generation (PDF)
  - Waste pickup at HCF
  - Verification at CBWTF
  - Attendance marking with GPS enforcement
  - QR-based biomedical waste bag tracking
  - Bluetooth scale integration
  - Alerts for missing bags and mismatches

We now want to build a **web-based Admin Portal ecosystem** that will act as:
1. The **operational control plane** for CBWTFs
2. The **business & compliance portal** for HCFs
3. The **SaaS command center** for the SmartCBWTF platform owner

This is NOT a demo dashboard.
This must be **production-grade**, **auditable**, **multi-tenant**, **resellable**, and **scalable across India**.

The portal must integrate with the **existing backend APIs**, extending them where required.
No business logic is allowed in the frontend.

Preferred stack:
- React + TypeScript
- Cloud-native deployment (AWS / GCP / Azure — provider-agnostic design)

GOALS
-----
1. Reduce operational friction for CBWTF owners
2. Increase CBWTF revenue and waste recovery
3. Enforce compliance automatically (not manually)
4. Convert raw data into clear, actionable insights
5. Enable SmartCBWTF to operate as a SaaS platform with subscriptions
6. Make the platform so indispensable that CBWTFs do not hesitate to pay

PRIMARY USER ROLES
------------------
1. **SmartCBWTF Super Admin** (Platform Owner / SaaS Admin)
2. **CBWTF Admin** (Facility Owner / Operator)
3. **HCF Admin** (Healthcare Facility Representative)

ROLE DEFINITIONS
----------------
### 1. SmartCBWTF Super Admin (SYSTEM SUPERADMIN)
This role controls the ENTIRE platform.

Capabilities:
- Onboard and manage multiple CBWTFs (multi-tenant SaaS)
- Control subscriptions, plans, billing, and access
- Enable/disable features per CBWTF
- View platform-wide analytics:
  - Waste collected per CBWTF
  - Waste per HCF
  - Waste per state / city
  - Category-wise trends (Yellow/Red/Blue/White)
  - Blue waste compliance trends
- Monitor staff performance across CBWTFs
- System-wide alerts and anomalies
- Manage global configuration defaults
- Full audit access across tenants

This is the **command center of the SaaS**.

---

### 2. CBWTF Admin
This role manages a specific CBWTF facility.

Capabilities:
- Full operational control of their CBWTF
- HCF onboarding, approval, suspension
- QR code & label issuance
- Waste collection & verification monitoring
- Driver & vehicle tracking
- Route planning & optimisation
- Attendance monitoring
- Billing, invoicing, penalties
- Compliance reporting (CPCB/SPCB)
- Insights on waste quality & recovery
- AI-assisted operational intelligence
- Manage CBWTF users (drivers, operators, accountants)

---

### 3. HCF Admin
This role manages a specific Healthcare Facility.

Capabilities:
- View waste pickup history
- View category-wise waste data
- View compliance summary
- View and download agreements & invoices
- Make payments
- Raise disputes
- Request additional pickups
- Send feedback/messages to CBWTF
- See insights on waste generation trends

NO system-level control.

---

PORTAL ARCHITECTURE REQUIREMENTS
--------------------------------
- React + TypeScript SPA
- Modular, role-based navigation
- Strict API-driven architecture
- Multi-tenant aware (tenant_id / facility_id everywhere)
- Feature-flag driven
- Fully audit-log friendly
- Configurable without redeploying Android apps
- Cloud-native & horizontally scalable

PORTAL MODULES (DETAILED)
------------------------

### 1. Authentication & Identity
- JWT-based login
- Role-based access control (RBAC)
- User management per tenant:
  - Create / disable users
  - Assign roles & scope
- Session management
- Password reset
- Future MFA hooks
- Every admin action must be audit logged

---

### 2. SmartCBWTF Super Admin Dashboard
Platform-wide intelligence:
- Total waste collected (platform-wide)
- Waste by CBWTF
- Waste by HCF
- Waste by state / city
- Category-wise composition
- Blue waste compliance trends
- Active CBWTFs vs inactive
- Subscription revenue metrics
- Staff performance metrics
- System health & alert overview

Purpose: **Operate SmartCBWTF as a SaaS business**

---

### 3. CBWTF Executive Dashboard
Insight-driven, not operational clutter:
- Total waste collected (Today / Month / YTD)
- Category-wise breakdown
- Blue waste % (flag if <55%)
- Verification mismatch rate
- Missing bag count
- Revenue:
  - Invoiced
  - Paid
  - Outstanding
- Alerts by severity
- Trend charts (daily / monthly / yearly)

---

### 4. HCF Management
- Full HCF registry
- Detailed HCF profiles:
  - Registration data
  - Agreement history
  - GPS & geofence
  - Status
- Per-HCF configuration:
  - Geofence radius
  - Waste thresholds
  - Blue waste expectations
- Analytics per HCF:
  - Waste trends
  - Category mix
  - Attendance
  - Missed pickups
- Approve / reject registrations

---

### 5. QR Code & Label Management
- Issue QR codes:
  - Per HCF
  - Per waste category
  - With validity period
- Bulk QR generation
- Lifecycle tracking:
  - Issued / Used / Expired / Voided
- Printable QR PDFs
- Manual overrides with justification
- Full audit trail

---

### 6. Waste Collection & Verification Monitoring
- Unified timeline of all bag events
- Drill-down per bag:
  - QR ID
  - HCF
  - Driver
  - GPS
  - Timestamps
  - Weight delta
- Automatic detection of:
  - Collected but unverified bags
  - Weight mismatches
  - Verification delays
- Immutable historical records

---

### 7. Vehicle, Route & Driver Intelligence
- Real-time vehicle tracking (if available)
- Route playback
- Planned vs actual route comparison
- Deviation detection
- Route optimisation engine:
  - HCF priority
  - Time windows
- AI-assisted suggestions:
  - Fuel optimisation
  - Time reduction
  - Route efficiency scoring

AI is **advisory only**, never autonomous.

---

### 8. Attendance Management
- Attendance logs by driver & HCF
- GPS distance verification
- Anomaly detection
- Exportable reports

---

### 9. Billing, Invoicing & Payments
CBWTF Admin:
- Configure billing models
- Generate invoices
- Apply penalties & adjustments
- Track outstanding balances

HCF Admin:
- View invoices
- Download PDFs
- Make payments
- Raise disputes

AUTOMATED REMINDER ENGINE:
- Configurable reminder schedules
- Daily reminders until payment clears
- Escalation levels
- Multi-recipient emails
- Stop automatically on payment
- Fully auditable reminder logs

---

### 10. Alerts & Intelligence Engine
Automated detection of:
- Abnormal waste drops
- Blue waste below threshold
- Weight mismatches
- Route violations
- Verification delays
- Attendance anomalies

Alerts must:
- Be configurable
- Have severity levels
- Require acknowledgment
- Be audit logged

---

### 11. Configuration & Customisation (CRITICAL)
Backend-driven configuration:
- Agreement templates
- Terms & Conditions
- Branding
- Geofence defaults
- Thresholds
- Reminder rules
- Feature toggles

Changes must reflect dynamically in:
- Android app
- Web portals

---

### 12. Compliance & CPCB / SPCB Integration
- API integration with CPCB/SPCB
- Configurable reporting windows
- Manual & scheduled submission
- Submission logs
- Payload snapshots
- Acknowledgement storage
- CSV/PDF fallback
- Full audit trail

---

### NON-GOALS
- No gimmicky AI
- No vanity dashboards
- No frontend business logic
- No hardcoded assumptions
- No single-tenant shortcuts

---

EXPECTED OUTPUT
---------------
1. Clear portal information architecture
2. Page-wise functional breakdown
3. Required backend API extensions
4. RBAC matrix across all roles
5. Multi-tenant SaaS configuration strategy
6. Clear separation of Super Admin / CBWTF Admin / HCF Admin
7. AI usage ONLY where it adds operational value
8. Risks, trade-offs, and mitigation strategies

Build this as a **serious enterprise SaaS**, not a startup demo.
Proceed.
