package com.epam.gym.service;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.dto.response.TrainingResponse;
import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.exception.EntityNotFoundException;
import com.epam.gym.mapper.TrainerMapper;
import com.epam.gym.mapper.TrainingMapper;
import com.epam.gym.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

/**
 * Business logic for Training sessions and training types.
 * <p>
 * All methods except {@link #addTraining} and {@link #getTrainingTypes} are
 * reached only via routes authenticated globally by
 * {@code AuthenticationInterceptor}, so they no longer need to call
 * {@link AuthService} themselves.
 * <p>
 * {@link #addTraining} is the one remaining exception: its controller
 * endpoint is still marked {@code @NoAuth} because it authenticates using
 * credentials from the request body instead of {@code X-Auth-*} headers
 * (legacy behavior, tracked as a TODO). It therefore still performs its own
 * explicit authentication call below. Once that endpoint is migrated to
 * header-based auth, remove {@code authService} usage here too.
 */
@Service
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private final TrainingDao trainingDao;
    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;
    private final TrainingTypeDao trainingTypeDao;
    private final AuthService authService;
    private final TrainingMapper trainingMapper;
    private final TrainerMapper trainerMapper;

    @Autowired
    public TrainingService(TrainingDao trainingDao,
                           TraineeDao traineeDao,
                           TrainerDao trainerDao,
                           TrainingTypeDao trainingTypeDao,
                           AuthService authService,
                           TrainingMapper trainingMapper,
                           TrainerMapper trainerMapper) {
        this.trainingDao = trainingDao;
        this.traineeDao = traineeDao;
        this.trainerDao = trainerDao;
        this.trainingTypeDao = trainingTypeDao;
        this.authService = authService;
        this.trainingMapper = trainingMapper;
        this.trainerMapper = trainerMapper;
    }

    // ---------- Endpoint 14: Add training ----------
    // NOTE: controller endpoint is @NoAuth (credentials come from the request
    // body, not headers — see TrainingController TODO), so this is the only
    // remaining place in this service that must authenticate explicitly.
    @Transactional
    public void addTraining(String callerUsername, String callerPassword,
                            String traineeUsername, String trainerUsername,
                            String trainingName, LocalDate trainingDate,
                            Integer trainingDuration) {
        authService.authenticate(callerUsername, callerPassword);
        validateTrainingFields(trainingName, trainingDate, trainingDuration);

        Trainee trainee = traineeDao.findByUsername(traineeUsername)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainee not found: " + traineeUsername));
        Trainer trainer = trainerDao.findByUsername(trainerUsername)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainer not found: " + trainerUsername));

        Training training = Training.builder()
                .trainee(trainee)
                .trainer(trainer)
                .trainingName(trainingName)
                .trainingType(trainer.getSpecialization())
                .trainingDate(trainingDate)
                .trainingDuration(trainingDuration)
                .build();

        // Ensure the trainer is linked to the trainee (M:M)
        trainee.getTrainers().add(trainer);
        traineeDao.update(trainee);

        trainingDao.save(training);
        log.info("Added training '{}' (trainee='{}', trainer='{}', date={})",
                trainingName, traineeUsername, trainerUsername, trainingDate);
    }

    // ---------- Endpoint 12: Trainee trainings by criteria ----------
    @Transactional(readOnly = true)
    public List<TrainingResponse> getTraineeTrainings(String username,
                                                      LocalDate fromDate, LocalDate toDate,
                                                      String trainerName, TrainingTypeName trainingType) {
        List<Training> result = trainingDao.findTraineeTrainings(
                username, fromDate, toDate, trainerName, trainingType);
        log.debug("Trainee '{}' trainings found: {}", username, result.size());
        return trainingMapper.toResponseList(result);
    }

    // ---------- Endpoint 13: Trainer trainings by criteria ----------
    @Transactional(readOnly = true)
    public List<TrainingResponse> getTrainerTrainings(String username,
                                                      LocalDate fromDate, LocalDate toDate,
                                                      String traineeName) {
        List<Training> result = trainingDao.findTrainerTrainings(
                username, fromDate, toDate, traineeName);
        log.debug("Trainer '{}' trainings found: {}", username, result.size());
        return trainingMapper.toResponseList(result);
    }

    // ---------- Endpoint 11: Update trainee's trainers list ----------
    @Transactional
    public List<TrainerShortResponse> updateTraineeTrainers(String traineeUsername,
                                                            List<String> trainerUsernames) {
        if (trainerUsernames == null || trainerUsernames.isEmpty()) {
            throw new IllegalArgumentException("Trainers list must not be empty");
        }

        Trainee trainee = traineeDao.findByUsername(traineeUsername)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainee not found: " + traineeUsername));

        List<Trainer> trainers = trainerDao.findByUsernames(trainerUsernames);
        if (trainers.size() != trainerUsernames.size()) {
            throw new EntityNotFoundException("One or more trainers not found");
        }

        trainee.setTrainers(new HashSet<>(trainers));
        Trainee updated = traineeDao.update(trainee);
        log.info("Updated trainers list for trainee '{}' ({} trainers)",
                traineeUsername, trainers.size());
        return trainerMapper.toShortList(List.copyOf(updated.getTrainers()));
    }

    // ---------- Endpoint 17: Get training types (public, no auth) ----------
    @Transactional(readOnly = true)
    public List<TrainingTypeResponse> getTrainingTypes() {
        List<TrainingType> types = trainingTypeDao.findAll();
        log.debug("Training types found: {}", types.size());
        return trainingMapper.toTypeResponseList(types);
    }

    // ---------- validation ----------
    private void validateTrainingFields(String name, LocalDate date, Integer duration) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Training name is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("Training date is required");
        }
        if (duration == null || duration <= 0) {
            throw new IllegalArgumentException("Training duration must be a positive number");
        }
    }
}