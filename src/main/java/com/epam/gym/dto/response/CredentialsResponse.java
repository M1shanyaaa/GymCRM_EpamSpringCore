package com.epam.gym.dto.response;

/**
 * Response for registration (endpoints 1, 2): generated credentials.
 */
public record CredentialsResponse(
        String username,
        String password
) {
}