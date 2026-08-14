package com.epam.gym.dto.response;

import com.epam.gym.model.TrainingTypeName;

/**
 * Endpoint 17: Training type item.
 */
public record TrainingTypeResponse(
        TrainingTypeName trainingType,
        Long trainingTypeId
) {
}