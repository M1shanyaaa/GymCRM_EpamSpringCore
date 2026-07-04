package com.epam.gym.castomExeptions;

/**
 * Thrown when username/password authentication fails.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}