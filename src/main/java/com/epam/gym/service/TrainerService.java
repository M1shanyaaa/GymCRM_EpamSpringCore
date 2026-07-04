package com.epam.gym.service;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.model.User;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TrainerService {

    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private final TrainerDao trainerDao;
    private final TrainingTypeDao trainingTypeDao;
    private final UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Autowired
    public TrainerService(TrainerDao trainerDao,
                          TrainingTypeDao trainingTypeDao,
                          UsernameGenerator usernameGenerator,
                          PasswordGenerator passwordGenerator,
                          PasswordEncoder passwordEncoder,
                          AuthService authService) {
        this.trainerDao = trainerDao;
        this.trainingTypeDao = trainingTypeDao;
        this.usernameGenerator = usernameGenerator;
        this.passwordGenerator = passwordGenerator;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    // ---------- Function 1: Create Trainer profile ----------
    @Transactional
    public Trainer create(String firstName, String lastName, TrainingTypeName specialization) {
        validateRequired(firstName, lastName, specialization);

        TrainingType type = trainingTypeDao.findByName(specialization)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Training type not found: " + specialization));

        String rawPassword = passwordGenerator.generate();

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .username(usernameGenerator.generate(firstName, lastName))
                .password(passwordEncoder.encode(rawPassword))
                .isActive(true)
                .build();

        Trainer trainer = Trainer.builder()
                .user(user)
                .specialization(type)
                .build();

        Trainer saved = trainerDao.save(trainer);
        log.info("Created trainer profile: username='{}', id={}",
                saved.getUser().getUsername(), saved.getId());
        return saved;
    }

    // ---------- Function 5: Select Trainer profile by username ----------
    @Transactional(readOnly = true)
    public Trainer findByUsername(String username, String password) {
        authService.authenticate(username, password);
        return getTrainerOrThrow(username);
    }

    // ---------- Function 8: Trainer password change ----------
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        authService.authenticate(username, oldPassword);

        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("New password must not be blank");
        }

        Trainer trainer = getTrainerOrThrow(username);
        trainer.getUser().setPassword(passwordEncoder.encode(newPassword));
        trainerDao.update(trainer);
        log.info("Password changed for trainer '{}'", username);
    }

    // ---------- Function 9: Update Trainer profile ----------
    @Transactional
    public Trainer update(String username, String password,
                          String firstName, String lastName,
                          TrainingTypeName specialization) {
        authService.authenticate(username, password);
        validateRequired(firstName, lastName, specialization);

        TrainingType type = trainingTypeDao.findByName(specialization)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Training type not found: " + specialization));

        Trainer trainer = getTrainerOrThrow(username);
        trainer.getUser().setFirstName(firstName);
        trainer.getUser().setLastName(lastName);
        trainer.setSpecialization(type);

        Trainer updated = trainerDao.update(trainer);
        log.info("Updated trainer profile '{}'", username);
        return updated;
    }

    // ---------- Function 12: Activate/De-activate trainer (NOT idempotent) ----------
    @Transactional
    public void toggleActive(String username, String password) {
        authService.authenticate(username, password);

        Trainer trainer = getTrainerOrThrow(username);
        boolean newStatus = !trainer.getUser().isActive();
        trainer.getUser().setActive(newStatus);
        trainerDao.update(trainer);
        log.info("Trainer '{}' active status changed to {}", username, newStatus);
    }

    // ---------- Function 17: Trainers not assigned to a trainee ----------
    @Transactional(readOnly = true)
    public List<Trainer> findUnassignedTrainers(String traineeUsername, String password) {
        authService.authenticate(traineeUsername, password);

        List<Trainer> result = trainerDao.findUnassignedTrainers(traineeUsername);
        log.debug("Found {} unassigned trainers for trainee '{}'", result.size(), traineeUsername);
        return result;
    }

    // ---------- helpers ----------
    private Trainer getTrainerOrThrow(String username) {
        return trainerDao.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainer not found: " + username));
    }

    private void validateRequired(String firstName, String lastName, TrainingTypeName specialization) {
        if (!StringUtils.hasText(firstName) || !StringUtils.hasText(lastName)) {
            throw new IllegalArgumentException("First name and last name are required");
        }
        if (specialization == null) {
            throw new IllegalArgumentException("Specialization is required");
        }
    }
}