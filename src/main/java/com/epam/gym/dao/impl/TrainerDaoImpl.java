package com.epam.gym.dao.impl;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.model.Trainer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TrainerDaoImpl implements TrainerDao {

    private static final Logger log = LoggerFactory.getLogger(TrainerDaoImpl.class);

    private final Map<Long, Trainer> storage;
    private final AtomicLong idCounter = new AtomicLong(0);

    @Autowired
    public TrainerDaoImpl(@Qualifier("trainerStorage") Map<Long, Trainer> storage) {
        this.storage = storage;
    }

    @PostConstruct
    public void init() {
        syncIdCounter();
    }

    @Override
    public Trainer save(Trainer trainer) {
        Long id = idCounter.incrementAndGet();
        trainer.setUserId(id);
        storage.put(id, trainer);
        log.debug("Saved trainer with id={}", id);
        return trainer;
    }

    @Override
    public Optional<Trainer> findById(Long id) {
        log.debug("Finding trainer by id={}", id);
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Trainer> findAll() {
        log.debug("Finding all trainers, count={}", storage.size());
        return new ArrayList<>(storage.values());
    }

    @Override
    public Trainer update(Trainer trainer) {
        Long id = trainer.getUserId();
        if (id == null || !storage.containsKey(id)) {
            log.warn("Cannot update trainer: id={} not found", id);
            throw new NoSuchElementException("Trainer not found with id=" + id);
        }
        storage.put(id, trainer);
        log.debug("Updated trainer with id={}", id);
        return trainer;
    }

    /**
     * Used by storage initializer to sync the id counter
     * with pre-loaded data (so generated ids don't clash).
     */
    public void syncIdCounter() {
        long max = storage.keySet().stream().mapToLong(Long::longValue).max().orElse(0);
        idCounter.set(max);
        log.debug("Synced trainer id counter to {}", max);
    }
}
