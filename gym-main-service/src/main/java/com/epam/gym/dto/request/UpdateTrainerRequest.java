package com.epam.gym.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Endpoint 9: Update trainer profile request.
 * Specialization is read-only; username cannot be changed.
 */
public record UpdateTrainerRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotNull(message = "isActive is required")
        Boolean isActive
) {
}