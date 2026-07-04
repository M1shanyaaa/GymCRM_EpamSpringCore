package com.epam.gym.service;

import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles username/password authentication.
 * Must be invoked before any protected operation.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Checks whether the given username/password pair matches an existing user.
     *
     * @return true if credentials are valid, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean matches(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            log.warn("Authentication attempt with null username or password");
            return false;
        }

        return userDao.findByUsername(username)
                .map(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElseGet(() -> {
                    log.warn("Authentication failed: user '{}' not found", username);
                    return false;
                });
    }

    /**
     * Authenticates the user and throws if credentials are invalid.
     * Convenience method for guarding protected operations.
     *
     * @throws AuthenticationException if credentials don't match
     */
    @Transactional(readOnly = true)
    public void authenticate(String username, String rawPassword) {
        if (!matches(username, rawPassword)) {
            log.warn("Authentication failed for user '{}'", username);
            throw new AuthenticationException("Invalid username or password");
        }
        log.debug("User '{}' authenticated successfully", username);
    }
}