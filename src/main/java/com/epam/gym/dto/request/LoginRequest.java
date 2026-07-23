package com.epam.gym.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Endpoint 3: Login request.
 * Credentials are sent in the JSON body over POST, never in the URL/query
 * string or as plain headers, to avoid leaking them into access logs,
 * proxies, or browser history.
 */
public record LoginRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}