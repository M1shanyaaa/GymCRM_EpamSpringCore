package com.epam.gym.dao.impl;

import com.epam.gym.dao.UserDao;
import com.epam.gym.model.User;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserDaoImpl implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

    private final SessionFactory sessionFactory;

    @Autowired
    public UserDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        User user = sessionFactory.getCurrentSession()
                .createQuery("FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .uniqueResult();
        log.debug("findByUsername({}) -> found={}", username, user != null);
        return Optional.ofNullable(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        Long count = sessionFactory.getCurrentSession()
                .createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .uniqueResult();
        log.debug("existsByUsername({}) -> {}", username, count > 0);
        return count > 0;
    }

    @Override
    public User update(User user) {
        User merged = sessionFactory.getCurrentSession().merge(user);
        log.debug("update(user id={}) -> success", user.getId());
        return merged;
    }
}