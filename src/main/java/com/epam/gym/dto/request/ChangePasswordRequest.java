package com.epam.gym.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Endpoint 4: Change login (password) request.
 * Username is taken from the URL path variable ({@code /api/users/{username}/password}),
 * so it is intentionally NOT duplicated as a field in this DTO.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Old password is required")
        String oldPassword,

        @NotBlank(message = "New password is required")
        String newPassword
) {
}