package com.epam.gym.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    @Test
    void isBlocked_whenNoAttempts_returnsFalse() {
        // Given a fresh username
        String username = "testUser";

        // When/Then
        assertFalse(loginAttemptService.isBlocked(username),
                "User should not be blocked initially");
    }

    @Test
    void isBlocked_whenTwoFailedAttempts_returnsFalse() {
        String username = "testUser";

        // Given 2 failed attempts (less than max)
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);

        // When/Then
        assertFalse(loginAttemptService.isBlocked(username),
                "User should not be blocked after 2 failed attempts");
    }

    @Test
    void isBlocked_whenThreeFailedAttempts_returnsTrue() {
        String username = "testUser";

        // Given 3 failed attempts (reaches max)
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);

        // When/Then
        assertTrue(loginAttemptService.isBlocked(username),
                "User should be blocked after 3 failed attempts");
    }

    @Test
    void loginSucceeded_resetsFailedAttempts() {
        String username = "testUser";

        // Given 2 failed attempts
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);
        assertFalse(loginAttemptService.isBlocked(username));

        // When login succeeds
        loginAttemptService.loginSucceeded(username);

        // And another 2 failed attempts occur
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);

        // Then user is still not blocked (counter was reset)
        assertFalse(loginAttemptService.isBlocked(username),
                "Attempts should reset after a successful login");
    }


    @Test
    void clearOldAttempts_shouldRemoveEntriesOlderThan30Minutes() {
        String oldUser = "oldUser";
        String recentUser = "recentUser";

        for (int i = 0; i < 3; i++) {
            loginAttemptService.loginFailed(oldUser);
            loginAttemptService.loginFailed(recentUser);
        }

        assertTrue(loginAttemptService.isBlocked(oldUser));
        assertTrue(loginAttemptService.isBlocked(recentUser));

        @SuppressWarnings("unchecked")
        Map<String, Object> cache = (Map<String, Object>) ReflectionTestUtils.getField(loginAttemptService, "attemptsCache");

        Object oldAttempt = cache.get(oldUser);
        ReflectionTestUtils.setField(oldAttempt, "lastAttemptTime", LocalDateTime.now().minusMinutes(35));
        loginAttemptService.clearOldAttempts();

        assertFalse(loginAttemptService.isBlocked(oldUser), "Old user should be cleared from memory");
        assertTrue(loginAttemptService.isBlocked(recentUser), "Recent user should remain blocked");
    }
}