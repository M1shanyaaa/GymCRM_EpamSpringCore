package com.epam.gym.workload.exception;

public class WorkloadNotFoundException extends RuntimeException {
    public WorkloadNotFoundException(String message) {
        super(message);
    }
}