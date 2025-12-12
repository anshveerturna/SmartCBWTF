# HCF Registration Configuration Guide

This document provides detailed instructions for configuring the HCF (Health Care Facility) Registration system, including the Agreement Number Generator and Terms & Conditions management.

---

## Table of Contents

1. [Agreement Number Generator Configuration](#agreement-number-generator-configuration)
2. [Terms & Conditions Management](#terms--conditions-management)
3. [Facility Template Configuration](#facility-template-configuration)
4. [Email Configuration](#email-configuration)
5. [PDF Storage Configuration](#pdf-storage-configuration)
6. [API Reference](#api-reference)

---

## Agreement Number Generator Configuration

The Agreement Number Generator creates unique identifiers for HCF registration agreements. The format is fully configurable via `application.yml`.

### Default Format

```
<FACILITY_CODE>-<PREFIX>-<YEAR>-<SEQUENCE>
Example: DEL-HCF-2025-00001
```

### Configuration Properties

Add the following to your `backend/src/main/resources/application.yml`:

```yaml
app:
  agreement-number:
    # Prefix to include in agreement numbers (e.g., "HCF", "AGR", "BMW")
    prefix: "HCF"
    
    # Separator character between parts of the agreement number
    separator: "-"
    
    # Number of digits for the sequence number (padded with zeros)
    # e.g., 5 → "00001", 6 → "000001"
    sequence-digits: 5
    
    # Whether to include the facility code at the beginning
    # true  → "DEL-HCF-2025-00001"
    # false → "HCF-2025-00001"
    include-facility-code: true
    
    # Whether to include the year in the agreement number
    # true  → "DEL-HCF-2025-00001"
    # false → "DEL-HCF-00001"
    include-year: true
```

### Configuration Options Explained

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `prefix` | String | `"HCF"` | The prefix identifier for agreement types. Use "HCF" for healthcare facilities, "AGR" for general agreements, etc. |
| `separator` | String | `"-"` | Character(s) used to separate parts of the agreement number. Common options: `-`, `/`, `.` |
| `sequence-digits` | Integer | `5` | Number of digits for sequence number. Controls capacity (5 digits = 99,999 per facility/year) |
| `include-facility-code` | Boolean | `true` | Whether to prepend the CBWTF facility code (e.g., "DEL", "MUM") |
| `include-year` | Boolean | `true` | Whether to include current year. When enabled, sequence resets annually |

### Example Configurations

#### Standard Format (Recommended)
```yaml
app:
  agreement-number:
    prefix: "HCF"
    separator: "-"
    sequence-digits: 5
    include-facility-code: true
    include-year: true
# Result: DEL-HCF-2025-00001
```

#### Simple Sequential Format
```yaml
app:
  agreement-number:
    prefix: "AGR"
    separator: ""
    sequence-digits: 8
    include-facility-code: false
    include-year: false
# Result: AGR00000001
```

#### Regional Format (No Year)
```yaml
app:
  agreement-number:
    prefix: "BMW"
    separator: "/"
    sequence-digits: 6
    include-facility-code: true
    include-year: false
# Result: DEL/BMW/000001
```

### How It Works

1. **Atomic Generation**: Agreement numbers are generated atomically using database sequences to prevent duplicates even under concurrent requests.

2. **Per-Facility Sequences**: Each facility maintains its own sequence counter, stored in the `agreement_number_sequence` table.

3. **Annual Reset** (when `include-year: true`): Sequences reset to 1 at the start of each calendar year. Each facility/year combination has its own sequence.

4. **Database Schema**: The sequence is stored in:
```sql
CREATE TABLE agreement_number_sequence (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES facility(id),
    year INTEGER NOT NULL,
    last_sequence INTEGER NOT NULL DEFAULT 0,
    UNIQUE(facility_id, year)
);
```

---

## Terms & Conditions Management

Terms & Conditions are stored per-facility and versioned. HCFs must accept the latest active terms during registration.

### Database Schema

```sql
CREATE TABLE facility_terms (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES facility(id),
    version VARCHAR(64) NOT NULL,           -- e.g., "2025-01-01-v1"
    text_html TEXT NOT NULL,                -- HTML-formatted terms content
    effective_from DATE NOT NULL,
    active BOOLEAN DEFAULT FALSE,
    created_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(facility_id, version)
);
```

### Creating New Terms Version

Use the REST API to create a new terms version:

```bash
POST /api/facilities/{facilityId}/terms
Content-Type: application/json
Authorization: Bearer <admin-token>

{
  "version": "2025-06-01-v1",
  "textHtml": "<h2>BIOMEDICAL WASTE SERVICES</h2><h3>TERMS & CONDITIONS</h3>...",
  "effectiveFrom": "2025-06-01",
  "setActive": true
}
```

### Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `version` | String | Yes | Unique version identifier (e.g., "2025-06-01-v1") |
| `textHtml` | String | Yes | Full HTML content of the terms |
| `effectiveFrom` | Date | Yes | Date from which these terms are effective (YYYY-MM-DD) |
| `setActive` | Boolean | No | If true, activates this version and deactivates all others |

### Updating Terms Content

To update terms content, create a new version (terms are immutable once created for audit purposes):

```bash
POST /api/facilities/{facilityId}/terms
{
  "version": "2025-06-01-v2",
  "textHtml": "<h2>Updated terms content...</h2>",
  "effectiveFrom": "2025-06-01",
  "setActive": true
}
```

### Activating a Specific Version

```bash
PATCH /api/facilities/{facilityId}/terms/{termsId}/activate
Authorization: Bearer <admin-token>
```

### Fetching Latest Terms

The mobile app uses this endpoint to fetch terms for display:

```bash
GET /api/terms/latest?facilityId={optional-uuid}
```

- If `facilityId` is provided: Returns facility-specific terms
- If `facilityId` is omitted: Returns the first globally active terms (fallback)

### Default Terms Template

The system seeds default terms during migration. The default terms HTML content includes:

1. **Terms of Payment** - Payment schedules, GST rules, bouncing charges
2. **Responsibilities of Service Provider** - BMW Rules compliance, collection schedules
3. **Responsibilities of Waste Generator** - Segregation, labeling, storage requirements
4. **Declaration** - Consent acknowledgment

### Customizing Default Terms

To change the default terms for new facilities, edit the Flyway migration file:

```
backend/src/main/resources/db/migration/V4__hcf_agreement_terms_templates.sql
```

Find section 6 ("Insert default Terms & Conditions") and modify the `text_html` content.

**Important**: Once deployed, do NOT modify existing migrations. Create a new migration (e.g., V5__update_default_terms.sql) to update terms for existing facilities.

---

## Facility Template Configuration

Agreement PDF templates are stored per-facility and support placeholder substitution.

### Available Placeholders

The following placeholders are replaced when generating agreement PDFs:

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{{HCF_NAME}}` | Healthcare facility name | "City Hospital" |
| `{{HCF_ADDRESS}}` | Full address | "123 Medical Lane, Delhi" |
| `{{DOCTOR_NAME}}` | Doctor/Owner name | "Dr. Sharma" |
| `{{CONTACT_PHONE}}` | Phone number | "+91 98765 43210" |
| `{{EMAIL}}` | Email address | "admin@cityhospital.com" |
| `{{PAN_NO}}` | PAN number | "ABCDE1234F" |
| `{{GST_NO}}` | GST number | "07ABCDE1234F1Z5" |
| `{{AADHAR_NO}}` | Aadhar number (masked) | "XXXX-XXXX-1234" |
| `{{NO_OF_BEDS}}` | Number of beds | "50" |
| `{{BEDDED}}` | Bedded facility indicator | "Yes" / "No" |
| `{{AGREEMENT_NUMBER}}` | Generated agreement number | "DEL-HCF-2025-00001" |
| `{{AGREEMENT_DATE}}` | Agreement creation date | "December 12, 2025" |
| `{{START_DATE}}` | Agreement start date | "January 01, 2025" |
| `{{END_DATE}}` | Agreement end date | "December 31, 2025" |
| `{{MONTHLY_CHARGES}}` | Monthly service charges | "₹5,000.00" |
| `{{FACILITY_NAME}}` | CBWTF facility name | "Delhi CBWTF" |
| `{{FACILITY_ADDRESS}}` | CBWTF address | "Industrial Area, Delhi" |
| `{{FACILITY_CODE}}` | Facility code | "DEL" |
| `{{TERMS_VERSION}}` | T&C version | "2025-01-01-v1" |
| `{{TERMS_ACCEPTED_AT}}` | Acceptance timestamp | "2025-12-12 14:30:00" |
| `{{PCB_AUTHORIZATION_NO}}` | PCB Authorization Number | "PCB/DEL/2025/001" |
| `{{OTHER_NOTES}}` | Additional notes | "Special handling required" |

### Uploading a Custom Template

```bash
POST /api/facilities/{facilityId}/templates
Content-Type: multipart/form-data
Authorization: Bearer <admin-token>

{
  "name": "Custom Agreement Template",
  "templateType": "HTML",
  "version": "2025-v2",
  "setActive": true,
  "file": <HTML file>
}
```

---

## Email Configuration

Configure email notifications for registration confirmations:

```yaml
app:
  email:
    enabled: false                          # Set to true to enable email sending
    from-address: noreply@smartcbwtf.com    # Sender email address

spring:
  mail:
    host: smtp.example.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

### Email Templates

Registration confirmation emails include:
- Agreement number
- Agreement PDF attachment
- Welcome message with next steps

---

## PDF Storage Configuration

Configure where generated agreement PDFs are stored:

```yaml
app:
  pdf:
    storage-path: files/agreements          # Relative to application working directory
```

For production, consider using:
- Cloud storage (AWS S3, Azure Blob, GCS)
- Shared network storage
- CDN for PDF distribution

---

## API Reference

### Terms Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/terms/latest` | Get latest active terms |
| GET | `/api/facilities/{id}/terms` | List all terms versions for facility |
| POST | `/api/facilities/{id}/terms` | Create new terms version |
| PATCH | `/api/facilities/{id}/terms/{termsId}/activate` | Activate a terms version |

### Template Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/facilities/{id}/templates` | List all templates for facility |
| POST | `/api/facilities/{id}/templates` | Upload new template |
| PATCH | `/api/facilities/{id}/templates/{templateId}/activate` | Activate a template |

### HCF Registration

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/hcfs/register` | Register new HCF with terms acceptance |

#### Registration Request Body

```json
{
  "name": "City Hospital",
  "address": "123 Medical Lane, Delhi 110001",
  "doctorName": "Dr. Sharma",
  "phone": "+919876543210",
  "email": "admin@cityhospital.com",
  "panNo": "ABCDE1234F",
  "gstNo": "07ABCDE1234F1Z5",
  "aadharNo": "123456789012",
  "pcbAuthorizationNo": "PCB/DEL/2025/001",
  "bedded": true,
  "numberOfBeds": 50,
  "monthlyCharges": 5000.00,
  "otherNotes": "24-hour emergency",
  "termsAccepted": true,
  "termsVersion": "2025-01-01-v1",
  "gpsLatitude": 28.6139,
  "gpsLongitude": 77.2090,
  "gpsAccuracy": 10.5,
  "facilityId": "uuid-of-facility",
  "registeredByUserId": "uuid-of-user"
}
```

---

## Troubleshooting

### Terms Not Loading in Mobile App

1. **Check if terms exist**: Query the database:
   ```sql
   SELECT * FROM facility_terms WHERE active = true;
   ```

2. **Check API response**: Test the endpoint directly:
   ```bash
   curl -X GET http://localhost:8080/api/terms/latest
   ```

3. **Verify network**: Ensure the mobile device can reach the backend server

4. **Check logs**: Look for errors in backend logs when the endpoint is called

### Agreement Number Not Generating

1. **Check facility exists**: The HCF must be associated with a valid facility
2. **Check sequence table**: Verify the sequence record exists for facility/year
3. **Check configuration**: Ensure `application.yml` has `app.agreement-number` section

### PDF Generation Fails

1. **Check storage path**: Ensure `app.pdf.storage-path` directory exists and is writable
2. **Check template**: Verify the facility has an active template
3. **Check font resources**: PDFBox requires system fonts for PDF generation

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-12 | Initial configuration guide |

