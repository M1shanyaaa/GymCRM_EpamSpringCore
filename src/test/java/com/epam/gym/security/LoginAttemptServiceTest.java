package com.epam.gym.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}