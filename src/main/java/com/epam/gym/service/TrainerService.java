package com.epam.gym.service;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.model.Trainer;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Business logic for managing Trainer profiles.
 * Supports create / update / select.
 */
@Service
public class TrainerService {

    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private TrainerDao trainerDao;
    private UsernameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;

    public TrainerService(TrainerDao trainerDao, UsernameGenerator usernameGenerator, PasswordGenerator passwordGenerator) {
        this.trainerDao = trainerDao;
        this.usernameGenerator = usernameGenerator;
        this.passwordGenerator = passwordGenerator;
    }

    public Trainer create(Trainer trainer) {
        validateForCreate(trainer);

        String username = usernameGenerator.generate(trainer.getFirstName(), trainer.getLastName());
        String password = passwordGenerator.generate();

        trainer.setUsername(username);
        trainer.setPassword(password);

        Trainer saved = trainerDao.save(trainer);
        log.info("Created trainer profile: username='{}', id={}", saved.getUsername(), saved.getUserId());
        return saved;
    }

    public Trainer update(Trainer trainer) {
        if (trainer == null || trainer.getUserId() == null) {
            throw new IllegalArgumentException("Trainer and its id must not be null for update");
        }
        Trainer updated = trainerDao.update(trainer);
        log.info("Updated trainer profile: id={}", updated.getUserId());
        return updated;
    }

    public Trainer select(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Trainer id must not be null for select");
        }
        Optional<Trainer> trainer = trainerDao.findById(id);
        log.debug("Selected trainer by id={}, found={}", id, trainer.isPresent());
        return trainer.orElseThrow(() ->
                new NoSuchElementException("Trainer not found with id=" + id));
    }

    public List<Trainer> selectAll() {
        List<Trainer> trainers = trainerDao.findAll();
        log.debug("Selected all trainers, count={}", trainers.size());
        return trainers;
    }

    private void validateForCreate(Trainer trainer) {
        if (trainer == null) {
            throw new IllegalArgumentException("Trainer must not be null");
        }
        if (isBlank(trainer.getFirstName()) || isBlank(trainer.getLastName())) {
            throw new IllegalArgumentException("Trainer first name and last name are required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}