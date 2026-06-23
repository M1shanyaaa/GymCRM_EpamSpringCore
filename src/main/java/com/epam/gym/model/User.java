package com.epam.gym.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Abstract base class for system users (Trainer, Trainee).
 */
@Data
@NoArgsConstructor
@SuperBuilder
public abstract class User {

    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private boolean isActive;

    /**
     * Custom toString that intentionally hides the password
     * to avoid leaking sensitive data into logs.
     */
    @Override
    public String toString() {
        return "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", username='" + username + '\'' +
                ", password='****'" +
                ", isActive=" + isActive;
    }
}