package com.epam.gym.dao.impl;

import com.epam.gym.dao.TrainingDao;
import com.epam.gym.model.*;
import jakarta.persistence.criteria.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TrainingDaoImpl implements TrainingDao {

    private static final Logger log = LoggerFactory.getLogger(TrainingDaoImpl.class);

    private final SessionFactory sessionFactory;

    @Autowired
    public TrainingDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    // ---------- Add training (Function 16) ----------
    // Demonstrates manual session/transaction handling via openSession()
    // NOTE: normally we rely on getCurrentSession() + @Transactional (see other DAOs).
    // This is a deliberate example of low-level Hibernate usage.
    @Override
    public Training save(Training training) {
        sessionFactory.getCurrentSession().persist(training);
        log.debug("Persisted training with id={}", training.getId());
        return training;
    }

    // ---------- Trainee trainings by criteria (Function 14) ----------
    // Demonstrates the type-safe Criteria API with dynamic filters.
    @Override
    public List<Training> findTraineeTrainings(String traineeUsername,
                                               LocalDate fromDate,
                                               LocalDate toDate,
                                               String trainerName,
                                               TrainingTypeName trainingType) {
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Training> query = cb.createQuery(Training.class);
        Root<Training> root = query.from(Training.class);

        // joins
        Join<Training, Trainee> trainee = root.join("trainee");
        Join<Trainee, User> traineeUser = trainee.join("user");
        Join<Training, Trainer> trainer = root.join("trainer");
        Join<Trainer, User> trainerUser = trainer.join("user");

        List<Predicate> predicates = new ArrayList<>();
        // mandatory: by trainee username
        predicates.add(cb.equal(traineeUser.get("username"), traineeUsername));

        // optional filters
        if (fromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("trainingDate"), fromDate));
        }
        if (toDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("trainingDate"), toDate));
        }
        if (trainerName != null && !trainerName.isBlank()) {
            predicates.add(cb.equal(trainerUser.get("firstName"), trainerName));
        }
        if (trainingType != null) {
            Join<Training, TrainingType> type = root.join("trainingType");
            predicates.add(cb.equal(type.get("trainingTypeName"), trainingType));
        }

        query.select(root).where(predicates.toArray(new Predicate[0]));

        List<Training> result = session.createQuery(query).list();
        log.debug("findTraineeTrainings(user={}, from={}, to={}, trainer={}, type={}) -> count={}",
                traineeUsername, fromDate, toDate, trainerName, trainingType, result.size());
        return result;
    }

    // ---------- Trainer trainings by criteria (Function 15) ----------
    // Demonstrates dynamic HQL string building with optional filters.
    @Override
    public List<Training> findTrainerTrainings(String trainerUsername,
                                               LocalDate fromDate,
                                               LocalDate toDate,
                                               String traineeName) {
        StringBuilder hql = new StringBuilder(
                "SELECT t FROM Training t " +
                        "JOIN t.trainer tr JOIN tr.user tru " +
                        "JOIN t.trainee te JOIN te.user teu " +
                        "WHERE tru.username = :trainerUsername");

        if (fromDate != null) {
            hql.append(" AND t.trainingDate >= :fromDate");
        }
        if (toDate != null) {
            hql.append(" AND t.trainingDate <= :toDate");
        }
        if (traineeName != null && !traineeName.isBlank()) {
            hql.append(" AND teu.firstName = :traineeName");
        }

        var query = sessionFactory.getCurrentSession()
                .createQuery(hql.toString(), Training.class)
                .setParameter("trainerUsername", trainerUsername);

        if (fromDate != null) {
            query.setParameter("fromDate", fromDate);
        }
        if (toDate != null) {
            query.setParameter("toDate", toDate);
        }
        if (traineeName != null && !traineeName.isBlank()) {
            query.setParameter("traineeName", traineeName);
        }

        List<Training> result = query.list();
        log.debug("findTrainerTrainings(user={}, from={}, to={}, trainee={}) -> count={}",
                trainerUsername, fromDate, toDate, traineeName, result.size());
        return result;
    }
}