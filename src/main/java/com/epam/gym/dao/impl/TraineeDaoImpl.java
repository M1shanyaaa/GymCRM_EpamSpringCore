package com.epam.gym.dao.impl;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.model.Trainee;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TraineeDaoImpl implements TraineeDao {

    private static final Logger log = LoggerFactory.getLogger(TraineeDaoImpl.class);

    private final SessionFactory sessionFactory;

    @Autowired
    public TraineeDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Trainee save(Trainee trainee) {
        sessionFactory.getCurrentSession().persist(trainee);
        log.debug("Persisted trainee with id={}", trainee.getId());
        return trainee;
    }

    @Override
    public Trainee update(Trainee trainee) {
        Trainee merged = sessionFactory.getCurrentSession().merge(trainee);
        log.debug("Merged trainee with id={}", merged.getId());
        return merged;
    }

    @Override
    public Optional<Trainee> findById(Long id) {
        Trainee trainee = sessionFactory.getCurrentSession().get(Trainee.class, id);
        log.debug("findById({}) -> found={}", id, trainee != null);
        return Optional.ofNullable(trainee);
    }

    @Override
    public Optional<Trainee> findByUsername(String username) {
        Trainee trainee = sessionFactory.getCurrentSession()
                .createQuery(
                        "FROM Trainee t WHERE t.user.username = :username", Trainee.class)
                .setParameter("username", username)
                .uniqueResult();
        log.debug("findByUsername({}) -> found={}", username, trainee != null);
        return Optional.ofNullable(trainee);
    }

    @Override
    public void delete(Trainee trainee) {
        sessionFactory.getCurrentSession().remove(trainee);
        log.debug("Removed trainee with id={}", trainee.getId());
    }
}