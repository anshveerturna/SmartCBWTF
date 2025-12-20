package com.smartcbwtf.service;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.SystemConfig;
import com.smartcbwtf.domain.SystemConfigAudit;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.SystemConfigAuditRepository;
import com.smartcbwtf.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing system configuration with caching, validation, and audit
 * logging.
 */
@Service
public class SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigService.class);

    private final SystemConfigRepository configRepository;
    private final SystemConfigAuditRepository auditRepository;
    private final AppUserRepository userRepository;

    // In-memory cache for fast lookups
    private final Map<String, SystemConfig> configCache = new ConcurrentHashMap<>();
    private volatile Instant lastCacheRefresh = Instant.MIN;

    public SystemConfigService(
            SystemConfigRepository configRepository,
            SystemConfigAuditRepository auditRepository,
            AppUserRepository userRepository) {
        this.configRepository = configRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    // ========== READ OPERATIONS ==========

    /**
     * Get all configurations.
     */
    public List<SystemConfig> getAllConfigs() {
        return configRepository.findAllByOrderByCategoryAscConfigKeyAsc();
    }

    /**
     * Get configurations by category.
     */
    public List<SystemConfig> getByCategory(String category) {
        return configRepository.findByCategoryOrderByConfigKeyAsc(category);
    }

    /**
     * Get a single configuration value.
     * Uses cache for performance.
     */
    public Optional<SystemConfig> getByKey(String key) {
        // Check cache first
        if (configCache.containsKey(key)) {
            return Optional.of(configCache.get(key));
        }

        Optional<SystemConfig> config = configRepository.findByConfigKey(key);
        config.ifPresent(c -> configCache.put(key, c));
        return config;
    }

    /**
     * Get string value with default.
     */
    public String getString(String key, String defaultValue) {
        return getByKey(key).map(SystemConfig::getStringValue).orElse(defaultValue);
    }

    /**
     * Get integer value with default.
     */
    public int getInt(String key, int defaultValue) {
        return getByKey(key).map(SystemConfig::getNumberValue).orElse(defaultValue);
    }

    /**
     * Get boolean value with default.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return getByKey(key).map(SystemConfig::getBooleanValue).orElse(defaultValue);
    }

    /**
     * Get all configs grouped by category.
     */
    public Map<String, List<SystemConfig>> getAllGroupedByCategory() {
        return getAllConfigs().stream()
                .collect(Collectors.groupingBy(SystemConfig::getCategory));
    }

    // ========== UPDATE OPERATIONS ==========

    /**
     * Update a single configuration value.
     */
    @Transactional
    public SystemConfig updateConfig(String key, String newValue, UUID updatedById, String reason, String ipAddress) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + key));

        if (config.isReadonly()) {
            throw new IllegalArgumentException("Configuration is read-only: " + key);
        }

        // Validate value based on type
        validateValue(config, newValue);

        String oldValue = config.getConfigValue();

        // Update config
        config.setConfigValue(newValue);
        config.setVersion(config.getVersion() + 1);
        config.setUpdatedAt(Instant.now());

        if (updatedById != null) {
            userRepository.findById(updatedById).ifPresent(config::setUpdatedBy);
        }

        config = configRepository.save(config);

        // Create audit log
        AppUser changedBy = updatedById != null ? userRepository.findById(updatedById).orElse(null) : null;
        SystemConfigAudit audit = SystemConfigAudit.create(key, oldValue, newValue, changedBy, reason, ipAddress);
        auditRepository.save(audit);

        // Update cache
        configCache.put(key, config);

        log.info("Config updated: {} = {} (was: {})", key, newValue, oldValue);
        return config;
    }

    /**
     * Bulk update configurations for a category.
     */
    @Transactional
    public List<SystemConfig> bulkUpdateCategory(String category, Map<String, String> updates,
            UUID updatedById, String reason, String ipAddress) {
        List<SystemConfig> updatedConfigs = new ArrayList<>();

        for (Map.Entry<String, String> entry : updates.entrySet()) {
            try {
                SystemConfig config = updateConfig(entry.getKey(), entry.getValue(), updatedById, reason, ipAddress);
                updatedConfigs.add(config);
            } catch (Exception e) {
                log.error("Failed to update config {}: {}", entry.getKey(), e.getMessage());
                throw new RuntimeException("Failed to update " + entry.getKey() + ": " + e.getMessage());
            }
        }

        return updatedConfigs;
    }

    // ========== VALIDATION ==========

    private void validateValue(SystemConfig config, String value) {
        switch (config.getValueTypeEnum()) {
            case NUMBER -> {
                try {
                    double numValue = Double.parseDouble(value);
                    Map<String, Object> rules = config.getValidationRules();
                    if (rules != null) {
                        if (rules.containsKey("min")) {
                            double min = ((Number) rules.get("min")).doubleValue();
                            if (numValue < min) {
                                throw new IllegalArgumentException("Value must be at least " + min);
                            }
                        }
                        if (rules.containsKey("max")) {
                            double max = ((Number) rules.get("max")).doubleValue();
                            if (numValue > max) {
                                throw new IllegalArgumentException("Value must be at most " + max);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Value must be a number");
                }
            }
            case BOOLEAN -> {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("Value must be true or false");
                }
            }
            case JSON -> {
                try {
                    // Basic JSON validation - must start with [ or {
                    String trimmed = value.trim();
                    if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
                        throw new IllegalArgumentException("Value must be valid JSON");
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Value must be valid JSON");
                }
            }
            case STRING -> {
                // No special validation for strings
            }
        }
    }

    // ========== CACHE MANAGEMENT ==========

    /**
     * Refresh the configuration cache.
     */
    public void refreshCache() {
        configCache.clear();
        List<SystemConfig> all = configRepository.findAll();
        for (SystemConfig config : all) {
            configCache.put(config.getConfigKey(), config);
        }
        lastCacheRefresh = Instant.now();
        log.info("Configuration cache refreshed with {} entries", all.size());
    }

    // ========== MOBILE CONFIG ==========

    /**
     * Get configuration for mobile app.
     * Returns non-sensitive operational and security settings.
     */
    public Map<String, Object> getMobileConfig() {
        List<String> categories = List.of("OPERATIONAL", "SECURITY", "PLATFORM_GLOBAL");
        List<SystemConfig> configs = configRepository.findByIsSensitiveFalseAndCategoryIn(categories);

        Map<String, Object> result = new HashMap<>();
        for (SystemConfig config : configs) {
            Object value = switch (config.getValueTypeEnum()) {
                case NUMBER -> config.getNumberValue();
                case BOOLEAN -> config.getBooleanValue();
                case JSON -> config.getConfigValue(); // Return as string, client parses
                default -> config.getStringValue();
            };
            result.put(config.getConfigKey(), value);
        }

        return result;
    }

    // ========== AUDIT ==========

    /**
     * Get audit history for a configuration key.
     */
    public List<SystemConfigAudit> getAuditHistory(String key) {
        return auditRepository.findByConfigKeyOrderByChangedAtDesc(key);
    }

    /**
     * Get recent configuration changes.
     */
    public List<SystemConfigAudit> getRecentChanges() {
        return auditRepository.findTop20ByOrderByChangedAtDesc();
    }
}
