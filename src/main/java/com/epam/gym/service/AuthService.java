package com.epam.gym.service;

import com.epam.gym.dao.UserDao;
import com.epam.gym.dto.response.JwtResponse;
import com.epam.gym.exception.custom.AuthenticationException;
import com.epam.gym.exception.custom.EntityNotFoundException;
import com.epam.gym.exception.custom.UserLockedException;
import com.epam.gym.model.User;
import com.epam.gym.security.CustomUserDetailsService;
import com.epam.gym.security.JwtProperties;
import com.epam.gym.security.JwtService;
import com.epam.gym.security.LoginAttemptService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final MeterRegistry meterRegistry;

    // --- Spring Security specific beans ---
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final JwtProperties jwtProperties;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserDao userDao, PasswordEncoder passwordEncoder, MeterRegistry meterRegistry,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       CustomUserDetailsService userDetailsService, JwtProperties jwtProperties,
                       LoginAttemptService loginAttemptService) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.meterRegistry = meterRegistry;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.jwtProperties = jwtProperties;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Authenticates the user and generates a JWT token.
     * Includes brute-force protection to block users after multiple failed attempts.
     */
    @Transactional(readOnly = true)
    public JwtResponse login(String username, String rawPassword) {
        // 1. Check if the user is already blocked before attempting authentication
        if (loginAttemptService.isBlocked(username)) {
            log.warn("Login blocked for user '{}' due to multiple failed attempts", username);
            throw new UserLockedException("Account is temporarily locked due to too many failed attempts. Try again in 5 minutes.");
        }

        try {
            // 2. Delegate authentication to Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, rawPassword)
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            // 3. Record the failed attempt to the Brute Force tracker
            loginAttemptService.loginFailed(username);
            log.warn("Authentication failed for user '{}'", username);
            meterRegistry.counter("gym.auth.login.total", "status", "failure").increment();

            // Throw custom exception to be caught by GlobalExceptionHandler
            throw new AuthenticationException("Invalid username or password");
        }

        // 4. Reset the attempt counter on successful login
        loginAttemptService.loginSucceeded(username);

        // 5. Fetch UserDetails and generate token upon success
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String token = jwtService.generateToken(userDetails);

        meterRegistry.counter("gym.auth.login.total", "status", "success").increment();
        log.debug("User '{}' authenticated successfully, token generated", username);

        return new JwtResponse(token, jwtProperties.getExpiration());
    }

    /**
     * Changes the user's password after verifying the old one.
     */
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        // Verify current credentials using AuthenticationManager
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, oldPassword)
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new AuthenticationException("Invalid old password");
        }

        User user = userDao.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        user.setPassword(passwordEncoder.encode(newPassword));
        userDao.update(user);
        log.info("Password changed for user '{}'", username);
    }
}