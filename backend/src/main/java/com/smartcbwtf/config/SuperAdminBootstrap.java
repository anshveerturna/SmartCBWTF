package com.smartcbwtf.config;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.PasswordPolicyValidator;
import java.time.Instant;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class SuperAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrap.class);
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private static final String DEFAULT_USERNAME = "super_admin";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final Environment environment;
    private final String configuredUsername;
    private final String configuredPassword;
    private final String configuredEmail;
    private final String configuredFullName;

    public SuperAdminBootstrap(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            Environment environment,
            @Value("${app.bootstrap.super-admin.username:}") String configuredUsername,
            @Value("${app.bootstrap.super-admin.password:}") String configuredPassword,
            @Value("${app.bootstrap.super-admin.email:}") String configuredEmail,
            @Value("${app.bootstrap.super-admin.full-name:Platform Administrator}") String configuredFullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.environment = environment;
        this.configuredUsername = trimToEmpty(configuredUsername);
        this.configuredPassword = trimToEmpty(configuredPassword);
        this.configuredEmail = trimToEmpty(configuredEmail);
        this.configuredFullName = trimToEmpty(configuredFullName);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long activeSuperAdminCount = userRepository.countByRoleAndActive(SUPER_ADMIN_ROLE, true);
        if (StringUtils.hasText(configuredPassword)) {
            if (activeSuperAdminCount > 0) {
                log.warn("INITIAL_SUPER_ADMIN_PASSWORD is set but an active SUPER_ADMIN already exists; "
                        + "ignoring bootstrap credentials. Remove this environment variable after first launch.");
                return;
            }
            bootstrapConfiguredSuperAdmin();
            return;
        }

        if (isProdProfile() && activeSuperAdminCount == 0) {
            throw new IllegalStateException(
                    "Production startup requires an active SUPER_ADMIN or INITIAL_SUPER_ADMIN_PASSWORD.");
        }

        if (activeSuperAdminCount == 0) {
            log.warn("No active SUPER_ADMIN exists. Set INITIAL_SUPER_ADMIN_PASSWORD before production launch.");
        }
    }

    private void bootstrapConfiguredSuperAdmin() {
        validateBootstrapPassword();

        String username = StringUtils.hasText(configuredUsername) ? configuredUsername : DEFAULT_USERNAME;
        Instant now = Instant.now();
        AppUser user = userRepository.findByUsername(username).orElseGet(AppUser::new);

        if (user.getId() == null) {
            user.setUsername(username);
            user.setCreatedAt(now);
        }

        user.setRole(SUPER_ADMIN_ROLE);
        user.setActive(true);
        user.setForcePasswordChange(true);
        user.setMustChangePassword(true);
        user.setPasswordHash(passwordEncoder.encode(configuredPassword));
        user.setUpdatedAt(now);
        user.setPasswordChangedAt(now);

        if (StringUtils.hasText(configuredEmail)) {
            user.setEmail(configuredEmail);
        }
        if (StringUtils.hasText(configuredFullName)) {
            user.setFullName(configuredFullName);
        } else if (!StringUtils.hasText(user.getFullName())) {
            user.setFullName("Platform Administrator");
        }

        userRepository.save(user);
        log.info("Bootstrapped SUPER_ADMIN account '{}'. Password change is required on first login.", username);
    }

    private void validateBootstrapPassword() {
        passwordPolicyValidator.validateOrThrow(configuredPassword);
        if ("demo123".equalsIgnoreCase(configuredPassword) || "password".equalsIgnoreCase(configuredPassword)) {
            throw new IllegalStateException("INITIAL_SUPER_ADMIN_PASSWORD cannot use a known demo password.");
        }
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
