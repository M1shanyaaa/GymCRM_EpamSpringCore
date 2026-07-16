package com.epam.gym.service;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.response.CredentialsResponse;
import com.epam.gym.dto.response.TrainerProfileResponse;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.mapper.TrainerMapper;
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

/**
 * Business logic for Trainer entities.
 * <p>
 * Authentication for every method below except {@link #create} is enforced
 * globally by {@code AuthenticationInterceptor} before the request ever
 * reaches this service. This service therefore has no dependency on
 * {@link AuthService}.
 * <p>
 * Password changes are handled exclusively by {@link AuthService#changePassword}
 * (see {@code AuthController}) — do not duplicate that logic here.
 */
@Service
public class TrainerService {

    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private final TrainerDao trainerDao;
    private final TrainingTypeDao trainingTypeDao;
    private final UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TrainerMapper trainerMapper;

    @Autowired
    public TrainerService(TrainerDao trainerDao,
                          TrainingTypeDao trainingTypeDao,
                          UsernameGenerator usernameGenerator,
                          PasswordGenerator passwordGenerator,
                          PasswordEncoder passwordEncoder,
                          TrainerMapper trainerMapper) {
        this.trainerDao = trainerDao;
        this.trainingTypeDao = trainingTypeDao;
        this.usernameGenerator = usernameGenerator;
        this.passwordGenerator = passwordGenerator;
        this.passwordEncoder = passwordEncoder;
        this.trainerMapper = trainerMapper;
    }

    // ---------- Endpoint 2: Trainer registration (public, no auth) ----------
    @Transactional
    public CredentialsResponse create(String firstName, String lastName,
                                      TrainingTypeName specialization) {
        validateRequired(firstName, lastName);
        if (specialization == null) {
            throw new IllegalArgumentException("Specialization is required");
        }

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

        return new CredentialsResponse(saved.getUser().getUsername(), rawPassword);
    }

    // ---------- Endpoint 8: Get Trainer profile ----------
    @Transactional(readOnly = true)
    public TrainerProfileResponse getProfile(String username) {
        Trainer trainer = getTrainerOrThrow(username);
        return trainerMapper.toProfile(trainer);
    }

    // ---------- Endpoint 9: Update Trainer profile (specialization is read-only) ----------
    @Transactional
    public TrainerProfileResponse update(String username,
                                         String firstName, String lastName,
                                         boolean isActive) {
        validateRequired(firstName, lastName);

        Trainer trainer = getTrainerOrThrow(username);
        trainer.getUser().setFirstName(firstName);
        trainer.getUser().setLastName(lastName);
        trainer.getUser().setActive(isActive);
        // specialization intentionally NOT modified (read-only per spec)

        Trainer updated = trainerDao.update(trainer);
        log.info("Updated trainer profile '{}'", username);
        return trainerMapper.toProfile(updated);
    }

    // ---------- Endpoint 16: Activate/De-activate ----------
    @Transactional
    public void setActive(String username, boolean isActive) {
        Trainer trainer = getTrainerOrThrow(username);
        trainer.getUser().setActive(isActive);
        trainerDao.update(trainer);
        log.info("Trainer '{}' active status set to {}", username, isActive);
    }

    // ---------- Endpoint 10: Get not-assigned active trainers ----------
    @Transactional(readOnly = true)
    public List<TrainerShortResponse> findUnassignedTrainers(String traineeUsername) {
        List<Trainer> unassigned = trainerDao.findUnassignedTrainers(traineeUsername);
        return trainerMapper.toShortList(unassigned);
    }

    // ---------- helpers ----------
    private Trainer getTrainerOrThrow(String username) {
        return trainerDao.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainer not found: " + username));
    }

    private void validateRequired(String firstName, String lastName) {
        if (!StringUtils.hasText(firstName) || !StringUtils.hasText(lastName)) {
            throw new IllegalArgumentException("First name and last name are required");
        }
    }
}