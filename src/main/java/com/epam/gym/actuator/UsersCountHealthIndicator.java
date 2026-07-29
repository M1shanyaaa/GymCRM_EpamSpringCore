package com.epam.gym.actuator;

import com.epam.gym.dao.UserDao;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UsersCountHealthIndicator implements HealthIndicator {

    private final UserDao userDao;

    public UsersCountHealthIndicator(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Health health() {
        try {
            long usersCount = userDao.countAll();

            return Health.up()
                    .withDetail("message", "Database is reachable and users table is accessible")
                    .withDetail("total_users", usersCount)
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("message", "Failed to connect to the users table")
                    .build();
        }
    }
}