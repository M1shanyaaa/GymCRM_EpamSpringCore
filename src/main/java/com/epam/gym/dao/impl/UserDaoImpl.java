package com.epam.gym.dao.impl;

import com.epam.gym.dao.UserDao;
import com.epam.gym.model.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class UserDaoImpl implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<User> findByUsername(String username) {
        Optional<User> user = entityManager
                .createQuery("FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .getResultList()
                .stream()
                .findFirst();
        log.debug("findByUsername({}) -> found={}", username, user.isPresent());
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        Long count = entityManager
                .createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        log.debug("existsByUsername({}) -> {}", username, count > 0);
        return count > 0;
    }

    @Override
    public User update(User user) {
        User merged = entityManager.merge(user);
        log.debug("update(user id={}) -> success", user.getId());
        return merged;
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        Long count = entityManager
                .createQuery("SELECT COUNT(u) FROM User u", Long.class)
                .getSingleResult();
        log.debug("countAll() -> {}", count);
        return count != null ? count : 0L;
    }
}