package com.epam.gym.service;

import com.epam.gym.client.WorkloadClient;
import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.client.ActionType;
import com.epam.gym.dto.client.WorkloadRequest;
import com.epam.gym.dto.response.TrainerShortResponse;
import com.epam.gym.dto.response.TrainingResponse;
import com.epam.gym.dto.response.TrainingTypeResponse;
import com.epam.gym.exception.custom.EntityNotFoundException;
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Business logic for Training sessions and training types.
 * Security is handled globally by Spring Security via JWT filters.
 */
@Service
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private final TrainingDao trainingDao;
    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;
    private final TrainingTypeDao trainingTypeDao;
    private final TrainingMapper trainingMapper;
    private final TrainerMapper trainerMapper;
    private final WorkloadClient workloadClient;
    private final Timer searchTimer;

    @Autowired
    public TrainingService(TrainingDao trainingDao,
                           TraineeDao traineeDao,
                           TrainerDao trainerDao,
                           TrainingTypeDao trainingTypeDao,
                           TrainingMapper trainingMapper,
                           TrainerMapper trainerMapper,
                           WorkloadClient workloadClient,
                           MeterRegistry meterRegistry) {
        this.trainingDao = trainingDao;
        this.traineeDao = traineeDao;
        this.trainerDao = trainerDao;
        this.trainingTypeDao = trainingTypeDao;
        this.trainingMapper = trainingMapper;
        this.trainerMapper = trainerMapper;
        this.workloadClient = workloadClient;
        this.searchTimer = Timer.builder("gym.training.search.time")
                .description("Time taken to fetch trainings by criteria")
                .register(meterRegistry);
    }

    // ---------- Endpoint 14: Add training ----------
    @Transactional
    public void addTraining(String traineeUsername, String trainerUsername,
                            String trainingName, LocalDate trainingDate,
                            Integer trainingDuration) {

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

        // Send ADD event to workload microservice
        WorkloadRequest workloadRequest = WorkloadRequest.builder()
                .trainerUsername(trainer.getUser().getUsername())
                .trainerFirstName(trainer.getUser().getFirstName())
                .trainerLastName(trainer.getUser().getLastName())
                .isActive(trainer.getUser().isActive())
                .trainingDate(trainingDate)
                .trainingDuration(trainingDuration)
                .actionType(ActionType.ADD)
                .build();

        workloadClient.updateWorkload(workloadRequest);
        log.debug("Sent workload ADD request for trainer '{}'", trainerUsername);
    }

    // ---------- Endpoint 12: Trainee trainings by criteria ----------
    @Transactional(readOnly = true)
    public List<TrainingResponse> getTraineeTrainings(String username,
                                                      LocalDate fromDate, LocalDate toDate,
                                                      String trainerName, TrainingTypeName trainingType) {
        return searchTimer.record(() -> {
            List<Training> result = trainingDao.findTraineeTrainings(
                    username, fromDate, toDate, trainerName, trainingType);
            log.debug("Trainee '{}' trainings found: {}", username, result.size());
            return trainingMapper.toResponseList(result);
        });
    }

    // ---------- Endpoint 13: Trainer trainings by criteria ----------
    @Transactional(readOnly = true)
    public List<TrainingResponse> getTrainerTrainings(String username,
                                                      LocalDate fromDate, LocalDate toDate,
                                                      String traineeName) {
        return searchTimer.record(() -> {
            List<Training> result = trainingDao.findTrainerTrainings(
                    username, fromDate, toDate, traineeName);
            log.debug("Trainer '{}' trainings found: {}", username, result.size());
            return trainingMapper.toResponseList(result);
        });
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

    // ---------- Endpoint 17: Get training types ----------
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