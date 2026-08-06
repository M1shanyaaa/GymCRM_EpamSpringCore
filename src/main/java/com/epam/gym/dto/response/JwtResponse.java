package com.epam.gym.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing the JWT token")
public record JwtResponse(
        @Schema(description = "JWT Bearer token")
        String token,

        @Schema(description = "Expiration time in milliseconds")
        long expiresIn
) {
}