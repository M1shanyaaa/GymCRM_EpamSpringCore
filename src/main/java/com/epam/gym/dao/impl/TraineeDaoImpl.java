package com.epam.gym.dao.impl;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.model.Trainee;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TraineeDaoImpl implements TraineeDao {

    private static final Logger log = LoggerFactory.getLogger(TraineeDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Trainee save(Trainee trainee) {
        entityManager.unwrap(Session.class).persist(trainee);
        log.debug("Persisted trainee with id={}", trainee.getId());
        return trainee;
    }

    @Override
    public Trainee update(Trainee trainee) {
        Trainee merged = entityManager.unwrap(Session.class).merge(trainee);
        log.debug("Merged trainee with id={}", merged.getId());
        return merged;
    }

    @Override
    public Optional<Trainee> findById(Long id) {
        Trainee trainee = entityManager.unwrap(Session.class).get(Trainee.class, id);
        log.debug("findById({}) -> found={}", id, trainee != null);
        return Optional.ofNullable(trainee);
    }

    @Override
    public Optional<Trainee> findByUsername(String username) {
        Trainee trainee = entityManager.unwrap(Session.class)
                .createQuery(
                        "FROM Trainee t WHERE t.user.username = :username", Trainee.class)
                .setParameter("username", username)
                .uniqueResult();
        log.debug("findByUsername({}) -> found={}", username, trainee != null);
        return Optional.ofNullable(trainee);
    }

    @Override
    public void delete(Trainee trainee) {
        entityManager.unwrap(Session.class).remove(trainee);
        log.debug("Removed trainee with id={}", trainee.getId());
    }
}