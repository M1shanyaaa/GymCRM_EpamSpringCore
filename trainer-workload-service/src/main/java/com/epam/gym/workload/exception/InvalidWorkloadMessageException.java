package com.epam.gym.workload.exception;

/**
 * Thrown when a workload message is missing required information.
 * Such messages must NOT be retried — they go straight to the DLQ.
 */
public class InvalidWorkloadMessageException extends RuntimeException {
    public InvalidWorkloadMessageException(String message) {
        super(message);
    }
}