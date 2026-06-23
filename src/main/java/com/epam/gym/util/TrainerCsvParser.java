package com.epam.gym.util;

import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Maps a CSV row into a {@link Trainer}.
 * Expected columns:
 * userId, firstName, lastName, username, password, isActive, specialization
 */
@Component
public class TrainerCsvParser {

    private static final Logger log = LoggerFactory.getLogger(TrainerCsvParser.class);

    public Trainer parse(String[] row) {
        try {
            TrainingType specialization = new TrainingType(
                    TrainingTypeName.valueOf(row[6].trim().toUpperCase()));

            return Trainer.builder()
                    .userId(Long.parseLong(row[0].trim()))
                    .firstName(row[1].trim())
                    .lastName(row[2].trim())
                    .username(row[3].trim())
                    .password(row[4].trim())
                    .isActive(Boolean.parseBoolean(row[5].trim()))
                    .specialization(specialization)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse trainer row: {}", String.join(",", row), e);
            return null;
        }
    }
}