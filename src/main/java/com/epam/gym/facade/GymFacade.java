package com.epam.gym.facade;

import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.service.TraineeService;
import com.epam.gym.service.TrainerService;
import com.epam.gym.service.TrainingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public Trainee createTrainee(Trainee trainee) {
        log.debug("Facade: createTrainee");
        return traineeService.create(trainee);
    }

    public Trainee updateTrainee(Trainee trainee) {
        log.debug("Facade: updateTrainee");
        return traineeService.update(trainee);
    }

    public void deleteTrainee(Long id) {
        log.debug("Facade: deleteTrainee id={}", id);
        traineeService.delete(id);
    }

    public Trainee getTrainee(Long id) {
        log.debug("Facade: getTrainee id={}", id);
        return traineeService.select(id);
    }

    public List<Trainee> getAllTrainees() {
        log.debug("Facade: getAllTrainees");
        return traineeService.selectAll();
    }

    // ===================== Trainer =====================

    public Trainer createTrainer(Trainer trainer) {
        log.debug("Facade: createTrainer");
        return trainerService.create(trainer);
    }

    public Trainer updateTrainer(Trainer trainer) {
        log.debug("Facade: updateTrainer");
        return trainerService.update(trainer);
    }

    public Trainer getTrainer(Long id) {
        log.debug("Facade: getTrainer id={}", id);
        return trainerService.select(id);
    }

    public List<Trainer> getAllTrainers() {
        log.debug("Facade: getAllTrainers");
        return trainerService.selectAll();
    }

    // ===================== Training =====================

    public Training createTraining(Training training) {
        log.debug("Facade: createTraining");
        return trainingService.create(training);
    }

    public Training getTraining(Long id) {
        log.debug("Facade: getTraining id={}", id);
        return trainingService.select(id);
    }

    public List<Training> getAllTrainings() {
        log.debug("Facade: getAllTrainings");
        return trainingService.selectAll();
    }
}