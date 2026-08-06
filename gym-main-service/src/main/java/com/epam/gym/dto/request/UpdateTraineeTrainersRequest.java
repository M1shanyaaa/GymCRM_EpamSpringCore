package com.epam.gym.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Endpoint 11: Update trainee's trainer list request.
 */
public record UpdateTraineeTrainersRequest(

        @NotEmpty(message = "Trainers list must not be empty")
        List<String> trainerUsernames
) {
}