package com.epam.gym.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response for registration: generated credentials.
 */
@Schema(description = "Response containing the generated credentials and JWT token")
public record CredentialsResponse(
        @Schema(description = "Generated unique username")
        String username,

        @Schema(description = "Generated raw password (must be saved by the user)")
        String password,

        @Schema(description = "JWT Bearer token for immediate authentication")
        String token
) {
}