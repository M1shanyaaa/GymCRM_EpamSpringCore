package com.epam.gym.dao;

import com.epam.gym.model.Trainee;

import java.util.List;
import java.util.Optional;

public interface TraineeDao {
    Trainee save(Trainee trainee);
    Optional<Trainee> findById(Long id);
    List<Trainee> findAll();
    Trainee update(Trainee trainee);
    void deleteById(Long id);
}