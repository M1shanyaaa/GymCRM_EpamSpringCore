package com.epam.gym.service;

import com.epam.gym.client.WorkloadClient;
import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dto.client.ActionType;
import com.epam.gym.dto.client.WorkloadRequest;
import com.epam.gym.dto.response.CredentialsResponse;
import com.epam.gym.dto.response.TraineeProfileResponse;
import com.epam.gym.exception.custom.EntityNotFoundException;
import com.epam.gym.mapper.TraineeMapper;
import com.epam.gym.model.Role;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.model.User;
import com.epam.gym.security.CustomUserDetails;
import com.epam.gym.security.JwtService;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Business logic for Trainee entities.
 * <p>
 * Authentication for every method below except {@link #create} is enforced
 * globally by Spring Security (JWT filters) before the request ever
 * reaches this service. This service therefore has no dependency on AuthService.
 * <p>
 * Password changes are handled exclusively by AuthService (see AuthController)
 * — do not duplicate that logic here.
 */
@Service
public class TraineeService {

    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private final TraineeDao traineeDao;
    private final UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TraineeMapper traineeMapper;
    private final JwtService jwtService;
    private final WorkloadClient workloadClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Counter registrationCounter;

    @Autowired
    public TraineeService(TraineeDao traineeDao,
                          UsernameGenerator usernameGenerator,
                          PasswordGenerator passwordGenerator,
                          PasswordEncoder passwordEncoder,
                          TraineeMapper traineeMapper,
                          JwtService jwtService,
                          WorkloadClient workloadClient,
                          CircuitBreakerRegistry circuitBreakerRegistry,
                          MeterRegistry meterRegistry) {
        this.traineeDao = traineeDao;
        this.usernameGenerator = usernameGenerator;
        this.passwordGenerator = passwordGenerator;
        this.passwordEncoder = passwordEncoder;
        this.traineeMapper = traineeMapper;
        this.jwtService = jwtService;
        this.workloadClient = workloadClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.registrationCounter = Counter.builder("gym.trainee.registrations.total")
                .description("Total number of registered trainees")
                .register(meterRegistry);
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
                .role(Role.TRAINEE)
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

        registrationCounter.increment();

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + saved.getUser().getRole().name());
        UserDetails userDetails = new CustomUserDetails(saved.getUser(), Collections.singletonList(authority));
        String token = jwtService.generateToken(userDetails);

        return new CredentialsResponse(saved.getUser().getUsername(), rawPassword, token);
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

    // ---------- Endpoint 15: Activate/De-activate ----------
    @Transactional
    public void setActive(String username, boolean isActive) {
        Trainee trainee = getTraineeOrThrow(username);
        trainee.getUser().setActive(isActive);
        traineeDao.update(trainee);
        log.info("Trainee '{}' active status set to {}", username, isActive);
    }

    // ---------- Endpoint 7: Delete Trainee profile ----------
    @Transactional
    public void delete(String username) {
        Trainee trainee = getTraineeOrThrow(username);

        // Collect all trainings before deleting the trainee to adjust trainer workloads
        List<Training> traineeTrainings = new ArrayList<>(trainee.getTrainings());

        traineeDao.delete(trainee);
        log.info("Deleted trainee profile '{}' (cascade: user + trainings)", username);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("workloadService");

        // Send DELETE events to the workload microservice for each associated training
        for (Training training : traineeTrainings) {
            Trainer trainer = training.getTrainer();
            WorkloadRequest workloadRequest = WorkloadRequest.builder()
                    .trainerUsername(trainer.getUser().getUsername())
                    .trainerFirstName(trainer.getUser().getFirstName())
                    .trainerLastName(trainer.getUser().getLastName())
                    .isActive(trainer.getUser().isActive())
                    .trainingDate(training.getTrainingDate())
                    .trainingDuration(training.getTrainingDuration())
                    .actionType(ActionType.DELETE)
                    .build();

            // FIX
            try {
                circuitBreaker.executeRunnable(() -> workloadClient.updateWorkload(workloadRequest));
                log.debug("Sent workload DELETE request for trainer '{}'", trainer.getUser().getUsername());
            } catch (Exception ex) {
                log.warn("FALLBACK EXECUTED: Could not update workload for trainer '{}'. Reason: {}. Trainee is deleted in monolith, but workload sync failed.",
                        trainer.getUser().getUsername(), ex.getMessage());
            }
        }

        log.debug("Sent {} workload DELETE requests for trainee '{}'", traineeTrainings.size(), username);
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