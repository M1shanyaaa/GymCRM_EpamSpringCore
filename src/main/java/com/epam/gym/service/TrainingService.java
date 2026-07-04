package com.epam.gym.service;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingDao;
import com.epam.gym.exception.EntityNotFoundException;
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
import java.util.Set;

@Service
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private final TrainingDao trainingDao;
    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;
    private final AuthService authService;

    @Autowired
    public TrainingService(TrainingDao trainingDao,
                           TraineeDao traineeDao,
                           TrainerDao trainerDao,
                           AuthService authService) {
        this.trainingDao = trainingDao;
        this.traineeDao = traineeDao;
        this.trainerDao = trainerDao;
        this.authService = authService;
    }

    // ---------- Function 16: Add training ----------
    @Transactional
    public Training addTraining(String callerUsername, String callerPassword,
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

        Training saved = trainingDao.save(training);
        log.info("Added training '{}' (trainee='{}', trainer='{}', date={})",
                trainingName, traineeUsername, trainerUsername, trainingDate);
        return saved;
    }

    // ---------- Function 14: Trainee trainings by criteria ----------
    @Transactional(readOnly = true)
    public List<Training> getTraineeTrainings(String username, String password,
                                              LocalDate fromDate, LocalDate toDate,
                                              String trainerName, TrainingTypeName trainingType) {
        authService.authenticate(username, password);

        List<Training> result = trainingDao.findTraineeTrainings(
                username, fromDate, toDate, trainerName, trainingType);
        log.debug("Trainee '{}' trainings found: {}", username, result.size());
        return result;
    }

    // ---------- Function 15: Trainer trainings by criteria ----------
    @Transactional(readOnly = true)
    public List<Training> getTrainerTrainings(String username, String password,
                                              LocalDate fromDate, LocalDate toDate,
                                              String traineeName) {
        authService.authenticate(username, password);

        List<Training> result = trainingDao.findTrainerTrainings(
                username, fromDate, toDate, traineeName);
        log.debug("Trainer '{}' trainings found: {}", username, result.size());
        return result;
    }

    // ---------- Function 18: Update trainee's trainers list ----------
    @Transactional
    public Set<Trainer> updateTraineeTrainers(String traineeUsername, String password,
                                              List<String> trainerUsernames) {
        authService.authenticate(traineeUsername, password);

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
        return updated.getTrainers();
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