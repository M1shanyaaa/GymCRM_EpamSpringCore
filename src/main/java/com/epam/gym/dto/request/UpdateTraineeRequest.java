package com.epam.gym.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Endpoint 6: Update trainee profile request.
 * Username is taken from the path/param and cannot be changed (Note 7).
 */
public record UpdateTraineeRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        LocalDate dateOfBirth,

        String address,

        @NotNull(message = "isActive is required")
        Boolean isActive
) {
}