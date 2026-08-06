package com.epam.gym.exception.custom;

/**
 * Thrown when username/password authentication fails.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}