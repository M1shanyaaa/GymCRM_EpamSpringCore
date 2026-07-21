package com.epam.gym.service;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dto.response.CredentialsResponse;
import com.epam.gym.dto.response.TraineeProfileResponse;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.mapper.TraineeMapper;
import com.epam.gym.model.Trainee;
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

import java.time.LocalDate;

/**
 * Business logic for Trainee entities.
 * <p>
 * Authentication for every method below except {@link #create} is enforced
 * globally by {@code AuthenticationInterceptor} before the request ever
 * reaches this service — by the time these methods run, the caller is
 * already guaranteed to be authenticated as exactly the {@code username}
 * passed in. This service therefore has no dependency on {@link AuthService}.
 * <p>
 * Password changes are handled exclusively by {@link AuthService#changePassword}
 * (see {@code AuthController}) — do not duplicate that logic here.
 */
@Service
public class TraineeService {

    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private final TraineeDao traineeDao;
    private final UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TraineeMapper traineeMapper;

    @Autowired
    public TraineeService(TraineeDao traineeDao,
                          UsernameGenerator usernameGenerator,
                          PasswordGenerator passwordGenerator,
                          PasswordEncoder passwordEncoder,
                          TraineeMapper traineeMapper) {
        this.traineeDao = traineeDao;
        this.usernameGenerator = usernameGenerator;
        this.passwordGenerator = passwordGenerator;
        this.passwordEncoder = passwordEncoder;
        this.traineeMapper = traineeMapper;
    }

    // ---------- Endpoint 1: Trainee registration (public, no auth) ----------
    @Transactional
    public CredentialsResponse create(String firstName, String lastName,
                                      LocalDate dateOfBirth, String address) {
        validateRequired(firstName, lastName);

        String rawPassword = passwordGenerator.generate();

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .username(usernameGenerator.generate(firstName, lastName))
                .password(passwordEncoder.encode(rawPassword))
                .isActive(true)
                .build();

        Trainee trainee = Trainee.builder()
                .user(user)
                .dateOfBirth(dateOfBirth)
                .address(address)
                .build();

        Trainee saved = traineeDao.save(trainee);
        log.info("Created trainee profile: username='{}', id={}",
                saved.getUser().getUsername(), saved.getId());

        // raw password is returned only here, never stored/logged
        return new CredentialsResponse(saved.getUser().getUsername(), rawPassword);
    }

    // ---------- Endpoint 5: Get Trainee profile ----------
    @Transactional(readOnly = true)
    public TraineeProfileResponse getProfile(String username) {
        Trainee trainee = getTraineeOrThrow(username);
        return traineeMapper.toProfile(trainee);
    }

    // ---------- Endpoint 6: Update Trainee profile ----------
    @Transactional
    public TraineeProfileResponse update(String username,
                                         String firstName, String lastName,
                                         LocalDate dateOfBirth, String address,
                                         boolean isActive) {
        validateRequired(firstName, lastName);

        Trainee trainee = getTraineeOrThrow(username);
        trainee.getUser().setFirstName(firstName);
        trainee.getUser().setLastName(lastName);
        trainee.getUser().setActive(isActive);
        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);

        Trainee updated = traineeDao.update(trainee);
        log.info("Updated trainee profile '{}'", username);
        return traineeMapper.toProfile(updated);
    }

    // ---------- Endpoint 15: Activate/De-activate (NOT idempotent per spec) ----------
    @Transactional
    public void setActive(String username, boolean isActive) {
        Trainee trainee = getTraineeOrThrow(username);
        trainee.getUser().setActive(isActive);
        traineeDao.update(trainee);
        log.info("Trainee '{}' active status set to {}", username, isActive);
    }

    // ---------- Endpoint 7: Delete Trainee profile (hard delete + cascade) ----------
    @Transactional
    public void delete(String username) {
        Trainee trainee = getTraineeOrThrow(username);
        traineeDao.delete(trainee);
        log.info("Deleted trainee profile '{}' (cascade: user + trainings)", username);
    }

    // ---------- helpers ----------
    private Trainee getTraineeOrThrow(String username) {
        return traineeDao.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainee not found: " + username));
    }

    private void validateRequired(String firstName, String lastName) {
        if (!StringUtils.hasText(firstName) || !StringUtils.hasText(lastName)) {
            throw new IllegalArgumentException("First name and last name are required");
        }
    }
}