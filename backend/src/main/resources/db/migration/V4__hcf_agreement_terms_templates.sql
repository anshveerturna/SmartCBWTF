-- V4__hcf_agreement_terms_templates.sql
-- HCF Registration, Agreement Terms Acceptance, and Facility Templates

-- =============================================================================
-- 1. Add registration GPS fields to HCF table (for registration location tracking)
-- =============================================================================
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS registration_gps_lat DOUBLE PRECISION;
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS registration_gps_lon DOUBLE PRECISION;
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS registration_gps_accuracy DOUBLE PRECISION;

-- Additional HCF fields required for registration form
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS doctor_name VARCHAR(255);
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS pan_no VARCHAR(20);
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS gst_no VARCHAR(20);
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS aadhar_no VARCHAR(20);
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS monthly_charges NUMERIC(12,2);
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS bedded BOOLEAN DEFAULT FALSE;
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS pcb_authorization_no VARCHAR(100);
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS other_notes TEXT;
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS registered_by_user_id UUID REFERENCES app_user(id);

-- =============================================================================
-- 2. Add terms acceptance fields to Agreement table
-- =============================================================================
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS terms_accepted BOOLEAN DEFAULT FALSE;
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS terms_version VARCHAR(64);
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ;
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS terms_accepted_by UUID REFERENCES app_user(id);
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS template_id UUID;
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS template_version VARCHAR(64);

-- =============================================================================
-- 3. Create facility_template table for facility-specific PDF/HTML templates
-- =============================================================================
CREATE TABLE IF NOT EXISTS facility_template (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    name VARCHAR(255) NOT NULL,
    template_type VARCHAR(20) NOT NULL CHECK (template_type IN ('HTML', 'PDF')),
    content_location TEXT NOT NULL,
    variables_schema JSONB,
    version VARCHAR(64) NOT NULL,
    active BOOLEAN DEFAULT FALSE,
    created_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(facility_id, version)
);

CREATE INDEX IF NOT EXISTS idx_facility_template_facility_id ON facility_template(facility_id);
CREATE INDEX IF NOT EXISTS idx_facility_template_active ON facility_template(facility_id, active) WHERE active = TRUE;

-- =============================================================================
-- 4. Create facility_terms table for facility-specific Terms & Conditions
-- =============================================================================
CREATE TABLE IF NOT EXISTS facility_terms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    version VARCHAR(64) NOT NULL,
    text_html TEXT NOT NULL,
    effective_from DATE NOT NULL,
    active BOOLEAN DEFAULT FALSE,
    created_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(facility_id, version)
);

CREATE INDEX IF NOT EXISTS idx_facility_terms_facility_id ON facility_terms(facility_id);
CREATE INDEX IF NOT EXISTS idx_facility_terms_active ON facility_terms(facility_id, active) WHERE active = TRUE;

-- =============================================================================
-- 5. Create agreement_number_sequence table for atomic number generation
-- =============================================================================
CREATE TABLE IF NOT EXISTS agreement_number_sequence (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    year INTEGER NOT NULL,
    last_sequence INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(facility_id, year)
);

-- =============================================================================
-- 6. Insert default Terms & Conditions for existing facilities
-- =============================================================================
INSERT INTO facility_terms (facility_id, version, text_html, effective_from, active, created_at)
SELECT 
    f.id,
    '2025-01-01-v1',
    '<h2>BIO MEDICAL WASTE COLLECTION & DISPOSAL SERVICES</h2>
<h3>TERMS &amp; CONDITIONS</h3>

<h4>1. TERMS OF PAYMENT</h4>
<ol>
<li>All payment will be made advance of the month. In case payments are not received within month, service will be suspended and the HCF will be responsible for non-compliance of BMW rules &amp; Regulations. When continue service again shall be charge from date of stop service.</li>
<li>Don''t pay CASH or in Personal Account of global staff. If you pay then HCF will be self-responsible for that transaction.</li>
<li>Payment should be transfer by NEFT/RTGS/IMPS/Cheque &amp; online. (No Cash)</li>
<li>Cheque bouncing Charges for Rs. 500 each cheque.</li>
<li>GST on BMW Services is 5% will be charged extra as per Govt. rule.</li>
<li>Whose monthly billing charges is Rs 2500 or less will be billed quarterly and must be paid within 7 days.</li>
<li>If you want to stop the service or want to switch to Other Service Provider then you will have to pay us our full dues, otherwise the dues will be recovered by taking legal action against you. All disputes will be subject to Rudrapur Jurisdiction.</li>
</ol>

<h4>2. RESPONSIBILITIES OF THE "SERVICE PROVIDER"</h4>
<ol>
<li>That the "Service Provider" shall comply with provisions as stipulated in Schedule-1 of the BMW Rule 2025.</li>
<li>That the "Service Provider" shall collect the segregated bio-medical waste from one designated waste collection point within the premises of WASTE GENERATOR on Alternate basis except on Sundays &amp; National Holidays.</li>
<li>That the "Service Provider" shall schedule the timings for collecting the waste in consultation with the GENERATOR.</li>
<li>That the "Service Provider" shall transport the segregated waste in closed container vehicle to its treatment facility.</li>
<li>That the "Service Provider" shall not be held liable for any kind of the violation made by the WASTE GENERATOR / or its staff under Bio-medical Waste Rule 2025.</li>
</ol>

<h4>3. RESPONSIBILITIES OF THE WASTE GENERATOR</h4>
<ol>
<li>That the WASTE GENERATOR shall segregate the Bio-Medical waste at the point of generation in accordance with the BMW Rules 2025 and duly amended thereafter.</li>
<li>That the WASTE GENERATOR shall collect, pack, label and handover the segregated BMW in non-chlorinated bags as stipulated under BMW Rule 2025, which shall be arranged by the "Waste Generator" at its own cost.</li>
<li>That it shall be the sole responsibility of the Waste Generator to keep the BMW under lock and key so as to protect it from any sort of mishandling before it is handed over to the authorized person of Service Provider.</li>
<li>That the Waste Generator shall be responsible to disinfect and mutilate the sharps and handover it in sealed puncture proof containers to "Service Provider".</li>
<li>That the WASTE GENERATOR shall take all necessary steps to ensure that the waste is handled without causing any adverse effect to human health and environment.</li>
<li>That the WASTE GENERATOR shall establish a common secured waste collection end point within its premises for collection, storage of BMW before handing it over to "Service Provider".</li>
<li>That the WASTE GENERATOR shall designate a "Nodal Officer" to interact with "Service Provider".</li>
<li>That the WASTE GENERATOR shall apply and obtain necessary authorization from the Prescribed Authority under BMW Rules 2025 and duly amended thereafter or submit its necessary return to the Prescribed Authority from time to time as laid down the said Rules.</li>
</ol>

<h4>DECLARATION</h4>
<p>That I/We have read and understood the entire contents of this agreement and give my/our free consent to the contents, that both the parties undertake to remain bound by the terms and conditions set out herein above.</p>',
    '2025-01-01',
    TRUE,
    NOW()
FROM facility f
WHERE NOT EXISTS (
    SELECT 1 FROM facility_terms ft WHERE ft.facility_id = f.id
);

-- =============================================================================
-- 7. Insert default HTML template for existing facilities
-- =============================================================================
INSERT INTO facility_template (facility_id, name, template_type, content_location, variables_schema, version, active, created_at)
SELECT 
    f.id,
    'Default Agreement Template',
    'HTML',
    'templates/default_agreement.html',
    '{
        "variables": [
            "HCF_NAME", "HCF_ADDRESS", "DOCTOR_NAME", "CONTACT_PHONE", "EMAIL",
            "PAN_NO", "GST_NO", "AADHAR_NO", "NO_OF_BEDS", "BEDDED",
            "AGREEMENT_NUMBER", "AGREEMENT_DATE", "START_DATE", "END_DATE",
            "MONTHLY_CHARGES", "FACILITY_NAME", "FACILITY_ADDRESS", "FACILITY_CODE",
            "TERMS_ACCEPTED_LINE", "TERMS_VERSION", "TERMS_ACCEPTED_AT",
            "PCB_AUTHORIZATION_NO", "OTHER_NOTES"
        ]
    }'::JSONB,
    '2025-01-01-v1',
    TRUE,
    NOW()
FROM facility f
WHERE NOT EXISTS (
    SELECT 1 FROM facility_template ft WHERE ft.facility_id = f.id
);

-- Add foreign key constraint for template_id in agreement
ALTER TABLE agreement ADD CONSTRAINT fk_agreement_template 
    FOREIGN KEY (template_id) REFERENCES facility_template(id);
