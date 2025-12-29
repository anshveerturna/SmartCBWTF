CBWTF Admin Portal
Enterprise Implementation Plan (Comprehensive)
Executive Summary
This document defines the complete implementation plan for the CBWTF Admin Portal — a premium, enterprise-grade SaaS product for biomedical waste management facility owners.

Target Users: CBWTF owners (paying customers)
Access Level: Single-tenant (CBWTF_ADMIN sees ONLY their own data)
Non-Goals: SuperAdmin features, Android app, Payment gateway integration

Architecture Overview
Ownership Model
HCF → Agreement → CBWTF (Facility)
HCF (Healthcare Facility): Hospitals, clinics, labs that generate biomedical waste
Agreement: Legal contract binding HCF to CBWTF for waste management
CBWTF (Facility): Biomedical waste treatment facility that processes waste
IMPORTANT

Agreement is the single source of truth for ownership, validity, and enforcement.

Core Invariants
Invariant	Enforcement
ONE active agreement per HCF globally	Unique DB index
No new agreement if previous is ACTIVE	Service layer
No new agreement if dues ≠ CLEAR	Service layer
All operations require active agreement	assertAgreementActive()
PHASE 0: Agreement Foundation
Duration: 1 week

Purpose
Establish Agreement as a first-class ownership object with strict enforcement, snapshots for historical accuracy, and human-readable codes.

0.1 Agreement Entity
Schema
CREATE TABLE agreement (
    -- Identity
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agreement_code VARCHAR(20) UNIQUE NOT NULL,  -- AGR-2024-00001
    
    -- Parties
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
        -- ACTIVE: Contract in force
        -- EXPIRED: End date passed
        -- TERMINATED: Manually ended
        -- DISPUTED: Legal dispute open
    
    dues_status VARCHAR(20) NOT NULL DEFAULT 'CLEAR',
        -- CLEAR: No outstanding payments
        -- PENDING: Invoices unpaid
        -- DISPUTED: Payment dispute
    
    -- Contract Terms
    start_date DATE NOT NULL,
    end_date DATE,
    per_bed_per_day_rate DECIMAL(10,2) NOT NULL,
    terms_text TEXT,
    pdf_url VARCHAR(500),
    
    -- Termination (if applicable)
    termination_reason TEXT,
    terminated_at TIMESTAMP,
    terminated_by UUID REFERENCES app_user(id),
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES app_user(id),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
-- CRITICAL: Only one ACTIVE agreement per HCF across ALL CBWTFs
CREATE UNIQUE INDEX idx_one_active_per_hcf 
ON agreement(hcf_id) WHERE status = 'ACTIVE';
Java Entity
@Entity
@Table(name = "agreement")
public class Agreement {
    public enum Status { ACTIVE, EXPIRED, TERMINATED, DISPUTED }
    public enum DuesStatus { CLEAR, PENDING, DISPUTED }
    
    @Id @GeneratedValue
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String agreementCode;  // AGR-2024-00001
    
    @ManyToOne(optional = false)
    private Hcf hcf;
    
    @ManyToOne(optional = false)
    private Facility facility;
    
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    private DuesStatus duesStatus = DuesStatus.CLEAR;
    
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal perBedPerDayRate;
    
    private String terminationReason;
    private Instant terminatedAt;
    private UUID terminatedBy;
    
    private Instant createdAt = Instant.now();
}
0.2 Agreement Code Generation
Format
AGR-<YEAR>-<SEQUENCE>
Example: AGR-2024-00142
Why Human-Readable?
Referenced in emails to HCFs
Printed on invoices
Used in CPCB/SPCB reports
Referenced in legal disputes
Spoken over phone calls
Service
@Service
public class AgreementCodeGeneratorService {
    @Autowired
    private AgreementCodeSequenceRepository sequenceRepo;
    
    @Transactional
    public String generateNextCode() {
        int year = Year.now().getValue();
        AgreementCodeSequence seq = sequenceRepo.findByYear(year)
            .orElse(new AgreementCodeSequence(year, 0));
        seq.setLastValue(seq.getLastValue() + 1);
        sequenceRepo.save(seq);
        return String.format("AGR-%d-%05d", year, seq.getLastValue());
    }
}
0.3 Agreement Block Reasons
When a new agreement cannot be created, provide machine-readable reason:

public enum AgreementBlockReason {
    ACTIVE_AGREEMENT_EXISTS,  // HCF has active agreement (any CBWTF)
    UNPAID_DUES,              // Previous agreement has pending dues
    DISPUTE_OPEN,             // Open dispute on previous agreement
    BLACKLISTED               // HCF on platform blacklist
}
public class AgreementBlockedException extends RuntimeException {
    private final AgreementBlockReason reason;
    private final UUID existingAgreementId;  // If applicable
    // ...
}
Exposure
Consumer	How
CBWTF Admin UI	API returns block reason
SuperAdmin	Can view all blocks
Audit logs	Recorded with reason
0.4 Status Transition Rules
Allowed Transitions
Creation
End date reached / manual
Early termination
Dispute raised
Dispute resolved
ACTIVE
EXPIRED
TERMINATED
DISPUTED
Disallowed (No Resurrection)
❌ EXPIRED → ACTIVE
❌ TERMINATED → ACTIVE
❌ DISPUTED → ACTIVE
Enforcement
@Service
public class AgreementTransitionService {
    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
        Status.ACTIVE, Set.of(Status.EXPIRED, Status.TERMINATED, Status.DISPUTED),
        Status.DISPUTED, Set.of(Status.EXPIRED)
    );
    
    public void transition(Agreement agreement, Status to, String reason) {
        if (!ALLOWED.getOrDefault(agreement.getStatus(), Set.of()).contains(to)) {
            throw new IllegalTransitionException(agreement.getStatus(), to);
        }
        agreement.setStatus(to);
        if (to == Status.TERMINATED) {
            agreement.setTerminationReason(reason);
            agreement.setTerminatedAt(Instant.now());
            agreement.setTerminatedBy(getCurrentUserId());
        }
        auditLog.log("AGREEMENT_STATUS_CHANGED", agreement.getId(), to);
    }
}
0.5 Agreement Snapshots
Purpose
Preserve historical truth for invoices, reports, and disputes even when agreement terms change later.

Schema
CREATE TABLE agreement_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agreement_id UUID NOT NULL REFERENCES agreement(id),
    snapshot_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Frozen values at time of snapshot
    agreement_code VARCHAR(20) NOT NULL,
    hcf_id UUID NOT NULL,
    hcf_name VARCHAR(255) NOT NULL,
    hcf_gst VARCHAR(20),
    hcf_pan VARCHAR(20),
    facility_id UUID NOT NULL,
    facility_name VARCHAR(255) NOT NULL,
    per_bed_per_day_rate DECIMAL(10,2) NOT NULL,
    number_of_beds INTEGER,
    terms_text TEXT,
    status VARCHAR(20) NOT NULL,
    
    -- Why this snapshot was created
    snapshot_reason VARCHAR(50) NOT NULL,
        -- INVOICE_GENERATED
        -- CPCB_REPORT
        -- EXPORT_JOB
        -- DISPUTE_OPENED
    
    created_by UUID REFERENCES app_user(id)
);
CREATE INDEX idx_snapshot_agreement ON agreement_snapshot(agreement_id);
Usage
Document	Links To
Invoice	agreement_snapshot_id
CPCB Report	agreement_snapshot_id
Export Job	agreement_snapshot_id
Dispute	agreement_snapshot_id
Service
@Service
public class AgreementSnapshotService {
    public AgreementSnapshot createSnapshot(UUID agreementId, SnapshotReason reason) {
        Agreement a = agreementRepo.findById(agreementId).orElseThrow();
        Hcf hcf = a.getHcf();
        Facility f = a.getFacility();
        
        AgreementSnapshot snap = new AgreementSnapshot();
        snap.setAgreementId(agreementId);
        snap.setAgreementCode(a.getAgreementCode());
        snap.setHcfId(hcf.getId());
        snap.setHcfName(hcf.getName());
        snap.setHcfGst(hcf.getGstNo());
        snap.setFacilityId(f.getId());
        snap.setFacilityName(f.getName());
        snap.setPerBedPerDayRate(a.getPerBedPerDayRate());
        snap.setNumberOfBeds(hcf.getNumberOfBeds());
        snap.setStatus(a.getStatus().name());
        snap.setSnapshotReason(reason.name());
        
        return snapshotRepo.save(snap);
    }
}
0.6 Agreement Validation Service
Central Enforcement Point
@Service
public class AgreementValidationService {
    
    /**
     * Check if HCF can have a new agreement with this CBWTF.
     * Returns eligibility status with block reason if applicable.
     */
    public AgreementEligibility checkEligibility(UUID hcfId) {
        // 1. Check for ACTIVE agreement (any CBWTF)
        Optional<Agreement> active = agreementRepo.findActiveByHcf(hcfId);
        if (active.isPresent()) {
            return AgreementEligibility.blocked(
                AgreementBlockReason.ACTIVE_AGREEMENT_EXISTS,
                active.get().getId()
            );
        }
        
        // 2. Check for unpaid dues on any previous agreement
        List<Agreement> withDues = agreementRepo.findByHcfAndDuesStatus(
            hcfId, DuesStatus.PENDING
        );
        if (!withDues.isEmpty()) {
            return AgreementEligibility.blocked(
                AgreementBlockReason.UNPAID_DUES,
                withDues.get(0).getId()
            );
        }
        
        // 3. Check for open disputes
        List<Agreement> disputed = agreementRepo.findByHcfAndStatus(
            hcfId, Status.DISPUTED
        );
        if (!disputed.isEmpty()) {
            return AgreementEligibility.blocked(
                AgreementBlockReason.DISPUTE_OPEN,
                disputed.get(0).getId()
            );
        }
        
        // 4. Check blacklist (future)
        if (blacklistService.isBlacklisted(hcfId)) {
            return AgreementEligibility.blocked(
                AgreementBlockReason.BLACKLISTED, null
            );
        }
        
        return AgreementEligibility.eligible();
    }
    
    /**
     * Throws if HCF cannot have new agreement.
     */
    public void assertCanCreateAgreement(UUID hcfId) {
        AgreementEligibility e = checkEligibility(hcfId);
        if (!e.isEligible()) {
            throw new AgreementBlockedException(e.getBlockReason(), e.getBlockingAgreementId());
        }
    }
}
0.7 Agreement Guard Service
Universal Gate for All Operations
@Service
public class AgreementGuardService {
    
    /**
     * Assert that agreement is ACTIVE. Called by all operations.
     */
    public void assertAgreementActive(UUID agreementId) {
        Agreement a = agreementRepo.findById(agreementId)
            .orElseThrow(() -> new AgreementNotFoundException(agreementId));
        
        if (a.getStatus() != Agreement.Status.ACTIVE) {
            throw new AgreementNotActiveException(
                agreementId, 
                a.getStatus(),
                "Operation requires active agreement"
            );
        }
    }
    
    /**
     * Get active agreement for HCF under current CBWTF.
     */
    public Agreement getActiveAgreement(UUID hcfId, UUID facilityId) {
        return agreementRepo.findByHcfAndFacilityAndStatus(
            hcfId, facilityId, Status.ACTIVE
        ).orElseThrow(() -> new NoActiveAgreementException(hcfId, facilityId));
    }
}
Gated Operations
Operation	Gate Call Location
QR Generation	QRAuthorizationService.generateQR()
Bag Scanning	BagEventService.recordEvent()
Pickup	PickupService.recordPickup()
Verification	VerificationService.verify()
Invoice Generation	InvoiceService.generateInvoice()
CPCB Reporting	CPCBReportService.generateReport()
PHASE 0.5: HCF Identity Fingerprint
Duration: 2 days

Purpose
Detect duplicate/fraudulent HCF registrations across CBWTFs.

Identity Hash
@Service
public class HCFIdentityService {
    
    public String computeFingerprint(String name, String gst, String pan, 
                                      Double lat, Double lon) {
        String normalized = normalize(name) + "|" +
                           normalize(gst) + "|" +
                           normalize(pan) + "|" +
                           roundCoords(lat, lon, 3);  // 3 decimal places ≈ 100m
        return sha256(normalized);
    }
    
    private String normalize(String s) {
        if (s == null) return "";
        return s.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")  // Remove special chars
                .trim();
    }
    
    private String roundCoords(Double lat, Double lon, int decimals) {
        if (lat == null || lon == null) return "0|0";
        double factor = Math.pow(10, decimals);
        return Math.round(lat * factor) + "|" + Math.round(lon * factor);
    }
}
Schema Change
ALTER TABLE hcf ADD COLUMN identity_hash VARCHAR(64);
CREATE INDEX idx_hcf_identity_hash ON hcf(identity_hash);
Use Cases
Use Case	How
Duplicate detection	Match hash on registration
Fraud flagging	Same hash, different details
Blacklist matching	Store hashes of banned HCFs
PHASE 1: Multi-Tenant Foundation
Duration: 1 week

Purpose
Ensure CBWTF admin sees ONLY data from their own CBWTF's active agreements.

Tenant Context
public class TenantContext {
    private static final ThreadLocal<UUID> currentFacilityId = new ThreadLocal<>();
    
    public static UUID getFacilityId() {
        UUID id = currentFacilityId.get();
        if (id == null) throw new TenantNotSetException();
        return id;
    }
    
    public static void setFacilityId(UUID id) {
        currentFacilityId.set(id);
    }
    
    public static void clear() {
        currentFacilityId.remove();
    }
}
Tenant Context Filter
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)  // After auth filter
public class TenantContextFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest req, 
                                     HttpServletResponse res,
                                     FilterChain chain) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken) {
                UUID facilityId = extractFacilityId(auth);
                TenantContext.setFacilityId(facilityId);
            }
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}
HCF Queries via Agreement
Since HCF has no direct facility_id, all HCF queries must JOIN through Agreement:

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, UUID> {
    
    @Query("SELECT a FROM Agreement a " +
           "WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE'")
    List<Agreement> findActiveByFacility(@Param("facilityId") UUID facilityId);
    
    @Query("SELECT a.hcf FROM Agreement a " +
           "WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE'")
    List<Hcf> findHcfsByFacility(@Param("facilityId") UUID facilityId);
}
PHASE 2: CBWTF Dashboard
Duration: 1 week

Purpose
Operational command center for CBWTF owner.

Metrics
Metric	Source
Active HCFs	Count of ACTIVE agreements
Total waste today	BagEvent sum
Vehicles online	GPS events < 15 min
Staff attendance	Attendance records
Unpaid invoices	Invoice.status = ISSUED
Subscription days left	Facility.subscriptionExpiresAt
Risk Alerts
Severity	Condition
CRITICAL	Subscription expires < 7 days
CRITICAL	CPCB report overdue
HIGH	Invoice > 30 days unpaid
HIGH	Vehicle offline > 24h
MEDIUM	Agreement expires < 30 days
PHASE 3: Vehicle & GPS
Duration: 2 weeks

Vehicle Entity
CREATE TABLE vehicle (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES facility(id),
    registration_number VARCHAR(20) NOT NULL,
    gps_device_id VARCHAR(100),
    assigned_driver_id UUID REFERENCES app_user(id),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(facility_id, registration_number)
);
GPS Event (Append-Only)
CREATE TABLE gps_event (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    speed DECIMAL(6,2),
    recorded_at TIMESTAMP NOT NULL,
    received_at TIMESTAMP DEFAULT NOW(),
    source VARCHAR(20)  -- MOBILE_APP, IOT_DEVICE
);
CREATE INDEX idx_gps_vehicle_time ON gps_event(vehicle_id, recorded_at DESC);
CAUTION

GPS events are APPEND-ONLY. No updates or deletes. Legal defensibility.

PHASE 4: Staff Management
Duration: 1 week

Scope
Create/update/disable users under CBWTF
Password management
Activity tracking
GPS history per user
User Roles in CBWTF
Role	Permissions
DRIVER	Pickup, attendance
PLANT_OPERATOR	Verification
CBWTF_ADMIN	Full portal access
PHASE 5: HCF Registration
Duration: 2 weeks

Registration Flow
No
Yes
Yes
No
ACTIVE exists
Dues pending
Dispute open
Eligible
CBWTF Admin submits HCF
HCF exists?
Create new HCF
Check identity hash
Suspicious match?
Flag for review
Eligibility check
❌ Block: Active agreement
❌ Block: Unpaid dues
❌ Block: Dispute
✅ Create Agreement
Generate agreement_code
Create HCF user
Send welcome email
PHASE 6: QR Authorization
Duration: 1 week

QR ↔ Agreement Binding
ALTER TABLE qr_authorization 
ADD COLUMN agreement_id UUID NOT NULL REFERENCES agreement(id);
Lifecycle
Agreement Status	QR Effect
ACTIVE	Valid
EXPIRED	Auto-invalid
TERMINATED	Auto-revoked
DISPUTED	Blocked
Scheduled Job
@Scheduled(cron = "0 0 1 * * *")  // 1 AM daily
public void revokeQRsForExpiredAgreements() {
    List<Agreement> expired = agreementRepo.findExpiredWithActiveQRs();
    for (Agreement a : expired) {
        qrRepo.revokeAllByAgreement(a.getId(), "AGREEMENT_EXPIRED");
        auditLog.log("QRS_AUTO_REVOKED", a.getId());
    }
}
PHASE 7: Invoicing
Duration: 2 weeks

Invoice Generation
@Transactional
public Invoice generateInvoice(UUID agreementId, LocalDate from, LocalDate to) {
    // 1. Assert agreement active
    agreementGuard.assertAgreementActive(agreementId);
    
    // 2. Create snapshot for historical accuracy
    AgreementSnapshot snap = snapshotService.createSnapshot(
        agreementId, SnapshotReason.INVOICE_GENERATED
    );
    
    // 3. Calculate line items from bag events
    List<BagEvent> events = bagEventRepo.findByAgreementAndPeriod(
        agreementId, from, to
    );
    
    // 4. Apply rates from snapshot
    BigDecimal amount = calculateAmount(events, snap.getPerBedPerDayRate());
    
    // 5. Create invoice
    Invoice inv = new Invoice();
    inv.setAgreementId(agreementId);
    inv.setAgreementSnapshotId(snap.getId());
    inv.setPeriodStart(from);
    inv.setPeriodEnd(to);
    inv.setAmount(amount);
    inv.setStatus(InvoiceStatus.ISSUED);
    
    return invoiceRepo.save(inv);
}
PHASE 8: Email & Compliance
Duration: 1 week

Email Templates
Template	Trigger
hcf_welcome	Agreement created
hcf_credentials	User created
payment_reminder	7 days before due
payment_overdue	Overdue
agreement_expiring	30 days before end
CPCB Report
Uses agreement_snapshot_id for accuracy
Generates prescribed format
Audit logged
Timeline Summary
Phase	Focus	Duration
0	Agreement Foundation	1 week
0.5	HCF Fingerprint	2 days
1	Multi-Tenant	1 week
2	Dashboard	1 week
3	GPS	2 weeks
4	Staff	1 week
5	HCF Registration	2 weeks
6	QR	1 week
7	Invoicing	2 weeks
8	Email/Compliance	1 week
Total: ~12 weeks

Verification Strategy
Each phase must pass:

 Unit tests (>80% coverage)
 Invariant violation tests (must fail correctly)
 Tenant isolation tests
 Audit log verification
 Security review
