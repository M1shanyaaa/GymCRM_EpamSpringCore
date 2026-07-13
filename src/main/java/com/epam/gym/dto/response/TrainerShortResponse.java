package com.epam.gym.dto.response;

import com.epam.gym.model.TrainingTypeName;

/**
 * Short trainer view used inside trainee profile / trainers lists.
 */
public record TrainerShortResponse(
        String username,
        String firstName,
        String lastName,
        TrainingTypeName specialization
) {
}