INSERT INTO system_config (
    config_key,
    config_value,
    value_type,
    category,
    display_name,
    description,
    validation_rules
) VALUES (
    'security.password_max_length',
    '128',
    'NUMBER',
    'SECURITY',
    'Password Max Length',
    'Maximum password length',
    '{"min": 32, "max": 256}'
) ON CONFLICT (config_key) DO NOTHING;
