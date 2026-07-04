package com.epam.gym.dao.impl;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.model.Trainer;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainerDaoImpl implements TrainerDao {

    private static final Logger log = LoggerFactory.getLogger(TrainerDaoImpl.class);

    private final SessionFactory sessionFactory;

    @Autowired
    public TrainerDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Trainer save(Trainer trainer) {
        sessionFactory.getCurrentSession().persist(trainer);
        log.debug("Persisted trainer with id={}", trainer.getId());
        return trainer;
    }

    @Override
    public Trainer update(Trainer trainer) {
        Trainer merged = sessionFactory.getCurrentSession().merge(trainer);
        log.debug("Merged trainer with id={}", merged.getId());
        return merged;
    }

    @Override
    public Optional<Trainer> findById(Long id) {
        Trainer trainer = sessionFactory.getCurrentSession().get(Trainer.class, id);
        log.debug("findById({}) -> found={}", id, trainer != null);
        return Optional.ofNullable(trainer);
    }

    @Override
    public Optional<Trainer> findByUsername(String username) {
        Trainer trainer = sessionFactory.getCurrentSession()
                .createQuery(
                        "FROM Trainer t WHERE t.user.username = :username", Trainer.class)
                .setParameter("username", username)
                .uniqueResult();
        log.debug("findByUsername({}) -> found={}", username, trainer != null);
        return Optional.ofNullable(trainer);
    }

    @Override
    public List<Trainer> findUnassignedTrainers(String traineeUsername) {
        // Trainers that are NOT in the trainee's trainers set
        List<Trainer> result = sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT t FROM Trainer t WHERE t.id NOT IN " +
                                "(SELECT tr.id FROM Trainee te JOIN te.trainers tr " +
                                " WHERE te.user.username = :username)", Trainer.class)
                .setParameter("username", traineeUsername)
                .list();
        log.debug("findUnassignedTrainers({}) -> count={}", traineeUsername, result.size());
        return result;
    }
}