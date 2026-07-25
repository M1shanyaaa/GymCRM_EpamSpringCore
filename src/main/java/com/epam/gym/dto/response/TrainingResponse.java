package com.epam.gym.dto.response;

import com.epam.gym.model.TrainingTypeName;

import java.time.LocalDate;

/**
 * Endpoints 12/13: Training list item.
 * trainerName / traineeName is filled depending on the caller.
 */
public record TrainingResponse(
        String trainingName,
        LocalDate trainingDate,
        TrainingTypeName trainingType,
        Integer trainingDuration,
        String trainerName,
        String traineeName
) {
}