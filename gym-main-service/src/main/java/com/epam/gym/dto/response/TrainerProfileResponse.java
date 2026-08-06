package com.epam.gym.dto.response;

import com.epam.gym.model.TrainingTypeName;

import java.util.List;

/**
 * Endpoint 8/9: Trainer profile response.
 */
public record TrainerProfileResponse(
        String firstName,
        String lastName,
        TrainingTypeName specialization,
        boolean isActive,
        List<TraineeShortResponse> trainees
) {
}