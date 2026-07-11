package com.epam.gym.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Training entity. Links a Trainee and a Trainer for a specific session.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Training {

    private Long traineeId;
    private Long trainerId;
    private String trainingName;
    private TrainingType trainingType;
    private LocalDate trainingDate;
    private Duration trainingDuration;
}