package com.epam.gym.dao;

import com.epam.gym.model.Trainee;

import java.util.Optional;

public interface TraineeDao {

    Trainee save(Trainee trainee);

    Trainee update(Trainee trainee);

    Optional<Trainee> findById(Long id);

    Optional<Trainee> findByUsername(String username);

    void delete(Trainee trainee);
}