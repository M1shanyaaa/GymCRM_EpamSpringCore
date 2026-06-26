package com.epam.gym.service;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.model.Trainee;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Business logic for managing Trainee profiles.
 * Supports create / update / delete / select.
 */
@Service
public class TraineeService {

    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private TraineeDao traineeDao;
    private UsernameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;

    public TraineeService(PasswordEncoder passwordEncoder, TraineeDao traineeDao, UsernameGenerator usernameGenerator, PasswordGenerator passwordGenerator) {
        this.passwordEncoder = passwordEncoder;
        this.traineeDao = traineeDao;
        this.usernameGenerator = usernameGenerator;
        this.passwordGenerator = passwordGenerator;
    }

    /**
     * Creates a new trainee profile with generated username and password.
     */
    public Trainee create(Trainee trainee) {
        validateForCreate(trainee);

        String username = usernameGenerator.generate(trainee.getFirstName(), trainee.getLastName());
        String rawPassword = passwordGenerator.generate();

        trainee.setUsername(username);
        trainee.setPassword(passwordEncoder.encode(rawPassword));
        trainee.setActive(true);

        Trainee saved = traineeDao.save(trainee);
        log.info("Created trainee profile: username='{}', id={}", saved.getUsername(), saved.getUserId());
        return saved;
    }

    /**
     * Updates an existing trainee profile.
     */
    public Trainee update(Trainee trainee) {
        if (trainee == null || trainee.getUserId() == null) {
            throw new IllegalArgumentException("Trainee and its id must not be null for update");
        }
        Trainee updated = traineeDao.update(trainee);
        log.info("Updated trainee profile: id={}", updated.getUserId());
        return updated;
    }

    /**
     * Deletes a trainee profile by id.
     */
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Trainee id must not be null for delete");
        }
        boolean deleted = traineeDao.deleteById(id);
        if (deleted) {
            log.info("Deleted trainee profile: id={}", id);
        } else {
            log.warn("Delete requested for non-existent trainee: id={}", id);
        }
    }

    /**
     * Selects a trainee by id.
     */
    public Trainee select(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Trainee id must not be null for select");
        }
        Optional<Trainee> trainee = traineeDao.findById(id);
        log.debug("Selected trainee by id={}, found={}", id, trainee.isPresent());
        return trainee.orElseThrow(() ->
                new NoSuchElementException("Trainee not found with id=" + id));
    }

    /**
     * Returns all trainee profiles.
     */
    public List<Trainee> selectAll() {
        List<Trainee> trainees = traineeDao.findAll();
        log.debug("Selected all trainees, count={}", trainees.size());
        return trainees;
    }

    private void validateForCreate(Trainee trainee) {
        if (trainee == null) {
            throw new IllegalArgumentException("Trainee must not be null");
        }
        if (isBlank(trainee.getFirstName()) || isBlank(trainee.getLastName())) {
            throw new IllegalArgumentException("Trainee first name and last name are required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}