package com.epam.gym.service;

import com.epam.gym.dao.UserDao;
import com.epam.gym.dto.response.JwtResponse;
import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.model.User;
import com.epam.gym.security.CustomUserDetailsService;
import com.epam.gym.security.JwtProperties;
import com.epam.gym.security.JwtService;

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

    public AuthService(UserDao userDao, PasswordEncoder passwordEncoder, MeterRegistry meterRegistry,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       CustomUserDetailsService userDetailsService, JwtProperties jwtProperties) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.meterRegistry = meterRegistry;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Authenticates the user and generates a JWT token.
     */
    @Transactional(readOnly = true)
    public JwtResponse login(String username, String rawPassword) {
        try {
            // 1. Delegate authentication to Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, rawPassword)
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Authentication failed for user '{}'", username);
            meterRegistry.counter("gym.auth.login.total", "status", "failure").increment();

            // TODO (Phase 6): Add Brute Force tracking logic here

            // Throw custom exception to be caught by GlobalExceptionHandler
            throw new AuthenticationException("Invalid username or password");
        }

        // 2. Fetch UserDetails and generate token upon success
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