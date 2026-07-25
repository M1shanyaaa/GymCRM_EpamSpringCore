package com.epam.gym.actuator;

import com.epam.gym.dao.UserDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersCountHealthIndicatorTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UsersCountHealthIndicator indicator;

    @Test
    void health_shouldReturnUp_whenDatabaseReachable() {
        when(userDao.countAll()).thenReturn(42L);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("message", "Database is reachable and users table is accessible");
        assertThat(health.getDetails()).containsEntry("total_users", 42L);
    }

    @Test
    void health_shouldReturnDown_whenDatabaseThrowsException() {
        when(userDao.countAll()).thenThrow(new RuntimeException("Connection Timeout"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("message", "Failed to connect to the users table");
    }
}