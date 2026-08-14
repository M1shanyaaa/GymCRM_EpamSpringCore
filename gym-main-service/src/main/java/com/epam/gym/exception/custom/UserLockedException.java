package com.epam.gym.exception.custom;

public class UserLockedException extends RuntimeException {
    public UserLockedException(String message) {
        super(message);
    }
}