INSERT INTO system_config (
    config_key,
    config_value,
    value_type,
    category,
    display_name,
    description
) VALUES (
    'security.password_require_special',
    'true',
    'BOOLEAN',
    'SECURITY',
    'Require Special Character',
    'Require special character in password'
) ON CONFLICT (config_key) DO NOTHING;

UPDATE system_config
SET config_value = 'true',
    updated_at = NOW()
WHERE config_key = 'security.password_require_special'
  AND config_value = 'false'
  AND version = 1
  AND updated_by IS NULL;
