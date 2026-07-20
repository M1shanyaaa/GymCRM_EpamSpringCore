package com.epam.gym.service;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.exception.EntityNotFoundException;
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

@Service
public class TraineeService {

    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private final TraineeDao traineeDao;
    private final UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Autowired
    public TraineeService(TraineeDao traineeDao,
                          UsernameGenerator usernameGenerator,
                          PasswordGenerator passwordGenerator,
                          PasswordEncoder passwordEncoder,
                          AuthService authService) {
        this.traineeDao = traineeDao;
        this.usernameGenerator = usernameGenerator;
        this.passwordGenerator = passwordGenerator;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    // ---------- Function 2: Create Trainee profile ----------
    @Transactional
    public Trainee create(String firstName, String lastName,
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
        return saved;
    }

    // ---------- Function 6: Select Trainee profile by username ----------
    @Transactional(readOnly = true)
    public Trainee findByUsername(String username, String password) {
        authService.authenticate(username, password);
        return getTraineeOrThrow(username);
    }

    // ---------- Function 7: Trainee password change ----------
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        authService.authenticate(username, oldPassword);

        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("New password must not be blank");
        }

        Trainee trainee = getTraineeOrThrow(username);
        trainee.getUser().setPassword(passwordEncoder.encode(newPassword));
        traineeDao.update(trainee);
        log.info("Password changed for trainee '{}'", username);
    }

    // ---------- Function 10: Update Trainee profile ----------
    @Transactional
    public Trainee update(String username, String password,
                          String firstName, String lastName,
                          LocalDate dateOfBirth, String address) {
        authService.authenticate(username, password);
        validateRequired(firstName, lastName);

        Trainee trainee = getTraineeOrThrow(username);
        trainee.getUser().setFirstName(firstName);
        trainee.getUser().setLastName(lastName);
        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);

        Trainee updated = traineeDao.update(trainee);
        log.info("Updated trainee profile '{}'", username);
        return updated;
    }

    // ---------- Function 11: Activate/De-activate trainee (NOT idempotent) ----------
    @Transactional
    public void toggleActive(String username, String password) {
        authService.authenticate(username, password);

        Trainee trainee = getTraineeOrThrow(username);
        boolean newStatus = !trainee.getUser().isActive();
        trainee.getUser().setActive(newStatus);
        traineeDao.update(trainee);
        log.info("Trainee '{}' active status changed to {}", username, newStatus);
    }

    // ---------- Function 13: Delete Trainee profile by username (hard delete + cascade) ----------
    @Transactional
    public void delete(String username, String password) {
        authService.authenticate(username, password);

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