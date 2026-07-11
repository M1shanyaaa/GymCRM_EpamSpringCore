package com.epam.gym;

import com.epam.gym.config.AppConfig;
import com.epam.gym.facade.GymFacade;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Duration;
import java.time.LocalDate;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        var context = new AnnotationConfigApplicationContext(AppConfig.class);
        GymFacade facade = context.getBean(GymFacade.class);

        log.info("===== DEMO: working through GymFacade =====");

        // --- Create a new trainee (username/password auto-generated) ---
        Trainee newTrainee = Trainee.builder()
                .firstName("Peter")
                .lastName("Parker")
                .isActive(true)
                .dateOfBirth(LocalDate.of(2000, 3, 10))
                .address("New York")
                .build();

        Trainee createdTrainee = facade.createTrainee(newTrainee);
        log.info("Created trainee: {}", createdTrainee);

        // --- Create a trainee with the SAME name (to test serial suffix) ---
        Trainee duplicate = Trainee.builder()
                .firstName("Peter")
                .lastName("Parker")
                .isActive(true)
                .dateOfBirth(LocalDate.of(1998, 1, 1))
                .address("Queens")
                .build();
        Trainee createdDuplicate = facade.createTrainee(duplicate);
        log.info("Created duplicate trainee: {}", createdDuplicate);

        // --- Create a trainer ---
        Trainer newTrainer = Trainer.builder()
                .firstName("Bruce")
                .lastName("Wayne")
                .isActive(true)
                .specialization(new TrainingType(TrainingTypeName.STRENGTH))
                .build();
        Trainer createdTrainer = facade.createTrainer(newTrainer);
        log.info("Created trainer: {}", createdTrainer);

        // --- Create a training ---
        Training newTraining = Training.builder()
                .traineeId(createdTrainee.getUserId())
                .trainerId(createdTrainer.getUserId())
                .trainingName("Strength Session")
                .trainingType(new TrainingType(TrainingTypeName.STRENGTH))
                .trainingDate(LocalDate.now())
                .trainingDuration(Duration.ofMinutes(45))
                .build();
        facade.createTraining(newTraining);

        // --- List everything ---
        log.info("===== Current state =====");
        log.info("All trainees ({}):", facade.getAllTrainees().size());
        facade.getAllTrainees().forEach(t -> log.info("  {}", t));

        log.info("All trainers ({}):", facade.getAllTrainers().size());
        facade.getAllTrainers().forEach(t -> log.info("  {}", t));

        log.info("All trainings ({}):", facade.getAllTrainings().size());
        facade.getAllTrainings().forEach(t -> log.info("  {}", t));

        context.close();
    }
}