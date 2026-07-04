package com.epam.gym.dao;

import com.epam.gym.model.Trainer;

import java.util.List;
import java.util.Optional;

public interface TrainerDao {

    Trainer save(Trainer trainer);

    Trainer update(Trainer trainer);

    Optional<Trainer> findById(Long id);

    Optional<Trainer> findByUsername(String username);

    // trainers NOT assigned to given trainee
    List<Trainer> findUnassignedTrainers(String traineeUsername);
}