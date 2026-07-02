-- Retire legacy seeded users with public demo credentials.
-- Real first-super-admin creation now uses app.bootstrap.super-admin.* env settings.

UPDATE app_user
SET active = false,
    force_password_change = true,
    must_change_password = true,
    updated_at = NOW()
WHERE username = 'super_admin'
  AND password_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';

UPDATE app_user
SET active = false,
    force_password_change = true,
    must_change_password = true,
    updated_at = NOW()
WHERE username IN ('test_cbwtf_admin', 'test_driver', 'test_operator')
  AND password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsS8gWvD9.lSbL5mUe';
