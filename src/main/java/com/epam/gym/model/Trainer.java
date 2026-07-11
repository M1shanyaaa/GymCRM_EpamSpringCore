package com.epam.gym.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Trainer entity. Extends {@link User}.
 * Identified by userId (used as a key in trainer storage).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class Trainer extends User {

    private Long userId;
    private TrainingType specialization;
}