package com.epam.gym.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a training type entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingType {

    private TrainingTypeName trainingTypeName;
}