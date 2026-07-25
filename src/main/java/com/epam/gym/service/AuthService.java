package com.epam.gym.service;

import com.epam.gym.dao.UserDao;
import com.epam.gym.exception.AuthenticationException;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public void authenticate(String username, String rawPassword) {
        if (!matches(username, rawPassword)) {
            log.warn("Authentication failed for user '{}'", username);
            throw new AuthenticationException("Invalid username or password");
        }
        log.debug("User '{}' authenticated successfully", username);
    }

    /**
     * Changes the user's password after verifying the old one.
     *
     * @throws AuthenticationException if old credentials are invalid
     * @throws EntityNotFoundException if the user does not exist
     */
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        authenticate(username, oldPassword);  // verify current credentials

        User user = userDao.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found: " + username));

        user.setPassword(passwordEncoder.encode(newPassword));
        userDao.update(user);
        log.info("Password changed for user '{}'", username);
    }
}