package com.epam.gym;

import com.epam.gym.config.AppConfig;
import com.epam.gym.facade.GymFacade;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingTypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.TimeZone;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        var context = new AnnotationConfigApplicationContext(AppConfig.class);
        GymFacade facade = context.getBean(GymFacade.class);

        log.info("===== DEMO: working through GymFacade =====");

        // --- Create a trainee (username/password auto-generated) ---
        Trainee trainee = facade.createTrainee(
                "Peter", "Parker", LocalDate.of(2000, 3, 10), "New York");
        log.info("Created trainee: username='{}', id={}",
                trainee.getUser().getUsername(), trainee.getId());

        // --- Create a trainee with the SAME name (serial suffix demo) ---
        Trainee duplicate = facade.createTrainee(
                "Peter", "Parker", LocalDate.of(1998, 1, 1), "Queens");
        log.info("Created duplicate trainee: username='{}' (serial suffix)",
                duplicate.getUser().getUsername());

        // --- Create a trainer ---
        Trainer trainer = facade.createTrainer(
                "Bruce", "Wayne", TrainingTypeName.STRENGTH);
        log.info("Created trainer: username='{}', id={}",
                trainer.getUser().getUsername(), trainer.getId());

        log.info("===== DEMO finished — check the database =====");
        context.close();
    }
}