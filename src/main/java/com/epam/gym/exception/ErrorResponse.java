package com.epam.gym.exception;

import java.time.Instant;

public record ErrorResponse(

        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String transactionId
) {
    public static ErrorResponse of(int status, String error, String message,
                                   String path, String transactionId) {
        return new ErrorResponse(Instant.now(), status, error, message, path, transactionId);
    }
}