package com.epam.gym.util;

import com.epam.gym.dao.UserDao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UsernameGenerator {

    private static final Logger log = LoggerFactory.getLogger(UsernameGenerator.class);
    private static final String SEPARATOR = ".";

    private final UserDao userDao;

    @Autowired
    public UsernameGenerator(UserDao userDao) {
        this.userDao = userDao;
    }

    public String generate(String firstName, String lastName) {
        String base = firstName + SEPARATOR + lastName;

        if (!userDao.existsByUsername(base)) {
            log.debug("Generated unique username: {}", base);
            return base;
        }

        int suffix = 1;
        String candidate = base + suffix;
        while (userDao.existsByUsername(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        log.debug("Generated unique username with suffix: {}", candidate);
        return candidate;
    }
}