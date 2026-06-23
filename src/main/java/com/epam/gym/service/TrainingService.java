package com.epam.gym.service;

import com.epam.gym.dao.TrainingDao;
import com.epam.gym.model.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Business logic for managing Trainings.
 * Supports create / select.
 */
@Service
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private TrainingDao trainingDao;

    @Autowired
    public void setTrainingDao(TrainingDao trainingDao) {
        this.trainingDao = trainingDao;
    }

    public Training create(Training training) {
        validateForCreate(training);
        Training saved = trainingDao.save(training);
        log.info("Created training: name='{}', traineeId={}, trainerId={}",
                saved.getTrainingName(), saved.getTraineeId(), saved.getTrainerId());
        return saved;
    }

    public Training select(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Training id must not be null for select");
        }
        Optional<Training> training = trainingDao.findById(id);
        log.debug("Selected training by id={}, found={}", id, training.isPresent());
        return training.orElseThrow(() ->
                new NoSuchElementException("Training not found with id=" + id));
    }

    public List<Training> selectAll() {
        List<Training> trainings = trainingDao.findAll();
        log.debug("Selected all trainings, count={}", trainings.size());
        return trainings;
    }

    private void validateForCreate(Training training) {
        if (training == null) {
            throw new IllegalArgumentException("Training must not be null");
        }
        if (training.getTraineeId() == null || training.getTrainerId() == null) {
            throw new IllegalArgumentException("Training must reference both trainee and trainer");
        }
    }
}