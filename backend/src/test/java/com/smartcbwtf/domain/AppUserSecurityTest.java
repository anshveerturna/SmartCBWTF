package com.smartcbwtf.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AppUserSecurityTest {

    @Test
    void lockAccountUsesConfiguredMinutes() {
        AppUser user = new AppUser();
        Instant before = Instant.now();

        user.lockAccount(30);

        assertNotNull(user.getLockedUntil());
        long lockedSeconds = Duration.between(before, user.getLockedUntil()).getSeconds();
        assertTrue(lockedSeconds >= 29 * 60 && lockedSeconds <= 31 * 60,
                "lock duration should be close to configured minutes");
    }

    @Test
    void lockAccountUsesMinimumOneMinuteForInvalidInput() {
        AppUser user = new AppUser();
        Instant before = Instant.now();

        user.lockAccount(0);

        assertNotNull(user.getLockedUntil());
        long lockedSeconds = Duration.between(before, user.getLockedUntil()).getSeconds();
        assertTrue(lockedSeconds >= 59 && lockedSeconds <= 70,
                "invalid lock duration should default to 1 minute");
    }

    @Test
    void unlockAccountClearsLockAndFailedAttempts() {
        AppUser user = new AppUser();
        user.incrementFailedAttempts();
        user.incrementFailedAttempts();
        user.lockAccount(10);

        assertTrue(user.isLocked());
        assertTrue(user.getFailedLoginAttempts() > 0);

        user.unlockAccount();

        assertFalse(user.isLocked());
        assertNull(user.getLockedUntil());
        assertEquals(0, user.getFailedLoginAttempts());
    }
}
