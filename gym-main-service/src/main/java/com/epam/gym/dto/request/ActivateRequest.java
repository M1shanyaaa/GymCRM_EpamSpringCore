package com.epam.gym.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Endpoints 15/16: Activate/deactivate trainee/trainer request.
 */
public record ActivateRequest(

        @NotNull(message = "isActive is required")
        Boolean isActive
) {
}