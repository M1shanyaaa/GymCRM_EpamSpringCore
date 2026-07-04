package com.epam.gym.dao;

import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;

import java.util.List;
import java.util.Optional;

public interface TrainingTypeDao {
    Optional<TrainingType> findByName(TrainingTypeName name);
    List<TrainingType> findAll();
    TrainingType save(TrainingType trainingType);
}