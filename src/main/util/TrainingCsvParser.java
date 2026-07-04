package com.epam.gym.util;

import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Maps a CSV row into a {@link Training}.
 * Expected columns:
 * traineeId, trainerId, trainingName, trainingType, trainingDate, trainingDuration
 */
@Component
public class TrainingCsvParser {

    private static final Logger log = LoggerFactory.getLogger(TrainingCsvParser.class);

    public Training parse(String[] row) {
        try {
            TrainingType trainingType = new TrainingType(
                    TrainingTypeName.valueOf(row[3].trim().toUpperCase()));

            return Training.builder()
                    .traineeId(Long.parseLong(row[0].trim()))
                    .trainerId(Long.parseLong(row[1].trim()))
                    .trainingName(row[2].trim())
                    .trainingType(trainingType)
                    .trainingDate(LocalDate.parse(row[4].trim()))
                    .trainingDuration(Duration.parse(row[5].trim()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse training row: {}", String.join(",", row), e);
            return null;
        }
    }
}