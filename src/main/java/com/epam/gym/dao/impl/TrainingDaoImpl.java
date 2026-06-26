package com.epam.gym.dao.impl;

import com.epam.gym.dao.TrainingDao;
import com.epam.gym.model.Training;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TrainingDaoImpl implements TrainingDao {

    private static final Logger log = LoggerFactory.getLogger(TrainingDaoImpl.class);

    private final Map<Long, Training> storage;
    private final AtomicLong idCounter = new AtomicLong(0);

    @Autowired
    public TrainingDaoImpl(@Qualifier("trainingStorage") Map<Long, Training> storage) {
        this.storage = storage;
    }

    @PostConstruct
    public void init() {
        syncIdCounter();
    }

    @Override
    public Training save(Training training) {
        Long id = idCounter.incrementAndGet();
        training.setId(id);
        storage.put(id, training);
        log.debug("Saved training with id={}", id);
        return training;
    }

    @Override
    public Optional<Training> findById(Long id) {
        log.debug("Finding training by id={}", id);
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Training> findAll() {
        log.debug("Finding all trainings, count={}", storage.size());
        return new ArrayList<>(storage.values());
    }

    public void syncIdCounter() {
        long max = storage.keySet().stream().mapToLong(Long::longValue).max().orElse(0);
        idCounter.set(max);
        log.debug("Synced training id counter to {}", max);
    }


}