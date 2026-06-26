package com.epam.gym.util;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates unique usernames based on first and last name.
 * Rules:
 *  - base username = firstName.lastName
 *  - if it already exists (among trainees AND trainers),
 *    append the smallest serial number that makes it unique
 *    (e.g. John.Smith, John.Smith1, John.Smith2, ...)
 */
@Component
public class UsernameGenerator {

    private static final Logger log = LoggerFactory.getLogger(UsernameGenerator.class);

    private static final String SEPARATOR = ".";

    private TraineeDao traineeDao;
    private TrainerDao trainerDao;

    @Autowired
    public UsernameGenerator(TraineeDao traineeDao, TrainerDao trainerDao) {
        this.traineeDao = traineeDao;
        this.trainerDao = trainerDao;
    }

    /**
     * Generates a unique username for the given first/last name.
     */
    public String generate(String firstName, String lastName) {
        String baseUsername = firstName + SEPARATOR + lastName;
        Set<String> existingUsernames = collectExistingUsernames();

        if (!existingUsernames.contains(baseUsername)) {
            log.debug("Generated username '{}'", baseUsername);
            return baseUsername;
        }

        int serial = 1;
        String candidate = baseUsername + serial;
        while (existingUsernames.contains(candidate)) {
            serial++;
            candidate = baseUsername + serial;
        }

        log.debug("Username '{}' already exists, generated '{}'", baseUsername, candidate);
        return candidate;
    }

    /**
     * Collects all usernames currently used by trainees and trainers.
     */
    private Set<String> collectExistingUsernames() {
        Set<String> usernames = new HashSet<>();

        usernames.addAll(traineeDao.findAll().stream()
                .map(User::getUsername)
                .collect(Collectors.toSet()));

        usernames.addAll(trainerDao.findAll().stream()
                .map(User::getUsername)
                .collect(Collectors.toSet()));

        return usernames;
    }
}