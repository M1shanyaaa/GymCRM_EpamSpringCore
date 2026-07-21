package com.epam.gym.dao.impl;

import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.TrainingTypeName;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainingTypeDaoImpl implements TrainingTypeDao {

    private static final Logger log = LoggerFactory.getLogger(TrainingTypeDaoImpl.class);

    private final SessionFactory sessionFactory;

    @Autowired
    public TrainingTypeDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<TrainingType> findByName(TrainingTypeName name) {
        TrainingType result = sessionFactory.getCurrentSession()
                .createQuery("FROM TrainingType t WHERE t.trainingTypeName = :name", TrainingType.class)
                .setParameter("name", name)
                .uniqueResult();
        log.debug("findByName({}) -> found={}", name, result != null);
        return Optional.ofNullable(result);
    }

    @Override
    public List<TrainingType> findAll() {
        List<TrainingType> result = sessionFactory.getCurrentSession()
                .createQuery("FROM TrainingType", TrainingType.class)
                .list();
        log.debug("findAll trainingTypes -> count={}", result.size());
        return result;
    }

    @Override
    public TrainingType save(TrainingType trainingType) {
        sessionFactory.getCurrentSession().persist(trainingType);
        log.debug("Saved trainingType={}", trainingType.getTrainingTypeName());
        return trainingType;
    }
}