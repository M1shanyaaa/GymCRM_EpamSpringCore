package com.epam.gym.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Trainee entity. Extends {@link User}.
 * Identified by userId (used as a key in trainee storage).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class Trainee extends User {

    private Long userId;
    private LocalDate dateOfBirth;
    private String address;
}