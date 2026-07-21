package com.epam.gym.init;

import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * Seeds the TrainingType reference table on application startup.
 * Idempotent: inserts a type only if it doesn't already exist.
 */
@Component
public class TrainingTypeInitializer {

    private static final Logger log = LoggerFactory.getLogger(TrainingTypeInitializer.class);

    private final TrainingTypeDao trainingTypeDao;

    @Autowired
    public TrainingTypeInitializer(TrainingTypeDao trainingTypeDao) {
        this.trainingTypeDao = trainingTypeDao;
    }

    @Transactional
    public void seed() {
        Arrays.stream(TrainingTypeName.values()).forEach(name -> {
            if (trainingTypeDao.findByName(name).isEmpty()) {
                trainingTypeDao.save(new TrainingType(name));
                log.info("Seeded training type: {}", name);
            } else {
                log.debug("Training type already present: {}", name);
            }
        });
    }
}