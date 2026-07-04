package com.epam.gym.exception;

/**
 * Thrown when username/password authentication fails.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}