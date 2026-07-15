package com.epam.gym.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Endpoint 3: Login request.
 */
public record LoginRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}