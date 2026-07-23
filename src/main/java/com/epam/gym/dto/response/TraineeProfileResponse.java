package com.epam.gym.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoint 5/6: Trainee profile response.
 */
public record TraineeProfileResponse(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String address,
        boolean isActive,
        List<TrainerShortResponse> trainers
) {
}