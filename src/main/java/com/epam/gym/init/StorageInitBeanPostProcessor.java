package com.epam.gym.init;

import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.util.CsvReader;
import com.epam.gym.util.TraineeCsvParser;
import com.epam.gym.util.TrainerCsvParser;
import com.epam.gym.util.TrainingCsvParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Initializes in-memory storage Maps with data from CSV files at application startup.
 * Uses Spring bean post-processing: reacts to the storage Map beans by their bean names.
 * File paths are injected via property placeholders.
 */
@Component
public class StorageInitBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(StorageInitBeanPostProcessor.class);

    private static final String TRAINEE_STORAGE_BEAN = "traineeStorage";
    private static final String TRAINER_STORAGE_BEAN = "trainerStorage";
    private static final String TRAINING_STORAGE_BEAN = "trainingStorage";

    @Value("${storage.trainee.file}")
    private String traineeFile;

    @Value("${storage.trainer.file}")
    private String trainerFile;

    @Value("${storage.training.file}")
    private String trainingFile;

    private final CsvReader csvReader;
    private final TraineeCsvParser traineeParser;
    private final TrainerCsvParser trainerParser;
    private final TrainingCsvParser trainingParser;

    // Counter to assign keys to trainings (Training has no id field)
    private final AtomicLong trainingKeyCounter = new AtomicLong(0);

    @Autowired
    public StorageInitBeanPostProcessor(@Lazy CsvReader csvReader,
                                        @Lazy TraineeCsvParser traineeParser,
                                        @Lazy TrainerCsvParser trainerParser,
                                        @Lazy TrainingCsvParser trainingParser) {
        this.csvReader = csvReader;
        this.traineeParser = traineeParser;
        this.trainerParser = trainerParser;
        this.trainingParser = trainingParser;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        switch (beanName) {
            case TRAINEE_STORAGE_BEAN -> initTraineeStorage((Map<Long, Trainee>) bean);
            case TRAINER_STORAGE_BEAN -> initTrainerStorage((Map<Long, Trainer>) bean);
            case TRAINING_STORAGE_BEAN -> initTrainingStorage((Map<Long, Training>) bean);
            default -> { /* not a storage bean — ignore */ }
        }
        return bean;
    }

    private void initTraineeStorage(Map<Long, Trainee> storage) {
        log.info("Initializing trainee storage from {}", traineeFile);
        List<String[]> rows = csvReader.readAll(traineeFile);
        int loaded = 0;
        for (String[] row : rows) {
            Trainee trainee = traineeParser.parse(row);
            if (trainee != null && trainee.getUserId() != null) {
                storage.put(trainee.getUserId(), trainee);
                loaded++;
            }
        }
        log.info("Loaded {} trainees into storage", loaded);
    }

    private void initTrainerStorage(Map<Long, Trainer> storage) {
        log.info("Initializing trainer storage from {}", trainerFile);
        List<String[]> rows = csvReader.readAll(trainerFile);
        int loaded = 0;
        for (String[] row : rows) {
            Trainer trainer = trainerParser.parse(row);
            if (trainer != null && trainer.getUserId() != null) {
                storage.put(trainer.getUserId(), trainer);
                loaded++;
            }
        }
        log.info("Loaded {} trainers into storage", loaded);
    }

    private void initTrainingStorage(Map<Long, Training> storage) {
        log.info("Initializing training storage from {}", trainingFile);
        List<String[]> rows = csvReader.readAll(trainingFile);
        int loaded = 0;
        for (String[] row : rows) {
            Training training = trainingParser.parse(row);
            if (training != null) {
                storage.put(trainingKeyCounter.incrementAndGet(), training);
                loaded++;
            }
        }
        log.info("Loaded {} trainings into storage", loaded);
    }
}