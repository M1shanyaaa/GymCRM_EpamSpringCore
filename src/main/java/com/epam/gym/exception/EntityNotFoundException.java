package com.epam.gym.exception;

/**
 * Thrown when a requested entity is not found.
 */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}