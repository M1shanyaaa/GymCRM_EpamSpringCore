package com.epam.gym.facade;

import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.service.TraineeService;
import com.epam.gym.service.TrainerService;
import com.epam.gym.service.TrainingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Facade providing a single entry point to the Gym CRM subsystem.
 * Aggregates Trainee, Trainer and Training services.
 *
 * Per requirements, services are injected via constructor-based injection.
 */
@Component
public class GymFacade {

    private static final Logger log = LoggerFactory.getLogger(GymFacade.class);

    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;

    @Autowired
    public GymFacade(TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    // ===================== Trainee =====================

    public Trainee createTrainee(String firstName, String lastName,
                                 LocalDate dateOfBirth, String address) {
        log.debug("Facade: createTrainee");
        return traineeService.create(firstName, lastName, dateOfBirth, address);
    }

    public Trainee getTrainee(String username, String password) {
        log.debug("Facade: getTrainee username={}", username);
        return traineeService.findByUsername(username, password);
    }

    public Trainee updateTrainee(String username, String password,
                                 String firstName, String lastName,
                                 LocalDate dateOfBirth, String address) {
        log.debug("Facade: updateTrainee username={}", username);
        return traineeService.update(username, password, firstName, lastName, dateOfBirth, address);
    }

    public void changeTraineePassword(String username, String oldPassword, String newPassword) {
        log.debug("Facade: changeTraineePassword username={}", username);
        traineeService.changePassword(username, oldPassword, newPassword);
    }

    public void toggleTraineeActive(String username, String password) {
        log.debug("Facade: toggleTraineeActive username={}", username);
        traineeService.toggleActive(username, password);
    }

    public void deleteTrainee(String username, String password) {
        log.debug("Facade: deleteTrainee username={}", username);
        traineeService.delete(username, password);
    }

    // ===================== Trainer =====================

    public Trainer createTrainer(String firstName, String lastName,
                                 TrainingTypeName specialization) {
        log.debug("Facade: createTrainer");
        return trainerService.create(firstName, lastName, specialization);
    }

    public Trainer getTrainer(String username, String password) {
        log.debug("Facade: getTrainer username={}", username);
        return trainerService.findByUsername(username, password);
    }

    public Trainer updateTrainer(String username, String password,
                                 String firstName, String lastName,
                                 TrainingTypeName specialization) {
        log.debug("Facade: updateTrainer username={}", username);
        return trainerService.update(username, password, firstName, lastName, specialization);
    }

    public void changeTrainerPassword(String username, String oldPassword, String newPassword) {
        log.debug("Facade: changeTrainerPassword username={}", username);
        trainerService.changePassword(username, oldPassword, newPassword);
    }

    public void toggleTrainerActive(String username, String password) {
        log.debug("Facade: toggleTrainerActive username={}", username);
        trainerService.toggleActive(username, password);
    }

    public List<Trainer> getUnassignedTrainers(String traineeUsername, String password) {
        log.debug("Facade: getUnassignedTrainers traineeUsername={}", traineeUsername);
        return trainerService.findUnassignedTrainers(traineeUsername, password);
    }

    // ===================== Training =====================

    public Training addTraining(String callerUsername, String callerPassword,
                                String traineeUsername, String trainerUsername,
                                String trainingName, LocalDate trainingDate,
                                Integer trainingDuration) {
        log.debug("Facade: addTraining name={}", trainingName);
        return trainingService.addTraining(callerUsername, callerPassword,
                traineeUsername, trainerUsername, trainingName, trainingDate, trainingDuration);
    }

    public List<Training> getTraineeTrainings(String username, String password,
                                              LocalDate fromDate, LocalDate toDate,
                                              String trainerName, TrainingTypeName trainingType) {
        log.debug("Facade: getTraineeTrainings username={}", username);
        return trainingService.getTraineeTrainings(username, password, fromDate, toDate, trainerName, trainingType);
    }

    public List<Training> getTrainerTrainings(String username, String password,
                                              LocalDate fromDate, LocalDate toDate,
                                              String traineeName) {
        log.debug("Facade: getTrainerTrainings username={}", username);
        return trainingService.getTrainerTrainings(username, password, fromDate, toDate, traineeName);
    }

    public Set<Trainer> updateTraineeTrainers(String traineeUsername, String password,
                                              List<String> trainerUsernames) {
        log.debug("Facade: updateTraineeTrainers traineeUsername={}", traineeUsername);
        return trainingService.updateTraineeTrainers(traineeUsername, password, trainerUsernames);
    }
}