-- V18: Remove subscription plans (single-tier model) and clean up config
-- User requested no subscription plans

DELETE FROM system_config WHERE config_key = 'subscription.allowed_plans';

-- Update any subscription-related messaging
UPDATE system_config 
SET description = 'Default subscription duration in months (single-tier)'
WHERE config_key = 'subscription.default_duration_months';
