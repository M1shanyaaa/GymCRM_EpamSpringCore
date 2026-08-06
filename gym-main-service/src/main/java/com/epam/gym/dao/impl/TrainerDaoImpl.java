package com.epam.gym.dao.impl;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.model.Trainer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainerDaoImpl implements TrainerDao {

    private static final Logger log = LoggerFactory.getLogger(TrainerDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Trainer save(Trainer trainer) {
        entityManager.persist(trainer);
        log.debug("Persisted trainer with id={}", trainer.getId());
        return trainer;
    }

    @Override
    public Trainer update(Trainer trainer) {
        Trainer merged = entityManager.merge(trainer);
        log.debug("Merged trainer with id={}", merged.getId());
        return merged;
    }

    @Override
    public Optional<Trainer> findById(Long id) {
        Trainer trainer = entityManager.find(Trainer.class, id);
        log.debug("findById({}) -> found={}", id, trainer != null);
        return Optional.ofNullable(trainer);
    }

    @Override
    public Optional<Trainer> findByUsername(String username) {
        Optional<Trainer> trainer = entityManager
                .createQuery("FROM Trainer t WHERE t.user.username = :username", Trainer.class)
                .setParameter("username", username)
                .getResultList()
                .stream()
                .findFirst();
        log.debug("findByUsername({}) -> found={}", username, trainer.isPresent());
        return trainer;
    }

    @Override
    public List<Trainer> findByUsernames(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return List.of();
        }
        List<Trainer> result = entityManager
                .createQuery("SELECT t FROM Trainer t WHERE t.user.username IN (:usernames)", Trainer.class)
                .setParameter("usernames", usernames)
                .getResultList();
        log.debug("findByUsernames({}) -> count={}", usernames, result.size());
        return result;
    }

    @Override
    public List<Trainer> findUnassignedTrainers(String traineeUsername) {
        List<Trainer> result = entityManager
                .createQuery(
                        "SELECT t FROM Trainer t WHERE t.id NOT IN " +
                                "(SELECT tr.id FROM Trainee te JOIN te.trainers tr " +
                                " WHERE te.user.username = :username)", Trainer.class)
                .setParameter("username", traineeUsername)
                .getResultList();
        log.debug("findUnassignedTrainers({}) -> count={}", traineeUsername, result.size());
        return result;
    }
}