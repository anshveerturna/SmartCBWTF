-- V11__seed_test_cbwtf_and_user.sql
-- Test data for Phase 4 validation
-- All users have password 'password' (BCrypt hash below)

-- BCrypt hash for 'password'
-- $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS8gWvD9.lSbL5mUe

-- Step 1: Create test CBWTF facility
INSERT INTO facility (id, code, name, address, contact_email, contact_phone, 
                       subscription_plan, subscription_status, subscription_expires_at, 
                       created_at, updated_at)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'CBWTF-TEST-001',
    'Test CBWTF Facility',
    '123 Test Street, Test City, TS 12345',
    'test@testcbwtf.com',
    '9876543210',
    'PRO',
    'ACTIVE',
    CURRENT_TIMESTAMP + INTERVAL '365 days',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (code) DO NOTHING;

-- Step 2: Create test CBWTF Admin user
INSERT INTO app_user (id, username, password_hash, full_name, email, phone, role, 
                      facility_id, active, force_password_change, created_at, updated_at)
VALUES (
    'b2c3d4e5-f6a7-8901-bcde-f23456789012',
    'test_cbwtf_admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS8gWvD9.lSbL5mUe',
    'Test CBWTF Administrator',
    'admin@testcbwtf.com',
    '9876543211',
    'CBWTF_ADMIN',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;

-- Step 3: Create test Driver user
INSERT INTO app_user (id, username, password_hash, full_name, email, phone, role, 
                      facility_id, active, force_password_change, created_at, updated_at)
VALUES (
    'c3d4e5f6-a7b8-9012-cdef-345678901234',
    'test_driver',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS8gWvD9.lSbL5mUe',
    'Test Driver',
    'driver@testcbwtf.com',
    '9876543212',
    'DRIVER',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;

-- Step 4: Create test Plant Operator user
INSERT INTO app_user (id, username, password_hash, full_name, email, phone, role, 
                      facility_id, active, force_password_change, created_at, updated_at)
VALUES (
    'd4e5f6a7-b8c9-0123-defa-456789012345',
    'test_operator',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS8gWvD9.lSbL5mUe',
    'Test Plant Operator',
    'operator@testcbwtf.com',
    '9876543213',
    'PLANT_OPERATOR',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;

-- Note: Audit record skipped - test data doesn't need audit trail
