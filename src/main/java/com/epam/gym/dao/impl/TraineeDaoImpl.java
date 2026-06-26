package com.epam.gym.dao.impl;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.model.Trainee;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TraineeDaoImpl implements TraineeDao {

    private static final Logger log = LoggerFactory.getLogger(TraineeDaoImpl.class);

    private final Map<Long, Trainee> storage;
    private final AtomicLong idCounter = new AtomicLong(0);

    @Autowired
    public TraineeDaoImpl(@Qualifier("traineeStorage") Map<Long, Trainee> storage) {
        this.storage = storage;
    }

    @PostConstruct
    public void init() {
        syncIdCounter();
    }

    @Override
    public Trainee save(Trainee trainee) {
        Long id = idCounter.incrementAndGet();
        trainee.setUserId(id);
        storage.put(id, trainee);
        log.debug("Saved trainee with id={}", id);
        return trainee;
    }

    @Override
    public Optional<Trainee> findById(Long id) {
        log.debug("Finding trainee by id={}", id);
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Trainee> findAll() {
        log.debug("Finding all trainees, count={}", storage.size());
        return new ArrayList<>(storage.values());
    }

    @Override
    public Trainee update(Trainee trainee) {
        Long id = trainee.getUserId();
        if (id == null || !storage.containsKey(id)) {
            log.warn("Cannot update trainee: id={} not found", id);
            throw new NoSuchElementException("Trainee not found with id=" + id);
        }
        storage.put(id, trainee);
        log.debug("Updated trainee with id={}", id);
        return trainee;
    }

    @Override
    public boolean deleteById(Long id) {
        Trainee removed = storage.remove(id);
        if (removed != null) {
            log.debug("Deleted trainee with id={}", id);
            return true;
        } else {
            log.warn("Cannot delete trainee: id={} not found", id);
            return false;
        }
    }

    /**
     * Used by storage initializer to sync the id counter
     * with pre-loaded data (so generated ids don't clash).
     */
    public void syncIdCounter() {
        long max = storage.keySet().stream().mapToLong(Long::longValue).max().orElse(0);
        idCounter.set(max);
        log.debug("Synced trainee id counter to {}", max);
    }
}