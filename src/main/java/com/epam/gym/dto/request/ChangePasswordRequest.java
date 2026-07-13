package com.epam.gym.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Endpoint 4: Change login (password) request.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Old password is required")
        String oldPassword,

        @NotBlank(message = "New password is required")
        String newPassword
) {
}