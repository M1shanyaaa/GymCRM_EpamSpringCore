package com.epam.gym;

import com.epam.gym.config.AppConfig;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        var context = new AnnotationConfigApplicationContext(AppConfig.class);

        Map<Long, Trainee> trainees = context.getBean("traineeStorage", Map.class);
        Map<Long, Trainer> trainers = context.getBean("trainerStorage", Map.class);
        Map<Long, Training> trainings = context.getBean("trainingStorage", Map.class);

        log.info("=== Storage initialized ===");
        log.info("Trainees: {}", trainees.size());
        trainees.values().forEach(t -> log.info("  {}", t));

        log.info("Trainers: {}", trainers.size());
        trainers.values().forEach(t -> log.info("  {}", t));

        log.info("Trainings: {}", trainings.size());
        trainings.values().forEach(t -> log.info("  {}", t));

        context.close();
    }
}